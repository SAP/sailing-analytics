package com.sap.sailing.server.statistics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.KilometersPerHourSpeedWithBearingImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class StatisticsTest {
    private static final long START_OF_TRACKING = 100;
    private static final long START_OF_RACE = 200;
    private static final long END_OF_TRACKING = 400;
    private final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");
    private final Competitor comp = DomainFactory.INSTANCE.getOrCreateCompetitor("comp", "comp", "c", null, null, null, null,
            /* timeOnTimeFactor */ null,
            /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
    private final Boat boat = new BoatImpl("boat", "b", boatClass, "DE 12345");
    private final Mark mark1 = DomainFactory.INSTANCE.getOrCreateMark("mark1");
    private final Mark mark2 = DomainFactory.INSTANCE.getOrCreateMark("mark2");
    private final Mark mark3 = DomainFactory.INSTANCE.getOrCreateMark("mark3");
    private final ControlPoint gate = new ControlPointWithTwoMarksImpl(mark1, mark2, "gate", "gate");
    private final Waypoint waypoint1 = new WaypointImpl(gate);
    private final Waypoint waypoint2 = new WaypointImpl(mark3);
    private final Waypoint waypoint3 = new WaypointImpl(gate);
    private DynamicTrackedRegatta regatta;
    private DynamicTrackedRace trackedRace;

    @BeforeEach
    public void setUp() {
        regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass, false, 
                CompetitorRegistrationType.CLOSED, /* startDate */ null, /* endDate */null, null, null, "a", null,
                /* registrationLinkSecret */ UUID.randomUUID().toString()));

        final Course course = new CourseImpl("course",
                Arrays.asList(new Waypoint[] { waypoint1, waypoint2, waypoint3 }));
        Map<Competitor, Boat> competitors = new HashMap<>();
        competitors.put(comp, boat);
        RaceDefinition race = new RaceDefinitionImpl("race", course, boatClass, competitors);

        trackedRace = new DynamicTrackedRaceImpl(regatta, race, Collections.<Sideline>emptyList(),
                EmptyWindStore.INSTANCE, 0, 0, 0, /* useMarkPassingCalculator */ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
        trackedRace.setStartOfTrackingReceived(new MillisecondsTimePoint(START_OF_TRACKING));
        trackedRace.setEndOfTrackingReceived(new MillisecondsTimePoint(END_OF_TRACKING));
        trackedRace.setStartTimeReceived(new MillisecondsTimePoint(START_OF_RACE));

        regatta.addTrackedRace(trackedRace, Optional.empty());
    }

    private TrackedRaceStatisticsCacheImpl getStatisticsCacheWithRegattaAdded() throws Exception {
        TrackedRaceStatisticsCacheImpl trackedRaceStatisticsCache = new TrackedRaceStatisticsCacheImpl();
        trackedRaceStatisticsCache.regattaAdded(regatta);
        return trackedRaceStatisticsCache;
    }

    @Test
    public void testTrackedRaceStatisticsCacheWithoutFixes() throws Exception {
        TrackedRaceStatisticsCacheImpl trackedRaceStatisticsCache = getStatisticsCacheWithRegattaAdded();

        TrackedRaceStatistics statisticsForRace = trackedRaceStatisticsCache.getStatisticsWaitingForLatest(trackedRace);

        assertEquals(0, statisticsForRace.getNumberOfWindFixes());
        assertEquals(0, statisticsForRace.getNumberOfGPSFixes());
    }

    @Test
    public void testTrackedRaceStatisticsCacheWithFixes() throws Exception {
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295970, 8.638958), new MillisecondsTimePoint(START_OF_RACE),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295911, 8.638971),
                        new MillisecondsTimePoint(START_OF_RACE + 10),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295866, 8.638986),
                        new MillisecondsTimePoint(START_OF_RACE + 20),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295822, 8.639010),
                        new MillisecondsTimePoint(START_OF_RACE + 30),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295785, 8.639057),
                        new MillisecondsTimePoint(START_OF_RACE + 40),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295768, 8.639108),
                        new MillisecondsTimePoint(START_OF_RACE + 50),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295761, 8.639180),
                        new MillisecondsTimePoint(START_OF_RACE + 60),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295765, 8.639278),
                        new MillisecondsTimePoint(START_OF_RACE + 70),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295785, 8.639353),
                        new MillisecondsTimePoint(START_OF_RACE + 80),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295817, 8.639406),
                        new MillisecondsTimePoint(START_OF_RACE + 90),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));
        trackedRace.recordFix(comp,
                new GPSFixMovingImpl(new DegreePosition(49.295861, 8.639449),
                        new MillisecondsTimePoint(START_OF_RACE + 100),
                        new KilometersPerHourSpeedWithBearingImpl(1, new DegreeBearingImpl(100)), /* optionalTrueHeading */ null));

        List<MarkPassing> markPassings = new ArrayList<>();
        markPassings.add(new MarkPassingImpl(new MillisecondsTimePoint(START_OF_RACE), waypoint1, comp));
        markPassings.add(new MarkPassingImpl(new MillisecondsTimePoint(START_OF_RACE + 50), waypoint2, comp));
        markPassings.add(new MarkPassingImpl(new MillisecondsTimePoint(START_OF_RACE + 100), waypoint3, comp));

        trackedRace.updateMarkPassings(comp, markPassings);

        TrackedRaceStatisticsCacheImpl trackedRaceStatisticsCache = getStatisticsCacheWithRegattaAdded();

        TrackedRaceStatistics statisticsForRace = trackedRaceStatisticsCache.getStatisticsWaitingForLatest(trackedRace);

        assertEquals(11, statisticsForRace.getNumberOfGPSFixes());
        double distanceInMeters = statisticsForRace.getDistanceTraveled().getMeters();
        assertTrue(distanceInMeters > 53 && distanceInMeters < 57);
    }
}
