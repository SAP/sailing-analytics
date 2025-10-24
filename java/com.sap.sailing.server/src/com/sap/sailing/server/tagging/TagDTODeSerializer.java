package com.sap.sailing.server.tagging;

import java.util.ArrayList;
import java.util.List;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Serializes and deserializes {@link TagDTO tags}.
 */
public class TagDTODeSerializer {

    public static final String FIELD_TAG = "tag";
    public static final String FIELD_COMMENT = "comment";
    public static final String FIELD_HIDDEN_INFO = "hiddenInfo";
    public static final String FIELD_ORIGINAL_IMAGE_URL = "image";
    public static final String FIELD_RESIZED_IMAGE_URL = "resized_image";
    public static final String FIELD_USERNAME = "username";
    public static final String FIELD_VISIBLE_FOR_PUBLIC = "public";
    public static final String FIELD_RACE_TIMEPOINT = "raceTimepoint";
    public static final String FIELD_CREATED_AT = "createdAt";
    public static final String FIELD_REVOKED_AT = "revokedAt";

    private static final String UNIQUE_KEY_PREFIX = "sailing.tags.";

    private final JSONParser parser = new JSONParser();

    /**
     * Serializes {@link TagDTO tag} as {@link JSONObject}. Missing, optional parameters will not be put into result.
     * 
     * @param tag
     *            {@link TagDTO tag} to be serialized
     * @return {@link JSONObject json object}
     */
    public JSONObject serialize(TagDTO tag) {
        JSONObject result = new JSONObject();
        result.put(FIELD_TAG, tag.getTag());
        if (tag.getComment() != null) {
            result.put(FIELD_COMMENT, tag.getComment());
        }
        if (tag.getHiddenInfo() != null) {
            result.put(FIELD_HIDDEN_INFO, tag.getHiddenInfo());
        }
        if (tag.getImageURL() != null) {
            result.put(FIELD_ORIGINAL_IMAGE_URL, tag.getImageURL());
        }
        if (tag.getResizedImageURL() != null) {
            result.put(FIELD_RESIZED_IMAGE_URL, tag.getResizedImageURL());
        }
        result.put(FIELD_USERNAME, tag.getUsername());
        result.put(FIELD_VISIBLE_FOR_PUBLIC, tag.isVisibleForPublic());
        result.put(FIELD_RACE_TIMEPOINT, serializeTimePoint(tag.getRaceTimepoint()));
        result.put(FIELD_CREATED_AT, serializeTimePoint(tag.getCreatedAt()));
        if (tag.getRevokedAt() != null) {
            result.put(FIELD_REVOKED_AT, serializeTimePoint(tag.getRevokedAt()));
        }
        return result;
    }

    /**
     * Serializes list of {@link TagDTO tags} as {@link JSONArray}.
     * 
     * @param tags
     *            list of {@link TagDTO tags} to be serialized
     * @return {@link JSONArray json array}
     */
    public JSONArray serialize(List<TagDTO> tags) {
        JSONArray result = new JSONArray();
        for (TagDTO tag : tags) {
            result.add(serialize(tag));
        }
        return result;
    }

    /**
     * Deserializes {@link JSONObject jsonObject} to {@link TagDTO}.
     * 
     * @param jsonObject
     *            {@link JSONObject jsonObject} to be deserialized
     * @return <code>null</code> if <code>jsonObject</code> is no valid json for a tag, otherwise {@link TagDTO tag}
     */
    public TagDTO deserialize(JSONObject jsonObject) {
        try {
            String tag = (String) jsonObject.get(FIELD_TAG);
            String comment = jsonObject.get(FIELD_COMMENT) == null ? "" : (String) jsonObject.get(FIELD_COMMENT);
            String hiddenInfo = jsonObject.get(FIELD_HIDDEN_INFO) == null ? "" : (String) jsonObject.get(FIELD_HIDDEN_INFO);
            String imageURL = jsonObject.get(FIELD_ORIGINAL_IMAGE_URL) == null ? ""
                    : (String) jsonObject.get(FIELD_ORIGINAL_IMAGE_URL);
            String resizedImageURL = jsonObject.get(FIELD_RESIZED_IMAGE_URL) == null ? ""
                    : (String) jsonObject.get(FIELD_RESIZED_IMAGE_URL);
            String username = (String) jsonObject.get(FIELD_USERNAME);
            boolean visibleForPublic = (Boolean) jsonObject.get(FIELD_VISIBLE_FOR_PUBLIC);
            TimePoint raceTimePoint = deserializeTimePoint((Long) (jsonObject.get(FIELD_RACE_TIMEPOINT)));
            TimePoint createdAt = deserializeTimePoint((Long) (jsonObject.get(FIELD_CREATED_AT)));
            TimePoint revokedAt = jsonObject.get(FIELD_REVOKED_AT) == null ? null
                    : deserializeTimePoint((Long) jsonObject.get(FIELD_REVOKED_AT));
            return new TagDTO(tag, comment, hiddenInfo, imageURL, resizedImageURL, visibleForPublic, username, raceTimePoint, createdAt, revokedAt);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Deserializes {@link JSONArray jsonArray} to list of {@link TagDTO tags}.
     * 
     * @param jsonArray
     *            {@link JSONArray jsonArray} to be deserialized
     * @return list of {@link TagDTO tags}
     */
    public List<TagDTO> deserialize(JSONArray jsonArray) {
        return deserializeTags(jsonArray.toJSONString());
    }

    /**
     * Serializes list of {@link TagDTO tags} to json array.
     * 
     * @param tags
     *            tags to be seriaized
     * @return json array
     */
    public String serializeTags(List<TagDTO> tags) {
        JSONArray jsonTags = new JSONArray();
        for (TagDTO tag : tags) {
            jsonTags.add(serialize(tag));
        }
        return jsonTags.toJSONString();
    }

    /**
     * Deserializes json object to {@link TagDTO tag}.
     * 
     * @param jsonObject
     *            json object to be deseriaized
     * @return {@link TagDTO tag}
     */
    public TagDTO deserializeTag(String jsonObject) {
        TagDTO result = null;
        if (jsonObject != null && !jsonObject.isEmpty()) {
            try {
                JSONObject jsonTag = (JSONObject) parser.parse(jsonObject);
                result = deserialize(jsonTag);
            } catch (Exception e) {
                // do nothing => return null
            }
        }
        return result;

    }

    /**
     * Deserializes json array to list of {@link TagDTO tags}.
     * 
     * @param jsonArray
     *            json array to be deseriaized
     * @return list of {@link TagDTO tags}
     */
    public List<TagDTO> deserializeTags(String jsonArray) {
        JSONArray jsonTags;
        List<TagDTO> result = new ArrayList<TagDTO>();
        if (jsonArray != null && !jsonArray.isEmpty()) {
            try {
                jsonTags = (JSONArray) parser.parse(jsonArray);
                for (int i = 0; i < jsonTags.size(); i++) {
                    result.add(deserialize((JSONObject) jsonTags.get(i)));
                }
            } catch (Exception e) {
                // return all already parsed tags in case an error occurs => do nothing
            }
        }
        return result;
    }

    /**
     * Serializes given {@link TimePoint}.
     * 
     * @param timepoint
     *            {@link TimePoint} to be serialized
     * @return serialized timepoint milliseconds as long, <code>0</code> if <code>timepoint</code> is <code>null</code>
     */
    public long serializeTimePoint(TimePoint timepoint) {
        return timepoint == null ? 0 : timepoint.asMillis();
    }

    /**
     * Deserializes long to {@link MillisecondsTimePoint}.
     * 
     * @param timepoint
     *            timepoint to be deserialized
     * @return {@link TimePoint}
     */
    public TimePoint deserializeTimePoint(long timepoint) {
        return new MillisecondsTimePoint(timepoint);
    }

    /**
     * Combines <code>leaderboardName</code>, <code>raceColumnName</code> and <code>fleetName</code> to a unique key.
     * Used to store private tags in {@link com.sap.sse.security.interfaces.UserStore UserStore}.
     * 
     * @param leaderboardName
     *            leaderboard name
     * @param raceColumnName
     *            race column name
     * @param fleetName
     *            fleet name
     * @return unique key for given race
     */
    public String generateUniqueKey(String leaderboardName, String raceColumnName, String fleetName) {
        return UNIQUE_KEY_PREFIX + escape(leaderboardName) + "+" + escape(raceColumnName) + "+" + escape(fleetName);
    }

    /**
     * Escapes given string by replacing every occurrence of '/' by '//' and '+' by '/p'.
     * 
     * @param string
     *            string to be escaped
     * @return escaped string
     */
    private String escape(String string) {
        // '+' needs to be escaped as method replaceAll() expects the first parameter to be a regular expression and
        // not a simple string. As '+' has a different meaning in context of a regex it needs to be escaped by '\+'
        // which again needs to be escaped by '\\+'.
        return string.replaceAll("/", "//").replaceAll("\\+", "/p");
    }
}
