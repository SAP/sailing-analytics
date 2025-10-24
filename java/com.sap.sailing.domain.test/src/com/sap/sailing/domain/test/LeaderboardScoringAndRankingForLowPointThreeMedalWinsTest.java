package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPointFirstToWinThreeRaces;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * This class contains several tests for the {@link LowPointFirstToWinThreeRaces} scoring rule defined by
 * {@link ScoringSchemeType#LOW_POINT_FIRST_TO_WIN_THREE_RACES}. It tests that carried wins are applied properly, that
 * final participants always rank better than semi-finalists who did not advance to the final, and that ties in the
 * medal stage are broken first by the last medal race score, then by the opening series rank. Furthermore it contains
 * several negative tests that validate that the normal low point behavior is not changed and still works for those
 * same cases in case the {@link ScoringSchemeType#LOW_POINT} scoring scheme is used, e.g., for those competitors
 * who did not advance to the semi-final stage.
 */
public class LeaderboardScoringAndRankingForLowPointThreeMedalWinsTest extends LeaderboardScoringAndRankingTestBase {
    private static final String SEMIFINAL_SERIES_NAME = "Semifinal";
    private static final String SEMIFINAL_FLEET_A_NAME = "A";
    private static final String SEMIFINAL_FLEET_B_NAME = "B";
    private static final String SEMIFINAL_CARRY_COLUMN_NAME = "Carry SF";
    private static final String GRANDFINAL_SERIES_NAME = "Grand Final";
    private static final String GRANDFINAL_DEFAULT_FLEET_NAME = "Default";
    private static final String FINAL_CARRY_COLUMN_NAME = "Carry F";
    private static final double EPSILON = 0.000001;
    private Regatta regatta;
    private RegattaLeaderboard leaderboard;

    private Regatta setupRegatta() {
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("Kite", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                false, CompetitorRegistrationType.CLOSED, /* startDate */ null, /* endDate */ null, series, /* persistent */false,
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_FIRST_TO_WIN_THREE_RACES),
                /* ID */ "123", /* course area */ null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }

    private void setupMedalSeriesWithCarryOverAndSixRaceColumnsEach() {
        Iterable<? extends Fleet> semiFinalFleets = Arrays.asList(new Fleet[] { new FleetImpl(SEMIFINAL_FLEET_A_NAME, 0), new FleetImpl(SEMIFINAL_FLEET_B_NAME, 0)});
        List<String> semiFinalRaceColumnNames = new ArrayList<String>();
        semiFinalRaceColumnNames.add(SEMIFINAL_CARRY_COLUMN_NAME);
        semiFinalRaceColumnNames.add("SF1");
        semiFinalRaceColumnNames.add("SF2");
        semiFinalRaceColumnNames.add("SF3");
        semiFinalRaceColumnNames.add("SF4");
        semiFinalRaceColumnNames.add("SF5");
        semiFinalRaceColumnNames.add("SF6");
        Series semiFinalSeries = new SeriesImpl(SEMIFINAL_SERIES_NAME, /* isMedal */ true, /* isFleetsCanRunInParallel */ true,
                semiFinalFleets, semiFinalRaceColumnNames, /* trackedRegattaRegistry */ null);
        semiFinalSeries.setFirstColumnIsNonDiscardableCarryForward(true);
        semiFinalSeries.setStartsWithZeroScore(true);
        series.add(semiFinalSeries);
        Set<? extends Fleet> grandFinalFleets = Collections.singleton(new FleetImpl(GRANDFINAL_DEFAULT_FLEET_NAME));
        List<String> grandFinalRaceColumnNames = new ArrayList<String>();
        grandFinalRaceColumnNames.add(FINAL_CARRY_COLUMN_NAME);
        grandFinalRaceColumnNames.add("F1");
        grandFinalRaceColumnNames.add("F2");
        grandFinalRaceColumnNames.add("F3");
        grandFinalRaceColumnNames.add("F4");
        grandFinalRaceColumnNames.add("F5");
        grandFinalRaceColumnNames.add("F6");
        Series grandFinalSeries = new SeriesImpl(GRANDFINAL_SERIES_NAME, /* isMedal */ true, /* isFleetsCanRunInParallel */ true,
                grandFinalFleets, grandFinalRaceColumnNames, /* trackedRegattaRegistry */ null);
        grandFinalSeries.setFirstColumnIsNonDiscardableCarryForward(true);
        grandFinalSeries.setStartsWithZeroScore(true);
        series.add(grandFinalSeries);
    }

    private void setupOpeningSeriesWithOneRaceColumnPerSeries() {
        List<Fleet> qualificationFleets = new ArrayList<>();
        for (String qualificationFleetName : new String[] { "Yellow", "Blue" }) {
            qualificationFleets.add(new FleetImpl(qualificationFleetName));
        }
        List<String> qualificationRaceColumnNames = new ArrayList<String>();
        qualificationRaceColumnNames.add("R1");
        Series qualificationSeries = new SeriesImpl("Qualification", /* isMedal */ false,
                /* isFleetsCanRunInParallel */ true, qualificationFleets, qualificationRaceColumnNames,
                /* trackedRegattaRegistry */ null);
        series.add(qualificationSeries);
        List<Fleet> finalFleets = new ArrayList<>();
        int fleetRank = 0;
        for (String qualificationFleetName : new String[] { "Gold", "Silver" }) {
            finalFleets.add(new FleetImpl(qualificationFleetName, fleetRank++));
        }
        List<String> finalRaceColumnNames = new ArrayList<String>();
        finalRaceColumnNames.add("R2");
        Series finalSeries = new SeriesImpl("Final", /* isMedal */ false,
                /* isFleetsCanRunInParallel */ true, finalFleets, finalRaceColumnNames,
                /* trackedRegattaRegistry */ null);
        series.add(finalSeries);
    }

    @BeforeEach
    public void setUp() {
        series = new ArrayList<Series>();
        setupOpeningSeriesWithOneRaceColumnPerSeries();
        setupMedalSeriesWithCarryOverAndSixRaceColumnsEach();
        regatta = setupRegatta();
        // leaderboard set-up
        leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
    }
    
    @Test
    public void testWithRandomRaceOutcomes20() throws NoWindException {
        testWithRandomRaceOutcomes(/* numberOfCompetitors */ 20);
    }

    @Test
    public void testWithRandomRaceOutcomes40() throws NoWindException {
        testWithRandomRaceOutcomes(/* numberOfCompetitors */ 40);
    }

    @Test
    public void testWithRandomRaceOutcomes60() throws NoWindException {
        testWithRandomRaceOutcomes(/* numberOfCompetitors */ 60);
    }

    @Test
    public void testWithRandomRaceOutcomes80() throws NoWindException {
        testWithRandomRaceOutcomes(/* numberOfCompetitors */ 80);
    }

    @Test
    public void testWithRandomRaceOutcomes100() throws NoWindException {
        testWithRandomRaceOutcomes(/* numberOfCompetitors */ 100);
    }

    /**
     * In this test the opening series winner will win the first race, and should get a score of 2, all other finalists
     * should be scored with Low_Points restarting at 0 for the medal series. The non finalists score should not change
     * during the medalseries.
     */
    private void testWithRandomRaceOutcomes(final int numberOfCompetitors) {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        final Iterable<Competitor> openingSeriesRankResult = raceAndAssertOpeningSeriesAndRandomSemiFinals(now, later, numberOfCompetitors);
        final Iterable<Competitor> rankResultsAfterSemifinals = leaderboard.getCompetitorsFromBestToWorst(later);
        // assemble final fleet:
        final List<Competitor> finalists = new ArrayList<>();
        finalists.add(Util.get(openingSeriesRankResult, 0));
        finalists.add(Util.get(openingSeriesRankResult, 1));
        finalists.add(Util.get(rankResultsAfterSemifinals,2));
        finalists.add(Util.get(rankResultsAfterSemifinals,3));
        runRacesInFinalUntilThreeWins(finalists, regatta.getSeriesByName(GRANDFINAL_SERIES_NAME), GRANDFINAL_DEFAULT_FLEET_NAME, now);
        // check total results:
        final Iterable<Competitor> rankResultsAfterGrandFinal = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(3.0, leaderboard.getNetPoints(Util.get(rankResultsAfterGrandFinal, 0), later), EPSILON);
        assertFinalistRanks(leaderboard, regatta.getSeriesByName(GRANDFINAL_SERIES_NAME), finalists, rankResultsAfterGrandFinal, openingSeriesRankResult, later);
        // assert that non-medalists remain in the order computed at the end of the opening series
        for (int i=10; i<Util.size(openingSeriesRankResult); i++) {
            assertSame(Util.get(openingSeriesRankResult, i), Util.get(rankResultsAfterGrandFinal, i));
        }
    }

    /**
     * In this test the opening series winner will win the first race, and should get a score of 2, all other finalists
     * should be scored with Low_Points restarting at 0 for the medal series. The non finalists score should not change
     * during the medalseries.
     */
    @Test
    public void testWithTiesInFinal() {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        final Iterable<Competitor> openingSeriesRankResult = raceAndAssertOpeningSeriesAndRandomSemiFinals(now, later, /* numberOfCompetitors */ 1000);
        final Iterable<Competitor> rankResultsAfterSemifinals = leaderboard.getCompetitorsFromBestToWorst(later);
        // assemble final fleet:
        final List<Competitor> finalists = new ArrayList<>();
        finalists.add(Util.get(openingSeriesRankResult, 0));
        finalists.add(Util.get(openingSeriesRankResult, 1));
        finalists.add(Util.get(rankResultsAfterSemifinals, 2));
        finalists.add(Util.get(rankResultsAfterSemifinals, 3));
        // the two promoted semi-finalists have no carried wins; if they don't win they are tied on their zero wins;
        // if they score equal, e.g., both DNF (5), the tie has to be broken by their opening series rank:
        final TrackedRace finalRace = new MockedTrackedRaceWithStartTimeAndRanks(now, finalists); // (0) wins, with two carried wins has three wins
        final Series grandFinalSeries = regatta.getSeriesByName(GRANDFINAL_SERIES_NAME);
        final RaceColumnInSeries f1Column = grandFinalSeries.getRaceColumnByName("F1");
        f1Column.setTrackedRace(grandFinalSeries.getFleetByName(GRANDFINAL_DEFAULT_FLEET_NAME), finalRace);
        leaderboard.getScoreCorrection().correctScore(finalists.get(2), f1Column, 5.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(finalists.get(2), f1Column, MaxPointsReason.DNC);
        leaderboard.getScoreCorrection().correctScore(finalists.get(3), f1Column, 5.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(finalists.get(3), f1Column, MaxPointsReason.DNF);
        // check total results:
        final Iterable<Competitor> rankResultsAfterGrandFinal = leaderboard.getCompetitorsFromBestToWorst(later);
        assertSame(finalists.get(0), Util.get(rankResultsAfterGrandFinal, 0)); // winner of last race wins the regatta
        assertEquals(3.0, leaderboard.getNetPoints(Util.get(rankResultsAfterGrandFinal, 0), later), EPSILON);
        assertSame(finalists.get(2), Util.get(rankResultsAfterGrandFinal, 2));
        assertSame(finalists.get(3), Util.get(rankResultsAfterGrandFinal, 3));
    }
    
    /**
     * In this test A races one race, B two. The semi-finals are crafted such that there are ties on wins
     * between competitors from A and B, so checking the last race's rank shall break the tie within the
     * respective semi-final fleet (A or B, respectively). Things are further crafted such that two competitors
     * in B tied on wins are also tied on score in their last B semi-final race, both scoring DNF, so the
     * tie-breaking comparison must continue with the last-but-one race. For the A fleet the set-up is such
     * that they only race one race, but two competitors not winning both score a DNF, so their tie needs to
     * be resolved based on their opening series ranks.<p>
     * 
     * Once the A/B fleets have their internal tie-breaking done, their non-winning ranks have to be compared
     * pair-wise (2nd vs. 2nd, 3rd vs. 3rd, and so on) based on their opening series rank for overall ranking.
     */
    @Test
    public void testWithTiesInSemiFinalWithDifferentNumbersOfSemifinalRaces() {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        createCompetitorsAndRunAndAssertOpeningSeries(now, later, /* numberOfCompetitors */ 1000);
        final Iterable<Competitor> openingSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        final Util.Pair<List<Competitor>, List<Competitor>> semiFinalists = assignCarryForwardWinsToSemiFinalistsAndGrandFinalists(later);
        // let top-seeded competitor from A with two carried wins win the first race immediately:
        // scores for the A semi-finalists in the order of their seeding:
        //   Carried Wins  SF1        Wins
        //        2         1          3
        //        1         2          1
        //                  5 (DNF)    0       <------- tied with 4th seed based on wins, tied on last race, better by seed from opening series
        //                  5 (DNF)    0
        final TrackedRace sf1ARace = new MockedTrackedRaceWithStartTimeAndRanks(now, semiFinalists.getA());
        final RaceColumn sf1Column = leaderboard.getRaceColumnByName("SF1");
        leaderboard.getScoreCorrection().setMaxPointsReason(semiFinalists.getA().get(2), sf1Column, MaxPointsReason.DNF);
        leaderboard.getScoreCorrection().correctScore(semiFinalists.getA().get(2), sf1Column, 5.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(semiFinalists.getA().get(3), sf1Column, MaxPointsReason.DNF);
        leaderboard.getScoreCorrection().correctScore(semiFinalists.getA().get(3), sf1Column, 5.0);
        sf1Column.setTrackedRace(sf1Column.getFleetByName(SEMIFINAL_FLEET_A_NAME), sf1ARace);
        // scores for the B semi-finalists in the order of their seeding:
        //   Carried Wins  SF1  SF2     Wins
        //        2         4    1       3
        //        1         3    5 (DNF) 1
        //                  2    2       0
        //                  1    5 (DNF) 1      <------ tied with 2nd seed based on wins, same rank/score in SF2, better in SF1
        final TrackedRace sf1BRace = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(
                semiFinalists.getB().get(3),
                semiFinalists.getB().get(2),
                semiFinalists.getB().get(1),
                semiFinalists.getB().get(0)));
        // The expected orders for A/B: A1,A2,A3/A4 (depending on opening series score); B1,B4,B2,B3
        // This merges into places 5-10 overall, based on Opening Series rank: A2/B4, A3/A4/B2, A3/A4/B3 (where A3/A4 is decided based on Opening Series again)
        sf1Column.setTrackedRace(sf1Column.getFleetByName(SEMIFINAL_FLEET_B_NAME), sf1BRace);
        final TrackedRace sf2BRace = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(
                semiFinalists.getB().get(0),
                semiFinalists.getB().get(2),
                semiFinalists.getB().get(1),
                semiFinalists.getB().get(3)));
        final RaceColumn sf2Column = leaderboard.getRaceColumnByName("SF2");
        sf2Column.setTrackedRace(sf2Column.getFleetByName(SEMIFINAL_FLEET_B_NAME), sf2BRace);
        leaderboard.getScoreCorrection().setMaxPointsReason(semiFinalists.getB().get(1), sf2Column, MaxPointsReason.DNF);
        leaderboard.getScoreCorrection().correctScore(semiFinalists.getB().get(1), sf2Column, 5.0);
        leaderboard.getScoreCorrection().setMaxPointsReason(semiFinalists.getB().get(3), sf2Column, MaxPointsReason.DNF);
        leaderboard.getScoreCorrection().correctScore(semiFinalists.getB().get(3), sf2Column, 5.0);
        final Iterable<Competitor> rankResultsAfterSemifinals = leaderboard.getCompetitorsFromBestToWorst(later);
        // Overall ranks 1 and 2 are taken by the finalists; 3 and 4 by the semi-final winners. Start looking at rank 5 (zero-based 4):
        assertRanksBasedOnOpeningSeriesRanking(/* zeroBasedRankOfCompetitorWithBetterOpeningSeriesScore */ 2,
                semiFinalists.getA().get(0), semiFinalists.getB().get(0),
                openingSeriesRankResult, rankResultsAfterSemifinals);
        assertRanksBasedOnOpeningSeriesRanking(/* zeroBasedRankOfCompetitorWithBetterOpeningSeriesScore */ 4,
                semiFinalists.getA().get(1), semiFinalists.getB().get(3),
                openingSeriesRankResult, rankResultsAfterSemifinals);
        // find out whether A3 or A4 scored better in the opening series:
        final Competitor betterOfA3AndA4AfterOpeningSeries;
        final Competitor worseOfA3AndA4AfterOpeningSeries;
        if (Util.indexOf(openingSeriesRankResult, semiFinalists.getA().get(2)) < Util.indexOf(openingSeriesRankResult, semiFinalists.getA().get(3))) {
            betterOfA3AndA4AfterOpeningSeries = semiFinalists.getA().get(2);
            worseOfA3AndA4AfterOpeningSeries = semiFinalists.getA().get(3);
        } else {
            betterOfA3AndA4AfterOpeningSeries = semiFinalists.getA().get(3);
            worseOfA3AndA4AfterOpeningSeries = semiFinalists.getA().get(2);
        }
        assertRanksBasedOnOpeningSeriesRanking(/* zeroBasedRankOfCompetitorWithBetterOpeningSeriesScore */ 6,
                betterOfA3AndA4AfterOpeningSeries, semiFinalists.getB().get(1),
                openingSeriesRankResult, rankResultsAfterSemifinals);
        assertRanksBasedOnOpeningSeriesRanking(/* zeroBasedRankOfCompetitorWithBetterOpeningSeriesScore */ 8,
                worseOfA3AndA4AfterOpeningSeries, semiFinalists.getB().get(2),
                openingSeriesRankResult, rankResultsAfterSemifinals);
    }

    private void assertRanksBasedOnOpeningSeriesRanking(
            final int expectedZeroBasedRankAfterSemifinalsOfCompetitorWithBetterOpeningSeriesScore,
            final Competitor aSemiFinalist, final Competitor bSemiFinalist,
            final Iterable<Competitor> openingSeriesRankResult,
            final Iterable<Competitor> rankResultsAfterSemifinals) {
        if (Util.indexOf(openingSeriesRankResult, aSemiFinalist) < Util.indexOf(openingSeriesRankResult, bSemiFinalist)) {
            assertSame(Util.get(rankResultsAfterSemifinals, expectedZeroBasedRankAfterSemifinalsOfCompetitorWithBetterOpeningSeriesScore), aSemiFinalist);
            assertSame(Util.get(rankResultsAfterSemifinals, expectedZeroBasedRankAfterSemifinalsOfCompetitorWithBetterOpeningSeriesScore+1), bSemiFinalist);
        } else {
            assertSame(Util.get(rankResultsAfterSemifinals, expectedZeroBasedRankAfterSemifinalsOfCompetitorWithBetterOpeningSeriesScore+1), aSemiFinalist);
            assertSame(Util.get(rankResultsAfterSemifinals, expectedZeroBasedRankAfterSemifinalsOfCompetitorWithBetterOpeningSeriesScore), bSemiFinalist);
        }
    }
    
    /**
     * @return the ranking results after the opening series and before adding the carried wins
     */
    private Iterable<Competitor> raceAndAssertOpeningSeriesAndRandomSemiFinals(TimePoint now, TimePoint later, int numberOfCompetitors) {
        createCompetitorsAndRunAndAssertOpeningSeries(now, later, numberOfCompetitors);
        final Iterable<Competitor> openingSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        final Util.Pair<List<Competitor>, List<Competitor>> semiFinalists = assignCarryForwardWinsToSemiFinalistsAndGrandFinalists(later);
        // lottery for semi-final races until we have three wins:
        final Series semiFinalSeries = regatta.getSeriesByName(SEMIFINAL_SERIES_NAME);
        runRacesInFinalUntilThreeWins(semiFinalists.getA(), semiFinalSeries, SEMIFINAL_FLEET_A_NAME, now);
        runRacesInFinalUntilThreeWins(semiFinalists.getB(), semiFinalSeries, SEMIFINAL_FLEET_B_NAME, now);
        // check that the semi-final winners took three wins (including carried wins) each (note, they are at zero-based ranks 2/3 due to the
        // grand finalists already at zero-based ranks 0/1):
        final Iterable<Competitor> rankResultsAfterSemifinals = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(3.0, leaderboard.getNetPoints(Util.get(rankResultsAfterSemifinals, 2), later), EPSILON);
        assertEquals(3.0, leaderboard.getNetPoints(Util.get(rankResultsAfterSemifinals, 3), later), EPSILON);
        assertSemiFinalistRanks(leaderboard, semiFinalSeries, semiFinalists,
                leaderboard.getCompetitorsFromBestToWorst(later), openingSeriesRankResult, later);
        return openingSeriesRankResult;
    }

    /**
     * @param numberOfCompetitors
     *            must be evenly divisible by four, so we can have two opening series fleets and promote the better half
     *            of each of the qualification series races to gold and half to silver, each.
     */
    private void createCompetitorsAndRunAndAssertOpeningSeries(TimePoint now, TimePoint later, int numberOfCompetitors) {
        assertEquals(0, numberOfCompetitors % 4, "Number of competitors must be evenly divisible by four, but "+numberOfCompetitors+" isn't.");
        final int NUMBER_OF_COMPETITORS_PER_RACE = numberOfCompetitors / 2;
        // Competitor set-up
        final List<Competitor> competitors = createCompetitors(2*NUMBER_OF_COMPETITORS_PER_RACE);
        final List<Competitor> yellow = new ArrayList<>(competitors.subList(0, NUMBER_OF_COMPETITORS_PER_RACE));
        final List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);
        final List<Competitor> gold = new ArrayList<>();
        gold.addAll(yellow.subList(0, NUMBER_OF_COMPETITORS_PER_RACE/2));
        gold.addAll(blue.subList(0, NUMBER_OF_COMPETITORS_PER_RACE/2));
        Collections.shuffle(gold);
        final List<Competitor> silver = new ArrayList<>();
        silver.addAll(yellow.subList(NUMBER_OF_COMPETITORS_PER_RACE/2, NUMBER_OF_COMPETITORS_PER_RACE));
        silver.addAll(blue.subList(NUMBER_OF_COMPETITORS_PER_RACE/2, NUMBER_OF_COMPETITORS_PER_RACE));
        Collections.shuffle(silver);
        // assign points by creating races with competitors in the order defined above and check opening series scoring
        executePreSeries(yellow, blue, gold, silver, now);
        assertOpeningSeriesTiesBrokenProperly(leaderboard, later);
    }
    
    /**
     * Returns the semi-finalists for A and B fleets and set the carried wins for semi-finalists
     * and grand finalists.
     */
    private Pair<List<Competitor>, List<Competitor>> assignCarryForwardWinsToSemiFinalistsAndGrandFinalists(TimePoint timePoint) {
        // now compute and enter the wins carried forward:
        // - opening series winner carries two, second carries one win straight to the grand final
        // - third/fourth carry two wins to semi-final, fifth/sixth carry one win to semi-final
        // - ranks three to ten compete in the semi-final
        // - semi-final fleet A: 3, 6, 7, 10; semi-final fleet B: 4, 5, 8, 9
        final Iterable<Competitor> openingSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(timePoint);
        leaderboard.getScoreCorrection().correctScore(Util.get(openingSeriesRankResult, 0), leaderboard.getRaceColumnByName(FINAL_CARRY_COLUMN_NAME), 2.0);
        leaderboard.getScoreCorrection().correctScore(Util.get(openingSeriesRankResult, 1), leaderboard.getRaceColumnByName(FINAL_CARRY_COLUMN_NAME), 1.0);
        final Map<String, List<Competitor>> semiFinalCompetitorsByFleetName = new HashMap<>();
        final List<Competitor> sfACompetitors = new ArrayList<>();
        semiFinalCompetitorsByFleetName.put(SEMIFINAL_FLEET_A_NAME, sfACompetitors);
        sfACompetitors.add(Util.get(openingSeriesRankResult, 2));
        sfACompetitors.add(Util.get(openingSeriesRankResult, 5));
        sfACompetitors.add(Util.get(openingSeriesRankResult, 6));
        sfACompetitors.add(Util.get(openingSeriesRankResult, 9));
        final List<Competitor> sfBCompetitors = new ArrayList<>();
        semiFinalCompetitorsByFleetName.put(SEMIFINAL_FLEET_B_NAME, sfBCompetitors);
        sfBCompetitors.add(Util.get(openingSeriesRankResult, 3));
        sfBCompetitors.add(Util.get(openingSeriesRankResult, 4));
        sfBCompetitors.add(Util.get(openingSeriesRankResult, 7));
        sfBCompetitors.add(Util.get(openingSeriesRankResult, 8));
        leaderboard.getScoreCorrection().correctScore(sfACompetitors.get(0), leaderboard.getRaceColumnByName(SEMIFINAL_CARRY_COLUMN_NAME), 2.0);
        leaderboard.getScoreCorrection().correctScore(sfACompetitors.get(1), leaderboard.getRaceColumnByName(SEMIFINAL_CARRY_COLUMN_NAME), 1.0);
        leaderboard.getScoreCorrection().correctScore(sfBCompetitors.get(0), leaderboard.getRaceColumnByName(SEMIFINAL_CARRY_COLUMN_NAME), 2.0);
        leaderboard.getScoreCorrection().correctScore(sfBCompetitors.get(1), leaderboard.getRaceColumnByName(SEMIFINAL_CARRY_COLUMN_NAME), 1.0);
        // assert that the finalists already rank at the top:
        final Iterable<Competitor> rankResultsAfterApplyingCarriedWins = leaderboard.getCompetitorsFromBestToWorst(timePoint);
        assertSame(Util.get(openingSeriesRankResult, 0), Util.get(rankResultsAfterApplyingCarriedWins, 0));
        assertSame(Util.get(openingSeriesRankResult, 1), Util.get(rankResultsAfterApplyingCarriedWins, 1));
        // assert that semi-finalists are still ordered by their opening series rank because when tied on wins
        // the tie must be broken by the opening series rank:
        for (int i=2; i<10; i++) {
            assertSame(Util.get(openingSeriesRankResult, i), Util.get(rankResultsAfterApplyingCarriedWins, i));
        }
        return new Util.Pair<>(sfACompetitors, sfBCompetitors);
    }
    
    /**
     * The competitor orders provided in the lists define the points scored in the respective race, one-based
     */
    private void executePreSeries(List<Competitor> yellowR1, List<Competitor> blueR1, List<Competitor> goldR2, List<Competitor> silverR2, TimePoint now) {
        final RaceColumn qColumnR1 = series.get(0).getRaceColumnByName("R1");
        final TrackedRace qYellowR1 = new MockedTrackedRaceWithStartTimeAndRanks(now, yellowR1);
        qColumnR1.setTrackedRace(qColumnR1.getFleetByName("Yellow"), qYellowR1);
        final TrackedRace qBlueR1 = new MockedTrackedRaceWithStartTimeAndRanks(now, blueR1);
        qColumnR1.setTrackedRace(qColumnR1.getFleetByName("Blue"), qBlueR1);
        final RaceColumn fColumnR2 = series.get(1).getRaceColumnByName("R2");
        final TrackedRace fGoldR2 = new MockedTrackedRaceWithStartTimeAndRanks(now, goldR2);
        fColumnR2.setTrackedRace(fColumnR2.getFleetByName("Gold"), fGoldR2);
        final TrackedRace fSilverR2 = new MockedTrackedRaceWithStartTimeAndRanks(now, silverR2);
        fColumnR2.setTrackedRace(fColumnR2.getFleetByName("Silver"), fSilverR2);
    }

    /**
     * For all finalists adjacent in the {@code rankResultsAfterSemiFinal} checks that the competitor ranked better
     * has more or equal wins; if equal wins, scored better in the last race; if scored equal in the last race had
     * better rank in {@code openingSeriesRankResult}.
     */
    private void assertSemiFinalistRanks(Leaderboard leaderboard, Series semiFinalSeries, Pair<List<Competitor>, List<Competitor>> semiFinalists,
            Iterable<Competitor> rankResultsAfterSemiFinal, Iterable<Competitor> openingSeriesRankResult, TimePoint timePoint) {
        assertTrue(semiFinalSeries.isMedal());
        // assert that all finalists form the top overall ranks
        final List<Competitor> semiFinalistsInRankedOrder = Util.subList(rankResultsAfterSemiFinal, 2, 2+semiFinalists.getA().size()+semiFinalists.getB().size());
        final Set<Competitor> allSemifinalists = new HashSet<>();
        allSemifinalists.addAll(semiFinalists.getA());
        allSemifinalists.addAll(semiFinalists.getB());
        assertEquals(allSemifinalists, new HashSet<>(semiFinalistsInRankedOrder));
        // assert that each two adjacent odd/even-numbered semi-finalists in the overall
        // ranking stem from the two different semi-final fleets:
        for (int i=0; i<semiFinalists.getA().size(); i += 2) {
            assertTrue(!semiFinalists.getA().contains(semiFinalistsInRankedOrder.get(i)) ||
                    semiFinalists.getB().contains(semiFinalistsInRankedOrder.get(i+1)));
            assertTrue(!semiFinalists.getA().contains(semiFinalistsInRankedOrder.get(i+1)) ||
                    semiFinalists.getB().contains(semiFinalistsInRankedOrder.get(i)));
        }
        assertCorrectMedalSeriesSequence(leaderboard, semiFinalSeries, semiFinalists.getA(), rankResultsAfterSemiFinal, openingSeriesRankResult, timePoint);
        assertCorrectMedalSeriesSequence(leaderboard, semiFinalSeries, semiFinalists.getB(), rankResultsAfterSemiFinal, openingSeriesRankResult, timePoint);
    }

    /**
     * Constructs a sorted sequence from the {@code competitorsFromOneFleet}, sorted by their order in
     * {@code rankResultsAfterSeries}, then compares all adjacent pairs in that sequence for their correct comparison,
     * using
     * {@link #assertCorrectMedalSeriesSequence(Leaderboard, Series, Competitor, Competitor, Iterable, TimePoint)}.
     */
    private void assertCorrectMedalSeriesSequence(Leaderboard leaderboard, Series semiFinalSeries, List<Competitor> competitorsFromOneFleet,
            Iterable<Competitor> rankResultsAfterSeries, Iterable<Competitor> openingSeriesRankResult,
            TimePoint timePoint) {
        final Iterable<Competitor> competitorsFromOneFleetOrderedByRankResults =
                Util.filter(rankResultsAfterSeries, competitorsFromOneFleet::contains);
        Competitor previous = null;
        for (final Competitor next : competitorsFromOneFleetOrderedByRankResults) {
            if (previous != null) {
                assertCorrectMedalSeriesSequence(leaderboard, semiFinalSeries, previous, next, openingSeriesRankResult, timePoint);
            }
            previous = next;
        }
    }

    /**
     * For all finalists adjacent in the {@code rankResultsAfterGrandFinal} checks that the competitor ranked better
     * has more or equal wins; if equal wins, scored better in the last race; if scored equal in the last race had
     * better rank in {@code openingSeriesRankResult}.
     */
    private void assertFinalistRanks(Leaderboard leaderboard, Series medalSeries, Iterable<Competitor> finalists,
            Iterable<Competitor> rankResultsAfterGrandFinal, Iterable<Competitor> openingSeriesRankResult, TimePoint timePoint) {
        assertTrue(medalSeries.isMedal());
        // assert that all finalists form the top overall ranks
        final List<Competitor> finalistsInRankedOrder = Util.subList(rankResultsAfterGrandFinal, 0, Util.size(finalists));
        assertEquals(Util.asSet(finalists), new HashSet<>(finalistsInRankedOrder));
        Competitor previous = null;
        for (final Competitor next : finalistsInRankedOrder) {
            if (previous != null) {
                assertCorrectMedalSeriesSequence(leaderboard, medalSeries, previous, next, openingSeriesRankResult, timePoint);
            }
            previous = next;
        }
    }

    /**
     * Can be used to assert the ranking <em>within</em> a single medal series fleet, such as the grand final,
     * or the semi-final fleet A, or the semi-final fleet B.<p>
     * 
     * Checks that {@code previous} (considered as ranked better than {@code next}) has more or equal wins; if equal
     * wins, scored better in the last race, then last-but-one race, and so on; if scored equal in all races up to the first
     * in the medal series, check that "previous" had better rank in {@code openingSeriesRankResult}.
     */
    private void assertCorrectMedalSeriesSequence(Leaderboard leaderboard, Series medalSeries, Competitor previous,
            Competitor next, Iterable<Competitor> openingSeriesRankResult, TimePoint timePoint) {
        // assert same fleet
        assertSame(medalSeries.getRaceColumns().iterator().next().getFleetOfCompetitor(previous),
                   medalSeries.getRaceColumns().iterator().next().getFleetOfCompetitor(next));
        // assert previous is really "better" than next
        assertTrue(leaderboard.getNetPoints(previous, timePoint) >= leaderboard.getNetPoints(next, timePoint));
        if (leaderboard.getNetPoints(previous, timePoint).doubleValue() == leaderboard.getNetPoints(next, timePoint).doubleValue()) {
            // equal number of wins; look at last race scored, then last-but-one, and so on, until a difference is found
            // or we reach the first race:
            final List<? extends RaceColumnInSeries> raceColumnsInReverseOrder = Util.asList(medalSeries.getRaceColumns());
            Collections.reverse(raceColumnsInReverseOrder);
            boolean differenceFound = false;
            for (final RaceColumnInSeries raceColumn : raceColumnsInReverseOrder) {
                if (!raceColumn.isCarryForward()) {
                    final Double pointsScoredByPrevious = leaderboard.getTotalPoints(previous, raceColumn, timePoint);
                    final Double pointsScoredByNext = leaderboard.getTotalPoints(next, raceColumn, timePoint);
                    assertTrue((pointsScoredByPrevious==null && pointsScoredByNext==null)
                            || pointsScoredByPrevious <= pointsScoredByNext);
                    if (pointsScoredByPrevious != null && pointsScoredByPrevious < pointsScoredByNext) {
                        differenceFound = true;
                        break;
                    }
                }
            }
            if (!differenceFound) {
                // equal score in all races in the medal series; expect tie to have been broken by opening series
                assertTrue(Util.indexOf(openingSeriesRankResult, previous) < Util.indexOf(openingSeriesRankResult, next));
            }
        }
    }

    /**
     * Creates races and attaches them to the series/fleet starting with the first non-carry column
     * until one competitor achieves three wins. The competitors are expected to be ordered by their
     * carried wins (two wins for the first competitor, one win for the second). The {@code orderedCompetitors}
     * list will be shuffled in place by this method.
     */
    private void runRacesInFinalUntilThreeWins(final List<Competitor> orderedCompetitors, final Series series,
            final String fleetName, TimePoint timePoint) {
        final Map<Competitor, Integer> wins = new HashMap<>();
        wins.put(orderedCompetitors.get(0), 2);
        wins.put(orderedCompetitors.get(1), 1);
        for (final RaceColumn sfColumn : series.getRaceColumns()) {
            if (!sfColumn.isCarryForward()) {
                Collections.shuffle(orderedCompetitors);
                final TrackedRace sfRace = new MockedTrackedRaceWithStartTimeAndRanks(timePoint, orderedCompetitors);
                sfColumn.setTrackedRace(series.getFleetByName(fleetName), sfRace);
                final Competitor raceWinner = orderedCompetitors.get(0);
                final int oldWins = wins.getOrDefault(raceWinner, 0);
                final int newWins = oldWins+1;
                wins.put(raceWinner, newWins);
                if (newWins == 3) {
                    break;
                }
            }
        }
    }

    private void assertOpeningSeriesTiesBrokenProperly(Leaderboard leaderboard, TimePoint timePoint) {
        final Iterable<Competitor> openingSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(timePoint);
        Competitor previous = null;
        for (final Competitor next : openingSeriesRankResult) {
            if (previous != null) {
                assertCorrectOpeningSeriesSequence(leaderboard, previous, next, timePoint);
            }
            previous = next;
        }
    }

    private void assertCorrectOpeningSeriesSequence(Leaderboard leaderboard, Competitor previous, Competitor next, TimePoint timePoint) {
        final RaceColumn r1Column = regatta.getRaceColumnByName("R1");
        final RaceColumn r2Column = regatta.getRaceColumnByName("R2");
        final int previousFleetOrdering = r2Column.getFleetOfCompetitor(previous).getOrdering();
        final int nextFleetOrdering = r2Column.getFleetOfCompetitor(next).getOrdering();
        assertTrue(+ previousFleetOrdering <= nextFleetOrdering,
                "Competitor "+previous+" ranked better although their final series fleet ranks worse than that of "+next);
        if (previousFleetOrdering == nextFleetOrdering) {
            // for equal fleet ordering ranking must be decided by points, and ties are to be broken by A8.1
            final Double previousNetPoints = leaderboard.getNetPoints(previous, timePoint);
            final Double nextNetPoints = leaderboard.getNetPoints(next, timePoint);
            assertTrue(previousNetPoints <= nextNetPoints, "Competitor "+previous+" ranked better although more points than "+next);
            if (previousNetPoints.doubleValue() == nextNetPoints.doubleValue()) {
                final double previousR1Score = leaderboard.getTotalPoints(previous, r1Column, timePoint);
                final double previousR2Score = leaderboard.getTotalPoints(previous, r2Column, timePoint);
                final double nextR1Score = leaderboard.getTotalPoints(next, r1Column, timePoint);
                final double nextR2Score = leaderboard.getTotalPoints(next, r2Column, timePoint);
                assertTrue(
                        Math.min(previousR1Score, previousR2Score) <= Math.min(nextR1Score, nextR2Score),
                        "A8.1 tie-break broken: " + previous + " scored [" + previousR1Score + ", " + previousR2Score
                                + "], " + next + " scored [" + nextR1Score + ", " + nextR2Score + "], yet " + previous
                                + " was ranked better");
                if (Math.min(previousR1Score, previousR2Score) == Math.min(nextR1Score, nextR2Score)) {
                    // it had to be broken by the last race
                    assertTrue(
                            previousR2Score < nextR2Score,
                            "Tie-break by last race broken: " + previous + " scored " + previousR2Score + ", " + next
                                    + " scored " + nextR2Score + ", yet " + previous + " was ranked better");
                }
            }
        }
    }
}
