package com.sap.sailing.windestimation.integration;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Executor;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.maneuverdetection.TrackTimeInfo;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.CompleteManeuverCurve;
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
import com.sap.sailing.windestimation.aggregator.msthmm.MstGraphLevel;
import com.sap.sailing.windestimation.aggregator.msthmm.MstManeuverGraphGenerator.MstManeuverGraphComponents;
import com.sap.sailing.windestimation.data.ManeuverWithEstimatedType;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverClassifiersCache;
import com.sap.sailing.windestimation.model.regressor.twdtransition.GaussianBasedTwdTransitionDistributionCache;
import com.sap.sailing.windestimation.windinference.DummyBasedTwsCalculatorImpl;
import com.sap.sailing.windestimation.windinference.MiddleCourseBasedTwdCalculatorImpl;
import com.sap.sailing.windestimation.windinference.PolarsBasedTwsCalculatorImpl;
import com.sap.sailing.windestimation.windinference.WindTrackCalculator;
import com.sap.sailing.windestimation.windinference.WindTrackCalculatorImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
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
     * Contains update requests scheduled by {@link #newManeuverSpotsDetected(Competitor, Iterable, TrackTimeInfo)}. Adding
     * and removing elements must {@code synchronize} on this {@link IncrementalMstHmmWindEstimationForTrackedRace} object.
     * When adding an element and {@link #updateTask} is {@code null}, a new update task must be scheduled and assigned
     * to {@link #updateTask} while holding this object's monitor ({@code synchronized}). When checking for the next
     * element to be removed and then deciding to terminate and clear the {@link #updateTask}, also this object's
     * monitor must be held.
     */
    private final ConcurrentLinkedDeque<Triple<Competitor, Iterable<CompleteManeuverCurve>, TrackTimeInfo>> updateQueue;
    
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
        this.estimatedWindTrack = new WindTrackWithConfidenceForEachWindFixImpl(millisecondsOverWhichToAverage,
                WindSourceType.MANEUVER_BASED_ESTIMATION.getBaseConfidence(),
                WindSourceType.MANEUVER_BASED_ESTIMATION.useSpeed() && polarDataService != null,
                IncrementalMstHmmWindEstimationForTrackedRace.class.getSimpleName()+" "+
                trackedRace.getRaceIdentifier(), false, windTrackWithConfidences);
        this.updateQueue = new ConcurrentLinkedDeque<>();
        this.trackedRace = trackedRace;
        this.windSource = windSource;
        DistanceAndDurationAwareWindTransitionProbabilitiesCalculator transitionProbabilitiesCalculator = new DistanceAndDurationAwareWindTransitionProbabilitiesCalculator(
                gaussianBasedTwdTransitionDistributionCache, true);
        this.mstManeuverGraphGenerator = new IncrementalMstManeuverGraphGenerator(
                new CompleteManeuverCurveToManeuverForEstimationConverter(trackedRace, polarDataService),
                transitionProbabilitiesCalculator, maneuverClassifiersCache);
        this.bestPathsCalculator = new MstBestPathsCalculatorImpl(transitionProbabilitiesCalculator);
        this.windTrackCalculator = new WindTrackCalculatorImpl(new MiddleCourseBasedTwdCalculatorImpl(),
                polarDataService == null ? new DummyBasedTwsCalculatorImpl()
                        : new PolarsBasedTwsCalculatorImpl(polarDataService));
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
     * multiple threads will block all but one of those threads.<p>
     * 
     * With this task, a separate queue ({@link IncrementalMstHmmWindEstimationForTrackedRace#updateQueue}) is
     * used to pull updates from that queue and add them to the maneuver graph generator, then processing the updates
     * to generate new wind estimations.<p>
     * 
     * A task of this type repeats this until the queue is empty and then terminates. Trying to fetch the next update
     * from the queue, deciding whether to terminate this task, and adding the next update to the queue are all synchronized
     * on the enclosing {@link IncrementalMstHmmWindEstimationForTrackedRace} object, ensuring that exactly one task
     * exists for the enclosing instance whenever an update is enqueued or processing.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class GraphRecalculationTask implements Runnable {
        @Override
        public void run() {
            logger.fine(()->"This is a new recalculation task for "+trackedRace.getRaceIdentifier());
            Triple<Competitor, Iterable<CompleteManeuverCurve>, TrackTimeInfo> nextUpdate;
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
                    updateGraphGenerator(nextUpdate.getA(), nextUpdate.getB(), nextUpdate.getC());
                }
            } while (nextUpdate != null);
        }
        
        private void updateGraphGenerator(Competitor competitor, Iterable<CompleteManeuverCurve> newManeuvers, TrackTimeInfo trackTimeInfo) {
            List<ManeuverWithEstimatedType> maneuversWithEstimatedType = new ArrayList<>();
            final MstManeuverGraphComponents graphComponents;
            for (CompleteManeuverCurve newManeuverSpot : newManeuvers) {
                // The add(...) method on IncrementalMstManeuverGraphGenerator is synchronized on the one instance per race.
                // But this newManeuverSpotsDetected method may be called by separate threads for different competitors.
                // If the calculation takes long, many pooled threads may block, reducing throughput to sequential
                // processing. See also bug 5824. We therefore enqueue the CompleteManeuverCurve maneuver spots and
                // run at most a single task as long as there are maneuver spots in the queue. Only the addition and
                // removal of tasks from the queue is synchronized with the creation and termination of the task.
                mstManeuverGraphGenerator.add(competitor, newManeuverSpot, trackTimeInfo);
            }
            graphComponents = mstManeuverGraphGenerator.parseGraph();
            if (graphComponents != null) {
                Iterable<GraphLevelInference<MstGraphLevel>> bestPath = bestPathsCalculator.getBestNodes(graphComponents);
                for (GraphLevelInference<MstGraphLevel> inference : bestPath) {
                    ManeuverWithEstimatedType maneuverWithEstimatedType = new ManeuverWithEstimatedType(
                            inference.getGraphLevel().getManeuver(), inference.getGraphNode().getManeuverType(),
                            inference.getConfidence());
                    maneuversWithEstimatedType.add(maneuverWithEstimatedType);
                }
                Collections.sort(maneuversWithEstimatedType);
                List<WindWithConfidence<Pair<Position, TimePoint>>> newWindTrack = windTrackCalculator
                        .getWindTrackFromManeuverClassifications(maneuversWithEstimatedType);
                Map<Pair<Position, TimePoint>, WindWithConfidence<Pair<Position, TimePoint>>> newWindTrackMap = new HashMap<>(
                        newWindTrack.size());
                for (WindWithConfidence<Pair<Position, TimePoint>> wind : newWindTrack) {
                    newWindTrackMap.put(wind.getRelativeTo(), wind);
                }
                // Now we're adjusting windTrackWithConfidences and the estimatedWindTrack incrementally and consistently
                // so that afterwards the contents match the newWindTrack
                // FIXME bug6026: the "consistently" seems to be causing problems when a new wind estimation model is ingested
                List<WindWithConfidence<Pair<Position, TimePoint>>> windFixesToAdd = new ArrayList<>();
                estimatedWindTrack.lockForWrite();
                try {
                    for (Iterator<WindWithConfidence<Pair<Position, TimePoint>>> previousWindFixesIterator = windTrackWithConfidences
                            .values().iterator(); previousWindFixesIterator.hasNext();) {
                        WindWithConfidence<Pair<Position, TimePoint>> previousWind = previousWindFixesIterator.next();
                        WindWithConfidence<Pair<Position, TimePoint>> newWind = newWindTrackMap
                                .get(previousWind.getRelativeTo());
                        if (newWind == null) {
                            previousWindFixesIterator.remove();
                            trackedRace.removeWind(previousWind.getObject(), windSource);
                        } else if (!isWindNearlySame(newWind.getObject(), previousWind.getObject())) {
                            previousWindFixesIterator.remove();
                            trackedRace.removeWind(previousWind.getObject(), windSource);
                            windFixesToAdd.add(newWind);
                        }
                    }
                    for (WindWithConfidence<Pair<Position, TimePoint>> newWind : newWindTrack) {
                        if (!windTrackWithConfidences.containsKey(newWind.getRelativeTo())) {
                            windFixesToAdd.add(newWind);
                        }
                    }
                    for (WindWithConfidence<Pair<Position, TimePoint>> windFixToAdd : windFixesToAdd) {
                        windTrackWithConfidences.put(windFixToAdd.getRelativeTo(), windFixToAdd);
                        trackedRace.recordWind(windFixToAdd.getObject(), windSource, false);
                    }
                } finally {
                    estimatedWindTrack.unlockAfterWrite();
                }
            }
        }
    }

    /**
     * Enqueues an update into {@link #updateQueue} and ensures that a {@link GraphRecalculationTask} exists to handle it.
     * The method is {@code synchronized} to implement the choreography with {@link GraphRecalculationTask} which also synchronizes
     * on this object while trying to fetch the next update from the {@link #updateQueue} and if not having retrieved an element
     * setting {@link #updateTask} to {@code null} and terminating the task.
     * 
     * @see #updateTask
     */
    @Override
    public synchronized void newManeuverSpotsDetected(Competitor competitor, Iterable<CompleteManeuverCurve> newManeuvers, TrackTimeInfo trackTimeInfo) {
        final boolean queueWasEmpty = updateQueue.isEmpty();
        updateQueue.add(new Triple<>(competitor, newManeuvers, trackTimeInfo));
        logger.fine(()->"Currently "+updateQueue.size()+" update jobs enqueued for race "+trackedRace.getRaceIdentifier());
        if (queueWasEmpty && updateTask == null) {
            logger.fine(()->"Creating a new recalculation task for "+trackedRace.getRaceIdentifier());
            updateTask = new GraphRecalculationTask();
            IncrementalMstHmmWindEstimationForTrackedRace.this.notifyAll();
            recalculator.execute(updateTask);
        }
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
