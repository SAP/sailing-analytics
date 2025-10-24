package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.LoadingQueueDoneCallBack;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.tractrac.model.lib.api.data.IControlPassing;
import com.tractrac.model.lib.api.data.IControlPassings;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.subscription.lib.api.control.IControlPassingsListener;

public class ReceiveMarkPassingDataTest extends AbstractTracTracLiveTest {
    private static final Logger logger = Logger.getLogger(ReceiveMarkPassingDataTest.class.getName());
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
    @BeforeEach
    public void setupListener() {
        final IRace race = getTracTracRace();
        logger.info("Setting up listener for race "+race.getName()+" with ID "+race.getId());
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
                    public void gotControlPassings(long timestamp, IRaceCompetitor raceCompetitor, IControlPassings controlPassings) {
                        logger.info("Received control passings "+controlPassings+" for competitor "+raceCompetitor);
                        if (first) {
                            logger.info("Was first");
                            synchronized (semaphor) {
                                firstData[0] = controlPassings;
                                logger.info("Notifying all");
                                semaphor.notifyAll();
                                logger.info("unsubscribing");
                                getRaceSubscriber().unsubscribeControlPassings(this);
                            }
                            first = false;
                        }
                    }
                });
            }

            @Override
            public void callBackWhenLoadingQueueIsDone(LoadingQueueDoneCallBack callback) {    
            }
        };
        List<Receiver> receivers = new ArrayList<Receiver>();
        receivers.add(receiver);
        logger.info("Getting update receivers for TracTrac race "+getTracTracRace().getName()+" with ID "+getTracTracRace().getId()+
                " where the event has as its first race "+getTracTracEvent().getRaces().iterator().next().getName()+" with ID "+
                getTracTracEvent().getRaces().iterator().next().getId());
        for (Receiver r : DomainFactory.INSTANCE.getUpdateReceivers(
                new DynamicTrackedRegattaImpl(DomainFactory.INSTANCE.getOrCreateDefaultRegatta(
                        EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, getTracTracRace(), /* trackedRegattaRegistry */null)),
                        getTracTracRace(), EmptyWindStore.INSTANCE, /* delayToLiveInMillis */ 0l,
                /* simulator */null,
                new DynamicRaceDefinitionSet() {
                    @Override
                    public void addRaceDefinition(RaceDefinition race, DynamicTrackedRace trackedRace) {
                    }
                }, /* trackedRegattaRegistry */null,
                mock(RaceLogAndTrackedRaceResolver.class), /* markPassingRaceFingerprintRegistry */ null, mock(LeaderboardGroupResolver.class), /* courseDesignUpdateURI */null, /* tracTracPassword */
                /* tracTracUsername */null, null, getEventSubscriber(), getRaceSubscriber(), /*ignoreTracTracMarkPassings*/ false,
                RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS, new DefaultRaceTrackingHandler(),
                /* raceAndCompetitorStatusWithRaceLogReconciler */ null, ReceiverType.RACECOURSE, ReceiverType.MARKPOSITIONS, ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS)) {
            receivers.add(r);
            addReceiverToStopDuringTearDown(r);
        }
        addListenersForStoredDataAndStartController(receivers);
        logger.info("Waiting for race definition "+race.getName()+" with ID "+race.getId());
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
        final IControlPassing entry = firstData[0].getPassings().iterator().next();
        assertNotNull(entry);
        // we expect to find the mark passings in order, so as we traverse the course for
        // its waypoints and compare their control points to the control point received,
        // the first waypoint is used
        boolean found = false;
        for (Waypoint waypoint : raceDefinition.getCourse().getWaypoints()) {
            if (waypoint.getControlPoint() == DomainFactory.INSTANCE.getOrCreateControlPoint(entry.getControl())) {
                found = true;
            }
        }
        assertTrue(found);
    }
}
