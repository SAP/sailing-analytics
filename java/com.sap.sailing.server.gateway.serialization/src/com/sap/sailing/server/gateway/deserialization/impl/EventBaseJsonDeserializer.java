package com.sap.sailing.server.gateway.deserialization.impl;

import java.io.Serializable;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.impl.EventBaseImpl;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.EventJsonSerializer;

public class EventBaseJsonDeserializer implements JsonDeserializer<EventBase> {
	private JsonDeserializer<Venue> venueDeserializer;
	
	public EventBaseJsonDeserializer(JsonDeserializer<Venue> venueDeserializer) {
		this.venueDeserializer = venueDeserializer;
	}
	
	public EventBase deserialize(JSONObject object) throws JsonDeserializationException {
		Serializable id = (Serializable) object.get(EventJsonSerializer.FIELD_ID);
		String name = object.get(EventJsonSerializer.FIELD_NAME).toString();
		String publicationUrl = object.get(EventJsonSerializer.FIELD_PUBLICATION_URL).toString();
		
		JSONObject venueObject = Helpers.getNestedObjectSafe(
				object, 
				EventJsonSerializer.FIELD_VENUE);
		Venue venue = venueDeserializer.deserialize(venueObject);
		
		return new EventBaseImpl(
				name, 
				venue, 
				publicationUrl, 
				true, 
				Helpers.tryUuidConversion(id));
	}

}
