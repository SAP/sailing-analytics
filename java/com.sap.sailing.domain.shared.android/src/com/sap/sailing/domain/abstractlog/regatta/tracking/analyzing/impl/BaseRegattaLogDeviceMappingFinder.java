package com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogCloseOpenEndedDeviceMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEvent;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.racelogtracking.DeviceMapping;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.WithID;
import com.sap.sse.common.impl.TimeRangeImpl;

/**
 * Extracts the {@link DeviceMapping}s with the appropriate {@link TimeRange}s from the {@link RaceLog}.
 * Based on the {@link RegattaLogDeviceMappingEvent}s found in the {@code RaceLog} for the tracked {@code items}
 * ({@code Competitor}s and {@code Marks}), the actual mappings are created.
 * <p>
 * Mappings can be defined with an open-ended time range (e.g. track this from now on), which should then be
 * closed by a corresponding {@link RegattaLogCloseOpenEndedDeviceMappingEvent}.
 * <p>
 * The process of removing conflicts between mappings to ensure that only one mapping was active for
 * one tracked item has been removed. Instead, every {@code DeviceMappingEvent} now results directly in one
 * {@code DeviceMapping}, the only changes being introduced by closing open time ranges in cases where an
 * according {@code CloseOpenEndedDeviceMappingEvent} exists.
 */
public abstract class BaseRegattaLogDeviceMappingFinder<ItemT extends WithID>
        extends RegattaLogDeviceMappingFinder<ItemT> {
    
    public BaseRegattaLogDeviceMappingFinder(RegattaLog log) {
        super(log);
    }

    /**
     * By revoking existing mappings for {@code item} and replacing them by mappings that exclude {@code fixTimePoint},
     * any fix at {@code fixTimePoint} will no longer be linked to {@code item}. If a mapping is limited to the exact
     * fix time point, the mapping is only revoked and not replaced. If one boundary is exactly the fix time point, the
     * interval is revoked and replaced by a single interval with that border adjusted so it excludes the fix.
     */
    public void removeTimePointFromMapping(ItemT item, TimePoint fixTimePoint) throws NotRevokableException {
        Map<ItemT, List<RegattaLogDeviceMappingEvent<ItemT>>> events = new HashMap<ItemT, List<RegattaLogDeviceMappingEvent<ItemT>>>();
        Map<Serializable, RegattaLogCloseOpenEndedDeviceMappingEvent> closingEvents = new HashMap<Serializable, RegattaLogCloseOpenEndedDeviceMappingEvent>();
        findUnrevokedMappingAndClosingEvents(events, closingEvents);
        for (final RegattaLogDeviceMappingEvent<ItemT> event : events.get(item)) {
            final TimePoint from = event.getFrom();
            final RegattaLogCloseOpenEndedDeviceMappingEvent closingEvent = closingEvents.get(event.getId());
            final TimePoint toInclusive = closingEvent != null ? closingEvent.getClosingTimePointInclusive() : event.getToInclusive();
            final TimeRange mappingTimeRange = new TimeRangeImpl(from, toInclusive, /* inclusive */ true);
            if (mappingTimeRange.includes(fixTimePoint)) {
                if (closingEvent != null) {
                    log.revokeEvent(closingEvent.getAuthor(), closingEvent,
                            "removing single time point " + fixTimePoint + " from mapping for " + item);
                }
                log.revokeEvent(event.getAuthor(), event,
                        "removing single time point " + fixTimePoint + " from mapping for " + item);
                final TimePoint endOfFirstHalf = fixTimePoint.minusResolution();
                final TimePoint startOfSecondHalf = fixTimePoint.plusResolution();
                if (!endOfFirstHalf.before(from)) {
                    log.add(createDeviceMappingEvent(item, event.getAuthor(), from, endOfFirstHalf, event.getDevice()));
                }
                if (toInclusive == null || !toInclusive.before(startOfSecondHalf)) {
                    log.add(createDeviceMappingEvent(item, event.getAuthor(), startOfSecondHalf, toInclusive,
                            event.getDevice()));
                }
            }
        }
    }

    /**
     * Checks if the regatta log has one or more device mappings for {@code item} that cover the time point
     * {@code fixTimePoint}.
     */
    public boolean hasMappingFor(ItemT item, TimePoint fixTimePoint) {
        boolean result = false;
        Map<ItemT, List<RegattaLogDeviceMappingEvent<ItemT>>> events = new HashMap<ItemT, List<RegattaLogDeviceMappingEvent<ItemT>>>();
        Map<Serializable, RegattaLogCloseOpenEndedDeviceMappingEvent> closingEvents = new HashMap<Serializable, RegattaLogCloseOpenEndedDeviceMappingEvent>();
        findUnrevokedMappingAndClosingEvents(events, closingEvents);
        final List<RegattaLogDeviceMappingEvent<ItemT>> mappingsList = events.get(item);
        if (mappingsList != null) {
            for (final RegattaLogDeviceMappingEvent<ItemT> event : mappingsList) {
                final TimePoint from = event.getFrom();
                final RegattaLogCloseOpenEndedDeviceMappingEvent closingEvent = closingEvents.get(event.getId());
                final TimePoint toInclusive = closingEvent != null ? closingEvent.getClosingTimePointInclusive() : event.getToInclusive();
                final TimeRange mappingTimeRange = new TimeRangeImpl(from, toInclusive, /* inclusive */ true);
                if (mappingTimeRange.includes(fixTimePoint)) {
                    result = true;
                    break;
                }
            }
        }
        return result;
    }

    protected abstract RegattaLogDeviceMappingEvent<ItemT> createDeviceMappingEvent(ItemT item,
            AbstractLogEventAuthor author, TimePoint plus, TimePoint toInclusive, DeviceIdentifier deviceId);

}
