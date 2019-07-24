package com.sap.sailing.windestimation;

import com.sap.sailing.windestimation.model.regressor.twdtransition.GaussianBasedTwdTransitionDistributionCache;
import com.sap.sailing.windestimation.model.store.ModelStore;
import com.sap.sailing.windestimation.model.store.MongoDbModelStoreImpl;
import com.sap.sse.mongodb.MongoDBService;

public class DefaultModelCaches {

    private static final ModelStore MODEL_STORE = new MongoDbModelStoreImpl(MongoDBService.INSTANCE.getDB());

    public static final GaussianBasedTwdTransitionDistributionCache GAUSSIAN_TWD_DELTA_TRANSITION_DISTRIBUTION_CACHE = new GaussianBasedTwdTransitionDistributionCache(
            MODEL_STORE, false, Long.MAX_VALUE);

    private DefaultModelCaches() {
    }

}
