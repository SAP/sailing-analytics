package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.Date;
import java.util.UUID;

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
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonSerializer;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

public class EventDataJsonSerializerWithNullValuesTest {
    protected final UUID expectedId = UUID.randomUUID();
    protected final String expectedName = "ab";
    protected final String expectedDescription = null;
    protected final TimePoint expectedStartDate = new MillisecondsTimePoint(new Date());
    protected final TimePoint expectedEndDate = new MillisecondsTimePoint(new Date());
    protected final Venue expectedVenue = new VenueImpl("Expected Venue");
    protected final URL expectedOfficialWebsiteURL = null;
    protected final URL expectedBaseURL = null;
    protected final LeaderboardGroup expectedLeaderbaordGroup = mock(LeaderboardGroup.class);
    
    protected JsonSerializer<Venue> venueSerializer;
    protected EventBaseJsonSerializer serializer;
    protected EventBaseJsonDeserializer deserializer;
    protected EventBase event;

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
        when(event.getStartDate()).thenReturn(expectedStartDate);
        when(event.getEndDate()).thenReturn(expectedEndDate);
        when(event.getVenue()).thenReturn(expectedVenue);
        when(event.getImages()).thenReturn(Collections.<ImageDescriptor>emptySet());
        when(event.getVideos()).thenReturn(Collections.<VideoDescriptor>emptySet());
        // ... and the serializer itself.		
        serializer = new EventBaseJsonSerializer(new VenueJsonSerializer(new CourseAreaJsonSerializer()),
                new LeaderboardGroupBaseJsonSerializer(), new TrackingConnectorInfoJsonSerializer());
        deserializer = new EventBaseJsonDeserializer(
                new VenueJsonDeserializer(new CourseAreaJsonDeserializer(DomainFactory.INSTANCE)),
                new LeaderboardGroupBaseJsonDeserializer(), new TrackingConnectorInfoJsonDeserializer());

        when(expectedLeaderbaordGroup.getId()).thenReturn(UUID.randomUUID());
        when(expectedLeaderbaordGroup.getName()).thenReturn("LG");
        when(expectedLeaderbaordGroup.getDescription()).thenReturn("LG Description");
        when(expectedLeaderbaordGroup.hasOverallLeaderboard()).thenReturn(false);
        doReturn(Collections.<LeaderboardGroup>singleton(expectedLeaderbaordGroup)).when(event).getLeaderboardGroups();
    }

    @Test
    public void testBasicAttributes() throws MalformedURLException {
        JSONObject result = serializer.serialize(event);
        assertEquals(expectedId,
                UUID.fromString(result.get(EventBaseJsonSerializer.FIELD_ID).toString()));
        assertEquals(expectedName,
                result.get(EventBaseJsonSerializer.FIELD_NAME));
        assertEquals(expectedDescription,
                result.get(EventBaseJsonSerializer.FIELD_DESCRIPTION));
        assertEquals(expectedOfficialWebsiteURL,
                result.get(EventBaseJsonSerializer.FIELD_OFFICIAL_WEBSITE_URL) == null ? null :
                    new URL((String) result.get(EventBaseJsonSerializer.FIELD_OFFICIAL_WEBSITE_URL)));
        assertEquals(expectedDescription,
                result.get(EventBaseJsonSerializer.FIELD_DESCRIPTION));
        assertEquals(expectedStartDate,
                new MillisecondsTimePoint(((Number) result.get(EventBaseJsonSerializer.FIELD_START_DATE)).longValue()));
        assertEquals(expectedEndDate,
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
        assertEquals(expectedLeaderbaordGroup.getName(), deserializedEvent.getLeaderboardGroups().iterator().next().getName());
        assertEquals(expectedLeaderbaordGroup.getDescription(), deserializedEvent.getLeaderboardGroups().iterator().next().getDescription());
        assertEquals(expectedLeaderbaordGroup.getId(), deserializedEvent.getLeaderboardGroups().iterator().next().getId());
        assertEquals(expectedLeaderbaordGroup.hasOverallLeaderboard(), deserializedEvent.getLeaderboardGroups().iterator().next().hasOverallLeaderboard());
    }

    @Test
    public void testSerializesVenue() throws JsonDeserializationException {
        JSONObject result = serializer.serialize(event);
        EventBase event = deserializer.deserialize(result);
        assertEquals(expectedVenue.getName(), event.getVenue().getName());
    }

}
