package com.sap.sailing.windestimation.aggregator.hmm;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.sse.common.Util.Pair;

/**
 * Infers best path within possible solution space with hidden states for each observation using an adapted variant of
 * Viterbi for conventional HMM models which allows to label each provided maneuver with its most suitable maneuver
 * type. Forward-Backward algorithm is used to induce the confidence for each produced {@link GraphLevelInference}.<p>
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class BestPathsCalculator {

    private GraphLevel lastLevel;
    private Map<GraphLevel, BestPathsPerLevel> bestPathsPerLevel;
    private final boolean preciseConfidence;
    private final GraphNodeTransitionProbabilitiesCalculator<GraphLevel> transitionProbabilitiesCalculator;

    public BestPathsCalculator(GraphNodeTransitionProbabilitiesCalculator<GraphLevel> transitionProbabilitiesCalculator) {
        this(true, transitionProbabilitiesCalculator);
    }

    public BestPathsCalculator(boolean preciseConfidence,
            GraphNodeTransitionProbabilitiesCalculator<GraphLevel> transitionProbabilitiesCalculator) {
        this.preciseConfidence = preciseConfidence;
        this.transitionProbabilitiesCalculator = transitionProbabilitiesCalculator;
    }

    public GraphNodeTransitionProbabilitiesCalculator<GraphLevel> getTransitionProbabilitiesCalculator() {
        return transitionProbabilitiesCalculator;
    }

    public void computeBestPathsFromScratch() {
        GraphLevel previousLevel = lastLevel;
        if (previousLevel != null) {
            // find first level
            while (previousLevel.getPreviousLevel() != null) {
                previousLevel = previousLevel.getPreviousLevel();
            }
            computeBestPathsFromScratch(previousLevel);
        }
    }

    public void computeBestPathsFromScratch(GraphLevel firstLevel) {
        resetState();
        GraphLevel currentLevel = firstLevel;
        do {
            computeBestPathsToNextLevel(currentLevel);
        } while ((currentLevel = currentLevel.getNextLevel()) != null);
    }

    public void recomputeBestPathsFromLevel(GraphLevel fromLevel) {
        List<GraphLevel> levelsToKeep = new LinkedList<>();
        GraphLevel currentLevel = fromLevel.getPreviousLevel();
        while (currentLevel != null) {
            levelsToKeep.add(currentLevel);
            currentLevel = currentLevel.getPreviousLevel();
        }
        Iterator<Entry<GraphLevel, BestPathsPerLevel>> iterator = bestPathsPerLevel.entrySet().iterator();
        while (iterator.hasNext()) {
            Entry<GraphLevel, BestPathsPerLevel> entry = iterator.next();
            if (!levelsToKeep.contains(entry.getKey())) {
                iterator.remove();
            }
        }
        lastLevel = fromLevel.getPreviousLevel();
        currentLevel = fromLevel;
        do {
            computeBestPathsToNextLevel(currentLevel);
        } while (currentLevel != null);
    }

    public void resetState() {
        lastLevel = null;
        bestPathsPerLevel = null;
    }

    public void computeBestPathsToNextLevel(GraphLevel nextLevel) {
        GraphLevel previousLevel = nextLevel.getPreviousLevel();
        if (previousLevel != lastLevel) {
            throw new IllegalArgumentException(
                    "The previous level of next level does not match with the last level processed by this calculator");
        }
        GraphLevel currentLevel = nextLevel;
        if (previousLevel == null) {
            bestPathsPerLevel = new HashMap<>();
            BestPathsPerLevel bestPathsUntilLevel = new BestPathsPerLevel(currentLevel);
            for (GraphNode<GraphLevel> currentNode : currentLevel.getLevelNodes()) {
                double probability = currentNode.getConfidence() / currentLevel.getLevelNodes().size();
                BestManeuverNodeInfo<GraphLevel> currentNodeInfo = bestPathsUntilLevel.addBestPreviousNodeInfo(currentNode, null,
                        probability, currentNode.getValidWindRange().toIntersected());
                currentNodeInfo.setForwardProbability(probability);
            }
            bestPathsPerLevel.put(currentLevel, bestPathsUntilLevel);
        } else {
            BestPathsPerLevel bestPathsUntilPreviousLevel = bestPathsPerLevel.get(previousLevel);
            BestPathsPerLevel bestPathsUntilLevel = new BestPathsPerLevel(currentLevel);
            for (GraphNode<GraphLevel> currentNode : currentLevel.getLevelNodes()) {
                double bestProbabilityFromStart = 0;
                double forwardProbability = 0;
                GraphNode<GraphLevel> bestPreviousNode = null;
                IntersectedWindRange bestIntersectedWindRange = null;
                for (GraphNode<GraphLevel> previousNode : previousLevel.getLevelNodes()) {
                    IntersectedWindRange previousNodeIntersectedWindRange = bestPathsUntilPreviousLevel
                            .getBestPreviousNodeInfo(previousNode).getIntersectedWindRange();
                    Pair<IntersectedWindRange, Double> newWindRangeAndProbability = transitionProbabilitiesCalculator
                            .mergeWindRangeAndGetTransitionProbability(previousNode, previousLevel,
                                    previousNodeIntersectedWindRange, currentNode, currentLevel);
                    double transitionObservationMultipliedProbability = newWindRangeAndProbability.getB()
                            * currentNode.getConfidence();
                    double probabilityFromStart = bestPathsUntilPreviousLevel.getNormalizedProbabilityToNodeFromStart(
                            previousNode) * transitionObservationMultipliedProbability;
                    forwardProbability += transitionObservationMultipliedProbability
                            * bestPathsUntilPreviousLevel.getNormalizedForwardProbability(previousNode);
                    if (probabilityFromStart > bestProbabilityFromStart) {
                        bestProbabilityFromStart = probabilityFromStart;
                        bestPreviousNode = previousNode;
                        bestIntersectedWindRange = newWindRangeAndProbability.getA();
                    }
                }
                BestManeuverNodeInfo<GraphLevel> currentNodeInfo = bestPathsUntilLevel.addBestPreviousNodeInfo(currentNode,
                        bestPreviousNode, bestProbabilityFromStart, bestIntersectedWindRange);
                currentNodeInfo.setForwardProbability(forwardProbability);
            }
            bestPathsPerLevel.put(currentLevel, bestPathsUntilLevel);
        }
        this.lastLevel = currentLevel;
    }

    public List<GraphLevelInference<GraphLevel>> getBestPath(GraphLevel lastLevel, GraphNode<GraphLevel> lastNode) {
        double probabilitiesSum = 0;
        BestPathsPerLevel bestPathsUntilLastLevel = bestPathsPerLevel.get(lastLevel);
        double lastNodeProbability = bestPathsUntilLastLevel.getNormalizedProbabilityToNodeFromStart(lastNode);
        if (preciseConfidence) {
            if (!bestPathsUntilLastLevel.isBackwardProbabilitiesComputed()) {
                computeBackwardProbabilities();
            }
            GraphLevel firstLevel = lastLevel;
            while (firstLevel.getPreviousLevel() != null) {
                firstLevel = firstLevel.getPreviousLevel();
            }
            probabilitiesSum = bestPathsUntilLastLevel.getForwardProbabilitiesSum();
        } else {
            for (GraphNode<GraphLevel> node : lastLevel.getLevelNodes()) {
                double probability = bestPathsUntilLastLevel.getNormalizedProbabilityToNodeFromStart(node);
                probabilitiesSum += probability;
            }
        }
        List<GraphLevelInference<GraphLevel>> result = new LinkedList<>();
        GraphNode<GraphLevel> currentNode = lastNode;
        GraphLevel currentLevel = lastLevel;
        while (currentLevel != null) {
            BestPathsPerLevel currentLevelInfo = bestPathsPerLevel.get(currentLevel);
            BestManeuverNodeInfo<GraphLevel> currentNodeInfo = currentLevelInfo.getBestPreviousNodeInfo(currentNode);
            double nodeConfidence;
            if (preciseConfidence) {
                nodeConfidence = currentLevelInfo.getNormalizedForwardBackwardProbability(currentNode);
            } else {
                nodeConfidence = lastNodeProbability / probabilitiesSum;
            }
            GraphLevelInference<GraphLevel> entry = new GraphLevelInference<>(currentLevel, currentNode, nodeConfidence);
            result.add(0, entry);
            currentNode = currentNodeInfo.getBestPreviousNode();
            currentLevel = currentLevel.getPreviousLevel();
        }
        return result;
    }

    public List<GraphLevelInference<GraphLevel>> getBestPath(GraphLevel lastLevel) {
        BestPathsPerLevel bestPathsUntilLevel = bestPathsPerLevel.get(lastLevel);
        double maxProbability = 0;
        GraphNode<GraphLevel> bestLastNode = null;
        for (GraphNode<GraphLevel> lastNode : lastLevel.getLevelNodes()) {
            double probability = bestPathsUntilLevel.getNormalizedProbabilityToNodeFromStart(lastNode);
            if (maxProbability < probability) {
                maxProbability = probability;
                bestLastNode = lastNode;
            }
        }
        return getBestPath(lastLevel, bestLastNode);
    }

    public boolean isPreciseConfidence() {
        return preciseConfidence;
    }

    public void computeBackwardProbabilities() {
        GraphLevel currentLevel = lastLevel;
        BestPathsPerLevel bestPathsUntilLastLevel = bestPathsPerLevel.get(currentLevel);
        for (GraphNode<GraphLevel> currentNode : currentLevel.getLevelNodes()) {
            BestManeuverNodeInfo<GraphLevel> currentNodeInfo = bestPathsUntilLastLevel.getBestPreviousNodeInfo(currentNode);
            currentNodeInfo.setBackwardProbability(1.0);
        }
        GraphLevel nextLevel = currentLevel;
        while ((currentLevel = currentLevel.getPreviousLevel()) != null) {
            BestPathsPerLevel bestPathsUntilLevel = bestPathsPerLevel.get(currentLevel);
            BestPathsPerLevel bestPathsUntilNextLevel = bestPathsPerLevel.get(nextLevel);
            for (GraphNode<GraphLevel> currentNode : currentLevel.getLevelNodes()) {
                BestManeuverNodeInfo<GraphLevel> currentNodeInfo = bestPathsUntilLevel.getBestPreviousNodeInfo(currentNode);
                double backwardProbability = 0;
                for (GraphNode<GraphLevel> nextNode : nextLevel.getLevelNodes()) {
                    Pair<IntersectedWindRange, Double> newWindRangeAndProbability = transitionProbabilitiesCalculator
                            .mergeWindRangeAndGetTransitionProbability(currentNode, currentLevel,
                                    currentNodeInfo.getIntersectedWindRange(), nextNode, nextLevel);
                    backwardProbability += nextNode.getConfidence() * newWindRangeAndProbability.getB()
                            * bestPathsUntilNextLevel.getNormalizedBackwardProbability(nextNode);
                }
                currentNodeInfo.setBackwardProbability(backwardProbability);
            }
            nextLevel = currentLevel;
        }
    }

}
