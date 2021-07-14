package com.sap.sailing.server.impl;

import static com.sap.sse.common.HttpRequestHeaderConstants.HEADER_FORWARD_TO_REPLICA;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.anniversary.DetailedRaceInfo;
import com.sap.sailing.domain.anniversary.SimpleRaceInfo;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.EventBase;
import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.statistics.Statistics;
import com.sap.sailing.server.gateway.deserialization.impl.CourseAreaJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.EventBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.LeaderboardGroupBaseJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.StatisticsByYearJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.StatisticsJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.TrackingConnectorInfoJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.VenueJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.DetailedRaceInfoJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.SimpleRaceInfoJsonSerializer;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.concurrent.LockUtil;
import com.sap.sse.concurrent.NamedReentrantReadWriteLock;
import com.sap.sse.util.HttpUrlConnectionHelper;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * A set of {@link RemoteSailingServerReference}s including a cache of their {@link EventBase events} that is
 * periodically updated.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class RemoteSailingServerSet {
    private static final int POLLING_INTERVAL_IN_SECONDS = 60;
    private NamedReentrantReadWriteLock lock = new NamedReentrantReadWriteLock("lock for RemoteSailingServerSet", /* fair */ true);

    private static final Logger logger = Logger.getLogger(RemoteSailingServerSet.class.getName());

    /**
     * Holds the remote server references managed by this set. Keys are the
     * {@link RemoteSailingServerReference#getName() names} of the server references.
     */
    private final ConcurrentMap<String, RemoteSailingServerReference> remoteSailingServers;

    private final ConcurrentMap<RemoteSailingServerReference, Util.Pair<Iterable<EventBase>, Exception>> cachedEventsForRemoteSailingServers;

    private final ConcurrentMap<RemoteSailingServerReference, Util.Pair<Map<Integer, Statistics>, Exception>> cachedStatisticsByYearForRemoteSailingServers;
    private final StatisticsJsonDeserializer statisticsJsonDeserializer;
    
    /**
     * Tracked races content is not replicated, because only the master (e.g. archive) determines anniversaries. The
     * results of this determination are replicated. In a replica, {@link #cachedTrackedRacesForRemoteSailingServers}
     * remains empty, as no remote servers are queried for their tracked races lists.
     */
    private final ConcurrentMap<RemoteSailingServerReference, Util.Pair<Iterable<SimpleRaceInfo>, Exception>> cachedTrackedRacesForRemoteSailingServers;
    
    /**
     * {@link Set} of {@link Runnable callback}s, which are called when retrieving tracked races from a remote server
     */
    private final Set<Runnable> remoteRaceResultReceivedCallbacks = ConcurrentHashMap.newKeySet();
    
    /**
     * {@link AtomicBoolean Flag} to enable or disable retrieval of tracked races from remote servers. Usually
     * <code>false</code>/disable for replicas in order to save bandwidth and CPU.
     */
    private final AtomicBoolean retrieveRemoteRaceResult = new AtomicBoolean(true);
    
    private final ConcurrentMap<RemoteSailingServerReference, Future<?>> runningUpdateTasksPerServerReference;

    /**
     * @param scheduler
     *            Used to schedule the periodic updates of the {@link #cachedEventsForRemoteSailingServers event cache}
     */
    public RemoteSailingServerSet(ScheduledExecutorService scheduler, SharedDomainFactory<?> baseDomainFactory) {
        this.statisticsJsonDeserializer = StatisticsJsonDeserializer.create(baseDomainFactory);
        remoteSailingServers = new ConcurrentHashMap<>();
        cachedEventsForRemoteSailingServers = new ConcurrentHashMap<>();
        cachedStatisticsByYearForRemoteSailingServers = new ConcurrentHashMap<>();
        cachedTrackedRacesForRemoteSailingServers = new ConcurrentHashMap<>();
        runningUpdateTasksPerServerReference = new ConcurrentHashMap<>();
        scheduler.scheduleAtFixedRate(new Runnable() {
            @Override
            public void run() {
                updateRemoteSailingServerReferenceEventCaches();
            }
        }, /* initialDelay */ 0, POLLING_INTERVAL_IN_SECONDS, TimeUnit.SECONDS);
    }

    public void clear() {
        LockUtil.executeWithWriteLock(lock, () -> {
            remoteSailingServers.clear();
            cachedEventsForRemoteSailingServers.clear();
            cachedStatisticsByYearForRemoteSailingServers.clear();
            cachedTrackedRacesForRemoteSailingServers.clear();
        });
    }

    public void add(RemoteSailingServerReference remoteSailingServerReference) {
        LockUtil.executeWithWriteLock(lock, () -> {
            if (remoteSailingServers.containsKey(remoteSailingServerReference.getName())) {
                remove(remoteSailingServerReference.getName());
            }
            remoteSailingServers.put(remoteSailingServerReference.getName(), remoteSailingServerReference);
            triggerAsynchronousEventCacheUpdate(remoteSailingServerReference);
        });
    }

    private void updateRemoteSailingServerReferenceEventCaches() {
        LockUtil.executeWithReadLock(lock, () -> {
            for (RemoteSailingServerReference ref : remoteSailingServers.values()) {
                triggerAsynchronousEventCacheUpdate(ref);
            }
        });
    }

    /**
     * If this set has a {@link RemoteSailingServerReference} whose {@link RemoteSailingServerReference#getName() name}
     * equals <code>name</code>, it is returned. Otherwise, <code>null</code> is returned.
     */
    public RemoteSailingServerReference getServerReferenceByName(String name) {
        return remoteSailingServers.get(name);
    }
    
    /**
     * @return a snapshot copy of the internal map with all remote sailing server references currently known, regardless
     *         of whether or not they are {@link #getLiveRemoteServerReferences() live}.
     */
    public Map<String, RemoteSailingServerReference> getAllRemoteServerReferences() {
        return new HashMap<>(remoteSailingServers);
    }

    private void triggerAsynchronousEventCacheUpdate(final RemoteSailingServerReference ref) {
        final Future<?> lastJobForRef = runningUpdateTasksPerServerReference.get(ref);
        if (lastJobForRef == null || lastJobForRef.isDone()) {
            runningUpdateTasksPerServerReference.put(ref,
                    ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor().submit(()->{
                        updateRemoteServerEventCacheSynchronously(ref);
                        updateRemoteServerStatisticsCacheSynchronously(ref);
                        if (retrieveRemoteRaceResult.get()) {
                            updateRemoteServerTrackedRacesCacheSynchronously(ref);
                        }
                    }));
        } else {
            logger.warning("Not re-scheduling update for remote reference "+ref+" because a previous job for it is still not done yet.");
        }
    }

    /**
     * Sets the {@link #retrieveRemoteRaceResult flag} to control whether or not this {@link RemoteSailingServerSet}
     * instance attempts to load tracked races lists from remote servers. It is expected to be set to <code>false</code>
     * for replicas, what will also clear the {@link #cachedTrackedRacesForRemoteSailingServers tracked races cache} of
     * this instance.
     * 
     * @param retrieveRemoteRaceResult
     *            <code>true</code> to enable tracked races retrieval from remote servers, <code>false</code> to disable
     */
    public void setRetrieveRemoteRaceResult(boolean retrieveRemoteRaceResult) {
        this.retrieveRemoteRaceResult.set(retrieveRemoteRaceResult);
        synchronized (cachedEventsForRemoteSailingServers) {
            if (!this.retrieveRemoteRaceResult.get()) {
                cachedTrackedRacesForRemoteSailingServers.clear();
            }
        }
    }

    /**
     * This method will load the list of all tracked races from the provided {@link RemoteSailingServerReference remote
     * server} instance (transitively), by calling REST API's <code>TrackedRaceListResource</code>
     * ("/v1/trackedRaces/getRaces").
     * 
     * @param ref
     *            the {@link RemoteSailingServerReference} to load tracked races from
     */
    private void updateRemoteServerTrackedRacesCacheSynchronously(RemoteSailingServerReference ref) {
        Util.Pair<Iterable<SimpleRaceInfo>, Exception> result;
        try {
            logger.fine("Updating racelist for remote server " + ref + " from URL " + ref.getURL());
            final SimpleRaceInfoJsonSerializer deserializer = new SimpleRaceInfoJsonSerializer();
            final Set<SimpleRaceInfo> races = new HashSet<>();
            for (Object remoteWithRaces : getJSONFromRemoteRacesListSynchronously(ref)) {
                JSONObject remoteWithRacesAsJson = (JSONObject) remoteWithRaces;
                String remoteUrlAsString = (String) remoteWithRacesAsJson
                        .get(DetailedRaceInfoJsonSerializer.FIELD_REMOTEURL);
                URL remoteUrl;
                if (remoteUrlAsString != null && !remoteUrlAsString.isEmpty()) {
                    remoteUrl = new URL(remoteUrlAsString);
                } else {
                    // if the race was local to the remote server, indicated by a null URL; use the remote server's URL
                    remoteUrl = ref.getURL();
                }
                JSONArray raceListForOneRemote = (JSONArray) remoteWithRacesAsJson
                        .get(DetailedRaceInfoJsonSerializer.FIELD_RACES);
                for (Object remoteRace : raceListForOneRemote) {
                    JSONObject remoteRaceAsJson = (JSONObject) remoteRace;
                    SimpleRaceInfo event = deserializer.deserialize(remoteRaceAsJson, remoteUrl);
                    races.add(event);
                }
            }
            result = new Util.Pair<Iterable<SimpleRaceInfo>, Exception>(races, /* exception */ null);
        } catch (Exception e) {
            logger.log(Level.INFO,
                    "Exception trying to fetch AnniversaryRaceData from remote server " + ref + ": " + e.getMessage(),
                    e);
            result = new Util.Pair<Iterable<SimpleRaceInfo>, Exception>(/* events */ null, e);
        }
        synchronized (cachedTrackedRacesForRemoteSailingServers) {
            if (retrieveRemoteRaceResult.get()) {
                updateCache(ref, result, cachedTrackedRacesForRemoteSailingServers::put);
            }
        }
        notifyRemoteRaceResultReceivedCallbacks();
    }

    /**
     * Calls all {@link #addRemoteRaceResultReceivedCallback(Runnable) registered} callbacks, to notify them about the
     * availability of newer, but not necessarily different tracked races results.
     */
    private void notifyRemoteRaceResultReceivedCallbacks() {
        new HashSet<>(remoteRaceResultReceivedCallbacks).forEach(Runnable::run);
    }

    /**
     * Build the URL for retrieving the remote servers race list. This is complemented by the end point here:
     * {@link com.sap.sailing.server.gateway.jaxrs.api.TrackedRaceListResource#raceList(Boolean, String, String)}
     * @param ref the remote reference
     * @return the URL for retrieving the remote races.
     * @throws MalformedURLException
     */
    private URL getRaceListURL(RemoteSailingServerReference ref) throws MalformedURLException {
        URL remoteServerBaseURL = ref.getURL();
        String endpoint = "/trackedRaces/getRaces?transitive=true";
        return getEndpointUrl(remoteServerBaseURL, endpoint);
    }

    private URL getEndpointUrl(URL remoteServerBaseURL, final String endpoint) throws MalformedURLException {
        return getURL(remoteServerBaseURL, "sailingserver/api/v1"+endpoint);
    }

    private Util.Pair<Iterable<EventBase>, Exception> updateRemoteServerEventCacheSynchronously(
            RemoteSailingServerReference ref) {
        Util.Pair<Iterable<EventBase>, Exception> result = loadEventsForRemoteServerReference(ref.getName(),
                ref.isInclude(), ref.getSelectedEventIds(), ref.getURL());
        final Pair<Iterable<EventBase>, Exception> finalResult = result;
        LockUtil.executeWithWriteLock(lock, () -> {
            // check that the server was not removed while no lock was held
            if (remoteSailingServers.containsValue(ref)) {
                cachedEventsForRemoteSailingServers.put(ref, finalResult);
            } else {
                logger.fine("Omitted update for " + ref + " as it was removed");
            }
        });
        return result;
    }
    
    private Util.Pair<Iterable<EventBase>, Exception> loadEventsForRemoteServerReference(final String serverName,
            final boolean include, final Set<UUID> selectedEvents, final URL url) {
        BufferedReader bufferedReader = null;
        Util.Pair<Iterable<EventBase>, Exception> result;
        try {
            try {
                final URL eventsURL = getEventsURL(include, selectedEvents, url);
                logger.fine("Updating events for remote server " + serverName + " from URL " + eventsURL);
                URLConnection urlConnection = HttpUrlConnectionHelper.redirectConnection(eventsURL);
                bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));
                JSONParser parser = new JSONParser();
                Object eventsAsObject = parser.parse(bufferedReader);
                EventBaseJsonDeserializer deserializer = new EventBaseJsonDeserializer(
                        new VenueJsonDeserializer(new CourseAreaJsonDeserializer(DomainFactory.INSTANCE)),
                        new LeaderboardGroupBaseJsonDeserializer(), new TrackingConnectorInfoJsonDeserializer());
                JSONArray eventsAsJsonArray = (JSONArray) eventsAsObject;
                final Set<EventBase> events = new HashSet<>();
                for (Object eventAsObject : eventsAsJsonArray) {
                    JSONObject eventAsJson = (JSONObject) eventAsObject;
                    EventBase event = deserializer.deserialize(eventAsJson);
                    events.add(event);
                }
                if (selectedEvents != null) {
                    Set<EventBase> filteredEvents = events.stream()
                            .filter(element -> include ? selectedEvents.contains(element.getId())
                                    : !selectedEvents.contains(element.getId()))
                            .collect(Collectors.toSet());
                    result = new Util.Pair<Iterable<EventBase>, Exception>(filteredEvents, /* exception */ null);
                } else {
                    result = new Util.Pair<Iterable<EventBase>, Exception>(events, /* exception */ null);
                }
            } finally {
                if (bufferedReader != null) {
                    bufferedReader.close();
                }
            }
        } catch (IOException | ParseException e) {
            logger.log(Level.INFO,
                    "Exception trying to fetch events from remote server " + serverName + ": " + e.getMessage(), e);
            result = new Util.Pair<Iterable<EventBase>, Exception>(/* events */ null, e);
        }
        return result;
    }

    private void updateRemoteServerStatisticsCacheSynchronously(RemoteSailingServerReference ref) {
        Util.Pair<Map<Integer, Statistics>, Exception> result;
        try {
            final URL statisticsByYearURL = getStatisticsByYearURL(ref.getURL());
            logger.fine("Updating by-year statistics for remote server " + ref + " from URL " + statisticsByYearURL);
            JSONArray statisticsByYear = getJsonFromRemoteServerSynchronously(ref, statisticsByYearURL);
            StatisticsByYearJsonDeserializer deserializer = new StatisticsByYearJsonDeserializer(statisticsJsonDeserializer);
            final Map<Integer, Statistics> statisticsByYearMap = new HashMap<>();
            for (Object entry : statisticsByYear) {
                JSONObject jsonEntry = (JSONObject) entry;
                Pair<Integer, Statistics> pair = deserializer.deserialize(jsonEntry);
                statisticsByYearMap.put(pair.getA(), pair.getB());
            }
            result = new Pair<Map<Integer, Statistics>, Exception>(statisticsByYearMap, /* exception */ null);
        } catch (IOException | ParseException e) {
            logger.log(Level.INFO,
                    "Exception trying to fetch by-year statistics from remote server " + ref + ": " + e.getMessage(),
                    e);
            result = new Util.Pair<Map<Integer, Statistics>, Exception>(/* statistics by year */ null, e);
        }
        updateCache(ref, result, cachedStatisticsByYearForRemoteSailingServers::put);
    }

    private JSONArray getJSONFromRemoteRacesListSynchronously(final RemoteSailingServerReference ref)
            throws MalformedURLException, IOException, ParseException {
        JSONArray data;
        final URL url = getRaceListURL(ref);
        final StringBuffer formParams = new StringBuffer("transitive=true");
        if (!ref.getSelectedEventIds().isEmpty()) {
            formParams.append("&events=");
            Iterator<UUID> iter = ref.getSelectedEventIds().iterator();
            while (iter.hasNext()) {
                formParams.append(URLEncoder.encode(iter.next().toString(), "utf-8"));
                if (iter.hasNext()) {
                    formParams.append(URLEncoder.encode(",", "utf-8"));
                }
            }
            formParams.append("&pred=" + (ref.isInclude() ? "incl" : "excl"));
        }
        HttpURLConnection urlConnection = (HttpURLConnection) HttpUrlConnectionHelper.redirectConnection(url,
                Duration.ONE_SECOND.times(1000), "POST", (connection) -> {
                    connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                    connection.setRequestProperty(HEADER_FORWARD_TO_REPLICA.getA(), HEADER_FORWARD_TO_REPLICA.getB());
                }, Optional.of(outputStream -> {
                    try (OutputStreamWriter writer = new OutputStreamWriter(outputStream, "utf-8")) {
                        writer.write(formParams.toString());
                    }
                }));
        final int responseCode = urlConnection.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), "utf-8"))) {
                JSONParser parser = new JSONParser();
                data = (JSONArray) parser.parse(bufferedReader);
            }
        } else {
            // fallback to old interface when new interface was not found.
            // can be removed when all servers are upgraded
            data = getJsonFromRemoteServerSynchronously(ref, url);
        }
        return data;
    }
    
    private JSONArray getJsonFromRemoteServerSynchronously(RemoteSailingServerReference ref,
            final URL url) throws IOException, ParseException {
        logger.fine("Updating data for remote server " + ref + " from URL " + url);
        URLConnection urlConnection = HttpUrlConnectionHelper.redirectConnection(url);
        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(urlConnection.getInputStream(), "UTF-8"))) {
            JSONParser parser = new JSONParser();
            return (JSONArray) parser.parse(bufferedReader);
        }
    }

    private <T> void updateCache(final RemoteSailingServerReference ref, final Util.Pair<T, Exception> result,
            final BiConsumer<RemoteSailingServerReference, Util.Pair<T, Exception>> updateCacheCallback) {
        LockUtil.executeWithWriteLock(lock, () -> {
            // check that the server was not removed while no lock was held
            if (remoteSailingServers.containsValue(ref)) {
                updateCacheCallback.accept(ref, result);
            } else {
                logger.fine("Omitted update for " + ref + " as it was removed");
            }
        });
    }

    private URL getEventsURL(final boolean include, final Set<UUID> selectedEvents, final URL url)
            throws MalformedURLException {
        final String basePath = "/events";
        final StringBuilder eventsEndpointName = new StringBuilder(basePath);
        eventsEndpointName.append("?include=").append(include);
        if (selectedEvents != null) {
            for (final UUID eventId : selectedEvents) {
                eventsEndpointName.append("&id=").append(eventId.toString());
            }
        }
        return getEndpointUrl(url, eventsEndpointName.toString());
    }
    
    private URL getStatisticsByYearURL(URL remoteServerBaseURL) throws MalformedURLException {
        return getEndpointUrl(remoteServerBaseURL, "/statistics/years");
    }

    private URL getURL(URL remoteServerBaseURL, String subURL) throws MalformedURLException {
        String baseURL = remoteServerBaseURL.toExternalForm();
        if (!baseURL.endsWith("/")) {
            baseURL += "/";
        }
        return new URL(baseURL + subURL);
    }

    public Map<RemoteSailingServerReference, Util.Pair<Iterable<EventBase>, Exception>> getCachedEventsForRemoteSailingServers() {
        LockUtil.lockForRead(lock);
        try {
            return Collections.unmodifiableMap(cachedEventsForRemoteSailingServers);
        } finally {
            LockUtil.unlockAfterRead(lock);
        }
    }

    public RemoteSailingServerReference remove(String name) {
        RemoteSailingServerReference ref = null;
        LockUtil.lockForWrite(lock);
        try{
            ref = remoteSailingServers.remove(name);
            if (ref != null) {
                cachedEventsForRemoteSailingServers.remove(ref);
                cachedStatisticsByYearForRemoteSailingServers.remove(ref);
                cachedTrackedRacesForRemoteSailingServers.remove(ref);
            }
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
        notifyRemoteRaceResultReceivedCallbacks();
        return ref;
    }

    /**
     * Synchronously fetches the latest events list for the remote server reference specified. The result is cached. If
     * <code>ref</code> was not yet part of this remote sailing server reference set, it is automatically added.
     * 
     * @param <code>forceUpdate</code>
     *            is used to trigger cache update in case the list of excluded events is changed
     */
    public Util.Pair<Iterable<EventBase>, Exception> getEventsOrException(RemoteSailingServerReference ref, boolean forceUpdate) {
        LockUtil.lockForWrite(lock);
        try {
            if (forceUpdate || !remoteSailingServers.containsKey(ref.getName())) {
                remoteSailingServers.put(ref.getName(), ref);
            }
        } finally {
            LockUtil.unlockAfterWrite(lock);
        }
        return updateRemoteServerEventCacheSynchronously(ref);
    }

    /**
     * Loads complete list of events for given remote reference server by sending {@link boolean} include parameter with
     * <code>false</code> value and an empty exclude list. Can be used, e.g., by a UI letting the user select which events
     * to pick for inclusion/exclusion.
     */
    public Util.Pair<Iterable<EventBase>, Exception> getEventsComplete(RemoteSailingServerReference ref) {
        return loadEventsForRemoteServerReference(ref.getName(), false, /* eventIds */ null, ref.getURL());
    }

    public Iterable<RemoteSailingServerReference> getLiveRemoteServerReferences() {
        List<RemoteSailingServerReference> result = new ArrayList<>();
        LockUtil.lockForRead(lock);
        try {
            for (Map.Entry<RemoteSailingServerReference, Pair<Iterable<EventBase>, Exception>> e : cachedEventsForRemoteSailingServers.entrySet()) {
                if (e.getValue().getB() == null) {
                    // no exception; reference considered live
                    result.add(e.getKey());
                }
            }
        } finally {
            LockUtil.unlockAfterRead(lock);
        }
        return result;
    }
    
    public Map<RemoteSailingServerReference, Util.Pair<Map<Integer, Statistics>, Exception>> getCachedStatisticsForRemoteSailingServers() {
        LockUtil.lockForRead(lock);
        try {
            return Collections.unmodifiableMap(cachedStatisticsByYearForRemoteSailingServers);
        } finally {
            LockUtil.unlockAfterRead(lock);
        }
    }

    /**
     * Provided the currently cached tracked races lists group by the {@link RemoteSailingServerReference remote server}
     * instances where they were originally retrieved from. This cache is determined by periodical updates.
     */
    public Map<RemoteSailingServerReference, Pair<Iterable<SimpleRaceInfo>, Exception>> getCachedRaceList() {
        LockUtil.lockForRead(lock);
        try {
            return Collections.unmodifiableMap(cachedTrackedRacesForRemoteSailingServers);
        } finally {
            LockUtil.unlockAfterRead(lock);
        }
    }

    /**
     * Retrieves {@link DetailedRaceInfo detailed information} for the provided {@link SimpleRaceInfo race} from the
     * {@link SimpleRaceInfo#getRemoteUrl() corresponding remote server URL} (blocking operation).
     * 
     * @param matching
     *            the {@link SimpleRaceInfo} to get the detailed information for
     * @return the retrieved {@link DetailedRaceInfo detailed information}
     */
    public DetailedRaceInfo getDetailedInfoBlocking(SimpleRaceInfo matching) {
        try {
            URL remoteRequestUrl = getDetailRaceInfoURL(matching.getRemoteUrl(), matching);
            logger.fine("Loading DetailedRace data for remote server from URL " + remoteRequestUrl);
            URLConnection urlConnection = HttpUrlConnectionHelper.redirectConnection(remoteRequestUrl);
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(urlConnection.getInputStream(), "UTF-8"))) {
                JSONParser parser = new JSONParser();
                JSONObject detailedInfoAsJson =  (JSONObject) parser.parse(bufferedReader);
                DetailedRaceInfoJsonSerializer detailedRaceListJsonSerializer = new DetailedRaceInfoJsonSerializer();
                DetailedRaceInfo detailedInfoAsJava = detailedRaceListJsonSerializer.deserialize(detailedInfoAsJson);
                if (detailedInfoAsJava.getRemoteUrl() == null) {
                    // not transitive, use direct url
                    detailedInfoAsJava = new DetailedRaceInfo(detailedInfoAsJava, matching.getRemoteUrl());
                }
                return detailedInfoAsJava;
            }
        } catch (ParseException | IOException e) {
            throw new IllegalStateException("RemoteUrl could not be called ", e);
        }
    }

    private URL getDetailRaceInfoURL(URL remoteServerBaseURL, SimpleRaceInfo matching)
            throws MalformedURLException, UnsupportedEncodingException {
        String raceName = URLEncoder.encode(matching.getIdentifier().getRaceName(), "UTF-8").replace("+", "%20");
        String regattaName = URLEncoder.encode(matching.getIdentifier().getRegattaName(), "UTF-8").replace("+", "%20");
        String params = "raceName=" + raceName + "&regattaName=" + regattaName;
        return getEndpointUrl(remoteServerBaseURL, "/trackedRaces/raceDetails?" + params);
    }

    /**
     * Registers a {@link Runnable callback} which is {@link Runnable#run() called}, after a retrieval of tracked races
     * from a {@link RemoteSailingServerReference remote server} instance.
     * 
     * @param callback
     *            {@link Runnable} to register
     */
    public void addRemoteRaceResultReceivedCallback(Runnable callback) {
        this.remoteRaceResultReceivedCallbacks.add(callback);
    }
    
    /**
     * Unregisters the given {@link Runnable callback} if it was {@link #add(RemoteSailingServerReference) registered}
     * before, otherwise this is operation has no effect.
     * 
     * @param callback
     *            {@link Runnable} to unregister
     */
    public void removeRemoteRaceResultReceivedCallback(Runnable callback){
        this.remoteRaceResultReceivedCallbacks.remove(callback);
    }
}
