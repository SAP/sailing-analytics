package com.sap.sailing.domain.leaderboard.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindError;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.LeaderboardEntryDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.domain.common.dto.LegEntryDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.dto.RaceDTO;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.NumberOfCompetitorsInLeaderboardFetcher;
import com.sap.sailing.domain.leaderboard.ResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.ScoreCorrection.Result;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.leaderboard.caching.LeaderboardDTOCache;
import com.sap.sailing.domain.leaderboard.caching.LiveLeaderboardUpdater;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.MarkPassingManeuver;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedLegOfCompetitor;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.util.impl.LockUtil;
import com.sap.sailing.util.impl.NamedReentrantReadWriteLock;
import com.sap.sailing.util.impl.RaceColumnListeners;
import com.sap.sailing.util.impl.ThreadFactoryWithPriority;

/**
 * Base implementation for various types of leaderboards. The {@link RaceColumnListener} implementation forwards events
 * received to all {@link RaceColumnListener} subscribed with this leaderboard. To which objects this leaderboard subscribes
 * as {@link RaceColumnListener} is left to the concrete subclasses to implement, but the race columns seem like useful
 * candidates.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class AbstractSimpleLeaderboardImpl implements Leaderboard, RaceColumnListener {
    private static final long serialVersionUID = 330156778603279333L;
    
    private static final Logger logger = Logger.getLogger(AbstractSimpleLeaderboardImpl.class.getName());

    static final Double DOUBLE_0 = new Double(0);

    private final SettableScoreCorrection scoreCorrection;

    private ThresholdBasedResultDiscardingRule crossLeaderboardResultDiscardingRule;

    /**
     * The optional display name mappings for competitors. This allows a user to override the tracking-provided
     * competitor names for display in a leaderboard.
     */
    private final Map<Competitor, String> displayNames;

    /** the display name of the leaderboard */
    private String displayName;

    /**
     * Backs the {@link #getCarriedPoints(Competitor)} API with data. Can be used to prime this leaderboard
     * with aggregated results of races not tracked / displayed by this leaderboard in detail. The points
     * provided by this map are considered by {@link #getTotalPoints(Competitor, TimePoint)}.
     */
    private final Map<Competitor, Double> carriedPoints;

    private final RaceColumnListeners raceColumnListeners;
    
    /**
     * A set that manages the difference between {@link #getCompetitors()} and {@link #getAllCompetitors()}. Access
     * is controlled by the {@link #suppressedCompetitorsLock} lock.
     */
    private final Set<Competitor> suppressedCompetitors;
    private final NamedReentrantReadWriteLock suppressedCompetitorsLock;

    private final Map<Pair<TrackedRace, Competitor>, RunnableFuture<RaceDetails>> raceDetailsAtEndOfTrackingCache;

    /**
     * This executor needs to be a different one than {@link #executor} because the tasks run by {@link #executor}
     * can depend on the results of the tasks run by {@link #raceDetailsExecutor}, and an {@link Executor} doesn't
     * move a task that is blocked by waiting for another {@link FutureTask} to the side but blocks permanently,
     * ending in a deadlock (one that cannot easily be detected by the Eclipse debugger either).
     */
    private transient Executor raceDetailsExecutor;

    /**
     * Used to remove all these listeners from their tracked races when this servlet is {@link #destroy() destroyed}.
     */
    private transient Set<CacheInvalidationListener> cacheInvalidationListeners;

    private transient ThreadPoolExecutor executor;

    private transient LiveLeaderboardUpdater liveLeaderboardUpdater;

    private transient LeaderboardDTOCache leaderboardDTOCache;
    
    /**
     * A leaderboard entry representing a snapshot of a cell at a given time point for a single race/competitor.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    class EntryImpl implements Entry {
        private final Callable<Integer> trackedRankProvider;
        private final Double netPoints;
        private final boolean isNetPointsCorrected;
        private final Double totalPoints;
        private final MaxPointsReason maxPointsReason;
        private final boolean discarded;
        private final Fleet fleet;

        private EntryImpl(Callable<Integer> trackedRankProvider, Double netPoints, boolean isNetPointsCorrected, Double totalPoints,
                MaxPointsReason maxPointsReason, boolean discarded, Fleet fleet) {
            super();
            this.trackedRankProvider = trackedRankProvider;
            this.netPoints = netPoints;
            this.isNetPointsCorrected = isNetPointsCorrected;
            this.totalPoints = totalPoints;
            this.maxPointsReason = maxPointsReason;
            this.discarded = discarded;
            this.fleet = fleet;
        }
        @Override
        public int getTrackedRank() {
            try {
                return trackedRankProvider.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        @Override
        public Double getNetPoints() {
            return netPoints;
        }
        @Override
        public boolean isNetPointsCorrected() {
            return isNetPointsCorrected;
        }
        @Override
        public Double getTotalPoints() {
            return totalPoints;
        }
        @Override
        public MaxPointsReason getMaxPointsReason() {
            return maxPointsReason;
        }
        @Override
        public boolean isDiscarded() {
            return discarded;
        }
        @Override
        public Fleet getFleet() {
            return fleet;
        }
    }
    
    /**
     * Computing the competitors can be a bit expensive, particularly if the fleet is large and there may be suppressed
     * competitors, and the leaderboard may be a meta-leaderboard that refers to other leaderboards which each have
     * several tracked races attached from where the competitors need to be retrieved. Ideally, the competitors list
     * would be cached, but that is again difficult because we would have to monitor all changes in all dependent
     * leaderboards and columns and tracked races properly.
     * <p>
     * 
     * As it turns out, one of the most frequent uses of the {@link AbstractSimpleLeaderboardImpl#getCompetitors}
     * competitors list is to determine their number which in turn is only required for high-point scoring systems and
     * for computing the default score for penalties. Again, the most frequently used low-point family of scoring schemes
     * does not require this number. Yet, the scoring scheme requires an argument for polymorphic use by those that
     * need it. Instead of computing it for each call, this interface lets us defer the actual calculation until the
     * point when it's really needed. Once asked, this object will cache the result. Therefore, a new one should be
     * constructed each time the number shall be computed.
     * 
     * @author Axel Uhl (D043530)
     * 
     */
    public class NumberOfCompetitorsFetcherImpl implements NumberOfCompetitorsInLeaderboardFetcher {
        private int numberOfCompetitors = -1;
        
        @Override
        public int getNumberOfCompetitorsInLeaderboard() {
            if (numberOfCompetitors == -1) {
                numberOfCompetitors = Util.size(getCompetitors());
            }
            return numberOfCompetitors;
        }
    }

    /**
     * Handles the invalidation of the {@link SailingServiceImpl#raceDetailsAtEndOfTrackingCache} entries if the tracked race
     * changes in any way.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    private class CacheInvalidationListener implements RaceChangeListener {
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
                raceDetailsAtEndOfTrackingCache.remove(new Pair<TrackedRace, Competitor>(trackedRace, competitor));
                removeFromTrackedRace();
            }
        }

        @Override
        public void competitorPositionChanged(GPSFixMoving fix, Competitor competitor) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void statusChanged(TrackedRaceStatus newStatus) {
            // when the status changes away from LOADING, calculations may start or resume, making it necessary to clear the cache
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void markPositionChanged(GPSFix fix, Mark mark) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void markPassingReceived(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings,
                Iterable<MarkPassing> markPassings) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void windDataReceived(Wind wind, WindSource windSource) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void windDataRemoved(Wind wind, WindSource windSource) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void raceTimesChanged(TimePoint startOfTracking, TimePoint endOfTracking, TimePoint startTimeReceived) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void delayToLiveChanged(long delayToLiveInMillis) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }

        @Override
        public void windSourcesToExcludeChanged(Iterable<? extends WindSource> windSourcesToExclude) {
            invalidateCacheAndRemoveThisListenerFromTrackedRace();
        }
    }
    
    private static class UUIDGenerator implements LeaderboardDTO.UUIDGenerator {
        @Override
        public String generateRandomUUID() {
            return UUID.randomUUID().toString();
        }
    }

    public AbstractSimpleLeaderboardImpl(ThresholdBasedResultDiscardingRule resultDiscardingRule) {
        this.carriedPoints = new HashMap<Competitor, Double>();
        this.scoreCorrection = createScoreCorrection();
        this.displayNames = new HashMap<Competitor, String>();
        this.crossLeaderboardResultDiscardingRule = resultDiscardingRule;
        this.suppressedCompetitors = new HashSet<Competitor>();
        this.suppressedCompetitorsLock = new NamedReentrantReadWriteLock("suppressedCompetitorsLock", /* fair */ false);
        this.raceColumnListeners = new RaceColumnListeners();
        this.raceDetailsAtEndOfTrackingCache = new HashMap<Pair<TrackedRace, Competitor>, RunnableFuture<RaceDetails>>();
        initTransientFields();
    }
    
    /**
     * Produces the score correction object to use in this leaderboard. Used by the constructor. Subclasses may override
     * this method to create a more specific type of score correction. This implementation produces an object of type
     * {@link ScoreCorrectionImpl}.
     */
    protected SettableScoreCorrection createScoreCorrection() {
        return new ScoreCorrectionImpl(this);
    }
    
    private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
        ois.defaultReadObject();
        initTransientFields();
    }

    private void initTransientFields() {
        this.raceDetailsExecutor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        this.cacheInvalidationListeners = new HashSet<CacheInvalidationListener>();
        // When many updates are triggered in a short period of time by a single thread, ensure that the single thread
        // providing the updates is not outperformed by all the re-calculations happening here. Leave at least one
        // core to other things, but by using at least three threads ensure that no simplistic deadlocks may occur.
        final int THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 3);
        executor = new ThreadPoolExecutor(/* corePoolSize */ THREAD_POOL_SIZE,
                /* maximumPoolSize */ THREAD_POOL_SIZE,
                /* keepAliveTime */ 60, TimeUnit.SECONDS,
                /* workQueue */ new LinkedBlockingQueue<Runnable>(), new ThreadFactoryWithPriority(Thread.NORM_PRIORITY-1));
    }

    @Override
    public void destroy() {
        for (CacheInvalidationListener cacheInvalidationListener : cacheInvalidationListeners) {
            cacheInvalidationListener.removeFromTrackedRace();
        }
    }

    @Override
    public SettableScoreCorrection getScoreCorrection() {
        return scoreCorrection;
    }

    @Override
    public String getDisplayName(Competitor competitor) {
        return displayNames.get(competitor);
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    @Override
    public ResultDiscardingRule getResultDiscardingRule() {
        return crossLeaderboardResultDiscardingRule;
    }

    @Override
    public void setCarriedPoints(Competitor competitor, double carriedPoints) {
        Double oldCarriedPoints = this.carriedPoints.put(competitor, carriedPoints);
        getScoreCorrection().notifyListenersAboutCarriedPointsChange(competitor, oldCarriedPoints, carriedPoints);
    }

    @Override
    public double getCarriedPoints(Competitor competitor) {
        Double result = carriedPoints.get(competitor);
        return result == null ? 0 : result;
    }

    @Override
    public void unsetCarriedPoints(Competitor competitor) {
        Double oldCarriedPoints = carriedPoints.remove(competitor);
        getScoreCorrection().notifyListenersAboutCarriedPointsChange(competitor, oldCarriedPoints, null);
    }

    @Override
    public boolean hasCarriedPoints() {
        return !carriedPoints.isEmpty();
    }

    @Override
    public boolean hasCarriedPoints(Competitor competitor) {
        return carriedPoints.containsKey(competitor);
    }

    @Override
    public void setDisplayName(Competitor competitor, String displayName) {
        String oldDisplayName = displayNames.get(competitor);
        displayNames.put(competitor, displayName);
        getRaceColumnListeners().notifyListenersAboutCompetitorDisplayNameChanged(competitor, oldDisplayName, displayName);
    }

    @Override
    public void setCrossLeaderboardResultDiscardingRule(ThresholdBasedResultDiscardingRule discardingRule) {
        ResultDiscardingRule oldDiscardingRule = getResultDiscardingRule();
        this.crossLeaderboardResultDiscardingRule = discardingRule;
        getRaceColumnListeners().notifyListenersAboutResultDiscardingRuleChanged(oldDiscardingRule, discardingRule);
    }

    @Override
    public Double getNetPoints(final Competitor competitor, final RaceColumn raceColumn, final TimePoint timePoint) throws NoWindException {
        return getScoreCorrection().getCorrectedScore(
                new Callable<Integer>() {
                    public Integer call() throws NoWindException {
                        return getTrackedRank(competitor, raceColumn, timePoint);
                    }
                }, competitor,
                raceColumn, timePoint, new NumberOfCompetitorsFetcherImpl(), getScoringScheme()).getCorrectedScore();
    }

    @Override
    public MaxPointsReason getMaxPointsReason(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        return getScoreCorrection().getMaxPointsReason(competitor, raceColumn, timePoint);
    }

    @Override
    public boolean isDiscarded(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        return isDiscarded(competitor, raceColumn, getRaceColumns(), timePoint);
    }
    
    private boolean isDiscarded(Competitor competitor, RaceColumn raceColumn, Iterable<RaceColumn> raceColumnsToConsider, TimePoint timePoint) {
        return !raceColumn.isMedalRace() && getMaxPointsReason(competitor, raceColumn, timePoint).isDiscardable()
                && getResultDiscardingRule().getDiscardedRaceColumns(competitor, this, raceColumnsToConsider, timePoint).contains(
                        raceColumn);
    }

    @Override
    public Double getTotalPoints(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) throws NoWindException {
        return getTotalPoints(competitor, raceColumn, getRaceColumns(), timePoint);
    }
    
    @Override
    public Double getTotalPoints(Competitor competitor, RaceColumn raceColumn,
            Iterable<RaceColumn> raceColumnsToConsider, TimePoint timePoint) throws NoWindException {
        Double result;
        if (isDiscarded(competitor, raceColumn, raceColumnsToConsider, timePoint)) {
            result = 0.0;
        } else {
            final Double netPoints = getNetPoints(competitor, raceColumn, timePoint);
            if (netPoints == null) {
                result = null;
            } else {
                result = raceColumn.getFactor() * netPoints;
            }
        }
        return result;
    }

    @Override
    public Double getTotalPoints(Competitor competitor, TimePoint timePoint) throws NoWindException {
        // when a column with isStartsWithZeroScore() is found, only reset score if the competitor scored in any race from there on
        boolean needToResetScoreUponNextNonEmptyEntry = false;
        double result = getCarriedPoints(competitor);
        for (RaceColumn r : getRaceColumns()) {
            if (r.isStartsWithZeroScore()) {
                needToResetScoreUponNextNonEmptyEntry = true;
            }
            if (getScoringScheme().isValidInTotalScore(this, r, timePoint)) {
                final Double totalPoints = getTotalPoints(competitor, r, timePoint);
                if (totalPoints != null) {
                    if (needToResetScoreUponNextNonEmptyEntry) {
                        result = 0;
                        needToResetScoreUponNextNonEmptyEntry = false;
                    }
                    result += totalPoints;
                }
            }
        }
        return result;
    }

    /**
     * All competitors with non-<code>null</code> net points are added to the result which is then sorted by net points in ascending
     * order. The fleet, if ordered, is the primary ordering criterion, followed by the net points.
     */
    @Override
    public List<Competitor> getCompetitorsFromBestToWorst(final RaceColumn raceColumn, TimePoint timePoint) throws NoWindException {
        final Map<Competitor, Pair<Double, Fleet>> netPointsAndFleet = new HashMap<Competitor, Pair<Double, Fleet>>();
        for (Competitor competitor : getCompetitors()) {
            Double netPoints = getNetPoints(competitor, raceColumn, timePoint);
            if (netPoints != null) {
                netPointsAndFleet.put(competitor, new Pair<Double, Fleet>(netPoints, raceColumn.getFleetOfCompetitor(competitor)));
            }
        }
        List<Competitor> result = new ArrayList<Competitor>(netPointsAndFleet.keySet());
        Collections.sort(result, new Comparator<Competitor>() {
            @Override
            public int compare(Competitor o1, Competitor o2) {
                int comparisonResult;
                if (o1 == o2) {
                    comparisonResult = 0;
                } else {
                    final Fleet o1Fleet = netPointsAndFleet.get(o1).getB();
                    final Fleet o2Fleet = netPointsAndFleet.get(o2).getB();
                    if (o1Fleet == null) {
                        if (o2Fleet == null) {
                            comparisonResult = 0;
                        } else {
                            comparisonResult = 1; // o1 ranks "worse" because it doesn't have a fleet set while o2 has
                        }
                    } else {
                        if (o2Fleet == null) {
                            comparisonResult = -1; // o1 ranks "better" because it has a fleet set while o2 hasn't
                        } else {
                            comparisonResult = o1Fleet.compareTo(o2Fleet);
                        }
                    }
                    if (comparisonResult == 0) {
                        comparisonResult = getScoringScheme().getScoreComparator(/* nullScoresAreBetter */ false).compare(
                                netPointsAndFleet.get(o1).getA(), netPointsAndFleet.get(o2).getA());
                    }
                }
                return comparisonResult;
            }
        });
        return result;
    }

    @Override
    public List<Competitor> getCompetitorsFromBestToWorst(TimePoint timePoint) throws NoWindException {
        return getCompetitorsFromBestToWorst(getRaceColumns(), timePoint);
    }
    
    private List<Competitor> getCompetitorsFromBestToWorst(Iterable<RaceColumn> raceColumnsToConsider, TimePoint timePoint) throws NoWindException {
        List<Competitor> result = new ArrayList<Competitor>();
        for (Competitor competitor : getCompetitors()) {
            result.add(competitor);
        }
        Collections.sort(result, getTotalRankComparator(raceColumnsToConsider, timePoint));
        return result;
    }
    
    @Override
    public int getTotalRankOfCompetitor(Competitor competitor, TimePoint timePoint) throws NoWindException {
        List<Competitor> competitorsFromBestToWorst = getCompetitorsFromBestToWorst(timePoint);
        return competitorsFromBestToWorst.indexOf(competitor) + 1;
    }

    protected Comparator<? super Competitor> getTotalRankComparator(Iterable<RaceColumn> raceColumnsToConsider, TimePoint timePoint) throws NoWindException {
        return new LeaderboardTotalRankComparator(this, timePoint, getScoringScheme(), /* nullScoresAreBetter */ false, raceColumnsToConsider);
    }

    @Override
    public RaceColumn getRaceColumnByName(String columnName) {
        RaceColumn result = null;
        for (RaceColumn r : getRaceColumns()) {
            if (r.getName().equals(columnName)) {
                result = r;
                break;
            }
        }
        return result;
    }

    @Override
    public Competitor getCompetitorByName(String competitorName) {
        for (Competitor competitor : getAllCompetitors()) {
            if (competitor.getName().equals(competitorName)) {
                return competitor;
            }
        }
        return null;
    }

    @Override
    public boolean countRaceForComparisonWithDiscardingThresholds(Competitor competitor, RaceColumn raceColumn, TimePoint timePoint) {
        TrackedRace trackedRaceForCompetitorInColumn;
        return getScoringScheme().isValidInTotalScore(this, raceColumn, timePoint) && 
               (getScoreCorrection().isScoreCorrected(competitor, raceColumn, timePoint) ||
                       ((trackedRaceForCompetitorInColumn=raceColumn.getTrackedRace(competitor)) != null &&
                        trackedRaceForCompetitorInColumn.hasStarted(timePoint)));
    }
    
    @Override
    public void addRaceColumnListener(RaceColumnListener listener) {
        getRaceColumnListeners().addRaceColumnListener(listener);
    }

    @Override
    public void removeRaceColumnListener(RaceColumnListener listener) {
        getRaceColumnListeners().removeRaceColumnListener(listener);
    }

    @Override
    public Entry getEntry(final Competitor competitor, final RaceColumn race, final TimePoint timePoint) throws NoWindException {
        Callable<Integer> trackedRankProvider = new Callable<Integer>() {
            public Integer call() throws NoWindException {
                return getTrackedRank(competitor, race, timePoint);
            }
        };
        final Result correctedResults = getScoreCorrection().getCorrectedScore(trackedRankProvider, competitor, race,
                timePoint, new NumberOfCompetitorsFetcherImpl(), getScoringScheme());
        boolean discarded = isDiscarded(competitor, race, timePoint);
        final Double correctedScore = correctedResults.getCorrectedScore();
        return new EntryImpl(trackedRankProvider, correctedScore, correctedResults.isCorrected(),
                discarded ? DOUBLE_0
                        : correctedScore == null ? null : Double.valueOf(correctedScore * race.getFactor()),
                        correctedResults.getMaxPointsReason(), discarded, race.getFleetOfCompetitor(competitor));
    }

    @Override
    public Map<RaceColumn, List<Competitor>> getRankedCompetitorsFromBestToWorstAfterEachRaceColumn(TimePoint timePoint) throws NoWindException {
        Map<RaceColumn, List<Competitor>> result = new HashMap<>();
        List<RaceColumn> raceColumnsToConsider = new ArrayList<>();
        for (RaceColumn raceColumn : getRaceColumns()) {
            raceColumnsToConsider.add(raceColumn);
            result.put(raceColumn, getCompetitorsFromBestToWorst(raceColumnsToConsider, timePoint));
        }
        return result;
    }

    @Override
    public Map<Pair<Competitor, RaceColumn>, Entry> getContent(final TimePoint timePoint) throws NoWindException {
        Map<Pair<Competitor, RaceColumn>, Entry> result = new HashMap<Pair<Competitor, RaceColumn>, Entry>();
        Map<Competitor, Set<RaceColumn>> discardedRaces = new HashMap<Competitor, Set<RaceColumn>>();
        for (final RaceColumn raceColumn : getRaceColumns()) {
            for (final Competitor competitor : getCompetitors()) {
                Callable<Integer> trackedRankProvider = new Callable<Integer>() {
                    @Override
                    public Integer call() throws Exception {
                        return getTrackedRank(competitor, raceColumn, timePoint);
                    }
                };
                Result correctedResults = getScoreCorrection().getCorrectedScore(trackedRankProvider, competitor, raceColumn,
                        timePoint, new NumberOfCompetitorsFetcherImpl(), getScoringScheme());
                Set<RaceColumn> discardedRacesForCompetitor = discardedRaces.get(competitor);
                if (discardedRacesForCompetitor == null) {
                    discardedRacesForCompetitor = getResultDiscardingRule().getDiscardedRaceColumns(competitor, this, getRaceColumns(), timePoint);
                    discardedRaces.put(competitor, discardedRacesForCompetitor);
                }
                boolean discarded = discardedRacesForCompetitor.contains(raceColumn);
                final Double correctedScore = correctedResults.getCorrectedScore();
                Entry entry = new EntryImpl(trackedRankProvider, correctedScore,
                        correctedResults.isCorrected(), discarded ? DOUBLE_0 : (correctedScore==null?null:
                                Double.valueOf((correctedScore * raceColumn.getFactor()))),
                                correctedResults.getMaxPointsReason(), discarded,
                                raceColumn.getFleetOfCompetitor(competitor));
                result.put(new Pair<Competitor, RaceColumn>(competitor, raceColumn), entry);
            }
        }
        return result;
    }

    @Override
    public void trackedRaceLinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        getRaceColumnListeners().notifyListenersAboutTrackedRaceLinked(raceColumn, fleet, trackedRace);
    }

    @Override
    public void trackedRaceUnlinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        getRaceColumnListeners().notifyListenersAboutTrackedRaceUnlinked(raceColumn, fleet, trackedRace);
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
    
    @Override
    public void isMedalRaceChanged(RaceColumn raceColumn, boolean newIsMedalRace) {
        getRaceColumnListeners().notifyListenersAboutIsMedalRaceChanged(raceColumn, newIsMedalRace);
    }
    
    @Override
    public void isStartsWithZeroScoreChanged(RaceColumn raceColumn, boolean newIsStartsWithZeroScore) {
        getRaceColumnListeners().notifyListenersAboutIsStartsWithZeroScoreChanged(raceColumn, newIsStartsWithZeroScore);
    }

    @Override
    public void isFirstColumnIsNonDiscardableCarryForwardChanged(RaceColumn raceColumn, boolean firstColumnIsNonDiscardableCarryForward) {
        getRaceColumnListeners().notifyListenersAboutIsFirstColumnIsNonDiscardableCarryForwardChanged(raceColumn, firstColumnIsNonDiscardableCarryForward);
    }

    @Override
    public void raceColumnMoved(RaceColumn raceColumn, int newIndex) {
        getRaceColumnListeners().notifyListenersAboutRaceColumnMoved(raceColumn, newIndex);
    }

    @Override
    public void factorChanged(RaceColumn raceColumn, Double oldFactor, Double newFactor) {
        getRaceColumnListeners().notifyListenersAboutFactorChanged(raceColumn, oldFactor, newFactor);
    }

    /**
     * A leaderboard will only accept the addition of a race column if the column's name is unique across the leaderboard.
     */
    @Override
    public boolean canAddRaceColumnToContainer(RaceColumn newRaceColumn) {
        boolean result = true;
        for (RaceColumn raceColumn : getRaceColumns()) {
            if (raceColumn.getName().equals(newRaceColumn.getName())) {
                result = false;
                break;
            }
        }
        return result;
    }

    @Override
    public void raceColumnAddedToContainer(RaceColumn raceColumn) {
        getRaceColumnListeners().notifyListenersAboutRaceColumnAddedToContainer(raceColumn);
    }

    @Override
    public void raceColumnRemovedFromContainer(RaceColumn raceColumn) {
        getRaceColumnListeners().notifyListenersAboutRaceColumnRemovedFromContainer(raceColumn);
    }

    @Override
    public void competitorDisplayNameChanged(Competitor competitor, String oldDisplayName, String displayName) {
        getRaceColumnListeners().notifyListenersAboutCompetitorDisplayNameChanged(competitor, oldDisplayName, displayName);
    }

    @Override
    public void resultDiscardingRuleChanged(ResultDiscardingRule oldDiscardingRule, ResultDiscardingRule newDiscardingRule) {
        getRaceColumnListeners().notifyListenersAboutResultDiscardingRuleChanged(oldDiscardingRule, newDiscardingRule);
    }

    @Override
    public boolean isTransient() {
        return false;
    }

    /**
     * Finds out the time point when any of the {@link Leaderboard#getTrackedRaces() tracked races currently attached to
     * the <code>leaderboard</code>} and the {@link Leaderboard#getScoreCorrection() score corrections} have last been
     * modified. If no tracked race is attached and no time-stamped score corrections have been applied to the leaderboard,
     * <code>null</code> is returned. The time point computed this way is a good choice for normalizing queries for later time
     * points in an attempt to achieve more cache hits.<p>
     * 
     * Note, however, that the result does not tell about structural changes to the leaderboard and therefore cannot be used
     * to determine the need for cache invalidation. For example, if a column is added to a leaderboard after the time point
     * returned by this method but that column's attached tracked race has finished before the time point returned by this method,
     * the result of this method won't change. Still, the contents of the leaderboard will change by a change in column structure.
     * A different means to determine the possibility of changes that happened to this leaderboard must be used for cache
     * management. Such a facility has to listen for score correction changes, tracked races being attached or detached and
     * the column structure changing.
     * 
     * @see TrackedRace#getTimePointOfNewestEvent()
     * @see SettableScoreCorrection#getTimePointOfLastCorrectionsValidity()
     */
    @Override
    public TimePoint getTimePointOfLatestModification() {
        TimePoint result = null;
        for (TrackedRace trackedRace : getTrackedRaces()) {
            if (result == null || (trackedRace.getTimePointOfNewestEvent() != null && trackedRace.getTimePointOfNewestEvent().after(result))) {
                result = trackedRace.getTimePointOfNewestEvent();
            }
        }
        TimePoint timePointOfLastScoreCorrection = getScoreCorrection().getTimePointOfLastCorrectionsValidity();
        if (timePointOfLastScoreCorrection != null && (result == null || timePointOfLastScoreCorrection.after(result))) {
            result = timePointOfLastScoreCorrection;
        }
        return result;
    }

    @Override
    public Pair<GPSFixMoving, Speed> getMaximumSpeedOverGround(Competitor competitor, TimePoint timePoint) {
        Pair<GPSFixMoving, Speed> result = null;
        // TODO should we ensure that competitor participated in all race columns?
        for (TrackedRace trackedRace : getTrackedRaces()) {
            if (Util.contains(trackedRace.getRace().getCompetitors(), competitor)) {
                NavigableSet<MarkPassing> markPassings = trackedRace.getMarkPassings(competitor);
                if (!markPassings.isEmpty()) {
                    TimePoint from = markPassings.first().getTimePoint();
                    TimePoint to;
                    if (timePoint.after(markPassings.last().getTimePoint()) &&
                            markPassings.last().getWaypoint() == trackedRace.getRace().getCourse().getLastWaypoint()) {
                        // stop counting when competitor finished the race
                        to = markPassings.last().getTimePoint();
                    } else {
                        to = timePoint;
                    }
                    Pair<GPSFixMoving, Speed> maxSpeed = trackedRace.getTrack(competitor).getMaximumSpeedOverGround(from, to);
                    if (result == null || result.getB() == null ||
                            (maxSpeed != null && maxSpeed.getB() != null && maxSpeed.getB().compareTo(result.getB()) > 0)) {
                        result = maxSpeed;
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Long getTotalTimeSailedInLegTypeInMilliseconds(Competitor competitor, LegType legType, TimePoint timePoint) throws NoWindException {
        Long result = null;
        // TODO should we ensure that competitor participated in all race columns?
        outerLoop:
        for (TrackedRace trackedRace : getTrackedRaces()) {
            if (Util.contains(trackedRace.getRace().getCompetitors(), competitor)) {
                trackedRace.getRace().getCourse().lockForRead();
                try {
                    for (Leg leg : trackedRace.getRace().getCourse().getLegs()) {
                        TrackedLegOfCompetitor trackedLeg = trackedRace.getTrackedLeg(competitor, leg);
                        if (trackedLeg.hasStartedLeg(timePoint)) {
                            // find out leg type at the time the competitor started the leg
                            try {
                                LegType trackedLegType = trackedRace.getTrackedLeg(leg).getLegType(
                                        trackedLeg.getStartTime());
                                if (legType == trackedLegType) {
                                    Long millisecondsSpendOnDownwind = trackedLeg.getTimeInMilliSeconds(timePoint);
                                    if (millisecondsSpendOnDownwind != null) {
                                        if (result == null) {
                                            result = millisecondsSpendOnDownwind;
                                        } else {
                                            result += millisecondsSpendOnDownwind;
                                        }
                                    } else {
                                        // Although the competitor has started the leg, no value was produced. This
                                        // means that
                                        // the competitor didn't finish the leg before tracking ended. No useful value
                                        // can
                                        // be obtained for this competitor anymore.
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
    public Long getTotalTimeSailedInMilliseconds(Competitor competitor, TimePoint timePoint) {
        Long result = null;
        for (TrackedRace trackedRace : getTrackedRaces()) {
            if (Util.contains(trackedRace.getRace().getCompetitors(), competitor)) {
                NavigableSet<MarkPassing> markPassings = trackedRace.getMarkPassings(competitor);
                if (!markPassings.isEmpty()) {
                    TimePoint from = trackedRace.getStartOfRace(); // start counting at race start, not when the competitor passed the line
                    if (from != null && !timePoint.before(from)) { // but only if the race started after timePoint
                        TimePoint to;
                        if (timePoint.after(markPassings.last().getTimePoint())
                                && markPassings.last().getWaypoint() == trackedRace.getRace().getCourse()
                                        .getLastWaypoint()) {
                            // stop counting when competitor finished the race
                            to = markPassings.last().getTimePoint();
                        } else {
                            if (trackedRace.getEndOfTracking() != null
                                    && timePoint.after(trackedRace.getEndOfTracking())) {
                                result = null; // race not finished until end of tracking; no reasonable value can be
                                               // computed for competitor
                                break;
                            } else {
                                to = timePoint;
                            }
                        }
                        long timeSpent = to.asMillis() - from.asMillis();
                        if (result == null) {
                            result = timeSpent;
                        } else {
                            result += timeSpent;
                        }
                    }
                }
            }
        }
        return result;
    }

    @Override
    public Distance getTotalDistanceTraveled(Competitor competitor, TimePoint timePoint) {
        Distance result = null;
        for (TrackedRace trackedRace : getTrackedRaces()) {
            if (Util.contains(trackedRace.getRace().getCompetitors(), competitor)) {
                Distance distanceSailedInRace = trackedRace.getDistanceTraveled(competitor, timePoint);
                if (distanceSailedInRace != null) {
                    if (result == null) {
                        result = distanceSailedInRace;
                    } else {
                        result = result.add(distanceSailedInRace);
                    }
                }
            }
        }
        return result;
    }

    protected RaceColumnListeners getRaceColumnListeners() {
        return raceColumnListeners;
    }
    
    @Override
    public Iterable<Competitor> getCompetitors() {
        // TODO bug 1348: try to cache result; invalidation would have to listen for columns added and suppression changing;
        // for meta-leaderbaords, transitive listening to those events would be necessary.
        Set<Competitor> result = new HashSet<>();
        for (Competitor competitor : getAllCompetitors()) {
            if (!isSuppressed(competitor)) {
                result.add(competitor);
            }
        }
        return result;
    }

    /**
     * Defines the difference between {@link #getCompetitors} and {@link #getAllCompetitors}. If a competitor is suppressed,
     * it won't participate in the scoring process, particularly because it isn't considered by {@link #getCompetitorsFromBestToWorst(TimePoint)}
     * nor {@link #getCompetitorsFromBestToWorst(RaceColumn, TimePoint)}.
     */
    private boolean isSuppressed(Competitor competitor) {
        LockUtil.lockForRead(suppressedCompetitorsLock);
        try {
            return suppressedCompetitors.contains(competitor);
        } finally {
            LockUtil.unlockAfterRead(suppressedCompetitorsLock);
        }
    }
    
    @Override
    public Iterable<Competitor> getSuppressedCompetitors() {
        LockUtil.lockForRead(suppressedCompetitorsLock);
        try {
            return new HashSet<Competitor>(suppressedCompetitors);
        } finally {
            LockUtil.unlockAfterRead(suppressedCompetitorsLock);
        }
    }
    
    @Override
    public void setSuppressed(Competitor competitor, boolean suppressed) {
        LockUtil.lockForWrite(suppressedCompetitorsLock);
        try {
            if (suppressed) {
                suppressedCompetitors.add(competitor);
            } else {
                suppressedCompetitors.remove(competitor);
            }
            getScoreCorrection().notifyListenersAboutIsSuppressedChange(competitor, suppressed);
        } finally {
            LockUtil.unlockAfterWrite(suppressedCompetitorsLock);
        }
    }

    @Override
    public TimePoint getNowMinusDelay() {
        final MillisecondsTimePoint now = MillisecondsTimePoint.now();
        final Long delayToLiveInMillis = getDelayToLiveInMillis();
        TimePoint timePoint = delayToLiveInMillis == null ? now : now.minus(delayToLiveInMillis);
        return timePoint;
    }
    
    @Override
    public void raceLogEventAdded(RaceColumn raceColumn, RaceLogIdentifier raceLogIdentifier, RaceLogEvent event) {
        getRaceColumnListeners().notifyListenersAboutRaceLogEventAdded(raceColumn, raceLogIdentifier, event);
    }

    @Override
    public LeaderboardDTO computeDTO(final TimePoint timePoint,
            final Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails, final boolean waitForLatestAnalyses,
            TrackedRegattaRegistry trackedRegattaRegistry, final DomainFactory baseDomainFactory)
            throws NoWindException {
        long startOfRequestHandling = System.currentTimeMillis();
        LeaderboardDTO result = null;
        result = new LeaderboardDTO(this.getScoreCorrection().getTimePointOfLastCorrectionsValidity() == null ? null
                : this.getScoreCorrection().getTimePointOfLastCorrectionsValidity().asDate(),
                this.getScoreCorrection() == null ? null : this.getScoreCorrection().getComment(), this
                        .getScoringScheme().isHigherBetter(), new UUIDGenerator());
        result.competitors = new ArrayList<CompetitorDTO>();
        result.name = this.getName();
        result.competitorDisplayNames = new HashMap<CompetitorDTO, String>();
        for (Competitor suppressedCompetitor : this.getSuppressedCompetitors()) {
            result.setSuppressed(baseDomainFactory.convertToCompetitorDTO(suppressedCompetitor), true);
        }
        for (RaceColumn raceColumn : this.getRaceColumns()) {
            RaceColumnDTO raceColumnDTO = result.createEmptyRaceColumn(raceColumn.getName(), raceColumn.isMedalRace(),
                    this.getScoringScheme().isValidInTotalScore(this, raceColumn, timePoint));
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
                result.addRace(raceColumn.getName(), raceColumn.getExplicitFactor(), raceColumn.getFactor(), fleetDTO,
                        raceColumn.isMedalRace(), raceIdentifier, race);
            }
            result.setCompetitorsFromBestToWorst(raceColumnDTO,
                    baseDomainFactory.getCompetitorDTOList(this.getCompetitorsFromBestToWorst(raceColumn, timePoint)));
        }
        result.setDelayToLiveInMillisForLatestRace(this.getDelayToLiveInMillis());
        result.rows = new HashMap<CompetitorDTO, LeaderboardRowDTO>();
        result.hasCarriedPoints = this.hasCarriedPoints();
            if (this.getResultDiscardingRule() instanceof ThresholdBasedResultDiscardingRule) {
                result.discardThresholds = ((ThresholdBasedResultDiscardingRule) this.getResultDiscardingRule()).getDiscardIndexResultsStartingWithHowManyRaces();
            } else {
                result.discardThresholds = null;
            }
        // competitor, leading to square effort. We therefore need to compute the leg ranks for those race where leg
        // details
        // are requested only once and pass them into getLeaderboardEntryDTO
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
                                legRanksCache.put(trackedLeg.getLeg(), trackedLeg.getRanks(timePoint));
                            }
                        } finally {
                            trackedRace.getRace().getCourse().unlockAfterRead();
                        }
                    }
                }
            }
        }
        for (final Competitor competitor : this.getCompetitorsFromBestToWorst(timePoint)) {
            CompetitorDTO competitorDTO = baseDomainFactory.convertToCompetitorDTO(competitor);
            LeaderboardRowDTO row = new LeaderboardRowDTO();
            row.competitor = competitorDTO;
            row.fieldsByRaceColumnName = new HashMap<String, LeaderboardEntryDTO>();
            row.carriedPoints = this.hasCarriedPoints(competitor) ? this.getCarriedPoints(competitor) : null;
            row.totalPoints = this.getTotalPoints(competitor, timePoint);
            addOverallDetailsToRow(timePoint, competitor, row);
            result.competitors.add(competitorDTO);
            Map<String, Future<LeaderboardEntryDTO>> futuresForColumnName = new HashMap<String, Future<LeaderboardEntryDTO>>();
            for (final RaceColumn raceColumn : this.getRaceColumns()) {
                RunnableFuture<LeaderboardEntryDTO> future = new FutureTask<LeaderboardEntryDTO>(
                        new Callable<LeaderboardEntryDTO>() {
                            @Override
                            public LeaderboardEntryDTO call() {
                                try {
                                    Entry entry = AbstractSimpleLeaderboardImpl.this.getEntry(competitor, raceColumn,
                                            timePoint);
                                    return getLeaderboardEntryDTO(
                                            entry,
                                            raceColumn,
                                            competitor,
                                            timePoint,
                                            namesOfRaceColumnsForWhichToLoadLegDetails != null
                                                    && namesOfRaceColumnsForWhichToLoadLegDetails.contains(raceColumn
                                                            .getName()), waitForLatestAnalyses, legRanksCache,
                                            baseDomainFactory);
                                } catch (NoWindException e) {
                                    logger.info("Exception trying to compute leaderboard entry for competitor "
                                            + competitor.getName() + " in race column " + raceColumn.getName() + ": "
                                            + e.getMessage());
                                    logger.throwing(AbstractSimpleLeaderboardImpl.class.getName(),
                                            "computeLeaderboardByName.future.call()", e);
                                    throw new NoWindError(e);
                                }
                            }
                        });
                executor.execute(future);
                futuresForColumnName.put(raceColumn.getName(), future);
            }
            for (Map.Entry<String, Future<LeaderboardEntryDTO>> raceColumnNameAndFuture : futuresForColumnName
                    .entrySet()) {
                try {
                    row.fieldsByRaceColumnName.put(raceColumnNameAndFuture.getKey(), raceColumnNameAndFuture.getValue()
                            .get());
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                } catch (ExecutionException e) {
                    // See also bug 1371: for stability reasons, don't let the exception percolate but rather accept
                    // null values.
                    // If new evidence is provided, a re-calculation of the leaderboard will be triggered anyway. So
                    // this helps
                    // robustness from a user's perspective.
                    logger.log(
                            Level.SEVERE,
                            AbstractSimpleLeaderboardImpl.class.getName() + ".computeDTO(" + this.getName() + ", "
                                    + timePoint + ", " + namesOfRaceColumnsForWhichToLoadLegDetails
                                    + "): exception during computing leaderboard entry for competitor "
                                    + competitor.getName() + " in race column " + raceColumnNameAndFuture.getKey()
                                    + ". Leaving empty.", e);
                }
            }
            result.rows.put(competitorDTO, row);
            String displayName = this.getDisplayName(competitor);
            if (displayName != null) {
                result.competitorDisplayNames.put(competitorDTO, displayName);
            }
        }
        logger.info("computeLeaderboardByName("+this.getName()+", "+timePoint+", "+namesOfRaceColumnsForWhichToLoadLegDetails+") took "+
                (System.currentTimeMillis()-startOfRequestHandling)+"ms");
        return result;
    }

    private void addOverallDetailsToRow(final TimePoint timePoint,
            final Competitor competitor, LeaderboardRowDTO row) throws NoWindException {
        final Pair<GPSFixMoving, Speed> maximumSpeedOverGround = this.getMaximumSpeedOverGround(competitor, timePoint);
        if (maximumSpeedOverGround != null && maximumSpeedOverGround.getB() != null) {
            row.maximumSpeedOverGroundInKnots = maximumSpeedOverGround.getB().getKnots();
            row.whenMaximumSpeedOverGroundWasAchieved = maximumSpeedOverGround.getA().getTimePoint().asDate();
        }
        final Long totalTimeSailedDownwindInMilliseconds = this.getTotalTimeSailedInLegTypeInMilliseconds(competitor, LegType.DOWNWIND, timePoint);
        row.totalTimeSailedDownwindInSeconds = totalTimeSailedDownwindInMilliseconds==null?null:1./1000.*totalTimeSailedDownwindInMilliseconds;
        final Long totalTimeSailedUpwindInMilliseconds = this.getTotalTimeSailedInLegTypeInMilliseconds(competitor, LegType.UPWIND, timePoint);
        row.totalTimeSailedUpwindInSeconds = totalTimeSailedUpwindInMilliseconds==null?null:1./1000.*totalTimeSailedUpwindInMilliseconds;
        final Long totalTimeSailedReachingInMilliseconds = this.getTotalTimeSailedInLegTypeInMilliseconds(competitor, LegType.REACHING, timePoint);
        row.totalTimeSailedReachingInSeconds = totalTimeSailedReachingInMilliseconds==null?null:1./1000.*totalTimeSailedReachingInMilliseconds;
        final Long totalTimeSailedInMilliseconds = this.getTotalTimeSailedInMilliseconds(competitor, timePoint);
        row.totalTimeSailedInSeconds = totalTimeSailedInMilliseconds==null?null:1./1000.*totalTimeSailedInMilliseconds;
        final Distance totalDistanceTraveledInMeters = this.getTotalDistanceTraveled(competitor, timePoint);
        row.totalDistanceTraveledInMeters = totalDistanceTraveledInMeters==null?null:totalDistanceTraveledInMeters.getMeters();
    }

    /**
     * @param waitForLatestAnalyses
     *            if <code>false</code>, this method is allowed to read the maneuver analysis results from a cache that
     *            may not reflect all data already received; otherwise, the method will always block for the latest
     *            cache updates to have happened before returning.
     */
    private LeaderboardEntryDTO getLeaderboardEntryDTO(Entry entry, RaceColumn raceColumn, Competitor competitor,
            TimePoint timePoint, boolean addLegDetails, boolean waitForLatestAnalyses,
            Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache, DomainFactory baseDomainFactory) throws NoWindException {
        LeaderboardEntryDTO entryDTO = new LeaderboardEntryDTO();
        TrackedRace trackedRace = raceColumn.getTrackedRace(competitor);
        entryDTO.race = trackedRace == null ? null : trackedRace.getRaceIdentifier();
        entryDTO.netPoints = entry.getNetPoints();
        entryDTO.netPointsCorrected = entry.isNetPointsCorrected();
        entryDTO.totalPoints = entry.getTotalPoints();
        entryDTO.reasonForMaxPoints = entry.getMaxPointsReason();
        entryDTO.discarded = entry.isDiscarded();
        if (trackedRace != null) {
            entryDTO.timePointOfLastPositionFixAtOrBeforeQueryTimePoint = getTimePointOfLastFixAtOrBefore(competitor, trackedRace, timePoint);
            if(entryDTO.timePointOfLastPositionFixAtOrBeforeQueryTimePoint != null) {
                long timeDifferenceInMs = timePoint.asMillis() - entryDTO.timePointOfLastPositionFixAtOrBeforeQueryTimePoint.getTime();
                entryDTO.timeSinceLastPositionFixInSeconds = timeDifferenceInMs == 0 ? 0.0 : timeDifferenceInMs / 1000.0;  
            } else {
                entryDTO.timeSinceLastPositionFixInSeconds = null;  
            }
        }
        if (addLegDetails && trackedRace != null) {
            try {
                RaceDetails raceDetails = getRaceDetails(trackedRace, competitor, timePoint, waitForLatestAnalyses, legRanksCache);
                entryDTO.legDetails = raceDetails.getLegDetails();
                entryDTO.windwardDistanceToOverallLeaderInMeters = raceDetails.getWindwardDistanceToOverallLeader() == null ? null
                        : raceDetails.getWindwardDistanceToOverallLeader().getMeters();
                entryDTO.averageCrossTrackErrorInMeters = raceDetails.getAverageCrossTrackError() == null ? null
                        : raceDetails.getAverageCrossTrackError().getMeters();
                final TimePoint startOfRace = trackedRace.getStartOfRace();
                if (startOfRace != null) {
                    Waypoint startWaypoint = trackedRace.getRace().getCourse().getFirstWaypoint();
                    NavigableSet<MarkPassing> competitorMarkPassings = trackedRace.getMarkPassings(competitor);
                    trackedRace.lockForRead(competitorMarkPassings);
                    try {
                        if (!competitorMarkPassings.isEmpty()) {
                            final MarkPassing firstMarkPassing = competitorMarkPassings.iterator().next();
                            if (firstMarkPassing.getWaypoint() == startWaypoint) {
                                Distance distanceToStartLineFiveSecondsBeforeStartOfRace = trackedRace.getDistanceToStartLine(competitor, 5);
                                entryDTO.distanceToStartLineFiveSecondsBeforeStartInMeters = distanceToStartLineFiveSecondsBeforeStartOfRace == null ? null
                                        : distanceToStartLineFiveSecondsBeforeStartOfRace.getMeters();
                                Speed speedFiveSecondsBeforeStartOfRace = trackedRace.getSpeed(competitor, 5);
                                entryDTO.speedOverGroundFiveSecondsBeforeStartInKnots = speedFiveSecondsBeforeStartOfRace == null ? null
                                        : speedFiveSecondsBeforeStartOfRace.getKnots();
                                Distance distanceToStartLineAtStartOfRace = trackedRace.getDistanceToStartLine(
                                        competitor, startOfRace);
                                entryDTO.distanceToStartLineAtStartOfRaceInMeters = distanceToStartLineAtStartOfRace == null ? null
                                        : distanceToStartLineAtStartOfRace.getMeters();
                                Speed speedAtStartTime = trackedRace.getTrack(competitor)
                                        .getEstimatedSpeed(startOfRace);
                                entryDTO.speedOverGroundAtStartOfRaceInKnots = speedAtStartTime == null ? null
                                        : speedAtStartTime.getKnots();
                                TimePoint competitorStartTime = firstMarkPassing.getTimePoint();
                                Speed competitorSpeedWhenPassingStart = trackedRace.getTrack(competitor)
                                        .getEstimatedSpeed(competitorStartTime);
                                entryDTO.speedOverGroundAtPassingStartWaypointInKnots = competitorSpeedWhenPassingStart == null ? null
                                        : competitorSpeedWhenPassingStart.getKnots();
                                entryDTO.startTack = trackedRace.getTack(competitor, competitorStartTime);
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
            } catch (ExecutionException e) {
                throw new RuntimeException(e); // the future used to calculate the leg details was interrupted; escalate as runtime exception
            }
        }
        final Fleet fleet = entry.getFleet();
        entryDTO.fleet = fleet == null ? null : baseDomainFactory.convertToFleetDTO(fleet);
        return entryDTO;
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

    private static class RaceDetails {
        private final List<LegEntryDTO> legDetails;
        private final Distance windwardDistanceToOverallLeader;
        private final Distance averageCrossTrackError;
        public RaceDetails(List<LegEntryDTO> legDetails, Distance windwardDistanceToOverallLeader,
                Distance averageCrossTrackError) {
            super();
            this.legDetails = legDetails;
            this.windwardDistanceToOverallLeader = windwardDistanceToOverallLeader;
            this.averageCrossTrackError = averageCrossTrackError;
        }
        public List<LegEntryDTO> getLegDetails() {
            return legDetails;
        }
        public Distance getWindwardDistanceToOverallLeader() {
            return windwardDistanceToOverallLeader;
        }
        public Distance getAverageCrossTrackError() {
            return averageCrossTrackError;
        }
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
            boolean waitForLatestAnalyses, Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache) throws NoWindException, InterruptedException, ExecutionException {
        RaceDetails raceDetails;
        if (trackedRace.getEndOfTracking() != null && trackedRace.getEndOfTracking().compareTo(timePoint) < 0) {
            raceDetails = getRaceDetailsForEndOfTrackingFromCacheOrCalculateAndCache(trackedRace, competitor, legRanksCache);
        } else {
            raceDetails = calculateRaceDetails(trackedRace, competitor, timePoint, waitForLatestAnalyses, legRanksCache);
        }
        return raceDetails;
    }

    private RaceDetails getRaceDetailsForEndOfTrackingFromCacheOrCalculateAndCache(final TrackedRace trackedRace,
            final Competitor competitor, final Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache)
                    throws NoWindException, InterruptedException, ExecutionException {
        final Pair<TrackedRace, Competitor> key = new Pair<TrackedRace, Competitor>(trackedRace, competitor);
        RunnableFuture<RaceDetails> raceDetails;
        synchronized (raceDetailsAtEndOfTrackingCache) {
            raceDetails = raceDetailsAtEndOfTrackingCache.get(key);
            if (raceDetails == null) {
                raceDetails = new FutureTask<RaceDetails>(new Callable<RaceDetails>() {
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
                                legRanksCache);
                    }
                });
                raceDetailsExecutor.execute(raceDetails);
                raceDetailsAtEndOfTrackingCache.put(key, raceDetails);
                final CacheInvalidationListener cacheInvalidationListener = new CacheInvalidationListener(trackedRace, competitor);
                trackedRace.addListener(cacheInvalidationListener);
                cacheInvalidationListeners.add(cacheInvalidationListener);
            }
        }
        return raceDetails.get();
    }

    private RaceDetails calculateRaceDetails(TrackedRace trackedRace, Competitor competitor, TimePoint timePoint,
            boolean waitForLatestAnalyses, Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache) throws NoWindException {
        List<LegEntryDTO> legDetails;
        legDetails = new ArrayList<LegEntryDTO>();
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
                    legEntry = createLegEntry(trackedLeg, timePoint, waitForLatestAnalyses, legRanksCache);
                } else {
                    legEntry = null;
                }
                legDetails.add(legEntry);
            }
            final Distance windwardDistanceToOverallLeader = trackedRace == null ? null : trackedRace
                    .getWindwardDistanceToOverallLeader(competitor, timePoint);
            final Distance averageCrossTrackError = trackedRace == null ? null : trackedRace.getAverageCrossTrackError(
                    competitor, timePoint, waitForLatestAnalyses);
            return new RaceDetails(legDetails, windwardDistanceToOverallLeader, averageCrossTrackError);
        } finally {
            course.unlockAfterRead();
        }
    }

    private LegEntryDTO createLegEntry(TrackedLegOfCompetitor trackedLeg, TimePoint timePoint,
            boolean waitForLatestAnalyses, Map<Leg, LinkedHashMap<Competitor, Integer>> legRanksCache) throws NoWindException {
        LegEntryDTO result;
        if (trackedLeg == null) {
            result = null;
        } else {
            result = new LegEntryDTO();
            final Speed averageSpeedOverGround = trackedLeg.getAverageSpeedOverGround(timePoint);
            result.averageSpeedOverGroundInKnots = averageSpeedOverGround == null ? null : averageSpeedOverGround.getKnots();
            final Distance averageCrossTrackError = trackedLeg.getAverageCrossTrackError(timePoint, waitForLatestAnalyses);
            result.averageCrossTrackErrorInMeters = averageCrossTrackError == null ? null : averageCrossTrackError.getMeters();
            Double speedOverGroundInKnots;
            if (trackedLeg.hasFinishedLeg(timePoint))  {
                speedOverGroundInKnots = averageSpeedOverGround == null ? null : averageSpeedOverGround.getKnots();
            } else {
                final SpeedWithBearing speedOverGround = trackedLeg.getSpeedOverGround(timePoint);
                speedOverGroundInKnots = speedOverGround == null ? null : speedOverGround.getKnots();
            }
            result.currentSpeedOverGroundInKnots = speedOverGroundInKnots == null ? null : speedOverGroundInKnots;
            Distance distanceTraveled = trackedLeg.getDistanceTraveled(timePoint);
            result.distanceTraveledInMeters = distanceTraveled == null ? null : distanceTraveled.getMeters();
            result.estimatedTimeToNextWaypointInSeconds = trackedLeg.getEstimatedTimeToNextMarkInSeconds(timePoint);
            result.timeInMilliseconds = trackedLeg.getTimeInMilliSeconds(timePoint);
            result.finished = trackedLeg.hasFinishedLeg(timePoint);
            result.gapToLeaderInSeconds = trackedLeg.getGapToLeaderInSeconds(timePoint,
                    legRanksCache.get(trackedLeg.getLeg()).entrySet().iterator().next().getKey());
            if (result.gapToLeaderInSeconds != null) {
                // FIXME problem: asking just after the beginning of the leg yields very different values from asking for the end of the previous leg.
                // This is because for the previous leg it's decided based on the mark passings; for the next (current) leg it's decided based on
                // windward distance and VMG
                Double gapAtEndOfPreviousLegInSeconds = getGapAtEndOfPreviousLegInSeconds(trackedLeg);
                if (gapAtEndOfPreviousLegInSeconds != null) {
                    result.gapChangeSinceLegStartInSeconds = result.gapToLeaderInSeconds - gapAtEndOfPreviousLegInSeconds;
                }
            }
            LinkedHashMap<Competitor, Integer> legRanks = legRanksCache.get(trackedLeg.getLeg());
            if (legRanks != null) {
                result.rank = legRanks.get(trackedLeg.getCompetitor());
            } else {
                result.rank = trackedLeg.getRank(timePoint);
            }
            result.started = trackedLeg.hasStartedLeg(timePoint);
            Speed velocityMadeGood;
            if (trackedLeg.hasFinishedLeg(timePoint)) {
                velocityMadeGood = trackedLeg.getAverageVelocityMadeGood(timePoint);
            } else {
                velocityMadeGood = trackedLeg.getVelocityMadeGood(timePoint);
            }
            result.velocityMadeGoodInKnots = velocityMadeGood == null ? null : velocityMadeGood.getKnots();
            Distance windwardDistanceToGo = trackedLeg.getWindwardDistanceToGo(timePoint);
            result.windwardDistanceToGoInMeters = windwardDistanceToGo == null ? null : windwardDistanceToGo
                    .getMeters();
            final TimePoint startOfRace = trackedLeg.getTrackedLeg().getTrackedRace().getStartOfRace();
            if (startOfRace != null && trackedLeg.hasStartedLeg(timePoint)) {
                // not using trackedLeg.getManeuvers(...) because it may not catch the mark passing maneuver starting this leg
                // because that may have been detected as slightly before the mark passing time, hence associated with the previous leg
                List<Maneuver> maneuvers = trackedLeg.getTrackedLeg().getTrackedRace()
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
                            if (!maneuver.getTimePoint().before(startOfLeg) && (!trackedLeg.hasFinishedLeg(timePoint) ||
                                    maneuver.getTimePoint().before(trackedLeg.getFinishTime()))) {
                                if (maneuver.getManeuverLoss() != null) {
                                    result.numberOfManeuvers.put(maneuver.getType(),
                                            result.numberOfManeuvers.get(maneuver.getType()) + 1);
                                    totalManeuverLossInMeters.put(maneuver.getType(),
                                            totalManeuverLossInMeters.get(maneuver.getType())
                                            + maneuver.getManeuverLoss().getMeters());
                                }
                            }
                            break;
                        case MARK_PASSING:
                            // analyze all mark passings, not only those after this leg's start, to catch the mark passing
                            // maneuver starting this leg, even if its time point is slightly before the mark passing starting this leg
                            MarkPassingManeuver mpm = (MarkPassingManeuver) maneuver;
                            if (mpm.getWaypointPassed() == trackedLeg.getLeg().getFrom()) {
                                result.sideToWhichMarkAtLegStartWasRounded = mpm.getSide();
                            }
                            break;
                        default:
                            /* Do nothing here.
                             * Throwing an exception destroys the toggling (and maybe other behaviour) of the leaderboard.
                             */
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
        }
        return result;
    }

    private Double getGapAtEndOfPreviousLegInSeconds(TrackedLegOfCompetitor trackedLeg) throws NoWindException {
        final Double result;
        final Course course = trackedLeg.getTrackedLeg().getTrackedRace().getRace().getCourse();
        course.lockForRead();
        try {
            int indexOfStartWaypoint = course.getIndexOfWaypoint(trackedLeg.getLeg().getFrom());
            if (indexOfStartWaypoint == 0) {
                // trackedLeg was the first leg; gap is determined by gap of start line passing time points
                Iterable<MarkPassing> markPassingsForLegStart = trackedLeg.getTrackedLeg().getTrackedRace().getMarkPassingsInOrder(trackedLeg.getLeg().getFrom());
                trackedLeg.getTrackedLeg().getTrackedRace().lockForRead(markPassingsForLegStart);
                try {
                    final Iterator<MarkPassing> markPassingsIter = markPassingsForLegStart.iterator();
                    if (markPassingsIter.hasNext()) {
                        TimePoint firstStart = markPassingsIter.next().getTimePoint();
                        final MarkPassing markPassingForFrom = trackedLeg.getTrackedLeg().getTrackedRace()
                                .getMarkPassing(trackedLeg.getCompetitor(), trackedLeg.getLeg().getFrom());
                        if (markPassingForFrom != null) {
                            TimePoint competitorStart = markPassingForFrom.getTimePoint();
                            result = (double) (competitorStart.asMillis() - firstStart.asMillis()) / 1000.;
                        } else {
                            result = null;
                        }
                    } else {
                        result = null;
                    }
                } finally {
                    trackedLeg.getTrackedLeg().getTrackedRace().unlockAfterRead(markPassingsForLegStart);
                }
            } else {
                TrackedLeg previousTrackedLeg = trackedLeg.getTrackedLeg().getTrackedRace().getTrackedLeg(course.getLegs().get(indexOfStartWaypoint-1));
                TrackedLegOfCompetitor previousTrackedLegOfCompetitor = previousTrackedLeg.getTrackedLeg(trackedLeg.getCompetitor());
                result = previousTrackedLegOfCompetitor.getGapToLeaderInSeconds(previousTrackedLegOfCompetitor.getFinishTime());
            }
            return result;
        } finally {
            course.unlockAfterRead();
        }
    }

    private LeaderboardDTO getLiveLeaderboard(Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails,
            TrackedRegattaRegistry trackedRegattaRegistry, DomainFactory baseDomainFactory) throws NoWindException {
        LiveLeaderboardUpdater liveLeaderboardUpdater = getLiveLeaderboardUpdater(trackedRegattaRegistry,
                baseDomainFactory);
        return liveLeaderboardUpdater.getLiveLeaderboard(namesOfRaceColumnsForWhichToLoadLegDetails);
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
    
    private LeaderboardDTOCache getLeaderboardDTOCache() {
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
    
    @Override
    public LeaderboardDTO getLeaderboardDTO(TimePoint timePoint, Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails,
            TrackedRegattaRegistry trackedRegattaRegistry, DomainFactory baseDomainFactory) throws NoWindException, InterruptedException, ExecutionException {
        LeaderboardDTO result = null;
        if (timePoint == null) {
            // date==null means live mode; however, if we're after the end of all races and after all score
            // corrections, don't use the live leaderboard updater which would keep re-calculating over and over again, but map
            // this to a usual non-live call which uses the regular LeaderboardDTOCache which is invalidated properly
            // when the tracked race associations or score corrections or tracked race contents changes:
            final TimePoint nowMinusDelay = this.getNowMinusDelay();
            final TimePoint timePointOfLatestModification = this.getTimePointOfLatestModification();
            if (timePointOfLatestModification != null && !nowMinusDelay.before(timePointOfLatestModification)) {
                // if there hasn't been any modification to the leaderboard since nowMinusDelay, use non-live mode
                // and pull the result from the regular leaderboard cache:
                timePoint = timePointOfLatestModification;
            } else {
                // don't use the regular leaderboard cache; the race still seems to be on; use the live leaderboard updater instead:
                timePoint = null;
                result = this.getLiveLeaderboard(namesOfRaceColumnsForWhichToLoadLegDetails, trackedRegattaRegistry, baseDomainFactory);
            }
        }
        if (timePoint != null) {
            // in replay we'd like up-to-date results; they are still cached
            // which is OK because the cache is invalidated whenever any of the tracked races attached to the
            // leaderboard changes.
            result = getLeaderboardDTOCache().getLeaderboardByName(timePoint, namesOfRaceColumnsForWhichToLoadLegDetails,
                    baseDomainFactory, trackedRegattaRegistry);
        }
        return result;
    }
    
    public String toString() {
        return getName() + " " + (getDefaultCourseArea() != null ? getDefaultCourseArea().getName() : "<No course area defined>") + " " + (getScoringScheme() != null ? getScoringScheme().getType().name() : "<No scoring scheme set>");
    }

}
