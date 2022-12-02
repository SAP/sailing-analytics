package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

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
public class LeaderboardScoringAndRankingTestForLowPointThreeMedalWins extends LeaderboardScoringAndRankingTestBase {
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

    @Before
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
        final List<Competitor> openingSeriesRankResult = raceAndAssertOpeningSeriesAndRandomSemiFinals(now, later, numberOfCompetitors);
        final List<Competitor> rankResultsAfterSemifinals = leaderboard.getCompetitorsFromBestToWorst(later);
        // assemble final fleet:
        final List<Competitor> finalists = new ArrayList<>();
        finalists.add(openingSeriesRankResult.get(0));
        finalists.add(openingSeriesRankResult.get(1));
        finalists.add(rankResultsAfterSemifinals.get(2));
        finalists.add(rankResultsAfterSemifinals.get(3));
        runRacesInFinalUntilThreeWins(finalists, regatta.getSeriesByName(GRANDFINAL_SERIES_NAME), GRANDFINAL_DEFAULT_FLEET_NAME, now);
        // check total results:
        final List<Competitor> rankResultsAfterGrandFinal = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(3.0, leaderboard.getNetPoints(rankResultsAfterGrandFinal.get(0), later), EPSILON);
        assertFinalistRanks(leaderboard, regatta.getSeriesByName(GRANDFINAL_SERIES_NAME), finalists, rankResultsAfterGrandFinal, openingSeriesRankResult, later);
        // assert that non-medalists remain in the order computed at the end of the opening series
        for (int i=10; i<openingSeriesRankResult.size(); i++) {
            assertSame(openingSeriesRankResult.get(i), rankResultsAfterGrandFinal.get(i));
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
        final List<Competitor> openingSeriesRankResult = raceAndAssertOpeningSeriesAndRandomSemiFinals(now, later, /* numberOfCompetitors */ 100);
        final List<Competitor> rankResultsAfterSemifinals = leaderboard.getCompetitorsFromBestToWorst(later);
        // assemble final fleet:
        final List<Competitor> finalists = new ArrayList<>();
        finalists.add(openingSeriesRankResult.get(0));
        finalists.add(openingSeriesRankResult.get(1));
        finalists.add(rankResultsAfterSemifinals.get(2));
        finalists.add(rankResultsAfterSemifinals.get(3));
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
        final List<Competitor> rankResultsAfterGrandFinal = leaderboard.getCompetitorsFromBestToWorst(later);
        assertSame(finalists.get(0), rankResultsAfterGrandFinal.get(0)); // winner of last race wins the regatta
        assertEquals(3.0, leaderboard.getNetPoints(rankResultsAfterGrandFinal.get(0), later), EPSILON);
        assertSame(finalists.get(2), rankResultsAfterGrandFinal.get(2));
        assertSame(finalists.get(3), rankResultsAfterGrandFinal.get(3));
    }
    
    /**
     * In this test A races one race, B two. The semi-finals are crafted such that there are ties on wins
     * between competitors from A and B, so checking the last race's rank shall break the tie.
     */
    @Test
    public void testWithTiesInSemiFinalWithDifferentNumbersOfSemifinalRaces() {
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        createCompetitorsAndRunAndAssertOpeningSeries(now, later, /* numberOfCompetitors */ 100);
        final List<Competitor> openingSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        final Util.Pair<List<Competitor>, List<Competitor>> semiFinalists = assignCarryForwardWinsToSemiFinalistsAndGrandFinalists(later);
        // let top-seeded competitor from A with two carried wins win the first race immediately:
        // scores for the A semi-finalists in the order of their seeding:
        //   Carried Wins  SF1        Wins
        //        2         1          3
        //        1         2          1
        //                  3          0
        //                  4          0
        final TrackedRace sf1ARace = new MockedTrackedRaceWithStartTimeAndRanks(now, semiFinalists.getA());
        final RaceColumn sf1Column = leaderboard.getRaceColumnByName("SF1");
        sf1Column.setTrackedRace(sf1Column.getFleetByName(SEMIFINAL_FLEET_A_NAME), sf1ARace);
        // scores for the B semi-finalists in the order of their seeding:
        //   Carried Wins  SF1  SF2   Wins
        //        2         4    1     3
        //        1         3    2     1
        //                  2    3     0
        //                  1    4     1
        final TrackedRace sf1BRace = new MockedTrackedRaceWithStartTimeAndRanks(now, Arrays.asList(
                semiFinalists.getB().get(3),
                semiFinalists.getB().get(2),
                semiFinalists.getB().get(1),
                semiFinalists.getB().get(0)));
        sf1Column.setTrackedRace(sf1Column.getFleetByName(SEMIFINAL_FLEET_B_NAME), sf1BRace);
        final TrackedRace sf2BRace = new MockedTrackedRaceWithStartTimeAndRanks(now, semiFinalists.getB());
        final RaceColumn sf2Column = leaderboard.getRaceColumnByName("SF2");
        sf2Column.setTrackedRace(sf2Column.getFleetByName(SEMIFINAL_FLEET_B_NAME), sf2BRace);
        final List<Competitor> rankResultsAfterSemifinals = leaderboard.getCompetitorsFromBestToWorst(later);
        // So we should see ties on one win each for zero index-based A(1), B(1), and B(3).
        // Their last race's scores are, respectively,                 2  ,  2  ,      4
        // So A(1) and B(1) need to break their tie based on the opening series, and B(3) ranks worse.
        // Overall ranks 1 and 2 are taken by the finalists; 3 and 4 by the semi-final winners. Start looking at rank 5 (zero-based 4):
        if (openingSeriesRankResult.indexOf(semiFinalists.getA().get(1)) < openingSeriesRankResult.indexOf(semiFinalists.getB().get(1))) {
            assertSame(rankResultsAfterSemifinals.get(4), semiFinalists.getA().get(1));
            assertSame(rankResultsAfterSemifinals.get(5), semiFinalists.getB().get(1));
        } else {
            assertSame(rankResultsAfterSemifinals.get(5), semiFinalists.getA().get(1));
            assertSame(rankResultsAfterSemifinals.get(4), semiFinalists.getB().get(1));
        }
        assertSame(rankResultsAfterSemifinals.get(6), semiFinalists.getB().get(3));
        // And we should see ties on zero wins each for zero index-based A(2), A(3), and B(2)
        // Their last race's scores are, respectively,                    3  ,  4  ,      3
        // So A(2) and B(2) are tied on the last race too, and the opening series rank decides,
        // and A(3) follows
        if (openingSeriesRankResult.indexOf(semiFinalists.getA().get(2)) < openingSeriesRankResult.indexOf(semiFinalists.getB().get(2))) {
            assertSame(rankResultsAfterSemifinals.get(7), semiFinalists.getA().get(2));
            assertSame(rankResultsAfterSemifinals.get(8), semiFinalists.getB().get(2));
        } else {
            assertSame(rankResultsAfterSemifinals.get(8), semiFinalists.getA().get(2));
            assertSame(rankResultsAfterSemifinals.get(7), semiFinalists.getB().get(2));
        }
        assertSame(rankResultsAfterSemifinals.get(9), semiFinalists.getA().get(3));
    }
    
    /**
     * @return the ranking results after the opening series and before adding the carried wins
     */
    private List<Competitor> raceAndAssertOpeningSeriesAndRandomSemiFinals(TimePoint now, TimePoint later, int numberOfCompetitors) {
        createCompetitorsAndRunAndAssertOpeningSeries(now, later, numberOfCompetitors);
        final List<Competitor> openingSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        final Util.Pair<List<Competitor>, List<Competitor>> semiFinalists = assignCarryForwardWinsToSemiFinalistsAndGrandFinalists(later);
        // lottery for semi-final races until we have three wins:
        final Series semiFinalSeries = regatta.getSeriesByName(SEMIFINAL_SERIES_NAME);
        runRacesInFinalUntilThreeWins(semiFinalists.getA(), semiFinalSeries, SEMIFINAL_FLEET_A_NAME, now);
        runRacesInFinalUntilThreeWins(semiFinalists.getB(), semiFinalSeries, SEMIFINAL_FLEET_B_NAME, now);
        // check that the semi-final winners took three wins (including carried wins) each:
        final List<Competitor> rankResultsAfterSemifinals = leaderboard.getCompetitorsFromBestToWorst(later);
        assertEquals(3.0, leaderboard.getNetPoints(rankResultsAfterSemifinals.get(2), later), EPSILON);
        assertEquals(3.0, leaderboard.getNetPoints(rankResultsAfterSemifinals.get(3), later), EPSILON);
        assertSemiFinalistRanks(leaderboard, semiFinalSeries, semiFinalists, leaderboard.getCompetitorsFromBestToWorst(later), openingSeriesRankResult, later);
        return openingSeriesRankResult;
    }

    /**
     * @param numberOfCompetitors
     *            must be evenly divisible by four, so we can have two opening series fleets and promote the better half
     *            of each of the qualification series races to gold and half to silver, each.
     */
    private void createCompetitorsAndRunAndAssertOpeningSeries(TimePoint now, TimePoint later, int numberOfCompetitors) {
        assertEquals("Number of competitors must be evenly divisible by four, but "+numberOfCompetitors+" isn't.", 0, numberOfCompetitors % 4);
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
     * Returns the semi-finalists for A and B fleets
     */
    private Pair<List<Competitor>, List<Competitor>> assignCarryForwardWinsToSemiFinalistsAndGrandFinalists(TimePoint timePoint) {
        // now compute and enter the wins carried forward:
        // - opening series winner carries two, second carries one win straight to the grand final
        // - third/fourth carry two wins to semi-final, fifth/sixth carry one win to semi-final
        // - ranks three to ten compete in the semi-final
        // - semi-final fleet A: 3, 6, 7, 10; semi-final fleet B: 4, 5, 8, 9
        final List<Competitor> openingSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(timePoint);
        leaderboard.getScoreCorrection().correctScore(openingSeriesRankResult.get(0), leaderboard.getRaceColumnByName(FINAL_CARRY_COLUMN_NAME), 2.0);
        leaderboard.getScoreCorrection().correctScore(openingSeriesRankResult.get(1), leaderboard.getRaceColumnByName(FINAL_CARRY_COLUMN_NAME), 1.0);
        final Map<String, List<Competitor>> semiFinalCompetitorsByFleetName = new HashMap<>();
        final List<Competitor> sfACompetitors = new ArrayList<>();
        semiFinalCompetitorsByFleetName.put(SEMIFINAL_FLEET_A_NAME, sfACompetitors);
        sfACompetitors.add(openingSeriesRankResult.get(2));
        sfACompetitors.add(openingSeriesRankResult.get(5));
        sfACompetitors.add(openingSeriesRankResult.get(6));
        sfACompetitors.add(openingSeriesRankResult.get(9));
        final List<Competitor> sfBCompetitors = new ArrayList<>();
        semiFinalCompetitorsByFleetName.put(SEMIFINAL_FLEET_B_NAME, sfBCompetitors);
        sfBCompetitors.add(openingSeriesRankResult.get(3));
        sfBCompetitors.add(openingSeriesRankResult.get(4));
        sfBCompetitors.add(openingSeriesRankResult.get(7));
        sfBCompetitors.add(openingSeriesRankResult.get(8));
        leaderboard.getScoreCorrection().correctScore(sfACompetitors.get(0), leaderboard.getRaceColumnByName(SEMIFINAL_CARRY_COLUMN_NAME), 2.0);
        leaderboard.getScoreCorrection().correctScore(sfACompetitors.get(1), leaderboard.getRaceColumnByName(SEMIFINAL_CARRY_COLUMN_NAME), 1.0);
        leaderboard.getScoreCorrection().correctScore(sfBCompetitors.get(0), leaderboard.getRaceColumnByName(SEMIFINAL_CARRY_COLUMN_NAME), 2.0);
        leaderboard.getScoreCorrection().correctScore(sfBCompetitors.get(1), leaderboard.getRaceColumnByName(SEMIFINAL_CARRY_COLUMN_NAME), 1.0);
        // assert that the finalists already rank at the top:
        final List<Competitor> rankResultsAfterApplyingCarriedWins = leaderboard.getCompetitorsFromBestToWorst(timePoint);
        assertSame(openingSeriesRankResult.get(0), rankResultsAfterApplyingCarriedWins.get(0));
        assertSame(openingSeriesRankResult.get(1), rankResultsAfterApplyingCarriedWins.get(1));
        // assert that semi-finalists are still ordered by their opening series rank because when tied on wins
        // the tie must be broken by the opening series rank:
        for (int i=2; i<10; i++) {
            assertSame(openingSeriesRankResult.get(i), rankResultsAfterApplyingCarriedWins.get(i));
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
     * For all finalists adjacent in the {@code rankResultsAfterGrandFinal} checks that the competitor ranked better
     * has more or equal wins; if equal wins, scored better in the last race; if scored equal in the last race had
     * better rank in {@code openingSeriesRankResult}.
     */
    private void assertSemiFinalistRanks(Leaderboard leaderboard, Series semiFinalSeries, Pair<List<Competitor>, List<Competitor>> semiFinalists,
            List<Competitor> rankResultsAfterSemiFinal, List<Competitor> openingSeriesRankResult, TimePoint timePoint) {
        assertTrue(semiFinalSeries.isMedal());
        // assert that all finalists form the top overall ranks
        final List<Competitor> semiFinalistsInRankedOrder = rankResultsAfterSemiFinal.subList(2, 2+semiFinalists.getA().size()+semiFinalists.getB().size());
        final Set<Competitor> allSemifinalists = new HashSet<>();
        allSemifinalists.addAll(semiFinalists.getA());
        allSemifinalists.addAll(semiFinalists.getB());
        assertEquals(allSemifinalists, new HashSet<>(semiFinalistsInRankedOrder));
        Competitor previous = null;
        for (final Competitor next : semiFinalistsInRankedOrder) {
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
    private void assertFinalistRanks(Leaderboard leaderboard, Series medalSeries, List<Competitor> finalists,
            List<Competitor> rankResultsAfterGrandFinal, List<Competitor> openingSeriesRankResult, TimePoint timePoint) {
        assertTrue(medalSeries.isMedal());
        // assert that all finalists form the top overall ranks
        final List<Competitor> finalistsInRankedOrder = rankResultsAfterGrandFinal.subList(0, finalists.size());
        assertEquals(new HashSet<>(finalists), new HashSet<>(finalistsInRankedOrder));
        Competitor previous = null;
        for (final Competitor next : finalistsInRankedOrder) {
            if (previous != null) {
                assertCorrectMedalSeriesSequence(leaderboard, medalSeries, previous, next, openingSeriesRankResult, timePoint);
            }
            previous = next;
        }
    }

    /**
     * Checks that {@code previous} (considered as ranked better than {@code next}) has more or equal wins; if equal
     * wins, scored better in the last race; if scored equal in the last race had better rank in
     * {@code openingSeriesRankResult}.
     */
    private void assertCorrectMedalSeriesSequence(Leaderboard leaderboard, Series medalSeries, Competitor previous,
            Competitor next, List<Competitor> openingSeriesRankResult, TimePoint timePoint) {
        assertTrue(leaderboard.getNetPoints(previous, timePoint) >= leaderboard.getNetPoints(next, timePoint));
        if (leaderboard.getNetPoints(previous, timePoint).doubleValue() == leaderboard.getNetPoints(next, timePoint).doubleValue()) {
            // equal number of wins; look at last race scored:
            final Double previousLastScore = getLastScoredRace(leaderboard, medalSeries, previous, timePoint);
            final Double nextLastScore = getLastScoredRace(leaderboard, medalSeries, next, timePoint);
            assertTrue(previousLastScore <= nextLastScore);
            if (previousLastScore.doubleValue() == nextLastScore.doubleValue()) {
                // equal score in last race; expect tie to have been broken by opening series
                assertTrue(openingSeriesRankResult.indexOf(previous) < openingSeriesRankResult.indexOf(next));
            }
        }
    }

    private Double getLastScoredRace(Leaderboard leaderboard, Series medalSeries, Competitor competitor, TimePoint timePoint) {
        Iterable<Double> pointsScored = Util.map(medalSeries.getRaceColumns(), raceColumn->leaderboard.getTotalPoints(competitor, raceColumn, timePoint));
        return Util.last(Util.filter(pointsScored, points->points!=null));
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
        final List<Competitor> openingSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(timePoint);
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
        assertTrue("Competitor "+previous+" ranked better although their final series fleet ranks worse than that of "+next,
                + previousFleetOrdering <= nextFleetOrdering);
        if (previousFleetOrdering == nextFleetOrdering) {
            // for equal fleet ordering ranking must be decided by points, and ties are to be broken by A8.1
            final Double previousNetPoints = leaderboard.getNetPoints(previous, timePoint);
            final Double nextNetPoints = leaderboard.getNetPoints(next, timePoint);
            assertTrue("Competitor "+previous+" ranked better although more points than "+next, previousNetPoints <= nextNetPoints);
            if (previousNetPoints.doubleValue() == nextNetPoints.doubleValue()) {
                final double previousR1Score = leaderboard.getTotalPoints(previous, r1Column, timePoint);
                final double previousR2Score = leaderboard.getTotalPoints(previous, r2Column, timePoint);
                final double nextR1Score = leaderboard.getTotalPoints(next, r1Column, timePoint);
                final double nextR2Score = leaderboard.getTotalPoints(next, r2Column, timePoint);
                assertTrue(
                        "A8.1 tie-break broken: " + previous + " scored [" + previousR1Score + ", " + previousR2Score
                                + "], " + next + " scored [" + nextR1Score + ", " + nextR2Score + "], yet " + previous
                                + " was ranked better",
                        Math.min(previousR1Score, previousR2Score) <= Math.min(nextR1Score, nextR2Score));
                if (Math.min(previousR1Score, previousR2Score) == Math.min(nextR1Score, nextR2Score)) {
                    // it had to be broken by the last race
                    assertTrue(
                            "Tie-break by last race broken: " + previous + " scored " + previousR2Score + ", " + next
                                    + " scored " + nextR2Score + ", yet " + previous + " was ranked better",
                            previousR2Score < nextR2Score);
                }
            }
        }
    }
}
