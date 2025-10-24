package com.sap.sailing.domain.test;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.NoWindException;
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

public class RaceColumnCacheTest extends AbstractLeaderboardTest {
    @Test
    public void testInvalidationOfLeaderboardAfterSeriesChanged()
            throws NoWindException, InterruptedException, ExecutionException {
        TimePoint timePoint = TimePoint.now();
        TrackedRegattaRegistry trackedRegattaRegistry = mock(TrackedRegattaRegistry.class);
        Fleet fleet = new FleetImpl("TestFleet");
        Set<Fleet> fleets = new HashSet<>();
        fleets.add(fleet);
        Series series = new SeriesImpl("TestSeries", false, true, fleets, new HashSet<String>(), trackedRegattaRegistry);
        Set<Series> seriesSet = new HashSet<>();
        seriesSet.add(series);
        BoatClass boatClass = new BoatClassImpl("TestClass", true);
        String raceColumnName = "TestRace";
        RaceColumn raceColumn = series.addRaceColumn(raceColumnName, trackedRegattaRegistry);
        ScoringScheme scoringScheme = new LowPoint();
        Regatta regatta = new RegattaImpl("TestRegatta", new BoatClassImpl(BoatClassMasterdata.PIRATE),
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED,
                /* startDate */ null, /* endDate */ null, seriesSet, false, scoringScheme, UUID.randomUUID(),
                mock(CourseArea.class), null,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        Course course = new CourseImpl(raceColumnName, new HashSet<>());
        TrackedRace mockedTrackedRace = createSpyedTrackedRace(regatta, course, timePoint, boatClass);
        raceColumn.setTrackedRace(fleet, mockedTrackedRace);
        Set<String> raceColumnNames = new HashSet<>();
        raceColumnNames.add(raceColumnName);
        RegattaLeaderboard spyLeaderboard = Mockito.spy(createRegattaLeaderboard(regatta, new ThresholdBasedResultDiscardingRuleImpl(new int[0])));
        DomainFactory baseDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        spyLeaderboard.getLeaderboardDTO(/* TimePoint */ timePoint,
                /* namesOfRaceColumnsForWhichToLoadLegDetails */ raceColumnNames,
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ trackedRegattaRegistry,
                /* baseDomainFactory */ baseDomainFactory, /* fillTotalPointsUncorrected */ false);
        // Assert that computeDTO was invoked exactly once by the call to getLeaderboardDTO
        verify(spyLeaderboard, times(1)).computeDTO(Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        // Assert that computeDTO is not invoked a second time because the result is assumed to come from the cache:
        spyLeaderboard.getLeaderboardDTO(/* TimePoint */ timePoint,
                /* namesOfRaceColumnsForWhichToLoadLegDetails */ raceColumnNames,
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ trackedRegattaRegistry,
                /* baseDomainFactory */ baseDomainFactory, /* fillTotalPointsUncorrected */ false);
        verify(spyLeaderboard, times(1)).computeDTO(Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        // making a change that is expected to invalidate the cache:
        series.setCrossFleetMergedRanking(true);
        spyLeaderboard.getLeaderboardDTO(/* TimePoint */ timePoint,
                /* namesOfRaceColumnsForWhichToLoadLegDetails */ raceColumnNames,
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ trackedRegattaRegistry,
                /* baseDomainFactory */ baseDomainFactory, /* fillTotalPointsUncorrected */ false);
        // after the cache invalidation we expect another call to computeDTO when asking for the leaderboard DTO
        verify(spyLeaderboard, times(2)).computeDTO(Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
        // and now it's cached again
        spyLeaderboard.getLeaderboardDTO(/* TimePoint */ timePoint,
                /* namesOfRaceColumnsForWhichToLoadLegDetails */ raceColumnNames,
                /* addOverallDetails */ false, /* trackedRegattaRegistry */ trackedRegattaRegistry,
                /* baseDomainFactory */ baseDomainFactory, /* fillTotalPointsUncorrected */ false);
        verify(spyLeaderboard, times(2)).computeDTO(Mockito.any(), Mockito.any(), Mockito.anyBoolean(),
                Mockito.anyBoolean(), Mockito.any(), Mockito.any(), Mockito.anyBoolean());
    }

    protected RegattaLeaderboard createRegattaLeaderboard(Regatta mockedRegatta,
            ThresholdBasedResultDiscardingRule rule) {
        return new RegattaLeaderboardImpl(mockedRegatta, rule);
    }

    private TrackedRace createSpyedTrackedRace(Regatta regatta, Course course, TimePoint timePoint,
            BoatClass boatClass) {
        TrackedRegatta mockedTrackedRegatta = createMockedTrackedRegatta(regatta);
        RaceDefinition mockedRace = createMockedRace(course, boatClass);
        TrackedRace spyedTrackedRace = spy(new DynamicTrackedRaceImpl(mockedTrackedRegatta, mockedRace,
                new HashSet<Sideline>(), EmptyWindStore.INSTANCE, 5000, 20000, 20000,
                /* useMarkPassingCalculator */ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null,
                /* markPassingRaceFingerprintRegistry */ null));
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
        when(mockedRaceDefinition.getBoatOfCompetitor(Mockito.any())).thenReturn(boat);
        return mockedRaceDefinition;
    }

    private Map<Competitor, Boat> createCompetitorAndBoatMap(BoatClass boatClass) {
        Map<Competitor, Boat> competitors = new HashMap<>();
        Competitor c = new CompetitorImpl(UUID.randomUUID(), "TestCompetitor", "BYC", Color.BLACK, null, null,
                mock(DynamicTeam.class), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null,
                null);
        Boat b = new BoatImpl("Boot", "b", boatClass, null);
        competitors.put(c, b);
        return competitors;
    }

    private TrackedRegatta createMockedTrackedRegatta(Regatta regatta) {
        TrackedRegatta mockedTrackedRegatta = mock(DynamicTrackedRegatta.class);
        when(mockedTrackedRegatta.getRegatta()).thenReturn(regatta);
        return mockedTrackedRegatta;
    }
}
