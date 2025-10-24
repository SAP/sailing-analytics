package com.sap.sailing.domain.shared.tracking;

/**
 * A predicate for a fix, for use in
 * {@link Track#getInterpolatedValue(com.sap.sse.common.TimePoint, com.sap.sse.common.Util.Function)}, used, e.g., to
 * provide a rule for when a fix shall be accepted during the search for surrounding fixes.
 * 
 * @author Axel Uhl (d043530)
 *
 * @param <FixType>
 */
public interface FixAcceptancePredicate<FixType> {
    boolean isAcceptFix(FixType fix);
}
