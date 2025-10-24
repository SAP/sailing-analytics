package com.sap.sailing.gwt.home.desktop.places.event.regatta;

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
import com.sap.sse.gwt.client.mutationobserver.ElementSizeMutationObserver;
import com.sap.sse.gwt.client.mutationobserver.ElementSizeMutationObserver.DomMutationCallback;
import com.sap.sse.gwt.client.shared.settings.DefaultOnSettingsLoadedCallback;
import com.sap.sse.gwt.client.shared.settings.PipelineLevel;
import com.sap.sse.gwt.shared.GwtHttpRequestUtils;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;
import com.sap.sse.security.ui.settings.PlaceBasedComponentContextWithSettingsStorage;
import com.sap.sse.security.ui.settings.StoredSettingsLocation;

/**
 * An abstract regatta tabView with some shared functions between the leaderboard tab and competitors chart tab 
 */
public abstract class SharedLeaderboardRegattaTabView<T extends AbstractEventRegattaPlace> extends Composite implements RegattaTabView<T>,
        LeaderboardUpdateListener {
    private boolean initialLeaderboardSizeCalculated = false;

    public SharedLeaderboardRegattaTabView() {
    }

    public void createSharedLeaderboardPanel(String leaderboardName,
            RegattaAnalyticsDataManager regattaAnalyticsManager, UserService userService, SubscriptionServiceFactory subscriptionServiceFactory, String placeToken,
            final Consumer<MultiRaceLeaderboardPanel> consumer, Iterable<DetailType> availableDetailTypes) {

        // FIXME remove
        boolean autoExpandLastRaceColumn = GwtHttpRequestUtils
                .getBooleanParameter(LeaderboardUrlSettings.PARAM_AUTO_EXPAND_LAST_RACE_COLUMN, false);

        final PlaceBasedComponentContextWithSettingsStorage<MultiRaceLeaderboardSettings> componentContext = createLeaderboardComponentContext(
                leaderboardName, userService, subscriptionServiceFactory, placeToken, availableDetailTypes);
        componentContext.getInitialSettings(new DefaultOnSettingsLoadedCallback<MultiRaceLeaderboardSettings>() {
            @Override
            public void onSuccess(MultiRaceLeaderboardSettings leaderboardSettings) {
                final MultiRaceLeaderboardPanel leaderboardPanel = regattaAnalyticsManager
                        .createMultiRaceLeaderboardPanel(null, componentContext, //
                                leaderboardSettings, //
                                "leaderboardGroupName", // TODO: keep using magic string? ask frank!
                                leaderboardName, //
                                true, autoExpandLastRaceColumn, availableDetailTypes);
                leaderboardPanel.addAttachHandler(new Handler() {

                    @Override
                    public void onAttachOrDetach(AttachEvent event) {
                        if (!event.isAttached()) {
                            componentContext.dispose();
                        }
                    }

                });

                if (ElementSizeMutationObserver.isSupported()) {
                    ElementSizeMutationObserver observer = new ElementSizeMutationObserver(new DomMutationCallback() {
                        @Override
                        public void onSizeChanged(int newWidth, int newHeight) {
                            if (!initialLeaderboardSizeCalculated && leaderboardPanel.getLeaderboard() != null) {
                                initialLeaderboardSizeCalculated = true;
                                if (newWidth > 0 && newHeight > 0 && newWidth > 1500) {
                                    final int numberOfLastRacesToShow = (1500 - 600) / 50;
                                    SecurityChildSettingsContext context = 
                                            new SecurityChildSettingsContext(leaderboardPanel.getLeaderboard(), 
                                            leaderboardPanel.getPaywallResolver());
                                    MultiRaceLeaderboardSettings newSettings = MultiRaceLeaderboardSettings
                                            .createDefaultSettingsWithLastNRaceColumnSelection(numberOfLastRacesToShow, context);
                                    componentContext.addAdditionalSettingsLayerForComponent(leaderboardPanel,
                                            PipelineLevel.SYSTEM_DEFAULTS, newSettings,
                                            leaderboardPanel::updateSettings);
                                }
                            }
                        }
                    });
                    observer.observe(leaderboardPanel.getLeaderboardTable().getElement());
                }
                consumer.consume(leaderboardPanel);
            }
        });
    }
    
    protected PlaceBasedComponentContextWithSettingsStorage<MultiRaceLeaderboardSettings> createLeaderboardComponentContext(
            String leaderboardName, UserService userService, SubscriptionServiceFactory subscriptionServiceFactory, String placeToken,
            Iterable<DetailType> availableDetailTypes) {
        PaywallResolver paywallResolver = new PaywallResolverImpl(userService, subscriptionServiceFactory);
        final MultiRaceLeaderboardPanelLifecycle lifecycle = new MultiRaceLeaderboardPanelLifecycle(null, StringMessages.INSTANCE, availableDetailTypes, paywallResolver);
        final StoredSettingsLocation storageDefinition = StoredSettingsLocationFactory.createStoredSettingsLocatorForEventRegattaLeaderboard(leaderboardName);

        final PlaceBasedComponentContextWithSettingsStorage<MultiRaceLeaderboardSettings> componentContext = new PlaceBasedComponentContextWithSettingsStorage<>(
                lifecycle, userService, storageDefinition, placeToken);
        return componentContext;
    }
}
