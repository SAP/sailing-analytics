package com.sap.sailing.domain.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.maptrack.client.io.TypeController;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.tractrac.clientmodule.Race;
import com.tractrac.clientmodule.RaceCompetitor;
import com.tractrac.clientmodule.data.ICallbackData;
import com.tractrac.clientmodule.data.MarkPassingsData;

public class ReceiveMarkPassingDataTest extends AbstractTracTracLiveTest {
    final private Object semaphor = new Object();
    final private MarkPassingsData[] firstData = new MarkPassingsData[1];
    private RaceDefinition raceDefinition;
    
    public ReceiveMarkPassingDataTest() throws URISyntaxException,
            MalformedURLException {
        super();
    }
    
    /**
     * Sets up a single listener so that the rather time-consuming race setup is received only once, and all
     * tests in this class share a single feed execution. The listener fills in the first event received
     * into {@link #firstTracked} and {@link #firstData}. All events are converted into {@link GPSFixMovingImpl}
     * objects and appended to the {@link DynamicTrackedRace}s.
     */
    @Before
    public void setupListener() {
        final Race race = getTracTracEvent().getRaceList().iterator().next();
        Receiver receiver = new Receiver() {
            @Override
            public Iterable<TypeController> getTypeControllersAndStart() {
                final TypeController[] markPassingsListener = new TypeController[1];
                markPassingsListener[0] = MarkPassingsData.subscribe(race,
                        new ICallbackData<RaceCompetitor, MarkPassingsData>() {
                            private boolean first = true;

                            @Override
                            public void gotData(RaceCompetitor route, MarkPassingsData record, boolean isLiveData) {
                                if (first) {
                                    synchronized (semaphor) {
                                        firstData[0] = record;
                                        semaphor.notifyAll();
                                        getController().remove(markPassingsListener[0]);
                                    }
                                    first = false;
                                }
                            }
                        });
                return Collections.singleton(markPassingsListener[0]);
            }

            @Override
            public void stopPreemptively() {
            }

            @Override
            public void stopAfterProcessingQueuedEvents() {
            }

            @Override
            public void join() {
            }

            @Override
            public void join(long timeoutInMilliseconds) {
            }

            @Override
            public void stopAfterNotReceivingEventsForSomeTime(long timeoutInMilliseconds) {
            }
        };
        List<Receiver> receivers = new ArrayList<Receiver>();
        receivers.add(receiver);
        for (Receiver r : DomainFactory.INSTANCE.getUpdateReceivers(
                new DynamicTrackedRegattaImpl(DomainFactory.INSTANCE.getOrCreateDefaultRegatta(getTracTracEvent(), /* trackedRegattaRegistry */ null)),
                getTracTracEvent(), EmptyWindStore.INSTANCE, /* startOfTracking */ null, /* endOfTracking */ null, /* delayToLiveInMillis */ 0l,
                /* simulateWithStartTimeNow */ false, new DynamicRaceDefinitionSet() {
                    @Override
                    public void addRaceDefinition(RaceDefinition race) {}
                },
                /* trackedRegattaRegistry */ null, ReceiverType.RACECOURSE, ReceiverType.MARKPOSITIONS, ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS)) {
            receivers.add(r);
        }
        addListenersForStoredDataAndStartController(receivers);
        raceDefinition = DomainFactory.INSTANCE.getAndWaitForRaceDefinition(race);
        synchronized (semaphor) {
            while (firstData[0] == null) {
                try {
                    semaphor.wait();
                } catch (InterruptedException e) {
                    // print, ignore, wait on
                    e.printStackTrace();
                }
            }
        }
    }

    @Test
    public void testReceiveCompetitorPosition() {
        synchronized (semaphor) {
            while (firstData[0] == null) {
                try {
                    semaphor.wait();
                } catch (InterruptedException e) {
                    // print, ignore, wait on
                    e.printStackTrace();
                }
            }
        }
        assertNotNull(firstData[0]);
        assertTrue(firstData[0].getPassings().size() > 0);
        MarkPassingsData.Entry entry = firstData[0].getPassings().iterator().next();
        assertNotNull(entry);
        // we expect to find the mark passings in order, so as we traverse the course for
        // its waypoints and compare their control points to the control point received,
        // the first waypoint is used
        boolean found = false;
        for (Waypoint waypoint : raceDefinition.getCourse().getWaypoints()) {
            if (waypoint.getControlPoint() == DomainFactory.INSTANCE.getOrCreateControlPoint(entry.getControlPoint())) {
                found = true;
            }
        }
        assertTrue(found);
    }

}
