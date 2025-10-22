package com.sap.sailing.domain.leaderboard;

import java.io.Serializable;
import java.util.concurrent.Callable;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;

/**
 * Manages score corrections for a competitor in a race, in particular handling the following use cases:
 * <ul>
 * <li>competitor disqualified: maximum points will be granted to the competitor for that race</li>
 * <li>imprecise tracking for finish line: jury changed final rankings; usually several competitors affected</li>
 * </ul>
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface ScoreCorrection extends Serializable {
    /**
     * A score correction as valid at the time point specified with the query that produced this object.
     * 
     * @author Axel Uhl (d043530)
     * 
     */
    public interface Result {
        /**
         * @return <code>null</code> in case there is no score attributed to the competitor for the race column in
         *         question. This can, e.g., mean that the race hasn't started yet or the competitor wasn't enlisted in
         *         the race at all.
         */
        Double getCorrectedScore();
        
        Double getUncorrectedScore();

        MaxPointsReason getMaxPointsReason();
        
        Double getIncrementalScoreCorrectionInPoints();

        boolean isCorrected();
        
        boolean isCorrectedIncrementally();
        
        /**
         * @return the time point for which this result is valid
         */
        TimePoint getTimePoint();
    }
    
    /**
     * Returns the effective score for the <code>competitor</code> scored in race <code>trackedRace</code>. If no
     * explicit correction has been recorded in this score correction object, the uncorrected score will be returned,
     * and {@link MaxPointsReason#NONE} will be listed as the {@link Result#getMaxPointsReason() correction reason}.
     * Note, though, that {@link MaxPointsReason#NONE} can also be the reason for an explicit score correction, e.g., if
     * the tracking results were overruled by the jury. Clients may use
     * {@link #isScoreCorrected(Competitor, TrackedRace, TimePoint)} to detect the difference.
     * 
     * @param trackedRankProvider
     *            can provide the tracked rank if needed; will only be called if there is no score correction for the
     *            <code>timePoint</code> specified.
     * @param timePoint
     *            the time point for which to get the corrected score; score corrections have a validity time interval.
     *            Only the last score correction valid at <code>timePoint</code> is considered.
     * @param numberOfCompetitorsFetcher
     *            can determine the number of competitors to use as the basis for penalty score calculation
     *            ("max points") if needed
     */
    default Result getCorrectedScore(Callable<Integer> trackedRankProvider, Competitor competitor, RaceColumn raceColumn,
            Leaderboard leaderboard, TimePoint timePoint, NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsFetcher, ScoringScheme scoringScheme) {
        return getCorrectedScore(trackedRankProvider, competitor, raceColumn, leaderboard, timePoint,
                numberOfCompetitorsFetcher, scoringScheme, new LeaderboardDTOCalculationReuseCache(timePoint));
    }

    Result getCorrectedScore(Callable<Integer> trackedRankProvider, Competitor competitor, RaceColumn raceColumn,
            Leaderboard leaderboard, TimePoint timePoint,
            NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsFetcher, ScoringScheme scoringScheme,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache);

    /**
     * @param timePoint
     *            the time point at which the max points reason is to be valid
     * 
     * @return a non-<code>null</code> result
     */
    MaxPointsReason getMaxPointsReason(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint);

    /**
     * Note the difference between what this method does and a more naive comparison of uncorrected and corrected score.
     * Should, for some reason, the uncorrected score change later, an existing score correction would still remain in
     * place whereas if no score correction exists for the competitor/race combination, the resulting score after
     * "correction" will still be the uncorrected value.
     * <p>
     * 
     * @param timePoint
     *            the time point for which to check whether the competitor's score was corrected at that time point; in
     *            other words, this method checks if there is a score correction for <code>competitor</code> in column
     *            <code>raceColumn</code> that is valid at <code>timePoint</code>.
     * 
     * @return if an explicit score correction was made for the combination of <code>competitor</code> and
     *         <code>raceColumn</code>
     */
    boolean isScoreCorrected(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint);

    boolean isScoreCorrectedIncrementally(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint);
    
    Double getIncementalScoreCorrectionInPoints(Competitor competitor, RaceColumn raceColumn);
    
    /**
     * Checks if this score correction object has any score corrections for any competitor valid at any time point for
     * the race column specified by <code>raceInLeaderboard</code>.
     */
    boolean hasCorrectionFor(RaceColumn raceInLeaderboard);
    
    /**
     * Similar to {@link #hasCorrectionFor(RaceColumn)}, but returns <code>true</code> only if score corrections are found
     * for any competitor who in <code>raceInLeaderboard</code> is not in a tracked race and hence the fleet assignment
     * cannot be determined. This is helpful, e.g., for progress detection. If score corrections are present for such
     * untracked competitors then all untracked fleets need to be assumed as finished.
     * @param fleet TODO
     */
    boolean hasCorrectionForNonTrackedFleet(RaceColumn raceInLeaderboard, Fleet fleet);
    
    /**
     * @return all race columns for which this score corrections object has at least one correction; note that this
     *         object may hold corrections also for competitors that are not currently returned by
     *         {@link Leaderboard#getCompetitors()}, e.g., because one or more tracked races are not currently attached
     *         to the leaderboard. It is still useful to retain these corrections as the race(s) may re-appear later on.
     *         In particular, those corrections should be stored persistently.
     */
    Iterable<RaceColumn> getRaceColumnsThatHaveCorrections();

    /**
     * @return all competitors for which this score corrections object has at least one correction in column
     *         <code>raceColumn</code>; note that this object may hold corrections also for competitors that are not
     *         currently returned by {@link Leaderboard#getCompetitors()}, e.g., because one or more tracked races are
     *         not currently attached to the leaderboard. It is still useful to retain these corrections as the race(s)
     *         may re-appear later on. In particular, those corrections should be stored persistently.
     */
    Iterable<Competitor> getCompetitorsThatHaveCorrectionsIn(RaceColumn raceColumn);

    /**
     * Tells when the score correction was last updated. This should usually be the "validity time" and not the
     * "transaction time." In other words, if scores provided by the race committee are updated to this score correction
     * at time X, and the race committee's scores are tagged with time Y, then this method should return Y, not X. If
     * Y is not available for some reason, X may be used as a default.
     */
    TimePoint getTimePointOfLastCorrectionsValidity();
    
    /**
     * A free-form comment to display to the viewers of the leaderboard that has these score corrections. It should make
     * crystal clear if the scores are preliminary or not yet jury-finalized. If <code>null</code> is returned, this
     * always has to be interpreted as "preliminary" because then no comment as to the correctness have been made.
     */
    String getComment();
}
