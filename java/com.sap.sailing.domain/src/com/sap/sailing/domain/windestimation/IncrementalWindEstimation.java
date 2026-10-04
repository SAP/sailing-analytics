package com.sap.sailing.domain.windestimation;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;

/**
 * Wind estimator which interacts with maneuver detector and maintains a wind track with estimated wind fixes. All in
 * all, this instance can be seen as a wind source with corresponding wind track. The managed wind track is prone to
 * changes which are communicated to tracked race via its
 * {@link TrackedRace#recordWind(com.sap.sailing.domain.common.Wind, WindSource, boolean)} and
 * {@link TrackedRace#removeWind(com.sap.sailing.domain.common.Wind, WindSource)}.
 *
 * @author Vladislav Chumak (D069712)
 *
 */
public interface IncrementalWindEstimation extends WindEstimationInteraction {

    /**
     * @return The wind source assigned to this wind track
     */
    WindSource getWindSource();

    /**
     * Gets the produced wind track of this wind estimation
     */
    WindTrack getWindTrack();

    /**
     * Feeds already-classified {@link Maneuver}s (typically loaded from the persistent maneuver
     * cache after a fingerprint match, or produced by prior maneuver detection on this race)
     * into the estimator. Unlike
     * {@link WindEstimationInteraction#newManeuverSpotsDetected(Competitor, Iterable, com.sap.sailing.domain.maneuverdetection.TrackTimeInfo)},
     * this hand-off skips the MST/HMM classification stages because the maneuver's type
     * ({@link Maneuver#getType()}) is already known; the estimator only needs to convert each
     * maneuver into a wind fix and merge those fixes into its wind track.
     * <p>
     *
     * The typical caller is
     * {@link com.sap.sailing.domain.tracking.impl.TrackedRaceImpl#setWindEstimation}: when a
     * previously-null wind estimation transitions to a non-null one on a race whose maneuvers
     * are already known (from DB load or from a prior computation), the maneuvers can be fed
     * directly to the newly-installed estimator without re-running detection. See bug6241.
     * <p>
     *
     * The default implementation does nothing, so existing implementors of
     * {@link IncrementalWindEstimation} that don't need this hook remain source- and
     * binary-compatible.
     *
     * @param competitor
     *            the competitor on whose track the maneuvers occurred; must not be {@code null}
     * @param maneuvers
     *            the already-classified maneuvers to fold into the estimator's wind track; may
     *            be empty; must not be {@code null}. Only maneuvers whose
     *            {@link Maneuver#getType() type} maps to a classifiable maneuver type
     *            (TACK, JIBE, HEAD_UP, BEAR_AWAY) contribute a wind fix; others are skipped.
     */
    default void alreadyClassifiedManeuversAvailable(Competitor competitor, Iterable<Maneuver> maneuvers) {
    }

    /**
     * Allows test set-ups to wait until this estimator is done in case it performs tasks
     * asynchronously.
     */
    default void waitUntilDone() throws InterruptedException {}

}
