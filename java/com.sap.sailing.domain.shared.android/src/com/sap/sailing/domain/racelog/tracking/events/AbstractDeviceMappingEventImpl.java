package com.sap.sailing.domain.racelog.tracking.events;

import java.io.Serializable;
import java.util.Collections;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WithID;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.impl.RaceLogEventImpl;
import com.sap.sailing.domain.racelog.tracking.DeviceIdentifier;
import com.sap.sailing.domain.racelog.tracking.DeviceMappingEvent;

public abstract class AbstractDeviceMappingEventImpl<ItemType extends WithID> extends RaceLogEventImpl implements
		DeviceMappingEvent<ItemType> {
	private static final long serialVersionUID = -8439653251231710356L;

	private final ItemType mappedTo;
	private final DeviceIdentifier device;
	private final TimePoint from;
	private final TimePoint to;
	
	public AbstractDeviceMappingEventImpl(TimePoint createdAt, RaceLogEventAuthor author,
			TimePoint logicalTimePoint, Serializable pId, int pPassId, ItemType mappedTo,
			DeviceIdentifier device, TimePoint from, TimePoint to) {
		super(createdAt, author, logicalTimePoint, pId, Collections.<Competitor>emptyList(), pPassId);
		this.mappedTo = mappedTo;
		this.device = device;
		this.from = from;
		this.to = to;
	}
	
	public ItemType getMappedTo() {
		return mappedTo;
	}
	public DeviceIdentifier getDevice() {
		return device;
	}
	public TimePoint getFrom() {
		return from;
	}
	public TimePoint getTo() {
		return to;
	}
}
