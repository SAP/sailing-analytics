package com.sap.sailing.domain.abstractlog.regatta.events.impl;

import java.io.Serializable;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.regatta.MappingEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorSensorDataMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sse.common.TimePoint;

/**
 * Mapping event for devices mapped for Bravo type fixes.
 */
public class RegattaLogDeviceCompetitorBravoMappingEventImpl extends RegattaLogDeviceMappingEventImpl<Competitor>
        implements RegattaLogDeviceCompetitorSensorDataMappingEvent {
    private static final long serialVersionUID = -1494030544804758753L;


    public RegattaLogDeviceCompetitorBravoMappingEventImpl(TimePoint createdAt, TimePoint logicalTimePoint,
            AbstractLogEventAuthor author, Serializable pId, Competitor mappedTo, DeviceIdentifier device,
            TimePoint from, TimePoint to) {
        super(createdAt, logicalTimePoint, author, pId, mappedTo, device, from, to);
    }

    public RegattaLogDeviceCompetitorBravoMappingEventImpl(TimePoint logicalTimePoint, AbstractLogEventAuthor author,
            Competitor mappedTo, DeviceIdentifier device, TimePoint from, TimePoint to) {
        super(logicalTimePoint, author, mappedTo, device, from, to);
    }

    @Override
    public void accept(RegattaLogEventVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void accept(MappingEventVisitor visitor) {
        visitor.visit(this);
    }
}
