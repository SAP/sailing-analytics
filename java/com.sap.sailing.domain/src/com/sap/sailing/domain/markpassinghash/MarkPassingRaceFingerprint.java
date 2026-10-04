package com.sap.sailing.domain.markpassinghash;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceFingerprint;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * An instance of this class represents a composite fingerprint of those components of a {@link TrackedRace} that are
 * relevant for computing
 * {@link TrackedRace#getMarkPassing(com.sap.sailing.domain.base.Competitor, com.sap.sailing.domain.base.Waypoint) mark
 * passings} in the {@link MarkPassingCalculator}. It can be {@link #matches(TrackedRace) matched} against a
 * {@link TrackedRace} instance to see whether the {@link TrackedRace} will produce a set of {@link MarkPassing}s equal
 * to that of the {@link TrackedRace} from which this fingerprint was produced.
 * <p>
 * 
 * To produce a fingerprint from a {@link TrackedRace} or from a JSON representation use
 * {@link MarkPassingRaceFingerprintFactory}.
 * <p>
 * 
 * The {@link #equals(Object)} and {@link #hashCode()} methods are defined based on the contents of this fingerprint.
 * 
 * @author Fabian Kallenbach (i550803)
 * @author Axel Uhl (d043530)
 */
public interface MarkPassingRaceFingerprint extends RaceFingerprint {
    /**
     * Returns a {@link JSONObject} of the hash values.
     */
    JSONObject toJson();

    /**
     * Incrementally computes the composite fingerprint of the {@code trackedRace} and compares to this fingerprint
     * component by component. The fingerprint components are computed in ascending order of computational complexity,
     * trying to fail early / fast.<p>
     * 
     * The implementation may require to obtain the race's {@link Course#lockForRead() read lock}. Note that conversely
     * updates to the course that happen under the course's write lock will trigger listeners which may synchronize
     * on certain objects, such as the {@link MarkPassingCalculator}. Therefore, should this method be called
     * while holding an object monitor ("synchronized") that a course update listener may also require, make
     * sure to first obtain at least the course read lock before invoking this method. See also bug 5803.
     * 
     * @return {@code true} if the {@code trackedRace} produces a fingerprint equal to this one if passed to
     *         {@link MarkPassingRaceFingerprintFactory#createFingerprint(TrackedRace)}
     */
    boolean matches(TrackedRace trackedRace);
}