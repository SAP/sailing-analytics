package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.server.tagging.TagDTODeSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Tests {@link TagDTODeSerializer} which is used for (de-)serialization of {@link TagDTO tags}.
 */
public class TagDTODeSerializerTest {

    private final static Logger logger = Logger.getLogger(TaggingServiceTest.class.getName());
    private final static TagDTODeSerializer serializer = new TagDTODeSerializer();

    @Test
    public void testSerializeSingleTag() {
        logger.entering(getClass().getName(), "testSerializeSingleTag");
        final String title = "Tag Title";
        final String comment = "Comment";
        final String hiddenInfo = "this is a hidden info\nthat comes in two lines";
        final String imageURL = "";
        final String username = "user";
        final TimePoint raceTimePoint = new MillisecondsTimePoint(1234);
        final TimePoint createdAt = MillisecondsTimePoint.now();
        final TimePoint revokedAt = new MillisecondsTimePoint(5678);

        final TagDTO tag = new TagDTO(title, comment, hiddenInfo, imageURL, imageURL, false, username, raceTimePoint, createdAt);
        final TagDTO revokedTag = new TagDTO(title, comment, hiddenInfo, imageURL, imageURL, false, username, raceTimePoint,
                createdAt, revokedAt);
        final JSONObject tagJson = serializer.serialize(tag);
        assertEquals(title, tagJson.get(TagDTODeSerializer.FIELD_TAG), "title equals serialized title");
        assertEquals(comment, tagJson.get(TagDTODeSerializer.FIELD_COMMENT), "comment equals serialized comment");
        assertEquals(hiddenInfo, tagJson.get(TagDTODeSerializer.FIELD_HIDDEN_INFO), "hidden info equals serialized hidden info");
        assertEquals(imageURL, tagJson.get(TagDTODeSerializer.FIELD_ORIGINAL_IMAGE_URL), "imageURL equals serialized imageURL");
        assertEquals(username, tagJson.get(TagDTODeSerializer.FIELD_USERNAME), "username equals serialized username");
        assertEquals(raceTimePoint.asMillis(), tagJson.get(TagDTODeSerializer.FIELD_RACE_TIMEPOINT),
                "raceTimepoint equals serialized raceTimepoint");
        assertEquals(createdAt.asMillis(), tagJson.get(TagDTODeSerializer.FIELD_CREATED_AT),
                "createdAt timepoint equals serialized createdAt timepoint");
        assertEquals(null, tagJson.get(TagDTODeSerializer.FIELD_REVOKED_AT),
                "revokedAt timepoint is null for non-revoked tags");
        final JSONObject revokedTagJson = serializer.serialize(revokedTag);
        assertEquals(revokedAt.asMillis(), revokedTagJson.get(TagDTODeSerializer.FIELD_REVOKED_AT),
                "revokedAt timepoint equals serialized revokedAt");
        logger.exiting(getClass().getName(), "testSerializeSingleTag");
    }

    @Test
    public void testSerializeMultipleTags() {
        logger.entering(getClass().getName(), "testSerializeMultipleTags");
        final String title = "Tag Title";
        final String comment = "Comment";
        final String hiddenInfo = null;
        final String imageURL = "";
        final String resizedImageURL = "";
        final String username = "user";
        final TimePoint raceTimePoint = new MillisecondsTimePoint(1234);
        final TimePoint createdAt = MillisecondsTimePoint.now();
        final TimePoint revokedAt = new MillisecondsTimePoint(5678);

        final TagDTO tag1 = new TagDTO(title + "1", comment, hiddenInfo, imageURL, resizedImageURL, false, username, raceTimePoint, createdAt);
        final JSONObject tag1Json = serializer.serialize(tag1);
        final TagDTO tag2 = new TagDTO(title + "2", comment, hiddenInfo, imageURL, resizedImageURL, true, username, raceTimePoint,
                createdAt, revokedAt);
        final JSONObject tag2Json = serializer.serialize(tag2);
        final List<TagDTO> tags = Arrays.asList(tag1, tag2);
        final JSONArray tagsJson = serializer.serialize(tags);
        assertEquals(tag1Json, (JSONObject) tagsJson.get(0),
                "First serialized tag equals serialized version of JSON array");
        assertEquals(tag2Json, (JSONObject) tagsJson.get(1),
                "Second serialized tag equals serialized version of JSON array");
        logger.exiting(getClass().getName(), "testSerializeMultipleTags");
    }

    @Test
    public void testDeserializeSingleTag() {
        logger.entering(getClass().getName(), "testDeserializeSingleTag");
        final String title = "Tag Title";
        final String comment = "Comment";
        final String hiddenInfo = "some hidden info";
        final String imageURL = "";
        final String resizedImageURL = "";
        final String username = "user";
        final boolean visibileForPublic = true;
        final TimePoint raceTimePoint = new MillisecondsTimePoint(1234);
        final TimePoint createdAt = MillisecondsTimePoint.now();
        final TimePoint revokedAt = new MillisecondsTimePoint(5678);
        final TagDTO tag = new TagDTO(title, comment, hiddenInfo, imageURL, imageURL, visibileForPublic, username, raceTimePoint,
                createdAt, revokedAt);
        final JSONObject tagJson = new JSONObject();
        tagJson.put(TagDTODeSerializer.FIELD_TAG, title);
        tagJson.put(TagDTODeSerializer.FIELD_COMMENT, comment);
        tagJson.put(TagDTODeSerializer.FIELD_HIDDEN_INFO, hiddenInfo);
        tagJson.put(TagDTODeSerializer.FIELD_ORIGINAL_IMAGE_URL, imageURL);
        tagJson.put(TagDTODeSerializer.FIELD_RESIZED_IMAGE_URL, resizedImageURL);
        tagJson.put(TagDTODeSerializer.FIELD_USERNAME, username);
        tagJson.put(TagDTODeSerializer.FIELD_VISIBLE_FOR_PUBLIC, visibileForPublic);
        tagJson.put(TagDTODeSerializer.FIELD_RACE_TIMEPOINT, raceTimePoint.asMillis());
        tagJson.put(TagDTODeSerializer.FIELD_CREATED_AT, createdAt.asMillis());
        tagJson.put(TagDTODeSerializer.FIELD_REVOKED_AT, revokedAt.asMillis());
        assertEquals(tag, serializer.deserializeTag(tagJson.toJSONString()), "Deserialize tag from string");
        assertEquals(tag, serializer.deserialize(tagJson), "Deserialize tag from JSON object");
        logger.exiting(getClass().getName(), "testDeserializeSingleTag");
    }

    @Test
    public void testDeserializeMultipleTags() {
        logger.entering(getClass().getName(), "testDeserializeMultipleTags");
        final String title = "Tag Title";
        final String comment = "Comment";
        final String hiddenInfo = "hiddenInfo";
        final String imageURL = "";
        final String resizedImageURL = "";
        final String username = "user";
        final boolean visibileForPublic = true;
        final TimePoint raceTimePoint = new MillisecondsTimePoint(1234);
        final TimePoint createdAt = MillisecondsTimePoint.now();
        final TimePoint revokedAt = new MillisecondsTimePoint(5678);
        final TagDTO tag1 = new TagDTO(title + "1", comment, hiddenInfo, imageURL, resizedImageURL, visibileForPublic, username,
                raceTimePoint, createdAt, revokedAt);
        final TagDTO tag2 = new TagDTO(title + "2", comment, hiddenInfo, imageURL, resizedImageURL, visibileForPublic, username,
                raceTimePoint, createdAt, revokedAt);
        final JSONObject tag1Json = new JSONObject();
        final JSONObject tag2Json = new JSONObject();
        tag1Json.put(TagDTODeSerializer.FIELD_TAG, title + "1");
        tag2Json.put(TagDTODeSerializer.FIELD_TAG, title + "2");
        tag1Json.put(TagDTODeSerializer.FIELD_COMMENT, comment);
        tag2Json.put(TagDTODeSerializer.FIELD_COMMENT, comment);
        tag1Json.put(TagDTODeSerializer.FIELD_HIDDEN_INFO, hiddenInfo);
        tag2Json.put(TagDTODeSerializer.FIELD_HIDDEN_INFO, hiddenInfo);
        tag1Json.put(TagDTODeSerializer.FIELD_ORIGINAL_IMAGE_URL, imageURL);
        tag2Json.put(TagDTODeSerializer.FIELD_RESIZED_IMAGE_URL, resizedImageURL);
        tag1Json.put(TagDTODeSerializer.FIELD_USERNAME, username);
        tag2Json.put(TagDTODeSerializer.FIELD_USERNAME, username);
        tag1Json.put(TagDTODeSerializer.FIELD_VISIBLE_FOR_PUBLIC, visibileForPublic);
        tag2Json.put(TagDTODeSerializer.FIELD_VISIBLE_FOR_PUBLIC, visibileForPublic);
        tag1Json.put(TagDTODeSerializer.FIELD_RACE_TIMEPOINT, raceTimePoint.asMillis());
        tag2Json.put(TagDTODeSerializer.FIELD_RACE_TIMEPOINT, raceTimePoint.asMillis());
        tag1Json.put(TagDTODeSerializer.FIELD_CREATED_AT, createdAt.asMillis());
        tag2Json.put(TagDTODeSerializer.FIELD_CREATED_AT, createdAt.asMillis());
        tag1Json.put(TagDTODeSerializer.FIELD_REVOKED_AT, revokedAt.asMillis());
        tag2Json.put(TagDTODeSerializer.FIELD_REVOKED_AT, revokedAt.asMillis());
        JSONArray tagsJson = new JSONArray();
        tagsJson.add(tag1Json);
        tagsJson.add(tag2Json);
        List<TagDTO> deserialzedFromString = serializer.deserializeTags(tagsJson.toJSONString());
        List<TagDTO> deserialzedFromArray = serializer.deserialize(tagsJson);
        assertEquals(tag1, deserialzedFromString.get(0), "Deserialize tag1 from string");
        assertEquals(tag2, deserialzedFromString.get(1), "Deserialize tag2 from string");
        assertEquals(tag1, deserialzedFromArray.get(0), "Deserialize tag1 from JSON array");
        assertEquals(tag2, deserialzedFromArray.get(1), "Deserialize tag2 from JSON array");
        logger.exiting(getClass().getName(), "testDeserializeMultipleTags");
    }

    @Test
    public void testSerializeTimePoint() {
        logger.entering(getClass().getName(), "testSerializeTimePoint");
        long millis = 1234;
        assertEquals(0, serializer.serializeTimePoint(null), "Serialize timepoint with null value");
        assertEquals(1234, serializer.serializeTimePoint(new MillisecondsTimePoint(millis)),
                "Serialize timepoint with non-null value");
        logger.exiting(getClass().getName(), "testSerializeTimePoint");
    }

    @Test
    public void testDeserializeTimePoint() {
        logger.entering(getClass().getName(), "testDeserializeTimePoint");
        long millis = 1234;
        assertEquals(millis, serializer.deserializeTimePoint(millis).asMillis(), "Deserialize timepoint");
        logger.exiting(getClass().getName(), "testDeserializeTimePoint");
    }

    @Test
    public void testGenerateUniqueKey() {
        logger.entering(getClass().getName(), "testGenerateUniqueKey");
        final String prefix = "sailing.tags.";
        final String leaderboard = "Leaderboard";
        final String racecolumn = "RaceColumn";
        final String fleet = "Fleet";
        assertEquals(prefix + leaderboard + "+" + racecolumn + "+" + fleet,
                serializer.generateUniqueKey(leaderboard, racecolumn, fleet));
        assertEquals(prefix + leaderboard + " +" + racecolumn + "//+" + fleet,
                serializer.generateUniqueKey(leaderboard + " ", racecolumn + "/", fleet));
        assertEquals(prefix + leaderboard + "+" + racecolumn + "//+" + fleet,
                serializer.generateUniqueKey(leaderboard, racecolumn + "/", fleet));
        assertEquals(prefix + leaderboard + "+" + racecolumn + "/p+" + fleet,
                serializer.generateUniqueKey(leaderboard, racecolumn + "+", fleet));
        assertEquals(prefix + leaderboard + "+" + racecolumn + "/p/p+" + fleet,
                serializer.generateUniqueKey(leaderboard, racecolumn + "++", fleet));
        assertEquals(prefix + leaderboard + "+" + racecolumn + "//p+" + fleet,
                serializer.generateUniqueKey(leaderboard, racecolumn + "/p", fleet));
        assertEquals(prefix + leaderboard + "+" + racecolumn + "\\+" + fleet,
                serializer.generateUniqueKey(leaderboard, racecolumn + "\\", fleet));
        logger.exiting(getClass().getName(), "testGenerateUniqueKey");
    }
}
