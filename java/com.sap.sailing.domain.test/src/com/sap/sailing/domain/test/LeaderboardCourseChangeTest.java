package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LeaderboardCourseChangeTest {
    /**
     * See bug 2011
     * 
     * @throws NoWindException
     * @throws InterruptedException
     * @throws ExecutionException
     */
    @Test
    public void testLeaderboardDTOCreationForCourseChange() throws NoWindException, InterruptedException, ExecutionException {
        Date date = Calendar.getInstance().getTime();
        TimePoint timePoint = new MillisecondsTimePoint(date);
        TrackedRegattaRegistry trackedRegattaRegistry = mock(TrackedRegattaRegistry.class);
        Fleet fleet = new FleetImpl("TestFleet");
        Series series = createSeries(trackedRegattaRegistry, fleet);
        Set<Series> seriesSet = new HashSet<>();
        seriesSet.add(series);
        BoatClass boatClass = new BoatClassImpl("TestClass", true);
        String raceColumnName = "TestRace";
        RaceColumn raceColumn = series.addRaceColumn(raceColumnName, trackedRegattaRegistry);
        ScoringScheme scoringScheme = new LowPoint();
        Regatta mockedRegatta = new RegattaImpl(RegattaImpl.getDefaultName("TestRegatta", boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null, seriesSet, false, scoringScheme,
                UUID.randomUUID(), mock(CourseArea.class), OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        ControlPoint start = new MarkImpl("Start");
        ControlPoint m1 = new MarkImpl("M1");
        ControlPoint m2 = new MarkImpl("M2");
        ControlPoint finish = new MarkImpl("Finish");
        Set<Waypoint> waypoints = new HashSet<>();
        Waypoint startWaypoint = new WaypointImpl(start);
        waypoints.add(startWaypoint);
        Waypoint m1Waypoint1 = new WaypointImpl(m1);
        Waypoint m2Waypoint1 = new WaypointImpl(m2);
        Waypoint m1Waypoint2 = new WaypointImpl(m1);
        Waypoint m2Waypoint2 = new WaypointImpl(m2);
        Waypoint m1Waypoint3 = new WaypointImpl(m1);
        waypoints.add(m1Waypoint1);
        waypoints.add(m2Waypoint1);
        waypoints.add(m1Waypoint2);
        waypoints.add(m2Waypoint2);
        waypoints.add(m1Waypoint3);
        Waypoint finishWaypoint = new WaypointImpl(finish);
        waypoints.add(finishWaypoint);
        Course course = new CourseImpl(raceColumnName, waypoints);
        TrackedRace mockedTrackedRace = createSpyedTrackedRace(mockedRegatta, course, timePoint, boatClass);
        raceColumn.setTrackedRace(fleet, mockedTrackedRace);
        Set<String> raceColumnNames = new HashSet<>();
        raceColumnNames.add(raceColumnName);
        int[] ruleRaw = { 5, 3, 2 };
        ThresholdBasedResultDiscardingRule rule = new ThresholdBasedResultDiscardingRuleImpl(ruleRaw);
        Leaderboard leaderboard = createRegattaLeaderboard(mockedRegatta, rule);
        DomainFactory baseDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        LeaderboardDTO leaderboardDTO = leaderboard.getLeaderboardDTO(timePoint, raceColumnNames, false,
                trackedRegattaRegistry, baseDomainFactory, /* fillTotalPointsUncorrected */ false);
        assertEquals(6, leaderboardDTO.rows.values().iterator().next().fieldsByRaceColumnName.values().iterator()
                .next().legDetails.size());
        course.removeWaypoint(2);
        course.removeWaypoint(3);
        leaderboardDTO = leaderboard.getLeaderboardDTO(timePoint, raceColumnNames, false, trackedRegattaRegistry,
                baseDomainFactory, /* fillTotalPointsUncorrected */ false);
        assertEquals(4, leaderboardDTO.rows.values().iterator().next().fieldsByRaceColumnName.values().iterator()
                .next().legDetails.size());
    }

    protected RegattaLeaderboard createRegattaLeaderboard(Regatta mockedRegatta,
            ThresholdBasedResultDiscardingRule rule) {
        return new RegattaLeaderboardImpl(mockedRegatta, rule);
    }

    private TrackedRace createSpyedTrackedRace(Regatta regatta, Course course, TimePoint timePoint, BoatClass boatClass) {
        TrackedRegatta mockedTrackedRegatta = createMockedTrackedRegatta(regatta);
        RaceDefinition mockedRace = createMockedRace(course, boatClass);
        TrackedRace spyedTrackedRace = spy(new DynamicTrackedRaceImpl(mockedTrackedRegatta, mockedRace,
                new HashSet<Sideline>(), EmptyWindStore.INSTANCE, 5000, 20000, 20000,
                /* useMarkPassingCalculator */ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null));

        return spyedTrackedRace;
    }

    private RaceDefinition createMockedRace(Course course, BoatClass boatClass) {
        RaceDefinition mockedRaceDefinition = mock(RaceDefinition.class);
        when(mockedRaceDefinition.getCourse()).thenReturn(course);
        when(mockedRaceDefinition.getBoatClass()).thenReturn(new BoatClassImpl("TestClass", true));
        when(mockedRaceDefinition.getName()).thenReturn("TestRace");
        Map<Competitor, Boat> competitorAndBoatMap = createCompetitorAndBoatMap(boatClass);
        when(mockedRaceDefinition.getCompetitors()).thenReturn(competitorAndBoatMap.keySet());
        when(mockedRaceDefinition.getCompetitorsAndTheirBoats()).thenReturn(competitorAndBoatMap);
        Boat boat = competitorAndBoatMap.get(competitorAndBoatMap.keySet().iterator().next());
        when(mockedRaceDefinition.getBoatOfCompetitor(any())).thenReturn(boat);
        return mockedRaceDefinition;
    }

    private Map<Competitor, Boat> createCompetitorAndBoatMap(BoatClass boatClass) {
        Map<Competitor, Boat> competitors = new HashMap<>();
        Competitor c = new CompetitorImpl(UUID.randomUUID(), "TestCompetitor", "BYC", Color.BLACK, null, null,
                mock(DynamicTeam.class),
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        Boat b = new BoatImpl("Boot", "b", boatClass, null);
        competitors.put(c, b);
        return competitors;
    }

    private TrackedRegatta createMockedTrackedRegatta(Regatta regatta) {
        TrackedRegatta mockedTrackedRegatta = mock(DynamicTrackedRegatta.class);
        when(mockedTrackedRegatta.getRegatta()).thenReturn(regatta);
        return mockedTrackedRegatta;
    }

    private Series createSeries(TrackedRegattaRegistry trackedRegattaRegistry, Fleet fleet) {
        Set<Fleet> fleets = new HashSet<>();
        fleets.add(fleet);

        Series series = new SeriesImpl("TestSeries", false, true, fleets, new HashSet<String>(), trackedRegattaRegistry);
        return series;
    }

}
