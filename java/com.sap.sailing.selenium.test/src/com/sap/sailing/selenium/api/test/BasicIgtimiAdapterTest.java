package com.sap.sailing.selenium.api.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.igtimiadapter.DataAccessWindow;
import com.sap.sailing.domain.igtimiadapter.Permission;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWA;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWS;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.Type;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class BasicIgtimiAdapterTest extends AbstractTestWithIgtimiConnection {
    @SeleniumTestCase
    public void testGetDataAccessWindows() throws ClientProtocolException, IllegalStateException, IOException, ParseException {
        Iterable<DataAccessWindow> daws = connection.getDataAccessWindows(Permission.read, /* startTime */ null,
                /* endTime */ null, /* deviceSerialNumbers */ Collections.singleton("DC-GD-AAED"));
        assertFalse(Util.isEmpty(daws));
        for (DataAccessWindow daw : daws) {
            assertEquals("DC-GD-AAED", daw.getDeviceSerialNumber());
            assertTrue(daw.getId() == 1);
        }
    }
    
    @SeleniumTestCase
    public void testGetAllDataAccessWindows() throws ClientProtocolException, IllegalStateException, IOException, ParseException {
        Iterable<DataAccessWindow> daws = connection.getDataAccessWindows(Permission.read, /* startTime */ null,
                /* endTime */ null, /* deviceSerialNumbers */ null);
        assertFalse(Util.isEmpty(daws));
    }
    
    @SeleniumTestCase
    public void testGetResourceData() throws ClientProtocolException, IllegalStateException, IOException, ParseException, java.text.ParseException {
        final Map<Type, Double> typesAndCompression = new HashMap<>();
        typesAndCompression.put(Type.gps_latlong, 0.0);
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.GERMAN);
        final TimePoint start = new MillisecondsTimePoint(dateFormat.parse("2025-01-07T07:00:00Z"));
        final TimePoint end   = new MillisecondsTimePoint(dateFormat.parse("2025-01-08T18:00:00Z"));
        final Iterable<Fix> data = connection.getResourceData(start, end,
                Collections.singleton("DC-GD-AAED"), typesAndCompression);
        assertTrue(data.iterator().hasNext());
    }
    
    @SeleniumTestCase
    public void testDataAccessWindowForGivenTimeFrame() throws java.text.ParseException, IllegalStateException, ClientProtocolException, IOException, ParseException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.GERMAN);
        TimePoint start = new MillisecondsTimePoint(dateFormat.parse("2025-01-07T07:00:00Z"));
        TimePoint end   = new MillisecondsTimePoint(dateFormat.parse("2025-01-08T18:00:00Z"));
        Iterable<DataAccessWindow> daws = connection.getDataAccessWindows(Permission.read, start, end, /* deviceSerialNumbers; get all devices available for that time */ null);
        assertFalse(Util.isEmpty(daws));
        for (DataAccessWindow daw : daws) {
            assertTrue((daw.getStartTime().compareTo(start)<=0 && daw.getEndTime().compareTo(start)>0)  // spans start
                    || (daw.getStartTime().compareTo(end)<=0 && daw.getEndTime().compareTo(end)>0)      // spans end
                    || (daw.getStartTime().compareTo(start)>=0 && daw.getEndTime().compareTo(end)<=0)); // lies within
        }
    }
    
    @SeleniumTestCase
    public void testResourceDataForGivenTimeFrame() throws java.text.ParseException, IllegalStateException, ClientProtocolException, IOException, ParseException {
        final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssX", Locale.GERMAN);
        final TimePoint start = new MillisecondsTimePoint(dateFormat.parse("2025-01-07T07:00:00Z"));
        final TimePoint end   = new MillisecondsTimePoint(dateFormat.parse("2025-01-08T18:00:00Z"));
        final Iterable<DataAccessWindow> daws = connection.getDataAccessWindows(Permission.read, start, end, /* deviceSerialNumbers; get all devices available for that time */ null);
        final Set<String> deviceSerialNumbers = new HashSet<>();
        for (final DataAccessWindow daw : daws) {
            deviceSerialNumbers.add(daw.getDeviceSerialNumber());
        }
        final Iterable<Fix> windData = connection.getResourceData(start, end, deviceSerialNumbers, Type.gps_latlong, Type.AWA, Type.AWS, Type.HDG);
        assertFalse(Util.isEmpty(windData));
        boolean foundWind = false;
        for (Fix fix : windData) {
            foundWind = foundWind || fix instanceof AWA || fix instanceof AWS;
            assertTrue(fix.getTimePoint().compareTo(start) >= 0 && fix.getTimePoint().compareTo(end) <= 0);
        }
        assertTrue(foundWind);
    }
    
    @SeleniumTestCase
    public void testReadLatestData() throws IllegalStateException, ClientProtocolException, IOException, ParseException {
        Iterable<Fix> fixes = connection.getLatestFixes(Arrays.asList(new String[] { "DC-GD-AAED", "GA-EN-AAEA", "DD-EE-AAGA" }), Type.SOG );
        assertEquals(1, Util.size(fixes));
        Iterator<Fix> i = fixes.iterator();
        Fix fix1 = i.next();
        assertEquals("DC-GD-AAED", fix1.getSensor().getDeviceSerialNumber());
        assertEquals(Type.SOG, fix1.getType());
    }
}
