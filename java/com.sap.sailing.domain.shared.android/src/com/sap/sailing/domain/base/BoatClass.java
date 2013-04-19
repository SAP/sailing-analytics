package com.sap.sailing.domain.base;

import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Named;

public interface BoatClass extends Named, IsManagedBySharedDomainFactory {
    /**
     * The distance returned by this method should be appropriate for use in
     * {@link TrackedRace#approximate(Competitor, Distance, TimePoint, TimePoint)} so that penalty circles and other
     * maneuvers are detected reliably.
     */
    Distance getMaximumDistanceForCourseApproximation();
    
    long getApproximateManeuverDurationInMilliseconds();

    /**
     * If the averaged courses over ground differ by at least this degree angle, a maneuver will
     * be assumed. Note that this should be much less than the tack angle because averaging may
     * span across the actual maneuver.
     */
    double getManeuverDegreeAngleThreshold();

    double getMinimumAngleBetweenDifferentTacksDownwind();

    double getMinimumAngleBetweenDifferentTacksUpwind();
    
    /**
     * Most olympic boat classes start their race with an upwind leg. Some other classes such
     * as the Extreme Sailing Series / Extreme40 do not necessarily start with an upwind leg.
     * Knowing this is relevant for the wind estimation fallback strategy. If the first leg of
     * a boat class doesn't have to be an upwind leg it's not permissible to estimate the wind
     * based on the course layout.<p>
     * 
     * The result of calling this method suggests a good default for this boat class. It is
     * <em>not</em> an authoritative, prescriptive value. Races with this boat class may still
     * start with a non-upwind leg even though this method returns <code>true</code>.
     */
    boolean typicallyStartsUpwind();
    
    Distance getHullLength();

    /**
     * Downwind leg-based wind estimations are inherently less confident than upwind leg-based estimations because
     * jibing angles vary more greatly from boat to boat than tacking angles.
     * 
     * @param numberOfBoatsInSmallestCluster the larger the number of boats, the more confident the estimate is considered to be
     * ("wisdom of the crowds"). The minimum confidence for just one boat in the smallest cluster is still guaranteed to be
     * greater than zero.
     */
    double getDownwindWindEstimationConfidence(int numberOfBoatsInSmallestCluster);

    double getUpwindWindEstimationConfidence(int numberOfBoatsInSmallestCluster);
}
