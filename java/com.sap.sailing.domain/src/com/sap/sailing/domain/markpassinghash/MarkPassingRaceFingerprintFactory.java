package com.sap.sailing.domain.markpassinghash;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.markpassinghash.impl.MarkPassingRaceFingerprintFactoryImpl;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * Factory for the creation of a {@link MarkPassingRaceFingerprint}.
 *
 * @author Fabian Kallenbach (i550803)
 */
public interface MarkPassingRaceFingerprintFactory {
    MarkPassingRaceFingerprintFactory INSTANCE = new MarkPassingRaceFingerprintFactoryImpl();

    /**
     * Creates a {@link MarkPassingRaceFingerprint} out of a given {@link TrackedRace}.
     */
    MarkPassingRaceFingerprint createFingerprint(TrackedRace trackedRace);

    /**
     * Creates a {@link MarkPassingRaceFingerprint} out of a given {@link JSONObject}, as produced by
     * {@link MarkPassingRaceFingerprint#toJson()}.
     */
    MarkPassingRaceFingerprint fromJson(JSONObject json);
}