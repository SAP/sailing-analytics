package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SelectionModel;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnInSeriesDTO;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.MarkedAsyncCallback;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.ScoringSchemeTypeFormatter;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sse.gwt.ui.DataEntryDialog.DialogCallback;


public class RegattaDetailsComposite extends Composite {
    private RegattaDTO regatta;

    private final CaptionPanel mainPanel;

    private final SailingServiceAsync sailingService;
    private final ErrorReporter errorReporter;
    private final StringMessages stringMessages;
    private final RegattaRefresher regattaRefresher;
    
    private final Label regattaName;
    private final Label boatClassName;
    private final Label scoringSystem;
    private final Label defaultCourseArea;
    private final Label configuration;

    private final SelectionModel<SeriesDTO> seriesSelectionModel;
    private final CellTable<SeriesDTO> seriesTable;
    private ListDataProvider<SeriesDTO> seriesListDataProvider;

    private static AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);

    public RegattaDetailsComposite(final SailingServiceAsync sailingService, final RegattaRefresher regattaRefresher,  
            final ErrorReporter errorReporter, final StringMessages stringMessages) {
        this.sailingService = sailingService;
        this.regattaRefresher = regattaRefresher;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        regatta = null;
        mainPanel = new CaptionPanel(stringMessages.regatta());
        VerticalPanel vPanel = new VerticalPanel();
        mainPanel.add(vPanel);
        Grid grid = new Grid(5, 2);
        vPanel.add(grid);
        
        regattaName = new Label();
        regattaName.ensureDebugId("NameLabel");
        grid.setWidget(0 , 0, new Label(stringMessages.regattaName() + ":"));
        grid.setWidget(0 , 1, regattaName);
        
        boatClassName = new Label();
        boatClassName.ensureDebugId("BoatClassLabel");
        grid.setWidget(1 , 0, new Label(stringMessages.boatClass() + ":"));
        grid.setWidget(1 , 1, boatClassName);
        
        defaultCourseArea = new Label();
        defaultCourseArea.ensureDebugId("CourseAreaLabel");
        grid.setWidget(2 , 0, new Label(stringMessages.courseArea() + ":"));
        grid.setWidget(2 , 1, defaultCourseArea);
        
        configuration = new Label();
        configuration.ensureDebugId("RacingProcedureLabel");
        grid.setWidget(3, 0, new Label(stringMessages.racingProcedureConfiguration() + ":"));
        grid.setWidget(3, 1, configuration);
        
        scoringSystem = new Label();
        scoringSystem.ensureDebugId("ScoringSystemLabel");
        grid.setWidget(4 , 0, new Label(stringMessages.scoringSystem() + ":"));
        grid.setWidget(4 , 1, scoringSystem);
        
        seriesTable = createRegattaSeriesTable();
        seriesTable.ensureDebugId("SeriesCellTable");
        seriesSelectionModel = new SingleSelectionModel<SeriesDTO>();
        seriesSelectionModel.addSelectionChangeHandler(new SelectionChangeEvent.Handler() {
            @Override
            public void onSelectionChange(SelectionChangeEvent event) {
            }
        });
        seriesTable.setSelectionModel(seriesSelectionModel);
        seriesListDataProvider = new ListDataProvider<SeriesDTO>();
        seriesListDataProvider.addDataDisplay(seriesTable);
        vPanel.add(seriesTable);
        
        initWidget(mainPanel);
    }

    private CellTable<SeriesDTO> createRegattaSeriesTable() {
        CellTable<SeriesDTO> table = new CellTable<SeriesDTO>(/* pageSize */10000, tableRes);
        table.setWidth("100%");
        
        TextColumn<SeriesDTO> seriesNameColumn = new TextColumn<SeriesDTO>() {
            @Override
            public String getValue(SeriesDTO series) {
                return series.getName();
            }
        };

        TextColumn<SeriesDTO> isMedalSeriesColumn = new TextColumn<SeriesDTO>() {
            @Override
            public String getValue(SeriesDTO series) {
                return series.isMedal() ? stringMessages.yes() : stringMessages.no();
            }
        };

        TextColumn<SeriesDTO> startsWithZeroScoreColumn = new TextColumn<SeriesDTO>() {
            @Override
            public String getValue(SeriesDTO series) {
                return series.isStartsWithZeroScore() ? stringMessages.yes() : stringMessages.no();
            }
        };

        TextColumn<SeriesDTO> hasSplitFleetContiguousScoringColumn = new TextColumn<SeriesDTO>() {
            @Override
            public String getValue(SeriesDTO series) {
                return series.hasSplitFleetContiguousScoring() ? stringMessages.yes() : stringMessages.no();
            }
        };

        TextColumn<SeriesDTO> isFirstColumnIsNonDiscardableCarryForwardColumn = new TextColumn<SeriesDTO>() {
            @Override
            public String getValue(SeriesDTO series) {
                return series.isFirstColumnIsNonDiscardableCarryForward() ? stringMessages.yes() : stringMessages.no();
            }
        };

        TextColumn<SeriesDTO> racesColumn = new TextColumn<SeriesDTO>() {
            @Override
            public String getValue(SeriesDTO series) {
                String result = "";
                if(!series.getRaceColumns().isEmpty()) {
                    int raceColumnsCount = series.getRaceColumns().size();
                    int i = 1;
                    for(RaceColumnDTO raceColumn: series.getRaceColumns()) {
                        result += raceColumn.getRaceColumnName();
                        if(i < raceColumnsCount) {
                            result += ", ";
                        }
                        i++;
                    }
                } else {
                    result = stringMessages.noRacesYet();
                }
                return result;
            }
        };
        
        TextColumn<SeriesDTO> fleetsColumn = new TextColumn<SeriesDTO>() {
            @Override
            public String getValue(SeriesDTO series) {
                String result = "";
                int fleetsCount = series.getFleets().size();
                int i = 1;
                for(FleetDTO fleet: series.getFleets()) {
                    result += fleet.getName();
                    result += "(" + fleet.getOrderNo() + ") ";
                    if (i < fleetsCount) {
                        result += ", ";
                    }
                    i++;
                }
                return result;
            }
        };

        TextColumn<SeriesDTO> discardsColumn = new TextColumn<SeriesDTO>() {
            @Override
            public String getValue(SeriesDTO series) {
                final String result;
                if (series.getDiscardThresholds() == null) {
                    result = stringMessages.no();
                } else {
                    StringBuilder sb = new StringBuilder();
                    boolean first = true;
                    for (int threshold : series.getDiscardThresholds()) {
                        if (first) {
                            first = false;
                        } else {
                            sb.append(", ");
                        }
                        sb.append(threshold);
                    }
                    result = sb.toString();
                }
                return result;
            }
        };

        ImagesBarColumn<SeriesDTO, SeriesConfigImagesBarCell> seriesActionColumn = new ImagesBarColumn<SeriesDTO, SeriesConfigImagesBarCell>(
                new SeriesConfigImagesBarCell(stringMessages));
        seriesActionColumn.setFieldUpdater(new FieldUpdater<SeriesDTO, String>() {
            @Override
            public void update(int index, final SeriesDTO series, String value) {
                if (SeriesConfigImagesBarCell.ACTION_EDIT.equals(value)) {
                    editRacesOfRegattaSeries(regatta, series);
                } else if (SeriesConfigImagesBarCell.ACTION_REMOVE.equals(value)) {
                    RegattaIdentifier identifier = new RegattaName(regatta.getName());
                    sailingService.removeSeries(identifier, series.getName(), new MarkedAsyncCallback<Void>(
                            new AsyncCallback<Void>() {
                                @Override
                                public void onFailure(Throwable cause) {
                                    errorReporter.reportError("Error trying to remove series " + series.getName()
                                            + ": " + cause.getMessage());
                                }
        
                                @Override
                                public void onSuccess(Void result) {
                                    regattaRefresher.fillRegattas();
                                }
                    }));
                }

            }
        });
        
        table.addColumn(seriesNameColumn, stringMessages.series());
        table.addColumn(isMedalSeriesColumn, stringMessages.medalSeries());
        table.addColumn(racesColumn, stringMessages.races());
        table.addColumn(fleetsColumn, stringMessages.fleets());
        table.addColumn(discardsColumn, stringMessages.discarding());
        table.addColumn(isFirstColumnIsNonDiscardableCarryForwardColumn, stringMessages.firstRaceIsNonDiscardableCarryForward());
        table.addColumn(startsWithZeroScoreColumn, stringMessages.startsWithZeroScore());
        table.addColumn(hasSplitFleetContiguousScoringColumn, stringMessages.hasSplitFleetContiguousScoring());
        table.addColumn(seriesActionColumn, stringMessages.actions());
        
        return table;
    }
    
    public RegattaDTO getRegatta() {
        return regatta;
    }

    public void setRegatta(RegattaDTO regatta) {
        this.regatta = regatta;
        updateRegattaDetails();
    }

    private void editRacesOfRegattaSeries(final RegattaDTO regatta, final SeriesDTO series) {
        SeriesEditDialog raceDialog = new SeriesEditDialog(regatta, series, stringMessages, 
                new DialogCallback<SeriesDescriptor>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(final SeriesDescriptor result) {
                        updateRacesOfRegattaSeries(regatta, result);
                    }
                });
        raceDialog.ensureDebugId("SeriesEditDialog");
        raceDialog.show();
    }

    private void updateRacesOfRegattaSeries(final RegattaDTO regatta, final SeriesDescriptor seriesDescriptor) {
        final SeriesDTO series = seriesDescriptor.getSeries();
        final List<RaceColumnDTO> newRaceColumns = seriesDescriptor.getRaces();
        final boolean isMedalChanged = series.isMedal() != seriesDescriptor.isMedal();
        final boolean isStartsWithZeroScoreChanged = series.isStartsWithZeroScore() != seriesDescriptor.isStartsWithZeroScore();
        final boolean isFirstColumnIsNonDiscardableCarryForwardChanged = series.isFirstColumnIsNonDiscardableCarryForward() != seriesDescriptor.isFirstColumnIsNonDiscardableCarryForward();
        final boolean hasSplitFleetContiguousScoringChanged = series.hasSplitFleetContiguousScoring() != seriesDescriptor.hasSplitFleetContiguousScoring();
        final boolean seriesResultDiscardingThresholdsChanged = !Arrays.equals(series.getDiscardThresholds(),
                seriesDescriptor.getResultDiscardingThresholds());       
        final boolean seriesNameChanged = !series.getName().equals(seriesDescriptor.getSeriesName());
        final RegattaIdentifier regattaIdentifier = new RegattaName(regatta.getName());
        List<RaceColumnDTO> existingRaceColumns = series.getRaceColumns();
        final List<String> raceColumnsToAdd = new ArrayList<String>();
        final List<String> raceColumnsToRemove = new ArrayList<String>();
        
        // TODO see bug 1447: the resulting order currently doesn't necessarily match the order of races in this dialog!
        for (RaceColumnDTO newRaceColumn : newRaceColumns) {
            if (!existingRaceColumns.contains(newRaceColumn)) {
                raceColumnsToAdd.add(newRaceColumn.getName());
            }
        }
        for (RaceColumnDTO existingRaceColumn : existingRaceColumns) {
            if (!newRaceColumns.contains(existingRaceColumn)) {
                raceColumnsToRemove.add(existingRaceColumn.getName());
            }
        }
        StringBuilder racesToRemove = new StringBuilder();
        boolean first = true;
        for (String raceColumnToRemove : raceColumnsToRemove) {
            if (first) {
                first = false;
            } else {
                racesToRemove.append(", ");
            }
            racesToRemove.append(raceColumnToRemove);
        }
        if (Window.confirm(stringMessages.reallyRemoveRace(racesToRemove.toString()))) {
            sailingService.addRaceColumnsToSeries(regattaIdentifier, series.getName(), raceColumnsToAdd,
                    new AsyncCallback<List<RaceColumnInSeriesDTO>>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError("Error trying to add race columns " + raceColumnsToAdd
                                    + " to series " + series.getName() + ": " + caught.getMessage());

                        }

                        @Override
                        public void onSuccess(List<RaceColumnInSeriesDTO> raceColumns) {
                            regattaRefresher.fillRegattas();
                        }
                    });

            sailingService.removeRaceColumnsFromSeries(regattaIdentifier, series.getName(), raceColumnsToRemove,
                    new AsyncCallback<Void>() {
                        @Override
                        public void onFailure(Throwable caught) {
                            errorReporter.reportError("Error trying to remove race columns " + raceColumnsToAdd
                                    + " from series " + series.getName() + ": " + caught.getMessage());
                        }

                        @Override
                        public void onSuccess(Void v) {
                            regattaRefresher.fillRegattas();
                        }
                    });
            if (isMedalChanged || seriesResultDiscardingThresholdsChanged || isStartsWithZeroScoreChanged
                    || isFirstColumnIsNonDiscardableCarryForwardChanged || hasSplitFleetContiguousScoringChanged
                    || seriesNameChanged) {
                sailingService.updateSeries(regattaIdentifier, series.getName(), seriesDescriptor.getSeriesName(),
                        seriesDescriptor.isMedal(), seriesDescriptor.getResultDiscardingThresholds(),
                        seriesDescriptor.isStartsWithZeroScore(),
                        seriesDescriptor.isFirstColumnIsNonDiscardableCarryForward(),
                        seriesDescriptor.hasSplitFleetContiguousScoring(), series.getFleets(),
                        new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Error trying to update series " + series.getName() + ": "
                                        + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(Void result) {
                                regattaRefresher.fillRegattas();
                            }
                        });
            }
        }
    }

    private void updateRegattaDetails() {
        if (regatta != null) {
            regattaName.setText(regatta.getName());
            boatClassName.setText(regatta.boatClass != null ? regatta.boatClass.getName() : "");
            defaultCourseArea.setText(regatta.defaultCourseAreaUuid == null ? "" : regatta.defaultCourseAreaName);
            if (regatta.configuration != null) {
                configuration.setText(stringMessages.configured());
            } else {
                configuration.setText(stringMessages.none());
            }
            ScoringSchemeType scoringScheme = regatta.scoringScheme;
            String scoringSystemText = scoringScheme == null ? "" : ScoringSchemeTypeFormatter.format(scoringScheme, stringMessages);               
            scoringSystem.setText(scoringSystemText);
            seriesListDataProvider.getList().clear();
            seriesListDataProvider.getList().addAll(regatta.series);
        } 
    }
}
