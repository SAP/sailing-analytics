package com.sap.sailing.domain.abstractlog.regatta.events.impl;

import java.io.Serializable;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.regatta.MappingEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceCompetitorMappingEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDeviceMappingEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sse.common.TimePoint;

public class RegattaLogDeviceCompetitorMappingEventImpl extends RegattaLogDeviceMappingEventImpl<Competitor> implements
        RegattaLogDeviceCompetitorMappingEvent {
    private static final long serialVersionUID = -1494030544804758753L;

    public RegattaLogDeviceCompetitorMappingEventImpl(TimePoint createdAt, TimePoint logicalTimePoint,
            AbstractLogEventAuthor author, Serializable pId, Competitor mappedTo, DeviceIdentifier device,
            TimePoint from, TimePoint toInclusive) {
        super(createdAt, logicalTimePoint, author, pId, mappedTo, device, from, toInclusive);
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
