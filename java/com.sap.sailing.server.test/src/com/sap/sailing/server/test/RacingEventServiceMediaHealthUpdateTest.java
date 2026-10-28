package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.CreateEvent;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.media.MimeType;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;
import com.sap.sse.shared.media.impl.ImageDescriptorImpl;
import com.sap.sse.shared.media.impl.VideoDescriptorImpl;

public class RacingEventServiceMediaHealthUpdateTest {
    private RacingEventService service;
    private Event event;
    private ImageDescriptor imageDescriptor;
    private VideoDescriptor videoDescriptor;
    
    @BeforeEach
    public void setUp() throws MalformedURLException {
        MongoDBService.INSTANCE.getDB().drop();
        service = new RacingEventServiceImpl();
        final UUID eventId = UUID.randomUUID();
        imageDescriptor = new ImageDescriptorImpl(new URL("https://example.com/image.png"), TimePoint.now());
        videoDescriptor = new VideoDescriptorImpl(new URL("https://example.com/video.mp4"), MimeType.mp4, TimePoint.now());
        final CreateEvent createEventOperation = new CreateEvent("Event Name", "Event Description",
                /* startDate */ TimePoint.now(), /* endDate */ TimePoint.now().plus(Duration.ONE_WEEK),
                "The Venue", /* isPublic */ true, eventId, /* officialWebsiteURL */ null, /* baseURL */ null,
                /* sailorsInfoWebsiteURLs */ null, Collections.singleton(imageDescriptor), Collections.singleton(videoDescriptor),
                /* leaderboardGroupIds */ Collections.emptySet());
        event = service.apply(createEventOperation);
    }
    
    @Test
    public void testSettingImageHealthy() {
        service.updateEventImageHealth(event.getId(), imageDescriptor.getURL().toString(), /* missing */ true, /* missingMailNotificationSent */ true);
        final RacingEventService newService = new RacingEventServiceImpl(); // assuming it reads from the default (test) DB
        final Event newEvent = newService.getEvent(event.getId());
        assertEquals(event.getName(), newEvent.getName());
        final ImageDescriptor newImageDescriptor = newEvent.getImages().iterator().next();
        assertEquals(imageDescriptor.isMissing(), newImageDescriptor.isMissing());
        assertEquals(imageDescriptor.isMissingMailNotificationSent(), newImageDescriptor.isMissingMailNotificationSent());
    }
    
    @Test
    public void testTogglingImageHealthyTrueFalse() {
        service.updateEventImageHealth(event.getId(), imageDescriptor.getURL().toString(), /* missing */ true, /* missingMailNotificationSent */ true);
        service.updateEventImageHealth(event.getId(), imageDescriptor.getURL().toString(), /* missing */ false, /* missingMailNotificationSent */ false);
        final RacingEventService newService = new RacingEventServiceImpl(); // assuming it reads from the default (test) DB
        final Event newEvent = newService.getEvent(event.getId());
        assertEquals(event.getName(), newEvent.getName());
        final ImageDescriptor newImageDescriptor = newEvent.getImages().iterator().next();
        assertFalse(newImageDescriptor.isMissing());
        assertFalse(newImageDescriptor.isMissingMailNotificationSent());
    }
    
    @Test
    public void testTogglingImageHealthyFalseTrue() {
        service.updateEventImageHealth(event.getId(), imageDescriptor.getURL().toString(), /* missing */ false, /* missingMailNotificationSent */ false);
        service.updateEventImageHealth(event.getId(), imageDescriptor.getURL().toString(), /* missing */ true, /* missingMailNotificationSent */ true);
        final RacingEventService newService = new RacingEventServiceImpl(); // assuming it reads from the default (test) DB
        final Event newEvent = newService.getEvent(event.getId());
        assertEquals(event.getName(), newEvent.getName());
        final ImageDescriptor newImageDescriptor = newEvent.getImages().iterator().next();
        assertTrue(newImageDescriptor.isMissing());
        assertTrue(newImageDescriptor.isMissingMailNotificationSent());
    }
    
    @Test
    public void testSettingVideoHealthy() {
        service.updateEventVideoHealth(event.getId(), videoDescriptor.getURL().toString(), /* missing */ true, /* missingMailNotificationSent */ true);
        final RacingEventService newService = new RacingEventServiceImpl(); // assuming it reads from the default (test) DB
        final Event newEvent = newService.getEvent(event.getId());
        assertEquals(event.getName(), newEvent.getName());
        final VideoDescriptor newVideoDescriptor = newEvent.getVideos().iterator().next();
        assertEquals(videoDescriptor.isMissing(), newVideoDescriptor.isMissing());
        assertEquals(videoDescriptor.isMissingMailNotificationSent(), newVideoDescriptor.isMissingMailNotificationSent());
    }

    @Test
    public void testTogglingVideoHealthyTrueFalse() {
        service.updateEventVideoHealth(event.getId(), videoDescriptor.getURL().toString(), /* missing */ true, /* missingMailNotificationSent */ true);
        service.updateEventVideoHealth(event.getId(), videoDescriptor.getURL().toString(), /* missing */ false, /* missingMailNotificationSent */ false);
        final RacingEventService newService = new RacingEventServiceImpl(); // assuming it reads from the default (test) DB
        final Event newEvent = newService.getEvent(event.getId());
        assertEquals(event.getName(), newEvent.getName());
        final VideoDescriptor newVideoDescriptor = newEvent.getVideos().iterator().next();
        assertFalse(newVideoDescriptor.isMissing());
        assertFalse(newVideoDescriptor.isMissingMailNotificationSent());
    }
    
    @Test
    public void testTogglingVideoHealthyFalseTrue() {
        service.updateEventVideoHealth(event.getId(), videoDescriptor.getURL().toString(), /* missing */ false, /* missingMailNotificationSent */ false);
        service.updateEventVideoHealth(event.getId(), videoDescriptor.getURL().toString(), /* missing */ true, /* missingMailNotificationSent */ true);
        final RacingEventService newService = new RacingEventServiceImpl(); // assuming it reads from the default (test) DB
        final Event newEvent = newService.getEvent(event.getId());
        assertEquals(event.getName(), newEvent.getName());
        final VideoDescriptor newVideoDescriptor = newEvent.getVideos().iterator().next();
        assertTrue(newVideoDescriptor.isMissing());
        assertTrue(newVideoDescriptor.isMissingMailNotificationSent());
    }
    
}
