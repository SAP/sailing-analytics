package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

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

public class ManeuverAnalysis505Test extends AbstractManeuverDetectionTestCase {
    public ManeuverAnalysis505Test() throws MalformedURLException, URISyntaxException {
        super();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///"+new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(), new MillisecondsTimePoint(
                new GregorianCalendar(2011, 05, 23).getTime()));
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:53:30")),
                        new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(55))),
                new WindSourceImpl(WindSourceType.WEB));
    }
    
    /**
     * Tests the 505 Race 2 for competitor "Findel" at a time where the maneuver detection test is likely to fail
     */
    @Test
    public void testManeuversForFindelCriticalDetection() throws ParseException, NoWindException {
        Competitor competitor = getCompetitorByName("Findel");
        assertNotNull(competitor);
        Date fromDate = dateFormat.parse("06/23/2011-15:28:00");
        Date toDate = dateFormat.parse("06/23/2011-15:29:50");
        assertNotNull(fromDate);
        assertNotNull(toDate);
        Iterable<Maneuver> maneuvers = getTrackedRace().getManeuvers(competitor, new MillisecondsTimePoint(fromDate),
                new MillisecondsTimePoint(toDate), /* waitForLatest */ true);
        maneuversInvalid = new ArrayList<Maneuver>();
        Util.addAll(maneuvers, maneuversInvalid);

        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:28:24")), TACK_TOLERANCE);

        List<ManeuverType> maneuverTypesFound = new ArrayList<ManeuverType>();
        maneuverTypesFound.add(ManeuverType.TACK);
        assertAllManeuversOfTypesDetected(maneuverTypesFound, maneuversInvalid);
    }
    
    /**
     * Test for 505 Race 2 for competitor "Findel"
     */
    @Test
    public void testManeuversForFindel() throws ParseException, NoWindException {
        Competitor competitor = getCompetitorByName("Findel");
        assertNotNull(competitor);
        Date fromDate = dateFormat.parse("06/23/2011-15:28:04");
        Date toDate = dateFormat.parse("06/23/2011-16:38:01");
        assertNotNull(fromDate);
        assertNotNull(toDate);
        Iterable<Maneuver> maneuvers = getTrackedRace().getManeuvers(competitor, new MillisecondsTimePoint(fromDate),
                new MillisecondsTimePoint(toDate), /* waitForLatest */ true);
        maneuversInvalid = new ArrayList<Maneuver>();
        Util.addAll(maneuvers, maneuversInvalid);
        
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:28:24")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:38:01")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:40:28")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:40:52")), TACK_TOLERANCE);

        assertManeuver(maneuvers, ManeuverType.JIBE,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:46:07")), JIBE_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.JIBE,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:49:06")), JIBE_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.JIBE,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:50:50")), JIBE_TOLERANCE);

        /*
         * Findel's track has an interesting challenge. When Findel, at 15:53:30, rounds the leeward gate, this has to
         * be recognized as a HEAD_UP by the algorithm, and the wind has to be set to point to 55deg exactly during the
         * maneuver (to protect against a somewhat off estimation that reads 43deg which would cause this to be
         * recognized as a jibe) whereas Findel's course *before* the maneuver was 48deg, then heading up by 111deg.
         */
        assertManeuver(maneuvers, ManeuverType.PENALTY_CIRCLE,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:53:45")), PENALTYCIRCLE_TOLERANCE);

        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:54:01")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:58:27")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:03:19")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:04:41")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:05:25")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:05:43")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:06:16")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:07:33")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:11:27")), TACK_TOLERANCE);

        assertManeuver(maneuvers, ManeuverType.JIBE,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:13:28")), JIBE_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.JIBE,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:18:27")), JIBE_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.JIBE,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:21:28")), JIBE_TOLERANCE);

        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:26:14")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:28:21")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:31:36")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:38:00")), TACK_TOLERANCE);

        List<ManeuverType> maneuverTypesFound = new ArrayList<ManeuverType>();
        maneuverTypesFound.add(ManeuverType.TACK);
        maneuverTypesFound.add(ManeuverType.JIBE);
        maneuverTypesFound.add(ManeuverType.PENALTY_CIRCLE);
        assertAllManeuversOfTypesDetected(maneuverTypesFound, maneuversInvalid);
    }
}
