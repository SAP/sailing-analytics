package com.sap.sailing.domain.racelog.tracking.events;

import java.io.Serializable;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.devices.DeviceIdentifier;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.tracking.DeviceMarkMappingEvent;

public class DeviceMarkMappingEventImpl extends AbstractDeviceMappingEventImpl<Mark>
	implements DeviceMarkMappingEvent {
	public DeviceMarkMappingEventImpl(TimePoint createdAt, RaceLogEventAuthor author, TimePoint logicalTimePoint,
			Serializable pId, int pPassId, Mark mappedTo, DeviceIdentifier device, TimePoint from, TimePoint to) {
		super(createdAt, author, logicalTimePoint, pId, pPassId, mappedTo, device, from, to);
	}

	private static final long serialVersionUID = -1494030544804758753L;

	@Override
	public void accept(RaceLogEventVisitor visitor) {
		visitor.visit(this);
	}
}
