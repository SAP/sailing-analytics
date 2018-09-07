package com.sap.sailing.windestimation.maneuvergraph.pointofsail;

import java.util.Collections;
import java.util.List;

import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.windestimation.data.CompetitorTrackWithEstimationData;
import com.sap.sailing.windestimation.data.FineGrainedPointOfSail;
import com.sap.sailing.windestimation.data.ManeuverForEstimation;
import com.sap.sailing.windestimation.data.transformer.EstimationDataUtil;
import com.sap.sailing.windestimation.maneuverclassifier.ManeuverClassifier;
import com.sap.sailing.windestimation.maneuverclassifier.ManeuverClassifiersCache;
import com.sap.sailing.windestimation.maneuverclassifier.ManeuverEstimationResult;
import com.sap.sse.common.Util.Pair;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class PointOfSailSequenceGraph {

    private GraphLevel firstGraphLevel = null;
    private GraphLevel lastGraphLevel = null;
    private final PolarDataService polarService;
    private final BestPathsCalculator bestPathsCalculator;
    private ManeuverClassifiersCache maneuverClassifiersCache;

    public PointOfSailSequenceGraph(List<CompetitorTrackWithEstimationData<ManeuverForEstimation>> competitorTracks,
            ManeuverClassifiersCache maneuverClassifiersCache, BestPathsCalculator bestPathsCalculator) {
        this.polarService = maneuverClassifiersCache.getPolarDataService();
        this.maneuverClassifiersCache = maneuverClassifiersCache;
        this.bestPathsCalculator = bestPathsCalculator;
        List<ManeuverForEstimation> usefulManeuvers = EstimationDataUtil
                .getUsefulManeuversSortedByTimePoint(competitorTracks);
        for (ManeuverForEstimation maneuver : usefulManeuvers) {
            appendManeuverAsGraphLevel(maneuver);
        }
    }

    private void appendManeuverAsGraphLevel(ManeuverForEstimation maneuver) {
        ManeuverClassifier bestClassifier = maneuverClassifiersCache.getBestClassifier(maneuver);
        ManeuverEstimationResult maneuverEstimationResult = bestClassifier.classifyManeuver(maneuver);
        GraphLevel newManeuverNodesLevel = new GraphLevel(maneuver, maneuverEstimationResult);
        if (firstGraphLevel == null) {
            firstGraphLevel = newManeuverNodesLevel;
            lastGraphLevel = newManeuverNodesLevel;

        } else {
            lastGraphLevel.appendNextManeuverNodesLevel(newManeuverNodesLevel);
            lastGraphLevel = newManeuverNodesLevel;
        }
        newManeuverNodesLevel.computeProbabilitiesFromPreviousLevelToThisLevel();
        bestPathsCalculator.computeBestPathsToNextLevel(newManeuverNodesLevel);
    }

    public GraphLevel getFirstGraphLevel() {
        return firstGraphLevel;
    }

    public GraphLevel getLastGraphLevel() {
        return lastGraphLevel;
    }

    public PolarDataService getPolarService() {
        return polarService;
    }

    protected GraphLevel recomputeTransitionProbabilitiesAtLevelsWhereNeeded() {
        GraphLevel currentLevel = this.getLastGraphLevel();
        GraphLevel lastReadjustedLevel = null;
        while (currentLevel != null) {
            if (currentLevel.isCalculationOfTransitionProbabilitiesNeeded()) {
                currentLevel.computeProbabilitiesFromPreviousLevelToThisLevel();
                lastReadjustedLevel = currentLevel;
            }
            currentLevel = currentLevel.getPreviousLevel();
        }
        return lastReadjustedLevel;
    }

    public List<WindWithConfidence<ManeuverForEstimation>> estimateWindTrack() {
        List<WindWithConfidence<ManeuverForEstimation>> windTrack = Collections.emptyList();
        GraphLevel lastGraphLevel = this.lastGraphLevel;
        if (lastGraphLevel != null) {
            WindTrackFromManeuverGraphExtractor windTrackFromManeuverGraphExtractor = new WindTrackFromManeuverGraphExtractor(
                    polarService);
            List<Pair<GraphLevel, FineGrainedPointOfSail>> bestPath = bestPathsCalculator.getBestPath(lastGraphLevel);
            double bestPathConfidence = bestPathsCalculator.getConfidenceOfBestPath(bestPath);
            windTrack = windTrackFromManeuverGraphExtractor.getWindTrack(bestPath, bestPathConfidence);
        }
        return windTrack;
    }

}
