package com.sap.sailing.domain.leaderboard.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RunnableFuture;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;

import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.race.InvalidatesLeaderboardCache;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CPUMeteringType;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.LeaderboardChangeListener;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.SailNumberCanonicalizerAndMatcher;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.RegattaScoreCorrections;
import com.sap.sailing.domain.common.RegattaScoreCorrections.ScoreCorrectionsForRace;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.dto.BasicRaceDTO;
import com.sap.sailing.domain.common.dto.BoatClassDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.domain.common.dto.MetaLeaderboardRaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.sharding.ShardingType;
import com.sap.sailing.domain.common.tracking.BravoExtendedFix;
import com.sap.sailing.domain.common.tracking.BravoFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.ScoreCorrectionMapping;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCache;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCalculationReuseCache;
import com.sap.sailing.domain.leaderboard.caching.LiveLeaderboardUpdater;
import com.sap.sailing.domain.leaderboard.meta.MetaLeaderboardColumn;
import com.sap.sailing.domain.orc.impl.ORCPerformanceCurveByImpliedWindRankingMetric;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.ranking.RankingMetric.CompetitorRankingInfo;
import com.sap.sailing.domain.ranking.RankingMetric.RankingInfo;
import com.sap.sailing.domain.regattalike.LeaderboardThatHasRegattaLike;
import com.sap.sailing.domain.sharding.ShardingContext;
import com.sap.sailing.domain.tracking.BravoFixTrack;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindPositionMode;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimingStats;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.ThreadPoolUtil;
import com.sap.sse.util.impl.FutureTaskWithTracingGet;

public abstract class AbstractLeaderboardWithCache implements Leaderboard {
    private static final long serialVersionUID = -5651389357061229100L;
    private static final Logger logger = Logger.getLogger(AbstractLeaderboardWithCache.class.getName());
    
    private final MaxPointsReason[] MAX_POINTS_REASONS_THAT_IDENTIFY_NON_FINISHED_RACES = new MaxPointsReason[] {
            MaxPointsReason.DNS, MaxPointsReason.DNF, MaxPointsReason.DNC };
    private transient LiveLeaderboardUpdater liveLeaderboardUpdater;
    protected static final ExecutorService executor = ThreadPoolUtil.INSTANCE.getDefaultForegroundTaskThreadPoolExecutor();
    private transient LeaderboardDTOCache leaderboardDTOCache;
    private transient Map<com.sap.sse.common.Util.Pair<TrackedRace, Competitor>, RunnableFuture<RaceDetails>> raceDetailsAtEndOfTrackingCache;

    /** the display name of the leaderboard */
    private String displayName;

    private transient Set<LeaderboardChangeListener> leaderboardChangeListeners;
    
    /**
     * Keeps statistics about the re-calculation times in different short-term time ranges.
     */
    private transient TimingStats timingStats;
    
    /**
     * Used to remove all these listeners from their tracked races when this servlet is {@link #destroy() destroyed}.
     */
    private transient Set<CacheInvalidationListener> cacheInvalidationListeners;

    private static class RaceDetails {
        private final List<LegEntryDTO> legDetails;
        private final Distance windwardDistanceToCompetitorFarthestAhead;
        private final Distance averageAbsoluteCrossTrackError;
        private final Distance averageSignedCrossTrackError;
        private final Duration gapToLeaderInOwnTime;
        private final Double percentTargetBoatSpeed;
        private final Duration timeSailedSinceRaceStart;
        private final Duration correctedTime;
        private final Duration correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead;

        public RaceDetails(List<LegEntryDTO> legDetails, Distance windwardDistanceToCompetitorFarthestAhead,
                Distance averageAbsoluteCrossTrackError, Distance averageSignedCrossTrackError,
                Duration gapToLeaderInOwnTime, Double percentTargetBoatSpeed,
                Duration timeSailedSinceRaceStart, Duration correctedTime, Duration correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead) {
            super();
            this.legDetails = legDetails;
            this.windwardDistanceToCompetitorFarthestAhead = windwardDistanceToCompetitorFarthestAhead;
            this.averageAbsoluteCrossTrackError = averageAbsoluteCrossTrackError;
            this.averageSignedCrossTrackError = averageSignedCrossTrackError;
            this.gapToLeaderInOwnTime = gapToLeaderInOwnTime;
            this.percentTargetBoatSpeed = percentTargetBoatSpeed;
            this.correctedTime = correctedTime;
            this.timeSailedSinceRaceStart = timeSailedSinceRaceStart;
            this.correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead = correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead;
        }
        public List<LegEntryDTO> getLegDetails() {
            return legDetails;
        }
        public Distance getWindwardDistanceToCompetitorFarthestAhead() {
            return windwardDistanceToCompetitorFarthestAhead;
        }
        public Distance getAverageAbsoluteCrossTrackError() {
            return averageAbsoluteCrossTrackError;
        }
        public Distance getAverageSignedCrossTrackError() {
            return averageSignedCrossTrackError;
        }
        public Duration getGapToLeaderInOwnTime() {
            return gapToLeaderInOwnTime;
        }
        public Duration getTimeSailedSinceRaceStart() {
            return timeSailedSinceRaceStart;
        }
        public Duration getCorrectedTime() {
            return correctedTime;
        }
        public Duration getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead() {
            return correctedTimeAtEstimatedArrivalAtCompetitorFarthestAhead;
        }
        public Double getPercentTargetBoatSpeed() {
            return percentTargetBoatSpeed;
        }
    }

    /**
     * Handles the invalidation of the {@link SailingServiceImpl#raceDetailsAtEndOfTrackingCache} entries if the tracked
     * race changes in any way. In particular, for {@link #statusChanged}, when the status changes away from LOADING,
     * calculations may start or resume, making it necessary to clear the cache.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private class CacheInvalidationListener extends AbstractRaceChangeListener {
        private final TrackedRace trackedRace;
        private final Competitor competitor;

        public CacheInvalidationListener(TrackedRace trackedRace, Competitor competitor) {
            this.trackedRace = trackedRace;
            this.competitor = competitor;
        }

        public TrackedRace getTrackedRace() {
            return trackedRace;
        }

        public void removeFromTrackedRace() {
            trackedRace.removeListener(this);
        }

        private void invalidateCacheAndRemoveThisListenerFromTrackedRace() {
            synchronized (raceDetailsAtEndOfTrackingCache) {
                raceDetailsAtEndOfTrackingCache.remove(new com.sap.sse.common.Util.Pair<TrackedRace, Competitor>(trackedRace, competitor));
                removeFromTrackedRace();
            }
        }
        
        @Override
        protected void defaultAction() {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }
    }
    
    protected AbstractLeaderboardWithCache() {
        this.raceDetailsAtEndOfTrackingCache = new HashMap<>();
        initTransientFields();
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        initTransientFields();
    }

    private void initTransientFields() {
        this.raceDetailsAtEndOfTrackingCache = new HashMap<>();
        this.cacheInvalidationListeners = new HashSet<>();
        this.leaderboardChangeListeners = new HashSet<>();
        this.timingStats = createTimingStats();
        // When many updates are triggered in a short period of time by a single thread, ensure that the single thread
        // providing the updates is not outperformed by all the re-calculations happening here. Leave at least one
        // core to other things, but by using at least three threads ensure that no simplistic deadlocks may occur.
    }
    
    /**
     * Creates the {@link #timingStats} statistics keeper with a few time intervals, tracking
     * the re-computing times.
     */
    private TimingStats createTimingStats() {
        return new TimingStats(Duration.ONE_SECOND.times(5), Duration.ONE_SECOND.times(30), Duration.ONE_MINUTE);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        final String oldDisplayName = this.displayName;
        this.displayName = displayName;
        notifyLeaderboardChangeListeners(listener->{
            try {
                listener.displayNameChanged(oldDisplayName, displayName);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception trying to notify listener "+listener+" about the display name of leaderboard "+
                        getName()+" changing from "+oldDisplayName+" to "+displayName, e);
            }
        });
    }
    
   protected void trackedRaceUnlinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        // It's generally possible that a leaderboard links to the same tracked race in multiple columns / fleets;
        // only if it no longer references the trackedRace currently unlinked from one column/fleet, also unlink
        // all cache invalidation listeners for said trackedRace
        if (!Util.contains(getTrackedRaces(), trackedRace)) {
            synchronized (cacheInvalidationListeners) {
                for (Iterator<CacheInvalidationListener> cacheInvalidationListenerIter=cacheInvalidationListeners.iterator();
                        cacheInvalidationListenerIter.hasNext(); ) {
                    CacheInvalidationListener cacheInvalidationListener = cacheInvalidationListenerIter.next();
                    if (cacheInvalidationListener.getTrackedRace() == trackedRace) {
                        cacheInvalidationListener.removeFromTrackedRace();
                        cacheInvalidationListenerIter.remove();
                    }
                }
            }
        }
    }
    
    protected void notifyLeaderboardChangeListeners(Consumer<LeaderboardChangeListener> notifier) {
        final Set<LeaderboardChangeListener> workingListeners;
        synchronized (leaderboardChangeListeners) {
            workingListeners = new HashSet<>(leaderboardChangeListeners);
        }
        for (final LeaderboardChangeListener listener : workingListeners) {
            try {
                notifier.accept(listener);
            } catch (Exception e) {
                logger.log(Level.WARNING, "Exception trying to notify listener "+listener+" about a change in leaderboard "+
                        getName(), e);
            }
        }
    }

    @Override
    public void addLeaderboardChangeListener(LeaderboardChangeListener listener) {
        synchronized (leaderboardChangeListeners) {
            leaderboardChangeListeners.add(listener);
        }
    }

    @Override
    public void removeLeaderboardChangeListener(LeaderboardChangeListener listener) {
        synchronized (leaderboardChangeListeners) {
            leaderboardChangeListeners.remove(listener);
        }
    }

    @Override
    public LeaderboardDTO getLeaderboardDTO(TimePoint timePoint,
            Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            TrackedRegattaRegistry trackedRegattaRegistry, DomainFactory baseDomainFactory, boolean fillTotalPointsUncorrected) throws NoWindException,
            InterruptedException, ExecutionException {
        LeaderboardDTO result = null;
        if (timePoint == null) {
            // timePoint==null means live mode; however, if we're after the end of all races and after all score
            // corrections, don't use the live leaderboard updater which would keep re-calculating over and over again, but map
            // this to a usual non-live call which uses the regular LeaderboardDTOCache which is invalidated properly
            // when the tracked race associations or score corrections or tracked race contents changes:
            final TimePoint nowMinusDelay = this.getNowMinusDelay();
            final TimePoint timePointOfLatestModification = this.getTimePointOfLatestModification();
            if (fillTotalPointsUncorrected || (timePointOfLatestModification != null && !nowMinusDelay.before(timePointOfLatestModification))) {
                // if there hasn't been any modification to the leaderboard since nowMinusDelay, use non-live mode
                // and pull the result from the regular leaderboard cache:
                timePoint = timePointOfLatestModification;
            } else {
                // don't use the regular leaderboard cache; the race still seems to be on; use the live leaderboard updater instead:
                timePoint = null;
                result = this.getLiveLeaderboard(namesOfRaceColumnsForWhichToLoadLegDetails, addOverallDetails, trackedRegattaRegistry, baseDomainFactory);
            }
        }
        if (timePoint != null) {
            if (fillTotalPointsUncorrected) {
                // explicitly filling the uncorrected total points requires uncached recalculation
                result = computeDTO(timePoint, namesOfRaceColumnsForWhichToLoadLegDetails, addOverallDetails,
                        /* waitForLatestAnalyses=false because otherwise this may block, e.g., for background tasks
                           such as maneuver and mark passing calculations */ false,
                        trackedRegattaRegistry, baseDomainFactory, fillTotalPointsUncorrected);
            } else {
                // in replay we'd like up-to-date results; they are still cached
                // which is OK because the cache is invalidated whenever any of the tracked races attached to the
                // leaderboard changes.
                result = getLeaderboardDTOCache().getLeaderboardByName(timePoint,
                        namesOfRaceColumnsForWhichToLoadLegDetails, addOverallDetails, baseDomainFactory,
                        trackedRegattaRegistry);
            }
        }
        return result;
    }
    
    private LeaderboardDTO getLiveLeaderboard(Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails,
            boolean addOverallDetails, TrackedRegattaRegistry trackedRegattaRegistry, DomainFactory baseDomainFactory) throws NoWindException, ExecutionException {
        LiveLeaderboardUpdater liveLeaderboardUpdater = getLiveLeaderboardUpdater(trackedRegattaRegistry,
                baseDomainFactory);
        return liveLeaderboardUpdater.getLiveLeaderboard(namesOfRaceColumnsForWhichToLoadLegDetails, addOverallDetails);
    }

    private LiveLeaderboardUpdater getLiveLeaderboardUpdater(TrackedRegattaRegistry trackedRegattaRegistry,
            DomainFactory baseDomainFactory) {
        LiveLeaderboardUpdater result = this.liveLeaderboardUpdater;
        if (result == null) {
            synchronized (this) {
                result = this.liveLeaderboardUpdater;
                if (result == null) {
                    this.liveLeaderboardUpdater = new LiveLeaderboardUpdater(this, trackedRegattaRegistry, baseDomainFactory);
                    result = this.liveLeaderboardUpdater;
                }
            }
        }
        return result;
    }
    
    @Override
    public LeaderboardDTO computeDTO(final TimePoint timePoint,
            final Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, boolean addOverallDetails,
            final boolean waitForLatestAnalyses, TrackedRegattaRegistry trackedRegattaRegistry, final DomainFactory baseDomainFactory,
            final boolean fillTotalPointsUncorrected)
            throws NoWindException {
        return callWithCPUMeterWithException(()->{
            ShardingContext.checkConstraint(ShardingType.LEADERBOARDNAME, getName());
            final TimePoint startOfRequestHandling = MillisecondsTimePoint.now();
            final LeaderboardDTOCalculationReuseCache cache = new LeaderboardDTOCalculationReuseCache(timePoint);
            final BoatClass boatClass = getBoatClass();
            final LeaderboardDTO result = new LeaderboardDTO(this.getName(), timePoint.asDate(), 
                    this.getScoreCorrection().getTimePointOfLastCorrectionsValidity() == null ? null
                            : this.getScoreCorrection().getTimePointOfLastCorrectionsValidity().asDate(),
                    this.getScoreCorrection() == null ? null : this.getScoreCorrection().getComment(), this.getScoringScheme() == null ? null : this.getScoringScheme().getType(), this
                                    .getScoringScheme().isHigherBetter(), () -> UUID.randomUUID().toString(),
                            addOverallDetails, boatClass==null?null:new BoatClassDTO(boatClass.getName(), boatClass.getHullLength(), boatClass.getHullBeam()));
            result.type = getLeaderboardType();
            result.competitors = new ArrayList<>();
            result.displayName = this.getDisplayName();
            result.competitorDisplayNames = new HashMap<>();
            boolean isLeaderboardThatHasRegattaLike = this instanceof LeaderboardThatHasRegattaLike;
            if (isLeaderboardThatHasRegattaLike) {
                LeaderboardThatHasRegattaLike regattaLikeLeaderboard = (LeaderboardThatHasRegattaLike) this;
                result.canBoatsOfCompetitorsChangePerRace = regattaLikeLeaderboard.getRegattaLike().canBoatsOfCompetitorsChangePerRace();
            } else {
                result.canBoatsOfCompetitorsChangePerRace = false;
            }
            for (Competitor suppressedCompetitor : this.getSuppressedCompetitors()) {
                result.setSuppressed(baseDomainFactory.convertToCompetitorDTO(suppressedCompetitor), true);
            }
            // Now create the race columns and, as a future task, set their competitorsFromBestToWorst, then wait for all these
            // futures to finish:
            Map<RaceColumn, Future<List<CompetitorDTO>>> competitorsFromBestToWorstTasks = new HashMap<>();
            for (final RaceColumn raceColumn : this.getRaceColumns()) {
                boolean isMetaLeaderboardColumn = raceColumn instanceof MetaLeaderboardColumn;
                RaceColumnDTO raceColumnDTO = result.createEmptyRaceColumn(raceColumn.getName(), raceColumn.isMedalRace(),
                        raceColumn instanceof RaceColumnInSeries ? ((RaceColumnInSeries) raceColumn).getRegatta().getName() : null,
                        raceColumn instanceof RaceColumnInSeries ? ((RaceColumnInSeries) raceColumn).getSeries().getName() : null,
                        isMetaLeaderboardColumn, raceColumn.isOneAlwaysStaysOne());
                if (isMetaLeaderboardColumn && raceColumnDTO instanceof MetaLeaderboardRaceColumnDTO) {
                    calculateRacesMetadata((MetaLeaderboardColumn) raceColumn, (MetaLeaderboardRaceColumnDTO) raceColumnDTO, baseDomainFactory);
                }
                for (Fleet fleet : raceColumn.getFleets()) {
                    RegattaAndRaceIdentifier raceIdentifier = null;
                    RaceDTO race = null;
                    TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                    final FleetDTO fleetDTO = baseDomainFactory.convertToFleetDTO(fleet);
                    if (trackedRace != null) {
                        raceIdentifier = new RegattaNameAndRaceName(trackedRace.getTrackedRegatta().getRegatta().getName(),
                                trackedRace.getRace().getName());
                        race = baseDomainFactory.createRaceDTO(trackedRegattaRegistry, /* withGeoLocationData */ false, raceIdentifier, trackedRace);
                    }
                    // Note: the RaceColumnDTO won't be created by the following addRace call because it has been created
                    // above by the result.createEmptyRaceColumn call
                    result.addRace(raceColumn.getName(), raceColumn.getExplicitFactor(), getScoringScheme().getScoreFactor(raceColumn),
                            raceColumn instanceof RaceColumnInSeries ? ((RaceColumnInSeries) raceColumn).getRegatta().getName() : null,
                            raceColumn instanceof RaceColumnInSeries ? ((RaceColumnInSeries) raceColumn).getSeries().getName() : null,
                            fleetDTO, raceColumn.isMedalRace(), raceIdentifier, race, isMetaLeaderboardColumn, raceColumn.isOneAlwaysStaysOne());
                }
                Future<List<CompetitorDTO>> task = executor.submit(cpuMeterCallable(
                        () -> baseDomainFactory.getCompetitorDTOList(AbstractLeaderboardWithCache.this.getCompetitorsFromBestToWorst(
                                raceColumn, timePoint, cache)), CPUMeteringType.COMPETITORS_FROM_BEST_TO_WORST.name()));
                competitorsFromBestToWorstTasks.put(raceColumn, task);
            }
            // wait for the competitor orderings to have been computed for all race columns before continuing; subsequent tasks may depend on these data
            for (Map.Entry<RaceColumn, Future<List<CompetitorDTO>>> raceColumnAndTaskToJoin : competitorsFromBestToWorstTasks.entrySet()) {
                try {
                    result.setCompetitorsFromBestToWorst(raceColumnAndTaskToJoin.getKey().getName(), raceColumnAndTaskToJoin.getValue().get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    // See also bug 1371: for stability reasons, don't let the exception percolate but rather accept
                    // null values.
                    // If new evidence is provided, a re-calculation of the leaderboard will be triggered anyway. So
                    // this helps robustness from a user's perspective.
                    logger.log(
                            Level.SEVERE,
                            AbstractSimpleLeaderboardImpl.class.getName() + ".computeDTO(" + this.getName() + ", "
                                    + timePoint + ", " + namesOfRaceColumnsForWhichToLoadLegDetails+", addOverallDetails="+addOverallDetails
                                    + "): exception during computing competitor ordering for race column "+raceColumnAndTaskToJoin.getKey().getName(), e);
                }
            }
            result.setDelayToLiveInMillisForLatestRace(this.getDelayToLiveInMillis());
            result.rows = new HashMap<>();
            result.hasCarriedPoints = this.hasCarriedPoints();
            if (this.getResultDiscardingRule() instanceof ThresholdBasedResultDiscardingRule) {
                result.discardThresholds = ((ThresholdBasedResultDiscardingRule) this.getResultDiscardingRule())
                        .getDiscardIndexResultsStartingWithHowManyRaces();
            } else {
                result.discardThresholds = null;
            }
            // Computing the competitor leg ranks is expensive, especially in live mode, in case new events keep
            // invalidating the ranks cache in TrackedLegImpl. The problem then is that the sorting based on wind data is repeated for
            // each competitor, leading to square effort. We therefore need to compute the leg ranks for those races where leg
            // details are requested only once and pass them into getLeaderboardEntryDTO
            final Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache = new HashMap<Leg, LinkedHashMap<Competitor, Integer>>();
            for (final RaceColumn raceColumn : this.getRaceColumns()) {
                // if details for the column are requested, cache the leg's ranks
                if (namesOfRaceColumnsForWhichToLoadLegDetails != null
                        && namesOfRaceColumnsForWhichToLoadLegDetails.contains(raceColumn.getName())) {
                    for (Fleet fleet : raceColumn.getFleets()) {
                        TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                        if (trackedRace != null) {
                            trackedRace.getRace().getCourse().lockForRead();
                            try {
                                for (TrackedLeg trackedLeg : trackedRace.getTrackedLegs()) {
                                    legRanksCache.put(trackedLeg.getLeg(), trackedLeg.getRanks(timePoint, cache)); // TODO bug5147: leg ranks may also need to be merged across equal-weighted fleets if RaceColumn.hasCrossFleetMergedRanking() is true
                                }
                            } finally {
                                trackedRace.getRace().getCourse().unlockAfterRead();
                            }
                        }
                    }
                }
            }
            final ConcurrentMap<Pair<RaceColumn, Competitor>, RankingInfo> rankingInfoCache = new ConcurrentHashMap<>();
            final ConcurrentMap<Pair<Competitor, String>, Pair<LeaderboardRowDTO, Future<LeaderboardEntryDTO>>> futuresForCompetitorAndColumnName = new ConcurrentHashMap<>();
            for (final Competitor competitor : this.getCompetitorsFromBestToWorst(timePoint, cache)) {
                CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
                LeaderboardRowDTO row = new LeaderboardRowDTO();
                row.competitor = competitorDTO;
                row.fieldsByRaceColumnName = new HashMap<String, LeaderboardEntryDTO>();
                row.carriedPoints = this.hasCarriedPoints(competitor) ? this.getCarriedPoints(competitor) : null;
                row.netPoints = this.getNetPoints(competitor, timePoint);
                if (addOverallDetails) {
                    addOverallDetailsToRow(timePoint, competitor, row);
                }
                result.competitors.add(competitorDTO);
                final Set<RaceColumn> discardedRaceColumns = getResultDiscardingRule().getDiscardedRaceColumns(competitor, this, getRaceColumns(), timePoint, getScoringScheme());
                for (final RaceColumn raceColumn : this.getRaceColumns()) {
                    // in case boats can't change set the also the boat on the row to simplify access
                    if (result.canBoatsOfCompetitorsChangePerRace == false && row.boat == null) {
                        final Boat boatOfCompetitor = getBoatOfCompetitor(competitor, raceColumn);
                        // find a raceColumn where a boat is available
                        row.boat = boatOfCompetitor == null ? null : baseDomainFactory.convertToBoatDTO(boatOfCompetitor);
                    }
                    final boolean computeLegDetails = namesOfRaceColumnsForWhichToLoadLegDetails != null &&
                            namesOfRaceColumnsForWhichToLoadLegDetails.contains(raceColumn.getName());
                    Future<LeaderboardEntryDTO> future = executor.submit(() -> {
                        // if leg details are to be requested, the ranking info needs to be provided:
                        // TODO bug5143 (performance): shouldn't the rankingInfoCache be passed on because detail computations need them all?
                        final RankingInfo rankingInfo = computeLegDetails ? rankingInfoCache.computeIfAbsent(new Pair<>(raceColumn, competitor),
                                raceColumnAndCompetitor->{
                                    final TrackedRace trackedRace = raceColumnAndCompetitor.getA().getTrackedRace(raceColumnAndCompetitor.getB());
                                    return trackedRace==null?null:trackedRace.getRankingMetric().getRankingInfo(timePoint, cache);
                                }) : null;
                        Entry entry = AbstractLeaderboardWithCache.this.getEntry(competitor, raceColumn, timePoint, discardedRaceColumns, cache);
                        return getLeaderboardEntryDTO(entry, raceColumn, competitor, timePoint, computeLegDetails,
                                rankingInfo, waitForLatestAnalyses, legRanksCache, baseDomainFactory,
                                fillTotalPointsUncorrected, cache);
                    });
                    futuresForCompetitorAndColumnName.put(new Pair<>(competitor, raceColumn.getName()), new Pair<>(row, future));
                }
                if (addOverallDetails) {
                    //this reuses several prior calculated fields, so must be evaluated after them
                    row.totalScoredRaces = this.getTotalRaces(competitor, row, timePoint);
                }
                result.rows.put(competitorDTO, row);
                String displayName = this.getDisplayName(competitor);
                if (displayName != null) {
                    result.competitorDisplayNames.put(competitorDTO, displayName);
                }
                if (isLeaderboardThatHasRegattaLike) {
                    LeaderboardThatHasRegattaLike regattaLikeLeaderboard = (LeaderboardThatHasRegattaLike) this;
                    final Duration regattaLevelTimeOnDistanceAllowancePerNauticalMile = regattaLikeLeaderboard.getRegattaLike().getTimeOnDistanceAllowancePerNauticalMile(competitor, Optional.empty());
                    final Double regattaLevelTimeOnTimeFactor = regattaLikeLeaderboard.getRegattaLike().getTimeOnTimeFactor(competitor, Optional.empty());
                    row.effectiveTimeOnDistanceAllowancePerNauticalMile = regattaLevelTimeOnDistanceAllowancePerNauticalMile;
                    row.effectiveTimeOnTimeFactor = regattaLevelTimeOnTimeFactor;
                }
            } // competitors
            for (java.util.Map.Entry<Pair<Competitor, String>, Pair<LeaderboardRowDTO, Future<LeaderboardEntryDTO>>> competitorAndRaceColumnNameAndRowAndFuture : futuresForCompetitorAndColumnName.entrySet()) {
                try {
                    final LeaderboardRowDTO rowForCompetitor = competitorAndRaceColumnNameAndRowAndFuture.getValue().getA();
                    final String columnName = competitorAndRaceColumnNameAndRowAndFuture.getKey().getB();
                    final Future<LeaderboardEntryDTO> future = competitorAndRaceColumnNameAndRowAndFuture.getValue().getB();
                    rowForCompetitor.fieldsByRaceColumnName.put(columnName, future.get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    // See also bug 1371: for stability reasons, don't let the exception percolate but rather accept
                    // null values.
                    // If new evidence is provided, a re-calculation of the leaderboard will be triggered anyway. So
                    // this helps robustness from a user's perspective.
                    final Competitor competitor = competitorAndRaceColumnNameAndRowAndFuture.getKey().getA();
                    logger.log(
                            Level.SEVERE,
                            AbstractSimpleLeaderboardImpl.class.getName() + ".computeDTO(" + this.getName() + ", "
                                    + timePoint + ", " + namesOfRaceColumnsForWhichToLoadLegDetails+", addOverallDetails="+addOverallDetails
                                    + "): exception during computing leaderboard entry for competitor "
                                    + competitor.getName() + " in race column " + competitorAndRaceColumnNameAndRowAndFuture.getKey()
                                    + ". Leaving empty.", e);
                }
            }
            final Duration computeTime = startOfRequestHandling.until(MillisecondsTimePoint.now());
            logger.info("computeLeaderboardByName(" + this.getName() + ", " + timePoint + ", "
                    + namesOfRaceColumnsForWhichToLoadLegDetails + ", addOverallDetails=" + addOverallDetails + ") took "
                    + computeTime);
            updateStats(startOfRequestHandling, computeTime);
            return result;
        }, CPUMeteringType.LEADERBOARD_COMPUTE_DTO.name());
    }
 
    /**
     * Updates statistics about how long it took to compute DTOs for this leaderboard.
     * 
     * @param startOfRequestHandling
     *            when the request to compute the DTO was received
     * @param computeDuration
     *            how long it took to complete the DTO calculation request
     */
    private void updateStats(TimePoint startOfRequestHandling, Duration computeDuration) {
        try {
            timingStats.recordTiming(startOfRequestHandling, computeDuration);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception trying to update leaderboard compute time stats", e);
        }
    }
    
    @Override
    public Map<Duration, Pair<Duration, Integer>> getComputationTimeStatistics() {
        return timingStats.getAverageDurationsAndNumberOfRequests();
    }

    private Integer getTotalRaces(Competitor competitor, LeaderboardRowDTO row, TimePoint timePoint) {
        int amount = 0;
        for (RaceColumn raceColumn : getRaceColumns()) {
            // reuse calculations already done earlier
            LeaderboardEntryDTO entry = row.fieldsByRaceColumnName.get(raceColumn.getName());
            if (entry != null && entry.netPoints != null) {
                if (entry.reasonForMaxPoints.equals(MaxPointsReason.NONE)
                        || !Util.contains(Arrays.asList(MAX_POINTS_REASONS_THAT_IDENTIFY_NON_FINISHED_RACES),
                                entry.reasonForMaxPoints)) {
                    if (raceColumn instanceof MetaLeaderboardColumn) {
                        Leaderboard leaderBoardForMeta = ((MetaLeaderboardColumn) raceColumn).getLeaderboard();
                        for (RaceColumn subRace : leaderBoardForMeta.getRaceColumns()) {
                            Double netPointsForSubRace = leaderBoardForMeta.getNetPoints(competitor, subRace, timePoint);
                            MaxPointsReason subMaxPointsReason = leaderBoardForMeta.getMaxPointsReason(competitor, subRace, timePoint);
                            if(netPointsForSubRace != null){
                                if (subMaxPointsReason.equals(MaxPointsReason.NONE) || !Util.contains(
                                        Arrays.asList(MAX_POINTS_REASONS_THAT_IDENTIFY_NON_FINISHED_RACES),
                                        subMaxPointsReason)) {
                                    amount++;
                                }
                            }
                        }
                    } else {
                        amount++;
                    }

                }
            }
        }
        return amount;
    }

    /**
     * @param rankingInfo
     *            must be provided when {@code addLegDetails} is {@code true}; it may, however, be the case that there
     *            is no {@link TrackedRace} for the {@code competitor} for which this entry shall be computed; in such
     *            cases it is permissible to pass {@code null} for this parameter despite {@code addLegDetails} being
     *            set.
     * @param waitForLatestAnalyses
     *            if <code>false</code>, this method is allowed to read the maneuver analysis results from a cache that
     *            may not reflect all data already received; otherwise, the method will always block for the latest
     *            cache updates to have happened before returning.
     * @param fillTotalPointsUncorrected
     *            tells if {@link LeaderboardEntryDTO#totalPointsUncorrected} shall be filled; filling it is rather
     *            expensive, especially when compared to simply retrieving a score correction, and particularly if in a
     *            larger fleet a number of competitors haven't properly finished the race. This should only be used for
     *            leaderboard editing where a user needs to see what the uncorrected score was that would be used when
     *            the correction was removed.
     */
    private LeaderboardEntryDTO getLeaderboardEntryDTO(Entry entry, RaceColumn raceColumn, Competitor competitor,
            TimePoint timePoint, boolean addLegDetails, RankingInfo rankingInfo, boolean waitForLatestAnalyses,
            Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache, DomainFactory baseDomainFactory,
            boolean fillTotalPointsUncorrected, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache)
            throws NotEnoughDataHasBeenAddedException {
        final LeaderboardEntryDTO entryDTO = new LeaderboardEntryDTO();
        final TrackedRace trackedRace = raceColumn.getTrackedRace(competitor);
        entryDTO.race = trackedRace == null ? null : trackedRace.getRaceIdentifier();
        Boat boat = getBoatOfCompetitor(competitor, raceColumn);
        entryDTO.boat = boat == null ? null : baseDomainFactory.convertToBoatDTO(boat);
        entryDTO.totalPoints = entry.getTotalPoints();
        if (fillTotalPointsUncorrected) {
            entryDTO.totalPointsUncorrected = entry.getTotalPointsUncorrected();
        }
        entryDTO.incrementalScoreCorrectionInPoints = entry.getIncrementalScoreCorrectionInPoints();
        entryDTO.totalPointsCorrected = entry.isTotalPointsCorrected();
        entryDTO.netPoints = entry.getNetPoints();
        entryDTO.reasonForMaxPoints = entry.getMaxPointsReason();
        entryDTO.discarded = entry.isDiscarded();
        final GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace == null ? null : trackedRace.getTrack(competitor);
        if (trackedRace != null) {
            Date timePointOfLastPositionFixAtOrBeforeQueryTimePoint = getTimePointOfLastFixAtOrBefore(competitor, trackedRace, timePoint);
            if (track != null) {
                entryDTO.averageSamplingInterval = track.getAverageIntervalBetweenRawFixes();
                entryDTO.currentSpeedAndCourseOverGround = track.getEstimatedSpeed(timePoint);
            }
            if (timePointOfLastPositionFixAtOrBeforeQueryTimePoint != null) {
                long timeDifferenceInMs = timePoint.asMillis() - timePointOfLastPositionFixAtOrBeforeQueryTimePoint.getTime();
                entryDTO.timeSinceLastPositionFixInSeconds = timeDifferenceInMs == 0 ? 0.0 : timeDifferenceInMs / 1000.0;  
            } else {
                entryDTO.timeSinceLastPositionFixInSeconds = null;  
            }
            final Distance averageRideHeight = trackedRace.getAverageRideHeight(competitor, timePoint);
            entryDTO.averageRideHeightInMeters = averageRideHeight == null ? null : averageRideHeight.getMeters();
        }
        if (addLegDetails && trackedRace != null) {
            try {
                RaceDetails raceDetails = getRaceDetails(trackedRace, competitor, timePoint, waitForLatestAnalyses,
                        legRanksCache, rankingInfo, cache);
                entryDTO.legDetails = raceDetails.getLegDetails();
                entryDTO.windwardDistanceToCompetitorFarthestAheadInMeters = raceDetails.getWindwardDistanceToCompetitorFarthestAhead() == null ? null
                        : raceDetails.getWindwardDistanceToCompetitorFarthestAhead().getMeters();
                entryDTO.gapToLeaderInOwnTime = trackedRace.getRankingMetric().getGapToLeaderInOwnTime(rankingInfo, competitor, cache);
                entryDTO.averageAbsoluteCrossTrackErrorInMeters = raceDetails.getAverageAbsoluteCrossTrackError() == null ? null
                        : raceDetails.getAverageAbsoluteCrossTrackError().getMeters();
                entryDTO.averageSignedCrossTrackErrorInMeters = raceDetails.getAverageSignedCrossTrackError() == null ? null
                        : raceDetails.getAverageSignedCrossTrackError().getMeters();
                entryDTO.timeSailedSinceRaceStart = raceDetails.getTimeSailedSinceRaceStart();
                entryDTO.calculatedTime = raceDetails.getCorrectedTime();
                if (trackedRace != null && trackedRace.getRankingMetric() instanceof ORCPerformanceCurveByImpliedWindRankingMetric) {
                        entryDTO.impliedWind = ((ORCPerformanceCurveByImpliedWindRankingMetric) trackedRace.getRankingMetric()).getImpliedWind(competitor, timePoint, cache);
                }
                entryDTO.calculatedTimeAtEstimatedArrivalAtCompetitorFarthestAhead = raceDetails.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead();
                entryDTO.gapToLeaderInOwnTime = raceDetails.getGapToLeaderInOwnTime();
                entryDTO.percentTargetBoatSpeed = raceDetails.getPercentTargetBoatSpeed();
                try {
                    BravoFixTrack<Competitor> sensorTrack = trackedRace.getSensorTrack(competitor, BravoFixTrack.TRACK_NAME);
                    if (sensorTrack != null) {
                        final BravoFix bravoFix = sensorTrack.getFirstFixAtOrAfter(timePoint);
                        entryDTO.heel = bravoFix == null ? null : bravoFix.getHeel();
                        entryDTO.pitch = bravoFix == null ? null : bravoFix.getPitch();
                        if (sensorTrack.hasExtendedFixes() && bravoFix instanceof BravoExtendedFix) {
                            BravoExtendedFix fix = (BravoExtendedFix) bravoFix;
                            entryDTO.setExpeditionAWA(fix.getExpeditionAWA());
                            entryDTO.setExpeditionAWS(fix.getExpeditionAWS());
                            entryDTO.setExpeditionTWA(fix.getExpeditionTWA());
                            entryDTO.setExpeditionTWS(fix.getExpeditionTWS());
                            entryDTO.setExpeditionTWD(fix.getExpeditionTWD());
                            entryDTO.setExpeditionBoatSpeed(fix.getExpeditionBSP());
                            entryDTO.setExpeditionTargBoatSpeed(fix.getExpeditionBSP_TR());
                            entryDTO.setExpeditionSOG(fix.getExpeditionSOG());
                            entryDTO.setExpeditionCOG(fix.getExpeditionCOG());
                            entryDTO.setExpeditionForestayLoad(fix.getExpeditionForestayLoad());
                            entryDTO.setExpeditionRake(fix.getExpeditionRake());
                            entryDTO.setExpeditionHeading(fix.getExpeditionHDG());
                            entryDTO.setExpeditionHeel(fix.getExpeditionHeel());
                            entryDTO.setExpeditionTargetHeel(fix.getExpeditionTG_Heell());
                            entryDTO.setExpeditionTimeToGunInSeconds(fix.getExpeditionTmToGunInSeconds());
                            entryDTO.setExpeditionTimeToBurnToLineInSeconds(fix.getExpeditionTmToBurnInSeconds());
                            entryDTO.setExpeditionDistanceBelowLineInMeters(fix.getExpeditionBelowLnInMeters());
                            entryDTO.setExpeditionCourseDetail(fix.getExpeditionCourse());
                            entryDTO.setExpeditionBaro(fix.getExpeditionBARO());
                            entryDTO.setExpeditionLoadP(fix.getExpeditionLoadP());
                            entryDTO.setExpeditionLoadS(fix.getExpeditionLoadS());
                            entryDTO.setExpeditionJibCarPort(fix.getExpeditionJibCarPort());
                            entryDTO.setExpeditionJibCarStbd(fix.getExpeditionJibCarStbd());
                            entryDTO.setExpeditionMastButt(fix.getExpeditionMastButt());
                            entryDTO.setExpeditionRateOfTurn(fix.getExpeditionRateOfTurn());
                        }
                    }
                } catch (Exception e) {
                   logger.log(Level.WARNING, "There was an error determining expedition or extended data", e);
                }
                final TimePoint startOfRace = trackedRace.getStartOfRace();
                if (startOfRace != null) {
                    Waypoint startWaypoint = trackedRace.getRace().getCourse().getFirstWaypoint();
                    NavigableSet<MarkPassing> competitorMarkPassings = trackedRace.getMarkPassings(competitor);
                    trackedRace.lockForRead(competitorMarkPassings);
                    try {
                        if (!competitorMarkPassings.isEmpty()) {
                            final MarkPassing firstMarkPassing = competitorMarkPassings.iterator().next();
                            if (firstMarkPassing.getWaypoint() == startWaypoint) {
                                Distance distanceToStartLineFiveSecondsBeforeStartOfRace = trackedRace.getDistanceToStartLine(competitor, /*milliseconds before start*/ 5000);
                                entryDTO.distanceToStartLineFiveSecondsBeforeStartInMeters = distanceToStartLineFiveSecondsBeforeStartOfRace == null ? null
                                        : distanceToStartLineFiveSecondsBeforeStartOfRace.getMeters();
                                Speed speedFiveSecondsBeforeStartOfRace = trackedRace.getSpeed(competitor, /*milliseconds before start*/ 5000);
                                entryDTO.speedOverGroundFiveSecondsBeforeStartInKnots = speedFiveSecondsBeforeStartOfRace == null ? null
                                        : speedFiveSecondsBeforeStartOfRace.getKnots();
                                Distance distanceToStartLineAtStartOfRace = trackedRace.getDistanceToStartLine(
                                        competitor, startOfRace);
                                entryDTO.distanceToStartLineAtStartOfRaceInMeters = distanceToStartLineAtStartOfRace == null ? null
                                        : distanceToStartLineAtStartOfRace.getMeters();
                                Speed speedAtStartTime = track == null ? null : track.getEstimatedSpeed(startOfRace);
                                entryDTO.speedOverGroundAtStartOfRaceInKnots = speedAtStartTime == null ? null
                                        : speedAtStartTime.getKnots();
                                TimePoint competitorStartTime = firstMarkPassing.getTimePoint();
                                entryDTO.timeBetweenRaceStartAndCompetitorStartInSeconds = startOfRace.until(competitorStartTime).asSeconds();
                                Speed competitorSpeedWhenPassingStart = track == null ? null : track
                                        .getEstimatedSpeed(competitorStartTime);
                                entryDTO.speedOverGroundAtPassingStartWaypointInKnots = competitorSpeedWhenPassingStart == null ? null
                                        : competitorSpeedWhenPassingStart.getKnots();
                                try {
                                    entryDTO.startTack = trackedRace.getTack(competitor, competitorStartTime);
                                } catch (NoWindException nwe) {
                                    entryDTO.startTack = null; // leave empty in case no wind information is available
                                }
                                Distance distanceFromStarboardSideOfStartLineWhenPassingStart = trackedRace
                                        .getDistanceFromStarboardSideOfStartLineWhenPassingStart(competitor);
                                entryDTO.distanceToStarboardSideOfStartLineInMeters = distanceFromStarboardSideOfStartLineWhenPassingStart == null ? null
                                        : distanceFromStarboardSideOfStartLineWhenPassingStart.getMeters();
                            }
                        }
                    } finally {
                        trackedRace.unlockAfterRead(competitorMarkPassings);
                    }
                }
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            } catch (ExecutionException | MaxIterationsExceededException | FunctionEvaluationException e) {
                throw new RuntimeException(e); // the future used to calculate the leg details was interrupted; escalate as runtime exception
            }
        }
        final Fleet fleet = entry.getFleet();
        entryDTO.fleet = fleet == null ? null : baseDomainFactory.convertToFleetDTO(fleet);
        return entryDTO;
    }

    private Boat getBoatOfCompetitor(Competitor competitor, RaceColumn raceColumn) {
        final Boat boat;
        if (competitor.hasBoat()) {
            boat = ((CompetitorWithBoat) competitor).getBoat();
        } else {
            boat = getBoatOfCompetitor(competitor, raceColumn, raceColumn.getFleetOfCompetitor(competitor));
        }
        return boat;
    }

    private void calculateRacesMetadata(MetaLeaderboardColumn metaLeaderboardColumn, MetaLeaderboardRaceColumnDTO columnDTO,
            final DomainFactory baseDomainFactory) {
        for (final RaceColumn raceColumn : metaLeaderboardColumn.getLeaderboard().getRaceColumns()) {
            for (Fleet fleet : raceColumn.getFleets()) {
                TrackedRace trackedRace = raceColumn.getTrackedRace(fleet);
                if (trackedRace != null) {
                    String regattaName = trackedRace.getTrackedRegatta().getRegatta().getName();
                    String raceName = trackedRace.getRace().getName();
                    RegattaAndRaceIdentifier raceIdentifier = new RegattaNameAndRaceName(regattaName, raceName);
                    columnDTO.addRace(new BasicRaceDTO(raceIdentifier, baseDomainFactory.createTrackedRaceDTO(trackedRace)));
                }
            }
        }
    }

    private void addOverallDetailsToRow(final TimePoint timePoint,
            final Competitor competitor, LeaderboardRowDTO row) throws NoWindException {
        final com.sap.sse.common.Util.Pair<GPSFixMoving, Speed> maximumSpeedOverGround = this.getMaximumSpeedOverGround(competitor, timePoint);
        if (maximumSpeedOverGround != null && maximumSpeedOverGround.getB() != null) {
            row.maximumSpeedOverGroundInKnots = maximumSpeedOverGround.getB().getKnots();
            row.whenMaximumSpeedOverGroundWasAchieved = maximumSpeedOverGround.getA().getTimePoint().asDate();
        }
        Map<TrackedLeg, LegType> legTypeCache = new HashMap<>();
        final Duration totalTimeSailedDownwind = this.getTotalTimeSailedInLegType(competitor, LegType.DOWNWIND, timePoint, legTypeCache);
        row.totalTimeSailedDownwindInSeconds = totalTimeSailedDownwind==null?null:totalTimeSailedDownwind.asSeconds();
        final Duration totalTimeSailedUpwind = this.getTotalTimeSailedInLegType(competitor, LegType.UPWIND, timePoint, legTypeCache);
        row.totalTimeSailedUpwindInSeconds = totalTimeSailedUpwind==null?null:totalTimeSailedUpwind.asSeconds();
        final Duration totalTimeSailedReaching = this.getTotalTimeSailedInLegType(competitor, LegType.REACHING, timePoint, legTypeCache);
        row.totalTimeSailedReachingInSeconds = totalTimeSailedReaching==null?null:totalTimeSailedReaching.asSeconds();
        final Duration totalTimeSailed = this.getTotalTimeSailed(competitor, timePoint);
        row.totalTimeSailedInSeconds = totalTimeSailed==null?null:totalTimeSailed.asSeconds();
        final Distance totalDistanceTraveled = this.getTotalDistanceTraveled(competitor, timePoint);
        row.totalDistanceTraveledInMeters = totalDistanceTraveled==null?null:totalDistanceTraveled.getMeters();
        final Distance totalDistanceFoiled = this.getTotalDistanceFoiled(competitor, timePoint);
        row.totalDistanceFoiledInMeters = totalDistanceFoiled==null?null:totalDistanceFoiled.getMeters();
        final Duration totalDurationFoiled = this.getTotalDurationFoiled(competitor, timePoint);
        row.totalDurationFoiledInSeconds = totalDurationFoiled==null?null:totalDurationFoiled.asSeconds();
    }

    protected LeaderboardDTOCache getLeaderboardDTOCache() {
        LeaderboardDTOCache result = this.leaderboardDTOCache;
        if (result == null) {
            synchronized (this) {
                result = this.leaderboardDTOCache;
                if (result == null) {
                    // The leaderboard cache is invalidated upon all competitor and mark position changes; some analyzes
                    // are pretty expensive, such as the maneuver re-calculation. Waiting for the latest analysis after only a
                    // single fix was updated is too expensive if users use the replay feature while a race is still running.
                    // Therefore, using waitForLatestAnalyses==false seems appropriate here.
                    this.leaderboardDTOCache = new LeaderboardDTOCache(/* waitForLatestAnalyses */false, this);
                    result = this.leaderboardDTOCache;
                }
            }
        }
        return result;
    }
    
    /**
     * If <code>timePoint</code> is after the end of the race's tracking the query will be adjusted to obtain the values
     * at the end of the {@link TrackedRace#getEndOfTracking() race's tracking time}. If the time point adjusted this
     * way equals the end of the tracking time, the query results will be looked up in a cache first and if not found,
     * they will be stored to the cache after calculating them. A cache invalidation {@link RaceChangeListener listener}
     * will be registered with the race which will be triggered for any event received by the race.
     * @param waitForLatestAnalyses
     *            if <code>false</code>, this method is allowed to read the maneuver analysis results from a cache that
     *            may not reflect all data already received; otherwise, the method will always block for the latest
     *            cache updates to have happened before returning.
     */
    private RaceDetails getRaceDetails(TrackedRace trackedRace, Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalyses, Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache,
            RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache)
            throws InterruptedException, ExecutionException, MaxIterationsExceededException,
            FunctionEvaluationException, NotEnoughDataHasBeenAddedException {
        final RaceDetails raceDetails;
        if (trackedRace.getEndOfTracking() != null && trackedRace.getEndOfTracking().compareTo(timePoint) < 0) {
            raceDetails = getRaceDetailsForEndOfTrackingFromCacheOrCalculateAndCache(trackedRace, competitor, legRanksCache, rankingInfo, cache);
        } else {
            raceDetails = calculateRaceDetails(trackedRace, competitor, timePoint, waitForLatestAnalyses, legRanksCache, cache, rankingInfo);
        }
        return raceDetails;
    }

    private RaceDetails getRaceDetailsForEndOfTrackingFromCacheOrCalculateAndCache(final TrackedRace trackedRace,
            final Competitor competitor, final Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache,
            RankingInfo rankingInfo, final WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws InterruptedException, ExecutionException {
        final com.sap.sse.common.Util.Pair<TrackedRace, Competitor> key = new com.sap.sse.common.Util.Pair<TrackedRace, Competitor>(trackedRace, competitor);
        RunnableFuture<RaceDetails> raceDetails;
        final boolean needToRunRaceDetails; // when found in cache, another call to this method is already running it; else, it needs to be run here
        synchronized (raceDetailsAtEndOfTrackingCache) {
            raceDetails = raceDetailsAtEndOfTrackingCache.get(key);
            if (raceDetails == null) {
                needToRunRaceDetails = true;
                raceDetails = new FutureTaskWithTracingGet<RaceDetails>("RaceDetails for "+trackedRace, new Callable<RaceDetails>() {
                    @Override
                    public RaceDetails call() throws Exception {
                        TimePoint end = trackedRace.getEndOfRace();
                        if (end == null) {
                            end = trackedRace.getEndOfTracking();
                        }
                        return calculateRaceDetails(trackedRace, competitor, end,
                                // TODO see bug 1358: for now, use waitForLatest==false until we've switched to optimistic locking for the course read lock
                                /* TODO old comment when it was still true: "because this is done only once after end of tracking" */
                                /* waitForLatestAnalyses (maneuver and cross track error) */ false,
                                legRanksCache, cache,
                                // can't re-use rankingInfo because we're now computing for a different time point:
                                trackedRace.getRankingMetric().getRankingInfo(end, cache));
                    }
                });
                raceDetailsAtEndOfTrackingCache.put(key, raceDetails); // this way, 
                final CacheInvalidationListener cacheInvalidationListener = new CacheInvalidationListener(trackedRace, competitor);
                trackedRace.addListener(cacheInvalidationListener);
                cacheInvalidationListeners.add(cacheInvalidationListener);
            } else {
                needToRunRaceDetails = false;
            }
        }
        // now, outside the synchronized block, run task if needed:
        if (needToRunRaceDetails) {
            raceDetails.run();
        }
        return raceDetails.get();
    }

    private RaceDetails calculateRaceDetails(TrackedRace trackedRace, Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalyses, Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache, final RankingInfo rankingInfo)
            throws MaxIterationsExceededException, FunctionEvaluationException, NotEnoughDataHasBeenAddedException {
        final List<LegEntryDTO> legDetails = new ArrayList<LegEntryDTO>();
        final Course course = trackedRace.getRace().getCourse();
        course.lockForRead(); // hold back any course re-configurations while looping over the legs
        try {
            for (Leg leg : course.getLegs()) {
                LegEntryDTO legEntry;
                // We loop over a copy of the course's legs; during a course change, legs may become "stale," even with
                // regard to the leg/trackedLeg structures inside the tracked race which is updated by the course change
                // immediately. That's why we've acquired a read lock for the course above.
                TrackedLegOfCompetitor trackedLeg = trackedRace.getTrackedLeg(competitor, leg);
                if (trackedLeg != null && trackedLeg.hasStartedLeg(timePoint)) {
                    legEntry = createLegEntry(trackedLeg, timePoint, waitForLatestAnalyses, legRanksCache, rankingInfo, cache);
                } else {
                    legEntry = null;
                }
                legDetails.add(legEntry);
            }
            final Distance windwardDistanceToCompetitorFarthestAhead = trackedRace == null ? null : trackedRace
                    .getWindwardDistanceToCompetitorFarthestAhead(competitor, timePoint, WindPositionMode.LEG_MIDDLE, rankingInfo, cache);
            Distance averageAbsoluteCrossTrackError;
            averageAbsoluteCrossTrackError = trackedRace == null ? null : trackedRace.getAverageAbsoluteCrossTrackError(
                competitor, timePoint, waitForLatestAnalyses, cache);
            Distance averageSignedCrossTrackError;
            averageSignedCrossTrackError = trackedRace == null ? null : trackedRace.getAverageSignedCrossTrackError(
                competitor, timePoint, waitForLatestAnalyses, cache);
            final CompetitorRankingInfo competitorRankingInfo = rankingInfo.getCompetitorRankingInfo().apply(competitor);
            return new RaceDetails(legDetails, windwardDistanceToCompetitorFarthestAhead, averageAbsoluteCrossTrackError, averageSignedCrossTrackError,
                    trackedRace.getRankingMetric().getGapToLeaderInOwnTime(rankingInfo, competitor, cache),
                    trackedRace.getPercentTargetBoatSpeed(competitor, timePoint, cache),
                    trackedRace.getTimeSailedSinceRaceStart(competitor, timePoint),
                    competitorRankingInfo == null ? null : competitorRankingInfo.getCorrectedTime(), competitorRankingInfo == null ? null : competitorRankingInfo.getCorrectedTimeAtEstimatedArrivalAtCompetitorFarthestAhead());
        } finally {
            course.unlockAfterRead();
        }
    }

    private LegEntryDTO createLegEntry(TrackedLegOfCompetitor trackedLeg, TimePoint timePoint,
            boolean waitForLatestAnalyses, Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache,
            RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        LegEntryDTO result;
        final Duration time = trackedLeg.getTime(timePoint);
        if (trackedLeg == null || time == null) {
            result = null;
        } else {
            result = new LegEntryDTO();
            try {
                result.legType = trackedLeg.getTrackedLeg().getLegType(timePoint);
            } catch (NoWindException nwe) {
                result.legType = null; // can't determine leg type without wind data
            }
            final Speed averageSpeedOverGround = trackedLeg.getAverageSpeedOverGround(timePoint);
            result.averageSpeedOverGroundInKnots = averageSpeedOverGround == null ? null : averageSpeedOverGround.getKnots();
            final boolean hasFinishedLeg = trackedLeg.hasFinishedLeg(timePoint);
            Distance currentOrAverageAbsoluteCrossTrackError;
            if (hasFinishedLeg) {
                currentOrAverageAbsoluteCrossTrackError = trackedLeg.getAverageAbsoluteCrossTrackError(timePoint, waitForLatestAnalyses);
            } else {
                currentOrAverageAbsoluteCrossTrackError = trackedLeg.getAbsoluteCrossTrackError(timePoint);
            }
            result.currentOrAverageAbsoluteCrossTrackErrorInMeters = currentOrAverageAbsoluteCrossTrackError == null ? null : currentOrAverageAbsoluteCrossTrackError.getMeters();
            Distance currentOrAverageSignedCrossTrackError;
            if (hasFinishedLeg) {
                currentOrAverageSignedCrossTrackError = trackedLeg.getAverageSignedCrossTrackError(timePoint, waitForLatestAnalyses);
            } else {
                currentOrAverageSignedCrossTrackError = trackedLeg.getSignedCrossTrackError(timePoint);
            }
            result.currentOrAverageSignedCrossTrackErrorInMeters = currentOrAverageSignedCrossTrackError == null ? null : currentOrAverageSignedCrossTrackError.getMeters();
            Double speedOverGroundInKnots;
            if (hasFinishedLeg) {
                speedOverGroundInKnots = averageSpeedOverGround == null ? null : averageSpeedOverGround.getKnots();
                final Distance averageRideHeight = trackedLeg.getAverageRideHeight(timePoint);
                result.currentRideHeightInMeters = averageRideHeight == null ? null : averageRideHeight.getMeters();
            } else {
                final SpeedWithBearing speedOverGround = trackedLeg.getSpeedOverGround(timePoint);
                speedOverGroundInKnots = speedOverGround == null ? null : speedOverGround.getKnots();
                final Distance rideHeight = trackedLeg.getRideHeight(timePoint);
                result.currentRideHeightInMeters = rideHeight == null ? null : rideHeight.getMeters();
            }
            Bearing heel = trackedLeg.getHeel(timePoint);
            result.currentHeelInDegrees = heel == null ? null : heel.getDegrees();
            Bearing pitch = trackedLeg.getPitch(timePoint);
            result.currentPitchInDegrees = pitch == null ? null : pitch.getDegrees();
            
            Distance distanceFoiled = trackedLeg.getDistanceFoiled(timePoint);
            result.currentDistanceFoiledInMeters = distanceFoiled == null ? null : distanceFoiled.getMeters();
            Duration durationFoiled = trackedLeg.getDurationFoiled(timePoint);
            result.currentDurationFoiledInSeconds = durationFoiled == null ? null : durationFoiled.asSeconds();

            result.currentSpeedOverGroundInKnots = speedOverGroundInKnots == null ? null : speedOverGroundInKnots;
            Distance distanceTraveled = trackedLeg.getDistanceTraveled(timePoint);
            result.distanceTraveledInMeters = distanceTraveled == null ? null : distanceTraveled.getMeters();
            Distance distanceTraveledConsideringGateStart = trackedLeg.getDistanceTraveledConsideringGateStart(timePoint);
            result.distanceTraveledIncludingGateStartInMeters = distanceTraveledConsideringGateStart == null ? null : distanceTraveledConsideringGateStart.getMeters();
            final Duration estimatedTimeToNextMarkInSeconds = trackedLeg.getEstimatedTimeToNextMark(timePoint, WindPositionMode.EXACT, cache);
            result.estimatedTimeToNextWaypointInSeconds = estimatedTimeToNextMarkInSeconds==null?null:estimatedTimeToNextMarkInSeconds.asSeconds();
            result.timeInMilliseconds = time.asMillis();
            result.finished = hasFinishedLeg;
            final TimePoint legFinishTime = trackedLeg.getFinishTime();
            // See bug 3829: there is an unlikely possibility that legFinishTime is null and the call to hasFinishedLeg below
            // says that the leg has already finished. This can happen if the corresponding mark passing arrives between the two
            // calls. To avoid having to use expensive locking, we'll just double-check here if legFinishTime is null and
            // treat this as if trackedLeg.hasFinishedLeg(timePoint) had returned false.
            result.correctedTotalTime = trackedLeg.hasStartedLeg(timePoint) ? trackedLeg.getTrackedLeg().getTrackedRace().getRankingMetric().getCorrectedTime(trackedLeg.getCompetitor(),
                    hasFinishedLeg && legFinishTime != null ? legFinishTime : timePoint, cache) : null;
            // fetch the leg gap in own corrected time from the ranking metric
            final Duration gapToLeaderInOwnTime = trackedLeg.getTrackedLeg().getTrackedRace().getRankingMetric().
                    getLegGapToLegLeaderInOwnTime(trackedLeg, timePoint, rankingInfo, cache);
            result.gapToLeaderInSeconds = gapToLeaderInOwnTime == null ? null : gapToLeaderInOwnTime.asSeconds();
            if (result.gapToLeaderInSeconds != null) {
                final Duration gapAtEndOfPreviousLeg = getGapAtEndOfPreviousLeg(trackedLeg, rankingInfo, cache);
                if (gapAtEndOfPreviousLeg != null) {
                    result.gapChangeSinceLegStartInSeconds = result.gapToLeaderInSeconds - gapAtEndOfPreviousLeg.asSeconds();
                }
            }
            LinkedHashMap<Competitor, Integer> legRanks = legRanksCache.get(trackedLeg.getLeg());
            if (legRanks != null) {
                result.rank = legRanks.get(trackedLeg.getCompetitor());
            } else {
                result.rank = trackedLeg.getRank(timePoint, cache);
            }
            result.started = trackedLeg.hasStartedLeg(timePoint);
            Speed velocityMadeGood;
            if (hasFinishedLeg) {
                velocityMadeGood = trackedLeg.getAverageVelocityMadeGood(timePoint);
            } else {
                velocityMadeGood = trackedLeg.getVelocityMadeGood(timePoint, WindPositionMode.EXACT, cache);
            }
            result.velocityMadeGoodInKnots = velocityMadeGood == null ? null : velocityMadeGood.getKnots();
            Distance windwardDistanceToGo = trackedLeg.getWindwardDistanceToGo(timePoint, WindPositionMode.LEG_MIDDLE);
            result.windwardDistanceToGoInMeters = windwardDistanceToGo == null ? null : windwardDistanceToGo
                    .getMeters();
            final TimePoint startOfRace = trackedLeg.getTrackedLeg().getTrackedRace().getStartOfRace();
            if (startOfRace != null && trackedLeg.hasStartedLeg(timePoint)) {
                // not using trackedLeg.getManeuvers(...) because it may not catch the mark passing maneuver starting this leg
                // because that may have been detected as slightly before the mark passing time, hence associated with the previous leg
                Iterable<Maneuver> maneuvers = trackedLeg.getTrackedLeg().getTrackedRace()
                        .getManeuvers(trackedLeg.getCompetitor(), startOfRace, timePoint, waitForLatestAnalyses);
                if (maneuvers != null) {
                    result.numberOfManeuvers = new HashMap<ManeuverType, Integer>();
                    result.numberOfManeuvers.put(ManeuverType.TACK, 0);
                    result.numberOfManeuvers.put(ManeuverType.JIBE, 0);
                    result.numberOfManeuvers.put(ManeuverType.PENALTY_CIRCLE, 0);
                    Map<ManeuverType, Double> totalManeuverLossInMeters = new HashMap<ManeuverType, Double>();
                    totalManeuverLossInMeters.put(ManeuverType.TACK, 0.0);
                    totalManeuverLossInMeters.put(ManeuverType.JIBE, 0.0);
                    totalManeuverLossInMeters.put(ManeuverType.PENALTY_CIRCLE, 0.0);
                    TimePoint startOfLeg = trackedLeg.getStartTime();
                    for (Maneuver maneuver : maneuvers) {
                        // don't count maneuvers that were in previous legs
                        switch (maneuver.getType()) {
                        case TACK:
                        case JIBE:
                        case PENALTY_CIRCLE:
                            if (!maneuver.getTimePoint().before(startOfLeg) && (legFinishTime == null || legFinishTime.after(timePoint) ||
                                    maneuver.getTimePoint().before(legFinishTime))) {
                                if (maneuver.getManeuverLoss() != null) {
                                    result.numberOfManeuvers.put(maneuver.getType(),
                                            result.numberOfManeuvers.get(maneuver.getType()) + 1);
                                    totalManeuverLossInMeters.put(maneuver.getType(),
                                            totalManeuverLossInMeters.get(maneuver.getType())
                                            + maneuver.getManeuverLoss().getProjectedDistanceLost().getMeters());
                                }
                            }
                            break;
                        default:
                            /* Do nothing here.
                             * Throwing an exception destroys the toggling (and maybe other behaviour) of the leaderboard.
                             */
                        }
                        if(maneuver.isMarkPassing()) {
                            // analyze all mark passings, not only those after this leg's start, to catch the mark passing
                            // maneuver starting this leg, even if its time point is slightly before the mark passing starting this leg
                            if (maneuver.getMarkPassing().getWaypoint() == trackedLeg.getLeg().getFrom()) {
                                result.sideToWhichMarkAtLegStartWasRounded = maneuver.getToSide();
                            }
                        }
                    }
                    result.averageManeuverLossInMeters = new HashMap<ManeuverType, Double>();
                    for (ManeuverType maneuverType : new ManeuverType[] { ManeuverType.TACK, ManeuverType.JIBE,
                            ManeuverType.PENALTY_CIRCLE }) {
                        if (result.numberOfManeuvers.get(maneuverType) != 0) {
                            result.averageManeuverLossInMeters.put(
                                    maneuverType,
                                    totalManeuverLossInMeters.get(maneuverType)
                                    / result.numberOfManeuvers.get(maneuverType));
                        }
                    }
                }
            }
            BiFunction<BiFunction<TrackedLegOfCompetitor, TimePoint, Double>, BiFunction<TrackedLegOfCompetitor, TimePoint, Double>, Double> extractDoubleValue = (
                    currentValueExtractor, averageValueExtractor) -> (hasFinishedLeg
                            ? averageValueExtractor.apply(trackedLeg, timePoint) : currentValueExtractor.apply(trackedLeg, timePoint));
            result.setExpeditionAWA(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionAWA, TrackedLegOfCompetitor::getAverageExpeditionAWA));
            result.setExpeditionAWS(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionAWS, TrackedLegOfCompetitor::getAverageExpeditionAWS));
            result.setExpeditionTWA(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTWA, TrackedLegOfCompetitor::getAverageExpeditionTWA));
            result.setExpeditionTWS(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTWS, TrackedLegOfCompetitor::getAverageExpeditionTWS));
            result.setExpeditionTWD(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTWD, TrackedLegOfCompetitor::getAverageExpeditionTWD));
            result.setExpeditionTargTWA(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTargTWA, TrackedLegOfCompetitor::getAverageExpeditionTargTWA));
            result.setExpeditionBoatSpeed(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionBoatSpeed, TrackedLegOfCompetitor::getAverageExpeditionBoatSpeed));
            result.setExpeditionTargBoatSpeed(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTargBoatSpeed, TrackedLegOfCompetitor::getAverageExpeditionTargBoatSpeed));
            result.setExpeditionSOG(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionSOG, TrackedLegOfCompetitor::getAverageExpeditionSOG));
            result.setExpeditionCOG(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionCOG, TrackedLegOfCompetitor::getAverageExpeditionCOG));
            result.setExpeditionForestayLoad(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionForestayLoad, TrackedLegOfCompetitor::getAverageExpeditionForestayLoad));
            result.setExpeditionRake(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionRake, TrackedLegOfCompetitor::getAverageExpeditionRake));
            result.setExpeditionCourseDetail(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionCourseDetail, TrackedLegOfCompetitor::getAverageExpeditionCourseDetail));
            result.setExpeditionHeading(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionHeading, TrackedLegOfCompetitor::getAverageExpeditionHeading));
            result.setExpeditionVMG(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionVMG, TrackedLegOfCompetitor::getAverageExpeditionVMG));
            result.setExpeditionVMGTargVMGDelta(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionVMGTargVMGDelta, TrackedLegOfCompetitor::getAverageExpeditionVMGTargVMGDelta));
            result.setExpeditionRateOfTurn(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionRateOfTurn, TrackedLegOfCompetitor::getAverageExpeditionRateOfTurn));
            result.setExpeditionRudderAngle(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionRudderAngle, TrackedLegOfCompetitor::getAverageExpeditionRudderAngle));
            result.setExpeditionTargetHeel(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTargetHeel, TrackedLegOfCompetitor::getAverageExpeditionTargetHeel));
            result.setExpeditionTimeToPortLayline(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTimeToPortLayline, TrackedLegOfCompetitor::getAverageExpeditionTimeToPortLayline));
            result.setExpeditionTimeToStbLayline(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTimeToStbLayline, TrackedLegOfCompetitor::getAverageExpeditionTimeToStbLayline));
            result.setExpeditionDistToPortLayline(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionDistToPortLayline, TrackedLegOfCompetitor::getAverageExpeditionDistToPortLayline));
            result.setExpeditionDistToStbLayline(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionDistToStbLayline, TrackedLegOfCompetitor::getAverageExpeditionDistToStbLayline));
            result.setExpeditionTimeToGunInSeconds(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTimeToGunInSeconds, TrackedLegOfCompetitor::getAverageExpeditionTimeToGunInSeconds));
            result.setExpeditionTimeToCommitteeBoat(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTimeToCommitteeBoat, TrackedLegOfCompetitor::getAverageExpeditionTimeToCommitteeBoat));
            result.setExpeditionTimeToPin(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTimeToPin, TrackedLegOfCompetitor::getAverageExpeditionTimeToPin));
            result.setExpeditionTimeToBurnToLineInSeconds(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTimeToBurnToLineInSeconds, TrackedLegOfCompetitor::getAverageExpeditionTimeToBurnToLineInSeconds));
            result.setExpeditionTimeToBurnToCommitteeBoat(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTimeToBurnToCommitteeBoat, TrackedLegOfCompetitor::getAverageExpeditionTimeToBurnToCommitteeBoat));
            result.setExpeditionTimeToBurnToPin(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionTimeToBurnToPin, TrackedLegOfCompetitor::getAverageExpeditionTimeToBurnToPin));
            result.setExpeditionDistanceToCommitteeBoat(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionDistanceToCommitteeBoat, TrackedLegOfCompetitor::getAverageExpeditionDistanceToCommitteeBoat));
            result.setExpeditionDistanceToPinDetail(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionDistanceToPinDetail, TrackedLegOfCompetitor::getAverageExpeditionDistanceToPinDetail));
            result.setExpeditionDistanceBelowLineInMeters(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionDistanceBelowLineInMeters, TrackedLegOfCompetitor::getAverageExpeditionDistanceBelowLineInMeters));
            result.setExpeditionLineSquareForWindDirection(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionLineSquareForWindDirection, TrackedLegOfCompetitor::getAverageExpeditionLineSquareForWindDirection));
            result.setExpeditionBaroIfAvailable(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionBaroIfAvailable, TrackedLegOfCompetitor::getAverageExpeditionBaroIfAvailable));
            result.setExpeditionLoadSIfAvailable(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionLoadSIfAvailable, TrackedLegOfCompetitor::getAverageExpeditionLoadSIfAvailable));
            result.setExpeditionLoadPIfAvailable(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionLoadPIfAvailable, TrackedLegOfCompetitor::getAverageExpeditionLoadPIfAvailable));
            result.setExpeditionJibCarPortIfAvailable(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionJibCarPortIfAvailable, TrackedLegOfCompetitor::getAverageExpeditionJibCarPortIfAvailable));
            result.setExpeditionJibCarStbdIfAvailable(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionJibCarStbdIfAvailable, TrackedLegOfCompetitor::getAverageExpeditionJibCarStbdIfAvailable));
            result.setExpeditionMastButtIfAvailable(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionMastButtIfAvailable, TrackedLegOfCompetitor::getAverageExpeditionMastButtIfAvailable));
            result.setExpeditionKickerTensionIfAvailable(extractDoubleValue.apply(TrackedLegOfCompetitor::getExpeditionKickerTensionIfAvailable, TrackedLegOfCompetitor::getAverageExpeditionKickerTensionIfAvailable));
        }
        return result;
    }

    private Duration getGapAtEndOfPreviousLeg(TrackedLegOfCompetitor trackedLeg, final RankingInfo rankingInfo, WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) {
        final Duration result;
        final Course course = trackedLeg.getTrackedLeg().getTrackedRace().getRace().getCourse();
        // if trackedLeg is the first leg, compute the gap at the start of this leg; otherwise, compute gap
        // at the end of the previous leg
        final TimePoint timePoint = trackedLeg.getStartTime();
        if (course.getFirstWaypoint() == trackedLeg.getLeg().getFrom()) {
            result = Duration.NULL;
        } else {
            final TrackedLegOfCompetitor tloc = trackedLeg.getTrackedLeg().getTrackedRace()
                    .getTrackedLegFinishingAt(trackedLeg.getLeg().getFrom()).getTrackedLeg(trackedLeg.getCompetitor());
            result = trackedLeg.getTrackedLeg().getTrackedRace().getRankingMetric().getLegGapToLegLeaderInOwnTime(tloc,
                    timePoint, rankingInfo, cache);
        }
        return result;
    }

    @Override
    public int getTotalRankOfCompetitor(Competitor competitor, TimePoint timePoint) {
        return Util.indexOf(getCompetitorsFromBestToWorst(timePoint), competitor) + 1;
    }

    protected void regattaLogEventAdded(RegattaLogEvent event) {
        invalidateCacheIfEventSaysSo(event);
    }
    
    protected void raceLogEventAdded(RaceColumn raceColumn, RaceLogIdentifier raceLogIdentifier, RaceLogEvent event) {
        invalidateCacheIfEventSaysSo(event);
    }

    private void invalidateCacheIfEventSaysSo(AbstractLogEvent<?> event) {
        if (event instanceof InvalidatesLeaderboardCache) {
            // make sure to invalidate the cache as this event indicates that
            // it changes values the cache could still hold
            if (leaderboardDTOCache != null) {
                leaderboardDTOCache.invalidate(this);
            }
            synchronized (raceDetailsAtEndOfTrackingCache) {
                raceDetailsAtEndOfTrackingCache.clear();
            }
        }
    }
    
    /**
     * Determines the time point of the last raw fix (with outliers not removed) for <code>competitor</code> in
     * <code>trackedRace</code>. If the competitor's track is <code>null</code> or empty, <code>null</code> is returned.
     * @param trackedRace must not be <code>null</code>
     * @param atOrBefore find the last fix at or before the time point specified
     */
    private Date getTimePointOfLastFixAtOrBefore(Competitor competitor, TrackedRace trackedRace, TimePoint atOrBefore) {
        assert trackedRace != null;
        final Date timePointOfLastPositionFix;
        GPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
        if (track == null) {
            timePointOfLastPositionFix = null;
        } else {
            GPSFixMoving lastFix = track.getLastFixAtOrBefore(atOrBefore);
            if (lastFix == null) {
                timePointOfLastPositionFix = null;
            } else {
                timePointOfLastPositionFix = lastFix.getTimePoint().asDate();
            }
        }
        return timePointOfLastPositionFix;
    }

    @Override
    public Duration getTotalTimeSailedInLegType(Competitor competitor, LegType legType, TimePoint timePoint) throws NoWindException {
        return getTotalTimeSailedInLegType(competitor, legType, timePoint, new HashMap<TrackedLeg, LegType>());
    }
    
    private Duration getTotalTimeSailedInLegType(Competitor competitor, LegType legType, TimePoint timePoint, Map<TrackedLeg, LegType> legTypeCache) throws NoWindException {
        Duration result = null;
        // TODO should we ensure that competitor participated in all race columns?
        outerLoop:
        for (TrackedRace trackedRace : getTrackedRaces()) {
            if (Util.contains(trackedRace.getRace().getCompetitors(), competitor)) {
                trackedRace.getRace().getCourse().lockForRead();
                try {
                    for (Leg leg : trackedRace.getRace().getCourse().getLegs()) {
                        TrackedLegOfCompetitor trackedLegOfCompetitor = trackedRace.getTrackedLeg(competitor, leg);
                        if (trackedLegOfCompetitor.hasStartedLeg(timePoint)) {
                            // find out leg type at the time the competitor started the leg
                            try {
                                final TrackedLeg trackedLeg = trackedRace.getTrackedLeg(leg);
                                LegType trackedLegType = legTypeCache.get(trackedLeg);
                                if (trackedLegType == null) {
                                    final TimePoint startTime = trackedLegOfCompetitor.getStartTime();
                                    TimePoint finishTime = trackedLegOfCompetitor.getFinishTime();
                                    if (finishTime == null) {
                                        finishTime = timePoint;
                                    }
                                    trackedLegType = trackedLeg.getLegType(startTime.plus(startTime.until(finishTime).divide(2))); // middle of the leg
                                    legTypeCache.put(trackedLeg, trackedLegType);
                                }
                                if (legType == trackedLegType) {
                                    Duration timeSpentInLegOfType = trackedLegOfCompetitor.getTime(timePoint);
                                    if (timeSpentInLegOfType != null) {
                                        if (result == null) {
                                            result = timeSpentInLegOfType;
                                        } else {
                                            result = result.plus(timeSpentInLegOfType);
                                        }
                                    } else {
                                        // Although the competitor has started the leg, no value was produced. This
                                        // means that the competitor didn't finish the leg before tracking ended. No useful value
                                        // can be obtained for this competitor anymore.
                                        result = null;
                                        break outerLoop;
                                    }
                                }
                            } catch (NoWindException nwe) {
                                // without wind there is no leg type and hence there is no reasonable value for this:
                                result = null;
                                break outerLoop;
                            }
                        }
                    }
                } finally {
                    trackedRace.getRace().getCourse().unlockAfterRead();
                }
            }
        }
        return result;
    }

    @Override
    public Duration getTotalTimeSailed(Competitor competitor, TimePoint timePoint) {
        Duration result = null;
        for (TrackedRace trackedRace : getTrackedRaces()) {
            if (Util.contains(trackedRace.getRace().getCompetitors(), competitor)) {
                final Duration timeSpent = trackedRace.getTimeSailedSinceRaceStart(competitor, timePoint);
                if (timeSpent != null) {
                    if (result == null) {
                        result = timeSpent;
                    } else {
                        result=result.plus(timeSpent);
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Distance getTotalDistanceTraveled(Competitor competitor, TimePoint timePoint) {
        return getTotals(competitor, timePoint, Distance::add, TrackedRace::getDistanceTraveled);
    }
    
    @Override
    public Distance getTotalDistanceFoiled(Competitor competitor, TimePoint timePoint) {
        return getTotals(competitor, timePoint, Distance::add, TrackedRace::getDistanceFoiled);
    }
    
    @Override
    public Duration getTotalDurationFoiled(Competitor competitor, TimePoint timePoint) {
        return getTotals(competitor, timePoint, Duration::plus, TrackedRace::getDurationFoiled);
    }
    
    @FunctionalInterface
    private static interface ValueFromRaceGetter<T> {
        T get(TrackedRace trackedRace, Competitor competitor, TimePoint timePoint);
    }
    
    private <T> T getTotals(Competitor competitor, TimePoint timePoint, BiFunction<T, T, T> adder, ValueFromRaceGetter<T> valueGetter) {
        T result = null;
        for (TrackedRace trackedRace : getTrackedRaces()) {
            TimePoint startOfRace;
            if (Util.contains(trackedRace.getRace().getCompetitors(), competitor) &&
                    (startOfRace=trackedRace.getStartOfRace()) != null &&
                    !startOfRace.after(timePoint)) {
                T distanceSailedInRace = valueGetter.get(trackedRace, competitor, timePoint);
                if (distanceSailedInRace != null) {
                    if (result == null) {
                        result = distanceSailedInRace;
                    } else {
                        result = adder.apply(result, distanceSailedInRace);
                    }
                } else {
                    // if competitor has not finished one single race in the whole
                    // series then we can not return a meaningful value for all
                    // all races
                    return null;
                }
            }
        }
        return result;
    }

    @Override
    public Iterable<Competitor> getAllCompetitors() {
        return getAllCompetitorsWithRaceDefinitionsConsidered().getB();
    }

    @Override
    public void destroy() {
        for (CacheInvalidationListener cacheInvalidationListener : cacheInvalidationListeners) {
            cacheInvalidationListener.removeFromTrackedRace();
        }
    }

    @Override
    public boolean hasScores(Competitor competitor, TimePoint timePoint) {
        for (final RaceColumn raceColumn : getRaceColumns()) {
            if (getTotalPoints(competitor, raceColumn, timePoint) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public ScoreCorrectionMapping mapRegattaScoreCorrections(RegattaScoreCorrections regattaScoreCorrections,
            Map<String, RaceColumn> raceNumberOrNameToRaceColumnMap, Map<String, Competitor> sailIdToCompetitorMap,
            boolean allowRaceDefaultsByOrder, boolean allowPartialImport) {
        final SailNumberCanonicalizerAndMatcher sailNumberCanonicalizer = new SailNumberCanonicalizerAndMatcher();
        final Map<String, Competitor> competitorsByTheirCanonicalizedSailNumber = sailNumberCanonicalizer.canonicalizeLeaderboardSailIDs(getAllCompetitors());
        final Map<String, RaceColumn> raceMappings = new HashMap<>(raceNumberOrNameToRaceColumnMap);
        final Map<String, Competitor> competitorMappings = new HashMap<>(sailIdToCompetitorMap);
        final Iterator<RaceColumn> raceColumnIterator = getRaceColumns().iterator();
        for (final ScoreCorrectionsForRace raceCorrection : regattaScoreCorrections.getScoreCorrectionsForRaces()) {
            final RaceColumn currentRaceColumn = raceColumnIterator.hasNext() ? raceColumnIterator.next() : null;
            raceMappings.putIfAbsent(raceCorrection.getRaceNameOrNumber(), allowRaceDefaultsByOrder ? currentRaceColumn : null);
            for (final String sailIdOrShortName : raceCorrection.getSailIDs()) {
                competitorMappings.putIfAbsent(sailIdOrShortName, competitorsByTheirCanonicalizedSailNumber.get(sailNumberCanonicalizer.canonicalizeSailID(sailIdOrShortName,
                        /* default nationality IOC code */ null)));
            }
        }
        return new ScoreCorrectionMappingImpl(raceMappings, competitorMappings, regattaScoreCorrections);
    }
}
