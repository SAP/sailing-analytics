package com.sap.sailing.windestimation.model;

import java.util.List;

import com.sap.sailing.domain.maneuverdetection.ShortTimeAfterLastHitCache;
import com.sap.sailing.windestimation.model.store.ModelStore;
import com.sap.sailing.windestimation.model.store.PersistableModel;
import com.sap.sailing.windestimation.model.store.PersistenceContextType;

public abstract class AbstractModelCache<InstanceType, T extends ContextSpecificModelMetadata<InstanceType>, ModelType extends TrainableModel<InstanceType, T>>
        implements ModelCache<InstanceType, ModelType> {

    private final ShortTimeAfterLastHitCache<T, ModelType> modelCache;
    private final ModelLoader<InstanceType, T, ModelType> modelLoader;
    private final long preserveLoadedModelsMillis;
    private final ModelStore modelStore;
    private final ModelFactory<InstanceType, T, ModelType> modelFactory;
    private final boolean preloadAllModels;

    public AbstractModelCache(ModelStore modelStore, boolean preloadAllModels, long preserveLoadedModelsMillis,
            ModelFactory<InstanceType, T, ModelType> modelFactory) {
        this.modelStore = modelStore;
        this.preloadAllModels = preloadAllModels;
        this.preserveLoadedModelsMillis = preserveLoadedModelsMillis;
        this.modelFactory = modelFactory;
        this.modelCache = new ShortTimeAfterLastHitCache<>(preserveLoadedModelsMillis,
                contextSpecificModelMetadata -> loadModel(contextSpecificModelMetadata));
        this.modelLoader = new ModelLoader<>(modelMetadata -> modelCache.getCachedValue(modelMetadata),
                preloadAllModels ? null : modelStore, modelFactory);
        if (preloadAllModels) {
            preloadAllModels();
        }
    }

    private void preloadAllModels() {
        List<PersistableModel<?, ?>> loadedModels = modelStore.loadAllPersistedModels(getPersistenceContextType());
        for (PersistableModel<?, ?> persistableModel : loadedModels) {
            @SuppressWarnings("unchecked")
            ModelType loadedModel = (ModelType) persistableModel;
            modelCache.addToCache(loadedModel.getContextSpecificModelMetadata(), loadedModel);
        }
    }

    protected ModelType loadModel(T contextSpecificModelMetadata) {
        ModelType bestModel = modelLoader.loadBestModel(contextSpecificModelMetadata);
        return bestModel;
    }

    public ModelType getBestModel(T contextSpecificModelMetadata) {
        return modelCache.getValue(contextSpecificModelMetadata);
    }

    public ModelType getBestModel(InstanceType instance) {
        T modelMetadata = getContextSpecificModelMetadata(instance);
        ModelType bestModel = getBestModel(modelMetadata);
        return bestModel;
    }

    public abstract T getContextSpecificModelMetadata(InstanceType instance);

    public long getPreserveLoadedModelsMillis() {
        return preserveLoadedModelsMillis;
    }

    @Override
    public void clearCache() {
        modelCache.clearCache();
        if (preloadAllModels) {
            preloadAllModels();
        }
    }

    @Override
    public boolean isReady() {
        ModelType omnipresentModel = getBestModel(
                modelFactory.getContextSpecificModelMetadataWhichModelIsAlwaysPresentAndHasMinimalFeatures());
        return omnipresentModel != null;
    }

    @Override
    public PersistenceContextType getPersistenceContextType() {
        return modelFactory.getPersistenceContextType();
    }

}
