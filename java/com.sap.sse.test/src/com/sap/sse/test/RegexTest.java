package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

public class RegexTest {
    private static final Logger logger = Logger.getLogger(RegexTest.class.getName());
    private final Pattern trailingVersionPattern = Pattern.compile("^(.*) ([0-9]+)\\.([0-9]+)(\\.([0-9]+))?$");
    
    @Test
    public void test1_0() {
        final Matcher matcher = trailingVersionPattern.matcher("Image 1.0");
        assertTrue(matcher.matches());
        assertEquals(5, matcher.groupCount());
        assertEquals("Image", matcher.group(1));
        assertEquals("1", matcher.group(2));
        assertEquals("0", matcher.group(3));
        assertNull(matcher.group(4));
        assertNull(matcher.group(5));
    }
    
    @Test
    public void multiline() {
        final String x = "HTTP/1.1 302 Found\n"
                + "Date: Thu, 03 Dec 2020 21:55:45 GMT\n"
                + "Server: Apache/2.4.46 (Amazon) OpenSSL/1.0.2k-fips\n"
                + "Location: https://b.sapsailing.com/gwt/Home.html\n"
                + "Content-Length: 222\n"
                + "Content-Type: text/html; charset=iso-8859-1\n"
                + "\n"
                + "<!DOCTYPE HTML PUBLIC \"-//IETF//DTD HTML 2.0//EN\">\n"
                + "<html><head>\n"
                + "<title>302 Found</title>\n"
                + "</head><body>\n"
                + "<h1>Found</h1>\n"
                + "<p>The document has moved <a href=\"https://b.sapsailing.com/gwt/Home.html\">here</a>.</p>\n"
                + "</body></html>\n";
        assertTrue(x.matches("(?ms).*^Location: https://b.sapsailing.com/gwt/Home.html$.*"));
    }

    @Test
    public void test4_5_17() {
        final Matcher matcher = trailingVersionPattern.matcher("Image Bla 4.5.17");
        assertTrue(matcher.matches());
        assertEquals(5, matcher.groupCount());
        assertEquals("Image Bla", matcher.group(1));
        assertEquals("4", matcher.group(2));
        assertEquals("5", matcher.group(3));
        assertEquals(".17", matcher.group(4));
        assertEquals("17", matcher.group(5));
    }

    @Test
    public void testEscapingForEcho() {
        final String value = "abc\\'\"\\\"";
        logger.info("Unescaped value: "+value);
        final String escaped = "\""+value.replaceAll("\\\\", "\\\\\\\\").replaceAll("\"", "\\\\\"").replaceAll("'", "\\\\'")+"\"";
        logger.info("Escaped value: "+escaped);
        assertEquals("\"abc\\\\\\'\\\"\\\\\\\"\"", escaped);
    }

    @Test
    public void testNonExponentialExpeditionPattern() {
        final Pattern completeLinePattern = Pattern
                .compile("#([0-9]*)((,([0-9][0-9]*),(-?[0-9]*(\\.[0-9]*)?))*)\\*X?([0-9a-fA-F][0-9a-fA-F]*)");
        final Matcher m1 = completeLinePattern.matcher("#,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,X");
        assertFalse(m1.matches());
        final Matcher m2 = completeLinePattern.matcher("#,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,,0,*35");
        assertTrue(m2.matches());
    }
    
    @Test
    public void testPolynomialSailwaveRaceScorePattern() {
        final Pattern oneRaceScorePattern = Pattern.compile("^\\(?([0-9]+\\.[0-9]+)( ([A-Z][A-Z][A-Z]))?.*$");
        final Matcher m1 = oneRaceScorePattern.matcher("0.000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000000_ OCS");
        assertTrue(m1.matches());
    }
    
    @Test
    public void testPolynomialSailwaveNationalityPattern() {
        final Pattern nationalityPattern = Pattern.compile("^(<img .*\\btitle=\")?([A-Za-z][A-Za-z][A-Za-z])(\".*>)?$");
        final StringBuilder sb = new StringBuilder().append("AAA\" ");
        for (int i=0; i<1000; i++) {
            sb.append("btitle=\"aaa\" ");
        }
        final Matcher m1 = nationalityPattern.matcher(sb.toString());
        assertFalse(m1.matches());
    }
}
