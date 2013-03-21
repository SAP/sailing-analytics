package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Date;

import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.i18n.client.TimeZone;
import com.google.gwt.text.client.DateTimeFormatRenderer;
import com.google.gwt.user.client.ui.Grid;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.RegattaRefresher;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;

public class TrackedRacesManagementPanel extends AbstractRaceManagementPanel {
    private final DateTimeFormatRenderer dateFormatter = new DateTimeFormatRenderer(
            DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT));
    private final DateTimeFormatRenderer timeFormatter = new DateTimeFormatRenderer(
            DateTimeFormat.getFormat(PredefinedFormat.TIME_LONG));
    private final DateTimeFormatRenderer durationFormatter = new DateTimeFormatRenderer(
            DateTimeFormat.getFormat(PredefinedFormat.TIME_MEDIUM), TimeZone.createTimeZone(0));

    private final Grid raceDataGrid;
    
    public TrackedRacesManagementPanel(final SailingServiceAsync sailingService, ErrorReporter errorReporter,
            RegattaRefresher regattaRefresher, StringMessages stringConstants) {
        super(sailingService, errorReporter, regattaRefresher, stringConstants);

        raceDataGrid = new Grid(6,2);
        this.selectedRaceContentPanel.add(raceDataGrid);
        
        raceDataGrid.setText(0, 0, "StartTime:");
        raceDataGrid.setText(1, 0, "EndTime:");
        raceDataGrid.setText(2, 0, "Duration:");
        raceDataGrid.setText(3, 0, "Start of tracking:");
        raceDataGrid.setText(4, 0, "End of tracking:");
        raceDataGrid.setText(5, 0, "Delay to live (ms):");
    }

    @Override
    void refreshSelectedRaceData() {
        if (singleSelectedRace != null && selectedRaceDTO != null) {
            if (selectedRaceDTO.startOfRace != null) {
                raceDataGrid.setText(0, 1, dateFormatter.render(selectedRaceDTO.startOfRace) + " "
                        + timeFormatter.render(selectedRaceDTO.startOfRace));
            } else {
                raceDataGrid.setText(0, 1, "");
            }
            if (selectedRaceDTO.endOfRace != null) {
                raceDataGrid.setText(1, 1, dateFormatter.render(selectedRaceDTO.endOfRace) + " "
                        + timeFormatter.render(selectedRaceDTO.endOfRace));
            } else {
                raceDataGrid.setText(1, 1, "");
            }
            if(selectedRaceDTO.startOfRace != null && selectedRaceDTO.endOfRace != null) {
                Date duration = new Date(selectedRaceDTO.endOfRace.getTime() - selectedRaceDTO.startOfRace.getTime());
                raceDataGrid.setText(2, 1, durationFormatter.render(duration));
            } else {
                raceDataGrid.setText(2, 1, "");
            }
            if(selectedRaceDTO.trackedRace.startOfTracking != null) {
                raceDataGrid.setText(3, 1, dateFormatter.render(selectedRaceDTO.trackedRace.startOfTracking) + " "
                        + timeFormatter.render(selectedRaceDTO.trackedRace.startOfTracking));
            } else {
                raceDataGrid.setText(3, 1, "");
            }
            if(selectedRaceDTO.trackedRace.endOfTracking != null) {
                raceDataGrid.setText(4, 1, dateFormatter.render(selectedRaceDTO.trackedRace.endOfTracking) + " "
                        + timeFormatter.render(selectedRaceDTO.trackedRace.endOfTracking));
            } else {
                raceDataGrid.setText(4, 1, "");
            }
            raceDataGrid.setText(5, 1, "" + selectedRaceDTO.trackedRace.delayToLiveInMs);
        }
    }
}
