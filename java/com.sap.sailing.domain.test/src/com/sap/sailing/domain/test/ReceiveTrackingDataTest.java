package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;

public class ReceiveTrackingDataTest extends AbstractTracTracLiveTest {
    final private Object semaphor = new Object();
    final private Competitor[] firstTracked = new Competitor[1];
    final private GPSFix[] firstData = new GPSFix[1];

    public ReceiveTrackingDataTest() throws URISyntaxException,
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
        final DomainFactory domainFactory = DomainFactory.INSTANCE;
        final RaceChangeListener positionListener = new AbstractRaceChangeListener() {
            private boolean first = true;
            
            @Override
            public void competitorPositionChanged(GPSFixMoving fix, Competitor competitor, AddResult addedOrReplaced) {
                synchronized (semaphor) {
                    if (first) {
                        firstTracked[0] = competitor;
                        firstData[0] = fix;
                        first = false;
                    }
                    semaphor.notifyAll();
                }
            }
        };
        Regatta regatta = domainFactory.getOrCreateDefaultRegatta(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                getTracTracRace(), /* trackedRegattaRegistry */ null);
        DynamicTrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        trackedRegatta.addRaceListener(new RaceListener() {
            @Override
            public void raceAdded(TrackedRace trackedRace) {
                System.out.println("Subscribing raw position listener for race "+trackedRace);
                ((DynamicTrackedRace) trackedRace).addListener(positionListener);
            }
            @Override
            public void raceRemoved(TrackedRace trackedRace) {
            }
        }, Optional.empty(), /* synchronous */ false);
        for (Receiver receiver : domainFactory
                .getUpdateReceivers(trackedRegatta, /* delayToLiveInMillis */0l,
                        /* simulator */null, EmptyWindStore.INSTANCE, new DynamicRaceDefinitionSet() {
                            @Override
                            public void addRaceDefinition(RaceDefinition race, DynamicTrackedRace trackedRace) {
                            }
                        },
                        /* trackedRegattaRegistry */null, mock(RaceLogAndTrackedRaceResolver.class), /* markPassingRaceFingerprintRegistry */ null, mock(LeaderboardGroupResolver.class), /* courseDesignUpdateURI */
                        getTracTracRace(), null, /* tracTracUsername */null, /* tracTracPassword */null, getEventSubscriber(), getRaceSubscriber(), /*ignoreTracTracMarkPassings*/ false,
                        RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS, new DefaultRaceTrackingHandler(), /* raceAndCompetitorStatusWithRaceLogReconciler */ null)) {
            receiver.subscribe();
            getRaceSubscriber().start();
            addReceiverToStopDuringTearDown(receiver);
        }
        addListenersForStoredDataAndStartController(domainFactory
                .getUpdateReceivers(trackedRegatta, /* delayToLiveInMillis */0l, null, /* simulator */
                        EmptyWindStore.INSTANCE, /* simulateWithStartTimeNow */
                        new DynamicRaceDefinitionSet() {
                            @Override
                            public void addRaceDefinition(RaceDefinition race, DynamicTrackedRace trackedRace) {
                            }
                        }, /* trackedRegattaRegistry */null, mock(RaceLogAndTrackedRaceResolver.class), /* markPassingRaceFingerprintRegistry */ null, mock(LeaderboardGroupResolver.class), /* courseDesignUpdateURI */
                        getTracTracRace(), null, /* tracTracUsername */null, /* tracTracPassword */null, getEventSubscriber(), getRaceSubscriber(), /*ignoreTracTracMarkPassings*/ false,
                        RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS, new DefaultRaceTrackingHandler(), /* raceAndCompetitorStatusWithRaceLogReconciler */ null));
    }

    @Test
    public void testReceiveCompetitorPosition() {
        synchronized (semaphor) {
            while (firstTracked[0] == null) {
                try {
                    semaphor.wait();
                } catch (InterruptedException e) {
                    // print, ignore, wait on
                    e.printStackTrace();
                }
            }
        }
        assertNotNull(firstTracked[0]);
        assertNotNull(firstData[0]);
    }

}
