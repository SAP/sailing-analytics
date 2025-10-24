package com.sap.sailing.selenium.test.home;

import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.xml.bind.DatatypeConverter;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO;
import com.sap.sailing.selenium.pages.adminconsole.event.EventConfigurationPanelPO.EventEntryPO;
import com.sap.sailing.selenium.pages.adminconsole.event.EventUpdateDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.event.UploadVideosDialogPO;
import com.sap.sailing.selenium.pages.adminconsole.event.VideosTabPO;
import com.sap.sailing.selenium.pages.home.event.EventPage;
import com.sap.sailing.selenium.pages.home.event.MediaPO;
import com.sap.sailing.selenium.pages.home.event.MediaUploadDialogPO;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class MediaUploadTest extends AbstractSeleniumTest {

    private static final String EVENT = "Kieler Woche 2015"; //$NON-NLS-1$
    private static final String BOAT_CLASS_49ER = "49er"; //$NON-NLS-1$
    private static final String REGATTA_49ER = "KW 2015 Olympic - 49er"; //$NON-NLS-1$
    private static final String REGATTA_49ER_WITH_SUFFIX = REGATTA_49ER + " (" + BOAT_CLASS_49ER + ")"; //$NON-NLS-1$
    private static final String EVENT_DESC = "Kieler Woche 2015"; //$NON-NLS-1$
    private static final String VENUE = "Kieler F�rde"; //$NON-NLS-1$
    private static final Date EVENT_START_TIME = DatatypeConverter.parseDateTime("2015-06-20T08:00:00-00:00").getTime();
    private static final Date EVENT_END_TIME = DatatypeConverter.parseDateTime("2015-06-28T20:00:00-00:00").getTime();
    private static final String YOUTUBE_URL_1 = "https://youtu.be/tJFuTKPH_i8";
    private static final String YOUTUBE_URL_2 = "https://www.youtube.com/watch?v=tJFuTKPH_i8";
    private static final String VIMEO_URL = "https://vimeo.com/91732048";
    private static final String UNKNOWN_URL = "https://exmaple.video.com/download/video.pdf";
    private static final String MP4_URL = "https://exmaple.video.com/download/video.mp4";
    private static final String MOV_URL = "https://exmaple.video.com/download/video.mov";

    @Override
    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();

    }

    @SeleniumTestCase
    public void testHomepageMedia() throws UnsupportedEncodingException {
        final AdminConsolePage adminConsole = AdminConsolePage.goToPage(getWebDriver(), getContextRoot());
        final EventConfigurationPanelPO events = adminConsole.goToEvents();
        events.createEventWithDefaultLeaderboardGroupRegattaAndDefaultLeaderboard(EVENT, EVENT_DESC, VENUE,
                EVENT_START_TIME, EVENT_END_TIME, true, REGATTA_49ER_WITH_SUFFIX, BOAT_CLASS_49ER, EVENT_START_TIME,
                EVENT_END_TIME, false);
        EventEntryPO entryPO = events.getEventEntry(EVENT);
        String eventUrl = entryPO.getEventURL();
        EventUpdateDialogPO eventUpdateDialogPO = new EventUpdateDialogPO(getWebDriver(),
                entryPO.clickActionImage("UPDATE", "eventDialog"));
        VideosTabPO videosTabPO = eventUpdateDialogPO.goToVideosTab();
        UploadVideosDialogPO uploadVideosDialogPO = videosTabPO.clickAddVideoBtn();
        // check manual entry of external URL. This is not testing the video upload.
        uploadVideosDialogPO.enterUrl(UNKNOWN_URL);
        uploadVideosDialogPO.getMimeTypeString("unknown");
        uploadVideosDialogPO.enterUrl(MP4_URL);
        uploadVideosDialogPO.getMimeTypeString("mp4");
        uploadVideosDialogPO.enterUrl(MOV_URL);
        uploadVideosDialogPO.getMimeTypeString("mov");
        uploadVideosDialogPO.enterUrl(VIMEO_URL);
        uploadVideosDialogPO.getMimeTypeString("vimeo");
        uploadVideosDialogPO.enterUrl(YOUTUBE_URL_1);
        uploadVideosDialogPO.getMimeTypeString("youtube");
        uploadVideosDialogPO.enterUrl(YOUTUBE_URL_2);
        uploadVideosDialogPO.getMimeTypeString("youtube");
        uploadVideosDialogPO.pressOk();
        eventUpdateDialogPO.clickOkButtonOrThrow();
        EventPage eventPage = EventPage.goToEventUrl(getWebDriver(), eventUrl);
        eventPage.checkWhatsNewDialog();
        MediaPO mediaPO = eventPage.selectMedia();
        MediaUploadDialogPO mediaUploadDialogPO = mediaPO.clickAddMediaButton();
        mediaUploadDialogPO.enterUrl(YOUTUBE_URL_1);
        mediaUploadDialogPO.getMimeTypeString(0, "youtube");
        mediaUploadDialogPO.enterUrl(YOUTUBE_URL_2);
        mediaUploadDialogPO.getMimeTypeString(0, "youtube");
        mediaUploadDialogPO.enterUrl(VIMEO_URL);
        mediaUploadDialogPO.getMimeTypeString(0, "vimeo");
        mediaUploadDialogPO.enterUrl(MOV_URL);
        mediaUploadDialogPO.getMimeTypeString(0, "mov");
        mediaUploadDialogPO.enterUrl(MP4_URL);
        mediaUploadDialogPO.getMimeTypeString(0, "mp4");
        mediaUploadDialogPO.enterUrl(UNKNOWN_URL);
        mediaUploadDialogPO.getMimeTypeString(0, "unknown");
    }
}
