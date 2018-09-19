package com.sap.sailing.windestimation.maneuvergraph;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.windestimation.data.CoarseGrainedManeuverType;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.maneuverclassifier.ManeuverEstimationResult;
import com.sap.sailing.windestimation.maneuverclassifier.ManeuverTypeForClassification;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.impl.DegreeBearingImpl;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class GraphLevel {

    private static final double PENALTY_CIRCLE_PROBABILITY_BONUS = 0.3;
    private final ManeuverForEstimation maneuver;
    private ManeuverEstimationResult maneuverEstimationResult;

    private GraphLevel previousLevel = null;
    private GraphLevel nextLevel = null;

    private final List<GraphNode> levelNodes = new ArrayList<>();
    private final GraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator;

    public GraphLevel(ManeuverForEstimation maneuver, ManeuverEstimationResult maneuverEstimationResult,
            GraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator) {
        this.maneuver = maneuver;
        this.maneuverEstimationResult = maneuverEstimationResult;
        this.transitionProbabilitiesCalculator = transitionProbabilitiesCalculator;
        initNodes();
    }

    private void initNodes() {
        maneuverEstimationResult.getManeuverTypeLikelihood(CoarseGrainedManeuverType.TACK);
        maneuverEstimationResult.getManeuverTypeLikelihood(CoarseGrainedManeuverType.JIBE);
        maneuverEstimationResult.getManeuverTypeLikelihood(CoarseGrainedManeuverType.BEAR_AWAY);
        maneuverEstimationResult.getManeuverTypeLikelihood(CoarseGrainedManeuverType.HEAD_UP);
        for (ManeuverTypeForClassification maneuverType : ManeuverTypeForClassification.values()) {
            switch (maneuverType) {
            case TACK:
                initTackNode();
                break;
            case JIBE:
                initJibeNode();
                break;
            case OTHER:
                initHeadUpNode();
                initBearAwayNode();
                break;
            }
        }
        normalizeNodeConfidences();
    }

    private void addManeuverNode(ManeuverTypeForClassification maneuverType, Tack tackAfter, WindCourseRange windRange,
            double confidence) {
        GraphNode maneuverNode = new GraphNode(maneuverType, tackAfter, windRange, confidence, levelNodes.size());
        levelNodes.add(maneuverNode);
    }

    private void initBearAwayNode() {
        Tack tackAfter;
        if (maneuver.getCourseChangeInDegrees() < 0) {
            tackAfter = Tack.PORT;
        } else {
            tackAfter = Tack.STARBOARD;
        }
        WindCourseRange windRange = transitionProbabilitiesCalculator.getWindCourseRangeForManeuverType(maneuver,
                CoarseGrainedManeuverType.BEAR_AWAY);
        double confidence = maneuverEstimationResult.getManeuverTypeLikelihood(CoarseGrainedManeuverType.BEAR_AWAY);
        addManeuverNode(ManeuverTypeForClassification.OTHER, tackAfter, windRange, confidence);
    }

    private void initHeadUpNode() {
        Tack tackAfter;
        if (maneuver.getCourseChangeInDegrees() < 0) {
            tackAfter = Tack.STARBOARD;
        } else {
            tackAfter = Tack.PORT;
        }
        WindCourseRange windRange = transitionProbabilitiesCalculator.getWindCourseRangeForManeuverType(maneuver,
                CoarseGrainedManeuverType.HEAD_UP);
        double confidence = maneuverEstimationResult.getManeuverTypeLikelihood(CoarseGrainedManeuverType.HEAD_UP);
        addManeuverNode(ManeuverTypeForClassification.OTHER, tackAfter, windRange, confidence);
    }

    private void initJibeNode() {
        WindCourseRange windRange = transitionProbabilitiesCalculator.getWindCourseRangeForManeuverType(maneuver,
                CoarseGrainedManeuverType.JIBE);
        double confidence = maneuverEstimationResult.getManeuverTypeLikelihood(CoarseGrainedManeuverType.JIBE);
        Tack tackAfter = maneuver.getCourseChangeWithinMainCurveInDegrees() < 0 ? Tack.STARBOARD : Tack.PORT;
        addManeuverNode(ManeuverTypeForClassification.JIBE, tackAfter, windRange, confidence);
    }

    private void initTackNode() {
        WindCourseRange windRange = transitionProbabilitiesCalculator.getWindCourseRangeForManeuverType(maneuver,
                CoarseGrainedManeuverType.TACK);
        double confidence = maneuverEstimationResult.getManeuverTypeLikelihood(CoarseGrainedManeuverType.TACK);
        Tack tackAfter = maneuver.getCourseChangeWithinMainCurveInDegrees() < 0 ? Tack.PORT : Tack.STARBOARD;
        addManeuverNode(ManeuverTypeForClassification.TACK, tackAfter, windRange, confidence);
    }

    public ManeuverForEstimation getManeuver() {
        return maneuver;
    }

    public List<GraphNode> getLevelNodes() {
        return levelNodes;
    }

    protected void setNextLevel(GraphLevel nextLevel) {
        this.nextLevel = nextLevel;
    }

    public GraphLevel getNextLevel() {
        return nextLevel;
    }

    protected void setPreviousLevel(GraphLevel previousLevel) {
        this.previousLevel = previousLevel;
    }

    public GraphLevel getPreviousLevel() {
        return previousLevel;
    }

    public void appendNextManeuverNodesLevel(GraphLevel nextManeuverNodesLevel) {
        setNextLevel(nextManeuverNodesLevel);
        nextManeuverNodesLevel.setPreviousLevel(this);
    }

    public void upgradeLevelNodesConsideringPenaltyCircle(ManeuverForEstimation penaltyCircle) {
        Bearing courseAtLowestSpeed = penaltyCircle.getCourseAtLowestSpeed();
        Bearing from = courseAtLowestSpeed.add(new DegreeBearingImpl(90.0));
        double angleTowardStarboard = 180;
        WindCourseRange windRange = new WindCourseRange(from.getDegrees(), angleTowardStarboard);
        for (GraphNode currentNode : levelNodes) {
            IntersectedWindRange intersectedWindRange = currentNode.getValidWindRange().intersect(windRange);
            if (intersectedWindRange.getViolationRange() == 0) {
                currentNode.setConfidence(currentNode.getConfidence() + PENALTY_CIRCLE_PROBABILITY_BONUS);
            }
        }
        normalizeNodeConfidences();
    }

    private void normalizeNodeConfidences() {
        double probabilitiesSum = 0;
        for (GraphNode currentNode : levelNodes) {
            probabilitiesSum += currentNode.getConfidence();
        }
        for (GraphNode currentNode : levelNodes) {
            currentNode.setConfidence(currentNode.getConfidence() / probabilitiesSum);
        }
    }

}
