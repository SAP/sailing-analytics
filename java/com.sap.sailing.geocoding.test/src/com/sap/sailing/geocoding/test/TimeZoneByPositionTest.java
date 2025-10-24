package com.sap.sailing.geocoding.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.geocoding.impl.ReverseGeocoderImpl;
import com.sap.sse.common.TimePoint;

public class TimeZoneByPositionTest {
    @Test
    public void testSimpleTimeZoneQueryWithDST() throws MalformedURLException, IOException, ParseException, java.text.ParseException {
        final Date aDayInJune = new SimpleDateFormat("yyyy-MM-dd").parse("2023-06-10");
        final TimeZone tz = new ReverseGeocoderImpl().getTimeZone(new DegreePosition(49, 9),
                TimePoint.of(aDayInJune));
        assertNotNull(tz);
        assertEquals("Europe/Berlin", tz.getID());
        assertEquals(2, tz.getOffset(aDayInJune.getTime())/3600/1000);
    }
    
    @Test
    public void testSimpleTimeZoneQueryNoDST() throws MalformedURLException, IOException, ParseException, java.text.ParseException {
        final Date aDayInJanuary = new SimpleDateFormat("yyyy-MM-dd").parse("2023-01-10");
        final TimeZone tz = new ReverseGeocoderImpl().getTimeZone(new DegreePosition(52.10791896401003, 4.255260790428081),
                TimePoint.of(aDayInJanuary));
        assertNotNull(tz);
        assertEquals("Europe/Amsterdam", tz.getID());
        assertEquals(1, tz.getOffset(aDayInJanuary.getTime())/3600/1000);
    }
}
