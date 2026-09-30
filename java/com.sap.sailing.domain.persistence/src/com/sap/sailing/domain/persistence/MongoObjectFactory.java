package com.sap.sailing.domain.persistence;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bson.Document;

import com.mongodb.DBObject;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.anniversary.DetailedRaceInfo;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.base.SailingServerConfiguration;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.dto.AnniversaryType;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sse.common.Util.Pair;

/**
 * Offers methods to construct {@link DBObject MongoDB objects} from domain objects.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface MongoObjectFactory {
    /**
     * Registers for changes of the wind coming from <code>windSource</code> on the <code>trackedRace</code>. Each
     * update received will be appended to the MongoDB and can later be retrieved. The key used to identify the race is
     * the {@link RaceDefinition#getName() race name} and the {@link Regatta#getName() regatta name}.
     */
    void addWindTrackDumper(TrackedRegatta trackedRegatta, TrackedRace trackedRace, WindSource windSource);

    /**
     * Stores the configuration data of <code>leaderboard</code> in the Mongo DB associated with this
     * factory. 
     */
    void storeLeaderboard(Leaderboard leaderboard);
    
    /**
     * Removes the leaderboard named <code>name</code> from the database.
     */
    void removeLeaderboard(String leaderboardName);

    /**
     * Stores the group, if it doesn't exist or updates it. Leaderboards in the group, which aren't stored in the
     * database, will be stored. If the leaderboard group has an {@link LeaderboardGroup#getOverallLeaderboard() overall
     * leaderboard}, it will be stored / updated as well.
     */
    void storeLeaderboardGroup(LeaderboardGroup leaderboardGroup);
    
    /**
     * Removes the group with the ID <code>leaderboardGroupId</code> from the database.
     */
    void removeLeaderboardGroup(UUID leaderboardGroupId);

    /**
     * Renames the group with the ID <code>leaderboardGroupId</code>.
     */
    void renameLeaderboardGroup(UUID leaderboardGroupId, String newName);

    /**
     * Stores the event with its name, venue and the venue's course areas, as well as the links to the
     * {@link Event#getLeaderboardGroups() leaderboard groups associated with the event}. These links are stored
     * separately and are not loaded by the corresponding {@link DomainObjectFactory#loadEvent(String)} call again. The
     * rationale behind this is that the events are usually loaded as the first thing in a server. The leaderboard
     * groups are not yet loaded at that time, so we usually cannot establish the links at the time the event is loaded.
     * Instead, a separate call, {@link DomainObjectFactory#loadLeaderboardGroupLinksForEvents}, is required after both,
     * the events and the leaderboard groups have been loaded, to establish the links again.
     * <p>
     * 
     * The regattas obtained by {@link Event#getRegattas()} are <em>not</em> stored by this call. They need to be stored
     * separately by calls to {@link #storeRegatta} where a reference to their owning event is stored.
     * <p>
     * 
     * 
     */
    void storeEvent(Event event);

    /**
     * Renames the event with the name <code>oldName</code>.
     */
    void renameEvent(Serializable id, String newName);

    /**
     * Removes the event named <code>eventName</code> from the database.
     */
    void removeEvent(Serializable id);

    void storeServerConfiguration(SailingServerConfiguration serverConfiguration);

    /**
     * Stores a registered sailing server 
     * @param serves the servers to store
     */
    void storeSailingServer(RemoteSailingServerReference server);

    /**
     * Stores the list of selected events for remote sailing server. If <code>include</code> parameter is not set then
     * all events are loaded, if it's set to <code>true</code> then selected events are loaded inclusively, if to
     * <code>false</code> then exclusively.
     * 
     * @param name
     *            to get target sailing server by
     * @param include
     *            the flag determining whether to load events inclusively or exclusively
     * @param selectedEventIds
     *            the list of event id's to exclude from being loading
     */
    void updateSailingServer(String name, boolean include, Set<UUID> selectedEventIds);

    void removeSailingServer(String name);

    /**
     * Stores the regatta together with its name, {@link Series} definitions and an optional link to the
     * {@link Event} to which the regatta belongs.
     * @param oldSeriesNameNewName 
     */
    void storeRegatta(Regatta regatta);

    void removeRegatta(Regatta regatta);

    void storeRegattaForRaceID(String id, Regatta regatta);

    void removeRegattaForRaceID(String raceIDAsString, Regatta regatta);

    /**
     * Stores a competitor. This should not be done for competitors for which
     * the master data is supplied by other systems, such as TracTrac, but rather for smartphone tracking,
     * where this data is otherwise not recoverable.
     * @param competitor the competitor to store/update in the database
     */
    void storeCompetitor(Competitor competitor);

    /**
     * Like {@link #storeCompetitor(Competitor)}, but for a collection of competitors that are all
     * expected to be new, having a unique {@link Competitor#getId() ID}.
     */
    void storeCompetitors(Iterable<? extends Competitor> competitors);

    void removeAllCompetitors();

    void removeCompetitor(Competitor competitor);
    
    /**
     * Stores a boat. This should not be done for boats for which
     * the master data is supplied by other systems, such as TracTrac, but rather for smartphone tracking,
     * where this data is otherwise not recoverable.
     * @param boat the boat to store/update in the database
     */
    void storeBoat(Boat boat);

    /**
     * Like {@link #storeBoat(Boat)}, but for a collection of boats that are all
     * expected to be new, having a unique {@link Boat#getId() ID}.
     */
    void storeBoats(Iterable<? extends Boat> boats);

    void removeAllBoats();

    void removeBoat(Boat boat);

    MongoDatabase getDatabase();

    void storeDeviceConfiguration(DeviceConfiguration configuration);

    void removeDeviceConfiguration(UUID id);
    
    void removeRaceLog(RaceLogIdentifier identifier);
    
    void removeAllRaceLogs();
    
    void removeRegattaLog(RegattaLikeIdentifier identifier);
    
    void removeAllRegattaLogs();

    void storeResultUrl(String resultProviderName, URL url);

    void removeResultUrl(String resultProviderName, URL url);

    
    /**
     * Updates the database such that the next call to
     * {@link DomainObjectFactory#loadConnectivityParametersForRacesToRestore(Consumer<RaceTrackingConnectivityParameter>)} won't return an object equivalent to
     * {@code params} anymore; in other words, the race whose connectivity parameters are described by {@code params}
     * will no longer be considered as to be restored.
     * 
     * @see #addConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters)
     */
    void removeConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters params) throws MalformedURLException;
    
    /**
     * Updates the database such that the next call to
     * {@link DomainObjectFactory#loadConnectivityParametersForRacesToRestore(Consumer<RaceTrackingConnectivityParameter>)} will return an object equivalent to
     * {@code params}; in other words, the race whose connectivity parameters are described by {@code params} will be
     * considered as to be restored.
     * 
     * @see #removeConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters)
     */
    void addConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters params);

    /**
     * Removes all {@link RaceTrackingConnectivityParameters} objects from those to restore; short for calling
     * {@link #removeConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters)} for all parameter
     * objects obtained through {@link DomainObjectFactory#loadConnectivityParametersForRacesToRestore(Consumer<RaceTrackingConnectivityParameter>)}.
     */
    void removeAllConnectivityParametersForRacesToRestore();

    /**
     * Stores determined Anniversary races.
     */
    void storeAnniversaryData(ConcurrentHashMap<Integer, Pair<DetailedRaceInfo, AnniversaryType>> knownAnniversaries);

    Document storeWind(Wind wind);

    /**
     * Stores a race's mark passings persistently.
     * 
     * @param raceIdentifier
     *            identifies the race to which the mark passings belong
     * @param fingerprint
     *            a composite fingerprint of the race in the state at which the mark passings were captured
     */
    void storeMarkPassings(RaceIdentifier raceIdentifier, MarkPassingRaceFingerprint fingerprint, Map<Competitor, Map<Waypoint, MarkPassing>> markPassings, Course course);
    
    void removeMarkPassings(RaceIdentifier raceIdentifier);
    
    void storeManeuvers(RaceIdentifier raceIdentifier, ManeuverRaceFingerprint fingerprint, Course course, Map<Competitor, List<Maneuver>> maneuvers);
    
    void removeManeuvers(RaceIdentifier raceIdentifier);
}
