package com.sap.sailing.gwt.autoplay.client.app;

import com.google.gwt.event.shared.SimpleEventBus;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayFailureEvent;
import com.sap.sailing.gwt.autoplay.client.places.config.ConfigPlace;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystemImpl;
import com.sap.sse.gwt.client.mvp.ErrorView;


public class AutoPlayClientFactoryImpl extends AutoPlayClientFactoryBase {

    private AutoPlayContext currentContext;
    private final SailingDispatchSystem dispatch = new SailingDispatchSystemImpl();

    public AutoPlayClientFactoryImpl() {
        this(new SimpleEventBus());
    }
    
    private AutoPlayClientFactoryImpl(EventBus eventBus) {
        this(eventBus, new PlaceController(eventBus));
    }

    private AutoPlayClientFactoryImpl(EventBus eventBus, PlaceController placeController) {
        this(eventBus, placeController, new AutoplayNavigatorImpl(placeController));
    }

    private AutoPlayClientFactoryImpl(EventBus eventBus, PlaceController placeController,
            AutoPlayPlaceNavigator navigator) {
        super(new AutoPlayMainViewImpl(eventBus), eventBus, placeController, navigator);
    }
    

    @Override
    public ErrorView createErrorView(String errorMessage, Throwable errorReason) {
        return null;
    }

    @Override
    public Place getDefaultPlace() {
        return new ConfigPlace();
    }

    @Override
    public void setAutoPlayContext(AutoPlayContext ctx) {
        this.currentContext = ctx;
    }

    @Override
    public AutoPlayContext getAutoPlayCtx() {
        if (currentContext == null) {
            getEventBus().fireEvent(new AutoPlayFailureEvent("No autoplay context found"));
        }
        return currentContext;
    }

    @Override
    public SailingDispatchSystem getDispatch() {
        return dispatch;
    }

}
