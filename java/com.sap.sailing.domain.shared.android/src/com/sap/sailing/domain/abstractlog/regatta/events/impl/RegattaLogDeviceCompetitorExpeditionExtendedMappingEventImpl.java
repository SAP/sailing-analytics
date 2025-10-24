package com.sap.sailing.domain.abstractlog.regatta.events.impl;

import java.io.Serializable;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sse.common.TimePoint;

/**
 * Mapping event for devices mapped for "Expedition" extended type fixes, coming from the
 * "Expedition" software that was originally used in the context of this solution only for
 * handling wind data but now is also used to forward sensor data into competitor tracks.
 */
public class RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl extends AbstractRegattaLogDeviceCompetitorSensorDataMappingEventImpl {
    private static final long serialVersionUID = -7865925670153838864L;

    public RegattaLogDeviceCompetitorExpeditionExtendedMappingEventImpl(TimePoint createdAt, TimePoint logicalTimePoint,
            AbstractLogEventAuthor author, Serializable pId, Competitor mappedTo, DeviceIdentifier device,
            TimePoint from, TimePoint to) {
        super(createdAt, logicalTimePoint, author, pId, mappedTo, device, from, to);
    }
}
