package com.sap.sailing.windestimation;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JOptionPane;

import com.sap.sailing.windestimation.data.importer.AggregatedDistanceBasedTwdTransitionImporter;
import com.sap.sailing.windestimation.data.importer.AggregatedDurationBasedTwdTransitionImporter;
import com.sap.sailing.windestimation.data.importer.DistanceBasedTwdTransitionImporter;
import com.sap.sailing.windestimation.data.importer.DurationBasedTwdTransitionImporter;
import com.sap.sailing.windestimation.data.importer.ManeuverAndWindImporter;
import com.sap.sailing.windestimation.data.importer.PolarDataImporter;
import com.sap.sailing.windestimation.data.persistence.maneuver.ManeuverForEstimationPersistenceManager;
import com.sap.sailing.windestimation.data.persistence.maneuver.RegularManeuversForEstimationPersistenceManager;
import com.sap.sailing.windestimation.data.persistence.twdtransition.AggregatedSingleDimensionBasedTwdTransitionPersistenceManager.AggregatedSingleDimensionType;
import com.sap.sailing.windestimation.datavisualization.AggregatedDistanceDimensionPlot;
import com.sap.sailing.windestimation.datavisualization.AggregatedDurationDimensionPlot;
import com.sap.sailing.windestimation.model.classifier.maneuver.ManeuverClassifierTrainer;
import com.sap.sailing.windestimation.model.classifier.maneuver.PersistedManeuverClassifiersScorePrinter;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DistanceBasedTwdTransitionStdRegressorTrainer;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DurationBasedTwdTransitionStdRegressorTrainer;
import com.sap.sailing.windestimation.util.LoggingUtil;

/**
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class SimpleModelsTraining {

    private static final ExecutorService executorService = Executors.newFixedThreadPool(3);

    public static void main(String[] args) throws Exception {
        new ManeuverForEstimationPersistenceManager().dropCollection();
        new RegularManeuversForEstimationPersistenceManager().dropCollection();
        executeInThreadPool(() -> PolarDataImporter.main(args));
        ManeuverAndWindImporter.main(args);
        awaitThreadPoolCompletion();
        executeInThreadPool(() -> {
            ManeuverClassifierTrainer.main(args);
            Thread.sleep(1000);
            PersistedManeuverClassifiersScorePrinter.main(args);
        });
        executeInThreadPool(() -> {
            DurationBasedTwdTransitionImporter.main(args);
            Thread.sleep(1000);
            AggregatedDurationBasedTwdTransitionImporter.main(args);
        });
        DistanceBasedTwdTransitionImporter.main(args);
        Thread.sleep(1000);
        AggregatedDistanceBasedTwdTransitionImporter.main(args);
        awaitThreadPoolCompletion();
        do {
            AggregatedDurationDimensionPlot.main(args);
            showInfoAboutDataCleaning(AggregatedSingleDimensionType.DURATION);
            AggregatedDurationDimensionPlot.awaitWindowClosed();
        } while (JOptionPane.YES_OPTION != askDataCleaningFinished(AggregatedSingleDimensionType.DURATION));
        do {
            AggregatedDistanceDimensionPlot.main(args);
            showInfoAboutDataCleaning(AggregatedSingleDimensionType.DISTANCE);
            AggregatedDistanceDimensionPlot.awaitWindowClosed();
        } while (JOptionPane.YES_OPTION != askDataCleaningFinished(AggregatedSingleDimensionType.DISTANCE));
        DurationBasedTwdTransitionStdRegressorTrainer.main(args);
        DistanceBasedTwdTransitionStdRegressorTrainer.main(args);
        Thread.sleep(1000);
        ExportedModelsGenerator.main(args);
        LoggingUtil.logInfo("Model training finished. You can upload the generated file to a server instance.");
    }

    private static int askDataCleaningFinished(AggregatedSingleDimensionType dimension) {
        Object[] options = { "Continue with model training", "Restart Graphical Tool" };
        int res = JOptionPane.showOptionDialog(null,
                "Have you finished the data cleaning for " + dimension + " dimension?", "Continue with model training?",
                JOptionPane.DEFAULT_OPTION, JOptionPane.INFORMATION_MESSAGE, null, options, options[0]);
        switch (res) {
        case 0:
            return JOptionPane.YES_OPTION;
        case 1:
            return JOptionPane.NO_OPTION;
        }
        return JOptionPane.CLOSED_OPTION;
    }

    private static void showInfoAboutDataCleaning(AggregatedSingleDimensionType dimension) {
        JOptionPane.showMessageDialog(null, "Now, clean the data for " + dimension
                + " dimension. Remove instances from MongoDB collection \"" + dimension.getCollectioName()
                + "\" which do not make sense. E.g. values which are represented by a small number of supporting instances (see histogram), values which cause implausible zig zag sections within zero-mean standard deviation curve and etc. Close the graphical tool, when you are done to resume model training.");
    }
    
    private static void awaitThreadPoolCompletion() throws InterruptedException {
        executorService.awaitTermination(24, TimeUnit.HOURS);
        Thread.sleep(1000L);
    }

    private static void executeInThreadPool(RunnableWithExceptionsCatch runnable) {
        executorService.execute(() -> {
            try {
                runnable.run();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
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
