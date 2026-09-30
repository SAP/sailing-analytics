package com.sap.sailing.windestimation.aggregator.hmm;

import com.sap.sailing.windestimation.aggregator.hmm.WindCourseRange.CombinationModeOnViolation;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sailing.windestimation.data.TwdTransition;
import com.sap.sailing.windestimation.data.transformer.ManeuverForEstimationTransformer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class IntersectedWindRangeBasedTransitionProbabilitiesCalculator<GL extends GraphLevelBase<GL>>
        implements GraphNodeTransitionProbabilitiesCalculator<GL> {

    private static final double LA_PLACE_TRANSITION_PROBABILITY = 0.001;

    // TODO make the value below boat class specific?
    /**
     * Sum of the smallest possible absolute TWA upwind and smallest possible (180 deg - absolute TWA downwind). The
     * amount is used to limit the possible wind range considering head-up and bear-away maneuvers. Make sure that the
     * maximum allowed course change for clean maneuvers (currently 120 deg, see
     * {@link ManeuverForEstimationTransformer#isManeuverEligibleForAnalysis(double, double)}) plus the value of this
     * constant does not exceed 180 deg.
     */
    protected static final int MIN_BEATING_ANGLE_PLUS_MIN_RUNNING_ANGLE = 40;
    private static final double MAX_ABS_WIND_COURSE_DEVIATION_TOLERANCE_WITHIN_ANALYSIS_INTERVAL_IN_DEGREES = 40;
    protected final boolean propagateIntersectedWindRangeOfHeadupAndBearAway;

    public IntersectedWindRangeBasedTransitionProbabilitiesCalculator(
            boolean propagateIntersectedWindRangeOfHeadupAndBearAway) {
        this.propagateIntersectedWindRangeOfHeadupAndBearAway = propagateIntersectedWindRangeOfHeadupAndBearAway;
    }

    @Override
    public Pair<IntersectedWindRange, Double> mergeWindRangeAndGetTransitionProbability(GraphNode<GL> previousNode,
            GL previousLevel, IntersectedWindRange previousIntersectedWindRange, GraphNode<GL> currentNode,
            GL currentLevel) {
        Duration durationPassed = getDuration(previousLevel.getManeuver(), currentLevel.getManeuver());
        Distance distancePassed = getDistance(previousLevel.getManeuver(), currentLevel.getManeuver());
        double transitionProbabilitySum = 0;
        double transitionProbabilityUntilCurrentNode = -1;
        IntersectedWindRange intersectedWindRangeUntilCurrentNode = null;
        for (GraphNode<GL> node : currentLevel.getLevelNodes()) {
            IntersectedWindRange intersectedWindRange = previousIntersectedWindRange.intersect(node.getValidWindRange(),
                    CombinationModeOnViolation.INTERSECTION);
            TwdTransition twdTransition = constructTwdTransition(durationPassed, distancePassed,
                    intersectedWindRange.getViolationRange(), previousNode.getManeuverType(), node.getManeuverType());
            double transitionProbability = getPenaltyFactorForTransition(twdTransition);
            transitionProbabilitySum += transitionProbability;
            if (node == currentNode) {
                transitionProbabilityUntilCurrentNode = transitionProbability;
                intersectedWindRangeUntilCurrentNode = propagateIntersectedWindRangeOfHeadupAndBearAway
                        && (node.getManeuverType() == ManeuverTypeForClassification.BEAR_AWAY
                                || node.getManeuverType() == ManeuverTypeForClassification.HEAD_UP)
                                        ? intersectedWindRange
                                        : node.getValidWindRange().toIntersected();
            }
        }
        if (transitionProbabilityUntilCurrentNode < 0) {
            throw new IllegalArgumentException("currentNode not contained in currentLevel");
        }
        double normalizedTransitionProbabilityUntilCurrentNode = transitionProbabilityUntilCurrentNode
                / transitionProbabilitySum;
        return new Pair<>(intersectedWindRangeUntilCurrentNode, normalizedTransitionProbabilityUntilCurrentNode);
    }

    protected double getPenaltyFactorForTransition(TwdTransition twdTransition) {
        double violationRange = twdTransition.getTwdChange().getDegrees();
        double penaltyFactor;
        if (violationRange == 0) {
            penaltyFactor = 1.0;
        } else {
            if (violationRange <= MAX_ABS_WIND_COURSE_DEVIATION_TOLERANCE_WITHIN_ANALYSIS_INTERVAL_IN_DEGREES) {
                penaltyFactor = 1 / (1 + Math.pow(violationRange
                        / MAX_ABS_WIND_COURSE_DEVIATION_TOLERANCE_WITHIN_ANALYSIS_INTERVAL_IN_DEGREES * 2, 2));
            } else {
                penaltyFactor = 1 / (1 + (Math.pow(violationRange, 2)));
            }
        }
        return penaltyFactor + LA_PLACE_TRANSITION_PROBABILITY;
    }

    public WindCourseRange getWindCourseRangeForManeuverType(ManeuverForEstimation maneuver,
            ManeuverTypeForClassification maneuverType) {
        switch (maneuverType) {
        case TACK:
            return getTackWindRange(maneuver);
        case JIBE:
            return getJibeWindRange(maneuver);
        case HEAD_UP:
            return getHeadUpWindRange(maneuver);
        case BEAR_AWAY:
            return getBearAwayWindRange(maneuver);
        default:
            throw new IllegalArgumentException();
        }
    }

    protected WindCourseRange getBearAwayWindRange(ManeuverForEstimation maneuver) {
        Bearing invertedCourseBefore = maneuver.getSpeedWithBearingBefore().getBearing().reverse();
        double angleTowardStarboard = invertedCourseBefore
                .getDifferenceTo(maneuver.getSpeedWithBearingAfter().getBearing()).abs().getDegrees();
        angleTowardStarboard -= MIN_BEATING_ANGLE_PLUS_MIN_RUNNING_ANGLE;
        assert (angleTowardStarboard > 0); // FIXME bug5050: this breaks for "Jibe Sets", jibing after the mark rounding
        Bearing from;
        if (maneuver.getCourseChangeInDegrees() < 0) {
            from = invertedCourseBefore.add(new DegreeBearingImpl(MIN_BEATING_ANGLE_PLUS_MIN_RUNNING_ANGLE));
        } else {
            from = maneuver.getSpeedWithBearingAfter().getBearing();
        }
        WindCourseRange windRange = new WindCourseRange(from.getDegrees(), angleTowardStarboard);
        return windRange;
    }

    protected WindCourseRange getHeadUpWindRange(ManeuverForEstimation maneuver) {
        Bearing invertedCourseAfter = maneuver.getSpeedWithBearingAfter().getBearing().reverse();
        double angleTowardStarboard = invertedCourseAfter
                .getDifferenceTo(maneuver.getSpeedWithBearingBefore().getBearing()).abs().getDegrees();
        angleTowardStarboard -= MIN_BEATING_ANGLE_PLUS_MIN_RUNNING_ANGLE;
        assert (angleTowardStarboard > 0); // FIXME bug5050: this breaks for "Kiwi Drops", tacking after the mark rounding
        Bearing from;
        if (maneuver.getCourseChangeInDegrees() < 0) {
            from = maneuver.getSpeedWithBearingBefore().getBearing();
        } else {
            from = invertedCourseAfter.add(new DegreeBearingImpl(MIN_BEATING_ANGLE_PLUS_MIN_RUNNING_ANGLE));
        }
        WindCourseRange windRange = new WindCourseRange(from.getDegrees(), angleTowardStarboard);
        return windRange;
    }

    protected WindCourseRange getJibeWindRange(ManeuverForEstimation maneuver) {
        Bearing middleCourse = maneuver.getMiddleCourse();
        double absCourseChangeDeg = Math.abs(maneuver.getCourseChangeInDegrees());
        double middleAngleRange = maneuver.getDeviationFromOptimalJibeAngleInDegrees() == null
                ? absCourseChangeDeg * 0.2
                : Math.abs(maneuver.getDeviationFromOptimalJibeAngleInDegrees());
        if (middleAngleRange < 10) {
            middleAngleRange = 10;
        }
        Bearing from = middleCourse.add(new DegreeBearingImpl(-middleAngleRange / 2.0));
        WindCourseRange windRange = new WindCourseRange(from.getDegrees(), middleAngleRange);
        return windRange;
    }

    protected WindCourseRange getTackWindRange(ManeuverForEstimation maneuver) {
        Bearing middleCourse = maneuver.getMiddleCourse();
        double absCourseChangeDeg = Math.abs(maneuver.getCourseChangeInDegrees());
        double middleAngleRange = maneuver.getDeviationFromOptimalTackAngleInDegrees() == null
                ? absCourseChangeDeg * 0.1
                : Math.abs(maneuver.getDeviationFromOptimalTackAngleInDegrees());
        Bearing from = middleCourse.add(new DegreeBearingImpl(-middleAngleRange / 2.0));
        from = from.reverse();
        WindCourseRange windRange = new WindCourseRange(from.getDegrees(), middleAngleRange);
        return windRange;
    }

    protected TwdTransition constructTwdTransition(Duration durationPassed, Distance distancePassed,
            double twdChangeInDegrees, ManeuverTypeForClassification fromManeuverType,
            ManeuverTypeForClassification toManeuverType) {
        DegreeBearingImpl twdChange = new DegreeBearingImpl(twdChangeInDegrees);
        TwdTransition twdTransition = new TwdTransition(distancePassed, durationPassed, twdChange, fromManeuverType,
                toManeuverType);
        return twdTransition;
    }

    protected Distance getDistance(ManeuverForEstimation fromManeuver, ManeuverForEstimation toManeuver) {
        return fromManeuver.getManeuverPosition().getDistance(toManeuver.getManeuverPosition());
    }

    protected Duration getDuration(ManeuverForEstimation fromManeuver, ManeuverForEstimation toManeuver) {
        return fromManeuver.getManeuverTimePoint().until(toManeuver.getManeuverTimePoint()).abs();
    }
}
