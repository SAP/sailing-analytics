package com.sap.sailing.domain.tracking.impl;

import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Duration;

public class PositionInterpolationAndExtrapolationWithSpeedInfoTest extends PositionInterpolationAndExtrapolationTest<GPSFixMoving> {
    private SpeedWithBearing speed;

    @BeforeEach
    public void setUp() {
        super.setUp();
        setTrack(new DynamicGPSFixMovingTrackImpl<Object>(new Object(), /* millisecondsOverWhichToAverage */ 8000));
        speed = new KnotSpeedWithBearingImpl(6 /* knots */, Bearing.NORTH);
    }
    
    @Override
    protected DynamicGPSFixTrack<Object, GPSFixMoving> getTrack() {
        return (DynamicGPSFixTrack<Object, GPSFixMoving>) super.getTrack();
    }

    @Test
    public void testEmptyTrack() {
        assertNull(getTrack().getEstimatedPosition(now, /* extrapolate */ true));
        assertNull(getTrack().getEstimatedPosition(now, /* extrapolate */ false));
    }
    
    @Test
    public void testFixBeforeNow() {
        final GPSFixMoving fixBeforeNow = new GPSFixMovingImpl(p1, now.minus(Duration.ONE_HOUR), speed, /* optionalTrueHeading */ null);
        getTrack().add(fixBeforeNow);
        assertPos(p1, /* extrapolate */ false);
        assertPos(p1.translateGreatCircle(speed.getBearing(), speed.travel(now.minus(Duration.ONE_HOUR), now)), /* extrapolate */ true);
    }

    @Test
    public void testFixAfterNow() {
        GPSFixMoving fixAfterNow = new GPSFixMovingImpl(p1, now.plus(Duration.ONE_HOUR), speed, /* optionalTrueHeading */ null);
        getTrack().add(fixAfterNow);
        assertPos(p1, /* extrapolate */ false);
        assertPos(p1.translateGreatCircle(speed.getBearing().reverse() /* travel backwards */, speed.travel(now.minus(Duration.ONE_HOUR), now)), /* extrapolate */ true);
    }
    
    @Test
    public void testExactMatch() {
        GPSFixMoving fixNow = new GPSFixMovingImpl(p1, now, speed, /* optionalTrueHeading */ null);
        getTrack().add(fixNow);
        assertPos(p1, /* extrapolate */ false);
        assertPos(p1, /* extrapolate */ true);
    }

    @Test
    public void testInBetween() {
        GPSFixMoving fixBeforeNow = new GPSFixMovingImpl(p1, now.minus(Duration.ONE_HOUR), speed, /* optionalTrueHeading */ null);
        getTrack().add(fixBeforeNow);
        GPSFixMoving fixAfterNow = new GPSFixMovingImpl(p2, now.plus(Duration.ONE_HOUR), speed, /* optionalTrueHeading */ null);
        getTrack().add(fixAfterNow);
        Position middle = p1.translateGreatCircle(p1.getBearingGreatCircle(p2), p1.getDistance(p2).scale(0.5));
        assertPos(middle, /* extrapolate */ false);
        assertPos(middle, /* extrapolate */ true);
    }
}
