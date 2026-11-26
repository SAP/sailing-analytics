package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.scoring.AdditionalScoringInformationType;
import com.sap.sailing.domain.abstractlog.race.scoring.impl.RaceLogAdditionalScoringInformationEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.RaceColumnInSeriesImpl;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardTotalRankComparator;
import com.sap.sailing.domain.leaderboard.impl.RegattaLeaderboardWithOtherTieBreakingLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.FlexibleMetaLeaderboard;
import com.sap.sailing.domain.test.mock.MockedTrackedRaceWithStartTimeAndRanks;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/** Rule-derived boundaries and legacy characterizations, separate from corrected production replay. */
class ScoringSchemeInteractionTest extends LeaderboardScoringAndRankingTestBase {
    private static final TimePoint START = new MillisecondsTimePoint(/* millis */ 1000);
    private static final TimePoint AFTER = new MillisecondsTimePoint(/* millis */ 2000);
    private static final double TOLERANCE = 0.00000001;

    @BeforeAll
    static void requireEnabledJavaAssertions() {
        assertTrue(RaceColumnInSeriesImpl.class.desiredAssertionStatus(),
                "Run scoring regression tests with JVM assertions enabled (-ea)");
    }

    @TestFactory
    Stream<DynamicTest> fixedEightDoesNotInterpolateIncompleteFleets() {
        return Stream.of(/* sizes */ 3, /* sizes */ 8, /* sizes */ 10).map((final Integer size) -> DynamicTest.dynamicTest("fixed eight / " + size, () -> {
            final List<Competitor> competitors = createCompetitors(size);
            final FlexibleLeaderboard board = flexible("Fixed eight", ScoringSchemeType.HIGH_POINT_WINNER_GETS_EIGHT);
            final RaceColumn column = addRace(board, "R1", competitors);
            assertEquals(/* expected */ 8.0, board.getTotalPoints(competitors.get(/* index */ 0), column, AFTER), TOLERANCE);
            assertEquals(size == 3 ? 6.0 : 1.0, board.getTotalPoints(competitors.get(size - 1), column, AFTER), TOLERANCE);
            final Competitor first = competitors.get(/* index */ 0);
            board.getScoreCorrection().setMaxPointsReason(first, column, MaxPointsReason.DNF);
            assertEquals(/* expected */ 0.0, board.getTotalPoints(first, column, AFTER), TOLERANCE);
            board.getScoreCorrection().correctScore(first, column, /* correctedScore */ 2.5);
            assertEquals(/* expected */ 2.5, board.getTotalPoints(first, column, AFTER), TOLERANCE);
            assertEquals(MaxPointsReason.DNF, board.getMaxPointsReason(first, column, AFTER));
        }));
    }

    @Test
    void leagueOverallUsesChildPointSumsBeforeLastActRank() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
        final Competitor a = competitors.get(/* index */ 0);
        final Competitor b = competitors.get(/* index */ 1);
        final FlexibleLeaderboard first = flexible("First", ScoringSchemeType.LOW_POINT);
        final RaceColumn firstRace = addRace(first, "R1", competitors);
        first.getScoreCorrection().correctScore(b, firstRace, /* correctedScore */ 4.0);
        final FlexibleLeaderboard last = flexible("Last", ScoringSchemeType.LOW_POINT);
        final RaceColumn lastRace = addRace(last, "R1", Arrays.asList(b, a));
        last.getScoreCorrection().correctScore(a, lastRace, /* correctedScore */ 10.0);
        last.getScoreCorrection().correctScore(b, lastRace, /* correctedScore */ 2.0);
        final FlexibleMetaLeaderboard overall = overall(ScoringSchemeType.LOW_POINT_LEAGUE_OVERALL, first, last);
        assertEquals(/* expected */ 3.0, overall.getNetPoints(a, AFTER), TOLERANCE);
        assertEquals(/* expected */ 3.0, overall.getNetPoints(b, AFTER), TOLERANCE);
        assertOrder(overall, b, a); // Child totals 6 versus 11.
        last.getScoreCorrection().correctScore(a, lastRace, /* correctedScore */ 5.0);
        assertOrder(overall, b, a); // Child totals both 6; B wins the last act.
        last.getScoreCorrection().correctScore(a, lastRace, /* correctedScore */ 4.0);
        assertOrder(overall, a, b); // Child totals 5 versus 6, despite B winning the last act.
    }

    @Test
    void nestedNetPointsOverallUsesWeightedChildNetPointsAndReactsToCorrections() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
        final Competitor a = competitors.get(/* index */ 0);
        final Competitor b = competitors.get(/* index */ 1);
        final FlexibleLeaderboard first = flexible("First", ScoringSchemeType.LOW_POINT);
        final RaceColumn firstRace = addRace(first, "R1", competitors);
        first.getScoreCorrection().correctScore(b, firstRace, /* correctedScore */ 4.0);
        final FlexibleLeaderboard last = flexible("Last", ScoringSchemeType.LOW_POINT);
        final RaceColumn lastRace = addRace(last, "R1", Arrays.asList(b, a));
        last.getScoreCorrection().correctScore(a, lastRace, /* correctedScore */ 10.0);
        last.getScoreCorrection().correctScore(b, lastRace, /* correctedScore */ 2.0);
        final FlexibleMetaLeaderboard overall = overall(ScoringSchemeType.LOW_POINT_OVERALL_USING_NET_POINTS, first, last);
        overall.getRaceColumnByName(last.getName()).setFactor(/* factor */ 2.0);
        final FlexibleMetaLeaderboard nested = overall(ScoringSchemeType.LOW_POINT_OVERALL_USING_NET_POINTS, overall);
        assertEquals(/* expected */ 21.0, nested.getNetPoints(a, AFTER), TOLERANCE);
        assertEquals(/* expected */ 8.0, nested.getNetPoints(b, AFTER), TOLERANCE);
        assertOrder(nested, b, a);
        last.getScoreCorrection().correctScore(a, lastRace, /* correctedScore */ 2.5);
        assertEquals(/* expected */ 6.0, nested.getNetPoints(a, AFTER), TOLERANCE);
        assertOrder(nested, a, b);
    }

    @TestFactory
    Stream<DynamicTest> essOverallFiveActEligibilityWithWeightedColumns() {
        return Stream.of(ScoringSchemeType.HIGH_POINT_ESS_OVERALL, ScoringSchemeType.HIGH_POINT_ESS_OVERALL_12)
                .map((final ScoringSchemeType type) -> DynamicTest.dynamicTest(type.name(), () -> {
                    final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
                    final Competitor a = competitors.get(/* index */ 0);
                    final Competitor b = competitors.get(/* index */ 1);
                    final List<Leaderboard> acts = new ArrayList<>();
                    for (int i = 0; i < 5; i++) {
                        final FlexibleLeaderboard act = flexible("Act " + i, ScoringSchemeType.HIGH_POINT);
                        addRace(act, "R1", i < 4 ? competitors : Collections.singletonList(b));
                        acts.add(act);
                    }
                    final FlexibleMetaLeaderboard overall = overall(type, acts.toArray(new Leaderboard[0]));
                    for (int i = 0; i < acts.size(); i++) {
                        overall.getRaceColumnByName(acts.get(i).getName()).setFactor(i < 4 ? 2.0 : 0.5);
                    }
                    assertTrue(overall.getNetPoints(a, AFTER) > overall.getNetPoints(b, AFTER));
                    assertOrder(overall, b, a); // Four acts are insufficient despite the larger points sum.
                    final RaceColumn last = acts.get(/* index */ 4).getRaceColumnByName("R1");
                    last.setTrackedRace(last.getFleets().iterator().next(), new MockedTrackedRaceWithStartTimeAndRanks(START, competitors));
                    assertOrder(overall, a, b);
                    final double maximum = type == ScoringSchemeType.HIGH_POINT_ESS_OVERALL ? 10 : 12;
                    assertEquals(maximum * 8.5, overall.getNetPoints(a, AFTER), TOLERANCE);
                }));
    }

    @Test
    void externalTieBreakerFollowsTrackedLastFinishThenQualification() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 3);
        final Competitor a = competitors.get(/* index */ 0);
        final Competitor b = competitors.get(/* index */ 1);
        final Competitor c = competitors.get(/* index */ 2);
        final Regatta qualificationRegatta = simpleRegatta("Qualification", ScoringSchemeType.LOW_POINT);
        final RegattaLeaderboard qualification = createLeaderboard(qualificationRegatta, new int[0]);
        attach(qualification, competitors);
        final Regatta finals = simpleRegatta("Final", ScoringSchemeType.HIGH_POINT_BY_WINS_TIES_LASTLY_BROKEN_BY_OTHER_LEADERBOARD);
        final RegattaLeaderboard board = new RegattaLeaderboardWithOtherTieBreakingLeaderboardImpl(finals,
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]), () -> qualification);
        attach(board, Arrays.asList(c, b, a));
        assertOrder(board, c, b, a); // A and B both have zero wins; tracked rank beats Qualification rank.
        final RaceColumn column = board.getRaceColumnByName("R1");
        column.enableCompetitorRegistrationOnRaceLog(column.getFleets().iterator().next());
        for (final Competitor competitor : competitors) {
            column.registerCompetitor(competitor, ((CompetitorWithBoat) competitor).getBoat(), column.getFleets().iterator().next());
            board.getScoreCorrection().correctScore(competitor, column, competitor == c ? 1.0 : 0.0);
        }
        column.releaseTrackedRace(column.getFleets().iterator().next());
        assertOrder(board, c, a, b); // No tracked ranks; use the actual other leaderboard.
        qualification.getScoreCorrection().correctScore(b, qualification.getRaceColumnByName("R1"), /* correctedScore */ 0.5);
        assertOrder(board, c, b, a);
    }

    @Test
    void automaticRedressIncludesDiscardedScoresButScaStopsAtItsColumn() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 3);
        final Competitor a = competitors.get(/* index */ 0);
        final FlexibleLeaderboard board = new FlexibleLeaderboardImpl("Redress",
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3 }),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_WITH_AUTOMATIC_RDG), /* courseArea */ null);
        addRace(board, "R1", competitors);
        final RaceColumn redress = addRace(board, "R2", competitors);
        final RaceColumn worst = addRace(board, "R3", Arrays.asList(competitors.get(/* index */ 1), competitors.get(/* index */ 2), a));
        board.getScoreCorrection().setMaxPointsReason(a, redress, MaxPointsReason.RDG);
        assertEquals(/* expected */ 2.0, board.getTotalPoints(a, redress, AFTER), TOLERANCE);
        assertTrue(board.getResultDiscardingRule().getDiscardedRaceColumns(a, board, board.getRaceColumns(), AFTER,
                board.getScoringScheme()).contains(worst));
        board.getScoreCorrection().setMaxPointsReason(a, redress, MaxPointsReason.SCA);
        assertEquals(/* expected */ 1.0, board.getTotalPoints(a, redress, AFTER), TOLERANCE);
        board.getScoreCorrection().correctScore(a, redress, /* correctedScore */ 1.625);
        assertEquals(/* expected */ 1.625, board.getTotalPoints(a, redress, AFTER), TOLERANCE);
        assertEquals(MaxPointsReason.SCA, board.getMaxPointsReason(a, redress, AFTER));
    }

    @TestFactory
    Stream<DynamicTest> ess2017PenaltiesUseTheSameRaceMaximumAsFinishers() {
        return Stream.of(MaxPointsReason.DNS, MaxPointsReason.DNF, MaxPointsReason.RET,
                MaxPointsReason.DSQ, MaxPointsReason.OCS).map((final MaxPointsReason reason) ->
                DynamicTest.dynamicTest("ESS 2017 / " + reason, () -> {
                    final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 8);
                    final FlexibleLeaderboard board = flexible("ESS 2017", ScoringSchemeType.HIGH_POINT_FIRST_GETS_TWELVE_OR_EIGHT_2017);
                    final RaceColumn normal = addRace(board, "Normal", competitors);
                    final RaceColumn reduced = addRace(board, "Reduced", competitors);
                    final RaceLog log = reduced.getRaceLog(reduced.getFleets().iterator().next());
                    log.add(new RaceLogAdditionalScoringInformationEventImpl(START, AFTER,
                            new LogEventAuthorImpl("Fixture", /* priority */ 1), "eight", /* passId */ 0,
                            AdditionalScoringInformationType.MAX_POINTS_DECREASE_MAX_SCORE));
                    final Competitor penalized = competitors.get(/* index */ 6);
                    final Competitor disqualified = competitors.get(/* index */ 7);
                    for (final RaceColumn column : Arrays.asList(normal, reduced)) {
                        board.getScoreCorrection().setMaxPointsReason(penalized, column, reason);
                        board.getScoreCorrection().setMaxPointsReason(disqualified, column, MaxPointsReason.DSQ);
                        final double maximum = column == normal ? 12.0 : 8.0;
                        final double lastFinisher = column == normal ? 7.0 : 3.0;
                        final double penalty = reason == MaxPointsReason.DSQ || reason == MaxPointsReason.OCS
                                ? lastFinisher - 2 : lastFinisher - 1;
                        assertEquals(maximum, board.getTotalPoints(competitors.get(/* index */ 0), column, AFTER), TOLERANCE);
                        assertEquals(lastFinisher, board.getTotalPoints(competitors.get(/* index */ 5), column, AFTER), TOLERANCE);
                        assertEquals(penalty, board.getTotalPoints(penalized, column, AFTER), TOLERANCE);
                        assertEquals(lastFinisher - 2, board.getTotalPoints(disqualified, column, AFTER), TOLERANCE);
                        column.setFactor(/* factor */ 2.0);
                        assertEquals(penalty * 2, board.getNetPoints(penalized, column, AFTER, Collections.emptySet()), TOLERANCE);
                    }
                    // A fixed award still takes precedence, while removing it restores the calculated penalty.
                    board.getScoreCorrection().correctScore(disqualified, reduced, /* correctedScore */ 2.5);
                    assertEquals(/* expected */ 2.5, board.getTotalPoints(disqualified, reduced, AFTER), TOLERANCE);
                    board.getScoreCorrection().uncorrectScore(disqualified, reduced);
                    assertEquals(/* expected */ 1.0, board.getTotalPoints(disqualified, reduced, AFTER), TOLERANCE);
                }));
    }

    @Test
    void thirdCompetitorChangesTheTiedCohortGraphAndCorrectionsRebuildIt() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 3);
        final Competitor a = competitors.get(/* index */ 0);
        final Competitor b = competitors.get(/* index */ 1);
        final Competitor c = competitors.get(/* index */ 2);
        final FlexibleLeaderboard board = flexible("Cohort", ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE);
        addRace(board, "AB", Arrays.asList(a, b));
        addRace(board, "BC", Arrays.asList(b, c));
        final RaceColumn ca = addRace(board, "CA", Arrays.asList(c, a));
        for (final Competitor first : competitors) {
            for (final Competitor second : competitors) {
                assertEquals(/* expected */ 0, compareBetter(board, first, second, board.getRaceColumns(), Collections.emptyMap()));
            }
        }
        // A beats B directly, but C closes the cycle. Every input permutation must yield the same display order.
        for (int offset = 0; offset < competitors.size(); offset++) {
            for (final boolean reverse : Arrays.asList(/* values */ false, /* values */ true)) {
                final List<Competitor> input = new ArrayList<>(competitors);
                Collections.rotate(input, offset);
                if (reverse) {
                    Collections.reverse(input);
                }
                input.sort(new LeaderboardTotalRankComparator(board, AFTER, board.getScoringScheme(),
                        /* nullScoresAreBetter */ false, board.getRaceColumns(), /* cache */ null));
                assertEquals(competitors, input);
            }
        }
        board.getScoreCorrection().correctScore(c, ca, /* correctedScore */ 2.0);
        assertTrue(compareBetter(board, a, b, board.getRaceColumns(), Collections.emptyMap()) < 0);
        assertOrder(board, c, a, b);
        board.getScoreCorrection().correctScore(c, ca, /* correctedScore */ 1.0);
        assertEquals(/* expected */ 0, compareBetter(board, a, b, board.getRaceColumns(), Collections.emptyMap()));
        assertOrder(board, a, b, c);
    }

    @Test
    void graphRespectsColumnAndTimeScopesAndPenaltyChanges() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 3);
        final Competitor a = competitors.get(/* index */ 0);
        final Competitor b = competitors.get(/* index */ 1);
        final Competitor c = competitors.get(/* index */ 2);
        final FlexibleLeaderboard board = flexible("Scoped graph", ScoringSchemeType.HIGH_POINT_FIRST_GETS_ONE);
        final RaceColumn ab = addRace(board, "AB", Arrays.asList(a, b));
        final RaceColumn bc = addRace(board, "BC", Arrays.asList(b, c));
        final RaceColumn ca = board.addRace(new MockedTrackedRaceWithStartTimeAndRanks(
                new MillisecondsTimePoint(/* millis */ 3000), Arrays.asList(c, a)), "CA", /* medalRace */ false);
        final TimePoint later = new MillisecondsTimePoint(/* millis */ 4000);
        assertNull(board.getTotalPoints(c, ca, AFTER));
        assertTrue(compareBetter(board, a, b, board.getRaceColumns(), Collections.emptyMap(), AFTER) < 0);
        assertEquals(/* expected */ 0, compareBetter(board, a, b, board.getRaceColumns(), Collections.emptyMap(), later));
        assertTrue(compareBetter(board, a, b, Arrays.asList(ab, bc), Collections.emptyMap(), later) < 0);
        board.getScoreCorrection().setMaxPointsReason(c, ca, MaxPointsReason.DSQ);
        assertEquals(/* expected */ -1.0, board.getTotalPoints(c, ca, later), TOLERANCE);
        assertEquals(competitors, Util.asList(board.getCompetitorsFromBestToWorst(later)));
        board.getScoreCorrection().correctScore(c, ca, /* correctedScore */ 0.0);
        assertEquals(/* expected */ 0.0, board.getTotalPoints(c, ca, later), TOLERANCE);
        assertEquals(MaxPointsReason.DSQ, board.getMaxPointsReason(c, ca, later));
        // The DSQ also promotes A's raw finish rank; restore it before comparing an equal-total cohort again.
        assertEquals(/* expected */ 1.0, board.getTotalPoints(a, ca, later), TOLERANCE);
        board.getScoreCorrection().setMaxPointsReason(c, ca, MaxPointsReason.NONE);
        board.getScoreCorrection().correctScore(c, ca, /* correctedScore */ 1.0);
        assertEquals(/* expected */ 0.0, board.getTotalPoints(a, ca, later), TOLERANCE);
        assertEquals(/* expected */ 0, compareBetter(board, a, b, board.getRaceColumns(), Collections.emptyMap(), later));
    }

    @Test
    void scopedA8UsesExistingDiscardsWithoutComparingSubsetSums() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
        final Competitor a = competitors.get(/* index */ 0);
        final Competitor b = competitors.get(/* index */ 1);
        final FlexibleLeaderboard board = new FlexibleLeaderboardImpl("A8 scope",
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3 }),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* courseArea */ null);
        final RaceColumn first = addRace(board, "F1", competitors);
        final RaceColumn last = addRace(board, "F2", competitors);
        final RaceColumn outside = addRace(board, "Q", competitors);
        board.getScoreCorrection().correctScore(a, first, /* correctedScore */ 1.0);
        board.getScoreCorrection().correctScore(a, last, /* correctedScore */ 8.0);
        board.getScoreCorrection().correctScore(a, outside, /* correctedScore */ 9.0);
        board.getScoreCorrection().correctScore(b, first, /* correctedScore */ 2.0);
        board.getScoreCorrection().correctScore(b, last, /* correctedScore */ 3.0);
        board.getScoreCorrection().correctScore(b, outside, /* correctedScore */ 10.0);
        final Map<Competitor, Set<RaceColumn>> discards = new HashMap<>();
        for (final Competitor competitor : competitors) {
            discards.put(competitor, board.getResultDiscardingRule().getDiscardedRaceColumns(competitor, board,
                    board.getRaceColumns(), AFTER, board.getScoringScheme()));
            assertEquals(Collections.singleton(outside), discards.get(competitor));
        }
        final List<RaceColumn> scope = Arrays.asList(first, last);
        // 1,8 wins A8.1 against 2,3 despite having the larger subset sum.
        assertTrue(compareBetter(board, a, b, scope, discards) < 0);
        board.getScoreCorrection().correctScore(a, first, /* correctedScore */ 8.0);
        board.getScoreCorrection().correctScore(a, last, /* correctedScore */ 1.0);
        board.getScoreCorrection().correctScore(a, outside, /* correctedScore */ 2.0);
        board.getScoreCorrection().correctScore(b, first, /* correctedScore */ 9.0);
        discards.put(a, board.getResultDiscardingRule().getDiscardedRaceColumns(a, board, board.getRaceColumns(), AFTER,
                board.getScoringScheme()));
        assertEquals(Collections.singleton(first), discards.get(a));
        assertTrue(compareBetter(board, a, b, Collections.singletonList(first), discards) > 0);
        // Recomputing discards for that single-column scope would wrongly reintroduce A's excluded 8.
        final Map<Competitor, Set<RaceColumn>> recomputed = new HashMap<>();
        for (final Competitor competitor : competitors) {
            recomputed.put(competitor, Collections.emptySet());
        }
        assertTrue(compareBetter(board, a, b, Collections.singletonList(first), recomputed) < 0);
        assertEquals(Collections.singleton(first), discards.get(a));
    }

    @Test
    void halfWeightedRaceMustNotBeInverseWeightedAgainForA8() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 8);
        final Competitor a = competitors.get(/* index */ 5);
        final Competitor b = competitors.get(/* index */ 7);
        final Competitor c = competitors.get(/* index */ 6);
        final FlexibleLeaderboard board = flexible("Half-weighted A8", ScoringSchemeType.LOW_POINT);
        final RaceColumn halfWeighted = addRace(board, "Q", competitors);
        halfWeighted.setFactor(/* factor */ 0.5);
        final List<Competitor> finalOrder = new ArrayList<>(competitors.subList(/* fromIndex */ 0, /* toIndex */ 5));
        finalOrder.addAll(Arrays.asList(b, a, c));
        final RaceColumn fullWeighted = addRace(board, "F", finalOrder);
        // Real sixth/eighth and seventh/sixth finishes; no corrections, discards, carry or Medal criteria.
        assertEquals(/* expected */ 6.0, board.getTotalPoints(a, halfWeighted, AFTER), TOLERANCE);
        assertEquals(/* expected */ 8.0, board.getTotalPoints(b, halfWeighted, AFTER), TOLERANCE);
        assertEquals(/* expected */ 7.0, board.getTotalPoints(a, fullWeighted, AFTER), TOLERANCE);
        assertEquals(/* expected */ 6.0, board.getTotalPoints(b, fullWeighted, AFTER), TOLERANCE);
        assertEquals(/* expected */ 3.0, board.getNetPoints(a, halfWeighted, AFTER, Collections.emptySet()), TOLERANCE);
        assertEquals(/* expected */ 10.0, board.getNetPoints(a, AFTER), TOLERANCE);
        assertEquals(/* expected */ 10.0, board.getNetPoints(b, AFTER), TOLERANCE);
        // A8.1 favors A: sorted original scores (6,7) beat (6,8). Weighted scores (3,7) also beat (4,6).
        // The former inverse scaling compared A=(7,12) with B=(6,16), wrongly preferring B.
        final List<Competitor> expected = new ArrayList<>(competitors.subList(/* fromIndex */ 0, /* toIndex */ 5));
        expected.addAll(Arrays.asList(a, b, c));
        assertOrder(board, expected.toArray(new Competitor[0]));
    }

    @TestFactory
    Stream<DynamicTest> a81WeightsOriginalScoresExactlyOnceForOrdinaryAndMedalRaces() {
        return Stream.of(/* medalRace */ false, /* medalRace */ true).flatMap((final Boolean medalRace) ->
                Stream.of(/* factor */ 0.5, /* factor */ 2.0).map((final Double factor) ->
                        DynamicTest.dynamicTest("A8.1 / Medal=" + medalRace + " / factor=" + factor, () -> {
            final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
            final Competitor a = competitors.get(/* index */ 0);
            final Competitor b = competitors.get(/* index */ 1);
            final FlexibleLeaderboard board = flexible("Weighted A8.1", ScoringSchemeType.LOW_POINT);
            final RaceColumn first = board.addRace(new MockedTrackedRaceWithStartTimeAndRanks(START, competitors), "R1", medalRace);
            final RaceColumn last = board.addRace(new MockedTrackedRaceWithStartTimeAndRanks(START, competitors), "R2", medalRace);
            first.setFactor(factor);
            last.setFactor(/* factor */ 1.0);
            board.getScoreCorrection().correctScore(a, first, factor == 0.5 ? 4.0 : 1.0);
            board.getScoreCorrection().correctScore(b, first, factor == 0.5 ? 8.0 : 2.0);
            board.getScoreCorrection().correctScore(a, last, /* correctedScore */ 5.0);
            board.getScoreCorrection().correctScore(b, last, /* correctedScore */ 3.0);
            assertEquals(/* expected */ 7.0, board.getNetPoints(a, AFTER), TOLERANCE);
            assertEquals(/* expected */ 7.0, board.getNetPoints(b, AFTER), TOLERANCE);
            // In both cases weighted lists (2,5) beat (3,4), despite B's better last race.
            // Half weighting must not compare original (4,5) against (3,8).
            // Double weighting must not scale again to (4,5) against (3,8), including on the Medal path.
            assertOrder(board, a, b);
        })));
    }

    @TestFactory
    Stream<DynamicTest> a81PreservesOneWhenScalingWithDiscardsOrMedalScores() {
        return Stream.of(/* medalRace */ false, /* medalRace */ true).map((final Boolean medalRace) ->
                DynamicTest.dynamicTest("One stays one in A8.1 / Medal=" + medalRace, () -> {
            final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
            final Competitor a = competitors.get(/* index */ 0);
            final Competitor b = competitors.get(/* index */ 1);
            final Regatta regatta = createRegatta(/* qualifying */ 3, new String[] { "Default" }, /* final */ 0,
                    new String[] { "Default" }, /* medal */ true, /* numberOfMedalRaces */ 3, "One stays one in A8",
                    DomainFactory.INSTANCE.getOrCreateBoatClass("Fixture", /* typicallyStartsUpwind */ true),
                    DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
            final RegattaLeaderboard board = createLeaderboard(regatta, new int[0]);
            final String prefix = medalRace ? "M" : "Q";
            final RaceColumn first = board.getRaceColumnByName(prefix + "1");
            final RaceColumn last = board.getRaceColumnByName(prefix + "2");
            final RaceColumn worst = board.getRaceColumnByName(prefix + "3");
            final Series series = ((RaceColumnInSeries) first).getSeries();
            series.setOneAlwaysStaysOne(true);
            series.setResultDiscardingRule(new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3 }));
            for (final RaceColumn column : series.getRaceColumns()) {
                column.setTrackedRace(column.getFleets().iterator().next(), new MockedTrackedRaceWithStartTimeAndRanks(START, competitors));
                column.setFactor(/* factor */ 1.0);
            }
            first.setFactor(/* factor */ 2.0);
            board.getScoreCorrection().correctScore(a, first, /* correctedScore */ 1.0);
            board.getScoreCorrection().correctScore(b, first, /* correctedScore */ 2.0);
            board.getScoreCorrection().correctScore(a, last, /* correctedScore */ 4.0);
            board.getScoreCorrection().correctScore(b, last, /* correctedScore */ 2.0);
            board.getScoreCorrection().correctScore(a, worst, /* correctedScore */ 9.0);
            board.getScoreCorrection().correctScore(b, worst, /* correctedScore */ 9.0);
            for (final Competitor competitor : competitors) {
                assertEquals(medalRace ? Collections.emptySet() : Collections.singleton(worst),
                        board.getResultDiscardingRule().getDiscardedRaceColumns(competitor, board,
                                board.getRaceColumns(), AFTER, board.getScoringScheme()));
                assertEquals(medalRace ? 14.0 : 5.0, board.getNetPoints(competitor, AFTER), TOLERANCE);
            }
            // Weighted (1,4) beats (2,3), with an extra 9 each for non-discardable Medals.
            // Ordinary multiplication would tie (2,4) with (2,4), leaving B ahead on last-race countback.
            assertOrder(board, a, b);
        }));
    }

    @TestFactory
    Stream<DynamicTest> a81ExcludesZeroFactorColumnsWithScoredMissingOrExplicitNullEntries() {
        return Stream.of(/* nullScoresAreBetter */ false, /* nullScoresAreBetter */ true).map((final Boolean nullScoresAreBetter) ->
                DynamicTest.dynamicTest("Zero-factor A8.1 / null better=" + nullScoresAreBetter, () -> {
            final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
            final Competitor a = competitors.get(/* index */ 0);
            final Competitor b = competitors.get(/* index */ 1);
            final FlexibleLeaderboard board = flexible("Zero-factor A8.1", ScoringSchemeType.LOW_POINT);
            final RaceColumn zero = addRace(board, "Ignored", Collections.singletonList(a));
            zero.setFactor(/* factor */ 0.0);
            final RaceColumn first = addRace(board, "R1", competitors);
            final RaceColumn last = addRace(board, "R2", competitors);
            board.getScoreCorrection().correctScore(a, first, /* correctedScore */ 2.0);
            board.getScoreCorrection().correctScore(b, first, /* correctedScore */ 3.0);
            board.getScoreCorrection().correctScore(a, last, /* correctedScore */ 5.0);
            board.getScoreCorrection().correctScore(b, last, /* correctedScore */ 4.0);
            assertNull(board.getTotalPoints(b, zero, AFTER));
            assertOrder(board, a, b);
            final Map<Competitor, Set<RaceColumn>> discards = new HashMap<>();
            for (final Competitor competitor : competitors) {
                discards.put(competitor, Collections.emptySet());
            }
            // Make the counting scores identical: zero-factor participation must not break this sporting tie.
            board.getScoreCorrection().correctScore(b, first, /* correctedScore */ 2.0);
            board.getScoreCorrection().correctScore(b, last, /* correctedScore */ 5.0);
            assertEquals(/* expected */ 0, compareBetter(board, a, b, board.getRaceColumns(), discards));
            assertEquals(/* expected */ 0, compareBetter(board, b, a, board.getRaceColumns(), discards));
            final List<Pair<RaceColumn, Double>> aScores = scores(board, a, board.getRaceColumns(), AFTER);
            final List<Pair<RaceColumn, Double>> bScores = scores(board, b, board.getRaceColumns(), AFTER);
            bScores.add(/* index */ 0, new Pair<>(zero, /* b */ null));
            assertEquals(/* expected */ 0, board.getScoringScheme().compareByBetterScore(a, aScores, b, bScores,
                    board.getRaceColumns(), nullScoresAreBetter, AFTER, board, discards,
                    (final Competitor competitor, final RaceColumn column) -> board.getTotalPoints(competitor, column, AFTER), /* cache */ null));
            assertEquals(/* expected */ 0, board.getScoringScheme().compareByBetterScore(b, bScores, a, aScores,
                    board.getRaceColumns(), nullScoresAreBetter, AFTER, board, discards,
                    (final Competitor competitor, final RaceColumn column) -> board.getTotalPoints(competitor, column, AFTER), /* cache */ null));
        }));
    }

    @TestFactory
    Stream<DynamicTest> firstToTwoBeatsLowerScoreSumsWithEitherCarryFlagAndLowPointDoesNot() {
        return Stream.of(ScoringSchemeType.LOW_POINT_FIRST_TO_WIN_TWO_RACES, ScoringSchemeType.LOW_POINT)
                .flatMap((final ScoringSchemeType type) -> Stream.of(/* carryFlags */ false, /* carryFlags */ true)
                .map((final Boolean carryFlag) -> DynamicTest.dynamicTest(type + " / carry flag " + carryFlag, () -> {
            final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 4);
            final Competitor a = competitors.get(/* index */ 0);
            final Competitor b = competitors.get(/* index */ 1);
            final Competitor c = competitors.get(/* index */ 2);
            final Competitor d = competitors.get(/* index */ 3);
            final Regatta regatta = createRegatta(/* qualifying */ 1, new String[] { "Default" }, /* final */ 0,
                    new String[] { "Default" }, /* medal */ true, /* numberOfMedalRaces */ 4, "First to two",
                    DomainFactory.INSTANCE.getOrCreateBoatClass("Fixture", /* typicallyStartsUpwind */ true),
                    DomainFactory.INSTANCE.createScoringScheme(type));
            final RegattaLeaderboard board = createLeaderboard(regatta, new int[0]);
            final Series medals = ((RaceColumnInSeries) board.getRaceColumnByName("M1")).getSeries();
            medals.setStartsWithZeroScore(true);
            medals.setFirstColumnIsNonDiscardableCarryForward(carryFlag);
            for (final RaceColumn column : medals.getRaceColumns()) {
                column.setFactor(/* factor */ 1.0); // Isolate win precedence from LowPoint's default double Medal weighting.
            }
            final RaceColumn qualification = board.getRaceColumnByName("Q1");
            qualification.setTrackedRace(qualification.getFleets().iterator().next(),
                    new MockedTrackedRaceWithStartTimeAndRanks(START, competitors));
            final RaceColumn carry = board.getRaceColumnByName("M1");
            final Fleet fleet = carry.getFleets().iterator().next();
            carry.enableCompetitorRegistrationOnRaceLog(fleet);
            for (int i = 0; i < competitors.size(); i++) {
                final Competitor competitor = competitors.get(i);
                carry.registerCompetitor(competitor, ((CompetitorWithBoat) competitor).getBoat(), fleet);
                board.getScoreCorrection().correctScore(competitor, carry, (double) i + 1);
            }
            final RaceColumn first = board.getRaceColumnByName("M2");
            first.setTrackedRace(fleet, new MockedTrackedRaceWithStartTimeAndRanks(START, Arrays.asList(d, a, b, c)));
            final RaceColumn second = board.getRaceColumnByName("M3");
            second.setTrackedRace(fleet, new MockedTrackedRaceWithStartTimeAndRanks(START, Arrays.asList(b, a, c, d)));
            assertOrder(board, a, b, d, c); // No two-win champion: Q plus Final points, with Qualification reset.
            assertEquals(/* expected */ 5.0, board.getNetPoints(a, AFTER), TOLERANCE);
            final RaceColumn last = board.getRaceColumnByName("M4");
            last.setTrackedRace(fleet, new MockedTrackedRaceWithStartTimeAndRanks(START, Arrays.asList(d, a, b, c)));
            final boolean countsWins = type == ScoringSchemeType.LOW_POINT_FIRST_TO_WIN_TWO_RACES;
            assertOrder(board, countsWins ? new Competitor[] { d, a, b, c } : new Competitor[] { a, b, d, c });
            assertEquals(/* expected */ 7.0, board.getNetPoints(a, AFTER), TOLERANCE);
            assertEquals(/* expected */ 10.0, board.getNetPoints(d, AFTER), TOLERANCE);
            first.setTrackedRace(fleet, new MockedTrackedRaceWithStartTimeAndRanks(START, Arrays.asList(b, c, d, a)));
            second.setTrackedRace(fleet, new MockedTrackedRaceWithStartTimeAndRanks(START, Arrays.asList(c, b, d, a)));
            last.setTrackedRace(fleet, new MockedTrackedRaceWithStartTimeAndRanks(START, Arrays.asList(a, b, c, d)));
            assertOrder(board, countsWins ? new Competitor[] { a, b, c, d } : new Competitor[] { b, c, a, d });
            assertEquals(/* expected */ 10.0, board.getNetPoints(a, AFTER), TOLERANCE);
            assertEquals(/* expected */ 7.0, board.getNetPoints(b, AFTER), TOLERANCE);
        })));
    }

    @TestFactory
    Stream<DynamicTest> carryFlagChangesEqualTotalTieWithoutChangingBonusWins() {
        return Stream.of(/* carryFlags */ false, /* carryFlags */ true).map((final Boolean carryFlag) ->
                DynamicTest.dynamicTest("carry tie / " + carryFlag, () -> {
                    final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 4);
                    final Competitor a = competitors.get(/* index */ 0);
                    final Competitor b = competitors.get(/* index */ 1);
                    final Competitor c = competitors.get(/* index */ 2);
                    final Competitor d = competitors.get(/* index */ 3);
                    final Regatta regatta = createRegatta(/* qualifying */ 1, new String[] { "Default" }, /* final */ 0,
                            new String[] { "Default" }, /* medal */ true, /* numberOfMedalRaces */ 3, "Carry tie",
                            DomainFactory.INSTANCE.getOrCreateBoatClass("Fixture", /* typicallyStartsUpwind */ true),
                            DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT_FIRST_TO_WIN_TWO_RACES));
                    final RegattaLeaderboard board = createLeaderboard(regatta, new int[0]);
                    final RaceColumn carry = board.getRaceColumnByName("M1");
                    final Series medals = ((RaceColumnInSeries) carry).getSeries();
                    medals.setStartsWithZeroScore(true);
                    medals.setFirstColumnIsNonDiscardableCarryForward(carryFlag);
                    final Fleet fleet = carry.getFleets().iterator().next();
                    final List<Competitor> qualificationOrder = Arrays.asList(c, a, b, d);
                    final RaceColumn qualification = board.getRaceColumnByName("Q1");
                    qualification.setTrackedRace(qualification.getFleets().iterator().next(),
                            new MockedTrackedRaceWithStartTimeAndRanks(START, qualificationOrder));
                    carry.enableCompetitorRegistrationOnRaceLog(fleet);
                    for (int i = 0; i < qualificationOrder.size(); i++) {
                        final Competitor competitor = qualificationOrder.get(i);
                        carry.registerCompetitor(competitor, ((CompetitorWithBoat) competitor).getBoat(), fleet);
                        board.getScoreCorrection().correctScore(competitor, carry, (double) i + 1);
                    }
                    board.getRaceColumnByName("M2").setTrackedRace(fleet,
                            new MockedTrackedRaceWithStartTimeAndRanks(START, Arrays.asList(b, c, a, d)));
                    board.getRaceColumnByName("M3").setTrackedRace(fleet,
                            new MockedTrackedRaceWithStartTimeAndRanks(START, Arrays.asList(d, c, a, b)));
                    // No two-win champion. A=(2,3,3), B=(3,1,4): direct carry favors A, unflagged A8 favors B.
                    assertEquals(/* expected */ 8.0, board.getNetPoints(a, AFTER), TOLERANCE);
                    assertEquals(/* expected */ 8.0, board.getNetPoints(b, AFTER), TOLERANCE);
                    assertOrder(board, carryFlag ? new Competitor[] { c, a, b, d } : new Competitor[] { c, b, a, d });
                }));
    }

    @Test
    void absentFinalScoreDefersResetButAValidZeroTriggersIt() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
        final Competitor a = competitors.get(/* index */ 0);
        final Competitor b = competitors.get(/* index */ 1);
        final Regatta regatta = createRegatta(/* qualifying */ 1, new String[] { "Default" }, /* final */ 1,
                new String[] { "Default" }, /* medal */ false, /* numberOfMedalRaces */ 0, "Deferred reset",
                DomainFactory.INSTANCE.getOrCreateBoatClass("Fixture", /* typicallyStartsUpwind */ true),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT));
        final RegattaLeaderboard board = createLeaderboard(regatta, new int[0]);
        final RaceColumn qualification = board.getRaceColumnByName("Q1");
        qualification.setTrackedRace(qualification.getFleets().iterator().next(),
                new MockedTrackedRaceWithStartTimeAndRanks(START, competitors));
        board.getScoreCorrection().correctScore(a, qualification, /* correctedScore */ 5.0);
        final RaceColumn finalRace = board.getRaceColumnByName("F1");
        ((RaceColumnInSeries) finalRace).getSeries().setStartsWithZeroScore(true);
        final Fleet fleet = finalRace.getFleets().iterator().next();
        finalRace.enableCompetitorRegistrationOnRaceLog(fleet);
        for (final Competitor competitor : competitors) {
            finalRace.registerCompetitor(competitor, ((CompetitorWithBoat) competitor).getBoat(), fleet);
            assertNull(board.getTotalPoints(competitor, finalRace, AFTER));
        }
        assertEquals(/* expected */ 5.0, board.getNetPoints(a, AFTER), TOLERANCE);
        assertEquals(/* expected */ 2.0, board.getNetPoints(b, AFTER), TOLERANCE);
        assertOrder(board, b, a);
        board.getScoreCorrection().correctScore(a, finalRace, /* correctedScore */ 0.0);
        assertEquals(/* expected */ 0.0, board.getTotalPoints(a, finalRace, AFTER), TOLERANCE);
        assertEquals(/* expected */ 0.0, board.getNetPoints(a, AFTER), TOLERANCE);
        assertEquals(/* expected */ 2.0, board.getNetPoints(b, AFTER), TOLERANCE);
        assertOrder(board, a, b);
        board.getScoreCorrection().correctScore(b, finalRace, /* correctedScore */ 1.0);
        assertEquals(/* expected */ 1.0, board.getNetPoints(b, AFTER), TOLERANCE);
        assertOrder(board, a, b);
        board.getScoreCorrection().uncorrectScore(a, finalRace);
        assertNull(board.getTotalPoints(a, finalRace, AFTER));
        assertEquals(/* expected */ 5.0, board.getNetPoints(a, AFTER), TOLERANCE);
        assertOrder(board, b, a);
    }

    private int compareBetter(final Leaderboard board, final Competitor first, final Competitor second,
            final Iterable<RaceColumn> scope, final Map<Competitor, Set<RaceColumn>> discards) {
        return compareBetter(board, first, second, scope, discards, AFTER);
    }

    private int compareBetter(final Leaderboard board, final Competitor first, final Competitor second,
            final Iterable<RaceColumn> scope, final Map<Competitor, Set<RaceColumn>> discards, final TimePoint timePoint) {
        return board.getScoringScheme().compareByBetterScore(first, scores(board, first, scope, timePoint), second,
                scores(board, second, scope, timePoint), scope, /* nullScoresAreBetter */ false, timePoint, board, discards,
                (final Competitor competitor, final RaceColumn column) -> board.getTotalPoints(competitor, column, timePoint), /* cache */ null);
    }

    private List<Pair<RaceColumn, Double>> scores(final Leaderboard board, final Competitor competitor,
            final Iterable<RaceColumn> scope, final TimePoint timePoint) {
        final List<Pair<RaceColumn, Double>> result = new ArrayList<>();
        for (final RaceColumn column : scope) {
            final Double points = board.getTotalPoints(competitor, column, timePoint);
            if (points != null) {
                result.add(new Pair<>(column, points));
            }
        }
        return result;
    }

    @Test
    void unstartedRawRaceHasNoScoreAndCannotTriggerDiscardThreshold() throws Exception {
        final List<Competitor> competitors = createCompetitors(/* numberOfCompetitors */ 2);
        final FlexibleLeaderboard board = new FlexibleLeaderboardImpl("Threshold",
                new ThresholdBasedResultDiscardingRuleImpl(new int[] { 2 }),
                DomainFactory.INSTANCE.createScoringScheme(ScoringSchemeType.LOW_POINT), /* courseArea */ null);
        final RaceColumn first = addRace(board, "R1", competitors);
        final RaceColumn future = board.addRace(new MockedTrackedRaceWithStartTimeAndRanks(new MillisecondsTimePoint(/* millis */ 3000),
                Arrays.asList(competitors.get(/* index */ 1), competitors.get(/* index */ 0))), "R2", /* medalRace */ false);
        final Competitor a = competitors.get(/* index */ 0);
        assertNull(board.getTotalPoints(a, future, AFTER));
        assertTrue(board.getResultDiscardingRule().getDiscardedRaceColumns(a, board, board.getRaceColumns(), AFTER,
                board.getScoringScheme()).isEmpty());
        final TimePoint later = new MillisecondsTimePoint(/* millis */ 4000);
        assertEquals(Collections.singleton(future), board.getResultDiscardingRule().getDiscardedRaceColumns(a, board,
                board.getRaceColumns(), later, board.getScoringScheme()));
        assertEquals(/* expected */ 1.0, board.getTotalPoints(a, first, later), TOLERANCE);
    }

    private FlexibleLeaderboard flexible(final String name, final ScoringSchemeType type) {
        return new FlexibleLeaderboardImpl(name, new ThresholdBasedResultDiscardingRuleImpl(new int[0]),
                DomainFactory.INSTANCE.createScoringScheme(type), /* courseArea */ null);
    }

    private RaceColumn addRace(final FlexibleLeaderboard board, final String name, final List<Competitor> finishOrder) throws Exception {
        return board.addRace(new MockedTrackedRaceWithStartTimeAndRanks(START, finishOrder), name, /* medalRace */ false);
    }

    private FlexibleMetaLeaderboard overall(final ScoringSchemeType type, final Leaderboard... children) {
        final FlexibleMetaLeaderboard result = new FlexibleMetaLeaderboard("Overall " + children[0].getName(),
                DomainFactory.INSTANCE.createScoringScheme(type), new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        for (final Leaderboard child : children) {
            result.addLeaderboard(child);
        }
        return result;
    }

    private Regatta simpleRegatta(final String name, final ScoringSchemeType type) {
        return createSimpleRegatta(/* numberOfRaces */ 1, name,
                DomainFactory.INSTANCE.getOrCreateBoatClass("Fixture", /* typicallyStartsUpwind */ true),
                DomainFactory.INSTANCE.createScoringScheme(type));
    }

    private void attach(final RegattaLeaderboard board, final List<Competitor> finishOrder) {
        final RaceColumn column = board.getRaceColumnByName("R1");
        column.setTrackedRace(column.getFleets().iterator().next(), new MockedTrackedRaceWithStartTimeAndRanks(START, finishOrder));
    }

    private void assertOrder(final Leaderboard board, final Competitor... expected) {
        assertEquals(Arrays.asList(expected), Util.asList(board.getCompetitorsFromBestToWorst(AFTER)));
        final LeaderboardTotalRankComparator comparator = new LeaderboardTotalRankComparator(board, AFTER,
                board.getScoringScheme(), /* nullScoresAreBetter */ false, board.getRaceColumns(), /* cache */ null);
        // All ordered pairs must agree with this whole-field order, including self-comparison and reverse direction.
        for (int i = 0; i < expected.length; i++) {
            for (int j = 0; j < expected.length; j++) {
                assertEquals(Integer.signum(i - j), Integer.signum(comparator.compare(expected[i], expected[j])),
                        board.getName() + " / " + expected[i].getName() + " versus " + expected[j].getName());
            }
        }
    }
}
