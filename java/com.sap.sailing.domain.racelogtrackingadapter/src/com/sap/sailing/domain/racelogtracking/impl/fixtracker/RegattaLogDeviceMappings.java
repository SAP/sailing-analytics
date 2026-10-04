package com.sap.sailing.domain.racelogtracking.impl.fixtracker;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogCloseOpenEndedDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceBoatSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMarkMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.regatta.impl.BaseRegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceMappingFinder;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.racelog.tracking.DoesNotHaveRegattaLogException;
import com.sap.sailing.domain.racelogtracking.DeviceMapping;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.MultiTimeRangeImpl;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;

/**
 * Holds DeviceMappings to make it possible to track changes. This makes it possible to only process mappings that
 * weren't processed before.
 *
 * @param <ItemT> The type of items the DeviceMappings are mapped to
 */
public abstract class RegattaLogDeviceMappings<ItemT extends WithID> {
    private static final Logger logger = Logger.getLogger(RegattaLogDeviceMappings.class.getName());
    /**
     * We maintain our own collection that holds the RegattaLogs. The known RegattaLogs should by in sync with the ones that
     * can be obtained from the TrackedRace. When stopping, there could be a concurrency issue that leads to a listener
     * not being removed. This is prevented by remembering all RegattaLogs to which we attached a listener. So we can be
     * sure to not produce a memory leak.
     */
    private final Set<RegattaLog> knownRegattaLogs = new HashSet<>();

    /**
     * Lock object to be used when accessing {@link #knownRegattaLogs}.
     */
    private final NamedReentrantReadWriteLock knownRegattaLogsLock;
    
    /**
     * Lock object to be used when accessing {@link #mappings} or {@link #mappingsByDevice}.
     */
    private final NamedReentrantReadWriteLock mappingsLock;

    private final Map<ItemT, List<DeviceMappingWithRegattaLogEvent<ItemT>>> mappings = new HashMap<>();
    private final Map<DeviceIdentifier, List<DeviceMappingWithRegattaLogEvent<ItemT>>> mappingsByDevice = new HashMap<>();
    
    /**
     * A cache that holds the device mappings as the {@link Pair#getB() second} component of the values in this map,
     * such that exactly these device mappings apply for any time point {@link TimeRange#includes(TimePoint) included}
     * by the {@link TimeRange} that is the {@link Pair#getA() first} component of a value in this map. This map's keys
     * match with the {@link DeviceMapping#getDevice() device identifiers} of the {@link Pair#getB() second} components
     * of their corresponding values.
     * <p>
     * 
     * This cache is designed to work well for cases where mappings change at a frequency orders of magnitude less than
     * the frequency with which fixes arrive and are to be mapped to items. Furthermore, the cache hit rates benefit
     * from mappings covering large time ranges.
     * <p>
     * 
     * Any change to the mappings for a device will remove the mapping for the device's {@link DeviceIdentifier
     * identifier} from this map.
     * <p>
     * 
     * Access to this map has to undergo the same locking drill as any access to {@link #mappings}, using the
     * {@link #mappingsLock}.
     */
    private final Map<DeviceIdentifier, Pair<TimeRange, List<DeviceMappingWithRegattaLogEvent<ItemT>>>> cachedMappings = new HashMap<>();
    
    /**
     * When the {@link #cachedMappings} are to be updated, the corresponding update job is stored in this field. It must be executed
     * under the {@link #mappingsLock}'s write lock. However, when other updates to {@link #mappings} or {@link #mappingsByDevice}
     * are performed (usually by the {@link #updateMappingsInternal()} method), this field will be cleared because the cache update
     * will most likely be obsolete.
     */
    private Runnable cacheUpdateJob;
    
    private int cacheHits;
    private int cacheMisses;
    
    private final RegattaLogEventVisitor regattaLogEventVisitor = new BaseRegattaLogEventVisitor() {
        @Override
        public void visit(RegattaLogDeviceCompetitorSensorDataMappingEvent event) {
            logger.log(Level.FINE, "New CompetitorSensorDataMapping for: " + event.getMappedTo() + "; device: " + event.getDevice());
            updateMappings();
        }
        @Override
        public void visit(RegattaLogDeviceBoatSensorDataMappingEvent event) {
            logger.log(Level.FINE, "New BoatSensorDataMapping for: " + event.getMappedTo() + "; device: " + event.getDevice());
            updateMappings();
        }

        @Override
        public void visit(RegattaLogDeviceMarkMappingEvent event) {
            logger.log(Level.FINE,
                    "New DeviceMarkMapping for: " + event.getMappedTo() + "; device: " + event.getDevice());
            updateMappings();
        }

        @Override
        public void visit(RegattaLogDeviceCompetitorMappingEvent event) {
            logger.log(Level.FINE,
                    "New DeviceCompetitorMapping for: " + event.getMappedTo() + "; device: " + event.getDevice());
            updateMappings();
        }
        
        public void visit(RegattaLogDeviceBoatMappingEvent event) {
            logger.log(Level.FINE,
                    "New DeviceBoatMappingEvent for: " + event.getMappedTo() + "; device: " + event.getDevice());
            updateMappings();
        }

        @Override
        public void visit(RegattaLogCloseOpenEndedDeviceMappingEvent event) {
            logger.log(Level.FINE, "CloseOpenEndedDeviceMapping closed: " + event.getDeviceMappingEventId());
            updateMappings();
        }

        @Override
        public void visit(RegattaLogRevokeEvent event) {
            logger.log(Level.FINE, "Mapping revoked for: " + event.getRevokedEventId());
            updateMappings();
        };
    };
    
    public RegattaLogDeviceMappings(Iterable<RegattaLog> initialRegattaLogs, String raceNameForLock) {
        mappingsLock = new NamedReentrantReadWriteLock("DeviceMapping lock for race " + raceNameForLock, false);
        knownRegattaLogsLock = new NamedReentrantReadWriteLock("Lock for known RegattaLogs of race " + raceNameForLock, false);
        final boolean hasRegattaLogs;
        LockUtil.lockForWrite(knownRegattaLogsLock);
        try {
            initialRegattaLogs.forEach(this::addRegattaLogUnlocked);
            hasRegattaLogs = !knownRegattaLogs.isEmpty();
        } finally {
            LockUtil.unlockAfterWrite(knownRegattaLogsLock);
        }
        if (hasRegattaLogs) {
            updateMappings();
        }
    }
    
    public void addRegattaLog(RegattaLog regattaLog) {
        LockUtil.executeWithWriteLock(knownRegattaLogsLock, () -> addRegattaLogUnlocked(regattaLog));
        updateMappings();
    }

    private void addRegattaLogUnlocked(RegattaLog log) {
        log.addListener(regattaLogEventVisitor);
        knownRegattaLogs.add(log);
    }
    
    public void stop() {
        LockUtil.executeWithWriteLock(knownRegattaLogsLock, () -> {
            knownRegattaLogs.forEach((log) -> log.removeListener(regattaLogEventVisitor));
        });
    }
    
    public void updateMappings() {
        try {
            updateMappingsInternal();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Could not update device mappings", e);
        }
    }
    
    /**
     * Calls the given callback for every DeviceMapping that is known for the given {@link DeviceIdentifier} that
     * includes the given {@link TimePoint}.<p>
     * 
     * Searches the {@link #cachedMappings} for a match for the {@code device}; if found, checks whether {@code timePoint}
     * is within the time range for which device mappings were cached, and if so, uses those device mappings. Otherwise,
     * the device mappings are calculated and a cache update is carried out after releasing the read-lock of
     * {@link #mappingsLock} and after obtaining its write-lock. Should an update have been squeezed in between releasing
     * the read-lock and obtaining the write-lock, the cache update is not carried out.
     * 
     * @param device
     *            the device to get the mappings for
     * @param timePoint
     *            the TimePoint to check DeviceMappings for
     * @param callback
     *            the callback to call for every known mapping of the given {@link DeviceIdentifier} that includes the
     *            given {@link TimePoint}.
     */
    public void forEachMappingOfDeviceIncludingTimePoint(DeviceIdentifier device, TimePoint timePoint,
            Consumer<DeviceMappingWithRegattaLogEvent<ItemT>> callback) {
        LockUtil.executeWithReadLock(mappingsLock, () -> {
            final Pair<TimeRange, List<DeviceMappingWithRegattaLogEvent<ItemT>>> cachedTimeRangeForDevice = cachedMappings.get(device);
            if (cachedTimeRangeForDevice != null && cachedTimeRangeForDevice.getA().includes(timePoint)) {
                cacheHits++;
                logger.fine(() -> "Device mapping cache hit for mapper " + this + " for device " + device
                        + " and time point " + timePoint + ", included in cached range "
                        + cachedTimeRangeForDevice.getA() + "; " + cacheHits + " hits, " + cacheMisses + " misses");
                cachedTimeRangeForDevice.getB().forEach(mapping->callback.accept(mapping));
            } else {
                final List<DeviceMappingWithRegattaLogEvent<ItemT>> mappingsForDevice = mappingsByDevice.get(device);
                TimeRange timeRangeForCache = null;
                final List<DeviceMappingWithRegattaLogEvent<ItemT>> deviceMappingsForCache = new LinkedList<>();
                cacheMisses++;
                if (mappingsForDevice != null) {
                    for (final DeviceMappingWithRegattaLogEvent<ItemT> mapping : mappingsForDevice) {
                        if (mapping.getTimeRange().includes(timePoint)) {
                            if (timeRangeForCache == null) {
                                timeRangeForCache = mapping.getTimeRange();
                            } else {
                                timeRangeForCache = timeRangeForCache.intersection(mapping.getTimeRange());
                            }
                            deviceMappingsForCache.add(mapping);
                            callback.accept(mapping);
                        }
                    }
                }
                final TimeRange finalTimeRangeForCache = timeRangeForCache;
                logger.fine(() -> "Device mapping cache miss for mapper " + this + " for device " + device
                        + " and time point " + timePoint + ", determined cachable range "
                        + finalTimeRangeForCache + "; " + cacheHits + " hits, " + cacheMisses + " misses");
                if (timeRangeForCache != null) {
                    cacheUpdateJob = ()->cachedMappings.put(device, new Pair<>(finalTimeRangeForCache, deviceMappingsForCache));
                }
            }
        });
        if (cacheUpdateJob != null) {
            LockUtil.executeWithWriteLock(mappingsLock, ()->{
                if (cacheUpdateJob != null) {
                    logger.fine(()-> "Device mapping cache miss for mapper " + this + " performs cache update.");
                    cacheUpdateJob.run();
                } else {
                    logger.fine(() -> "Device mapping cache miss for mapper " + this
                            + " does not update the cache because the mappings were updated in between");
                }
            });
        }
    }
    
    public void forEachItemAndCoveredTimeRanges(final BiConsumer<ItemT, Map<RegattaLogDeviceMappingEvent<ItemT>, MultiTimeRange>> consumer) {
        HashMap<ItemT, List<DeviceMappingWithRegattaLogEvent<ItemT>>> allMappings = LockUtil.executeWithReadLockAndResult(mappingsLock, () -> new HashMap<>(mappings));
        allMappings.forEach((item, mappings) -> {
            final Map<RegattaLogDeviceMappingEvent<ItemT>, MultiTimeRange> coveredTimeRanges = calculateCoveredTimeRanges(mappings);
            consumer.accept(item, coveredTimeRanges);
        });
    }

    /**
     * Calculates an association of mapping events to the covered {@link MultiTimeRange}.
     */
    private Map<RegattaLogDeviceMappingEvent<ItemT>, MultiTimeRange> calculateCoveredTimeRanges(
            final List<DeviceMappingWithRegattaLogEvent<ItemT>> mappingsForItem) {
        final Map<RegattaLogDeviceMappingEvent<ItemT>, MultiTimeRange> coveredTimeRanges = new HashMap<>();
        if (mappingsForItem != null) {
            Map<Pair<DeviceIdentifier, Class<?>>, Iterable<DeviceMappingWithRegattaLogEvent<ItemT>>> groupedMappings = groupMappingsByDeviceIdAndMappingType(mappingsForItem);
            groupedMappings.entrySet().forEach(entry -> {
                Iterable<DeviceMappingWithRegattaLogEvent<ItemT>> mappingsForDeviceIdAndMappingType = entry.getValue();
                final MultiTimeRange coveredTimeRange = getCoveredTimeRange(mappingsForDeviceIdAndMappingType);
                if (!coveredTimeRange.isEmpty()) {
                    coveredTimeRanges.put(Util.get(mappingsForDeviceIdAndMappingType, 0).getRegattaLogEvent(), coveredTimeRange);
                }
            });
        }
        return coveredTimeRanges;
    }
    
    /**
     * To be implemented by subclasses to calculate the current {@link DeviceMapping}s.
     * 
     * @return All currently known DeviceMappings
     */
    protected Map<ItemT, List<DeviceMappingWithRegattaLogEvent<ItemT>>> calculateMappings() {
        Map<ItemT, List<DeviceMappingWithRegattaLogEvent<ItemT>>> result = new HashMap<>();
        forEachRegattaLog(
                (log) -> result.putAll(new RegattaLogDeviceMappingFinder<ItemT>(log).analyze()));
        return result;
    }

    protected void forEachRegattaLog(Consumer<RegattaLog> regattaLogConsumer) {
        LockUtil.executeWithReadLock(knownRegattaLogsLock, () -> knownRegattaLogs.forEach(regattaLogConsumer));
    }

    /**
     * Adjusts the {@link #mappings} map according to the device mappings provided from the {@link #calculateMappings()}
     * method. Afterwards, the start and end of tracking is {@link #updateStartAndEndOfTracking() updated} from the
     * mapping intervals.
     * 
     * @param loadIfNotCovered
     *            if <code>true</code>, the fixes for the mappings will be loaded based on a comparison of the previous
     *            mappings and the new mappings.
     * 
     * @throws DoesNotHaveRegattaLogException
     */
    private final <FixT extends Timed, TrackT extends DynamicTrack<FixT>> void updateMappingsInternal() {
        final Map<ItemT, List<DeviceMappingWithRegattaLogEvent<ItemT>>> newMappings;
        final Map<ItemT, List<DeviceMappingWithRegattaLogEvent<ItemT>>> oldMappings = new HashMap<>();
        final Set<DeviceIdentifier> oldDeviceIds = new HashSet<>();
        final Set<DeviceIdentifier> newDeviceIds = new HashSet<>();
        LockUtil.lockForWrite(mappingsLock);
        try {
            newMappings = calculateMappings();
            oldMappings.putAll(mappings);
            oldDeviceIds.addAll(mappingsByDevice.keySet());
            mappings.clear();
            cachedMappings.clear();
            mappings.putAll(newMappings);
            mappingsByDevice.clear();
            cacheUpdateJob = null;
            for (ItemT item : newMappings.keySet()) {
                for (DeviceMappingWithRegattaLogEvent<ItemT> mapping : newMappings.get(item)) {
                    List<DeviceMappingWithRegattaLogEvent<ItemT>> list = mappingsByDevice.get(mapping.getDevice());
                    if (list == null) {
                        list = new ArrayList<>();
                        mappingsByDevice.put(mapping.getDevice(), list);
                    }
                    list.add(mapping);
                }
            }
            newDeviceIds.addAll(mappingsByDevice.keySet());
        } finally {
            LockUtil.unlockAfterWrite(mappingsLock);
        }
        calculateAndApplyDiff(oldMappings, newMappings, oldDeviceIds, newDeviceIds);
    }
    
    /**
     * Calculates <em>and applies</em> the mapping changes by removing listeners no longer needed for the mappings removed,
     * and by loading and adding the fixes for extended or added mappings.
     */
    private void calculateAndApplyDiff(Map<ItemT, ? extends Iterable<DeviceMappingWithRegattaLogEvent<ItemT>>> previousMappings,
            Map<ItemT, ? extends Iterable<DeviceMappingWithRegattaLogEvent<ItemT>>> newMappings,
            Set<DeviceIdentifier> oldDeviceIds, Set<DeviceIdentifier> newDeviceIds) {
        final Set<DeviceIdentifier> removedDeviceIds = new HashSet<>(oldDeviceIds);
        removedDeviceIds.removeAll(newDeviceIds);
        removedDeviceIds.forEach(this::deviceIdRemovedInternal);
        final Set<DeviceIdentifier> addedDeviceIds = new HashSet<>(newDeviceIds);
        addedDeviceIds.removeAll(oldDeviceIds);
        addedDeviceIds.forEach(this::deviceIdAddedInternal);
        // Only deviceIdentifier and mappingTypes are covered that are contained in newMappings.
        // Those that are only found in oldMappings won't lead to new covered TimeRanges.
        // DeviceIdentifiers, that aren't needed at all are already handled above.
        newMappings.forEach((item, mappingsForItem) -> {
            final Map<RegattaLogDeviceMappingEvent<ItemT>, MultiTimeRange> newlyCoveredTimeRanges = new HashMap<>();
            // The mappings are processes grouped by DeviceIdentifier and mapping type
            // to build a consistent overall mapping update without the risk of fixes
            // to get loaded multiple times
            this.processNewAndChangedMappingsByDeviceIdAndEventType(previousMappings.get(item), mappingsForItem,
                    (deviceIdentifier, mappingType, oldMappingsForDeviceIdAndMappingType,
                            newMappingsForDeviceIdAndMappingType) -> {
                        assert (newMappingsForDeviceIdAndMappingType != null);
                        assert (!Util.isEmpty(newMappingsForDeviceIdAndMappingType));
                        final MultiTimeRange newCoveredTimeRanges = getCoveredTimeRange(
                                newMappingsForDeviceIdAndMappingType)
                                        .subtract(getCoveredTimeRange(oldMappingsForDeviceIdAndMappingType));
                        if (!newCoveredTimeRanges.isEmpty()) {
                            newlyCoveredTimeRanges.put(
                                    Util.get(newMappingsForDeviceIdAndMappingType, 0).getRegattaLogEvent(),
                                    newCoveredTimeRanges);
                        }
                    });
            if (!newlyCoveredTimeRanges.isEmpty()) {
                // all updates for one item are handled at once
                newTimeRangesCoveredInternal(item, newlyCoveredTimeRanges);
            }
        });
    }

    /**
     * Processes the old and new mappings for mapping update by {@link DeviceIdentifier} and mapping type (class of the
     * mapping event). the old and new mappings are first grouped by the already mentioned criteria. For each group in
     * the new mappings, the associated group in the old mappings is identified. For these pairs, the given callback is
     * called. Groups only found in the old mappings are explicitly not handled because these can't lead to new covered
     * time ranges.
     */
    private void processNewAndChangedMappingsByDeviceIdAndEventType(
            Iterable<DeviceMappingWithRegattaLogEvent<ItemT>> oldMappings,
            Iterable<DeviceMappingWithRegattaLogEvent<ItemT>> newMappings,
            GroupedOldAndNewMappingsCallback<ItemT> callback) {
        final Map<Pair<DeviceIdentifier, Class<?>>, Iterable<DeviceMappingWithRegattaLogEvent<ItemT>>> groupedOldMappings = groupMappingsByDeviceIdAndMappingType(
                oldMappings != null ? oldMappings : Collections.emptySet());
        final Map<Pair<DeviceIdentifier, Class<?>>, Iterable<DeviceMappingWithRegattaLogEvent<ItemT>>> groupedNewMappings = groupMappingsByDeviceIdAndMappingType(
                newMappings);
        groupedNewMappings.forEach((key, newMappingsForDeviceIdAndMappingType) -> {
            Iterable<DeviceMappingWithRegattaLogEvent<ItemT>> oldMappingsForDeviceIdAndMappingType = groupedOldMappings
                    .get(key);
            callback.process(
                    key.getA(), key.getB(), oldMappingsForDeviceIdAndMappingType != null
                            ? oldMappingsForDeviceIdAndMappingType : Collections.emptySet(),
                    newMappingsForDeviceIdAndMappingType);
        });
    }
    
    /**
     * Groups the given {@link DeviceMapping}s by pairs of {@link DeviceIdentifier} and mapping type (class of the
     * mapping event).
     */
    private Map<Pair<DeviceIdentifier, Class<?>>, Iterable<DeviceMappingWithRegattaLogEvent<ItemT>>> groupMappingsByDeviceIdAndMappingType(
            Iterable<DeviceMappingWithRegattaLogEvent<ItemT>> mappings) {
        return Util.group(mappings, value -> new Pair<>(value.getDevice(), value.getEventType()), HashSet::new);
    }
    
    /**
     * Calculates the {@link MultiTimeRange} as union of the {@link TimeRange}s of the given mappings.
     */
    private MultiTimeRange getCoveredTimeRange(Iterable<DeviceMappingWithRegattaLogEvent<ItemT>> mappings) {
        MultiTimeRange result = new MultiTimeRangeImpl();
        for (DeviceMappingWithRegattaLogEvent<ItemT> mapping : mappings) {
            result = result.union(mapping.getTimeRange());
        }
        return result;
    }

    /**
     * Internal callback interface for {@link #processNewAndChangedMappingsByDeviceIdAndEventType}.
     */
    private interface GroupedOldAndNewMappingsCallback<ItemT extends WithID> {
        void process(DeviceIdentifier deviceIdentifier, Class<?> mappingType,
                Iterable<DeviceMappingWithRegattaLogEvent<ItemT>> oldMappings,
                Iterable<DeviceMappingWithRegattaLogEvent<ItemT>> newMappings);
    }

    /**
     * Called when at least one mapping for a previously not available {@link DeviceIdentifier} was added.
     */
    protected abstract void deviceIdAdded(DeviceIdentifier deviceIdentifier);

    private void deviceIdAddedInternal(DeviceIdentifier deviceIdentifier) {
        try {
            deviceIdAdded(deviceIdentifier);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while adding deviceIdentifier " + deviceIdentifier, e);
        }
    }

    /**
     * Called when the last available mapping for a {@link DeviceIdentifier} was removed.
     */
    protected abstract void deviceIdRemoved(DeviceIdentifier deviceIdentifier);

    private void deviceIdRemovedInternal(DeviceIdentifier deviceIdentifier) {
        try {
            deviceIdRemoved(deviceIdentifier);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "error while removing deviceIdentifier " + deviceIdentifier, e);
        }
    }

    /**
     * Called when a new MultiTimeRange could be identified that is now covered by the mappings for a item,
     * DeviceIdentifier and mappingType.
     */
    protected abstract void newTimeRangesCovered(ItemT item, Map<RegattaLogDeviceMappingEvent<ItemT>, MultiTimeRange> newlyCoveredTimeRanges);

    private void newTimeRangesCoveredInternal(ItemT item, Map<RegattaLogDeviceMappingEvent<ItemT>, MultiTimeRange> newlyCoveredTimeRanges) {
        try {
            newTimeRangesCovered(item, newlyCoveredTimeRanges);
        } catch (Exception e) {
            logger.log(Level.SEVERE,
                    "error while calling newTimeRangeCovered for item " + item, e);
        }
    }
}
