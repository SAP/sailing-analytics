package com.sap.sailing.domain.tractracadapter.impl;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.leaderboard.LeaderboardGroupResolver;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.tracking.AbstractRaceTrackerImpl;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.AbstractRaceChangeListener;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.TracTracConnectionConstants;
import com.sap.sailing.domain.tractracadapter.TracTracRaceTracker;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.util.impl.ThreadFactoryWithPriority;
import com.tractrac.model.lib.api.event.CreateModelException;
import com.tractrac.model.lib.api.event.DataSource;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.event.IRaceCompetitor;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.IRaceSubscriber;
import com.tractrac.subscription.lib.api.SubscriberInitializationException;
import com.tractrac.subscription.lib.api.SubscriptionLocator;
import com.tractrac.subscription.lib.api.event.IConnectionStatusListener;
import com.tractrac.subscription.lib.api.event.ILiveDataEvent;
import com.tractrac.subscription.lib.api.event.IStoredDataEvent;
import com.tractrac.subscription.lib.api.race.IRaceCompetitorListener;
import com.tractrac.subscription.lib.api.race.IRacesListener;
import com.tractrac.util.lib.api.exceptions.TimeOutException;

public class TracTracRaceTrackerImpl extends AbstractRaceTrackerImpl<RaceTrackingConnectivityParametersImpl>
        implements IConnectionStatusListener, TracTracRaceTracker, DynamicRaceDefinitionSet, TrackingDataLoader {
    private static final Logger logger = Logger.getLogger(TracTracRaceTrackerImpl.class.getName());
    
    /**
     * A scheduler for the periodic checks of the paramURL documents for the advent of {@link IMapItem control points}
     * with static position information otherwise not available through {@link MarkPassingReceiver}'s events.
     */
    static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1, new ThreadFactoryWithPriority(Thread.NORM_PRIORITY, /* daemon */ true));

    /**
     * This value indicated how many stored data packets we allow that are not in the right sequence Background: It can
     * happen that the progress for storedData hops around and delivers a progress that is lower than one received
     * before. This can happen but only at a maximum of times this constant describes. To provide some background, here
     * is an excerpt of a description received from Jorge Piera Llodra from TracTrac on 2014-02-05:
     * 
     * <i>
     * <p>
     * "Our library creates a new thread per data type where a data type is associated with a subscription. e.g: there
     * is a data type for the competitor positions, other for the mark positions, other for the course update, other for
     * the start/stop times... Every thread calculates its individual progress and its weight and the progress that you
     * receive in the "storedDataProgress" method is a function of all the individual progresses and all the weights
     * reported by the threads. The function that calculates the total progress is:
     * 
     * <pre>
     * total_progress = sum(progress(thread_i)) / sum(weight(thread_i))
     * </pre>
     * 
     * The weight is also the maximum individual progress that a thread can send: if a thread has a weight = 10 its
     * progress only can be between 0 and 10, e.g,:
     * <p>
     * 
     * You are subscribed to receive competitor positions and the current course. All the threads have a default weight
     * and the beginning the send the values:
     * <ul>
     * <li>Competitor positions thread -> weight = 10, progress = 0</li>
     * <li>Course thread -> weight = 1, progress = 0</li>
     * </ul>
     * The total progress that you receive is:
     * 
     * <pre>
     *   total_progress = 0 + 0 / 10 + 1 = 0 / 11 = 0
     * </pre>
     * 
     * Then, the "Course thread" retrieves the course from the server and it sends a new progress message to the system:
     * 
     * <pre>
     *   Course thread -&gt; weight = 1, progress = 1  ---&gt total_progress = 0 + 1 /  10 + 1 = 1 / 11 = 0.090909091
     * </pre>
     * 
     * Then, the "Competitor positions thread" goes to the server and it checks that there is a high number of positions
     * for the competitors. It decides to change its weight:
     * 
     * <pre>
     *   Competitor positions thread -&gt weight = 50, progress = 0 --&gt total_progress = 0 + 1 / 50 + 1 = 1 / 51 = 0.019607843
     * </pre>
     * 
     * Then, the "Competitor positions thread" starts to retrieve positions and it sends several messages updating the
     * progress:
     * <ul>
     * <li>
     * 
     * <pre>
     * Competitor positions thread -&gt weight = 50, progress = 1 --&gt; total_progress = 1 + 1 / 50 + 1 = 2 / 51 = 0.039215686
     * </pre>
     * 
     * </li>
     * <li>
     * 
     * <pre>
     * Competitor positions thread -&gt weight = 50, progress = 2 --&gt total_progress = 2 + 1 / 50 + 1 = 3 / 51 = 0.058823529
     * </pre>
     * 
     * </li>
     * <li>
     * 
     * <pre>
     * Competitor positions thread -&gt weight = 50, progress = 3 --&gt total_progress = 3 + 1 / 50 + 1 = 4 / 51 = 0.078431373
     * </pre>
     * 
     * </li>
     * <li>
     * 
     * <pre>
     * ...
     * </pre>
     * 
     * </li>
     * <li>
     * 
     * <pre>
     * Competitor positions thread -&gt weight = 50, progress = 50 --&gt total_progress = 50 + 1 / 50 + 1 = 51 / 51 = 1.0
     * </pre>
     * 
     * </li>
     * </ul>
     * 
     * This example shows that is possible to receive more that 3 values of the progress lower than one already
     * received. It happens because the weight of the threads changes."</i>
     * <p>
     * 
     * We assume that there won't be more than eight threads in TTCM receiving data for the same race, based on Jorge's
     * statement from 2014-02-06: "One thread per subscription where the subscriptions are:
     * <ul>
     * <li>Competitor positions</li>
     * <li>Mark positions</li>
     * <li>Mark passings</li>
     * <li>Route</li>
     * <li>Start/Stop times for race</li>
     * <li>Start/Stop times for event</li>
     * <li>Messages for race</li>
     * <li>Messages for event</li>
     * </ul>
     * Potentially, you can create 8 threads per TTCM (connecting only with one single race)."
     */
    static final Integer MAX_STORED_PACKET_HOP_ALLOWANCE = 1000;

    private static final long TIMEOUT_FOR_RACE_TO_APPEAR_FOR_STOPPING_TRACKING_IN_MILLIS = Duration.ONE_MINUTE.times(5).asMillis();
    
    private final IEvent tractracEvent;
    private final IRace tractracRace;
    private final com.sap.sailing.domain.base.Regatta regatta;
    private final IEventSubscriber eventSubscriber;
    private final IRaceSubscriber raceSubscriber;
    private final IRacesListener racesListener;
    private final IRaceCompetitorListener competitorsListener;
    private final Set<Receiver> receivers;
    private final DomainFactory domainFactory;
    private final WindStore windStore;
    /**
     * Needs to be {@code volatile} because one thread (e.g., the {@link RaceCourseReceiver}) writes it, another thread
     * such as the callback set up by the {@link #stopped(Object)} method and executed by the last {@link Receiver}
     * emptying its queue, leading to a call to
     * {@link AbstractLoadingQueueDoneCallBack#executeWhenAllReceiversAreDoneLoading()} will read it. We need
     * to ensure that the reading threads always read what other writers wrote.
     */
    private volatile RaceDefinition race;
    private final DynamicTrackedRegatta trackedRegatta;
    private TrackedRaceStatus lastStatus;
    private Map<Object, Util.Pair<Integer, Float>> lastProgressPerID;

    /**
     * paramURL, liveURI and storedURI for TracTrac connection, voided of "random" part
     */
    private final Object id;

    /**
     * Tells if this tracker was created with a valid live URI. If not, the tracker will stop and unregister itself
     * from the {@link RacingEventService} after having received all stored data.
     */
    private final boolean isLiveTracking;

    /**
     * Tells whether the {@link #stop(boolean)} method has been called. This prevents further calls to that method
     * from having any effects.
     */
    private boolean stopped;

    private final TrackedRegattaRegistry trackedRegattaRegistry;

    private final Simulator simulator;

    /**
     * Registered as a listener on the TracTrac race and competitor statuses, as well as on the {@link RaceLog}s of the
     * {@link TrackedRace} tracked by this tracker once it shows up in
     * {@link #addRaceDefinition(RaceDefinition, DynamicTrackedRace)}. Responsible to keep the race log entries in sync
     * with what TracTrac announces for results.
     */
    private final RaceAndCompetitorStatusWithRaceLogReconciler reconciler;

    /**
     * Creates a race tracked for the specified URL/URIs and starts receiving all available existing and future push
     * data from there. Receiving continues until {@link #stop(boolean)} is called.
     * <p>
     * 
     * A race tracker uses the <code>paramURL</code> for the TracTrac Java client to register for push data about one
     * race. The {@link DomainFactory} is asked to retrieve an existing or create a new
     * {@link com.sap.sailing.domain.base.Regatta} based on the TracTrac event. The {@link RaceDefinition} for the race,
     * however, isn't created until the {@link Course} has been received. Therefore, the {@link RaceCourseReceiver} will
     * create the {@link RaceDefinition} and will add it to the {@link com.sap.sailing.domain.base.Regatta}.
     * <p>
     * 
     * The link to the {@link RaceDefinition} is created in the {@link DomainFactory} when the
     * {@link RaceCourseReceiver} creates the {@link TrackedRace} object. Starting then, the {@link DomainFactory} will
     * respond with the {@link RaceDefinition} when its {@link DomainFactory#getRaceID(com.tractrac.model.lib.api.event.IRace)} is called with the
     * TracTrac {@link IEvent} as argument that is used for its tracking.
     * <p>
     * @param windStore
     *            Provides the capability to obtain the {@link WindTrack}s for the different wind sources. A trivial
     *            implementation is {@link EmptyWindStore} which simply provides new, empty tracks. This is always
     *            available but loses track of the wind, e.g., during server restarts.
     * @param trackedRegattaRegistry
     *            used to create the {@link TrackedRegatta} for the domain event
     * @param timeoutInMilliseconds
     *            use -1 to wait for the race and all its data forever; otherwise specify the milliseconds after which
     *            you expect things to have loaded; see also
     *            {@link RaceTracker#TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS}.
     */
    TracTracRaceTrackerImpl(DomainFactory domainFactory, RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            WindStore windStore, TrackedRegattaRegistry trackedRegattaRegistry,
            RaceLogAndTrackedRaceResolver raceLogResolver, LeaderboardGroupResolver leaderboardGroupResolver,
            RaceTrackingConnectivityParametersImpl connectivityParams, long timeoutInMilliseconds,
            RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry) throws URISyntaxException, SubscriberInitializationException,
            IOException, InterruptedException, CreateModelException, TimeOutException {
        this(/* regatta */ null, domainFactory, raceLogStore, regattaLogStore, windStore, trackedRegattaRegistry,
                raceLogResolver, leaderboardGroupResolver, connectivityParams, timeoutInMilliseconds,
                raceTrackingHandler, markPassingRaceFingerprintRegistry);
    }
    
    /**
     * Use this constructor if the {@link Regatta} in which to arrange the {@link RaceDefinition}s created by this
     * tracker is already known up-front, particularly if it has a specific configuration to use. Other constructors may
     * create a default {@link Regatta} with only a single default {@link Series} and {@link Fleet} which may not always
     * be what you want.
     * 
     * @param regatta
     *            if <code>null</code>, then <code>domainFactory.getOrCreateRegatta(tractracEvent)</code> will be used
     *            to obtain a default regatta
     * @param timeoutInMilliseconds
     *            use -1 to wait for the race and all its data forever; otherwise specify the milliseconds after which
     *            you expect things to have loaded; see also
     *            {@link RaceTracker#TIMEOUT_FOR_RECEIVING_RACE_DEFINITION_IN_MILLISECONDS}.
     */
    TracTracRaceTrackerImpl(final Regatta regatta, DomainFactory domainFactory, RaceLogStore raceLogStore,
            RegattaLogStore regattaLogStore, WindStore windStore, TrackedRegattaRegistry trackedRegattaRegistry,
            RaceLogAndTrackedRaceResolver raceLogResolver, LeaderboardGroupResolver leaderboardGroupResolver,
            RaceTrackingConnectivityParametersImpl connectivityParams, long timeoutInMilliseconds,
            RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry)
            throws URISyntaxException, SubscriberInitializationException, IOException, InterruptedException, CreateModelException, TimeOutException {
        super(connectivityParams);
        final URL paramURL = connectivityParams.getParamURL();
        final URI liveURI = connectivityParams.getLiveURI();
        final URI storedURI = connectivityParams.getStoredURI();
        final URI tracTracUpdateURI = connectivityParams.getUpdateURI();
        final TimePoint startOfTracking = connectivityParams.getStartOfTracking();
        final TimePoint endOfTracking = connectivityParams.getEndOfTracking();
        final long delayToLiveInMillis = connectivityParams.getDelayToLiveInMillis();
        final Duration offsetToStartTimeOfSimulatedRace = connectivityParams.getOffsetToStartTimeOfSimulatedRace();
        final boolean useInternalMarkPassingAlgorithm = connectivityParams.isUseInternalMarkPassingAlgorithm();
        final String tracTracUsername = connectivityParams.getTracTracUsername();
        final String tracTracPassword = connectivityParams.getTracTracPassword();
        final String raceStatus = connectivityParams.getRaceStatus();
        final String raceVisibility = connectivityParams.getRaceVisibility();
        final boolean useOfficialEventsToUpdateRaceLog = connectivityParams.isUseOfficialEventsToUpdateRaceLog();
        this.trackedRegattaRegistry = trackedRegattaRegistry;
        this.tractracRace = connectivityParams.getTractracRace();
        this.tractracEvent = tractracRace.getEvent();
        this.id = createID(paramURL, liveURI, storedURI);
        isLiveTracking = liveURI != null;
        this.race = null; // no race received yet
        this.domainFactory = domainFactory;
        this.lastProgressPerID = new HashMap<>();
        if (offsetToStartTimeOfSimulatedRace != null) {
            simulator = new Simulator(windStore, offsetToStartTimeOfSimulatedRace);
            // don't write the transformed wind fixes into the DB again... see also bug 1974 
            this.windStore = EmptyWindStore.INSTANCE;
        } else {
            simulator = null;
            this.windStore = windStore;
        }
        // check if there is a directory configured where stored data files can be cached
        // only cache files for races in REPLAY state
        final URI effectiveStoredURI; // may be altered by legacy caching mechanism; won't affect this tracker's ID
        if ( (raceStatus != null && raceStatus.equals(TracTracConnectionConstants.REPLAY_STATUS)) || 
                (raceVisibility != null && raceVisibility.equals(TracTracConnectionConstants.REPLAY_VISIBILITY)) ) {
            effectiveStoredURI = checkForCachedStoredData(storedURI);
        } else {
            effectiveStoredURI = storedURI;
        }
        logger.info("Starting race tracker: " + tractracRace.getName() + " " + paramURL + " " + liveURI + " "
                + effectiveStoredURI + " startOfTracking:"
                + (startOfTracking != null ? startOfTracking.asMillis() : "n/a") + " endOfTracking:"
                + (endOfTracking != null ? endOfTracking.asMillis() : "n/a"));

        // Initialize data controller using live and stored data sources
        eventSubscriber = domainFactory.getOrCreateEventSubscriber(tractracEvent, liveURI, effectiveStoredURI);
        if (useOfficialEventsToUpdateRaceLog) {
            reconciler = new RaceAndCompetitorStatusWithRaceLogReconciler(domainFactory, raceLogResolver, tractracRace);
        } else {
            reconciler = null;
        }
        racesListener = new IRacesListener() {
            @Override public void abandonRace(long timestamp, UUID raceId) {}
            @Override public void addRace(long timestamp, IRace race) {}
            @Override public void deleteRace(long timestamp, UUID raceId) {}
            @Override public void reloadRace(long timestamp, UUID raceId) {
                if (raceId.equals(tractracRace.getId())) {
                    logger.warning("reloadRace("+raceId+") for race "+tractracRace+
                            " in event "+tractracEvent+" not supported yet. Consider re-loading the race manually");
                }
            }
            @Override public void startTracking(long timestamp, UUID raceId) {}
            @Override public void dataSourceChanged(long timestamp, IRace race, DataSource oldDataSource, URI oldLiveURI, URI oldStoredURI) {}
            @Override
            public void updateRace(long timestamp, IRace race) {
                if (Util.equalsWithNull(race.getId(), TracTracRaceTrackerImpl.this.tractracRace.getId())) {
                    int delayToLiveInMillis = race.getLiveDelay()*1000;
                    if (getRace() != null) {
                        DynamicTrackedRace trackedRace = getTrackedRegatta().getExistingTrackedRace(getRace());
                        if (trackedRace != null) {
                            if (reconciler != null) {
                                logger.info("Handling a race status update for race "+race.getName()+" with status "+race.getStatus()+
                                        " and status time "+race.getStatusLastChangedTime());
                                // in case a race status change was the reason for this update, reconcile with the race log(s)
                                reconciler.reconcileRaceStatus(race, trackedRace);
                            }
                            logger.info("Setting delay to live for race "+trackedRace.getRace().getName()+" to "+delayToLiveInMillis+"ms");
                            trackedRace.setDelayToLiveInMillis(delayToLiveInMillis);
                        }
                    }
                }
            }
        };
        eventSubscriber.subscribeRaces(racesListener);
        // Start live and stored data streams
        final Regatta effectiveRegatta;
        raceSubscriber = SubscriptionLocator.getSusbcriberFactory().createRaceSubscriber(tractracRace, liveURI, effectiveStoredURI);
        raceSubscriber.subscribeConnectionStatus(this);
        // Try to find a pre-associated event based on the Race ID
        if (regatta == null) {
            Serializable raceID = domainFactory.getRaceID(tractracRace);
            effectiveRegatta = trackedRegattaRegistry.getRememberedRegattaForRace(raceID);
        } else {
            effectiveRegatta = regatta;
        }
        // if regatta is still null, no previous assignment of any of the races in this TracTrac event to a Regatta was
        // found;
        // in this case, create a default regatta based on the TracTrac event data
        final Regatta regattaInWhichToTryToRemoveExistingRace = effectiveRegatta == null ? domainFactory.getOrCreateDefaultRegatta(
                raceLogStore, regattaLogStore, tractracRace, trackedRegattaRegistry) : effectiveRegatta;
        // removeRace may detach the domain regatta from the domain factory if that
        // removed the last race; therefore, it's important to getOrCreate the
        // domain regatta *after* calling removeRace
        final RaceDefinition raceDefinition = domainFactory.removeRace(tractracRace.getEvent(), tractracRace, regattaInWhichToTryToRemoveExistingRace, trackedRegattaRegistry);
        if (raceDefinition != null) {
            trackedRegattaRegistry.removeRace(regattaInWhichToTryToRemoveExistingRace, raceDefinition);
        }
        // Look up / create the Regatta and TrackedRegatta after they may potentially have been removed by the
        // removeRace statements above:
        this.regatta = effectiveRegatta == null ? domainFactory.getOrCreateDefaultRegatta(
                raceLogStore, regattaLogStore, tractracRace, trackedRegattaRegistry) : effectiveRegatta;
        trackedRegatta = trackedRegattaRegistry.getOrCreateTrackedRegatta(this.regatta);
        receivers = new HashSet<Receiver>();
        for (Receiver receiver : domainFactory.getUpdateReceivers(getTrackedRegatta(), delayToLiveInMillis,
                simulator, windStore, this, trackedRegattaRegistry, raceLogResolver, markPassingRaceFingerprintRegistry, leaderboardGroupResolver,
                tractracRace, tracTracUpdateURI, tracTracUsername, tracTracPassword, eventSubscriber,
                raceSubscriber, useInternalMarkPassingAlgorithm, timeoutInMilliseconds, raceTrackingHandler, reconciler)) {
            receivers.add(receiver);
        }
        competitorsListener = new IRaceCompetitorListener() {
            @Override
            public void addRaceCompetitor(long timestamp, IRaceCompetitor raceCompetitor) {
                try {
                    trackedRegattaRegistry.updateRaceCompetitors(getRegatta(), getRace());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void updateRaceCompetitor(long timestamp, IRaceCompetitor raceCompetitor) {
                if (!raceCompetitor.getCompetitor().isNonCompeting()) {
                    TracTracRaceTrackerImpl.this.domainFactory.updateCompetitor(raceCompetitor.getCompetitor(), raceTrackingHandler);
                }
            }

            @Override
            public void deleteRaceCompetitor(long timestamp, UUID competitorId) {
                try {
                    trackedRegattaRegistry.updateRaceCompetitors(getRegatta(), getRace());
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void removeOffsetPositions(long timestamp, UUID competitorId, int offset) {
            }
        };
        raceSubscriber.subscribeRaceCompetitor(competitorsListener);
        addListenersForStoredDataAndStartController(receivers);
    }

    private URI checkForCachedStoredData(URI storedURI) {
        final String CACHE_DIR_PROPERTY = "tractrac.mtb.cache.dir";
        if (System.getProperty(CACHE_DIR_PROPERTY) != null) {
            final String directory = System.getProperty(CACHE_DIR_PROPERTY);
            if (new File(directory).exists()) {
                final String[] pathFragments = storedURI.getPath().split("\\/");
                final String mtbFileName = pathFragments[pathFragments.length-1];
                final String directoryAndFileName = directory+"/"+mtbFileName;
                final File f = new File(directoryAndFileName);
                if (!f.exists()) {
                    FileOutputStream mtbOutStream = null;
                    try {
                        logger.info("Starting to download " + storedURI + " to cache dir " + directoryAndFileName);
                        InputStream in = storedURI.toURL().openStream();
                        mtbOutStream = new FileOutputStream(f);
                        byte data[] = new byte[1024];
                        int count;
                        while ((count = in.read(data, 0, 1024)) != -1)
                        {
                            mtbOutStream.write(data, 0, count);
                        }
                        logger.info("Finished downloading file to cache!");
                    } catch (Exception ex) {
                        // never throw but display
                        ex.printStackTrace();
                    } finally {
                        if (mtbOutStream != null) {
                            try {
                                mtbOutStream.close();
                            } catch (IOException e) {
                                // ignore
                            }   
                        }
                    }
                } else {
                    logger.info("Found file " + directoryAndFileName + "! Reusing it for this race!");
                }
                
                try {
                    // notice us using three slashes here - this is because of a bug in the TracAPI
                    return new URI("file:///" + directoryAndFileName);
                } catch (URISyntaxException e) {
                    e.printStackTrace();
                }
            }
        }
        return storedURI;
    }

    @Override
    public DynamicTrackedRegatta getTrackedRegatta() {
        return trackedRegatta;
    }

    public static Object createID(URL paramURL, final URI liveURI, final URI storedURI) {
        // see also bug5380: no longer use liveURI and storedURI as part of the ID; the paramURL
        // is sufficient for identifying the race, and differences in live/stored URI can be caused
        // by TracTrac providing the same race through several load-balanced server instances that
        // serve through different ports.
        return getParamURLStrippedOfRandomParam(paramURL);
    }

    public static URL getParamURLStrippedOfRandomParam(URL paramURL) {
        URL paramURLStrippedOfRandomParam;
        if (paramURL == null) {
            paramURLStrippedOfRandomParam = null;
        } else {
            final String query = paramURL.getQuery();
            if (query == null) {
                paramURLStrippedOfRandomParam = paramURL;
            } else {
                final StringJoiner stringJoiner = new StringJoiner("&", "?", "");
                stringJoiner.setEmptyValue("");
                String[] queryParams = query.split("&");
                for (String queryParam : queryParams) {
                    String[] nameValue = queryParam.split("=");
                    if (!"random".equalsIgnoreCase(nameValue[0])) {
                        final StringBuilder param = new StringBuilder();
                        param.append(nameValue[0]);
                        if (nameValue.length > 1) {
                            param.append('=');
                            param.append(nameValue[1]);
                        }
                        stringJoiner.add(param.toString());
                    }
                }
                try {
                    paramURLStrippedOfRandomParam = new URL(paramURL.getProtocol(), paramURL.getHost(), paramURL.getPort(),
                            paramURL.getPath()+stringJoiner.toString()+(paramURL.getRef() == null || paramURL.getRef().isEmpty() ?
                                    "" : ("#"+paramURL.getRef())));
                } catch (MalformedURLException e) {
                    // this is pretty strange as we only removed one parameter; log and continue with original URL as a default
                    logger.log(Level.SEVERE, "Error trying to strip the \"random\" parameter from the TracTrac params_url "+
                            paramURL, e);
                    paramURLStrippedOfRandomParam = paramURL;
                }
            }
        }
        return paramURLStrippedOfRandomParam;
    }
    
    @Override
    public Object getID() {
        return id;
    }

    @Override
    public WindStore getWindStore() {
        return windStore;
    }

    @Override
    public RaceHandle getRaceHandle() {
        return new RaceHandleImpl(domainFactory, tractracRace, getTrackedRegatta(), this);
    }
    
    @Override
    public RaceDefinition getRace() {
        return race;
    }
    
    protected void addListenersForStoredDataAndStartController(Iterable<Receiver> listenersForStoredData) {
        for (Receiver receiver : listenersForStoredData) {
            receiver.subscribe();
        }
        eventSubscriber.start();
        raceSubscriber.start();
    }
    
    @Override
    public com.sap.sailing.domain.base.Regatta getRegatta() {
        return regatta;
    }
    
    @Override
    protected void onStop(boolean stopReceiversPreemtively, boolean willBeRemoved) throws InterruptedException {
        if (!stopped) {
            stopped = true;
            eventSubscriber.unsubscribeRaces(racesListener);
            raceSubscriber.unsubscribeRaceCompetitor(competitorsListener);
            raceSubscriber.stop();
            eventSubscriber.stop();
            raceSubscriber.unsubscribeConnectionStatus(this);
            for (Receiver receiver : receivers) {
                if (stopReceiversPreemtively) {
                    receiver.stopPreemptively();
                } else {
                    receiver.stopAfterProcessingQueuedEvents();
                }
            }
            if (!stopReceiversPreemtively) {
                // wait for their queues to be worked down before signaling the FINISHED state.
                new AbstractLoadingQueueDoneCallBack(receivers) {
                    @Override
                    protected void executeWhenAllReceiversAreDoneLoading() {
                        lastStatus = new TrackedRaceStatusImpl(willBeRemoved ? TrackedRaceStatusEnum.REMOVED : TrackedRaceStatusEnum.FINISHED, /* will be ignored */1.0);
                        updateStatusOfTrackedRaces();
                    }
                };
            } else {
                // queues contents were cleared preemptively; this means we're done with loading immediately
                lastStatus = new TrackedRaceStatusImpl(willBeRemoved ? TrackedRaceStatusEnum.REMOVED : TrackedRaceStatusEnum.FINISHED, /* will be ignored */1.0);
                updateStatusOfTrackedRaces();
            }
            if (stopReceiversPreemtively && simulator != null) {
                simulator.stop();
            }
        }
    }

    /**
     * Propagates {@link #lastStatus} to all tracked races to which this tracker writes.
     * 
     * @see #updateStatusOfTrackedRace(DynamicTrackedRace)
     */
    private void updateStatusOfTrackedRaces() {
        if (getRace() != null) {
            DynamicTrackedRace trackedRace = getTrackedRegatta().getExistingTrackedRace(getRace());
            if (trackedRace != null) {
                updateStatusOfTrackedRace(trackedRace);
            }
        }
    }

    /**
     * Propagates {@link #lastStatus} to <code>trackedRace</code>'s {@link TrackedRace#getStatus() status}. If
     * {@link #lastStatus} is a {@link TrackedRaceStatusEnum#FINISHED FINISHED} status, the progress value is taken from
     * the tracked race's current status instead of overwriting it with the progress indicated by
     * {@link #lastStatus}.
     */
    private void updateStatusOfTrackedRace(DynamicTrackedRace trackedRace) {
        // can't update a race status once it has been set to FINISHED
        if (lastStatus != null && trackedRace.getStatus() != null && trackedRace.getStatus().getStatus() != TrackedRaceStatusEnum.FINISHED) {
            final TrackedRaceStatus status;
            if (lastStatus.getStatus() == TrackedRaceStatusEnum.FINISHED) {
                // in this case use the tracked race's progress value:
                status = new TrackedRaceStatusImpl(lastStatus.getStatus(), trackedRace.getStatus() == null ? 0.0
                        : trackedRace.getStatus().getLoadingProgress());
            } else {
                status = lastStatus;
            }
            trackedRace.onStatusChanged(this, status);
        }
    }

    private void storedDataProgress(float progress) {
        if (lastStatus.getStatus().equals(TrackedRaceStatusEnum.ERROR)) {
            return;
        }
        Integer counter = 0;
        final Util.Pair<Integer, Float> lastProgressPair = lastProgressPerID.get(getID());
        if (lastProgressPair != null) {
            Float lastProgress = lastProgressPair.getB();
            counter = lastProgressPair.getA();
            if (progress < lastProgress.floatValue()) {
                if (counter.intValue() > MAX_STORED_PACKET_HOP_ALLOWANCE) {
                    try {
                        logger.severe("Got " + MAX_STORED_PACKET_HOP_ALLOWANCE + " times a value for progress " + progress + " that is lower than one already received " + lastProgress + "! This is a severe error - stopping receivers for " + getID() + " now!");
                        stop(/* stopReceiversPreemptively */ true);
                        /* make sure to indicate that this race is erroneous */
                        lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.ERROR, 0.0);
                        updateStatusOfTrackedRaces();
                        return;
                    } catch (Exception e) {
                        logger.severe("Exception stopping race tracker: " + e.getMessage());
                        e.printStackTrace();
                    }
                } else {
                    counter += 1;
                }
            }
        }
        logger.info("Stored data progress in tracker "+getID()+" for race(s) "+getRace()+": "+progress);
        lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, progress);
        if (progress==1.0) {
            new AbstractLoadingQueueDoneCallBack(receivers) {
                @Override
                protected void executeWhenAllReceiversAreDoneLoading() {
                    lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, progress);
                    updateStatusOfTrackedRaces();
                }
            };
        }
        lastProgressPerID.put(getID(), new Util.Pair<Integer, Float>(counter, progress));
        updateStatusOfTrackedRaces();
    }

    @Override
    public void addRaceDefinition(final RaceDefinition race, final DynamicTrackedRace trackedRace) {
        logger.info("Setting race for tracker "+this+" with ID "+getID()+" to "+race);
        synchronized (this) { // use synchronized to ensure the effects become visible to other threads reading later
            this.race = race;
        }
        updateStatusOfTrackedRace(trackedRace);
        registerRaceAndCompetitorStatusWithRaceLogReconciler(trackedRace);
        notifyRaceCreationListeners();
    }

    /**
     * Creates a listener on the {@code trackedRace} that listens for race logs being attached and detached and maps
     * this to respective calls to
     * {@link RaceAndCompetitorStatusWithRaceLogReconciler#raceLogAttached(TrackedRace, com.sap.sailing.domain.abstractlog.race.RaceLog)}
     * and
     * {@link RaceAndCompetitorStatusWithRaceLogReconciler#raceLogDetached(TrackedRace, com.sap.sailing.domain.abstractlog.race.RaceLog)}.
     * Furthermore, the {@link #reconciler} is {@link RaceAndCompetitorStatusWithRaceLogReconciler#raceLogAttached(TrackedRace, RaceLog) informed}
     * about all {@link RaceLog}s already currently attached to the {@link TrackedRace}. As a result, the {@link #reconciler} will listen
     * to all those race logs for events that may require reconciliation of results, race and competitor statuses.
     */
    private void registerRaceAndCompetitorStatusWithRaceLogReconciler(final DynamicTrackedRace trackedRace) {
        if (reconciler != null) {
            trackedRace.addListener(new AbstractRaceChangeListener() {
                @Override
                public void raceLogAttached(RaceLog raceLog) {
                    reconciler.raceLogAttached(trackedRace, raceLog);
                }
    
                @Override
                public void raceLogDetached(RaceLog raceLog) {
                    reconciler.raceLogDetached(trackedRace, raceLog);
                }
            });
            for (final RaceLog alreadyAttachedRaceLog : trackedRace.getAttachedRaceLogs()) {
                reconciler.raceLogAttached(trackedRace, alreadyAttachedRaceLog);
            }
        }
    }

    /**
     * When this tracker is informed that the race cannot be loaded, e.g., because its boat class does
     * not match the regatta's boat class, the tracker will {@link #stop} preemptively.
     */
    @Override
    public void raceNotLoaded(String reason) throws MalformedURLException, IOException, InterruptedException {
        logger.severe("Race for tracker "+this+" with ID "+getID()+" did not load: "+reason+". Stopping tracker.");
        if (race != null) {
            if (race == null) {
                trackedRegattaRegistry.stopTracker(regatta, this);
            } else {
                trackedRegattaRegistry.stopTracking(regatta, race);
            }
        }
    }

    @Override
    public void gotLiveDataEvent(ILiveDataEvent liveDataEvent) {
        logger.info("Status change in tracker "+getID()+" for race(s) "+getRace()+": "+liveDataEvent);
    }

    @Override
    public void gotStoredDataEvent(IStoredDataEvent storedDataEvent) {
        logger.info("Status change in tracker "+this+" with ID "+getID()+" for race(s) "+getRace()+": "+storedDataEvent);
        switch (storedDataEvent.getType()) {
        case Begin:
            logger.info("Stored data begin in tracker "+getID()+" for race(s) "+getRace());
            lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, 0);
            updateStatusOfTrackedRaces();
            break;
        case End:
            logger.info("Stored data end in tracker "+getID()+" for race(s) "+getRace());
            if (isLiveTracking) {
                lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1);
                updateStatusOfTrackedRaces();
            }
            break;
        case Progress:
            storedDataProgress(storedDataEvent.getProgress());
            break;
        case Error:
            logger.warning("Error with stored data in tracker "+getID()+" for race(s) "+getRace()+": "+storedDataEvent.getError());
            break;
        }
    }

    @Override
    public void stopped(Object o) {
        logger.info("stopped TracTrac tracking in tracker " + this + " with ID " + getID() + " for " + getRace()
                + " while in status " + lastStatus);
        new AbstractLoadingQueueDoneCallBack(receivers) {
            @Override
            protected void executeWhenAllReceiversAreDoneLoading() {
                lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.FINISHED, 1.0);
                updateStatusOfTrackedRaces();
                if (!stopped) {
                    // launch background thread that may have to wait for the race to appear; wait with a generous timeout
                    final Thread stopper = new Thread(()->{
                        try {
                            if (getRaceHandle().getRace(TIMEOUT_FOR_RACE_TO_APPEAR_FOR_STOPPING_TRACKING_IN_MILLIS) != null) {
                                // See also bug 1517; with TracAPI we assume that when stopped(IEvent) is called by the
                                // TracAPI then all subscriptions have received all their data and it's therefore safe to stop all
                                // subscriptions at this point without missing any data.
                                logger.info("Calling stopTracking for tracker "+this+" with ID "+getID());
                                trackedRegattaRegistry.stopTracking(regatta, getRace());
                            } else {
                                logger.warning("Didn't receive RaceDefinition for tracker "+this+" with ID "+getID()+
                                        " within "+TIMEOUT_FOR_RACE_TO_APPEAR_FOR_STOPPING_TRACKING_IN_MILLIS+
                                        "ms; unable to call stopTracking(...)");
                            }
                        } catch (InterruptedException | IOException e) {
                            logger.log(Level.INFO, "Interrupted while trying to stop tracker " + this, e);
                        }
                    }, "Stopper for tracker "+this+" with ID " + getID());
                    stopper.setDaemon(true);
                    stopper.start();
                }
            }
        };
    }

}
