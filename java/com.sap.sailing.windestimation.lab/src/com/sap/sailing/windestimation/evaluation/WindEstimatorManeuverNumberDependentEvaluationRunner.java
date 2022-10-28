package com.sap.sailing.windestimation.evaluation;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
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

public class WindEstimatorManeuverNumberDependentEvaluationRunner {
    private static final Logger logger = Logger
            .getLogger(WindEstimatorManeuverNumberDependentEvaluationRunner.class.getName());

    private static final Integer MAX_RACES = null;
    private static final int MAX_MANEUVERS = 10;
    private static final boolean EVALUATE_PER_COMPETITOR_TRACK = true;
    private static final boolean ENABLE_MARKS_INFORMATION = false;
    private static final boolean ENABLE_SCALED_SPEED = false;
    private static final boolean ENABLE_POLARS = true;
    private static final double MIN_CORRECT_ESTIMATIONS_RATIO_FOR_CORRECT_RACE = 0.75;
    private static final double MAX_TWS_DEVIATION_PERCENT = 0.2;
    private static final int MAX_TWD_DEVIATION_DEG = 20;
    private static final EvaluatableWindEstimationImplementation WIND_ESTIMATION_IMPLEMENTATION = EvaluatableWindEstimationImplementation.MST_HMM;

    private static final File csvFile = new File(
            "maneuverNumberDependentEvaluation" + WIND_ESTIMATION_IMPLEMENTATION + ".csv");

    public static void main(String[] args) throws Exception {
        logger.info("Connecting to MongoDB");
        RaceWithCompleteManeuverCurvePersistenceManager persistenceManager = new RaceWithCompleteManeuverCurvePersistenceManager();
        logger.info("Loading polar data");
        PolarDataService polarService = PolarDataServiceAccessUtil.getPersistedPolarService();
        logger.info("Wind estimator evaluation started...");
        ModelStore modelStore = new MongoDbModelStoreImpl(persistenceManager.getDb());
        WindEstimatorFactories estimatorFactories = new WindEstimatorFactories(polarService,
                new ManeuverFeatures(ENABLE_POLARS, ENABLE_SCALED_SPEED, ENABLE_MARKS_INFORMATION), modelStore);
        double[] avgErrorDegreesPerManeuverCount = new double[MAX_MANEUVERS];
        double[] avgConfidencePerManeuverCount = new double[MAX_MANEUVERS];
        double[] avgConfidenceOfCorrectEstimationsPerManeuverCount = new double[MAX_MANEUVERS];
        double[] avgConfidenceOfIncorrectEstimationsPerManeuverCount = new double[MAX_MANEUVERS];
        double[] accuracyCorrectRacesPerManeuverCount = new double[MAX_MANEUVERS];
        double[] accuracyCorrectManeuversPerManeuverCount = new double[MAX_MANEUVERS];
        double[] emptyEstimationsPercentagePerManeuverCount = new double[MAX_MANEUVERS];
        for (int fixedNumberOfManeuvers = 1; fixedNumberOfManeuvers <= MAX_MANEUVERS; fixedNumberOfManeuvers++) {
            logger.info("Running evaluation with " + fixedNumberOfManeuvers + " maneuvers");
            WindEstimationEvaluator<CompleteManeuverCurveWithEstimationData> evaluator = new WindEstimationEvaluatorImpl<>(
                    MAX_TWD_DEVIATION_DEG, MAX_TWS_DEVIATION_PERCENT, MIN_CORRECT_ESTIMATIONS_RATIO_FOR_CORRECT_RACE,
                    EVALUATE_PER_COMPETITOR_TRACK, MAX_MANEUVERS, true, fixedNumberOfManeuvers);
            PersistedElementsIterator<RaceWithEstimationData<CompleteManeuverCurveWithEstimationData>> racesIterator = persistenceManager
                    .getIterator(persistenceManager.getFilterQueryForYear(2018, false));
            if (MAX_RACES != null) {
                racesIterator = racesIterator.limit(MAX_RACES);
            }
            WindEstimatorEvaluationResult evaluationResult = evaluator.evaluateWindEstimator(
                    estimatorFactories.get(WIND_ESTIMATION_IMPLEMENTATION),
                    new TargetWindFromCompleteManeuverCurveWithEstimationDataExtractor(), racesIterator,
                    racesIterator.getNumberOfElements());
            int i = fixedNumberOfManeuvers - 1;
            avgErrorDegreesPerManeuverCount[i] = evaluationResult
                    .getAvgAbsWindCourseErrorInDegreesOfCorrectAndIncorrectWindDirectionEstimations();
            avgConfidencePerManeuverCount[i] = evaluationResult
                    .getAvgConfidenceOfCorrectAndIncorrectWindDirectionEstimations();
            avgConfidenceOfCorrectEstimationsPerManeuverCount[i] = evaluationResult
                    .getAvgConfidenceOfCorrectWindDirectionEstimations();
            avgConfidenceOfIncorrectEstimationsPerManeuverCount[i] = evaluationResult
                    .getAvgConfidenceOfIncorrectWindDirectionEstimations();
            accuracyCorrectRacesPerManeuverCount[i] = evaluationResult.getAccuracyOfWindDirectionEstimation();
            accuracyCorrectManeuversPerManeuverCount[i] = evaluationResult
                    .getPercentageOfCorrectlyEstimatedManeuverTypes();
            emptyEstimationsPercentagePerManeuverCount[i] = evaluationResult
                    .getPercentageOfEmptyWindDirectionEstimations();
        }
        logger.info("Wind estimator evaluation finished with the following provided arguments:\r\n"
                + buildConfigurationString() + "\r\n");
        toCsv(avgErrorDegreesPerManeuverCount, avgConfidencePerManeuverCount,
                avgConfidenceOfCorrectEstimationsPerManeuverCount, avgConfidenceOfIncorrectEstimationsPerManeuverCount,
                accuracyCorrectRacesPerManeuverCount, accuracyCorrectManeuversPerManeuverCount,
                emptyEstimationsPercentagePerManeuverCount);
    }

    private static String buildConfigurationString() throws IllegalArgumentException, IllegalAccessException {
        StringBuilder str = new StringBuilder();
        for (Field field : WindEstimatorManeuverNumberDependentEvaluationRunner.class.getDeclaredFields()) {
            str.append("\t- ");
            str.append(field.getName());
            str.append(": \t ");
            str.append(field.get(null) + "\r\n");
        }
        return str.toString();
    }

    public static void toCsv(double[] avgErrorDegreesPerManeuverCount, double[] avgConfidencePerManeuverCount,
            double[] avgConfidenceOfCorrectEstimationsPerManeuverCount,
            double[] avgConfidenceOfIncorrectEstimationsPerManeuverCount, double[] accuracyCorrectRacesPerManeuverCount,
            double[] accuracyCorrectManeuversPerManeuverCount, double[] emptyEstimationsPercentagePerManeuverCount)
            throws IOException {
        try (FileWriter out = new FileWriter(csvFile)) {
            String line = "Number of maneuvers; Accuracy (correct races); Accuracy (correct maneuvers); Percentage of empty estimations; Avg. confidence; Avg. confidence (correct races); Avg. confidence (incorrect races); Avg. error in degrees\r\n";
            System.out.println(line);
            out.write(line);
            for (int i = 0; i < MAX_MANEUVERS; i++) {
                line = (i + 1) + ";" + +accuracyCorrectRacesPerManeuverCount[i] + ";"
                        + accuracyCorrectManeuversPerManeuverCount[i] + ";"
                        + emptyEstimationsPercentagePerManeuverCount[i] + ";" + avgConfidencePerManeuverCount[i] + ";"
                        + avgConfidenceOfCorrectEstimationsPerManeuverCount[i] + ";"
                        + avgConfidenceOfIncorrectEstimationsPerManeuverCount[i] + ";"
                        + avgErrorDegreesPerManeuverCount[i] + "\r\n";
                System.out.println(line);
                out.write(line);
            }
        }
        logger.info("CSV with evaluation results have been stored in: " + csvFile.getAbsolutePath());
    }

}
