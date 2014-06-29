package com.sap.sailing.domain.confidence.impl;

import com.sap.sailing.domain.common.Speed;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.confidence.HasConfidenceAndIsScalable;
import com.sap.sailing.domain.common.confidence.Weigher;
import com.sap.sailing.domain.common.confidence.impl.ConfidenceBasedAveragerImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.ScalableWind;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Util;

/**
 * In order to enable the aggregation of {@link Wind} objects whose {@link WindSource}'s {@link WindSourceType} suggests
 * {@link WindSourceType#useSpeed() not to use their speed values}, we keep track separately of the confidence sum
 * of the {@link Speed} objects in this averager.<p>
 * 
 * If a {@link WindSource}'s {@link WindSourceType} suggests {@link WindSourceType#useSpeed() not to use their speed
 * values}, the separate confidence sum for the speed values is not touched, and the separate speed sum is not
 * incremented. When dividing by the confidence sum eventually, the separate speed sum is divided separately by the
 * separate speed confidence sum, and a new result object is constructed using the speed obtained this way.

 * @author Axel Uhl (d043530)
 */
public class ConfidenceBasedWindAveragerImpl<RelativeTo> extends
        ConfidenceBasedAveragerImpl<ScalableWind, Wind, RelativeTo> implements ConfidenceBasedWindAverager<RelativeTo> {

    public ConfidenceBasedWindAveragerImpl(Weigher<RelativeTo> weigher) {
        super(weigher);
    }

    @Override
    public WindWithConfidence<RelativeTo> getAverage(
            Iterable<? extends HasConfidenceAndIsScalable<ScalableWind, Wind, RelativeTo>> values, RelativeTo at) {
        boolean atLeastOneFixWasMarkedToUseSpeed = false;
        if (values == null || Util.isEmpty(values)) {
            return null;
        } else {
            ScalableWind numerator = null;
            double confidenceSum = 0;
            double speedConfidenceSum = 0;
            double knotSum = 0;
            for (HasConfidenceAndIsScalable<ScalableWind, Wind, RelativeTo> next : values) {
                double relativeWeight = (getWeigher() == null ? 1.0 : getWeigher().getConfidence(next.getRelativeTo(), at)) * next.getConfidence();
                ScalableWind weightedNext = next.getScalableValue().multiply(relativeWeight).getValue();
                double weighedNextKnots = next.getObject().getKnots() * relativeWeight;
                if (numerator == null) {
                    numerator = weightedNext;
                } else {
                    numerator = numerator.add(weightedNext);
                }
                confidenceSum += relativeWeight;
                // handle speed (without bearing) separately:
                if (weightedNext.useSpeed()) {
                    atLeastOneFixWasMarkedToUseSpeed = true;
                    speedConfidenceSum += relativeWeight;
                    knotSum += weighedNextKnots;
                }
            }
            // TODO consider greater variance to reduce the confidence
            double newConfidence = confidenceSum / Util.size(values);
            Wind preResult = numerator.divide(confidenceSum);
            // if only values with useSpeed=false were aggregated, use the original result, otherwise compute
            // separate speed average:
            Wind result;
            if (!atLeastOneFixWasMarkedToUseSpeed) {
                result = preResult;
            } else {
                result = new WindImpl(preResult.getPosition(), preResult.getTimePoint(), new KnotSpeedWithBearingImpl(
                    knotSum / speedConfidenceSum, preResult.getBearing()));
            }
            return new WindWithConfidenceImpl<RelativeTo>(result, newConfidence, at, atLeastOneFixWasMarkedToUseSpeed);
        }
    }

}
