package com.sap.sse.common;

import com.sap.sse.common.impl.CentralAngleDistance;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.RadianBearingImpl;
import com.sap.sse.common.impl.RadianPosition;
import com.sap.sse.common.util.RoundingUtil;

public class AbstractPosition implements Position {
    private static final long serialVersionUID = -3057027562787541064L;

    public int hashCode() {
        return (int) (4711. * getLngRad() * getLatRad());
    }

    public boolean equals(Object o) {
        if (o == null) {
            return false;
        } if (this == o) {
            return true;
        } else {
            return o instanceof Position && getLatRad() == ((Position) o).getLatRad()
                    && getLngRad() == ((Position) o).getLngRad();
        }
    }

    @Override
    public double getLngRad() {
        return getLngDeg() / 180. * Math.PI;
    }

    @Override
    public double getLatRad() {
        return getLatDeg() / 180. * Math.PI;
    }

    @Override
    public double getLatDeg() {
        return getLatRad() / Math.PI * 180.;
    }

    @Override
    public double getLngDeg() {
        return getLngRad() / Math.PI * 180.;
    }

    @Override
    public double getCentralAngleRad(Position p) {
        // Sinnott:
        double dLat = p.getLatRad() - getLatRad();
        double dLon = p.getLngRad() - getLngRad();
        double a = Math.sin(dLat / 2.) * Math.sin(dLat / 2.) + Math.cos(getLatRad()) * Math.cos(p.getLatRad())
                * Math.sin(dLon / 2.) * Math.sin(dLon / 2.);
        return 2. * Math.atan2(Math.sqrt(a), Math.sqrt(1. - a));
        // Spherical Law of Cosines; simpler formula, but doesn't work well for very small distances
        // return Math.acos(Math.sin(getLatRad()) * Math.sin(p.getLatRad())
        // + Math.cos(getLatRad()) * Math.cos(p.getLatRad())
        // * Math.cos(p.getLngRad() - getLngRad()));
    }

    @Override
    public Distance getDistance(Position p) {
        final Distance result;
        if (p == this || this.equals(p)) {
            result = Distance.NULL;
        } else {
            result = new CentralAngleDistance(getCentralAngleRad(p));
        }
        return result;
    }
    
    @Override
    public double getQuickApproximateNauticalMileDistance(Position p) {
        double latDeg = getLatDeg();
        double pLatDeg = p.getLatDeg();
        final double latDiffDeg = Math.abs(latDeg - pLatDeg);
        double cosineOfAverageLatitude = Math.cos((latDeg+pLatDeg)/2./180.*Math.PI);
        final double normalizedLngDiffDeg = cosineOfAverageLatitude * Math.abs(getLngDeg() - p.getLngDeg());
        // One degree of latitude or one degree of longitude at the equator each correspond to 60 nautical miles.
        return Math.sqrt(latDiffDeg*latDiffDeg + normalizedLngDiffDeg*normalizedLngDiffDeg) * 60.;
    }

    @Override
    public Bearing getBearingGreatCircle(Position p) {
        final Bearing bearing;
        if (p != null) {
            double result = Math.atan2(Math.sin(p.getLngRad() - getLngRad()) * Math.cos(p.getLatRad()),
                    Math.cos(getLatRad()) * Math.sin(p.getLatRad())
                            - Math.sin(getLatRad()) * Math.cos(p.getLatRad()) * Math.cos(p.getLngRad() - getLngRad()));
            if (result < 0) {
                result = result + 2 * Math.PI;
            }
            bearing = new RadianBearingImpl(result);
        } else {
            bearing = null;
        }
        return bearing;
    }

    @Override
    public Position translateRhumb(Bearing bearing, Distance distance) {
        /*
         * This algorithm is limited to distances such that dlon < pi/2, i.e those that extend around less than one
         * quarter of the circumference of the earth in longitude. A completely general, but more complicated algorithm
         * is necessary if greater distances are allowed.
         */
        double distanceRad = distance.getKilometers() / 6371.0; // r = 6371 means earth's radius in km
        double lat1 = getLatRad();
        double lon1 = getLngRad();
        double bearingRad = bearing.getRadians();
        double lat2 = Math.asin(Math.sin(lat1) * Math.cos(distanceRad) + Math.cos(lat1) * Math.sin(distanceRad)
                * Math.cos(bearingRad));
        double lon2 = lon1
                + Math.atan2(Math.sin(bearingRad) * Math.sin(distanceRad) * Math.cos(lat1), Math.cos(distanceRad)
                        - Math.sin(lat1) * Math.sin(lat2));
        lon2 = (lon2 + 3 * Math.PI) % (2 * Math.PI) - Math.PI; // normalize to -180..+180
        return new DegreePosition(lat2 / Math.PI * 180., lon2 / Math.PI * 180.);
    }

    @Override
    public Position translateGreatCircle(Bearing bearing, Distance distance) {
        double lat = Math.asin(Math.sin(getLatRad()) * Math.cos(distance.getCentralAngleRad()) + Math.cos(getLatRad())
                * Math.sin(distance.getCentralAngleRad()) * Math.cos(bearing.getRadians()));
        double lng = getLngRad()
                + Math.atan2(
                        Math.sin(bearing.getRadians()) * Math.sin(distance.getCentralAngleRad())
                                * Math.cos(getLatRad()),
                        Math.cos(distance.getCentralAngleRad()) - Math.sin(getLatRad()) * Math.sin(lat));
        return new RadianPosition(lat, lng);
    }

    @Override
    public Distance absoluteCrossTrackError(Position p, Bearing bearing) {
        return new CentralAngleDistance(Math.abs(crossTrackError(p, bearing).getCentralAngleRad()));
    }
    
    @Override
    public Distance crossTrackError(Position p, Bearing bearing) {
        return new CentralAngleDistance(Math.asin(Math.sin(p.getCentralAngleRad(this))
                * Math.sin(p.getBearingGreatCircle(this).getRadians() - bearing.getRadians())));
    }

    @Override
    public Position projectToLineThrough(Position pos, Bearing bearing) {
        return pos.translateGreatCircle(bearing, this.alongTrackDistance(pos, bearing));
    }

    @Override
    public Distance alongTrackDistance(Position from, Bearing bearing) {
        final Distance result;
        if (from != null && bearing != null) {
            double direction = Math.signum(Math.cos(from.getBearingGreatCircle(this).getRadians() - bearing.getRadians()));
            // Test if denominator gets ridiculously small; if so, the cross-track error is about 90° central angle.
            // This means that the cross-track error is maximized, and that there is no way to determine how far along
            // the great circle described by pos2 and bearing we should travel. This is an exception which will
            // surface as a division-by-zero exception or a NaN result
            result = new CentralAngleDistance(direction
                    * Math.acos(Math.cos(from.getCentralAngleRad(this))
                            / Math.cos(crossTrackError(from, bearing).getCentralAngleRad())));
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public Distance getDistanceToLine(Position left, Position right) {
        final Distance result;
        final Distance crossTrackError = this.crossTrackError(left, left.getBearingGreatCircle(right));
        final int factor = crossTrackError.getMeters()>0?1:-1;
        double toLeft = Math.abs(left.getBearingGreatCircle(this).getDifferenceTo(left.getBearingGreatCircle(right))
                .getDegrees());
        double toRight = Math.abs(right.getBearingGreatCircle(this).getDifferenceTo(right.getBearingGreatCircle(left))
                .getDegrees());
        if (toLeft > 90) {
            result = this.getDistance(left).scale(factor);
        } else if (toRight > 90) {
            result = this.getDistance(right).scale(factor);
        } else {
            result = crossTrackError;
        }
        return result;
    }

    @Override
    public Position getLocalCoordinates(Position localOrigin, Bearing localEquatorBearing) {
    	return this.getTargetCoordinates(localOrigin, localEquatorBearing, new DegreePosition(0.0, 0.0), new DegreeBearingImpl(90.0));
    }

    @Override
    public Position getTargetCoordinates(Position localOrigin, Bearing localEquatorBearing, Position targetOrigin, Bearing targetEquatorBearing) {
    	Bearing localBearing = localEquatorBearing.getDifferenceTo(localOrigin.getBearingGreatCircle(this));
    	Distance localDistance = this.getDistance(localOrigin);
    	return targetOrigin.translateGreatCircle(targetEquatorBearing.add(localBearing), localDistance);
    }
  
    @Override
    public String toString() {
        return "(" + getLatDeg() + "," + getLngDeg() + ")";
    }

    @Override
    public Position getIntersection(Bearing thisBearing, Position to, Bearing toBearing) {
        /*
         * See http://www.movable-type.co.uk/scripts/latlong-vectors.html#intersection for explanation
         */
        double radBearing1 = thisBearing.getRadians();
        double radLatPos1 = getLatRad();
        double radLngPos1 = getLngRad();
        double[] greatCircle1 = createGreatCircleVector(radBearing1, radLatPos1, radLngPos1);
        double radBearing2 = toBearing.getRadians();
        double radLatPos2 = to.getLatRad();
        double radLngPos2 = to.getLngRad();
        double[] greatCircle2 = createGreatCircleVector(radBearing2, radLatPos2, radLngPos2);
        double[] intersection1 = computeCrossProductOf3PartVectors(greatCircle1, greatCircle2);
        double[] intersection2 = computeCrossProductOf3PartVectors(greatCircle2, greatCircle1);
        Position intersectionPosition1 = cartesianVectorToPosition(intersection1);
        Position intersectionPosition2 = cartesianVectorToPosition(intersection2);
        Distance sumOfDistances1 = getDistance(intersectionPosition1).add(intersectionPosition1.getDistance(to));
        Distance sumOfDistances2 = getDistance(intersectionPosition2).add(intersectionPosition2.getDistance(to));
        return sumOfDistances1.compareTo(sumOfDistances2) < 0 ? intersectionPosition1 : intersectionPosition2;
    }

    @Override
    public SpeedWithBearing getSpeedWithBearingToReachOnGreatCircle(Position to, Duration inTime) {
        final Bearing bearing = getBearingGreatCircle(to);
        final Distance distance = getDistance(to);
        final Speed speed = distance.inTime(inTime);
        return new KnotSpeedWithBearingImpl(speed.getKnots(), bearing);
    }

    private Position cartesianVectorToPosition(double[] vector) {
        double lat = Math.atan2(vector[2], Math.sqrt(vector[0]*vector[0] + vector[1]*vector[1]));
        double lng = Math.atan2(vector[1], vector[0]);
        return new RadianPosition(lat, lng);
    }

    private double[] computeCrossProductOf3PartVectors(double[] vec1, double[] vec2) {
        double[] crossProduct= new double[3];
        crossProduct[0] = vec1[1]*vec2[2] - vec1[2]*vec2[1];
        crossProduct[1] = vec1[2]*vec2[0] - vec1[0]*vec2[2];
        crossProduct[2] = vec1[0]*vec2[1] - vec1[1]*vec2[0];
        return crossProduct;
    }

    private double[] createGreatCircleVector(double radBearing, double radLatPos, double radLngPos) {
        double[] greatCircle = new double[3];
        greatCircle[0] = Math.sin(radLngPos) * Math.cos(radBearing) - Math.sin(radLatPos) * Math.cos(radLngPos) * Math.sin(radBearing);
        greatCircle[1] = -Math.cos(radLngPos) * Math.cos(radBearing) - Math.sin(radLatPos) * Math.sin(radLngPos) * Math.sin(radBearing);
        greatCircle[2] = Math.cos(radLatPos) * Math.sin(radBearing);
        return greatCircle;
    }

    @Override
    public String getAsDegreesAndDecimalMinutesWithCardinalPoints() {
        final String lat = (getLatDeg()>=0 ? "N" : "S") + getDegreesAndDecimalMinutesOfNonNegativeAngle(Math.abs(getLatDeg()), /* degreePlaces */ 2, /* minuteDecimals */ 3);
        final String lng = (getLngDeg()>=0 ? "E" : "W") + getDegreesAndDecimalMinutesOfNonNegativeAngle(Math.abs(getLngDeg()), /* degreePlaces */ 3, /* minuteDecimals */ 3);
        return lat+" "+lng;
    }
    
    private String getDegreesAndDecimalMinutesOfNonNegativeAngle(double nonNegativeAngle, int degreePlaces, int minuteDecimals) {
        final double abs = Math.abs(nonNegativeAngle);
        int integerDegrees = (int) nonNegativeAngle;
        double minutes = RoundingUtil.format((abs-(int) abs)*60.0, 3);
        if (minutes >= 60.0) {
            minutes -= 60.0;
            integerDegrees++;
        }
        return Util.padPositiveValue(integerDegrees, degreePlaces, 0, /* round */ true) + "°"+
                Util.padPositiveValue(minutes, 2, minuteDecimals, /* round */ true)+"'";
    }
    
    @Override
    public String getAsSignedDecimalDegrees() {
        return "("+getLatDeg()+", "+getLngDeg()+")";
    }
}
