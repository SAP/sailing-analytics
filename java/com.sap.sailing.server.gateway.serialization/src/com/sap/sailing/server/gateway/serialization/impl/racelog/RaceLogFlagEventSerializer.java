package com.sap.sailing.server.gateway.serialization.impl.racelog;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;

public class RaceLogFlagEventSerializer extends BaseRaceLogEventSerializer {
	
	public static final String VALUE_CLASS = RaceLogFlagEvent.class.getSimpleName();
	public static final String FIELD_UPPER_FLAG = "upperFlag";
	public static final String FIELD_LOWER_FLAG = "lowerFlag";
	public static final String FIELD_DISPLAYED = "displayed";
	
	public RaceLogFlagEventSerializer(
			JsonSerializer<Competitor> competitorSerializer) {
		super(competitorSerializer);
	}

	@Override
	protected String getClassFieldValue() {
		return VALUE_CLASS;
	}
	
	@Override
	public JSONObject serialize(RaceLogEvent object) {
		RaceLogFlagEvent flagEvent = (RaceLogFlagEvent) object;
		
		JSONObject result = super.serialize(flagEvent);
		result.put(FIELD_UPPER_FLAG, flagEvent.getUpperFlag());
		result.put(FIELD_LOWER_FLAG, flagEvent.getLowerFlag());
		result.put(FIELD_DISPLAYED, flagEvent.isDisplayed());
		
		return result;
	}

}
