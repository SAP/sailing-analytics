package com.sap.sailing.gwt.ui.adminconsole;

import static com.sap.sse.security.shared.HasPermissions.DefaultActions.UPDATE;

import java.util.Date;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.text.client.DateTimeFormatRenderer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Grid;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.gwt.ui.adminconsole.places.AdminConsoleView.Presenter;
import com.sap.sailing.gwt.ui.client.DurationAsHoursMinutesSecondsFormatter;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.gwt.client.dialog.DataEntryDialog.DialogCallback;
import com.sap.sse.security.ui.client.UserService;

public class TrackedRacesManagementPanel extends AbstractRaceManagementPanel {
    private final DateTimeFormatRenderer dateFormatter = new DateTimeFormatRenderer(
            DateTimeFormat.getFormat(PredefinedFormat.DATE_SHORT));
    private final DateTimeFormatRenderer timeFormatter = new DateTimeFormatRenderer(
            DateTimeFormat.getFormat(PredefinedFormat.TIME_LONG));
    private final DurationAsHoursMinutesSecondsFormatter durationFormatter = new DurationAsHoursMinutesSecondsFormatter();

    private final UserService userService;
    private final Grid raceDataGrid;
    private final Button setStartTimeButton;
    
    public TrackedRacesManagementPanel(final Presenter presenter, final StringMessages stringMessages) {
        super(presenter, /* actionButtonsEnabled */ true,
                stringMessages);
        this.userService = presenter.getUserService();
        this.setStartTimeButton = new Button(stringMessages.setStartTimeReceived(), new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                new SetStartTimeReceivedDialog(stringMessages, new DialogCallback<Date>() {
                    @Override
                    public void ok(Date newStartTimeReceived) {
                        sailingServiceWrite.setStartTimeReceivedForRace(selectedRaceDTO.getRaceIdentifier(), newStartTimeReceived, new AsyncCallback<RaceDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                TrackedRacesManagementPanel.this.errorReporter.reportError(stringMessages.errorSettingStartTime(caught.getMessage()));
                            }
                            @Override
                            public void onSuccess(RaceDTO result) {
                                selectedRaceDTO = result;
                                refreshSelectedRaceData();
                                TrackedRacesManagementPanel.this.presenter.getRegattasRefresher().reloadAndCallFillAll();
                            }
                        });
                    }
                    @Override
                    public void cancel() { }
                }).show();
            }
        });
        this.selectedRaceContentPanel.add(setStartTimeButton);

        this.raceDataGrid = new Grid(7, 2);
        this.raceDataGrid.setText(0, 0, stringMessages.startTime() + ":");
        this.raceDataGrid.setText(1, 0, stringMessages.endTime() + ":");
        this.raceDataGrid.setText(2, 0, stringMessages.finishingTime() + ":");
        this.raceDataGrid.setText(3, 0, stringMessages.duration() + ":");
        this.raceDataGrid.setText(4, 0, stringMessages.startOfTracking() + ":");
        this.raceDataGrid.setText(5, 0, stringMessages.endOfTracking() + ":");
        this.raceDataGrid.setText(6, 0, stringMessages.delayForLiveMode() + " (ms):");
        this.selectedRaceContentPanel.add(raceDataGrid);
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
            if (selectedRaceDTO.raceFinishedTime != null) {
                raceDataGrid.setText(2, 1, dateFormatter.render(selectedRaceDTO.raceFinishedTime) + " "
                        + timeFormatter.render(selectedRaceDTO.raceFinishedTime));
            } else {
                raceDataGrid.setText(2, 1, "");
            }
            if (selectedRaceDTO.startOfRace != null && selectedRaceDTO.endOfRace != null) {
                Duration duration = new MillisecondsDurationImpl(selectedRaceDTO.endOfRace.getTime() - selectedRaceDTO.startOfRace.getTime());
                raceDataGrid.setText(3, 1, durationFormatter.getHoursMinutesSeconds(duration));
            } else {
                raceDataGrid.setText(3, 1, "");
            }
            if (selectedRaceDTO.trackedRace != null && selectedRaceDTO.trackedRace.startOfTracking != null) {
                raceDataGrid.setText(4, 1, dateFormatter.render(selectedRaceDTO.trackedRace.startOfTracking) + " "
                        + timeFormatter.render(selectedRaceDTO.trackedRace.startOfTracking));
            } else {
                raceDataGrid.setText(4, 1, "");
            }
            if (selectedRaceDTO.trackedRace != null && selectedRaceDTO.trackedRace.endOfTracking != null) {
                raceDataGrid.setText(5, 1, dateFormatter.render(selectedRaceDTO.trackedRace.endOfTracking) + " "
                        + timeFormatter.render(selectedRaceDTO.trackedRace.endOfTracking));
            } else {
                raceDataGrid.setText(5, 1, "");
            }
            if (selectedRaceDTO.trackedRace != null) {
                raceDataGrid.setText(6, 1, "" + selectedRaceDTO.trackedRace.delayToLiveInMs);
            }
        }
        setStartTimeButton.setVisible(selectedRaceDTO != null && userService.hasPermission(selectedRaceDTO, UPDATE));
    }
}
