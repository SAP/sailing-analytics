package com.sap.sailing.domain.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.BuoyImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.GateImpl;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;

public abstract class TrackBasedTest {
    private DynamicTrackedRace trackedRace;
    
    protected DynamicTrackedRace getTrackedRace() {
        return trackedRace;
    }

    protected void setTrackedRace(DynamicTrackedRace trackedRace) {
        this.trackedRace = trackedRace;
    }

    public static CompetitorImpl createCompetitor(String competitorName) {
        return new CompetitorImpl(123, competitorName, new TeamImpl("STG", Collections.singleton(
                new PersonImpl(competitorName, new NationalityImpl("GER"),
                /* dateOfBirth */null, "This is famous " + competitorName)), new PersonImpl("Rigo van Maas",
                new NationalityImpl("NED"),
                /* dateOfBirth */null, "This is Rigo, the coach")), new BoatImpl(competitorName + "'s boat",
                new BoatClassImpl("505", /* typicallyStartsUpwind */true), null));
    }
    
    /**
     * For {@link #trackedRace}'s race course, creates a list of mark passings using the time points specified, in order
     * for the waypoints.
     */
    protected List<MarkPassing> createMarkPassings(Competitor competitor, TimePoint... timePoints) {
        List<MarkPassing> result = new ArrayList<MarkPassing>();
        Iterator<Waypoint> wpIter = getTrackedRace().getRace().getCourse().getWaypoints().iterator();
        for (TimePoint timePoint : timePoints) {
            result.add(new MarkPassingImpl(timePoint, wpIter.next(), competitor));
        }
        return result;
    }

    /**
     * Creates a simple two-lap upwind-downwind course for a race/event with given name and boat class name with the
     * competitors specified. The marks are laid out such that the upwind/downwind leg detection should be alright.
     * Wind is coming from the north. A single wind fix with bearing 180deg (from=0deg) is added to the {@link WindSourceType#WEB}
     * wind track using <code>timePointForFixes</code> as time point.
     * 
     * @param timePointForFixes
     *            a wind fix will be inserted into the {@link WindSourceType#WEB} wind track which is aligned with the
     *            course layout; the value of this parameter will be used as the time stamp for this wind fix. Using a
     *            time that is reasonably within the race time (mark passing times or whatever is collected for the
     *            tracked race returned by this method) is important because otherwise confidences of wind readouts may
     *            be ridiculously low.
     */
    protected DynamicTrackedRace createTestTrackedRace(String regattaName, String raceName, String boatClassName,
            Iterable<Competitor> competitors, TimePoint timePointForFixes) {
        BoatClassImpl boatClass = new BoatClassImpl(boatClassName, /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(regattaName, boatClass);
        TrackedRegatta trackedRegatta = new TrackedRegattaImpl(regatta);
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        // create a two-lap upwind/downwind course:
        BuoyImpl left = new BuoyImpl("Left lee gate buoy");
        BuoyImpl right = new BuoyImpl("Right lee gate buoy");
        ControlPoint leeGate = new GateImpl(left, right, "Lee Gate");
        Buoy windwardMark = new BuoyImpl("Windward mark");
        waypoints.add(new WaypointImpl(leeGate));
        waypoints.add(new WaypointImpl(windwardMark));
        waypoints.add(new WaypointImpl(leeGate));
        waypoints.add(new WaypointImpl(windwardMark));
        waypoints.add(new WaypointImpl(leeGate));
        Course course = new CourseImpl(raceName, waypoints);
        RaceDefinition race = new RaceDefinitionImpl(raceName, course, boatClass, competitors);
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race, EmptyWindStore.INSTANCE,
                /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 30000,
                /* delay for wind estimation cache invalidation */ 0);
        // in this simplified artificial course, the top mark is exactly north of the right leeward gate
        DegreePosition topPosition = new DegreePosition(54.48, 10.24);
        TimePoint afterTheRace = new MillisecondsTimePoint(timePointForFixes.asMillis() + 36000000); // 10h after the fix time
        trackedRace.getOrCreateTrack(left).addGPSFix(new GPSFixImpl(new DegreePosition(54.4680424, 10.234451), new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(right).addGPSFix(new GPSFixImpl(new DegreePosition(54.4680424, 10.24), new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(left).addGPSFix(new GPSFixImpl(new DegreePosition(54.4680424, 10.234451), afterTheRace));
        trackedRace.getOrCreateTrack(right).addGPSFix(new GPSFixImpl(new DegreePosition(54.4680424, 10.24), afterTheRace));
        trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, afterTheRace));
        trackedRace.recordWind(new WindImpl(topPosition, timePointForFixes, new KnotSpeedWithBearingImpl(
                /* speedInKnots */14.7, new DegreeBearingImpl(180))), new WindSourceImpl(WindSourceType.WEB));
        return trackedRace;
    }

}
