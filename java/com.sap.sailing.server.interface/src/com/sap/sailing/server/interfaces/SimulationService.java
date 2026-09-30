package com.sap.sailing.server.interfaces;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutionException;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.PathType;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.SimulationResults;
import com.sap.sse.util.SmartFutureCache;
import com.sap.sse.util.ThreadPoolUtil;
import com.sap.sse.util.impl.NamedTracingScheduledThreadPoolExecutor;

public interface SimulationService {
    long getSimulationResultsVersion(LegIdentifier legIdentifier);
    
    /**
     * Tries to fetch a {@link SimulationResults} object for the race leg identified by {@code legIdentifier} from the
     * cache. If no such cache entry is found, {@code null} is returned, but the calculation of the cache entry is
     * triggered so that later calls for the same leg will return a valid result once the calculation has finished.
     * <p>
     * 
     * There is no guarantee about how long this may take as the {@link SimulationService} uses a
     * {@link SmartFutureCache} which in turn is based on the
     * {@link ThreadPoolUtil#getDefaultBackgroundTaskThreadPoolExecutor() default background executor} which during
     * certain phases in the life cycle of the application may be crowded with re-calculation jobs that can take minutes
     * or even hours to complete.
     * <p>
     * 
     * If a client would like to understand the situation regarding such {@code null} results better, a look into
     * {@link ThreadPoolUtil#getDefaultBackgroundTaskThreadPoolExecutor()}.{@link NamedTracingScheduledThreadPoolExecutor#getQueue()
     * getQueue()}.{@link BlockingQueue#size() size()} can help.
     */
    SimulationResults getSimulationResults(LegIdentifier legIdentifier);

    Map<PathType, Path> getAllPathsEvenTimed(SimulationParameters simuPars, long millisecondsStep)
            throws InterruptedException, ExecutionException;

    Iterable<BoatClass> getBoatClassesWithPolarData();

    /**
     * @param boatClass
     *            must be one that is in the result set of {@link #getBoatClassesWithPolarData()}. Otherwise,
     *            {@code null} will be returned.
     */
    PolarDiagram getPolarDiagram(BoatClass boatClass);

    /**
     * Obtains a boat class from the {@link RacingEventService} by name, using {@link RacingEventService#getBaseDomainFactory()} and its
     * {@link SharedDomainFactory#getBoatClass(String)} method.
     */
    BoatClass getBoatClass(String name);

    Optional<Integer> getTaskQueueSize();
}
