package com.sap.sailing.domain.maneuverhash.impl;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintFactory;
import com.sap.sailing.domain.tracking.TrackedRace;

public class ManeuverRaceFingerprintFactoryImpl implements ManeuverRaceFingerprintFactory {
    @Override
    public ManeuverRaceFingerprint createFingerprint(TrackedRace trackedRace) {
        return new ManeuverRaceFingerprintImpl(trackedRace);
    }

    @Override
    public ManeuverRaceFingerprint fromJson(JSONObject json) {
        return new ManeuverRaceFingerprintImpl(json);
    }
}
