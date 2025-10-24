package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.common.sensordata.BravoExtendedSensorDataMetadata;
import com.sap.sailing.domain.common.tracking.impl.BravoExtendedFixImpl;
import com.sap.sailing.domain.common.tracking.impl.DoubleVectorFixImpl;
import com.sap.sailing.domain.tracking.DynamicBravoFixTrack;
import com.sap.sailing.domain.tracking.impl.BravoFixTrackImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class BravoFixTrackInterpolationWithNullTest {
    @Test
    public void testFixesWithNonNullFieldsAreSelected() {
        DynamicBravoFixTrack<CourseArea> track = new BravoFixTrackImpl<>(new CourseAreaImpl("Test", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null),
                "test", /* hasExtendedFixes */ true);
        track.add(createFix(10l, 5., null));
        track.add(createFix(20l, null, 7.));
        track.add(createFix(30l, 7., null));
        assertEquals(6., track.getHeel(new MillisecondsTimePoint(20l)).getDegrees(), 0.00001);
    }

    @Test
    public void testFixesWithNonNullFieldsAreSelectedForMultipleNullsInARow() {
        DynamicBravoFixTrack<CourseArea> track = new BravoFixTrackImpl<>(new CourseAreaImpl("Test", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null),
                "test", /* hasExtendedFixes */ true);
        track.add(createFix(10l, 5., null));
        track.add(createFix(20l, null, 7.));
        track.add(createFix(30l, null, 7.));
        track.add(createFix(40l, null, 7.));
        track.add(createFix(50l, 7., null));
        assertEquals(6., track.getHeel(new MillisecondsTimePoint(30l)).getDegrees(), 0.00001);
        assertEquals(5., track.getHeel(new MillisecondsTimePoint(10l)).getDegrees(), 0.00001);
    }

    private BravoExtendedFixImpl createFix(long timePointAsMillis, Double heel, Double pitch) {
        final Double[] fixData = new Double[Math.max(BravoExtendedSensorDataMetadata.HEEL.getColumnIndex(),
                BravoExtendedSensorDataMetadata.PITCH.getColumnIndex())+1];
        fixData[BravoExtendedSensorDataMetadata.HEEL.getColumnIndex()] = heel;
        fixData[BravoExtendedSensorDataMetadata.PITCH.getColumnIndex()] = pitch;
        return new BravoExtendedFixImpl(new DoubleVectorFixImpl(new MillisecondsTimePoint(timePointAsMillis), fixData));
    }
}
