package com.sap.sailing.domain.leaderboard.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;
import com.sap.sailing.domain.leaderboard.ScoreCorrectionListener;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

/**
 * Implements the basic logic of assigning a maximum score to a competitor in a race if that competitor was
 * disqualified, did not start or did not finish. The maximum score is determined by counting the number of competitors
 * listed in the regatta to which the race belongs.
 * <p>
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class ScoreCorrectionImpl implements SettableScoreCorrection {
    private static final long serialVersionUID = -7088305215528928135L;
    private static final Logger logger = Logger.getLogger(ScoreCorrectionImpl.class.getName());

    /**
     * If no max point reason is provided for a competitor/race, {@link MaxPointsReason#NONE} should be the default.
     */
    private final ConcurrentMap<Pair<Competitor, RaceColumn>, MaxPointsReason> maxPointsReasons;

    /**
     * If no score correction is provided here, the uncorrected points are the default.
     */
    private final ConcurrentMap<Pair<Competitor, RaceColumn>, Double> correctedScores;
    
    /**
     * If a {@link #correctedScores fixed corrected score} exists, it is used in
     * {@link #getCorrectedScore(Callable, Competitor, RaceColumn, Leaderboard, TimePoint, NumberOfCompetitorsInLeaderboardFetcher, ScoringScheme, WindLegTypeAndLegBearingAndORCPerformanceCurveCache)}
     * if any {@link #maxPointsReasons} suggests that the correction shall be applied (e.g., after the end of the race
     * only). If no absolute {@link #correctedScores corrected score} is set for a competitor in a race column or the
     * {@link #maxPointsReasons} suggests that it doesn't apply at the time point in question, an incremental score
     * correction may apply.
     * <p>
     * 
     * Incremental score corrections are added to the score derived from the ranking at the given point in time, before
     * applying a column factor. This is independent of the use of {@link LowPoint} or {@link HighPoint} scoring scheme
     * variants, so for penalties in {@link HighPoint} schemes values should probably be negative. Column factor
     * application may be contradicting the Notice of Race / Sailing Instructions for a specific event where it may
     * read, e.g., that {@link MaxPointsReason#STP} penalties shall be applied <em>after</em> doubling a medal race's
     * score. In such cases values divided by the column factor must be used so that the final result matches the
     * expected incremental offset again.
     * <p>
     * 
     * @since 2023-07-25; this means that de-serializing an object written by an older version of this class will lead
     *        to serious problems, e.g., to {@link NullPointerException}s when trying to access this field.
     */
    private final ConcurrentMap<Pair<Competitor, RaceColumn>, Double> incrementalScoreCorrection;

    /**
     * If <code>null</code>, despite a non-<code>null</code> {@link #timePointOfLastCorrectionsValidity} value the
     * result have to be assumed to be preliminary and need to be displayed with a corresponding hint.
     */
    private String comment;

    /**
     * Tells when the score correction was last updated. This should usually be the "validity time" and not the
     * "transaction time." In other words, if scores provided by the race committee are updated to this score correction
     * at time X, and the race committee's scores are tagged with time Y, then this method should return Y, not X. If Y
     * is not available for some reason, X may be used as a default.
     */
    private TimePoint timePointOfLastCorrectionsValidity;

    private transient Set<ScoreCorrectionListener> scoreCorrectionListeners;
    
    private final Leaderboard leaderboard;

    public ScoreCorrectionImpl(Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
        this.maxPointsReasons = new ConcurrentHashMap<>();
        this.correctedScores = new ConcurrentHashMap<>();
        this.incrementalScoreCorrection = new ConcurrentHashMap<>();
        this.scoreCorrectionListeners = new HashSet<ScoreCorrectionListener>();
    }

    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        this.scoreCorrectionListeners = new HashSet<ScoreCorrectionListener>();
    }
    
    private Set<ScoreCorrectionListener> getScoreCorrectionListeners() {
        synchronized (scoreCorrectionListeners) {
            return new HashSet<ScoreCorrectionListener>(scoreCorrectionListeners);
        }
    }

    @Override
    public void addScoreCorrectionListener(ScoreCorrectionListener listener) {
        synchronized (scoreCorrectionListeners) {
            scoreCorrectionListeners.add(listener);
        }
    }

    @Override
    public void removeScoreCorrectionListener(ScoreCorrectionListener listener) {
        synchronized (scoreCorrectionListeners) {
            scoreCorrectionListeners.remove(listener);
        }
    }

    protected void notifyListeners(Competitor competitor, RaceColumn raceColumn, Double oldCorrectedScore, Double newCorrectedScore) {
        for (ScoreCorrectionListener listener : getScoreCorrectionListeners()) {
            listener.correctedScoreChanged(competitor, raceColumn, oldCorrectedScore, newCorrectedScore);
        }
    }
    
    protected void notifyListenersAboutIncrementalScoreChange(Competitor competitor, RaceColumn raceColumn, Double oldScoreOffsetInPoints, Double newScoreOffsetInPoints) {
        for (ScoreCorrectionListener listener : getScoreCorrectionListeners()) {
            listener.incrementalScoreCorrectionChanged(competitor, raceColumn, oldScoreOffsetInPoints, newScoreOffsetInPoints);
        }
    }

    protected void notifyListeners(Competitor competitor, RaceColumn raceColumn,
            MaxPointsReason oldMaxPointsReason, MaxPointsReason newMaxPointsReason) {
        for (ScoreCorrectionListener listener : getScoreCorrectionListeners()) {
            listener.maxPointsReasonChanged(competitor, raceColumn, oldMaxPointsReason, newMaxPointsReason);
        }
    }

    @Override
    public void notifyListenersAboutCarriedPointsChange(Competitor competitor, Double oldCarriedPoints, Double newCarriedPoints) {
        for (ScoreCorrectionListener listener : getScoreCorrectionListeners()) {
            listener.carriedPointsChanged(competitor, oldCarriedPoints, newCarriedPoints);
        }
    }

    @Override
    public void notifyListenersAboutIsSuppressedChange(Competitor competitor, boolean suppressed) {
        for (ScoreCorrectionListener listener : getScoreCorrectionListeners()) {
            listener.isSuppressedChanged(competitor, suppressed);
        }
    }

    @Override
    public void notifyListenersAboutLastCorrectionsValidityChanged(TimePoint oldTimePointOfLastCorrectionsValidity,
            TimePoint newTimePointOfLastCorrectionsValidity) {
        for (ScoreCorrectionListener listener : getScoreCorrectionListeners()) {
            listener.timePointOfLastCorrectionsValidityChanged(oldTimePointOfLastCorrectionsValidity, newTimePointOfLastCorrectionsValidity);
        }
    }

    @Override
    public void notifyListenersAboutCommentChanged(String oldComment, String newComment) {
        for (ScoreCorrectionListener listener : getScoreCorrectionListeners()) {
            listener.commentChanged(oldComment, newComment);
        }
    }

    @Override
    public void setMaxPointsReason(Competitor competitor, RaceColumn raceColumn, MaxPointsReason reason) {
        Pair<Competitor, RaceColumn> key = raceColumn.getKey(competitor);
        MaxPointsReason oldMaxPointsReason;
        if (reason == null) {
            oldMaxPointsReason = maxPointsReasons.remove(key);
        } else {
            oldMaxPointsReason = maxPointsReasons.put(key, reason);
        }
        notifyListeners(competitor, raceColumn, oldMaxPointsReason, reason);
    }

    @Override
    public void correctScore(Competitor competitor, RaceColumn raceColumn, double points) {
        Double oldScore = correctedScores.put(raceColumn.getKey(competitor), points);
        notifyListeners(competitor, raceColumn, oldScore, points);
    }

    @Override
    public boolean isScoreCorrected(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        final Pair<Competitor, RaceColumn> key = raceColumn.getKey(competitor);
        return (correctedScores.containsKey(key) && !isCertainlyBeforeRaceFinish(timePoint, raceColumn, competitor))
                || (maxPointsReasons.containsKey(key) && isMaxPointsReasonApplicable(maxPointsReasons.get(key), timePoint, raceColumn, competitor));
    }

    @Override
    public void uncorrectScore(Competitor competitor, RaceColumn raceColumn) {
        final Double oldScore = correctedScores.remove(raceColumn.getKey(competitor));
        notifyListeners(competitor, raceColumn, oldScore, null);
    }
    
    @Override
    public void correctScoreIncrementally(Competitor competitor, RaceColumn raceColumn, double scoreOffsetInPoints) {
        final Double oldScoreOffsetInPoints = incrementalScoreCorrection.put(raceColumn.getKey(competitor), scoreOffsetInPoints);
        notifyListenersAboutIncrementalScoreChange(competitor, raceColumn, oldScoreOffsetInPoints, scoreOffsetInPoints);
    }

    @Override
    public boolean isScoreCorrectedIncrementally(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        final Pair<Competitor, RaceColumn> key = raceColumn.getKey(competitor);
        return (incrementalScoreCorrection.containsKey(key) && !isCertainlyBeforeRaceFinish(timePoint, raceColumn, competitor))
                || (maxPointsReasons.containsKey(key) && isMaxPointsReasonApplicable(maxPointsReasons.get(key), timePoint, raceColumn, competitor));
    }

    @Override
    public Double getIncementalScoreCorrectionInPoints(Competitor competitor, RaceColumn raceColumn) {
        return incrementalScoreCorrection.get(raceColumn.getKey(competitor));
    }

    @Override
    public void uncorrectScoreIncrementally(Competitor competitor, RaceColumn raceColumn) {
        final Double oldScoreOffsetInPoints = incrementalScoreCorrection.remove(raceColumn.getKey(competitor));
        notifyListenersAboutIncrementalScoreChange(competitor, raceColumn, oldScoreOffsetInPoints, null);
    }
    
    /**
     * Based on the order of the {@link Leaderboard#getRaceColumns() race columns} in the {@link #getLeaderboard()
     * leaderboard to which this score correction object belongs}, tries to determine whether the <code>timePoint</code>
     * is before the start of <code>competitor</code>'s race in the race column specified by <code>raceColumn</code>. If
     * there is a {@link RaceColumn#getTrackedRace(Competitor) tracked race for the competitor associated with the race
     * column}, that race's start time is used for the calculation. Otherwise, if there is a tracked race column prior
     * to <code>raceColumn</code> in the leaderboard in which <code>competitor</code> competes, and the competitor
     * hasn't finished that race at <code>timePoint</code>, we also know that this must be before the start of
     * <code>competitor</code>'s race in <code>raceColumn</code> because we assume that the same competitor can only
     * compete in one race at a time within a single leaderboard.
     * <p>
     * 
     * In all other cases, <code>false</code> is returned which can either mean that <code>timePoint</code> is certainly
     * known to be after the race start of <code>competitor</code> in the race for <code>raceColumn</code>, or we just
     * can't tell, e.g., because <code>competitor</code>'s race for <code>raceColumn</code> is not tracked, and
     * <code>timePoint</code> is after all prior race column's finishing time for <code>competitor</code>.
     * <p>
     * 
     * This method can be used to decide whether to apply a {@link MaxPointsReason#DNC}, {@link MaxPointsReason#DNS} or
     * {@link MaxPointsReason#OCS} correction for <code>competitor</code> at <code>timePoint</code> in the race for
     * <code>raceColumn</code>, because the correction can be applied at race start but should not be applied any
     * earlier than that because it would incorrectly influence the total scores displayed for the competitor during
     * earlier races.
     */
    private boolean isCertainlyBeforeRaceStart(TimePoint timePoint, RaceColumn raceColumn, Competitor competitor) {
        final boolean result;
        TrackedRace trackedRace = raceColumn.getTrackedRace(competitor);
        final TimePoint startOfRace;
        if (trackedRace != null && (startOfRace = trackedRace.getStartOfRace()) != null) {
            result = timePoint.before(startOfRace);
        } else {
            boolean preResult = false;
            for (RaceColumn rc : getLeaderboard().getRaceColumns()) {
                if (rc == raceColumn) {
                    break;
                }
                TrackedRace rcTrackedRace = rc.getTrackedRace(competitor);
                if (rcTrackedRace != null) {
                    NavigableSet<MarkPassing> markPassings = rcTrackedRace.getMarkPassings(competitor);
                    if (markPassings != null && !markPassings.isEmpty()) {
                        MarkPassing lastMarkPassing = markPassings.last();
                        if (timePoint.before(lastMarkPassing.getTimePoint())) {
                            preResult = true;
                            break;
                        }
                    } else {
                        // if available, use the end of the race as indicator for how long competitor may have been in the race
                        TimePoint endOfRace = rcTrackedRace.getEndOfRace();
                        if (endOfRace != null && timePoint.before(endOfRace)) {
                            preResult = true;
                            break;
                        }
                    }
                }
            }
            result = preResult;
        }
        return result;
    }
    
    /**
     * Based on the order of the {@link Leaderboard#getRaceColumns() race columns} in the {@link #getLeaderboard()
     * leaderboard to which this score correction object belongs}, tries to determine whether the <code>timePoint</code>
     * is before the finish or abandoning of <code>competitor</code>'s race in the race column specified by
     * <code>raceColumn</code>. If there is a {@link RaceColumn#getTrackedRace(Competitor) tracked race for the
     * competitor associated with the race column}, that race's finish time for <code>competitor</code> (if defined) or
     * the {@link TrackedRace#getEndOfRace() end of the race} is used for the calculation. Otherwise, if there is a
     * tracked race column prior to <code>raceColumn</code> in the leaderboard in which <code>competitor</code>
     * competes, and the competitor has finished that race after <code>timePoint</code>, we also know that this must be
     * before the end of <code>competitor</code>'s race in <code>raceColumn</code> because we assume that the same
     * competitor can only compete in one race at a time within a single leaderboard.
     * <p>
     * 
     * In all other cases, <code>false</code> is returned which can either mean that <code>timePoint</code> is certainly
     * known to be after the race finish of <code>competitor</code> in the race for <code>raceColumn</code>, or we just
     * can't tell, e.g., because <code>competitor</code>'s race for <code>raceColumn</code> is not tracked, and
     * <code>timePoint</code> is after all prior race column's finishing time for <code>competitor</code>.
     * <p>
     * 
     * This method can be used to decide whether to apply a score correction for <code>competitor</code> at
     * <code>timePoint</code> in the race for <code>raceColumn</code>, because the correction should not be applied
     * before race end because it would incorrectly influence the total scores displayed for the competitor during
     * earlier races.
     */
    private boolean isCertainlyBeforeRaceFinish(TimePoint timePoint, RaceColumn raceColumn, Competitor competitor) {
        return isCertainlyBeforeRaceFinish(timePoint, raceColumn, competitor, new LeaderboardDTOCalculationReuseCache(timePoint));
    }
    
    /**
     * Same as {@link #isCertainlyBeforeRaceFinish(TimePoint, RaceColumn, Competitor)}, but with the possibility to
     * specify a re-use cache, introduced particularly for speeding up the {@link Course#getLastWaypoint()} lookup which
     * is expected to yield the same result for the entire leaderboard calculation round-trip, however upon each
     * invocation requires locking.
     */
    private boolean isCertainlyBeforeRaceFinish(TimePoint timePoint, RaceColumn raceColumn, Competitor competitor, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final boolean result;
        TrackedRace trackedRace = raceColumn.getTrackedRace(competitor);
        final TimePoint endOfRace = trackedRace == null ? null : trackedRace.getFinishedTime() == null ? trackedRace.getEndOfRace() : trackedRace.getFinishedTime();
        // endOfRace != null means we at least will have a fallback result even if there are no mark passings for the competitor
        if (endOfRace != null) {
            NavigableSet<MarkPassing> markPassings = trackedRace.getMarkPassings(competitor);
            final MarkPassing lastMarkPassing;
            // count race as finished for the competitor if the finish mark passing exists or the time is after the end of race
            if (markPassings != null && !markPassings.isEmpty() &&
                    (lastMarkPassing = markPassings.last()).getWaypoint() == trackedRace.getRace().getCourse().getLastWaypoint()) {
                result = timePoint.before(lastMarkPassing.getTimePoint());
            } else {
                // if available, use the end of the race as indicator for how long competitor may have been in the race
                result = timePoint.before(endOfRace);
            }
        } else {
            boolean preResult = false;
            for (RaceColumn rc : getLeaderboard().getRaceColumns()) {
                if (rc == raceColumn) {
                    break;
                }
                TrackedRace rcTrackedRace = rc.getTrackedRace(competitor);
                if (rcTrackedRace != null) {
                    NavigableSet<MarkPassing> markPassings = rcTrackedRace.getMarkPassings(competitor);
                    if (markPassings != null && !markPassings.isEmpty()) {
                        MarkPassing lastMarkPassing = markPassings.last();
                        if (timePoint.before(lastMarkPassing.getTimePoint())) {
                            preResult = true;
                            break;
                        }
                    } else {
                        // if available, use the end of the race as indicator for how long competitor may have been in the race
                        TimePoint rcEndOfRace = rcTrackedRace.getEndOfRace();
                        if (rcEndOfRace != null) {
                            if (timePoint.before(rcEndOfRace)) {
                                preResult = true;
                                break;
                            }
                        } else {
                            TimePoint rcStartOfRace = rcTrackedRace.getStartOfRace();
                            if (rcStartOfRace != null && timePoint.before(rcStartOfRace)) {
                                preResult = true;
                                break;
                            }
                        }
                    }
                }
            }
            result = preResult;
        }
        return result;
    }
    
    @Override
    public MaxPointsReason getMaxPointsReason(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        return getAnnotatedMaxPointsReason(competitor, raceColumn, timePoint).getMaxPointsReason();
    }

    private static class AnnotatedMaxPointsReason {
        private final MaxPointsReason maxPointsReason;
        private final boolean maxPointsReasonExistsButIsNotApplicableForTimePoint;
        private final boolean calculateScoreDuringRace;
        public AnnotatedMaxPointsReason(MaxPointsReason maxPointsReason,
                boolean maxPointsReasonExistsButIsNotApplicableForTimePoint, boolean calculateScoreDuringRace) {
            super();
            this.maxPointsReason = maxPointsReason;
            this.maxPointsReasonExistsButIsNotApplicableForTimePoint = maxPointsReasonExistsButIsNotApplicableForTimePoint;
            this.calculateScoreDuringRace = calculateScoreDuringRace;
        }
        public MaxPointsReason getMaxPointsReason() {
            return maxPointsReason;
        }
        public boolean isMaxPointsReasonExistsButIsNotApplicableForTimePoint() {
            return maxPointsReasonExistsButIsNotApplicableForTimePoint;
        }
        /**
         * If {@code true}, the score is to be calculated and not to be taken from an "official" score correction.
         * A typical example is an {@link MaxPointsReason#STP} which will usually be an incremental scoring penalty
         * which will get added to the score obtained based on the current rank. A corrected final score will be
         * used only after the competitor has finished.
         */
        public boolean isCalculateScoreDuringRace() {
            return calculateScoreDuringRace;
        }
    }
    
    protected AnnotatedMaxPointsReason getAnnotatedMaxPointsReason(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        MaxPointsReason maxPointsReason = maxPointsReasons.get(raceColumn.getKey(competitor));
        final boolean maxPointsReasonExistsButIsNotApplicableForTimePoint;
        final boolean calculateScoreDuringRace;
        if (maxPointsReason == null) {
            maxPointsReason = MaxPointsReason.NONE;
            maxPointsReasonExistsButIsNotApplicableForTimePoint = false;
            calculateScoreDuringRace = false;
        } else if (!isMaxPointsReasonApplicable(maxPointsReason, timePoint, raceColumn, competitor)) {
            maxPointsReason = MaxPointsReason.NONE;
            maxPointsReasonExistsButIsNotApplicableForTimePoint = true;
            calculateScoreDuringRace = false;
        } else {
            maxPointsReasonExistsButIsNotApplicableForTimePoint = false;
            calculateScoreDuringRace = maxPointsReason.isCalculateScoreDuringRace() && isCertainlyBeforeRaceFinish(timePoint, raceColumn, competitor);
        }
        return new AnnotatedMaxPointsReason(maxPointsReason, maxPointsReasonExistsButIsNotApplicableForTimePoint, calculateScoreDuringRace);
    }

    private boolean isMaxPointsReasonApplicable(MaxPointsReason maxPointsReason, TimePoint timePoint, RaceColumn raceColumn, Competitor competitor) {
        return (maxPointsReason.isAppliesAtStartOfRace() && !isCertainlyBeforeRaceStart(timePoint, raceColumn, competitor))
                || !isCertainlyBeforeRaceFinish(timePoint, raceColumn, competitor);
    }

    /**
     * If the {@link #getMaxPointsReason(Competitor, TrackedRace, TimePoint)} for the <code>competitor</code> for the
     * <code>raceColumn</code>'s tracked race is not {@link MaxPointsReason#NONE}, the
     * {@link #getMaxPoints(TrackedRace) maximum score} is computed for the competitor. Otherwise, the
     * <code>uncorrectedScore</code> is returned.
     * <p>
     * 
     * The current implementation considers <code>timePoint</code> by comparing it to the <code>competitor</code>'s
     * times for the tracked race associated for that competitor in <code>raceColumn</code>. If there is no such tracked
     * race, any score correction available will be applied unchanged. If a tracked race is attached to the column for
     * the competitor's fleet, the score correction is applied if <code>timePoint</code> is after the competitor
     * finished or aborted the race. If the <code>timePoint</code> is after the {@link TrackedRace#getStartOfRace() race
     * start time}, a score correction for the competitor for that race is considered if it is a
     * {@link MaxPointsReason#DNS}, {@link MaxPointsReason#DNC} or {@link MaxPointsReason#OCS} code. Those penalties
     * apply already from the start of the race and will cause the penalty score to be applied already during the race
     * time interval, so the competitor will be sorted to the end of the leaderboard already during replay.
     * <p>
     */
    @Override
    public Result getCorrectedScore(final Callable<Integer> trackedRankProvider, final Competitor competitor,
            final RaceColumn raceColumn, Leaderboard leaderboard, final TimePoint timePoint,
            final NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher,
            final ScoringScheme scoringScheme, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Double correctedScore;
        final AnnotatedMaxPointsReason maxPointsReason = getAnnotatedMaxPointsReason(competitor, raceColumn, timePoint);
        if (maxPointsReason.getMaxPointsReason() == MaxPointsReason.NONE) {
            // could be that there is a MaxPointsReason that just doesn't apply yet at timePoint; in this
            // case ignore the score correction and deliver the uncorrected result
            if (maxPointsReason.isMaxPointsReasonExistsButIsNotApplicableForTimePoint()) {
                correctedScore = getUncorrectedScore(competitor, raceColumn, trackedRankProvider, scoringScheme, numberOfCompetitorsInLeaderboardFetcher, timePoint, cache);
            } else {
                correctedScore = getCorrectedNonMaxedScore(competitor, raceColumn, trackedRankProvider, scoringScheme, numberOfCompetitorsInLeaderboardFetcher, timePoint, cache);
            }
        } else {
            // allow explicit override even when max points reason is specified; calculation may be wrong,
            // e.g., in case we have an untracked race and the number of competitors is estimated incorrectly
            final Double correctedNonMaxedScore;
            if ((maxPointsReason.isCalculateScoreDuringRace() && isCertainlyBeforeRaceFinish(timePoint, raceColumn, competitor))
            || (correctedNonMaxedScore = correctedScores.get(raceColumn.getKey(competitor))) == null) {
                final Supplier<Double> uncorrectedScoreProvider = ()->getUncorrectedScore(competitor, raceColumn, trackedRankProvider, scoringScheme, numberOfCompetitorsInLeaderboardFetcher, timePoint, cache);
                final Double incrementalScoreCorrectionForCompetitorInColumn = incrementalScoreCorrection.get(raceColumn.getKey(competitor));
                if (incrementalScoreCorrectionForCompetitorInColumn != null) {
                    final Double uncorrectedScore = uncorrectedScoreProvider.get();
                    correctedScore = uncorrectedScore == null ? null : (uncorrectedScore + incrementalScoreCorrectionForCompetitorInColumn);
                } else {
                    correctedScore = scoringScheme.getPenaltyScore(raceColumn, competitor, maxPointsReason.getMaxPointsReason(),
                            getNumberOfCompetitorsInRace(raceColumn, competitor, numberOfCompetitorsInLeaderboardFetcher),
                            numberOfCompetitorsInLeaderboardFetcher, timePoint, leaderboard, uncorrectedScoreProvider);
                }
            } else {
                correctedScore = correctedNonMaxedScore;
            }
        }
        return new Result() {
            @Override
            public MaxPointsReason getMaxPointsReason() {
                return maxPointsReason.getMaxPointsReason();
            }

            @Override
            public Double getCorrectedScore() {
                return correctedScore;
            }

            @Override
            public Double getIncrementalScoreCorrectionInPoints() {
                return incrementalScoreCorrection.get(raceColumn.getKey(competitor));
            }

            @Override
            public boolean isCorrected() {
                return isScoreCorrected(competitor, raceColumn, getTimePoint());
            }

            @Override
            public boolean isCorrectedIncrementally() {
                return isScoreCorrectedIncrementally(competitor, raceColumn, getTimePoint());
            }

            @Override
            public TimePoint getTimePoint() {
                return timePoint;
            }

            @Override
            public Double getUncorrectedScore() {
                Double resultUncorrected = 0.0;
                try {
                    resultUncorrected = scoringScheme.getScoreForRank(leaderboard, raceColumn,
                            competitor, trackedRankProvider.call(), new Callable<Integer>() {
                                @Override
                                public Integer call() {
                                    return getNumberOfCompetitorsInRace(raceColumn, competitor, numberOfCompetitorsInLeaderboardFetcher);
                                }
                            }, numberOfCompetitorsInLeaderboardFetcher, timePoint);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return resultUncorrected;
            }
        };
    }

    protected Integer getNumberOfCompetitorsInRace(RaceColumn raceColumn, Competitor competitor, NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher) {
        Integer result;
        final TrackedRace trackedRace = raceColumn.getTrackedRace(competitor);
        if (trackedRace == null) {
            final int estimatedSizeOfLargestFleet;
            final int numberOfCompetitorsInLeaderboard = numberOfCompetitorsInLeaderboardFetcher.getNumberOfCompetitorsInLeaderboard();
            if (raceColumn instanceof RaceColumnInSeries) {
                final int numberOfFleets = Util.size(((RaceColumnInSeries) raceColumn).getSeries().getFleets());
                estimatedSizeOfLargestFleet = numberOfCompetitorsInLeaderboard / numberOfFleets
                        + (int) Math.signum(numberOfCompetitorsInLeaderboard % numberOfFleets); // round up
            } else {
                estimatedSizeOfLargestFleet = numberOfCompetitorsInLeaderboard;
            }
            result = estimatedSizeOfLargestFleet;
        } else {
            result = Util.size(trackedRace.getRace().getCompetitors());
        }
        return result;
    }

    /**
     * Under the assumption that the competitor is not assigned the maximum score due to disqualification or other
     * reasons, computes the corrected score. If {@link #correctedScores} contains an entry for the
     * <code>competitor</code>'s key, it is used. Otherwise, the <code>uncorrectedScore</code> is returned.
     * @param scoringScheme
     *            used to transform the tracked rank into a score if there is no score correction applied
     * 
     * @return <code>null</code> in case the <code>competitor</code> has no score assigned in that race which is the
     *         case if the score is not corrected by these score corrections, and the <code>trackedRankProvider</code>
     *         delivers 0 as the rank, or if the score is not corrected and the scoring scheme cannot find the
     *         competitor in any tracked race of the <code>raceColumn</code>, meaning there cannot be a tracked rank for
     *         the competitor regardless what <code>trackedRankProvider</code> delivers.
     */
    private Double getCorrectedNonMaxedScore(final Competitor competitor, final RaceColumn raceColumn,
            Callable<Integer> trackedRankProvider, ScoringScheme scoringScheme,
            final NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        Double correctedNonMaxedScore = correctedScores.get(raceColumn.getKey(competitor));
        Double result;
        if (correctedNonMaxedScore == null || isCertainlyBeforeRaceFinish(timePoint, raceColumn, competitor, cache)) {
            result = getUncorrectedScore(competitor, raceColumn, trackedRankProvider, scoringScheme,
                    numberOfCompetitorsInLeaderboardFetcher, timePoint, cache);
        } else {
            result = correctedNonMaxedScore;
        }
        return result;
    }

    private Double getUncorrectedScore(final Competitor competitor, final RaceColumn raceColumn,
            Callable<Integer> trackedRankProvider, ScoringScheme scoringScheme,
            final NumberOfCompetitorsInLeaderboardFetcher numberOfCompetitorsInLeaderboardFetcher, TimePoint timePoint,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Double result;
        try {
            int trackedRank = trackedRankProvider.call();
            result = scoringScheme.getScoreForRank(leaderboard, raceColumn, competitor,
                    trackedRank, ()->getNumberOfCompetitorsInRace(raceColumn, competitor, numberOfCompetitorsInLeaderboardFetcher),
                    numberOfCompetitorsInLeaderboardFetcher, timePoint);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception while computing uncorrected score", e);
            throw new RuntimeException(e);
        }
        return result;
    }

    @Override
    public Double getExplicitScoreCorrection(Competitor competitor, RaceColumn raceColumn) {
        return correctedScores.get(raceColumn.getKey(competitor));
    }

    @Override
    public boolean hasCorrectionFor(RaceColumn raceInLeaderboard) {
        return internalHasScoreCorrectionFor(raceInLeaderboard);
    }

    private boolean internalHasScoreCorrectionFor(RaceColumn raceInLeaderboard) {
        for (final Pair<Competitor, RaceColumn> correctedScoresKey : correctedScores.keySet()) {
            if (correctedScoresKey.getB() == raceInLeaderboard) {
                return true;
            }
        }
        for (Pair<Competitor, RaceColumn> maxPointsReasonsKey : maxPointsReasons.keySet()) {
            if (maxPointsReasonsKey.getB() == raceInLeaderboard) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean hasCorrectionForNonTrackedFleet(RaceColumn raceInLeaderboard, Fleet fleet) {
        boolean result;
        if (raceInLeaderboard.getTrackedRace(fleet) == null) {
            result = false;
            for (final Competitor competitor : raceInLeaderboard.getAllCompetitors(fleet)) {
                final Pair<Competitor, RaceColumn> key = raceInLeaderboard.getKey(competitor);
                if (correctedScores.containsKey(key) || maxPointsReasons.containsKey(key)) {
                    result = true;
                    break;
                }
            }
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public TimePoint getTimePointOfLastCorrectionsValidity() {
        return timePointOfLastCorrectionsValidity;
    }

    @Override
    public String getComment() {
        return comment;
    }

    @Override
    public void setTimePointOfLastCorrectionsValidity(TimePoint timePointOfLastCorrectionsValidity) {
        final TimePoint oldTimePointOfLastCorrectionsValidity = this.timePointOfLastCorrectionsValidity;
        this.timePointOfLastCorrectionsValidity = timePointOfLastCorrectionsValidity;
        if (!Util.equalsWithNull(oldTimePointOfLastCorrectionsValidity, timePointOfLastCorrectionsValidity)) {
            notifyListenersAboutLastCorrectionsValidityChanged(oldTimePointOfLastCorrectionsValidity, timePointOfLastCorrectionsValidity);
        }
    }

    @Override
    public void setComment(String scoreCorrectionComment) {
        final String oldComment = this.comment;
        this.comment = scoreCorrectionComment;
        if (!Util.equalsWithNull(oldComment, scoreCorrectionComment)) {
            notifyListenersAboutCommentChanged(oldComment, scoreCorrectionComment);
        }
    }

    protected Leaderboard getLeaderboard() {
        return leaderboard;
    }

    @Override
    public Iterable<RaceColumn> getRaceColumnsThatHaveCorrections() {
        Set<RaceColumn> result = new HashSet<>();
        for (Pair<Competitor, RaceColumn> correctedScoresKey : correctedScores.keySet()) {
            result.add(correctedScoresKey.getB());
        }
        for (Pair<Competitor, RaceColumn> maxPointsReasonsKey : maxPointsReasons.keySet()) {
            result.add(maxPointsReasonsKey.getB());
        }
        return result;
    }

    @Override
    public Iterable<Competitor> getCompetitorsThatHaveCorrectionsIn(RaceColumn raceColumn) {
        Set<Competitor> result = new HashSet<>();
        for (Pair<Competitor, RaceColumn> correctedScoresKey : correctedScores.keySet()) {
            if (raceColumn == correctedScoresKey.getB()) {
                result.add(correctedScoresKey.getA());
            }
        }
        for (Pair<Competitor, RaceColumn> maxPointsReasonsKey : maxPointsReasons.keySet()) {
            if (raceColumn == maxPointsReasonsKey.getB()) {
                result.add(maxPointsReasonsKey.getA());
            }
        }
        return result;
    }

}
