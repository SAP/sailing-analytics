package com.sap.sailing.windestimation.maneuvergraph.bestpath;

import java.util.HashMap;
import java.util.Map;

import com.sap.sailing.windestimation.maneuvergraph.FineGrainedPointOfSail;
import com.sap.sailing.windestimation.maneuvergraph.GraphLevel;
import com.sap.sailing.windestimation.maneuvergraph.ProbabilityUtil;

class BestPathsUntilLevel {
    private FineGrainedPointOfSail[] bestPreviousNodes = new FineGrainedPointOfSail[FineGrainedPointOfSail
            .values().length];
    private double[] probabilitiesOfBestPathToNodeFromStart = new double[bestPreviousNodes.length];
    private WindRange[] windDeviationWithinBestPaths = new WindRange[bestPreviousNodes.length];
    private Map<String, SailingStatistics[]> pathStatisticsForNodesPerBoatClassName = new HashMap<>();

    public SailingStatistics getPathStatistics(GraphLevel currentLevel, FineGrainedPointOfSail currentNode) {
        SailingStatistics[] pathStatistics = pathStatisticsForNodesPerBoatClassName
                .get(currentLevel.getManeuver().getBoatClass().getName());
        return pathStatistics == null ? null : pathStatistics[currentNode.ordinal()];
    }

    public void setPathStatistics(GraphLevel currentLevel, FineGrainedPointOfSail currentNode,
            SailingStatistics pathStatisticsUntilNode) {
        SailingStatistics[] existingPathStatistics = pathStatisticsForNodesPerBoatClassName
                .get(currentLevel.getManeuver().getBoatClass().getName());
        if (existingPathStatistics == null) {
            existingPathStatistics = new SailingStatistics[bestPreviousNodes.length];
            pathStatisticsForNodesPerBoatClassName.put(currentLevel.getManeuver().getBoatClass().getName(),
                    existingPathStatistics);
        }
        existingPathStatistics[currentNode.ordinal()] = pathStatisticsUntilNode;
    }

    public void setWindDeviation(FineGrainedPointOfSail currentNode, WindRange windDeviation) {
        windDeviationWithinBestPaths[currentNode.ordinal()] = windDeviation;
    }

    public double getProbabilityOfBestPathToNodeFromStart(FineGrainedPointOfSail pointOfSail) {
        return probabilitiesOfBestPathToNodeFromStart[pointOfSail.ordinal()];
    }

    public void setProbabilityOfBestPathToNodeFromStart(FineGrainedPointOfSail pointOfSail, double probability) {
        probabilitiesOfBestPathToNodeFromStart[pointOfSail.ordinal()] = probability;
    }

    public FineGrainedPointOfSail getBestPreviousNode(FineGrainedPointOfSail currentNode) {
        return bestPreviousNodes[currentNode.ordinal()];
    }

    public void setBestPreviousNode(FineGrainedPointOfSail currentNode, FineGrainedPointOfSail bestPreviousNode) {
        bestPreviousNodes[currentNode.ordinal()] = bestPreviousNode;
    }

    public WindRange getWindDeviation(FineGrainedPointOfSail pointOfSail) {
        return windDeviationWithinBestPaths[pointOfSail.ordinal()];
    }

    public void normalizeProbabilities() {
        ProbabilityUtil.normalizeLikelihoodArray(probabilitiesOfBestPathToNodeFromStart);
    }

}
