package com.sap.sailing.domain.leaderboard.caching;

import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardCacheManager;
import com.sap.sailing.domain.leaderboard.ScoreCorrection;
import com.sap.sailing.domain.leaderboard.ScoreCorrectionListener;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.util.impl.LockUtil;
import com.sap.sailing.util.impl.ThreadFactoryWithPriority;

/**
 * Caches the expensive to compute {@link LeaderboardDTO} results of a
 * {@link SailingServiceImpl#computeLeaderboardByName(String, com.sap.sailing.domain.common.TimePoint, Collection, boolean)} call.
 * By listening as {@link RaceChangeListener} on all tracked races attached to the leaderboard, and by updating this list
 * by listening as {@link RaceColumnListener} on the {@link Leaderboard}, each time a race attached to a leaderboard for which
 * this cache holds one or more {@link LeaderboardDTO}s changes, the cache entries for that leaderboard are removed. Also,
 * when the {@link ScoreCorrection}s of a leaderboard change, a {@link ScoreCorrectionListener} that is registered will be
 * notified and removes the leaderboard's cache entries from this cache.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public class LeaderboardDTOCache implements LeaderboardCache {
    private static final Logger logger = Logger.getLogger(LeaderboardDTOCache.class.getName());
    
    /**
     * In live operations, {@link #getLeaderboardByName(Date, Collection, DomainFactory, TrackedRegattaRegistry)} is the application's
     * bottleneck. When two clients ask the same data for the same leaderboard with their
     * <code>waitForLatestAnalyses</code> parameters set to <code>false</code>, expansion state and (quantized) time
     * stamp, no two computations should be spawned for the two clients. Instead, if the computation is still running,
     * all clients asking the same wait for the single result. Results are cached in this LRU-based evicting cache.
     */
    private final Map<Util.Pair<TimePoint, Collection<String>>, FutureTask<LeaderboardDTO>> leaderboardCache;
    private int leaderboardByNameCacheHitCount;
    private int leaderboardByNameCacheMissCount;
    
    /**
     * Tells if leaderboard computations shall wait for long-running analyses to complete or if they instead use the
     * last good analysis result, even if it is a bit outdated as compared to the time point queried. This particularly
     * applies to the wind estimation and the maneuver analysis.
     */
    private final boolean waitForLatestAnalyses;
    
    private static final int THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 3);
    /**
     * A multi-threaded executor for the currently running leaderboard requests, executing the {@link Future}s currently
     * pending.
     */
    private static final Executor computeLeadearboardByNameExecutor = new ThreadPoolExecutor(/* corePoolSize */ THREAD_POOL_SIZE,
            /* maximumPoolSize */ THREAD_POOL_SIZE,
            /* keepAliveTime */ 60, TimeUnit.SECONDS,
            /* workQueue */ new LinkedBlockingQueue<Runnable>(), new ThreadFactoryWithPriority(Thread.NORM_PRIORITY-1));

    private final LeaderboardCacheManager leaderboardCacheManager;
    
    private final Leaderboard leaderboard;
    
    public LeaderboardDTOCache(boolean waitForLatestAnalyses, Leaderboard leaderboard) {
        this.leaderboard = leaderboard;
        this.waitForLatestAnalyses = waitForLatestAnalyses;
        // if the leaderboard becomes weakly referenced and eventually GCed, then so can the cached results for it
        this.leaderboardCache = new LinkedHashMap<Pair<TimePoint, Collection<String>>, FutureTask<LeaderboardDTO>>(16, 0.75f, /* access-based eviction */ true) {
            private static final long serialVersionUID = 7287916997229815039L;
            @Override
            protected boolean removeEldestEntry(Map.Entry<Pair<TimePoint, Collection<String>>, FutureTask<LeaderboardDTO>> e) {
                return size() > 10; // remember 10 LeaderboardDTOs per leaderborad
            }
        };
        this.leaderboardCacheManager = new LeaderboardCacheManager(this);
    }
    
    @Override
    public void invalidate(Leaderboard leaderboard) {
        synchronized (leaderboardCache) {
            leaderboardCache.clear();
        }
    }
    
    @Override
    public void add(Leaderboard leaderboard) {}
    
    /**
     * If the cache holds entries for the <code>leaderboard</code> requested, compare <code>timePoint</code> to the
     * {@link #getLatestModification latest modification} affecting the <code>leaderboard</code>. If
     * <code>timePoint</code> is after that time, adjust it to the {@link #getLatestModification latest modification
     * time} for cache lookup and computation. This will increase chances that a subsequent request will achieve a cache
     * hit.
     * <p>
     * 
     * The {@link #waitForLatestAnalyses} field is passed on to
     * {@link SailingServiceImpl#computeLeaderboardByName(Leaderboard, TimePoint, Collection, boolean)} if a new cache
     * entry needs to be computed. Caching distinguished between
     */
    public LeaderboardDTO getLeaderboardByName(final TimePoint timePoint, final Collection<String> namesOfRaceColumnsForWhichToLoadLegDetails,
            final DomainFactory baseDomainFactory, final TrackedRegattaRegistry trackedRegattaRegistry) throws NoWindException, InterruptedException,
            ExecutionException {
        long startOfRequestHandling = System.currentTimeMillis();
        final TimePoint adjustedTimePoint;
        TimePoint timePointOfLastModification = leaderboard.getTimePointOfLatestModification();
        if (timePointOfLastModification != null && timePoint.after(timePointOfLastModification)) {
            adjustedTimePoint = timePointOfLastModification; 
            logger.fine("Adjusted time point in getLeaderboardByName from "+timePoint+" to "+adjustedTimePoint);
        } else {
            adjustedTimePoint = timePoint;
        }
        Util.Pair<TimePoint, Collection<String>> key = new Util.Pair<TimePoint, Collection<String>>(adjustedTimePoint,
                namesOfRaceColumnsForWhichToLoadLegDetails);
        FutureTask<LeaderboardDTO> future = null;
        boolean cacheHit = false;
        /*
         * Waiting for latest analyzes results largely regards wind estimation and maneuver cache; see SmartFutureCache.
         * Even if waitForLatestAnalysis is requested, it is OK to cache. The cache would be invalidated when the race
         * changes, forcing a new re-calculation based on the latest analysis results. Once the race stabilizes, the
         * latest analysis results for maneuvers and wind estimation will no longer change and can quickly be obtained
         * from the respective SmartFutureCache. At the same time, if a LeaderboardDTOCache entry is found, that was
         * based on the latest analysis results at the time. If new evidence is received, that would also invalidate the
         * LeaderboardDTOCache. Therefore, it's okay to re-use the LeaderboardDTOCache match even if the latest analysis
         * results are requested.
         */
        future = leaderboardCache.get(key);
        if (future == null) {
            final Thread callerThread = Thread.currentThread();
            future = new FutureTask<LeaderboardDTO>(new Callable<LeaderboardDTO>() {
                @Override
                public LeaderboardDTO call() throws Exception {
                    // The outer getLeaderboardByName(...) method will always wait for this future's completion.
                    // Therefore, it's safe to propagate the calling thread's locks to this one:
                    LockUtil.propagateLockSetFrom(callerThread);
                    try {
                        LeaderboardDTO result = leaderboard.computeDTO(adjustedTimePoint,
                                namesOfRaceColumnsForWhichToLoadLegDetails, waitForLatestAnalyses,
                                trackedRegattaRegistry, baseDomainFactory);
                        return result;
                    } finally {
                        LockUtil.unpropagateLockSetFrom(callerThread);
                    }
                }
            });
            computeLeadearboardByNameExecutor.execute(future);
            this.leaderboardCacheManager.add(leaderboard); // ensure the leaderboard is tracked for changes to invalidate
            leaderboardCache.put(key, future);
        } else {
            cacheHit = true;
        }
        if (cacheHit) {
            leaderboardByNameCacheHitCount++;
            logger.info("Cache hit in getLeaderboardByName("+leaderboard.getName()+", "+adjustedTimePoint+", "+namesOfRaceColumnsForWhichToLoadLegDetails+")");
        } else {
            leaderboardByNameCacheMissCount++;
        }
        logger.info("getLeaderboardByName cache hit vs. miss: "+leaderboardByNameCacheHitCount+"/"+leaderboardByNameCacheMissCount);
        LeaderboardDTO result = future.get();
        logger.fine("getLeaderboardByName("+leaderboard.getName()+", "+adjustedTimePoint+", "+namesOfRaceColumnsForWhichToLoadLegDetails+") took "+
                (System.currentTimeMillis()-startOfRequestHandling)+"ms");
        return result;
    }
}
