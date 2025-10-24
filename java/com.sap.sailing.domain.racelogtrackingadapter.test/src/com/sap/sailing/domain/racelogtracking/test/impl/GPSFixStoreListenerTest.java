package com.sap.sailing.domain.racelogtracking.test.impl;

import java.util.ConcurrentModificationException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.MongoSensorFixStoreImpl;
import com.sap.sailing.domain.racelog.tracking.FixReceivedListener;
import com.sap.sailing.domain.racelogtracking.test.AbstractGPSFixStoreTest;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Triple;

@Timeout(value = 3, unit = TimeUnit.SECONDS)
public class GPSFixStoreListenerTest extends AbstractGPSFixStoreTest {
    /**
     * {@link MongoSensorFixStoreImpl} had broken synchronization of the listeners collection (add/removeListener
     * methods were synchronized but notifyListeners was not synchronized).
     * <br>
     * Changed the implementation in the context of bug 4162. Because the listeners aren't notified anymore while
     * holding the lock, the formerly expected {@link TimoutRuntimeException} does not occur anymore. But with the given setup,
     * we can ensure, that adding a listener does not cause a {@link ConcurrentModificationException}.
     */
    @Test
    public void addingAListenerWhileNotifyingListenersDoesNotCauseConcurrentModificationExceptionOrDeadlock() throws InterruptedException {
        CyclicBarrier barrier = new CyclicBarrier(2);
        // We need 3 listener instances to guarantee that the iterator isn't finished
        // when adding another listener in the thread below.
        store.addListener(new ListenerAwaitingBarrier(barrier), device);
        store.addListener(new ListenerAwaitingBarrier(barrier), device);
        store.addListener(new ListenerAwaitingBarrier(barrier), device);

        Thread thread = new Thread() {
            public void run() {
                try {
                    barrier.await(100, TimeUnit.MILLISECONDS);
                    // During iteration in the main thread this causes a modification that makes the iterator throw a
                    // ConcurrentModificationException on next()
                    store.addListener((DeviceIdentifier device, GPSFixMoving fix, boolean returnManeuverChanges, boolean returnLiveDelay) -> {
                        return null;
                    }, device);
                    barrier.await(100, TimeUnit.MILLISECONDS);
                    barrier.await(100, TimeUnit.MILLISECONDS);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            };
        };
        thread.start();
        try {
            store.storeFix(device, createFix(100, 10, 20, 30, 40));
        } finally {
            // This ensures that the thread is terminated when the test finishes
            // JUnit may behave crazy if there are additional tests running after the test finished
            thread.join(500);
        }
    }

    private static class ListenerAwaitingBarrier implements FixReceivedListener<GPSFixMoving> {

        private final CyclicBarrier barrier;

        public ListenerAwaitingBarrier(CyclicBarrier barrier) {
            this.barrier = barrier;
        }

        @Override
        public Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> fixReceived(DeviceIdentifier device, GPSFixMoving fix, boolean returnManeuverChanges, boolean returnLiveDelay) {
            try {
                barrier.await(100, TimeUnit.MILLISECONDS);
            } catch (TimeoutException e) {
                throw new TimoutRuntimeException(e);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return null;
        }
    }
    
    private static class TimoutRuntimeException extends RuntimeException {
        private static final long serialVersionUID = 6349762933223278846L;

        public TimoutRuntimeException(TimeoutException e) {
            super(e);
        }
    }
}
