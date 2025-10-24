package com.sap.sailing.server.gateway.deserialization.impl;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.impl.StrippedEventImpl;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.server.gateway.serialization.impl.EventBaseJsonSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.media.impl.ImageDescriptorImpl;
import com.sap.sse.shared.media.impl.VideoDescriptorImpl;

public class EventBaseJsonDeserializer implements JsonDeserializer<EventBase> {
    private final JsonDeserializer<Venue> venueDeserializer;
    private final JsonDeserializer<LeaderboardGroupBase> leaderboardGroupDeserializer;
    private final JsonDeserializer<TrackingConnectorInfo>trackingConnectorInfoDeserializer;

    public EventBaseJsonDeserializer(JsonDeserializer<Venue> venueDeserializer,
            JsonDeserializer<LeaderboardGroupBase> leaderboardGroupDeserializer,
            JsonDeserializer<TrackingConnectorInfo> trackingConnectorInfoDeserializer) {
        this.venueDeserializer = venueDeserializer;
        this.leaderboardGroupDeserializer = leaderboardGroupDeserializer;
        this.trackingConnectorInfoDeserializer = trackingConnectorInfoDeserializer;
    }

    public EventBase deserialize(JSONObject eventJson) throws JsonDeserializationException {
        UUID id = UUID.fromString((String) eventJson.get(EventBaseJsonSerializer.FIELD_ID));
        String name = (String) eventJson.get(EventBaseJsonSerializer.FIELD_NAME);
        String description = (String) eventJson.get(EventBaseJsonSerializer.FIELD_DESCRIPTION);
        String officialWebsiteURLAsString = (String) eventJson.get(EventBaseJsonSerializer.FIELD_OFFICIAL_WEBSITE_URL);
        String baseURLAsString = (String) eventJson.get(EventBaseJsonSerializer.FIELD_BASE_URL);
        Number startDate = (Number) eventJson.get(EventBaseJsonSerializer.FIELD_START_DATE);
        Number endDate = (Number) eventJson.get(EventBaseJsonSerializer.FIELD_END_DATE);
        final Venue venue;
        if (eventJson.get(EventBaseJsonSerializer.FIELD_VENUE) != null) {
            JSONObject venueObject = Helpers.getNestedObjectSafe(eventJson, EventBaseJsonSerializer.FIELD_VENUE);
            venue = venueDeserializer.deserialize(venueObject);
        } else {
            venue = null;
        }
        JSONArray leaderboardGroupsJson = (JSONArray) eventJson.get(EventBaseJsonSerializer.FIELDS_LEADERBOARD_GROUPS);
        List<LeaderboardGroupBase> leaderboardGroups = new ArrayList<LeaderboardGroupBase>();
        if (leaderboardGroupsJson != null) {
            for (Object lgJson : leaderboardGroupsJson) {
                leaderboardGroups.add(leaderboardGroupDeserializer.deserialize((JSONObject) lgJson));
            }
        }
        final Set<TrackingConnectorInfo> trackingConnectorInfos = new HashSet<>();
        final JSONArray trackingConnectorInfosJson = (JSONArray) eventJson
                .get(EventBaseJsonSerializer.FIELD_TRACKING_CONNECTOR_INFOS);
        if (trackingConnectorInfosJson != null) {
            for (Object jsonPair : trackingConnectorInfosJson) {
                trackingConnectorInfos.add(trackingConnectorInfoDeserializer.deserialize((JSONObject) jsonPair));
            }
        }
        StrippedEventImpl result = new StrippedEventImpl(name,
                startDate == null ? null : new MillisecondsTimePoint(startDate.longValue()),
                endDate == null ? null : new MillisecondsTimePoint(endDate.longValue()), venue, /* is public */ true,
                id, leaderboardGroups, trackingConnectorInfos);
        result.setDescription(description);
        if (officialWebsiteURLAsString != null) {
            try {
                result.setOfficialWebsiteURL(new URL(officialWebsiteURLAsString));
            } catch (MalformedURLException e) {
                throw new JsonDeserializationException("Error deserializing official website URL for event "+name, e);
            }
        }
        if (baseURLAsString != null) {
            try {
                result.setBaseURL(new URL(baseURLAsString));
            } catch (MalformedURLException e) {
                throw new JsonDeserializationException("Error deserializing base URL for event "+name, e);
            }
        }
        JSONArray imagesJson = (JSONArray) eventJson.get(EventBaseJsonSerializer.FIELD_IMAGES);
        if (imagesJson != null) {
            for (Object imageJson : imagesJson) {
                ImageDescriptor imgaeDescriptor = loadImage((JSONObject) imageJson);
                if (imgaeDescriptor != null) {
                   result.addImage(imgaeDescriptor);
                }
            }            
        }
        JSONArray videosJson = (JSONArray) eventJson.get(EventBaseJsonSerializer.FIELD_VIDEOS);
        if (videosJson != null) {
            for (Object videoJson : videosJson) {
                VideoDescriptor videoDescriptor = loadVideo((JSONObject) videoJson);
                if (videoDescriptor != null) {
                   result.addVideo(videoDescriptor);
                }
            }            
        }
        JSONArray sailorsInfoWebsiteURLsJson = (JSONArray) eventJson.get(EventBaseJsonSerializer.FIELD_SAILORS_INFO_WEBSITE_URLS);
        if (sailorsInfoWebsiteURLsJson != null) {
            for (Object sailorsInfoWebsiteURLJson : sailorsInfoWebsiteURLsJson) {
                JSONObject sailorsInfoWebsiteURLJsonObject = (JSONObject) sailorsInfoWebsiteURLJson;
                String localeString = (String) sailorsInfoWebsiteURLJsonObject.get(EventBaseJsonSerializer.FIELD_LOCALE);
                // TODO use Locale.forLanguageTag(localeRaw) -> only possible with Android API Level 21
                result.setSailorsInfoWebsiteURL(localeString == null ? null : new Locale(localeString),  Helpers.getURLField(sailorsInfoWebsiteURLJsonObject, EventBaseJsonSerializer.FIELD_URL));
            } 
        }
        return result;
    }
    
    private ImageDescriptor loadImage(JSONObject imageJson) {
        ImageDescriptor image = null;
        URL imageURL = Helpers.getURLField(imageJson, EventBaseJsonSerializer.FIELD_SOURCE_URL);
        if (imageURL != null) {
            String title = (String) imageJson.get(EventBaseJsonSerializer.FIELD_TITLE);
            String subtitle = (String) imageJson.get(EventBaseJsonSerializer.FIELD_SUBTITLE);
            String copyright = (String) imageJson.get(EventBaseJsonSerializer.FIELD_COPYRIGHT);
            String localeRaw = (String)  imageJson.get(EventBaseJsonSerializer.FIELD_LOCALE);
            // TODO use Locale.forLanguageTag(localeRaw) -> only possible with Android API Level 21
            Locale locale = localeRaw != null ? new Locale(localeRaw) : null; 
            Number imageWidth = (Number) imageJson.get(EventBaseJsonSerializer.FIELD_IMAGE_WIDTH_IN_PX);
            Number imageHeight = (Number) imageJson.get(EventBaseJsonSerializer.FIELD_IMAGE_HEIGHT_IN_PX);
            Number createdAtDateInMs = (Number) imageJson.get(EventBaseJsonSerializer.FIELD_CREATEDATDATE);
            TimePoint createdAtDate = createdAtDateInMs != null ? new MillisecondsTimePoint(createdAtDateInMs.longValue()) : null;
            JSONArray tags = (JSONArray) imageJson.get(EventBaseJsonSerializer.FIELD_TAGS);
            List<String> imageTags = new ArrayList<String>();
            if (tags != null) {
                for (Object tagObject : tags) {
                    imageTags.add((String) tagObject);
                }
            }
            image = new ImageDescriptorImpl(imageURL, createdAtDate);
            image.setCopyright(copyright);
            image.setLocale(locale);
            image.setTitle(title);
            image.setSubtitle(subtitle);
            image.setTags(imageTags);
            if (imageWidth != null && imageHeight != null) {
                image.setSize(imageWidth.intValue(), imageHeight.intValue());
            }
        }
        return image;
    }
    
    private VideoDescriptor loadVideo(JSONObject videoJson) {
        VideoDescriptor video = null;
        URL videoURL = Helpers.getURLField(videoJson, EventBaseJsonSerializer.FIELD_SOURCE_URL);
        if(videoURL != null) {
            String title = (String) videoJson.get(EventBaseJsonSerializer.FIELD_TITLE);
            String subtitle = (String) videoJson.get(EventBaseJsonSerializer.FIELD_SUBTITLE);
            String copyright = (String) videoJson.get(EventBaseJsonSerializer.FIELD_COPYRIGHT);
            String localeRaw = (String)  videoJson.get(EventBaseJsonSerializer.FIELD_LOCALE);
            // TODO use Locale.forLanguageTag(localeRaw) -> only possible with Android API Level 21
            Locale locale = localeRaw != null ? new Locale(localeRaw) : null; 
            Object mimeTypeRaw = videoJson.get(EventBaseJsonSerializer.FIELD_MIMETYPE);
            MimeType mimeType = mimeTypeRaw == null ? null : MimeType.valueOf((String) mimeTypeRaw);
            Number createdAtDateInMs = (Number) videoJson.get(EventBaseJsonSerializer.FIELD_CREATEDATDATE);
            TimePoint createdAtDate = createdAtDateInMs != null ? new MillisecondsTimePoint(createdAtDateInMs.longValue()) : null;
            JSONArray tags = (JSONArray) videoJson.get(EventBaseJsonSerializer.FIELD_TAGS);
            Integer lengthInSeconds = (Integer) videoJson.get(EventBaseJsonSerializer.FIELD_VIDEO_LENGTH_IN_SECONDS);
            URL thumbnailURL = Helpers.getURLField(videoJson, EventBaseJsonSerializer.FIELD_VIDEO_THUMBNAIL_URL);
            List<String> videoTags = new ArrayList<String>();
            if (tags != null) {
                for (Object tagObject : tags) {
                    videoTags.add((String) tagObject);
                }
            }
            video = new VideoDescriptorImpl(videoURL, mimeType, createdAtDate);
            video.setCopyright(copyright);
            video.setLocale(locale);
            video.setTitle(title);
            video.setSubtitle(subtitle);
            video.setTags(videoTags);
            video.setLengthInSeconds(lengthInSeconds);
            video.setThumbnailURL(thumbnailURL);
        }
        return video;
    }
}
