package com.sap.sailing.domain.maneuverhash;

import com.sap.sailing.domain.maneuverhash.impl.ManeuverRaceFingerprintFactoryImpl;
import com.sap.sailing.domain.tracking.RaceFingerprintFactory;

public interface ManeuverRaceFingerprintFactory extends RaceFingerprintFactory<ManeuverRaceFingerprint> {
    ManeuverRaceFingerprintFactory INSTANCE = new ManeuverRaceFingerprintFactoryImpl();
}
