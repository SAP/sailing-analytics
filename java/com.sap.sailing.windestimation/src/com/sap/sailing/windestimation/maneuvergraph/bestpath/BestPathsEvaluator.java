package com.sap.sailing.windestimation.maneuvergraph.bestpath;

import java.util.List;

import com.sap.sailing.windestimation.maneuvergraph.FineGrainedPointOfSail;
import com.sap.sailing.windestimation.maneuvergraph.ManeuverNodesLevel;
import com.sap.sse.common.Util.Pair;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public interface BestPathsEvaluator<T extends ManeuverNodesLevel<T>> {

    BestPathEvaluationResult<T> evaluateBestPath(List<Pair<T, FineGrainedPointOfSail>> bestPath);

}
