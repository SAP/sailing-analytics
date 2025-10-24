package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.domain.tractracadapter.TracTracRaceTracker;
import com.sap.sailing.domain.tractracadapter.impl.DomainFactoryImpl;
import com.sap.sailing.domain.tractracadapter.impl.RaceTrackingConnectivityParametersImpl;

public class MultipleClassesInRegattaTest {
    private static final boolean tractracTunnel = Boolean.valueOf(System.getProperty("tractrac.tunnel", "false"));
    private static final String tractracTunnelHost = System.getProperty("tractrac.tunnel.host", "localhost");
    private DomainFactory domainFactory;
    private TracTracRaceTracker kiwotest1;
    private TracTracRaceTracker kiwotest2;
    private TracTracRaceTracker kiwotest3;
    private TracTracRaceTracker weym470may112014_2;
    
    @BeforeEach
    public void setUp() {
        domainFactory = new DomainFactoryImpl(new com.sap.sailing.domain.base.impl.DomainFactoryImpl(com.sap.sailing.domain.base.DomainFactory.TEST_RACE_LOG_RESOLVER));
    }
    
    @Test
    public void testLoadTwoRacesWithEqualEventNameButDifferentClasses() throws Exception {
        String httpAndHost = "http://" + TracTracConnectionConstants.HOST_NAME;
        String liveURI = "tcp://" + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_LIVE;
        String storedURI = "tcp://" + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_STORED;
        String courseDesignUpdateURI = "http://tracms.traclive.dk/update_course";
        String tracTracUsername = "tracTest";
        String tracTracPassword = "tracTest";
        if (tractracTunnel) {
            liveURI   = "tcp://"+tractracTunnelHost+":"+TracTracConnectionConstants.PORT_TUNNEL_LIVE;
            storedURI = "tcp://"+tractracTunnelHost+":"+TracTracConnectionConstants.PORT_TUNNEL_STORED;
        }
        kiwotest1 = domainFactory
                .createRaceTracker(
                        EmptyRaceLogStore.INSTANCE,
                        EmptyRegattaLogStore.INSTANCE, EmptyWindStore.INSTANCE, new DummyTrackedRegattaRegistry(),
                        mock(RaceLogAndTrackedRaceResolver.class), mock(LeaderboardGroupResolver.class), createConnectivityParams(httpAndHost, liveURI, storedURI, courseDesignUpdateURI,
                                tracTracUsername, tracTracPassword, "cce678c8-97e6-11e0-9aed-406186cbf87c"), RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        new DefaultRaceTrackingHandler(), /* markPassingRaceFingerprintRegistry */ null);
        kiwotest2 = domainFactory
                .createRaceTracker(
                        EmptyRaceLogStore.INSTANCE,
                        EmptyRegattaLogStore.INSTANCE, EmptyWindStore.INSTANCE, new DummyTrackedRegattaRegistry(),
                        mock(RaceLogAndTrackedRaceResolver.class), mock(LeaderboardGroupResolver.class), createConnectivityParams(httpAndHost, liveURI, storedURI, courseDesignUpdateURI,
                                tracTracUsername, tracTracPassword, "11290bd6-97e7-11e0-9aed-406186cbf87c"), RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        new DefaultRaceTrackingHandler(), /* markPassingRaceFingerprintRegistry */ null);
        kiwotest3 = domainFactory
                .createRaceTracker(
                        EmptyRaceLogStore.INSTANCE,
                        EmptyRegattaLogStore.INSTANCE, EmptyWindStore.INSTANCE, new DummyTrackedRegattaRegistry(),
                        mock(RaceLogAndTrackedRaceResolver.class), mock(LeaderboardGroupResolver.class), createConnectivityParams(httpAndHost, liveURI, storedURI, courseDesignUpdateURI,
                                tracTracUsername, tracTracPassword, "39635b24-97e7-11e0-9aed-406186cbf87c"), RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        new DefaultRaceTrackingHandler(), /* markPassingRaceFingerprintRegistry */ null);
        weym470may112014_2 = domainFactory
                .createRaceTracker(
                        EmptyRaceLogStore.INSTANCE,
                        EmptyRegattaLogStore.INSTANCE, EmptyWindStore.INSTANCE, new DummyTrackedRegattaRegistry(),
                        mock(RaceLogAndTrackedRaceResolver.class), mock(LeaderboardGroupResolver.class), createConnectivityParams(httpAndHost, liveURI, storedURI, courseDesignUpdateURI,
                                tracTracUsername, tracTracPassword, "04498426-7dfd-11e0-8236-406186cbf87c"), RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                        new DefaultRaceTrackingHandler(), /* markPassingRaceFingerprintRegistry */ null);

        assertEquals("STG", kiwotest1.getRegatta().getBoatClass().getName());
        assertEquals("5O5", kiwotest2.getRegatta().getBoatClass().getName());
        assertEquals("49er", kiwotest3.getRegatta().getBoatClass().getName());
        assertEquals("STG", weym470may112014_2.getRegatta().getBoatClass().getName());
        assertNotSame(kiwotest1.getRegatta(), kiwotest2.getRegatta());
        assertNotSame(kiwotest1.getRegatta(), kiwotest3.getRegatta());
        assertNotSame(kiwotest2.getRegatta(), kiwotest3.getRegatta());
    }

    private RaceTrackingConnectivityParametersImpl createConnectivityParams(String httpAndHost, String liveURI,
            String storedURI, String courseDesignUpdateURI, String tracTracUsername, String tracTracPassword, String raceId)
            throws Exception, MalformedURLException, URISyntaxException {
        return new RaceTrackingConnectivityParametersImpl(new URL(
                httpAndHost
                + "/events/event_20110505_SailingTea/clientparams.php?event=event_20110505_SailingTea&race="+raceId),
                new URI(liveURI), new URI(storedURI), new URI(courseDesignUpdateURI),
                /* startOfTracking */null, /* endOfTracking */null, /* delayToLiveInMillis */0l,
                /* offsetToStartTimeOfSimulatedRace */ null, /* ignoreTracTracMarkPassings*/
                false, EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, domainFactory,
                tracTracUsername, tracTracPassword, TracTracConnectionConstants.ONLINE_STATUS, TracTracConnectionConstants.ONLINE_VISIBILITY,
                /* trackWind */ false, /* correctWindDirectionByMagneticDeclination */ true, /* preferReplayIfAvailable */ false, /* timeoutInMillis */ -1,
                /* useOfficialEventsToUpdateRaceLog */ false, /* liveURIFromConfiguration */ null, /* storedURIFromConfiguration */ null);
    }
    
    @AfterEach
    public void tearDown() throws MalformedURLException, IOException, InterruptedException {
        kiwotest1.stop(/* preemptive */ false);
        kiwotest2.stop(/* preemptive */ false);
        kiwotest3.stop(/* preemptive */ false);
        weym470may112014_2.stop(/* preemptive */ false);
    }
}
