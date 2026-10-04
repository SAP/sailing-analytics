package com.sap.sailing.domain.markpassinghash;

import com.sap.sailing.domain.markpassinghash.impl.MarkPassingRaceFingerprintFactoryImpl;
import com.sap.sailing.domain.tracking.RaceFingerprintFactory;

/**
 * Factory for the creation of a {@link MarkPassingRaceFingerprint}.
 *
 * @author Fabian Kallenbach (i550803)
 */
public interface MarkPassingRaceFingerprintFactory extends RaceFingerprintFactory<MarkPassingRaceFingerprint> {
    MarkPassingRaceFingerprintFactory INSTANCE = new MarkPassingRaceFingerprintFactoryImpl();
}