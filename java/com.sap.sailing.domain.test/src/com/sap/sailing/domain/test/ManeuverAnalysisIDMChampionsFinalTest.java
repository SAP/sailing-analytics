package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.TrackedLegImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ManeuverAnalysisIDMChampionsFinalTest extends AbstractManeuverDetectionTestCase {

    public ManeuverAnalysisIDMChampionsFinalTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"+new File("resources/event_20110929_Internatio-Champions_Cup_Final.mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///"+new File("resources/event_20110929_Internatio-Champions_Cup_Final.txt").getCanonicalPath()),
                /* liveUri */ null, /* storedUri */ storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        fixApproximateMarkPositionsForWindReadOut(getTrackedRace());
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, MillisecondsTimePoint.now(), new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(65))), new WindSourceImpl(WindSourceType.WEB));
    }
    
    /**
     * If a leg's type needs to be determined, some wind data is required to decide on upwind,
     * downwind or reaching leg. Wind information is queried by {@link TrackedLegImpl} based on
     * the marks' positions. Therefore, approximate mark positions are set here for all marks
     * of {@link #getTrackedRace()}'s courses for the time span starting at the epoch up to now.
     */
    public static void fixApproximateMarkPositionsForWindReadOut(DynamicTrackedRace race) {
        TimePoint epoch = new MillisecondsTimePoint(0l);
        TimePoint now = MillisecondsTimePoint.now();
        Map<String, Position> markPositions = new HashMap<String, Position>();
        markPositions.put("G2 Start-Finish - 1", new DegreePosition(53.96744, 10.89441));
        markPositions.put("G2 Start-Finish - 2", new DegreePosition(53.96798, 10.89401));
        markPositions.put("G2 Mark4 - 2", new DegreePosition(53.96689, 10.89375));
        markPositions.put("G2 Mark4 - 1", new DegreePosition(53.96718, 10.89339));
        markPositions.put("G2 Mark1", new DegreePosition(53.96506, 10.88896));
        for (Waypoint w : race.getRace().getCourse().getWaypoints()) {
            for (Mark mark : w.getMarks()) {
                race.getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()), epoch));
                race.getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()), now));
            }
        }
    }

    @Override
    protected String getExpectedEventName() {
        return "Internationale Deutche Meisterschaft";
    }

    /**
     * Tests the 505 Race 2 for competitor "Findel" at a time where the maneuver detection test is likely to fail
     */
    @Test
    public void testPenaltyCirclePolgarKoySeelig4thLeg() throws ParseException, NoWindException {
        Competitor competitor = getCompetitorByName("Polgar\\+Koy\\+Seelig");
        assertNotNull(competitor);
        Date toDate = new Date(1317650038784l); // that's shortly after their penalty circle
        Date fromDate = new Date(toDate.getTime() - 450000l);
        final SimpleDateFormat dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
        TimePoint maneuverTime = new MillisecondsTimePoint(dateFormatter.parse("2011-10-03T15:52:31.000+0200"));
        Iterable<Maneuver> maneuvers = getTrackedRace().getManeuvers(competitor, new MillisecondsTimePoint(fromDate),
                new MillisecondsTimePoint(toDate), /* waitForLatest */ true);
        maneuversInvalid = new ArrayList<Maneuver>();
        Util.addAll(maneuvers, maneuversInvalid);
        assertManeuver(maneuvers, ManeuverType.PENALTY_CIRCLE, maneuverTime, PENALTYCIRCLE_TOLERANCE);
        List<ManeuverType> maneuverTypesFound = new ArrayList<ManeuverType>();
        maneuverTypesFound.add(ManeuverType.PENALTY_CIRCLE);
        assertAllManeuversOfTypesDetected(maneuverTypesFound, maneuversInvalid);
    }

}
