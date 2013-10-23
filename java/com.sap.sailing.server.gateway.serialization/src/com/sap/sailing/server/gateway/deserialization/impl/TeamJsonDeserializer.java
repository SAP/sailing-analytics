package com.sap.sailing.server.gateway.deserialization.impl;

import java.util.HashSet;
import java.util.Set;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Person;
import com.sap.sailing.domain.base.Team;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.TeamJsonSerializer;

public class TeamJsonDeserializer implements JsonDeserializer<Team> {

	private final JsonDeserializer<Person> personDeserializer;

	public static TeamJsonDeserializer create() {
		return new TeamJsonDeserializer(new PersonJsonDeserializer(
				new NationalityJsonDeserializer(DomainFactory.INSTANCE)));
	}

	public TeamJsonDeserializer(JsonDeserializer<Person> personDeserializer) {
		this.personDeserializer = personDeserializer;
	}

	@Override
	public Team deserialize(JSONObject object)
			throws JsonDeserializationException {
		String name = (String) object.get(TeamJsonSerializer.FIELD_NAME);
		Person coach = personDeserializer.deserialize((JSONObject) object
				.get(TeamJsonSerializer.FIELD_COACH));
		Set<Person> sailors = new HashSet<Person>();

		JSONArray sailorsJson = (JSONArray) object
				.get(TeamJsonSerializer.FIELD_SAILORS);
		for (Object sailorObject : sailorsJson) {
			sailors.add(personDeserializer
					.deserialize((JSONObject) sailorObject));
		}
		return new TeamImpl(name, sailors, coach);
	}

}
