package com.sap.sailing.domain.tractracadapter.impl;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.subscription.lib.api.SubscriberInitializationException;

public class RaceTrackingConnectivityParametersImpl implements RaceTrackingConnectivityParameters {
    private final URL paramURL;
    private final URI liveURI;
    private final URI storedURI;
    private final URI courseDesignUpdateURI;
    private final TimePoint startOfTracking;
    private final TimePoint endOfTracking;
    private final RaceLogStore raceLogStore;
    private final DomainFactory domainFactory;
    private final long delayToLiveInMillis;
    private final boolean simulateWithStartTimeNow;
    private final String tracTracUsername;
    private final String tracTracPassword;
    private final String raceStatus;

    public RaceTrackingConnectivityParametersImpl(URL paramURL, URI liveURI, URI storedURI, URI courseDesignUpdateURI, TimePoint startOfTracking,
            TimePoint endOfTracking, long delayToLiveInMillis, boolean simulateWithStartTimeNow, 
            RaceLogStore raceLogStore, DomainFactory domainFactory, String tracTracUsername, String tracTracPassword, String raceStatus) {
        super();
        this.paramURL = paramURL;
        this.liveURI = liveURI;
        this.storedURI = storedURI;
        this.courseDesignUpdateURI = courseDesignUpdateURI;
        this.startOfTracking = startOfTracking;
        this.endOfTracking = endOfTracking;
        this.delayToLiveInMillis = delayToLiveInMillis;
        this.domainFactory = domainFactory;
        this.simulateWithStartTimeNow = simulateWithStartTimeNow;
        this.raceLogStore = raceLogStore;
        this.tracTracUsername = tracTracUsername;
        this.tracTracPassword = tracTracPassword;
        this.raceStatus = raceStatus;
    }

    @Override
    public RaceTracker createRaceTracker(TrackedRegattaRegistry trackedRegattaRegistry, WindStore windStore) throws MalformedURLException,
            FileNotFoundException, URISyntaxException, CreateModelException, SubscriberInitializationException {
        RaceTracker tracker = domainFactory.createRaceTracker(paramURL, liveURI, storedURI, courseDesignUpdateURI, startOfTracking,
                endOfTracking, delayToLiveInMillis, simulateWithStartTimeNow, raceLogStore, windStore, tracTracUsername, tracTracPassword, raceStatus, trackedRegattaRegistry);
        return tracker;
    }

    @Override
    public RaceTracker createRaceTracker(Regatta regatta, TrackedRegattaRegistry trackedRegattaRegistry, WindStore windStore)
            throws Exception {
        RaceTracker tracker = domainFactory.createRaceTracker(regatta, paramURL, liveURI, storedURI, courseDesignUpdateURI, startOfTracking,
                endOfTracking, delayToLiveInMillis, simulateWithStartTimeNow, raceLogStore, windStore, tracTracUsername, tracTracPassword, raceStatus, trackedRegattaRegistry);
        return tracker;
    }

    @Override
    public Util.Triple<URL, URI, URI> getTrackerID() {
        return TracTracRaceTrackerImpl.createID(paramURL, liveURI, storedURI);
    }

    @Override
    public long getDelayToLiveInMillis() {
        return delayToLiveInMillis;
    }

}
