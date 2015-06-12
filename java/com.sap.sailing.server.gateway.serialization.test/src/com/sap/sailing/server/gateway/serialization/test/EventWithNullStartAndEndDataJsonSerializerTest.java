package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.LeaderboardGroupBase;
import com.sap.sailing.domain.base.Venue;
import com.sap.sailing.domain.base.impl.VenueImpl;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.impl.CourseAreaJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.EventBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.LeaderboardGroupBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.VenueJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CourseAreaJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.EventBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.LeaderboardGroupBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.VenueJsonSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.media.ImageDescriptor;
import com.sap.sse.common.media.VideoDescriptor;

public class EventWithNullStartAndEndDataJsonSerializerTest {

    protected final UUID expectedId = UUID.randomUUID();
    protected final String expectedName = "ab";
    protected final String expectedDescription = "cd";
    protected final TimePoint expectedStartDate = null;
    protected final TimePoint expectedEndDate = null;
    protected final Venue expectedVenue = new VenueImpl("Expected Venue");
    protected final URL expectedOfficialWebsiteURL;
    protected final URL expectedLogoImageURL;
    protected final LeaderboardGroupBase expectedLeaderboardGroup = mock(LeaderboardGroupBase.class);
    protected final Iterable<LeaderboardGroupBase> expectedLeaderboardGroups = Collections.singleton(expectedLeaderboardGroup);

    protected JsonSerializer<Venue> venueSerializer;
    protected EventBaseJsonSerializer serializer;
    protected EventBaseJsonDeserializer deserializer;
    protected EventBase event;

    public EventWithNullStartAndEndDataJsonSerializerTest() throws MalformedURLException {
        expectedOfficialWebsiteURL = new URL("http://official.website.com");
        expectedLogoImageURL = new URL("http://official.logo.com/logo.png");
    }
    
    @Before
    public void setUp() {
        // Event and its basic attributes ...
        when(expectedLeaderboardGroup.getId()).thenReturn(UUID.randomUUID());
        when(expectedLeaderboardGroup.getName()).thenReturn("My Leaderboard Group");
        when(expectedLeaderboardGroup.getDescription()).thenReturn("This is My Leaderboard Group");
        when(expectedLeaderboardGroup.hasOverallLeaderboard()).thenReturn(true);
        event = mock(EventBase.class);
        when(event.getId()).thenReturn(expectedId);
        when(event.getName()).thenReturn(expectedName);
        when(event.getDescription()).thenReturn(expectedDescription);
        when(event.getOfficialWebsiteURL()).thenReturn(expectedOfficialWebsiteURL);
        when(event.getLogoImageURL()).thenReturn(expectedLogoImageURL);
        when(event.getStartDate()).thenReturn(expectedStartDate);
        when(event.getEndDate()).thenReturn(expectedEndDate);
        when(event.getVenue()).thenReturn(expectedVenue);
        when(event.getImageURLs()).thenReturn(Collections.<URL>emptySet());
        when(event.getVideoURLs()).thenReturn(Collections.<URL>emptySet());
        when(event.getSponsorImageURLs()).thenReturn(Collections.<URL>emptySet());
        when(event.getImages()).thenReturn(Collections.<ImageDescriptor>emptySet());
        when(event.getVideos()).thenReturn(Collections.<VideoDescriptor>emptySet());
        doReturn(expectedLeaderboardGroups).when(event).getLeaderboardGroups();

        // ... and the serializer itself.		
        serializer = new EventBaseJsonSerializer(new VenueJsonSerializer(new CourseAreaJsonSerializer()), new LeaderboardGroupBaseJsonSerializer());
        deserializer = new EventBaseJsonDeserializer(new VenueJsonDeserializer(new CourseAreaJsonDeserializer(DomainFactory.INSTANCE)), new LeaderboardGroupBaseJsonDeserializer());
    }

    @Test
    public void testBasicAttributes() {
        JSONObject result = serializer.serialize(event);
        assertEquals(expectedId, UUID.fromString(result.get(EventBaseJsonSerializer.FIELD_ID).toString()));
        assertEquals(expectedName, result.get(EventBaseJsonSerializer.FIELD_NAME));
        assertEquals(expectedDescription, result.get(EventBaseJsonSerializer.FIELD_DESCRIPTION));
        assertNull(result.get(EventBaseJsonSerializer.FIELD_START_DATE));
        assertNull(result.get(EventBaseJsonSerializer.FIELD_END_DATE));
    }

    @Test
    public void testBasicAttributesAfterDeserialization() throws JsonDeserializationException {
        final JSONObject result = serializer.serialize(event);
        final EventBase deserializedEvent = deserializer.deserialize(result);
        assertEquals(expectedId, deserializedEvent.getId());
        assertEquals(expectedName, deserializedEvent.getName());
        assertEquals(expectedDescription, deserializedEvent.getDescription());
        assertEquals(expectedOfficialWebsiteURL, deserializedEvent.getOfficialWebsiteURL());
        assertEquals(expectedLogoImageURL, deserializedEvent.getLogoImageURL());
        LeaderboardGroupBase deserializedLg = deserializedEvent.getLeaderboardGroups().iterator().next();
        assertEquals(expectedLeaderboardGroup.getName(), deserializedLg.getName());
        assertEquals(expectedLeaderboardGroup.getDescription(), deserializedLg.getDescription());
        assertEquals(expectedLeaderboardGroup.hasOverallLeaderboard(), deserializedLg.hasOverallLeaderboard());
        assertNull(deserializedEvent.getStartDate());
        assertNull(deserializedEvent.getEndDate());
    }

    @Test
    public void testSerializesVenue() throws JsonDeserializationException {
        JSONObject result = serializer.serialize(event);
        EventBase event = deserializer.deserialize(result);
        assertEquals(expectedVenue.getName(), event.getVenue().getName());
    }
}
