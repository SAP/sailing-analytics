package com.sap.sailing.domain.leaderboard;

import java.io.Serializable;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.impl.AbstractSimpleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardTotalRankComparator;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

/**
 * A leaderboard has a scoring scheme that decides how race ranks map to scores, how penalties are to be scored,
 * and how scores are to be compared (are lower or higher scores better?). The scoring scheme can either be
 * provided by the {@link Regatta} or by a {@link FlexibleLeaderboard}. In any case, it is reachable through
 * the {@link Leaderboard} interface.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface ScoringScheme extends Serializable {
    /**
     * The factor by which a medal race score is multiplied by default in the overall point scheme.
     * 
     * @see #getFactor()
     */
    static final double DEFAULT_MEDAL_RACE_FACTOR = 2.0;
    
    /**
     * If this returns <code>true</code>, a higher score is better. For example, the Extreme Sailing Series uses this
     * scoring scheme, as opposed to the olympic sailing classes which use a low-point system.
     */
    boolean isHigherBetter();
    
    /**
     * A comparator in line with the result of {@link #isHigherBetter()}. The comparator returns "less" for results
     * considered "better."
     * 
     * @param nullScoresAreBetter
     *            if <code>true</code>, a <code>null</code> score will be considered "better" ("less") than a non-
     *            <code>null</code> score; otherwise, <code>null</code> scores will be considered "worse" ("greater")
     *            than non-<code>null</code> scores.
     */
    Comparator<Double> getScoreComparator(boolean nullScoresAreBetter);
    
    /**
     * For a <code>rank</code> that a <code>competitor</code> achieved in a race, returns the score attributed to this
     * rank according to this scoring scheme. A scoring scheme may need to know the competitor list for the race. Therefore,
     * the race column as well as the competitor need to be passed although some trivial scoring schemes may not need them.<p>
     * 
     * If the <code>competitor</code> has no {@link RaceColumn#getTrackedRace(Competitor) tracked race} in the column in which
     * the competitor participated, <code>null</code> is returned, meaning the competitor has no score assigned for that
     * race.
     */
    Double getScoreForRank(Leaderboard leaderboard, RaceColumn raceColumn, Competitor competitor,
            int rank, Callable<Integer> numberOfCompetitorsInRaceFetcher,
            NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher, TimePoint timePoint);
    
    /**
     * If a competitor is disqualified, a penalty score is attributed by this scoring scheme. Some schemes require to
     * know the number of competitors in the race, some need to know the total number of competitors in the leaderboard
     * or regatta.
     * @param numberOfCompetitorsInLeaderboardFetcher
     *            if it returns <code>null</code>, the caller cannot determine the number of competitors in the single
     *            race; otherwise, this parameter tells the number of competitors in the same race as
     *            <code>competitor</code>, not in the entire <code>raceColumn</code> (those may be more in case of split
     *            fleets). The scoring scheme may use this number, if available, to infer a penalty score.
     * @param timePoint
     *            an optional timePoint that may help the scheme to determine the penalty related to a certain point in
     *            time only.
     * @param leaderboard
     *            may be required in case a "penalty" such as a redress needs to inspect the scores of other race
     *            columns as well; implementations need to take great care not to cause endless recursions by
     *            naively asking the leaderboard for scores which would recurse into this method
     */
    Double getPenaltyScore(RaceColumn raceColumn, Competitor competitor, MaxPointsReason maxPointsReason,
            Integer numberOfCompetitorsInRace, NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher,
            TimePoint timePoint, Leaderboard leaderboard, Supplier<Double> uncorrectedScoreProvider);

    /**
     * @param competitor1Scores
     *            scores of the first competitor, in the order of race columns in the leaderboard
     * @param competitor2Scores
     *            scores of the second competitor, in the order of race columns in the leaderboard
     * @param discardedRaceColumnsPerCompetitor
     *            for each competitor holds the result of {@link Leaderboard#getResultDiscardingRule()
     *            Leaderborad.getResultDiscardingRule()}{@code .}{@link ResultDiscardingRule#getDiscardedRaceColumns(Competitor, Leaderboard, Iterable, TimePoint, ScoringScheme)
     *            getDiscardedRaceColumns(...)}. This accelerates things considerable because we do not have to make this expensive calculation
     *            for each competitor again.
     */
    int compareByBetterScore(Competitor o1, List<Util.Pair<RaceColumn, Double>> competitor1Scores, Competitor o2,
            List<Util.Pair<RaceColumn, Double>> competitor2Scores, Iterable<RaceColumn> raceColumnsToConsider,
            boolean nullScoresAreBetter, TimePoint timePoint, Leaderboard leaderboard,
            Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * In case two competitors scored in different numbers of races, this scoring scheme decides whether this
     * decides terminally their mutual ranking. If not, <code>0</code> is returned and the comparator needs to look
     * at other criteria to compare the competitors.
     */
    int compareByNumberOfRacesScored(int competitor1NumberOfRacesScored, int competitor2NumberOfRacesScored);
    
    /**
     * Having scored in a later medal series than the other is considered better. -1 means no medal series score at all.
     * With a lesser result encoding "better" the direction of default integer comparison between the two parameters is
     * reversed.
     */
    default int compareByMedalRaceParticipation(int zeroBasedIndexOfLastMedalSeriesInWhichO1Scored,
            int zeroBasedIndexOfLastMedalSeriesInWhichO2Scored) {
        return -Integer.compare(zeroBasedIndexOfLastMedalSeriesInWhichO1Scored, zeroBasedIndexOfLastMedalSeriesInWhichO2Scored);
    }

    ScoringSchemeType getType();

    /**
     * Usually, when all other sorting criteria end up in a tie, the last race sailed is used to decide, and from there
     * backwards. This implements Racing Rules of Sailing (RRS) rule A8.2:
     * <p>
     * 
     * <em>"A8.2 If a tie remains between two or more boats, they shall be ranked in order of their scores in the last
     * race. Any remaining ties shall be broken by using the tied boats' scores in the next-to-last race and so on until
     * all ties are broken. These scores shall be used even if some of them are excluded scores."</em>
     */
    int compareByLastRace(List<Util.Pair<RaceColumn, Double>> o1Scores, List<Util.Pair<RaceColumn, Double>> o2Scores, boolean nullScoresAreBetter, Competitor o1, Competitor o2, TimePoint timePoint, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * Under certain circumstances, a scoring scheme may decide that the scores of a column are not (yet) to be used
     * for the leaderboard's net scores. This may, e.g., be the case if a column is split into more than one fleet and
     * those fleets are unordered. In that case, scores need to be available for all fleets before the column counts
     * for the net scores. Another example is a scoring scheme that defines elimination rounds and awards no points
     * to a competitor in a round from which the competitor got promoted to the next round. Such promotion columns
     * then have no scores and don't count in the number of races starting from where discards are applied.<p>
     */
    boolean isValidInNetScore(Leaderboard leaderboard, RaceColumn raceColumn, Competitor competitor, TimePoint at);

    /**
     * Some scoring schemes are applied to {@link LeaderboardGroupMetaLeaderboard} instances. These instances of a
     * leaderboard are based on other leaderboards grouped in a {@link LeaderboardGroup}. It can happen that the
     * {@link ScoringScheme} needs to have a look at the total points of the other leaderboards in that group. The
     * ordering of the list containing the total points matches the order in the group.
     * @throws NoWindException 
     */
    int compareByLatestRegattaInMetaLeaderboard(Leaderboard leaderboard, Competitor o1, Competitor o2, TimePoint timePoint);
    
    int compareByOtherTieBreakingLeaderboard(RegattaLeaderboardWithOtherTieBreakingLeaderboard leaderboard, Competitor o1, Competitor o2, TimePoint timePoint);

    /**
     * Returning {@code true} makes the number of wins in a medal series the primary ranking criteria.
     * The number of wins that makes a competitor the overall winner must be returned by {@link #getTargetAmountOfMedalRaceWins()}.
     */
    default boolean isMedalWinAmountCriteria() {
        return false;
    }
    
    /**
     * Returning {@code true} makes the {@link RaceColumn#isCarryForward() carry forward score} in a
     * {@link Series#isMedal() medal series} a secondary ranking criteria for competitors that have an equal overall
     * score.
     */
    default boolean isCarryForwardInMedalsCriteria() {
        return false;
    }
    
    /**
     * Usually, the scores in each leaderboard column count as they are for the overall score. However, if a column is a
     * medal race column it usually counts double. Under certain circumstances, columns may also count with factors
     * different from 1 or 2. For example, we've seen cases in the Extreme Sailing Series where the race committee
     * defined that in the overall series leaderboard the last two columns each count 1.5 times their scores.
     */
    default double getScoreFactor(RaceColumn raceColumn) {
        Double factor = raceColumn.getExplicitFactor();
        if (factor == null) {
            factor = raceColumn.isMedalRace() ? DEFAULT_MEDAL_RACE_FACTOR : 1.0;
        }
        return factor;
    }
    
    /**
     * Computes a score corrected by a {@link #getScoreFactor(RaceColumn) column factor} and potentially other
     * column-specific rules, such as that despite multiplying, the original score 1 is to map to 1 again.
     * Respects {@link RaceColumn#isOneAlwaysStaysOne()} on the {@code raceColumn}.
     * 
     * @see #getOriginalScoreFromScoreScaledByFactor(RaceColumn, double)
     * @see ScoringSchemeType#getScaledScore(double, double, boolean)
     */
    default double getScoreScaledByFactor(RaceColumn raceColumn, double originalScore) {
        return ScoringSchemeType.getScaledScore(getScoreFactor(raceColumn), originalScore, raceColumn.isOneAlwaysStaysOne());
    }
    
    /**
     * "Un-scales" a score; the inverse of {@link #getScoreScaledByFactor(RaceColumn, double)}. Respects
     * {@link RaceColumn#isOneAlwaysStaysOne()} on the {@code raceColumn}.
     * 
     * @see #getScoreScaledByFactor(RaceColumn, double)
     * @see ScoringSchemeType#getUnscaledScore(double, double, boolean)
     */
    default double getOriginalScoreFromScoreScaledByFactor(RaceColumn raceColumn, double scaledScore) {
        return ScoringSchemeType.getUnscaledScore(getScoreFactor(raceColumn), scaledScore, raceColumn.isOneAlwaysStaysOne());
    }
    
    /**
     * Returns true if a race column evaluates to be a win for the given competitor at the given timepoint. If the
     * competitor is not scored for this race, {@code false} is returned. "Winning" means to be sorted to the top for
     * that column, considering any score corrections and penalties, too.<p>
     */
    default boolean isWin(Leaderboard leaderboard, Competitor competitor, RaceColumn raceColumn, TimePoint timePoint,
            Function<Competitor, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Fleet fleetOfCompetitor = raceColumn.getFleetOfCompetitor(competitor);
        final Iterable<Competitor> competitorsFromBestToWorstInColumn = leaderboard.getCompetitorsFromBestToWorst(raceColumn, timePoint, totalPointsSupplier, cache);
        for (final Competitor betterCompetitor : competitorsFromBestToWorstInColumn) {
            if (betterCompetitor != competitor && raceColumn.getFleetOfCompetitor(betterCompetitor) == fleetOfCompetitor) {
                // found a better competitor in same fleet; competitor obviously did not score a win
                return false;
            } else if (betterCompetitor == competitor) {
                return true;
            }
        }
        return false; // it is a bit strange that we're asked for a competitor that we didn't find in the column, but that's certainly not a win
    }

    /**
     * Computes the score sum to be displayed in the "sum" column in the leaderboard when considering only the
     * {@code raceColumnsToConsider}.
     * <p>
     * 
     * This default implementation starts with the {@link Leaderboard#getCarriedPoints(Competitor) carried points} for
     * the {@code leaderboard}, then enumerates the {@code raceColumnsToConsider}, and for each of them computes the net
     * points the {@code competitor} has been awarded in that column, unless the leaderboard's
     * {@link Leaderboard#getResultDiscardingRule() discarding rule} has decided to exclude that column's score. The
     * scores are added up, unless a race column is found that has {@link RaceColumn#isStartsWithZeroScore()} set and
     * the competitor scores in that or any of the following columns in which case the score sum so far is reset to
     * zero.
     * 
     * @param raceColumnsToConsider
     *            a sequence of {@link RaceColumn}s, expected to be a prefix of or the same as
     *            {@code leaderboard.}{@link Leaderboard#getRaceColumns() getRaceColumns()}.
     */
    default double getNetPoints(AbstractSimpleLeaderboardImpl leaderboard, Competitor competitor,
            Iterable<RaceColumn> raceColumnsToConsider, TimePoint timePoint) {
        // when a column with isStartsWithZeroScore() is found, only reset score if the competitor scored in any race
        // from there on
        boolean needToResetScoreUponNextNonEmptyEntry = false;
        double result = leaderboard.getCarriedPoints(competitor);
        final Set<RaceColumn> discardedRaceColumns = leaderboard.getResultDiscardingRule().getDiscardedRaceColumns(competitor, leaderboard,
                raceColumnsToConsider, timePoint, this);
        for (RaceColumn raceColumn : raceColumnsToConsider) {
            if (raceColumn.isStartsWithZeroScore()) {
                needToResetScoreUponNextNonEmptyEntry = true;
            }
            if (isValidInNetScore(leaderboard, raceColumn, competitor, timePoint)) {
                final Double netPoints = getNetPointsForScoreSum(leaderboard, competitor, raceColumn, timePoint, discardedRaceColumns);
                if (netPoints != null) {
                    if (needToResetScoreUponNextNonEmptyEntry) {
                        result = 0;
                        needToResetScoreUponNextNonEmptyEntry = false;
                    }
                    result += netPoints;
                }
            }
        }
        return result;
    }

    /**
     * By default, the scores to use for the net points sum are what
     * {@link Leaderboard#getNetPoints(Competitor, RaceColumn, TimePoint, Set)} delivers. Specialized scoring
     * schemes may change this, e.g., to counting wins in the medal series.
     */
    default Double getNetPointsForScoreSum(AbstractSimpleLeaderboardImpl leaderboard, Competitor competitor,
            RaceColumn raceColumn, TimePoint timePoint, final Set<RaceColumn> discardedRaceColumns) {
        return leaderboard.getNetPoints(competitor, raceColumn, timePoint, discardedRaceColumns);
    }
    
    default Pair<Integer, Boolean> getNewNumberOfMedalRacesWon(int numberOfMedalRacesWonSoFar,
            boolean clearNumberOfMedalRacesWonUponNextValidMedalRaceScore,
            Leaderboard leaderboard, Competitor competitor, RaceColumn raceColumn,
            TimePoint timePoint, Function<Competitor, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Double totalPoints = totalPointsSupplier.apply(competitor);
        final int winCount = getWinCount(leaderboard, competitor, raceColumn, totalPoints, timePoint, totalPointsSupplier, cache);
        final int newNumberOfMedalRacesWonSoFar;
        final boolean newClearNumberOfMedalRacesWonUponNextValidMedalRaceScore;
        if (raceColumn.isStartsWithZeroScore()) {
            if (totalPoints != null) {
                newNumberOfMedalRacesWonSoFar = winCount;
                newClearNumberOfMedalRacesWonUponNextValidMedalRaceScore = false;
            } else {
                newNumberOfMedalRacesWonSoFar = numberOfMedalRacesWonSoFar;
                newClearNumberOfMedalRacesWonUponNextValidMedalRaceScore = true;
            }
        } else {
            if (totalPoints != null) {
                newClearNumberOfMedalRacesWonUponNextValidMedalRaceScore = false;
                if (clearNumberOfMedalRacesWonUponNextValidMedalRaceScore) {
                    newNumberOfMedalRacesWonSoFar = winCount;
                } else {
                    newNumberOfMedalRacesWonSoFar = numberOfMedalRacesWonSoFar + winCount;
                }
            } else {
                newClearNumberOfMedalRacesWonUponNextValidMedalRaceScore = clearNumberOfMedalRacesWonUponNextValidMedalRaceScore;
                newNumberOfMedalRacesWonSoFar = numberOfMedalRacesWonSoFar;
            }
        }
        return new Pair<>(newNumberOfMedalRacesWonSoFar, newClearNumberOfMedalRacesWonUponNextValidMedalRaceScore);
    }

    /**
     * @param totalPointsSupplier
     *            can supply the scores for the competitors in the {@code raceColumn}. In particular,
     *            {@code totalPointsSupplier.apply(competitor).equals(totalPoints)} holds true, meaning that the total
     *            points supplied are consistent for the race column and the {@code competitor} provided in the
     *            {@code totalPoints} parameter.
     */
    default int getWinCount(Leaderboard leaderboard, Competitor competitor, RaceColumn raceColumn,
            final Double totalPoints, TimePoint timePoint, Function<Competitor, Double> totalPointsSupplier,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return totalPoints == null ? 0 : isWin(leaderboard, competitor, raceColumn, timePoint, totalPointsSupplier, cache) ? 1 : 0;
    }
    
    /**
     * Compares by the number of races won in the medal series. This default implementation simply compares the two
     * numbers, and the competitor with the greater number is scored better ("less"). It is used if
     * {@link #isMedalWinAmountCriteria()} returns {@code true} (which by default it doesn't).
     */
    default int compareByMedalRacesWon(int numberOfMedalRacesWonO1, int numberOfMedalRacesWonO2) {
        return Integer.compare(numberOfMedalRacesWonO2, numberOfMedalRacesWonO1);
    }

    default int compareByScoreSum(Competitor o1, List<Pair<RaceColumn, Double>> o1Scores, double o1ScoreSum, Competitor o2,
            List<Pair<RaceColumn, Double>> o2Scores, double o2ScoreSum, boolean nullScoresAreBetter, boolean haveValidMedalRaceScores, Supplier<Map<Competitor, Integer>> competitorsRankedByOpeningSeries) {
        return getScoreComparator(nullScoresAreBetter).compare(o1ScoreSum, o2ScoreSum);
    }

    /**
     * Precondition: either both scored in medal race or both didn't. If both scored, the better score sum wins. If both
     * scored the same sum in the medal races, apply
     * {@link #compareByScoreSum(Competitor, List, double, Competitor, List, double, boolean, boolean, Supplier)} to
     * apply the same tie breaking to the medal races as for other series.
     * <p>
     * 
     * This is to be applied only if the net score of both competitors are equal to each other.
     */
    default int compareByMedalRaceScore(Competitor o1, Competitor o2, Double o1MedalRaceScore, Double o2MedalRaceScore,
            List<Pair<RaceColumn, Double>> o1ScoringMedalRaces, List<Pair<RaceColumn, Double>> o2ScoringMedalRaces,
            TimePoint timePoint, Leaderboard leaderboard,
            Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, boolean nullScoresAreBetter,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        assert o1MedalRaceScore != null || o2MedalRaceScore == null;
        int result;
        if (o1MedalRaceScore != null) {
            result = getScoreComparator(nullScoresAreBetter).compare(o1MedalRaceScore, o2MedalRaceScore);
            if (result == 0 && o1ScoringMedalRaces.size() > 1) { // only work calling if more than one scoring medal race
                result = compareByBetterScore(o1, o1ScoringMedalRaces, o2, o2ScoringMedalRaces,
                        Util.map(o1ScoringMedalRaces, Pair::getA), nullScoresAreBetter, timePoint, leaderboard,
                        discardedRaceColumnsPerCompetitor, totalPointsSupplier, cache);
            }
        } else {
            result = 0;
        }
        return result;
    }
    
    /**
     * Compares by the scores of a single race column. If only one of the competitors has a result this competitor is
     * ranked better than the other one.
     */
    default int compareBySingleRaceColumnScore(Double o1Score, Double o2Score, boolean nullScoresAreBetter) {
        return Comparator
                .nullsLast((Double o1s, Double o2s) -> getScoreComparator(nullScoresAreBetter).compare(o1s, o2s))
                .compare(o1Score, o2Score);
    }

    default int compareByLastMedalRacesCriteria(Competitor o1, List<Pair<RaceColumn, Double>> o1Scores, Competitor o2,
            List<Pair<RaceColumn, Double>> o2Scores, boolean nullScoresAreBetter, Leaderboard leaderboard,
            Iterable<RaceColumn> raceColumnsToConsider, BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache, TimePoint timePoint, int zeroBasedIndexOfLastMedalSeriesInWhichBothScored,
            int numberOfMedalRacesWonO1, int numberOfMedalRacesWonO2) {
        return 0;
    }

    LeaderboardTotalRankComparator getOpeningSeriesRankComparator(Iterable<RaceColumn> raceColumnsToConsider, boolean nullScoresAreBetter,
            TimePoint timePoint, Leaderboard leaderboard,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * This default implementation decides competitor participation in a medal race by having scored a non-{@code null}
     * score.
     */
    default boolean isParticipatingInMedalRace(Competitor competitor, Double competitorMedalRaceScore,
            RaceColumnInSeries medalRaceColumn, Supplier<Map<Competitor, Integer>> competitorsRankedByOpeningSeries) {
        return competitorMedalRaceScore != null;
    }
}
