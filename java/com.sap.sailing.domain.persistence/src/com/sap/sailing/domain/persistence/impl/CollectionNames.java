package com.sap.sailing.domain.persistence.impl;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;

/**
 * Defines literals providing the names for MongoDB collections. The literal documentation described the semantics
 * of the collection identified by that literal.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public enum CollectionNames {
    /**
     * Stores the wind fixes recorded from persistent wind sources.
     */
    WIND_TRACKS,
    
    /**
     * Stores the leaderboards with their names, score corrections, competitor display name overrides, race
     * columns and their fleets as well as the per-fleet tracked race assignments.
     */
    LEADERBOARDS,
    
    /**
     * Stores the leaderboard group configurations with references to the {@link #LEADERBOARDS} collection
     */
    LEADERBOARD_GROUPS,
    
    /**
     * Top-level event information about events such as Kieler Woche 2011, or IDM Travemuende 2011, including name and
     * course areas.
     */
    EVENTS,
    
    /**
     * The links that connect events to leaderboard groups; each element stores the ID of an event and a list
     * of leaderboard group IDs.
     */
    LEADERBOARD_GROUP_LINKS_FOR_EVENTS,
    
    /** 
     * Stores the registered sailing servers.
     */
    SAILING_SERVERS,

    /** 
     * Stores the configuration of the server instance.
     */
    SERVER_CONFIGURATION,

    /**
     * Stores regatta definitions including their series layout and fleets and race columns. Regattas can reference
     * the event from the {@link #EVENTS} collection to which they belong.
     */
    REGATTAS,
    
    /**
     * Stores boat class-specific master data such as the class's hull length, logo, name, number of sailors, etc.
     * To be implemented in future versions.
     */
    BOAT_CLASSES,
    
    /**
     * Stores the mapping of {@link RaceDefinition#getId race IDs} to regatta names for automatic re-association
     * when tracking races again without explicitly specifying a regatta.
     */
    REGATTA_FOR_RACE_ID, 
    
    /**
     * Legacy store for competitors before implementation of bug2822
     * Contains the old competitors with contained boats.
     */
    COMPETITORS_BAK,

    /**
     * Stores competitors with or without boat references.
     */
    COMPETITORS,

    /**
     * Stores boats.
     */
    BOATS,

    /**
     * Stores the race log events for a tracked race.
     */
    RACE_LOGS,
    
    REGATTA_LOGS,
    
    /**
     * Stores configurations for mobile devices.
     */
    CONFIGURATIONS,
    
    /**
     * Stores {@link GPSFix}es
     */
    GPS_FIXES,
    
    /**
     * Metadata for the GPSFixes, grouped by Device Identifier.
     */
    GPS_FIXES_METADATA,

    /**
     * URLs providing race results
     */
    RESULT_URLS,
    
    /**
     * DB representations of {@link RaceTrackingConnectivityParameters} objects, serialized in a type-specific
     * way by corresponding implementations of the RaceTrackingConnectivityParametersMongoHandler} interface.
     */
    CONNECTIVITY_PARAMS_FOR_RACES_TO_BE_RESTORED,
    
    /**
     * Contains the known anniversaries
     */
    ANNIVERSARIES,
    
    /**
     * Contains the hashes for the {@link MarkPassingRaceFingerprint} and the mark passings for those races
     */
    MARKPASSINGS,
    
    /**
     * Contains the hashes for the {@link ManeuverRaceFingerprint} and the maneuvers for those races
     */
    MANEUVERS;
}
