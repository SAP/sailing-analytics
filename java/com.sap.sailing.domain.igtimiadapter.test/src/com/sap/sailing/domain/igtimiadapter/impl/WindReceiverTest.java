package com.sap.sailing.domain.igtimiadapter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.sap.sailing.declination.DeclinationService;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.igtimiadapter.IgtimiWindListener;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWA;
import com.sap.sailing.domain.igtimiadapter.datatypes.AWS;
import com.sap.sailing.domain.igtimiadapter.datatypes.COG;
import com.sap.sailing.domain.igtimiadapter.datatypes.Fix;
import com.sap.sailing.domain.igtimiadapter.datatypes.GpsLatLong;
import com.sap.sailing.domain.igtimiadapter.datatypes.HDG;
import com.sap.sailing.domain.igtimiadapter.datatypes.SOG;
import com.sap.sailing.domain.igtimiadapter.shared.IgtimiWindReceiver;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

@Timeout(value = 2, unit=TimeUnit.MINUTES) // 2 minutes timeout for the test
public class WindReceiverTest {
    @Test
    public void simpleWindReceiverTest() {
        final List<Wind> windReceived = new ArrayList<>();
        final String deviceSerialNumber = "Non-Existing Test Device";
        IgtimiWindReceiver receiver = new IgtimiWindReceiver(DeclinationService.INSTANCE);
        receiver.addListener(new IgtimiWindListener() {
            @Override
            public void windDataReceived(Wind wind, Set<Fix> fixesUsed, String deviceSerialNumber) {
                windReceived.add(wind);
            }
        });
        TimePoint timePoint = MillisecondsTimePoint.now();
        Map<Integer, Object> awaMap = new HashMap<>(); awaMap.put(1, 123. /* degrees from */);
        Map<Integer, Object> awsMap = new HashMap<>(); awsMap.put(1, 22.224 /* kmh */);
        Map<Integer, Object> hdgMap = new HashMap<>(); hdgMap.put(1, 20. /* true degrees */);
        Map<Integer, Object> gpsLatLongMap = new HashMap<>(); gpsLatLongMap.put(2, 49.8 /* lat */); gpsLatLongMap.put(1, 8.93 /* lng */);
        Map<Integer, Object> sogMap = new HashMap<>(); sogMap.put(1, 18.52 /* kmh */);
        Map<Integer, Object> cogMap = new HashMap<>(); cogMap.put(1, 22. /* degrees; 2 degrees drift */);
        final SensorImpl sensor = new SensorImpl(deviceSerialNumber, 0);
        SpeedWithBearing boatSogCog = new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(22));
        SpeedWithBearing apparentWind = new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(20).add(new DegreeBearingImpl(123).reverse()));
        SpeedWithBearing expectedTrueWind = apparentWind.add(boatSogCog);
        receiver.received(Arrays.asList(new AWA(timePoint, sensor, awaMap),
                                        new AWS(timePoint, sensor, awsMap),
                                        new HDG(timePoint, sensor, hdgMap),
                                        new GpsLatLong(timePoint, sensor, gpsLatLongMap),
                                        new SOG(timePoint, sensor, sogMap),
                                        new COG(timePoint, sensor, cogMap)));
        assertFalse(windReceived.isEmpty());
        assertEquals(1, windReceived.size());
        Wind wind = windReceived.get(0);
        assertEquals(timePoint, wind.getTimePoint());
        assertEquals(49.8, wind.getPosition().getLatDeg(), 0.00000001);
        assertEquals(8.93, wind.getPosition().getLngDeg(), 0.00000001);
        assertEquals(expectedTrueWind.getKnots(), wind.getKnots(), 0.00000001);
        assertEquals(expectedTrueWind.getBearing().getDegrees(), wind.getBearing().getDegrees(), 0.00000001);
    }

    @Test
    public void trueWindCalculationTest() {
        final List<Wind> windReceived = new ArrayList<>();
        final String deviceSerialNumber = "Non-Existing Test Device";
        IgtimiWindReceiver receiver = new IgtimiWindReceiver(DeclinationService.INSTANCE);
        receiver.addListener(new IgtimiWindListener() {
            @Override
            public void windDataReceived(Wind wind, Set<Fix> fixesUsed, String deviceSerialNumber) {
                windReceived.add(wind);
            }
        });
        TimePoint timePoint = MillisecondsTimePoint.now();
        Map<Integer, Object> awaMap = new HashMap<>(); awaMap.put(1, 208. /* degrees from */);
        Map<Integer, Object> awsMap = new HashMap<>(); awsMap.put(1, 30.869136 /* kmh */);
        Map<Integer, Object> hdgMap = new HashMap<>(); hdgMap.put(1, 177. /* true degrees */);
        Map<Integer, Object> gpsLatLongMap = new HashMap<>(); gpsLatLongMap.put(2, 23.6 /* lat */); gpsLatLongMap.put(1, 58.3 /* lng */);
        Map<Integer, Object> sogMap = new HashMap<>(); sogMap.put(1, 15.0915776 /* kmh */);
        Map<Integer, Object> cogMap = new HashMap<>(); cogMap.put(1, 354.3 /* degrees; 2 degrees drift */);
        final SensorImpl sensor = new SensorImpl(deviceSerialNumber, 0);
        receiver.received(Arrays.asList(new AWA(timePoint, sensor, awaMap),
                                        new AWS(timePoint, sensor, awsMap),
                                        new HDG(timePoint, sensor, hdgMap),
                                        new GpsLatLong(timePoint, sensor, gpsLatLongMap),
                                        new SOG(timePoint, sensor, sogMap),
                                        new COG(timePoint, sensor, cogMap)));
        assertFalse(windReceived.isEmpty());
        assertEquals(1, windReceived.size());
        Wind wind = windReceived.get(0);
        assertEquals(timePoint, wind.getTimePoint());
        SpeedWithBearing boatSogCog = new KnotSpeedWithBearingImpl(8.1488, new DegreeBearingImpl(354.3));
        SpeedWithBearing apparentWindTo = new KnotSpeedWithBearingImpl(16.668, new DegreeBearingImpl(177).add(new DegreeBearingImpl(208).reverse()));
        SpeedWithBearing expectedTrueWind = apparentWindTo.add(boatSogCog);
        assertEquals(expectedTrueWind.getKnots(), wind.getKnots(), 0.00000001);
        assertEquals(expectedTrueWind.getBearing().getDegrees(), wind.getBearing().getDegrees(), 0.00000001);
    }

    /**
     * See bug 1867; constructs two data sets for two different devices. The AWA, HDG, GPS, SOG and COG fixes have equal time
     * points for both devices. The AWS fixes have later time points. This is to re-produce the problem where the wind fix receiver
     * mixes up the fixes from the different devices when looking at the AWS fixes to construct the wind fix.
     */
    @Test
    public void simpleWindReceiverTestWithTwoDevices() {
        final Map<String, Wind> windReceived = new HashMap<>();
        final String deviceSerialNumber1 = "Non-Existing Test Device #1";
        final String deviceSerialNumber2 = "Non-Existing Test Device #2";
        IgtimiWindReceiver receiver = new IgtimiWindReceiver(DeclinationService.INSTANCE);
        receiver.addListener(new IgtimiWindListener() {
            @Override
            public void windDataReceived(Wind wind, Set<Fix> fixesUsed, String deviceSerialNumber) {
                windReceived.put(deviceSerialNumber, wind);
            }
        });
        TimePoint timePoint1 = MillisecondsTimePoint.now();
        TimePoint timePoint2 = timePoint1.plus(3000);
        TimePoint timePoint3 = timePoint2.plus(1000);
        Map<Integer, Object> awaMap1 = new HashMap<>(); awaMap1.put(1, 123. /* degrees from */);
        Map<Integer, Object> awsMap1 = new HashMap<>(); awsMap1.put(1, 22.224 /* kmh */);
        Map<Integer, Object> hdgMap1 = new HashMap<>(); hdgMap1.put(1, 20. /* true degrees */);
        Map<Integer, Object> gpsLatLongMap1 = new HashMap<>(); gpsLatLongMap1.put(2, 49.8 /* lat */); gpsLatLongMap1.put(1, 8.93 /* lng */);
        Map<Integer, Object> sogMap1 = new HashMap<>(); sogMap1.put(1, 18.52 /* kmh */);
        Map<Integer, Object> cogMap1 = new HashMap<>(); cogMap1.put(1, 22. /* degrees; 2 degrees drift */);
        final SensorImpl sensor1 = new SensorImpl(deviceSerialNumber1, 0);

        Map<Integer, Object> awaMap2 = new HashMap<>(); awaMap2.put(1, 234. /* degrees from */);
        Map<Integer, Object> awsMap2 = new HashMap<>(); awsMap2.put(1, 44.448 /* kmh */);
        Map<Integer, Object> hdgMap2 = new HashMap<>(); hdgMap2.put(1, 20. /* true degrees */);
        Map<Integer, Object> gpsLatLongMap2 = new HashMap<>(); gpsLatLongMap2.put(2, 49.9 /* lat */); gpsLatLongMap2.put(1, 8.87 /* lng */);
        Map<Integer, Object> sogMap2 = new HashMap<>(); sogMap2.put(1, 37.04 /* kmh */);
        Map<Integer, Object> cogMap2 = new HashMap<>(); cogMap2.put(1, 44. /* degrees; 2 degrees drift */);
        final SensorImpl sensor2 = new SensorImpl(deviceSerialNumber2, 0);

        receiver.received(Arrays.asList(new AWA(timePoint1, sensor1, awaMap1),
                                        new HDG(timePoint1, sensor1, hdgMap1),
                                        new GpsLatLong(timePoint1, sensor1, gpsLatLongMap1),
                                        new SOG(timePoint1, sensor1, sogMap1),
                                        new COG(timePoint1, sensor1, cogMap1),
                                        
                                        new AWA(timePoint1, sensor2, awaMap2),
                                        new HDG(timePoint1, sensor2, hdgMap2),
                                        new GpsLatLong(timePoint1, sensor2, gpsLatLongMap2),
                                        new SOG(timePoint1, sensor2, sogMap2),
                                        new COG(timePoint1, sensor2, cogMap2),

                                        new AWS(timePoint2, sensor1, awsMap1),
                                        new AWS(timePoint3, sensor2, awsMap2)));

        assertFalse(windReceived.isEmpty());
        assertEquals(2, windReceived.size());
        Wind wind1 = windReceived.get(deviceSerialNumber1);
        assertEquals(timePoint2, wind1.getTimePoint());
        assertEquals(49.8, wind1.getPosition().getLatDeg(), 0.00000001);
        assertEquals(8.93, wind1.getPosition().getLngDeg(), 0.00000001);
        SpeedWithBearing boatSogCog1 = new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(22));
        SpeedWithBearing apparentWind1 = new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(20).add(new DegreeBearingImpl(123).reverse()));
        SpeedWithBearing expectedTrueWind1 = apparentWind1.add(boatSogCog1);
        assertEquals(expectedTrueWind1.getKnots(), wind1.getKnots(), 0.00000001);
        assertEquals(expectedTrueWind1.getBearing().getDegrees(), wind1.getBearing().getDegrees(), 0.00000001);

        Wind wind2 = windReceived.get(deviceSerialNumber2);
        assertEquals(timePoint3, wind2.getTimePoint());
        assertEquals(49.9, wind2.getPosition().getLatDeg(), 0.00000001);
        assertEquals(8.87, wind2.getPosition().getLngDeg(), 0.00000001);
        SpeedWithBearing boatSogCog2 = new KnotSpeedWithBearingImpl(20, new DegreeBearingImpl(44));
        SpeedWithBearing apparentWind2 = new KnotSpeedWithBearingImpl(24, new DegreeBearingImpl(20).add(new DegreeBearingImpl(234).reverse()));
        SpeedWithBearing expectedTrueWind2 = apparentWind2.add(boatSogCog2);
        assertEquals(expectedTrueWind2.getKnots(), wind2.getKnots(), 0.00000001);
        assertEquals(expectedTrueWind2.getBearing().getDegrees(), wind2.getBearing().getDegrees(), 0.00000001);
    }
}
