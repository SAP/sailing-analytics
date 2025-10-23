package com.sap.sailing.domain.tractracadapter.impl;

import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;

public class TracTracConfigurationImpl implements TracTracConfiguration {
    private static final long serialVersionUID = 1L;
    private final String name;
    private final String jsonURL;
    private final String liveDataURI;
    private final String storedDataURI;
    private final String updateURI;
    private final String tracTracApiToken;
    private final String creatorName;

    public TracTracConfigurationImpl(String creatorName, String name, String jsonURL, String liveDataURI,
            String storedDataURI, String courseDesignUpdateURI,
            String tracTracApiToken) {
        this.creatorName = creatorName;
        this.name = name;
        this.jsonURL = jsonURL;
        this.liveDataURI = liveDataURI;
        this.storedDataURI = storedDataURI;
        this.updateURI = courseDesignUpdateURI;
        this.tracTracApiToken = tracTracApiToken;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getJSONURL() {
        return jsonURL;
    }

    @Override
    public String getLiveDataURI() {
        return liveDataURI;
    }

    @Override
    public String getStoredDataURI() {
        return storedDataURI;
    }

    @Override
    public String toString() {
        try {
            return getName()+": "+getJSONURL()+ " ("+getLiveDataURI()+", "+getStoredDataURI()+")";
        } catch (Exception e) {
            return "<Exception during TracTracConfiguration.toString(): "+e.getMessage()+">";
        }
    }

    @Override
    public String getUpdateURI() {
        return updateURI;
    }

    @Override
    public String getTracTracApiToken() {
        return tracTracApiToken;
    }

    public String getCreatorName() {
        return creatorName;
    }

}
