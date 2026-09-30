package com.sap.sailing.gwt.home.shared.app;

import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.http.client.UrlBuilder;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceHistoryMapper;
import com.google.gwt.safehtml.shared.SafeUri;
import com.google.gwt.safehtml.shared.UriUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Window;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sse.gwt.client.LinkUtil;
import com.sap.sse.gwt.settings.UrlBuilderUtil;

public class PlaceNavigation<T extends Place> {
    private final PlaceNavigator placeNavigator;

    private final PlaceHistoryMapper mapper;
    private final T destinationPlace;
    private final String baseUrl;
    private final boolean isDestinationOnRemoteServer;

    public PlaceNavigation(T destinationPlace, PlaceNavigator placeNavigator, PlaceHistoryMapper mapper) {
        this.placeNavigator = placeNavigator;
        this.destinationPlace = destinationPlace;
        this.mapper = mapper;
        String locationURL = getLocationURL();
        this.isDestinationOnRemoteServer = !(placeNavigator.isStandaloneServer() || isLocationOnLocalhost(locationURL) || isLocationOnDefaultSapSailingServer(locationURL));
        this.baseUrl = isDestinationOnRemoteServer ? SharedLandscapeConstants.DEFAULT_SAILING_SERVER_URL : locationURL;
    }

    public PlaceNavigation(String baseUrl, T destinationPlace, boolean isDestinationOnRemoteServer,
            PlaceNavigator placeNavigator, PlaceHistoryMapper mapper) {
        this.mapper = mapper;
        this.destinationPlace = destinationPlace;
        this.isDestinationOnRemoteServer = isDestinationOnRemoteServer;
        this.placeNavigator = placeNavigator;
        this.baseUrl = isDestinationOnRemoteServer ? baseUrl : getLocationURL();
    }

    public String getTargetUrl() {
        return buildPlaceUrl();
    }

    public SafeUri getSafeTargetUrl() {
        return UriUtils.fromString(getTargetUrl());
    }

    public String getHistoryUrl() {
        String placeUrl = buildPlaceUrl();
        if (placeUrl.startsWith("#")) {
            placeUrl = placeUrl.substring(1, placeUrl.length());
        }
        return placeUrl;
    }

    public Place getPlace() {
        return destinationPlace;
    }

    private String buildPlaceUrl() {
        if (isRemotePlace()) {
            final UrlBuilder urlBuilder = UrlBuilderUtil.createUrlBuilderFromBaseURLAndPathWithCleanParameters(baseUrl,
                    "/gwt/Home.html");
            urlBuilder.setHash(getPlaceToken());
            return urlBuilder.buildString();
        }
        return getPlaceTokenWithHash();
    }

    public boolean isRemotePlace() {
        return isDestinationOnRemoteServer;
    }
    
    private String getPlaceTokenWithHash() {
        return "#" + getPlaceToken();
    }

    private String getPlaceToken() {
        return mapper.getToken(destinationPlace);
    }

    private boolean isLocationOnDefaultSapSailingServer(String urlToCheck) {
        return urlToCheck.contains(SharedLandscapeConstants.DEFAULT_SAILING_SERVER);
    }

    private boolean isLocationOnLocalhost(String urlToCheck) {
        return urlToCheck.contains("localhost") || urlToCheck.contains("127.0.0.1");
    }

    private String getLocationURL() {
        return Window.Location.getProtocol() + "//" + Window.Location.getHostName() + ":" + Window.Location.getPort();
    }

    public void goToPlace() {
        placeNavigator.goToPlace(this);
    }
    
    public void configureAnchorElement(AnchorElement e) {
        DOM.sinkEvents(e, Event.ONCLICK);
        e.setHref(getTargetUrl());
        Event.setEventListener(e, new EventListener() {
            @Override
            public void onBrowserEvent(Event event) {
                if (LinkUtil.handleLinkClick(event)) {
                    event.preventDefault();
                    event.stopPropagation();
                    goToPlace();
                }
            }
        });
    }
    
    public String getFullQualifiedUrl() {
        return Window.Location.createUrlBuilder().setHash(getTargetUrl()).buildString();
    }
    
}
