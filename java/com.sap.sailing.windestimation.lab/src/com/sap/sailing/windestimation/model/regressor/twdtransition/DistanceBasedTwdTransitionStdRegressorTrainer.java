package com.sap.sailing.windestimation.model.regressor.twdtransition;

import com.sap.sailing.windestimation.data.TwdTransition;
import com.sap.sailing.windestimation.data.persistence.twdtransition.AggregatedSingleDimensionBasedTwdTransitionPersistenceManager;
import com.sap.sailing.windestimation.data.persistence.twdtransition.AggregatedSingleDimensionBasedTwdTransitionPersistenceManager.AggregatedSingleDimensionType;
import com.sap.sailing.windestimation.model.exception.ModelPersistenceException;
import com.sap.sailing.windestimation.model.regressor.IncrementalSingleDimensionPolynomialRegressor;
import com.sap.sailing.windestimation.model.regressor.twdtransition.DistanceBasedTwdTransitionRegressorModelContext.DistanceValueRange;
import com.sap.sailing.windestimation.model.store.FileSystemModelStoreImpl;
import com.sap.sailing.windestimation.model.store.ModelDomainType;
import com.sap.sailing.windestimation.model.store.ModelStore;
import com.sap.sailing.windestimation.model.store.MongoDbModelStoreImpl;

/**
 * Trains TWD delta standard deviation by considering the distance passed between two measurements.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class DistanceBasedTwdTransitionStdRegressorTrainer extends TwdTransitionAggregatedStdRegressorTrainer {

    public DistanceBasedTwdTransitionStdRegressorTrainer(
            AggregatedSingleDimensionBasedTwdTransitionPersistenceManager persistenceManager,
            ModelStore regressorModelStore) {
        super(persistenceManager, regressorModelStore);
    }

    public static void main(String[] args) throws Exception {
        AggregatedSingleDimensionBasedTwdTransitionPersistenceManager distanceBasedPersistenceManager = new AggregatedSingleDimensionBasedTwdTransitionPersistenceManager(
                AggregatedSingleDimensionType.DISTANCE);
        final ModelStore modelStore;
        if (args.length > 0) {
            modelStore = new FileSystemModelStoreImpl(args[0]);
        } else {
            modelStore = new MongoDbModelStoreImpl(distanceBasedPersistenceManager.getDb());
        }
        train(distanceBasedPersistenceManager, modelStore);
    }

    public static void train(
            final ModelStore modelStore) throws ModelPersistenceException, Exception {
        AggregatedSingleDimensionBasedTwdTransitionPersistenceManager distanceBasedPersistenceManager = new AggregatedSingleDimensionBasedTwdTransitionPersistenceManager(
                AggregatedSingleDimensionType.DISTANCE);
        train(distanceBasedPersistenceManager, modelStore);
    }
    
    public static void train(
            AggregatedSingleDimensionBasedTwdTransitionPersistenceManager distanceBasedPersistenceManager,
            final ModelStore modelStore) throws ModelPersistenceException, Exception {
        modelStore.deleteAll(ModelDomainType.DISTANCE_BASED_TWD_DELTA_STD_REGRESSOR);
        DistanceBasedTwdTransitionRegressorModelFactory distanceBasedTwdTransitionRegressorModelFactory = new DistanceBasedTwdTransitionRegressorModelFactory();
        for (DistanceValueRange distanceValueRange : DistanceValueRange.values()) {
            DistanceBasedTwdTransitionRegressorModelContext modelContext = new DistanceBasedTwdTransitionRegressorModelContext(
                    distanceValueRange);
            IncrementalSingleDimensionPolynomialRegressor<TwdTransition, DistanceBasedTwdTransitionRegressorModelContext> model = distanceBasedTwdTransitionRegressorModelFactory
                    .getNewModel(modelContext);
            DistanceBasedTwdTransitionStdRegressorTrainer trainer = new DistanceBasedTwdTransitionStdRegressorTrainer(
                    distanceBasedPersistenceManager, modelStore);
            trainer.trainRegressor(model);
        }
    }

}
