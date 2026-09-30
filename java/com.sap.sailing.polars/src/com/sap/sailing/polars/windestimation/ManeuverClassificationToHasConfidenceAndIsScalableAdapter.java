package com.sap.sailing.polars.windestimation;

import java.util.function.Function;

import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sse.common.confidence.ConfidenceBasedAverager;
import com.sap.sse.common.scalablevalue.HasConfidenceAndIsScalable;
import com.sap.sse.common.scalablevalue.ScalableValue;

/**
 * {@link ManeuverClassification}s need to be aggregated for different figures based on their confidence of being a
 * maneuver of a specific {@link ManeuverType type}. This can conveniently be done using a
 * {@link ConfidenceBasedAverager} and by adapting the {@link ManeuverClassification} objects to what the averaged
 * needs: a {@link HasConfidenceAndIsScalable} implementation.
 * <p>
 * 
 * This abstract class only contributes determining the confidence based on the likelihood for the maneuver to be of the
 * type specified. Subclasses need to add the logic for extracting the
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ManeuverClassificationToHasConfidenceAndIsScalableAdapter<ValueType, BaseType>
        implements Function<ManeuverClassification, HasConfidenceAndIsScalable<ValueType, BaseType, Void>> {
    private final Function<ManeuverClassification, ScalableValue<ValueType, BaseType>> mapper;
    private final ManeuverType maneuverType;

    /**
     * @param maneuverType
     *            confidences are defined to be the
     *            {@link PolarDataService#getManeuverLikelihoodAndTwsTwa(com.sap.sailing.domain.base.BoatClass, com.sap.sailing.domain.common.Speed, double, ManeuverType)
     *            likelihood that the maneuver is of this type}.
     * @param mapper
     *            maps the {@link ManeuverClassification} to the key figure that is a scalable value and that shall be
     *            averaged by a {@link ConfidenceBasedAverager}.
     */
    public ManeuverClassificationToHasConfidenceAndIsScalableAdapter(ManeuverType maneuverType,
            Function<ManeuverClassification, ScalableValue<ValueType, BaseType>> mapper) {
        this.maneuverType = maneuverType;
        this.mapper = mapper;
    }

    @Override
    public HasConfidenceAndIsScalable<ValueType, BaseType, Void> apply(final ManeuverClassification t) {
        return new HasConfidenceAndIsScalable<ValueType, BaseType, Void>() {
            private static final long serialVersionUID = -5617237614367649676L;

            @Override
            public ScalableValue<ValueType, BaseType> getScalableValue() {
                return mapper.apply(t);
            }

            @Override
            public double getConfidence() {
                return t.getLikelihoodForManeuverType(maneuverType);
            }

            @Override
            public Void getRelativeTo() {
                return null;
            }

            @Override
            public BaseType getObject() {
                return getScalableValue().divide(1);
            }
        };
    }

}
