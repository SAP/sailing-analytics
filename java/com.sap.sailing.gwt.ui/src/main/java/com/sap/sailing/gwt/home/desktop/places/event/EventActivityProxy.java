package com.sap.sailing.gwt.home.desktop.places.event;

import com.google.gwt.core.client.GWT;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO.EventType;
import com.sap.sailing.gwt.home.desktop.app.DesktopPlacesNavigator;
import com.sap.sailing.gwt.home.desktop.places.event.multiregatta.AbstractMultiregattaEventPlace;
import com.sap.sailing.gwt.home.desktop.places.event.multiregatta.EventMultiregattaActivity;
import com.sap.sailing.gwt.home.desktop.places.event.multiregatta.overviewtab.MultiregattaOverviewPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.AbstractEventRegattaPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.EventRegattaActivity;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.leaderboardtab.RegattaLeaderboardPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.overviewtab.RegattaOverviewPlace;
import com.sap.sailing.gwt.home.mobile.places.event.latestnews.LatestNewsPlace;
import com.sap.sailing.gwt.home.mobile.places.event.minileaderboard.MiniLeaderboardPlace;
import com.sap.sailing.gwt.home.shared.places.event.AbstractEventActivityProxy;
import com.sap.sailing.gwt.home.shared.places.event.AbstractEventPlace;
import com.sap.sailing.gwt.home.shared.places.event.EventContext;

public class EventActivityProxy extends AbstractEventActivityProxy<EventClientFactory> {

    private DesktopPlacesNavigator homePlacesNavigator;

    public EventActivityProxy(AbstractEventPlace place, EventClientFactory clientFactory,
            DesktopPlacesNavigator homePlacesNavigator) {
        super(clientFactory, place);
        this.homePlacesNavigator = homePlacesNavigator;
    }

    @Override
    protected void afterEventLoad(final EventClientFactory clientFactory, final EventViewDTO event,
            final AbstractEventPlace place) {
        GWT.runAsync(new AbstractRunAsyncCallback() {
            @Override
            public void onSuccess() {
                if (place instanceof AbstractEventRegattaPlace) {
                    super.onSuccess(new EventRegattaActivity((AbstractEventRegattaPlace) place, event, clientFactory,
                            homePlacesNavigator, getNavigationPathDisplay()));
                }
                if (place instanceof AbstractMultiregattaEventPlace) {
                    super.onSuccess(new EventMultiregattaActivity((AbstractMultiregattaEventPlace) place, event,
                            clientFactory, homePlacesNavigator, getNavigationPathDisplay()));
                }
            }
        });
    }
    
    @Override
    protected AbstractEventPlace verifyAndAdjustPlace(EventViewDTO event) {
        AbstractEventPlace adjustedPlace = super.verifyAndAdjustPlace(event);
        EventContext contextWithoutRegatta = new EventContext(adjustedPlace.getCtx()).withRegattaId(null);
        if(adjustedPlace instanceof LatestNewsPlace) {
            if(event.getType() == EventType.MULTI_REGATTA) {
                return new MultiregattaOverviewPlace(contextWithoutRegatta);
            }
            return new RegattaOverviewPlace(contextWithoutRegatta);
        }
        
        if(adjustedPlace instanceof MiniLeaderboardPlace) {
            return new RegattaLeaderboardPlace(adjustedPlace.getCtx());
        }
        return adjustedPlace;
    }

}
