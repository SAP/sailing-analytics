package com.sap.sailing.server.interfaces;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.security.auth.Subject;

import org.apache.commons.math.FunctionEvaluationException;
import org.apache.commons.math.MaxIterationsExceededException;
import org.apache.shiro.authz.UnauthorizedException;

import com.sap.sailing.competitorimport.CompetitorProvider;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.anniversary.DetailedRaceInfo;
import com.sap.sailing.domain.anniversary.SimpleRaceInfo;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.LeaderboardSearchResult;
import com.sap.sailing.domain.base.LeaderboardSearchResultBase;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RegattaRegistry;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.base.SailingServerConfiguration;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.impl.DynamicCompetitorWithBoat;
import com.sap.sailing.domain.common.CompetitorDescriptor;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.DataImportSubProgress;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RaceFetcher;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaFetcher;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.RegattaName;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.WindFinderReviewedSpotsCollectionIdProvider;
import com.sap.sailing.domain.common.dto.AnniversaryType;
import com.sap.sailing.domain.common.impl.MasterDataImportObjectCreationCountImpl;
import com.sap.sailing.domain.common.media.MediaTrack;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.common.racelog.tracking.DoesNotHaveRegattaLogException;
import com.sap.sailing.domain.common.racelog.tracking.MarkAlreadyUsedInRaceException;
import com.sap.sailing.domain.leaderboard.EventResolver;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.leaderboard.LeaderboardRegistry;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithOtherTieBreakingLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoringScheme;
import com.sap.sailing.domain.leaderboard.impl.DelegatingRegattaLeaderboardWithCompetitorElimination;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.tracking.SensorFixStoreSupplier;
import com.sap.sailing.domain.ranking.RankingMetricConstructor;
import com.sap.sailing.domain.regattalike.LeaderboardThatHasRegattaLike;
import com.sap.sailing.domain.resultimport.ResultUrlProvider;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.statistics.Statistics;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParametersHandler;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.TrackerManager;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTracker;
import com.sap.sailing.server.operationaltransformation.RemoveEvent;
import com.sap.sse.common.Distance;
import com.sap.sse.common.PairingListCreationException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.TypeBasedServiceFinderFactory;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.search.Result;
import com.sap.sse.common.search.Searchable;
import com.sap.sse.filestorage.FileStorageManagementService;
import com.sap.sse.pairinglist.PairingList;
import com.sap.sse.pairinglist.PairingListTemplate;
import com.sap.sse.replication.ReplicableWithObjectInputStream;
import com.sap.sse.security.SecurityService;
import com.sap.sse.shared.media.ImageDescriptor;
import com.sap.sse.shared.media.VideoDescriptor;

/**
 * An OSGi service that can be used to track boat races using a TracTrac connector that pushes live GPS boat location,
 * waypoint, coarse and mark passing data.
 * <p>
 * 
 * If a race/regatta is already being tracked, another {@link #addTracTracRace(URL, URI, URI, WindStore, long)} or
 * {@link #addRegatta(URL, URI, URI, WindStore, long)} call will have no effect, even if a different {@link WindStore}
 * is requested.
 * <p>
 * 
 * When the tracking of a race/regatta is {@link #stopTracking(Regatta, RaceDefinition) stopped}, the next time it's
 * started to be tracked, a new {@link TrackedRace} at least will be constructed. This also means that when a
 * {@link TrackedRegatta} exists that still holds other {@link TrackedRace}s, the no longer tracked {@link TrackedRace}
 * will be removed from the {@link TrackedRegatta}. corresponding information is removed also from the
 * {@link DomainFactory}'s caches to ensure that clean, fresh data is received should another tracking request be issued
 * later.
 * <p>
 * 
 * During receiving the initial load for a replication in {@link #initiallyFillFromInternal(java.io.ObjectInputStream)},
 * tracked regattas read from the stream are observed (see {@link RaceListener}) by this object for automatic updates to
 * the default leaderboard and for automatic linking to leaderboard columns. It is assumed that no explicit replication
 * of these operations will happen based on the changes performed on the replication master.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface RacingEventService extends TrackedRegattaRegistry, RegattaFetcher, RegattaRegistry, MarkPassingRaceFingerprintRegistry,
        RaceFetcher, LeaderboardRegistry, EventResolver, LeaderboardGroupResolver, TrackerManager,
        Searchable<LeaderboardSearchResult, KeywordQueryWithOptionalEventQualification>,
        ReplicableWithObjectInputStream<RacingEventService, RacingEventServiceOperation<?>>, RaceLogAndTrackedRaceResolver,
        SensorFixStoreSupplier, WindFinderReviewedSpotsCollectionIdProvider {
    @Override
    Regatta getRegatta(RegattaName regattaName);

    @Override
    RaceDefinition getRace(RegattaAndRaceIdentifier raceIdentifier);

    /**
     * Looks for a {@link DynamicTrackedRace} inside the {@link TrackedRegatta} identified by {@code regatta},
     * keyed by the race definition {@code race}. If no such race is found, e.g., because it is still in its "loading"
     * phase and the {@link TrackedRace} hasn't been created and added to the {@link TrackedRegatta} yet, or because
     * it is in the process of being removed, with the {@link TrackedRace} already gone but the {@link RaceDefinition}
     * still available (see also bug 5982), this method will return {@code null} without any waiting or blocking.
     */
    DynamicTrackedRace getTrackedRace(Regatta regatta, RaceDefinition race);

    /**
     * This method does not for the tracked race to appear. If any of regatta, race definition or tracked race are not
     * found, {@code null} is returned.
     */
    DynamicTrackedRace getTrackedRace(RegattaAndRaceIdentifier raceIdentifier);
    
    /**
     * Traverses through the event's {@link Event#getLeaderboardGroups() leaderboard groups} and from there on to the
     * {@link Leaderboard}s and finds }
     * 
     * @param at
     *            the time point that must be between a {@link TrackedRace}'s {@link TrackedRace#getStartOfTracking()
     *            start of tracking} and {@link TrackedRace#getEndOfTracking() end of tracking} for the tracked race to
     *            be returned
     */
    Iterable<TrackedRace> getAllTrackedRacesForEventTrackingAt(Event event, TimePoint at);

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
     * Looks at the mark tracks in the tracked races attached to the <code>leaderboard</code> and tries to find a
     * track for the <code>mark</code> requested there which has fixes before and after <code>timePoint</code> (to
     * ensure that no track cropping has taken place, removing the fixes for the interesting time period). Note, that
     * no lookup is performed in the {@link RegattaLog} of the {@code leaderboard}, so mark pings that have not yet
     * been applied to a {@link TrackedRace} will not be considered by this method.
     * <p>
     * 
     * @return the position obtained by interpolation but never extrapolation from the track identified as described
     *         above
     */
    Position getMarkPosition(Mark mark, LeaderboardThatHasRegattaLike leaderboard, TimePoint timePoint);

    /**
     * Stops tracking all races of the regatta specified. This will also stop tracking wind for all races tracked by any
     * {@link RaceTracker} associated with this {@code regatta}. See {@link #stopTrackingWind(Regatta, RaceDefinition)}.
     * If there were multiple calls to {@link #addTracTracRace(URL, URI, URI, WindStore, long)} with an equal
     * combination of URLs/URIs, the {@link TracTracRaceTracker} already tracking the race was re-used. The trackers
     * will be stopped by this call regardless of how many calls were made that ensured they were tracking.
     */
    void stopTracking(Regatta regatta, boolean willBeRemoved) throws MalformedURLException, IOException, InterruptedException;

    /**
     * @param port
     *            the UDP port on which to listen for incoming messages from Expedition clients
     * @param correctByDeclination
     *            An optional service to convert the wind bearings (which the receiver may
     *            believe to be true bearings) from magnetic to true bearings.
     * @throws SocketException
     *             thrown, e.g., in case there is already another listener on the port requested
     */
    void startTrackingWind(Regatta regatta, RaceDefinition race, boolean correctByDeclination);

    /**
     * If a {@link WindTracker} exists for {@code race}, it is stopped and the {@link RaceTrackingConnectivityParameters}
     * through which the {@code race} was created are updated to not track wind when restoring that race upon a server
     * restart.
     */
    void stopTrackingWind(Regatta regatta, RaceDefinition race) throws SocketException, IOException;

    /**
     * The {@link Triple#getC() third component} of the triples returned is a wind tracker-specific
     * comment where a wind tracker may provide information such as its type name or, if applicable,
     * connectivity information such as the network port on which it receives wind information.
     */
    Iterable<Util.Triple<Regatta, RaceDefinition, String>> getWindTrackedRaces();

    /**
     * Creates a new leaderboard with the <code>name</code> specified.
     * @param discardThresholds
     *            Tells the thresholds from which on a next higher number of worst races will be discarded per
     *            competitor. Example: <code>[3, 6]</code> means that starting from three races the single worst race
     *            will be discarded; starting from six races, the two worst races per competitor are discarded.
     * 
     * @return the leaderboard created
     */
    FlexibleLeaderboard addFlexibleLeaderboard(String leaderboardName, String leaderboardDisplayName,
            int[] discardThresholds, ScoringScheme scoringScheme, Iterable<? extends Serializable> courseAreaIds);

    RegattaLeaderboard addRegattaLeaderboard(RegattaIdentifier regattaIdentifier, String leaderboardDisplayName, int[] discardThresholds);

    RegattaLeaderboardWithEliminations addRegattaLeaderboardWithEliminations(String leaderboardName, String leaderboardDisplayName, RegattaLeaderboard fullRegattaLeaderboard);

    RegattaLeaderboardWithOtherTieBreakingLeaderboard addRegattaLeaderboardWithOtherTieBreakingLeaderboard(RegattaIdentifier regattaIdentifier,
            String leaderboardDisplayName, int[] discardThresholds, RegattaLeaderboard otherTieBreakingLeaderboard);

    /**
     * Removes the leaderboard specified by {@code leaderboardName} as well as all delegating leaderboards that reference it,
     * in particular those {@link DelegatingRegattaLeaderboardWithCompetitorElimination} for which the leaderboard specified
     * by {@code leaderboardName} was their underlying regatta leaderboard. If no leaderboard named as specified by the
     * {@code leaderboardName} parameter exists, this method has no effect.
     */
    void removeLeaderboard(String leaderboardName);

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

    /**
     * Stops all {@link RaceTracker}s associated with the {@code regatta} which also stops their wind tracking.
     */
    void stopTrackingAndRemove(Regatta regatta) throws MalformedURLException, IOException, InterruptedException;

    /**
     * Removes the regatta as well as all regatta leaderboards for that regatta
     */
    void removeRegatta(Regatta regatta) throws MalformedURLException, IOException, InterruptedException;
    
    /**
     * Removes the given series
     */
    void removeSeries(Series series) throws MalformedURLException, IOException, InterruptedException;

    /**
     * Returns {@code null} if the regatta or the race definition or the tracked regatta or the tracked race are not
     * found.
     */
    DynamicTrackedRace getExistingTrackedRace(RegattaAndRaceIdentifier raceIdentifier);

    /**
     * Obtains an unmodifiable map of the leaderboard groups configured in this service keyed by their uuids.
     */
    Map<UUID, LeaderboardGroup> getLeaderboardGroups();

    /**
     * Creates a new group with the name <code>groupName</code>, the description <code>desciption</code> and the
     * leaderboards with the names in <code>leaderboardNames</code> and saves it in the database. If the
     * {@code overallLeaderboardScoringSchemeType} is not {@code null}, an overall leaderboard will be created
     * and set as the new leaderboard group's {@link LeaderboardGroup#getOverallLeaderboard() overall leaderboard}.
     * Callers shall manage the security aspects of the overall leaderboard.
     * 
     * @param groupName
     *            The name of the new group
     * @param description
     *            The description of the new group
     * @param leaderboardNames
     *            The names of the leaderboards, which should be contained by the new group.<br />
     *            If there isn't a leaderboard with one of these names an {@link IllegalArgumentException} is thrown.
     * @return The new leaderboard group
     */
    LeaderboardGroup addLeaderboardGroup(UUID leaderboardGroupId, String groupName, String description,
            String displayName, boolean displayGroupsInReverseOrder, List<String> leaderboardNames,
            int[] overallLeaderboardDiscardThresholds, ScoringSchemeType overallLeaderboardScoringSchemeType);

    /**
     * Removes the group with the name <code>groupName</code> from the service and the database. This leaves
     * any {@link LeaderboardGroup#getOverallLeaderboard() overall leaderboard} untouched. Callers have to
     * ensure to maintain the overall leaderboard, if it exists, accordingly.
     * 
     * @param leaderboardGroupId
     *            The ID of the group which shall be removed.
     */
    void removeLeaderboardGroup(UUID leaderboardGroupId);

    /**
     * Updates the group data in the persistant store.
     */
    void updateStoredLeaderboardGroup(LeaderboardGroup leaderboardGroup);

    DynamicTrackedRace createTrackedRace(RegattaAndRaceIdentifier raceIdentifier, WindStore windStore,
            long delayToLiveInMillis, long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            boolean useMarkPassingCalculator, TrackingConnectorInfo trackingConnectorInfo);

    /**
     * Creates a regatta and replicates this to all replicas currently attached.
     * @param series
     *            the series must not have any {@link RaceColumn}s yet
     * @param controlTrackingFromStartAndFinishTimes
     *            cannot be {@code true} if {@link useStartTimeInference} is also {@code true}
     */
    default Regatta createRegatta(String regattaName, String boatClassName, boolean canBoatsOfCompetitorsChangePerRace,
            CompetitorRegistrationType competitorRegistrationType, String registrationLinkSecret, TimePoint startDate, TimePoint endDate, Serializable id, Iterable<? extends Series> series,
            boolean persistent, ScoringScheme scoringScheme, Serializable courseAreaId, Double buoyZoneRadiusInHullLengths,
            boolean useStartTimeInference, boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange, RankingMetricConstructor rankingMetricConstructor) {
        return createRegatta(regattaName, boatClassName, canBoatsOfCompetitorsChangePerRace, competitorRegistrationType,
                registrationLinkSecret, startDate, endDate, id, series, persistent, scoringScheme,
                courseAreaId==null?Collections.emptySet():Collections.singleton(courseAreaId), buoyZoneRadiusInHullLengths, useStartTimeInference,
                controlTrackingFromStartAndFinishTimes, autoRestartTrackingUponCompetitorSetChange,
                rankingMetricConstructor);
    }

    Regatta createRegatta(String regattaName, String boatClassName, boolean canBoatsOfCompetitorsChangePerRace,
            CompetitorRegistrationType competitorRegistrationType, String registrationLinkSecret, TimePoint startDate, TimePoint endDate, Serializable id, Iterable<? extends Series> series,
            boolean persistent, ScoringScheme scoringScheme, Iterable<? extends Serializable> courseAreaIds, Double buoyZoneRadiusInHullLengths,
            boolean useStartTimeInference, boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange, RankingMetricConstructor rankingMetricConstructor);

    /**
     * @param controlTrackingFromStartAndFinishTimes
     *            cannot be {@code true} if {@link useStartTimeInference} is also {@code true}
     */
    Regatta updateRegatta(RegattaIdentifier regattaIdentifier, TimePoint startDate, TimePoint endDate,
            Iterable<? extends Serializable> newCourseAreaIds, RegattaConfiguration regattaConfiguration,
            Iterable<? extends Series> series, Double buoyZoneRadiusInHullLengths, boolean useStartTimeInference,
            boolean controlTrackingFromStartAndFinishTimes, boolean autoRestartTrackingUponCompetitorSetChange,
            String registrationLinkSecret, CompetitorRegistrationType registrationType);

    /**
     * Adds <code>raceDefinition</code> to the {@link Regatta} such that it will appear in {@link Regatta#getAllRaces()}
     * and {@link Regatta#getRaceByName(String)}.
     * 
     * @param addToRegatta identifier of an regatta that must exist already
     */
    void addRace(RegattaIdentifier addToRegatta, RaceDefinition raceDefinition);

    /**
     * Updates the leaderboard group identified by {@code leaderboardGroupId}. If
     * {@code overallLeaderboardScoringSchemeType} is {@code null} and the leaderboard group currently
     * has an overall leaderboard, that overall leaderboard is removed from the group and deleted.
     * Conversely, if {@code overallLeaderboardScoringSchemeType} is not {@code null} but the
     * leaderboard group currently has no overall leaderboard set, it is created. Callers have to
     * manage all security aspects around this.
     */
    void updateLeaderboardGroup(UUID leaderboardGroupId, String newName, String description, String displayName,
            List<String> leaderboardNames, int[] overallLeaderboardDiscardThresholds, ScoringSchemeType overallLeaderboardScoringSchemeType);

    /**
     * @return a thread-safe copy of the events currently known by the service; it's safe for callers to iterate over
     *         the iterable returned, and no risk of a {@link ConcurrentModificationException} exists
     */
    Iterable<Event> getAllEvents();
    
    /**
     * @return a thread-safe copy of the service' Regattas filtered by {@link regattaIds} parameter with taking the
     *         {@code include} include parameter into account; it's safe for callers to iterate over the iterable
     *         returned, and no risk of a {@link ConcurrentModificationException} exists
     */
    Iterable<Regatta> getRegattasSelectively(boolean include, Iterable<UUID> regattaIds);
    
    /**
     * @return a thread-safe copy of the service' events filtered by {@link eventIds} parameter with taking the
     *         {@code include} include parameter into account; it's safe for callers to iterate over the iterable
     *         returned, and no risk of a {@link ConcurrentModificationException} exists
     */
    Iterable<Event> getEventsSelectively(boolean include, Iterable<UUID> eventIds);

    /**
     * Creates a new event with the name <code>eventName</code>, the venue <code>venue</code> and the regattas with the
     * names in <code>regattaNames</code>, saves it in the database and replicates it. Use for TESTING only!
     * 
     * @param eventName
     *            The name of the new event
     * @param startDate
     *            The start date of the event
     * @param endDate
     *            The end date of the event
     * @param isPublic
     *            Indicates whether the event is public accessible via the publication URL or not
     * @param id
     *            The id of the new event
     * @param venue
     *            The name of the venue of the new event
     * @return The new event
     */
    Event addEvent(String eventName, String eventDescription, TimePoint startDate, TimePoint endDate, String venueName, boolean isPublic, UUID id);

    /**
     * Updates a sailing event with the name <code>eventName</code>, the venue<code>venue</code> and the regattas with
     * the names in <code>regattaNames</code> and updates it in the database.
     * 
     * @param eventName
     *            The name of the event to update
     * @param startDate
     *            The start date of the event
     * @param endDate
     *            The end date of the event
     * @param venueName
     *            The name of the venue of the event
     * @param isPublic
     *            Indicates whether the event is public accessible via the publication URL or not
     * @param windFinderReviewedSpotCollectionIds
     *            IDs of WindFinder spot collections, reviewed by www.windfinder.com, to use by the WindFinder wind
     *            tracker and to expose on the event's UI
     * @return The new event
     */
    void updateEvent(UUID id, String eventName, String eventDescription, TimePoint startDate, TimePoint endDate,
            String venueName, boolean isPublic, Iterable<UUID> leaderboardGroupIds, URL officialWebsiteURL, URL baseURL, 
            Map<Locale, URL> sailorsInfoWebsiteURLs, Iterable<ImageDescriptor> images, Iterable<VideoDescriptor> videos,
            Iterable<String> windFinderReviewedSpotCollectionIds);

    /**
     * Renames a sailing event. If a sailing event by the name <code>oldName</code> does not exist in {@link #getEvents()},
     * or if a event with the name <code>newName</code> already exists, an {@link IllegalArgumentException} is thrown.
     * If the method completes normally, the rename has been successful, and the event previously obtained by calling
     * {@link #getEventByName(String) getEventByName(oldName)} can now be obtained by calling
     * {@link #getEventByName(String) getEventByName(newName)}.
     */
    void renameEvent(UUID id, String newEventName);

    /**
     * Does not replicate automatically; see {@link RemoveEvent} for the replicable operation that calls this method.
     */
    void removeEvent(UUID id);

    
    /**
     * @return a thread-safe copy of the events (or the exception that occurred trying to obtain the events; arranged in
     *         a {@link Util.Pair}) of all (remote) sailing server instances currently known by the service; it's safe for
     *         callers to iterate over the iterable returned, and no risk of a {@link ConcurrentModificationException}
     *         exists
     */
    Map<RemoteSailingServerReference, Util.Pair<Iterable<EventBase>, Exception>> getPublicEventsOfAllSailingServers();

    RemoteSailingServerReference addRemoteSailingServerReference(String name, URL url, boolean include);

    /**
     * Updates the include flag and selected events id's for remote sailing server with the given name
     * 
     * @param name
     *            is used to find the target remote sailing server reference by
     * @param include
     *            flag which determining the inclusion factor for selected events
     * @param selectedEventIds
     *            the list of selected event id's
     * @return the updated remote sailing server reference
     */
    RemoteSailingServerReference updateRemoteSailingServerReference(String name, boolean include,
            Set<UUID> selectedEventIds);

    void removeRemoteSailingServerReference(String name);

    
    CourseArea[] addCourseAreas(UUID eventId, String[] courseAreaNames, UUID[] courseAreaIds, Position[] centerPositions, Distance[] radiuses);

    com.sap.sailing.domain.base.DomainFactory getBaseDomainFactory();

    CourseArea getCourseArea(Serializable courseAreaId);

    /**
     * Adds the specified mediaTrack to the in-memory media library.
     * Important note: Only if mediaTrack.dbId != null the mediaTrack will be persisted in the the database.
     * @param mediaTrack
     */
    void mediaTrackAdded(MediaTrack mediaTrack);

    /**
     * Calling mediaTrackAdded for every entry in the specified collection. 
     * @param mediaTracks
     */
    void mediaTracksAdded(Iterable<MediaTrack> mediaTracks);
    
    void mediaTrackTitleChanged(MediaTrack mediaTrack);

    void mediaTrackUrlChanged(MediaTrack mediaTrack);

    void mediaTrackStartTimeChanged(MediaTrack mediaTrack);

    void mediaTrackDurationChanged(MediaTrack mediaTrack);

    void mediaTrackAssignedRacesChanged(MediaTrack mediaTrack);
    
    void mediaTrackDeleted(MediaTrack mediaTrack);

    /**
     * In contrast to mediaTracksAdded, this method takes mediaTracks with a given dbId.
     * Checks if the track already exists in the library and the database and adds/stores it
     * accordingly. If a track already exists and override, its properties are checked for changes 
     * @param override If true, track properties (title, url, start time, duration, not mime type!) will be 
     * overwritten with the values from the track to be imported.
     * @param mediaTrack
     */
    void mediaTracksImported(Iterable<MediaTrack> mediaTracksToImport, MasterDataImportObjectCreationCountImpl creatingCount, boolean override) throws Exception;
    
    Iterable<MediaTrack> getMediaTracksForRace(RegattaAndRaceIdentifier regattaAndRaceIdentifier);
    
    Iterable<MediaTrack> getMediaTracksInTimeRange(RegattaAndRaceIdentifier regattaAndRaceIdentifier);

    Iterable<MediaTrack> getAllMediaTracks();

    Iterable<URL> getResultImportUrls(String resultProviderName) throws UnauthorizedException;

    void removeResultImportURLs(String resultProviderName, Set<URL> toRemove)
            throws UnauthorizedException, Exception;

    void addResultImportUrl(String resultProviderName, URL url) throws UnauthorizedException, Exception;

    Optional<ResultUrlProvider> getUrlBasedScoreCorrectionProvider(String resultProviderName);

    void reloadRaceLog(String leaderboardName, String raceColumnName, String fleetName);

    RaceLog getRaceLog(String leaderboardName, String raceColumnName, String fleetName);

    Map<Competitor, Boat> getCompetitorToBoatMappingsForRace(String leaderboardName, String raceColumnName, String fleetName);
    
    /**
     * @return a pair with the found or created regatta, and a boolean that tells whether the regatta was created during
     *         the call
     */
    Util.Pair<Regatta, Boolean> getOrCreateRegattaWithoutReplication(String fullRegattaName, String boatClassName, 
            boolean canBoatsOfCompetitorsChangePerRace, CompetitorRegistrationType competitorRegistrationType,
            String registrationLinkSecret, TimePoint startDate, TimePoint endDate, Serializable id,
            Iterable<? extends Series> series, boolean persistent, ScoringScheme scoringScheme,
            Iterable<? extends Serializable> courseAreaIds, Double buoyZoneRadiusInHullLengths,
            boolean useStartTimeInference, boolean controlTrackingFromStartAndFinishTimes,
            boolean autoRestartTrackingUponCompetitorSetChange, RankingMetricConstructor rankingMetricConstructor);

    /**
     * @return map where keys are the toString() representation of the {@link RaceDefinition#getId() IDs} of races passed to
     * {@link #setRegattaForRace(Regatta, RaceDefinition)}. It helps remember the connection between races and regattas.
     */
    ConcurrentHashMap<String, Regatta> getPersistentRegattasForRaceIDs();
    
    Event createEventWithoutReplication(String eventName, String eventDescription, TimePoint startDate, TimePoint endDate, String venue,
            boolean isPublic, UUID id, URL officialWebsiteURL, URL baseURL, Map<Locale, URL> sailorsInfoWebsiteURLs,
            Iterable<ImageDescriptor> images, Iterable<VideoDescriptor> videos);

    void setRegattaForRace(Regatta regatta, String raceIdAsString);

    CourseArea[] addCourseAreasWithoutReplication(UUID eventId, UUID[] courseAreaIds, String[] courseAreaNames, Position[] centerPositions, Distance[] radiuses);

    CourseArea[] removeCourseAreaWithoutReplication(UUID eventId, UUID[] courseAreaIds);

    /**
     * Returns a mobile device's configuration.
     * @param identifier of the client (may include event)
     * @return the {@link DeviceConfiguration}
     */
    DeviceConfiguration getDeviceConfigurationById(UUID id);
    
    /**
     * Returns a mobile device's configuration by its name. This was unique in the past, but this constraint will
     * be removed in future releases, so using this method is considered deprecated.
     */
    DeviceConfiguration getDeviceConfigurationByName(String deviceConfigurationName);
    
    /**
     * Adds a device configuration. The effect is replicated.
     * 
     * @param matcher
     *            defining for which the configuration applies.
     * @param configuration
     *            of the device.
     */
    void createOrUpdateDeviceConfiguration(DeviceConfiguration configuration);
    
    /**
     * Removes a configuration by its ID. The effect is replicated.
     */
    void removeDeviceConfiguration(UUID id);

    /**
     * Returns all configurations and their matching objects. 
     * @return the {@link DeviceConfiguration}s.
     */
    Iterable<DeviceConfiguration> getAllDeviceConfigurations();

    /**
     * Forces a new start time on the RaceLog identified by the passed parameters.
     * @param leaderboardName name of the RaceLog's leaderboard.
     * @param raceColumnName name of the RaceLog's column
     * @param fleetName name of the RaceLog's fleet
     * @param authorName name of the {@link AbstractLogEventAuthor} the {@link RaceLogStartTimeEvent} will be created with
     * @param authorPriority priority of the author.
     * @param passId Pass identifier of the new start time event.
     * @param logicalTimePoint logical {@link TimePoint} of the new event.
     * @param startTime the new Start-Time
     * @param courseAreaId the ID of the course area on which the start is happening, or {@code null} if not known
     * @return
     */
    TimePoint setStartTimeAndProcedure(String leaderboardName, String raceColumnName, String fleetName, String authorName,
            int authorPriority, int passId, TimePoint logicalTimePoint, TimePoint startTime, RacingProcedureType racingProcedure, UUID courseAreaId);
    
    /**
     * Forces a new end time identified by the passed parameters.
     * @param leaderboardName name of the RaceLog's leaderboard.
     * @param raceColumnName name of the RaceLog's column
     * @param fleetName name of the RaceLog's fleet
     * @param authorName name of the {@link AbstractLogEventAuthor} the {@link RaceLogStartTimeEvent} will be created with
     * @param authorPriority priority of the author.
     * @param passId Pass identifier of the new start time event.
     * @param logicalTimePoint logical {@link TimePoint} of the new event.
     * @return
     */
    TimePoint setEndTime(String leaderboardName, String raceColumnName, String fleetName, String authorName,
            int authorPriority, int passId, TimePoint logicalTimePoint);

    /**
     * Forces a new finishing time identified by the passed parameters.
     * @param leaderboardName name of the RaceLog's leaderboard.
     * @param raceColumnName name of the RaceLog's column
     * @param fleetName name of the RaceLog's fleet
     * @param authorName name of the {@link AbstractLogEventAuthor} the {@link RaceLogStartTimeEvent} will be created with
     * @param authorPriority priority of the author.
     * @param passId Pass identifier of the new start time event.
     * @param logicalTimePoint logical {@link TimePoint} of the new event.
     * @return
     */
    TimePoint setFinishingTime(String leaderboardName, String raceColumnName, String fleetName, String authorName,
            Integer authorPriority, int passId, MillisecondsTimePoint millisecondsTimePoint);

    /**
     * Gets the start time, pass identifier and racing procedure for the queried race. Start time might be <code>null</code>.
     */
    Util.Triple<TimePoint, Integer, RacingProcedureType> getStartTimeAndProcedure(String leaderboardName, String raceColumnName, String fleetName);

    /**
     * Gets the finishing and finish times as well as the pass identifier for the queried race. The first TimePoint is the
     * finishing time, the second on is the finish time. Finishing and/or finish times might be <code>null</code>.
     */
    com.sap.sse.common.Util.Triple<TimePoint, TimePoint, Integer> getFinishingAndFinishTime(
            String leaderboardName, String raceColumnName, String fleetName);
    
    MongoObjectFactory getMongoObjectFactory();
    
    DomainObjectFactory getDomainObjectFactory();
    
    WindStore getWindStore();

    PolarDataService getPolarDataService();

    SimulationService getSimulationService();

    /**
     * {@link TaggingService} can be used to perform all CRUD operations on private and pulic
     * {@link com.sap.sailing.domain.common.dto.TagDTO tags}. This service is used by the REST API and GWT client and
     * needs to perform independant of the requesting resource.
     * 
     * @return instance of TaggingService
     */
    TaggingService getTaggingService();

    RaceTracker getRaceTrackerById(Object id);
    
    /**
     * Tries to obtain a priority-0 author from a currently logged-in {@link Subject}. If no user
     * is currently logged on or subject's {@link Subject#getPrincipal() principal} is not set,
     * a default server author object with priority 0 is returned as default.
     */
    AbstractLogEventAuthor getServerAuthor();
    
    CompetitorAndBoatStore getCompetitorAndBoatStore();
    
    TypeBasedServiceFinderFactory getTypeBasedServiceFinderFactory();

    /**
     * This lock exists to allow only one master data import at a time to avoid situation where multiple Imports
     * override each other in unpredictable fashion
     */
    DataImportLockWithProgress getDataImportLock();

    DataImportProgress createOrUpdateDataImportProgressWithReplication(UUID importOperationId,
            double overallProgressPct, DataImportSubProgress subProgress, double subProgressPct);

    DataImportProgress createOrUpdateDataImportProgressWithoutReplication(UUID importOperationId,
            double overallProgressPct, DataImportSubProgress subProgress, double subProgressPct);

    void setDataImportFailedWithReplication(UUID importOperationId, String errorMessage);

    void setDataImportFailedWithoutReplication(UUID importOperationId, String errorMessage);

    void setDataImportDeleteProgressFromMapTimerWithReplication(UUID importOperationId);

    void setDataImportDeleteProgressFromMapTimerWithoutReplication(UUID importOperationId);

    /**
     * For the reference to a remote sailing server, updates its events cache and returns the event list or, if fetching
     * the event list from the remote server did fail, the exception for which it failed. If the {@link forceUpdate}
     * parameter is <code>true</code> then the remote server will be replaced in cache
     */
    Util.Pair<Iterable<EventBase>, Exception> updateRemoteServerEventCacheSynchronously(
            RemoteSailingServerReference ref, boolean forceUpdate);
    
    Util.Pair<Iterable<EventBase>, Exception> getCompleteRemoteServerReference(RemoteSailingServerReference ref);

    /**
     * Searches the content of this server, not that of any remote servers referenced by any {@link RemoteSailingServerReference}s.
     */
    @Override
    Result<LeaderboardSearchResult> search(KeywordQueryWithOptionalEventQualification query);

    /**
     * Searches a specific remote server whose reference has the {@link RemoteSailingServerReference#getName() name}
     * <code>remoteServerReferenceName</code>. If a remote server reference with that name is not known,
     * <code>null</code> is returned. Otherwise, a non-<code>null</code> and possibly empty search result set is
     * returned.
     */
    Result<LeaderboardSearchResultBase> searchRemotely(String remoteServerReferenceName, KeywordQueryWithOptionalEventQualification query);

    /**
     * Gets the configuration of the local sailing server instances.
     */
    SailingServerConfiguration getSailingServerConfiguration();
    
    void updateServerConfiguration(SailingServerConfiguration serverConfiguration);
    
    /**
     * References to remote servers may be dead or alive. This is internally determined by regularly polling those
     * servers for their events list. If the events list cannot be successfully retrieved, the server is considered "dead."
     * This method returns the "live" server references.
     */
    Iterable<RemoteSailingServerReference> getLiveRemoteServerReferences();

    /**
     * Obtains all remote sailing server references, regardless their "live" state
     */
    Map<String, RemoteSailingServerReference> getAllRemoteServerReferences();

    /**
     * @return {@code null} if no remote server reference by the name specified exists
     */
    RemoteSailingServerReference getRemoteServerReferenceByName(String remoteServerReferenceName);

    /**
     * @return {@code null} if no remote server reference with that URL specified exists
     */
    RemoteSailingServerReference getRemoteServerReferenceByUrl(URL remoteServerReferenceUrl);

    void addRegattaWithoutReplication(Regatta regatta);

    void addEventWithoutReplication(Event event);

    /**
     * Adds the leaderboard group to this service; if the group has an overall leaderboard, the overall leaderboard
     * is added to this service as well. For both, the group and the overall leaderboard, any previously existing
     * objects by the same name of that type will be replaced.<p>
     * 
     * For this method it is permissible to add the leaderboard group even if another one by an equal name but different
     * {@link LeaderboardGroup#getId() ID} exists. This will make it possible at least to <em>import</em> another leaderboard
     * group with an already existing name but different ID and later rename it to a unique name.
     */
    void addLeaderboardGroupWithoutReplication(LeaderboardGroup leaderboardGroup);

    /**
     * @return {@code null} if no service can be found in the OSGi registry
     */
    FileStorageManagementService getFileStorageManagementService();

    /**
     * Gets the {@link RaceTracker} associated with a given {@link RegattaAndRaceIdentifier}. If the {@link RaceTracker}
     * is already available, the {@code callback} is invoked immediately. If the {@link RaceTracker} isn't available
     * yet, the given callback will be informed asynchronously on registration of the RaceTracker in question.
     */
    void getRaceTrackerByRegattaAndRaceIdentifier(RegattaAndRaceIdentifier raceIdentifier, Consumer<RaceTracker> callback);

    /**
     * When restoring tracked races was requested upon creation of this service and after the corresponding restore records
     * were read from the persistent store, this method returns the number of races to be restored. Otherwise, it returns 0.
     * 
     * @see #getNumberOfTrackedRacesRestored()
     */
    long getNumberOfTrackedRacesToRestore();

    /**
     * When restoring tracked races was requested upon creation of this service, this method tells the number of races
     * whose loading process has already been triggered. Otherwise, it returns 0.
     * 
     * @see #getNumberOfTrackedRacesToRestore()
     */
    int getNumberOfTrackedRacesRestored();

    /**
     * Provides {@link Statistics statistic information} for every year which is covered by the local
     * server.
     * 
     * @return a map of {@link Statistics statistic objects} keyed be the year they are representing
     */
    Map<Integer, Statistics> getLocalStatisticsByYear();

    /**
     * Provides {@link Statistics statistic information} for every year which is covered by the local and
     * all remote servers.
     * 
     * @return a map of {@link Statistics statistic objects} keyed be the year they are representing
     */
    Map<Integer, Statistics> getOverallStatisticsByYear();

    /**
     * Obtains information about all {@link TrackedRace}s connected to {@link Event}s managed locally on this server
     * instance or reachable through a remote server reference, having a non-{@code null}
     * {@link TrackedRace#getStartOfRace() start time}. Being "connected" here means that the race is linked to a
     * {@link Leaderboard} that is part of a {@link LeaderboardGroup} which is in turn
     * {@link Event#getLeaderboardGroups() linked} to the {@link Event}. The list can be filtered by a predicate that is
     * used to inspect the UUIDs of the associated events.
     * 
     * @param eventListFilter
     *            a predicate that can be used to filter on the uuids of the events to which the races are assigned to.
     * 
     * @return a new map whose keys identify the race and whose values have a short info about the race that will allow,
     *         e.g., to sort by start time and therefore identify "anniversary" races in a central instance. All
     *         {@link SimpleRaceInfo#getRemoteUrl()} values will be {@code null} for races managed locally on this
     *         server; for races obtained through remote server references, the remote URL will be that of the remote
     *         server reference. Callers may modify the map as each call to this method will produce a new copy. The
     *         value of the map consists out of a set to reflect the situation where races are assigned to multiple
     *         events. Therefore see also {@link SimpleRaceInfo#getEventID()}.
     */
    Map<RegattaAndRaceIdentifier, Set<SimpleRaceInfo>> getRemoteRaceList(Predicate<UUID> eventListFilter);

    /**
     * Obtains information about all {@link TrackedRace}s connected to {@link Event}s managed locally on this server
     * instance, having a non-{@code null} {@link TrackedRace#getStartOfRace() start time}. Being "connected" here means
     * that the race is linked to a {@link Leaderboard} that is part of a {@link LeaderboardGroup} which is in turn
     * {@link Event#getLeaderboardGroups() linked} to the {@link Event}.
     * 
     * @param eventListFilter
     *            a predicate that can be used to filter on the uuids of the events to which the races are assigned to.
     * 
     * @return a new map whose keys identify the race and whose values have a short info about the race that will allow,
     *         e.g., to sort by start time and therefore identify "anniversary" races in a central instance. All
     *         {@link SimpleRaceInfo#getRemoteUrl()} values will be {@code null}, meaning that the tracked races live
     *         locally on this server. Callers may modify the map as each call to this method will produce a new copy. The
     *         value of the map consists out of a set to reflect the situation where races are assigned to multiple
     *         events. Therefore see also {@link SimpleRaceInfo#getEventID()}.
     */
    Map<RegattaAndRaceIdentifier, Set<SimpleRaceInfo>> getLocalRaceList(Predicate<UUID> eventListFilter);

    /**
     * Provides a {@link DetailedRaceInfo} for the given {@link RegattaAndRaceIdentifier}. The algorithm first tries to
     * resolve this via {@link getFullDetailsForRaceLocal(RegattaAndRaceIdentifier)}, if no local result can be
     * determined, the identifier is resolved against the cached remote race list in the
     * {@link com.sap.sailing.server.impl.RemoteSailingServerSet}. If a match is found, the remoteUrl stored in the
     * match is used to make a remote REST call to retrieve the required information from the remote server. This method
     * is intended to be used to resolve detailed information for determined anniversary races.
     * 
     * @return a DetailedRaceInfo object or null if the race could not be resolved
     */
    DetailedRaceInfo getFullDetailsForRaceCascading(RegattaAndRaceIdentifier regattaNameAndRaceName);

    /**
     * Provides a {@link DetailedRaceInfo} for the given {@link RegattaAndRaceIdentifier}. This method only tries to
     * resolve the race against locally tracked races reachable from an event that have a startOfRace that is not
     * {@code null}.
     * 
     * @return a DetailedRaceInfo object or null if the race could not be resolved
     */
    DetailedRaceInfo getFullDetailsForRaceLocal(RegattaAndRaceIdentifier raceIdentifier);

    /**
     * Provides number and {@link AnniversaryType type} information for the next anniversary race.
     * 
     * @return a {@link Pair} containing the next anniversary number and {@link AnniversaryType type}, or
     *         <code>null</code> if next anniversary can't be determined
     */
    Pair<Integer, AnniversaryType> getNextAnniversary();

    /**
     * @return the amount of races that are tracked have a startTime and are either remotely or locally resolvable, or <code>null</code> if next anniversary can't be determined
     */
    int getCurrentRaceCount();

    /**
     * Provides a {@link Map} of all known anniversaries, keyed by the number of the anniversary race.
     * 
     * @return the {@link Map} of known anniversaries (key = anniversary number / value = {@link Pair} containing
     *         {@link DetailedRaceInfo race} and {@link AnniversaryType type} information)
     */
    Map<Integer, Pair<DetailedRaceInfo, AnniversaryType>> getKnownAnniversaries();

    /**
     * Returns the {@link AnniversaryRaceDeterminator} used by this service. This is needed for replication for
     * anniversary races only.
     */
    AnniversaryRaceDeterminator getAnniversaryRaceDeterminator();
    
    /**
     * Returns a calculated {@link PairingListTemplate}, specified by flights, groups and competitors.
     *
     * @param leaderboardName
     *            the name of the leaderboard
     * @param competitorsCount
     *            count of competitor
     * @param flightMultiplier
     *            specifies how often the flights will be cloned
     * @param boatChangeFactor
     *            specifies the priority of well distributed assignment of competitors to boats (smallest factor) or
     *            minimization of boat changes within a {@link PairingList} (highest factor); valid factors are
     *            {@code 0..numberOfFlights}
     * @return calculated {@link PairingListTemplate}
     */
    PairingListTemplate createPairingListTemplate(final int flightsCount, final int groupsCount,
            final int competitorsCount, final int flightMultiplier, final int boatChangeFactor);
    
    /**
     * Matches the competitors of a leaderboard to the {@link PairingList}
     * 
     * @param pairingListTemplate
     *            the returned {@link PairingList} is based upon it
     * @param leaderboardName
     *            name of the leaderboard
     * @return {@link PairingList} that contains competitor objects matched to {@link RaceColumn}s and {@link Fleet}s
     * @throws PairingListCreationException
     *             for example if the number of boats available for the leaderboard is too small to create the pairing
     *             list
     */
    PairingList<RaceColumn, Fleet, Competitor,Boat> getPairingListFromTemplate(PairingListTemplate pairingListTemplate, 
            final String leaderboardName, final Iterable<RaceColumn> selectedFlights) throws PairingListCreationException;

    /**
     * From a {@link CompetitorDescriptor} looks up or creates a {@link CompetitorWithBoat} object which is then guaranteed
     * to be in the {@link CompetitorStore}.
     */
    DynamicCompetitorWithBoat convertCompetitorDescriptorToCompetitorWithBoat(CompetitorDescriptor competitorDescriptor, String searchTag);

    /**
     * Required for replicated operations, so they can obtain a valid instance of the SecurityService
     */
    SecurityService getSecurityService();

    /**
     * If the leaderboard can be resolved to a regatta, and the given secret is correct, return true
     */
    default boolean skipChecksDueToCorrectSecret(String leaderboardName, String secret) {
        final boolean result;
        if (leaderboardName == null && secret == null) {
            result = false;
        } else {
            Regatta regatta = getRegattaByName(leaderboardName);
            if (regatta == null) {
                if (secret != null) {
                    logger.warning(
                            "Attempt to skip security checks using regatta secret \"" + secret + "\" for leaderboard \""
                                    + leaderboardName + "\", but a regatta with the same name could not be resolved");
                } // else regatta not found, but no secret specified either; checks won't be skipped, but no skipping was really requested
                result = false;
            } else {
                result = Util.equalStringsWithEmptyIsNull(regatta.getRegistrationLinkSecret(), secret);
            }
        }
        return result;
    }
    
    CourseAndMarkConfigurationFactory getCourseAndMarkConfigurationFactory();
    
    /**
     * @return A {@link RaceTrackingConnectivityParameters} for a given {@link RaceDefinition}. 
     * Used for MasterDataImport
     */
    RaceTrackingConnectivityParameters getConnectivityParametersByRace(RaceDefinition raceDefinition);

    RaceTrackingHandler getPermissionAwareRaceTrackingHandler();
    
    /**
     * Uses the {@link #getPermissionAwareRaceTrackingHandler()} as the {@link RaceTrackingHandler}
     */
    @Override
    RaceHandle addRace(RegattaIdentifier regattaToAddTo, RaceTrackingConnectivityParameters params, long timeoutInMilliseconds) throws Exception;
    
    TypeBasedServiceFinder<RaceTrackingConnectivityParametersHandler> getRaceTrackingConnectivityParamsServiceFinder();
    
    /**
     * Import MasterData from a remote server URL. A caller might provide either targetServerUsername and
     * targetServerPassword or targetServerBearerToken. If neither of those are provided the method will try to create
     * or get the bearer token for the current user.
     * 
     * @return the leaderboard groups imported as the keys, and the events belonging to those leaderboard groups as
     *         values
     */
    Map<LeaderboardGroup, ? extends Iterable<Event>> importMasterData(final String urlAsString,
            final UUID[] leaderboardGroupIds, final boolean override, final boolean compress, final boolean exportWind,
            final boolean exportDeviceConfigurations, String targetServerUsername, String targetServerPassword,
            String targetServerBearerToken, final boolean exportTrackedRacesAndStartTracking,
            final UUID importOperationId) throws IllegalArgumentException;

    void addOrReplaceExpeditionDeviceConfiguration(UUID deviceConfigurationId, String name, Integer expeditionBoatId);

    void removeExpeditionDeviceConfiguration(UUID deviceUuid);
    
    /**
     * Returns the number of tracked races that are not {@link TrackedRace#hasFinishedLoading() done with loading}.
     */
    int getNumberOfTrackedRacesStillLoading();
    /**
     * Returns the number of tracked races restored during server start-up that are
     * {@link TrackedRace#hasFinishedLoading() done with loading}.
     */
    int getNumberOfTrackedRacesRestoredDoneLoading();

    /**
     * Short for {@link #findEventsContainingLeaderboardAndMatchingAtLeastOneCourseArea(Leaderboard, Iterable)
     * findEventsContainingLeaderboardAndMatchingAtLeastOneCourseArea(leaderboard, getAllEvents())}.
     */
    Event findEventContainingLeaderboardAndMatchingAtLeastOneCourseArea(Leaderboard leaderboard);

    /**
     * Identifies all Events, that use the given {@link Leaderboard}'s {@link CourseArea}s and contain it in their
     * {@link LeaderboardGroup}
     * 
     * @return A Set of Events, may be empty, but never {@code null}
     */
    Set<Event> findEventsContainingLeaderboardAndMatchingAtLeastOneCourseArea(Leaderboard leaderboard, Iterable<Event> events);

    void revokeMarkDefinitionEventInRegattaLog(String leaderboardName, String raceColumnName, String fleetName, String markId)
            throws DoesNotHaveRegattaLogException, MarkAlreadyUsedInRaceException;

    void addMarkToRegattaLog(String leaderboardName, Mark mark) throws DoesNotHaveRegattaLogException;

    Pair<Boolean, String> checkIfMarksAreUsedInOtherRaceLogs(String leaderboardName, String raceColumnName,
            String fleetName, Set<String> markIds);

    Iterable<CompetitorProvider> getAllCompetitorProviders();

    /**
     * @param leaderboardGroupId
     *            if not {@code null}, this takes precedence over the {@code leaderboardGroupName} parameter which will
     *            then be ignored and will be used to look up an optional leaderboard group providing the context, e.g.,
     *            for seasonal scores from an overall leaderboard
     * @param leaderboardGroupName
     *            evaluated only if {@code leaderboardGroupId} was {@code null}; may even be {@code null} if
     *            {@code leaderboardGroupId} is {@code null} too because leaderboard group resolution is optional. If a
     *            non-{@code null} name is provided here and if {@code leaderboardGroupId} was {@code null} then the
     *            name is used to try to resolve the leaderboard group by name.
     */
    Double getCompetitorRaceDataEntry(DetailType dataType, TrackedRace trackedRace, Competitor competitor,
            TimePoint timePoint, LeaderboardGroup leaderboardGroup, String leaderboardName,
            WindLegTypeAndLegBearingAndORCPerformanceCurveCache cache) throws NoWindException,
            NotEnoughDataHasBeenAddedException, MaxIterationsExceededException, FunctionEvaluationException;
}
