package com.sap.sse.common.confidence;

import java.io.Serializable;

import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.scalablevalue.ScalableValue;

/**
 * Some values, particularly those obtained from real-world measurements, are not always accurate. Some values are
 * derived by interpolating or extrapolating data series obtained through measurement or even estimation. Some values
 * are simply guessed by humans and entered into the system.
 * <p>
 * 
 * All those values have a certain level of confidence. In case multiple sources of information about the same entity or
 * phenomenon are available, knowing the confidence of each value helps in weighing and averaging these values more
 * properly than would be possible without a confidence value.
 * <p>
 * 
 * In simple cases, the type used to compute a weighed average over the things equipped with a confidence level is the
 * same as the type of the things themselves. In particular, this is the case for scalar types such as {@link Double}
 * and {@link Distance}. For non-scalar values, averaging may be non-trivial. For example, averaging a bearing cannot
 * simply happen by computing the arithmetic average of the bearing's angles. Instead, an intermediate structure
 * providing the sinus and cosinus values of the bearing's angle is used to compute the weighed average tangens.
 * <p>
 * 
 * Generally, the relationship between the type implementing this interface, the <code>ValueType</code> and the
 * <code>AveragesTo</code> type is this: an instance of the implementing type can transform itself into a
 * {@link ScalableValue} which is then used for computing a weighed sum. The values' weight is their
 * {@link #getConfidence() confidence}. The sum (which is still a {@link ScalableValue} because
 * {@link ScalableValue#add(ScalableValue)} returns again a {@link ScalableValue}) is then
 * {@link ScalableValue#divide(double) divided} by the sum of the confidences. This "division" is expected to
 * produce an object of type <code>AveragesTo</code>. Usually, <code>AveragesTo</code> would be the same as the class
 * implementing this interface.
 * 
 * @author Axel Uhl (d043530)
 * 
 * @param <ValueType>
 *            the type of the scalable value used for scalar operations during aggregation
 * @param <RelativeTo>
 *            the type of the object relative to which the confidence applies; for example, if the
 *            base type is a position and the <code>RelativeTo</code> type is {@link TimePoint},
 *            the confidence of the position is relative to a certain time point. Together with
 *            a correspondingly-typed weigher, such a value with confidence can be aggregated with
 *            other values, again relative to (maybe a different) time point.
 */
public interface HasConfidence<ValueType, BaseType, RelativeTo> extends Serializable {
    /**
     * A confidence is a number between 0.0 and 1.0 (inclusive) where 0.0 means that the value is randomly guessed while
     * 1.0 means the value is authoritatively known for a fact. It represents the weight with which a value is to be
     * considered by averaging, interpolation and extrapolation algorithms.
     * <p>
     * 
     * An averaging algorithm for a sequence of <code>n</code> tuples <code>(v1, c1), ..., (vn, cn)</code> of a value
     * <code>vi</code> with a confidence <code>ci</code> each can for example look like this:
     * <code>a := (c1*v1 + ... + cn*vn) / (c1 + ... + cn)</code>. For a single value with a confidence this trivially
     * results in <code>c1*v1 / (c1)</code> which is equivalent to <code>v1</code>. As another example, consider two
     * values with equal confidence <code>0.8</code>. Then, <code>a := (0.8*v1 + 0.8*vn) / (0.8 + 0.8)</code> which
     * resolves to <code>0.5*v1 + 0.5*v2</code> which is obviously the arithmetic mean of the two values. If one value
     * has confidence <code>0.8</code> and the other <code>0.4</code>, then
     * <code>a := (0.8*v1 + 0.4*vn) / (0.8 + 0.4)</code> which resolves to <code>2/3*v1 + 1/3*v2</code> which is a
     * weighed average.
     * <p>
     * 
     * Note, that this doesn't exactly take facts for facts. In other words, if one value is provided with a confidence
     * of <code>1.0</code>, the average may still be influenced by other values. However, this cleanly resolves otherwise
     * mutually contradictory "facts" such a <code>(v1, 1.0), (v2, 1.0)</code> with <code>v1 != v2</code>. It is
     * considered bad practice to claim a fact as soon as it results from any kind of measurement or estimation. All
     * measurement devices produce some statistical errors, no matter how small (cf. Heisenberg ;-) ).
     */
    double getConfidence();
    
    /**
     * The confidence attached to a value is usually relative to some reference point, such as a time point or
     * a position. For example, when a <code>GPSFixTrack</code> is asked to deliver an estimation for the tracked item's
     * {@link Position} at some given {@link TimePoint}, the track computes some average from a number of GPS fixes. The resulting
     * position has a certain confidence, depending on the time differences between the fixes and the time point for which
     * the position estimation was requested. The result therefore carries this reference time point for which the estimation
     * was requested so that when the result is to be used in further estimations it is clear relative to which time point the
     * confidence is to be interpreted.<p>
     * 
     * In this context, a single GPS fix is a measurement whose values may also have a confidence attached. This confidence could be
     * regarded as relative to the fix's time point.
     */
    RelativeTo getRelativeTo();

    /**
     * The object annotated by a confidence
     */
    BaseType getObject();
}
