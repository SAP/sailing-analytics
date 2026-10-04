package com.sap.sailing.domain.leaderboard.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

/**
 * A special low-point scoring scheme that uses one or more medal series to eliminate some competitors
 * during each such medal series. Competitors may be promoted to later series based on their opening
 * series results. For example, the scheme may define that the winner of the opening series automatically
 * advances to the final medal series, and the second and third ranking competitors advance to the last-but-one
 * medal series already; all others have to race a medal race/series to qualify for the next series, and
 * some of those will be eliminated on the way.<p>
 * 
 * The number of competitors from the opening series that qualify to later medal series is provided as
 * an array to the {@link #LowPointWithEliminatingMedalSeriesWithPromotions(int[]) constructor}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class LowPointWithEliminatingMedalSeriesWithPromotions extends LowPoint {
    private static final long serialVersionUID = 7759999270911627798L;
    
    /**
     * The last index for this array refers to the last medal race; e.g., the "Grand Final" race
     * in an iQFOil regatta with the medal series consisting of a "Quarter Final," a "Semi Final"
     * and a "Grand Final". The last-but-one index in the example would then refer to the Semi Final,
     * and so on. The array may be empty, meaning that no competitor is promoted into any medal
     * race. The field always has to refer to a valid array.
     */
    private final int[] numberOfPromotedCompetitorsIntoLastMedalRaces;

    /**
     * 
     * @param numberOfPromotedCompetitorsIntoLastMedalRaces
     *            The last index for this array refers to the last medal series; e.g., the "Grand Final" race in an iQFOil
     *            regatta with three medal series ("Quarter Final," "Semi Final" and "Grand Final"). The
     *            last-but-one index in the example would then refer to the Semi Final series, and so on. The array may be
     *            empty, meaning that no competitor is promoted into any medal race. The field always has to refer to a
     *            valid array.
     */
    public LowPointWithEliminatingMedalSeriesWithPromotions(int[] numberOfPromotedCompetitorsIntoLastMedalRaces) {
        super();
        if (numberOfPromotedCompetitorsIntoLastMedalRaces == null) {
            throw new NullPointerException("array specifying number of promoted competitors must not be null");
        }
        this.numberOfPromotedCompetitorsIntoLastMedalRaces = numberOfPromotedCompetitorsIntoLastMedalRaces;
    }
    
    @Override
    public ScoringSchemeType getType() {
        return super.getType();
    }

    /**
     * Counts the number of competitors in a medal series that, without sailing in it, will be ranked better
     * than those that do sail in it. This is because these many competitors have already advanced to later
     * medal series based on their opening series rank.
     */
    public int getNumberOfCompetitorsBetterThanThoseSailingInSeries(Series medalSeries) {
        final List<? extends Series> allMedalSeries = Util.asList(Util.filter(medalSeries.getRegatta().getSeries(), series->series.isMedal()));
        int result = 0;
        int indexInNumberOfPromotedCompetitorsIntoLastMedalRaces = numberOfPromotedCompetitorsIntoLastMedalRaces.length-1;
        if (allMedalSeries.size() > 1) {
            for (final ListIterator<? extends Series> i=allMedalSeries.listIterator(allMedalSeries.size()); i.hasPrevious() && i.previous() != medalSeries; ) {
                result += numberOfPromotedCompetitorsIntoLastMedalRaces[indexInNumberOfPromotedCompetitorsIntoLastMedalRaces--];
            }
        }
        return result;
    }
    
    public int getNumberOfCompetitorsAdvancingFromOpeningSeriesToOrThroughSeries(Series medalSeries) {
        final List<? extends Series> allMedalSeries = Util.asList(Util.filter(medalSeries.getRegatta().getSeries(), series->series.isMedal()));
        int result = 0;
        if (numberOfPromotedCompetitorsIntoLastMedalRaces.length > 0) {
            int indexInNumberOfPromotedCompetitorsIntoLastMedalRaces = numberOfPromotedCompetitorsIntoLastMedalRaces.length;
            if (!allMedalSeries.isEmpty()) {
                result = numberOfPromotedCompetitorsIntoLastMedalRaces[--indexInNumberOfPromotedCompetitorsIntoLastMedalRaces];
                for (final ListIterator<? extends Series> i=allMedalSeries.listIterator(allMedalSeries.size());
                     indexInNumberOfPromotedCompetitorsIntoLastMedalRaces > 0 && i.hasPrevious() && i.previous() != medalSeries; ) {
                    result += numberOfPromotedCompetitorsIntoLastMedalRaces[--indexInNumberOfPromotedCompetitorsIntoLastMedalRaces];
                }
            }
        }
        return result;
    }
    
    /**
     * In addition to the default implementation (assumed to check for a non-{@code null} medal race score}, this
     * specialized implementation is aware of the promotion scheme and defines competitors as "participants" of the
     * medal race represented by {@code medalRaceColumn} if their opening series rank was
     * {@link #getNumberOfCompetitorsBetterThanThoseSailingInSeries(RaceColumnInSeries) good enough} to have been promoted
     * to {@code medalRaceColumn}'s or a later medal series already. In this case it is not necessary for the competitor
     * to have <em>scored</em> in {@code medalRaceColumn} yet.<p>
     * 
     * Note: Since the promotion decision is made based on the opening series rank, promotion is considered even if
     * more opening series races are to come. This, however, will not change the results because the promotion scheme
     * will keep the rank ordering among those promoted.
     */
    @Override
    public boolean isParticipatingInMedalRace(Competitor competitor, Double competitorMedalRaceScore,
            RaceColumnInSeries medalRaceColumn, Supplier<Map<Competitor, Integer>> competitorsRankedByOpeningSeries) {
        final Integer openingSeriesRank;
        return super.isParticipatingInMedalRace(competitor, competitorMedalRaceScore, medalRaceColumn, competitorsRankedByOpeningSeries) ||
                ((openingSeriesRank = competitorsRankedByOpeningSeries.get().get(competitor)) != null &&
                 openingSeriesRank <= getNumberOfCompetitorsAdvancingFromOpeningSeriesToOrThroughSeries(medalRaceColumn.getSeries()));
    }
    
    /**
     * The sum of medal race scores is not of interest to ranking when using this scoring scheme. Instead, the previous
     * medal series, or if already at the first medal series, the opening series will be used for tie-breaking.
     */
    @Override
    public int compareByMedalRaceScore(Competitor o1, Competitor o2, Double o1MedalRaceScore, Double o2MedalRaceScore, List<Pair<RaceColumn, Double>> o1ScoringMedalRaces, List<Pair<RaceColumn, Double>> o2ScoringMedalRaces, TimePoint timePoint, Leaderboard leaderboard, Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor, BiFunction<Competitor, RaceColumn, Double> totalPointSupplier, boolean nullScoresAreBetter, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        return 0;
    }

    /**
     * Both competitors competed in or were advanced through/to the same medal series. If during the last medal series
     * in which one of them scored they both raced, their scores can be compared in the regular way, as implemented by
     * the superclass. If one of the two did not race because they were advanced already to the next medal series, and
     * the other one scored in the medal series, the one advanced through to the next series already will rank better.
     * If both did not sail / score in the last series they are compared by the previous series.
     * 
     * @param haveValidMedalRaceScores
     *            if {@code true} then this means that both raced in or were advanced to a medal series
     */
    @Override
    public int compareByScoreSum(Competitor o1, List<Pair<RaceColumn, Double>> o1Scores, double o1ScoreSum,
            Competitor o2, List<Pair<RaceColumn, Double>> o2Scores, double o2ScoreSum, boolean nullScoresAreBetter,
            boolean haveValidMedalRaceScores, Supplier<Map<Competitor, Integer>> competitorsRankedByOpeningSeries) {
        final int result;
        final Series lastSeriesO1ScoredIn = getLastSeriesCompetitorScoredIn(o1Scores);
        final Series lastSeriesO2ScoredIn = getLastSeriesCompetitorScoredIn(o2Scores);
        // do regular score comparison if both scored only in non-medal series or up to the same medal series:
        if (lastSeriesO1ScoredIn == lastSeriesO2ScoredIn ||
                ((lastSeriesO1ScoredIn == null || !lastSeriesO1ScoredIn.isMedal()) &&
                 (lastSeriesO2ScoredIn == null || !lastSeriesO2ScoredIn.isMedal()))) {
            result = super.compareByScoreSum(o1, o1Scores, o1ScoreSum, o2, o2Scores, o2ScoreSum, nullScoresAreBetter, haveValidMedalRaceScores, competitorsRankedByOpeningSeries);
        } else {
            // at least one competitor scored in a medal series that is assumed to come after all non-medal series;
            final int o1ZeroBasedLastScoredSeriesIndex = lastSeriesO1ScoredIn == null ? -1 :
                Util.indexOf(lastSeriesO1ScoredIn.getRegatta().getSeries(), lastSeriesO1ScoredIn);
            final int o2ZeroBasedLastScoredSeriesIndex = lastSeriesO2ScoredIn == null ? -1 :
                Util.indexOf(lastSeriesO2ScoredIn.getRegatta().getSeries(), lastSeriesO2ScoredIn);
            final int openingSeriesRankO1 = competitorsRankedByOpeningSeries.get().get(o1);
            final int openingSeriesRankO2 = competitorsRankedByOpeningSeries.get().get(o2);
            final int numberOfCompetitorsBetterThanThoseSailingInLastSeriesAnyOfTheTwoCompetitorsScoredIn;
            assert o1ZeroBasedLastScoredSeriesIndex != o2ZeroBasedLastScoredSeriesIndex;
            if (o1ZeroBasedLastScoredSeriesIndex > o2ZeroBasedLastScoredSeriesIndex) {
                numberOfCompetitorsBetterThanThoseSailingInLastSeriesAnyOfTheTwoCompetitorsScoredIn = getNumberOfCompetitorsBetterThanThoseSailingInSeries(lastSeriesO1ScoredIn);
                if (openingSeriesRankO2 <= numberOfCompetitorsBetterThanThoseSailingInLastSeriesAnyOfTheTwoCompetitorsScoredIn) {
                    result = 1; // o2 is better than o1 because their opening series rank has promoted them through the series o1 scored in last
                } else {
                    result = -1; // o1 is better because o2 hasn't sailed in that series, but not because promoted through but not qualified
                }
            } else {
                numberOfCompetitorsBetterThanThoseSailingInLastSeriesAnyOfTheTwoCompetitorsScoredIn = getNumberOfCompetitorsBetterThanThoseSailingInSeries(lastSeriesO2ScoredIn);
                if (openingSeriesRankO1 <= numberOfCompetitorsBetterThanThoseSailingInLastSeriesAnyOfTheTwoCompetitorsScoredIn) {
                    result = -1; // o1 is better than o2 because their opening series rank has promoted them through the series o2 scored in last
                } else {
                    result = 1; // o2 is better because o1 hasn't sailed in that series, but not because promoted through but not qualified
                }
            }
        }
        return result;
    }

    private Series getLastSeriesCompetitorScoredIn(List<Pair<RaceColumn, Double>> competitorScores) {
        Series result = null;
        for (final ListIterator<Pair<RaceColumn, Double>> i=competitorScores.listIterator(competitorScores.size()); i.hasPrevious(); ) {
            final Pair<RaceColumn, Double> score = i.previous();
            if (score.getB() != null && score.getA() instanceof RaceColumnInSeries) {
                result = ((RaceColumnInSeries) score.getA()).getSeries();
                break;
            }
        }
        return result;
    }

    /**
     * If two competitors are tied, check if the tie is in a medal series, and if so, compare again with a new
     * {@link LeaderboardTotalRankComparator}, but with all races from that medal series removed from the race columns
     * to consider. If the tie was in the first medal series then this will resort to comparing by the opening series
     * rank. If the tie was not in a medal series, use the default implementation from the superclass.
     */
    @Override
    public int compareByBetterScore(Competitor o1, List<Pair<RaceColumn, Double>> o1Scores, Competitor o2,
            List<Pair<RaceColumn, Double>> o2Scores, Iterable<RaceColumn> raceColumnsToConsider,
            boolean nullScoresAreBetter, TimePoint timePoint, Leaderboard leaderboard,
            Map<Competitor, Set<RaceColumn>> discardedRaceColumnsPerCompetitor,
            BiFunction<Competitor, RaceColumn, Double> totalPointsSupplier,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final int result;
        final Pair<RaceColumn, Double> lastColumnO1ScoredIn = Util.last(Util.filter(o1Scores, o1s->o1s.getB() != null));
        final Pair<RaceColumn, Double> lastColumnO2ScoredIn = Util.last(Util.filter(o1Scores, o2s->o2s.getB() != null));
        final RaceColumnInSeries lastMedalRaceColumnScored;
        if ((lastColumnO1ScoredIn != null && lastColumnO1ScoredIn.getA().isMedalRace()) && lastColumnO1ScoredIn.getA() instanceof RaceColumnInSeries) {
            lastMedalRaceColumnScored = (RaceColumnInSeries) lastColumnO1ScoredIn.getA();
        } else if ((lastColumnO2ScoredIn != null && lastColumnO2ScoredIn.getA().isMedalRace()) && lastColumnO2ScoredIn.getA() instanceof RaceColumnInSeries) {
            lastMedalRaceColumnScored = (RaceColumnInSeries) lastColumnO2ScoredIn.getA();
        } else {
            lastMedalRaceColumnScored = null;
        }
        if (lastMedalRaceColumnScored != null) {
            final List<RaceColumn> raceColumnsToConsiderWithoutThoseOfLastMedalSeriesToConsider = new ArrayList<>();
            for (final RaceColumn raceColumnToConsider : raceColumnsToConsider) {
                if (raceColumnToConsider instanceof RaceColumnInSeries
                        && ((RaceColumnInSeries) raceColumnToConsider).getSeries() == lastMedalRaceColumnScored.getSeries()) {
                    break;
                }
                raceColumnsToConsiderWithoutThoseOfLastMedalSeriesToConsider.add(raceColumnToConsider);
            }
            result = new LeaderboardTotalRankComparator(leaderboard, timePoint, this, nullScoresAreBetter,
                    raceColumnsToConsiderWithoutThoseOfLastMedalSeriesToConsider, totalPointsSupplier, cache).compare(o1, o2);
        } else {
            result = super.compareByBetterScore(o1, o1Scores, o2, o2Scores, raceColumnsToConsider, nullScoresAreBetter,
                    timePoint, leaderboard, discardedRaceColumnsPerCompetitor, totalPointsSupplier, cache);
        }
        return result;
    }
}
