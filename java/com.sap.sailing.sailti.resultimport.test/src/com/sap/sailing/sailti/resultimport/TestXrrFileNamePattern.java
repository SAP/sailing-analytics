package com.sap.sailing.sailti.resultimport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.regex.Matcher;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.TimePoint;

public class TestXrrFileNamePattern {
    @Test
    public void testXrrFileNamePatternWithSimpleExample() throws ParseException, MalformedURLException {
        final SailtiEventResultsParserImpl parser = new SailtiEventResultsParserImpl(new URL("http://localhost"));
        final String sample = "<p>470 Men<br/><a href=\"/uploaded_files/XML-Pelicano_131_2360_20190406143343.xml\">";
        final Matcher matcher = SailtiEventResultsParserImpl.classAndXrrLinkPattern.matcher(sample);
        assertTrue(matcher.matches());
        assertEquals("470 Men", parser.getBoatClassName(matcher));
        final TimePoint expectedPublishedAtTimePoint = TimePoint.of(new SimpleDateFormat("yyyyMMddhhmmssX").parse("20190406143343Z"));
        assertEquals(expectedPublishedAtTimePoint, parser.getTimePoint(matcher));
    }
}
