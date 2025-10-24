package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * This is a test for bug 2009 (see http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=2009).
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class PhilippBuhlsDoublePenaltyCircleAtKielerWoche2014Test extends AbstractManeuverDetectionTestCase {
    public PhilippBuhlsDoublePenaltyCircleAtKielerWoche2014Test() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Override
    protected String getExpectedEventName() {
        return "Kieler Woche 2014 - Olympic Week";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20140619_KieleWoche-R1_Blue_Laser.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(
                new URL("file:///" + new File("resources/event_20140619_KieleWoche-R1_Blue_Laser.txt").getCanonicalPath()),
                /* liveUri */null, /* storedUri */storedUri, new ReceiverType[] { ReceiverType.MARKPASSINGS,
                        ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, new MillisecondsTimePoint(dateFormat.parse("06/21/2014-13:03:35")),
                        new KnotSpeedWithBearingImpl(18, new DegreeBearingImpl(296))),
                new WindSourceImpl(WindSourceType.WEB));
    }
    
    /**
     * Asserts that Philipp Buhl is having two penalty circles detected in the time between 13:03:18+0200 and 13:03:47+0200
     */
    @Test
    public void testDoublePenaltyForPhilippAndTobiasAndMaximAndDharmender() throws ParseException, NoWindException {
        assertTwoPenalties("Buhl",             "06/21/2014-13:03:18", "06/21/2014-13:03:47", "06/21/2014-13:03:30", "06/21/2014-13:03:40");
        assertTwoPenalties("Dharmender Singh", "06/21/2014-12:51:40", "06/21/2014-12:52:40", "06/21/2014-12:52:01", "06/21/2014-12:52:10");
        assertTwoPenalties("Schadewaldt",      "06/21/2014-12:46:50", "06/21/2014-12:47:30", "06/21/2014-12:47:10", "06/21/2014-12:47:19");
        assertTwoPenalties("Maxim Nikolaev",   "06/21/2014-12:49:22", "06/21/2014-12:50:13", "06/21/2014-12:49:37", "06/21/2014-12:49:52");
    }

    private void assertTwoPenalties(String competitorName, final String from, final String to,
            final String firstPenalty, final String secondPenalty) throws ParseException, NoWindException {
        Competitor competitor = getCompetitorByName(competitorName);
        Date fromDate = dateFormat.parse(from);
        Date toDate = dateFormat.parse(to);
        assertNotNull(fromDate);
        assertNotNull(toDate);
        assertNotNull(competitor);
        Iterable<Maneuver> maneuvers = getTrackedRace().getManeuvers(competitor, new MillisecondsTimePoint(fromDate),
                new MillisecondsTimePoint(toDate), /* waitForLatest */ true);
        maneuversInvalid = new ArrayList<Maneuver>();
        Util.addAll(maneuvers, maneuversInvalid);
        for (Maneuver maneuver : maneuvers) {
            if (maneuver.getType() == ManeuverType.PENALTY_CIRCLE) {
                assertTrue(Math.abs(maneuver.getDirectionChangeInDegrees()) < 700); // the second penalty has to count for its own
            }
        }
        assertManeuver(maneuvers, ManeuverType.PENALTY_CIRCLE, new MillisecondsTimePoint(dateFormat.parse(firstPenalty)), 5000);
        assertManeuver(maneuvers, ManeuverType.PENALTY_CIRCLE, new MillisecondsTimePoint(dateFormat.parse(secondPenalty)), 5000);
        assertAllManeuversOfTypesDetected(Collections.singletonList(ManeuverType.PENALTY_CIRCLE), maneuversInvalid);
    }
}
