package com.sap.sailing.domain.tracking;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;

/**
 * Factory for the creation of a {@link MarkPassingRaceFingerprint}.
 *
 * @author Fabian Kallenbach (i550803)
 */
public interface RaceFingerprintFactory<RFP extends RaceFingerprint> {
    /**
     * Creates a {@link RaceFingerprint} out of a given {@link TrackedRace}.
     */
    RFP createFingerprint(TrackedRace trackedRace);

    /**
     * Creates a {@link RaceFingerprint} out of a given {@link JSONObject}, as produced by
     * {@link RaceFingerprint#toJson()}.
     */
    RFP fromJson(JSONObject json);
}