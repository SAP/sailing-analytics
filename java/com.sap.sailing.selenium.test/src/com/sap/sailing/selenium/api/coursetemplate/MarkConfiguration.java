package com.sap.sailing.selenium.api.coursetemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.selenium.api.core.JsonWrapper;

public class MarkConfiguration extends JsonWrapper {

    private static final String FIELD_ID = "id";
    private static final String FIELD_MARK_TEMPLATE_ID = "markTemplateId";
    private static final String FIELD_MARK_PROPERTIES_ID = "markPropertiesId";
    private static final String FIELD_ASSOCIATED_ROLE_ID = "associatedRoleId";
    private static final String FIELD_FREESTYLE_PROPERTIES = "freestyleProperties";
    private static final String FIELD_EFFECTIVE_PROPERTIES = "effectiveProperties";
    private static final String FIELD_MARK_ID = "markId";
    private static final String FIELD_POSITIONING = "positioning";
    private static final String FIELD_STORE_TO_INVENTORY = "storeToInventory";
    private static final String FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_MAPPINGS = "trackingDevices";
    private static final String FIELD_MARK_CONFIGURATION_LAST_KNOWN_POSITION = "lastKnownPosition";
    private static final String FIELD_LATITUDE_DEG = "lat_deg";
    private static final String FIELD_LONGITUDE_DEG = "lon_deg";
    private static final String FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_NAME = "associatedRole";
    private static final String FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_SHORT_NAME = "associatedRoleShortName";

    public MarkConfiguration(final JSONObject json) {
        super(json);
    }

    public static MarkConfiguration createFreestyle(final UUID markTemplateId, final UUID markPropertiesId,
            final UUID associatedRoleId, final String name, final String shortName, final String color,
            final String shape, final String pattern, final String markType, Set<String> tags) {
        MarkConfiguration markConfiguration = new MarkConfiguration(new JSONObject());
        markConfiguration.getJson().put(FIELD_ID, UUID.randomUUID().toString());
        if (markTemplateId != null) {
            markConfiguration.getJson().put(FIELD_MARK_TEMPLATE_ID, markTemplateId.toString());
        }
        if (markPropertiesId != null) {
            markConfiguration.getJson().put(FIELD_MARK_PROPERTIES_ID, markPropertiesId.toString());
        }
        if (associatedRoleId != null) {
            markConfiguration.getJson().put(FIELD_ASSOCIATED_ROLE_ID, associatedRoleId.toString());
        }
        markConfiguration.getJson().put(FIELD_FREESTYLE_PROPERTIES,
                new FreestyleProperties(name, shortName, color, shape, pattern, markType, tags).getJson());
        return markConfiguration;
    }

    public static MarkConfiguration createMarkPropertiesBased(final UUID markPropertiesId,
            final UUID associatedRoleId) {
        MarkConfiguration markConfiguration = new MarkConfiguration(new JSONObject());
        markConfiguration.getJson().put(FIELD_ID, UUID.randomUUID().toString());
        markConfiguration.getJson().put(FIELD_MARK_PROPERTIES_ID, markPropertiesId.toString());
        if (associatedRoleId != null) {
            markConfiguration.getJson().put(FIELD_ASSOCIATED_ROLE_ID, associatedRoleId.toString());
        }
        return markConfiguration;
    }

    public static MarkConfiguration createMarkBased(final UUID markId, final UUID associatedRoleId) {
        MarkConfiguration markConfiguration = new MarkConfiguration(new JSONObject());
        markConfiguration.getJson().put(FIELD_ID, UUID.randomUUID().toString());
        markConfiguration.getJson().put(FIELD_MARK_ID, markId.toString());
        if (associatedRoleId != null) {
            markConfiguration.getJson().put(FIELD_ASSOCIATED_ROLE_ID, associatedRoleId.toString());
        }
        return markConfiguration;
    }

    public static MarkConfiguration createMarkTemplateBased(final UUID markTemplateId, final UUID associatedRoleId) {
        MarkConfiguration markConfiguration = new MarkConfiguration(new JSONObject());
        markConfiguration.getJson().put(FIELD_ID, UUID.randomUUID().toString());
        markConfiguration.getJson().put(FIELD_MARK_TEMPLATE_ID, markTemplateId.toString());
        if (associatedRoleId != null) {
            markConfiguration.getJson().put(FIELD_ASSOCIATED_ROLE_ID, associatedRoleId.toString());
        }
        return markConfiguration;
    }

    public String getId() {
        return this.get(FIELD_ID);
    }

    public void setTrackingDeviceId(UUID deviceId) {
        getJson().put(FIELD_POSITIONING, new Positioning(deviceId).getJson());
    }

    public void setFixedPosition(double latDeg, double lngDeg) {
        getJson().put(FIELD_POSITIONING, new Positioning(latDeg, lngDeg).getJson());
    }
    
    public Positioning getPositioning() {
        final JSONObject positioningObject = (JSONObject) get(FIELD_POSITIONING);
        return positioningObject == null ? null : new Positioning(positioningObject);
    }

    public void unsetPositioning() {
        getJson().put(FIELD_POSITIONING, null);
    }

    public List<DeviceMapping> getDeviceMappings() {
        final JSONArray deviceMappings = get(FIELD_MARK_CONFIGURATION_TRACKING_DEVICE_MAPPINGS);
        final List<DeviceMapping> result;
        if (deviceMappings == null) {
            result = Collections.emptyList();
        } else {
            result = deviceMappings.stream().map(m -> new DeviceMapping((JSONObject) m)).collect(Collectors.toList());
        }
        
        return result;
    }
    
    public DeviceMapping getSingleDeviceMapping() {
        final List<DeviceMapping> deviceMappings = getDeviceMappings();
        assertEquals(1, deviceMappings.size());
        return deviceMappings.get(0);
    }
    
    public Position getLastKnownPosition() {
        final JSONObject positionJson = (JSONObject) get(FIELD_MARK_CONFIGURATION_LAST_KNOWN_POSITION);
        return positionJson != null
                ? new DegreePosition(((Number) positionJson.get(FIELD_LATITUDE_DEG)).doubleValue(),
                        ((Number) positionJson.get(FIELD_LONGITUDE_DEG)).doubleValue()) : null;
    }

    public UUID getMarkTemplateId() {
        final Object markTemplateId = get(FIELD_MARK_TEMPLATE_ID);
        return markTemplateId != null ? UUID.fromString((String) markTemplateId) : null;
    }

    public FreestyleProperties getEffectiveProperties() {
        final JSONObject effectivePropertiesJson = (JSONObject) get(FIELD_EFFECTIVE_PROPERTIES);
        return effectivePropertiesJson != null ? new FreestyleProperties(effectivePropertiesJson) : null;
    }

    public FreestyleProperties getFreestyleProperties() {
        final JSONObject effectivePropertiesJson = (JSONObject) get(FIELD_FREESTYLE_PROPERTIES);
        return effectivePropertiesJson != null ? new FreestyleProperties(effectivePropertiesJson) : null;
    }

    public boolean isStoreToInventory() {
        return Boolean.TRUE.equals(get(FIELD_STORE_TO_INVENTORY));
    }

    public void setStoreToInventory(boolean storeToInventory) {
        getJson().put(FIELD_STORE_TO_INVENTORY, storeToInventory);
    }

    public String getAssociatedRoleId() {
        return (String) get(FIELD_ASSOCIATED_ROLE_ID);
    }
    
    public void setNameOfMarkRoleToCreate(String markRoleName) {
        getJson().put(FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_NAME, markRoleName);
    }

    public void setShortNameOfMarkRoleToCreate(String markRoleShortName) {
        getJson().put(FIELD_MARK_CONFIGURATION_ASSOCIATED_ROLE_SHORT_NAME, markRoleShortName);
    }

    public UUID getMarkPropertiesId() {
        final String markPropertiesId = (String) get(FIELD_MARK_PROPERTIES_ID);
        return markPropertiesId != null ? UUID.fromString(markPropertiesId) : null;
    }
    
    public void setMarkPropertiesId(UUID markPropertiesId) {
        getJson().put(FIELD_MARK_PROPERTIES_ID, markPropertiesId.toString());
    }

    public UUID getMarkId() {
        final String markId = (String) get(FIELD_MARK_ID);
        return markId != null ? UUID.fromString(markId) : null;
    }
}
