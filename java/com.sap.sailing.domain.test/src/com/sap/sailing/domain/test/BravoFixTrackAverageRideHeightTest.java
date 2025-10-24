package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.common.sensordata.BravoExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.impl.BravoExtendedFixImpl;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.tracking.DynamicBravoFixTrack;
import com.sap.sailing.domain.tracking.impl.BravoFixTrackImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class BravoFixTrackAverageRideHeightTest {
    private DynamicBravoFixTrack<CourseArea> track;
    
    @BeforeEach
    public void setUp() {
        track = new BravoFixTrackImpl<>(new CourseAreaImpl("Test", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null),
                "test", /* hasExtendedFixes */ true);
        track.add(createFix(10l, /* rideHeightPort */ 0.6, /* rideHeightStarboard */ 0.6, /* heel */ 10., /* pitch */ 5.));
        track.add(createFix(20l, /* rideHeightPort */ 0.6, /* rideHeightStarboard */ 0.6, /* heel */ 10., /* pitch */ 5.));
        track.add(createFix(30l, /* rideHeightPort */ 0.6, /* rideHeightStarboard */ 0.6, /* heel */ 10., /* pitch */ 5.));
    }
    
    @Test
    public void testAverageRideHeightCachingForEmptyInterval() {
        assertNull(track.getAverageRideHeight(t(11l), t(19l)));
    }
    
    @Test
    public void testAverageRideHeightCachingForNonEmptyInterval() {
        assertEquals(0.6, track.getAverageRideHeight(t(11l), t(21l)).getMeters(), 0.00001); // make sure that the cached "null" result doesn't offend surrounding calculations
        assertEquals(0.6, track.getAverageRideHeight(t(9l), t(21l)).getMeters(), 0.00001); // also when the first fix is included
        assertEquals(0.6, track.getAverageRideHeight(t(9l), t(31l)).getMeters(), 0.00001); // ensure that results are still correct when cache intervals are "added"
    }

    @Test
    public void testAverageRideHeightCachingForNonEmptyIntervalAfterHavingCachedEmptyIntervalResult() {
        testAverageRideHeightCachingForEmptyInterval();
        assertEquals(0.6, track.getAverageRideHeight(t(11l), t(21l)).getMeters(), 0.00001); // make sure that the cached "null" result doesn't offend surrounding calculations
        assertEquals(0.6, track.getAverageRideHeight(t(9l), t(21l)).getMeters(), 0.00001); // also when the first fix is included
        assertEquals(0.6, track.getAverageRideHeight(t(9l), t(31l)).getMeters(), 0.00001); // ensure that results are still correct when cache intervals are "added"
    }

    private BravoExtendedFixImpl createFix(long timePointAsMillis, Double rideHeightPort, Double rideHeightStarboard, Double heel, Double pitch) {
        final Double[] fixData = new Double[Collections.max(Arrays.asList(
                BravoExtendedSensorDataMetadata.HEEL.getColumnIndex()+1,
                BravoExtendedSensorDataMetadata.PITCH.getColumnIndex()+1,
                BravoExtendedSensorDataMetadata.RIDE_HEIGHT_PORT_HULL.getColumnIndex()+1,
                BravoExtendedSensorDataMetadata.RIDE_HEIGHT_STBD_HULL.getColumnIndex()+1))];
        fixData[BravoExtendedSensorDataMetadata.HEEL.getColumnIndex()] = heel;
        fixData[BravoExtendedSensorDataMetadata.PITCH.getColumnIndex()] = pitch;
        fixData[BravoExtendedSensorDataMetadata.RIDE_HEIGHT_PORT_HULL.getColumnIndex()] = rideHeightPort;
        fixData[BravoExtendedSensorDataMetadata.RIDE_HEIGHT_STBD_HULL.getColumnIndex()] = rideHeightStarboard;
        return new BravoExtendedFixImpl(new DoubleVectorFixImpl(t(timePointAsMillis), fixData));
    }

    private MillisecondsTimePoint t(long timePointAsMillis) {
        return new MillisecondsTimePoint(timePointAsMillis);
    }
}
