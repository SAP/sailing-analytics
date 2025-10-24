package com.sap.sailing.datamining.impl.data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;

import com.sap.sailing.datamining.Activator;
import com.sap.sailing.datamining.SailingClusterGroups;
import com.sap.sailing.datamining.data.HasTrackedLegOfCompetitorContext;
import com.sap.sailing.datamining.data.HasTackTypeSegmentContext;
import com.sap.sailing.datamining.data.HasTrackedLegContext;
import com.sap.sailing.datamining.impl.components.TackTypeSegmentRetrievalProcessor;
import com.sap.sailing.datamining.shared.TackTypeSegmentsDataMiningSettings;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Positioned;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.datamining.data.Cluster;
import com.sap.sse.datamining.shared.impl.dto.ClusterDTO;

/**
 * Equality is based on the {@link #getTrackedLegOfCompetitor()} and the {@link #sliceNumber} only. The current slicing implementation
 * uses the time sailed in the leg as the basis for slicing. Other slicing criteria, such as windward distance traveled, would be much
 * harder to implement because we would need to find out iteratively, between which fixes the competitor sailed, say, one tenth of the
 * windward distance in the leg.
 */
public abstract class AbstractTrackedLegSliceOfCompetitorWithContext implements HasTrackedLegOfCompetitorContext, Positioned {
    private static final long serialVersionUID = 7748860191111327007L;
    private static final long DEFAULT_NUMBER_OF_SLICES = 10;

    private final HasTrackedLegContext trackedLegContext;
    private final TackTypeSegmentsDataMiningSettings settings;
    
    private final TrackedLegOfCompetitor trackedLegOfCompetitor;
    private final Competitor competitor;

    private Integer rankAtSliceStart;
    private boolean isRankAtSliceStartInitialized;
    private Integer rankAtSliceFinish;
    private boolean isRankAtSliceFinishInitialized;
    private int sliceNumber;

    public AbstractTrackedLegSliceOfCompetitorWithContext(HasTrackedLegContext trackedLegContext, TrackedLegOfCompetitor trackedLegOfCompetitor, TackTypeSegmentsDataMiningSettings settings, int sliceNumber) {
        this.trackedLegContext = trackedLegContext;
        this.trackedLegOfCompetitor = trackedLegOfCompetitor;
        this.competitor = trackedLegOfCompetitor.getCompetitor();
        this.settings = settings;
        this.sliceNumber = sliceNumber;
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = (prime * result + ((trackedLegOfCompetitor == null) ? 0 : trackedLegOfCompetitor.hashCode())) ^ sliceNumber;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        AbstractTrackedLegSliceOfCompetitorWithContext other = (AbstractTrackedLegSliceOfCompetitorWithContext) obj;
        if (sliceNumber != other.sliceNumber) {
            return false;
        }
        if (trackedLegOfCompetitor == null) {
            if (other.trackedLegOfCompetitor != null)
                return false;
        } else if (!trackedLegOfCompetitor.equals(other.trackedLegOfCompetitor))
            return false;
        return true;
    }
    
    /**
     * Picks the time point that is in the middle between the time point when the competitor entered
     * the leg and the time point the competitor finished the leg. If no leg start/finish time exists
     * for the competitor, start/end of race and then start/end of tracking are used as fall-back values.
     */
    public TimePoint getTimePoint() {
        final TrackedLeg trackedLeg = getTrackedLegContext().getTrackedLeg();
        final TrackedRace trackedRace = trackedLeg.getTrackedRace();
        return getTimePointBetweenLegSliceStartAndLegFinish(trackedRace);
    }

    @Override
    public HasTrackedLegContext getTrackedLegContext() {
        return trackedLegContext;
    }

    @Override
    public TrackedLegOfCompetitor getTrackedLegOfCompetitor() {
        return trackedLegOfCompetitor;
    }

    @Override
    public Competitor getCompetitor() {
        return competitor;
    }
    
    @Override
    public Boat getBoat() {
        Boat boatOfCompetitor = getTrackedRace().getBoatOfCompetitor(getCompetitor());
        return boatOfCompetitor;
    }
    
    @Override
    public ClusterDTO getPercentageClusterForRelativeScoreInRace() {
        final ClusterDTO result;
        Double relativeScore = getTrackedLegContext().getTrackedRaceContext().getRelativeScoreForCompetitor(getCompetitor());
        if (relativeScore == null) {
            result = null;
        } else {
            SailingClusterGroups clusterGroups = Activator.getClusterGroups();
            Cluster<Double> cluster = clusterGroups.getPercentageClusterGroup().getClusterFor(relativeScore);
            result = new ClusterDTO(clusterGroups.getPercentageClusterFormatter().format(cluster));
        }
        return result;
    }
    
    protected <R> R getSomethingForLegTrackingInterval(BiFunction<TimePoint, TimePoint, R> resultSupplier) {
        final TimePoint startTime = getSliceStartTime();
        final TimePoint finishTime = getSliceFinishTime();
        return getSomethingForInterval(resultSupplier, startTime, finishTime);
    }

    /**
     * @return {@code null} if the competitor hasn't started the leg; otherwise, the start of the slice as split between
     * the competitors leg start time point and {@link #getEffectiveEndOfLeg(TimePoint)}, into {@link #DEFAULT_NUMBER_OF_SLICES}
     * parts of equal duration.
     */
    protected TimePoint getSliceStartTime() {
        return getTimePointAtStartOfSlice(sliceNumber-1); // sliceNumber is one-based, parameter is zero-based, so this means the beginning of the slice
    }
    
    /**
     * @return {@code null} if the competitor hasn't started the leg; otherwise, the non-{@code null} end of the slice
     *         as split between the competitors leg start time point and {@link #getEffectiveEndOfLeg(TimePoint)}, into
     *         {@link #DEFAULT_NUMBER_OF_SLICES} parts of equal duration.
     */
    protected TimePoint getSliceFinishTime() {
        return getTimePointAtStartOfSlice(sliceNumber); // sliceNumber is one-based, parameter is zero-based, so this means the end of the slice
    }

    private TimePoint getTimePointAtStartOfSlice(int zeroBasedSliceNumber) {
        final TimePoint legStartTime = getTrackedLegOfCompetitor().getStartTime();
        final TimePoint sliceFinishTime;
        if (legStartTime == null) {
            sliceFinishTime = null;
        } else {
            final TimePoint finishTime = getTrackedLegOfCompetitor().getFinishTime();
            final TimePoint effectiveEndOfLeg = getEffectiveEndOfLeg(finishTime);
            sliceFinishTime = legStartTime.plus(legStartTime.until(effectiveEndOfLeg).divide(DEFAULT_NUMBER_OF_SLICES).times(zeroBasedSliceNumber)); // sliceNumber is one-based, so this means the end of the slice
        }
        return sliceFinishTime;
    }

    /**
     * @return a non-{@code null} time point; {@code finishTime} if not {@code null}; otherwise the {@link TrackedRace#getEndOfRace() end of the race} if
     * not {@code null}; otherwise the {@link TrackedRace#getEndOfTracking() end of tracking} if not {@code null}; otherwise the current time.
     */
    protected TimePoint getEffectiveEndOfLeg(final TimePoint finishTime) {
        return finishTime != null ? finishTime :
            getTrackedLegContext().getTrackedLeg().getTrackedRace().getEndOfRace() != null ? getTrackedLegContext().getTrackedLeg().getTrackedRace().getEndOfRace() :
                getTrackedLegContext().getTrackedLeg().getTrackedRace().getEndOfTracking() != null ? getTrackedLegContext().getTrackedLeg().getTrackedRace().getEndOfTracking() :
                    TimePoint.now();
    }

    protected <R> R getSomethingForInterval(BiFunction<TimePoint, TimePoint, R> resultSupplier,
            final TimePoint startTime, final TimePoint finishTime) {
        final R result;
        if (startTime != null) {
            final TrackedRace trackedRace = getTrackedLegContext().getTrackedLeg().getTrackedRace();
            final TimePoint effectiveEndOfInterval = finishTime != null ? finishTime :
                trackedRace.getEndOfTracking() != null ? trackedRace.getEndOfTracking() : TimePoint.now();
            result = resultSupplier.apply(startTime, effectiveEndOfInterval);
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public Duration getTimeSpentFoiling() {
        return getSomethingForLegTrackingInterval((start, end) -> {
            BravoFixTrack<Competitor> bravoFixTrack = getTrackedLegContext().getTrackedLeg().getTrackedRace().getSensorTrack(getCompetitor(), BravoFixTrack.TRACK_NAME);
            return bravoFixTrack == null ? Duration.NULL : bravoFixTrack.getTimeSpentFoiling(start, end);
        });
    }

    @Override
    public Distance getDistanceSpentFoiling() {
        return getSomethingForLegTrackingInterval((start, end) -> {
            BravoFixTrack<Competitor> bravoFixTrack = getTrackedLegContext().getTrackedLeg().getTrackedRace().getSensorTrack(getCompetitor(), BravoFixTrack.TRACK_NAME);
            return bravoFixTrack == null ? Distance.NULL : bravoFixTrack.getDistanceSpentFoiling(start, end);
        });
    }

    @Override
    public Distance getDistanceTraveled() {
        return getSomethingForLegTrackingInterval((start, end) -> {
            return start == null ? null :
                getTrackedLegOfCompetitor().getDistanceTraveled(end).add(getTrackedLegOfCompetitor().getDistanceTraveled(start).scale(-1));
        });
    }
    
    @Override
    public Double getSpeedAverageInKnots() {
        final Double result;
        if (getTrackedRace() == null || getSliceStartTime() == null) {
            result = null;
        } else {
            final TrackedLegOfCompetitor trackedLegOfCompetitor = getTrackedLegOfCompetitor();
            final Distance distanceTraveledInSlice = trackedLegOfCompetitor.getDistanceTraveled(getSliceFinishTime())
                    .add(trackedLegOfCompetitor.getDistanceTraveled(getSliceStartTime()).scale(-1));
            final Duration sliceDuration = getSliceStartTime().until(getSliceFinishTime());
            final Speed averageSOG = distanceTraveledInSlice.inTime(sliceDuration);
            result = averageSOG == null ? null :averageSOG.getKnots();
        }
        return result;
    }
    
    @Override
    public Integer getRankGainsOrLosses() {
        Integer rankAtSliceStart = getRankAtSliceStart();
        Integer rankAtSliceFinish = getRankAtSliceFinish();
        return rankAtSliceStart != null && rankAtSliceFinish != null ? rankAtSliceStart - rankAtSliceFinish : null;
    }
    
    private Integer getRankAtSliceStart() {
        final Integer result;
        if (getTrackedLegContext().getTrackedRaceContext().getTrackedRace() != null) {
            if (!isRankAtSliceStartInitialized) {
                int rank = getTrackedLegOfCompetitor().getRank(getSliceStartTime());
                rankAtSliceStart = rank == 0 ? null : rank;
                isRankAtSliceStartInitialized = true;
            }
            result = rankAtSliceStart;
        } else {
            result = null;
        }
        return result;
    }

    /**
     * If the competitor hasn't started and finished the leg, {@code null} will be returned.
     * 
     * @param ratioOfLegSliceTimeSpent
     *            the ratio of the time the competitor has spent in this slice of the leg; 0=just starting the leg; 1=just
     *            finished the slice of the leg
     */
    private Integer getRankAtTimePercent(double ratioOfLegSliceTimeSpent) {
        final Integer result;
        final TrackedRace trackedRace = getTrackedLegContext().getTrackedRaceContext().getTrackedRace();
        if (trackedRace != null) {
            final TimePoint startTime = getSliceStartTime();
            final TimePoint finishTime = getSliceFinishTime();
            if (startTime != null && finishTime != null) {
                final TimePoint timePoint = startTime.plus(startTime.until(finishTime).times(ratioOfLegSliceTimeSpent));
                result = trackedRace.getRank(getCompetitor(), timePoint);
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    /**
     * A "non-official" hypothetical rank for upwind and downwind legs, pretending we didn't have wind information and
     * hence couldn't compute a wind-based rank. This may be used, e.g., to compare the wind-based prediction to the rhumb
     * line-based prediction. If the competitor hasn't started and finished the leg, {@code null} will be returned.
     * 
     * @param ratioOfLegTimeSpent
     *            the ratio of the time the competitor has spent in the leg overall; 0=just starting the leg; 1=just
     *            finished the leg
     */
    private Integer getRhumbLineBasedRankAtTimePercent(double ratioOfLegTimeSpent) {
        final Integer result;
        final TrackedRace trackedRace = getTrackedLegContext().getTrackedRaceContext().getTrackedRace();
        if (trackedRace != null) {
            final TimePoint startTime = getSliceStartTime();
            final TimePoint finishTime = getSliceFinishTime();
            if (startTime != null && finishTime != null) {
                final TimePoint timePoint = startTime.plus(startTime.until(finishTime).times(ratioOfLegTimeSpent));
                final WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache = new LeaderboardDTOCalculationReuseCache(timePoint) {
                    /**
                     * Always returns {@link LegType#REACHING} to force rhumb line-based calculation
                     */
                    @Override
                    public LegType getLegType(TrackedLeg trackedLeg, TimePoint timePoint) throws NoWindException {
                        return LegType.REACHING;
                    }
                };
                // RaceRankComparator requires course read lock
                trackedRace.getRace().getCourse().lockForRead();
                final List<Competitor> rankedCompetitors = new ArrayList<>();
                try {
                    Comparator<Competitor> comparator = trackedRace.getRankingMetric().getRaceRankingComparator(timePoint, cache);
                    Util.addAll(trackedRace.getRace().getCompetitors(), rankedCompetitors);
                    Collections.sort(rankedCompetitors, comparator);
                } finally {
                    trackedRace.getRace().getCourse().unlockAfterRead();
                }
                result = rankedCompetitors.indexOf(getCompetitor()) + 1;
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Double getRelativeRank() {
        Leaderboard leaderboard = getTrackedLegContext().getTrackedRaceContext().getLeaderboardContext().getLeaderboard();
        double competitorCount = Util.size(leaderboard.getCompetitors());
        Integer rankAtSliceFinish = getRankAtSliceFinish();
        return rankAtSliceFinish == null ? null : rankAtSliceFinish / competitorCount;
    }
    
    public Integer getRankAtSliceFinish() {
        if (!isRankAtSliceFinishInitialized) {
            int rank = getTrackedLegOfCompetitor().getRank(getSliceFinishTime());
            rankAtSliceFinish = rank == 0 ? null : rank;
            isRankAtSliceFinishInitialized = true;
        }
        return rankAtSliceFinish;
    }
    
    @Override
    public Long getTimeTakenInSeconds() {
        TimePoint startTime = getSliceStartTime();
        TimePoint finishTime = getSliceFinishTime();
        final Long result;
        if (startTime == null || finishTime == null) {
            result = null;
        } else {
            result = (finishTime.asMillis() - startTime.asMillis()) / 1000;
        }
        return result;
    }

    protected TimePoint getTimePointBetweenLegSliceStartAndLegFinish(final TrackedRace trackedRace) {
        final TimePoint competitorLegSliceStartTime = getSliceStartTime();
        final TimePoint competitorLegSliceEndTime =  getSliceFinishTime();
        final TimePoint startTime = competitorLegSliceStartTime != null ? competitorLegSliceStartTime :
            trackedRace.getStartOfRace() != null ? trackedRace.getStartOfRace() : trackedRace.getStartOfTracking();
        final TimePoint endTime = competitorLegSliceEndTime != null ? competitorLegSliceEndTime :
            trackedRace.getEndOfRace() != null ? trackedRace.getEndOfRace() : trackedRace.getEndOfTracking();
        final TimePoint timepoint = endTime == null ? startTime : startTime == null ? null : startTime.plus(startTime.until(endTime).divide(2));
        return timepoint;
    }

    @Override
    public Integer getNumberOfManeuvers() {
        return getNumberOfJibes() + getNumberOfTacks();
    }

    @Override
    public Integer getNumberOfJibes() {
        return getSomethingForLegTrackingInterval((start, end) -> {
            return getNumberOf(ManeuverType.JIBE, start, end);
        });
    }

    @Override
    public Integer getNumberOfTacks() {
        return getSomethingForLegTrackingInterval((start, end) -> {
            return getNumberOf(ManeuverType.TACK, start, end);
        });
    }
    
    private int getNumberOf(ManeuverType maneuverType, TimePoint start, TimePoint end) {
        TrackedRace trackedRace = getTrackedRace();
        int number = 0;
        if (trackedRace != null) {
            for (Maneuver maneuver : trackedRace.getManeuvers(getCompetitor(), start, end, false)) {
                if (maneuver.getType() == maneuverType) {
                    number++;
                }
            }
        }
        return number;
    }

    @Override
    public Integer getRankAfterFirstQuarter() {
        return getRankAtTimePercent(0.25);
    }

    @Override
    public Integer getRankAfterSecondQuarter() {
        return getRankAtTimePercent(0.5);
    }

    @Override
    public Integer getRankAfterThirdQuarter() {
        return getRankAtTimePercent(0.75);
    }

    @Override
    public Double getRankAverageAcrossFirstThreeQuarters() {
        return getRankAverageAcrossFirstThreeQuarters(this::getRankAtTimePercent);
    }

    private Double getRankAverageAcrossFirstThreeQuarters(final Function<Double, Integer> rankFunction) {
        int rankSum = 0;
        int count = 0;
        for (double ratio=0.25; ratio<1.0; ratio+=0.25) {
            final Integer rankAtRatio = rankFunction.apply(ratio);
            if (rankAtRatio != null) {
                rankSum += rankAtRatio;
                count++;
            }
        }
        return count == 0 ? null : (double) rankSum / (double) count;
    }

    @Override
    public Integer getRankRhumbLineBasedAfterFirstQuarter() {
        return getRhumbLineBasedRankAtTimePercent(0.25);
    }

    @Override
    public Integer getRankRhumbLineBasedAfterSecondQuarter() {
        return getRhumbLineBasedRankAtTimePercent(0.5);
    }

    @Override
    public Integer getRankRhumbLineBasedAfterThirdQuarter() {
        return getRhumbLineBasedRankAtTimePercent(0.75);
    }

    @Override
    public Double getRankRhumbLineBasedAverageAcrossFirstThreeQuarters() {
        return getRankAverageAcrossFirstThreeQuarters(this::getRhumbLineBasedRankAtTimePercent);
    }

    private Double getRankPredictionError(final Supplier<? extends Number> rankPredictor) {
        final Integer rankAtFinish = getRankAtSliceFinish();
        final Double result;
        if (rankAtFinish != null) {
            final Number predictiveRank = rankPredictor.get();
            if (predictiveRank != null) {
                final int numberOfCompetitors = Util.size(getTrackedRace().getRace().getCompetitors());
                if (numberOfCompetitors != 0) {
                    result = Math.abs((double) rankAtFinish-predictiveRank.doubleValue()) / (double) numberOfCompetitors;
                } else {
                    // this is a bit strange because we obviously have a non-null getCompetitor() in the race...
                    result = null;
                }
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Double getRankPredictionErrorAfterFirstQuarter() {
        return getRankPredictionError(this::getRankAfterFirstQuarter);
    }
    
    @Override
    public Double getRankPredictionErrorAfterSecondQuarter() {
        return getRankPredictionError(this::getRankAfterSecondQuarter);
    }

    @Override
    public Double getRankPredictionErrorAfterThirdQuarter() {
        return getRankPredictionError(this::getRankAfterThirdQuarter);
    }

    @Override
    public Double getRankPredictionErrorAcrossFirstThreeQuarters() {
        return getRankPredictionError(this::getRankAverageAcrossFirstThreeQuarters);
    }

    @Override
    public Double getRankPredictionErrorRhumbLineBasedAfterFirstQuarter() {
        return getRankPredictionError(this::getRankRhumbLineBasedAfterFirstQuarter);
    }

    @Override
    public Double getRankPredictionErrorRhumbLineBasedAfterSecondQuarter() {
        return getRankPredictionError(this::getRankRhumbLineBasedAfterSecondQuarter);
    }

    @Override
    public Double getRankPredictionErrorRhumbLineBasedAfterThirdQuarter() {
        return getRankPredictionError(this::getRankRhumbLineBasedAfterThirdQuarter);
    }

    @Override
    public Double getRankPredictionErrorRhumbLineBasedAcrossFirstThreeQuarters() {
        return getRankPredictionError(this::getRankRhumbLineBasedAverageAcrossFirstThreeQuarters);
    }

    @Override
    public Speed getVelocityMadeGood() {
        final TimePoint finishTime = getSliceFinishTime();
        return getTrackedLegOfCompetitor().getAverageVelocityMadeGood(finishTime == null ? TimePoint.now() : finishTime);
    }

    @Override
    public double getRatioDurationLongVsShortTack() {
        final TackTypeRatioCollector<Duration> resultProcessor = new TackTypeRatioCollector<Duration>(Duration.NULL) {
            @Override
            protected Duration add(Duration a, Duration b) {
                return a.plus(b);
            }

            @Override
            protected double divide(Duration a, Duration b) {
                return a.divide(b);
            }

            @Override
            protected Duration getAddable(HasTackTypeSegmentContext element) {
                return element.getDuration();
            }
        };
        final TackTypeSegmentRetrievalProcessor tackTypeSegmentRetriever = new TackTypeSegmentRetrievalProcessor(
                /* executor */ null,
                Collections.emptySet(), settings, 0, "TackTypeSegments");
        return Util.stream(tackTypeSegmentRetriever.retrieveData(
                new RaceOfCompetitorWithContext(getTrackedLegContext().getTrackedRaceContext(), competitor, settings)))
                .filter(tt->tt.getLegNumber() == getTrackedLegContext().getLegNumber()).collect(resultProcessor);
    }
    
    protected boolean isTackTypeSegmentIntersectingWithLegSlice(HasTackTypeSegmentContext tackTypeSegmentContext) {
        return getSliceStartTime() != null &&
                tackTypeSegmentContext.getLegNumber() == getTrackedLegContext().getLegNumber()
                && TimeRange.create(tackTypeSegmentContext.getStartOfTackTypeSegment(), tackTypeSegmentContext.getEndOfTackTypeSegment())
                    .intersects(TimeRange.create(getSliceStartTime(), getSliceFinishTime()));
    }

    @Override
    public double getRatioDistanceLongVsShortTack() {
        final TackTypeRatioCollector<Distance> resultProcessor = new TackTypeRatioCollector<Distance>(Distance.NULL) {
            @Override
            protected Distance add(Distance a, Distance b) {
                return a.add(b);
            }

            @Override
            protected double divide(Distance a, Distance b) {
                return a.divide(b);
            }

            @Override
            protected Distance getAddable(HasTackTypeSegmentContext element) {
                return element.getDistance();
            }
        };
        final TackTypeSegmentRetrievalProcessor tackTypeSegmentRetriever = new TackTypeSegmentRetrievalProcessor(
                /* executor */ null,
                Collections.emptySet(), settings, 0, "TackTypeSegments");
        return Util.stream(tackTypeSegmentRetriever.retrieveData(
                new RaceOfCompetitorWithContext(getTrackedLegContext().getTrackedRaceContext(), competitor, settings)))
                .filter(tt->tt.getLegNumber() == getTrackedLegContext().getLegNumber()).collect(resultProcessor);
    }

    
    @Override
    public Position getPosition() {
        final TrackedLeg trackedLeg = getTrackedLegContext().getTrackedLeg();
        final TrackedRace trackedRace = trackedLeg.getTrackedRace();
        final TimePoint timepoint = getTimePointBetweenLegSliceStartAndLegFinish(trackedRace);
        final Position result;
        if (timepoint == null) {
            result = null;
        } else {
            result = trackedLeg.getMiddleOfLeg(timepoint);
        }
        return result;
    }

    protected Integer getTheSliceNumber() {
        return sliceNumber;
    }
}
