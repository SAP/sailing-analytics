package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.impl.VenueImpl;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.server.gateway.deserialization.impl.CourseAreaJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.EventBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.LeaderboardGroupBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.TrackingConnectorInfoJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.VenueJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CourseAreaJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.EventBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.LeaderboardGroupBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TrackingConnectorInfoJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.VenueJsonSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.media.impl.ImageDescriptorImpl;
import com.sap.sse.shared.media.impl.VideoDescriptorImpl;

public class EventDataJsonSerializerWithImagesAndVideosTest {
    private final UUID expectedId = UUID.randomUUID();
    private final String expectedName = "ab";
    private final Locale locale = Locale.GERMAN;
    private final TimePoint expectedStartDate = new MillisecondsTimePoint(new Date());
    private final TimePoint expectedEndDate = new MillisecondsTimePoint(new Date());
    private final Venue expectedVenue = new VenueImpl("Expected Venue");
    private final LeaderboardGroup expectedLeaderboardGroup = mock(LeaderboardGroup.class);
    private final URL imageURL;
    private final String copyright = "copyright by Alex";
    private final String imageTitle = "My image title";
    private final String imageSubtitle = "My image subtitle";
    private final Integer imageWidth = 500;
    private final Integer imageHeight = 300;
    
    private final URL videoURL;
    private final URL videoThumbnailURL;
    private final Locale localeFr = Locale.FRENCH;
    private final Locale localeFrCa = Locale.CANADA_FRENCH;
    private final Integer videoLengthInSeconds = 2  * 60 * 60 * 1000; // 2h 
    private final MimeType mimeType = MimeType.mp4;
    private final String copyright2 = "copyright by Don";
    private final String videoTitle = "My video title";
    private final String videoSubtitle = "My video subtitle";
    
    private final TimePoint createdAt = MillisecondsTimePoint.now(); 
    private List<ImageDescriptor> images = new ArrayList<ImageDescriptor>();
    private List<VideoDescriptor> videos = new ArrayList<VideoDescriptor>();

    private EventBaseJsonSerializer serializer;
    private EventBaseJsonDeserializer deserializer;
    private EventBase event;
    
    public EventDataJsonSerializerWithImagesAndVideosTest() throws MalformedURLException {
        imageURL = new URL("http://some.host/with/some/file2.jpg");
        ImageDescriptor image1 = new ImageDescriptorImpl(imageURL, createdAt);
        image1.setCopyright(copyright);
        image1.setLocale(locale);
        image1.setSize(imageWidth, imageHeight);
        image1.setTitle(imageTitle);
        image1.setSubtitle(imageSubtitle);
        image1.addTag("Tag1");
        image1.addTag("Tag2");
        image1.addTag("Tag3");
        images.add(image1);
        
        videoURL = new URL("http://some.host/with/some/video.mpg");
        videoThumbnailURL = new URL("http://some.host/with/some/video_thumbnail.jpg");
        
        VideoDescriptor video1 = new VideoDescriptorImpl(videoURL, mimeType, createdAt);
        video1.setCopyright(copyright2);
        video1.setLocale(localeFrCa);
        video1.setTitle(videoTitle);
        video1.setSubtitle(videoSubtitle);
        video1.setThumbnailURL(videoThumbnailURL);
        video1.setLengthInSeconds(videoLengthInSeconds);
        video1.addTag("Tag1");
        video1.addTag("Tag2");
        video1.addTag("Tag3");
        video1.addTag("Tag4");
        videos.add(video1);
    }
    
    @BeforeEach
    public void setUp() {
        // Event and its basic attributes ...
        event = mock(EventBase.class);
        when(event.getId()).thenReturn(expectedId);
        when(event.getName()).thenReturn(expectedName);
        when(event.getStartDate()).thenReturn(expectedStartDate);
        when(event.getEndDate()).thenReturn(expectedEndDate);
        when(event.getVenue()).thenReturn(expectedVenue);
        when(event.getImages()).thenReturn(images);
        when(event.getVideos()).thenReturn(videos);
        // ... and the serializer itself.		
        serializer = new EventBaseJsonSerializer(new VenueJsonSerializer(new CourseAreaJsonSerializer()),
                new LeaderboardGroupBaseJsonSerializer(), new TrackingConnectorInfoJsonSerializer());
        deserializer = new EventBaseJsonDeserializer(
                new VenueJsonDeserializer(new CourseAreaJsonDeserializer(DomainFactory.INSTANCE)),
                new LeaderboardGroupBaseJsonDeserializer(), new TrackingConnectorInfoJsonDeserializer());

        when(expectedLeaderboardGroup.getId()).thenReturn(UUID.randomUUID());
        when(expectedLeaderboardGroup.getName()).thenReturn("LG");
        when(expectedLeaderboardGroup.getDescription()).thenReturn("LG Description");
        when(expectedLeaderboardGroup.getDisplayName()).thenReturn("LG Display Name");
        when(expectedLeaderboardGroup.hasOverallLeaderboard()).thenReturn(false);
        doReturn(Collections.<LeaderboardGroup>singleton(expectedLeaderboardGroup)).when(event).getLeaderboardGroups();
    }

    @Test
    public void testImageAndVideosAttributes() throws MalformedURLException {
        JSONObject result = serializer.serialize(event);
        JSONArray imagesJson = (JSONArray) result.get(EventBaseJsonSerializer.FIELD_IMAGES);
        JSONObject image1Json = (JSONObject) imagesJson.get(0);
        assertEquals(imageURL, new URL((String) image1Json.get(EventBaseJsonSerializer.FIELD_SOURCE_URL)));
        assertEquals(createdAt, new MillisecondsTimePoint(((Number) image1Json.get(EventBaseJsonSerializer.FIELD_CREATEDATDATE)).longValue()));
        assertEquals(copyright, (String) image1Json.get(EventBaseJsonSerializer.FIELD_COPYRIGHT));
        assertEquals(imageTitle, (String) image1Json.get(EventBaseJsonSerializer.FIELD_TITLE));
        assertEquals(imageSubtitle, (String) image1Json.get(EventBaseJsonSerializer.FIELD_SUBTITLE));
        assertEquals(imageWidth, (Integer) image1Json.get(EventBaseJsonSerializer.FIELD_IMAGE_WIDTH_IN_PX));
        assertEquals(imageHeight, (Integer) image1Json.get(EventBaseJsonSerializer.FIELD_IMAGE_HEIGHT_IN_PX));
        JSONArray imageTagsJson = (JSONArray) image1Json.get(EventBaseJsonSerializer.FIELD_TAGS);
        assertEquals(3, imageTagsJson.size());
        
        JSONArray videosJson = (JSONArray) result.get(EventBaseJsonSerializer.FIELD_VIDEOS);
        JSONObject video1Json = (JSONObject) videosJson.get(0);
        assertEquals(videoURL, new URL((String) video1Json.get(EventBaseJsonSerializer.FIELD_SOURCE_URL)));
        assertEquals(createdAt, new MillisecondsTimePoint(((Number) video1Json.get(EventBaseJsonSerializer.FIELD_CREATEDATDATE)).longValue()));
        assertEquals(copyright2, (String) video1Json.get(EventBaseJsonSerializer.FIELD_COPYRIGHT));
        assertEquals(videoTitle, (String) video1Json.get(EventBaseJsonSerializer.FIELD_TITLE));
        assertEquals(videoSubtitle, (String) video1Json.get(EventBaseJsonSerializer.FIELD_SUBTITLE));
        assertEquals(videoThumbnailURL, new URL((String) video1Json.get(EventBaseJsonSerializer.FIELD_VIDEO_THUMBNAIL_URL)));
        assertEquals(videoLengthInSeconds, (Integer) video1Json.get(EventBaseJsonSerializer.FIELD_VIDEO_LENGTH_IN_SECONDS));
        JSONArray videoTagsJson = (JSONArray) video1Json.get(EventBaseJsonSerializer.FIELD_TAGS);
        assertEquals(4, videoTagsJson.size());
    }

    @Test
    public void testImageAndVideosAfterDeserialization() throws JsonDeserializationException {
        final JSONObject result = serializer.serialize(event);
        final EventBase deserializedEvent = deserializer.deserialize(result);

        ImageDescriptor image1 = deserializedEvent.getImages().iterator().next();
        assertEquals(createdAt, image1.getCreatedAtDate());
        assertEquals(imageURL, image1.getURL());
        assertEquals(locale, image1.getLocale());
        assertEquals(copyright, image1.getCopyright());
        assertEquals(imageTitle, image1.getTitle());
        assertEquals(imageSubtitle, image1.getSubtitle());
        assertEquals(imageWidth, image1.getWidthInPx());
        assertEquals(imageHeight, image1.getHeightInPx());
        assertEquals(3, Util.size(image1.getTags()));
        
        VideoDescriptor video1 = deserializedEvent.getVideos().iterator().next();
        assertEquals(createdAt, video1.getCreatedAtDate());
        assertEquals(videoURL, video1.getURL());
        assertEquals(mimeType, video1.getMimeType());
        assertEquals(copyright2, video1.getCopyright());
        // Only the language part of the locale is being used
        assertEquals(localeFr, video1.getLocale());
        assertEquals(videoTitle, video1.getTitle());
        assertEquals(videoSubtitle, video1.getSubtitle());
        assertEquals(videoThumbnailURL, video1.getThumbnailURL());
        assertEquals(videoLengthInSeconds, video1.getLengthInSeconds());
        assertEquals(4, Util.size(video1.getTags()));
    }
}
