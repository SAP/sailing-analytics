package com.sap.sailing.server.gateway.serialization.impl;

import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sse.common.media.ImageDescriptor;
import com.sap.sse.common.media.VideoDescriptor;

public class EventBaseJsonSerializer implements JsonSerializer<EventBase> {
    public static final String FIELD_ID = "id";
    public static final String FIELD_NAME = "name";
    public static final String FIELD_DESCRIPTION = "description";
    public static final String FIELD_START_DATE = "startDate";
    public static final String FIELD_END_DATE = "endDate";
    public static final String FIELD_VENUE = "venue";
    public static final String FIELD_IMAGE_URLS = "imageURLs";
    public static final String FIELD_VIDEO_URLS = "videoURLs";
    public static final String FIELD_SPONSOR_IMAGE_URLS = "sponsorImageURLs";
    public static final String FIELD_LOGO_IMAGE_URL = "logoImageURL";
    public static final String FIELD_IMAGE_SIZES = "imageSizes";
    public static final String FIELD_IMAGE_URL = "imageURL";
    public static final String FIELD_IMAGE_WIDTH = "imageWidth";
    public static final String FIELD_IMAGE_HEIGHT = "imageHeight";
    public static final String FIELD_VIDEO_URL = "videoURL";
    public static final String FIELD_OFFICIAL_WEBSITE_URL = "officialWebsiteURL";
    public static final String FIELDS_LEADERBOARD_GROUPS = "leaderboardGroups";

    public static final String FIELD_SOURCE_URL = "sourceURL";
    public static final String FIELD_TITLE = "title";
    public static final String FIELD_SUBTITLE = "subtitle";
    public static final String FIELD_MIMETYPE = "mimetype";
    public static final String FIELD_COPYRIGHT = "copyright";
    public static final String FIELD_CREATEDATDATE = "createdAtDate";
    public static final String FIELD_LOCALE = "locale";
    public static final String FIELD_TAGS = "tags";
    // specific image fields
    public static final String FIELD_IMAGES = "images";
    public static final String FIELD_IMAGE_WIDTH_IN_PX = "widthInPixel";
    public static final String FIELD_IMAGE_HEIGHT_IN_PX = "heightInPixel";
    // specific video fields
    public static final String FIELD_VIDEOS = "videos";
    public static final String FIELD_VIDEO_THUMBNAIL_URL = "thumbnailURL";
    public static final String FIELD_VIDEO_LENGTH_IN_SECONDS = "lengthInSeconds";

    private final JsonSerializer<Venue> venueSerializer;
    private final JsonSerializer<? super LeaderboardGroupBase> leaderboardGroupBaseSerializer;

    public EventBaseJsonSerializer(JsonSerializer<Venue> venueSerializer, JsonSerializer<? super LeaderboardGroupBase> leaderboardGroupBaseSerializer) {
        this.leaderboardGroupBaseSerializer = leaderboardGroupBaseSerializer;
        this.venueSerializer = venueSerializer;
    }

    @SuppressWarnings("deprecation")
    public JSONObject serialize(EventBase event) {
        JSONObject result = new JSONObject();
        result.put(FIELD_ID, event.getId().toString());
        result.put(FIELD_NAME, event.getName());
        result.put(FIELD_DESCRIPTION, event.getDescription());
        result.put(FIELD_OFFICIAL_WEBSITE_URL, event.getOfficialWebsiteURL() != null ? event.getOfficialWebsiteURL().toString() : null);
        result.put(FIELD_START_DATE, event.getStartDate() != null ? event.getStartDate().asMillis() : null);
        result.put(FIELD_END_DATE, event.getStartDate() != null ? event.getEndDate().asMillis() : null);
        result.put(FIELD_VENUE, venueSerializer.serialize(event.getVenue()));
        result.put(FIELD_LOGO_IMAGE_URL, event.getLogoImageURL() != null ? event.getLogoImageURL().toString() : null);
        result.put(FIELD_IMAGE_URLS, getURLsAsStringArray(event.getImageURLs()));
        result.put(FIELD_VIDEO_URLS, getURLsAsStringArray(event.getVideoURLs()));
        result.put(FIELD_SPONSOR_IMAGE_URLS, getURLsAsStringArray(event.getSponsorImageURLs()));
        JSONArray leaderboardGroups = new JSONArray();
        result.put(FIELDS_LEADERBOARD_GROUPS, leaderboardGroups);
        for (LeaderboardGroupBase lg : event.getLeaderboardGroups()) {
            leaderboardGroups.add(leaderboardGroupBaseSerializer.serialize(lg));
        }
        JSONArray imageSizes = new JSONArray();
        result.put(FIELD_IMAGE_SIZES, imageSizes);
        for (ImageDescriptor image : event.getImages()) {
            addImageSize(image, imageSizes);
        }
        JSONArray jsonImages = new JSONArray();
        for(ImageDescriptor imageDescriptor: event.getImages()) {
            addImage(imageDescriptor, jsonImages);
        }
        result.put(FIELD_IMAGES, jsonImages);
        JSONArray jsonVideos = new JSONArray();
        for(VideoDescriptor videoDescriptor: event.getVideos()) {
            addVideo(videoDescriptor, jsonVideos);
        }
        result.put(FIELD_VIDEOS, jsonVideos);
        return result;
    }

    private void addImage(ImageDescriptor image, JSONArray imagesJson) {
        JSONObject imageJson = new JSONObject();
        imageJson.put(FIELD_SOURCE_URL, image.getURL().toString());
        // TODO use toLanguageTag() when deserializer can use Locale.forLanguageTag()
        imageJson.put(FIELD_LOCALE, image.getLocale() != null ? image.getLocale().getLanguage() : null);
        imageJson.put(FIELD_TITLE, image.getTitle());
        imageJson.put(FIELD_SUBTITLE, image.getSubtitle());
        imageJson.put(FIELD_MIMETYPE, image.getMimeType().name());
        imageJson.put(FIELD_COPYRIGHT, image.getCopyright());
        imageJson.put(FIELD_IMAGE_WIDTH_IN_PX, image.getWidthInPx());
        imageJson.put(FIELD_IMAGE_HEIGHT_IN_PX, image.getHeightInPx());
        imageJson.put(FIELD_CREATEDATDATE, image.getCreatedAtDate().asMillis());
        JSONArray tags = new JSONArray();
        for (String tag : image.getTags()) {
            tags.add(tag);
        }
        imageJson.put(FIELD_TAGS, tags);
        imagesJson.add(imageJson);
    }

    private void addVideo(VideoDescriptor video, JSONArray videosJson) {
        JSONObject videoJson = new JSONObject();
        videoJson.put(FIELD_SOURCE_URL, video.getURL().toString());
        // TODO use toLanguageTag() when deserializer can use Locale.forLanguageTag()
        videoJson.put(FIELD_LOCALE, video.getLocale() != null ? video.getLocale().getLanguage() : null);
        videoJson.put(FIELD_VIDEO_THUMBNAIL_URL, video.getThumbnailURL() != null ? video.getThumbnailURL().toString() : null);
        videoJson.put(FIELD_TITLE, video.getTitle());
        videoJson.put(FIELD_SUBTITLE, video.getSubtitle());
        videoJson.put(FIELD_MIMETYPE, video.getMimeType().name());
        videoJson.put(FIELD_COPYRIGHT, video.getCopyright());
        videoJson.put(FIELD_VIDEO_LENGTH_IN_SECONDS, video.getLengthInSeconds());
        videoJson.put(FIELD_CREATEDATDATE, video.getCreatedAtDate().asMillis());
        JSONArray tags = new JSONArray();
        for (String tag : video.getTags()) {
            tags.add(tag);
        }
        videoJson.put(FIELD_TAGS, tags);
        videosJson.add(videoJson);
    }

    /**
     * For backward compatibility the size of an image is also serialized as
     * JSON object to <code>imageSizes</code>
     */
    private void addImageSize(ImageDescriptor image, JSONArray imageSizes) {
        if(image.getHeightInPx() != null && image.getWidthInPx() != null) {
            JSONObject imageSizeJson = new JSONObject();
            imageSizes.add(imageSizeJson);
            imageSizeJson.put(FIELD_IMAGE_URL, image.getURL().toString());
            imageSizeJson.put(FIELD_IMAGE_WIDTH, image.getWidthInPx());
            imageSizeJson.put(FIELD_IMAGE_HEIGHT, image.getHeightInPx());
        }
    }

    private JSONArray getURLsAsStringArray(Iterable<URL> urls) {
        JSONArray jsonImageURLs = new JSONArray();
        for (URL url : urls) {
            jsonImageURLs.add(url.toString());
        }
        return jsonImageURLs;
    }
}
