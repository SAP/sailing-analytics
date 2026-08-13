package com.sap.sailing.server.gateway.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.entity.EntityBuilder;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.message.BasicNameValuePair;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.common.sharding.ShardingType;
import com.sap.sailing.server.gateway.deserialization.impl.CompareServersResultJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CourseAreaJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.DataImportProgressJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.EventBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.LeaderboardGroupBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.MasterDataImportResultJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.RemoteSailingServerReferenceJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.TrackingConnectorInfoJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.VenueJsonDeserializer;
import com.sap.sailing.server.gateway.interfaces.CompareServersResult;
import com.sap.sailing.server.gateway.interfaces.MasterDataImportResult;
import com.sap.sailing.server.gateway.interfaces.SailingServer;
import com.sap.sailing.server.gateway.jaxrs.api.CompareServersResource;
import com.sap.sailing.server.gateway.jaxrs.api.EventsResource;
import com.sap.sailing.server.gateway.jaxrs.api.LeaderboardGroupsResource;
import com.sap.sailing.server.gateway.jaxrs.api.LeaderboardsResource;
import com.sap.sailing.server.gateway.jaxrs.api.MasterDataImportResource;
import com.sap.sailing.server.gateway.jaxrs.api.RemoteServerReferenceResource;
import com.sap.sailing.server.gateway.serialization.LeaderboardGroupConstants;
import com.sap.sailing.server.gateway.serialization.impl.EventBaseJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.MasterDataImportResultJsonSerializer;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.util.impl.SecuredServerImpl;
import com.sap.sse.shared.json.JsonDeserializationException;

public class SailingServerImpl extends SecuredServerImpl implements SailingServer {
    private static final Logger logger = Logger.getLogger(SailingServerImpl.class.getName());
    private final String GATEWAY_URL_PREFIX = "sailingserver/api";
    
    public SailingServerImpl(URL baseUrl, String bearerToken) {
        super(baseUrl, bearerToken);
    }

    @Override
    public Iterable<UUID> getLeaderboardGroupIds() throws ClientProtocolException, IOException, ParseException {
        final URL leaderboardGroupsUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX+LeaderboardGroupsResource.V1_LEADERBOARDGROUPS+LeaderboardGroupsResource.IDENTIFIABLE);
        final HttpGet getLeaderboardGroups = new HttpGet(leaderboardGroupsUrl.toString());
        final JSONArray jsonResponse = (JSONArray) getJsonParsedResponse(getLeaderboardGroups).getA();
        return Util.map(jsonResponse, o->UUID.fromString(((JSONObject) o).get(LeaderboardGroupConstants.ID).toString()));
    }

    @Override
    public Iterable<String> getLeaderboardNames() throws Exception {
        final URL leaderboardsUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX+LeaderboardsResource.V1_LEADERBOARDS);
        final HttpGet getLeaderboards = new HttpGet(leaderboardsUrl.toString());
        final Pair<Object, Integer> jsonParsedResponse = getJsonParsedResponse(getLeaderboards);
        final JSONArray jsonResponse = (JSONArray) jsonParsedResponse.getA();
        if (jsonResponse == null) {
            throw new IllegalAccessError("Error accessing leaderboard names; response status "+jsonParsedResponse.getB());
        }
        return Util.map(jsonResponse, o->o.toString());
    }

    @Override
    public String getLeaderboardShardingKey(String leaderboardName) throws Exception {
        // We could try to acquire this from the "leaderboards" REST API endpoint, field shardingLeaderboardName,
        // but we can as well shortcut it by replicating the implementation here:
        return ShardingType.LEADERBOARDNAME.encodeShardingInfo(leaderboardName);
    }
    
    /**
     * Establishes a map with all leaderboardnames and their keys and takes the value for the according shardingkey.
     */
    @Override
    public String getLeaderboardFromShardingKey(String shardingKey) throws Exception {
        Map<String, String> mapping = new HashMap<>();
        for (String leaderboard : getLeaderboardNames()) {
            mapping.put(getLeaderboardFromShardingKey(leaderboard), leaderboard);
        }
        return mapping.get(shardingKey);
    }
    
    @Override
    public Iterable<UUID> getEventIds() throws ClientProtocolException, IOException, ParseException {
        final URL eventsUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX+EventsResource.V1_EVENTS);
        final HttpGet getEvents = new HttpGet(eventsUrl.toString());
        final JSONArray jsonResponse = (JSONArray) getJsonParsedResponse(getEvents).getA();
        return Util.map(jsonResponse, o->UUID.fromString(((JSONObject) o).get(EventBaseJsonSerializer.FIELD_ID).toString()));
    }

    @Override
    public Iterable<EventBase> getEvents() throws ClientProtocolException, IOException, ParseException, JsonDeserializationException {
        final URL eventsUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX+EventsResource.V1_EVENTS);
        final HttpGet getEvents = new HttpGet(eventsUrl.toString());
        final JSONArray jsonResponse = (JSONArray) getJsonParsedResponse(getEvents).getA();
        final EventBaseJsonDeserializer deserializer = new EventBaseJsonDeserializer(
                new VenueJsonDeserializer(new CourseAreaJsonDeserializer(com.sap.sailing.domain.base.DomainFactory.INSTANCE)),
                new LeaderboardGroupBaseJsonDeserializer(),
                new TrackingConnectorInfoJsonDeserializer());
        final List<EventBase> result = new ArrayList<>();
        for (final Object o : jsonResponse) {
            result.add(deserializer.deserialize((JSONObject) o));
        }
        return result;
    }

    @Override
    public MasterDataImportResult importMasterData(SailingServer from, Iterable<UUID> leaderboardGroupIds,
            boolean override, boolean compress, boolean exportWind, boolean exportDeviceConfigs,
            boolean exportTrackedRacesAndStartTracking, Optional<UUID> progressTrackingUuid) throws ClientProtocolException, IOException, ParseException {
        final URL mdiUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX + MasterDataImportResource.V1_MASTERDATAIMPORT);
        final List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(MasterDataImportResource.REMOTE_SERVER_URL_FORM_PARAM, from.getBaseUrl().toString()));
        params.add(new BasicNameValuePair(MasterDataImportResource.REMOTE_SERVER_BEARER_TOKEN_FORM_PARAM, from.getBearerToken()));
        for (final UUID leaderboardGroupId : leaderboardGroupIds) {
            params.add(new BasicNameValuePair(MasterDataImportResultJsonSerializer.LEADERBOARDGROUP_UUID_FORM_PARAM, leaderboardGroupId.toString()));
        }
        params.add(new BasicNameValuePair(MasterDataImportResultJsonSerializer.OVERRIDE_FORM_PARAM, Boolean.toString(override)));
        params.add(new BasicNameValuePair(MasterDataImportResultJsonSerializer.COMPRESS_FORM_PARAM, Boolean.toString(compress)));
        params.add(new BasicNameValuePair(MasterDataImportResultJsonSerializer.EXPORT_WIND_FORM_PARAM, Boolean.toString(exportWind)));
        params.add(new BasicNameValuePair(MasterDataImportResultJsonSerializer.EXPORT_DEVICE_CONFIGS_FORM_PARAM, Boolean.toString(exportDeviceConfigs)));
        params.add(new BasicNameValuePair(MasterDataImportResultJsonSerializer.EXPORT_TRACKED_RACES_AND_START_TRACKING_FORM_PARAM, Boolean.toString(exportTrackedRacesAndStartTracking)));
        progressTrackingUuid.ifPresent(ptid->params.add(new BasicNameValuePair(MasterDataImportResource.PROGRESS_TRACKING_UUID_FORM_PARAM, ptid.toString())));
        final HttpPost importMasterData = new HttpPost(mdiUrl.toString());
        importMasterData.setEntity(EntityBuilder.create()
                .setContentType(ContentType.APPLICATION_FORM_URLENCODED).setParameters(params).build());
        final Pair<Object, Integer> responseAndStatus = getJsonParsedResponse(importMasterData);
        final MasterDataImportResult result;
        if (responseAndStatus.getB() >= 200 && responseAndStatus.getB() < 300) {
            logger.info("Received an OK status "+responseAndStatus.getB()+" for MDI from "+from);
            final JSONObject jsonResponse = (JSONObject) responseAndStatus.getA();
            result = new MasterDataImportResultJsonDeserializer().deserialize(jsonResponse);
        } else {
            logger.warning("Received a non-OK status "+responseAndStatus.getB()+" for MDI from "+from);
            result = null;
        }
        return result;
    }

    @Override
    public DataImportProgress getMasterDataImportProgress(UUID progressTrackingUuid) throws ClientProtocolException, IOException, ParseException {
        final URL progressUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX + MasterDataImportResource.V1_MASTERDATAIMPORT + MasterDataImportResource.PROGRESS+
                "?"+MasterDataImportResource.PROGRESS_TRACKING_UUID+"="+progressTrackingUuid);
        final HttpGet getMasterDataImportProgress = new HttpGet(progressUrl.toString());
        final JSONObject jsonResponse = (JSONObject) getJsonParsedResponse(getMasterDataImportProgress).getA();
        return new DataImportProgressJsonDeserializer().deserialize(jsonResponse);
    }

    @Override
    public CompareServersResult compareServers(Optional<SailingServer> a, SailingServer b, Optional<Iterable<UUID>> leaderboardGroupIds) throws ClientProtocolException, IOException, ParseException {
        final URL compareServersUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX + CompareServersResource.V1_COMPARESERVERS);
        final List<NameValuePair> params = new ArrayList<>();
        a.ifPresent(aa->{
            params.add(new BasicNameValuePair(CompareServersResource.BEARER1_FORM_PARAM, aa.getBearerToken()));
            params.add(new BasicNameValuePair(CompareServersResource.SERVER1_FORM_PARAM, aa.getBaseUrl().toString()));
        });
        if (b.getBearerToken() != null) {
            params.add(new BasicNameValuePair(CompareServersResource.BEARER2_FORM_PARAM, b.getBearerToken()));
        }
        params.add(new BasicNameValuePair(CompareServersResource.SERVER2_FORM_PARAM, b.getBaseUrl().toString()));
        leaderboardGroupIds.ifPresent(lgids->{
            for (final UUID leaderboardGroupId : lgids) {
                params.add(new BasicNameValuePair(CompareServersResource.LEADERBOARDGROUP_UUID_FORM_PARAM, leaderboardGroupId.toString()));
            }
        });
        final HttpPost compareServersPostRequest = new HttpPost(compareServersUrl.toString());
        compareServersPostRequest.setEntity(EntityBuilder.create().setContentType(ContentType.APPLICATION_FORM_URLENCODED)
                .setParameters(params).build());
        final JSONObject jsonResponse = (JSONObject) getJsonParsedResponse(compareServersPostRequest).getA();
        return new CompareServersResultJsonDeserializer().deserialize(jsonResponse);
    }
    
    @Override
    public Iterable<RemoteSailingServerReference> getRemoteServerReferences() throws ClientProtocolException, IOException, ParseException {
        final URL getRemoteReferencesUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX + RemoteServerReferenceResource.V1_REMOTESERVERREFERENCE);
        final HttpGet getRequest = new HttpGet(getRemoteReferencesUrl.toString());
        final JSONArray remoteServerReferencesJson = (JSONArray) getJsonParsedResponse(getRequest).getA();
        final Set<RemoteSailingServerReference> result = new HashSet<>();
        for (final Object o : remoteServerReferencesJson) {
            final RemoteSailingServerReference ref = new RemoteSailingServerReferenceJsonDeserializer().deserialize((JSONObject) o);
            result.add(ref);
        }
        return result;
    }

    @Override
    public RemoteSailingServerReference addRemoteServerReference(SailingServer referencedServer, boolean includeSpecifiedEvents) throws JsonDeserializationException, ClientProtocolException, IOException, ParseException {
        final URL addRemoteReferenceUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX + RemoteServerReferenceResource.V1_REMOTESERVERREFERENCE+RemoteServerReferenceResource.ADD);
        final List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_URL, referencedServer.getBaseUrl().toString()));
        params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_NAME, referencedServer.getBaseUrl().toString()));
        params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_IS_INCLUDE, Boolean.toString(includeSpecifiedEvents)));
        final HttpPost addRemoteReference = new HttpPost(addRemoteReferenceUrl.toString());
        addRemoteReference.setEntity(EntityBuilder.create().setContentType(ContentType.APPLICATION_FORM_URLENCODED)
                .setParameters(params).build());
        return new RemoteSailingServerReferenceJsonDeserializer().deserialize((JSONObject) getJsonParsedResponse(addRemoteReference).getA());
    }

    @Override
    public RemoteSailingServerReference removeRemoteServerReference(SailingServer referencedServer) throws JsonDeserializationException, ClientProtocolException, IOException, ParseException {
        final HttpPost removeRequest = new HttpPost(
                new URL(getBaseUrl(), GATEWAY_URL_PREFIX + RemoteServerReferenceResource.V1_REMOTESERVERREFERENCE
                        + RemoteServerReferenceResource.REMOVE).toString());
        final List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_URL, referencedServer.getBaseUrl().toString()));
        removeRequest.setEntity(EntityBuilder.create().setContentType(ContentType.APPLICATION_FORM_URLENCODED)
                .setParameters(params).build());
        final Pair<Object, Integer> responseAndStatus = getJsonParsedResponse(removeRequest);
        if (responseAndStatus.getB() >= 200 && responseAndStatus.getB() < 300) {
            return new RemoteSailingServerReferenceJsonDeserializer().deserialize((JSONObject) responseAndStatus.getA());
        } else {
            throw new IllegalArgumentException("Received "+responseAndStatus.getA()+" from server with status code "+responseAndStatus.getB());
        }
    }

    @Override
    public RemoteSailingServerReference addRemoteServerEventReferences(SailingServer referencedServer,
            Iterable<UUID> eventIds) throws Exception {
        final RemoteSailingServerReference result;
        final String referencedServerBaseUrl = referencedServer.getBaseUrl().toString();
        final RemoteSailingServerReference existingRef = getExistingRemoteServerReference(referencedServerBaseUrl);
        if (existingRef == null) {
            final URL addRemoteReferenceUrl = new URL(getBaseUrl(), GATEWAY_URL_PREFIX + RemoteServerReferenceResource.V1_REMOTESERVERREFERENCE+RemoteServerReferenceResource.ADD);
            final List<NameValuePair> params = new ArrayList<>();
            params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_URL, referencedServerBaseUrl));
            params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_NAME, referencedServerBaseUrl));
            params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_IS_INCLUDE, Boolean.toString(true)));
            final HttpPost addRemoteReference = new HttpPost(addRemoteReferenceUrl.toString());
            addRemoteReference.setEntity(EntityBuilder.create().setContentType(ContentType.APPLICATION_FORM_URLENCODED)
                    .setParameters(params).build());
            result = new RemoteSailingServerReferenceJsonDeserializer().deserialize((JSONObject) getJsonParsedResponse(addRemoteReference).getA());
        } else {
            if (existingRef.isInclude()) {
                final Set<UUID> eventsToAdd = new HashSet<>();
                Util.addAll(eventIds, eventsToAdd);
                Util.removeAll(existingRef.getSelectedEventIds(), eventsToAdd);
                if (!eventsToAdd.isEmpty()) {
                    eventsToAdd.addAll(existingRef.getSelectedEventIds());
                    existingRef.updateSelectedEventIds(eventsToAdd);
                    doRemoteServerReferenceUpdate(existingRef);
                    result = existingRef;
                } else {
                    // nothing to do, all events migrated are already referenced by the inclusive reference
                    result = existingRef;
                }
            } else {
                // exclusive; are any of those events we migrated excluded explicitly? If so, "un-exclude":
                final Set<UUID> eventsToUnexclude = new HashSet<>();
                Util.addAll(eventIds, eventsToUnexclude);
                Util.retainAll(existingRef.getSelectedEventIds(), eventsToUnexclude);
                if (!eventsToUnexclude.isEmpty()) {
                    final Set<UUID> eventsToExclude = new HashSet<>(existingRef.getSelectedEventIds());
                    Util.removeAll(eventIds, eventsToExclude);
                    existingRef.updateSelectedEventIds(eventsToExclude);
                    result = doRemoteServerReferenceUpdate(existingRef);
                } else {
                    // nothing to do, all events migrated are already referenced by the inclusive reference
                    result = existingRef;
                }
            }
        }
        return result;
    }

    @Override
    public RemoteSailingServerReference removeRemoteServerEventReferences(SailingServer referencedServer, Iterable<UUID> eventIds) throws Exception {
        final RemoteSailingServerReference existingRef = getExistingRemoteServerReference(referencedServer.getBaseUrl().toString());
        final RemoteSailingServerReference result;
        if (existingRef != null) {
            if (existingRef.isInclude()) {
                final Set<UUID> eventIdsToKeep = new HashSet<>(existingRef.getSelectedEventIds());
                Util.removeAll(eventIds, eventIdsToKeep);
                if (eventIdsToKeep.isEmpty()) {
                    // remove ref entirely
                    removeRemoteServerReference(referencedServer);
                    result = null;
                } else {
                    existingRef.updateSelectedEventIds(eventIdsToKeep);
                    doRemoteServerReferenceUpdate(existingRef);
                    result = existingRef;
                }
            } else {
                // check if more events now need to be excluded
                if (!Util.containsAll(existingRef.getSelectedEventIds(), eventIds)) {
                    final Set<UUID> extendedEventIdsToExclude = new HashSet<>(existingRef.getSelectedEventIds());
                    Util.addAll(eventIds, extendedEventIdsToExclude);
                    existingRef.updateSelectedEventIds(extendedEventIdsToExclude);
                    doRemoteServerReferenceUpdate(existingRef);
                    result = existingRef;
                } else {
                    // everything that needs to be excluded is already excluded; nothing more to do
                    result = existingRef;
                }
            }
        } else {
            // no reference to serverUrlToDelete found, therefore nothing to delete
            result = existingRef;
        }
        return result;
    }

    private RemoteSailingServerReference doRemoteServerReferenceUpdate(RemoteSailingServerReference updatedRef) throws IOException, Exception {
        final HttpPut updateRequest = new HttpPut(
                new URL(getBaseUrl(), GATEWAY_URL_PREFIX + RemoteServerReferenceResource.V1_REMOTESERVERREFERENCE
                        + RemoteServerReferenceResource.UPDATE).toString());
        final List<NameValuePair> params = new ArrayList<>();
        params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_NAME, updatedRef.getName()));
        params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_IS_INCLUDE, Boolean.toString(updatedRef.isInclude())));
        for (final UUID eventId : updatedRef.getSelectedEventIds()) {
            params.add(new BasicNameValuePair(RemoteServerReferenceResource.REMOTE_SERVER_EVENT_IDS, eventId.toString()));
        }
        updateRequest.setEntity(EntityBuilder.create().setContentType(ContentType.APPLICATION_FORM_URLENCODED).setParameters(params).build());
        return new RemoteSailingServerReferenceJsonDeserializer().deserialize((JSONObject) getJsonParsedResponse(updateRequest).getA());
    }

    private RemoteSailingServerReference getExistingRemoteServerReference(final String remoteReferenceServerUrl) throws MalformedURLException, Exception {
        final URL remoteReferenceServerUrlAsUrl = new URL(remoteReferenceServerUrl);
        final URL remoteReferenceServerUrlWithoutTrailingSlash = remoteReferenceServerUrl.endsWith("/")
                ? new URL(remoteReferenceServerUrl.substring(0, remoteReferenceServerUrl.length() - 1))
                : remoteReferenceServerUrlAsUrl;
        for (final RemoteSailingServerReference ref : getRemoteServerReferences()) {
            if (ref.getURL().equals(remoteReferenceServerUrlAsUrl) || ref.getURL().equals(remoteReferenceServerUrlWithoutTrailingSlash)) {
                return ref;
            }
        }
        return null;
    }
}
