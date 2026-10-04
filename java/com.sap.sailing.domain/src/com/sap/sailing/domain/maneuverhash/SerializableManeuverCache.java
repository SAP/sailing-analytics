package com.sap.sailing.domain.maneuverhash;

import java.io.Serializable;

public interface SerializableManeuverCache extends ManeuverCache, Serializable {
    /**
     * Should this serializable maneuver cache maintain a {@link ManeuverRaceFingerprintRegistry}, it won't be serializable.
     * Therefore, instances containing objects of this type must call this method after being de-serialized when they have
     * been wired correctly and have access to the {@link ManeuverRaceFingerprintRegistry}.
     */
    void setManeuverRaceFingerprintRegistry(ManeuverRaceFingerprintRegistry maneuverRaceFingerprintRegistry);

    void ensureFilled();
}
