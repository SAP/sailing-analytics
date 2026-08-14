package com.sap.sailing.server.gateway.interfaces;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Optional;
import java.util.UUID;

import org.apache.http.client.ClientProtocolException;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sse.security.util.SecuredServer;
import com.sap.sse.shared.json.JsonDeserializationException;

/**
 * Represents a remote instance of a server process or an entire application replica set with a master and zero or more
 * replicas, running the Sailing Analytics, and exposes various methods as a convenient Java API which are implemented
 * using the remote server's REST API. In short, this is a Java facade for a REST API.
 * <p>
 * 
 * Objects of this type manage authentication information required for executing its methods as part of their immutable
 * state, therefore at object construction time. Authentication information may be provided in the form of
 * username/password which can then be used to obtain a bearer token for remote access. Or the bearer token for the
 * server can be provided explicitly. Or, as the default, the constructor checks whether running in the scope of an
 * authenticated session and then uses that session user's bearer token, assuming that the server to be represented by
 * this object shares its security service with the server where this object is constructed.
 * <p>
 * 
 * Constructs objects whose type conforms with this interface by using {@link SailingServerFactory}.
 * 
 * @author Axel Uhl (d043530)
 */
public interface SailingServer extends SecuredServer {
    /**
     * The server's base URL, ending with a slash "/" character
     */
    URL getBaseUrl();
    
    Iterable<UUID> getLeaderboardGroupIds() throws Exception;
    
    Iterable<String> getLeaderboardNames() throws Exception;
    
    String getLeaderboardShardingKey(String leaderboardName) throws Exception;
    
    String getLeaderboardFromShardingKey(String shardingKey) throws Exception;

    Iterable<UUID> getEventIds() throws Exception;

    /**
     * Fetches all public events from the remote server, reusing the {@code /v1/events} endpoint which serializes
     * venue and course areas.
     */
    Iterable<EventBase> getEvents() throws Exception;

    MasterDataImportResult importMasterData(SailingServer from, Iterable<UUID> leaderboardGroupIds, boolean override,
            boolean compress, boolean exportWind, boolean exportDeviceConfigs,
            boolean exportTrackedRacesAndStartTracking, Optional<UUID> progressTrackingUuid) throws Exception;
    
    DataImportProgress getMasterDataImportProgress(UUID progressTrackingUuid) throws Exception;

    /**
     * Compares all {@link #getLeaderboardGroupIds() leaderboard groups} of {@code a} (or this server, if {@code a} is
     * not present) with the corresponding leaderboard groups in {@code b}. The result of the comparison is returned.
     * Note that this way leaderboard groups may exist in {@code b} that don't exist in {@code a} without producing a
     * 
     * @param a
     *            if not present, this server is compared against {@code b}
     * @param leaderboardGroupIds
     *            if present, specifies which leaderboard groups shall be compared; otherwise, all leaderboard groups of
     *            both servers are compared, and those missing on either end will be reported
     */
    CompareServersResult compareServers(Optional<SailingServer> a, SailingServer b, Optional<Iterable<UUID>> leaderboardGroupIds) throws Exception;

    /**
     * Obtains the {@link RemoteSailingServerReference}s established in this server. These references point to
     * other servers / application replica sets and make those other servers' content visible in the events list
     * on this server. As users navigate to those other servers' events they leave the scope of this server. The
     * remote references can optionally specify a set of events to include / exclude. 
     */
    Iterable<RemoteSailingServerReference> getRemoteServerReferences() throws JsonDeserializationException,
            MalformedURLException, ClientProtocolException, IOException, ParseException;

    /**
     * Adds a remote sailing server reference to this server. If {@code includeSpecifiedEvents} is {@code true}, events
     * must explicitly be specified in later calls to make the reference take effect. If {@code false}, all events on the
     * remote server will be included by default, and event IDs can later be specified for individual exclusion.<p>
     * 
     * Should a remote sailing server reference already exist to the {@code referencedServer} specified, no change is
     * performed, even if the existing remote server reference has an "include/exclude" specification that deviates
     * from {@code includeSpecifiedEvents}.
     * 
     * @return the reference added or the existing reference found
     */
    RemoteSailingServerReference addRemoteServerReference(SailingServer referencedServer, boolean includeSpecifiedEvents)
            throws JsonDeserializationException, ClientProtocolException, IOException, ParseException;
    
    /**
     * @return the reference removed, or {@code null} if no such reference was found
     */
    RemoteSailingServerReference removeRemoteServerReference(SailingServer referencedServer)
            throws JsonDeserializationException, ClientProtocolException, IOException, ParseException;
    
    /**
     * Ensures that a remote sailing server reference to {@code referencedServer} exists and includes the events
     * with the IDs specified in {@code eventIds}. If no remote sailing server reference to that server exists yet,
     * a new one is created and configured to explicitly include only the events with the IDs from {@code eventIds}.<p>
     * 
     * If a reference to {@code referencedServer} already exists, this method will ensure that the events with the IDs provided
     * by {@code eventIds} are part of the exposed events, either if the reference is "inclusive" by ensuring those IDs are
     * part of the event ID list, or if "exclusive" by ensuring they are <em>not</em> part of the event ID list excluded.
     * 
     * @return the resulting remote sailing server reference
     */
    RemoteSailingServerReference addRemoteServerEventReferences(SailingServer referencedServer, Iterable<UUID> eventIds) throws Exception;

    /**
     * Ensures that <em>no</em> remote sailing server reference to {@code referencedServer} exists that includes the
     * events with the IDs specified in {@code eventIds}. If a remote sailing server reference to that server exists,
     * this method will ensure that the events with the IDs provided by {@code eventIds} are not part of the exposed
     * events, either if the reference is "inclusive" by ensuring those IDs are not part of the event ID list, or if
     * "exclusive" by ensuring they <em>are</em> part of the event ID list excluded. If the event IDs are removed from
     * an existing "inclusive" reference and this makes the reference expose no events anymore at all, the reference is
     * removed.
     * 
     * @return the resulting remote sailing server reference, or {@code null} if the remote sailing server reference was
     *         removed as a result of calling this method
     */
    RemoteSailingServerReference removeRemoteServerEventReferences(SailingServer referencedServer, Iterable<UUID> eventIds) throws Exception;
}
