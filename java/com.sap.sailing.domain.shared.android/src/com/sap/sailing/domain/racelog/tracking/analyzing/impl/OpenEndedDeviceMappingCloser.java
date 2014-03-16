package com.sap.sailing.domain.racelog.tracking.analyzing.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogEventFactory;
import com.sap.sailing.domain.racelog.analyzing.impl.RaceLogAnalyzer;
import com.sap.sailing.domain.racelog.tracking.CloseOpenEndedDeviceMappingEvent;
import com.sap.sailing.domain.racelog.tracking.DeviceMapping;
import com.sap.sailing.domain.racelog.tracking.DeviceMappingEvent;

/**
 * Generates events that close the events that are responsible for the {@link #mapping}
 * that is passed to the constructor.
 * @author Fredrik Teschke
 *
 */
public class OpenEndedDeviceMappingCloser extends RaceLogAnalyzer<List<CloseOpenEndedDeviceMappingEvent>> {
    private final DeviceMapping<?> mapping;
    private final RaceLogEventAuthor author;
    private final TimePoint closingTimePoint;

    public OpenEndedDeviceMappingCloser(RaceLog raceLog, DeviceMapping<?> mapping, RaceLogEventAuthor author,
            TimePoint closingTimePoint) {
        super(raceLog);
        this.mapping = mapping;
        this.author = author;
        this.closingTimePoint = closingTimePoint;
    }

    @Override
    protected List<CloseOpenEndedDeviceMappingEvent> performAnalysis() {
        List<CloseOpenEndedDeviceMappingEvent> result = new ArrayList<CloseOpenEndedDeviceMappingEvent>();

        for (Serializable eventId : mapping.getOriginalRaceLogEventIds()) {
            DeviceMappingEvent<?> event = (DeviceMappingEvent<?>) raceLog.getEventById(eventId);
            if (event.getFrom() == null || event.getTo() == null) {
                result.add(RaceLogEventFactory.INSTANCE.createCloseOpenEndedDeviceMappingEvent(
                        MillisecondsTimePoint.now(), author, raceLog.getCurrentPassId(), event.getId(), closingTimePoint));
            }
        }
        return result;
    }
}
