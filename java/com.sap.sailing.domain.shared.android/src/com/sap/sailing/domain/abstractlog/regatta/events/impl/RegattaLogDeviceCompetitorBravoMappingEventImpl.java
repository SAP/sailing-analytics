package com.sap.sailing.domain.abstractlog.regatta.events.impl;

import java.io.Serializable;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sse.common.TimePoint;

/**
 * Mapping event for devices mapped for Bravo type fixes.
 */
public class RegattaLogDeviceCompetitorBravoMappingEventImpl extends AbstractRegattaLogDeviceCompetitorSensorDataMappingEventImpl {
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
}
