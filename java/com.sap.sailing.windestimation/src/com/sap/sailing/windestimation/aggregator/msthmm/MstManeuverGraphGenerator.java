package com.sap.sailing.windestimation.aggregator.msthmm;

import java.util.ArrayList;
import java.util.List;

import com.sap.sailing.windestimation.aggregator.graph.Tree;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverWithProbabilisticTypeClassification;

/**
 * Incremental Minimum Spanning Tree (MST) builder which is capable to parse an acyclic directed graph for
 * {@link MstManeuverGraph}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class MstManeuverGraphGenerator extends AbstractMstGraphGenerator<ManeuverWithProbabilisticTypeClassification>
        implements Cloneable {

    private final MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator;

    public MstManeuverGraphGenerator(MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator) {
        this.transitionProbabilitiesCalculator = transitionProbabilitiesCalculator;
    }

    protected MstManeuverGraphGenerator(List<NodeWithNeighbors<ManeuverWithProbabilisticTypeClassification>> nodes,
            MstGraphNodeTransitionProbabilitiesCalculator transitionProbabilitiesCalculator) {
        super(nodes);
        this.transitionProbabilitiesCalculator = transitionProbabilitiesCalculator;
    }

    public MstGraphNodeTransitionProbabilitiesCalculator getTransitionProbabilitiesCalculator() {
        return transitionProbabilitiesCalculator;
    }
    
    @Override
    protected double getDistanceBetweenObservations(ManeuverWithProbabilisticTypeClassification o1,
            ManeuverWithProbabilisticTypeClassification o2) {
        double compoundDistance = transitionProbabilitiesCalculator.getCompoundDistance(o1.getManeuver(),
                o2.getManeuver());
        return compoundDistance;
    }

    public MstManeuverGraphComponents parseGraph() {
        List<NodeWithNeighbors<ManeuverWithProbabilisticTypeClassification>> nodes = getNodes();
        if (nodes.isEmpty()) {
            return null;
        }
        List<MstGraphLevel> leaves = new ArrayList<>();
        NodeWithNeighbors<ManeuverWithProbabilisticTypeClassification> firstNode = nodes.get(0);
        MstGraphLevel firstGraphLevel = new MstGraphLevel(firstNode.getObservation(),
                transitionProbabilitiesCalculator);
        parseGraphFromNodes(firstNode, firstGraphLevel, null, leaves);
        MstManeuverGraphComponents graphComponents = new MstManeuverGraphComponents(firstGraphLevel, leaves);
        return graphComponents;
    }

    private void parseGraphFromNodes(NodeWithNeighbors<ManeuverWithProbabilisticTypeClassification> previousNode,
            MstGraphLevel previousGraphLevel,
            NodeWithNeighbors<ManeuverWithProbabilisticTypeClassification> parentOfPreviousNode,
            List<MstGraphLevel> leaves) {
        List<NodeWithDistance<ManeuverWithProbabilisticTypeClassification>> childNodes = previousNode.getNeighbors();
        if (childNodes.size() <= 1 && (childNodes.isEmpty() || parentOfPreviousNode != null)) {
            leaves.add(previousGraphLevel);
        } else {
            for (NodeWithDistance<ManeuverWithProbabilisticTypeClassification> childNodeWithDistance : childNodes) {
                NodeWithNeighbors<ManeuverWithProbabilisticTypeClassification> childNode = childNodeWithDistance
                        .getNodeWithNeighbors();
                if (childNode != parentOfPreviousNode) {
                    MstGraphLevel newGraphLevel = previousGraphLevel.addChild(childNodeWithDistance.getDistance(),
                            childNode.getObservation(), transitionProbabilitiesCalculator);
                    parseGraphFromNodes(childNode, newGraphLevel, previousNode, leaves);
                }
            }
        }
    }

    @Override
    public MstManeuverGraphGenerator clone() {
        return new MstManeuverGraphGenerator(getClonedNodes(), transitionProbabilitiesCalculator);
    }

    public static class MstManeuverGraphComponents implements Tree<MstGraphLevel> {
        private final MstGraphLevel root;
        private final List<MstGraphLevel> leaves;

        public MstManeuverGraphComponents(MstGraphLevel root, List<MstGraphLevel> leaves) {
            this.root = root;
            this.leaves = leaves;
        }

        @Override
        public MstGraphLevel getRoot() {
            return root;
        }

        public List<MstGraphLevel> getLeaves() {
            return leaves;
        }
        
        @Override
        public String toString() {
            StringBuilder result = new StringBuilder();
            result.append("MST maneuver graph:\n");
            appendGraphLevelToStringBuilder(root, result, 0);
            return result.toString();
        }
        
        private void appendGraphLevelToStringBuilder(MstGraphLevel graphLevel, StringBuilder result, int indent) {
            for (int i = 0; i < indent; i++) {
                result.append(" ");
            }
            result.append(graphLevel);
            result.append("\n");
            for (MstGraphLevel child : graphLevel.getChildren()) {
                appendGraphLevelToStringBuilder(child, result, indent + 1);
            }
        }
    }
}
