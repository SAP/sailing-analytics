package com.sap.sailing.declination.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;
import java.text.ParseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.declination.impl.DeclinationImporter;
import com.sap.sailing.declination.impl.DeclinationServiceImpl;
import com.sap.sailing.domain.common.impl.CentralAngleDistance;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class DeclinationServiceTest<I extends DeclinationImporter> extends AbstractDeclinationTest<I> {
    protected DeclinationService service;
    
    @BeforeEach
    public void setUp() {
        service = new DeclinationServiceImpl(new CentralAngleDistance(1./180.*Math.PI), importer);
    }
    
    @Test
    public void testSimpleDeclinationQueryMatchedInStore() throws IOException, ClassNotFoundException, ParseException {
        Declination result = service.getDeclination(new MillisecondsTimePoint(simpleDateFormat.parse("2011-02-03").getTime()),
                new DegreePosition(51, -5), /* timeoutForOnlineFetchInMilliseconds */ 3000);
        assertEquals(-3.-14./60., result.getBearing().getDegrees(), 0.0000001);
        assertEquals(0.+09./60., result.getAnnualChange().getDegrees(), 0.0000001);
    }

    @Test
    public void testDeclinationQueryNotMatchedInStore() throws IOException, ClassNotFoundException, ParseException {
        Declination result = service.getDeclination(new MillisecondsTimePoint(simpleDateFormat.parse("2020-02-03").getTime()),
                new DegreePosition(51, -5), /* timeoutForOnlineFetchInMilliseconds */ 5000);
        assertNotNull(result);
        assertEquals(-1.531, result.getBearing().getDegrees(), 0.001);
        assertEquals(0.20272, result.getAnnualChange().getDegrees(), 0.001);
    }

    @Test
    public void testDeclinationQueryNotMatchedInStoreWithTooShortTimeout() throws IOException, ClassNotFoundException, ParseException {
        Declination result = service.getDeclination(new MillisecondsTimePoint(simpleDateFormat.parse("2010-02-03").getTime()),
                new DegreePosition(51, -5), /* timeoutForOnlineFetchInMilliseconds */ 10);
        assertNull(result);
    }
}
