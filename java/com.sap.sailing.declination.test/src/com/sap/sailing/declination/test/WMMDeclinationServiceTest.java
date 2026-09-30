package com.sap.sailing.declination.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.impl.WMMCalculatorDeclinationService;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class WMMDeclinationServiceTest {
    private static WMMCalculatorDeclinationService service;
    private final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    
    @BeforeAll
    public static void setUp() {
        service = new WMMCalculatorDeclinationService();
    }
    
    @Test
    public void testSimpleDeclinationQueryMatchedInStore() throws IOException, ClassNotFoundException, ParseException {
        Declination result = service.getDeclination(new MillisecondsTimePoint(simpleDateFormat.parse("2011-02-03").getTime()),
                new DegreePosition(51, -5), /* timeoutForOnlineFetchInMilliseconds */ 3000);
        assertEquals(-3.-14./60., result.getBearing().getDegrees(), 0.25);
    }

    @Test
    public void testDeclinationQueryNotMatchedInStore() throws IOException, ClassNotFoundException, ParseException {
        Declination result = service.getDeclination(new MillisecondsTimePoint(simpleDateFormat.parse("2020-02-03").getTime()),
                new DegreePosition(51, -5), /* timeoutForOnlineFetchInMilliseconds */ 5000);
        assertNotNull(result);
        assertEquals(-1.531, result.getBearing().getDegrees(), 0.1);
    }
}
