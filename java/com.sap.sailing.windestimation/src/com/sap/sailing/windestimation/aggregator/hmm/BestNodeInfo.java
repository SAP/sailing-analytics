package com.sap.sailing.windestimation.aggregator.hmm;

/**
 * Contains information about best path until the node from level represented in {@link BestPathsPerLevel}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class BestNodeInfo {

    private double probabilityFromStart;
    private double forwardProbability;
    private double backwardProbability;
    private final IntersectedWindRange intersectedWindRange;

    public BestNodeInfo(double probabilityFromStart, IntersectedWindRange intersectedWindRange) {
        this.probabilityFromStart = probabilityFromStart;
        this.intersectedWindRange = intersectedWindRange;
    }

    public double getProbabilityFromStart() {
        return probabilityFromStart;
    }

    public void setProbabilityFromStart(double probabilityFromStart) {
        this.probabilityFromStart = probabilityFromStart;
    }

    public double getForwardProbability() {
        return forwardProbability;
    }

    public void setForwardProbability(double forwardProbability) {
        this.forwardProbability = forwardProbability;
    }

    public double getBackwardProbability() {
        return backwardProbability;
    }

    public void setBackwardProbability(double backwardProbability) {
        this.backwardProbability = backwardProbability;
    }

    public IntersectedWindRange getIntersectedWindRange() {
        return intersectedWindRange;
    }

}
