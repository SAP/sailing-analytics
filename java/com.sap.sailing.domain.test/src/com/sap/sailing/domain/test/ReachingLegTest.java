package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.GateImpl;
import com.sap.sailing.domain.base.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.WindImpl;

public class ReachingLegTest extends TrackBasedTest {
    private List<Competitor> competitors;
    private CompetitorImpl plattner;
    private CompetitorImpl hunger;
    private MillisecondsTimePoint start;
    private Competitor schomaeker;

    /**
     * Creates the race and two competitors ({@link #plattner} and {@link #hunger}) and sets the start line passing for both of them
     * to {@link #start}.
     */
    @Before
    public void setUp() {
        competitors = new ArrayList<Competitor>();
        hunger = createCompetitor("Wolfgang Hunger");
        competitors.add(hunger);
        plattner = createCompetitor("Dr. Hasso Plattner");
        competitors.add(plattner);
        schomaeker = createCompetitor("Meike Schom�ker");
        competitors.add(schomaeker);
        start = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime());
        setTrackedRace(createTestTrackedRace("Kieler Woche", "505 Race 2", "505", competitors, start));
        List<MarkPassing> hungersMarkPassings = createMarkPassings(hunger, start);
        getTrackedRace().updateMarkPassings(hunger, hungersMarkPassings);
        List<MarkPassing> plattnersMarkPassings = createMarkPassings(plattner, start);
        getTrackedRace().updateMarkPassings(plattner, plattnersMarkPassings);
    }
    
    protected DynamicTrackedRace createTestTrackedRace(String regattaName, String raceName, String boatClassName,
            Iterable<Competitor> competitors, TimePoint timePointForFixes) {
        BoatClassImpl boatClass = new BoatClassImpl(boatClassName, /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(null, regattaName, boatClass, /* trackedRegattaRegistry */ null,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), "123");
        TrackedRegatta trackedRegatta = new TrackedRegattaImpl(regatta);
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        // create a two-lap upwind/downwind course:
        MarkImpl left = new MarkImpl("Left lee gate buoy");
        MarkImpl right = new MarkImpl("Right lee gate buoy");
        ControlPoint leeGate = new GateImpl(left, right, "Lee Gate");
        Mark windwardMark = new MarkImpl("Windward mark");
        Mark offsetMark = new MarkImpl("Offset mark");
        waypoints.add(new WaypointImpl(leeGate));
        waypoints.add(new WaypointImpl(windwardMark));
        waypoints.add(new WaypointImpl(offsetMark));
        waypoints.add(new WaypointImpl(leeGate));
        Course course = new CourseImpl(raceName, waypoints);
        RaceDefinition race = new RaceDefinitionImpl(raceName, course, boatClass, competitors);
        DynamicTrackedRace trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race, EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 0,
                /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 30000,
                /* delay for wind estimation cache invalidation */ 0);
        // in this simplified artificial course, the top mark is exactly north of the right leeward gate, the offset
        // mark is slightly west of the top mark; wind from the north makes the leg from top to offset a reaching leg
        Position leftPosition = new DegreePosition(0, -0.00001);
        Position rightPosition = new DegreePosition(0, 0.00001);
        Position topPosition = new DegreePosition(1, 0);
        Position offsetPosition = new DegreePosition(1, -0.0001);
        TimePoint afterTheRace = new MillisecondsTimePoint(timePointForFixes.asMillis() + 36000000); // 10h after the fix time
        trackedRace.getOrCreateTrack(left).addGPSFix(new GPSFixImpl(leftPosition, new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(right).addGPSFix(new GPSFixImpl(rightPosition, new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(offsetMark).addGPSFix(new GPSFixImpl(offsetPosition, new MillisecondsTimePoint(0)));
        trackedRace.getOrCreateTrack(left).addGPSFix(new GPSFixImpl(leftPosition, afterTheRace));
        trackedRace.getOrCreateTrack(right).addGPSFix(new GPSFixImpl(rightPosition, afterTheRace));
        trackedRace.getOrCreateTrack(windwardMark).addGPSFix(new GPSFixImpl(topPosition, afterTheRace));
        trackedRace.getOrCreateTrack(offsetMark).addGPSFix(new GPSFixImpl(offsetPosition, afterTheRace));
        trackedRace.recordWind(new WindImpl(topPosition, timePointForFixes, new KnotSpeedWithBearingImpl(
                /* speedInKnots */14.7, new DegreeBearingImpl(180))), new WindSourceImpl(WindSourceType.WEB));
        return trackedRace;
    }

    @Test
    public void testSecondLegIsReaching() throws NoWindException {
        Iterator<TrackedLeg> legIter = getTrackedRace().getTrackedLegs().iterator();
        assertEquals(LegType.UPWIND, legIter.next().getLegType(start));
        assertEquals(LegType.REACHING, legIter.next().getLegType(start));
        assertEquals(LegType.DOWNWIND, legIter.next().getLegType(start));
    }
    
    @Test
    public void testHungerInReachingPlattnerInUpwind() {
        // give Hunger a mark passing for the windward mark, putting him into the reaching leg
        final MillisecondsTimePoint whenHungerFinishedUpwind = new MillisecondsTimePoint(start.asMillis()+600000);
        getTrackedRace().updateMarkPassings(hunger, createMarkPassings(hunger, start, whenHungerFinishedUpwind));
        assertEquals(
                getTrackedRace().getRace().getCourse().getLegs().get(1),
                getTrackedRace().getTrackedLeg(hunger,
                        new MillisecondsTimePoint(whenHungerFinishedUpwind.asMillis() + 10000)).getLeg());
        assertEquals(
                getTrackedRace().getRace().getCourse().getLegs().get(0),
                getTrackedRace().getTrackedLeg(plattner,
                        new MillisecondsTimePoint(whenHungerFinishedUpwind.asMillis() + 10000)).getLeg());
    }

    @Test
    public void testHungerAndPlattnerInReaching() {
        // give Hunger and Plattner a mark passing for the windward mark, putting both of them into the reaching leg
        final MillisecondsTimePoint whenBothFinishedUpwind = new MillisecondsTimePoint(start.asMillis()+600000);
        getTrackedRace().updateMarkPassings(hunger, createMarkPassings(hunger, start, whenBothFinishedUpwind));
        getTrackedRace().updateMarkPassings(plattner, createMarkPassings(plattner, start, whenBothFinishedUpwind));
        assertEquals(
                getTrackedRace().getRace().getCourse().getLegs().get(1),
                getTrackedRace().getTrackedLeg(hunger,
                        new MillisecondsTimePoint(whenBothFinishedUpwind.asMillis() + 10000)).getLeg());
        assertEquals(
                getTrackedRace().getRace().getCourse().getLegs().get(1),
                getTrackedRace().getTrackedLeg(plattner,
                        new MillisecondsTimePoint(whenBothFinishedUpwind.asMillis() + 10000)).getLeg());
    }

    @Test
    public void testDistanceWithHungerAndPlattnerInReaching() throws NoWindException {
        // give Hunger and Plattner a mark passing for the windward mark, putting both of them into the reaching leg
        final MillisecondsTimePoint whenBothFinishedUpwind = new MillisecondsTimePoint(start.asMillis()+600000);
        final MillisecondsTimePoint timePointInReaching = new MillisecondsTimePoint(whenBothFinishedUpwind.asMillis()+10000);
        Position windwardMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(0).getTo(), timePointInReaching);
        Position offsetMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(1).getTo(), timePointInReaching);
        Distance distanceOfReachingLeg = windwardMarkPos.getDistance(offsetMarkPos);
        assertTrue(distanceOfReachingLeg.getMeters() > 0);
        getTrackedRace().updateMarkPassings(hunger, createMarkPassings(hunger, start, whenBothFinishedUpwind));
        getTrackedRace().updateMarkPassings(plattner, createMarkPassings(plattner, start, whenBothFinishedUpwind));
        getTrackedRace().recordFix(
                hunger,
                new GPSFixMovingImpl(offsetMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(windwardMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(windwardMarkPos).getMeters(), 0.00001);
        Distance hungersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(hunger, timePointInReaching);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(plattner, timePointInReaching);
        assertEquals(0., hungersDistanceToLeader.getMeters(), 0.00001);
        assertEquals(distanceOfReachingLeg.getMeters(), plattnersDistanceToLeader.getMeters(), 0.001);
    }

    @Test
    public void testDistanceWithHungerInReachingAndPlattnerInUpwind() throws NoWindException {
        // give Hunger and Plattner a mark passing for the windward mark, putting both of them into the reaching leg
        final MillisecondsTimePoint whenBothFinishedUpwind = new MillisecondsTimePoint(start.asMillis()+600000);
        final MillisecondsTimePoint timePointInReaching = new MillisecondsTimePoint(whenBothFinishedUpwind.asMillis()+10000);
        Position windwardMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(0).getTo(), timePointInReaching);
        Position offsetMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(1).getTo(), timePointInReaching);
        Distance distanceOfReachingLeg = windwardMarkPos.getDistance(offsetMarkPos);
        assertTrue(distanceOfReachingLeg.getMeters() > 0);
        getTrackedRace().updateMarkPassings(hunger, createMarkPassings(hunger, start, whenBothFinishedUpwind));
        getTrackedRace().recordFix(
                hunger,
                new GPSFixMovingImpl(offsetMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(windwardMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(windwardMarkPos).getMeters(), 0.00001);
        Distance hungersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(hunger, timePointInReaching);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(plattner, timePointInReaching);
        assertEquals(0., hungersDistanceToLeader.getMeters(), 0.00001);
        assertEquals(distanceOfReachingLeg.getMeters(), plattnersDistanceToLeader.getMeters(), 0.001);
    }

    @Test
    public void testDistanceWithHungerInDownwindAndPlattnerInReaching() throws NoWindException {
        // give Hunger and Plattner a mark passing for the windward mark, putting both of them into the reaching leg
        final MillisecondsTimePoint whenBothFinishedUpwind = new MillisecondsTimePoint(start.asMillis()+600000);
        final MillisecondsTimePoint timePointInReaching = new MillisecondsTimePoint(whenBothFinishedUpwind.asMillis()+10000);
        Position windwardMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(0).getTo(), timePointInReaching);
        Position offsetMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(1).getTo(), timePointInReaching);
        Distance distanceOfReachingLeg = windwardMarkPos.getDistance(offsetMarkPos);
        assertTrue(distanceOfReachingLeg.getMeters() > 0);
        getTrackedRace().updateMarkPassings(hunger, createMarkPassings(hunger, start, whenBothFinishedUpwind));
        getTrackedRace().recordFix(
                hunger,
                new GPSFixMovingImpl(offsetMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(windwardMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(windwardMarkPos).getMeters(), 0.00001);
        Distance hungersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(hunger, timePointInReaching);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(plattner, timePointInReaching);
        assertEquals(0., hungersDistanceToLeader.getMeters(), 0.00001);
        assertEquals(distanceOfReachingLeg.getMeters(), plattnersDistanceToLeader.getMeters(), 0.001);
    }

    @Test
    public void testDistanceWithHungerInDownwindAndPlattnerInUpwindWithReachingInBetween() throws NoWindException {
        // give Hunger a mark passing for the windward mark, consume Plattner is stuck at the starting line, putting
        // the empty reaching leg between the two; see if the reaching leg's non-windward distance is counted, and if
        // Plattner's distance is accumulated correctly
        final MillisecondsTimePoint whenHungerFinishedUpwind = new MillisecondsTimePoint(start.asMillis()+600000);
        final MillisecondsTimePoint whenHungerFinishedReaching = new MillisecondsTimePoint(whenHungerFinishedUpwind.asMillis()+10000);
        final MillisecondsTimePoint timePointToConsider = new MillisecondsTimePoint(whenHungerFinishedReaching.asMillis()+10000);
        Position leewardPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(0).getFrom(), timePointToConsider);
        Position windwardMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(0).getTo(), timePointToConsider);
        Position offsetMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(1).getTo(), timePointToConsider);
        Distance distanceOfReachingLeg = windwardMarkPos.getDistance(offsetMarkPos);
        Distance windwardDistanceOfUpwindLeg = leewardPos.getDistance(windwardMarkPos);
        assertTrue(distanceOfReachingLeg.getMeters() > 0);
        getTrackedRace().updateMarkPassings(hunger, createMarkPassings(hunger, start, whenHungerFinishedUpwind, whenHungerFinishedReaching));
        getTrackedRace().recordFix(
                hunger,
                new GPSFixMovingImpl(offsetMarkPos, timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(leewardPos, timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(0))));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(leewardPos).getMeters(), 0.00001);
        Distance hungersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(hunger, timePointToConsider);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(plattner, timePointToConsider);
        assertEquals(0., hungersDistanceToLeader.getMeters(), 0.00001);
        assertEquals(distanceOfReachingLeg.getMeters()+windwardDistanceOfUpwindLeg.getMeters(), plattnersDistanceToLeader.getMeters(), 0.001);
    }

    @Test
    public void testWindwardDistanceToOverallLeaderWithHungerFirstAtWindwardMarkButSecondAfterReachingLeg() throws NoWindException {
        // Hunger finished first upwind before Schomaeker; Schomaeker finished reaching before Hunger, leading now;
        // Schoemaeker made it half through the downwind while Hunger got stuck at the offset mark
        final MillisecondsTimePoint whenHungerFinishedUpwind = new MillisecondsTimePoint(start.asMillis()+600000);
        final MillisecondsTimePoint whenSchomaekerFinishedUpwind = new MillisecondsTimePoint(whenHungerFinishedUpwind.asMillis()+1000);
        final MillisecondsTimePoint whenSchomaekerFinishedReaching = new MillisecondsTimePoint(whenSchomaekerFinishedUpwind.asMillis()+10000);
        final MillisecondsTimePoint whenHungerFinishedReaching = new MillisecondsTimePoint(whenSchomaekerFinishedReaching.asMillis()+1000);
        final MillisecondsTimePoint timePointToConsider = new MillisecondsTimePoint(whenHungerFinishedReaching.asMillis()+10000);
        
        Position leewardPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(0).getFrom(), timePointToConsider);
        Position windwardMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(0).getTo(), timePointToConsider);
        Position offsetMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(1).getTo(), timePointToConsider);
        Distance distanceOfReachingLeg = windwardMarkPos.getDistance(offsetMarkPos);
        Distance windwardDistanceOfUpwindLeg = leewardPos.getDistance(windwardMarkPos);
        assertTrue(distanceOfReachingLeg.getMeters() > 0);
        getTrackedRace().updateMarkPassings(hunger, createMarkPassings(hunger, start, whenHungerFinishedUpwind, whenHungerFinishedReaching));
        getTrackedRace().updateMarkPassings(schomaeker, createMarkPassings(schomaeker, start, whenSchomaekerFinishedUpwind, whenSchomaekerFinishedReaching));
        getTrackedRace().recordFix(
                hunger,
                new GPSFixMovingImpl(offsetMarkPos, timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        getTrackedRace().recordFix(
                schomaeker,
                new GPSFixMovingImpl(offsetMarkPos.translateGreatCircle(
                        offsetMarkPos.getBearingGreatCircle(leewardPos), offsetMarkPos.getDistance(leewardPos)
                                .scale(0.5)), timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(180))));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(leewardPos, timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(0))));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(leewardPos).getMeters(), 0.00001);
        Distance schomaekersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(schomaeker, timePointToConsider);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(plattner, timePointToConsider);
        assertEquals(0., schomaekersDistanceToLeader.getMeters(), 0.00001);
        // distance to leading Schomaeker expected to be the entire upwind distance plus the offset distance plus half the downwind
        assertEquals(distanceOfReachingLeg.getMeters()+windwardDistanceOfUpwindLeg.getMeters()*1.5, plattnersDistanceToLeader.getMeters(), 0.001);
    }

    @Test
    public void testWindwardDistanceOnReachingLegProjectsOntoLegDirection() throws NoWindException {
        final MillisecondsTimePoint whenBothFinishedUpwind = new MillisecondsTimePoint(start.asMillis()+600000);
        final MillisecondsTimePoint timePointToConsider = new MillisecondsTimePoint(whenBothFinishedUpwind.asMillis()+10000);
        
        Position windwardMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(0).getTo(), timePointToConsider);
        Position offsetMarkPos = getTrackedRace().getApproximatePosition(getTrackedRace().getRace().getCourse().getLegs().get(1).getTo(), timePointToConsider);
        Distance distanceOfReachingLeg = windwardMarkPos.getDistance(offsetMarkPos);
        assertTrue(distanceOfReachingLeg.getMeters() > 0);
        getTrackedRace().updateMarkPassings(hunger, createMarkPassings(hunger, start, whenBothFinishedUpwind));
        getTrackedRace().updateMarkPassings(plattner, createMarkPassings(plattner, start, whenBothFinishedUpwind));
        Bearing reachingLegBearing = windwardMarkPos.getBearingGreatCircle(offsetMarkPos);
        // Hunger turns left after the windward mark
        getTrackedRace().recordFix(
                hunger,
                new GPSFixMovingImpl(windwardMarkPos.translateGreatCircle(new DegreeBearingImpl(reachingLegBearing.getDegrees()-45),
                        distanceOfReachingLeg.scale(1./Math.sqrt(2.))), timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        // Plattner turns right after the windward mark; both travel the same distance projected along the leg
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(windwardMarkPos.translateGreatCircle(new DegreeBearingImpl(reachingLegBearing.getDegrees()+45),
                        distanceOfReachingLeg.scale(1./Math.sqrt(2.))), timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270))));
        // with 90deg separating them, traveling 1/sqrt(2) the distance of the leg should put them the distance of the leg apart geometrically
        assertEquals(
                distanceOfReachingLeg.getMeters(),
                getTrackedRace()
                        .getTrack(hunger)
                        .getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(
                                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointToConsider, /* extrapolate */
                                        false)).getMeters(), 0.00001);
        // however, projected onto the leg their distance should be 0
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToOverallLeader(plattner, timePointToConsider);
        assertEquals(0., plattnersDistanceToLeader.getMeters(), 0.00001);
    }
}
