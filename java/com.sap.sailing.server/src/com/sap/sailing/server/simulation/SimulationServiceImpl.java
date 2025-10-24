package com.sap.sailing.server.simulation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.CPUMeteringType;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.LegIdentifier;
import com.sap.sailing.domain.common.LegIdentifierImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.PathType;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.shared.tracking.AddResult;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaListener;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.SimulationService;
import com.sap.sailing.simulator.Path;
import com.sap.sailing.simulator.PolarDiagram;
import com.sap.sailing.simulator.SimulationParameters;
import com.sap.sailing.simulator.SimulationResults;
import com.sap.sailing.simulator.Simulator;
import com.sap.sailing.simulator.impl.PolarDiagramGPS;
import com.sap.sailing.simulator.impl.SimulationParametersImpl;
import com.sap.sailing.simulator.impl.SimulatorImpl;
import com.sap.sailing.simulator.impl.SparseSimulationDataException;
import com.sap.sailing.simulator.util.SailingSimulatorConstants;
import com.sap.sailing.simulator.windfield.WindFieldGenerator;
import com.sap.sailing.simulator.windfield.impl.WindFieldTrackedRaceImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.concurrent.RunnableWithResultAndException;
import com.sap.sse.util.SmartFutureCache;

public class SimulationServiceImpl implements SimulationService {
    private static final Logger logger = Logger.getLogger(SimulationService.class.getName());

    final private ScheduledExecutorService executor;
    final private SmartFutureCache<LegIdentifier, SimulationResults, SmartFutureCache.EmptyUpdateInterval> cache;
    final private ConcurrentMap<LegIdentifier, TimePoint> cacheReadTimePoints;
    final private RacingEventService racingEventService;
    final private ScheduledExecutorService scheduler;
    final private Duration CACHE_ENTRY_EXPIRY_DURATION = Duration.ONE_MINUTE.times(10);
    
    /**
     * Keys are regatta names
     */
    final private HashMap<String, SimulationRaceListener> raceListeners;
    final private HashMap<RaceIdentifier, LegChangeListener> legListeners;
    final private long WAIT_MILLIS = 20000; // milliseconds to wait until earliest cache-update for simulation
    
    public SimulationServiceImpl(ScheduledExecutorService executor, RacingEventService racingEventService) {
        this.cacheReadTimePoints = new ConcurrentHashMap<>();
        this.executor = executor;
        this.scheduler = executor;
        this.racingEventService = racingEventService;
        if (racingEventService != null) {
            this.raceListeners = new HashMap<String, SimulationRaceListener>();
            this.legListeners = new HashMap<RaceIdentifier, LegChangeListener>();
            this.cache = new SmartFutureCache<LegIdentifier, SimulationResults, SmartFutureCache.EmptyUpdateInterval>(
                    new SmartFutureCache.AbstractCacheUpdater<LegIdentifier, SimulationResults, SmartFutureCache.EmptyUpdateInterval>() {
                        @Override
                        public SimulationResults computeCacheUpdate(LegIdentifier key, SmartFutureCache.EmptyUpdateInterval updateInterval) throws Exception {
                            return racingEventService.getTrackedRace(key).getTrackedRegatta().callWithCPUMeterWithException(()->{
                                logger.info("Simulation Started: \"" + key.toString() + "\"");
                                SimulationResults results = computeSimulationResults(key);
                                logger.info("Simulation Finished: \"" + key.toString() + "\", Results-Version: "+ (results==null?0:results.getVersion().asMillis()));
                                return results;
                            }, CPUMeteringType.SIMULATOR.name());
                        }
                    }, "SmartFutureCache.simulationService (" + racingEventService.toString() + ")");
            scheduler.scheduleAtFixedRate(()->expireCacheEntries(), CACHE_ENTRY_EXPIRY_DURATION.asMillis(), CACHE_ENTRY_EXPIRY_DURATION.asMillis(), TimeUnit.MILLISECONDS);
        } else {
            this.raceListeners = null;
            this.legListeners = null;
            this.cache = null;
        }
    }
    
    @Override
    public Iterable<BoatClass> getBoatClassesWithPolarData() {
        return racingEventService.getPolarDataService().getAllBoatClassesWithPolarSheetsAvailable();
    }
    
    @Override
    public BoatClass getBoatClass(String name) {
        return racingEventService.getBaseDomainFactory().getBoatClass(name);
    }

    @Override
    public PolarDiagram getPolarDiagram(BoatClass boatClass) {
        try {
            return new PolarDiagramGPS(boatClass, racingEventService.getPolarDataService());
        } catch (SparseSimulationDataException e) {
            logger.warning("Request for polar diagram of boat class " + boatClass.getName()
                    + " failed due to sparse polar data. Was it really returned by getBoatClassesWithPolarData()?");
            return null;
        }
    }

    private void expireCacheEntries() {
        final TimePoint expireAllOlderThan = TimePoint.now().minus(CACHE_ENTRY_EXPIRY_DURATION);
        for (final Iterator<Entry<LegIdentifier, TimePoint>> cacheReadTimePointsIterator = cacheReadTimePoints.entrySet().iterator(); cacheReadTimePointsIterator.hasNext(); ) {
            final Entry<LegIdentifier, TimePoint> cacheReadTimePointsEntry = cacheReadTimePointsIterator.next();
            if (cacheReadTimePointsEntry.getValue().before(expireAllOlderThan)) {
                logger.info("Removing expired simulator result for leg "+cacheReadTimePointsEntry.getKey()+
                        " because it was last accessed at "+cacheReadTimePointsEntry.getValue()+
                        " which is more than "+CACHE_ENTRY_EXPIRY_DURATION+" before now.");
                cacheReadTimePointsIterator.remove();
                cache.remove(cacheReadTimePointsEntry.getKey());
            }
        }
    }

    /**
     * When a {@link TrackedRace} is removed from its {@link TrackedRegatta}, all {@link #cache} entries that still
     * exist for that race will be removed. Since a {@link LegChangeListener} will have made all necessary removals
     * when the race's course changes, we can assume that cache entries may remain only for legs that still exist in
     * the tracked race.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class SimulationRaceListener implements RaceListener {
        @Override
        public void raceAdded(TrackedRace trackedRace) {
        }

        @Override
        public void raceRemoved(TrackedRace trackedRace) {
            RegattaAndRaceIdentifier raceIdentifier = trackedRace.getRaceIdentifier();
            LegChangeListener listener = legListeners.get(raceIdentifier); 
            if (listener != null) {
                trackedRace.removeListener(listener);
            }
            int legNumber = 1;
            Iterator<TrackedLeg> iterator = trackedRace.getTrackedLegs().iterator(); 
            while (iterator.hasNext()) {
                LegIdentifier key = new LegIdentifierImpl(raceIdentifier, legNumber);
                cache.remove(key);
                legNumber++;
                iterator.next();
            }
            legListeners.remove(raceIdentifier);
        }
    }
    
    /**
     * A stateful listener whose {@link #legIdentifier} may change over time, updated to the most recent request. When
     * changes to the race are received that are considered relevant for the simulation results, a cache update will be
     * triggered {@link SimulationServiceImpl#WAIT_MILLIS} milliseconds after the change event. To avoid redundant
     * triggers, more updates received before the wait period expires are ignored as long as they are for the same leg.<p>
     * 
     * TODO the {@link #covered} and {@link #legIdentifier} fields with the stateful design and the dependence on {@link #isLive}
     * seem a bit "smelly." The {@link #legIdentifier} field is updated only in "live" mode when a simulator result is requested.
     * It therefore remains at the last leg for which a result was requested while the race was in live mode. This doesn't
     * necessarily have to be the race's last leg. When updates strike---such as a mark moving---then the simulation results
     * for all of the race's legs at least need to be invalidated. The way the implementation looks right now it seems that
     * results for legs that were requested earlier will not be updated because {@link #legIdentifier} does not point to them.
     * And since {@link #legIdentifier} is no more updated for non-live races, updates to older leg simulation results will
     * never happen...
     */
    private class LegChangeListener extends AbstractRaceChangeListener {
        private final TrackedRace trackedRace;
        
        /**
         * Contains a mapping for those {@link LegIdentifier}s for which a cache update trigger has been scheduled. The
         * value is always {@code true}. A concurrent map is used in order to have atmic behavior of
         * {@link ConcurrentMap#computeIfAbsent(Object, java.util.function.Function)}.
         */
        private final ConcurrentMap<LegIdentifier, Boolean> cacheUpdateTriggerScheduledForLeg;

        public LegChangeListener(TrackedRace trackedRace) {
            this.trackedRace = trackedRace;
            this.cacheUpdateTriggerScheduledForLeg = new ConcurrentHashMap<>();
        }

        @Override
        public void startOfRaceChanged(TimePoint oldStartOfRace, TimePoint newStartOfRace) {
            // relevant for simulation: start of leg 1 changes; requires update of leg 1
            scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(/* zero-based */ 0, /* wait */ 0);
        }

        @Override
        public void finishedTimeChanged(TimePoint oldFinishedTime, TimePoint newFinishedTime) {
            // not relevant for simulation because we care when the boats entered the leg, not when the last one finished
        }

        @Override
        public void windSourcesToExcludeChanged(Iterable<? extends WindSource> windSourcesToExclude) {
            // relevant for simulation: update all legs when wind changes overall
            scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(/* zero-based */ 0, /* wait */ 0);
        }

        @Override
        public void startOfTrackingChanged(TimePoint oldStartOfTracking, TimePoint newStartOfTracking) {
            // irrelevant for simulation
        }

        @Override
        public void endOfTrackingChanged(TimePoint oldEndOfTracking, TimePoint newEndOfTracking) {
            // irrelevant for simulation
        }

        @Override
        public void startTimeReceivedChanged(TimePoint startTimeReceived) {
            // irrelevant for simulation: should be covered by startOfRaceChanged()
        }

        @Override
        public void markPositionChanged(GPSFix fix, Mark mark, boolean firstInTrack, AddResult addedOrReplaced) {
            // relevant for simulation
            defaultAction();
        }

        @Override
        public void windDataReceived(Wind wind, WindSource windSource) {
            // relevant for simulation: update legs influenced by wind
            defaultAction();
        }

        @Override
        public void windDataRemoved(Wind wind, WindSource windSource) {
            // relevant for simulation: update legs influenced by wind
            defaultAction();
        }

        @Override
        public void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
            // relevant for simulation: update legs influenced by wind
            defaultAction();
        }

        @Override
        public void competitorPositionChanged(GPSFixMoving fix, Competitor item, AddResult addedOrReplaced) {
            // irrelevant for simulation: covered by wind estimation
        }

        @Override
        public void markPassingReceived(Competitor competitor, Map<Waypoint, MarkPassing> oldMarkPassings, Iterable<MarkPassing> markPassings) {
            // relevant for simulation: update start- and end-times of legs
            final Pair<Integer, Integer> firstAndLastZeroBasedNumberOfLegWithChangedAdjacendMarkPassing = getFirstAndLastZeroBasedNumberOfLegWithChangedAdjacendMarkPassing(oldMarkPassings, markPassings);
            if (firstAndLastZeroBasedNumberOfLegWithChangedAdjacendMarkPassing != null) {
                scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(firstAndLastZeroBasedNumberOfLegWithChangedAdjacendMarkPassing.getA(), firstAndLastZeroBasedNumberOfLegWithChangedAdjacendMarkPassing.getB(), WAIT_MILLIS);
            }
        }

        private Pair<Integer, Integer> getFirstAndLastZeroBasedNumberOfLegWithChangedAdjacendMarkPassing(
                Map<Waypoint, MarkPassing> oldMarkPassings, Iterable<MarkPassing> markPassings) {
            final Course course = trackedRace.getRace().getCourse();
            final Set<Integer> affectedZeroBasedWaypointIndexes = new HashSet<>();
            final Set<Waypoint> removed = new HashSet<>(oldMarkPassings.keySet());
            for (final MarkPassing newMarkPassing : markPassings) {
                removed.remove(newMarkPassing.getWaypoint());
                final MarkPassing oldMarkPassing = oldMarkPassings.get(newMarkPassing.getWaypoint());
                if (oldMarkPassing == null || !oldMarkPassing.getTimePoint().equals(newMarkPassing.getTimePoint())) {
                    // mark passing changed
                    affectedZeroBasedWaypointIndexes.add(course.getIndexOfWaypoint(newMarkPassing.getWaypoint()));
                }
            }
            removed.forEach(waypointWithMarkPassingRemoved->affectedZeroBasedWaypointIndexes.add(course.getIndexOfWaypoint(waypointWithMarkPassingRemoved)));
            final Pair<Integer, Integer> result;
            if (affectedZeroBasedWaypointIndexes.isEmpty()) {
                result = null;
            } else {
                final int min = Collections.min(affectedZeroBasedWaypointIndexes);
                final int max = Collections.max(affectedZeroBasedWaypointIndexes);
                // now extend to each side because a mark passing change for a waypoint affects both
                // adjacent legs, unless there is no adjacent leg in that direction
                result = new Pair<>(min == 0 ? 0 : min-1, max==course.getNumberOfWaypoints()-1 ? max : max+1);
            }
            return result;
        }

        @Override
        public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
            // irrelevant for simulation
        }

        @Override
        public void delayToLiveChanged(long delayToLiveInMillis) {
            // irrelevant for simulation
        }

        @Override
        public void waypointAdded(int zeroBasedIndex, Waypoint waypointThatGotAdded) {
            // relevant for simulation: legs with indices starting at zeroBasedIndex are affected and need to have their simulation results removed from the cache
            final int numberOfWaypoints = trackedRace.getRace().getCourse().getNumberOfWaypoints();
            scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(zeroBasedIndex, numberOfWaypoints, /* wait */ 0);
        }

        @Override
        public void waypointRemoved(int zeroBasedIndex, Waypoint waypointThatGotRemoved) {
            // relevant for simulation: legs with indices starting at zeroBasedIndex are affected and need to have their simulation results removed from the cache
            final int numberOfWaypoints = trackedRace.getRace().getCourse().getNumberOfWaypoints();
            scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(zeroBasedIndex, numberOfWaypoints, /* wait */ 0);
        }

        @Override
        protected void defaultAction() {
            scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(0, trackedRace.getRace().getCourse().getNumberOfWaypoints(), WAIT_MILLIS);
        }

        private void scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(final int fromZeroBasedLegIndex, final int toZeroBasedLegIndexExclusive, long schedulingDelayMillis) {
            for (int zeroBasedLegNumber=fromZeroBasedLegIndex; zeroBasedLegNumber<toZeroBasedLegIndexExclusive; zeroBasedLegNumber++) {
                scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(zeroBasedLegNumber, WAIT_MILLIS);
            }
        }
        
        private void scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduled(int zeroBasedLegNumber, long schedulingDelayMillis) {
            final LegIdentifierImpl key = new LegIdentifierImpl(trackedRace.getRaceIdentifier(), zeroBasedLegNumber+1);
            scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduledYet(key, WAIT_MILLIS);
        }

        private void scheduleUpdateTriggerForLegIfCachedAndNoUpdateScheduledYet(final LegIdentifier cacheKey, long schedulingDelayMillis) {
            // several calls to this method may occur more or less concurrently; try to reduce redundant scheduling
            // as far as possible by using the atomic computeIfAbsent method on the ConcurrentMap:
            cacheUpdateTriggerScheduledForLeg.computeIfAbsent(cacheKey, ck->{
                final Boolean result;
                if (cache.get(cacheKey, /* waitForLatest */ false) != null) {
                    scheduler.schedule(() -> triggerUpdate(cacheKey), schedulingDelayMillis, TimeUnit.MILLISECONDS);
                    result = true;
                } else {
                    result = null;
                }
                return result;
            });
        }

        private void triggerUpdate(final LegIdentifier legIdentifier) {
            cacheUpdateTriggerScheduledForLeg.remove(legIdentifier); // now it's no longer scheduled but actually running
            logger.info("Simulation Scheduled Update Triggered: \"" + legIdentifier.toString() + "\"");
            cache.triggerUpdate(legIdentifier, null);
        }
    }

    @Override
    public long getSimulationResultsVersion(LegIdentifier legIdentifier) {
        SimulationResults result = cache.get(legIdentifier, false);
        long version = (result == null ? 0 : result.getVersion().asMillis());
        logger.fine("Simulation Results-Version: " + version);
        return version;
    }

    @Override
    public SimulationResults getSimulationResults(LegIdentifier legIdentifier) {
        SimulationResults result = cache.get(legIdentifier, false);
        if (result == null) {
            logger.fine("Simulation Get: Cache Empty: \"" + legIdentifier.toString() + "\"");
            if (!raceListeners.containsKey(legIdentifier.getRegattaName())) {
                final Regatta regatta = racingEventService.getRegattaByName(legIdentifier.getRegattaName());
                final DynamicTrackedRegatta trackedRegatta = racingEventService.getTrackedRegatta(regatta);
                final SimulationRaceListener raceListener = new SimulationRaceListener(); 
                raceListeners.put(legIdentifier.getRegattaName(), raceListener);
                trackedRegatta.addRaceListener(raceListener, /* Not replicated */ Optional.empty(), /* synchronous */ false);
                racingEventService.addTrackedRegattaListener(new TrackedRegattaListener() {
                    @Override public void regattaAdded(TrackedRegatta trackedRegatta) {}

                    @Override
                    public void regattaRemoved(TrackedRegatta tr) {
                        if (trackedRegatta == tr) {
                            tr.removeRaceListener(raceListener);
                        }
                    }
                });
            }
            if (!legListeners.containsKey(legIdentifier.getRaceIdentifier())) {
                final TrackedRace trackedRace = racingEventService.getTrackedRace(legIdentifier);
                if (trackedRace != null) {
                    LegChangeListener listener = new LegChangeListener(trackedRace);
                    legListeners.put(legIdentifier.getRaceIdentifier(), listener);
                    trackedRace.addListener(listener);
                }
            }
            logger.info("Simulation Get: Update Triggered: \"" + legIdentifier.toString() + "\"");
            cache.triggerUpdate(legIdentifier, null);
            result = cache.get(legIdentifier, true); // take first simulation result that becomes available
        }
        if (result == null) {
            logger.fine("Simulation Get: Null-Result: \"" + legIdentifier.toString() + "\"");
        } else {
            recordCacheHit(legIdentifier);
        }
        return result;
    }

    private void recordCacheHit(LegIdentifier legIdentifier) {
        cacheReadTimePoints.put(legIdentifier, TimePoint.now());
    }

    private List<Position> getLinePositions(Waypoint wayPoint, TimePoint at, TrackedRace trackedRace) {
        List<Position> line = new ArrayList<Position>();
        if (wayPoint != null) {
            for (Mark lineMark : wayPoint.getMarks()) {
                Position estimatedMarkPosition = trackedRace.getOrCreateTrack(lineMark).getEstimatedPosition(at, /* extrapolate */ false);
                if (estimatedMarkPosition != null) {
                    line.add(estimatedMarkPosition);
                }
            }
        }
        return line;
    }

    private SimulationResults computeSimulationResults(LegIdentifier legIdentifier) throws InterruptedException,
            ExecutionException {
        final TimePoint simulationStartTime = TimePoint.now();
        final SimulationResults result;
        TrackedRace trackedRace = racingEventService.getTrackedRace(legIdentifier);
        if (trackedRace != null) {
            result = trackedRace.getTrackedRegatta().callWithCPUMeterWithException((RunnableWithResultAndException<SimulationResults, ExecutionException>) ()->{
                boolean isLive = trackedRace.isLive(simulationStartTime);
                int zeroBasedlegNumber = legIdentifier.getOneBasedLegIndex()-1;
                Course raceCourse = trackedRace.getRace().getCourse();
                Leg leg = raceCourse.getLegs().get(zeroBasedlegNumber);
                // get previous mark or start line as start-position
                Waypoint fromWaypoint = leg.getFrom();
                // get next mark as end-position
                Waypoint toWaypoint = leg.getTo();
                final TimePoint startTimePoint;
                final TimePoint endTimePoint;
                final Duration legDuration; // FIXME the startTimePoint and endTimePoint may originate from different competitors; their difference therefore may not be any real time spent by any single competitor in that leg
                final MarkPassing toMarkPassing = Util.first(trackedRace.getMarkPassingsInOrder(toWaypoint));
                if (toMarkPassing != null) {
                    final Optional<MarkPassing> fromMarkPassing = trackedRace.getMarkPassings(toMarkPassing.getCompetitor()).stream().filter(mp->mp.getWaypoint() == fromWaypoint).findAny();
                    if (fromMarkPassing.isPresent()) {
                        startTimePoint = fromMarkPassing.get().getTimePoint();
                        // find the leg finish mark passing of the same competitor; the competitor may not have finished yet, may have been passed by
                        // another competitor who then finished the leg already (this time as the fastest).
                        endTimePoint = toMarkPassing.getTimePoint();
                        legDuration = startTimePoint.until(endTimePoint);
                    } else {
                        legDuration = Duration.NULL;
                        if (isLive && zeroBasedlegNumber == 0) {
                            endTimePoint = simulationStartTime;
                            startTimePoint = simulationStartTime;
                        } else {
                            startTimePoint = null;
                            endTimePoint = null;
                        }
                    }
                } else {
                    legDuration = Duration.NULL;
                    startTimePoint = simulationStartTime;
                    endTimePoint = simulationStartTime;
                }
                Position startPosition = null;
                List<Position> startLine = null;
                Position endPosition = null;
                List<Position> endLine = null;
                if (startTimePoint != null) {
                    startPosition = trackedRace.getApproximatePosition(fromWaypoint, startTimePoint);
                    List<Position> line = this.getLinePositions(fromWaypoint, startTimePoint, trackedRace);
                    if (line.size() == 2) {
                        startLine = line;
                    }
                }
                if (endTimePoint != null) {
                    endPosition = trackedRace.getApproximatePosition(toWaypoint, endTimePoint);
                    List<Position> line = this.getLinePositions(toWaypoint, endTimePoint, trackedRace);
                    if (line.size() == 2) {
                        endLine = line;
                    }
                } else if (startTimePoint != null) {
                    endPosition = trackedRace.getApproximatePosition(toWaypoint, startTimePoint);
                }
                // determine legtype upwind/downwind/reaching
                LegType legType = null;
                if (startTimePoint != null) {
                    try {
                        legType = trackedRace.getTrackedLeg(leg).getLegType(startTimePoint);
                    } catch (NoWindException e) {
                        return null;
                    }
                } else {
                    return null;
                }
                // get windfield
                WindFieldGenerator windField = new WindFieldTrackedRaceImpl(trackedRace);
                Duration timeStep = new MillisecondsDurationImpl(15 * 1000);
                windField.generate(startTimePoint, null, timeStep);
                // prepare simulation-parameters
                List<Position> course = new ArrayList<Position>();
                course.add(startPosition);
                course.add(endPosition);
                BoatClass boatClass = trackedRace.getRace().getBoatClass();
                PolarDataService polarDataService = getPolarDataService();
                PolarDiagram polarDiagram;
                try {
                    polarDiagram = new PolarDiagramGPS(boatClass, polarDataService);
                } catch (SparseSimulationDataException e) {
                    polarDiagram = null;
                    // TODO: raise a UI message, to inform user about missing polar data resulting in unability to simulate
                }
                Map<PathType, Path> paths = null;
                if (polarDiagram != null) {
                    double simuStepSeconds = startPosition.getDistance(endPosition).getNauticalMiles()
                            / ((PolarDiagramGPS) polarDiagram).getAvgSpeedInKnots() * 3600 / 100;
                    Duration simuStep = new MillisecondsDurationImpl(Math.round(simuStepSeconds) * 1000);
                    SimulationParameters simulationPars = new SimulationParametersImpl(course, startLine, endLine, polarDiagram,
                            windField, simuStep, SailingSimulatorConstants.ModeEvent, true, true, legType);
                    try {
                        paths = getAllPathsEvenTimed(simulationPars, timeStep.asMillis());
                    } catch (InterruptedException e) {
                        throw new ExecutionException(e);
                    }
                }
                // prepare simulator-results
                return new SimulationResults(startTimePoint, timeStep, legDuration, startPosition,
                        endPosition, paths, null, TimePoint.now());
            }, CPUMeteringType.SIMULATOR.name());
        } else {
            result = null;
        }
        return result;
    }

    private PolarDataService getPolarDataService() {
        return racingEventService.getPolarDataService();
    }

    public Map<PathType, Path> getAllPaths(SimulationParameters simulationParameters) throws InterruptedException,
            ExecutionException {
        final Map<PathType, Path> result = new HashMap<>();
        if (simulationParameters.getBoatPolarDiagram() != null) {
            Simulator simulator = new SimulatorImpl(simulationParameters);
            Future<Path> taskOmniscient = null;
            if (simulationParameters.showOmniscient()) {
                // schedule omniscient task
                taskOmniscient = executor.submit(() -> simulator.getPath(PathType.OMNISCIENT));
            }
            Future<Path> task1TurnerLeft = null;
            Future<Path> task1TurnerRight = null;
            if (simulationParameters.getLegType() != LegType.REACHING) {
                // schedule 1-turner tasks
                task1TurnerLeft = executor.submit(() -> simulator.getPath(PathType.ONE_TURNER_LEFT));
                task1TurnerRight = executor.submit(() -> simulator.getPath(PathType.ONE_TURNER_RIGHT));
            }
            Future<Path> taskOpportunistLeft = null;
            Future<Path> taskOpportunistRight = null;
            if (simulationParameters.showOpportunist()) {        
                // schedule opportunist tasks (which depend on 1-turner results)
                taskOpportunistLeft = executor.submit(() -> simulator.getPath(PathType.OPPORTUNIST_LEFT));
                taskOpportunistRight = executor.submit(() -> simulator.getPath(PathType.OPPORTUNIST_RIGHT));
            }
            Path path1TurnerLeft = null;
            Path path1TurnerRight = null;
            if (simulationParameters.getLegType() != LegType.REACHING) {
                // collect 1-turner results
                path1TurnerLeft = task1TurnerLeft.get();
                result.put(PathType.ONE_TURNER_LEFT, path1TurnerLeft);
                path1TurnerRight = task1TurnerRight.get();
                result.put(PathType.ONE_TURNER_RIGHT, path1TurnerRight);
            }
            Path pathOpportunistLeft = null;
            Path pathOpportunistRight = null;
            if (simulationParameters.showOpportunist()) {
                // collect opportunist results
                pathOpportunistLeft = taskOpportunistLeft.get();
                if (path1TurnerLeft != null) {
                    if (!path1TurnerLeft.getAlgorithmTimedOut() && (pathOpportunistLeft.getTurnCount() == 1)) {
                        pathOpportunistLeft = path1TurnerLeft;
                    }
                }
                result.put(PathType.OPPORTUNIST_LEFT, pathOpportunistLeft);
                pathOpportunistRight = taskOpportunistRight.get();
                if (path1TurnerRight != null) {
                    if (!path1TurnerRight.getAlgorithmTimedOut() && (pathOpportunistRight.getTurnCount() == 1)) {
                        pathOpportunistRight = path1TurnerRight;
                    }
                }
                result.put(PathType.OPPORTUNIST_RIGHT, pathOpportunistRight);
            }
            if (simulationParameters.showOmniscient()) {
                // collect omniscient result (last, since usually slowest calculation)
                Path pathOmniscient = taskOmniscient.get();
                if (path1TurnerLeft != null) {
                    if (!path1TurnerLeft.getAlgorithmTimedOut() && (pathOmniscient.getFinalTime().after(path1TurnerLeft.getFinalTime()))) {
                        pathOmniscient = path1TurnerLeft;
                    }
                }
                if (path1TurnerRight != null) {
                    if (!path1TurnerRight.getAlgorithmTimedOut() && (pathOmniscient.getFinalTime().after(path1TurnerRight.getFinalTime()))) {
                        pathOmniscient = path1TurnerRight;
                    }
                }
                if (pathOpportunistLeft != null) {
                    if (!pathOpportunistLeft.getAlgorithmTimedOut() && (pathOmniscient.getFinalTime().after(pathOpportunistLeft.getFinalTime()))) {
                        pathOmniscient = pathOpportunistLeft;
                    }
                }
                if (pathOpportunistRight != null) {
                    if (!pathOpportunistRight.getAlgorithmTimedOut() && (pathOmniscient.getFinalTime().after(pathOpportunistRight.getFinalTime()))) {
                        pathOmniscient = pathOpportunistRight;
                    }
                }
                result.put(PathType.OMNISCIENT, pathOmniscient);
            }
        }
        // return combined result
        return result;
    }

    public Map<PathType, Path> getAllPathsEvenTimed(SimulationParameters simuPars, long millisecondsStep)
            throws InterruptedException, ExecutionException {
        Map<PathType, Path> allTimedPaths = new TreeMap<PathType, Path>();
        Map<PathType, Path> allPaths = this.getAllPaths(simuPars);
        for (Entry<PathType, Path> entry : allPaths.entrySet()) {
            PathType pathType = entry.getKey();
            Path pathValue = entry.getValue();
            if (pathValue != null) {
                allTimedPaths.put(pathType, pathValue.getEvenTimedPath(millisecondsStep));
            }
        }
        return allTimedPaths;
    }

}
