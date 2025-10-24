package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Locale;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.media.NatureOfClaim;
import com.sap.sse.common.media.TakedownNoticeRequestContext;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;
import com.sap.sse.util.TakedownNoticeFactory;

public class TestTakedownNoticeFactory {
    private static final String TEST_STRING_MESSAGES_BASE_NAME = "stringmessages/Test_StringMessages";
    private static ResourceBundleStringMessages TEST_STRING_MESSAGES;

    private ResourceBundleStringMessages getTestStringMessages() {
        if (TEST_STRING_MESSAGES == null) {
            TEST_STRING_MESSAGES = new ResourceBundleStringMessagesImpl(TEST_STRING_MESSAGES_BASE_NAME,
                    TestTakedownNoticeFactory.class.getClassLoader(), StandardCharsets.UTF_8.name());
        }
        return TEST_STRING_MESSAGES;
    }

    @Test
    public void testSimpleMessage() {
        final TakedownNoticeRequestContext context = new TakedownNoticeRequestContext("competitorImage", "THA 4152", "https://example.com/image.jpg",
                "https://localhost/some/test/url#with-a-fragment", NatureOfClaim.DEFAMATORY_CONTENT,
                "This is the user comment", Arrays.asList("https://example.com/explanation1.html", "https://example.com/explanation2.html"), "theUserName");
        final TakedownNoticeFactory factory = new TakedownNoticeFactory();
        assertEquals(
                "The image with URL https://example.com/image.jpg that it used as a competitor image for THA 4152 has been reported by user theUserName for reason: DEFAMATORY_CONTENT. The user sends the following additional explanation: \"This is the user comment\". Additional URLs to help clarify this request are: https://example.com/explanation1.html, https://example.com/explanation2.html.",
                factory.getLocalizedMessage(context, Locale.ENGLISH, getTestStringMessages()));
        assertEquals(
                "Die Entfernung des Bildes mit der URL https://example.com/image.jpg, welches als Teilnehmer-Bild für THA 4152 verwendet wird, wurde vom Benutzer theUserName aus folgendem Grund angefordert: DEFAMATORY_CONTENT. Der Benutzer gibt die folgende weitere Erklärung ab: \"This is the user comment\". Zusätzliche Verweise zur Klärung des Sachverhalts: https://example.com/explanation1.html, https://example.com/explanation2.html.",
                factory.getLocalizedMessage(context, Locale.GERMAN, getTestStringMessages()));
    }
}
