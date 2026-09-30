package com.sap.sailing.racecommittee.app.utils;

import com.sap.sse.common.Position;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sailing.racecommittee.app.R;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;

import android.content.Context;

public class GeoUtils {
    public static Position getPositionForGivenPointDistanceAndBearing(Position givenPoint, Distance distance,
            Bearing windDirection) {
        final double earthRadiusInMeters = 6371000.0;
        double brng = windDirection.getRadians();
        double lat1 = givenPoint.getLatRad();
        double lon1 = givenPoint.getLngRad();
        double dist = distance.getMeters() / earthRadiusInMeters;

        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(dist) + Math.cos(lat1) * Math.sin(dist) * Math.cos(brng));
        double lon2 = lon1 + Math.atan2(Math.sin(brng) * Math.sin(dist) * Math.cos(lat1),
                Math.cos(dist) - Math.sin(lat1) * Math.sin(lat2));
        lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI;

        return new DegreePosition(Math.toDegrees(lat2), Math.toDegrees(lon2));
    }

    public static String getInDMSFormat(Context context, double degree) {
        degree = java.lang.Math.abs(degree);
        int d = (int) degree;
        degree = (degree - d) * 60.0;
        int m = (int) degree;
        double s = (degree - m) * 60;
        if (degree < 0) { // put the sign back on the degrees
            d = -d;
        }
        return context.getString(R.string.geo_coords, d, m, s);
    }

}
