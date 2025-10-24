package com.sap.sailing.domain.racelogtracking.impl.fixtracker;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.regatta.MappingEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMarkMappingEvent;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.tracking.DoubleVectorFix;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.racelog.tracking.FixReceivedListener;
import com.sap.sailing.domain.racelog.tracking.SensorFixMapper;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelogsensortracking.SensorFixMapperFactory;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicSensorFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sailing.domain.tracking.impl.OutlierFilter;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.TimeRangeImpl;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * This class listens to RaceLog Events, changes to the race and fix loading events and properly handles mappings and
 * fix loading.<br>
 * The two main responsibility are to 1. load fixes already available in the DB when a race is tracked and 2. add fixes
 * newly sent by trackers to {@link Track}s of a {@link TrackedRace} if the fix is relevant for the race. In order to
 * save memory we try to keep the set of loaded fixes as constrained as possible. In general the following rules apply:
 * <ul>
 * <li>The fixes loaded to a track for a specific item (Mark, Competitor) are associated to a device that needs to be
 * mapped to the specific item using a {@link RegattaLogDeviceMappingEvent} in the {@link RegattaLog}. Only fixes that
 * are associated to the mapped device in the mapped {@link TimeRange} are allowed to be loaded into an item's
 * {@link Track}</li>
 * <li>If startOfTracking isn't available for the underlying {@link TrackedRace}, no fixes are loaded at all. This
 * prevents everything to be loaded in cases where a device is mapped for a whole {@link Regatta}'s time range and a
 * late race is tracked without startOfTracking being initially available.</li>
 * <li>If endOfTracking isn't available yet for a race, all fixes after startOfTracking are loaded into the respective
 * {@link Track}. This case is assumed to only occur in live scenarios where the endOfTracking is defined late while
 * boats cross the finishing line. This isn't much of a problem because no fixes are available for the future.</li>
 * <li>No loaded fixes are ever removed from {@link Track}s. Even if the tracking times change to be more restrictive,
 * the fixes in the time range not covered anymore are held in the {@link Track}.</li>
 * </ul>
 * Marks can either be tracked or just pinged using the respective app. If a Mark is pinged early in the morning before
 * the start of a race, no fix is available for this Mark in the tracked {@link TimeRange}. The semantic of pinging a
 * Mark is that the position is assumed to be correct until it is pinged again. So in extreme cases this single fix
 * needs to be used for all races of a day. To ensure this semantic there are additional special rules for
 * {@link Mark}s:
 * <ul>
 * <li>Fixes in the tracking time range are loaded/tracked using the above mentioned rules.</li>
 * <li>If at least one fix is available, no additional fix is loaded at all. If no fix is available for a {@link Mark}
 * in the tracking time range, the following rules apply:
 * <ul>
 * <li>If no fix isn't available yet before/after the tracking time range, the single fix is loaded that is the best
 * (nearest) before startOfTracking/after endOfTracking.</li>
 * <li>If additional fixes get available (either by tracking or by a new device mapping), only the best single fixes are
 * taken using the rule above. If there is already a best fix before startOfTracking/after endOfTracking, the new best
 * fix is only used if it is better (nearer) as the existing one.</li>
 * </ul>
 * </li>
 * </ul>
 * There is a corner case that is assumed to be acceptable for the specific semantic of a {@link Mark}'s fixes:<br>
 * If a Marks is tracked and not pinged, any new fix transferred from the tracking device is treated as being better
 * than the one before. So all fixes are being recorded starting at the time point when the operator starts tracking for
 * a race and startOfTracking is in the future.<p>
 * Loading jobs are scheduled with a dedicated thread pool {@link #executor} that has "foreground" priority. With this, the number
 * of concurrently executing loading jobs is restricted to approximately the number of CPUs on the executing host.<p>
 * Some related classes:
 * <ul>
 * <li>{@link RaceChangeListener}</li>
 * <li>{@link RegattaLogEventVisitor}</li>
 * <li>{@link FixReceivedListener}</li>
 * </ul>
 * 
 */
public class FixLoaderAndTracker implements TrackingDataLoader {
    private static final Logger logger = Logger.getLogger(FixLoaderAndTracker.class.getName());
    private static final ScheduledExecutorService executor = ThreadPoolUtil.INSTANCE.createForegroundTaskThreadPoolExecutor(
            FixLoaderAndTracker.class.getSimpleName());
    private final DynamicTrackedRace trackedRace;
    private final SensorFixStore sensorFixStore;
    private RegattaLogDeviceMappings<WithID> deviceMappings;
    
    /**
     * Loading fixes into tracks is done one a per item base using jobs that are being run on an executor. These jobs
     * are recognized to be able to calculate an overall progress. To ensure a consistent progress, no job is removed
     * when finished. The set is cleared instead, when all jobs are finished.<br>
     * The alternative would be to use a {@link TrackingDataLoader} per loading job. This would make things more
     * complicated to ensure a consistent progress and not leak loader instances. In addition we would need to implement
     * one more {@link TrackingDataLoader} that ensures the loading state of the associated {@link TrackedRace}.
     */
    private final Set<AbstractLoadingJob> loadingJobs = ConcurrentHashMap.newKeySet();
    
    /**
     * This map contains the last maneuver for each competitor ID that was sent back. It is used to efficiently piggyback a
     * notification about new possible maneuvers into the response to adding a GPS fix for smart phone tracking. This
     * allows the client to not require any additional polling for maneuver retrieval. The map is never cleared, as it
     * will not take a lot of memory itself, and will not grow by one entry for each competitor for which a fix
     * was submitted through the smartphone tracking connector.
     */
    private final ConcurrentHashMap<UUID, Maneuver> lastNotifiedManeuverCache = new ConcurrentHashMap<>();

    private final SensorFixMapperFactory sensorFixMapperFactory;
    
    /**
     * If set to {@code true} in the constructor, fixes loaded for competitor tracks will be subject to
     * outlier removal using the {@link OutlierFilter}.
     */
    private final boolean removeOutliersFromCompetitorTracks;
    
    /**
     * This flag is used to tell the loaders/trackers whether preemptive stopping has been requested. If switched
     * to {@code true}, running loaders will stop loading fixes and return immediately.
     */
    private AtomicBoolean preemptiveStopRequested = new AtomicBoolean(false);
    
    /**
     * When {@link #stop(boolean, boolean)} is called with {@code willBeStopped==true}, the fact will be recorded
     * in this attribute. This is then evaluated when updating general progress so as to set the race status to
     * {@link TrackedRaceStatusEnum#REMOVED} instead of {@link TrackedRaceStatusEnum#FINISHED}.
     */
    private AtomicBoolean willBeRemovedAfterStopping = new AtomicBoolean(false);
    
    private AtomicBoolean stopRequested = new AtomicBoolean(false);
    
    private final AbstractRaceChangeListener raceChangeListener = new AbstractRaceChangeListener() {
        @Override
        public void startOfTrackingChanged(TimePoint oldStartOfTracking, TimePoint newStartOfTracking) {
            if (newStartOfTracking != null) {
                if (oldStartOfTracking == null) {
                    // Fixes wheren't loaded while startOfTracking was null. So we need to load all fixes in the tracking interval now.
                    loadFixesWhenStartOfTrackingIsReceived();
                } else if (newStartOfTracking.before(oldStartOfTracking)) {
                    loadFixesForExtendedTimeRange(new TimeRangeImpl(newStartOfTracking, oldStartOfTracking));
                }
            }
        }

        @Override
        public void endOfTrackingChanged(TimePoint oldEndOfTracking, TimePoint newEndOfTracking) {
            if (trackedRace.getStartOfTracking() != null) {
                if (newEndOfTracking == null && oldEndOfTracking != null) {
                    loadFixesForExtendedTimeRange(new TimeRangeImpl(oldEndOfTracking, TimePoint.EndOfTime));
                } else if (newEndOfTracking != null && oldEndOfTracking != null && oldEndOfTracking.before(newEndOfTracking)) {
                    loadFixesForExtendedTimeRange(new TimeRangeImpl(oldEndOfTracking, newEndOfTracking));
                }
            }
        }

        public void regattaLogAttached(RegattaLog regattaLog) {
            deviceMappings.addRegattaLog(regattaLog);
        }
    };
    
    private final FixReceivedListener<Timed> listener = new FixReceivedListener<Timed>() {
        @Override
        public Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> fixReceived(DeviceIdentifier device, Timed fix, boolean returnManeuverChanges, boolean returnLiveDelay) {
            final Set<RegattaAndRaceIdentifier> maneuverChanged = new HashSet<>();
            final Map<RegattaAndRaceIdentifier, Duration> delayToLive = new HashMap<>();
            if (!preemptiveStopRequested.get() && trackedRace.getStartOfTracking() != null) {
                final TimePoint timePoint = fix.getTimePoint();
                deviceMappings.forEachMappingOfDeviceIncludingTimePoint(device, fix.getTimePoint(),
                        new Consumer<DeviceMappingWithRegattaLogEvent<WithID>>() {
                    @Override
                    public void accept(DeviceMappingWithRegattaLogEvent<WithID> mapping) {
                        mapping.getRegattaLogEvent().accept(new MappingEventVisitor() {
                            @Override
                            public void visit(RegattaLogDeviceCompetitorSensorDataMappingEvent event) {
                                recordSensorFixForCompetitor(event.getMappedTo(), event);
                            }
                            
                            @Override
                            public void visit(RegattaLogDeviceBoatSensorDataMappingEvent event) {
                                final Boat boat = event.getMappedTo();
                                final Competitor competitor = trackedRace.getCompetitorOfBoat(boat);
                                if (competitor != null) {
                                    recordSensorFixForCompetitor(competitor, event);
                                } else {
                                    logger.log(Level.FINE, ()->"Could not record fix for boat because no competitor could be determined. Boat: " + boat);
                                }
                            }
                            
                            private void recordSensorFixForCompetitor(Competitor competitor, RegattaLogDeviceMappingEvent<?> event) {
                                if (!preemptiveStopRequested.get()) {
                                    @SuppressWarnings("unchecked")
                                    SensorFixMapper<SensorFix, DynamicSensorFixTrack<Competitor, SensorFix>, Competitor> mapper = sensorFixMapperFactory
                                            .createCompetitorMapper((Class<? extends RegattaLogDeviceMappingEvent<?>>) event.getClass());
                                    DynamicSensorFixTrack<Competitor, SensorFix> track = mapper.getTrack(trackedRace, competitor);
                                    if (track != null && trackedRace.isWithinStartAndEndOfTracking(fix.getTimePoint())) {
                                        mapper.addFix(track, (DoubleVectorFix) fix);
                                        if (returnLiveDelay) {
                                            delayToLive.put(trackedRace.getRaceIdentifier(), new MillisecondsDurationImpl(trackedRace.getDelayToLiveInMillis()));
                                        }
                                    }
                                }
                            }
                            
                            @Override
                            public void visit(RegattaLogDeviceCompetitorMappingEvent event) {
                                recordForCompetitor(event.getMappedTo());
                            }
                            
                            @Override
                            public void visit(RegattaLogDeviceBoatMappingEvent event) {
                                final Boat boat = event.getMappedTo();
                                final Competitor comp = trackedRace.getCompetitorOfBoat(boat);
                                if (comp != null) {
                                    recordForCompetitor(comp);
                                } else {
                                    // this is not necessarily something to warn of; while a boat tracker may continuously track
                                    logger.log(Level.FINE,
                                            ()->"Could not record fix for boat because no competitor could be determined. Boat: " + boat);
                                }
                            }
                            
                            private void recordForCompetitor(Competitor comp) {
                                if (!preemptiveStopRequested.get()) {
                                    if (fix instanceof GPSFixMoving) {
                                        // try to record the fix, and only if it was really to the track,
                                        // check for maneuvers; otherwise, the fix may not have been accepted
                                        // by the race or the track, e.g., because the race's end-of-tracking
                                        // comes before the fix's time point
                                        if (trackedRace.recordFix(comp, (GPSFixMoving) fix)) {
                                            if (returnManeuverChanges) {
                                                RegattaAndRaceIdentifier maneuverChangedAnswer = detectIfManeuverChanged(comp);
                                                if (maneuverChangedAnswer != null) {
                                                    maneuverChanged.add(maneuverChangedAnswer);
                                                }
                                            }
                                            if (returnLiveDelay) {
                                                delayToLive.put(trackedRace.getRaceIdentifier(), new MillisecondsDurationImpl(trackedRace.getDelayToLiveInMillis()));
                                            }
                                        }
                                    } else {
                                        logger.log(Level.WARNING,
                                                String.format(
                                                        "Could not add fix for competitor (%s) in race (%s), as it"
                                                                + " is no GPSFixMoving, meaning it is missing COG/SOG values",
                                                                comp, trackedRace.getRace().getName()));
                                    }
                                }
                            }
                            
                            @Override
                            public void visit(RegattaLogDeviceMarkMappingEvent event) {
                                if (!preemptiveStopRequested.get()) {
                                    Mark mark = event.getMappedTo();
                                    final DynamicGPSFixTrack<Mark, GPSFix> markTrack = trackedRace.getOrCreateTrack(mark);
                                    final GPSFix firstFixAtOrAfter;
                                    final boolean forceFix;
                                    if (trackedRace.isWithinStartAndEndOfTracking(fix.getTimePoint())) {
                                        forceFix = false;
                                    } else {
                                        markTrack.lockForRead();
                                        try {
                                            if (Util.isEmpty(markTrack.getRawFixes())
                                                    || (firstFixAtOrAfter = markTrack.getFirstFixAtOrAfter(timePoint)) != null
                                                    && firstFixAtOrAfter.getTimePoint().equals(timePoint)) {
                                                // either the first fix or overwriting an existing one
                                                forceFix = true;
                                            } else {
                                                // checking if the given fix is "better" than an existing one
                                                TimePoint startOfTracking = trackedRace.getStartOfTracking();
                                                TimePoint endOfTracking = trackedRace.getStartOfTracking();
                                                if (startOfTracking != null) {
                                                    GPSFix fixAfterStartOfTracking = markTrack
                                                            .getFirstFixAtOrAfter(startOfTracking);
                                                    if (fixAfterStartOfTracking == null
                                                            || !trackedRace.isWithinStartAndEndOfTracking(
                                                                    fixAfterStartOfTracking.getTimePoint())) {
                                                        // There is no fix in the tracking interval, so this fix could be "better"
                                                        // than ones already available in the track
                                                        // Better means closer before/after the beginning/end of the tracking
                                                        // interval
                                                        if (timePoint.before(startOfTracking)) {
                                                            // check if it is closer to the beginning of the tracking interval
                                                            GPSFix fixBeforeStartOfTracking = markTrack
                                                                    .getLastFixAtOrBefore(startOfTracking);
                                                            forceFix = (fixBeforeStartOfTracking == null
                                                                    || fixBeforeStartOfTracking.getTimePoint().before(timePoint));
                                                        } else if (endOfTracking != null && timePoint.after(endOfTracking)) {
                                                            // check if it is closer to the end of the tracking interval
                                                            GPSFix fixAfterEndOfTracking = markTrack
                                                                    .getFirstFixAtOrAfter(endOfTracking);
                                                            forceFix = (fixAfterEndOfTracking == null
                                                                    || fixAfterEndOfTracking.getTimePoint().after(timePoint));
                                                        } else {
                                                            forceFix = false;
                                                        }
                                                    } else {
                                                        // there is already a fix in the tracking interval
                                                        forceFix = false;
                                                    }
                                                } else {
                                                    forceFix = false;
                                                }
                                            }
                                        } finally {
                                            markTrack.unlockAfterRead();
                                        }
                                    }
                                    trackedRace.recordFix(mark, (GPSFix) fix, /* only when in tracking interval */ !forceFix);
                                    if (returnLiveDelay) {
                                        delayToLive.put(trackedRace.getRaceIdentifier(), new MillisecondsDurationImpl(trackedRace.getDelayToLiveInMillis()));
                                    }
                                }
                            }
                        });
                    }
                });
            }
            return mergeManeuverChangedAndLiveDelayResult(maneuverChanged, delayToLive);
        }

        private Iterable<Triple<RegattaAndRaceIdentifier, Boolean, Duration>> mergeManeuverChangedAndLiveDelayResult(
                Set<RegattaAndRaceIdentifier> maneuverChanged, Map<RegattaAndRaceIdentifier, Duration> delayToLive) {
            final Map<RegattaAndRaceIdentifier, Pair<Boolean, Duration>> preResult = new HashMap<>();
            for (final Entry<RegattaAndRaceIdentifier, Duration> e : delayToLive.entrySet()) {
                preResult.put(e.getKey(), new Pair<>(maneuverChanged.contains(e.getKey()), e.getValue()));
            }
            for (final RegattaAndRaceIdentifier maneuverChanges : maneuverChanged) {
                if (!preResult.containsKey(maneuverChanges)) {
                    preResult.put(maneuverChanges, new Pair<>(true, null));
                }
            }
            return Util.map(preResult.entrySet(), e->new Triple<>(e.getKey(), e.getValue().getA(), e.getValue().getB()));
        }
    };

    /**
     * @param comp
     *            The resolved competitor for wich a gpsfix was just recorded.
     * @return Will return null or an RegattaAndRaceIdentifier, if the last maneuver for the given competitor changed
     *         since the last call to this method
     */
    private RegattaAndRaceIdentifier detectIfManeuverChanged(Competitor comp) {
        boolean changed = false;
        if (comp.getId() instanceof UUID) {
            Maneuver lastDetectedManeuver = Util.latest(trackedRace.getManeuvers(comp, false));
            if (lastDetectedManeuver != null) {
                Maneuver lastNotifiedManeuverOrNull = lastNotifiedManeuverCache.get(comp.getId());
                if (!lastDetectedManeuver.equals(lastNotifiedManeuverOrNull)) {
                    lastNotifiedManeuverCache.put((UUID) comp.getId(), lastDetectedManeuver);
                    changed = true;
                    logger.info(comp.getName() + " new maneuver is " + lastDetectedManeuver);
                }
            }
        }
        return changed ? trackedRace.getRaceIdentifier() : null;
    }

    public FixLoaderAndTracker(DynamicTrackedRace trackedRace, SensorFixStore sensorFixStore,
            SensorFixMapperFactory sensorFixMapperFactory, boolean removeOutliersFromCompetitorTracks) {
        this.sensorFixStore = sensorFixStore;
        this.sensorFixMapperFactory = sensorFixMapperFactory;
        this.trackedRace = trackedRace;
        this.removeOutliersFromCompetitorTracks = removeOutliersFromCompetitorTracks;
        startTracking();
    }

    /**
     * Loading fixes for {@link Mark}s needs special handling compared with {@link Competitor}s. If there are no fixes
     * available in the tracking interval, it is necessary to load other available fixes before/after the tracking
     * interval. this is due to the buoy pinger app can be used in the morning to ping some marks. The resulting fixes
     * need to also be available for races later on the day. This method ensures that if available, the best available
     * fixes are initially loaded when starting tracking. In contrast to that, mappings for {@link Competitor}s have no
     * special handling.
     */
    private void loadFixesForNewlyCoveredTimeRanges(WithID item,
            Map<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> newlyCoveredTimeRanges,
            Consumer<Double> progressConsumer, BooleanSupplier stopCallback) {
        if (trackedRace.getStartOfTracking() != null) {
            TimeRange trackingTimeRange = getTrackingTimeRange();
            loadFixesInTrackingTimeRange(newlyCoveredTimeRanges, trackingTimeRange, progressConsumer, stopCallback);
            if (item instanceof Mark) {
                Mark mark = (Mark) item;
                DynamicGPSFixTrack<Mark, GPSFix> track = trackedRace.getOrCreateTrack(mark);
                // load all mapped fixes if there was no fix in the tracking TimeRange
                GPSFix firstFixAfterStartOfTracking = track.getFirstFixAfter(trackingTimeRange.from());
                if (firstFixAfterStartOfTracking == null
                        || firstFixAfterStartOfTracking.getTimePoint().after(trackingTimeRange.to())) {
                    // There is no fix in the tracking interval -> looking for better fixes before start of tracking and
                    // after end of tracking
                    Iterable<GPSFix> betterFixesBeforeAndAfter = loadBetterFixesIfAvailable(trackingTimeRange, newlyCoveredTimeRanges);
                    for (GPSFix betterFixBeforeAndAfter : betterFixesBeforeAndAfter) {
                        track.add(betterFixBeforeAndAfter, true);
                    }
                }
            }
        }
    }

    /**
     * Loads fixes defined by the given mapping and {@link MultiTimeRange}. Only those fixes that are in the mapping
     * time range are being loaded.
     */
    private void loadFixesInTrackingTimeRange(
            Map<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> newlyCoveredTimeRanges,
            TimeRange trackingTimeRange, Consumer<Double> progressConsumer, BooleanSupplier stopCallback) {
        final MultiJobProgressUpdater progressUpdater = new MultiJobProgressUpdater(newlyCoveredTimeRanges.size());
        int currentJobNr = 0;
        for (Entry<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> e : newlyCoveredTimeRanges.entrySet()) {
            final int jobNr = currentJobNr;
            MultiTimeRange timeRange = e.getValue();
            RegattaLogDeviceMappingEvent<WithID> event = e.getKey();
            loadFixesForMultiTimeRange(timeRange.intersection(trackingTimeRange), event, p -> {
                progressUpdater.updateProgress(jobNr, p);
                progressConsumer.accept(progressUpdater.getProgress());
            }, stopCallback);
            currentJobNr++;
        }
        

    }

    /**
     * Loads fixes for the parts of the given {@link MultiTimeRange} using the given mapping event.
     */
    private void loadFixesForMultiTimeRange(MultiTimeRange effectiveRangeToLoad,
            RegattaLogDeviceMappingEvent<WithID> event, Consumer<Double> progressConsumer, BooleanSupplier stopCallback) {
        if (!effectiveRangeToLoad.isEmpty()) {
            int currentJobNr = 0;
            final int nrOfJobs = Util.size(effectiveRangeToLoad);
            final MultiJobProgressUpdater progressUpdater = new MultiJobProgressUpdater(nrOfJobs);
            for (TimeRange timeRange : effectiveRangeToLoad) {
                final int jobNr = currentJobNr;
                loadFixes(timeRange, event, p -> {
                    progressUpdater.updateProgress(jobNr, p);
                    progressConsumer.accept(progressUpdater.getProgress());
                }, stopCallback);
                currentJobNr++;
            }
        }
    }
    
    /**
     * Calls the given callback for every known mapping of the given item.
     * 
     * @param callback the callback to call for every known mapping
     */
    public boolean containsMappingThatIntersectsTimeRange(Iterable<DeviceMappingWithRegattaLogEvent<WithID>> mappings, TimeRange timeRange) {
        for (DeviceMappingWithRegattaLogEvent<WithID> mapping : mappings) {
            if (timeRange.intersects(mapping.getTimeRange())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Loads the fixes in the specified {@link TimeRange} using a visitor of the given mapping event.
     */
    private void loadFixes(TimeRange timeRangeToLoad, RegattaLogDeviceMappingEvent<? extends WithID> mappingEvent,
            final Consumer<Double> progressConsumer, final BooleanSupplier stopCallback) {
        if (timeRangeToLoad != null && !stopCallback.getAsBoolean()) {
            mappingEvent.accept(new MappingEventVisitor() {
                @Override
                public void visit(RegattaLogDeviceCompetitorSensorDataMappingEvent event) {
                    final Competitor competitor = event.getMappedTo();
                    loadSensorFixesForCompetitor(timeRangeToLoad, progressConsumer, stopCallback, event, competitor);
                }
                
                @Override
                public void visit(RegattaLogDeviceBoatSensorDataMappingEvent event) {
                    final Boat boat = event.getMappedTo();
                    final Competitor competitor = trackedRace.getCompetitorOfBoat(boat);
                    if (competitor != null) {
                        loadSensorFixesForCompetitor(timeRangeToLoad, progressConsumer, stopCallback, event, competitor);
                    } else {
                        logger.log(Level.WARNING,
                                "Could not load fixes for boat because no competitor could be determined. Boat: "
                                        + boat);
                    }
                }

                private void loadSensorFixesForCompetitor(TimeRange timeRangeToLoad, final Consumer<Double> progressConsumer,
                        final BooleanSupplier stopCallback, RegattaLogDeviceMappingEvent<?> event,
                        final Competitor competitor) {
                    @SuppressWarnings("unchecked")
                    SensorFixMapper<Timed, DynamicTrack<Timed>, Competitor> mapper = sensorFixMapperFactory
                            .createCompetitorMapper((Class<? extends RegattaLogDeviceMappingEvent<?>>) event.getClass());
                    DynamicTrack<Timed> track = mapper.getTrack(trackedRace, competitor);
                    if (track != null) {
                        // for split-fleet racing, device mappings coming from the regatta log may not be relevant
                        // for the trackedRace because the competitors may not compete in it; in this case, the
                        // competitor retrieved from the mapping event does not have a track in trackedRace
                        try {
                            sensorFixStore.<DoubleVectorFix> loadFixes(fix -> mapper.addFix(track, fix), event.getDevice(),
                                    timeRangeToLoad.from(), timeRangeToLoad.to(), /* toIsInclusive */ false,
                                    stopCallback, progressConsumer);
                        } catch (NoCorrespondingServiceRegisteredException | TransformationException e) {
                            logger.log(Level.WARNING, "Could not load track for competitor: " + event.getMappedTo()
                            + "; device: " + event.getDevice());
                        }
                    }
                }
                
                @Override
                public void visit(RegattaLogDeviceCompetitorMappingEvent event) {
                    loadForCompetitor(event.getMappedTo(), event);
                }
                
                @Override
                public void visit(RegattaLogDeviceBoatMappingEvent event) {
                    final Boat boat = event.getMappedTo();
                    final Competitor comp = trackedRace.getCompetitorOfBoat(boat);
                    if (comp != null) {
                        loadForCompetitor(comp, event);
                    } else {
                        logger.log(Level.WARNING,
                                "Could not load fixes for boat because no competitor could be determined. Boat: "
                                        + boat);
                    }
                }
                
                /**
                 * First loads the fixes into a temporary track which is then subject to outlier filtering (see
                 * {@link OutlierFilter}). The fixes that make it through outlier filtering are then inserted 
                 */
                private void loadForCompetitor(Competitor competitor, RegattaLogDeviceMappingEvent<?> event) {
                    DynamicGPSFixTrack<Competitor, GPSFixMoving> track = trackedRace.getTrack(competitor);
                    if (track != null) {
                        // for split-fleet racing, device mappings coming from the regatta log may not be relevant
                        // for the trackedRace because the competitors may not compete in it; in this case, the
                        // competitor retrieved from the mapping event does not have a track in trackedRace
                        final DynamicGPSFixTrack<Competitor, GPSFixMoving> loadedFixes =
                                new DynamicGPSFixMovingTrackImpl<Competitor>(track.getTrackedItem(), track.getMillisecondsOverWhichToAverageSpeed());
                        loadedFixes.suspendValidityAndMaxSpeedCaching(); // no validity nor max speed required on this track
                        try {
                            final DynamicGPSFixTrack<Competitor, GPSFixMoving> filteredTrack;
                            sensorFixStore.<GPSFixMoving> loadFixes(fix -> loadedFixes.add(fix, true), event.getDevice(),
                                    timeRangeToLoad.from(), timeRangeToLoad.to(), /* toIsInclusive */ false,
                                    stopCallback, progressConsumer);
                            if (removeOutliersFromCompetitorTracks) {
                                final Pair<Integer, DynamicGPSFixTrack<Competitor, GPSFixMoving>> filtered = new OutlierFilter().findAndRemoveInconsistenciesOnRawFixes(loadedFixes);
                                loadedFixes.lockForRead();
                                try {
                                    logger.info("Filtered competitor track for outliers; "+filtered.getA()+" outliers removed in track with "+Util.size(loadedFixes.getRawFixes())+" fixes");
                                } finally {
                                    loadedFixes.unlockAfterRead();
                                }
                                filteredTrack = filtered.getB();
                            } else {
                                filteredTrack = loadedFixes;
                            }
                            track.suspendValidityAndMaxSpeedCaching();
                            filteredTrack.lockForRead();
                            try {
                                for (final GPSFixMoving fix : filteredTrack.getRawFixes()) {
                                    track.add(fix, true);
                                }
                            } finally {
                                filteredTrack.unlockAfterRead();
                            }
                            track.resumeValidityAndMaxSpeedCaching();
                        } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
                            logger.log(Level.WARNING, "Could not load competitor track " + competitor + "; device "
                                    + event.getDevice());
                        }
                    }
                }
                
                @Override
                public void visit(RegattaLogDeviceMarkMappingEvent event) {
                    DynamicGPSFixTrack<Mark, GPSFix> track = trackedRace.getOrCreateTrack(event.getMappedTo());
                    try {
                        sensorFixStore.<GPSFix> loadFixes(fix -> track.add(fix, true), event.getDevice(),
                                timeRangeToLoad.from(), timeRangeToLoad.to(), /* toIsInclusive */ false,
                                stopCallback, progressConsumer);
                    } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
                        logger.log(Level.WARNING, "Could not load mark track " + event.getMappedTo());
                    }
                }
            });
        }
    }

    /**
     * Loads better fallback fixes if there is no fix in the tracking interval found. Returns the best (latest) fix
     * before the tracking interval found through any of the mappings (if any), and the best (earliest) fix after
     * the tracking interval found through any of the mappings (if any).
     * 
     * @param newlyCoveredTimeRanges mappings that are all mapping to the same item, such as a single Mark
     */
    private Iterable<GPSFix> loadBetterFixesIfAvailable(TimeRange trackingTimeRange,
            Map<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> newlyCoveredTimeRanges) {
        SortedSet<GPSFix> fixesBefore = new TreeSet<>(TimedComparator.INSTANCE);
        SortedSet<GPSFix> fixesAfter = new TreeSet<>(TimedComparator.INSTANCE);
        // coveredTimeRanges
        if (!preemptiveStopRequested.get()) {
            newlyCoveredTimeRanges.forEach((mappingEvent, coveredTimeRanges) -> {
                mappingEvent.accept(new MappingEventVisitor() {
                    @Override
                    public void visit(RegattaLogDeviceCompetitorSensorDataMappingEvent event) {
                        throw new UnsupportedOperationException();
                    }
                    
                    @Override
                    public void visit(RegattaLogDeviceBoatSensorDataMappingEvent event) {
                        throw new UnsupportedOperationException();
                    }
                    
                    @Override
                    public void visit(RegattaLogDeviceCompetitorMappingEvent event) {
                        throw new UnsupportedOperationException();
                    }
                    
                    @Override
                    public void visit(RegattaLogDeviceBoatMappingEvent event) {
                        throw new UnsupportedOperationException();
                    }
                    
                    @Override
                    public void visit(RegattaLogDeviceMarkMappingEvent event) {
                        DynamicGPSFixTrack<Mark, GPSFix> track = trackedRace.getOrCreateTrack(event.getMappedTo());
                        final TimePoint lastFixTimePointAtOrBeforeStartOfTracking;
                        if (!fixesBefore.isEmpty()) {
                            lastFixTimePointAtOrBeforeStartOfTracking = fixesBefore.last().getTimePoint();
                        } else {
                            final GPSFix lastFixAtOrBeforeStartOfTracking = track.getLastFixAtOrBefore(trackingTimeRange.from());
                            lastFixTimePointAtOrBeforeStartOfTracking = lastFixAtOrBeforeStartOfTracking == null ? null : lastFixAtOrBeforeStartOfTracking.getTimePoint();
                        }
                        // A better fix before start of tracking must be after the current best fix
                        final TimePoint from = lastFixTimePointAtOrBeforeStartOfTracking != null
                                ? lastFixTimePointAtOrBeforeStartOfTracking
                                : TimePoint.BeginningOfTime;
                        if (from.before(trackingTimeRange.from())) { // otherwise we'd be intersecting with an empty time range
                            final MultiTimeRange beforeRange = coveredTimeRanges
                                    .intersection(new TimeRangeImpl(from, trackingTimeRange.from()));
                            // starting to load newer ranges to make the first found fix the best available fix
                            Collection<TimeRange> inverseTimeRanges = Util.addAll(beforeRange,
                                    new TreeSet<>((timeRange1, timeRange2) -> -timeRange1.from().compareTo(timeRange2.from())));
                            for (TimeRange timeRange : inverseTimeRanges) {
                                try {
                                    if (sensorFixStore.<GPSFix> loadYoungestFix(fix -> fixesBefore.add(fix), event.getDevice(), timeRange)) {
                                        // new best fix before start of tracking found
                                        break;
                                    }
                                } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
                                    logger.log(Level.WARNING, "Could not load better fix for mark track " + event.getMappedTo());
                                }
                            }
                        }
                        final TimePoint firstFixTimePointAfterEndOfTracking;
                        if (!fixesAfter.isEmpty()) {
                            firstFixTimePointAfterEndOfTracking = fixesAfter.first().getTimePoint();
                        } else {
                            final GPSFix firstFixAtOrAfterEndOfTracking = track.getFirstFixAtOrAfter(trackingTimeRange.to());
                            firstFixTimePointAfterEndOfTracking = firstFixAtOrAfterEndOfTracking == null ? null : firstFixAtOrAfterEndOfTracking.getTimePoint();
                        }
                        // A better fix after end of tracking must be before the current best fix
                        final TimePoint to = firstFixTimePointAfterEndOfTracking != null ? firstFixTimePointAfterEndOfTracking : TimePoint.EndOfTime;
                        if (to.after(trackingTimeRange.to())) { // otherwise we'd be intersecting with an empty time range
                            MultiTimeRange afterRange = coveredTimeRanges.intersection(new TimeRangeImpl(trackingTimeRange.to(), to));
                            for (TimeRange timeRange : afterRange) {
                                try {
                                    if (sensorFixStore.<GPSFix> loadOldestFix(fix -> fixesAfter.add(fix), event.getDevice(), timeRange)) {
                                        // new best fix after end of tracking found
                                        break;
                                    }
                                } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
                                    logger.log(Level.WARNING, "Could not load better fix for mark track " + event.getMappedTo());
                                }
                                
                            }
                        }
                    }
                });
            });
        }
        final List<GPSFix> result = new ArrayList<>();
        if (!fixesBefore.isEmpty()) {
            result.add(fixesBefore.last());
        }
        if (!fixesAfter.isEmpty()) {
            result.add(fixesAfter.first());
        }
        return result;
    }

    private TimeRange getTrackingTimeRange() {
        final TimePoint startOfTracking = trackedRace.getStartOfTracking();
        // in case (erroneously) startOfTracking is *after* endOfTracking, return an empty interval starting and ending
        // at startOfTracking (see also bug 5354).
        final TimePoint endOfTracking = startOfTracking != null && trackedRace.getEndOfTracking() != null &&
                startOfTracking.after(trackedRace.getEndOfTracking()) ? startOfTracking : trackedRace.getEndOfTracking();
        return new TimeRangeImpl(startOfTracking == null ? TimePoint.BeginningOfTime : startOfTracking,
                endOfTracking == null ? TimePoint.EndOfTime : endOfTracking);
    }

    /**
     * Stops this {@link FixLoaderAndTracker}. No more fixes are loaded on model changes and no new fixes are being
     * tracked.<br>
     * If stopping non-preemtively, all already started loading jobs are finished. When GPS fixes are being loaded from
     * TracTrac for archived races, loading is automatically stopped. Finishing already started loading jobs ensures,
     * that e.g. bravo fixes are completely loaded even if loading from TracTrac is faster.<br>
     * If stopping preemptively, the call will block until all already started loading jobs are aborted or finished.
     * 
     * @param willBeRemoved
     *            if {@code true} then not only will tracking for the race be stopped, but the race is expected to be
     *            removed shortly by the caller; therefore it does not make sense to resume cache calculations for
     *            the race. The race status will then be set to {@link TrackedRaceStatusEnum#REMOVED} instead of
     *            {@link TrackedRaceStatusEnum#FINISHED}.
     */
    public void stop(boolean preemptive, boolean willBeRemoved) {
        preemptiveStopRequested.set(preemptive);
        willBeRemovedAfterStopping.set(willBeRemoved);
        stopRequested.set(true);
        trackedRace.removeListener(raceChangeListener);
        deviceMappings.stop();
        synchronized (loadingJobs) {
            if (loadingJobs.isEmpty()) {
                setStatusAndProgress(willBeRemoved ? TrackedRaceStatusEnum.REMOVED : TrackedRaceStatusEnum.FINISHED, 1.0);
            }
        }
        sensorFixStore.removeListener(listener);
    }

    private void startTracking() {
        // start out LOADING; if no mappings are found, the final updateStatusAndProgress() will recognize and switch to TRACKING
        setStatusAndProgress(TrackedRaceStatusEnum.LOADING, 0.0);
        this.deviceMappings = new FixLoaderDeviceMappings(trackedRace.getAttachedRegattaLogs(),
                trackedRace.getRace().getName());
        trackedRace.addListener(raceChangeListener);
        this.deviceMappings.updateMappings();
        updateStatusAndProgress(); // will switch to TRACKING immediately if no loading jobs are running based on the device mappings found
    }

    private void loadFixesForExtendedTimeRange(final TimeRange extendedTimeRange) {
        deviceMappings.forEachItemAndCoveredTimeRanges((item, mappingsAndCoveredTimeRanges) -> addLoadingJob(
                new LoadFixesInTrackingTimeRangeJob(mappingsAndCoveredTimeRanges, extendedTimeRange)));
    }
    
    private void loadFixesWhenStartOfTrackingIsReceived() {
        deviceMappings.forEachItemAndCoveredTimeRanges((item, mappingsAndCoveredTimeRanges) -> addLoadingJob(
                new LoadFixesForNewlyCoveredTimeRangesJob(item, mappingsAndCoveredTimeRanges)));
    }

    private void setStatusAndProgress(TrackedRaceStatusEnum status, double progress) {
        trackedRace.onStatusChanged(this, new TrackedRaceStatusImpl(status, progress));
    }
    
    /**
     * Updates the {@link FixLoaderAndTracker}'s overall state on the {@link TrackedRace} based on the progresses of
     * {@link #loadingJobs}.
     */
    private void updateStatusAndProgressWithErrorHandling() {
        try {
            updateStatusAndProgress();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error while updating status and progress for FixLoaderAndTracker", e);
        }
    }

    /**
     * Updates the {@link FixLoaderAndTracker}'s overall status on the {@link TrackedRace} based on the progresses of
     * {@link #loadingJobs}.
     */
    private void updateStatusAndProgress() {
        synchronized (loadingJobs) {
            final TrackedRaceStatusEnum status;
            final double progress;
            if (!loadingJobs.isEmpty()) {
                double progressSum = 0.0;
                boolean allFinished = true;
                for (AbstractLoadingJob loadingJob : loadingJobs) {
                    allFinished &= loadingJob.isFinished();
                    progressSum += loadingJob.getProgress();
                }
                if (allFinished) {
                    loadingJobs.clear();
                    status = stopRequested.get() ? willBeRemovedAfterStopping.get() ? TrackedRaceStatusEnum.REMOVED : TrackedRaceStatusEnum.FINISHED : TrackedRaceStatusEnum.TRACKING;
                    progress = 1.0;
                } else {
                    progress = progressSum / loadingJobs.size();
                    status = TrackedRaceStatusEnum.LOADING;
                }
            } else {
                status = stopRequested.get() ? willBeRemovedAfterStopping.get() ? TrackedRaceStatusEnum.REMOVED : TrackedRaceStatusEnum.FINISHED : TrackedRaceStatusEnum.TRACKING;
                progress = 1.0;
            }
            setStatusAndProgress(status, progress);
            loadingJobs.notifyAll();
        }
    }
    
    /**
     * Adds a {@link AbstractLoadingJob} to track its loading state and updates the {@link FixLoaderAndTracker}'s
     * overall state on the {@link TrackedRace}.
     */
    private void addLoadingJob(AbstractLoadingJob job) {
        synchronized (loadingJobs) {
            loadingJobs.add(job);
            updateStatusAndProgress();
        }
        executor.execute(job); // nothing security-related happening here, and we may not have a Subject/session; no need to associate with a Subject
    }
    
    /**
     * Used for testing purposes only. 
     */
    public boolean isStopRequested() {
        return stopRequested.get();
    }

    private class FixLoaderDeviceMappings extends RegattaLogDeviceMappings<WithID> {
        public FixLoaderDeviceMappings(Iterable<RegattaLog> initialRegattaLogs, String raceNameForLock) {
            super(initialRegattaLogs, raceNameForLock);
        }
        
        @Override
        protected void deviceIdAdded(DeviceIdentifier deviceIdentifier) {
            sensorFixStore.addListener(listener, deviceIdentifier);
        }
        
        @Override
        protected void deviceIdRemoved(DeviceIdentifier deviceIdentifier) {
            sensorFixStore.removeListener(listener, deviceIdentifier);
        }
        
        @Override
        protected void newTimeRangesCovered(WithID item,
            Map<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> newlyCoveredTimeRanges) {
            if (trackedRace.getStartOfTracking() != null) {
                addLoadingJob(new LoadFixesForNewlyCoveredTimeRangesJob(item, newlyCoveredTimeRanges));
            }
        }
    }
    
    /**
     * Abstract implementation of a job to load fixes into tracks that supports tracking the loading progress.
     * Subclasses are intended to be run using an executor.
     */
    private abstract class AbstractLoadingJob implements Runnable {
        volatile double currentProgress = 0d;
        volatile boolean isFinished = false;

        @Override
        public final void run() {
            updateStatusAndProgressWithErrorHandling();
            try {
                load(this::updateProgress, preemptiveStopRequested::get);
            } catch (Throwable e) {
                logger.log(Level.SEVERE, "Error while loading fixes, this is most likely a critical error! ", e);
            }
            finally {
                isFinished = true;
                updateProgress(1d);
            }
        }
        
        private void updateProgress(double newProgress) {
            currentProgress = newProgress;
            updateStatusAndProgressWithErrorHandling();
        }

        public boolean isFinished() {
            return isFinished;
        }

        public double getProgress() {
            return currentProgress;
        }

        protected abstract void load(Consumer<Double> progressConsumer, BooleanSupplier stopCallback);
    }
    
    public class MultiJobProgressUpdater {
        volatile double[] currentProgress = new double[1];

        public MultiJobProgressUpdater(int nrOfJobs) {
            super();
            currentProgress = new double[nrOfJobs];
        }
        
        void updateProgress(int jobNr, double progress) {
            if (jobNr >= 0 && jobNr < currentProgress.length) {
                currentProgress[jobNr] = progress;
            }
            
        }
        public double getProgress() {
            double progress = 0d;
            for (int i = 0; i < currentProgress.length; i++) {
                progress += currentProgress[i] / currentProgress.length;
            }
            return progress;
        }
    }
    /**
     * Loads fixes for an item's mappings in a defined tracking {@link TimeRange}. This is used when the tracking
     * {@link TimeRange} is extended. No better fixes for {@link Mark} are being loaded because if available, these must
     * have either already been loaded or the best fix is inside of the extended {@link TimeRange} that is completely loaded by
     * this job.
     */
    private class LoadFixesInTrackingTimeRangeJob extends AbstractLoadingJob {

        private final Map<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> newlyCoveredTimeRanges;
        private final TimeRange trackingTimeRange;

        public LoadFixesInTrackingTimeRangeJob(Map<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> newlyCoveredTimeRanges,
                TimeRange trackingTimeRange) {
            this.newlyCoveredTimeRanges = newlyCoveredTimeRanges;
            this.trackingTimeRange = trackingTimeRange;
        }
        
        @Override
        protected void load(Consumer<Double> progressConsumer, BooleanSupplier stopCallback) {
            loadFixesInTrackingTimeRange(newlyCoveredTimeRanges, trackingTimeRange, progressConsumer, stopCallback);
        }
    }
    
    /**
     * This is used when device mappings for an item changed so that fixes in a new {@link TimeRange} are covered. This is also used when initially loading fixes due to startOfTracking being initially set. If
     * the mapping is a {@link Mark}, best fixes outside of the tracking {@link TimeRange} are loaded if none is
     * available in the tracking {@link TimeRange}.
     */
    private class LoadFixesForNewlyCoveredTimeRangesJob extends AbstractLoadingJob {
        private final WithID item;
        private final Map<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> newlyCoveredTimeRanges;

        public LoadFixesForNewlyCoveredTimeRangesJob(WithID item,
                Map<RegattaLogDeviceMappingEvent<WithID>, MultiTimeRange> newlyCoveredTimeRanges) {
            this.item = item;
            this.newlyCoveredTimeRanges = newlyCoveredTimeRanges;
        }
        
        @Override
        protected void load(Consumer<Double> progressConsumer, BooleanSupplier stopCallback) {
            loadFixesForNewlyCoveredTimeRanges(item, newlyCoveredTimeRanges, progressConsumer, stopCallback);
        }
    }
}
