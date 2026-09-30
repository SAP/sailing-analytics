package com.sap.sailing.polars.mining;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import org.apache.commons.math.analysis.polynomials.PolynomialFunction;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.impl.SpeedWithBearingWithConfidenceImpl;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.PolarSheetGenerationSettings;
import com.sap.sailing.domain.common.PolarSheetsData;
import com.sap.sailing.domain.common.PolarSheetsHistogramData;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.impl.PolarSheetsDataImpl;
import com.sap.sailing.domain.common.impl.PolarSheetsHistogramDataImpl;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.polars.PolarsChangedListener;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.polars.impl.CubicEquation;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Speed;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.datamining.components.FilterCriterion;
import com.sap.sse.datamining.components.Processor;
import com.sap.sse.datamining.data.ClusterGroup;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.functions.ParameterProvider;
import com.sap.sse.datamining.functions.ParameterizedFunction;
import com.sap.sse.datamining.impl.components.GroupedDataEntry;
import com.sap.sse.datamining.impl.components.ParallelFilteringProcessor;
import com.sap.sse.datamining.impl.components.ParallelMultiDimensionsValueNestingGroupingProcessor;
import com.sap.sse.datamining.impl.functions.SimpleParameterizedFunction;
import com.sap.sse.util.ThreadPoolUtil;
import com.sap.sse.util.impl.ThreadFactoryWithPriority;

/**
 * Entry point for the aggregation of backend polar data and backend to that data.
 * <p>
 * 
 * Creates a polar data pipeline upon creation and puts incoming GPS fixes into that pipeline. Also holds references to
 * the actual data containers in which the aggregation results lay.
 * <p>
 * 
 * For more information on polars in SAP Sailing Analytics, please see:
 * <a href="https://wiki.sapsailing.com/wiki/howto/misc/polars">https://wiki.sapsailing.com/wiki/howto/misc/polars</a>
 * 
 * @author D054528 (Frederik Petersen)
 *
 */
public class PolarDataMiner {

    private static final int EXECUTOR_QUEUE_SIZE = 100;
    private static final int THREAD_POOL_SIZE = ThreadPoolUtil.INSTANCE.getReasonableThreadPoolSize();
    private final ThreadPoolExecutor executor = createExecutor();
    private static final ScheduledExecutorService processRacesThatFinishedLoadingExecutor = ThreadPoolUtil.INSTANCE
            .createBackgroundTaskThreadPoolExecutor(1,
                    PolarDataMiner.class.getName() + " processRacesThatFinishedLoadingExecutor");
    private final Map<BoatClass, AtomicInteger> stats = new ConcurrentHashMap<>();

    private final Queue<GPSFixMovingWithOriginInfo> fixQueue = new ConcurrentLinkedQueue<GPSFixMovingWithOriginInfo>();

    private static final Logger logger = Logger.getLogger(PolarDataMiner.class.getSimpleName());

    private final ConcurrentMap<BoatClass, Set<PolarsChangedListener>> listeners = new ConcurrentHashMap<>();

    /**
     * Coordinates callbacks registered through {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} and
     * {@link #raceFinishedLoading(TrackedRace, Runnable)} such that the callback for a given race is only registered on
     * the {@link #preFilteringProcessorForLoadedFixes loading pipeline's} drain <em>after</em> the race's fixes have
     * been queued into that pipeline. Otherwise, a caller of {@code runWhenPolarLoadingFinishedFor} could register a
     * drain callback while the pipeline is momentarily idle (before ingestion for that race started), and the callback
     * would fire immediately, before this race's fixes had even entered the pipeline.
     * <p>
     *
     * Semantics: the map is keyed by races whose fixes have <em>not yet</em> been queued into the loading pipeline.
     * Values are lists of callbacks parked pending that ingestion. When ingestion for a race completes queueing all its
     * fixes (see {@link #raceFinishedLoading(TrackedRace, Runnable)}), the race is removed from this map and all parked
     * callbacks, together with any callback passed to that {@code raceFinishedLoading} call itself, are registered on
     * the drain of {@link #preFilteringProcessorForLoadedFixes}. A race is added to this map by
     * {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} the first time a callback is registered for it
     * before its ingestion has started; subsequent {@code
     * runWhenPolarLoadingFinishedFor} calls for a race present in this map append to its list. Callers of
     * {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} that arrive <em>after</em> ingestion has completed
     * queueing (race not present in this map at that time) register their callback on the drain directly. See bug6241.
     * <p>
     *
     * Keyed strongly ({@link HashMap}). An entry pins its {@link TrackedRace} key while callbacks
     * are parked for it, which is intentional: the parked {@link Runnable}s typically capture the
     * same {@code race} strongly anyway (e.g. the wind-estimation install closure in
     * {@code RacingEventServiceImpl.scheduleWindEstimationInstallation}), so weak-keying this map
     * would be defeated by its own values and give a false sense of safety. Entries are removed
     * for live races in {@link #raceFinishedLoading} (once the parked callbacks are handed off to
     * the drain) and, for races that are removed before their fixes were ever ingested, by
     * {@link #raceRemoved(TrackedRace)}. Callers of
     * {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} are contractually required to
     * arrange for {@link #raceRemoved(TrackedRace)} to be called when the race goes away; see that
     * method's contract.
     */
    private final Map<TrackedRace, List<Runnable>> callbacksWaitingForFixIngestion = new HashMap<>();

    /**
     * Records races whose fixes have been fully queued into {@link #preFilteringProcessorForLoadedFixes}. Once a race
     * is in this set, a caller of {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} for that race
     * registers its callback on the drain directly (rather than parking it in {@link #callbacksWaitingForFixIngestion})
     * provided {@link #loadingOfAllRacesToRestoreStarted} is already {@code true}. Guarded by the monitor of
     * {@link #callbacksWaitingForFixIngestion}.
     * <p>
     *
     * There is no natural point during normal operation at which an entry could be removed, because a
     * {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} call for a race may legitimately arrive long after
     * ingestion (e.g. when a wind-estimation factory swap reschedules the install per race). To avoid pinning every
     * {@link TrackedRace} ever loaded -- and transitively all of its tracks -- for the lifetime of this miner (a real
     * leak on long-running ARCHIVE servers that load tens of thousands of races), this set is held weakly
     * ({@link Collections#newSetFromMap(Map) Collections.newSetFromMap(}{@link WeakHashMap
     * new WeakHashMap<>())}). Unlike {@link #callbacksWaitingForFixIngestion} this set has no values, so nothing
     * defeats the weak keys: an entry disappears once the race is no longer strongly reachable anywhere else, at which
     * point no further {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} call for it can occur anyway, so
     * losing the "already ingested" bit is harmless. As a belt-and-suspenders measure the entry is also removed
     * eagerly in {@link #raceRemoved(TrackedRace)} rather than waiting for garbage collection. For any race that is
     * still alive, the entry remains and the gate keeps working exactly as before. See bug6241.
     */
    private final Set<TrackedRace> racesWithIngestedFixes = Collections.newSetFromMap(new WeakHashMap<>());

    /**
     * Set by {@link #markLoadingOfAllRacesToRestoreStarted()} to signal that the caller (typically
     * {@code RacingEventServiceImpl.restoreTrackedRaces()}) has finished the enumeration loop that triggers loading for
     * every race to be restored during startup. It does <em>not</em> imply that all those races have already progressed
     * past {@code LOADING}: some may still be loading, some may take a long time, some may never leave {@code LOADING}
     * at all. The flag only signals that no <em>new</em> startup races will show up unannounced.
     * <p>
     *
     * Before this flag is set, the {@link #preFilteringProcessorForLoadedFixes loading pipeline} can transiently be
     * idle (counter==0) between two races' ingestion bursts; registering a drain callback in such a window would fire
     * it immediately, before other startup races have had a chance to feed fixes into the pipeline. Once this flag is
     * set, any pipeline idle window is a genuine drain of everything that has been ingested so far. Combined with the
     * {@link #racesWithIngestedFixes} gate, a callback for a specific race only fires once <em>that race's</em> fixes
     * have made it in <em>and</em> the pipeline has drained everything ingested up to that point.
     * <p>
     *
     * Callbacks registered via {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} whose race has already
     * been ingested but which arrive while this flag is still {@code false} are parked in
     * {@link #callbacksWaitingForLoadingOfAllRacesToRestoreToStart} until the flag flips.
     * <p>
     *
     * The flag is <em>initialized</em> to {@code false} only when this miner was constructed with
     * {@code waitForLoadingOfAllRacesToRestoreToBeStarted == true}. Otherwise (the default) it is initialized to
     * {@code true} so that the second gate is effectively bypassed and this miner behaves exactly as before bug6241's
     * second-gate addition; this suits ad-hoc clients and tests that instantiate a miner outside a startup-restore flow
     * and never call {@link #markLoadingOfAllRacesToRestoreStarted()}. See bug6241.
     */
    private volatile boolean loadingOfAllRacesToRestoreStarted;

    /**
     * Callbacks whose race has already been fully ingested but which arrived before
     * {@link #markLoadingOfAllRacesToRestoreStarted()} was called. They are held here until the flag flips, at which
     * point each is registered on {@link #preFilteringProcessorForLoadedFixes}'s drain. Guarded by the monitor of
     * {@link #callbacksWaitingForFixIngestion}.
     * <p>
     *
     * Lifecycle / leak-safety: this list is bounded and self-clearing under normal operation --
     * {@link #markLoadingOfAllRacesToRestoreStarted()} drains it fully and clears it exactly once, moving every parked
     * callback onto the pipeline drain. It is not keyed by {@link TrackedRace} and does not itself pin any race
     * (individual callbacks may still capture a race, but only transiently, until the drain fires). The only way it
     * can retain callbacks indefinitely is if {@link #markLoadingOfAllRacesToRestoreStarted()} is never called on a
     * miner constructed in gated mode -- i.e. a broken startup contract on the client side, not a per-race leak. Once
     * the flag is set, further callbacks bypass this list and go straight to the drain (see
     * {@link #registerOnDrainOrWaitForRestoreStart(Runnable)}), so the list stays empty thereafter. Because it is not
     * race-keyed, {@link #raceRemoved(TrackedRace)} does not prune it; a race removed after its callback landed here
     * but before the flag flips will simply have its (now moot) callback fire once on the next drain.
     */
    private final List<Runnable> callbacksWaitingForLoadingOfAllRacesToRestoreToStart = new ArrayList<>();

    /**
     * Snapshot of the constructor argument. Used only to distinguish a redundant call to
     * {@link #markLoadingOfAllRacesToRestoreStarted()} on a gated miner (worth a WARN, because the client's contract is
     * to call exactly once) from a call on a non-gated miner (silently ignored, because the client didn't request
     * gating).
     */
    private final boolean waitForLoadingOfAllRacesToRestoreToBeStarted;

    /**
     * Entry point to the data mining pipeline for incremental updates, usually by races in status
     * {@link TrackedRaceStatusEnum#TRACKING}. It receives its data through the
     * {@link #addFix(GPSFixMoving, Competitor, TrackedRace)} method and targets the same terminal processors
     * {@link #cubicRegressionPerCourseProcessor} and {@link #speedRegressionPerAngleClusterProcessor} that the
     * {@link #preFilteringProcessorForLoadedFixes} targets.
     */
    private ParallelFilteringProcessor<GPSFixMovingWithOriginInfo> preFilteringProcessor;

    /**
     * Entry point to the data mining pipeline for bulk updates during loading races, usually from races in status
     * {@link TrackedRaceStatusEnum#LOADING}. It receives its data through the
     * {@link #raceFinishedLoading(TrackedRace, Runnable)} method and targets the same terminal processors
     * {@link #cubicRegressionPerCourseProcessor} and {@link #speedRegressionPerAngleClusterProcessor} that the
     * {@link #preFilteringProcessor} targets.
     */
    private ParallelFilteringProcessor<GPSFixMovingWithOriginInfo> preFilteringProcessorForLoadedFixes;

    private final PolarSheetGenerationSettings backendPolarSheetGenerationSettings;

    /**
     * This processor uses two cubic regressions angle to the true wind over windspeed and boatspeed over windspeed for
     * each course (legtype tack combination)
     */
    private final CubicRegressionPerCourseProcessor cubicRegressionPerCourseProcessor;

    private final SpeedRegressionPerAngleClusterProcessor speedRegressionPerAngleClusterProcessor;
    private final ClusterGroup<Bearing> angleClusterGroup;

    private ThreadPoolExecutor createExecutor() {
        return new ThreadPoolExecutor(THREAD_POOL_SIZE, THREAD_POOL_SIZE, 60l, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(EXECUTOR_QUEUE_SIZE), new ThreadFactoryWithPriority(PolarDataMiner.class.getSimpleName(),
                        Thread.NORM_PRIORITY-1, /* daemon */true)) {
            @Override
            protected void afterExecute(Runnable r, Throwable t) {
                super.afterExecute(r, t);
                GPSFixMovingWithOriginInfo fix;
                while (this.getQueue().size() < (EXECUTOR_QUEUE_SIZE / 10) && (fix=fixQueue.poll()) != null) {
                    preFilteringProcessor.processElement(fix);
                }
            }
        };
    }

    /**
     * Convenience constructor equivalent to calling
     * {@link #PolarDataMiner(PolarSheetGenerationSettings, CubicRegressionPerCourseProcessor, SpeedRegressionPerAngleClusterProcessor, ClusterGroup, boolean)}
     * with {@code waitForLoadingOfAllRacesToRestoreToBeStarted == false}. Callbacks registered via
     * {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} then only wait for the specific race's fixes to
     * have been queued into the loading pipeline, not for a subsequent signal from the client. Suited for ad-hoc uses,
     * tests, and other flows that don't have a distinguished "startup restore" phase driving many races through this
     * miner.
     */
    public PolarDataMiner(PolarSheetGenerationSettings backendPolarSettings,
            CubicRegressionPerCourseProcessor cubicRegressionPerCourseProcessor,
            SpeedRegressionPerAngleClusterProcessor speedRegressionPerAngleClusterProcessor,
            ClusterGroup<Bearing> angleClusterGroup) {
        this(backendPolarSettings, cubicRegressionPerCourseProcessor,
                speedRegressionPerAngleClusterProcessor, angleClusterGroup,
                /* waitForLoadingOfAllRacesToRestoreToBeStarted */ false);
    }

    /**
     * @param waitForLoadingOfAllRacesToRestoreToBeStarted
     *            when {@code true}, this miner enters the "gated" mode described on
     *            {@link #loadingOfAllRacesToRestoreStarted}: callbacks registered via
     *            {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)} will not fire until the client has
     *            explicitly called {@link #markLoadingOfAllRacesToRestoreStarted()} <em>and</em> the specific race's
     *            fixes have made it into the loading pipeline. This is the mode used by the OSGi/production wiring,
     *            where {@code RacingEventServiceImpl} makes that promise. Constructing with {@code true} without ever
     *            calling {@code markLoadingOfAllRacesToRestoreStarted()} will result in callbacks being held
     *            indefinitely. When {@code false} (the default via the shorter constructor), the second gate is
     *            bypassed, matching the behavior before bug6241's addition.
     */
    public PolarDataMiner(PolarSheetGenerationSettings backendPolarSettings,
            CubicRegressionPerCourseProcessor cubicRegressionPerCourseProcessor,
            SpeedRegressionPerAngleClusterProcessor speedRegressionPerAngleClusterProcessor,
            ClusterGroup<Bearing> angleClusterGroup,
            boolean waitForLoadingOfAllRacesToRestoreToBeStarted) {
        cubicRegressionPerCourseProcessor.setListeners(listeners);
        speedRegressionPerAngleClusterProcessor.setListeners(listeners);
        backendPolarSheetGenerationSettings = backendPolarSettings;
        this.cubicRegressionPerCourseProcessor = cubicRegressionPerCourseProcessor;
        this.speedRegressionPerAngleClusterProcessor = speedRegressionPerAngleClusterProcessor;
        this.angleClusterGroup = angleClusterGroup;
        this.waitForLoadingOfAllRacesToRestoreToBeStarted = waitForLoadingOfAllRacesToRestoreToBeStarted;
        this.loadingOfAllRacesToRestoreStarted = !waitForLoadingOfAllRacesToRestoreToBeStarted;
        try {
            setUpWorkflow();
        } catch (ClassCastException | NoSuchMethodException | SecurityException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    
    public PolarDataMiner filterToBoatClasses(Iterable<BoatClass> boatClasses) {
        return new PolarDataMiner(backendPolarSheetGenerationSettings,
                                  cubicRegressionPerCourseProcessor.filterToBoatClasses(boatClasses),
                                  speedRegressionPerAngleClusterProcessor.filterToBoatClasses(boatClasses),
                                  angleClusterGroup);
    }

    /**
     * Creates a data mining workflow from processors that start with filtering fixes for best competitors in a race,
     * then enriching the fixes with context data required for further processing in the data mining pipeline,
     * {@link PolarDataDimensionCollectionFactory grouping} and then passing into
     * {@link #cubicRegressionPerCourseProcessor} and {@link #speedRegressionPerAngleClusterProcessor}, respectively.<p>
     * 
     * This can be used to establish two separate pipelines ending at the same two processors, namely
     * {@link #preFilteringProcessor} and {@link #preFilteringProcessorForLoadedFixes}, so that clients
     * can wait separately for the processing of those fixes introduced by loading races, as compared to
     * the continuous processing happening for live races.
     */
    private ParallelFilteringProcessor<GPSFixMovingWithOriginInfo> createWorkflow() throws ClassCastException, NoSuchMethodException, SecurityException {
        final Collection<Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, ?>> regressionPerCourseGrouperResultReceivers = new ArrayList<Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, ?>>();
        regressionPerCourseGrouperResultReceivers.add(cubicRegressionPerCourseProcessor);
        final Collection<ParameterizedFunction<?>> parameterizedDimensionsForCubicRegression = new ArrayList<>();
        for (Function<?> function : PolarDataDimensionCollectionFactory
                .getCubicRegressionPerCourseClusterKeyDimensions()) {
            parameterizedDimensionsForCubicRegression.add(new SimpleParameterizedFunction<>(function,
                    ParameterProvider.NULL));
        }
        final Processor<GPSFixMovingWithPolarContext, GroupedDataEntry<GPSFixMovingWithPolarContext>> cubicRegressionPerCourseGroupingProcessor = new ParallelMultiDimensionsValueNestingGroupingProcessor<GPSFixMovingWithPolarContext>(
                GPSFixMovingWithPolarContext.class, executor, regressionPerCourseGrouperResultReceivers,
                parameterizedDimensionsForCubicRegression);
        final Collection<Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, ?>> regressionPerAngleClusterGrouperResultReceivers = new ArrayList<Processor<GroupedDataEntry<GPSFixMovingWithPolarContext>, ?>>();
        regressionPerAngleClusterGrouperResultReceivers.add(speedRegressionPerAngleClusterProcessor);
        final Collection<ParameterizedFunction<?>> parameterizedDimensionsForRegressionPerAngleCluster = new ArrayList<>();
        for (Function<?> function : PolarDataDimensionCollectionFactory
                .getSpeedRegressionPerAngleClusterClusterKeyDimensions()) {
            parameterizedDimensionsForRegressionPerAngleCluster.add(new SimpleParameterizedFunction<>(function,
                    ParameterProvider.NULL));
        }
        final Processor<GPSFixMovingWithPolarContext, GroupedDataEntry<GPSFixMovingWithPolarContext>> regressionPerAngleClusterGroupingProcessor = new ParallelMultiDimensionsValueNestingGroupingProcessor<GPSFixMovingWithPolarContext>(
                GPSFixMovingWithPolarContext.class, executor, regressionPerAngleClusterGrouperResultReceivers,
                parameterizedDimensionsForRegressionPerAngleCluster);
        final Collection<Processor<GPSFixMovingWithPolarContext, ?>> filteringResultReceivers = new ArrayList<>();
        filteringResultReceivers.add(cubicRegressionPerCourseGroupingProcessor);
        filteringResultReceivers.add(regressionPerAngleClusterGroupingProcessor);
        final Processor<GPSFixMovingWithPolarContext, GPSFixMovingWithPolarContext> filteringProcessor = new ParallelFilteringProcessor<GPSFixMovingWithPolarContext>(
                GPSFixMovingWithPolarContext.class, executor, filteringResultReceivers, new PolarFixFilterCriteria(
                        backendPolarSheetGenerationSettings.getPctOfLeadingCompetitorsToInclude()));
        final Collection<Processor<GPSFixMovingWithPolarContext, ?>> enrichingResultReceivers = Arrays.asList(filteringProcessor);
        final AbstractEnrichingProcessor<GPSFixMovingWithOriginInfo, GPSFixMovingWithPolarContext> enrichingProcessor = new AbstractEnrichingProcessor<GPSFixMovingWithOriginInfo, GPSFixMovingWithPolarContext>(
                GPSFixMovingWithOriginInfo.class, GPSFixMovingWithPolarContext.class, executor,
                enrichingResultReceivers) {
            @Override
            protected GPSFixMovingWithPolarContext enrich(GPSFixMovingWithOriginInfo element) {
                GPSFixMovingWithPolarContext result = null;
                result = new GPSFixMovingWithPolarContext(element.getFix(), element.getTrackedRace(),
                        element.getCompetitor(), angleClusterGroup);
                return result;
            }
        };
        final Collection<Processor<GPSFixMovingWithOriginInfo, ?>> preFilterResultReceivers = Arrays.asList(enrichingProcessor);
        return new ParallelFilteringProcessor<GPSFixMovingWithOriginInfo>(
                GPSFixMovingWithOriginInfo.class, executor, preFilterResultReceivers,
                new FilterCriterion<GPSFixMovingWithOriginInfo>() {
                    @Override
                    public boolean matches(GPSFixMovingWithOriginInfo element) {
                        boolean result = false;
                        if (PolarFixFilterCriteria.isInLeadingCompetitors(element.getTrackedRace(),
                                element.getCompetitor(),
                                backendPolarSheetGenerationSettings.getPctOfLeadingCompetitorsToInclude())) {
                            result = true;
                            final BoatClass boatClass = element.getBoat().getBoatClass();
                            AtomicInteger count = stats.get(boatClass);
                            if (count == null) {
                                count = new AtomicInteger(1);
                                stats.put(boatClass, count);
                            } else {
                                count.getAndIncrement();
                            }
                        }
                        return result;
                    }

                    @Override
                    public Class<GPSFixMovingWithOriginInfo> getElementType() {
                        return GPSFixMovingWithOriginInfo.class;
                    }
                });
    }

    private void setUpWorkflow() throws ClassCastException, NoSuchMethodException, SecurityException {
        preFilteringProcessor = createWorkflow();
        preFilteringProcessorForLoadedFixes = createWorkflow();
    }

    public void addFix(GPSFixMoving fix, Competitor competitor, TrackedRace trackedRace) {
        // don't process fixes while LOADING because wind data is loading at the same time, and
        // unpredictable results may occur due to this
        if (trackedRace.getStatus().getStatus() != TrackedRaceStatusEnum.LOADING) {
            GPSFixMovingWithOriginInfo fixWithOriginInfo = new GPSFixMovingWithOriginInfo(fix, trackedRace, competitor);
            processFix(trackedRace, fixWithOriginInfo);
        }
    }

    private void processFix(TrackedRace trackedRace, GPSFixMovingWithOriginInfo fixWithOriginInfo) {
        if (executor.getQueue().size() >= EXECUTOR_QUEUE_SIZE / 10) { // in this case synchronous execution becomes likely, but
            // we are in a synchronous callback and don't want to spend too much time here in this foreground thread; queue it!
            fixQueue.add(fixWithOriginInfo);
        } else {
            preFilteringProcessor.processElement(fixWithOriginInfo);
        }
    }

    public boolean isCurrentlyActiveOrHasQueue() {
        boolean isActive = executor.getActiveCount() > 0;
        boolean hasQueue = executor.getQueue().size() > 0;
        return isActive || hasQueue;
    }

    /**
     * @param boatClass
     * @param windSpeed
     * @param trueWindAngle
     * @param useLinearRegression
     *            if true uses lin. regression in the wind interval, otherwise arithm. mean
     * @return
     * @throws NotEnoughDataHasBeenAddedException
     */
    public SpeedWithConfidence<Void> estimateBoatSpeed(BoatClass boatClass, Speed windSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        return speedRegressionPerAngleClusterProcessor.estimateBoatSpeed(boatClass, windSpeed, trueWindAngle);
    }
    
    public Pair<List<Speed>, Double> estimateWindSpeeds(BoatClass boatClass, Speed boatSpeed, Bearing trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        LegType legType;
        if (trueWindAngle.getDegrees() < 70) {
            legType = LegType.UPWIND;
        } else if (trueWindAngle.getDegrees() < 120) {
            legType = LegType.REACHING;
        } else {
            legType = LegType.DOWNWIND;
        }
        Set<SpeedWithBearingWithConfidence<Void>> resultSet = cubicRegressionPerCourseProcessor
                .estimateTrueWindSpeedAndAngleCandidates(boatClass, boatSpeed, legType, Tack.STARBOARD);
        double referenceTwsKnots = 10;
        if (!resultSet.isEmpty()) {
            double bestTwsKnots = Double.MAX_VALUE;
            for (SpeedWithBearingWithConfidence<Void> speedWithBearingWithConfidence : resultSet) {
                double twsKnots = speedWithBearingWithConfidence.getObject().getKnots();
                if (twsKnots > 2 && twsKnots < 20 && Math.abs(10 - twsKnots) < Math.abs(10 - bestTwsKnots)) {
                    bestTwsKnots = twsKnots;
                }
            }
            if (bestTwsKnots > 2 && bestTwsKnots < 20) {
                referenceTwsKnots = bestTwsKnots;
            }
        }
        return speedRegressionPerAngleClusterProcessor.estimateWindSpeeds(boatClass, boatSpeed, trueWindAngle,
                referenceTwsKnots);
    }

    public Set<SpeedWithBearingWithConfidence<Void>> estimateTrueWindSpeedAndAngleCandidates(BoatClass boatClass,
            Speed speedOverGround, LegType legType, Tack tack) {
        Set<SpeedWithBearingWithConfidence<Void>> resultSet = cubicRegressionPerCourseProcessor
                .estimateTrueWindSpeedAndAngleCandidates(boatClass, speedOverGround, legType, tack);
        if (resultSet.isEmpty()) {
            // FALLBACK function if no data was available
            resultSet = getAverageTrueWindSpeedAndAngleCandidatesWithFallbackFunction(boatClass, speedOverGround,
                    legType, tack);
        }
        return resultSet;
    }

    private Set<SpeedWithBearingWithConfidence<Void>> getAverageTrueWindSpeedAndAngleCandidatesWithFallbackFunction(
            BoatClass boatClass, Speed speedOverGround, LegType legType, Tack tack) {

        // The following is an estimation function. It only serves as a fallback. It's the same for all boatclasses and
        // returns
        // default maneuver angles.
        // The function is able to return boat speed values for windspeed values between 5kn and 25kn , which are some
        // kind of realistic
        // for sailing boats. They are taken from the 505 polars we gathered in the races until now.

        Set<SpeedWithBearingWithConfidence<Void>> resultSet = new HashSet<>();
        final int tackFactor = (tack.equals(Tack.PORT)) ? -1 : 1;
        if (legType.equals(LegType.UPWIND)) {
            CubicEquation upWindEquation = new CubicEquation(0.0002, -0.0245, 0.7602, -0.0463
                    - speedOverGround.getKnots());
            int angle = 49 * tackFactor;
            solveAndAddResults(resultSet, upWindEquation, angle);
        } else if (legType.equals(LegType.DOWNWIND)) {
            CubicEquation downWindEquation = new CubicEquation(0.0003, -0.0373, 1.5213, -2.1309
                    - speedOverGround.getKnots());
            int angle = 150 * tackFactor;
            solveAndAddResults(resultSet, downWindEquation, angle);
        }
        return resultSet;
        // return polarDataMiner.estimateTrueWindSpeedAndAngleCandidates(boatClass, speedOverGround, legType, tack);
    }

    private void solveAndAddResults(Set<SpeedWithBearingWithConfidence<Void>> result, CubicEquation equation, int angle) {
        double[] windSpeedCandidates = equation.solve();
        for (int i = 0; i < windSpeedCandidates.length; i++) {
            double windSpeedCandidateInKnots = windSpeedCandidates[i] > 0 ? windSpeedCandidates[i] : 0;
            if (windSpeedCandidateInKnots < 40) {
                result.add(new SpeedWithBearingWithConfidenceImpl<Void>(new KnotSpeedWithBearingImpl(
                        windSpeedCandidateInKnots, new DegreeBearingImpl(angle)), 0.00001, null));
            }
        }
    }

    public PolarSheetsData createFullSheetForBoatClass(BoatClass boatClass) {
        double[] defaultWindSpeeds = backendPolarSheetGenerationSettings.getWindSpeedStepping().getRawStepping();
        Number[][] averagedPolarDataByWindSpeed = new Number[defaultWindSpeeds.length][360];

        Map<Integer, Integer[]> dataCountPerAngleForWindspeed = new HashMap<>();
        Map<Integer, Map<Integer, PolarSheetsHistogramData>> histogramDataMap = new HashMap<>();

        int totalDataCount = 0;

        for (int windIndex = 0; windIndex < defaultWindSpeeds.length; windIndex++) {
            Double windSpeed = defaultWindSpeeds[windIndex];
            Integer[] perAngle = new Integer[360];
            Map<Integer, PolarSheetsHistogramData> perWindSpeed = new HashMap<>();
            for (int angle = 0; angle < 360; angle++) {
                SpeedWithConfidence<Void> speedWithConfidence;
                try {
                    int convertedAngle = convertAngleIfNecessary(angle);
                    SpeedWithConfidence<Void> regressionResult = speedRegressionPerAngleClusterProcessor
                            .estimateBoatSpeed(boatClass, new KnotSpeedImpl(windSpeed), new DegreeBearingImpl(
                                    convertedAngle));
                    if (regressionResult.getConfidence() > 0.1) {
                        speedWithConfidence = regressionResult;
                    } else {
                        // Low confidence. So put in 0 speed for chart
                        speedWithConfidence = new SpeedWithConfidenceImpl<Void>(new KnotSpeedImpl(0),
                                regressionResult.getConfidence(), null);
                    }
                } catch (NotEnoughDataHasBeenAddedException e) {
                    // No data so put in a 0 speed with 0 confidence
                    speedWithConfidence = new SpeedWithConfidenceImpl<Void>(new KnotSpeedImpl(0), 0, null);
                }

                averagedPolarDataByWindSpeed[windIndex][angle] = speedWithConfidence.getObject().getKnots();
                int dataCount = 200; /* FIXME */

                totalDataCount = totalDataCount + dataCount;
                // FIXME hard coded
                double coefficiantOfVariation = 0.8;
                double confidenceMeasure = 0.5;

                PolarSheetsHistogramDataImpl polarSheetsHistogramDataImpl = createEmptyHistogramData(perAngle, angle,
                        dataCount, coefficiantOfVariation, confidenceMeasure);
                perWindSpeed.put(angle, polarSheetsHistogramDataImpl);
            }
            histogramDataMap.put(windIndex, perWindSpeed);
            dataCountPerAngleForWindspeed.put(windIndex, perAngle);
        }
        PolarSheetsData data = new PolarSheetsDataImpl(averagedPolarDataByWindSpeed, totalDataCount,
                dataCountPerAngleForWindspeed, backendPolarSheetGenerationSettings.getWindSpeedStepping(),
                histogramDataMap);
        return data;
    }

    private int convertAngleIfNecessary(int angle) {
        int convertedAngle = angle;
        if (angle > 180) {
            convertedAngle = angle - 360;
        }
        return convertedAngle;
    }

    private PolarSheetsHistogramDataImpl createEmptyHistogramData(Integer[] perAngle, int angle, int dataCount,
            double coefficiantOfVariation, double confidenceMeasure) {
        perAngle[angle] = dataCount;
        Number[] xValues = {};
        Number[] yValues = {};
        ;
        Map<String, Integer[]> yValuesByGaugeIds = new HashMap<>();
        Map<String, Integer[]> yValuesByDay = new HashMap<>();
        Map<String, Integer[]> yValuesByDayAndGaugeId = new HashMap<>();
        PolarSheetsHistogramDataImpl polarSheetsHistogramDataImpl = new PolarSheetsHistogramDataImpl(angle, xValues,
                yValues, yValuesByGaugeIds, yValuesByDay, yValuesByDayAndGaugeId, dataCount, coefficiantOfVariation);
        polarSheetsHistogramDataImpl.setConfidenceMeasure(confidenceMeasure);
        return polarSheetsHistogramDataImpl;
    }

    public Set<BoatClass> getAvailableBoatClasses() {
        return speedRegressionPerAngleClusterProcessor.getAvailableBoatClasses();
    }

    public int[] getDataCountsForWindSpeed(BoatClass boatClass, Speed windSpeed, int startAngleInclusive,
            int endAngleExclusive) {
        int[] dataCounts = new int[360];
        for (int angle = 0; angle < 360; angle++) {
            if (angle >= startAngleInclusive && angle < endAngleExclusive) {
                dataCounts[angle] = 0; /* FIXME */
            } else {
                dataCounts[angle] = -1;
            }
        }
        return dataCounts;
    }

    public SpeedWithBearingWithConfidence<Void> getAverageSpeedAndCourseOverGround(BoatClass boatClass,
            Speed windSpeed, LegType legType) throws NotEnoughDataHasBeenAddedException {
        SpeedWithBearingWithConfidence<Void> averageSpeedAndCourseOverGround = null;
        averageSpeedAndCourseOverGround = cubicRegressionPerCourseProcessor.getAverageSpeedAndCourseOverGround(
                boatClass, windSpeed, legType);
        return averageSpeedAndCourseOverGround;
    }

    public PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, LegType legType)
            throws NotEnoughDataHasBeenAddedException {
        return cubicRegressionPerCourseProcessor.getSpeedRegressionFunction(boatClass, legType);
    }

    public PolynomialFunction getAngleRegressionFunction(BoatClass boatClass, LegType legType)
            throws NotEnoughDataHasBeenAddedException {
        return cubicRegressionPerCourseProcessor.getAngleRegressionFunction(boatClass, legType);
    }

    public PolynomialFunction getSpeedRegressionFunction(BoatClass boatClass, double trueWindAngle)
            throws NotEnoughDataHasBeenAddedException {
        return speedRegressionPerAngleClusterProcessor.getSpeedRegressionFunction(boatClass, trueWindAngle);
    }

    /**
     * Ingests the fixes from all competitors into the "loading" pipeline. When that pipeline finishes processing
     * the fixes ingested by this and other calls on this {@link PolarDataMiner} instance, it invokes all the
     * {@code callbackWhenAllLoadedFixesHaveBeenProcessed} callbacks provided to this method.
     * 
     * @param callbackWhenAllLoadedFixesHaveBeenProcessed
     *            if not {@code null}, this callback will be {@link Runnable#run() invoked} when the processing of all
     *            fixes ingested into this {@link PolarDataMiner} through this method have been processed. This assumes
     *            that background processes from the {@link #executor} continue to handle fixes ingested by a single call,
     *            and more calls for more races may be accepted in the meantime, thus holding back the invocation of
     *            the callback until the ingestions from <em>all</em> races have been processed. This is made possible
     *            by entertaining two "frontend" data mining workflow processor chains, both leading to the same terminal
     *            processors ({@link #cubicRegressionPerCourseProcessor} and {@link #speedRegressionPerAngleClusterProcessor}),
     *            one filled by this method, the other by {@link #addFix(GPSFixMoving, Competitor, TrackedRace)} used by
     *            incremental updates.
     */
    public void raceFinishedLoading(final TrackedRace race, Runnable callbackWhenAllLoadedFixesHaveBeenProcessed) {
        processRacesThatFinishedLoadingExecutor.execute(()->{ // no Subject association necessary here
            logger.info("All queued fixes for newly loaded race will process now. "
                    + (race.getRace() != null ? race.getRace().getName() : race.getRaceIdentifier().getRaceName()));
            for (final Competitor competitor : race.getRace().getCompetitors()) {
                final GPSFixTrack<Competitor, GPSFixMoving> track = race.getTrack(competitor);
                // it is necessary to release the track's lock before calling processElement
                // because processElement will transitively cause obtaining the course lock,
                // and other methods will first obtain the course and then the track lock, leading
                // to a deadlock. See also bug 4297.
                final List<GPSFixMoving> fixes = new ArrayList<>();
                track.lockForRead();
                try {
                    for (final GPSFixMoving fix : track.getFixes()) {
                        fixes.add(fix);
                    }
                } finally {
                    track.unlockAfterRead();
                }
                for (final GPSFixMoving fix : fixes) {
                    preFilteringProcessorForLoadedFixes.processElement(new GPSFixMovingWithOriginInfo(fix, race, competitor));
                }
            }
            // All of this race's fixes have now been submitted to preFilteringProcessorForLoadedFixes.
            // Any callbacks parked in callbacksWaitingForFixIngestion for this race, along with the
            // one passed to this method (if any), can now safely be handed off to the
            // "wait for global drain" stage. See bug6241.
            final List<Runnable> callbacksToHandOff;
            synchronized (callbacksWaitingForFixIngestion) {
                racesWithIngestedFixes.add(race);
                callbacksToHandOff = callbacksWaitingForFixIngestion.remove(race);
            }
            if (callbacksToHandOff != null) {
                for (final Runnable parked : callbacksToHandOff) {
                    registerOnDrainOrWaitForRestoreStart(parked);
                }
            }
            if (callbackWhenAllLoadedFixesHaveBeenProcessed != null) {
                registerOnDrainOrWaitForRestoreStart(callbackWhenAllLoadedFixesHaveBeenProcessed);
            }
            logger.info("Finished injecting fixes for race "
                    + (race.getRace() != null ? race.getRace().getName() : race.getRaceIdentifier().getRaceName())
                    + "; stats: " + stats);
        });
    }

    /**
     * Registers {@code callback} to fire once the {@link #preFilteringProcessorForLoadedFixes
     * loading pipeline} has fully drained <em>globally</em> (i.e. the fixes of <em>every</em>
     * race ingested so far, not only {@code race}) <em>and</em> the caller has announced (via
     * {@link #markLoadingOfAllRacesToRestoreStarted()}) that no further startup races will be
     * added. The {@code race} parameter does not scope the wait to that race's fixes -- the
     * terminal processors are shared and there is no per-race completion tracking; it only gates
     * <em>when</em> the callback is allowed onto the shared drain (see the two conditions below).
     * Waiting on {@code race} therefore effectively waits for the whole loaded-fix backlog; this
     * "wait for everything" behavior is intended (we want the polar model complete before any
     * maneuver-based wind estimation is installed) even though it introduces some sequentiality
     * on a large cold start. Unlike {@link #raceFinishedLoading(TrackedRace, Runnable)}, this
     * method does <em>not</em> ingest the race's fixes into the pipeline; it only observes the
     * pipeline. It may be called any number of times for the same race, before or after
     * {@code raceFinishedLoading} has been called for that race, and before or after the
     * "loading started" signal has flipped.
     * <p>
     *
     * Firing is gated by two conditions, both of which must hold:
     * <ol>
     *   <li>{@code race}'s fixes have been fully queued into the pipeline (i.e.
     *   {@link #raceFinishedLoading} has ingested them and added the race to
     *   {@link #racesWithIngestedFixes}). This prevents firing while the pipeline is idle simply
     *   because this race's ingestion hasn't started yet.</li>
     *   <li>{@link #markLoadingOfAllRacesToRestoreStarted()} has been called. This prevents
     *   firing during a transient global idle window that occurs between two startup races'
     *   ingestion bursts.</li>
     * </ol>
     *
     * Depending on which of these already hold at the time of the call, {@code callback} is
     * either registered on the drain immediately (both hold), parked in
     * {@link #callbacksWaitingForLoadingOfAllRacesToRestoreToStart} (fix ingestion done, signal
     * pending), or parked in {@link #callbacksWaitingForFixIngestion} (fix ingestion pending;
     * once ingestion completes, the callback moves to the drain or to the signal-pending list
     * as appropriate). See bug6241.
     * <p>
     *
     * LEAK CONTRACT -- read before calling. A {@code callback} parked here (in
     * {@link #callbacksWaitingForFixIngestion}) is held strongly, keyed by {@code race}, until
     * either its race's fixes are ingested ({@link #raceFinishedLoading}) or the race is
     * explicitly forgotten. Parked callbacks also typically capture {@code race} strongly
     * themselves. Consequently, if a race is registered here but its fixes are never ingested
     * (e.g. it is removed while still in {@link TrackedRaceStatusEnum#LOADING}), the entry --
     * and the whole {@link TrackedRace} with all its tracks -- would be pinned for the lifetime
     * of this miner. To prevent that, the caller MUST call {@link #raceRemoved(TrackedRace)} when
     * the race is removed from its regatta / the racing event service. In this codebase that is
     * wired through {@code RacingEventServiceImpl.RaceAdditionListener.raceRemoved(TrackedRace)}.
     * Do not rely on garbage collection to clean up {@link #callbacksWaitingForFixIngestion}: its
     * keys are strong precisely because weak keys would be defeated by the callbacks' own
     * strong references back to the race.
     *
     * @param callback
     *            must not be {@code null}
     */
    public void runWhenPolarLoadingFinishedFor(final TrackedRace race, final Runnable callback) {
        if (callback == null) {
            throw new NullPointerException("callback must not be null");
        }
        final boolean fixIngestionAlreadyDone;
        synchronized (callbacksWaitingForFixIngestion) {
            if (racesWithIngestedFixes.contains(race)) {
                fixIngestionAlreadyDone = true;
            } else {
                fixIngestionAlreadyDone = false;
                callbacksWaitingForFixIngestion
                        .computeIfAbsent(race, r -> new ArrayList<>())
                        .add(callback);
            }
        }
        if (fixIngestionAlreadyDone) {
            registerOnDrainOrWaitForRestoreStart(callback);
        }
    }

    /**
     * Forgets all state this miner holds for {@code race}, so that a removed race and its tracks
     * can be garbage-collected rather than pinned for the lifetime of the miner. Specifically it
     * drops any callbacks still parked for {@code race} in {@link #callbacksWaitingForFixIngestion}
     * (the install those callbacks would drive is for the now-removed race instance, so it needn't
     * fire; note this does not abort any of {@code race}'s fixes that are already being drained
     * through the loading pipeline) and removes the race from {@link #racesWithIngestedFixes}.
     * <p>
     *
     * This is the removal side of the {@link #runWhenPolarLoadingFinishedFor(TrackedRace, Runnable)}
     * leak contract: because parked callbacks are held strongly and typically capture the race
     * strongly themselves, {@link #callbacksWaitingForFixIngestion} cannot rely on weak keys and
     * must be pruned explicitly when a race goes away. Callers (in this codebase,
     * {@code RacingEventServiceImpl.RaceAdditionListener.raceRemoved(TrackedRace)}) must invoke
     * this when a race is removed from its regatta / the racing event service.
     * {@link #racesWithIngestedFixes} is already held weakly and would clear on its own, but is
     * pruned here too as a belt-and-suspenders measure so the memory is reclaimed promptly rather
     * than at the next garbage collection.
     * <p>
     *
     * Idempotent and safe to call for a race this miner never saw: a race with no state simply
     * results in no-ops. Guarded by the same {@link #callbacksWaitingForFixIngestion} monitor as
     * the registration methods.
     *
     * @param race
     *            the race to forget; must not be {@code null}
     */
    public void raceRemoved(final TrackedRace race) {
        if (race == null) {
            throw new NullPointerException("race must not be null");
        }
        synchronized (callbacksWaitingForFixIngestion) {
            callbacksWaitingForFixIngestion.remove(race);
            racesWithIngestedFixes.remove(race);
        }
    }

    /**
     * Second-stage gate for callbacks whose race's fixes are already ingested (or whose fix-ingestion parking has just
     * been drained by {@link #raceFinishedLoading(TrackedRace, Runnable)}). If
     * {@link #loadingOfAllRacesToRestoreStarted} is already set the callback goes straight to the drain; otherwise it
     * is parked in {@link #callbacksWaitingForLoadingOfAllRacesToRestoreToStart} where
     * {@link #markLoadingOfAllRacesToRestoreStarted()} will pick it up. See bug6241.
     */
    private void registerOnDrainOrWaitForRestoreStart(final Runnable callback) {
        final boolean signalAlreadyGiven;
        synchronized (callbacksWaitingForFixIngestion) {
            if (loadingOfAllRacesToRestoreStarted) {
                signalAlreadyGiven = true;
            } else {
                signalAlreadyGiven = false;
                callbacksWaitingForLoadingOfAllRacesToRestoreToStart.add(callback);
            }
        }
        if (signalAlreadyGiven) {
            preFilteringProcessorForLoadedFixes.runWhenFinishedProcessing(callback);
        }
    }

    /**
     * Announces that the caller (typically {@code RacingEventServiceImpl.restoreTrackedRaces()}) has finished the
     * enumeration loop that triggers loading for every race to be restored during startup. From now on, any transient
     * idle window on {@link #preFilteringProcessorForLoadedFixes} is a genuine drain of everything that has been
     * ingested up to that point; there won't be surprise ingestion bursts from previously unknown startup races.
     * Callbacks that have been parked in {@link #callbacksWaitingForLoadingOfAllRacesToRestoreToStart} (because they
     * were queued for a race whose fixes were already ingested, but arrived before this signal) are moved onto the
     * drain now. Idempotent: subsequent calls are logged and ignored. See bug6241.
     */
    public void markLoadingOfAllRacesToRestoreStarted() {
        final Iterable<Runnable> toRegisterOnDrain;
        synchronized (callbacksWaitingForFixIngestion) {
            if (loadingOfAllRacesToRestoreStarted) {
                if (waitForLoadingOfAllRacesToRestoreToBeStarted) {
                    logger.warning("markLoadingOfAllRacesToRestoreStarted() called more than once; ignoring.");
                }
                // otherwise: this miner is running without the second gate; the call is a
                // harmless no-op (the client didn't request gating, so there's nothing parked).
                toRegisterOnDrain = Collections.emptyList();
            } else {
                loadingOfAllRacesToRestoreStarted = true;
                toRegisterOnDrain = new ArrayList<>(callbacksWaitingForLoadingOfAllRacesToRestoreToStart);
                callbacksWaitingForLoadingOfAllRacesToRestoreToStart.clear();
            }
        }
        for (final Runnable callback : toRegisterOnDrain) {
            preFilteringProcessorForLoadedFixes.runWhenFinishedProcessing(callback);
        }
    }

    public void registerListener(BoatClass boatClass, PolarsChangedListener listener) {
        Set<PolarsChangedListener> listenersForBoatClass = listeners.get(boatClass);
        if (listenersForBoatClass == null) {
            Map<PolarsChangedListener, Boolean> mapForConcurrency = new ConcurrentHashMap<>();
            listenersForBoatClass = Collections.newSetFromMap(mapForConcurrency);
            listeners.put(boatClass, listenersForBoatClass);
        }
        listenersForBoatClass.add(listener);
    }

    public void unregisterListener(BoatClass boatClass, PolarsChangedListener listener) {
        Set<PolarsChangedListener> listenersForBoatClass = listeners.get(boatClass);
        if (listenersForBoatClass != null) {
            listenersForBoatClass.remove(listener);
        }
    }

    public CubicRegressionPerCourseProcessor getCubicRegressionPerCourseProcessor() {
        return cubicRegressionPerCourseProcessor;
    }

    public SpeedRegressionPerAngleClusterProcessor getSpeedRegressionPerAngleClusterProcessor() {
        return speedRegressionPerAngleClusterProcessor;
    }

    public PolarSheetGenerationSettings getPolarSheetGenerationSettings() {
        return backendPolarSheetGenerationSettings;
    }
}
