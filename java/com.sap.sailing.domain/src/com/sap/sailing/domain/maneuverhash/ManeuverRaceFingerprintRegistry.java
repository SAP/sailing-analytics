package com.sap.sailing.domain.maneuverhash;

import java.util.List;
import java.util.Map;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.TrackedRace;

public interface ManeuverRaceFingerprintRegistry {
    void storeManeuvers(RaceIdentifier raceIdentifier, ManeuverRaceFingerprint fingerprint, Map<Competitor, List<Maneuver>> Maneuvers, Course course);
 
    ManeuverRaceFingerprint getManeuverRaceFingerprint(RaceIdentifier raceIdentifier);
    
    Map<Competitor, List<Maneuver>> loadManeuvers(TrackedRace trackedRace, Course course);
    
    void removeStoredManeuvers(RaceIdentifier raceIdentifier);
}
