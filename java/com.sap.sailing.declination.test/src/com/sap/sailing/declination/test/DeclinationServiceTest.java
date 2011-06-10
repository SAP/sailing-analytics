package com.sap.sailing.declination.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.text.ParseException;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.declination.Declination;
import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.declination.impl.DeclinationServiceImpl;
import com.sap.sailing.domain.base.impl.CentralAngleDistance;
import com.sap.sailing.domain.base.impl.DegreePosition;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;

public class DeclinationServiceTest extends AbstractDeclinationTest {
    private DeclinationService service;
    
    @Before
    public void setUp() {
        service = new DeclinationServiceImpl(new CentralAngleDistance(1./180.*Math.PI));
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
        Declination result = service.getDeclination(new MillisecondsTimePoint(simpleDateFormat.parse("2010-02-03").getTime()),
                new DegreePosition(51, -5), /* timeoutForOnlineFetchInMilliseconds */ 3000);
        assertNotNull(result);
        assertEquals(-3.-27./60., result.getBearing().getDegrees(), 0.0000001);
        assertEquals(0.+09./60., result.getAnnualChange().getDegrees(), 0.0000001);
    }

    @Test
    public void testDeclinationQueryNotMatchedInStoreWithTooShortTimeout() throws IOException, ClassNotFoundException, ParseException {
        Declination result = service.getDeclination(new MillisecondsTimePoint(simpleDateFormat.parse("2010-02-03").getTime()),
                new DegreePosition(51, -5), /* timeoutForOnlineFetchInMilliseconds */ 10);
        assertNull(result);
    }
}
