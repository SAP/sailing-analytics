package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.domain.tractracadapter.impl.TracTracAdapterFactoryImpl;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sse.common.Util;

public class RaceTrackerTest {
    private static final Logger logger = Logger.getLogger(RaceTrackerTest.class.getName());
    protected static final boolean tractracTunnel = Boolean.valueOf(System.getProperty("tractrac.tunnel", "false"));
    protected static final String tractracTunnelHost = System.getProperty("tractrac.tunnel.host", "localhost");
    private final URL paramUrl;
    private final URI liveUri;
    private final URI storedUri;
    private final URI courseDesignUpdateUri;
    private final String tracTracUsername;
    private final String tracTracPassword;
    private RacingEventServiceImpl service;
    private RaceHandle raceHandle;
    private TracTracAdapterFactoryImpl tracTracAdapterFactory;
    
    public RaceTrackerTest() throws MalformedURLException, URISyntaxException {
        // for live simulation:
        //   paramUrl  = new URL("http://sapsimulation.tracdev.dk/simulateconf/j80race12.txt");
        //   liveUri   = new URI("tcp://sapsimulation.tracdev.dk:4420"); // or with tunneling: tcp://localhost:4420
        //   storedUri = new URI("tcp://sapsimulation.tracdev.dk:4421"); // or with tunneling: tcp://localhost:4421
        // for stored race, non-real-time simulation:
        paramUrl  = new URL("http://" + TracTracConnectionConstants.HOST_NAME + "/events/event_20110505_SailingTea/clientparams.php?event=event_20110505_SailingTea&race=bd8c778e-7c65-11e0-8236-406186cbf87c");
        
        if (tractracTunnel) {
            liveUri   = new URI("tcp://"+tractracTunnelHost+":"+TracTracConnectionConstants.PORT_TUNNEL_LIVE);
            storedUri = new URI("tcp://"+tractracTunnelHost+":"+TracTracConnectionConstants.PORT_TUNNEL_STORED);
        } else {
            // no tunnel:
            liveUri = new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_LIVE);
            storedUri = new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_STORED);
        }
        
        courseDesignUpdateUri = new URI("http://tracms.traclive.dk/update_course");
        tracTracUsername = "tracTest";
        tracTracPassword = "tracTest";
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        service = new RacingEventServiceImpl();
        logger.info("Calling service.addTracTracRace");
        tracTracAdapterFactory = new TracTracAdapterFactoryImpl();
        raceHandle = tracTracAdapterFactory.getOrCreateTracTracAdapter(service.getBaseDomainFactory()).addTracTracRace(
                service, paramUrl, liveUri, storedUri, courseDesignUpdateUri, EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, /* timeoutInMilliseconds */60000, tracTracUsername, tracTracPassword,
                TracTracConnectionConstants.ONLINE_STATUS, TracTracConnectionConstants.ONLINE_VISIBILITY,
                /* trackWind */ false, /* correctWindDirectionByMagneticDeclination */ false,
                /* timeoutInMillis */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                /* useOfficialEventsToUpdateRaceLog */ false, new DefaultRaceTrackingHandler());
        logger.info("Calling raceHandle.getRaces()");
        RaceDefinition race = raceHandle.getRace(); // wait for RaceDefinition to be completely wired in Regatta
        logger.info("Obtained race: "+race);
        assertNotNull(race);
        assertTrue(!Util.isEmpty(raceHandle.getRegatta().getAllRaces()));
    }
    
    @AfterEach
    public void tearDown() throws MalformedURLException, IOException, InterruptedException {
        logger.info("calling stopTrackingAndRemove("+raceHandle.getRegatta().getName()+" ("+raceHandle.getRegatta().hashCode()+"))");
        service.stopTrackingAndRemove(raceHandle.getRegatta());
    }

    private TrackedRace getTrackedRace(TrackedRegatta trackedRegatta) throws InterruptedException {
        final TrackedRace[] trackedRaces = new TrackedRace[1];
        trackedRegatta.addRaceListener(new RaceListener() {
            @Override
            public void raceAdded(TrackedRace trackedRace) {
                synchronized (trackedRaces) {
                    trackedRaces[0] = trackedRace;
                    trackedRaces.notifyAll();
                }
            }
            @Override
            public void raceRemoved(TrackedRace trackedRace) {
            }
        }, Optional.empty(), /* synchronous */ false);
        synchronized (trackedRaces) {
            if (trackedRaces[0] == null) {
                trackedRaces.wait();
            }
        }
        return trackedRaces[0];
    }

    @Test
    public void testInitialization() throws InterruptedException {
        logger.entering(getClass().getName(), "testInitialization");
        RaceDefinition race = raceHandle.getRace();
        assertNotNull(race);
        assertNotNull(getTrackedRace(raceHandle.getTrackedRegatta()));
        logger.exiting(getClass().getName(), "testInitialization");
    }
    
    @Test
    public void testStopTracking() throws Exception {
        logger.entering(getClass().getName(), "testStopTracking");
        assertTrue(!Util.isEmpty(raceHandle.getRegatta().getAllRaces()));
        TrackedRegatta oldTrackedRegatta = raceHandle.getTrackedRegatta();
        TrackedRace oldTrackedRace = getTrackedRace(oldTrackedRegatta);
        RaceDefinition oldRaceDefinition = oldTrackedRace.getRace();
        assertTrue(!Util.isEmpty(raceHandle.getRegatta().getAllRaces()));
        service.removeRegatta(raceHandle.getRegatta());
        RaceHandle myRaceHandle = tracTracAdapterFactory.getOrCreateTracTracAdapter(service.getBaseDomainFactory())
                .addTracTracRace(service, paramUrl, liveUri, storedUri, courseDesignUpdateUri,
                        EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, /* timeoutInMilliseconds */60000,
                        tracTracUsername, tracTracPassword, TracTracConnectionConstants.ONLINE_STATUS,
                        TracTracConnectionConstants.ONLINE_VISIBILITY, /* trackWind */ false,
                        /* correctWindDirectionByMagneticDeclination */ false, /* timeoutInMillis */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        /* useOfficialEventsToUpdateRaceLog */ false, new DefaultRaceTrackingHandler());
        TrackedRegatta newTrackedRegatta = myRaceHandle.getTrackedRegatta();
        assertNotSame(oldTrackedRegatta, newTrackedRegatta);
        TrackedRace newTrackedRace = getTrackedRace(newTrackedRegatta);
        // expecting a new tracked race to be created when starting over with tracking
        try {
            assertNotSame(oldTrackedRace, newTrackedRace);
            assertNotSame(oldRaceDefinition, newTrackedRace.getRace());
        } finally {
            service.stopTracking(myRaceHandle.getRegatta(), /* willBeRemoved */ false);
        }
        logger.exiting(getClass().getName(), "testStopTracking");
    }

    /**
     * This test asserts that tracking the same race twice doesn't create another tracker and in particular no
     * new tracked regatta / tracked race.
     * @throws Exception 
     */
    @Test
    public void testTrackingSameRaceWithoutStopping() throws Exception {
        logger.entering(getClass().getName(), "testTrackingSameRaceWithoutStopping");
        TrackedRegatta oldTrackedRegatta = raceHandle.getTrackedRegatta();
        TrackedRace oldTrackedRace = getTrackedRace(oldTrackedRegatta);
        RaceHandle myRaceHandle = tracTracAdapterFactory.getOrCreateTracTracAdapter(service.getBaseDomainFactory())
                .addTracTracRace(service, paramUrl, liveUri, storedUri, courseDesignUpdateUri,
                        EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, /* timeoutInMilliseconds */60000,
                        tracTracUsername, tracTracPassword, TracTracConnectionConstants.ONLINE_STATUS,
                        TracTracConnectionConstants.ONLINE_VISIBILITY, /* trackWind */ false,
                        /* correctWindDirectionByMagneticDeclination */ false, /* timeoutInMillis */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        /* useOfficialEventsToUpdateRaceLog */ false, new DefaultRaceTrackingHandler());
        TrackedRegatta newTrackedEvent = myRaceHandle.getTrackedRegatta();
        TrackedRace newTrackedRace = getTrackedRace(newTrackedEvent);
        // expecting a new tracked race to be created when starting over with tracking
        try {
            assertSame(oldTrackedRace, newTrackedRace);
            assertSame(raceHandle.getRaceTracker(), myRaceHandle.getRaceTracker());
        } finally {
            service.stopTracking(myRaceHandle.getRegatta(), /* willBeRemoved */ false);
        }
        logger.exiting(getClass().getName(), "testStopTracking");
    }
}
