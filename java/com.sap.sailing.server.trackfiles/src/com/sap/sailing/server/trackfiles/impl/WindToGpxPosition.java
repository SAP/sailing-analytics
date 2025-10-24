package com.sap.sailing.server.trackfiles.impl;

import slash.common.type.CompactCalendar;
import slash.navigation.gpx.GpxPosition;

import com.sap.sailing.domain.common.Wind;

/**
 * Speed in m/s and bearing is in Degrees
 * 
 * @author D054536
 * 
 */
public enum WindToGpxPosition implements WaypointCreator<Wind> {
    INSTANCE;

    @Override
    public GpxPosition getPosition(Wind waypoint) {
        final GpxPosition result;
        if (waypoint == null || waypoint.getPosition() == null) {
            result = null;
        } else {
            GpxPosition p = new GpxPosition(waypoint.getPosition().getLngDeg(), // lon
                    waypoint.getPosition().getLatDeg(), // lat
                    0., // elevation
                    waypoint.getKilometersPerHour(), // speed
                    CompactCalendar.fromDate(waypoint.getTimePoint().asDate()), // time
                    "" /* comment */);
            p.setHeading(waypoint.getBearing().getDegrees());
            result = p;
        }
        return result;
    }
}