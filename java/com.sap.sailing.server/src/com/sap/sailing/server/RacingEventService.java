package com.sap.sailing.server;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RegattaRegistry;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationIdentifier;
import com.sap.sailing.domain.base.configuration.DeviceConfigurationMatcher;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.RaceFetcher;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaFetcher;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.LeaderboardRegistry;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogStartTimeEvent;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.TrackerManager;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.server.masterdata.DataImportLockWithProgress;

/**
 * An OSGi service that can be used to track boat races using a TracTrac connector that pushes
 * live GPS boat location, waypoint, coarse and mark passing data.<p>
 * 
 * If a race/regatta is already being tracked, another {@link #addTracTracRace(URL, URI, URI, WindStore, long)} or
 * {@link #addRegatta(URL, URI, URI, WindStore, long)} call will have no effect, even if a different
 * {@link WindStore} is requested.<p>
 * 
 * When the tracking of a race/regatta is {@link #stopTracking(Regatta, RaceDefinition) stopped}, the next
 * time it's started to be tracked, a new {@link TrackedRace} at least will be constructed. This also
 * means that when a {@link TrackedRegatta} exists that still holds other {@link TrackedRace}s, the
 * no longer tracked {@link TrackedRace} will be removed from the {@link TrackedRegatta}.
 * corresponding information is removed also from the {@link DomainFactory}'s caches to ensure that
 * clean, fresh data is received should another tracking request be issued later.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface RacingEventService extends TrackedRegattaRegistry, RegattaFetcher, RegattaRegistry, RaceFetcher,
        LeaderboardRegistry, TrackerManager {
    @Override
    Regatta getRegatta(RegattaName regattaName);

    @Override
    RaceDefinition getRace(RegattaAndRaceIdentifier raceIdentifier);

    DynamicTrackedRace getTrackedRace(Regatta regatta, RaceDefinition race);

    DynamicTrackedRace getTrackedRace(RegattaAndRaceIdentifier raceIdentifier);

    /**
     * Obtains an unmodifiable map of the leaderboard configured in this service keyed by their names.
     */
    Map<String, Leaderboard> getLeaderboards();

    /**
     * @return a leaderboard whose {@link Leaderboard#getName()} method returns the value of the <code>name</code>
     *         parameter, or <code>null</code> if no such leaderboard is known to this service
     */
    Leaderboard getLeaderboardByName(String name);

    /**
     * Stops tracking all races of the regatta specified. This will also stop tracking wind for all races of this regatta.
     * See {@link #stopTrackingWind(Regatta, RaceDefinition)}. If there were multiple calls to
     * {@link #addTracTracRace(URL, URI, URI, WindStore, long)} with an equal combination of URLs/URIs, the {@link TracTracRaceTracker}
     * already tracking the race was re-used. The trackers will be stopped by this call regardless of how many calls
     * were made that ensured they were tracking.
     */
    void stopTracking(Regatta regatta) throws MalformedURLException, IOException, InterruptedException;

    /**
     * Removes <code>race</code> and any corresponding {@link #getTrackedRace(Regatta, RaceDefinition) tracked race}
     * from this service. If it was the last {@link RaceDefinition} in its {@link Regatta} and the regatta
     * {@link Regatta#isPersistent() is not stored persistently}, the <code>regatta</code> is removed as well and will no
     * longer be returned by {@link #getAllRegattas()}. The wind tracking is stopped for <code>race</code>.
     * <p>
     * 
     * Any {@link RaceTracker} for which <code>race</race> is the last race tracked that is still reachable
     * from {@link #getAllRegattas()} will be {@link RaceTracker#stop() stopped}.
     * 
     * The <code>race</code> will be also removed from all leaderboards containing a column that has <code>race</code>'s
     * {@link #getTrackedRace(Regatta, RaceDefinition) corresponding} {@link TrackedRace} as its
     * {@link RaceColumn#getTrackedRace(Fleet)}.
     * 
     * @param regatta
     *            the regatta from which to remove the race
     * @param race
     *            the race to remove
     */
    void removeRace(Regatta regatta, RaceDefinition race) throws MalformedURLException, IOException,InterruptedException;

    /**
     * @param port
     *            the UDP port on which to listen for incoming messages from Expedition clients
     * @param correctByDeclination
     *            An optional service to convert the wind bearings (which the receiver may
     *            believe to be true bearings) from magnetic to true bearings.
     * @throws SocketException
     *             thrown, e.g., in case there is already another listener on the port requested
     */
    void startTrackingWind(Regatta regatta, RaceDefinition race, boolean correctByDeclination) throws Exception;

    void stopTrackingWind(Regatta regatta, RaceDefinition race) throws SocketException, IOException;

    /**
     * The {@link Triple#getC() third component} of the triples returned is a wind tracker-specific
     * comment where a wind tracker may provide information such as its type name or, if applicable,
     * connectivity information such as the network port on which it receives wind information.
     */
    Iterable<Triple<Regatta, RaceDefinition, String>> getWindTrackedRaces();

    /**
     * Creates a new leaderboard with the <code>name</code> specified.
     * @param discardThresholds
     *            Tells the thresholds from which on a next higher number of worst races will be discarded per
     *            competitor. Example: <code>[3, 6]</code> means that starting from three races the single worst race
     *            will be discarded; starting from six races, the two worst races per competitor are discarded.
     * 
     * @return the leaderboard created
     */
    FlexibleLeaderboard addFlexibleLeaderboard(String leaderboardName, String leaderboardDisplayName, int[] discardThresholds, ScoringScheme scoringScheme, Serializable courseAreaId);

    RegattaLeaderboard addRegattaLeaderboard(RegattaIdentifier regattaIdentifier, String leaderboardDisplayName, int[] discardThresholds);

    void removeLeaderboard(String leaderboardName);

    /**
     * Renames a leaderboard. If a leaderboard by the name <code>oldName</code> does not exist in {@link #getLeaderboards()},
     * or if a leaderboard with the name <code>newName</code> already exists, an {@link IllegalArgumentException} is thrown.
     * If the method completes normally, the rename has been successful, and the leaderboard previously obtained by calling
     * {@link #getLeaderboardByName(String) getLeaderboardByName(oldName)} can now be obtained by calling
     * {@link #getLeaderboardByName(String) getLeaderboardByName(newName)}.
     */
    void renameLeaderboard(String oldName, String newName);

    RaceColumn addColumnToLeaderboard(String columnName, String leaderboardName, boolean medalRace);

    void moveLeaderboardColumnUp(String leaderboardName, String columnName);

    void moveLeaderboardColumnDown(String leaderboardName, String columnName);

    void removeLeaderboardColumn(String leaderboardName, String columnName);

    void renameLeaderboardColumn(String leaderboardName, String oldColumnName, String newColumnName);

    /**
     * @see RaceColumn#setFactor(Double)
     */
    void updateLeaderboardColumnFactor(String leaderboardName, String columnName, Double factor);

    /**
     * Updates the leaderboard data in the persistent store
     */
    void updateStoredLeaderboard(Leaderboard leaderboard);

    void updateStoredRegatta(Regatta regatta);

    void stopTrackingAndRemove(Regatta regatta) throws MalformedURLException, IOException, InterruptedException;

    /**
     * Removes the regatta as well as all regatta leaderboards for that regatta
     */
    void removeRegatta(Regatta regatta) throws MalformedURLException, IOException, InterruptedException;
    
    /**
     * Removes the given series
     */
    void removeSeries(Series series) throws MalformedURLException, IOException, InterruptedException;

    DynamicTrackedRace getExistingTrackedRace(RegattaAndRaceIdentifier raceIdentifier);

    /**
     * Obtains an unmodifiable map of the leaderboard groups configured in this service keyed by their names.
     */
    Map<String, LeaderboardGroup> getLeaderboardGroups();

    /**
     * @param groupName The name of the requested leaderboard group
     * @return The leaderboard group with the name <code>groupName</code>, or <code>null</code> if theres no such group
     */
    LeaderboardGroup getLeaderboardGroupByName(String groupName);

    /**
     * Creates a new group with the name <code>groupName</code>, the description <code>desciption</code> and the
     * leaderboards with the names in <code>leaderboardNames</code> and saves it in the database.
     * 
     * @param groupName
     *            The name of the new group
     * @param description
     *            The description of the new group
     * @param displayGroupsInReverseOrder TODO
     * @param leaderboardNames
     *            The names of the leaderboards, which should be contained by the new group.<br />
     *            If there isn't a leaderboard with one of these names an {@link IllegalArgumentException} is thrown.
     * @return The new leaderboard group
     */
    LeaderboardGroup addLeaderboardGroup(String groupName, String description, boolean displayGroupsInReverseOrder,
            List<String> leaderboardNames, int[] overallLeaderboardDiscardThresholds, ScoringSchemeType overallLeaderboardScoringSchemeType);

    /**
     * Removes the group with the name <code>groupName</code> from the service and the database.
     * @param groupName The name of the group which shall be removed.
     */
    void removeLeaderboardGroup(String groupName);

    /**
     * Renames the group with the name <code>oldName</code> to the <code>newName</code>.<br />
     * If there's no group with the name <code>oldName</code> or there's already a group with the name
     * <code>newName</code> a {@link IllegalArgumentException} is thrown.
     * 
     * @param oldName The old name of the group
     * @param newName The new name of the group
     */
    void renameLeaderboardGroup(String oldName, String newName);

    /**
     * Updates the group data in the persistant store.
     */
    void updateStoredLeaderboardGroup(LeaderboardGroup leaderboardGroup);

    DynamicTrackedRace createTrackedRace(RegattaAndRaceIdentifier raceIdentifier, WindStore windStore,
            long delayToLiveInMillis, long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed);

    Regatta getOrCreateDefaultRegatta(String regattaBaseName, String boatClassName, Serializable id);

    /**
     * @param series the series must not have any {@link RaceColumn}s yet
     */
    Regatta createRegatta(String regattaBaseName, String boatClassName, Serializable id, Iterable<? extends Series> series, boolean persistent, ScoringScheme scoringScheme, Serializable defaultCourseAreaId);
    
    Regatta updateRegatta(RegattaIdentifier regattaIdentifier, Serializable newDefaultCourseAreaId, RegattaConfiguration regattaConfiguration, Iterable<? extends Series> series);

    /**
     * Adds <code>raceDefinition</code> to the {@link Regatta} such that it will appear in {@link Regatta#getAllRaces()}
     * and {@link Regatta#getRaceByName(String)}.
     * 
     * @param addToRegatta identifier of an regatta that must exist already
     */
    void addRace(RegattaIdentifier addToRegatta, RaceDefinition raceDefinition);

    void updateLeaderboardGroup(String oldName, String newName, String description, List<String> leaderboardNames,
            int[] overallLeaderboardDiscardThresholds, ScoringSchemeType overallLeaderboardScoringSchemeType);

    /**
     * Executes an operation whose effects need to be replicated to any replica of this service known and
     * {@link OperationExecutionListener#executed(RacingEventServiceOperation) notifies} all registered
     * operation execution listeners about the execution of the operation.
     */
    <T> T apply(RacingEventServiceOperation<T> operation);

    void addOperationExecutionListener(OperationExecutionListener listener);

    void removeOperationExecutionListener(OperationExecutionListener listener);

    /**
     * Produces a one-shot serializable copy of those elements required for replication into <code>oos</code> so that
     * afterwards the {@link RacingEventServiceOperation}s can be {@link #apply(RacingEventServiceOperation) applied} to
     * maintain consistency with the master copy of the service. The dual operation is {@link #initiallyFillFrom}.
     */
    void serializeForInitialReplication(ObjectOutputStream oos) throws IOException;

    /**
     * Dual, reading operation for {@link #serializeForInitialReplication(ObjectOutputStream)}. In other words, when
     * this operation returns, this service instance is in a state "equivalent" to that of the service instance that
     * produced the stream contents in its {@link #serializeForInitialReplication(ObjectOutputStream)}. "Equivalent"
     * here means that a replica will have equal sets of regattas, tracked regattas, leaderboards and leaderboard groups but
     * will not have any active trackers for wind or positions because it relies on these elements to be sent through
     * the replication channel.
     * <p>
     * 
     * Tracked regattas read from the stream are observed (see {@link RaceListener}) by this object for automatic updates
     * to the default leaderboard and for automatic linking to leaderboard columns. It is assumed that no explicit
     * replication of these operations will happen based on the changes performed on the replication master.<p>
     * 
     * <b>Caution:</b> All relevant contents of this service instance will be replaced by the stream contents.
     * @throws InterruptedException 
     */
    void initiallyFillFrom(ObjectInputStream ois) throws IOException, ClassNotFoundException, InterruptedException;

    /**
     * @return a thread-safe copy of the events currently known by the service; it's safe for callers to iterate over
     *         the iterable returned, and no risk of a {@link ConcurrentModificationException} exists
     */
    Iterable<Event> getAllEvents();

    /**
     * Returns the event with given id. When no event is found, <b>null</b> is returned.
     * 
     * @param id
     * 			The id of the event.
     * @return The event with given id.
     */
    Event getEvent(Serializable id);

    /**
     * Creates a new event with the name <code>eventName</code>, the venue<code>venue</code> and the
     * regattas with the names in <code>regattaNames</code> and saves it in the database.
     * 
     * @param eventName
     *            The name of the new event
     * @param publicationUrl
     *            The publication URL of the new event
     * @param isPublic
     *            Indicates whether the event is public accessible via the publication URL or not
     * @param id
     *            The id of the new event
     * @param venue
     *            The name of the venue of the new event
     * @return The new event
     */
    Event addEvent(String eventName, String venueName, String publicationUrl, boolean isPublic, UUID id);

    /**
     * Updates a sailing event with the name <code>eventName</code>, the venue<code>venue</code> and the
     * regattas with the names in <code>regattaNames</code> and updates it in the database.
     * @param id TODO
     * @param eventName
     *            The name of the event to update
     * @param venueName
     *            The name of the venue of the event
     * @param publicationUrl
     *            The publication URL of the event
     * @param isPublic
     *            Indicates whether the event is public accessible via the publication URL or not
     * @param regattaNames
     *            The names of the regattas contained in the event.<br />
     * 
     * @return The new event
     */
    void updateEvent(UUID id, String eventName, String venueName, String publicationUrl, boolean isPublic, List<String> regattaNames);

    /**
     * Renames a sailing event. If a sailing event by the name <code>oldName</code> does not exist in {@link #getEvents()},
     * or if a event with the name <code>newName</code> already exists, an {@link IllegalArgumentException} is thrown.
     * If the method completes normally, the rename has been successful, and the event previously obtained by calling
     * {@link #getEventByName(String) getEventByName(oldName)} can now be obtained by calling
     * {@link #getEventByName(String) getEventByName(newName)}.
     */
    void renameEvent(UUID id, String newEventName);

    void removeEvent(UUID id);

    CourseArea addCourseArea(UUID eventId, String courseAreaName, UUID courseAreaId);

    com.sap.sailing.domain.base.DomainFactory getBaseDomainFactory();

    CourseArea getCourseArea(Serializable courseAreaId);

    void mediaTrackAdded(MediaTrack mediaTrack);

    void mediaTracksAdded(Collection<MediaTrack> mediaTracks);
    
    void mediaTrackTitleChanged(MediaTrack mediaTrack);

    void mediaTrackUrlChanged(MediaTrack mediaTrack);

    void mediaTrackStartTimeChanged(MediaTrack mediaTrack);

    void mediaTrackDurationChanged(MediaTrack mediaTrack);

    void mediaTrackDeleted(MediaTrack mediaTrack);

    Collection<MediaTrack> getMediaTracksForRace(RegattaAndRaceIdentifier regattaAndRaceIdentifier);

    Collection<MediaTrack> getAllMediaTracks();

    void reloadRaceLog(String leaderboardName, String raceColumnName, String fleetName);

    RaceLog getRaceLog(String leaderboardName, String raceColumnName, String fleetName);

    /**
     * @return a pair with the found or created regatta, and a boolean that tells whether the regatta was created during
     *         the call
     */
    Pair<Regatta, Boolean> getOrCreateRegattaWithoutReplication(String baseRegattaName, String boatClassName, Serializable id,
            Iterable<? extends Series> series, boolean persistent, ScoringScheme scoringScheme,
            Serializable defaultCourseAreaId);

    /**
     * @return map where keys are the toString() representation of the {@link RaceDefinition#getId() IDs} of races passed to
     * {@link #setRegattaForRace(Regatta, RaceDefinition)}. It helps remember the connection between races and regattas.
     */
    ConcurrentHashMap<String, Regatta> getPersistentRegattasForRaceIDs();

    /**
     * 
     * @param override If set to true, the mthod will override any existing connection
     */
    void setPersistentRegattaForRaceIDs(Regatta regatta, Iterable<String> raceIdStrings, boolean override);
    
    Event createEventWithoutReplication(String eventName, String venue, String publicationUrl, boolean isPublic,
            UUID id);

    CourseArea addCourseAreaWithoutReplication(UUID eventId, UUID courseAreaId, String courseAreaName);

    /**
     * Returns a mobile device's configuration.
     * @param identifier of the client (may include event)
     * @return the {@link DeviceConfiguration}
     */
    DeviceConfiguration getDeviceConfiguration(DeviceConfigurationIdentifier identifier);
    
    /**
     * Adds a device configuration.
     * @param matcher defining for which the configuration applies.
     * @param configuration of the device.
     */
    void createOrUpdateDeviceConfiguration(DeviceConfigurationMatcher matcher, DeviceConfiguration configuration);
    
    /**
     * Removes a configuration by its matching object.
     * @param matcher
     */
    void removeDeviceConfiguration(DeviceConfigurationMatcher matcher);

    /**
     * Returns all configurations and their matching objects. 
     * @return the {@link DeviceConfiguration}s.
     */
    Map<DeviceConfigurationMatcher, DeviceConfiguration> getAllDeviceConfigurations();

    /**
     * Forces a new start time on the RaceLog identified by the passed parameters.
     * @param leaderboardName name of the RaceLog's leaderboard.
     * @param raceColumnName name of the RaceLog's column
     * @param fleetName name of the RaceLog's fleet
     * @param authorName name of the {@link RaceLogEventAuthor} the {@link RaceLogStartTimeEvent} will be created with
     * @param authorPriority priority of the author.
     * @param passId Pass identifier of the new start time event.
     * @param logicalTimePoint logical {@link TimePoint} of the new event.
     * @param startTime the new Start-Time
     * @return
     */
    TimePoint setStartTime(String leaderboardName, String raceColumnName, String fleetName, String authorName,
            int authorPriority, int passId, TimePoint logicalTimePoint, TimePoint startTime);

    /**
     * Gets the start time and pass identifier for the queried race. Start time might be <code>null</code>.
     */
    Pair<TimePoint, Integer> getStartTime(String leaderboardName, String raceColumnName, String fleetName);

    MongoObjectFactory getMongoObjectFactory();
    
    DomainObjectFactory getDomainObjectFactory();
    
    WindStore getWindStore();

    /**
     * This lock exists to allow only one master data import at a time to avoid situation where multiple Imports
     * override each other in unpredictable fashion
     */
    DataImportLockWithProgress getDataImportLock();

    DataImportProgress createOrUpdateDataImportProgressWithReplication(UUID importOperationId,
            double overallProgressPct,
            String subProgressName, double subProgressPct);

    DataImportProgress createOrUpdateDataImportProgressWithoutReplication(UUID importOperationId,
            double overallProgressPct,
            String subProgressName, double subProgressPct);

    void setDataImportFailedWithReplication(UUID importOperationId, String errorMessage);

    void setDataImportFailedWithoutReplication(UUID importOperationId, String errorMessage);
}
