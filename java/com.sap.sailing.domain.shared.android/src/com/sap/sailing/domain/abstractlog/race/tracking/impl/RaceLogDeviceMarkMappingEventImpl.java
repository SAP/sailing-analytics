package com.sap.sailing.domain.abstractlog.race.tracking.impl;

import java.io.Serializable;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDeviceMarkMappingEvent;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sse.common.TimePoint;

@Deprecated //see bug2851
public class RaceLogDeviceMarkMappingEventImpl extends AbstractRaceLogDeviceMappingEventImpl<Mark> implements
        RaceLogDeviceMarkMappingEvent {
    private static final long serialVersionUID = -7223543830755457196L;

    public RaceLogDeviceMarkMappingEventImpl(TimePoint createdAt, TimePoint logicalTimePoint,
            AbstractLogEventAuthor author, Serializable pId, int pPassId, Mark mappedTo, DeviceIdentifier device,
            TimePoint from, TimePoint to) {
        super(createdAt, logicalTimePoint, author, pId, pPassId, mappedTo, device, from, to);
    }

    public RaceLogDeviceMarkMappingEventImpl(TimePoint logicalTimePoint, AbstractLogEventAuthor author, int pPassId,
            Mark mappedTo, DeviceIdentifier device, TimePoint from, TimePoint to) {
        super(logicalTimePoint, author, pPassId, mappedTo, device, from, to);
    }

    @Override
    public void accept(RaceLogEventVisitor visitor) {
        visitor.visit(this);
    }
}
