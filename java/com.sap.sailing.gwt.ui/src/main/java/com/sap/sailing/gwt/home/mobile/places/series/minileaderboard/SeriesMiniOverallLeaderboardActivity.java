package com.sap.sailing.gwt.home.mobile.places.series.minileaderboard;

import java.util.UUID;

import com.google.gwt.activity.shared.AbstractActivity;
import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.home.communication.fakeseries.EventSeriesViewDTO;
import com.sap.sailing.gwt.home.communication.fakeseries.GetEventSeriesViewAction;
import com.sap.sailing.gwt.home.desktop.places.fakeseries.overallleaderboardtab.EventSeriesOverallLeaderboardPlace;
import com.sap.sailing.gwt.home.mobile.app.MobileApplicationClientFactory;
import com.sap.sailing.gwt.home.mobile.places.event.minileaderboard.MiniLeaderboardPlace;
import com.sap.sailing.gwt.home.shared.app.ActivityCallback;
import com.sap.sailing.gwt.home.shared.app.NavigationPathDisplay;
import com.sap.sailing.gwt.home.shared.app.NavigationPathDisplay.NavigationItem;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesDefaultPlace;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.security.ui.client.UserService;

public class SeriesMiniOverallLeaderboardActivity extends AbstractActivity implements SeriesMiniOverallLeaderboardView.Presenter {
    private final MobileApplicationClientFactory clientFactory;
    private final SeriesMiniOverallLeaderboardPlace place;
    private EventSeriesViewDTO series;
    private final NavigationPathDisplay navigationPathDisplay;
    private final FlagImageResolver flagImageResolver;

    public SeriesMiniOverallLeaderboardActivity(SeriesMiniOverallLeaderboardPlace place, NavigationPathDisplay navigationPathDisplay, MobileApplicationClientFactory clientFactory, FlagImageResolver flagImageResolver) {
        this.navigationPathDisplay = navigationPathDisplay;
        this.clientFactory = clientFactory;
        this.place = place;
        this.flagImageResolver = flagImageResolver;
    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        final SeriesContext ctx = place.getCtx();
        clientFactory.getDispatch().execute(new GetEventSeriesViewAction(ctx), 
                new ActivityCallback<EventSeriesViewDTO>(clientFactory, panel) {

            @Override
            public void onSuccess(final EventSeriesViewDTO series) {
                ctx.updateLeaderboardGroupId(series.getLeaderboardGroupUUID());
                SeriesMiniOverallLeaderboardActivity.this.series = series;
                initUi(panel, eventBus, series);
            }
        });
    }

    private void initUi(AcceptsOneWidget panel, EventBus eventBus, EventSeriesViewDTO series) {
        final SeriesMiniOverallLeaderboardView view = new SeriesMiniOverallLeaderboardViewImpl(this, flagImageResolver);
        view.setQuickFinderValues(series.getDisplayName(), series.getEventsAndRegattasOfSeriesDescending());
        panel.setWidget(view.asWidget());
        
        initNavigationPath();
    }
    
    private void initNavigationPath() {
        navigationPathDisplay.showNavigationPath(new NavigationItem(series.getDisplayName(), clientFactory.getNavigator().getSeriesNavigation(new SeriesDefaultPlace(getCtx()), null, false)),
                new NavigationItem(StringMessages.INSTANCE.overallStandings(), clientFactory.getNavigator().getSeriesNavigation(place, null, false)));
    }

    @Override
    public SeriesContext getCtx() {
        return place.getCtx();
    }

    @Override
    public SailingDispatchSystem getDispatch() {
        return clientFactory.getDispatch();
    }
    
    @Override
    public PlaceNavigation<?> getOverallLeaderboardNavigation() {
        return clientFactory.getNavigator().getSeriesNavigation(new EventSeriesOverallLeaderboardPlace(getCtx()), null, false);
    }
    
    @Override
    public PlaceNavigation<?> getMiniOverallLeaderboardNavigation() {
        return clientFactory.getNavigator().getSeriesNavigation(new SeriesMiniOverallLeaderboardPlace(getCtx()), null, false);
    }
    
    @Override
    public PlaceNavigation<?> getMiniLeaderboardNavigation(UUID eventId, String leaderboardName) {
        return clientFactory.getNavigator()
                .getEventNavigation(new MiniLeaderboardPlace(eventId.toString(), leaderboardName), null, false);
    }
    
    @Override
    public PlaceNavigation<?> getSeriesNavigation() {
        return clientFactory.getNavigator().getSeriesNavigation(new SeriesDefaultPlace(getCtx()), null, false);
    }
    
    @Override
    public EventSeriesViewDTO getSeriesDTO() {
        return series;
    }

    @Override
    public UserService getUserService() {
        return clientFactory.getUserService();
    }
}
