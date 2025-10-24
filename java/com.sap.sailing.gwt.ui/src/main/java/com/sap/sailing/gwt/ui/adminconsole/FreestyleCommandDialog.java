package com.sap.sailing.gwt.ui.adminconsole;

import java.util.ArrayList;
import java.util.Map.Entry;
import java.util.NavigableMap;
import java.util.TreeMap;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Focusable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.controls.busyindicator.SimpleBusyIndicator;
import com.sap.sse.gwt.client.dialog.DataEntryDialog;
import com.sap.sse.gwt.client.dialog.DialogUtils;

public class FreestyleCommandDialog extends DataEntryDialog<Void> {
    private final TextBox commandTextBox;
    private final TextArea logOutputArea;
    private final StringMessages stringMessages;
    private final SailingServiceWriteAsync sailingServiceWrite;
    private final String deviceSerialNumber;
    private final SimpleBusyIndicator sendBusyIndicator;
    private final SimpleBusyIndicator fetchLogBusyIndicator;
    private final NavigableMap<TimePoint, String> logEntries;

    public FreestyleCommandDialog(StringMessages stringMessages, SailingServiceWriteAsync sailingServiceWrite, String deviceSerialNumber) {
        super(stringMessages.sendCommandsTo(deviceSerialNumber),
                stringMessages.sendCommandsToDescription(deviceSerialNumber), StringMessages.INSTANCE.close(),
                /* cancelButtonName */ null, /* validator */ null, /* animationEnabled */ true, /* callback */ null);
        this.stringMessages = stringMessages;
        this.sailingServiceWrite = sailingServiceWrite;
        this.deviceSerialNumber = deviceSerialNumber;
        logEntries = new TreeMap<>();
        commandTextBox = new TextBox();
        commandTextBox.setVisibleLength(80);
        logOutputArea = new TextArea();
        logOutputArea.setCharacterWidth(80);
        logOutputArea.setEnabled(false);
        sendBusyIndicator = new SimpleBusyIndicator();
        fetchLogBusyIndicator = new SimpleBusyIndicator();
        DialogUtils.linkEscapeToButton(getOkButton(), commandTextBox);
    }
    
    @Override
    protected Focusable getInitialFocusWidget() {
        return commandTextBox;
    }

    @Override
    protected Widget getAdditionalWidget() {
        final VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.setSpacing(5);
        final Button sendButton = new Button(stringMessages.send());
        sendButton.addClickHandler(e->sendCommand(commandTextBox.getValue()));
        DialogUtils.linkEnterToButton(sendButton, commandTextBox);
        DialogUtils.linkEscapeToButton(getOkButton(), sendButton);
        final HorizontalPanel horizontalPanel = new HorizontalPanel();
        horizontalPanel.setSpacing(5);
        horizontalPanel.add(commandTextBox);
        horizontalPanel.add(sendButton);
        horizontalPanel.add(sendBusyIndicator);
        verticalPanel.add(horizontalPanel);
        verticalPanel.add(new Label(stringMessages.commandLogOutput()));
        final HorizontalPanel logPanel = new HorizontalPanel();
        logPanel.add(logOutputArea);
        final VerticalPanel logButtonPanel = new VerticalPanel();
        final Button refreshLogButton = new Button(stringMessages.refresh());
        refreshLogButton.addClickHandler(e -> updateLog());
        logPanel.add(logButtonPanel);
        logButtonPanel.add(refreshLogButton);
        final Button enableOverTheAirLogButton = new Button(stringMessages.enableOverTheAirLog());
        enableOverTheAirLogButton.addClickHandler(e->enableOverTheAirLog(true));
        logButtonPanel.add(enableOverTheAirLogButton);
        final Button disableOverTheAirLogButton = new Button(stringMessages.disableOverTheAirLog());
        disableOverTheAirLogButton.addClickHandler(e->enableOverTheAirLog(false));
        logButtonPanel.add(disableOverTheAirLogButton);
        logPanel.add(fetchLogBusyIndicator);
        verticalPanel.add(logPanel);
        return verticalPanel;
    }

    private void enableOverTheAirLog(boolean enabled) {
        sailingServiceWrite.enableIgtimiDeviceOverTheAirLog(deviceSerialNumber, enabled, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                if (result) {
                    Notification.notify(stringMessages.ok(), NotificationType.SUCCESS);
                } else {
                    Notification.notify(stringMessages.noLiveConnectionFoundForIgtimiDevice(deviceSerialNumber), NotificationType.WARNING);
                }
                updateLog();
            }

            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(caught.getMessage(), NotificationType.ERROR);
            }
        });
    }

    private void sendCommand(String command) {
        sendBusyIndicator.setBusy(true);
        sailingServiceWrite.sendIgtimiCommand(deviceSerialNumber, command, new AsyncCallback<Boolean>() {
            @Override
            public void onSuccess(Boolean foundDevice) {
                sendBusyIndicator.setBusy(false);
                if (foundDevice) {
                    Notification.notify(stringMessages.ok(), NotificationType.SUCCESS);
                } else {
                    Notification.notify(stringMessages.noLiveConnectionFoundForIgtimiDevice(deviceSerialNumber), NotificationType.WARNING);
                }
                Scheduler.get().scheduleFixedDelay(new RepeatingCommand() {
                    @Override
                    public boolean execute() {
                        updateLog();
                        return false; // don't repeat
                    }
                }, /* delay in milliseconds */ 1000);
            }

            @Override
            public void onFailure(Throwable caught) {
                sendBusyIndicator.setBusy(false);
                Notification.notify(caught.getMessage(), NotificationType.ERROR);
            }
        });
    }

    private void updateLog() {
        fetchLogBusyIndicator.setBusy(true);
        // compute how long back to fetch the log entries; ask from last known entry up to now, defaulting to one minute if no log entry exists yet
        final Duration duration;
        if (logEntries.isEmpty()) {
            duration = Duration.ONE_MINUTE;
        } else {
            final TimePoint lastLogEntryTime = logEntries.lastKey();
            duration = lastLogEntryTime.until(TimePoint.now());
        }
        sailingServiceWrite.getIgtimiDeviceLogs(deviceSerialNumber, duration, new AsyncCallback<ArrayList<Pair<TimePoint, String>>>() {
            @Override
            public void onSuccess(ArrayList<Pair<TimePoint, String>> result) {
                fetchLogBusyIndicator.setBusy(false);
                result.forEach(logEntry -> logEntries.put(logEntry.getA(), logEntry.getB()));
                final StringBuilder sb = new StringBuilder();
                for (final Entry<TimePoint, String> logLine : logEntries.entrySet()) {
                    sb.append(logLine.getKey())
                      .append(": ")
                      .append(logLine.getValue())
                      .append("\n");
                }
                logOutputArea.setText(sb.toString());
            }

            @Override
            public void onFailure(Throwable caught) {
                fetchLogBusyIndicator.setBusy(false);
                Notification.notify(caught.getMessage(), NotificationType.ERROR);
            }
        });
    }
    
    @Override
    protected Void getResult() {
        return null;
    }
}
