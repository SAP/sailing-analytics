package com.sap.sse.util.apachelog;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.text.ParseException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.junit.jupiter.api.Test;

public class ApacheLogAnalysisTest {
    @Test
    public void testParseSingleLogLine() throws ParseException {
        final LogEntry entry = new LogEntry("ess2017.sapsailing.com 91.7.27.233 - - [15/Dec/2017:16:49:41 +0000] \"POST /gwt/service/sailing HTTP/1.1\" 200 437 \"https://ess2017.sapsailing.com/gwt/RaceBoard.html?regattaName=ESS+2017+Los+Cabos&raceName=Race+1&leaderboardName=ESS+2017+Los+Cabos&leaderboardGroupName=Extreme+Sailing+Series+2017&eventId=6eb987e5-1334-45f9-b54a-8b772a3fbb5b&mode=PLAYER\" \"Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36\"");
        assertEquals("ess2017.sapsailing.com", entry.getHostname());
        assertEquals("91.7.27.233", entry.getRequestorIpString());
        assertEquals("-", entry.getRemoteLogname());
        assertEquals("-", entry.getRemoteUser());
        assertEquals("15/Dec/2017:16:49:41 +0000", entry.getTimestampString());
        assertEquals("15/Dec/2017", entry.getDateString());
        final Calendar cal = new GregorianCalendar();
        cal.setTimeZone(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.DAY_OF_MONTH, 15);
        cal.set(Calendar.MONTH, 11);
        cal.set(Calendar.YEAR, 2017);
        cal.set(Calendar.HOUR_OF_DAY, 16);
        cal.set(Calendar.MINUTE, 49);
        cal.set(Calendar.SECOND, 41);
        cal.set(Calendar.MILLISECOND, 0);
        assertEquals(cal.getTime(), entry.getTimepoint().asDate());
        assertEquals("POST /gwt/service/sailing HTTP/1.1", entry.getRequest());
        assertEquals(200, entry.getHttpStatusCode());
        assertEquals(437, entry.getSize());
        assertEquals("https://ess2017.sapsailing.com/gwt/RaceBoard.html?regattaName=ESS+2017+Los+Cabos&raceName=Race+1&leaderboardName=ESS+2017+Los+Cabos&leaderboardGroupName=Extreme+Sailing+Series+2017&eventId=6eb987e5-1334-45f9-b54a-8b772a3fbb5b&mode=PLAYER", entry.getReferrer());
        assertEquals("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.94 Safari/537.36", entry.getUserAgent());
    }
    
    @Test
    public void testPadding() {
        assertEquals("00", String.format("%02d", 0));
        assertEquals("01", String.format("%02d", 1));
        assertEquals("12", String.format("%02d", 12));
    }
}
