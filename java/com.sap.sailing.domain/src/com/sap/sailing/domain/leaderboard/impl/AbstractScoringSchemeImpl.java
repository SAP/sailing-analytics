package com.sap.sailing.domain.leaderboard.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithOtherTieBreakingLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

public abstract class AbstractScoringSchemeImpl implements ScoringScheme {
    private static final long serialVersionUID = 6830414905539642446L;
    
    /**
     * Compares two scores equal if their difference is less than {@link #THRESHOLD_FOR_EQUALITY}.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private class ScoreComparator implements Comparator<Double>, Serializable {
        private static final long serialVersionUID = -2767385186133743330L;
        
        /**
         * Adding scores using {@code double} as type can lead to small differences ranging around
         * 10^-10 and less. If two scores vary by only this little they shall be considered equal.
         * 
         * See also bug 5145.
         */
        private static final double THRESHOLD_FOR_EQUALITY = 0.0000000001;
        
        private final boolean nullScoresAreBetter;
        
        public ScoreComparator(boolean nullScoresAreBetter) {
            this.nullScoresAreBetter = nullScoresAreBetter;
        }

        @Override
        public int compare(Double o1, Double o2) {
            // null means did not enlist in the race or race hasn't started for that competitor yet; null
            // sorts "worse" than non-null.
            int result;
            if (o1 == null) {
                if (o2 == null) {
                    result = 0;
                } else {
                    result = nullScoresAreBetter ? -1 : 1;
                }
            } else {
                if (o2 == null) {
                    result = nullScoresAreBetter ? 1 : -1;
                } else {
                    result = compareDoublesWithThreshold(o1, o2) * (isHigherBetter() ? -1 : 1);
                }
            }
            return result;
        }
        
        private int compareDoublesWithThreshold(double d1, double d2) {
            final int result;
            if (Math.abs(d1-d2) < THRESHOLD_FOR_EQUALITY) {
                result = 0;
            } else if (d1 > d2) {
                result = 1;
            } else {
                result = -1;
            }
            return result;
        }
    }
    
    private final boolean higherIsBetter;
    
    public AbstractScoringSchemeImpl(boolean higherIsBetter) {
        this.higherIsBetter = higherIsBetter;
    }    

    @Override
    public boolean isHigherBetter() {
        return higherIsBetter;
    }

    @Override
    public Comparator<Double> getScoreComparator(boolean nullScoresAreBetter) {
        return new ScoreComparator(nullScoresAreBetter);
    }
    
    /**
     * Assuming both competitors scored in the same number of races, compares the sorted scores based on World Sailing's
     * Racing Rules of Sailing (RRS) addendum A8.1:<p>
     * 
     * <em>"A8.1 If there is a series-score tie between two or more boats, each boat's race scores shall be listed in
     * order of best to worst, and at the first point(s) where there is a difference the tie shall be broken in favour
     * of the boat(s) with the best score(s). No excluded scores shall be used."</em>
     * Scores are weighted once before sorting; zero-factor columns do not participate in this tie break.
     */
    @Override
    public int compareByBetterScore(final Competitor o1, final List<Pair<RaceColumn, Double>> o1Scores,
            final Competitor o2, final List<Pair<RaceColumn, Double>> o2Scores, final Iterable<RaceColumn> raceColumnsToConsider,
            final boolean nullScoresAreBetter, final TimePoint timePoint, final Leaderboard leaderboard,
            final Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor,
            final BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier,
            final WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Comparator<Pair<RaceColumn, Double>> ruleA8_1ScoreComparator = getRuleA8_1ScoreComparator(nullScoresAreBetter);
        final boolean includeDiscardedResults = isConsiderDiscardedScoresDuringBetterScoreTieBreak();
        final List<Pair<RaceColumn, Double>> o1NetScores = getWeightedScoresForBetterScoreTieBreak(o1Scores,
                discardedRaceColumnsPerCompetitor.get(o1), includeDiscardedResults);
        final List<Pair<RaceColumn, Double>> o2NetScores = getWeightedScoresForBetterScoreTieBreak(o2Scores,
                discardedRaceColumnsPerCompetitor.get(o2), includeDiscardedResults);
        Collections.sort(o1NetScores, ruleA8_1ScoreComparator);
        Collections.sort(o2NetScores, ruleA8_1ScoreComparator);
        // now both lists are sorted from best to worst score
        final Iterator<Pair<RaceColumn, Double>> o1Iter = o1NetScores.iterator();
        final Iterator<Pair<RaceColumn, Double>> o2Iter = o2NetScores.iterator();
        int result = 0;
        while (result == 0 && o1Iter.hasNext() && o2Iter.hasNext()) {
            result = getScoreComparator(nullScoresAreBetter).compare(o1Iter.next().getB(), o2Iter.next().getB());
        }
        if (o1Iter.hasNext() != o2Iter.hasNext()) {
            // if, as may be allowed by some scoring scheme variants, competitors with different numbers of scored races are compared
            // and are equal for all races of the competitor who scored fewer races, the competitor who scored more races is preferred
            result = o1Iter.hasNext() ? -1 : 1;
        }
        return result;
    }

    private List<Pair<RaceColumn, Double>> getWeightedScoresForBetterScoreTieBreak(
            final List<Pair<RaceColumn, Double>> scores, final Set<RaceColumn> discards,
            final boolean includeDiscardedResults) {
        final List<Pair<RaceColumn, Double>> result = new ArrayList<>();
        for (final Pair<RaceColumn, Double> columnAndScore : scores) {
            final RaceColumn column = columnAndScore.getA();
            if (getScoreFactor(column) != 0 && (includeDiscardedResults || !discards.contains(column))) {
                final Double score = columnAndScore.getB();
                result.add(new Pair<>(column, score == null ? null : getScoreScaledByFactor(column, score)));
            }
        }
        return result;
    }
    
    /**
     * Usually, RRS A8.1-based rules will eliminate discarded results before starting to compare the remaining scores.
     * Some specializations then consider final series scores before they consider qualification series scores (see
     * {@link #getRuleA8_1ScoreComparator(boolean)} for details). This method tells whether or not to consider
     * discarded results in {@link #compareByBetterScore(Competitor, List, Competitor, List, Iterable, boolean, TimePoint, Leaderboard, Map, BiFunction, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)}.
     * This implementation returns {@code false}, thus implementing the default RRS A8.1 rule.
     */
    protected boolean isConsiderDiscardedScoresDuringBetterScoreTieBreak() {
        return false;
    }

    /**
     * Obtains a comparator that compares two non-discarded scores according the rule A8.1, or any
     * modification thereof (which subclasses can provide by overriding this method). This default implementation
     * simply uses the {@link #getScoreComparator(boolean) score comparator} to compare the net scores
     * directly.
     */
    protected Comparator<Pair<RaceColumn, Double>> getRuleA8_1ScoreComparator(boolean nullScoresAreBetter) {
        return (p1, p2)->getScoreComparator(nullScoresAreBetter).compare(p1.getB(), p2.getB());
    }

    /**
     * By default, this scoring scheme implementation does not compare the numbers of races scored.
     */
    @Override
    public int compareByNumberOfRacesScored(int competitor1NumberOfRacesScored, int competitor2NumberOfRacesScored) {
        return 0;
    }

    @Override
    public LeaderboardTotalRankComparator getOpeningSeriesRankComparator(Iterable<RaceColumn> raceColumnsToConsider,
            boolean nullScoresAreBetter, TimePoint timePoint,
            Leaderboard leaderboard,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        // as opening series we define everything that is not "medal race"
        final Iterable<RaceColumn> openingSeriesRaceColumnsFromThoseToConsider = Util.filter(raceColumnsToConsider, rc->!rc.isMedalRace());
        // pass on the totalPointsSupplier coming from the caller, most likely a LeaderboardTotalRankComparator,
        // to speed up / save the total points (re-)calculation
        final LeaderboardTotalRankComparator openingSeriesRankComparator =
                new LeaderboardTotalRankComparator(leaderboard, timePoint, this, nullScoresAreBetter, openingSeriesRaceColumnsFromThoseToConsider, totalPointsSupplier, cache);
        return openingSeriesRankComparator;
    }

    /**
     * Looks backwards starting at the last race until the first score difference is found, including the discarded
     * scores. This implements Racing Rules of Sailing (RRS) section A8.2:
     * <p>
     * 
     * <em>"A8.2 If a tie remains between two or more boats, they shall be ranked in order of their scores in the last
     * race. Any remaining ties shall be broken by using the tied boats' scores in the next-to-last race and so on until
     * all ties are broken. These scores shall be used even if some of them are excluded scores."</em>
     * 
     * @param o1ScoresIncludingDiscarded
     *            scores that include the points for those races that have been discarded (total points)
     * @param o2ScoresIncludingDiscarded
     *            scores that include the points for those races that have been discarded (total points)
     */
    @Override
    public int compareByLastRace(List<com.sap.sse.common.Util.Pair<RaceColumn, Double>> o1ScoresIncludingDiscarded,
            List<com.sap.sse.common.Util.Pair<RaceColumn, Double>> o2ScoresIncludingDiscarded,
            boolean nullScoresAreBetter, Competitor o1, Competitor o2, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        int result = 0;
        final Comparator<Double> pureScoreComparator = getScoreComparator(nullScoresAreBetter);
        ListIterator<Pair<RaceColumn, Double>> o1Iter = o1ScoresIncludingDiscarded.listIterator(o1ScoresIncludingDiscarded.size());
        ListIterator<Pair<RaceColumn, Double>> o2Iter = o2ScoresIncludingDiscarded.listIterator(o2ScoresIncludingDiscarded.size());
        while (result == 0 && o1Iter.hasPrevious() && o2Iter.hasPrevious()) {
            result = pureScoreComparator.compare(o1Iter.previous().getB(), o2Iter.previous().getB());
        }
        if (o1Iter.hasPrevious() != o2Iter.hasPrevious()) {
            // if, as may be allowed by some scoring scheme variants, competitors with different numbers of scored races are compared
            // and are equal for all races of the competitor who scored fewer races, the competitor who scored more races is preferred
            result = o1Iter.hasPrevious() ? -1 : 1;
        }
        return result;
    }

    protected int getNumberOfCompetitorsInBetterFleets(RaceColumn raceColumn, int competitorFleetOrdering) {
        int numberOfCompetitorsInBetterFleets = 0;
        for (Fleet fleet : raceColumn.getFleets()) {
            TrackedRace trackedRaceOfBetterFleet;
            if (fleet.getOrdering() != 0 && fleet.getOrdering() < competitorFleetOrdering &&
                    (trackedRaceOfBetterFleet=raceColumn.getTrackedRace(fleet)) != null) {
                numberOfCompetitorsInBetterFleets += Util.size(trackedRaceOfBetterFleet.getRace().getCompetitors());
            }
        }
        return numberOfCompetitorsInBetterFleets;
    }

    @Override
    public int compareByLatestRegattaInMetaLeaderboard(Leaderboard leaderboard, Competitor o1, Competitor o2, TimePoint timePoint) {
        return 0;
    }

    @Override
    public int compareByOtherTieBreakingLeaderboard(RegattaLeaderboardWithOtherTieBreakingLeaderboard leaderboard, Competitor o1, Competitor o2, TimePoint timePoint) {
        return 0;
    }
}
