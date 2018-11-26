package com.sap.sailing.windestimation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.windestimation.aggregator.clustering.ManeuverClassificationForClusteringImpl;
import com.sap.sailing.windestimation.aggregator.clustering.ManeuverClusteringBasedWindEstimationTrackImpl;
import com.sap.sailing.windestimation.aggregator.hmm.BestPathsCalculator;
import com.sap.sailing.windestimation.aggregator.hmm.IntersectedWindRangeBasedTransitionProbabilitiesCalculator;
import com.sap.sailing.windestimation.aggregator.hmm.ManeuverSequenceGraph;
import com.sap.sailing.windestimation.aggregator.outlierremoval.MeanBasedOutlierRemovalWindEstimator;
import com.sap.sailing.windestimation.aggregator.outlierremoval.NeighborBasedOutlierRemovalWindEstimator;
import com.sap.sailing.windestimation.classifier.maneuver.ManeuverWithEstimatedType;
import com.sap.sailing.windestimation.classifier.maneuver.ManeuverWithProbabilisticTypeClassification;
import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.windinference.MiddleCourseBasedTwdCalculatorImpl;

public class ManeuverClassificationsAggregatorFactory {

    private final PolarDataService polarDataService;

    public ManeuverClassificationsAggregatorFactory(PolarDataService polarDataService) {
        this.polarDataService = polarDataService;
    }

    public ManeuverClassificationsAggregator hmm() {
        return new ManeuverSequenceGraph(
                new BestPathsCalculator(new IntersectedWindRangeBasedTransitionProbabilitiesCalculator()));
    }

    public ManeuverClassificationsAggregator clustering() {
        return new ManeuverClassificationsAggregator() {

            @Override
            public List<ManeuverWithEstimatedType> aggregateManeuverClassifications(
                    RaceWithEstimationData<ManeuverWithProbabilisticTypeClassification> race) {
                BoatClass boatClass = race.getCompetitorTracks().isEmpty() ? null
                        : race.getCompetitorTracks().get(0).getBoatClass();
                ManeuverClusteringBasedWindEstimationTrackImpl windEstimator = new ManeuverClusteringBasedWindEstimationTrackImpl(
                        race, boatClass, polarDataService, 30000);
                try {
                    windEstimator.initialize();
                    List<ManeuverWithEstimatedType> tackManeuvers = windEstimator.getTackClusters().stream()
                            .flatMap(cluster -> cluster.stream())
                            .map(maneuverClassification -> new ManeuverWithEstimatedType(
                                    ((ManeuverClassificationForClusteringImpl) maneuverClassification).getManeuver(),
                                    ManeuverTypeForClassification.TACK,
                                    maneuverClassification.getLikelihoodForManeuverType(ManeuverType.TACK)))
                            .collect(Collectors.toList());
                    List<ManeuverWithEstimatedType> jibeManeuvers = windEstimator.getJibeClusters().stream()
                            .flatMap(cluster -> cluster.stream())
                            .map(maneuverClassification -> new ManeuverWithEstimatedType(
                                    ((ManeuverClassificationForClusteringImpl) maneuverClassification).getManeuver(),
                                    ManeuverTypeForClassification.JIBE,
                                    maneuverClassification.getLikelihoodForManeuverType(ManeuverType.JIBE)))
                            .collect(Collectors.toList());
                    List<ManeuverWithEstimatedType> result = new ArrayList<>();
                    result.addAll(tackManeuvers);
                    result.addAll(jibeManeuvers);
                    Collections.sort(result, (one, two) -> one.getManeuver().getManeuverTimePoint()
                            .compareTo(two.getManeuver().getManeuverTimePoint()));
                    return result;
                } catch (NotEnoughDataHasBeenAddedException e) {
                    return Collections.emptyList();
                }
            }
        };
    }

    public ManeuverClassificationsAggregator meanOutlier() {
        return new MeanBasedOutlierRemovalWindEstimator(new MiddleCourseBasedTwdCalculatorImpl());
    }

    public ManeuverClassificationsAggregator neighborOutlier() {
        return new NeighborBasedOutlierRemovalWindEstimator(new MiddleCourseBasedTwdCalculatorImpl());
    }

}
