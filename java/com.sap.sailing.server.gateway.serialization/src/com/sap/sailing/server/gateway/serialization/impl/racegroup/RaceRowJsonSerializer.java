package com.sap.sailing.server.gateway.serialization.impl.racegroup;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceCell;
import com.sap.sailing.domain.base.RaceRow;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;

public class RaceRowJsonSerializer implements JsonSerializer<RaceRow> {
	public static final String FIELD_FLEET = "fleet";
	public static final String FIELD_RACE_CELLS = "races";
	
	private JsonSerializer<Fleet> fleetSerializer;
	private JsonSerializer<RaceCell> cellSerializer;
	
	public RaceRowJsonSerializer(
			JsonSerializer<Fleet> fleetSerializer,
			JsonSerializer<RaceCell> cellSerializer) {
		this.fleetSerializer = fleetSerializer;
		this.cellSerializer = cellSerializer;
	}

	@Override
	public JSONObject serialize(RaceRow object) {
		JSONObject result = new JSONObject();
		
		result.put(FIELD_FLEET, fleetSerializer.serialize(object.getFleet()));
		JSONArray cells = new JSONArray(); 
		for (RaceCell cell : object.getCells()) {
			cells.add(cellSerializer.serialize(cell));
		}
		result.put(FIELD_RACE_CELLS, cells);
		
		return result;
	}

}
