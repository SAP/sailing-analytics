package com.sap.sailing.domain.base.impl;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.BoatClassMasterdata;
import com.sap.sailing.domain.common.BoatHullType;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sse.common.impl.NamedImpl;

public class BoatClassImpl extends NamedImpl implements BoatClass {
    private static final long serialVersionUID = 7194912853476256420L;

    /**
     * If the averaged courses over ground differ by at least this degree angle, a maneuver will
     * be assumed. Note that this should be much less than the tack angle because averaging may
     * span across the actual maneuver.
     */
    private static final double MANEUVER_DEGREE_ANGLE_THRESHOLD = /* minimumDegreeDifference */ 30.;

    private static final double MINIMUM_ANGLE_BETWEEN_DIFFERENT_TACKS_UPWIND = 60.;
    
    private static final double MINIMUM_ANGLE_BETWEEN_DIFFERENT_TACKS_DOWNWIND = 25.;

    private static final Distance MAXIMUM_DISTANCE_FOR_COURSE_APPROXIMATION = new MeterDistance(3);

    /**
     * Upwind course-based wind estimations are pretty confident for most boat classes.
     */
    private static final double UPWIND_WIND_ESTIMATION_CONFIDENCE = .9;

    /**
     * Downwind estimations are less confident than upwind because jibing angles vary more.
     */
    private static final double DOWNWIND_WIND_ESTIMATION_CONFIDENCE = .5;
    
    private final long approximateManeuverDurationInMilliseconds;

    private final boolean typicallyStartsUpwind;
    
    private final String displayName;

    private final Distance hullLength;
    
    private final Distance hullBeam;
    
    private final BoatHullType hullType;

    public BoatClassImpl(String name, boolean typicallyStartsUpwind) {
        this(name, typicallyStartsUpwind, /* displayName */ null,
                /* hull length */ new MeterDistance(5), /* hullBeam */ null, /* hullType */ null);
    }
    
    public BoatClassImpl(String name, BoatClassMasterdata masterData) {
        this(name, masterData.isTypicallyStartsUpwind(), masterData.getDisplayName(), masterData.getHullLength(),
                masterData.getHullBeam(), masterData.getHullType());
    }
    
    public BoatClassImpl(String name, boolean typicallyStartsUpwind, String displayName, 
            Distance hullLength, Distance hullBeam, BoatHullType hullType) {
        super(name);
        this.typicallyStartsUpwind = typicallyStartsUpwind;     
        this.displayName = displayName;
        this.hullLength = hullLength;
        this.hullBeam = hullBeam;
        this.hullType = hullType;
        approximateManeuverDurationInMilliseconds = APPROXIMATE_AVERAGE_MANEUVER_DURATION.asMillis();
    }    

    @Override
    public long getApproximateManeuverDurationInMilliseconds() {
        return approximateManeuverDurationInMilliseconds;
    }

    @Override
    public double getManeuverDegreeAngleThreshold() {
        return MANEUVER_DEGREE_ANGLE_THRESHOLD;
    }
    
    @Override
    public double getMinimumAngleBetweenDifferentTacksUpwind() {
        return MINIMUM_ANGLE_BETWEEN_DIFFERENT_TACKS_UPWIND;
    }

    @Override
    public double getMinimumAngleBetweenDifferentTacksDownwind() {
        return MINIMUM_ANGLE_BETWEEN_DIFFERENT_TACKS_DOWNWIND;
    }

    @Override
    public Distance getMaximumDistanceForCourseApproximation() {
        return MAXIMUM_DISTANCE_FOR_COURSE_APPROXIMATION;
    }

    @Override
    public boolean typicallyStartsUpwind() {
        return typicallyStartsUpwind;
    }

    @Override
    public Distance getHullLength() {
        return hullLength;
    }

    @Override
    public double getDownwindWindEstimationConfidence(int numberOfBoatsInSmallestCluster) {
        // Even for up to a million boats in the smallest cluster, the multiplier is still less than one.
        // The multiplier is 1/1000000 in case there is only one boat and approaches 1.0 for many boats.
        return DOWNWIND_WIND_ESTIMATION_CONFIDENCE * getConfidenceMultiplierForClusterSize(numberOfBoatsInSmallestCluster);
    }

    @Override
    public double getUpwindWindEstimationConfidence(int numberOfBoatsInSmallestCluster) {
        // Even for up to a million boats in the smallest cluster, the multiplier is still less than one.
        // The multiplier is 1/1000000 in case there is only one boat and approaches 1.0 for many boats.
        return UPWIND_WIND_ESTIMATION_CONFIDENCE * getConfidenceMultiplierForClusterSize(numberOfBoatsInSmallestCluster);
    }

    /**
     * Even for up to a million boats in the smallest cluster, the multiplier is still less than one.
     * The multiplier is 1/1000000 in case there is only one boat and approaches 1.0 for many boats.
     */
    private double getConfidenceMultiplierForClusterSize(int numberOfBoatsInSmallestCluster) {
        return 1.0 + 1.0/1000000.0 - 1.0/(double) numberOfBoatsInSmallestCluster;
    }

    @Override
    public BoatClass resolve(SharedDomainFactory domainFactory) {
        return domainFactory.getOrCreateBoatClass(getName(), typicallyStartsUpwind());
    }

    public String getDisplayName() {
        return displayName;
    }

    public Distance getHullBeam() {
        return hullBeam;
    }

    public BoatHullType getHullType() {
        return hullType;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((super.getName() == null) ? 0 : super.getName().hashCode());
        result = prime * result + (int) (approximateManeuverDurationInMilliseconds
                ^ (approximateManeuverDurationInMilliseconds >>> 32));
        result = prime * result + ((displayName == null) ? 0 : displayName.hashCode());
        result = prime * result + ((hullBeam == null) ? 0 : hullBeam.hashCode());
        result = prime * result + ((hullLength == null) ? 0 : hullLength.hashCode());
        result = prime * result + ((hullType == null) ? 0 : hullType.hashCode());
        result = prime * result + (typicallyStartsUpwind ? 1231 : 1237);
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        BoatClassImpl other = (BoatClassImpl) obj;
        if (super.getName() == null) {
            if (other.getName() != null)
                return false;
        } else if (!super.getName().equals(other.getName()))
            return false;
        if (approximateManeuverDurationInMilliseconds != other.approximateManeuverDurationInMilliseconds)
            return false;
        if (displayName == null) {
            if (other.displayName != null)
                return false;
        } else if (!displayName.equals(other.displayName))
            return false;
        if (hullBeam == null) {
            if (other.hullBeam != null)
                return false;
        } else if (!hullBeam.equals(other.hullBeam))
            return false;
        if (hullLength == null) {
            if (other.hullLength != null)
                return false;
        } else if (!hullLength.equals(other.hullLength))
            return false;
        if (hullType != other.hullType)
            return false;
        if (typicallyStartsUpwind != other.typicallyStartsUpwind)
            return false;
        return true;
    }
    
}
