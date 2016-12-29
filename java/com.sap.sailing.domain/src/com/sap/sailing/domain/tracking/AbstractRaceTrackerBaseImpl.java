package com.sap.sailing.domain.tracking;

import java.io.IOException;
import java.net.MalformedURLException;

/**
 * Base class for all {@link RaceTracker}s that must implement listener notifications
 */
public abstract class AbstractRaceTrackerBaseImpl implements RaceTracker {
    private final RaceTrackerListeners listeners = new RaceTrackerListeners();
    private final RaceTrackingConnectivityParameters connectivityParams;

    public AbstractRaceTrackerBaseImpl(RaceTrackingConnectivityParameters connectivityParams) {
        super();
        this.connectivityParams = connectivityParams;
    }

    /**
     * Ensure stop method does notify all listeners after tracker stopped.
     */
    @Override
    public final void stop(boolean preemptive) throws MalformedURLException, IOException, InterruptedException {
        try {
            listeners.onTrackerWillStop(preemptive);
        } finally {
            onStop(preemptive);
        }
    }

    /**
     * Template stop method for subclasses.
     * 
     * @param preemptive
     * @throws MalformedURLException
     * @throws IOException
     * @throws InterruptedException
     */
    protected void onStop(boolean preemptive) throws MalformedURLException, IOException, InterruptedException {
    }

    @Override
    public boolean add(Listener listener) {
        return listeners.addListener(listener);
    }

    @Override
    public void remove(Listener listener) {
        listeners.removeListener(listener);
    }

    @Override
    public RaceTrackingConnectivityParameters getConnectivityParams() {
        return connectivityParams;
    }
}
