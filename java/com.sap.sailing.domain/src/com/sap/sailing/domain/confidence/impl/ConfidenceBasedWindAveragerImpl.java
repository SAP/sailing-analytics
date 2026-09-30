package com.sap.sailing.domain.confidence.impl;

import java.util.Iterator;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.confidence.impl.ScalableWind;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.confidence.ConfidenceBasedWindAverager;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Speed;
import com.sap.sse.common.confidence.Weigher;
import com.sap.sse.common.confidence.impl.ConfidenceBasedAveragerImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.scalablevalue.HasConfidenceAndIsScalable;

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

    /**
     * @param weigher
     *            If <code>null</code>, 1.0 will be assumed as default confidence for all values provided, regardless
     *            the reference point relative to which the average is to be computed
     */
    public ConfidenceBasedWindAveragerImpl(Weigher<RelativeTo> weigher) {
        super(weigher);
    }

    @Override
    public WindWithConfidence<RelativeTo> getAverage(
            Iterable<? extends HasConfidenceAndIsScalable<ScalableWind, Wind, RelativeTo>> values, RelativeTo at) {
        return getAverage(values.iterator(), at);
    }

    @Override
    public WindWithConfidence<RelativeTo> getAverage(
            Iterator<? extends HasConfidenceAndIsScalable<ScalableWind, Wind, RelativeTo>> values, RelativeTo at) {
        final WindWithConfidence<RelativeTo> result;
        boolean atLeastOneFixWasMarkedToUseSpeed = false;
        if (values == null || !values.hasNext()) {
            result = null;
        } else {
            ScalableWind numerator = null;
            double confidenceSum = 0;
            double speedConfidenceSum = 0;
            double knotSum = 0;
            int count = 0;
            while (values.hasNext()) {
                HasConfidenceAndIsScalable<ScalableWind, Wind, RelativeTo> next = values.next();
                final double relativeWeight = getWeight(next, at);
                ScalableWind weightedNext = next.getScalableValue().multiply(relativeWeight).getValue();
                final double weighedNextKnots = next.getObject().getKnots() * relativeWeight;
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
                count++;
            }
            // TODO consider greater variance to reduce the confidence
            final double newConfidence = confidenceSum / count;
            if (confidenceSum == 0) {
                result = null;
            } else {
                Wind preResult = numerator.divide(confidenceSum);
                // if only values with useSpeed=false were aggregated, use the original result, otherwise compute
                // separate speed average:
                Wind windResult;
                if (!atLeastOneFixWasMarkedToUseSpeed) {
                    windResult = preResult;
                } else {
                    windResult = new WindImpl(preResult.getPosition(), preResult.getTimePoint(), new KnotSpeedWithBearingImpl(
                        knotSum / speedConfidenceSum, preResult.getBearing()));
                }
                result = new WindWithConfidenceImpl<RelativeTo>(windResult, newConfidence, at, atLeastOneFixWasMarkedToUseSpeed);
            }
        }
        return result;
    }

}
