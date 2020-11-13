package com.sap.sailing.server.gateway.jaxrs.api;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.StreamingOutput;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.anniversary.DetailedRaceInfo;
import com.sap.sailing.domain.anniversary.SimpleRaceInfo;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sailing.server.gateway.serialization.impl.DetailedRaceInfoJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.SimpleRaceInfoJsonSerializer;

@Path("/v1/trackedRaces")
public class TrackedRaceListResource extends AbstractSailingServerResource {
    private static final String HEADER_NAME_CONTENT_TYPE = "Content-Type";
    private static final String CONTENT_TYPE_JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=UTF-8";

    private final SimpleRaceInfoJsonSerializer simpleRaceListJsonSerializer = new SimpleRaceInfoJsonSerializer();
    private final DetailedRaceInfoJsonSerializer detailedRaceListJsonSerializer = new DetailedRaceInfoJsonSerializer();

    /**
     * Allows to query for more details on a specific race, implemented to allow for example to retrieve more
     * information about an anniversary. This call works transitively by asking a server that is known to have the race
     * in question in case that the race isn't found locally.
     */
    @GET
    @Produces(CONTENT_TYPE_JSON_UTF8)
    @Path("raceDetails")
    public Response getDetailsForRace(@QueryParam("raceName") String raceName,
            @QueryParam("regattaName") String regattaName) {
        final DetailedRaceInfo detailedRaceInfo = getService().getFullDetailsForRaceLocal(new RegattaNameAndRaceName(regattaName, raceName));
        if (detailedRaceInfo == null) {
            return Response.status(Status.NOT_FOUND).header(HEADER_NAME_CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8).build();
        }
        return getJsonResponse(streamingOutput(detailedRaceListJsonSerializer.serialize(detailedRaceInfo)));
    }

    /**
     * Returns a list of tracked races. By default, only TrackedRaces from the local instance are returned. The entries
     * are grouped by the remote URL from where they originated. Local entries have a {@code null} value for the
     * {@link DetailedRaceInfoJsonSerializer# FIELD_REMOTEURL remote URL} field. The order of the list returned is
     * undefined.<br>
     * Optionally a list of event UUIDs together with a predicate can be provided. The returned races list will be
     * filtered by the given ids. The predicate specifies the behavior of the filter.
     *
     * @param transitive
     *            when true indicates that the cached list of remote references shall be considered
     * @param events
     *            string list of event UUIDs
     * @param predicate
     *            depicts the semantic of the filtering, when "incl" only races belonging to the depicted event UUIDs
     *            are returned. When "excl" is provided the filtering behaves vice versa.
     */
    @GET
    @Produces(CONTENT_TYPE_JSON_UTF8)
    @Path("getRaces")
    public Response raceList(@QueryParam("transitive") @DefaultValue("false") Boolean transitive,
            @QueryParam("events") @DefaultValue("") String strEvents,
            @QueryParam("pred") @DefaultValue("excl") String predicate) {
        final boolean includeRemotes = transitive != null && Boolean.TRUE.equals(transitive);
        final Set<UUID> eventUUIDs = Arrays.asList(strEvents.split(","))
                .stream()
                .filter(StringUtils::isNotBlank)
                .map(UUID::fromString)
                .collect(Collectors.toSet());
        final Predicate<UUID> eventFilter;
        if ("incl".equals(predicate)) {
            eventFilter = (uuid)->eventUUIDs.contains(uuid);
        } else if ("excl".equals(predicate)) {
            eventFilter = (uuid)->!eventUUIDs.contains(uuid);
        }else {
            throw new IllegalArgumentException("unrecognized predicate " + predicate + " only \"excl\" and \"incl\" are possible");
        }
        final Map<RegattaAndRaceIdentifier, Set<SimpleRaceInfo>> distinctRaces = getDistinctRaces(includeRemotes,
                eventFilter);
        final HashMap<String, List<SimpleRaceInfo>> raceData = new HashMap<>();
        distinctRaces.values()
            .stream()
            .flatMap(Set::stream)
                .forEach(raceInfo -> {
                    final String remoteUrl = raceInfo.getRemoteUrl() == null ? null
                            : raceInfo.getRemoteUrl().toExternalForm();
                    List<SimpleRaceInfo> remoteList = raceData.get(remoteUrl);
                    if (remoteList == null) {
                        raceData.put(remoteUrl, remoteList = new ArrayList<>());
                    }
                    remoteList.add(raceInfo);
                });
        final JSONArray json = new JSONArray();
        for (Entry<String, List<SimpleRaceInfo>> raced : raceData.entrySet()) {
            JSONArray list = new JSONArray();
            for (SimpleRaceInfo simpleRaceInfo : raced.getValue()) {
                list.add(simpleRaceListJsonSerializer.serialize(simpleRaceInfo));
            }
            final JSONObject remote = new JSONObject();
            remote.put(DetailedRaceInfoJsonSerializer.FIELD_REMOTEURL, raced.getKey());
            remote.put(DetailedRaceInfoJsonSerializer.FIELD_RACES, list);
            json.add(remote);
        }
        return getJsonResponse(streamingOutput(json));
    }

    /**
     * Returns a list of all locally and remote tracked races that are currently known. The list is sorted by the
     * {@link SimpleRaceInfoJsonSerializer#FIELD_START_OF_RACE} field, and each {@link SimpleRaceInfo} object is put
     * together with an incrementing number starting at 0. Duplicate races are eliminated such that local copies take
     * precedence over remote ones. Races hosted on the server on which this method is invoked will be grouped under the
     * {@code null} value for the {@code remoteUrl} field.
     */
    @GET
    @Produces(CONTENT_TYPE_JSON_UTF8)
    @Path("allRaces")
    public Response fullRaceList() {
        JSONArray json = new JSONArray();
        Map<RegattaAndRaceIdentifier, Set<SimpleRaceInfo>> store = getDistinctRaces(/* include remotes */ true, (uuid)->true);
        List<SimpleRaceInfo> sorted = store.values().stream()
            .flatMap(races->races.stream())
            .sorted((o1,o2)->o1.getStartOfRace().compareTo(o2.getStartOfRace()))
            .collect(Collectors.toList());

        for (int i = 0; i < sorted.size(); i++) {
            SimpleRaceInfo current = sorted.get(i);
            JSONObject raceInfo = new JSONObject();
            raceInfo.put("racenumber", String.valueOf(i));
            final URL remoteUrl = current.getRemoteUrl();
            raceInfo.put("remoteUrl", remoteUrl == null ? null : remoteUrl.toExternalForm());
            raceInfo.put("raceinfo", simpleRaceListJsonSerializer.serialize(current));
            json.add(raceInfo);
        }
        return getJsonResponse(streamingOutput(json));
    }

    private Map<RegattaAndRaceIdentifier, Set<SimpleRaceInfo>>  getDistinctRaces(boolean includeRemotes, Predicate<UUID> eventListFilter) {
        final Map<RegattaAndRaceIdentifier, Set<SimpleRaceInfo>> distinctRaces = new HashMap<>();
        if (includeRemotes) {
            distinctRaces.putAll(getService().getRemoteRaceList(eventListFilter));
        }

        Map<RegattaAndRaceIdentifier, Set<SimpleRaceInfo>> localRaces = getService().getLocalRaceList(eventListFilter);
        localRaces.forEach((identifier, simpleRaceInfoSet) -> distinctRaces.compute(identifier, (key, valueSet) -> {
            Set<SimpleRaceInfo> mergedSet;
            if (valueSet != null) {
                valueSet.addAll(simpleRaceInfoSet);
                mergedSet = valueSet;
            } else {
                mergedSet = simpleRaceInfoSet;
            }
            return mergedSet;
        }));
        return distinctRaces;
    }

    private Response getJsonResponse(StreamingOutput json) {
        return Response.ok(json).header(HEADER_NAME_CONTENT_TYPE, CONTENT_TYPE_JSON_UTF8).build();
    }
}
