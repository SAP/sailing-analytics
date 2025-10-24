package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.impl.VenueImpl;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
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
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonSerializer;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.media.impl.ImageDescriptorImpl;

public class EventDataJsonSerializerTest {
    protected final UUID expectedId = UUID.randomUUID();
    protected final String expectedName = "ab";
    protected final String expectedDescription = "cd";
    protected final TimePoint expectedStartDate = new MillisecondsTimePoint(new Date());
    protected final TimePoint expectedEndDate = new MillisecondsTimePoint(new Date());
    protected final Venue expectedVenue = new VenueImpl("Expected Venue");
    protected final URL expectedOfficialWebsiteURL;
    protected final URL expectedBaseURL;
    protected final Map<Locale, URL> expectedSailorsInfoWebsiteURLs;
    protected final URL expectedLogoImageURL;
    protected final ImageDescriptor expectedLogoImageDescriptor;
    protected final LeaderboardGroup expectedLeaderboardGroup = mock(LeaderboardGroup.class);
    
    protected JsonSerializer<Venue> venueSerializer;
    protected EventBaseJsonSerializer serializer;
    protected EventBaseJsonDeserializer deserializer;
    protected EventBase event;

    public EventDataJsonSerializerTest() throws MalformedURLException {
        expectedOfficialWebsiteURL = new URL("http://official.website.com");
        expectedBaseURL = new URL("http://our.own.com");
        expectedLogoImageURL = new URL("http://official.logo.com/logo.png");
        expectedLogoImageDescriptor = new ImageDescriptorImpl(expectedLogoImageURL, MillisecondsTimePoint.now());
        expectedSailorsInfoWebsiteURLs = new HashMap<>();
        expectedSailorsInfoWebsiteURLs.put(null, new URL("http://sailorinfo.some-sailing-event.com"));
        expectedSailorsInfoWebsiteURLs.put(Locale.GERMAN, new URL("http://sailorinfo-de.some-sailing-event.com"));
    }
    
    // see https://groups.google.com/forum/?fromgroups=#!topic/mockito/iMumB0_bpdo
    @BeforeEach
    public void setUp() {
        // Event and its basic attributes ...
        event = mock(EventBase.class);
        when(event.getId()).thenReturn(expectedId);
        when(event.getName()).thenReturn(expectedName);
        when(event.getDescription()).thenReturn(expectedDescription);
        when(event.getOfficialWebsiteURL()).thenReturn(expectedOfficialWebsiteURL);
        when(event.getBaseURL()).thenReturn(expectedBaseURL);
        when(event.getSailorsInfoWebsiteURLs()).thenReturn(expectedSailorsInfoWebsiteURLs);
        when(event.getSailorsInfoWebsiteURL(null)).thenReturn(expectedSailorsInfoWebsiteURLs.get(null));
        when(event.getStartDate()).thenReturn(expectedStartDate);
        when(event.getEndDate()).thenReturn(expectedEndDate);
        when(event.getVenue()).thenReturn(expectedVenue);
        final CourseArea alpha = DomainFactory.INSTANCE.getOrCreateCourseArea(UUID.randomUUID(), "Alpha", new DegreePosition(49, 8), new NauticalMileDistance(2));
        expectedVenue.addCourseArea(alpha);
        final CourseArea bravo= DomainFactory.INSTANCE.getOrCreateCourseArea(UUID.randomUUID(), "Bravo", /* centerPosition */ null, /* radius */ null);
        expectedVenue.addCourseArea(bravo);
        when(event.getVideos()).thenReturn(Collections.<VideoDescriptor>emptySet());
        when(event.getImages()).thenReturn(Collections.<ImageDescriptor>singleton(expectedLogoImageDescriptor));
        when(event.getVideos()).thenReturn(Collections.<VideoDescriptor>emptySet());
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
    public void testBasicAttributes() throws MalformedURLException {
        JSONObject result = serializer.serialize(event);
        assertEquals(
                expectedId,
                UUID.fromString(result.get(EventBaseJsonSerializer.FIELD_ID).toString()));
        assertEquals(
                expectedName,
                result.get(EventBaseJsonSerializer.FIELD_NAME));
        assertEquals(
                expectedDescription,
                result.get(EventBaseJsonSerializer.FIELD_DESCRIPTION));
        assertEquals(
                expectedOfficialWebsiteURL,
                new URL((String) result.get(EventBaseJsonSerializer.FIELD_OFFICIAL_WEBSITE_URL)));
        assertEquals(
                expectedDescription,
                result.get(EventBaseJsonSerializer.FIELD_DESCRIPTION));
        assertEquals(
                expectedStartDate,
                new MillisecondsTimePoint(((Number) result.get(EventBaseJsonSerializer.FIELD_START_DATE)).longValue()));
        assertEquals(
                expectedEndDate,
                new MillisecondsTimePoint(((Number) result.get(EventBaseJsonSerializer.FIELD_END_DATE)).longValue()));
    }

    @Test
    public void testBasicAttributesAfterDeserialization() throws JsonDeserializationException {
        final JSONObject result = serializer.serialize(event);
        final EventBase deserializedEvent = deserializer.deserialize(result);
        assertEquals(expectedId, deserializedEvent.getId());
        assertEquals(expectedName, deserializedEvent.getName());
        assertEquals(expectedStartDate, deserializedEvent.getStartDate());
        assertEquals(expectedEndDate, deserializedEvent.getEndDate());
        assertEquals(expectedLeaderboardGroup.getName(), deserializedEvent.getLeaderboardGroups().iterator().next().getName());
        assertEquals(expectedLeaderboardGroup.getDescription(), deserializedEvent.getLeaderboardGroups().iterator().next().getDescription());
        assertEquals(expectedLeaderboardGroup.getDisplayName(), deserializedEvent.getLeaderboardGroups().iterator().next().getDisplayName());
        assertEquals(expectedLeaderboardGroup.getId(), deserializedEvent.getLeaderboardGroups().iterator().next().getId());
        assertEquals(expectedLeaderboardGroup.hasOverallLeaderboard(), deserializedEvent.getLeaderboardGroups().iterator().next().hasOverallLeaderboard());
        assertEquals(expectedSailorsInfoWebsiteURLs, new HashMap<Locale, URL>(deserializedEvent.getSailorsInfoWebsiteURLs()));
        assertEquals(1, Util.size(deserializedEvent.getImages()));
        assertEquals(expectedLogoImageURL, deserializedEvent.getImages().iterator().next().getURL());
    }

    @Test
    public void testSerializesVenue() throws JsonDeserializationException {
        JSONObject result = serializer.serialize(event);
        EventBase event = deserializer.deserialize(result);
        assertEquals(expectedVenue.getName(), event.getVenue().getName());
        final Map<UUID, CourseArea> expectedCourseAreasByUUID = new HashMap<>();
        for (final CourseArea expectedCourseArea : expectedVenue.getCourseAreas()) {
            expectedCourseAreasByUUID.put(expectedCourseArea.getId(), expectedCourseArea);
        }
        assertEquals(Util.size(expectedVenue.getCourseAreas()), Util.size(event.getVenue().getCourseAreas()));
        for (final CourseArea courseArea : event.getVenue().getCourseAreas()) {
            final CourseArea expectedCourseArea = expectedCourseAreasByUUID.get(courseArea.getId());
            assertEquals(expectedCourseArea.getName(), courseArea.getName());
            assertEquals(expectedCourseArea.getCenterPosition(), courseArea.getCenterPosition());
            assertEquals(expectedCourseArea.getRadius(), courseArea.getRadius());
        }
    }

}
