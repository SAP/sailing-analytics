package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ReachingLegTest extends TrackBasedTest {
    private Map<Competitor,Boat> competitorsAndBoats;
    private CompetitorWithBoat plattner;
    private CompetitorWithBoat hunger;
    private MillisecondsTimePoint start;
    private CompetitorWithBoat schomaeker;

    /**
     * Creates the race and two competitors ({@link #plattner} and {@link #hunger}) and sets the start line passing for both of them
     * to {@link #start}.
     */
    @BeforeEach
    public void setUp() {
        competitorsAndBoats = new LinkedHashMap<>();
        hunger = createCompetitorWithBoat("Wolfgang Hunger"); 
        competitorsAndBoats.put(hunger, hunger.getBoat());
        plattner = createCompetitorWithBoat("Dr. Hasso Plattner");
        competitorsAndBoats.put(plattner, plattner.getBoat());
        schomaeker = createCompetitorWithBoat("Meike Schom�ker");
        competitorsAndBoats.put(schomaeker, schomaeker.getBoat());
        start = new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime());
        setTrackedRace(createTrackedRace("Kieler Woche", "505 Race 2", "505", competitorsAndBoats, start));
        List<MarkPassing> hungersMarkPassings = createMarkPassings(hunger, start);
        getTrackedRace().updateMarkPassings(hunger, hungersMarkPassings);
        List<MarkPassing> plattnersMarkPassings = createMarkPassings(plattner, start);
        getTrackedRace().updateMarkPassings(plattner, plattnersMarkPassings);
    }
    
    protected DynamicTrackedRaceImpl createTrackedRace(String regattaName, String raceName, String boatClassName,
            Map<Competitor,Boat> competitorsAndBoats, TimePoint timePointForFixes) {
        BoatClassImpl boatClass = new BoatClassImpl(boatClassName, /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(EmptyRaceLogStore.INSTANCE, EmptyRegattaLogStore.INSTANCE,
                RegattaImpl.getDefaultName(regattaName, boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /*startDate*/ null, /*endDate*/ null, /* trackedRegattaRegistry */ null,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), "123", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        regatta.setControlTrackingFromStartAndFinishTimes(true);
        TrackedRegatta trackedRegatta = new DynamicTrackedRegattaImpl(regatta);
        List<Waypoint> waypoints = new ArrayList<Waypoint>();
        // create a two-lap upwind/downwind course:
        MarkImpl left = new MarkImpl("Left lee gate buoy");
        MarkImpl right = new MarkImpl("Right lee gate buoy");
        ControlPoint leeGate = new ControlPointWithTwoMarksImpl(left, right, "Lee Gate", "Lee Gate");
        Mark windwardMark = new MarkImpl("Windward mark");
        Mark offsetMark = new MarkImpl("Offset mark");
        waypoints.add(new WaypointImpl(leeGate));
        waypoints.add(new WaypointImpl(windwardMark));
        waypoints.add(new WaypointImpl(offsetMark));
        waypoints.add(new WaypointImpl(leeGate));
        Course course = new CourseImpl(raceName, waypoints);
        RaceDefinition race = new RaceDefinitionImpl(raceName, course, boatClass, competitorsAndBoats);
        DynamicTrackedRaceImpl trackedRace = new DynamicTrackedRaceImpl(trackedRegatta, race, Collections.<Sideline> emptyList(), EmptyWindStore.INSTANCE,
                /* delayToLiveInMillis */ 0,
                /* millisecondsOverWhichToAverageWind */ 30000, /* millisecondsOverWhichToAverageSpeed */ 30000,
                /* delay for wind estimation cache invalidation */ 0, /*useMarkPassingCalculator*/ false,
                OneDesignRankingMetric::new, mock(RaceLogAndTrackedRaceResolver.class), null, /* markPassingRaceFingerprintRegistry */ null);
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
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(windwardMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(windwardMarkPos).getMeters(), 0.00001);
        Distance hungersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(hunger, timePointInReaching, WindPositionMode.LEG_MIDDLE);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(plattner, timePointInReaching, WindPositionMode.LEG_MIDDLE);
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
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(windwardMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(windwardMarkPos).getMeters(), 0.00001);
        Distance hungersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(hunger, timePointInReaching, WindPositionMode.LEG_MIDDLE);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(plattner, timePointInReaching, WindPositionMode.LEG_MIDDLE);
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
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(windwardMarkPos, timePointInReaching, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointInReaching, /* extrapolate */false)
                        .getDistance(windwardMarkPos).getMeters(), 0.00001);
        Distance hungersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(hunger, timePointInReaching, WindPositionMode.LEG_MIDDLE);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(plattner, timePointInReaching, WindPositionMode.LEG_MIDDLE);
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
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(leewardPos, timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(0)), /* optionalTrueHeading */ null));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(leewardPos).getMeters(), 0.00001);
        Distance hungersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(hunger, timePointToConsider, WindPositionMode.LEG_MIDDLE);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(plattner, timePointToConsider, WindPositionMode.LEG_MIDDLE);
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
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        getTrackedRace().recordFix(
                schomaeker,
                new GPSFixMovingImpl(offsetMarkPos.translateGreatCircle(
                        offsetMarkPos.getBearingGreatCircle(leewardPos), offsetMarkPos.getDistance(leewardPos)
                                .scale(0.5)), timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(180)), /* optionalTrueHeading */ null));
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(leewardPos, timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(0)), /* optionalTrueHeading */ null));
        assertEquals(0.,
                getTrackedRace().getTrack(hunger).getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(offsetMarkPos).getMeters(), 0.00001);
        assertEquals(0.,
                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(leewardPos).getMeters(), 0.00001);
        Distance schomaekersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(schomaeker, timePointToConsider, WindPositionMode.LEG_MIDDLE);
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(plattner, timePointToConsider, WindPositionMode.LEG_MIDDLE);
        assertEquals(0., schomaekersDistanceToLeader.getMeters(), 0.00001);
        // distance to leading Schomaeker expected to be the entire upwind distance plus the offset distance plus half the downwind
        assertEquals(distanceOfReachingLeg.getMeters()+windwardDistanceOfUpwindLeg.getMeters()*1.5, plattnersDistanceToLeader.getMeters(), 0.01);
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
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        // Plattner turns right after the windward mark; both travel the same distance projected along the leg
        getTrackedRace().recordFix(
                plattner,
                new GPSFixMovingImpl(windwardMarkPos.translateGreatCircle(new DegreeBearingImpl(reachingLegBearing.getDegrees()+45),
                        distanceOfReachingLeg.scale(1./Math.sqrt(2.))), timePointToConsider, new KnotSpeedWithBearingImpl(12,
                        new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        // with 90deg separating them, traveling 1/sqrt(2) the distance of the leg should put them the distance of the leg apart geometrically
        assertEquals(
                distanceOfReachingLeg.getMeters(),
                getTrackedRace()
                        .getTrack(hunger)
                        .getEstimatedPosition(timePointToConsider, /* extrapolate */false)
                        .getDistance(
                                getTrackedRace().getTrack(plattner).getEstimatedPosition(timePointToConsider, /* extrapolate */
                                        false)).getMeters(), 0.01);
        // however, projected onto the leg their distance should be 0
        Distance plattnersDistanceToLeader = getTrackedRace().getWindwardDistanceToCompetitorFarthestAhead(plattner, timePointToConsider, WindPositionMode.LEG_MIDDLE);
        assertEquals(0., plattnersDistanceToLeader.getMeters(), 0.01);
    }
}
