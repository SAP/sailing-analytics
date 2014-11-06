package com.sap.sailing.gwt.home.client.app;

import com.google.gwt.core.client.GWT;
import com.google.gwt.place.shared.Place;
import com.google.gwt.place.shared.PlaceTokenizer;

public class PlaceNavigation<T extends Place> {
    private final T destinationPlace;
    private final PlaceTokenizer<T> tokenizer;
    private final String baseUrl;
    private final boolean isDestinationOnRemoteServer;
    
    public PlaceNavigation(String baseUrl, T destinationPlace, PlaceTokenizer<T> tokenizer) {
        this.destinationPlace = destinationPlace;
        this.tokenizer = tokenizer;
        this.baseUrl = baseUrl;
        this.isDestinationOnRemoteServer = !(isLocationOnLocalhost(baseUrl) || isLocationOnDefaultSapSailingServer(baseUrl));
    }

    public PlaceNavigation(String baseUrl, T destinationPlace, PlaceTokenizer<T> tokenizer, boolean isDestinationOnRemoteServer) {
        this.destinationPlace = destinationPlace;
        this.tokenizer = tokenizer;
        this.baseUrl = baseUrl;
        this.isDestinationOnRemoteServer = isDestinationOnRemoteServer;
    }
    
    public void gotoPlace() {
    }

    public String getTargetUrl() {
        return buildPlaceUrl();
    }

    public Place getPlace() {
        return destinationPlace;
    }

    private String buildPlaceUrl() {
        String url = baseUrl + "/gwt/Home.html";
        if(!GWT.isProdMode() && !isDestinationOnRemoteServer) {
            url += "?gwt.codesvr=127.0.0.1:9997"; 
        }
        url += "#" + destinationPlace.getClass().getSimpleName() + ":" + tokenizer.getToken(destinationPlace);
        return url;
    }
    
    public boolean isRemotePlace() {
        return isDestinationOnRemoteServer;
    }
    
    private boolean isLocationOnDefaultSapSailingServer(String urlToCheck) {
        return urlToCheck.contains(HomePlacesNavigator.DEFAULT_SAPSAILING_SERVER);
    }

    private boolean isLocationOnLocalhost(String urlToCheck) {
        return urlToCheck.contains("localhost") || urlToCheck.contains("127.0.0.1");
    }
}
