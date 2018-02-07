package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList; 
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.FieldUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent.ListHandler;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.RegattasDisplayer;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.common.client.DateAndTimeFormatterUtil;
import com.sap.sailing.gwt.ui.leaderboard.RankingMetricTypeFormatter;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RegattaDTO;
import com.sap.sailing.gwt.ui.shared.SeriesDTO;
import com.sap.sse.common.Util;
import com.sap.sse.common.util.NaturalComparator;
import com.sap.sse.gwt.adminconsole.AdminConsoleTableResources;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.celltable.EntityIdentityComparator;
import com.sap.sse.gwt.client.celltable.FlushableCellTable;
import com.sap.sse.gwt.client.celltable.ImagesBarColumn;
import com.sap.sse.gwt.client.celltable.RefreshableMultiSelectionModel;
import com.sap.sse.gwt.client.celltable.SelectionCheckboxColumn;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;

/**
 * A composite showing the list of all regattas 
 * @author Frank
 *
 */
public class RegattaListComposite extends Composite implements RegattasDisplayer {
    protected final CellTable<RegattaDTO> regattaTable;
    protected final ListDataProvider<RegattaDTO> regattaListDataProvider;
    private List<RegattaDTO> allRegattas;
    private final SimplePanel mainPanel;
    private final VerticalPanel panel;

    private final Label noRegattasLabel;

    private final SailingServiceAsync sailingService;
    protected final RefreshableMultiSelectionModel<RegattaDTO> refreshableRegattaMultiSelectionModel;
    private final ErrorReporter errorReporter;
    private final RegattaRefresher regattaRefresher;
    protected final StringMessages stringMessages;

    private final LabeledAbstractFilterablePanel<RegattaDTO> filterablePanelRegattas;

    protected static AdminConsoleTableResources tableRes = GWT.create(AdminConsoleTableResources.class);

    public static class AnchorCell extends AbstractCell<SafeHtml> {
        @Override
        public void render(com.google.gwt.cell.client.Cell.Context context, SafeHtml safeHtml, SafeHtmlBuilder sb) {
            sb.append(safeHtml);
        }
    }

    @SuppressWarnings("unchecked")
    public RegattaListComposite(final SailingServiceAsync sailingService, RegattaRefresher regattaRefresher,
            final ErrorReporter errorReporter, final StringMessages stringMessages) {
        this.sailingService = sailingService;
        this.regattaRefresher = regattaRefresher;
        this.errorReporter = errorReporter;
        this.stringMessages = stringMessages;
        allRegattas = new ArrayList<RegattaDTO>();
        
        mainPanel = new SimplePanel();
        panel = new VerticalPanel();
        mainPanel.setWidget(panel);
        Label filterRegattasLabel = new Label(stringMessages.filterRegattasByName() + ":");
        filterRegattasLabel.setWordWrap(false);
        noRegattasLabel = new Label(stringMessages.noRegattasYet());
        noRegattasLabel.ensureDebugId("NoRegattasLabel");
        noRegattasLabel.setWordWrap(false);
        panel.add(noRegattasLabel);

        regattaListDataProvider = new ListDataProvider<RegattaDTO>();
        
        filterablePanelRegattas = new LabeledAbstractFilterablePanel<RegattaDTO>(filterRegattasLabel, allRegattas,
                new CellTable<RegattaDTO>(), regattaListDataProvider) {
            @Override
            public Iterable<String> getSearchableStrings(RegattaDTO t) {
                List<String> string = new ArrayList<String>();
                string.add(t.getName());
                if (t.boatClass != null) {
                    string.add(t.boatClass.getName());
                }
                return string;
            }
        };
        filterablePanelRegattas.getTextBox().ensureDebugId("RegattasFilterTextBox");
        regattaTable = createRegattaTable();
        regattaTable.ensureDebugId("RegattasCellTable");
        filterablePanelRegattas.setTable(regattaTable);
        refreshableRegattaMultiSelectionModel = (RefreshableMultiSelectionModel<RegattaDTO>) regattaTable.getSelectionModel();
        regattaTable.setVisible(false);
        panel.add(filterablePanelRegattas);

        panel.add(regattaTable);
        initWidget(mainPanel);
    }
    
    public HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler) {
        return refreshableRegattaMultiSelectionModel.addSelectionChangeHandler(handler);
    }

    protected CellTable<RegattaDTO> createRegattaTable() {
        FlushableCellTable<RegattaDTO> table = new FlushableCellTable<RegattaDTO>(/* pageSize */10000, tableRes);
        regattaListDataProvider.addDataDisplay(table);
        table.setWidth("100%");

        SelectionCheckboxColumn<RegattaDTO> regattaSelectionCheckboxColumn = new SelectionCheckboxColumn<RegattaDTO>(
                tableRes.cellTableStyle().cellTableCheckboxSelected(),
                tableRes.cellTableStyle().cellTableCheckboxDeselected(),
                tableRes.cellTableStyle().cellTableCheckboxColumnCell(), new EntityIdentityComparator<RegattaDTO>() {
                    @Override
                    public boolean representSameEntity(RegattaDTO dto1, RegattaDTO dto2) {
                        return dto1.getRegattaIdentifier().equals(dto2.getRegattaIdentifier());
                    }
                    @Override
                    public int hashCode(RegattaDTO t) {
                        return t.getRegattaIdentifier().hashCode();
                    }
                }, filterablePanelRegattas.getAllListDataProvider(), table);
        
        ListHandler<RegattaDTO> columnSortHandler = new ListHandler<RegattaDTO>(regattaListDataProvider.getList());
        table.addColumnSortHandler(columnSortHandler);
        columnSortHandler.setComparator(regattaSelectionCheckboxColumn, regattaSelectionCheckboxColumn.getComparator());

        TextColumn<RegattaDTO> regattaNameColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return regatta.getName();
            }
        };
        regattaNameColumn.setSortable(true);
        columnSortHandler.setComparator(regattaNameColumn, new Comparator<RegattaDTO>() {
            @Override
            public int compare(RegattaDTO r1, RegattaDTO r2) {
                return new NaturalComparator().compare(r1.getName(), r2.getName());
            }
        });

        TextColumn<RegattaDTO> startEndDateColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return DateAndTimeFormatterUtil.formatDateRange(regatta.startDate, regatta.endDate);
            }
        };
        startEndDateColumn.setSortable(true);
        columnSortHandler.setComparator(startEndDateColumn, new Comparator<RegattaDTO>() {
            @Override
            public int compare(RegattaDTO r1, RegattaDTO r2) {
                int result;
                if(r1.startDate != null && r2.startDate != null) {
                    result = r2.startDate.compareTo(r1.startDate);
                } else if(r1.startDate == null && r2.startDate != null) {
                    result = 1;
                } else if(r1.startDate != null && r2.startDate == null) {
                    result = -1;
                } else {
                    result = 0;
                }
                return result;
            }
        });

        TextColumn<RegattaDTO> regattaBoatClassColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return regatta.boatClass != null ? regatta.boatClass.getName() : "";
            }
        };
        regattaBoatClassColumn.setSortable(true);
        columnSortHandler.setComparator(regattaBoatClassColumn, new Comparator<RegattaDTO>() {
            @Override
            public int compare(RegattaDTO r1, RegattaDTO r2) {
                return new NaturalComparator(false).compare(r1.boatClass.getName(), r2.boatClass.getName());
            }
        });

        TextColumn<RegattaDTO> rankingMetricColumn = new TextColumn<RegattaDTO>() {
            @Override
            public String getValue(RegattaDTO regatta) {
                return regatta.rankingMetricType != null ? RankingMetricTypeFormatter.format(regatta.rankingMetricType, stringMessages) : "";
            }
        };
        rankingMetricColumn.setSortable(true);
        columnSortHandler.setComparator(rankingMetricColumn, new Comparator<RegattaDTO>() {
            @Override
            public int compare(RegattaDTO r1, RegattaDTO r2) {
                return new NaturalComparator(false).compare(r1.rankingMetricType.name(), r2.rankingMetricType.name());
            }
        });

        ImagesBarColumn<RegattaDTO, RegattaConfigImagesBarCell> regattaActionColumn = new ImagesBarColumn<RegattaDTO, RegattaConfigImagesBarCell>(
                new RegattaConfigImagesBarCell(stringMessages));
        regattaActionColumn.setFieldUpdater(new FieldUpdater<RegattaDTO, String>() {
            @Override
            public void update(int index, RegattaDTO regatta, String value) {
                if (RegattaConfigImagesBarCell.ACTION_EDIT.equals(value)) {
                    editRegatta(regatta);
                } else if (RegattaConfigImagesBarCell.ACTION_REMOVE.equals(value)) {
                    
                    if (Window.confirm(stringMessages.doYouReallyWantToRemoveRegatta(regatta.getName()))) {
                        removeRegatta(regatta);
                    }
                }
            }
        });

        table.addColumn(regattaSelectionCheckboxColumn, regattaSelectionCheckboxColumn.getHeader());
        table.addColumn(regattaNameColumn, stringMessages.regattaName());
        table.addColumn(startEndDateColumn, stringMessages.from() + "/" + stringMessages.to());
        table.addColumn(regattaBoatClassColumn, stringMessages.boatClass());
        table.addColumn(rankingMetricColumn, stringMessages.rankingMetric());
        table.addColumn(regattaActionColumn, stringMessages.actions());
        table.setSelectionModel(regattaSelectionCheckboxColumn.getSelectionModel(), regattaSelectionCheckboxColumn.getSelectionManager());
        return table;
    }

    private void removeRegatta(final RegattaDTO regatta) {
        final RegattaIdentifier regattaIdentifier = new RegattaName(regatta.getName());
        sailingService.removeRegatta(regattaIdentifier, new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
            @Override
            public void onFailure(Throwable caught) {
                errorReporter.reportError("Error trying to remove regatta " + regatta.getName() + ": "
                        + caught.getMessage());
            }

            @Override
            public void onSuccess(Void result) {
                regattaRefresher.fillRegattas();
            }
        }));
    }

    private void editRegatta(final RegattaDTO toBeEdited) {
        final Collection<RegattaDTO> existingRegattas = getAllRegattas();
        sailingService.getEvents(new MarkedAsyncCallback<List<EventDTO>>(new AsyncCallback<List<EventDTO>>() {
            @Override
            public void onFailure(Throwable caught) {
                openEditRegattaDialog(toBeEdited, existingRegattas, Collections.<EventDTO> emptyList());
            }

            @Override
            public void onSuccess(List<EventDTO> events) {
                openEditRegattaDialog(toBeEdited, existingRegattas, Collections.unmodifiableList(events));
            }
        }));
    }

    private void openEditRegattaDialog(RegattaDTO regatta, Collection<RegattaDTO> existingRegattas,
            List<EventDTO> existingEvents) {
        RegattaWithSeriesAndFleetsDialog dialog = new RegattaWithSeriesAndFleetsEditDialog(regatta, existingRegattas,
                existingEvents, /*correspondingEvent*/ null, stringMessages, new DialogCallback<RegattaDTO>() {
                    @Override
                    public void cancel() {
                    }

                    @Override
                    public void ok(RegattaDTO editedRegatta) {
                        commitEditedRegatta(editedRegatta);
                    }
                });
        dialog.show();
    }

    private void commitEditedRegatta(final RegattaDTO editedRegatta) {
        final RegattaIdentifier regattaName = new RegattaName(editedRegatta.getName());

        sailingService.updateRegatta(regattaName, editedRegatta.startDate, editedRegatta.endDate, editedRegatta.defaultCourseAreaUuid,
                editedRegatta.configuration, editedRegatta.buoyZoneRadiusInHullLengths, editedRegatta.useStartTimeInference, editedRegatta.controlTrackingFromStartAndFinishTimes,
                new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error trying to update regatta " + editedRegatta.getName() + ": "
                                + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Void result) {
                        regattaRefresher.fillRegattas();
                    }
                }));

        final Iterator<SeriesDTO> seriesIter = editedRegatta.series.iterator();
        final Runnable r = new Runnable() {
            @Override
            public void run() {
                if (seriesIter.hasNext()) {
                    final SeriesDTO series = seriesIter.next();
                    sailingService.updateSeries(regattaName, series.getName(), series.getName(), series.isMedal(),
                        series.isFleetsCanRunInParallel(), series.getDiscardThresholds(), series.isStartsWithZeroScore(),
                        series.isFirstColumnIsNonDiscardableCarryForward(), series.hasSplitFleetContiguousScoring(),
                        series.getMaximumNumberOfDiscards(), series.getFleets(), new MarkedAsyncCallback<Void>(new AsyncCallback<Void>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                errorReporter.reportError("Error trying to update regatta " + editedRegatta.getName()
                                        + ": " + caught.getMessage());
                            }
    
                            @Override
                            public void onSuccess(Void result) {
                                regattaRefresher.fillRegattas();
                                run(); // update next series if iterator has next element
                            }
                        }));
                }
            }
        };
        r.run();
    }

    protected List<RegattaDTO> getSelectedRegattas() {
        return new ArrayList<RegattaDTO>(refreshableRegattaMultiSelectionModel.getSelectedSet());
    }

    @Override
    public void fillRegattas(Iterable<RegattaDTO> regattas) {
        if (Util.isEmpty(regattas)) {
            regattaTable.setVisible(false);
            noRegattasLabel.setVisible(true);
        } else {
            regattaTable.setVisible(true);
            noRegattasLabel.setVisible(false);
        }
        List<RegattaDTO> newAllRegattas = new ArrayList<RegattaDTO>();
        Util.addAll(regattas, newAllRegattas);
        allRegattas = newAllRegattas;
        filterablePanelRegattas.updateAll(allRegattas);
    }

    public List<RegattaDTO> getAllRegattas() {
        return allRegattas;
    }

    public RefreshableMultiSelectionModel<RegattaDTO> getRefreshableMultiSelectionModel() {
        return refreshableRegattaMultiSelectionModel;
    }

    public CellTable<RegattaDTO> getRegattaTable() {
        return regattaTable;
    }
}
