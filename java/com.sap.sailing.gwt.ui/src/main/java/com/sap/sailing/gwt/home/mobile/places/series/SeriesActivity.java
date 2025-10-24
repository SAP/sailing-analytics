package com.sap.sailing.gwt.home.mobile.places.series;

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
import com.sap.sailing.gwt.home.mobile.places.series.minileaderboard.SeriesMiniOverallLeaderboardPlace;
import com.sap.sailing.gwt.home.shared.app.ActivityCallback;
import com.sap.sailing.gwt.home.shared.app.NavigationPathDisplay;
import com.sap.sailing.gwt.home.shared.app.NavigationPathDisplay.NavigationItem;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.error.ErrorPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.AbstractSeriesPlace;
import com.sap.sailing.gwt.home.shared.places.fakeseries.SeriesContext;
import com.sap.sailing.gwt.ui.client.FlagImageResolver;
import com.sap.sse.security.ui.client.UserService;

public class SeriesActivity extends AbstractActivity implements SeriesView.Presenter {
    private final MobileApplicationClientFactory clientFactory;
    private final AbstractSeriesPlace place;
    private EventSeriesViewDTO series;
    private final NavigationPathDisplay navigationPathDisplay;
    private final FlagImageResolver flagImageResolver;
    
    public SeriesActivity(AbstractSeriesPlace place, NavigationPathDisplay navigationPathDisplay, MobileApplicationClientFactory clientFactory, FlagImageResolver flagImageResolver) {
        this.navigationPathDisplay = navigationPathDisplay;
        this.clientFactory = clientFactory;
        this.place = place;
        this.flagImageResolver = flagImageResolver;
    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        final SeriesContext ctx = place.getCtx();
        if (ctx.getLeaderboardGroupId() == null && ctx.getSeriesId() == null) {
            ErrorPlace errorPlace = new ErrorPlace("series and leaderboardGroup is null");
            errorPlace.setComingFrom(errorPlace);
            clientFactory.getPlaceController().goTo(errorPlace);
        } else {
            clientFactory.getDispatch().execute(new GetEventSeriesViewAction(ctx),
                    new ActivityCallback<EventSeriesViewDTO>(clientFactory, panel) {
                        @Override
                        public void onSuccess(EventSeriesViewDTO series) {
                            ctx.updateLeaderboardGroupId(series.getLeaderboardGroupUUID());
                            SeriesActivity.this.series = series;
                            initUi(panel, eventBus, series);
                        }
                    });
        }
    }
    
    private void initUi(final AcceptsOneWidget panel, EventBus eventBus, EventSeriesViewDTO series) {
        final SeriesView view = new SeriesViewImpl(this, flagImageResolver);
        view.setQuickFinderValues(series.getDisplayName(), series.getEventsAndRegattasOfSeriesDescending());
        panel.setWidget(view.asWidget());
        
        initNavigationPath();
    }
    
    private void initNavigationPath() {
        navigationPathDisplay.showNavigationPath(new NavigationItem(series.getDisplayName(), clientFactory.getNavigator().getSeriesNavigation(place, null, false)));
    }
    
    @Override
    public SeriesContext getCtx() {
        return place.getCtx();
    }

    public SailingDispatchSystem getDispatch() {
        return clientFactory.getDispatch();
    }
    
    @Override
    public PlaceNavigation<?> getMiniLeaderboardNavigation(UUID eventId, String leaderboardName) {
        return clientFactory.getNavigator()
                .getEventNavigation(new MiniLeaderboardPlace(eventId.toString(), leaderboardName), null, false);
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
    public PlaceNavigation<?> getEventNavigation(String eventId) {
        return clientFactory.getNavigator().getEventNavigation(eventId, null, false);
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
