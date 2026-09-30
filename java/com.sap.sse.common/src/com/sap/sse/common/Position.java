package com.sap.sse.common;

import java.io.Serializable;

public interface Position extends Serializable {
    double getLatRad();

    double getLngRad();

    double getLatDeg();

    double getLngDeg();
    
    /**
     * Generates a string representation of the form S03°45.235' E003°22.837'
     */
    String getAsDegreesAndDecimalMinutesWithCardinalPoints();
    
    /**
     * Generates a string representation of the form (-3.7539166666, 3.3806166666)
     */
    String getAsSignedDecimalDegrees();

    /**
     * Central angle between this position and the other position, in radians
     */
    double getCentralAngleRad(Position p);

    Distance getDistance(Position p);
    
    /**
     * The bearing from this position towards <code>p</code> on a great circle, using the shortest path.
     * 
     * @return {@code null} if {@code p} is {@code null}, otherwise the bearing from {@code this} {@link Position} to
     *         {@code p} along a great circle
     */
    Bearing getBearingGreatCircle(Position p);

    /**
     * Translates along a rhumb line, assuming travel at constant bearing. This
     * is therfore not translating along a great circle but as a straight line, e.g.,
     * on a Marcator projection.
     */
    Position translateRhumb(Bearing bearing, Distance distance);

    /**
     * Translates along a great circle, assuming travel at varying bearing where
     * <code>bearing</code> tells the bearing at this position. Invariant:
     * <code>translate(getBearingGreatCircle(p), getDistance(p)).equals(p)</code>
     */
    Position translateGreatCircle(Bearing bearing, Distance distance);

    /**
     * Projects this position onto the great circle through <code>p</code> with bearing <code>bearing</code>. Note that
     * if the angle between this position and the great circle is 90 degrees then there is no solution, and a
     * <code>NaN</code> or exception will result.
     */
    Position projectToLineThrough(Position p, Bearing bearing);

    /**
     * @return an unsigned distance
     */
    Distance absoluteCrossTrackError(Position p, Bearing bearing);
    
    /**
     * @return a signed distance; positive distances mean that this position is to the right (starboard) of the
     * line one gets when traveling from <code>p</code> with <code>bearing</code>
     */
    Distance crossTrackError(Position p, Bearing bearing);

    /**
     * Computes how far along the great circle starting at <code>from</code> and pointing to <code>bearing</code> one
     * has to travel to reach the projection of this position onto the great circle described by <code>from</code> and
     * <code>bearing</code>. Note that if the angle between this position and the great circle is 90 degrees then there
     * is no solution, and a <code>NaN</code> or exception will result.
     * <p>
     * 
     * The distance returned will be negative if one needs to travel towards the reverse of {@code bearing} to reach this
     * position starting at {@code from} on the shortest path.<p>
     * 
     * If either <code>from</code> or <code>bearing</code> or both are <code>null</code>, <code>null</code> is returned.
     */

    Distance alongTrackDistance(Position from, Bearing bearing);
    
    /**
     * Computes the distance from this position to the line between <code>left</code> and <code>right</code>. This distance
     * is positive if <code>left</code> is actually the position farther to the left, as seen from this position; negative
     * otherwise.
     */
    Distance getDistanceToLine(Position left, Position right);
    
    /**
     * Transform a position into its representation in a local geo-coordinate frame where <code>localOrigin</code> is
     * locally represented as <code>(latitude, longitude) = (0,0)</code> and <code>localEquatorBearing</code> is locally
     * represented as <code>East = 90 degrees</code>.
     * <p>
     * 
     * This can be used to move a GPS-track to the origin <code>(0,0)</code> and aligning wind direction with North, or
     * bearing of start line with East.
     */
    Position getLocalCoordinates(Position localOrigin, Bearing localEquatorBearing);
    
    /**
     * Transform a position from local geo-coordinate frame <code>(localOrigin, localEquatorBearing)</code>
     * into the corresponding position in target geo-coordinate frame <code>(targetOrigin, targetEquatorBearing)</code>.
     * The transformation is equivalent to a rotation of the earth, which moves <code>localOrigin</code>
     * to <code>targetOrigin</code> while aligning <code>localEquatorBearing</code> to <code>targetEquatorBearing</code>.<p>
     * 
     * This can be used to move a GPS-track from one location to another, in order to compare GPS-tracks from different areas
     * by superposing and aligning them based on wind direction or bearing of start line.
     */
    Position getTargetCoordinates(Position localOrigin, Bearing localEquatorBearing, Position targetOrigin, Bearing targetEquatorBearing);

    /**
     * Computes an approximate distance between this position and <code>p</code> by
     * using the average latitude's cosine to scale the longitudes and then calculate
     * the distance non-spherically in an imaginary Euklidian plane. The error is below
     * 1% even for several hundred nautical miles of randomly chosen distances, and even
     * lower (~.1%) for distances below ten nautical miles.<p>
     * 
     * The method executes efficiently and requires only one call to the cosine method.
     */
    double getQuickApproximateNauticalMileDistance(Position p);

    /**
     * Computes the intersection of two paths. The first path is defined by the underlying position and the parameter
     * <code>thisBearing</code>. The second path is defined by parameters <code>to</code> and <code>toBearing</code>
     * 
     * @param thisBearing defines first path with <code>this</code> position
     * @param to defines second path with <code>toBearing</code>
     * @param toBearing defines second path <code>to</code> position
     * @return position of intersection. Since there are usually 2 intersections, we return the smaller intersection.
     */
    Position getIntersection(Bearing thisBearing, Position to, Bearing toBearing);
    
    /**
     * Calculates the speed vector needed to reach the {@code to} position from this position in time
     * {@code inTime}. A {@link NullPointerException} will be thrown if either {@code to} or {@code inTime}
     * is {@code null}.
     */
    SpeedWithBearing getSpeedWithBearingToReachOnGreatCircle(Position to, Duration inTime);
}
