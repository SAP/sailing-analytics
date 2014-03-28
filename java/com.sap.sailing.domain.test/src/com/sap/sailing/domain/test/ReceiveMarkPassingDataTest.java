package com.sap.sailing.domain.test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sailing.domain.tractracadapter.impl.ControlPointAdapter;
import com.sap.sailing.domain.tractracadapter.impl.SynchronizationUtil;
import com.tractrac.model.lib.api.data.IControlPassing;
import com.tractrac.model.lib.api.data.IControlPassings;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.subscription.lib.api.control.IControlPassingsListener;

public class ReceiveMarkPassingDataTest extends AbstractTracTracLiveTest {
    final private Object semaphor = new Object();
    final private IControlPassings[] firstData = new IControlPassings[1];
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
        final IRace race = SynchronizationUtil.getRaces(getTracTracEvent()).iterator().next();
        Receiver receiver = new Receiver() {
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

            @Override
            public void subscribe() {
                getRaceSubscriber().subscribeControlPassings(new IControlPassingsListener() {
                    private boolean first = true;
                    
                    @Override
                    public void gotControlPassings(IRaceCompetitor raceCompetitor, IControlPassings controlPassings) {
                        if (first) {
                            synchronized (semaphor) {
                                firstData[0] = controlPassings;
                                semaphor.notifyAll();
                                getRaceSubscriber().unsubscribeControlPassings(this);
                            }
                            first = false;
                        }
                    }
                });
            }
        };
        List<Receiver> receivers = new ArrayList<Receiver>();
        receivers.add(receiver);
        for (Receiver r : DomainFactory.INSTANCE.getUpdateReceivers(
                new DynamicTrackedRegattaImpl(DomainFactory.INSTANCE.getOrCreateDefaultRegatta(
                        EmptyRaceLogStore.INSTANCE, getTracTracEvent(), /* trackedRegattaRegistry */null)),
                        SynchronizationUtil.getRaces(getTracTracEvent()).iterator().next(), getTracTracEvent(), EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 0l,
                /* simulator */null, new DynamicRaceDefinitionSet() {
                    @Override
                    public void addRaceDefinition(RaceDefinition race, DynamicTrackedRace trackedRace) {
                    }
                },
                /* trackedRegattaRegistry */null, /* courseDesignUpdateURI */null, /* tracTracUsername */null, /* tracTracPassword */
                null, getEventSubscriber(), getRaceSubscriber(), ReceiverType.RACECOURSE, ReceiverType.MARKPOSITIONS,
                ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS)) {
            receivers.add(r);
        }
        addListenersForStoredDataAndStartController(receivers);
        raceDefinition = DomainFactory.INSTANCE.getAndWaitForRaceDefinition(race.getId());
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
        IControlPassing entry = firstData[0].getPassings().iterator().next();
        assertNotNull(entry);
        // we expect to find the mark passings in order, so as we traverse the course for
        // its waypoints and compare their control points to the control point received,
        // the first waypoint is used
        boolean found = false;
        for (Waypoint waypoint : raceDefinition.getCourse().getWaypoints()) {
            if (waypoint.getControlPoint() == DomainFactory.INSTANCE.getOrCreateControlPoint(new ControlPointAdapter(entry.getControl()))) {
                found = true;
            }
        }
        assertTrue(found);
    }

}
