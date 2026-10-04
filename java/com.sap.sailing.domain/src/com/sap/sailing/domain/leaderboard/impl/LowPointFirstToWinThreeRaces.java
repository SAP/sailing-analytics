package com.sap.sailing.domain.leaderboard.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

/**
 * Similar to {@link LowPointFirstToWinTwoRaces}, but three races are needed to win. If the regatta has one or more
 * medal series then in those medal series the wins are counted as the primary ordering criterion. If such a medal
 * series starts with a non-discardable carry column then the meaning of that column is changed to mean a number of wins
 * carried into the series. For example, in the 2024 Olympic kite format the winner of the opening series carries two
 * wins, the competitor ranking second at the end of the opening series one win into the grand final which is to be
 * represented as a second medal series following a first semi-final medal series; and boats ranking third and fourth
 * after the opening series carry two wins into the semi-final medal series, and boats ranking fifth and sixth after the
 * opening series carry one win each into the semi-final medal series.
 * <p>
 * 
 * Competitors that score in a later medal series are considered better than those that don't (promotion / elimination
 * scheme).
 * <p>
 * 
 * Those carried wins are added to the
 * {@link #isWin(Leaderboard, Competitor, RaceColumn, TimePoint, Function, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)
 * wins} achieved in the medal series. More wins rank better. Equal numbers of races won make the score in the last race
 * in that series the first tie-breaker. Note that "last race" can be different races in case of multiple fleets in that
 * medal series, such as in a semi-final medal series split into fleets A and B. Should the tie not be resolved this
 * way, the next and last tie-breaking criterion is the opening series rank where the "opening series" is comprised of
 * all {@link Series} preceding any {@link Series#isMedal() medal series} and may itself consist of more than one
 * {@link Series} object, such as a "Qualification" and a "Final" series in case the fleet is split.
 * <p>
 * 
 * When a medal series is specified to {@link Series#isStartsWithZeroScore() start with zero scores} then this is also
 * applied to the number of wins counted so far. Therefore, if a second medal series follows a first medal series and
 * the second medal series has {@link Series#isStartsWithZeroScore()} set to {@code true} then the wins start counting
 * with zero, then a {@link Series#isFirstColumnNonDiscardableCarryForward() carry column} will be interpreted to hold
 * the number of wins carried into this series, and further wins will be added to that.
 * <p>
 * 
 * This scoring scheme changes the definition of the net points sum for all medal series participants to equal the
 * number of wins, considering the {@link Series#isStartsWithZeroScore()} and the wins carried over into the series as
 * specified by {@link Series#isFirstColumnNonDiscardableCarryForward()}.<p>
 * 
 * The penalty score calculation for an {@link MaxPointsReason#STP STP} IRM code is changed: instead of adding 1.0 to
 * the score from tracking, this scoring scheme adds 1.1. This shall help avoid ties, particularly when it comes to
 * deciding a race's winner which shall happen based on lowest score (<em>not</em> score equaling 1.0) now.
 */
public class LowPointFirstToWinThreeRaces extends LowPoint {
    private static final Logger logger = Logger.getLogger(LowPointFirstToWinThreeRaces.class.getName());
    
    private static final long serialVersionUID = 7072175334160798617L;

    @Override
    public ScoringSchemeType getType() {
        return ScoringSchemeType.LOW_POINT_FIRST_TO_WIN_THREE_RACES;
    }

    /**
     * We cannot simply go by the number of medal races won because during the semi-final medal series which is split
     * into two fleets the competitors are not ranked across these fleets based on wins but first within their fleet
     * based on wins, then based on last race, second-to-last, ..., and then lastly by the opening series rank, and only
     * then are the two semi-final fleets merged one by one, with two equal ranks in fleets A/B decided based on the
     * opening series rank again. This all happens in
     * {@link #compareByLastMedalRacesCriteria(Competitor, List, Competitor, List, boolean, Leaderboard, Iterable, BiFunction, WindLegTypeAndLegBearingAndORCPerformanceCurveCache, TimePoint, int, int, int)}.
     * Yet, we have to respond with {@code true} here in order to <em>count</em> the medal races won. We will then
     * ignore that result in {@link #compareByMedalRacesWon(int, int)} and instead do it all in
     * {@link #compareByLastMedalRacesCriteria(Competitor, List, Competitor, List, boolean, Leaderboard, Iterable, BiFunction, WindLegTypeAndLegBearingAndORCPerformanceCurveCache, TimePoint, int, int, int)}.
     */
    @Override
    public boolean isMedalWinAmountCriteria() {
        return true;
    }

    /**
     * Always returns {@code 0}; the heavy lifting is done by
     * {@link #compareByLastMedalRacesCriteria(Competitor, List, Competitor, List, boolean, Leaderboard, Iterable, BiFunction, WindLegTypeAndLegBearingAndORCPerformanceCurveCache, TimePoint, int, int, int)}.
     */
    @Override
    public int compareByMedalRacesWon(int numberOfMedalRacesWonO1, int numberOfMedalRacesWonO2) {
        return 0;
    }

    /**
     * Assumes that {@code o1} and {@code o2} participated and scored in the same number of medal series and compares
     * the two competitors with each other. If they both compete in the same fleet in their last medal series, first their
     * number of wins (including the carried-forward wins), then the scores in the last, last-but-one, etc. races until a
     * difference is found, and if no difference is found that way, by the opening series rank.<p>
     * 
     * Should the competitors have competed in different fleets in the same medal series (like the A/B fleets of
     * the semi-final stage) then first compute and compare their rank within their fleet by the criteria stated
     * above, and if they both ranked equally well in their respective fleet, compare by their opening series rank.
     */
    @Override
    public int compareByLastMedalRacesCriteria(Competitor o1, List<Pair<RaceColumn, Double>> o1Scores, Competitor o2,
            List<Pair<RaceColumn, Double>> o2Scores, boolean nullScoresAreBetter, Leaderboard leaderboard,
            Iterable<RaceColumn> raceColumnsToConsider,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache,
            TimePoint timePoint, int zeroBasedIndexOfLastMedalSeriesInWhichBothScored, int numberOfMedalRacesWonO1, int numberOfMedalRacesWonO2) {
        int result;
        if (zeroBasedIndexOfLastMedalSeriesInWhichBothScored < 0) {
            result = 0; // neither scored in a medal series, so nothing to compare
        } else {
            if (!(leaderboard instanceof RegattaLeaderboard)) {
                result = 0;
            } else {
                final Regatta regatta = ((RegattaLeaderboard) leaderboard).getRegatta();
                final Series medalSeriesInWhichBothScored = Util.get(regatta.getSeries(), zeroBasedIndexOfLastMedalSeriesInWhichBothScored);
                assert medalSeriesInWhichBothScored.isMedal();
                final RaceColumn firstNonCarryRaceColumnInMedalSeries = Util.first(Util.filter(medalSeriesInWhichBothScored.getRaceColumns(),
                        rc->!rc.isCarryForward()));
                final Fleet o1MedalFleet = firstNonCarryRaceColumnInMedalSeries.getFleetOfCompetitor(o1);
                final Fleet o2MedalFleet = firstNonCarryRaceColumnInMedalSeries.getFleetOfCompetitor(o2);
                // pass on the totalPointsSupplier coming from the caller, most likely a LeaderboardTotalRankComparator,
                // to speed up / save the total points (re-)calculation
                final Supplier<LeaderboardTotalRankComparator> openingSeriesTotalRankComparator =
                        ()->getOpeningSeriesRankComparator(raceColumnsToConsider, nullScoresAreBetter, timePoint, leaderboard, totalPointsSupplier, cache);
                if (o1MedalFleet == o2MedalFleet) {
                    result = compareByInMedalSeriesFleetRules(o1, o2, totalPointsSupplier, medalSeriesInWhichBothScored, openingSeriesTotalRankComparator,
                            nullScoresAreBetter, leaderboard, timePoint, cache);
                } else {
                    // o1 and o2 scored in different fleets of the same medal series; compare as follows:
                    // - by their rank inside their fleet
                    // Special case: one of the competitors received a score for the medal series but is not assigned to
                    // any of the fleets (o1MedalFleet==null || o2MedalFleet==null). In this case, consider a null fleet as worse:
                    if (o1MedalFleet == null) {
                        result = 1;
                        logger.warning("Competitor "+o1.getName()+" has a score in medal series "+medalSeriesInWhichBothScored.getName()+
                                " but is not assigned to a fleet; ranking worse than "+o2.getName());
                    } else if (o2MedalFleet == null) {
                        result = -1;
                        logger.warning("Competitor "+o2.getName()+" has a score in medal series "+medalSeriesInWhichBothScored.getName()+
                                " but is not assigned to a fleet; ranking worse than "+o1.getName());
                    } else {
                        final int o1RankInMedalSeriesFleet = getRankInMedalSeriesFleet(medalSeriesInWhichBothScored, o1MedalFleet, totalPointsSupplier, o1,
                                openingSeriesTotalRankComparator, firstNonCarryRaceColumnInMedalSeries, nullScoresAreBetter, leaderboard, timePoint, cache);
                        final int o2RankInMedalSeriesFleet = getRankInMedalSeriesFleet(medalSeriesInWhichBothScored, o2MedalFleet, totalPointsSupplier, o2,
                                openingSeriesTotalRankComparator, firstNonCarryRaceColumnInMedalSeries, nullScoresAreBetter, leaderboard, timePoint, cache);
                        result = Integer.compare(o1RankInMedalSeriesFleet, o2RankInMedalSeriesFleet);
                    }
                    if (result == 0) {
                        // - then by opening series rank
                        result = openingSeriesTotalRankComparator.get().compare(o1, o2);
                    }
                }
            }
        }
        return result;
    }

    private int getRankInMedalSeriesFleet(Series medalSeries, Fleet medalFleet,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, Competitor competitor,
            final Supplier<LeaderboardTotalRankComparator> openingSeriesTotalRankComparator, RaceColumn firstNonCarryRaceColumnInMedalSeries,
            boolean nullScoresAreBetter,
            Leaderboard leaderboard, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final List<Competitor> competitorsInMedalFleet = Util.asList(firstNonCarryRaceColumnInMedalSeries.getAllCompetitors(medalFleet));
        competitorsInMedalFleet.sort((c1, c2)->compareByInMedalSeriesFleetRules(c1, c2, totalPointsSupplier, medalSeries,
                openingSeriesTotalRankComparator, nullScoresAreBetter, leaderboard, timePoint, cache));
        return competitorsInMedalFleet.indexOf(competitor)+1;
    }

    /**
     * Compares two competitors who scored in the same fleet of the same medal series by the following criteria,
     * in this order of decreasing precedence:
     * <ul>
     * <li>The number of "wins" (including the wins carried from an earlier stage and noted in the corresponding "carry forward" column)</li>
     * <li>The scores achieved in the medal races, from last to first, until the first difference is found</li>
     * <li>The opening series rank</li>
     * </ul>
     */
    private int compareByInMedalSeriesFleetRules(Competitor o1, Competitor o2,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, final Series medalSeriesInWhichBothScored,
            final Supplier<LeaderboardTotalRankComparator> openingSeriesTotalRankComparator,
            boolean nullScoresAreBetter, Leaderboard leaderboard, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        int result;
        final int numberOfMedalRacesWonO1 = Util.stream(medalSeriesInWhichBothScored.getRaceColumns())
                .mapToInt(raceColumn -> getWinCount(leaderboard, o1, raceColumn,
                        totalPointsSupplier.apply(o1, raceColumn), timePoint,
                        c -> totalPointsSupplier.apply(c, raceColumn), cache))
                .sum();
        final int numberOfMedalRacesWonO2 = Util.stream(medalSeriesInWhichBothScored.getRaceColumns())
                .mapToInt(raceColumn -> getWinCount(leaderboard, o2, raceColumn,
                        totalPointsSupplier.apply(o2, raceColumn), timePoint,
                        c -> totalPointsSupplier.apply(c, raceColumn), cache))
                .sum();
        // compare by in-fleet rules:
        // - first by number of wins:
        if (numberOfMedalRacesWonO1 != numberOfMedalRacesWonO2) {
            result = Integer.compare(numberOfMedalRacesWonO2, numberOfMedalRacesWonO1);
        } else {
            // - then by comparing scores statrting at the last race in the series, going backward until tie is broken or first race is reached
            result = compareByScoresFromLastToFirstRace(medalSeriesInWhichBothScored.getRaceColumns(), o1, o2, totalPointsSupplier, nullScoresAreBetter);
            if (result == 0) {
                // - then by opening series rank
                result = openingSeriesTotalRankComparator.get().compare(o1, o2);
            }
        }
        return result;
    }

    /**
     * Ignores the first carry column in the medal series where the carried wins are expected. Both competitors
     * must have sailed in the same fleet in this race column.
     */
    private int compareByScoresFromLastToFirstRace(Iterable<? extends RaceColumnInSeries> raceColumns, Competitor o1,
            Competitor o2, BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, boolean nullScoresAreBetter) {
        assert Util.stream(raceColumns).allMatch(rc->rc.getFleetOfCompetitor(o1) == rc.getFleetOfCompetitor(o2));
        final List<? extends RaceColumnInSeries> medalRaceRolumnsWithReverseOrdering = Util.asList(raceColumns);
        Collections.reverse(medalRaceRolumnsWithReverseOrdering);
        int result = 0;
        for (final RaceColumnInSeries raceColumn : medalRaceRolumnsWithReverseOrdering) {
            final Double o1Score = totalPointsSupplier.apply(o1, raceColumn);
            final Double o2Score = totalPointsSupplier.apply(o2, raceColumn);
            result = getScoreComparator(nullScoresAreBetter).compare(o1Score, o2Score);
            if (result != 0) {
                break;
            }
        }
        return result;
    }

    /**
     * Still returns {@code null} if {@link Leaderboard#getNetPoints(Competitor, RaceColumn, TimePoint, Set)} returns {@code null}.
     * Otherwise, if the {@code raceColumn} is a medal race column and not the medal series' carry-forward column, 1.0 is returned
     * for a win, 0.0 for non-wins. The carry-column's contents in a medal series are returned as defined by the leaderboard.
     */
    @Override
    public Double getNetPointsForScoreSum(AbstractSimpleLeaderboardImpl leaderboard, Competitor competitor,
            RaceColumn raceColumn, TimePoint timePoint, Set<RaceColumn> discardedRaceColumns) {
        final Double result;
        final Double netPoints = super.getNetPointsForScoreSum(leaderboard, competitor, raceColumn, timePoint, discardedRaceColumns);
        if (netPoints != null && raceColumn.isMedalRace() && !raceColumn.isCarryForward()) {
            // TODO bug 5778: consider passing through a cache object
            final LeaderboardDTOCalculationReuseCache cache = new LeaderboardDTOCalculationReuseCache(timePoint);
            result = isWin(leaderboard, competitor, raceColumn, timePoint, c->leaderboard.getTotalPoints(c, raceColumn, timePoint, cache),
                    cache) ? 1.0 : 0.0;
        } else {
            result = netPoints; // includes the null case
        }
        return result;
    }

    /**
     * In equal-weighted semifinal fleets A/B a different number of races may be sailed until any one competitor
     * in that fleet has reached {@link #getTargetAmountOfMedalRaceWins()} wins. Therefore, the number of races
     * scored is not a criterion for this scoring scheme.
     */
    @Override
    public int compareByNumberOfRacesScored(int competitor1NumberOfRacesScored, int competitor2NumberOfRacesScored) {
        return 0;
    }
    
    @Override
    public double getScoreFactor(RaceColumn raceColumn) {
        Double factor = raceColumn.getExplicitFactor();
        if (factor == null) {
            factor = 1.0;
        }
        return factor;
    }

    /**
     * A carry-forward column in a medal series for this scoring scheme means that the points in the column represent a
     * number of wins carried forward into this series. Other than that, the regular logic applies: one point for a
     * {@link #isWin(Leaderboard, Competitor, RaceColumn, TimePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache, Function<Competitor, Double>)
     * win}, zero for non-win, adding to {@code numberOfMedalRacesWonSoFar} or starting with zero if
     * {@link RaceColumn#isStartsWithZeroScore()}.
     */
    @Override
    public int getWinCount(Leaderboard leaderboard, Competitor competitor, RaceColumn raceColumn,
            final Double totalPoints, TimePoint timePoint, Function<Competitor, Double> totalPointsSupplier,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Integer winCount;
        if (raceColumn.isCarryForward()) {
            winCount = totalPoints == null ? 0 : totalPoints.intValue();
        } else {
            winCount = super.getWinCount(leaderboard, competitor, raceColumn, totalPoints, timePoint, totalPointsSupplier, cache);
        }
        return winCount;
    }
    
    /**
     * After some back and forth between the Formula Kite class and World Sailing and the IOC, it seems they now settle for
     * an approach by which the "winner" of a race is called the competitor with the lowest score. With the other special rule
     * of making a standard penalty {@link MaxPointsReason#STP STP} count 1.1 (instead of 1.0) during the medal series, ties
     * between winner and runner-up are to be avoided.<p>
     * 
     * (Used to be, based on OG2024_SAL_ORIS_R8_V1.2_20230330.pdf: "A boat shall be credited with a win when it is scored with one point
     * in a medal series race.")
     */
    @Override
    public boolean isWin(Leaderboard leaderboard, Competitor competitor, RaceColumn raceColumn, TimePoint timePoint,
            Function<Competitor, Double> totalPointsSupplier,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Fleet fleetOfCompetitor = raceColumn.getFleetOfCompetitor(competitor);
        final Double competitorTotalPoints = totalPointsSupplier.apply(competitor);
        final Comparator<Double> scoreComparator = getScoreComparator(/* nullScoresAreBetter */ false);
        for (final Competitor otherCompetitor : leaderboard.getCompetitors()) {
            if (otherCompetitor != competitor && raceColumn.getFleetOfCompetitor(otherCompetitor) == fleetOfCompetitor) {
                final int totalPointsCompareResult = scoreComparator.compare(competitorTotalPoints, totalPointsSupplier.apply(otherCompetitor));
                if (totalPointsCompareResult > 0) {
                    // otherCompetitor is in same fleet and has a better score ("less" in the view of the score comparator)
                    // so competitor hasn't won this race
                    return false;
                } else if (totalPointsCompareResult == 0) {
                    // both have the same score; compare by finishing order:
                    final int finishingOrderCompareResult = Integer.compare(leaderboard.getTrackedRank(competitor, raceColumn, timePoint, cache),
                            leaderboard.getTrackedRank(otherCompetitor, raceColumn, timePoint, cache));
                    if (finishingOrderCompareResult > 0) {
                        // competitor is ranked worse than otherCompetitor at timePoint, so hasn't won
                        return false;
                    }
                }
            }
        }
        return true;
    }

    /**
     * When the competitors have valid medal race scores, this scoring scheme ignores the score sums altogether and
     * assumes that
     * {@link #compareByLastMedalRacesCriteria(Competitor, List, Competitor, List, boolean, Leaderboard, Iterable, BiFunction, WindLegTypeAndLegBearingAndORCPerformanceCurveCache, TimePoint, int, int, int)} 
     * handles the medal series ranking altogether.
     */
    @Override
    public int compareByScoreSum(Competitor o1, List<Pair<RaceColumn, Double>> o1Scores, double o1ScoreSum,
            Competitor o2, List<Pair<RaceColumn, Double>> o2Scores, double o2ScoreSum, boolean nullScoresAreBetter, boolean haveValidMedalRaceScores, Supplier<Map<Competitor, Integer>> competitorsRankedByOpeningSeries) {
        return haveValidMedalRaceScores ? 0 : super.compareByScoreSum(o1, o1Scores, o1ScoreSum, o2, o2Scores, o2ScoreSum, nullScoresAreBetter, haveValidMedalRaceScores, competitorsRankedByOpeningSeries);
    }

    /**
     * The sum of medal race scores is not of interest to ranking when using this scoring scheme. See
     * {@link #compareByMedalRacesWon(int, int)} instead.
     */
    @Override
    public int compareByMedalRaceScore(Competitor o1, Competitor o2, Double o1MedalRaceScore, Double o2MedalRaceScore, List<Pair<RaceColumn, Double>> o1ScoringMedalRaces, List<Pair<RaceColumn, Double>> o2ScoringMedalRaces, TimePoint timePoint, Leaderboard leaderboard, Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor, BiFunction<Competitor, RaceColumn, Double> totalPointSupplier, boolean nullScoresAreBetter, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return 0;
    }

    /**
     * The default implementation will do an RRS A8.1 comparison (sort results, compare one by one "from the top" and
     * decide upon the first difference), across <em>all</em> scores throughout the leaderboard. Here, however, we need
     * to distinguish between competitors who only sailed in the opening series (anything preceding the first medal
     * series), and those who did score in at least one medal race, because the medal series are evaluated only by wins
     * and as a secondary tie breaker then by the medal race scores looked at from the end, and after that the rank at
     * the end of the opening series.
     * <p>
     * 
     * It is safe to assume that if {@code o1} has valid medal series scores then so will {@code o2}, and vice versa,
     * because otherwise ranking by medal series participation would already have decided who ranks better. If none has
     * score in a medal race then we default to the {@code super} implementation with a default RRS A8.1 decision.
     * <p>
     * 
     * Otherwise (both have scored in at least one medal race) we have to assume that both sailed in the same medal
     * series, scored the same number of wins (including wins carried forward) and were also tied on the scores in their
     * respective last medal race. Then, the decision is to be made based on the opening series rank, which in itself
     * includes the entire tie-breaking rule set with
     * {@link #compareByBetterScore(Competitor, List, Competitor, List, Iterable, boolean, TimePoint, Leaderboard, Map, BiFunction, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)}
     * etc., only up to the end of the opening series.
     */
    @Override
    public int compareByBetterScore(Competitor o1, List<Pair<RaceColumn, Double>> o1Scores, Competitor o2,
            List<Pair<RaceColumn, Double>> o2Scores, Iterable<RaceColumn> raceColumnsToConsider,
            boolean nullScoresAreBetter, TimePoint timePoint, Leaderboard leaderboard,
            Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final int result;
        if (!hasMedalScores(o1Scores)) {
            result = compareByA81TieBreak(o1, o1Scores, o2, o2Scores, raceColumnsToConsider, nullScoresAreBetter, timePoint,
                    leaderboard, discardedRaceColumnsPerCompetitor, totalPointsSupplier, cache);
        } else {
            final LeaderboardTotalRankComparator openingSeriesRankComparator = getOpeningSeriesRankComparator(
                    raceColumnsToConsider, nullScoresAreBetter, timePoint, leaderboard, totalPointsSupplier, cache);
            result = openingSeriesRankComparator.compare(o1, o2);
        }
        return result;
    }

    /**
     * Computes a tie break; this default implementation delegates to the super-class implementation which is
     * expected to compute a regular A8.1 tie break, sorting the scores so the best are first, and then looking
     * for the first difference.<p>
     * 
     * Subclasses may change this behavior, e.g., so that 0 is returned and hence A8.1 is ignored; this will
     * usually then default to a comparison based on A8.2 (last race).
     */
    protected int compareByA81TieBreak(Competitor o1, List<Pair<RaceColumn, Double>> o1Scores, Competitor o2,
            List<Pair<RaceColumn, Double>> o2Scores, Iterable<RaceColumn> raceColumnsToConsider, boolean nullScoresAreBetter,
            TimePoint timePoint, Leaderboard leaderboard,
            Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return super.compareByBetterScore(o1, o1Scores, o2, o2Scores, raceColumnsToConsider, nullScoresAreBetter, timePoint,
                leaderboard, discardedRaceColumnsPerCompetitor, totalPointsSupplier, cache);
    }

    private boolean hasMedalScores(List<Pair<RaceColumn, Double>> o1Scores) {
        return o1Scores.stream().anyMatch(p->p.getA().isMedalRace() && p.getB() != null);
    }

    @Override
    public Double getPenaltyScore(RaceColumn raceColumn, Competitor competitor, MaxPointsReason maxPointsReason,
            Integer numberOfCompetitorsInRace,
            NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher, TimePoint timePoint,
            Leaderboard leaderboard, Supplier<Double> uncorrectedScoreProvider) {
        final Double result;
        if ((maxPointsReason == MaxPointsReason.STP || maxPointsReason == MaxPointsReason.SCP) && raceColumn.isMedalRace()) {
            final Double uncorrectedScore = uncorrectedScoreProvider.get();
            result = uncorrectedScore == null ? null : uncorrectedScore + 1.1;
        } else {
            result = super.getPenaltyScore(raceColumn, competitor, maxPointsReason, numberOfCompetitorsInRace,
                    numberOfCompetitorsInLeaderboardFetcher, timePoint, leaderboard, uncorrectedScoreProvider);
        }
        return result;
    }
}
