package com.sap.sailing.windestimation.aggregator.msthmm;

import com.sap.sailing.windestimation.aggregator.hmm.GraphLevelInference;
import com.sap.sailing.windestimation.aggregator.msthmm.MstManeuverGraphGenerator.MstManeuverGraphComponents;

/**
 * Infers best path within a Minimum Spanning Tree (MST) using an adapted variant of Viterbi for conventional HMM models
 * which allows to label each provided maneuver with its most suitable maneuver type.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface MstBestPathsCalculator {

    MstGraphNodeTransitionProbabilitiesCalculator getTransitionProbabilitiesCalculator();

    Iterable<GraphLevelInference<MstGraphLevel>> getBestNodes(MstManeuverGraphComponents graphComponents);

}
