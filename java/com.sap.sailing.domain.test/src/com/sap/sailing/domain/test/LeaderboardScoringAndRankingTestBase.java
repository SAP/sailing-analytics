package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndZeroRanks;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.impl.MarkPassingImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

public class LeaderboardScoringAndRankingTestBase extends AbstractLeaderboardTest {
    protected ArrayList<Series> series;

    protected RegattaLeaderboard createLeaderboard(Regatta regatta, int[] discardingThresholds) {
        ThresholdBasedResultDiscardingRuleImpl discardingRules = new ThresholdBasedResultDiscardingRuleImpl(discardingThresholds);
        return new RegattaLeaderboardImpl(regatta, discardingRules);
    }

    protected void checkScoresAfterSomeRaces(Leaderboard leaderboard, List<RaceColumn> raceColumnsToConsider,
            double[][] scoresAfterNRaces, TimePoint timePoint, Competitor[] competitors) throws NoWindException {
        for (int competitorIndex=0; competitorIndex<scoresAfterNRaces.length; competitorIndex++) {
            final Set<RaceColumn> discardedRaceColumns = leaderboard.getResultDiscardingRule()
                    .getDiscardedRaceColumns(competitors[competitorIndex], leaderboard, raceColumnsToConsider, timePoint, leaderboard.getScoringScheme());
            for (int raceColumnIndex=0; raceColumnIndex<raceColumnsToConsider.size(); raceColumnIndex++) {
                assertEquals(scoresAfterNRaces[competitorIndex][raceColumnIndex],
                        leaderboard.getNetPoints(competitors[competitorIndex], raceColumnsToConsider.get(raceColumnIndex),
                                timePoint, discardedRaceColumns), 0.00000001);
            }
        }
    }

    protected TimePoint createAndAttachTrackedRaces(Series theSeries, String fleetName, boolean withScores, Competitor[]... competitorLists) {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis()+1000);
        Iterator<? extends RaceColumn> columnIter = theSeries.getRaceColumns().iterator();
        for (Competitor[] competitorList : competitorLists) {
            RaceColumn raceColumn = columnIter.next();
            final TrackedRace trackedRace;
            if (withScores) {
                trackedRace = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(competitorList));
            } else {
                trackedRace = new MockedTrackedRaceWithStartTimeAndZeroRanks(now, Arrays.asList(competitorList));
            }
            raceColumn.setTrackedRace(raceColumn.getFleetByName(fleetName), trackedRace);
        }
        return later;
    }

    protected void createAndAttachTrackedRacesWithStartTimeAndLastMarkPassingTimes(
            Series theSeries, String fleetName, Competitor[][] competitorLists, TimePoint[] startTimes,
            Map<Competitor, TimePoint>[] lastMarkPassingTimesForCompetitors) {
        Iterator<? extends RaceColumn> columnIter = theSeries.getRaceColumns().iterator();
        int i=0;
        for (Competitor[] competitorList : competitorLists) {
            RaceColumn raceColumn = columnIter.next();
            final Map<Competitor, TimePoint> lastMarkPassingTimes = lastMarkPassingTimesForCompetitors[i];
            final Waypoint start = new WaypointImpl(new ControlPointWithTwoMarksImpl(new MarkImpl("Left StartBuoy"),
                    new MarkImpl("Right StartBuoy"), "Start", "Start"));
            final Waypoint finish = new WaypointImpl(new MarkImpl("FinishBuoy"));
            TrackedRace trackedRace = new MockedTrackedRaceWithStartTimeAndRanks(startTimes[i], Arrays.asList(competitorList)) {
                private static final long serialVersionUID = 1L;
                @Override
                public NavigableSet<MarkPassing> getMarkPassings(Competitor competitor) {
                    ArrayListNavigableSet<MarkPassing> result = new ArrayListNavigableSet<>(new TimedComparator());
                    result.add(new MarkPassingImpl(lastMarkPassingTimes.get(competitor), finish, competitor));
                    return result;
                }
                @Override
                public Duration getTimeSailedSinceRaceStart(Competitor competitor, TimePoint timePoint) {
                    final TimePoint timePointOfFinishMarkPassing = getMarkPassings(competitor).last().getTimePoint();
                    final TimePoint to = timePointOfFinishMarkPassing.before(timePoint) ? timePointOfFinishMarkPassing : timePoint;
                    return to.before(getStartOfRace()) ? null : getStartOfRace().until(to);
                }
            };
            trackedRace.getRace().getCourse().addWaypoint(0, start);
            trackedRace.getRace().getCourse().addWaypoint(1, finish);
            raceColumn.setTrackedRace(raceColumn.getFleetByName(fleetName), trackedRace);
            i++;
        }
    }

    public static List<Competitor> createCompetitors(int numberOfCompetitorsToCreate) {
        List<Competitor> result = new ArrayList<Competitor>();
        for (int i=1; i<=numberOfCompetitorsToCreate; i++) {
            result.add(createCompetitorWithBoat("C"+i));
        }
        return result;
    }

    protected Regatta createSimpleRegatta(final int numberOfRaces, final String regattaName, BoatClass boatClass, ScoringScheme scoringScheme) {
        series = new ArrayList<Series>();
        List<Fleet> fleets = new ArrayList<Fleet>();
        fleets.add(new FleetImpl("Default"));

        List<String> raceColumnNames = new ArrayList<String>();
        for (int i = 1; i <= numberOfRaces; i++) {
            raceColumnNames.add("R" + i);
        }
        Series defaultSeries = new SeriesImpl("Default", /* isMedal */false, /* isFleetsCanRunInParallel */true,
                fleets, raceColumnNames, /* trackedRegattaRegistry */null);
        series.add(defaultSeries);

        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName(regattaName, boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /* startDate */null, /* endDate */null, series, 
                /* persistent */false, scoringScheme, "123", null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }

    protected Regatta createRegatta(final int numberOfQualifyingRaces, String[] qualifyingFleetNames, final int numberOfFinalRaces,
            String[] finalFleetNames, boolean medalRaceAndSeries, final int numberOfMedalRaces, final String regattaBaseName, BoatClass boatClass, ScoringScheme scoringScheme) {
        series = new ArrayList<Series>();

        // -------- qualifying series ------------
        if (qualifyingFleetNames != null && qualifyingFleetNames.length > 0) {
            List<Fleet> qualifyingFleets = new ArrayList<Fleet>();
            for (String qualifyingFleetName : qualifyingFleetNames) {
                qualifyingFleets.add(new FleetImpl(qualifyingFleetName));
            }
            List<String> qualifyingRaceColumnNames = new ArrayList<String>();
            for (int i = 1; i <= numberOfQualifyingRaces; i++) {
                qualifyingRaceColumnNames.add("Q" + i);
            }
            Series qualifyingSeries = new SeriesImpl("Qualifying", /* isMedal */false, /* isFleetsCanRunInParallel */ true, qualifyingFleets,
                    qualifyingRaceColumnNames, /* trackedRegattaRegistry */null);
            series.add(qualifyingSeries);
        }

        // -------- final series ------------
        if (finalFleetNames != null && finalFleetNames.length > 0) {
            List<Fleet> finalFleets = new ArrayList<Fleet>();
            int fleetOrdering = 1;
            for (String finalFleetName : finalFleetNames) {
                finalFleets.add(new FleetImpl(finalFleetName, fleetOrdering++));
            }
            List<String> finalRaceColumnNames = new ArrayList<String>();
            for (int i = 1; i <= numberOfFinalRaces; i++) {
                finalRaceColumnNames.add("F" + i);
            }
            Series finalSeries = new SeriesImpl("Final", /* isMedal */false, /* isFleetsCanRunInParallel */ true, finalFleets, finalRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(finalSeries);
        }

        if (medalRaceAndSeries) {
            // ------------ medal --------------
            List<Fleet> medalFleets = new ArrayList<Fleet>();
            medalFleets.add(new FleetImpl("Medal"));
            List<String> medalRaceColumnNames = new ArrayList<String>();
            if(numberOfMedalRaces == 1) {
                medalRaceColumnNames.add("M");
            } else if(numberOfMedalRaces > 1) {
                for (int i = 1; i <= numberOfMedalRaces; i++) {
                    medalRaceColumnNames.add("M" + i);
                }
            }
            Series medalSeries = new SeriesImpl("Medal", /* isMedal */true, /* isFleetsCanRunInParallel */ true, medalFleets, medalRaceColumnNames, /* trackedRegattaRegistry */ null);
            series.add(medalSeries);
        }
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null, series, 
                /* persistent */ false, scoringScheme, "123", null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }
    
    protected Regatta createRegattaWithEliminations(final int numberOfEliminations, final int[] numbersOfHeatsPerRound,
            final String regattaBaseName, BoatClass boatClass, ScoringScheme scoringScheme) {
        series = new ArrayList<Series>();
        // example for numbersOfHeatsPerRound: [8, 4, 2, 2]
        for (int elimination=1; elimination<=numberOfEliminations; elimination++) {
            int heatNumber = 1;
            // create one elimination consisting of a number of rounds, each consisting of a number of heats
            int roundNumber = 1;
            for (int numberOfHeatsPerRound : numbersOfHeatsPerRound) {
                final boolean isFinalRound = roundNumber == numbersOfHeatsPerRound.length;
                // create one round as a series that has one fleet per heat
                List<Fleet> fleetsInRound = new ArrayList<Fleet>();
                for (int heatInRound=1; heatInRound<=numberOfHeatsPerRound; heatInRound++) {
                    final int ordering = numbersOfHeatsPerRound.length-roundNumber+1+
                            // in final round distinguish Final and Losers Final
                            (isFinalRound ? heatInRound-1 : 1);
                    fleetsInRound.add(new FleetImpl("Heat "+(heatNumber++), ordering));
                }
                List<String> raceColumnNameForRound = new ArrayList<String>();
                raceColumnNameForRound.add("E"+elimination+"R"+roundNumber);
                final String roundName;
                if (numbersOfHeatsPerRound.length-roundNumber == 2) {
                    roundName = "Quarter-Final";
                } else if (numbersOfHeatsPerRound.length-roundNumber == 1) {
                    roundName = "Semi-Final";
                } else if (numbersOfHeatsPerRound.length-roundNumber == 0) {
                    roundName = "Final";
                } else {
                    roundName = "Round "+roundNumber;
                }
                Series seriesForRound = new SeriesImpl("E"+elimination+" "+roundName, /* isMedal */ false, /* isFleetsCanRunInParallel */ true, fleetsInRound,
                        raceColumnNameForRound, /* trackedRegattaRegistry */null);
                if (isFinalRound) {
                    // last "Final" round; here, the fleets are contiguously scored
                    seriesForRound.setSplitFleetContiguousScoring(true);
                }
                series.add(seriesForRound);
                roundNumber++;
            }
        }
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName(regattaBaseName, boatClass.getName()), boatClass, 
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /*startDate*/ null, /*endDate*/ null, series,
                /* persistent */ false, scoringScheme, /* ID */ "123", /* course area */ null,
                OneDesignRankingMetric::new, /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }
}
