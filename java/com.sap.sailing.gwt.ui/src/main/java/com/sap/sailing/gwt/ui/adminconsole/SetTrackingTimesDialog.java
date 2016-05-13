package com.sap.sailing.gwt.ui.adminconsole;

import java.util.Date;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.i18n.client.DateTimeFormat.PredefinedFormat;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.abstractlog.TimePointSpecificationFoundInLog;
import com.sap.sailing.gwt.ui.client.DataEntryDialogWithBootstrap;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.BetterDateTimeBox;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;

public class SetTrackingTimesDialog extends DataEntryDialogWithBootstrap<RaceLogSetTrackingTimesDTO> {

    private static AdminConsoleResources resources = GWT.create(AdminConsoleResources.class);

    private final SailingServiceAsync service;
    private final ErrorReporter errorReporter;
    private final String leaderboardName;
    private final String raceColumnName;
    private final String fleetName;
    private final StringMessages stringMessages;

    private final DateTimeFormat dateTimeFormat = DateTimeFormat.getFormat(PredefinedFormat.DATE_TIME_LONG);
    private Label currentStartLabel;
    private TimePoint currentStart;
    private Label currentEndLabel;
    private TimePoint currentEnd;

    private BetterDateTimeBox startTimeBox;
    private BetterDateTimeBox endTimeBox;
    private TextBox authorNameBox;
    private com.sap.sse.gwt.client.controls.IntegerBox authorPriorityBox;

    protected SetTrackingTimesDialog(SailingServiceAsync service, ErrorReporter errorReporter, String leaderboardName,
            String raceColumnName, String fleetName, StringMessages stringMessages,
            DataEntryDialog.DialogCallback<RaceLogSetTrackingTimesDTO> callback) {
        super(stringMessages.setTrackingTimes(), stringMessages.setTrackingTimesDescription(), stringMessages
                .setTrackingTimes(), stringMessages.cancel(), new TrackingTimesValidator(stringMessages), callback);
        this.service = service;
        this.errorReporter = errorReporter;
        this.leaderboardName = leaderboardName;
        this.raceColumnName = raceColumnName;
        this.fleetName = fleetName;
        this.stringMessages = stringMessages;
    }

    @Override
    protected Widget getAdditionalWidget() {
        VerticalPanel content = new VerticalPanel();
        content.add(createCurrentPanel());
        content.add(createInputPanel());
        refreshTimes();
        return content;
    }

    private void refreshTimes() {
        service.getTrackingTimes(leaderboardName, raceColumnName, fleetName,
                new AsyncCallback<Util.Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error retrieving tracking times: " + caught.getMessage());
                    }

                    @Override
                    public void onSuccess(Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> result) {
                        currentStart = result == null || result.getA() == null ? null : result.getA().getTimePoint();
                        currentEnd = result == null || result.getB() == null ? null : result.getB().getTimePoint();
                        updateDateTimeLabelAndTimeBoxFromDate(currentStart, currentStartLabel, startTimeBox);
                        updateDateTimeLabelAndTimeBoxFromDate(currentEnd, currentEndLabel, endTimeBox);
                    }
                });
    }
    
    private void updateDateTimeLabelAndTimeBoxFromDate(final TimePoint timePoint, final Label label, final BetterDateTimeBox dateTimeBox) {
        if (timePoint == null) {
            label.setText(stringMessages.notAvailable());
            dateTimeBox.setValue(null);
        } else {
            label.setText(dateTimeFormat.format(timePoint.asDate()));
            dateTimeBox.setValue(timePoint.asDate());
        }
    }

    private Widget createInputPanel() {
        Grid content = new Grid(4, 3);
        
        Button startNow = new Button(stringMessages.now());
        startNow.addClickHandler(new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                startTimeBox.setValue(new Date());
            }
        });
        Button endNow = new Button(stringMessages.now());
        endNow.addClickHandler(new ClickHandler() {
            
            @Override
            public void onClick(ClickEvent event) {
                endTimeBox.setValue(new Date());
            }
        });

        startTimeBox = createDateTimeBox(null);
        startTimeBox.setFormat("dd/mm/yyyy hh:ii:ss");
        content.setWidget(0, 0, createLabel(stringMessages.startOfTracking()));
        content.setWidget(0, 1, startTimeBox);
        content.setWidget(0, 2, startNow);
        
        endTimeBox = createDateTimeBox(null);
        endTimeBox.setFormat("dd/mm/yyyy hh:ii:ss");
        content.setWidget(1, 0, createLabel(stringMessages.endOfTracking()));
        content.setWidget(1, 1, endTimeBox);
        content.setWidget(1, 2, endNow);
        
        authorNameBox = createTextBox("Shore");
        content.setWidget(2, 0, createLabel(stringMessages.authorName()));
        content.setWidget(2, 1, authorNameBox);
        authorPriorityBox = createIntegerBox(4, 2);
        content.setWidget(3, 0, createLabel(stringMessages.authorPriority()));
        content.setWidget(3, 1, authorPriorityBox);

        return content;
    }

    private Widget createCurrentPanel() {
        CaptionPanel current = new CaptionPanel(stringMessages.liveData());
        HorizontalPanel currentPanel = new HorizontalPanel();
        currentPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        Grid grid = new Grid(2, 2);
        currentStartLabel = new Label("");
        grid.setWidget(0, 0, createLabel(stringMessages.startOfTracking()));
        grid.setWidget(0, 1, currentStartLabel);

        currentEndLabel = new Label("");
        grid.setWidget(1, 0, createLabel(stringMessages.endOfTracking()));
        grid.setWidget(1, 1, currentEndLabel);
        currentPanel.add(grid);

        PushButton refreshButton = new PushButton(new Image(resources.reloadIcon()));
        refreshButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                refreshTimes();
            }
        });
        currentPanel.add(refreshButton);
        current.add(currentPanel);
        return current;
    }

    @Override
    protected RaceLogSetTrackingTimesDTO getResult() {
        return generateRaceLogSetTrackingTimesDTOWith(startTimeBox.getValue(), endTimeBox.getValue());
    }
    
    private RaceLogSetTrackingTimesDTO generateRaceLogSetTrackingTimesDTOWith(Date startTime, Date endTime) {
        RaceLogSetTrackingTimesDTO dto = new RaceLogSetTrackingTimesDTO();
        dto.leaderboardName = leaderboardName;
        dto.raceColumnName = raceColumnName;
        dto.fleetName = fleetName;
        dto.authorName = authorNameBox.getValue();
        dto.authorPriority = authorPriorityBox.getValue();
        dto.logicalTimePoint = MillisecondsTimePoint.now();
        dto.newStartOfTracking = startTime == null ? null : new MillisecondsTimePoint(startTime);
        dto.newEndOfTracking = endTime == null ? null : new MillisecondsTimePoint(endTime);
        dto.currentStartOfTracking = currentStart;
        dto.currentEndOfTracking = currentEnd;
        return dto;
    }

    private static class TrackingTimesValidator implements Validator<RaceLogSetTrackingTimesDTO> {
        private final StringMessages stringMessages;

        public TrackingTimesValidator(StringMessages stringMessages) {
            this.stringMessages = stringMessages;
        }

        @Override
        public String getErrorMessage(RaceLogSetTrackingTimesDTO dto) {
            final String result;
            if (dto.authorName == null || dto.authorPriority == null) {
                result = stringMessages.pleaseEnterAValue();
            } else {
                result = null;
            }
            return result;
        }
    }
}
