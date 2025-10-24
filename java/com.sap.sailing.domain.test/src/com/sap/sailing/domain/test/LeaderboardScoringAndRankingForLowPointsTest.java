package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
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
import com.sap.sailing.domain.leaderboard.impl.LowPointFirstToWinTwoRaces;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * This class contains several tests for the {@link LowPointFirstToWinTwoRaces} scoring rule defined by
 * {@link ScoringSchemeType#LOW_POINT_FIRST_TO_WIN_TWO_RACES}. It tests that the carry is correctly applied and that the
 * ordering is as expected. Furthermore it contains several negative tests, that validate, that the normal low point
 * behavior is not changed and still works for those same cases in case the {@link ScoringSchemeType#LOW_POINT} scoring
 * scheme is used.
 */
public class LeaderboardScoringAndRankingForLowPointsTest extends LeaderboardScoringAndRankingTestBase {
    private static final double EPSILON = 0.000001;

    private void executePreSeries(List<Competitor> yellow, List<Competitor> blue, TimePoint now) {
        RaceColumn qColumn = series.get(0).getRaceColumnByName("Q");
        TrackedRace qYellow = new MockedTrackedRaceWithStartTimeAndRanks(now, yellow);
        qColumn.setTrackedRace(qColumn.getFleetByName("Yellow"), qYellow);
        TrackedRace qBlue = new MockedTrackedRaceWithStartTimeAndRanks(now, blue);
        qColumn.setTrackedRace(qColumn.getFleetByName("Blue"), qBlue);
    }

    private void manuallyTransferCarry(Leaderboard leaderboard, List<Competitor> medalCompetitors) {
        int carryScore = 1;
        for (Competitor medalCompetitor : medalCompetitors) {
            leaderboard.getScoreCorrection().correctScore(medalCompetitor, leaderboard.getRaceColumnByName("Carry"), carryScore++);
        }
    }

    private Regatta setupRegatta(boolean useFirstTwoWins) {
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                false, CompetitorRegistrationType.CLOSED, /* startDate */ null, /* endDate */ null, series, /* persistent */false,
                DomainFactory.INSTANCE
                        .createScoringScheme(useFirstTwoWins ? ScoringSchemeType.LOW_POINT_FIRST_TO_WIN_TWO_RACES
                                : ScoringSchemeType.LOW_POINT),
                "123", /* course area */null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }

    private Regatta setupRegattaWithHighPoint() {
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("IMOCA", /* typicallyStartsUpwind */ false);
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                false, CompetitorRegistrationType.CLOSED, /* startDate */ null, /* endDate */ null, series, /* persistent */false,
                DomainFactory.INSTANCE
                        .createScoringScheme(ScoringSchemeType.HIGH_POINT_LAST_BREAKS_TIE),
                "123", /* course area */null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }

    private Regatta setupRegattaWithLowPointWithAutomaticRDGSCA() {
        final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("J/70", /* typicallyStartsUpwind */ true);
        Regatta regatta = new RegattaImpl(RegattaImpl.getDefaultName("Test Regatta", boatClass.getName()), boatClass,
                false, CompetitorRegistrationType.CLOSED, /* startDate */ null, /* endDate */ null, series, /* persistent */false,
                DomainFactory.INSTANCE
                        .createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_AUTOMATIC_RDG),
                "123", /* course area */null, OneDesignRankingMetric::new,
                /* registrationLinkSecret */ UUID.randomUUID().toString());
        return regatta;
    }

    private void setupMedalSeriesWithCarryOverAndFourRaceColumns() {
        Set<? extends Fleet> medalFleets = Collections.singleton(new FleetImpl("Default"));
        List<String> medalRaceColumnNames = new ArrayList<>();
        medalRaceColumnNames.add("Carry");
        medalRaceColumnNames.add("M1");
        medalRaceColumnNames.add("M2");
        medalRaceColumnNames.add("M3");
        medalRaceColumnNames.add("M4");
        Series medalSeries = new SeriesImpl("Medal", /* isMedal */true, /* isFleetsCanRunInParallel */ true,
                medalFleets, medalRaceColumnNames, /* trackedRegattaRegistry */ null);
        medalSeries.setFirstColumnIsNonDiscardableCarryForward(true);
        medalSeries.setStartsWithZeroScore(true);
        series.add(medalSeries);
    }

    private void setupQualificationSeriesWithOneRaceColumn() {
        final List<Fleet> qualificationFleets = new ArrayList<>();
        for (String qualificationFleetName : new String[] { "Yellow", "Blue" }) {
            qualificationFleets.add(new FleetImpl(qualificationFleetName));
        }
        List<String> qualificationRaceColumnNames = new ArrayList<>();
        qualificationRaceColumnNames.add("Q");
        Series qualificationSeries = new SeriesImpl("Qualification", /* isMedal */false,
                /* isFleetsCanRunInParallel */ true, qualificationFleets, qualificationRaceColumnNames,
                /* trackedRegattaRegistry */ null);
        series.add(qualificationSeries);
    }

    private List<Pair<Competitor, Double>> createCompetitorResultForTimestamp(TimePoint time, Leaderboard leaderboard) {
        List<Pair<Competitor, Double>> list = new ArrayList<>();
        for (Competitor competitor : leaderboard.getCompetitorsFromBestToWorst(time)) {
            list.add(new Pair<Competitor, Double>(competitor, leaderboard.getNetPoints(competitor, time)));
        }
        return list;
    }

    private void assertNonFinalistsAreBehindFinalistsAndNotChanged(
            List<Pair<Competitor, Double>> preSeriesScoreRankResult, List<Pair<Competitor, Double>> afterFinalResults) {
        List<Pair<Competitor, Double>> nonFinalistsPreScore = preSeriesScoreRankResult.subList(4,
                preSeriesScoreRankResult.size());
        List<Pair<Competitor, Double>> nonFinalistsAfterScore = afterFinalResults.subList(4, afterFinalResults.size());
        Assertions.assertEquals(nonFinalistsPreScore, nonFinalistsAfterScore);
    }
    
    @Test
    public void testTORTieBreakOnLastRace() {
        final TimePoint now = TimePoint.now();
        final TimePoint later = now.plus(Duration.ONE_MINUTE);
        series = new ArrayList<>();
        final List<Fleet> fleets = new ArrayList<>();
        final Fleet defaultFleet = new FleetImpl("Default");
        fleets.add(defaultFleet);
        List<String> raceColumnNames = Arrays.asList("Alicante", "Cape Town");
        Series qualificationSeries = new SeriesImpl("IMOCA In-Port", /* isMedal */false,
                /* isFleetsCanRunInParallel */ true, fleets, raceColumnNames,
                /* trackedRegattaRegistry */ null);
        series.add(qualificationSeries);
        final Regatta regatta = setupRegattaWithHighPoint();
        final List<Competitor> competitors = createCompetitors(5);
        qualificationSeries.getRaceColumnByName("Alicante").setTrackedRace(defaultFleet, new MockedTrackedRaceWithStartTimeAndRanks(now, competitors));
        qualificationSeries.getRaceColumnByName("Cape Town").setTrackedRace(defaultFleet, new MockedTrackedRaceWithStartTimeAndRanks(now, competitors));
        final Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        setScore(leaderboard, competitors.get(0), 4, 4);
        setScore(leaderboard, competitors.get(1), 5, 3);
        setScore(leaderboard, competitors.get(2), 0, 5);
        setScore(leaderboard, competitors.get(3), 2, 2);
        setScore(leaderboard, competitors.get(4), 3, 0);
        assertSame(competitors.get(0), Util.get(leaderboard.getCompetitorsFromBestToWorst(later), 0));
        assertSame(competitors.get(1), Util.get(leaderboard.getCompetitorsFromBestToWorst(later), 1));
        assertSame(competitors.get(2), Util.get(leaderboard.getCompetitorsFromBestToWorst(later), 2));
        assertSame(competitors.get(3), Util.get(leaderboard.getCompetitorsFromBestToWorst(later), 3));
        assertSame(competitors.get(4), Util.get(leaderboard.getCompetitorsFromBestToWorst(later), 4));
    }

    @Test
    public void testMultipleRDGsAndSCAs() {
        final TimePoint now = TimePoint.now();
        final TimePoint later = now.plus(Duration.ONE_MINUTE);
        series = new ArrayList<>();
        final List<Fleet> fleets = new ArrayList<>();
        final Fleet defaultFleet = new FleetImpl("Default");
        fleets.add(defaultFleet);
        List<String> raceColumnNames = Arrays.asList("R1", "R2", "R3", "R4", "R5");
        Series qualificationSeries = new SeriesImpl("Default", /* isMedal */false,
                /* isFleetsCanRunInParallel */ true, fleets, raceColumnNames,
                /* trackedRegattaRegistry */ null);
        series.add(qualificationSeries);
        final Regatta regatta = setupRegattaWithLowPointWithAutomaticRDGSCA();
        final List<Competitor> competitors = createCompetitors(1);
        final Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        setScore(leaderboard, competitors.get(0),
                scoreAndIrm(4.0, null),
                scoreAndIrm(7.0, null),
                scoreAndIrm(null, MaxPointsReason.RDG),
                scoreAndIrm(null, MaxPointsReason.SCA),
                scoreAndIrm(6.0, null));
        assertEquals(17.0/3.0, leaderboard.getTotalPoints(competitors.get(0), leaderboard.getRaceColumnByName("R3"), later), EPSILON);
        assertEquals(11.0/2.0, leaderboard.getTotalPoints(competitors.get(0), leaderboard.getRaceColumnByName("R4"), later), EPSILON);
    }

    private void setScore(Leaderboard leaderboard, Competitor competitor, double... scores) {
        int i=0;
        for (final RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            if (i < scores.length) {
                leaderboard.getScoreCorrection().correctScore(competitor, raceColumn, scores[i++]);
            }
        }
    }

    private Pair<Double, MaxPointsReason> scoreAndIrm(Double score, MaxPointsReason irm) {
        return new Pair<>(score, irm);
    }
    
    @SafeVarargs
    private final void setScore(Leaderboard leaderboard, Competitor competitor, Pair<Double, MaxPointsReason>... scores) {
        int i=0;
        for (final RaceColumn raceColumn : leaderboard.getRaceColumns()) {
            if (i < scores.length) {
                if (scores[i].getA() != null) {
                    leaderboard.getScoreCorrection().correctScore(competitor, raceColumn, scores[i].getA());
                }
                if (scores[i].getB() != null) {
                    leaderboard.getScoreCorrection().setMaxPointsReason(competitor, raceColumn, scores[i].getB());
                }
                i++;
            }
        }
    }

    /**
     * In this test the preseries winner will win the first race, and should get a score of 2, all other finalists
     * should be scored with Low_Points restarting at 0 for the medal series. The non finalists score should not change
     * during the medalseries.
     */
    @Test
    public void testFirstPreseriesWinsAgain() throws NoWindException {
        series = new ArrayList<>();
        setupQualificationSeriesWithOneRaceColumn();

        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(true);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);

        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preFirst);
        m1Results.add(preSecond);
        m1Results.add(preThird);
        m1Results.add(preFourth);

        TrackedRace mDefault = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), mDefault);

        Assertions.assertEquals(2.0, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(4, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(6, leaderboard.getNetPoints(preThird, later), EPSILON);
        Assertions.assertEquals(8, leaderboard.getNetPoints(preFourth, later), EPSILON);

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * In this test the second best in the preseries wins the first two medal races, and should get a score of 2, all
     * other finalists should be scored with Low_Points restarting at 0 for the medal series. The non finalists score
     * should not change during the medalseries.
     */
    @Test
    public void testSecondPreSeriesWinsTwice() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        setupQualificationSeriesWithOneRaceColumn();
        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(true);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);
        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preSecond);
        m1Results.add(preFirst);
        m1Results.add(preThird);
        m1Results.add(preFourth);

        TimePoint m1now = new MillisecondsTimePoint(later.asMillis() + 1000);
        TimePoint m1later = new MillisecondsTimePoint(m1now.asMillis() + 1000);

        TrackedRace m1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), m1Default);

        Assertions.assertEquals(3, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(3, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(6, leaderboard.getNetPoints(preThird, later), EPSILON);
        Assertions.assertEquals(8, leaderboard.getNetPoints(preFourth, later), EPSILON);
        
        Iterable<Competitor> res = leaderboard.getCompetitorsFromBestToWorst(later);
        Assertions.assertEquals(Util.get(res, 0), preFirst);
        Assertions.assertEquals(Util.get(res, 1), preSecond);
        
        RaceColumn m2 = leaderboard.getRaceColumnByName("M2");

        ArrayList<Competitor> m2Results = new ArrayList<>();
        m2Results.add(preSecond);
        m2Results.add(preFirst);
        m2Results.add(preThird);
        m2Results.add(preFourth);

        TimePoint m2now = new MillisecondsTimePoint(m1later.asMillis() + 1000);
        TimePoint m2later = new MillisecondsTimePoint(m2now.asMillis() + 1000);
        TrackedRace m2Default = new MockedTrackedRaceWithStartTimeAndRanks(m2now, m2Results);
        m2.setTrackedRace(m1.getFleetByName("Default"), m2Default);

        Assertions.assertEquals(4.0, leaderboard.getNetPoints(preSecond, m2later), EPSILON);
        Assertions.assertEquals(5.0, leaderboard.getNetPoints(preFirst, m2later), EPSILON);
        Assertions.assertEquals(9, leaderboard.getNetPoints(preThird, m2later), EPSILON);
        Assertions.assertEquals(12, leaderboard.getNetPoints(preFourth, m2later), EPSILON);
        
        Iterable<Competitor> res2 = leaderboard.getCompetitorsFromBestToWorst(m2later);
        Assertions.assertEquals(Util.get(res2, 0), preSecond);
        Assertions.assertEquals(Util.get(res2, 1), preFirst);
        Assertions.assertEquals(Util.get(res2, 2), preThird);
        Assertions.assertEquals(Util.get(res2, 3), preFourth);

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(m2later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * In this test the best in the preseries wins the second medal race, and should get a score of 2, all other
     * finalists should be scored with Low_Points restarting at 0 for the medal series. The non finalists score should
     * not change during the medalseries. Also asserts that during tie-breaking the pre-series carry score takes
     * precedence.
     */
    @Test
    public void testFirstWinsSecondRace() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        setupQualificationSeriesWithOneRaceColumn();
        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(true);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);
        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preSecond);
        m1Results.add(preFirst);
        m1Results.add(preThird);
        m1Results.add(preFourth);

        TimePoint m1now = new MillisecondsTimePoint(later.asMillis() + 1000);
        TimePoint m1later = new MillisecondsTimePoint(m1now.asMillis() + 1000);

        TrackedRace m1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), m1Default);

        Assertions.assertEquals(3, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(3, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(6, leaderboard.getNetPoints(preThird, later), EPSILON);
        Assertions.assertEquals(8, leaderboard.getNetPoints(preFourth, later), EPSILON);
        // assert that during tie-breaking the pre-series carry score takes precedence
        Iterable<Competitor> res = leaderboard.getCompetitorsFromBestToWorst(later);
        Assertions.assertEquals(Util.get(res, 0), preFirst);
        Assertions.assertEquals(Util.get(res, 1), preSecond);

        RaceColumn m2 = leaderboard.getRaceColumnByName("M2");

        ArrayList<Competitor> m2Results = new ArrayList<>();
        m2Results.add(preFirst);
        m2Results.add(preSecond);
        m2Results.add(preThird);
        m2Results.add(preFourth);

        TimePoint m2now = new MillisecondsTimePoint(m1later.asMillis() + 1000);
        TimePoint m2later = new MillisecondsTimePoint(m2now.asMillis() + 1000);
        TrackedRace m2Default = new MockedTrackedRaceWithStartTimeAndRanks(m2now, m2Results);
        m2.setTrackedRace(m1.getFleetByName("Default"), m2Default);

        Assertions.assertEquals(4, leaderboard.getNetPoints(preFirst, m2later), EPSILON);
        Assertions.assertEquals(5, leaderboard.getNetPoints(preSecond, m2later), EPSILON);
        Assertions.assertEquals(9, leaderboard.getNetPoints(preThird, m2later), EPSILON);
        Assertions.assertEquals(12, leaderboard.getNetPoints(preFourth, m2later), EPSILON);
        
        Iterable<Competitor> res2 = leaderboard.getCompetitorsFromBestToWorst(m2later);
        Assertions.assertEquals(Util.get(res2, 0), preFirst);
        Assertions.assertEquals(Util.get(res2, 1), preSecond);
        Assertions.assertEquals(Util.get(res2, 2), preThird);
        Assertions.assertEquals(Util.get(res2, 3), preFourth);

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(m2later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * In this test the worst finalist of the qualification wins the first and second medal races, and should get a
     * score of 2, all other finalists should be scored with Low_Points restarting at 0 for the medal series. The non
     * finalists score should not change during the medalseries
     */
    @Test
    public void testLastWinsTwoRaces() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        setupQualificationSeriesWithOneRaceColumn();
        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(true);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);
        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preFourth);
        m1Results.add(preFirst);
        m1Results.add(preSecond);
        m1Results.add(preThird);

        TimePoint m1now = new MillisecondsTimePoint(later.asMillis() + 1000);
        TimePoint m1later = new MillisecondsTimePoint(m1now.asMillis() + 1000);

        TrackedRace m1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), m1Default);

        Assertions.assertEquals(5, leaderboard.getNetPoints(preFourth, later), EPSILON);
        Assertions.assertEquals(3.0, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(5, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(7, leaderboard.getNetPoints(preThird, later), EPSILON);
        
        Iterable<Competitor> res = leaderboard.getCompetitorsFromBestToWorst(later);
        Assertions.assertEquals(Util.get(res, 0), preFirst);
        Assertions.assertEquals(Util.get(res, 1), preSecond);
        Assertions.assertEquals(Util.get(res, 2), preFourth);

        RaceColumn m2 = leaderboard.getRaceColumnByName("M2");

        ArrayList<Competitor> m2Results = new ArrayList<>();
        m2Results.add(preFourth);
        m2Results.add(preFirst);
        m2Results.add(preSecond);
        m2Results.add(preThird);

        TimePoint m2now = new MillisecondsTimePoint(m1later.asMillis() + 1000);
        TimePoint m2later = new MillisecondsTimePoint(m2now.asMillis() + 1000);
        TrackedRace m2Default = new MockedTrackedRaceWithStartTimeAndRanks(m2now, m2Results);
        m2.setTrackedRace(m1.getFleetByName("Default"), m2Default);

        Assertions.assertEquals(6.0, leaderboard.getNetPoints(preFourth, m2later), EPSILON);
        Assertions.assertEquals(5.0, leaderboard.getNetPoints(preFirst, m2later), EPSILON);
        Assertions.assertEquals(8, leaderboard.getNetPoints(preSecond, m2later), EPSILON);
        Assertions.assertEquals(11, leaderboard.getNetPoints(preThird, m2later), EPSILON);
        
        Iterable<Competitor> res2 = leaderboard.getCompetitorsFromBestToWorst(m2later);
        Assertions.assertEquals(Util.get(res2, 0), preFourth);
        Assertions.assertEquals(Util.get(res2, 1), preFirst);
        Assertions.assertEquals(Util.get(res2, 2), preSecond);
        Assertions.assertEquals(Util.get(res2, 3), preThird);

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(m2later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * In this test no finalist reaches two wins before terminating (eg. due to bad weather) all finalists should be
     * scored with Low_Points restarting at 0 for the medal series. The non finalists score should not change during the
     * medalseries.
     */
    @Test
    public void noTwoWinsAbortAfterTwoRacesAndTieBreaker() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        setupQualificationSeriesWithOneRaceColumn();
        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(true);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);
        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preFourth);
        m1Results.add(preFirst);
        m1Results.add(preSecond);
        m1Results.add(preThird);

        TimePoint m1now = new MillisecondsTimePoint(later.asMillis() + 1000);
        TimePoint m1later = new MillisecondsTimePoint(m1now.asMillis() + 1000);

        TrackedRace m1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), m1Default);

        Assertions.assertEquals(5, leaderboard.getNetPoints(preFourth, later), EPSILON);
        Assertions.assertEquals(3.0, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(5, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(7, leaderboard.getNetPoints(preThird, later), EPSILON);
        
        
        Iterable<Competitor> res = leaderboard.getCompetitorsFromBestToWorst(later);
        Assertions.assertEquals(Util.get(res, 0), preFirst);
        Assertions.assertEquals(Util.get(res, 1), preSecond);
        Assertions.assertEquals(Util.get(res, 2), preFourth);

        RaceColumn m2 = leaderboard.getRaceColumnByName("M2");

        ArrayList<Competitor> m2Results = new ArrayList<>();
        m2Results.add(preThird);
        m2Results.add(preFirst);
        m2Results.add(preSecond);
        m2Results.add(preFourth);

        TimePoint m2now = new MillisecondsTimePoint(m1later.asMillis() + 1000);
        TimePoint m2later = new MillisecondsTimePoint(m2now.asMillis() + 1000);
        TrackedRace m2Default = new MockedTrackedRaceWithStartTimeAndRanks(m2now, m2Results);
        m2.setTrackedRace(m1.getFleetByName("Default"), m2Default);

        Assertions.assertEquals(9, leaderboard.getNetPoints(preFourth, m2later), EPSILON);
        Assertions.assertEquals(5.0, leaderboard.getNetPoints(preFirst, m2later), EPSILON);
        Assertions.assertEquals(8, leaderboard.getNetPoints(preSecond, m2later), EPSILON);
        Assertions.assertEquals(8, leaderboard.getNetPoints(preThird, m2later), EPSILON);

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(m2later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * Using normal lowpoints, being the best in the preseries and having an additional win in the medals, should not
     * make you the overall winner in case another finalist has a better score.
     */
    @Test
    public void negativeTestFirstPreseriesWinsAgain() throws NoWindException {
        series = new ArrayList<Series>();
        setupQualificationSeriesWithOneRaceColumn();

        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(false);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);
        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);
        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);
        List<Competitor> medalCompetitors = Util.subList(preSeriesRankResult, 0, 4);
        manuallyTransferCarry(leaderboard, medalCompetitors);
        Competitor preFirst = medalCompetitors.get(0);
        Competitor preSecond = medalCompetitors.get(1);
        Competitor preThird = medalCompetitors.get(2);
        Competitor preFourth = medalCompetitors.get(3);
        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");
        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preFirst);
        m1Results.add(preSecond);
        m1Results.add(preThird);
        m1Results.add(preFourth);
        TrackedRace mDefault = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), mDefault);

        Assertions.assertEquals(4, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(8, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(12, leaderboard.getNetPoints(preThird, later), EPSILON);
        Assertions.assertEquals(16, leaderboard.getNetPoints(preFourth, later), EPSILON);
        Assertions.assertEquals(1, leaderboard.getTotalRankOfCompetitor(preFirst, later));
        Assertions.assertEquals(2, leaderboard.getTotalRankOfCompetitor(preSecond, later));
        Assertions.assertEquals(3, leaderboard.getTotalRankOfCompetitor(preThird, later));
        Assertions.assertEquals(4, leaderboard.getTotalRankOfCompetitor(preFourth, later));

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * Using normal lowpoints, having two wins should not matter, also the racecolum factor is expected to be 2 instead
     * of 1.
     */
    @Test
    public void negativeTestSecondPreSeriesWinsTwice() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        setupQualificationSeriesWithOneRaceColumn();
        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(false);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);
        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preSecond);
        m1Results.add(preFirst);
        m1Results.add(preThird);
        m1Results.add(preFourth);

        TimePoint m1now = new MillisecondsTimePoint(later.asMillis() + 1000);
        TimePoint m1later = new MillisecondsTimePoint(m1now.asMillis() + 1000);

        TrackedRace m1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), m1Default);

        Assertions.assertEquals(6, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(6.0, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(12, leaderboard.getNetPoints(preThird, later), EPSILON);
        Assertions.assertEquals(16, leaderboard.getNetPoints(preFourth, later), EPSILON);

        RaceColumn m2 = leaderboard.getRaceColumnByName("M2");

        ArrayList<Competitor> m2Results = new ArrayList<>();
        m2Results.add(preSecond);
        m2Results.add(preFirst);
        m2Results.add(preThird);
        m2Results.add(preFourth);

        TimePoint m2now = new MillisecondsTimePoint(m1later.asMillis() + 1000);
        TimePoint m2later = new MillisecondsTimePoint(m2now.asMillis() + 1000);
        TrackedRace m2Default = new MockedTrackedRaceWithStartTimeAndRanks(m2now, m2Results);
        m2.setTrackedRace(m1.getFleetByName("Default"), m2Default);

        Assertions.assertEquals(8, leaderboard.getNetPoints(preSecond, m2later), EPSILON);
        Assertions.assertEquals(10.0, leaderboard.getNetPoints(preFirst, m2later), EPSILON);
        Assertions.assertEquals(18, leaderboard.getNetPoints(preThird, m2later), EPSILON);
        Assertions.assertEquals(24, leaderboard.getNetPoints(preFourth, m2later), EPSILON);
        Assertions.assertEquals(1, leaderboard.getTotalRankOfCompetitor(preSecond, m2later));
        Assertions.assertEquals(2, leaderboard.getTotalRankOfCompetitor(preFirst, m2later));
        Assertions.assertEquals(3, leaderboard.getTotalRankOfCompetitor(preThird, m2later));
        Assertions.assertEquals(4, leaderboard.getTotalRankOfCompetitor(preFourth, m2later));
        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(m2later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * Using normal lowpoints, having two wins should not matter, also the racecolum factor is expected to be 2 instead
     * of 1.
     */
    @Test
    public void negativeTestFirstWinsSecondRace() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        setupQualificationSeriesWithOneRaceColumn();
        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(false);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);
        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preSecond);
        m1Results.add(preFirst);
        m1Results.add(preThird);
        m1Results.add(preFourth);

        TimePoint m1now = new MillisecondsTimePoint(later.asMillis() + 1000);
        TimePoint m1later = new MillisecondsTimePoint(m1now.asMillis() + 1000);

        TrackedRace m1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), m1Default);

        Assertions.assertEquals(6, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(6, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(12, leaderboard.getNetPoints(preThird, later), EPSILON);
        Assertions.assertEquals(16, leaderboard.getNetPoints(preFourth, later), EPSILON);
        
        
        Iterable<Competitor> res = leaderboard.getCompetitorsFromBestToWorst(later);
        Assertions.assertEquals(Util.get(res, 0), preSecond);
        Assertions.assertEquals(Util.get(res, 1), preFirst);

        RaceColumn m2 = leaderboard.getRaceColumnByName("M2");

        ArrayList<Competitor> m2Results = new ArrayList<>();
        m2Results.add(preFirst);
        m2Results.add(preSecond);
        m2Results.add(preThird);
        m2Results.add(preFourth);

        TimePoint m2now = new MillisecondsTimePoint(m1later.asMillis() + 1000);
        TimePoint m2later = new MillisecondsTimePoint(m2now.asMillis() + 1000);
        TrackedRace m2Default = new MockedTrackedRaceWithStartTimeAndRanks(m2now, m2Results);
        m2.setTrackedRace(m1.getFleetByName("Default"), m2Default);

        Assertions.assertEquals(8, leaderboard.getNetPoints(preFirst, m2later), EPSILON);
        Assertions.assertEquals(10, leaderboard.getNetPoints(preSecond, m2later), EPSILON);
        Assertions.assertEquals(18, leaderboard.getNetPoints(preThird, m2later), EPSILON);
        Assertions.assertEquals(24, leaderboard.getNetPoints(preFourth, m2later), EPSILON);

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(m2later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * Using normal lowpoints, the racecolum factor is expected to be 2 instead of 1.
     */
    @Test
    public void negativeTestLastWinsTwoRaces() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        setupQualificationSeriesWithOneRaceColumn();
        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(false);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);

        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preFourth);
        m1Results.add(preFirst);
        m1Results.add(preSecond);
        m1Results.add(preThird);

        TimePoint m1now = new MillisecondsTimePoint(later.asMillis() + 1000);
        TimePoint m1later = new MillisecondsTimePoint(m1now.asMillis() + 1000);

        TrackedRace m1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), m1Default);

        Assertions.assertEquals(10, leaderboard.getNetPoints(preFourth, later), EPSILON);
        Assertions.assertEquals(6.0, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(10, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(14, leaderboard.getNetPoints(preThird, later), EPSILON);
        
        Iterable<Competitor> res = leaderboard.getCompetitorsFromBestToWorst(later);
        Assertions.assertEquals(Util.get(res, 0), preFirst);
        Assertions.assertEquals(Util.get(res, 1), preFourth);
        Assertions.assertEquals(Util.get(res, 2), preSecond);

        RaceColumn m2 = leaderboard.getRaceColumnByName("M2");

        ArrayList<Competitor> m2Results = new ArrayList<>();
        m2Results.add(preFourth);
        m2Results.add(preFirst);
        m2Results.add(preSecond);
        m2Results.add(preThird);

        TimePoint m2now = new MillisecondsTimePoint(m1later.asMillis() + 1000);
        TimePoint m2later = new MillisecondsTimePoint(m2now.asMillis() + 1000);
        TrackedRace m2Default = new MockedTrackedRaceWithStartTimeAndRanks(m2now, m2Results);
        m2.setTrackedRace(m1.getFleetByName("Default"), m2Default);

        Assertions.assertEquals(12, leaderboard.getNetPoints(preFourth, m2later), EPSILON);
        Assertions.assertEquals(10, leaderboard.getNetPoints(preFirst, m2later), EPSILON);
        Assertions.assertEquals(16, leaderboard.getNetPoints(preSecond, m2later), EPSILON);
        Assertions.assertEquals(22, leaderboard.getNetPoints(preThird, m2later), EPSILON);

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(m2later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }

    /**
     * Using normal lowpoints, tie braking is done with manual carry decimals.
     */
    @Test
    public void negativeNoTwoWinsAbortAfterTwoRacesAndTieBreaker() throws NoWindException {
        series = new ArrayList<Series>();
        // -------- qualification series ------------
        setupQualificationSeriesWithOneRaceColumn();
        setupMedalSeriesWithCarryOverAndFourRaceColumns();
        Regatta regatta = setupRegatta(false);
        List<Competitor> competitors = createCompetitors(12);
        List<Competitor> yellow = new ArrayList<>(competitors.subList(0, 6));
        List<Competitor> blue = new ArrayList<>(competitors);
        blue.removeAll(yellow);
        Collections.shuffle(yellow);
        Collections.shuffle(blue);

        Leaderboard leaderboard = createLeaderboard(regatta, /* discarding thresholds */ new int[0]);
        TimePoint now = MillisecondsTimePoint.now();
        TimePoint later = new MillisecondsTimePoint(now.asMillis() + 1000);
        executePreSeries(yellow, blue, now);

        Iterable<Competitor> preSeriesRankResult = leaderboard.getCompetitorsFromBestToWorst(later);
        List<Competitor> medalCompetitorsBestToWorst = Util.subList(preSeriesRankResult, 0, 4);
        List<Pair<Competitor, Double>> preSeriesScoreRankResult = createCompetitorResultForTimestamp(later,
                leaderboard);
        manuallyTransferCarry(leaderboard, medalCompetitorsBestToWorst);

        Competitor preFirst = medalCompetitorsBestToWorst.get(0);
        Competitor preSecond = medalCompetitorsBestToWorst.get(1);
        Competitor preThird = medalCompetitorsBestToWorst.get(2);
        Competitor preFourth = medalCompetitorsBestToWorst.get(3);

        RaceColumn m1 = leaderboard.getRaceColumnByName("M1");

        ArrayList<Competitor> m1Results = new ArrayList<>();
        m1Results.add(preFourth);
        m1Results.add(preFirst);
        m1Results.add(preSecond);
        m1Results.add(preThird);

        TimePoint m1now = new MillisecondsTimePoint(later.asMillis() + 1000);
        TimePoint m1later = new MillisecondsTimePoint(m1now.asMillis() + 1000);

        TrackedRace m1Default = new MockedTrackedRaceWithStartTimeAndRanks(now, m1Results);
        m1.setTrackedRace(m1.getFleetByName("Default"), m1Default);

        Assertions.assertEquals(10, leaderboard.getNetPoints(preFourth, later), EPSILON);
        Assertions.assertEquals(6.0, leaderboard.getNetPoints(preFirst, later), EPSILON);
        Assertions.assertEquals(10, leaderboard.getNetPoints(preSecond, later), EPSILON);
        Assertions.assertEquals(14, leaderboard.getNetPoints(preThird, later), EPSILON);
        
        Iterable<Competitor> res = leaderboard.getCompetitorsFromBestToWorst(later);
        Assertions.assertEquals(Util.get(res, 0), preFirst);
        Assertions.assertEquals(Util.get(res, 1), preFourth);
        Assertions.assertEquals(Util.get(res, 2), preSecond);

        RaceColumn m2 = leaderboard.getRaceColumnByName("M2");

        ArrayList<Competitor> m2Results = new ArrayList<>();
        m2Results.add(preThird);
        m2Results.add(preFirst);
        m2Results.add(preSecond);
        m2Results.add(preFourth);

        TimePoint m2now = new MillisecondsTimePoint(m1later.asMillis() + 1000);
        TimePoint m2later = new MillisecondsTimePoint(m2now.asMillis() + 1000);
        TrackedRace m2Default = new MockedTrackedRaceWithStartTimeAndRanks(m2now, m2Results);
        m2.setTrackedRace(m1.getFleetByName("Default"), m2Default);

        Assertions.assertEquals(18, leaderboard.getNetPoints(preFourth, m2later), EPSILON);
        Assertions.assertEquals(10, leaderboard.getNetPoints(preFirst, m2later), EPSILON);
        Assertions.assertEquals(16, leaderboard.getNetPoints(preSecond, m2later), EPSILON);
        Assertions.assertEquals(16, leaderboard.getNetPoints(preThird, m2later), EPSILON);
        
        Iterable<Competitor> res2 = leaderboard.getCompetitorsFromBestToWorst(m2later);
        Assertions.assertEquals(Util.get(res2, 0), preFirst);
        Assertions.assertEquals(Util.get(res2, 1), preThird);
        Assertions.assertEquals(Util.get(res2, 2), preSecond);
        Assertions.assertEquals(Util.get(res2, 3), preFourth);

        List<Pair<Competitor, Double>> afterFinalResults = createCompetitorResultForTimestamp(m2later, leaderboard);
        assertNonFinalistsAreBehindFinalistsAndNotChanged(preSeriesScoreRankResult, afterFinalResults);
    }
}
