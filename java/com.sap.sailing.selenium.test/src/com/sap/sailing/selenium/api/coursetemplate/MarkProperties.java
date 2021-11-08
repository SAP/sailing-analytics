package com.sap.sailing.selenium.api.coursetemplate;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.selenium.api.core.JsonWrapper;

public class MarkProperties extends JsonWrapper {

    private static final String FIELD_ID = "id";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_SHORTNAME = "shortName";
    private static final String FIELD_COLOR = "color";
    private static final String FIELD_SHAPE = "shape";
    private static final String FIELD_PATTERN = "pattern";
    private static final String FIELD_MARKTYPE = "markType";
    private static final String FIELD_POSITIONING = "positioning";
    private static final String FIELD_POSITIONING_POSITION = "position";
    private static final String FIELD_POSITIONING_DEVICE_IDENTIFIER = "device_identifier";
    private static final String FIELD_POSITIONING_FIXED_POSITION_LATDEG = "latitude_deg";
    private static final String FIELD_POSITIONING_FIXED_POSITION_LONDEG = "longitude_deg";
    private static final String FIELD_TAGS = "tags";
    

    public MarkProperties(JSONObject json) {
        super(json);
    }

    public UUID getId() {
        final String uuid = get(FIELD_ID);
        return uuid != null ? UUID.fromString(uuid) : null;
    }

    public String getName() {
        return get(FIELD_NAME);
    }

    public String getShortName() {
        return get(FIELD_SHORTNAME);
    }

    public String getColor() {
        return get(FIELD_COLOR);
    }

    public String getShape() {
        return get(FIELD_SHAPE);
    }

    public String getPattern() {
        return get(FIELD_PATTERN);
    }

    public MarkType getMarkType() {
        String markType = get(FIELD_MARKTYPE);
        return markType != null ? MarkType.valueOf(markType) : null;
    }

    public Boolean hasDevice() {
        return get(FIELD_POSITIONING) != null && ((JSONObject) get(FIELD_POSITIONING)).get(FIELD_POSITIONING_DEVICE_IDENTIFIER) != null;
    }
    
    public void setLatDeg(double latDeg) {
        JSONObject jsonPositioning = get(FIELD_POSITIONING);
        if (jsonPositioning == null) {
            jsonPositioning = new JSONObject();
            getJson().put(FIELD_POSITIONING, jsonPositioning);
        }
        final Positioning positioning = new Positioning(jsonPositioning);
        positioning.setLatitudeDeg(latDeg);
    }

    public Double getLatDeg() {
        final JSONObject position = (JSONObject) ((JSONObject) get(FIELD_POSITIONING)).get(FIELD_POSITIONING_POSITION);
        return (Double) (position == null ? null : position.get(FIELD_POSITIONING_FIXED_POSITION_LATDEG));
    }

    public Double getLonDeg() {
        final JSONObject position = (JSONObject) ((JSONObject) get(FIELD_POSITIONING)).get(FIELD_POSITIONING_POSITION);
        return (Double) (position == null ? null : position.get(FIELD_POSITIONING_FIXED_POSITION_LONDEG));
    }
    
    public Set<String> getTags() {
        JSONArray jsonTags = get(FIELD_TAGS);
        final Set<String> tags = new HashSet<>();
        if(jsonTags != null) {
            for(Object tag : jsonTags) {
                tags.add(tag.toString());
            }
        }
        return tags;
    }

    public void setLonDeg(double lonDeg) {
        JSONObject jsonPositioning = get(FIELD_POSITIONING);
        if (jsonPositioning == null) {
            jsonPositioning = new JSONObject();
            getJson().put(FIELD_POSITIONING, jsonPositioning);
        }
        final Positioning positioning = new Positioning(jsonPositioning);
        positioning.setLongitudeDeg(lonDeg);
    }
}
