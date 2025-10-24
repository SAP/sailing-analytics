package com.sap.sailing.gwt.home.desktop.places.fakeseries.leaderboardstab;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.logical.shared.AttachEvent.Handler;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.common.client.controls.tabbar.TabView;
import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO.EventSeriesState;
import com.sap.sailing.gwt.home.desktop.partials.old.multileaderboard.OldMultiLeaderboard;
import com.sap.sailing.gwt.home.desktop.partials.old.multileaderboard.OldMultiLeaderboardDelegateFullscreenViewer;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.EventSeriesAnalyticsDataManager;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.SeriesTabView;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.SeriesView;
import com.sap.sailing.gwt.settings.client.leaderboard.LeaderboardUrlSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.MultipleMultiLeaderboardPanelLifecycle;
import com.sap.sailing.gwt.settings.client.utils.StoredSettingsLocationFactory;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.MultiLeaderboardProxyPanel;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.shared.settings.DefaultOnSettingsLoadedCallback;
import com.sap.sse.gwt.shared.GwtHttpRequestUtils;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;
import com.sap.sse.security.ui.settings.PlaceBasedComponentContextWithSettingsStorage;
import com.sap.sse.security.ui.settings.StoredSettingsLocation;

public class EventSeriesLeaderboardsTabView extends Composite implements SeriesTabView<EventSeriesLeaderboardsPlace>,
        LeaderboardUpdateListener {

    interface MyBinder extends UiBinder<HTMLPanel, EventSeriesLeaderboardsTabView> {
    }

    private static MyBinder ourUiBinder = GWT.create(MyBinder.class);

    private SeriesView.Presenter currentPresenter;

    @UiField(provided = true)
    protected OldMultiLeaderboard leaderboard;

    public EventSeriesLeaderboardsTabView() {
        leaderboard = new OldMultiLeaderboard(new OldMultiLeaderboardDelegateFullscreenViewer());
    }

    @Override
    public Class<EventSeriesLeaderboardsPlace> getPlaceClassForActivation() {
        return EventSeriesLeaderboardsPlace.class;
    }

    @Override
    public void setPresenter(SeriesView.Presenter currentPresenter) {
        this.currentPresenter = currentPresenter;
    }
    
    @Override
    public SeriesView.Presenter getPresenter() {
        return currentPresenter;
    }

    @Override
    public TabView.State getState() {
        return currentPresenter.getSeriesDTO().isHasAnalytics() ? TabView.State.VISIBLE : TabView.State.INVISIBLE;
    }

    @Override
    public void start(final EventSeriesLeaderboardsPlace myPlace, final AcceptsOneWidget contentArea) {
        contentArea.setWidget(currentPresenter.getErrorAndBusyClientFactory().createBusyView());
        String leaderboardName = currentPresenter.getSeriesDTO().getLeaderboardId();
        if (leaderboardName != null && !leaderboardName.isEmpty()) {
            final EventSeriesAnalyticsDataManager regattaAnalyticsManager = currentPresenter.getCtx()
                    .getAnalyticsManager();
            regattaAnalyticsManager.getAvailableDetailTypesForLeaderboard(leaderboardName,
                    null, new AsyncCallback<Iterable<DetailType>>() {
                        @Override
                        public void onSuccess(Iterable<DetailType> result) {
                            final boolean autoExpandLastRaceColumn = GwtHttpRequestUtils.getBooleanParameter(
                                    LeaderboardUrlSettings.PARAM_AUTO_EXPAND_LAST_RACE_COLUMN, false);
                            currentPresenter.getSailingService().getLeaderboard(leaderboardName, new AsyncCallback<StrippedLeaderboardDTO>() {
                                
                                @Override
                                public void onSuccess(StrippedLeaderboardDTO leaderboardDTO) {
                                    final ComponentContext<MultiRaceLeaderboardSettings> componentContext = createLeaderboardComponentContext(
                                            leaderboardName, currentPresenter.getUserService(), currentPresenter.getSubscriptionServiceFactory(), /* FIXME placeToken */ null, result, leaderboardDTO);
                                    componentContext.getInitialSettings(
                                            new DefaultOnSettingsLoadedCallback<MultiRaceLeaderboardSettings>() {
                                                @Override
                                                public void onSuccess(MultiRaceLeaderboardSettings settings) {
                                                    MultiLeaderboardProxyPanel leaderboardPanel = regattaAnalyticsManager
                                                            .createMultiLeaderboardPanel(null, componentContext, settings, null, // TODO: preselectedLeaderboardName
                                                                    "leaderboardGroupName", leaderboardName, true, // TODO @FM this information came from place, now hard coded. check with frank
                                                                    autoExpandLastRaceColumn, result);
                                                    leaderboardPanel.addAttachHandler(new Handler() {
                                                        @Override
                                                        public void onAttachOrDetach(AttachEvent event) {
                                                            if (!event.isAttached()) {
                                                                componentContext.dispose();
                                                            }
                                                        }
                                                    });
                                                    initWidget(
                                                            ourUiBinder.createAndBindUi(EventSeriesLeaderboardsTabView.this));
                                                    leaderboard.setMultiLeaderboard(leaderboardPanel,
                                                            currentPresenter.getAutoRefreshTimer());
                                                    leaderboardPanel
                                                            .addLeaderboardUpdateListener(EventSeriesLeaderboardsTabView.this);
                                                    if (currentPresenter.getSeriesDTO()
                                                            .getState() != EventSeriesState.RUNNING) {
                                                        // TODO: this.leaderboard.hideRefresh();
                                                    } else {
                                                        // Turn on auto refresh button at parent leaderboard
                                                        leaderboard.turnOnAutoPlay();
                                                    }
                                                    regattaAnalyticsManager.hideCompetitorChart();
                                                    leaderboardPanel.setVisible(true);
                                                    contentArea.setWidget(EventSeriesLeaderboardsTabView.this);
                                                }
                                            });
                                }
                                
                                @Override
                                public void onFailure(Throwable caught) {
                                    // TODO Auto-generated method stub
                                    
                                }
                            });
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            showMessageLabelAndGoToHome("Could not load detaillist", contentArea);
                        }
                    });
        } else {
            showMessageLabelAndGoToHome("No leaderboard specified, cannot proceed to leaderboardpage", contentArea);
        }
    }

    private void showMessageLabelAndGoToHome(final String message, final AcceptsOneWidget contentArea) {
        contentArea.setWidget(new Label(message));
        new Timer() {
            @Override
            public void run() {
                currentPresenter.getHomeNavigation().goToPlace();
            }
        }.schedule(3000);
    }

    private ComponentContext<MultiRaceLeaderboardSettings> createLeaderboardComponentContext(String leaderboardName, UserService userService,
            SubscriptionServiceFactory subscriptionServiceFactory, String placeToken, Iterable<DetailType> availableDetailTypes, AbstractLeaderboardDTO leaderboardDTO) {
        PaywallResolver paywallResolver = new PaywallResolverImpl(userService, subscriptionServiceFactory);
        final MultipleMultiLeaderboardPanelLifecycle lifecycle = new MultipleMultiLeaderboardPanelLifecycle(StringMessages.INSTANCE, availableDetailTypes, paywallResolver, leaderboardDTO);
        final StoredSettingsLocation storageDefinition = StoredSettingsLocationFactory.createStoredSettingsLocatorForSeriesRegattaLeaderboards(leaderboardName);

        final ComponentContext<MultiRaceLeaderboardSettings> componentContext = new PlaceBasedComponentContextWithSettingsStorage<>(
                lifecycle, userService, storageDefinition, placeToken);
        return componentContext;
    }

    @Override
    public void updatedLeaderboard(LeaderboardDTO leaderboard) {
        this.leaderboard.updatedMultiLeaderboard(leaderboard, true);
    }

    @Override
    public void currentRaceSelected(RaceIdentifier raceIdentifier, RaceColumnDTO raceColumn) {
    }

    @Override
    public void stop() {
    }

    @Override
    public EventSeriesLeaderboardsPlace placeToFire() {
        return new EventSeriesLeaderboardsPlace(currentPresenter.getCtx());
    }
}