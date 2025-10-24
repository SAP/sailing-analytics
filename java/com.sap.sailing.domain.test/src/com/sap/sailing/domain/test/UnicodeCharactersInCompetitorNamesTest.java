package com.sap.sailing.domain.test;

import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sailing.domain.base.Competitor;
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

@Timeout(value = 2, unit = TimeUnit.MINUTES)
public class UnicodeCharactersInCompetitorNamesTest {
    protected static final boolean tractracTunnel = Boolean.valueOf(System.getProperty("tractrac.tunnel", "false"));
    protected static final String tractracTunnelHost = System.getProperty("tractrac.tunnel.host", "localhost");
    private DomainFactory domainFactory;

    @BeforeEach
    public void setUp() {
        domainFactory = new DomainFactoryImpl(new com.sap.sailing.domain.base.impl.DomainFactoryImpl(com.sap.sailing.domain.base.DomainFactory.TEST_RACE_LOG_RESOLVER));
    }
    
    public static void main(String[] args) throws Exception {
        UnicodeCharactersInCompetitorNamesTest t = new UnicodeCharactersInCompetitorNamesTest();
        t.setUp();
        t.readJSONURLAndCheckCompetitorNames();
        t.testFindUnicodeCharactersInCompetitorNames();
    }
    
    @Test
    public void testFindUnicodeCharactersInCompetitorNames() throws Exception {
        TracTracRaceTracker fourtyninerYellow_2 = domainFactory
                .createRaceTracker(
                        EmptyRaceLogStore.INSTANCE,
                        EmptyRegattaLogStore.INSTANCE,
                        EmptyWindStore.INSTANCE, new DummyTrackedRegattaRegistry(),
                        mock(RaceLogAndTrackedRaceResolver.class), mock(LeaderboardGroupResolver.class), new RaceTrackingConnectivityParametersImpl(new URL(
                                "http://"
                                        + TracTracConnectionConstants.HOST_NAME
                                        + "/events/event_20110609_KielerWoch/clientparams.php?event=event_20110609_KielerWoch&race=5b08a9ee-9933-11e0-85be-406186cbf87c"),
                                tractracTunnel ? new URI("tcp://" + tractracTunnelHost + ":"
                                        + TracTracConnectionConstants.PORT_TUNNEL_LIVE) : new URI("tcp://"
                                        + TracTracConnectionConstants.HOST_NAME + ":" + TracTracConnectionConstants.PORT_LIVE),
                                        tractracTunnel ? new URI("tcp://" + tractracTunnelHost + ":"
                                                + TracTracConnectionConstants.PORT_TUNNEL_STORED)
                                                : new URI("tcp://" + TracTracConnectionConstants.HOST_NAME + ":"
                                                        + TracTracConnectionConstants.PORT_STORED),
                                                new URI("http://tracms.traclive.dk/update_course"),
                                                /* startOfTracking */null, /* endOfTracking */null, /* delayToLiveInMillis */0l,
                                                /* offsetToStartTimeOfSimulatedRace */ null, /* ignoreTracTracMarkPassings*/
                                                false, EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE, domainFactory,
                                                "tracTest", "tracTest", "", "", /* trackWind */ false, /* correctWindDirectionByMagneticDeclination */ true,
                                                /* preferReplayIfAvailable */ false, /* timeoutInMillis */ (int) RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                                                /* useOfficialEventsToUpdateRaceLog */ false, /* liveURIFromConfiguration */ null, /* storedURIFromConfiguration */ null),
                                                RaceTracker.TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS,
                                                new DefaultRaceTrackingHandler(), /* markPassingRaceFingerprintRegistry */ null);

        Iterable<Competitor> competitors = fourtyninerYellow_2.getRaceHandle().getRace().getCompetitors();
        for (Competitor competitor : competitors) {
            System.out.println(competitor.getName());
        }
        fourtyninerYellow_2.stop(/* preemptive */ false);
    }
    
    @Test
    public void readJSONURLAndCheckCompetitorNames() throws MalformedURLException, IOException {
        System.out.println("Default charset: " + Charset.defaultCharset().name() + ". Supported: "
                + Charset.isSupported(Charset.defaultCharset().name()));
        String charsetname = System.getProperty("test.charset", "UTF-8");
        System.out.println("Using "+charsetname+" for input stream reader");
        BufferedReader localBufferedReader = new BufferedReader(
                new InputStreamReader(
                        new URL(
                                "http://" + TracTracConnectionConstants.HOST_NAME + "/events/event_20110609_KielerWoch/clientparams.php?event=event_20110609_KielerWoch&race=5b08a9ee-9933-11e0-85be-406186cbf87c")
                                .openStream(), charsetname));
        String line;
        while ((line=localBufferedReader.readLine()) != null) {
            System.out.println(line);
        }
        localBufferedReader.close();
    }
}
