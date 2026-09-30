package com.sap.sailing.domain.maneuverhash.impl;

import java.util.Set;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.maneuverdetection.impl.ManeuverDetectorImpl;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.markpassinghash.impl.MarkPassingRaceFingerprintImpl;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.common.Position;
import com.sap.sse.common.Util;

public class ManeuverRaceFingerprintImpl extends MarkPassingRaceFingerprintImpl implements ManeuverRaceFingerprint {
    private final int windHash;
    private final int detectorVersion;

    private static enum JSON_FIELDS {
        WIND_HASH, DETECTOR_VERSION
    };

    public ManeuverRaceFingerprintImpl(TrackedRace trackedRace) {
        super(trackedRace);
        this.detectorVersion = ManeuverDetectorImpl.DETECTOR_VERSION;
        this.windHash = calculateWindHash(trackedRace);
    }

    public ManeuverRaceFingerprintImpl(JSONObject json) {
        super(json);
        this.detectorVersion =  ((Number) json.get(JSON_FIELDS.DETECTOR_VERSION.name())).intValue();
        this.windHash = ((Number) json.get(JSON_FIELDS.WIND_HASH.name())).intValue();
    }

    @Override
    public JSONObject toJson() {
        final JSONObject result = super.toJson();
        result.put(JSON_FIELDS.DETECTOR_VERSION.name(), detectorVersion);
        result.put(JSON_FIELDS.WIND_HASH.name(), windHash);
        return result;
    }

    @Override
    public boolean matches(TrackedRace trackedRace) {
        boolean result = super.matches(trackedRace);
        if (result) {
            if (!Util.equalsWithNull(detectorVersion, ManeuverDetectorImpl.DETECTOR_VERSION)) {
                result = false;
            } else if (windHash != calculateWindHash(trackedRace)) {
                result = false;
            }
        }
        return result;
    }

    private int calculateWindHash(TrackedRace trackedRace) {
        final Set<WindSource> windSoures = trackedRace.getWindSources();
        int res = 0;
        final Set<WindSource> windSouresToExclude = trackedRace.getWindSourcesToExclude();
        for (WindSource w : windSoures) {
            if (w.getType().isObserved() && !windSouresToExclude.contains(w)) {
                final WindTrack windTrack = trackedRace.getOrCreateWindTrack(w);
                windTrack.lockForRead();
                try {
                    int k = w.getId() == null ? 0 : w.getId().hashCode();
                    int v = 0;
                    for (Wind wf : trackedRace.getOrCreateWindTrack(w).getFixes()) {
                        final Position wfPosition = wf.getPosition();
                        v = v + (int) ((wfPosition==null?0:wfPosition.getLatDeg()) + (wfPosition==null?0:wfPosition.getLngDeg())
                                + wf.getKilometersPerHour());
                    }
                    res = res ^ k;
                    res = res ^ v;
                } finally {
                    windTrack.unlockAfterRead();
                }
            }
        }
        return res;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + detectorVersion;
        result = prime * result + windHash;
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        if (!super.equals(obj))
            return false;
        ManeuverRaceFingerprintImpl other = (ManeuverRaceFingerprintImpl) obj;
        if (detectorVersion != other.detectorVersion)
            return false;
        if (windHash != other.windHash)
            return false;
        return true;
    }
}