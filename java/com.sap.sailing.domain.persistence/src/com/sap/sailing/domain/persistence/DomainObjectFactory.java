package com.sap.sailing.domain.persistence;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

import org.bson.Document;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.anniversary.DetailedRaceInfo;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.RegattaRegistry;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.base.SailingServerConfiguration;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.dto.AnniversaryType;
import com.sap.sailing.domain.leaderboard.EventResolver;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.leaderboard.LeaderboardRegistry;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboardWithEliminations;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParametersHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.Util.Pair;

/**
 * Offers methods to load domain objects from a Mongo DB
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface DomainObjectFactory {
    /**
     * @param regattaName only needed for backward compatibility because old wind tracks used the regatta name as part of the key
     */
    WindTrack loadWindTrack(String regattaName, RaceDefinition race, WindSource windSource, long millisecondsOverWhichToAverage);

    /**
     * @return the leaderboard loaded, if successful, or <code>null</code> if the leaderboard couldn't be loaded,
     * e.g., because the regatta for a regatta leaderboard couldn't be found
     */
    Leaderboard loadLeaderboard(String name, RegattaRegistry regattaRegistry, LeaderboardRegistry leaderboardRegistry);

    RaceIdentifier loadRaceIdentifier(Document dbObject);
    
    /**
     * Loads the leaderboard group that has <code>name</code> as its name.
     * <p>
     * 
     * If the leaderboard group does not yet have a UUID as its {@link LeaderboardGroup#getId() ID}, a new random UUID
     * is generated, assigned to the leaderboard group, and the leaderboard group is stored again (incremental
     * migration).
     * 
     * @param leaderboardRegistry
     *            if not <code>null</code>, then before creating and loading the leaderboard it is looked up in this
     *            registry and only loaded if not found there. If <code>leaderboardRegistry</code> is <code>null</code>,
     *            the leaderboard is loaded in any case. If the leaderboard is loaded and
     *            <code>leaderboardRegistry</code> is not <code>null</code>, the leaderboard loaded is
     *            {@link LeaderboardRegistry#addLeaderboard(Leaderboard) added to the registry}.
     * @return The group with the name <code>name</code>, or <code>null</code> if no such group exists.
     */
    LeaderboardGroup loadLeaderboardGroup(String name, RegattaRegistry regattaRegistry, LeaderboardRegistry leaderboardRegistry);
    
    /**
     * @param leaderboardRegistry
     *            if not <code>null</code>, then before creating and loading the leaderboard it is looked up in this
     *            registry and only loaded if not found there. If <code>leaderboardRegistry</code> is <code>null</code>,
     *            the leaderboard is loaded in any case. If the leaderboard is loaded and
     *            <code>leaderboardRegistry</code> is not <code>null</code>, the leaderboard loaded is
     *            {@link LeaderboardRegistry#addLeaderboard(Leaderboard) added to the registry}.
     * @return All groups in the database.
     */
    Iterable<LeaderboardGroup> getAllLeaderboardGroups(RegattaRegistry regattaRegistry, LeaderboardRegistry leaderboardRegistry);
    
    /**
     * @param leaderboardRegistry
     *            if not <code>null</code>, then before creating and loading the leaderboard it is looked up in this
     *            registry and only loaded if not found there. If <code>leaderboardRegistry</code> is <code>null</code>,
     *            the leaderboard is loaded in any case. If the leaderboard is loaded and
     *            <code>leaderboardRegistry</code> is not <code>null</code>, the leaderboard loaded is
     *            {@link LeaderboardRegistry#addLeaderboard(Leaderboard) added to the registry}.
     * @return All leaderboards in the database, which aren't contained by a leaderboard group
     */
    Iterable<Leaderboard> getLeaderboardsNotInGroup(RegattaRegistry regattaRegistry, LeaderboardRegistry leaderboardRegistry);

    Map<? extends WindSource, ? extends WindTrack> loadWindTracks(String regattaName, RaceDefinition race,
            long millisecondsOverWhichToAverageWind);

    Event loadEvent(String name);

    Iterable<Pair<Event, Boolean>> loadAllEvents();
    
    /**
     * The {@link MongoObjectFactory#storeEvent(Event)} method stores events and their links to leaderboard groups.
     * Loading the same data has to happen in two slices because there are cyclic references between events and
     * leaderboard groups, and the usual loading order, e.g., in <code>RacingEventService</code>, is to first load the
     * events, then the leaderboard groups. So the links between them can only be resolved after both types of objects
     * have finished loading. This method implements this step of loading and establishing the links.
     */
    void loadLeaderboardGroupLinksForEvents(EventResolver eventResolver, LeaderboardGroupResolver leaderboardGroupResolver);

    SailingServerConfiguration loadServerConfiguration();

    Iterable<RemoteSailingServerReference> loadAllRemoteSailingServerReferences();
    
    Regatta loadRegatta(String name, TrackedRegattaRegistry trackedRegattaRegistry);

    Iterable<Regatta> loadAllRegattas(TrackedRegattaRegistry trackedRegattaRegistry);

    Map<String, Regatta> loadRaceIDToRegattaAssociations(RegattaRegistry regattaRegistry);

    RaceLog loadRaceLog(RaceLogIdentifier identifier);

    RegattaLog loadRegattaLog(RegattaLikeIdentifier identifier);

    /**
     * Migrates the old COMPETITORS collection and the new BOATS collection.
     * The old COLLECTION is will be renamed to COMPETITORS_BAK for deveopment and test purposes
     * @return a collection of the old type where all competitors contain their boats or null if migration was not required.
     */
    Iterable<CompetitorWithBoat> migrateLegacyCompetitorsIfRequired();

    /**
     * Loads all competitors (with and without embedded boats) and resolves them via the domain factory.
     * Returns a collection of {@link Competitor} or {@link CompetitorWithBoat} objects. 
     */
    Collection<DynamicCompetitor> loadAllCompetitors();

    /**
     * Loads all boats and resolves them via the domain factory.
     */
    Collection<DynamicBoat> loadAllBoats();

    DomainFactory getBaseDomainFactory();

    Iterable<DeviceConfiguration> loadAllDeviceConfigurations();

    Map<String, Set<URL>> loadResultUrls();
    
    /**
     * Returned by {@link DomainObjectFactory#loadConnectivityParametersForRacesToRestore(Consumer)}.
     * 
     * @author Axel Uhl (D043530)
     *
     */
    static interface ConnectivityParametersLoadingResult {
        /**
         * @return the number of parameter sets that were loaded from the persistent store; each of these parameter sets
         *         describes for one race how it is to be loaded / tracked.
         */
        long getNumberOfParametersToLoad();
        
        /**
         * For each set of parameters obtained from the persistent store,
         * {@link DomainObjectFactory#loadConnectivityParametersForRacesToRestore(Consumer)} invokes a callback. This
         * method will return only after all these callbacks have been issued for all parameters loaded from the
         * persistent store.
         * <p>
         * 
         * Note that a callback implementation may itself trigger background actions. This method does not know about
         * those background actions and hence may return before those background actions have completed.
         */
        void waitForCompletionOfCallbacksForAllParameters() throws InterruptedException, ExecutionException;
    }
    
    /**
     * Loads all {@link RaceTrackingConnectivityParameters} objects from the database telling how to re-load those races
     * that were {@link MongoObjectFactory#addConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters)
     * marked as to be restored} before the server got re-started. Callers pass a {@code callback} that is invoked for
     * each parameters object retrieved from the persistent store. The call returns immediately; loading the parameters
     * happens in a background thread.
     * 
     * @param callback
     *            invoked for each connectivity params object successfully resolved; this pattern is preferred over a
     *            non-{@code void} return type because resolving the connectivity parameters objects itself happens
     *            through a callback pattern that may be triggered asynchronously by the services for handling the
     *            respective parameter types becoming available only later. For example, during the OSGi startup phase,
     *            when the {@code RacingEventService} is launched, not all persistence bundles will have run their
     *            activators; some may even depend on the {@code RacingEventService} and therefore won't start their
     *            activators before the activator of {@code RacingEventService} has completed. This would either result
     *            in a {@link NoCorrespondingServiceRegisteredException} or would have to be handled by not treating
     *            those connectivity parameter objects which defeats the whole purpose of this method.
     * @return the number of connectivity parameter objects found in the database; callers may use this to understand
     *         the progress with the {@code callback} invocations of which eventually there should be this many, at
     *         least if all handlers have ultimately become available.
     * 
     * @see MongoObjectFactory#addConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters)
     * @see MongoObjectFactory#removeConnectivityParametersForRaceToRestore(RaceTrackingConnectivityParameters)
     */
    ConnectivityParametersLoadingResult loadConnectivityParametersForRacesToRestore(
            Consumer<RaceTrackingConnectivityParameters> callback);

    RegattaLeaderboardWithEliminations loadRegattaLeaderboardWithEliminations(Document dbLeaderboard,
            String leaderboardName, String wrappedRegattaLeaderboardName, LeaderboardRegistry leaderboardRegistry);

    /**
     * Loads all stored anniversary races.
     */
    Map<? extends Integer, ? extends Pair<DetailedRaceInfo, AnniversaryType>> getAnniversaryData() throws MalformedURLException;
    
    TypeBasedServiceFinder<RaceTrackingConnectivityParametersHandler> getRaceTrackingConnectivityParamsServiceFinder();

    Wind loadWind(Document windDocument);

    Map<RaceIdentifier, MarkPassingRaceFingerprint> loadFingerprintsForMarkPassingHashes();
    
    Map<RaceIdentifier, ManeuverRaceFingerprint> loadFingerprintsForManeuverHashes();

    /**
     * For races that have a {@link MarkPassingRaceFingerprint} stored in the database (see {@link #loadFingerprintsForMarkPassingHashes()})
     * a caller can load the corresponding mark passings with this method.
     */
    Map<Competitor, Map<Waypoint, MarkPassing>> loadMarkPassings(RaceIdentifier raceIdentifier, Course course);
    
    Map<Competitor, List<Maneuver>> loadManeuvers(TrackedRace trackedRace, Course course);
}
