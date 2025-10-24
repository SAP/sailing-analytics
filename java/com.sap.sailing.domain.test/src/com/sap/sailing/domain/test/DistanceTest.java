package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sse.common.Distance;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class DistanceTest {
    @Test
    public void testDistances() {
        final Distance laserHullLength = BoatClassMasterdata.LASER_INT.getHullLength();
        final Distance laserHullBeam = BoatClassMasterdata.LASER_INT.getHullBeam(); 
        final Position pos = new DegreePosition(35.29989440459763, 139.4847256889834);
        final Position x = new DegreePosition(35.299894404588855, 139.4847718595392);
        final Position y = new DegreePosition(35.29990690516795, 139.48472568898336);
        assertTrue(x.getDistance(pos).scale(-1).add(laserHullLength).getMeters() < 0.1);
        assertTrue(y.getDistance(pos).scale(-1).add(laserHullBeam).getMeters() < 0.1);
    }
    
    @Test
    public void testSmallDistanceAlongLongitude() {
        final Distance laserHullLength = BoatClassMasterdata.LASER_INT.getHullLength();
        final Position pos = new DegreePosition(35, 0);
        final double degreesToGoNorthForLaserHullLength = laserHullLength.getCentralAngleDeg();
        final Position nextPos = new DegreePosition(35, degreesToGoNorthForLaserHullLength);
        final Distance greatCircleDistanceBetweenPosAndNew = pos.getDistance(nextPos);
        final Position newPositionByRhumbTranslation = pos.translateRhumb(new DegreeBearingImpl(0), laserHullLength);
        final Position newPositionByGreatCircleTranslation = pos.translateGreatCircle(new DegreeBearingImpl(0), laserHullLength);
        // great-circle translation is very imprecise at small distances: 0.8m is almost 20% off given the laser hull length of 4.19m 
        assertEquals(laserHullLength.getMeters(), greatCircleDistanceBetweenPosAndNew.getMeters(), 0.8);
        assertEquals(pos.getLngDeg(), newPositionByRhumbTranslation.getLngDeg(), 0.00000001);
        assertEquals(pos.getLatDeg()+degreesToGoNorthForLaserHullLength, newPositionByRhumbTranslation.getLatDeg(), 0.00001);
        assertEquals(pos.getLngDeg(), newPositionByGreatCircleTranslation.getLngDeg(), 0.00000001);
        assertEquals(pos.getLatDeg()+degreesToGoNorthForLaserHullLength, newPositionByGreatCircleTranslation.getLatDeg(), 0.00000001);
    }
    
    @Test
    public void testCentralAngleDegForMeterDistance() {
        final Distance meterDistance = new MeterDistance(40007863);
        assertEquals(360., meterDistance.getCentralAngleDeg(), 0.01);
    }
}
