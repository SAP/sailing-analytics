package com.sap.sailing.gwt.home.shared.places.event;

import java.util.UUID;
import java.util.logging.Logger;

import com.google.gwt.activity.shared.Activity;
import com.sap.sailing.gwt.home.communication.event.GetEventViewAction;
import com.sap.sailing.gwt.home.communication.eventview.EventViewDTO;
import com.sap.sailing.gwt.home.desktop.places.event.multiregatta.AbstractMultiregattaEventPlace;
import com.sap.sailing.gwt.home.desktop.places.event.multiregatta.mediatab.MultiregattaMediaPlace;
import com.sap.sailing.gwt.home.desktop.places.event.multiregatta.overviewtab.MultiregattaOverviewPlace;
import com.sap.sailing.gwt.home.desktop.places.event.multiregatta.regattastab.MultiregattaRegattasPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.AbstractEventRegattaPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.mediatab.RegattaMediaPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.overviewtab.RegattaOverviewPlace;
import com.sap.sailing.gwt.home.desktop.places.event.regatta.racestab.RegattaRacesPlace;
import com.sap.sailing.gwt.home.shared.app.ActivityProxyCallback;
import com.sap.sailing.gwt.home.shared.app.ClientFactoryWithDispatch;
import com.sap.sailing.gwt.home.shared.app.NavigationPathDisplay;
import com.sap.sailing.gwt.home.shared.app.ProvidesNavigationPath;
import com.sap.sse.gwt.client.mvp.AbstractActivityProxy;
import com.sap.sse.gwt.client.mvp.ClientFactory;

public abstract class AbstractEventActivityProxy<C extends ClientFactory & ClientFactoryWithDispatch> extends AbstractActivityProxy implements ProvidesNavigationPath {
    private static final Logger logger = Logger.getLogger(AbstractEventActivityProxy.class.getName());

    private final C clientFactory;
    private AbstractEventPlace place;
    private NavigationPathDisplay navigationPathDisplay;
    
    public AbstractEventActivityProxy(C clientFactory, AbstractEventPlace place) {
        this.clientFactory = clientFactory;
        this.place = place;
    }

    @Override
    protected final void startAsync() {
        GetEventViewAction action = new GetEventViewAction(UUID.fromString(place.getEventUuidAsString()));
        clientFactory.getDispatch().execute(action, new ActivityProxyCallback<EventViewDTO>(clientFactory, place) {
            @Override
            public void onSuccess(EventViewDTO event) {
                if(place instanceof EventDefaultPlace) {
                    place = getRealPlace(event.isMultiRegatta());
                }
                afterEventLoad(clientFactory, event, verifyAndAdjustPlace(event));
            }
        });
    }
    
    @Override
    public void setNavigationPathDisplay(NavigationPathDisplay navigationPathDisplay) {
        this.navigationPathDisplay = navigationPathDisplay;
    }
    
    protected NavigationPathDisplay getNavigationPathDisplay() {
        return navigationPathDisplay;
    }
    
    /**
     * This method is called after loading the event and verifying the {@link AbstractEventPlace}.
     * Subclasses can start an {@link Activity} based on the given {@link AbstractEventPlace}.
     * 
     * @param clientFactory the {@link ClientFactoryWithDispatch} needed in {@link Activity}
     * @param event event object
     * @param place the verified {@link AbstractEventPlace}
     */
    protected abstract void afterEventLoad(C clientFactory, EventViewDTO event, AbstractEventPlace place);

    private AbstractEventPlace getRealPlace(boolean isMultiRegatta) {
        if (!isMultiRegatta) {
            return new RegattaOverviewPlace(new EventContext(place.getCtx()).withRegattaId(null));
        }
        return new MultiregattaOverviewPlace(place.getCtx());
    }
    
    /**
     * Checks if the place is valid for the given event. If not, the place is automatically being adjusted.
     * 
     * @param event
     */
    protected AbstractEventPlace verifyAndAdjustPlace(EventViewDTO event) {
        EventContext contextWithoutRegatta = new EventContext(place.getCtx()).withRegattaId(null);
        // TODO check if regatta ID is valid
        if (place instanceof AbstractMultiregattaEventPlace && !event.isMultiRegatta()) {
            // Events with a type other than multi regatta only have regatta level pages
            if (place instanceof MultiregattaRegattasPlace) {
                return new RegattaRacesPlace(contextWithoutRegatta);
            }
            if (place instanceof MultiregattaMediaPlace) {
                return new RegattaMediaPlace(contextWithoutRegatta);
            }
            return new RegattaOverviewPlace(contextWithoutRegatta);
        }

        if (place instanceof AbstractEventRegattaPlace) {
            boolean regattaKnown = event.isRegattaIDKnown(place.getCtx().getRegattaId());
            if (!event.isMultiRegatta() && place.getCtx().getRegattaId() != null && !regattaKnown) {
                // Regatta ID unknown but unnecessary ...
                place.getCtx().withRegattaId(null);
            } else if (event.isMultiRegatta() && !regattaKnown) {
                logger.warning("Regatta ID "+place.getCtx().getRegattaId()+" is unknown; displaying event instead");
                return new MultiregattaRegattasPlace(contextWithoutRegatta);
            }
        }

        if (place instanceof RegattaMediaPlace && event.isMultiRegatta()) {
            // The media page for multi regatta events is on event level only but not on regatta level
            return new MultiregattaMediaPlace(contextWithoutRegatta);
        }
        
        return place;
    }

}
