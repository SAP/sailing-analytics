package com.sap.sailing.domain.maneuverhash;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.RaceFingerprint;

/**
 * Contains fingerprint data that encodes the state of the race relevant for computing all {@link Maneuver}s
 * for all {@link Competitor}s.
 */
public interface ManeuverRaceFingerprint extends RaceFingerprint {
}
