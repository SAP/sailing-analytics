package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;
import java.util.List;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WithID;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.racelog.tracking.TypeBasedServiceFinder;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.tracking.DeviceIdentifier;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogDeviceMappingEventSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.tracking.DeviceIdentifierJsonHandler;

public abstract class RaceLogDeviceMappingEventDeserializer<ItemT extends WithID>
	extends BaseRaceLogEventDeserializer {
	protected final TypeBasedServiceFinder<DeviceIdentifierJsonHandler> deviceServiceFinder;

	public RaceLogDeviceMappingEventDeserializer(
			JsonDeserializer<Competitor> competitorDeserializer,
			TypeBasedServiceFinder<DeviceIdentifierJsonHandler> deviceServiceFinder) {
		super(competitorDeserializer);
		this.deviceServiceFinder = deviceServiceFinder;
	}
	
	protected abstract RaceLogEvent furtherDeserialize(Serializable itemId, TimePoint from, TimePoint to,
			DeviceIdentifier device,  Serializable id, TimePoint createdAt, RaceLogEventAuthor author, TimePoint timePoint, int passId);
	
	@Override
	protected RaceLogEvent deserialize(JSONObject object, Serializable id,
			TimePoint createdAt, RaceLogEventAuthor author,
			TimePoint timePoint, int passId, List<Competitor> competitors)
			throws JsonDeserializationException {
		String deviceType = (String) object.get(RaceLogDeviceMappingEventSerializer.FIELD_DEVICE_TYPE);
		DeviceIdentifier device;
		try {
			device = deviceServiceFinder.findService(deviceType).transformBack(
					(JSONObject) object.get(RaceLogDeviceMappingEventSerializer.FIELD_DEVICE_ID));
		} catch (Exception e) {
			throw new JsonDeserializationException(e);
		}
		Serializable itemId = (Serializable) object.get(RaceLogDeviceMappingEventSerializer.FIELD_ITEM);
		long fromMillis = Long.parseLong((String) object.get(RaceLogDeviceMappingEventSerializer.FIELD_FROM_MILLIS));
		long toMillis = Long.parseLong((String) object.get(RaceLogDeviceMappingEventSerializer.FIELD_TO_MILLIS));
		TimePoint from = new MillisecondsTimePoint(fromMillis);
		TimePoint to = new MillisecondsTimePoint(toMillis);
		return furtherDeserialize(itemId, from, to, device, id, createdAt, author, timePoint, passId);
	}
}
