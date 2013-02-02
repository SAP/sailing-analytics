package com.sap.sailing.server.gateway.deserialization.impl;

import java.io.Serializable;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;

public class Helpers {
	
	public static JSONArray toJSONArraySafe(Object object)
			throws JsonDeserializationException {
		if (object instanceof JSONArray) {
			return (JSONArray) object;
		}
		throw new JsonDeserializationException(
				String.format("Expected a JSONArray, got %s.", object.getClass().getName()));
	}
	
	public static JSONObject toJSONObjectSafe(Object object)
			throws JsonDeserializationException {
		if (object instanceof JSONObject) {
			return (JSONObject) object;
		}
		throw new JsonDeserializationException(
				String.format("Expected a JSONObject, got %s.", object.getClass().getName()));
	}
	
	public static JSONObject getNestedObjectSafe(JSONObject parent, String fieldName) 
			throws JsonDeserializationException {
		Object childObject = parent.get(fieldName);
		if (!(childObject instanceof JSONObject)) {
			throw new JsonDeserializationException(
					String.format("Field %s with %s wasn't a nested JSON object.", 
							fieldName, childObject.toString()));
		}
		return (JSONObject) childObject;
	}
	
	public static JSONArray getNestedArraySafe(JSONObject parent, String fieldName) 
			throws JsonDeserializationException {
		Object childObject = parent.get(fieldName);
		if (!(childObject instanceof JSONArray)) {
			throw new JsonDeserializationException(
					String.format("Field %s with %s wasn't a nested JSON array.", 
							fieldName, childObject.toString()));
		}
		return (JSONArray) childObject;
	}
	
	public static Serializable tryUuidConversion(String id) {
		try {
			return UUID.fromString(id);
		} catch (IllegalArgumentException iae) {
			/// TODO: insert warning of non-uuid id.
		}
		return id;
	}

}
