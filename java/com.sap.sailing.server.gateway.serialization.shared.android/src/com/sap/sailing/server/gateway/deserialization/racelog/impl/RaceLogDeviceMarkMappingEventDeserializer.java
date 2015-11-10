package com.sap.sailing.server.gateway.deserialization.racelog.impl;

import java.io.Serializable;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sse.common.TimePoint;

public class RaceLogDeviceMarkMappingEventDeserializer extends
		RaceLogDeviceMappingEventDeserializer<Mark> {
	private final JsonDeserializer<Mark> markDeserializer;
	
	public RaceLogDeviceMarkMappingEventDeserializer(
			JsonDeserializer<Competitor> competitorDeserializer, JsonDeserializer<Mark> markDeserializer,
			JsonDeserializer<DeviceIdentifier> deviceDeserializer) {
		super(competitorDeserializer, deviceDeserializer);
		this.markDeserializer = markDeserializer;
	}

	@Override
	protected RaceLogEvent furtherDeserialize(JSONObject itemObject, TimePoint from, TimePoint to,
			DeviceIdentifier device, Serializable id, TimePoint createdAt,
			AbstractLogEventAuthor author, TimePoint timePoint, int passId) throws JsonDeserializationException {
		Mark mappedTo = (Mark) markDeserializer.deserialize(itemObject);
		return new RaceLogDeviceMarkMappingEventImpl(createdAt, timePoint, author, id, passId,
				mappedTo, device, from, to);
	}

}
