package com.sap.sailing.gwt.ui.regattaoverview;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.i18n.client.constants.TimeZoneConstants;
import com.google.gwt.i18n.shared.DateTimeFormat;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.domain.common.racelog.utils.GeoUtils;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.RaceInfoDTO.GateStartInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceInfoDTO.RRS26InfoDTO;
import com.sap.sailing.gwt.ui.shared.RegattaOverviewEntryDTO;

public class RaceDetailPanel extends SimplePanel {

    private static final int MAX_PROCEDURE_FIELDS = 2;
    private final DateTimeFormat timeFormatter = DateTimeFormat.getFormat("HH:mm:ss");
    private final DateTimeFormat dateFormatter = DateTimeFormat.getFormat("dd.MM.yyyy");
    private final DateTimeFormat durationFormatter = DateTimeFormat.getFormat("H'h' m'min' s'sec'");
    private final NumberFormat decimalFormat = NumberFormat.getFormat("#.##");
    private final FlagAlphabetInterpreter flagInterpreter;
    private final StringMessages stringMessages;

    private final Button closeButton;
    private final Label headerLabel = new Label();
    private final Label startTimeLabel = new Label();
    private final Label startDateLabel = new Label();
    private final Label finishTimeLabel = new Label();
    private final Label finishDurationLabel = new Label();
    private final Label protestTimeLabel = new Label();
    private final Label updateTimeLabel = new Label();
    private final Label vesselPositionLabel = new Label();
    private final Label windLabel = new Label();
    private final Label startProcedureLabel = new Label();

    private FlexTable procedureGrid;

    private RegattaOverviewEntryDTO data;

    public RaceDetailPanel(final StringMessages stringMessages, final ClickHandler closeButtonHandler) {
        this.stringMessages = stringMessages;
        this.flagInterpreter = new FlagAlphabetInterpreter(stringMessages);
        closeButton = new Button(stringMessages.close());
        closeButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                data = null;
                if (closeButtonHandler != null) {
                    closeButton.addClickHandler(closeButtonHandler);
                }
            }
        });

        this.addStyleName("RaceDetailPanel");

        HorizontalPanel headerPanel = new HorizontalPanel();
        headerPanel.addStyleName("RaceDetailPanel-header");
        headerPanel.setWidth("100%");
        headerPanel.add(headerLabel);
        headerPanel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_RIGHT);
        headerPanel.add(closeButton);

        int cellSpacing = 3;

        Grid basicsGrid = new Grid(3, 3);
        basicsGrid.setCellSpacing(cellSpacing);
        basicsGrid.setWidget(0, 0, new Label(stringMessages.startAt()));
        basicsGrid.setWidget(1, 0, new Label(stringMessages.finishAt()));
        basicsGrid.setWidget(2, 0, new Label(stringMessages.protestEndsAt()));
        basicsGrid.setWidget(0, 1, startTimeLabel);
        basicsGrid.setWidget(1, 1, finishTimeLabel);
        basicsGrid.setWidget(2, 1, protestTimeLabel);
        basicsGrid.setWidget(0, 2, startDateLabel);
        basicsGrid.setWidget(1, 2, finishDurationLabel);
        basicsGrid.setWidget(2, 2, new Label(""));

        Grid managementGrid = new Grid(3, 2);
        managementGrid.setCellSpacing(cellSpacing);
        managementGrid.setWidget(0, 0, new Label("Last update at"));
        managementGrid.setWidget(1, 0, new Label("Vessel position"));
        managementGrid.setWidget(2, 0, new Label("Last wind"));
        managementGrid.setWidget(0, 1, updateTimeLabel);
        managementGrid.setWidget(1, 1, vesselPositionLabel);
        managementGrid.setWidget(2, 1, windLabel);

        procedureGrid = new FlexTable();
        procedureGrid.setCellSpacing(cellSpacing);
        resetProcedureGrid();

        HorizontalPanel contentPanel = new HorizontalPanel();
        contentPanel.addStyleName("RaceDetailPanel-content");
        contentPanel.add(basicsGrid);
        contentPanel.add(managementGrid);
        contentPanel.add(procedureGrid);

        VerticalPanel mainPanel = new VerticalPanel();
        mainPanel.setWidth("100%");
        mainPanel.add(headerPanel);
        mainPanel.add(contentPanel);
        setWidget(mainPanel);
    }

    private void resetProcedureGrid() {
        procedureGrid.removeAllRows();
        procedureGrid.setWidget(0, 0, new Label("Start procedure"));
        procedureGrid.setWidget(0, 1, startProcedureLabel);
    }

    public void show(RegattaOverviewEntryDTO newEntry) {
        data = newEntry;
        updateUi();
    }

    private void updateUi() {
        if (data == null) {
            return;
        }
        String statusText = flagInterpreter.getMeaningOfRaceStateAndFlags(data.raceInfo.lastStatus,
                data.raceInfo.lastUpperFlag, data.raceInfo.lastLowerFlag, data.raceInfo.isLastFlagDisplayed);
        String headerLabelText = data.regattaDisplayName + " " + data.raceInfo.fleetName + " " + data.raceInfo.raceName
                + " (" + statusText + ")";
        headerLabel.setText(stringMessages.showingDetailsOfRace(headerLabelText));

        String startTimeLabelText = data.raceInfo.startTime == null ? "-" : timeFormatter
                .format(data.raceInfo.startTime);
        startTimeLabel.setText(startTimeLabelText);

        String startDateLabelText = data.raceInfo.startTime == null ? "" : dateFormatter
                .format(data.raceInfo.startTime);
        startDateLabel.setText(startDateLabelText);

        String finishTimeText = data.raceInfo.finishedTime == null ? "-" : timeFormatter
                .format(data.raceInfo.finishedTime);
        finishTimeLabel.setText(finishTimeText);

        String finishDurationText = "";
        if (data.raceInfo.finishedTime != null && data.raceInfo.startTime != null) {
            Date raceDuration = new Date(data.raceInfo.finishedTime.getTime() - data.raceInfo.startTime.getTime());
            finishDurationText = "(" + durationFormatter.format(raceDuration) + ")";
        }
        finishDurationLabel.setText(finishDurationText);

        String protestTimeText = data.raceInfo.protestFinishTime == null ? "-" : timeFormatter
                .format(data.raceInfo.protestFinishTime);
        protestTimeLabel.setText(protestTimeText);

        String updateTimeText = data.raceInfo.lastUpdateTime == null ? "-" : timeFormatter
                .format(data.raceInfo.lastUpdateTime);
        updateTimeLabel.setText(updateTimeText);

        String vesselPositionText = stringMessages.unknown();
        String windText = stringMessages.unknown();
        if (data.raceInfo.lastWind != null) {
            if (data.raceInfo.lastWind.position != null) {
                vesselPositionText = new HTML(GeoUtils.getDegMinSecFormatForDecimalDegree(data.raceInfo.lastWind.position.latDeg)+" "
                        + GeoUtils.getDegMinSecFormatForDecimalDegree(data.raceInfo.lastWind.position.lngDeg)+" ").getHTML();
            }
            windText = new HTML(decimalFormat.format(data.raceInfo.lastWind.trueWindFromDeg) + "&deg; "
                    + decimalFormat.format(data.raceInfo.lastWind.trueWindSpeedInKnots) + "knts").getHTML();
        }
        vesselPositionLabel.setText(vesselPositionText);
        windLabel.setText(windText);

        updateUiStartProcedureSpecifics();
    }

    final TimeZoneConstants timeZoneConstants = GWT.create(TimeZoneConstants.class);

    private void updateUiStartProcedureSpecifics() {
        resetProcedureGrid();
        if (data.raceInfo.startProcedure != null) {
            startProcedureLabel.setText(data.raceInfo.startProcedure.toString());
            if (data.raceInfo.startProcedureDTO != null) {
                switch (data.raceInfo.startProcedure) {
                case RRS26:
                    RRS26InfoDTO rrsInfo = (RRS26InfoDTO) data.raceInfo.startProcedureDTO;
                    if (rrsInfo != null) {
                        if (rrsInfo.startModeFlag != null) {
                            showProcedureInfo("Start mode", rrsInfo.startModeFlag.toString());
                        }
                    }
                    break;
                case GateStart:
                    GateStartInfoDTO gateInfo = (GateStartInfoDTO) data.raceInfo.startProcedureDTO;
                    if (gateInfo != null) {
                        if (gateInfo.pathfinderId != null) {
                            showProcedureInfo("Pathfinder", gateInfo.pathfinderId);
                        }
                        if (gateInfo.gateLineOpeningTime != null) {
                            showProcedureInfo("Gate Opening Time", (int) (gateInfo.gateLineOpeningTime / 1000. / 60.)
                                    + "min");
                        }
                    }
                    break;
                default:
                    break;
                }
            }
        } else {
            startProcedureLabel.setText("-");
        }
    }

    private void showProcedureInfo(String label, String value) {
        for (int i = 1; i < MAX_PROCEDURE_FIELDS + 1; i++) {
            if (!procedureGrid.isCellPresent(i, 0)) {
                procedureGrid.setWidget(i, 0, new Label(SafeHtmlUtils.fromString(label).asString()));
                procedureGrid.setWidget(i, 1, new Label(SafeHtmlUtils.fromString(value).asString()));
                return;
            }
        }
        throw new IllegalStateException("There are too many procedure specific fields.");
    }

    /**
     * Announces an update to a {@link RegattaOverviewEntryDTO}. Updates the UI of this {@link RaceDetailPanel} if
     * passed entry represents the same race as currently shown entry.
     * 
     * @param updated
     *            newEntry
     * @return <code>true</code> if UI is updated; otherwise <code>false</code>.
     */
    public boolean update(RegattaOverviewEntryDTO newEntry) {
        if (areOfSameRace(data, newEntry)) {
            show(newEntry);
            return true;
        }
        return false;
    }

    private static boolean areOfSameRace(RegattaOverviewEntryDTO left, RegattaOverviewEntryDTO right) {
        if (left == null || right == null) {
            return false;
        }

        return bothNullOrEquals(left.courseAreaIdAsString, right.courseAreaIdAsString)
                && bothNullOrEquals(left.regattaName, right.regattaName)
                && bothNullOrEquals(left.raceInfo.seriesName, right.raceInfo.seriesName)
                && bothNullOrEquals(left.raceInfo.fleetName, right.raceInfo.fleetName)
                && bothNullOrEquals(left.raceInfo.raceName, right.raceInfo.raceName);
    }

    private static boolean bothNullOrEquals(Object left, Object right) {
        return left == null ? right == null : left.equals(right);
    }

}