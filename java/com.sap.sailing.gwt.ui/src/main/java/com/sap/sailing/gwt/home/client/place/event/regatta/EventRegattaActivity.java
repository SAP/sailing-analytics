package com.sap.sailing.gwt.home.client.place.event.regatta;

import com.google.gwt.event.shared.EventBus;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.gwt.home.client.place.event.regatta.tabs.RegattaOverviewPlace;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.places.event.AbstractEventActivity;
import com.sap.sailing.gwt.home.desktop.places.event.EventClientFactory;
import com.sap.sailing.gwt.home.desktop.places.event.EventView;
import com.sap.sailing.gwt.home.desktop.places.event.EventView.PlaceCallback;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.places.event.EventContext;
import com.sap.sailing.gwt.ui.shared.eventview.EventViewDTO;
import com.sap.sailing.gwt.ui.shared.eventview.EventViewDTO.EventType;
import com.sap.sailing.gwt.ui.shared.eventview.RegattaReferenceDTO;
import com.sap.sailing.gwt.ui.shared.general.EventReferenceDTO;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;

public class EventRegattaActivity extends AbstractEventActivity<AbstractEventRegattaPlace> implements EventRegattaView.Presenter {
    private EventRegattaView currentView = new TabletAndDesktopEventView();
    private final UserAgentDetails userAgent = new UserAgentDetails(Window.Navigator.getUserAgent());
    private final AsyncActionsExecutor asyncActionsExecutor = new AsyncActionsExecutor();
    private final long delayBetweenAutoAdvancesInMilliseconds = 3000l;

    public EventRegattaActivity(AbstractEventRegattaPlace place, EventClientFactory clientFactory,
            DesktopPlacesNavigator homePlacesNavigator) {
        super(place, clientFactory, homePlacesNavigator);
        if (this.ctx.getRegattaAnalyticsManager() == null) {
            ctx.withRegattaAnalyticsManager(new RegattaAnalyticsDataManager(
                    clientFactory.getSailingService(),
                    asyncActionsExecutor,
                    new Timer(PlayModes.Live, PlayStates.Paused, delayBetweenAutoAdvancesInMilliseconds),
                    clientFactory.getErrorReporter(),
                    userAgent));
        }
    }

    @Override
    public void start(final AcceptsOneWidget panel, final EventBus eventBus) {
        currentView.registerPresenter(this);
        panel.setWidget(currentView);
        currentView.navigateTabsTo(currentPlace);
    }
    
    @Override
    public boolean needsSelectionInHeader() {
        EventViewDTO event = ctx.getEventDTO();
        return (event.getType() == EventType.SERIES_EVENT || event.getType() == EventType.MULTI_REGATTA);
    }
    
    @Override
    public void forPlaceSelection(PlaceCallback callback) {
        EventViewDTO event = ctx.getEventDTO();
        if (event.getType() == EventType.SERIES_EVENT) {
            for(EventReferenceDTO seriesEvent : event.getEventsOfSeries()) {
                AbstractEventRegattaPlace place = currentPlace.newInstanceWithContext(new EventContext().withId(seriesEvent.getId().toString()));
                callback.forPlace(place, seriesEvent.getDisplayName(), (event.getId().equals(seriesEvent.getId())));
            }
        } else {
            for(RegattaReferenceDTO regatta : event.getRegattas()) {
                AbstractEventRegattaPlace place = currentPlace.newInstanceWithContext(contextForRegatta(regatta.getId()));
                callback.forPlace(place, regatta.getDisplayName(), (ctx.getRegattaId().equals(regatta.getId())));
            }
        }
    }
    
    @Override
    public boolean showRegattaMetadata() {
        return ctx.getEventDTO().getType() == EventType.MULTI_REGATTA && ctx.getRegatta() != null;
    }
    
    @Override
    public PlaceNavigation<RegattaOverviewPlace> getCurrentRegattaOverviewNavigation() {
        return homePlacesNavigator.getEventNavigation(new RegattaOverviewPlace(ctx), null, false);
    }

    @Override
    public Timer getAutoRefreshTimer() {
        return ctx.getRegattaAnalyticsManager().getTimer();
    }

    @Override
    protected EventView<AbstractEventRegattaPlace, ?> getView() {
        return currentView;
    }
}
