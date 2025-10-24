package com.sap.sailing.windestimation.evaluation;

import java.lang.reflect.Field;
import java.util.logging.Logger;

import com.sap.sailing.domain.maneuverdetection.CompleteManeuverCurveWithEstimationData;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.windestimation.data.RaceWithEstimationData;
import com.sap.sailing.windestimation.data.persistence.maneuver.PersistedElementsIterator;
import com.sap.sailing.windestimation.data.persistence.maneuver.RaceWithCompleteManeuverCurvePersistenceManager;
import com.sap.sailing.windestimation.data.persistence.polars.PolarDataServiceAccessUtil;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverFeatures;
import com.sap.sailing.windestimation.model.store.ModelStore;
import com.sap.sailing.windestimation.model.store.MongoDbModelStoreImpl;

public class WindEstimatorEvaluationRunner {
    private static final Logger logger = Logger.getLogger(WindEstimatorEvaluationRunner.class.getName());

    private static final Integer MAX_RACES = null;
    private static final Integer FIXED_NUMBER_OF_MANEUVERS = 10;
    private static final boolean RANDOM_CLIPPING_OF_COMPETITOR_TRACKS = true;
    private static final boolean EVALUATE_PER_COMPETITOR_TRACK = true;
    private static final boolean ENABLE_MARKS_INFORMATION = false;
    private static final boolean ENABLE_SCALED_SPEED = true;
    private static final boolean ENABLE_POLARS = true;
    private static final double MIN_CORRECT_ESTIMATIONS_RATIO_FOR_CORRECT_RACE = 0.75;
    private static final double MAX_TWS_DEVIATION_PERCENT = 0.2;
    private static final int MAX_TWD_DEVIATION_DEG = 20;
    private static final EvaluatableWindEstimationImplementation WIND_ESTIMATION_IMPLEMENTATION = EvaluatableWindEstimationImplementation.MST_HMM;

    public static void main(String[] args) throws Exception {
        WindEstimationEvaluator<CompleteManeuverCurveWithEstimationData> evaluator = new WindEstimationEvaluatorImpl<CompleteManeuverCurveWithEstimationData>(
                MAX_TWD_DEVIATION_DEG, MAX_TWS_DEVIATION_PERCENT, MIN_CORRECT_ESTIMATIONS_RATIO_FOR_CORRECT_RACE,
                EVALUATE_PER_COMPETITOR_TRACK, FIXED_NUMBER_OF_MANEUVERS == null ? 1 : FIXED_NUMBER_OF_MANEUVERS,
                RANDOM_CLIPPING_OF_COMPETITOR_TRACKS, FIXED_NUMBER_OF_MANEUVERS);
        logger.info("Connecting to MongoDB");
        RaceWithCompleteManeuverCurvePersistenceManager persistenceManager = new RaceWithCompleteManeuverCurvePersistenceManager();
        logger.info("Loading polar data");
        PolarDataService polarService = PolarDataServiceAccessUtil.getPersistedPolarService();
        logger.info("Wind estimator evaluation started...");
        ModelStore classifierModelStore = new MongoDbModelStoreImpl(persistenceManager.getDb());
        PersistedElementsIterator<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> racesIterator = persistenceManager
                .getIterator(persistenceManager.getFilterQueryForYear(2018, false));
        if (MAX_RACES != null) {
            racesIterator = racesIterator.limit(MAX_RACES);
        }
        WindEstimatorFactories estimatorFactories = new WindEstimatorFactories(polarService,
                new ManeuverFeatures(ENABLE_POLARS, ENABLE_SCALED_SPEED, ENABLE_MARKS_INFORMATION),
                classifierModelStore);
        WindEstimatorEvaluationResult evaluationResult = evaluator.evaluateWindEstimator(
                estimatorFactories.get(WIND_ESTIMATION_IMPLEMENTATION),
                new TargetWindFromCompleteManeuverCurveWithEstimationDataExtractor(), racesIterator,
                racesIterator.getNumberOfElements());
        logger.info("Wind estimator evaluation finished with the following provided arguments:\r\n"
                + buildConfigurationString() + "\r\n");
        evaluationResult.printEvaluationStatistics(true);
    }

    private static String buildConfigurationString() throws IllegalArgumentException, IllegalAccessException {
        StringBuilder str = new StringBuilder();
        for (Field field : WindEstimatorEvaluationRunner.class.getDeclaredFields()) {
            str.append("\t- ");
            str.append(field.getName());
            str.append(": \t ");
            str.append(field.get(null) + "\r\n");
        }
        return str.toString();
    }

}
