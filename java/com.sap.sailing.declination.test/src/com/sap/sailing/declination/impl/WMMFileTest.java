package com.sap.sailing.declination.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.util.GregorianCalendar;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreePosition;

public class WMMFileTest {
    private static Geomagnetism g;

    @BeforeAll
    public static void setUpForClass() throws IOException, ParseException {
        g = new Geomagnetism(new BufferedReader(new InputStreamReader(WMMFileTest.class.getClassLoader().getResourceAsStream("WMM2025.COF"))));
    }
    
    @Test
    public void testWMM2025Reading() throws IOException {
        assertNotNull(g);
    }

    @Test
    public void testWMM2025Content1() throws IOException, ParseException {
        final double lat = 44.123;
        final double lng = 8.234;
        final GregorianCalendar cal = new GregorianCalendar(2025, 10, 4, 0, 46, 9);
        assertWMMEqualToOneTenthOfADegreeToDeclinationService(g, lat, lng, cal);
    }

    @Test
    public void testWMM2025Content2() throws IOException, ParseException {
        final double lat = 49.234;
        final double lng = 9.777;
        final GregorianCalendar cal = new GregorianCalendar(2025, 2, 7, 10, 22, 17);
        assertWMMEqualToOneTenthOfADegreeToDeclinationService(g, lat, lng, cal);
    }

    @Test
    public void testWMM2025ContentSouthernHemisphere() throws IOException, ParseException {
        final double lat = -44.123;
        final double lng = -8.234;
        final GregorianCalendar cal = new GregorianCalendar(2026, 8, 1, 3, 26, 7);
        assertWMMEqualToOneTenthOfADegreeToDeclinationService(g, lat, lng, cal);
    }

    @Test
    public void testWMM2025ContentFarEastEquator() throws IOException, ParseException {
        final double lat = 0.000;
        final double lng = 170.444;
        final GregorianCalendar cal = new GregorianCalendar(2027, 1, 24, 18, 0, 0);
        assertWMMEqualToOneTenthOfADegreeToDeclinationService(g, lat, lng, cal);
    }

    private void assertWMMEqualToOneTenthOfADegreeToDeclinationService(final Geomagnetism g, final double lat,
            final double lng, final GregorianCalendar cal) throws IOException, ParseException {
        final double declinationWMM2025 = g.calculate(lng, lat, /* altitude */ 0, cal).getDeclination();
        final DeclinationService s = DeclinationService.INSTANCE;
        final Declination declinationFromService = s.getDeclination(TimePoint.of(cal.getTimeInMillis()), new DegreePosition(lat, lng), /* timeout */ 10000);
        assertEquals(declinationFromService.getBearing().getDegrees(), declinationWMM2025, /* delta in degrees */ 0.15, "Deviation of more than 0.15 degrees; that's too much!");
    }
}
