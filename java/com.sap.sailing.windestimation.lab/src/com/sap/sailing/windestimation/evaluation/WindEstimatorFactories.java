package com.sap.sailing.windestimation.evaluation;

import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.windestimation.PolarsFittingBasedWindEstimationComponentImpl;
import com.sap.sailing.windestimation.SimpleConfigurableManeuverBasedWindEstimationComponentImpl;
import com.sap.sailing.windestimation.WindEstimationComponent;
import com.sap.sailing.windestimation.SimpleConfigurableManeuverBasedWindEstimationComponentImpl.ManeuverClassificationsAggregatorImplementation;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.maneuverclassifier.ManeuverFeatures;
import com.sap.sailing.windestimation.preprocessing.RaceElementsFilteringPreprocessingPipelineImpl;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindEstimatorFactories {

    private final PolarDataService polarService;
    private final ManeuverFeatures maneuverFeatures;

    public WindEstimatorFactories(PolarDataService polarService, ManeuverFeatures maneuverFeatures) {
        this.polarService = polarService;
        this.maneuverFeatures = maneuverFeatures;
    }

    public WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> get(
            EvaluatableWindEstimationImplementation windEstimationImplementation) {
        switch (windEstimationImplementation) {
        case HMM:
            return hmm();
        case CLUSTERING:
            return maneuverClustering();
        case MEAN_OUTLIER:
            return meanOutlierRemoval();
        case NEIGHBOR_OUTLIER:
            return neighborOutlierRemoval();
        case POLARS_FITTING:
            return polarsFitting();
        }
        throw new IllegalArgumentException(windEstimationImplementation + " is unsupported");
    }

    public WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> hmm() {
        return new WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>>() {

            @Override
            public WindEstimationComponent<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> createNewEstimatorInstance() {
                return new SimpleConfigurableManeuverBasedWindEstimationComponentImpl(maneuverFeatures, polarService,
                        ManeuverClassificationsAggregatorImplementation.HMM);
            }

            @Override
            public String toString() {
                return "HMM";
            }
        };
    }

    public WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> maneuverClustering() {
        return new WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>>() {

            @Override
            public WindEstimationComponent<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> createNewEstimatorInstance() {
                return new SimpleConfigurableManeuverBasedWindEstimationComponentImpl(maneuverFeatures, polarService,
                        ManeuverClassificationsAggregatorImplementation.CLUSTERING);
            }

            @Override
            public String toString() {
                return "Maneuver Clustering";
            }
        };
    }

    public WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> polarsFitting() {
        return new WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>>() {

            @Override
            public WindEstimationComponent<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> createNewEstimatorInstance() {
                return new PolarsFittingBasedWindEstimationComponentImpl<>(
                        new RaceElementsFilteringPreprocessingPipelineImpl(), polarService);
            }

            @Override
            public String toString() {
                return "Polars Fitting";
            }
        };
    }

    public WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> meanOutlierRemoval() {
        return new WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>>() {

            @Override
            public WindEstimationComponent<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> createNewEstimatorInstance() {
                return new SimpleConfigurableManeuverBasedWindEstimationComponentImpl(maneuverFeatures, polarService,
                        ManeuverClassificationsAggregatorImplementation.MEAN_OUTLIER);
            }

            @Override
            public String toString() {
                return "Mean Outlier Removal";
            }
        };
    }

    public WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> neighborOutlierRemoval() {
        return new WindEstimatorFactory<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>>() {

            @Override
            public WindEstimationComponent<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> createNewEstimatorInstance() {
                return new SimpleConfigurableManeuverBasedWindEstimationComponentImpl(maneuverFeatures, polarService,
                        ManeuverClassificationsAggregatorImplementation.NEIGHBOR_OUTLIER);
            }

            @Override
            public String toString() {
                return "Neighbor Outlier Removal";
            }
        };
    }
}
