package com.sap.sailing.gwt.home.desktop.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceController;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.Window;
import com.sap.sailing.gwt.home.shared.app.ApplicationHistoryMapper;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigation;
import com.sap.sailing.gwt.home.shared.app.PlaceNavigator;

public abstract class AbstractPlaceNavigator implements PlaceNavigator {
    protected final PlaceController placeController;

    private final ApplicationHistoryMapper mapper = GWT.create(ApplicationHistoryMapper.class);

    public final static String DEFAULT_SAPSAILING_SERVER = "www.sapsailing.com";
    public final static String DEFAULT_SAPSAILING_DEV_SERVER = "dev.sapsailing.com";
    public final static String DEFAULT_SAPSAILING_SERVER_URL = "http://" + DEFAULT_SAPSAILING_SERVER;

    protected AbstractPlaceNavigator(PlaceController placeController) {
        this.placeController = placeController;
    }

    public <T extends Place> void goToPlace(PlaceNavigation<T> placeNavigation) {
        if(placeNavigation.isRemotePlace()) {
            String destinationUrl = placeNavigation.getTargetUrl();
            History.newItem(History.getToken(), false);
            Window.Location.assign(destinationUrl);
        } else {
            placeController.goTo(placeNavigation.getPlace());
        }
    }

    protected <T extends Place> PlaceNavigation<T> createLocalPlaceNavigation(T destinationPlace) {
        return new PlaceNavigation<T>(null, destinationPlace, false, this, mapper);
    }


    protected <T extends Place> PlaceNavigation<T> createGlobalPlaceNavigation(T destinationPlace) {
        return new PlaceNavigation<T>(destinationPlace, this, mapper);
    }


    protected <T extends Place> PlaceNavigation<T> createPlaceNavigation(String baseUrl, boolean isOnRemoteServer,
            T destinationPlace) {
        return new PlaceNavigation<T>(baseUrl, destinationPlace, isOnRemoteServer, this, mapper);
    }


    public <T extends Place> void pushPlaceToHistoryStack(T destinationPlace) {
        String placeHistoryToken = mapper.getToken(destinationPlace);
        History.newItem(placeHistoryToken, false);
    }
}
