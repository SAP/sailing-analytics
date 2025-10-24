package com.sap.sailing.gwt.ui.leaderboard;

import java.util.UUID;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RootLayoutPanel;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.common.authentication.FixedSailingAuthentication;
import com.sap.sailing.gwt.common.authentication.SAPSailingHeaderWithAuthentication;
import com.sap.sailing.gwt.common.communication.routing.ProvidesLeaderboardRouting;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardContextDefinition;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardPerspectiveLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardPerspectiveOwnSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.MetaLeaderboardPerspectiveLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiCompetitorLeaderboardChartLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiCompetitorLeaderboardChartSettings;
import com.sap.sailing.gwt.settings.client.utils.StoredSettingsLocationFactory;
import com.sap.sailing.gwt.ui.client.AbstractSailingReadEntryPoint;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.SailingServiceHelper;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.shared.settings.OnSettingsLoadedCallback;
import com.sap.sse.gwt.settings.SettingsToUrlSerializer;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.settings.ComponentContextWithSettingsStorage;
import com.sap.sse.security.ui.settings.StoredSettingsLocation;

public class LeaderboardEntryPoint extends AbstractSailingReadEntryPoint implements ProvidesLeaderboardRouting {
    public static final long DEFAULT_REFRESH_INTERVAL_MILLIS = 3000l;

    private static final Logger logger = Logger.getLogger(LeaderboardEntryPoint.class.getName());

    private StringMessages stringmessages = StringMessages.INSTANCE;
    private UUID eventId;
    private String leaderboardName;
    private AbstractLeaderboardDTO leaderboardDTO;
    private LeaderboardContextDefinition leaderboardContextDefinition;

    @Override
    protected void doOnModuleLoad() {
        super.doOnModuleLoad();

        leaderboardContextDefinition = new SettingsToUrlSerializer()
                .deserializeFromCurrentLocation(new LeaderboardContextDefinition());

        eventId = leaderboardContextDefinition.getEventId();

        leaderboardName = leaderboardContextDefinition.getLeaderboardName();

        if (leaderboardName != null) {
            if (eventId == null) {
                checkLeaderboardNameAndCreateUI(); // use null-initialized event field
            } else {
                // TODO it seems we do not really need the EventDTO. What's the intention of loading it? Should we visualize some information in the header?
                getSailingService().getEventById(eventId, /* withStatisticalData */false,
                        new MarkedAsyncCallback<EventDTO>(new AsyncCallback<EventDTO>() {
                            @Override
                            public void onFailure(Throwable caught) {
                                reportError("Error trying to obtain event " + eventId + ": " + caught.getMessage());
                            }

                            @Override
                            public void onSuccess(EventDTO result) {
                                if(result != null) {
                                    leaderboardDTO = result.getLeaderboardByName(leaderboardName);
                                }
                                checkLeaderboardNameAndCreateUI();
                            }
                        }));
            }
        } else {
            RootPanel.get().add(new Label(getStringMessages().noSuchLeaderboard()));
        }
    }
    
    private void checkLeaderboardNameAndCreateUI() {
        if(leaderboardDTO == null) {
            getSailingService().getLeaderboard(leaderboardName, 
                       new MarkedAsyncCallback<StrippedLeaderboardDTO>(new AsyncCallback<StrippedLeaderboardDTO>() {
                            @Override
                            public void onSuccess(StrippedLeaderboardDTO leaderboardDTO) {
                                if (leaderboardDTO != null) {
                                    LeaderboardEntryPoint.this.leaderboardDTO = leaderboardDTO;
                                    Window.setTitle(leaderboardName);
                                    loadSettingsAndCreateUI();
                                } else {
                                    RootPanel.get().add(new Label(getStringMessages().noSuchLeaderboard()));
                                }
                            }

                            @Override
                            public void onFailure(Throwable t) {
                                reportError("Error trying to obtain list of leaderboard names: " + t.getMessage());
                            }
                        }));
        } else {
            loadSettingsAndCreateUI();
        }
    }

    private void loadSettingsAndCreateUI() {
        long delayBetweenAutoAdvancesInMilliseconds = DEFAULT_REFRESH_INTERVAL_MILLIS;
        final Timer timer = new Timer(PlayModes.Live, PlayStates.Paused, delayBetweenAutoAdvancesInMilliseconds);
        
        // make a single live request as the default but don't continue to play by default

        final StoredSettingsLocation storageDefinition = StoredSettingsLocationFactory
                .createStoredSettingsLocatorForLeaderboard(leaderboardContextDefinition);
        getSailingService().getAvailableDetailTypesForLeaderboard(leaderboardName,
                null, new AsyncCallback<Iterable<DetailType>>() {
                    @Override
                    public void onFailure(Throwable caught) {
                        logger.log(Level.SEVERE, "Could not load detailtypes", caught);
                    }

                    @Override
                    public void onSuccess(Iterable<DetailType> result) {
                        final Function<String, SailingServiceAsync> sailingServiceFactory = leaderboardName -> SailingServiceHelper
                                .createSailingServiceInstance(new ProvidesLeaderboardRouting() {
                                    @Override
                                    public String getLeaderboardName() {
                                        return leaderboardName;
                                    }
                                });
                        if (leaderboardDTO.type.isMetaLeaderboard()) {
                            PaywallResolver paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
                            // overall
                            MetaLeaderboardPerspectiveLifecycle rootComponentLifeCycle = new MetaLeaderboardPerspectiveLifecycle(
                                    stringmessages, leaderboardDTO, result, paywallResolver);
                            ComponentContext<PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings>> context = new ComponentContextWithSettingsStorage<>(
                                    rootComponentLifeCycle, getUserService(), storageDefinition);
                            context.getInitialSettings(
                                    new OnSettingsLoadedCallback<PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings>>() {
                                        @Override
                                        public void onSuccess(
                                                PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings> defaultSettings) {
                                            configureWithSettings(defaultSettings, timer);
                                            final MetaLeaderboardViewer leaderboardViewer = new MetaLeaderboardViewer(
                                                    null, context, rootComponentLifeCycle, defaultSettings,
                                                    sailingServiceFactory, new AsyncActionsExecutor(), timer, null,
                                                    leaderboardName, LeaderboardEntryPoint.this,
                                                    getStringMessages(), getActualChartDetailType(defaultSettings),
                                                    result, LeaderboardEntryPoint.this);
                                            createUi(leaderboardViewer, defaultSettings, timer,
                                                    leaderboardContextDefinition);
                                        }

                                        @Override
                                        public void onError(Throwable caught,
                                                PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings> fallbackDefaultSettings) {
                                            logger.log(Level.WARNING,
                                                    "Could not load initialsettings, useing default settings as fallback",
                                                    caught);
                                            onSuccess(fallbackDefaultSettings);
                                        }
                                    });
                        } else {
                            PaywallResolverImpl paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
                            LeaderboardPerspectiveLifecycle rootComponentLifeCycle = new LeaderboardPerspectiveLifecycle(
                                    StringMessages.INSTANCE, leaderboardDTO, result, paywallResolver);
                            ComponentContext<PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings>> context = new ComponentContextWithSettingsStorage<>(
                                    rootComponentLifeCycle, getUserService(), storageDefinition);
                            context.getInitialSettings(
                                    new OnSettingsLoadedCallback<PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings>>() {
                                        @Override
                                        public void onSuccess(
                                                PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings> defaultSettings) {
                                            getSailingService().getAvailableDetailTypesForLeaderboard(leaderboardName,
                                                    null, new AsyncCallback<Iterable<DetailType>>() {

                                                        @Override
                                                        public void onFailure(Throwable caught) {
                                                            logger.log(Level.SEVERE,
                                                                    "Could not load available detail types",
                                                                    caught);
                                                        }

                                                        @Override
                                                        public void onSuccess(Iterable<DetailType> result) {
                                                            configureWithSettings(defaultSettings, timer);
                                                            final MultiRaceLeaderboardViewer leaderboardViewer = new MultiRaceLeaderboardViewer(
                                                                    null, context, rootComponentLifeCycle,
                                                                    defaultSettings, sailingServiceFactory,
                                                                    new AsyncActionsExecutor(), timer, leaderboardName,
                                                                    LeaderboardEntryPoint.this, getStringMessages(),
                                                                    getActualChartDetailType(defaultSettings), result, LeaderboardEntryPoint.this);
                                                            createUi(leaderboardViewer, defaultSettings, timer,
                                                                    leaderboardContextDefinition);
                                                        }
                                                    });
                                        }

                                        @Override
                                        public void onError(Throwable caught,
                                                PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings> fallbackDefaultSettings) {
                                            logger.log(Level.WARNING,
                                                    "Could not load initialsettings, useing default settings as fallback",
                                                    caught);
                                            onSuccess(fallbackDefaultSettings);
                                        }
                                    });
                        }
                    }
                });
        
    }
    
    private DetailType getActualChartDetailType(
            PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings> settings) {
        MultiCompetitorLeaderboardChartSettings chartSettings = settings
                .findSettingsByComponentId(MultiCompetitorLeaderboardChartLifecycle.ID);
        DetailType chartDetailType = chartSettings == null ? null : chartSettings.getDetailType();
        
        if (chartDetailType == DetailType.REGATTA_NET_POINTS_SUM) {
            return chartDetailType;
        }
        return MultiCompetitorLeaderboardChartSettings.getDefaultDetailType(leaderboardDTO.type.isMetaLeaderboard());
    }
    
    private void createUi(Widget leaderboardViewer,
            PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings> settings, Timer timer,
            LeaderboardContextDefinition leaderboardContextSettings) {
        LeaderboardPerspectiveOwnSettings ownSettings = settings.getPerspectiveOwnSettings();
        
        DockLayoutPanel mainPanel = new DockLayoutPanel(Unit.PX);
        RootLayoutPanel.get().add(mainPanel);
        if (!ownSettings.isEmbedded()) {
            // Hack to shorten the leaderboardName in case of overall leaderboards
            String leaderboardDisplayName = leaderboardContextSettings.getDisplayName();
            if (leaderboardDisplayName == null || leaderboardDisplayName.isEmpty()) {
                leaderboardDisplayName = leaderboardName;
            }
            SAPSailingHeaderWithAuthentication header = new SAPSailingHeaderWithAuthentication(leaderboardDisplayName);
            PaywallResolver paywallResolver = new PaywallResolverImpl(getUserService(), getSubscriptionServiceFactory());
            new FixedSailingAuthentication(getUserService(), paywallResolver, header.getAuthenticationMenuView());
            mainPanel.addNorth(header, 75);
        }

        mainPanel.add(new ScrollPanel(leaderboardViewer));
    }

    protected void configureWithSettings(PerspectiveCompositeSettings<LeaderboardPerspectiveOwnSettings> settings,
            Timer timer) {
        LeaderboardPerspectiveOwnSettings perspectiveOwnSettings = settings.getPerspectiveOwnSettings();
        final String zoomTo = perspectiveOwnSettings.getZoomTo();
        if (zoomTo != null) {
            RootPanel.getBodyElement().setAttribute("style",
                    "zoom: " + zoomTo + ";-moz-transform: scale(" + zoomTo
                            + ");-moz-transform-origin: 0 0;-o-transform: scale(" + zoomTo
                            + ");-o-transform-origin: 0 0;-webkit-transform: scale(" + zoomTo
                            + ");-webkit-transform-origin: 0 0;");
        }
        
        if (perspectiveOwnSettings.isLifePlay()) {
            timer.setPlayMode(PlayModes.Live); // the leaderboard, viewed via the entry point, goes "live" and "playing"
                                               // if an auto-refresh
        }
    }
    
    @Override
    public String getLeaderboardName() {
        return leaderboardName;
    }
}
