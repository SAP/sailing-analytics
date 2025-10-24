package com.sap.sailing.windestimation.model;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;

import org.json.simple.parser.ParseException;

import com.sap.sailing.windestimation.data.AggregatedSingleDimensionBasedTwdTransition;
import com.sap.sailing.windestimation.data.importer.AggregatedDistanceBasedTwdTransitionImporter;
import com.sap.sailing.windestimation.data.importer.AggregatedDurationBasedTwdTransitionImporter;
import com.sap.sailing.windestimation.data.importer.DistanceBasedTwdTransitionImporter;
import com.sap.sailing.windestimation.data.importer.DurationBasedTwdTransitionImporter;
import com.sap.sailing.windestimation.data.importer.ManeuverAndWindImporter;
import com.sap.sailing.windestimation.data.importer.PolarDataImporter;
import com.sap.sailing.windestimation.data.persistence.maneuver.ManeuverForEstimationPersistenceManager;
import com.sap.sailing.windestimation.data.persistence.maneuver.RegularManeuversForEstimationPersistenceManager;
import com.sap.sailing.windestimation.data.persistence.twdtransition.AggregatedSingleDimensionBasedTwdTransitionPersistenceManager;
import com.sap.sailing.windestimation.data.persistence.twdtransition.AggregatedSingleDimensionBasedTwdTransitionPersistenceManager.AggregatedSingleDimensionType;
import com.sap.sailing.windestimation.datavisualization.AggregatedDistanceDimensionPlot;
import com.sap.sailing.windestimation.datavisualization.AggregatedDurationDimensionPlot;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverClassifierTrainer;
import com.sap.sailing.windestimation.model.classifier.maneuver.PersistedManeuverClassifiersScorePrinter;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DistanceBasedTwdTransitionRegressorModelContext;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DistanceBasedTwdTransitionRegressorModelContext.DistanceValueRange;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DistanceBasedTwdTransitionStdRegressorTrainer;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DurationBasedTwdTransitionRegressorModelContext;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DurationBasedTwdTransitionRegressorModelContext.DurationValueRange;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DurationBasedTwdTransitionStdRegressorTrainer;
import com.sap.sailing.windestimation.model.store.FileSystemModelStoreImpl;
import com.sap.sailing.windestimation.model.store.ModelStore;
import com.sap.sailing.windestimation.model.store.MongoDbModelStoreImpl;
import com.sap.sailing.windestimation.util.LoggingUtil;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class SimpleModelsTrainingPart1 {
    private static final Logger logger = Logger.getLogger(SimpleModelsTrainingPart1.class.getName());

    private static final int NUMBER_OF_THREADS = 15;
    private static ExecutorService executorService;

    /**
     * @param args
     *            {@code args[0]} must contain a valid bearer token that authenticates a user with {@code EXPORT}
     *            permission on the {@code TRACKED_RACE}s for wind data access. Only regattas/races are considered that
     *            the user authenticated by this token can {@code READ}. {@code args[1]} may contain a percentage of the
     *            maneuvers to use for training which defaults to 80; {@code args[2]} may contain a percentage of the
     *            maneuvers to use for testing which defaults to {@code 100-percentForTraining}. If {@code args[3]} is
     *            also provided, it is taken to be the file system path for storing the models that result from the
     *            training process; with this, the models are not stored in MongoDB which otherwise would be the
     *            default. If {@code args[4]} is provided and is something that {@link Boolean#valueOf(String)}
     *            evaluates to {@code true} then visuals are presented (requiring a display to be available to the Java
     *            process which may, e.g., not be the case for a docker container) that show the results of outlier
     *            removal for the wind regressions.
     */
    public static void main(String[] args) throws Exception {
        final String bearerToken = args[0];
        final int percentForTraining;
        final int percentForTesting;
        if (args.length > 1) {
            percentForTraining = Integer.valueOf(args[1]);
        } else {
            percentForTraining = 80;
        }
        if (args.length > 2) {
            percentForTesting = Integer.valueOf(args[2]);
        } else {
            percentForTesting = 100-percentForTraining;
        }
        final ManeuverForEstimationPersistenceManager maneuverForEstimationPersistenceManager = new ManeuverForEstimationPersistenceManager();
        final ModelStore modelStore;
        if (args.length > 3) {
            modelStore = new FileSystemModelStoreImpl(args[3]);
        } else {
            modelStore = new MongoDbModelStoreImpl(maneuverForEstimationPersistenceManager.getDb());
        }
        maneuverForEstimationPersistenceManager.dropCollection();
        new RegularManeuversForEstimationPersistenceManager().dropCollection();
        logger.info("Scheduling import of polar data");
        executeInThreadPool(() -> new PolarDataImporter().importPolarData(bearerToken));
        logger.info("Scheduling import of regattas with a bearer token of length "+bearerToken.length());
        executeInThreadPool(() -> new ManeuverAndWindImporter().importAllRegattas(bearerToken));
        awaitThreadPoolCompletion();
        executeInThreadPool(() -> {
            ManeuverClassifierTrainer.train(percentForTraining, percentForTesting, modelStore);
            Thread.sleep(1000);
            new PersistedManeuverClassifiersScorePrinter().printManeuverClassifiersScore();
        });
        executeInThreadPool(() -> {
            new DistanceBasedTwdTransitionImporter().importDistanceBasedTwdTransition();
        });
        executeInThreadPool(() -> {
            new DurationBasedTwdTransitionImporter().importDurationBasedTwdTransition();
        });
        awaitThreadPoolCompletion();
        executeInThreadPool(() -> {
            new AggregatedDurationBasedTwdTransitionImporter().importAggregatedDurationBasedTwdTransition();
        });
        executeInThreadPool(() -> {
            new AggregatedDistanceBasedTwdTransitionImporter().importAggregatedDistanceBasedTwdTransition();
        });
        awaitThreadPoolCompletion();
        // The following code would open pop-up windows that display charts of original TWD regressions before cleansing:
        final boolean showCharts = args.length > 4 && Boolean.valueOf(args[4]);
        if (showCharts) {
            AggregatedDurationDimensionPlot.main(args);
            showInfoAboutIntervalAdjustments(DurationBasedTwdTransitionRegressorModelContext.class, DurationValueRange.class);
            AggregatedDistanceDimensionPlot.main(args);
            showInfoAboutIntervalAdjustments(DistanceBasedTwdTransitionRegressorModelContext.class, DistanceValueRange.class);
        } else {
            enforceMonotonicZeroMeanSigmaGrowth(AggregatedSingleDimensionType.DURATION);
            enforceMonotonicZeroMeanSigmaGrowth(AggregatedSingleDimensionType.DISTANCE);
        }
        DurationBasedTwdTransitionStdRegressorTrainer.train(modelStore);
        DistanceBasedTwdTransitionStdRegressorTrainer.train(modelStore);
        Thread.sleep(1000);
        new ExportedModelsGenerator().export(modelStore);
        LoggingUtil.logInfo("Model training finished. You can upload the generated file to a server instance.");
    }

    static void enforceMonotonicZeroMeanSigmaGrowth(AggregatedSingleDimensionType dimensionType) throws UnknownHostException, JsonDeserializationException, ParseException {
        final AggregatedSingleDimensionBasedTwdTransitionPersistenceManager persistenceManager = new AggregatedSingleDimensionBasedTwdTransitionPersistenceManager(dimensionType);
        final List<AggregatedSingleDimensionBasedTwdTransition> allAggregatedElements = persistenceManager.getAllElements();
        Collections.sort(allAggregatedElements, (ae1, ae2)->Double.compare(ae1.getDimensionValue(), ae2.getDimensionValue()));
        double previousZeroMeanSigma = -1;
        for (final AggregatedSingleDimensionBasedTwdTransition aggregate : allAggregatedElements) {
            if (aggregate.getZeroMeanStd() >= previousZeroMeanSigma) {
                previousZeroMeanSigma = aggregate.getZeroMeanStd();
            } else {
                logger.info("Removing aggregate for dimension value "+aggregate.getDimensionValue()+" (dimension "+dimensionType.name()+
                        ") to achieve monotonic growth. The previous zeroMeanSigma value was "+previousZeroMeanSigma+
                        "; the aggregate for "+aggregate.getDimensionValue()+" has "+aggregate.getZeroMeanStd());
                persistenceManager.remove(aggregate);
            }
        }
    }

    private static void showInfoAboutIntervalAdjustments(Class<?> classToAdjust, Class<?> valueRangeEnum) {
        JOptionPane.showMessageDialog(null, "Now, open the source code of the class \"" + classToAdjust.getName()
                + "\".\nScroll down to the definition of the inner enum \"" + valueRangeEnum.getSimpleName()
                + "\",\nread its JavaDoc and adjust its interval definitions so that\neach interval can be learned by the adjusted regressor model configuration\nwith minimal error.\n\nPress OK after you are done.");
    }

    private static void awaitThreadPoolCompletion() throws InterruptedException {
        logger.info("Awaiting thread pool completion");
        executorService.shutdown();
        final long TIMEOUT_IN_HOURS = 48;
        boolean success = executorService.awaitTermination(TIMEOUT_IN_HOURS, TimeUnit.HOURS);
        if (!success) {
            logger.severe("Thread-pool was terminated after "+TIMEOUT_IN_HOURS+
                    " hours waiting time. Launching next step. You may, e.g., be seeing an empty chart in case the process is really still running."+
                    " In this case, please follow the log and keep refreshing until you see content.");
        } else {
            logger.info("Thread pool completed");
        }
        Thread.sleep(1000L);
    }

    private static void executeInThreadPool(RunnableWithExceptionsCatch runnable) {
        if (executorService == null || executorService.isShutdown()) {
            createNewThreadPool();
        }
        executorService.execute(() -> {
            try {
                runnable.run();
            } catch (Throwable t) {
                logger.log(Level.SEVERE, "FAILURE: Caught unexpected exception. Model training aborted", t);
                System.exit(1);
            }
        });
    }

    private static void createNewThreadPool() {
        executorService = ThreadPoolUtil.INSTANCE.createForegroundTaskThreadPoolExecutor(NUMBER_OF_THREADS,
                SimpleModelsTrainingPart1.class.getName());
    }

    /**
     * 
     * @author Vladislav Chumak (D069712)
     *
     */
    private static interface RunnableWithExceptionsCatch {
        void run() throws Exception;
    }

}
