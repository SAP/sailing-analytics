package com.sap.sailing.server.gateway.serialization.impl.racegroup;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.SeriesWithRows;
import com.sap.sailing.server.gateway.serialization.ExtendableJsonSerializer;
import com.sap.sailing.server.gateway.serialization.ExtensionJsonSerializer;

public class SeriesWithRowsJsonSerializer extends ExtendableJsonSerializer<SeriesWithRows> {
	public static final String FIELD_NAME = "name";
	public static final String FIELD_IS_MEDAL = "isMedal";
	
	public SeriesWithRowsJsonSerializer(ExtensionJsonSerializer<SeriesWithRows, ?> extensionSerializer) {
		super(extensionSerializer);
	}

	@Override
	protected JSONObject serializeFields(SeriesWithRows object) {
		JSONObject result = new JSONObject();
		
		result.put(FIELD_NAME, object.getName());
		result.put(FIELD_IS_MEDAL, object.isMedal());
		
		return result;
	}

}
