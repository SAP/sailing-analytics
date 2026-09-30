package com.sap.sailing.windestimation.model.regressor.twdtransition;

import com.sap.sailing.windestimation.data.ManeuverTypeForClassification;
import com.sap.sailing.windestimation.data.TwdTransition;
import com.sap.sailing.windestimation.model.ModelFactory;
import com.sap.sailing.windestimation.model.regressor.IncrementalSingleDimensionPolynomialRegressor;
import com.sap.sailing.windestimation.model.regressor.RegressorModelFactory;
import com.sap.sse.common.Duration;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MeterDistance;

/**
 * Base class for {@link DistanceBasedTwdTransitionRegressorModelFactory} and
 * {@link DurationBasedTwdTransitionRegressorModelFactory} containing common implementation of {@link ModelFactory}.
 * 
 * @author Vladislav Chumak (D069712)
 *
 * @param <MC>
 *            The type of model context associated with models which are constructed by this factory instance.
 */
public abstract class SingleDimensionBasedTwdTransitionRegressorModelFactory<MC extends SingleDimensionBasedTwdTransitionRegressorModelContext>
        implements RegressorModelFactory<TwdTransition, MC> {

    @Override
    public IncrementalSingleDimensionPolynomialRegressor<TwdTransition, MC> getNewModel(MC modelContext) {
        IncrementalSingleDimensionPolynomialRegressor<TwdTransition, MC> regressorModel = new IncrementalSingleDimensionPolynomialRegressor<>(
                modelContext, modelContext.getSupportedDimensionValueRange().getPolynomialDegree(),
                modelContext.getSupportedDimensionValueRange().isWithBias());
        return regressorModel;
    }

    /**
     * Creates a new model context for the provided instance.
     */
    public abstract MC createNewModelContext(TwdTransition twdTransition);

    @Override
    public MC getModelContextWhichModelAreAlwaysPresent() {
        TwdTransition twdTransition = new TwdTransition(new MeterDistance(100), Duration.ONE_MINUTE,
                new DegreeBearingImpl(5), ManeuverTypeForClassification.TACK, ManeuverTypeForClassification.TACK);
        MC modelContext = createNewModelContext(twdTransition);
        return modelContext;
    }

}
