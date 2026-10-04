package com.sap.sailing.windestimation.integration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.windestimation.IncrementalWindEstimation;
import com.sap.sailing.domain.windestimation.TimePointAndPositionWithToleranceComparator;
import com.sap.sailing.domain.windestimation.WindTrackWithConfidenceForEachWindFixImpl;
import com.sap.sailing.windestimation.aggregator.hmm.GraphLevelInference;
import com.sap.sailing.windestimation.aggregator.msthmm.DistanceAndDurationAwareWindTransitionProbabilitiesCalculator;
import com.sap.sailing.windestimation.aggregator.msthmm.MstBestPathsCalculator;
import com.sap.sailing.windestimation.aggregator.msthmm.MstBestPathsCalculatorImpl;
import com.sap.sailing.windestimation.aggregator.msthmm.MstGraphExportHelper;
import com.sap.sailing.windestimation.aggregator.msthmm.MstGraphLevel;
import com.sap.sailing.windestimation.aggregator.msthmm.MstManeuverGraphGenerator.MstManeuverGraphComponents;
import com.sap.sailing.windestimation.data.ManeuverCategory;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sailing.windestimation.data.ManeuverWithEstimatedType;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimation;
import com.sap.sailing.windestimation.data.SimpleManeuverForEstimationImpl;
import com.sap.sailing.windestimation.data.SimpleManeuverWithEstimatedType;
import com.sap.sailing.windestimation.data.SimpleManeuverWithEstimatedTypeImpl;
import com.sap.sailing.windestimation.data.transformer.ManeuverForEstimationTransformer;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverClassifiersCache;
import com.sap.sailing.windestimation.model.regressor.twdtransition.GaussianBasedTwdTransitionDistributionCache;
import com.sap.sailing.windestimation.windinference.MiddleCourseBasedTwdCalculatorImpl;
import com.sap.sailing.windestimation.windinference.PolarsBasedTwsCalculatorImpl;
import com.sap.sailing.windestimation.windinference.WindTrackCalculator;
import com.sap.sailing.windestimation.windinference.WindTrackCalculatorImpl;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Implementation of wind estimator which is meant to be assigned to a tracked race instance to provide a wind track
 * with estimated wind. Under the hood, it makes use of Minimum Spanning Tree based HMM which aggregates the maneuver
 * type classifications results such that a plausible wind track comes out. It operates incrementally, which means that
 * it maintains a state which is specific to the tracked race it is assigned to. The state is updated with each
 * {@link #newManeuverSpotsDetected(Competitor, Iterable, TrackTimeInfo)} call. The incremental state is managed in
 * {@link #mstManeuverGraphGenerator} which is responsible for incremental Minimum Spanning Tree computation for
 * provided maneuvers.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class IncrementalMstHmmWindEstimationForTrackedRace implements IncrementalWindEstimation {
    private static final Logger logger = Logger.getLogger(IncrementalMstHmmWindEstimationForTrackedRace.class.getName());

    private static final double WIND_COURSE_TOLERANCE_IN_DEGREES_TO_IGNORE_FOR_REUSE = 1.0;

    /**
     * Used by {@link PreClassifiedUpdate} to apply the same maneuver-eligibility filter that the graph path applies via
     * {@link CompleteManeuverCurveToManeuverForEstimationConverter}: maneuvers whose direction change (on either the
     * main curve or the stable-speed-and-course boundaries) is not classified as {@link ManeuverCategory#REGULAR} are
     * excluded from wind-track contribution. Without this filter, the DB-load / re-adaptation hand-off would contribute
     * wind fixes for maneuvers that the graph path silently skips, causing the estimator's track to contain extras at
     * positions/timepoints the graph path never produces. See bug6241.
     */
    private static final ManeuverForEstimationTransformer maneuverEligibilityFilter = new ManeuverForEstimationTransformer();

    private final IncrementalMstManeuverGraphGenerator mstManeuverGraphGenerator;
    private final MstBestPathsCalculator bestPathsCalculator;
    private final WindTrackCalculator windTrackCalculator;
    private final Map<Pair<Position, TimePoint>, WindWithConfidence<Pair<Position, TimePoint>>> windTrackWithConfidences = new TreeMap<>(
            new TimePointAndPositionWithToleranceComparator());
    private final TrackedRace trackedRace;
    private final WindSource windSource;
    private final WindTrackWithConfidenceForEachWindFixImpl estimatedWindTrack;
    private final static Executor recalculator = ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor();

    /**
     * A pending update to be processed by a {@link GraphRecalculationTask}. Two flavors are supported:
     * {@link NewSpotsUpdate} (from {@link #newManeuverSpotsDetected(Competitor, Iterable, TrackTimeInfo)}) which feeds
     * raw {@link CompleteManeuverCurve}s through the MST/HMM graph before reconciliation, and
     * {@link PreClassifiedUpdate} (from {@link #alreadyClassifiedManeuversAvailable(Competitor, Iterable)}) which skips
     * the graph because the maneuvers already carry their type.
     */
    private interface PendingUpdate {
        void apply();
    }

    /**
     * Contains update requests scheduled by
     * {@link #newManeuverSpotsDetected(Competitor, Iterable, TrackTimeInfo)} and
     * {@link #alreadyClassifiedManeuversAvailable(Competitor, Iterable)}. Adding and removing
     * elements must {@code synchronize} on this
     * {@link IncrementalMstHmmWindEstimationForTrackedRace} object. When adding an element and
     * {@link #updateTask} is {@code null}, a new update task must be scheduled and assigned to
     * {@link #updateTask} while holding this object's monitor ({@code synchronized}). When
     * checking for the next element to be removed and then deciding to terminate and clear the
     * {@link #updateTask}, also this object's monitor must be held.
     */
    private final ConcurrentLinkedDeque<PendingUpdate> updateQueue;
    
    /**
     * A task that is scheduled with the {@link #recalculator} and is set to a non-{@code null} task if and only if one
     * or more update requests are enqueued in {@link #updateQueue} or the last update is still processing in the task.
     * Setting and clearing this field must {@code synchronize} on this
     * {@link IncrementalMstHmmWindEstimationForTrackedRace} instance, in conjunction with adding elements to
     * the {@link #updateQueue}.
     */
    private GraphRecalculationTask updateTask;

    public IncrementalMstHmmWindEstimationForTrackedRace(TrackedRace trackedRace, WindSource windSource,
            PolarDataService polarDataService, long millisecondsOverWhichToAverage,
            ManeuverClassifiersCache maneuverClassifiersCache,
            GaussianBasedTwdTransitionDistributionCache gaussianBasedTwdTransitionDistributionCache) {
        // bug6241: enforced by callers (WindEstimationFactoryServiceImpl and the
        // per-race installation coordination in RacingEventServiceImpl) so that the
        // estimator captures a live polar service. Without polars, classification and
        // wind-speed inference degrade to null and Dummy-based, which is what produced
        // the incorrect wind estimation from maneuvers described by this bug.
        if (polarDataService == null) {
            throw new IllegalArgumentException(
                    "polarDataService must not be null; see bug6241 and WindEstimationFactoryServiceImpl."
                            + "createIncrementalWindEstimationTrack for the required precondition on the tracked race.");
        }
        this.estimatedWindTrack = new WindTrackWithConfidenceForEachWindFixImpl(millisecondsOverWhichToAverage,
                WindSourceType.MANEUVER_BASED_ESTIMATION.getBaseConfidence(),
                WindSourceType.MANEUVER_BASED_ESTIMATION.useSpeed(),
                IncrementalMstHmmWindEstimationForTrackedRace.class.getSimpleName()+" "+
                trackedRace.getRaceIdentifier(), false, windTrackWithConfidences);
        this.updateQueue = new ConcurrentLinkedDeque<>();
        this.trackedRace = trackedRace;
        this.windSource = windSource;
        final DistanceAndDurationAwareWindTransitionProbabilitiesCalculator transitionProbabilitiesCalculator = new DistanceAndDurationAwareWindTransitionProbabilitiesCalculator(
                gaussianBasedTwdTransitionDistributionCache, true);
        this.mstManeuverGraphGenerator = new IncrementalMstManeuverGraphGenerator(
                new CompleteManeuverCurveToManeuverForEstimationConverter(trackedRace, polarDataService),
                transitionProbabilitiesCalculator, maneuverClassifiersCache);
        this.bestPathsCalculator = new MstBestPathsCalculatorImpl(transitionProbabilitiesCalculator);
        this.windTrackCalculator = new WindTrackCalculatorImpl(new MiddleCourseBasedTwdCalculatorImpl(),
                new PolarsBasedTwsCalculatorImpl(polarDataService));
    }

    @Override
    public WindTrack getWindTrack() {
        return estimatedWindTrack;
    }
    
    @Override
    public void waitUntilDone() throws InterruptedException {
        synchronized (this) {
            while (updateTask != null) {
                this.wait();
            }
        }
    }

    /**
     * In
     * {@link IncrementalMstHmmWindEstimationForTrackedRace#newManeuverSpotsDetected(Competitor, Iterable, TrackTimeInfo)}
     * a sequence of maneuvers has to be inserted into the
     * {@link IncrementalMstHmmWindEstimationForTrackedRace#mstManeuverGraphGenerator maneuver graph generator} before
     * updating the wind estimations based on maneuvers. Adding a maneuver spot to the maneuver graph generator
     * synchronizes on that generator and therefore doing the same for multiple competitors for the same race from
     * multiple threads will block all but one of those threads.
     * <p>
     * 
     * With this task, a separate queue ({@link IncrementalMstHmmWindEstimationForTrackedRace#updateQueue}) is used to
     * pull updates from that queue and add them to the maneuver graph generator, then processing the updates to
     * generate new wind estimations.
     * <p>
     * 
     * A task of this type repeats this until the queue is empty and then terminates. Trying to fetch the next update
     * from the queue, deciding whether to terminate this task, and adding the next update to the queue are all
     * synchronized on the enclosing {@link IncrementalMstHmmWindEstimationForTrackedRace} object, ensuring that exactly
     * one task exists for the enclosing instance whenever an update is enqueued or processing.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class GraphRecalculationTask implements Runnable {
        @Override
        public void run() {
            logger.fine(()->"This is a new recalculation task for "+trackedRace.getRaceIdentifier());
            PendingUpdate nextUpdate;
            do {
                synchronized (IncrementalMstHmmWindEstimationForTrackedRace.this) {
                    nextUpdate = updateQueue.poll();
                    if (nextUpdate == null) {
                        logger.fine(()->"No more updates enqueued for "+trackedRace.getRaceIdentifier()+"; terminating update task");
                        updateTask = null;
                        IncrementalMstHmmWindEstimationForTrackedRace.this.notifyAll();
                    }
                }
                if (nextUpdate != null) {
                    logger.fine(()->"Handling next update task for "+trackedRace.getRaceIdentifier()+"; still "+updateQueue.size()+" tasks in the queue");
                    nextUpdate.apply();
                }
            } while (nextUpdate != null);
        }
    }

    /**
     * The classic path used by incremental maneuver detection:
     * {@link IncrementalMstHmmWindEstimationForTrackedRace#newManeuverSpotsDetected(Competitor, Iterable, TrackTimeInfo)}
     * enqueues one of these per notification, and its {@link #apply()} runs the raw {@link CompleteManeuverCurve}s
     * through the MST/HMM graph before reconciling the resulting wind fixes into the estimated wind track.
     */
    private class NewSpotsUpdate implements PendingUpdate {
        private final Competitor competitor;
        private final Iterable<CompleteManeuverCurve> newManeuvers;
        private final TrackTimeInfo trackTimeInfo;

        NewSpotsUpdate(final Competitor competitor, final Iterable<CompleteManeuverCurve> newManeuvers,
                final TrackTimeInfo trackTimeInfo) {
            this.competitor = competitor;
            this.newManeuvers = newManeuvers;
            this.trackTimeInfo = trackTimeInfo;
        }

        @Override
        public void apply() {
            final MstManeuverGraphComponents graphComponents;
            for (final CompleteManeuverCurve newManeuverSpot : newManeuvers) {
                // The add(...) method on IncrementalMstManeuverGraphGenerator is synchronized on the one instance per race.
                // But this newManeuverSpotsDetected method may be called by separate threads for different competitors.
                // If the calculation takes long, many pooled threads may block, reducing throughput to sequential
                // processing. See also bug 5824. We therefore enqueue the CompleteManeuverCurve maneuver spots and
                // run at most a single task as long as there are maneuver spots in the queue. Only the addition and
                // removal of tasks from the queue is synchronized with the creation and termination of the task.
                mstManeuverGraphGenerator.add(competitor, newManeuverSpot, trackTimeInfo);
            }
            graphComponents = mstManeuverGraphGenerator.parseGraph();
            if (logger.isLoggable(Level.FINE)) {
                try {
                    final String canonicalTmpPath = File.createTempFile("maneuverExport_", ".json").getCanonicalPath();
                    logger.fine("Exporting the maneuver graph to file after updating it with new maneuver spots for competitor "+competitor
                            +"; visualize by running com.sap.sailing.windestimation.lab/python/mst_graph_visualizer_graphviz.py "+canonicalTmpPath+" output.pdf");
                    MstGraphExportHelper.exportToFile(graphComponents, mstManeuverGraphGenerator.getTransitionProbabilitiesCalculator(), canonicalTmpPath);
                } catch (IOException e) {
                    logger.log(Level.WARNING, "Exporting the maneuver graph to file failed", e);
                }
            }
            if (graphComponents != null) {
                final List<SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation>> maneuversWithEstimatedType = new ArrayList<>();
                final Iterable<GraphLevelInference<MstGraphLevel>> bestPath = bestPathsCalculator.getBestNodes(graphComponents);
                for (final GraphLevelInference<MstGraphLevel> inference : bestPath) {
                    final SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation> maneuverWithEstimatedType = new ManeuverWithEstimatedType(
                            inference.getGraphLevel().getManeuver(), inference.getGraphNode().getManeuverType(),
                            inference.getConfidence());
                    maneuversWithEstimatedType.add(maneuverWithEstimatedType);
                }
                Collections.sort(maneuversWithEstimatedType);
                applyManeuverClassificationsToWindTrack(maneuversWithEstimatedType);
            }
        }
    }

    /**
     * The DB-load / re-adaptation path used by
     * {@link IncrementalMstHmmWindEstimationForTrackedRace#alreadyClassifiedManeuversAvailable(Competitor, Iterable)}:
     * each maneuver already carries its {@link Maneuver#getType() type}, so we skip the MST/HMM graph and go straight
     * to converting them into wind fixes and adding them into the wind track. To stay consistent with what the graph
     * path would produce for the same race, this update applies the same maneuver-eligibility filter that
     * {@link CompleteManeuverCurveToManeuverForEstimationConverter#convertCleanManeuverSpotToManeuverForEstimation(CompleteManeuverCurve, CompleteManeuverCurve, CompleteManeuverCurve, Competitor, TrackTimeInfo)}
     * applies (via {@link ManeuverForEstimationTransformer#isManeuverEligibleForAnalysis(double, double)}), and uses the same
     * {@code middleCourse} accessor that the graph-path adapter uses (see
     * {@link ConvertableManeuverForEstimationAdapterForCompleteManeuverCurve#getMiddleCourse()}, which reads from
     * {@link Maneuver#getManeuverCurveWithStableSpeedAndCourseBoundaries()}).
     * <p>
     *
     * Unlike {@link NewSpotsUpdate}, this update is scheduled <em>per competitor</em>: the caller (e.g.
     * {@code TrackedRaceImpl.feedAlreadyKnownManeuversToWindEstimation}) fires one update per competitor with just that
     * competitor's maneuvers. Consequently, the corresponding {@code newWindTrack} produced by
     * {@link #windTrackCalculator} covers only that competitor's contribution -- not the whole race. The reconciliation
     * body of {@link #applyManeuverClassificationsToWindTrack} is designed for whole-race inputs (produced by the MST
     * graph across all competitors) and would remove all other competitors' fixes on every per-competitor call. This
     * update therefore uses the additive-only helper {@link #addManeuverClassificationsToWindTrack} which inserts
     * missing fixes without removing existing ones. See bug6241.
     */
    private class PreClassifiedUpdate implements PendingUpdate {
        private final Competitor competitor;
        private final Iterable<Maneuver> maneuvers;

        PreClassifiedUpdate(final Competitor competitor, final Iterable<Maneuver> maneuvers) {
            this.competitor = competitor;
            this.maneuvers = maneuvers;
        }

        @Override
        public void apply() {
            final BoatClass boatClass = trackedRace.getRace().getBoatClass();
            final List<SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation>> maneuversWithEstimatedType = new ArrayList<>();
            for (final Maneuver maneuver : maneuvers) {
                final ManeuverTypeForClassification classification = mapManeuverType(maneuver.getType());
                final boolean isEligible = classification != null
                        && maneuverEligibilityFilter.isManeuverEligibleForAnalysis(
                                maneuver.getMainCurveBoundaries().getDirectionChangeInDegrees(),
                                maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getDirectionChangeInDegrees());
                if (isEligible) {
                    // DB-loaded maneuvers are considered clean: they are the ones that were persisted
                    // after having been accepted as valid, non-penalty-circle maneuvers.
                    // middleCourse is read from the stable-speed-and-course boundaries to match the
                    // graph-path adapter (see the class-level Javadoc).
                    final SimpleManeuverForEstimation forEstimation = new SimpleManeuverForEstimationImpl(
                            maneuver.getTimePoint(), maneuver.getPosition(),
                            maneuver.getManeuverCurveWithStableSpeedAndCourseBoundaries().getMiddleCourse(),
                            maneuver.getSpeedWithBearingBefore(), maneuver.getSpeedWithBearingAfter(),
                            /* clean */ true, boatClass);
                    // Full confidence: these maneuvers came from the persistent cache with their
                    // type already resolved by a prior computation that had all inputs available.
                    maneuversWithEstimatedType.add(new SimpleManeuverWithEstimatedTypeImpl<>(forEstimation,
                            classification, /* confidence */ 1.0));
                }
            }
            if (!maneuversWithEstimatedType.isEmpty()) {
                Collections.sort(maneuversWithEstimatedType);
                logger.fine(()->"Feeding "+maneuversWithEstimatedType.size()+" pre-classified maneuvers from competitor "
                        +competitor+" into the wind estimation of race "+trackedRace.getRaceIdentifier());
                addManeuverClassificationsToWindTrack(maneuversWithEstimatedType);
            }
        }
    }

    /**
     * Turns a list of already-typed maneuvers into wind fixes via {@link #windTrackCalculator} and merges them into the
     * {@link #estimatedWindTrack} incrementally and consistently, so that afterwards the contents match the
     * newly-produced wind track. Extracted from the previous inline body of
     * {@code GraphRecalculationTask.updateGraphGenerator} so that both the {@link NewSpotsUpdate MST/HMM path} and the
     * {@link PreClassifiedUpdate DB-load path} share exactly the same reconciliation semantics.
     * <p>
     * FIXME bug6026: the "consistently" seems to be causing problems when a new wind estimation model is ingested.
     */
    private void applyManeuverClassificationsToWindTrack(
            final List<SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation>> maneuversWithEstimatedType) {
        final List<WindWithConfidence<Pair<Position, TimePoint>>> newWindTrack = windTrackCalculator
                .getWindTrackFromManeuverClassifications(maneuversWithEstimatedType);
        final Map<Pair<Position, TimePoint>, WindWithConfidence<Pair<Position, TimePoint>>> newWindTrackMap = new HashMap<>(
                newWindTrack.size());
        for (final WindWithConfidence<Pair<Position, TimePoint>> wind : newWindTrack) {
            newWindTrackMap.put(wind.getRelativeTo(), wind);
        }
        // bug6241: mutate the internal windTrackWithConfidences map under the write lock, but
        // fire the corresponding trackedRace.removeWind/recordWind notifications OUTSIDE it.
        // Those downstream calls trigger maneuver-cache recalculation, which is the intended
        // bootstrap: NN+HMM pre-classifies spots -> wind fixes on the estimation track -> the
        // recalc re-runs detection so existing spots can be re-typed against the updated wind
        // picture. The re-run reads other wind sources and, in isManeuverSpotWindNearlySame,
        // also this estimation track (that read is what triggers a re-typing when the
        // estimation track changes). Holding the write lock across the downstream calls
        // therefore creates a lock-order inversion with those readers via SmartFutureCache
        // internals: the estimator holds write on the wind track and then acquires internal
        // state inside SmartFutureCache.triggerUpdate, while concurrent detection tasks
        // (running from SmartFutureCache callbacks) try to acquire the wind track's read
        // lock. Releasing the wind-track write lock before firing the downstream side-effects
        // removes the inversion. The invariant guarded by the lock -- the contents of
        // windTrackWithConfidences -- is fully updated by the time we release.
        final List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToRemove = new ArrayList<>();
        final List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToAdd = new ArrayList<>();
        estimatedWindTrack.lockForWrite();
        try {
            for (Iterator<WindWithConfidence<Pair<Position, TimePoint>>> previousWindFixesIterator = windTrackWithConfidences
                    .values().iterator(); previousWindFixesIterator.hasNext();) {
                final WindWithConfidence<Pair<Position, TimePoint>> previousWind = previousWindFixesIterator.next();
                final WindWithConfidence<Pair<Position, TimePoint>> newWind = newWindTrackMap
                        .get(previousWind.getRelativeTo());
                if (newWind == null) {
                    previousWindFixesIterator.remove();
                    windFixesToRemove.add(previousWind);
                } else if (!isWindNearlySame(newWind.getObject(), previousWind.getObject())) {
                    previousWindFixesIterator.remove();
                    windFixesToRemove.add(previousWind);
                    windFixesToAdd.add(newWind);
                }
            }
            for (final WindWithConfidence<Pair<Position, TimePoint>> newWind : newWindTrack) {
                if (!windTrackWithConfidences.containsKey(newWind.getRelativeTo())) {
                    windFixesToAdd.add(newWind);
                }
            }
            for (final WindWithConfidence<Pair<Position, TimePoint>> windFixToAdd : windFixesToAdd) {
                windTrackWithConfidences.put(windFixToAdd.getRelativeTo(), windFixToAdd);
            }
        } finally {
            estimatedWindTrack.unlockAfterWrite();
        }
        // Apply the collected deltas to the tracked race now that we no longer hold the
        // estimated wind track's write lock.
        for (final WindWithConfidence<Pair<Position, TimePoint>> windFixToRemove : windFixesToRemove) {
            trackedRace.removeWind(windFixToRemove.getObject(), windSource);
        }
        for (final WindWithConfidence<Pair<Position, TimePoint>> windFixToAdd : windFixesToAdd) {
            trackedRace.recordWind(windFixToAdd.getObject(), windSource, false);
        }
    }

    /**
     * Additive-only counterpart of {@link #applyManeuverClassificationsToWindTrack}: inserts any wind fixes for
     * {@code maneuversWithEstimatedType} that are not already present in {@link #windTrackWithConfidences}, and does
     * <em>not</em> remove any pre-existing fixes. Used by the DB-load / re-adaptation path
     * ({@link PreClassifiedUpdate}) which invokes per competitor: the corresponding {@code newWindTrack} covers only
     * one competitor's contribution, so the full reconciliation of {@link #applyManeuverClassificationsToWindTrack}
     * would clobber fixes belonging to other competitors on every call. Additive semantics are correct here because the
     * DB load supplies the whole race's typed maneuvers across the successive per-competitor calls, and the union of
     * those per-competitor wind fixes is the intended track content. See bug6241.
     * <p>
     *
     * As with {@link #applyManeuverClassificationsToWindTrack}, the mutation of {@link #windTrackWithConfidences}
     * happens under the wind track's write lock and the corresponding {@code trackedRace.recordWind} notifications are
     * fired outside the lock to avoid a lock-order inversion with concurrent maneuver detection reading the wind track.
     */
    private void addManeuverClassificationsToWindTrack(
            final List<SimpleManeuverWithEstimatedType<? extends SimpleManeuverForEstimation>> maneuversWithEstimatedType) {
        final List<WindWithConfidence<Pair<Position, TimePoint>>> newWindTrack = windTrackCalculator
                .getWindTrackFromManeuverClassifications(maneuversWithEstimatedType);
        final List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToAdd = new ArrayList<>();
        estimatedWindTrack.lockForWrite();
        try {
            for (final WindWithConfidence<Pair<Position, TimePoint>> newWind : newWindTrack) {
                if (!windTrackWithConfidences.containsKey(newWind.getRelativeTo())) {
                    windTrackWithConfidences.put(newWind.getRelativeTo(), newWind);
                    windFixesToAdd.add(newWind);
                }
            }
        } finally {
            estimatedWindTrack.unlockAfterWrite();
        }
        for (final WindWithConfidence<Pair<Position, TimePoint>> windFixToAdd : windFixesToAdd) {
            trackedRace.recordWind(windFixToAdd.getObject(), windSource, false);
        }
    }

    /**
     * Maps a domain {@link ManeuverType} to its wind-estimation counterpart, or {@code null} if the type doesn't
     * correspond to a maneuver kind the wind-track calculator can produce a wind fix from (e.g., PENALTY_CIRCLE or
     * UNKNOWN). Matches the mapping in ManeuverWithEstimatedTypeFromManeuverTest.
     */
    private static ManeuverTypeForClassification mapManeuverType(final ManeuverType type) {
        final ManeuverTypeForClassification result;
        switch (type) {
        case BEAR_AWAY:
            result = ManeuverTypeForClassification.BEAR_AWAY;
            break;
        case HEAD_UP:
            result = ManeuverTypeForClassification.HEAD_UP;
            break;
        case JIBE:
            result = ManeuverTypeForClassification.JIBE;
            break;
        case TACK:
            result = ManeuverTypeForClassification.TACK;
            break;
        case PENALTY_CIRCLE:
        case UNKNOWN:
        default:
            result = null;
            break;
        }
        return result;
    }

    /**
     * Enqueues the given {@link PendingUpdate} and ensures that a {@link GraphRecalculationTask} exists to process it.
     * Callers must not hold this object's monitor.
     */
    private synchronized void submit(final PendingUpdate update) {
        final boolean queueWasEmpty = updateQueue.isEmpty();
        updateQueue.add(update);
        logger.fine(()->"Currently "+updateQueue.size()+" update jobs enqueued for race "+trackedRace.getRaceIdentifier());
        if (queueWasEmpty && updateTask == null) {
            logger.fine(()->"Creating a new recalculation task for "+trackedRace.getRaceIdentifier());
            updateTask = new GraphRecalculationTask();
            IncrementalMstHmmWindEstimationForTrackedRace.this.notifyAll();
            recalculator.execute(updateTask);
        }
    }

    /**
     * Enqueues an update into {@link #updateQueue} and ensures that a {@link GraphRecalculationTask} exists to handle
     * it. The method is {@code synchronized} to implement the choreography with {@link GraphRecalculationTask} which
     * also synchronizes on this object while trying to fetch the next update from the {@link #updateQueue} and if not
     * having retrieved an element setting {@link #updateTask} to {@code null} and terminating the task.
     *
     * @see #updateTask
     */
    @Override
    public void newManeuverSpotsDetected(final Competitor competitor, final Iterable<CompleteManeuverCurve> newManeuvers,
            final TrackTimeInfo trackTimeInfo) {
        submit(new NewSpotsUpdate(competitor, newManeuvers, trackTimeInfo));
    }

    /**
     * Enqueues an already-classified-maneuvers hand-off; see
     * {@link IncrementalWindEstimation#alreadyClassifiedManeuversAvailable(Competitor, Iterable)}.
     */
    @Override
    public void alreadyClassifiedManeuversAvailable(final Competitor competitor, final Iterable<Maneuver> maneuvers) {
        submit(new PreClassifiedUpdate(competitor, maneuvers));
    }

    private boolean isWindNearlySame(Wind oneWind, Wind otherWind) {
        double bearingInDegrees = oneWind.getBearing().getDifferenceTo(otherWind.getBearing()).abs().getDegrees();
        if (bearingInDegrees > WIND_COURSE_TOLERANCE_IN_DEGREES_TO_IGNORE_FOR_REUSE) {
            return false;
        }
        return true;
    }

    @Override
    public WindSource getWindSource() {
        return windSource;
    }
}
