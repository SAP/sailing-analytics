package com.sap.sailing.gwt.regattaoverview.client;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sailing.gwt.common.client.SharedResources;
import com.sap.sailing.gwt.regattaoverview.client.RegattaRaceStatesComponent.EntryHandler;
import com.sap.sailing.gwt.settings.client.regattaoverview.RegattaOverviewContextDefinition;
import com.sap.sailing.gwt.settings.client.regattaoverview.RegattaRaceStatesSettings;
import com.sap.sailing.gwt.settings.client.utils.StoredSettingsLocationFactory;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.RaceGroupDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.player.TimeListener;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.components.LinkWithSettingsGenerator;
import com.sap.sse.gwt.client.shared.components.SettingsDialog;
import com.sap.sse.gwt.client.shared.perspective.IgnoreLocalSettings;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.shared.settings.DefaultOnSettingsLoadedCallback;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.settings.ComponentContextWithSettingsStorage;
import com.sap.sse.security.ui.settings.StoredSettingsLocation;

public class RegattaOverviewPanel extends SimplePanel {
    
    private final long serverUpdateRateInMs = 10000;
    private final long uiUpdateRateInMs = 450;
    
    private final Timer serverUpdateTimer;
    private final Timer uiUpdateTimer;
    
    private final SailingServiceAsync sailingService;
    protected final StringMessages stringMessages;
    protected final ErrorReporter errorReporter;
    
    private final RegattaOverviewContextDefinition regattaOverviewContextDefinition;
    private EventDTO eventDTO;
    private List<RaceGroupDTO> raceGroupDTOs;
    
    private RegattaRaceStatesComponent regattaRaceStatesComponent;
    
    private final Anchor settingsButton;
    private final Anchor refreshNowButton;
    private final Anchor startStopUpdatingButton;
    private final FlowPanel repeatedInfoLabel = new FlowPanel();
    private VerticalPanel mainPanel;
    private UserService userService;
    private ComponentContext<RegattaRaceStatesSettings> componentContext;
    
    private final RegattaOverviewResources.LocalCss style = RegattaOverviewResources.INSTANCE.css();
    private final SharedResources RES = SharedResources.INSTANCE;
    
    private EntryHandler entryClickedHandler = null;
    
    public void setEntryClickedHandler(EntryHandler handler) {
        entryClickedHandler = handler;
        if (regattaRaceStatesComponent != null) {
            regattaRaceStatesComponent.setEntryClickedHandler(handler);
        }
    }
    
    public RegattaOverviewPanel(
            SailingServiceAsync sailingService,
            UserService userService,
            final ErrorReporter errorReporter, 
            final StringMessages stringMessages, 
            RegattaOverviewContextDefinition regattaOverviewContextDefinition) {
        this.sailingService = sailingService;
        this.stringMessages = stringMessages;
        this.errorReporter = errorReporter;
        this.regattaOverviewContextDefinition = regattaOverviewContextDefinition;
        this.serverUpdateTimer = new Timer(PlayModes.Live, serverUpdateRateInMs);
        this.uiUpdateTimer = new Timer(PlayModes.Live, uiUpdateRateInMs);
        this.eventDTO = null;
        this.raceGroupDTOs = new ArrayList<RaceGroupDTO>();
        this.userService = userService;
        mainPanel = new VerticalPanel();
        setWidget(mainPanel);
        mainPanel.setWidth("100%");
        mainPanel.addStyleName(style.contentWrapper());
        refreshNowButton = new Anchor(stringMessages.refreshNow());
        refreshNowButton.setStyleName(RES.mainCss().button());
        refreshNowButton.addStyleName(style.button());
        refreshNowButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                regattaRaceStatesComponent.loadAndUpdateEventLog();
            }
        });
        settingsButton = new Anchor("&nbsp;", true);
        settingsButton.ensureDebugId("RegattaOverviewSettingsButton");
        settingsButton.setStyleName(style.settingsButton());
        settingsButton.addStyleName(RES.mainCss().button());
        settingsButton.addStyleName(style.button());
        settingsButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                // TODO should we always set ignoreLocalSettings=true when creating links?
                new SettingsDialog<RegattaRaceStatesSettings>(regattaRaceStatesComponent, stringMessages,
                        new LinkWithSettingsGenerator<>(regattaOverviewContextDefinition,
                                IgnoreLocalSettings.getIgnoreLocalSettingsFromCurrentUrl())).show();
            }            
        });
        startStopUpdatingButton = new Anchor("&nbsp;", true);
        startStopUpdatingButton.setStyleName(RES.mainCss().button());
        startStopUpdatingButton.addStyleName(style.refreshButton());
        startStopUpdatingButton.addStyleName(style.button());
        startStopUpdatingButton.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (serverUpdateTimer.getPlayState().equals(PlayStates.Playing)) {
                    serverUpdateTimer.pause();
                    startStopUpdatingButton.removeStyleName(style.refreshButton_live());
                } else if (serverUpdateTimer.getPlayState().equals(PlayStates.Paused)) {
                    serverUpdateTimer.play();
                    startStopUpdatingButton.addStyleName(style.refreshButton_live());
                }
            }
        });
        // FIXME The chained calls from here are independent from each other and should be implemented with Dispatching Framework
        sailingService.getEventById(regattaOverviewContextDefinition.getEvent(), false, new MarkedAsyncCallback<EventDTO>(
                new AsyncCallback<EventDTO>() {
                    @Override
                    public void onFailure(Throwable cause) {
                        settingsButton.setEnabled(false);
                        errorReporter.reportError(stringMessages.errorLoadingEvent(regattaOverviewContextDefinition.getEvent(), cause.getMessage()));
                        continueInitAfterEventRetrieved();
                    }
        
                    @Override
                    public void onSuccess(EventDTO result) {
                        setEvent(result);
                        continueInitAfterEventRetrieved();
                    }
                }));
    }

    private void continueInitAfterEventRetrieved() {
        sailingService.getRegattaStructureForEvent(regattaOverviewContextDefinition.getEvent(), new MarkedAsyncCallback<List<RaceGroupDTO>>(
                new AsyncCallback<List<RaceGroupDTO>>() {
                    @Override
                    public void onFailure(Throwable cause) {
                        errorReporter.reportError(stringMessages.errorLoadingRegattaStructure(regattaOverviewContextDefinition.getEvent(), cause.getMessage()));
                        continueInitAfterRaceGroupsRetrieved();
                    }
        
                    @Override
                    public void onSuccess(List<RaceGroupDTO> result) {
                        raceGroupDTOs.clear();
                        raceGroupDTOs.addAll(result);
                        continueInitAfterRaceGroupsRetrieved();
                    }
                }));
    }

    private void continueInitAfterRaceGroupsRetrieved() {
        final StoredSettingsLocation storageDefinition = StoredSettingsLocationFactory.createStoredSettingsLocatorForRegattaOverview(regattaOverviewContextDefinition);
        final RegattaRaceStatesComponentLifecycle lifecycle = new RegattaRaceStatesComponentLifecycle(eventDTO == null ? null : eventDTO.getVenue().getCourseAreas(), raceGroupDTOs);
        componentContext = new ComponentContextWithSettingsStorage<>(lifecycle, userService, storageDefinition);
        componentContext.getInitialSettings(new DefaultOnSettingsLoadedCallback<RegattaRaceStatesSettings>() {
            @Override
            public void onSuccess(RegattaRaceStatesSettings defaultSettings) {
                continueInitAfterSettingsRetrieved(defaultSettings);
            }
        });
    }

    private void continueInitAfterSettingsRetrieved(RegattaRaceStatesSettings defaultSettings) {
        regattaRaceStatesComponent = new RegattaRaceStatesComponent(null, componentContext, sailingService, errorReporter,
                stringMessages,
                regattaOverviewContextDefinition.getEvent(), eventDTO, raceGroupDTOs, defaultSettings, uiUpdateTimer);
        regattaRaceStatesComponent.setEntryClickedHandler(entryClickedHandler);
        regattaRaceStatesComponent.setWidth("100%");
        this.serverUpdateTimer.addTimeListener(new TimeListener() {
            @Override
            public void timeChanged(Date newTime, Date oldTime) {
                regattaRaceStatesComponent.onUpdateServer();
            }
        });
        this.serverUpdateTimer.play();
        startStopUpdatingButton.addStyleName(style.refreshButton_live());
        this.uiUpdateTimer.addTimeListener(new TimeListener() {
            @Override
            public void timeChanged(Date newTime, Date oldTime) {
                fireEvent(new EventTimeUpdateEvent(newTime));
            }
        });
        this.uiUpdateTimer.play();
        FlowPanel flowPanelLeft = new FlowPanel();
        flowPanelLeft.addStyleName(style.functionBarLeft());
        flowPanelLeft.add(repeatedInfoLabel);
        repeatedInfoLabel.setStyleName(style.repeatedInfoLabel());
        regattaRaceStatesComponent.setRepeatedInfoLabel(repeatedInfoLabel);
        FlowPanel flowPanelRight = new FlowPanel();
        flowPanelRight.addStyleName(style.functionBar());
        // flexTable.add(refreshNowButton);
        flowPanelRight.add(startStopUpdatingButton);
        flowPanelRight.add(settingsButton);
        FlexTable leftRightToolbar = new FlexTable();
        leftRightToolbar.setWidget(0, 1, flowPanelRight);
        leftRightToolbar.setWidget(0, 0, flowPanelLeft);
        leftRightToolbar.getElement().getStyle().setWidth(100, Unit.PCT);
        mainPanel.add(leftRightToolbar);
        mainPanel.add(regattaRaceStatesComponent);
        checkToEnableSettingsButton();
    }

    private void checkToEnableSettingsButton() {
        if (eventDTO != null && raceGroupDTOs.size() > 0) {
            settingsButton.setEnabled(true);
        }
    }
    
    private void setEvent(EventDTO event) {
        eventDTO = event;
        fireEvent(new EventDTOLoadedEvent(eventDTO));
    }
        
}
