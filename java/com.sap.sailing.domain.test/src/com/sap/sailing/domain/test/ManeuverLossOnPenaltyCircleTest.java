package com.sap.sailing.domain.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.number.OrderingComparison.greaterThan;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Optional;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ManeuverLossOnPenaltyCircleTest extends OnlineTracTracBasedTest {
    private static final Logger logger = Logger.getLogger(ManeuverLossOnPenaltyCircleTest.class.getName());

    public ManeuverLossOnPenaltyCircleTest() throws MalformedURLException, URISyntaxException {
    }
    
    protected String getExpectedEventName() {
        return "Sailing Champions League 2015";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:////"+new File("resources/SailingChampionsLeague2015-Race28.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:////"+new File("resources/SailingChampionsLeague2015-Race28.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        getTrackedRace().recordWind(new WindImpl(/* position */ null, MillisecondsTimePoint.now(),
                new KnotSpeedWithBearingImpl(13, new DegreeBearingImpl(113))), new WindSourceImpl(WindSourceType.WEB));
    }
    
    @Test
    public void testPenaltyLossForCanottieri() throws NoWindException, InterruptedException {
        Competitor canottieri = getCompetitorByName("Club Canottieri Roggero di Lauria");
        final Iterable<Maneuver> maneuversCanottieri = getTrackedRace().getManeuvers(canottieri, getTrackedRace().getStartOfRace(), getTrackedRace().getEndOfRace(), /* waitForLatest */ true);
        final Optional<Maneuver> penaltyCircleCanottieri = StreamSupport.stream(maneuversCanottieri.spliterator(), /* parallel */ false).filter(m->m.getType()==ManeuverType.PENALTY_CIRCLE).findAny();
        assertThat("Maneuver loss of "+penaltyCircleCanottieri.get()+" too small", penaltyCircleCanottieri.get().getManeuverLoss().getProjectedDistanceLost(), greaterThan(new MeterDistance(30)));
        logger.info("Maneuver loss of "+penaltyCircleCanottieri.get()+" was greater than 30m. Good.");
    }
}
