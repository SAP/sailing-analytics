package com.sap.sailing.domain.tracking;

import java.util.ConcurrentModificationException;

import org.junit.jupiter.api.Test;

public class RaceTrackerListenersTest {

    /**
     * Regression test for bug 4039. Removing a listener while the listener was called resulted in a
     * {@link ConcurrentModificationException} if there is another listener in the listeners collection.
     */
    @Test
    public void testThatListenersCanBeRemovedInListener() {
        final RaceTrackerListeners listeners = new RaceTrackerListeners();
        for (int i = 0; i < 2; i++) {
            listeners.addListener(new RaceTracker.Listener() {
                @Override
                public void onTrackerWillStop(boolean preemptive, boolean willBeRemoved) {
                    listeners.removeListener(this);
                }
            });
        }
        listeners.onTrackerWillStop(/* preemptive */ false, /* willBeRemoved */ false);
    }

}
