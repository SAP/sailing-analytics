package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.NavigableSet;
import java.util.TimeZone;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.DouglasPeucker;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ManeuverDetectionOnKielerWoche505Race2DataTest extends AbstractManeuverDetectionTestCase {
    private static final Logger logger = Logger
            .getLogger(ManeuverDetectionOnKielerWoche505Race2DataTest.class.getName());

    public ManeuverDetectionOnKielerWoche505Race2DataTest() throws MalformedURLException, URISyntaxException {
    }
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:////"+new File("resources/event_20110609_KielerWoch-505_Race_2.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:////"+new File("resources/event_20110609_KielerWoch-505_Race_2.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        OnlineTracTracBasedTest.fixApproximateMarkPositionsForWindReadOut(getTrackedRace(), new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()));
        getTrackedRace().recordWind(new WindImpl(/* position */ null, getTrackedRace().getStartOfRace(),
                new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(65))), new WindSourceImpl(WindSourceType.WEB));
        logger.info("Waiting for things to settle in, such as wind updates...");
        Thread.sleep(2000);
        logger.info("...hopefully all is settled now.");
    }
    
    @Test
    public void testDouglasPeuckerForHasso() throws NoWindException {
        Competitor hasso = getCompetitorByName("Dr.Plattner");
        GPSFixTrack<Competitor, GPSFixMoving> hassosTrack = getTrackedRace().getTrack(hasso);
        NavigableSet<MarkPassing> hassosMarkPassings = getTrackedRace().getMarkPassings(hasso);
        DouglasPeucker<Competitor, GPSFixMoving> dp = new DouglasPeucker<Competitor, GPSFixMoving>(hassosTrack);
        Iterator<MarkPassing> hassosMarkPassingsIter = hassosMarkPassings.iterator();
        TimePoint startMarkPassing = hassosMarkPassingsIter.next().getTimePoint();
        TimePoint firstWindwardMarkPassing = hassosMarkPassingsIter.next().getTimePoint();
        List<GPSFixMoving> firstLegFineApproximation = dp.approximate(new MeterDistance(20), startMarkPassing, firstWindwardMarkPassing);
        assertNotNull(firstLegFineApproximation);
        assertEquals(9, firstLegFineApproximation.size()); // changed from 11 to 9 with bug 5425 because a few outliers are now removed
        List<GPSFixMoving> firstLegCoarseApproximation = dp.approximate(new MeterDistance(50), startMarkPassing, firstWindwardMarkPassing);
        assertNotNull(firstLegCoarseApproximation);
        assertEquals(4, firstLegCoarseApproximation.size());
        TimePoint leewardGatePassing = hassosMarkPassingsIter.next().getTimePoint();
        List<GPSFixMoving> secondLegFineApproximation = dp.approximate(new MeterDistance(20), firstWindwardMarkPassing, leewardGatePassing);
        assertNotNull(secondLegFineApproximation);
        assertEquals(7, secondLegFineApproximation.size());
        List<GPSFixMoving> secondLegCoarseApproximation = dp.approximate(new MeterDistance(50), firstWindwardMarkPassing, leewardGatePassing);
        assertNotNull(secondLegCoarseApproximation);
        assertEquals(3, secondLegCoarseApproximation.size());
    }
    
    @Test
    public void testManeuversForHasso() throws NoWindException {
        Competitor hasso = getCompetitorByName("Dr.Plattner");
        NavigableSet<MarkPassing> hassosMarkPassings = getTrackedRace().getMarkPassings(hasso);
        Iterable<Maneuver> maneuvers = getTrackedRace().getManeuvers(hasso, hassosMarkPassings.first().getTimePoint(),
                hassosMarkPassings.last().getTimePoint(), /* waitForLatest */ true);
        maneuversInvalid = new ArrayList<Maneuver>();
        Util.addAll(maneuvers, maneuversInvalid);
        Calendar c = new GregorianCalendar(TimeZone.getTimeZone("Europe/Berlin"));
        c.clear();
        c.set(2011, 6-1, 23, 16, 5, 47);
        assertManeuver(maneuvers, ManeuverType.TACK, Tack.PORT, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 5000);
        c.set(2011, 6-1, 23, 16, 8, 35);
        assertManeuver(maneuvers, ManeuverType.TACK, Tack.STARBOARD, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 5000);
        c.set(2011, 6-1, 23, 16, 11, 03);
        assertManeuver(maneuvers, ManeuverType.TACK, Tack.PORT, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 5000);
        c.set(2011, 6-1, 23, 16, 16, 13);
        assertManeuver(maneuvers, ManeuverType.JIBE, Tack.PORT, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 5000);
        c.set(2011, 6-1, 23, 16, 20, 1);
        assertManeuver(maneuvers, ManeuverType.JIBE, Tack.STARBOARD, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 5000);
        // The following used to be required to be recognized as a Gybe. However, the maneuver
        // angle reported after full maneuver analysis is only -14deg which is below the current
        // threshold for general gybing angles.
        c.set(2011, 6-1, 23, 16, 22, 25);
        assertManeuver(maneuvers, ManeuverType.JIBE, Tack.PORT, new MillisecondsTimePoint(c.getTime()), /* tolerance in milliseconds */ 5000);
    }
}
