package com.sap.sailing.domain.test;


import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.DegreeBearingImpl;
import com.sap.sailing.domain.common.DegreePosition;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.TrackedLegImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;

public class ManeuverAnalysisIDMChampionsFinalTest extends AbstractManeuverDetectionTestCase {

    public ManeuverAnalysisIDMChampionsFinalTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Before
    public void setUp() throws URISyntaxException, IOException, InterruptedException {
        super.setUp();
        super.setUp("event_20110929_Internatio",
        /* raceId */"92073072-ed26-11e0-a523-406186cbf87c", new ReceiverType[] { ReceiverType.MARKPASSINGS,
                ReceiverType.RACECOURSE, ReceiverType.RAWPOSITIONS });
        fixApproximateMarkPositionsForWindReadOut(getTrackedRace());
        getTrackedRace().setWindSource(WindSource.WEB);
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, MillisecondsTimePoint.now(), new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(65))), WindSource.WEB);
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
        Map<String, Position> buoyPositions = new HashMap<String, Position>();
        buoyPositions.put("G2 Start-Finish (left)", new DegreePosition(53.96003300000019, 10.878697000000084));
        buoyPositions.put("G2 Start-Finish (right)", new DegreePosition(53.9674420000693, 10.894410000058738));
        buoyPositions.put("G2 Mark4 (right)", new DegreePosition(53.96002200000019, 10.878875000000063));
        buoyPositions.put("G2 Mark4 (left)", new DegreePosition(53.9599880000002, 10.878665000000069));
        buoyPositions.put("G2 Mark1", new DegreePosition(53.96355800000006, 10.885751999999806));
        for (Waypoint w : race.getRace().getCourse().getWaypoints()) {
            for (Buoy buoy : w.getBuoys()) {
                race.getOrCreateTrack(buoy).addGPSFix(new GPSFixImpl(buoyPositions.get(buoy.getName()), epoch));
                race.getOrCreateTrack(buoy).addGPSFix(new GPSFixImpl(buoyPositions.get(buoy.getName()), now));
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
        Date fromDate = new Date(toDate.getTime()-450000l);
        Date maneuverTime = new Date(1317649967712l);
        List<Maneuver> maneuvers = getTrackedRace().getManeuvers(competitor, new MillisecondsTimePoint(fromDate),
                new MillisecondsTimePoint(toDate));
        maneuversInvalid = new ArrayList<Maneuver>(maneuvers);
        assertManeuver(maneuvers, Maneuver.Type.PENALTY_CIRCLE,
                new MillisecondsTimePoint(maneuverTime), PENALTYCIRCLE_TOLERANCE);
        List<Maneuver.Type> maneuverTypesFound = new ArrayList<Maneuver.Type>();
        maneuverTypesFound.add(Maneuver.Type.PENALTY_CIRCLE);
        assertAllManeuversOfTypesDetected(maneuverTypesFound, maneuversInvalid);
    }
    
}
