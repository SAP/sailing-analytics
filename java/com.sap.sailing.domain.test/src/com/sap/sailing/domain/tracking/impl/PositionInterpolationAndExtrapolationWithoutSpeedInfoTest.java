package com.sap.sailing.domain.tracking.impl;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sse.common.Duration;

public class PositionInterpolationAndExtrapolationWithoutSpeedInfoTest extends PositionInterpolationAndExtrapolationTest<GPSFix> {
    @BeforeEach
    public void setUp() {
        super.setUp();
        setTrack(new DynamicGPSFixTrackImpl<>(new Object(), /* millisecondsOverWhichToAverage */ 8000));
    }
    
    @Override
    protected DynamicGPSFixTrackImpl<Object> getTrack() {
        return (DynamicGPSFixTrackImpl<Object>) super.getTrack();
    }
    
    @Test
    public void testEmptyTrack() {
        assertNull(getTrack().getEstimatedPosition(now, /* extrapolate */ true));
        assertNull(getTrack().getEstimatedPosition(now, /* extrapolate */ false));
    }
    
    @Test
    public void testFixBeforeNow() {
        GPSFix fixBeforeNow = new GPSFixImpl(p1, now.minus(Duration.ONE_HOUR));
        getTrack().add(fixBeforeNow);
        assertPos(p1, /* extrapolate */ false);
        assertPos(p1, /* extrapolate */ true);
    }

    @Test
    public void testFixAfterNow() {
        GPSFix fixAfterNow = new GPSFixImpl(p1, now.plus(Duration.ONE_HOUR));
        getTrack().add(fixAfterNow);
        assertPos(p1, /* extrapolate */ false);
        assertPos(p1, /* extrapolate */ true);
    }
    
    @Test
    public void testExactMatch() {
        GPSFix fixNow = new GPSFixImpl(p1, now);
        getTrack().add(fixNow);
        assertPos(p1, /* extrapolate */ false);
        assertPos(p1, /* extrapolate */ true);
    }
    
    @Test
    public void testInBetweenFallsBackToPreviousPosition() {
        GPSFix fixBeforeNow = new GPSFixImpl(p1, now.minus(Duration.ONE_HOUR));
        getTrack().add(fixBeforeNow);
        GPSFix fixAfterNow = new GPSFixImpl(p2, now.plus(Duration.ONE_HOUR));
        getTrack().add(fixAfterNow);
        assertPos(p1, /* extrapolate */ false);
        assertPos(p1, /* extrapolate */ true);
    }
}
