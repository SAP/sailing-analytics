package com.sap.sailing.gwt.home.desktop.places.fakeseries;

import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.user.client.ui.Composite;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.home.desktop.places.Consumer;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardUrlSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardPanelLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.utils.StoredSettingsLocationFactory;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.MultiRaceLeaderboardPanel;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.shared.settings.DefaultOnSettingsLoadedCallback;
import com.sap.sse.gwt.shared.GwtHttpRequestUtils;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;
import com.sap.sse.security.ui.settings.PlaceBasedComponentContextWithSettingsStorage;
import com.sap.sse.security.ui.settings.StoredSettingsLocation;

/**
 * An abstract series tabView with some shared functions between the overall leaderboard tab and competitors chart tab
 */
public abstract class SharedLeaderboardEventSeriesTabView<T extends AbstractSeriesTabPlace> extends Composite
        implements SeriesTabView<T>, LeaderboardUpdateListener {

    public SharedLeaderboardEventSeriesTabView() {
    }

    protected void createSharedLeaderboardPanel(String leaderboardName,
            EventSeriesAnalyticsDataManager eventSeriesAnalyticsManager, UserService userService, SubscriptionServiceFactory subscriptionServiceFactory,
            String placeToken, final Consumer<MultiRaceLeaderboardPanel> consumer, Iterable<DetailType> availableDetailTypes) {
        
        // FIXME remove
        boolean autoExpandLastRaceColumn = GwtHttpRequestUtils
                .getBooleanParameter(LeaderboardUrlSettings.PARAM_AUTO_EXPAND_LAST_RACE_COLUMN, false);

        final ComponentContext<MultiRaceLeaderboardSettings> componentContext = createLeaderboardComponentContext(leaderboardName, userService,
                subscriptionServiceFactory, placeToken, availableDetailTypes);
        componentContext.getInitialSettings(new DefaultOnSettingsLoadedCallback<MultiRaceLeaderboardSettings>() {
            @Override
            public void onSuccess(MultiRaceLeaderboardSettings leaderboardSettings) {
                MultiRaceLeaderboardPanel leaderboardPanel = eventSeriesAnalyticsManager.createMultiRaceOverallLeaderboardPanel(null,
                        componentContext, leaderboardSettings,  "leaderboardGroupName",
                        leaderboardName, true, // this information came from place, now hard coded. check with frank
                        autoExpandLastRaceColumn, availableDetailTypes);
                leaderboardPanel.addAttachHandler(new Handler() {
                    @Override
                    public void onAttachOrDetach(AttachEvent event) {
                        if(!event.isAttached()) {
                            componentContext.dispose();
                        }
                    }
                });
                leaderboardPanel.addLeaderboardUpdateListener(SharedLeaderboardEventSeriesTabView.this);
                consumer.consume(leaderboardPanel);
            }
        });
    }

    protected ComponentContext<MultiRaceLeaderboardSettings> createLeaderboardComponentContext(String leaderboardName, UserService userService, SubscriptionServiceFactory subscriptionServiceFactory,
            String placeToken, Iterable<DetailType> availableDetailTypes) {
        PaywallResolver paywallResolver = new PaywallResolverImpl(userService, subscriptionServiceFactory);
        final MultiRaceLeaderboardPanelLifecycle lifecycle = new MultiRaceLeaderboardPanelLifecycle(null, StringMessages.INSTANCE, availableDetailTypes, paywallResolver);
        final StoredSettingsLocation storageDefinition = StoredSettingsLocationFactory.createStoredSettingsLocatorForSeriesOverallLeaderboard(leaderboardName);

        final ComponentContext<MultiRaceLeaderboardSettings> componentContext = new PlaceBasedComponentContextWithSettingsStorage<>(
                lifecycle, userService, storageDefinition, placeToken);
        return componentContext;
    }
}
