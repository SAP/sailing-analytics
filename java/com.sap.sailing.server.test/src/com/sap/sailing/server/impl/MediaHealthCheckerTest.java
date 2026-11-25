package com.sap.sailing.server.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Locale;
import java.util.Optional;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.UpdateEventImageHealth;
import com.sap.sailing.server.operationaltransformation.UpdateEventVideoHealth;
import com.sap.sse.common.Duration;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.impl.Ownership;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

public class MediaHealthCheckerTest {
    private static final String VALID_IMAGE_PATH = "/image";
    private static final String VALID_VIDEO_PATH = "/video";
    private static final String REDIRECT_PATH = "/redirect";
    private static final String HTTP_ERROR_PATH = "/not-found";
    private static final String RATE_LIMITED_PATH = "/rate-limited";
    private static final String RATE_LIMITED_REDIRECT_PATH = "/redirect-rate-limited";
    private static final String HTML_PATH = "/html";
    private static final String SLOW_IMAGE_PATH = "/slow-image";

    private TestHttpServer server;
    private byte[] imageBytes;

    @BeforeEach
    public void setUp() throws Exception {
        imageBytes = createImageBytes();
        server = new TestHttpServer();
        server.start();
    }

    @AfterEach
    public void tearDown() throws Exception {
        server.stop();
    }

    @Test
    public void testValidImageIsAvailable() throws Exception {
        assertEquals(Optional.of(/* value */ true), new MediaUrlHealthChecker().isImageAvailable(getUrl(VALID_IMAGE_PATH)));
    }

    @Test
    public void testRedirectToValidImageIsAvailable() throws Exception {
        assertEquals(Optional.of(/* value */ true), new MediaUrlHealthChecker().isImageAvailable(getUrl(REDIRECT_PATH)));
    }

    @Test
    public void testHttpErrorIsNotAvailable() throws Exception {
        assertEquals(Optional.of(/* value */ false), new MediaUrlHealthChecker().isImageAvailable(getUrl(HTTP_ERROR_PATH)));
    }

    @Test
    public void testHtmlResponseIsNotAvailable() throws Exception {
        assertEquals(Optional.of(/* value */ false), new MediaUrlHealthChecker().isImageAvailable(getUrl(HTML_PATH)));
    }

    @Test
    public void testReadTimeoutIsNotAvailable() throws Exception {
        final MediaUrlHealthChecker checker = new MediaUrlHealthChecker(Duration.ONE_SECOND.divide(10));
        assertEquals(Optional.of(/* value */ false), checker.isImageAvailable(getUrl(SLOW_IMAGE_PATH)));
    }

    @Test
    public void testConnectionFailureIsNotAvailable() throws Exception {
        final URL url = getUrl(VALID_IMAGE_PATH);
        server.stop();
        assertEquals(Optional.of(/* value */ false), new MediaUrlHealthChecker(Duration.ONE_SECOND).isImageAvailable(url));
    }

    @Test
    public void testRateLimitedImageHasNoVerdict() throws Exception {
        assertEquals(/* expected */ Optional.empty(), new MediaUrlHealthChecker().isImageAvailable(getUrl(RATE_LIMITED_PATH)));
    }

    @Test
    public void testRedirectToRateLimitedImageHasNoVerdict() throws Exception {
        assertEquals(/* expected */ Optional.empty(), new MediaUrlHealthChecker().isImageAvailable(getUrl(RATE_LIMITED_REDIRECT_PATH)));
    }

    @Test
    public void testRateLimitedVideoHasNoVerdict() throws Exception {
        assertEquals(/* expected */ Optional.empty(), new MediaUrlHealthChecker().isUrlReachable(getUrl(RATE_LIMITED_PATH)));
    }

    @Test
    public void testRedirectToRateLimitedVideoHasNoVerdict() throws Exception {
        assertEquals(/* expected */ Optional.empty(), new MediaUrlHealthChecker().isUrlReachable(getUrl(RATE_LIMITED_REDIRECT_PATH)));
    }

    @Test
    public void testRateLimitingIsLoggedAtInfo() throws Exception {
        final Logger logger = Logger.getLogger(MediaUrlHealthChecker.class.getName());
        final Handler handler = mock(Handler.class);
        final Level previousLevel = logger.getLevel();
        logger.setLevel(Level.INFO);
        logger.addHandler(handler);
        try {
            final URL url = getUrl(RATE_LIMITED_PATH);
            final MediaUrlHealthChecker checker = new MediaUrlHealthChecker();
            assertEquals(/* expected */ Optional.empty(), checker.isImageAvailable(url));
            assertEquals(/* expected */ Optional.empty(), checker.isUrlReachable(url));
            final ArgumentCaptor<LogRecord> records = ArgumentCaptor.forClass(LogRecord.class);
            verify(handler, times(/* wantedNumberOfInvocations */ 2)).publish(records.capture());
            for (final LogRecord record : records.getAllValues()) {
                assertEquals(Level.INFO, record.getLevel());
                assertTrue(record.getMessage().contains("429"));
                assertTrue(record.getMessage().contains(url.toString()));
            }
        } finally {
            logger.removeHandler(handler);
            logger.setLevel(previousLevel);
        }
    }

    @Test
    public void testRateLimitedEventImagesDoNotChangeStateOrNotify() throws Exception {
        final URL imageUrl = getUrl(RATE_LIMITED_PATH);
        final Iterable<Event> events = Arrays.asList(
                mockEvent("Healthy", imageUrl, /* missing */ false, /* notificationSent */ false),
                mockEvent("Missing", imageUrl, /* missing */ true, /* notificationSent */ false),
                mockEvent("Notified", imageUrl, /* missing */ true, /* notificationSent */ true),
                mockEvent("Notification only", imageUrl, /* missing */ false, /* notificationSent */ true));
        final SecurityService securityService = mock(SecurityService.class);
        for (final Event event : events) {
            mockOwnership(securityService, event, "owner");
        }
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(/* enabled */ true).checkMedia(events,
                securityService, new MediaUrlHealthChecker(), eventService);
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verify(securityService, never()).getOwnership(any());
        verify(eventService, never()).apply(any());
        for (final Event event : events) {
            final ImageDescriptor image = event.getImages().iterator().next();
            verify(image, never()).isMissing();
            verify(image, never()).isMissingMailNotificationSent();
        }
    }

    @Test
    public void testRateLimitedEventVideosDoNotChangeStateOrNotify() throws Exception {
        final URL videoUrl = getUrl(RATE_LIMITED_PATH);
        final Iterable<Event> events = Arrays.asList(
                mockEventWithVideo("Healthy", videoUrl, /* missing */ false, /* notificationSent */ false),
                mockEventWithVideo("Missing", videoUrl, /* missing */ true, /* notificationSent */ false),
                mockEventWithVideo("Notified", videoUrl, /* missing */ true, /* notificationSent */ true),
                mockEventWithVideo("Notification only", videoUrl, /* missing */ false, /* notificationSent */ true));
        final SecurityService securityService = mock(SecurityService.class);
        for (final Event event : events) {
            mockOwnership(securityService, event, "owner");
        }
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(/* enabled */ true).checkMedia(events,
                securityService, new MediaUrlHealthChecker(), eventService);
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verify(securityService, never()).getOwnership(any());
        verify(eventService, never()).apply(any());
        for (final Event event : events) {
            final VideoDescriptor video = event.getVideos().iterator().next();
            verify(video, never()).isMissing();
            verify(video, never()).isMissingMailNotificationSent();
        }
    }

    @Test
    public void testRateLimitedImageUrlIsCheckedOncePerRunAndRetriedNextRun() throws Exception {
        final URL imageUrl = getUrl(RATE_LIMITED_PATH);
        final Event firstEvent = mockEvent("First", imageUrl, /* missing */ false, /* notificationSent */ false);
        final Event secondEvent = mockEvent("Second", imageUrl, /* missing */ true, /* notificationSent */ true);
        final Iterable<Event> events = Arrays.asList(firstEvent, secondEvent);
        final SecurityService securityService = mock(SecurityService.class);
        final RacingEventService eventService = mock(RacingEventService.class);
        final TestImageUrlHealthChecker checker = new TestImageUrlHealthChecker(/* imageAvailable */ Optional.empty());
        final Activator activator = createActivatorWithOwnerNotificationEnabled(/* enabled */ true);
        activator.checkMedia(events, securityService, checker, eventService);
        assertEquals(/* expected */ 1, checker.getNumberOfChecks());
        activator.checkMedia(events, securityService, checker, eventService);
        assertEquals(/* expected */ 2, checker.getNumberOfChecks());
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verify(eventService, never()).apply(any());
    }

    @Test
    public void testRateLimitedVideoUrlIsCheckedOncePerRunAndRetriedNextRun() throws Exception {
        final URL videoUrl = getUrl(RATE_LIMITED_PATH);
        final Event firstEvent = mockEventWithVideo("First", videoUrl, /* missing */ false, /* notificationSent */ false);
        final Event secondEvent = mockEventWithVideo("Second", videoUrl, /* missing */ true, /* notificationSent */ true);
        final Iterable<Event> events = Arrays.asList(firstEvent, secondEvent);
        final SecurityService securityService = mock(SecurityService.class);
        final RacingEventService eventService = mock(RacingEventService.class);
        final TestVideoUrlHealthChecker checker = new TestVideoUrlHealthChecker(/* videoReachable */ Optional.empty());
        final Activator activator = createActivatorWithOwnerNotificationEnabled(/* enabled */ true);
        activator.checkMedia(events, securityService, checker, eventService);
        assertEquals(/* expected */ 1, checker.getNumberOfChecks());
        activator.checkMedia(events, securityService, checker, eventService);
        assertEquals(/* expected */ 2, checker.getNumberOfChecks());
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verify(eventService, never()).apply(any());
    }

    @Test
    public void testRateLimitedImageDoesNotPreventCheckingOtherMedia() throws Exception {
        final URL imageUrl = getUrl(RATE_LIMITED_PATH);
        final URL videoUrl = getUrl(HTTP_ERROR_PATH);
        final Event event = mockEvent("Event", imageUrl, /* missing */ true, /* notificationSent */ true);
        final VideoDescriptor video = mock(VideoDescriptor.class);
        when(video.getURL()).thenReturn(videoUrl);
        when(video.getTags()).thenReturn(Collections.emptyList());
        when(event.getVideos()).thenReturn(Collections.singleton(video));
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, event, "owner");
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(/* enabled */ true).checkMedia(Collections.singleton(event),
                securityService, new MediaUrlHealthChecker(), eventService);
        verify(eventService, never()).apply(any(UpdateEventImageHealth.class));
        verify(securityService, times(/* wantedNumberOfInvocations */ 1)).sendMail(eq("owner"), anyString(), anyString());
        verifyVideoHealthUpdate(eventService, event, videoUrl, /* missing */ true, /* notificationSent */ true);
    }

    @Test
    public void testBrokenEventImageNotifiesOwnerAndPersistsState() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, event, "owner");
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        verify(securityService, times(1)).sendMail(eq("owner"), anyString(), anyString());
        verifyImageHealthUpdate(eventService, event, imageUrl, /* missing */ true, /* notificationSent */ true);
    }

    @Test
    public void testAlreadyMissingAndNotifiedImageDoesNotNotifyAgain() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ true, /* notificationSent */ true);
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, event, "owner");
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verify(eventService, never()).apply(any(UpdateEventImageHealth.class));
    }

    @Test
    public void testMissingImageWithoutNotificationRetriesNotification() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ true, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, event, "owner");
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        verify(securityService, times(1)).sendMail(eq("owner"), anyString(), anyString());
        verifyImageHealthUpdate(eventService, event, imageUrl, /* missing */ true, /* notificationSent */ true);
    }

    @Test
    public void testRecoveredEventImageClearsPersistedState() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ true, /* notificationSent */ true);
        final SecurityService securityService = mock(SecurityService.class);
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(true);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verifyImageHealthUpdate(eventService, event, imageUrl, /* missing */ false, /* notificationSent */ false);
    }

    @Test
    public void testHealthyEventImageWithCleanStateIsNotUpdated() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(true);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        verify(eventService, never()).apply(any(UpdateEventImageHealth.class));
    }

    @Test
    public void testSameImageUrlIsCheckedOncePerRunButNotifiesBothOwners() throws Exception {
        final URL imageUrl = new URL("http://example.com/shared.jpg");
        final Event firstEvent = mockEvent("First Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final Event secondEvent = mockEvent("Second Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, firstEvent, "first-owner");
        mockOwnership(securityService, secondEvent, "second-owner");
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Arrays.asList(firstEvent, secondEvent),
                securityService, imageUrlHealthChecker, eventService);
        assertEquals(1, imageUrlHealthChecker.getNumberOfChecks());
        verify(securityService, times(1)).sendMail(eq("first-owner"), anyString(), anyString());
        verify(securityService, times(1)).sendMail(eq("second-owner"), anyString(), anyString());
        verify(eventService, times(2)).apply(any(UpdateEventImageHealth.class));
    }

    @Test
    public void testFailedMailLeavesNotificationUnsentForRetry() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, event, "owner");
        doThrow(new MailException("test failure")).when(securityService).sendMail(anyString(), anyString(), anyString());
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        verifyImageHealthUpdate(eventService, event, imageUrl, /* missing */ true, /* notificationSent */ false);
    }

    @Test
    public void testBrokenEventImageDoesNotNotifyOwnerWhenOwnerNotificationsAreDisabled() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, event, "owner");
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(false).checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verifyImageHealthUpdate(eventService, event, imageUrl, /* missing */ true, /* notificationSent */ false);
    }

    @Test
    public void testEventMediaAreNotCheckedWhenRacingEventServiceIsReplica() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        when(eventService.getMasterDescriptor()).thenReturn(mock(ReplicationMasterDescriptor.class));
        createActivator().checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        assertEquals(0, imageUrlHealthChecker.getNumberOfChecks());
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verify(eventService, never()).apply(any(UpdateEventImageHealth.class));
    }

    @Test
    public void testEventMediaAreCheckedWhenOnlySecurityServiceIsReplica() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        when(securityService.getMasterDescriptor()).thenReturn(mock(ReplicationMasterDescriptor.class));
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(true);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivator().checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        assertEquals(1, imageUrlHealthChecker.getNumberOfChecks());
        verify(eventService, never()).apply(any(UpdateEventImageHealth.class));
    }

    @Test
    public void testBrokenEventImageWithoutOwnerDoesNotSendMail() throws Exception {
        final URL imageUrl = new URL("http://example.com/image.jpg");
        final Event event = mockEvent("Event", imageUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        final TestImageUrlHealthChecker imageUrlHealthChecker = new TestImageUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivator().checkMedia(Collections.singleton(event),
                securityService, imageUrlHealthChecker, eventService);
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verifyImageHealthUpdate(eventService, event, imageUrl, /* missing */ true, /* notificationSent */ false);
    }

    @Test
    public void testValidVideoUrlIsReachable() throws Exception {
        assertEquals(Optional.of(/* value */ true), new MediaUrlHealthChecker().isUrlReachable(getUrl(VALID_VIDEO_PATH)));
    }

    @Test
    public void testHttpErrorVideoUrlIsNotReachable() throws Exception {
        assertEquals(Optional.of(/* value */ false), new MediaUrlHealthChecker().isUrlReachable(getUrl(HTTP_ERROR_PATH)));
    }
    
    @Test
    public void testYoutubeVideoReachable() throws Exception {
        final Optional<Boolean> reachable = new MediaUrlHealthChecker().isUrlReachable(new URL("https://youtu.be/5ohJe5rcBVM"));
        assumeTrue(reachable.isPresent(), "YouTube rate limiting prevented a reachability verdict");
        assertTrue(reachable.get());
    }

    @Test
    public void testBrokenEventVideoNotifiesOwnerAndPersistsState() throws Exception {
        final URL videoUrl = new URL("http://example.com/race.mp4");
        final Event event = mockEventWithVideo("Event", videoUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, event, "owner");
        final TestVideoUrlHealthChecker videoUrlHealthChecker = new TestVideoUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, videoUrlHealthChecker, eventService);
        verify(securityService, times(1)).sendMail(eq("owner"), anyString(), anyString());
        verifyVideoHealthUpdate(eventService, event, videoUrl, /* missing */ true, /* notificationSent */ true);
    }

    @Test
    public void testAlreadyMissingAndNotifiedVideoDoesNotNotifyAgain() throws Exception {
        final URL videoUrl = new URL("http://example.com/race.mp4");
        final Event event = mockEventWithVideo("Event", videoUrl, /* missing */ true, /* notificationSent */ true);
        final SecurityService securityService = mock(SecurityService.class);
        mockOwnership(securityService, event, "owner");
        final TestVideoUrlHealthChecker videoUrlHealthChecker = new TestVideoUrlHealthChecker(false);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, videoUrlHealthChecker, eventService);
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verify(eventService, never()).apply(any(UpdateEventVideoHealth.class));
    }

    @Test
    public void testRecoveredEventVideoClearsPersistedState() throws Exception {
        final URL videoUrl = new URL("http://example.com/race.mp4");
        final Event event = mockEventWithVideo("Event", videoUrl, /* missing */ true, /* notificationSent */ true);
        final SecurityService securityService = mock(SecurityService.class);
        final TestVideoUrlHealthChecker videoUrlHealthChecker = new TestVideoUrlHealthChecker(true);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, videoUrlHealthChecker, eventService);
        verify(securityService, never()).sendMail(anyString(), anyString(), anyString());
        verifyVideoHealthUpdate(eventService, event, videoUrl, /* missing */ false, /* notificationSent */ false);
    }

    @Test
    public void testHealthyEventVideoWithCleanStateIsNotUpdated() throws Exception {
        final URL videoUrl = new URL("http://example.com/race.mp4");
        final Event event = mockEventWithVideo("Event", videoUrl, /* missing */ false, /* notificationSent */ false);
        final SecurityService securityService = mock(SecurityService.class);
        final TestVideoUrlHealthChecker videoUrlHealthChecker = new TestVideoUrlHealthChecker(true);
        final RacingEventService eventService = mock(RacingEventService.class);
        createActivatorWithOwnerNotificationEnabled(true).checkMedia(Collections.singleton(event),
                securityService, videoUrlHealthChecker, eventService);
        verify(eventService, never()).apply(any(UpdateEventVideoHealth.class));
    }

    private Event mockEvent(String name, URL imageUrl, boolean missing, boolean notificationSent) {
        final Event event = mock(Event.class);
        final ImageDescriptor image = mock(ImageDescriptor.class);
        when(event.getId()).thenReturn(UUID.randomUUID());
        when(event.getIdentifier()).thenReturn(mock(QualifiedObjectIdentifier.class));
        when(event.getName()).thenReturn(name);
        when(event.getImages()).thenReturn(Collections.singleton(image));
        when(event.getVideos()).thenReturn(Collections.emptyList());
        when(image.getURL()).thenReturn(imageUrl);
        when(image.getTags()).thenReturn(Arrays.asList("Stage", "Teaser"));
        when(image.isMissing()).thenReturn(missing);
        when(image.isMissingMailNotificationSent()).thenReturn(notificationSent);
        return event;
    }

    private Event mockEventWithVideo(String name, URL videoUrl, boolean missing, boolean notificationSent) {
        final Event event = mock(Event.class);
        final VideoDescriptor video = mock(VideoDescriptor.class);
        when(event.getId()).thenReturn(UUID.randomUUID());
        when(event.getIdentifier()).thenReturn(mock(QualifiedObjectIdentifier.class));
        when(event.getName()).thenReturn(name);
        when(event.getImages()).thenReturn(Collections.emptyList());
        when(event.getVideos()).thenReturn(Collections.singleton(video));
        when(video.getURL()).thenReturn(videoUrl);
        when(video.getTags()).thenReturn(Arrays.asList("Stage"));
        when(video.isMissing()).thenReturn(missing);
        when(video.isMissingMailNotificationSent()).thenReturn(notificationSent);
        return event;
    }

    private void verifyImageHealthUpdate(RacingEventService eventService, Event event, URL imageUrl,
            boolean missing, boolean notificationSent) {
        final ArgumentCaptor<UpdateEventImageHealth> operationCaptor =
                ArgumentCaptor.forClass(UpdateEventImageHealth.class);
        verify(eventService, times(1)).apply(operationCaptor.capture());
        final RacingEventService target = mock(RacingEventService.class);
        operationCaptor.getValue().internalApplyTo(target);
        verify(target, times(1)).updateEventImageHealth(event.getId(), imageUrl.toString(), missing, notificationSent);
    }

    private void verifyVideoHealthUpdate(RacingEventService eventService, Event event, URL videoUrl, boolean missing, boolean notificationSent) {
        final ArgumentCaptor<UpdateEventVideoHealth> operationCaptor = ArgumentCaptor.forClass(UpdateEventVideoHealth.class);
        verify(eventService, times(1)).apply(operationCaptor.capture());
        final RacingEventService target = mock(RacingEventService.class);
        operationCaptor.getValue().internalApplyTo(target);
        verify(target, times(1)).updateEventVideoHealth(event.getId(), videoUrl.toString(), missing, notificationSent);
    }

    private void mockOwnership(SecurityService securityService, Event event, String username) {
        final OwnershipAnnotation ownership = mock(OwnershipAnnotation.class);
        final Ownership ownershipValue = mock(Ownership.class);
        final User owner = mock(User.class);
        when(securityService.getOwnership(event.getIdentifier())).thenReturn(ownership);
        when(ownership.getAnnotation()).thenReturn(ownershipValue);
        when(ownershipValue.getUserOwner()).thenReturn(owner);
        when(owner.getName()).thenReturn(username);
        when(owner.getLocaleOrDefault()).thenReturn(Locale.ROOT);
    }

    private URL getUrl(String path) throws Exception {
        return new URL("http://127.0.0.1:" + server.getPort() + path);
    }

    private byte[] createImageBytes() throws IOException {
        final BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", outputStream);
        return outputStream.toByteArray();
    }

    private static class TestImageUrlHealthChecker extends MediaUrlHealthChecker {
        private final Optional<Boolean> imageAvailable;
        private int numberOfChecks;

        private TestImageUrlHealthChecker(final boolean imageAvailable) {
            this(Optional.of(imageAvailable));
        }

        private TestImageUrlHealthChecker(final Optional<Boolean> imageAvailable) {
            this.imageAvailable = imageAvailable;
        }

        @Override
        Optional<Boolean> isImageAvailable(final URL imageUrl) {
            numberOfChecks++;
            return imageAvailable;
        }

        private int getNumberOfChecks() {
            return numberOfChecks;
        }
    }

    private static class TestVideoUrlHealthChecker extends MediaUrlHealthChecker {
        private final Optional<Boolean> videoReachable;
        private int numberOfChecks;

        private TestVideoUrlHealthChecker(final boolean videoReachable) {
            this(Optional.of(videoReachable));
        }

        private TestVideoUrlHealthChecker(final Optional<Boolean> videoReachable) {
            this.videoReachable = videoReachable;
        }

        @Override
        Optional<Boolean> isUrlReachable(final URL url) {
            numberOfChecks++;
            return videoReachable;
        }

        private int getNumberOfChecks() {
            return numberOfChecks;
        }
    }

    private class TestHttpServer implements Runnable {
        private final ServerSocket serverSocket;
        private final Thread serverThread;

        private TestHttpServer() throws IOException {
            serverSocket = new ServerSocket(0);
            serverThread = new Thread(this, MediaHealthCheckerTest.class.getSimpleName() + " HTTP server");
            serverThread.setDaemon(true);
        }

        private void start() {
            serverThread.start();
        }

        private int getPort() {
            return serverSocket.getLocalPort();
        }

        private void stop() throws InterruptedException, IOException {
            if (!serverSocket.isClosed()) {
                serverSocket.close();
            }
            serverThread.join();
        }

        @Override
        public void run() {
            while (!serverSocket.isClosed()) {
                try (Socket socket = serverSocket.accept()) {
                    handle(socket);
                } catch (SocketException e) {
                    if (!serverSocket.isClosed()) {
                        throw new RuntimeException(e);
                    }
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        private void handle(Socket socket) throws IOException {
            final BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII));
            final String requestLine = reader.readLine();
            if (requestLine == null) {
                return;
            }
            final String[] requestLineParts = requestLine.split(" ");
            final String path = requestLineParts[1];
            final OutputStream outputStream = socket.getOutputStream();
            switch (path) {
            case VALID_IMAGE_PATH:
                writeImage(outputStream);
                break;
            case VALID_VIDEO_PATH:
                writeResponse(outputStream, "200 OK", "Content-Type: video/mp4\r\n", new byte[0]);
                break;
            case REDIRECT_PATH:
                writeResponse(outputStream, "302 Found", "Location: " + VALID_IMAGE_PATH + "\r\n", new byte[0]);
                break;
            case RATE_LIMITED_PATH:
                writeResponse(outputStream, "429 Too Many Requests", "Content-Type: image/jpeg\r\nRetry-After: 60\r\n", imageBytes);
                break;
            case RATE_LIMITED_REDIRECT_PATH:
                writeResponse(outputStream, "302 Found", "Location: " + RATE_LIMITED_PATH + "\r\n", new byte[0]);
                break;
            case HTTP_ERROR_PATH:
                writeResponse(outputStream, "404 Not Found", "", new byte[0]);
                break;
            case HTML_PATH:
                writeResponse(outputStream, "200 OK", "Content-Type: text/html\r\n",
                        "<html><body>Not an image</body></html>".getBytes(StandardCharsets.US_ASCII));
                break;
            case SLOW_IMAGE_PATH:
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
                writeImage(outputStream);
                break;
            default:
                writeResponse(outputStream, "404 Not Found", "", new byte[0]);
                break;
            }
        }

        private void writeImage(OutputStream outputStream) throws IOException {
            writeResponse(outputStream, "200 OK", "Content-Type: image/jpeg\r\n", imageBytes);
        }

        private void writeResponse(OutputStream outputStream, String status, String headers, byte[] body)
                throws IOException {
            outputStream.write(("HTTP/1.1 " + status + "\r\n" + headers + "Content-Length: " + body.length
                    + "\r\nConnection: close\r\n\r\n").getBytes(StandardCharsets.US_ASCII));
            outputStream.write(body);
            outputStream.flush();
        }
    }

    private Activator createActivator() {
        return new Activator(ResourceBundleStringMessages.NULL) {
            @Override
            void scheduleMediaCheck(final Runnable checkTask, final Duration delay) {
                checkTask.run();
            }
        };
    }

    private Activator createActivatorWithOwnerNotificationEnabled(final boolean enabled) {
        final String propertyName = Activator.EVENT_MEDIA_OWNER_NOTIFICATION_ENABLED_PROPERTY_NAME;
        final String previousValue = System.getProperty(propertyName);
        System.setProperty(propertyName, Boolean.toString(enabled));
        try {
            return createActivator();
        } finally {
            if (previousValue == null) {
                System.clearProperty(propertyName);
            } else {
                System.setProperty(propertyName, previousValue);
            }
        }
    }
}
