package com.sap.sailing.domain.test;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;

public class ManeuverAnalysis505Test extends AbstractManeuverDetectionTestCase {

    public ManeuverAnalysis505Test() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Before
    public void setUp() throws URISyntaxException, IOException, InterruptedException {
        super.setUp();
        super.setUp("event_20110609_KielerWoch",
        /* raceId */"357c700a-9d9a-11e0-85be-406186cbf87c", new ReceiverType[] { ReceiverType.MARKPASSINGS,
                ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(), new MillisecondsTimePoint(
                new GregorianCalendar(2011, 05, 23).getTime()));
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, MillisecondsTimePoint.now(), new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(65))), new WindSourceImpl(WindSourceType.WEB));
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
        List<Maneuver> maneuvers = getTrackedRace().getManeuvers(competitor, new MillisecondsTimePoint(fromDate),
                new MillisecondsTimePoint(toDate), /* waitForLatest */ true);
        maneuversInvalid = new ArrayList<Maneuver>(maneuvers);

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
        List<Maneuver> maneuvers = getTrackedRace().getManeuvers(competitor, new MillisecondsTimePoint(fromDate),
                new MillisecondsTimePoint(toDate), /* waitForLatest */ true);
        maneuversInvalid = new ArrayList<Maneuver>(maneuvers);
        
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
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-15:50:41")), JIBE_TOLERANCE);

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
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:31:29")), TACK_TOLERANCE);
        assertManeuver(maneuvers, ManeuverType.TACK,
                new MillisecondsTimePoint(dateFormat.parse("06/23/2011-16:38:00")), TACK_TOLERANCE);

        List<ManeuverType> maneuverTypesFound = new ArrayList<ManeuverType>();
        maneuverTypesFound.add(ManeuverType.TACK);
        maneuverTypesFound.add(ManeuverType.JIBE);
        maneuverTypesFound.add(ManeuverType.PENALTY_CIRCLE);
        assertAllManeuversOfTypesDetected(maneuverTypesFound, maneuversInvalid);
    }
}
