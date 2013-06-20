package com.sap.sailing.gwt.ui.regattaoverview;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.i18n.client.DateTimeFormat;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.gwt.ui.client.ErrorReporter;
import com.sap.sailing.gwt.ui.client.MarkedAsyncCallback;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.TimeListener;
import com.sap.sailing.gwt.ui.client.Timer;
import com.sap.sailing.gwt.ui.client.Timer.PlayModes;
import com.sap.sailing.gwt.ui.client.Timer.PlayStates;
import com.sap.sailing.gwt.ui.client.shared.components.SettingsDialog;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;

public class RegattaOverviewPanel extends SimplePanel {
    
    private final long serverUpdateRateInMs = 10000;
    private final long uiUpdateRateInMs = 1000;
    
    private final Timer serverUpdateTimer;
    private final Timer uiUpdateTimer;
    
    private final SailingServiceAsync sailingService;
    protected final StringMessages stringMessages;
    protected final ErrorReporter errorReporter;
    
    private final String eventIdAsString;
    private EventDTO eventDTO;
    private List<RaceGroupDTO> raceGroupDTOs;
    private List<EventAndRaceGroupAvailabilityListener> eventRaceGroupListeners;
    
    private RegattaRaceStatesComponent regattaRaceStatesComponent;
    
    private final Label eventNameLabel;
    private final Label venueNameLabel;
    private final Label timeLabel;
    private final Button settingsButton;
    private final Button refreshNowButton;
    private final Button startStopUpdatingButton;
    
    private final DateTimeFormat timeFormatter = DateTimeFormat.getFormat("HH:mm:ss");
    
    private static final String STYLE_NAME_PREFIX = "RegattaOverview-";
    private static final String STYLE_REFRESH_STOP_TIME = STYLE_NAME_PREFIX + "RefreshStopTime";
    private static final String STYLE_FUNCTION_BAR = STYLE_NAME_PREFIX + "functionBar";
    private static final String STYLE_CONTENT_WRAPPER = STYLE_NAME_PREFIX + "contentWrapper";
    private static final String STYLE_TITLE_LABEL = STYLE_NAME_PREFIX + "TitleLabel";
    private static final String STYLE_EVENT_LABEL = STYLE_NAME_PREFIX + "EventLabel";
    private static final String STYLE_VENUE_LABEL = STYLE_NAME_PREFIX + "VenueLabel";
    private static final String STYLE_CLOCK_LABEL = STYLE_NAME_PREFIX + "ClockLabel";
    
    public RegattaOverviewPanel(SailingServiceAsync sailingService, final ErrorReporter errorReporter, final StringMessages stringMessages, 
            String eventIdAsString, RegattaRaceStatesSettings settings) {
        this.sailingService = sailingService;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.eventIdAsString = eventIdAsString;
        
        this.eventDTO = null;
        this.raceGroupDTOs = new ArrayList<RaceGroupDTO>();
        this.eventRaceGroupListeners = new ArrayList<EventAndRaceGroupAvailabilityListener>();
        retrieveEvent();
        retrieveRegattaStructure();
        
        VerticalPanel mainPanel = new VerticalPanel();
        setWidget(mainPanel);
        mainPanel.setWidth("100%");
        mainPanel.addStyleName(STYLE_CONTENT_WRAPPER);
        
        regattaRaceStatesComponent = new RegattaRaceStatesComponent(sailingService, errorReporter, stringMessages, eventIdAsString, settings);
        this.eventRaceGroupListeners.add(regattaRaceStatesComponent);
        regattaRaceStatesComponent.setWidth("100%");
        
        refreshNowButton = new Button(stringMessages.refreshNow());
        refreshNowButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                regattaRaceStatesComponent.loadAndUpdateEventLog();
            }
            
        });
        
        
        settingsButton = new Button(stringMessages.settings());
        settingsButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                new SettingsDialog<RegattaRaceStatesSettings>(regattaRaceStatesComponent, stringMessages).show();
            }            
        });
        
        startStopUpdatingButton = new Button(stringMessages.stopUpdating());
        startStopUpdatingButton.addClickHandler(new ClickHandler() {

            @Override
            public void onClick(ClickEvent event) {
                if (serverUpdateTimer.getPlayState().equals(PlayStates.Playing)) {
                    serverUpdateTimer.pause();
                    startStopUpdatingButton.setText(stringMessages.startUpdating());
                } else if (serverUpdateTimer.getPlayState().equals(PlayStates.Paused)) {
                    serverUpdateTimer.play();
                    startStopUpdatingButton.setText(stringMessages.stopUpdating());
                }
            }
            
        });
        
        this.refreshNowButton.getElement().getStyle().setMarginLeft(20.0, Unit.PX);
        this.startStopUpdatingButton.getElement().getStyle().setMarginLeft(20.0, Unit.PX);
        
        this.serverUpdateTimer = new Timer(PlayModes.Live, serverUpdateRateInMs);
        this.serverUpdateTimer.addTimeListener(new TimeListener() {

            @Override
            public void timeChanged(Date date) {
                regattaRaceStatesComponent.onUpdateServer();
            }
        });
        this.serverUpdateTimer.play();

        this.uiUpdateTimer = new Timer(PlayModes.Live, uiUpdateRateInMs);
        this.uiUpdateTimer.addTimeListener(new TimeListener() {

            @Override
            public void timeChanged(Date date) {
                onUpdateUI(date);
            }
        });
        this.uiUpdateTimer.play();
        
        eventNameLabel = new Label();
        eventNameLabel.addStyleName(STYLE_EVENT_LABEL);
        eventNameLabel.addStyleName(STYLE_TITLE_LABEL);
        
        venueNameLabel = new Label();
        venueNameLabel.addStyleName(STYLE_TITLE_LABEL);
        venueNameLabel.addStyleName(STYLE_VENUE_LABEL);
        
        timeLabel = new Label();
        timeLabel.addStyleName(STYLE_TITLE_LABEL);
        timeLabel.addStyleName(STYLE_CLOCK_LABEL);
        
        FlexTable flexTable = new FlexTable();
        flexTable.setWidth("100%");
        flexTable.addStyleName(STYLE_FUNCTION_BAR);
        
        Grid eventVenueGrid = new Grid(1, 2);
        eventVenueGrid.setCellPadding(5);
        eventVenueGrid.setWidget(0, 0, eventNameLabel);
        eventVenueGrid.setWidget(0, 1, venueNameLabel);
        
        flexTable.setWidget(0, 0, eventVenueGrid);
        flexTable.getCellFormatter().setVerticalAlignment(0, 0, HasVerticalAlignment.ALIGN_MIDDLE);
        
        HorizontalPanel refreshStartStopClockPanel = getRefreshStartStopClockPanel();
        
        flexTable.setWidget(0, 1, refreshStartStopClockPanel);
        
        mainPanel.add(flexTable);
        mainPanel.add(regattaRaceStatesComponent);
        
        // TODO bug 1351: never use System.currentTimeMillis() on the client when trying to compare anything with "server time"
        onUpdateUI(new Date());
    }

    private HorizontalPanel getRefreshStartStopClockPanel() {
        HorizontalPanel refreshStartStopClockPanel = new HorizontalPanel();
        refreshStartStopClockPanel.setSpacing(5);
        refreshStartStopClockPanel.setStyleName(STYLE_REFRESH_STOP_TIME);
        refreshStartStopClockPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        
        refreshStartStopClockPanel.add(settingsButton);
        refreshStartStopClockPanel.add(refreshNowButton);
        refreshStartStopClockPanel.add(startStopUpdatingButton);
        refreshStartStopClockPanel.add(timeLabel);
        return refreshStartStopClockPanel;
    }
    
    public void onUpdateUI(Date time) {
        timeLabel.setText(timeFormatter.format(time));
    }
    
    private void retrieveEvent() {
        sailingService.getEventByIdAsString(eventIdAsString, new MarkedAsyncCallback<EventDTO>() {

            @Override
            protected void handleFailure(Throwable cause) {
                settingsButton.setEnabled(false);
                errorReporter.reportError("Error trying to load event with id " + eventIdAsString + " : "
                        + cause.getMessage());
            }

            @Override
            protected void handleSuccess(EventDTO result) {
                if (result != null) {
                    setEvent(result);
                }
            }
        });
    }
    
    private void fillEventAndVenueName() {
        eventNameLabel.setText(eventDTO.getName());
        venueNameLabel.setText(eventDTO.venue.getName());
    }

    protected void setEvent(EventDTO event) {
        eventDTO = event;
        onEventUpdated();
    }

    private void onEventUpdated() {
        fillEventAndVenueName();
        
        for (EventAndRaceGroupAvailabilityListener listener : this.eventRaceGroupListeners) {
            listener.onEventUpdated(eventDTO);
        }
        
        checkToEnableSettingsButton();
    }
    
    private void retrieveRegattaStructure() {
        sailingService.getRegattaStructureForEvent(eventIdAsString, new MarkedAsyncCallback<List<RaceGroupDTO>>() {

            @Override
            protected void handleFailure(Throwable cause) {
                errorReporter.reportError("Error trying to load regattas for event with id " + eventIdAsString + " : "
                        + cause.getMessage());
            }

            @Override
            protected void handleSuccess(List<RaceGroupDTO> result) {
                if (result != null) {
                    setRaceGroups(result);
                }
            }
            
        });
    }

    protected void setRaceGroups(List<RaceGroupDTO> result) {
        raceGroupDTOs.clear();
        raceGroupDTOs.addAll(result);
        onRaceGroupsUpdated();
    }

    private void onRaceGroupsUpdated() {
        for (EventAndRaceGroupAvailabilityListener listener : this.eventRaceGroupListeners) {
            listener.onRaceGroupsUpdated(raceGroupDTOs);
        }
        checkToEnableSettingsButton();
    }

    private void checkToEnableSettingsButton() {
        if (eventDTO != null && raceGroupDTOs.size() > 0) {
            settingsButton.setEnabled(true);
        }
    }
    
}
