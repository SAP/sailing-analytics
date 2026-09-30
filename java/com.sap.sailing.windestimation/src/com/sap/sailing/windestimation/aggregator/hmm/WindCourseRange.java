package com.sap.sailing.windestimation.aggregator.hmm;

/**
 * Represents a range of inverted TWD. The range starts the reference course specified by {@link #getFromPortside()} and
 * encompasses all the range toward starboard which is limited by {@link #getAngleTowardStarboard()}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindCourseRange {

    private final double fromPortside;
    private final double angleTowardStarboard;

    public WindCourseRange(double fromPortside, double angleTowardStarboard) {
        this.fromPortside = fromPortside;
        this.angleTowardStarboard = angleTowardStarboard;
    }

    public double getFromPortside() {
        return fromPortside;
    }

    public double getAngleTowardStarboard() {
        return angleTowardStarboard;
    }

    public WindCourseRange invert() {
        double newFromPortside = fromPortside + angleTowardStarboard;
        if (newFromPortside >= 360) {
            newFromPortside -= 360;
        }
        double newAngleTowardStarboard = 360 - angleTowardStarboard;
        return new WindCourseRange(newFromPortside, newAngleTowardStarboard);
    }

    public IntersectedWindRange toIntersected() {
        return new IntersectedWindRange(fromPortside, angleTowardStarboard, 0);
    }

    /**
     * @param combinationModeOnViolation
     *            With {@link CombinationModeOnViolation#EXPANSION} the resulting range is the union of both ranges plus
     *            the gap between the two non-overlapping ranges; with {@link CombinationModeOnViolation#INTERSECTION}
     *            the resulting range describes only the gap. In both cases,
     *            {@link IntersectedWindRange#getViolationRange()} will return the size of the gap in degrees.
     */
    public IntersectedWindRange intersect(WindCourseRange nextWindRange,
            CombinationModeOnViolation combinationModeOnViolation) {
        double deviationFromPortsideBoundaryTowardStarboard = nextWindRange.fromPortside - fromPortside;
        if (deviationFromPortsideBoundaryTowardStarboard < 0) {
            deviationFromPortsideBoundaryTowardStarboard += 360;
        }
        double deviationFromPortsideTowardStarboardInDegrees = deviationFromPortsideBoundaryTowardStarboard
                - angleTowardStarboard;
        double newFromPortside = nextWindRange.fromPortside;
        double newAngleTowardStarboard = nextWindRange.angleTowardStarboard;
        double violationRange = 0;
        if (deviationFromPortsideTowardStarboardInDegrees <= 0) {
            // nextWindRange.fromPortside is within the range
            switch (combinationModeOnViolation) {
            case INTERSECTION:
                newFromPortside = nextWindRange.fromPortside;
                if (deviationFromPortsideTowardStarboardInDegrees + nextWindRange.angleTowardStarboard < 0) {
                    newAngleTowardStarboard = nextWindRange.angleTowardStarboard;
                } else {
                    newAngleTowardStarboard = Math.abs(deviationFromPortsideTowardStarboardInDegrees);
                }
                break;
            case EXPANSION:
                newFromPortside = fromPortside;
                newAngleTowardStarboard = angleTowardStarboard + deviationFromPortsideTowardStarboardInDegrees
                        + nextWindRange.angleTowardStarboard;
                break;
            case RANGE_OF_NEXT:
                break;
            }
        } else {
            double deviationFromPortsideBoundaryTowardPortside = 360 - deviationFromPortsideBoundaryTowardStarboard;
            double deviationFromPortsideTowardPortsideInDegrees = deviationFromPortsideBoundaryTowardPortside
                    - nextWindRange.angleTowardStarboard;
            if (deviationFromPortsideTowardPortsideInDegrees <= 0) {
                // fromPortside is within nextWindRange
                switch (combinationModeOnViolation) {
                case INTERSECTION:
                    newFromPortside = fromPortside;
                    if (deviationFromPortsideTowardPortsideInDegrees + angleTowardStarboard < 0) {
                        newAngleTowardStarboard = angleTowardStarboard;
                    } else {
                        newAngleTowardStarboard = Math.abs(deviationFromPortsideTowardPortsideInDegrees);
                    }
                    break;
                case EXPANSION:
                    newFromPortside = nextWindRange.fromPortside;
                    newAngleTowardStarboard = angleTowardStarboard + deviationFromPortsideTowardPortsideInDegrees
                            + nextWindRange.angleTowardStarboard;
                    break;
                case RANGE_OF_NEXT:
                    break;
                }
            } else {
                if (deviationFromPortsideTowardStarboardInDegrees < deviationFromPortsideTowardPortsideInDegrees) {
                    switch (combinationModeOnViolation) {
                    case INTERSECTION:
                        newFromPortside = fromPortside + angleTowardStarboard;
                        newAngleTowardStarboard = deviationFromPortsideTowardStarboardInDegrees;
                        break;
                    case EXPANSION:
                        newFromPortside = fromPortside;
                        newAngleTowardStarboard = angleTowardStarboard + deviationFromPortsideTowardStarboardInDegrees
                                + nextWindRange.angleTowardStarboard;
                    case RANGE_OF_NEXT:
                        break;
                    }
                    violationRange = deviationFromPortsideTowardStarboardInDegrees;
                } else {
                    switch (combinationModeOnViolation) {
                    case INTERSECTION:
                        newFromPortside = nextWindRange.fromPortside + nextWindRange.angleTowardStarboard;
                        newAngleTowardStarboard = deviationFromPortsideTowardPortsideInDegrees;
                        break;
                    case EXPANSION:
                        newFromPortside = nextWindRange.fromPortside;
                        newAngleTowardStarboard = angleTowardStarboard + deviationFromPortsideTowardPortsideInDegrees
                                + nextWindRange.angleTowardStarboard;
                    case RANGE_OF_NEXT:
                        break;
                    }
                    violationRange = deviationFromPortsideTowardPortsideInDegrees;
                }
                if (newFromPortside >= 360) {
                    newFromPortside -= 360;
                }
            }
        }
        if (combinationModeOnViolation == CombinationModeOnViolation.EXPANSION
                && this instanceof IntersectedWindRange) {
            violationRange += ((IntersectedWindRange) this).getViolationRange();
        }
        return new IntersectedWindRange(newFromPortside, newAngleTowardStarboard, violationRange);
    }

    public boolean isWindCourseWithinRange(double windCourseInDegrees) {
        double deviationFromPortsideBoundaryTowardStarboard = windCourseInDegrees - fromPortside;
        if (deviationFromPortsideBoundaryTowardStarboard < 0) {
            deviationFromPortsideBoundaryTowardStarboard += 360;
        }
        double deviationFromPortsideTowardStarboardInDegrees = deviationFromPortsideBoundaryTowardStarboard
                - angleTowardStarboard;
        return deviationFromPortsideTowardStarboardInDegrees <= 0;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        long temp;
        temp = Double.doubleToLongBits(angleTowardStarboard);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        temp = Double.doubleToLongBits(fromPortside);
        result = prime * result + (int) (temp ^ (temp >>> 32));
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
        WindCourseRange other = (WindCourseRange) obj;
        if (Double.doubleToLongBits(angleTowardStarboard) != Double.doubleToLongBits(other.angleTowardStarboard))
            return false;
        if (Double.doubleToLongBits(fromPortside) != Double.doubleToLongBits(other.fromPortside))
            return false;
        return true;
    }
    
    @Override
    public String toString() {
        double a = 180+fromPortside;
        if (a>360) {
            a -= 360;
        }
        double b = a+angleTowardStarboard;
        if (b>360) {
            b -= 360;
        }
        return "Wind from "+(angleTowardStarboard==0?a:(a+"-"+b))+"° ("+angleTowardStarboard+"°)";
    }

    public enum CombinationModeOnViolation {
        INTERSECTION, EXPANSION, RANGE_OF_NEXT
    }

}
