package com.sap.sailing.domain.tractracadapter.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.maptrack.client.io.TypeController;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.tracking.AbstractRaceTrackerImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.TracTracControlPoint;
import com.sap.sailing.domain.tractracadapter.TracTracRaceTracker;
import com.tractrac.clientmodule.ControlPoint;
import com.tractrac.clientmodule.Event;
import com.tractrac.clientmodule.Race;
import com.tractrac.clientmodule.data.DataController;
import com.tractrac.clientmodule.data.DataController.Listener;
import com.tractrac.clientmodule.setup.KeyValue;

import difflib.PatchFailedException;

public class TracTracRaceTrackerImpl extends AbstractRaceTrackerImpl implements Listener, TracTracRaceTracker, DynamicRaceDefinitionSet {
    private static final Logger logger = Logger.getLogger(TracTracRaceTrackerImpl.class.getName());
    
    /**
     * A scheduler for the periodic checks of the paramURL documents for the advent of {@link ControlPoint}s
     * with static position information otherwise not available through {@link MarkPassingReceiver}'s events.
     */
    static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    /**
     * This value indicated how many stored data packets we allow that are not in the right sequence
     * Background: It can happen that the progress for storedData hops around and delivers a progress
     * that is lower than one received before. This can happen but only at a maximum of times this constant describes.
     */
    static final Integer MAX_STORED_PACKET_HOP_ALLOWANCE = 3;
    
    private final Event tractracEvent;
    private final com.sap.sailing.domain.base.Regatta regatta;
    private final Thread ioThread;
    private final DataController controller;
    private final Set<Receiver> receivers;
    private final DomainFactory domainFactory;
    private final WindStore windStore;
    private final Set<RaceDefinition> races;
    private final DynamicTrackedRegatta trackedRegatta;
    private TrackedRaceStatus lastStatus;
    private HashMap<Triple<URL, URI, URI>, Pair<Integer, Float>> lastProgressPerID;

    /**
     * paramURL, liveURI and storedURI for TracTrac connection
     */
    private final Triple<URL, URI, URI> urls;
    private final ScheduledFuture<?> controlPointPositionPoller;

    /**
     * Tells if this tracker was created with a valid live URI. If not, the tracker will stop and unregister itself
     * from the {@link RacingEventService} after having received all stored data.
     */
    private final boolean isLiveTracking;

    /**
     * Creates a race tracked for the specified URL/URIs and starts receiving all available existing and future push
     * data from there. Receiving continues until {@link #stop()} is called.
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
     * respond with the {@link RaceDefinition} when its {@link DomainFactory#getRaces(Event)} is called with the
     * TracTrac {@link Event} as argument that is used for its tracking.
     * <p>
     * @param startOfTracking
     *            if <code>null</code>, all stored data from the "beginning of time" will be loaded that the event has
     *            to provide, particularly for the mark positions which are stored per event, not per race; otherwise,
     *            particularly the mark position loading will be constrained to this start time.
     * @param endOfTracking
     *            if <code>null</code>, all stored data until the "end of time" will be loaded that the event has to
     *            provide, particularly for the mark positions which are stored per event, not per race; otherwise,
     *            particularly the mark position loading will be constrained to this end time.
     * @param windStore
     *            Provides the capability to obtain the {@link WindTrack}s for the different wind sources. A trivial
     *            implementation is {@link EmptyWindStore} which simply provides new, empty tracks. This is always
     *            available but loses track of the wind, e.g., during server restarts.
     * @param trackedRegattaRegistry
     *            used to create the {@link TrackedRegatta} for the domain event
     */
    protected TracTracRaceTrackerImpl(DomainFactory domainFactory, URL paramURL, URI liveURI, URI storedURI, URI courseDesignUpdateURI,
            TimePoint startOfTracking, TimePoint endOfTracking, long delayToLiveInMillis,
            boolean simulateWithStartTimeNow, RaceLogStore raceLogStore, WindStore windStore, String tracTracUsername, String tracTracPassword, TrackedRegattaRegistry trackedRegattaRegistry)
            throws URISyntaxException, MalformedURLException, FileNotFoundException {
        this(KeyValue.setup(paramURL), domainFactory, paramURL, liveURI, storedURI, courseDesignUpdateURI, startOfTracking, endOfTracking,
                delayToLiveInMillis, simulateWithStartTimeNow, raceLogStore, windStore, tracTracUsername, tracTracPassword, trackedRegattaRegistry);
    }
    
    private TracTracRaceTrackerImpl(Event tractracEvent, DomainFactory domainFactory, URL paramURL, URI liveURI, URI storedURI, URI courseDesignUpdateURI,
            TimePoint startOfTracking, TimePoint endOfTracking, long delayToLiveInMillis, boolean simulateWithStartTimeNow,
            RaceLogStore raceLogStore, WindStore windStore, String tracTracUsername, String tracTracPassword, TrackedRegattaRegistry trackedRegattaRegistry) 
                throws URISyntaxException, MalformedURLException, FileNotFoundException {
        this(tractracEvent, null, domainFactory, paramURL, liveURI, storedURI, courseDesignUpdateURI,
                startOfTracking, endOfTracking, delayToLiveInMillis, simulateWithStartTimeNow, raceLogStore, windStore, 
                tracTracUsername, tracTracPassword, trackedRegattaRegistry);
    }
    
    /**
     * Use this constructor if the {@link Regatta} in which to arrange the {@link RaceDefinition}s created by this
     * tracker is already known up-front, particularly if it has a specific configuration to use. Other constructors
     * may create a default {@link Regatta} with only a single default {@link Series} and {@link Fleet} which may not
     * always be what you want.
     */
    protected TracTracRaceTrackerImpl(Regatta regatta, DomainFactory domainFactory, URL paramURL, URI liveURI, URI storedURI, URI courseDesignUpdateURI,
            TimePoint startOfTracking, TimePoint endOfTracking, long delayToLiveInMillis, boolean simulateWithStartTimeNow,
            RaceLogStore raceLogStore, WindStore windStore, String tracTracUsername, String tracTracPassword, TrackedRegattaRegistry trackedRegattaRegistry) 
                throws URISyntaxException, MalformedURLException, FileNotFoundException {
        this(KeyValue.setup(paramURL), regatta, domainFactory, paramURL, liveURI, storedURI, courseDesignUpdateURI, startOfTracking,
                endOfTracking, delayToLiveInMillis, simulateWithStartTimeNow, raceLogStore, windStore, 
                tracTracUsername, tracTracPassword, trackedRegattaRegistry);
    }
    
    /**
     * 
     * @param regatta
     *            if <code>null</code>, then <code>domainFactory.getOrCreateRegatta(tractracEvent)</code> will be used
     *            to obtain a default regatta
     * @param simulateWithStartTimeNow
     *            if <code>true</code>, the connector will adjust the time stamps of all events received such that the
     *            first mark passing for the first waypoint will be set to "now." It will delay the forwarding of all
     *            events received such that they seem to be sent in "real-time." So, more or less the time points
     *            attached to the events sent to the receivers will again approximate the wall time.
     */
    private TracTracRaceTrackerImpl(Event tractracEvent, final Regatta regatta, DomainFactory domainFactory,
            URL paramURL, URI liveURI, URI storedURI, URI tracTracUpdateURI, TimePoint startOfTracking, TimePoint endOfTracking,
            long delayToLiveInMillis, boolean simulateWithStartTimeNow, RaceLogStore raceLogStore, 
            WindStore windStore, String tracTracUsername, String tracTracPassword, TrackedRegattaRegistry trackedRegattaRegistry)
            throws URISyntaxException, MalformedURLException, FileNotFoundException {
        super();
        this.tractracEvent = tractracEvent;
        urls = createID(paramURL, liveURI, storedURI);
        isLiveTracking = liveURI != null;
        this.races = new HashSet<RaceDefinition>();
        this.windStore = windStore;
        this.domainFactory = domainFactory;
        this.lastProgressPerID = new HashMap<Triple<URL, URI, URI>, Pair<Integer, Float>>();
        final Simulator simulator;
        if (simulateWithStartTimeNow) {
            simulator = new Simulator(windStore);
        } else {
            simulator = null;
        }
        // can happen that TracTrac event is null (occurs when there is no Internet connection)
        // so lets raise some meaningful exception
        if (tractracEvent == null) {
            throw new RuntimeException("Connection failed. Could not connect to " + paramURL);
        }
        
        logger.info("Starting race tracker: " + tractracEvent.getName() + " " + paramURL + " " + liveURI + " "
                + storedURI + " startOfTracking:" + (startOfTracking != null ? startOfTracking.asMillis() : "n/a") + " endOfTracking:" + (endOfTracking != null ? endOfTracking.asMillis() : "n/a"));
        
        // check if there is a directory configured where stored data files can be cached
        storedURI = checkForCachedStoredData(storedURI);
        
        // Initialize data controller using live and stored data sources
        controller = new DataController(liveURI, storedURI, this);
        // Start live and stored data streams
        ioThread = new Thread(controller, "I/O for event "+tractracEvent.getName()+", race URL "+paramURL);
        Regatta effectiveRegatta = regatta;
        for (Race tractracRace : tractracEvent.getRaceList()) {
            // Try to find a pre-associated event based on the Race ID
            if (effectiveRegatta == null) {
                Serializable raceID = domainFactory.getRaceID(tractracRace);
                effectiveRegatta = trackedRegattaRegistry.getRememberedRegattaForRace(raceID);
            }
            // removeRace may detach the domain regatta from the domain factory if that
            // removed the last race; therefore, it's important to getOrCreate the
            // domain regatta *after* calling removeRace
            domainFactory.removeRace(tractracEvent, tractracRace, trackedRegattaRegistry);
        }
        // if regatta is still null, no previous assignment of any of the races in this TracTrac event to a Regatta was found;
        // in this case, create a default regatta based on the TracTrac event data
        this.regatta = effectiveRegatta == null ? domainFactory.getOrCreateDefaultRegatta(raceLogStore, tractracEvent, trackedRegattaRegistry) : effectiveRegatta;
        trackedRegatta = trackedRegattaRegistry.getOrCreateTrackedRegatta(this.regatta);
        receivers = new HashSet<Receiver>();
        Set<TypeController> typeControllers = new HashSet<TypeController>();
        for (Receiver receiver : domainFactory.getUpdateReceivers(getTrackedRegatta(), tractracEvent, startOfTracking,
                endOfTracking, delayToLiveInMillis, simulator, windStore, this, trackedRegattaRegistry, tracTracUpdateURI, tracTracUsername, tracTracPassword)) {
            receivers.add(receiver);
            for (TypeController typeController : receiver.getTypeControllersAndStart()) {
                typeControllers.add(typeController);
            }
        }
        addListenersForStoredDataAndStartController(typeControllers);
        // Read event data from configuration file
        synchronized (this) {
            controlPointPositionPoller = scheduleClientParamsPHPPoller(paramURL, simulator, tracTracUpdateURI,
                    delayToLiveInMillis, tracTracUsername, tracTracPassword);
            notifyAll(); // the stop(boolean) method will try to cancel the controlPointPositionPoller; this may happen even before the above assignment took place; synchronize!
        }
    }

    private URI checkForCachedStoredData(URI storedURI){
        if (System.getProperty("cache.dir") != null) {
            final String directory = System.getProperty("cache.dir");
            if (new File(directory).exists()) {
                final String[] pathFragments = storedURI.getPath().split("\\/");
                final String mtbFileName = pathFragments[pathFragments.length-1];
                final String directoryAndFileName = directory+"/"+mtbFileName;
                if (!new File(directoryAndFileName).exists()) {
                    FileOutputStream mtbOutStream = null;
                    try {
                        logger.info("Starting to download " + storedURI + " to cache dir " + directoryAndFileName);
                        InputStream in = storedURI.toURL().openStream();
                        mtbOutStream = new FileOutputStream(new File(directoryAndFileName));
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

    static Triple<URL, URI, URI> createID(URL paramURL, URI liveURI, URI storedURI) {
        return new Triple<URL, URI, URI>(paramURL, liveURI, storedURI);
    }
    
    /**
     * Control points may get added late in the race. If they don't have a tracker installed, their position will be
     * static. This position can be retrieved from {@link ControlPoint#getLat1()} etc. This method registers a task with
     * {@link #scheduler} that regularly polls the <code>paramURL</code> to see if any new control points have arrived
     * or positions for existing control points have been received. Any new information in this direction will be
     * entered into the {@link TrackedRace} for the {@link #getRaces() race} tracked by this tracker.
     * 
     * @param paramURL
     *            points to the document describing the race's metadata which will periodically be downloaded
     * @param simulator
     *            if not <code>null</code>, use this simulator to translate start/stop tracking times received through
     *            clientparams document
     * @return the task to cancel in case the tracker wants to terminate the poller
     */
    private ScheduledFuture<?> scheduleClientParamsPHPPoller(final URL paramURL, final Simulator simulator,
            final URI tracTracUpdateURI, final long delayToLiveInMillis, final String tracTracUsername, final String tracTracPassword) {
        final Runnable command = new Runnable() {
            @Override public void run() {
                pollAndParseClientParamsPHP(paramURL, simulator, tracTracUpdateURI, delayToLiveInMillis, tracTracUsername, tracTracPassword);
            }
        };
        // now run the command once immediately and synchronously; see also bug 1345
        command.run();
        
        // then schedule for periodic execution in background
        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(command, /* initialDelay */ 30000, /* delay */ 15000, /* unit */ TimeUnit.MILLISECONDS);
        return task;
    }


    private void pollAndParseClientParamsPHP(final URL paramURL, final Simulator simulator,
            final URI tracTracUpdateURI, long delayToLiveInMillis, final String tracTracUsername,
            final String tracTracPassword) {
        // If no race is found, extract all information necessary to create it, in particular the competitor list, course information,
        // data about side lines from the race's metadata as well as the dominant boat class for the race. Otherwise, look for changes
        // and update accordingly where possible.
        logger.fine("Fetching paramURL "+paramURL+" to check for updates for race(s) "+getRaces());
        final ClientParamsPHP clientParams;
        try {
            clientParams = new ClientParamsPHP(paramURL, new InputStreamReader(paramURL.openStream()));
            if (clientParams.getRace() != null) {
                List<Pair<com.sap.sailing.domain.base.ControlPoint, NauticalSide>> newCourseControlPointsWithPassingSide = getControlPointsWithPassingSide(
                        clientParams, new ControlPointProducer<com.sap.sailing.domain.base.ControlPoint>() {
                            @Override
                            public com.sap.sailing.domain.base.ControlPoint produceControlPoint(
                                    TracTracControlPoint ttControlPoint) {
                                return domainFactory.getOrCreateControlPoint(ttControlPoint);
                            }
                        });
                if (getRaces() == null || getRaces().isEmpty()) {
                    // create race definition / tracked race and add to event
                    final String raceName = clientParams.getRace().getName();
                    logger.log(Level.INFO, "Found data for non-existing race " + raceName + " in " + paramURL
                            + ". Creating RaceDefinition.");
                    final Iterable<Competitor> competitors = getCompetitors(clientParams);
                    final Iterable<com.sap.sailing.domain.tractracadapter.impl.ClientParamsPHP.Competitor> competitorsInClientParams = clientParams
                            .getCompetitors();
                    List<Pair<TracTracControlPoint, NauticalSide>> ttControlPointsAndPassingSide = getControlPointsWithPassingSide(
                            clientParams, new ControlPointProducer<TracTracControlPoint>() {
                                @Override
                                public TracTracControlPoint produceControlPoint(TracTracControlPoint ttControlPoint) {
                                    return ttControlPoint;
                                }
                            });
                    Course course = domainFactory.createCourse(clientParams.getRace().getDefaultRoute()
                            .getDescription(), ttControlPointsAndPassingSide);
                    List<Sideline> sidelines = domainFactory.createSidelines(clientParams.getRace().getMetadata(),
                            clientParams.getEvent().getControlPointList());
                    DynamicTrackedRace trackedRace = domainFactory.getOrCreateRaceDefinitionAndTrackedRace(
                            getTrackedRegatta(), clientParams.getRace().getId(), raceName, competitors,
                            getDominantBoatClass(competitorsInClientParams), course, sidelines, windStore,
                            delayToLiveInMillis, WindTrack.DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND, /* raceDefinitionSetToUpdate */
                            this, tracTracUpdateURI, tractracEvent.getId(), tracTracUsername, tracTracPassword);
                    if (simulator != null) {
                        simulator.setTrackedRace(trackedRace);
                    }
                }
                compareAndUpdateCourseIfNecessary(newCourseControlPointsWithPassingSide);
                updateStartStopTimesAndLiveDelay(clientParams, simulator);
                updateMarkPositionsIfNoPositionsReceivedYet(clientParams);
            }
        } catch (Exception e) {
            logger.info("Exception " + e.getMessage() + " while trying to read clientparams.php for races " + getRaces());
            logger.log(Level.SEVERE, "scheduleClientParamsPHPPoller.run", e);
        }
    }

    private Iterable<Competitor> getCompetitors(ClientParamsPHP clientParams) {
        List<Competitor> result = new ArrayList<>();
        for (ClientParamsPHP.Competitor cpc : clientParams.getCompetitors()) {
            result.add(getCompetitor(cpc));
        }
        return result;
    }

    private Competitor getCompetitor(com.sap.sailing.domain.tractracadapter.impl.ClientParamsPHP.Competitor competitor) {
        final com.sap.sailing.domain.tractracadapter.impl.ClientParamsPHP.BoatClass boatClass = competitor.getBoatClass();
        return domainFactory.getOrCreateCompetitor(
                competitor.getId(), boatClass==null?null:boatClass.getName(), competitor.getNationality(), competitor.getName(),
                competitor.getShortName());
    }

    private BoatClass getDominantBoatClass(
            Iterable<com.sap.sailing.domain.tractracadapter.impl.ClientParamsPHP.Competitor> competitorsInClientParams) {
        List<String> competitorClassNames = new ArrayList<>();
        for (com.sap.sailing.domain.tractracadapter.impl.ClientParamsPHP.Competitor competitor : competitorsInClientParams) {
            final com.sap.sailing.domain.tractracadapter.impl.ClientParamsPHP.BoatClass boatClass = competitor.getBoatClass();
            if (boatClass != null) {
                competitorClassNames.add(boatClass.getName());
            }
        }
        return domainFactory.getDominantBoatClass(competitorClassNames);
    }

    /**
     * set mark positions from static positions specified in document in case there is nothing loaded through TTCM yet
     */
    private void updateMarkPositionsIfNoPositionsReceivedYet(final ClientParamsPHP clientParams) {
        for (TracTracControlPoint controlPoint : clientParams.getEvent().getControlPointList()) {
            com.sap.sailing.domain.base.ControlPoint domainControlPoint = domainFactory.getOrCreateControlPoint(controlPoint);
            boolean first = true;
            for (Mark mark : domainControlPoint.getMarks()) {
                for (RaceDefinition raceDefinition : getRaces()) {
                    DynamicTrackedRace trackedRace = getTrackedRegatta().getExistingTrackedRace(raceDefinition);
                    if (trackedRace != null) {
                        DynamicGPSFixTrack<Mark, GPSFix> markTrack = trackedRace.getOrCreateTrack(mark);
                        if (markTrack.getFirstRawFix() == null) {
                            final Position position = first ? controlPoint.getMark1Position() : controlPoint.getMark2Position();
                            if (position != null) {
                                logger.info("Adding fix "+position+" from "+clientParams.getParamsUrl()+" for control point "+controlPoint.getName()+
                                        " in race "+raceDefinition.getName());
                                markTrack.addGPSFix(new GPSFixImpl(position, MillisecondsTimePoint.now()));
                            }
                        }
                    }
                }
                first = false;
            }
        }
    }

    /**
     * For all races tracked, the course is compared to the course described in <code>newCourseControlPointsWithPassingSide</code>.
     * If they differ, a {@link Course#update(Iterable, com.sap.sailing.domain.base.DomainFactory) course update} is triggered.
     */
    private void compareAndUpdateCourseIfNecessary(
            List<Pair<com.sap.sailing.domain.base.ControlPoint, NauticalSide>> newCourseControlPointsWithPassingSide) {
        assert getRaces() != null;
        // to check if a course update is required, compare to the existing course's control points:
        List<com.sap.sailing.domain.base.ControlPoint> newCourseControlPoints = new ArrayList<>();
        for (Pair<com.sap.sailing.domain.base.ControlPoint, NauticalSide> controlPointAndPassingSide : newCourseControlPointsWithPassingSide) {
            newCourseControlPoints.add(controlPointAndPassingSide.getA());
        }
        List<com.sap.sailing.domain.base.ControlPoint> currentCourseControlPoints = new ArrayList<>();
        for (RaceDefinition race : getRaces()) {
            final Course course = race.getCourse();
            for (Waypoint waypoint : course.getWaypoints()) {
                currentCourseControlPoints.add(waypoint.getControlPoint());
            }
            if (!newCourseControlPoints.equals(currentCourseControlPoints)) {
                logger.info("Detected course change based on clientparams.php contents for races " + getRaces());
                try {
                    course.update(newCourseControlPointsWithPassingSide, domainFactory.getBaseDomainFactory());
                } catch (PatchFailedException pfe) {
                    logger.severe("Failed to apply course update " + newCourseControlPointsWithPassingSide
                            + " to course " + course);
                    logger.log(Level.SEVERE, "scheduleClientParamsPHPPoller.run", pfe);
                }
            }
        }
    }

    private interface ControlPointProducer<T> {
        T produceControlPoint(TracTracControlPoint ttControlPoint);
    }
    
    private <T> List<Pair<T, NauticalSide>> getControlPointsWithPassingSide( final ClientParamsPHP clientParams, ControlPointProducer<T> controlPointProducer) {
        List<Pair<T, NauticalSide>> newCourseControlPointsWithPassingSide = new ArrayList<>();
        final List<? extends TracTracControlPoint> newTracTracControlPoints = clientParams.getRace().getDefaultRoute().getControlPoints();
        Map<Integer, NauticalSide> passingSideData = domainFactory.getMetadataParser().parsePassingSideData(
                clientParams.getRace().getDefaultRoute().getMetadata(), newTracTracControlPoints);
        int i = 1;
        for (TracTracControlPoint newTracTracControlPoint : newTracTracControlPoints) {
            NauticalSide nauticalSide = passingSideData.containsKey(i) ? passingSideData.get(i) : null;
            final T newControlPoint = controlPointProducer.produceControlPoint(newTracTracControlPoint);
            newCourseControlPointsWithPassingSide.add(new Pair<T, NauticalSide>(newControlPoint, nauticalSide));
            i++;
        }
        return newCourseControlPointsWithPassingSide;
    }

    private void updateStartStopTimesAndLiveDelay(ClientParamsPHP clientParams, Simulator simulator) throws ParseException {
        RaceDefinition currentRace = null;
        long delayInMillis = clientParams.getLiveDelayInMillis();
        RaceDefinition race = getRegatta().getRaceByName(clientParams.getRace().getName());
        if (race != null) {
            currentRace = race;
            final DynamicTrackedRace trackedRace = getTrackedRegatta().getExistingTrackedRace(currentRace);
            if (trackedRace != null) {
                trackedRace.setDelayToLiveInMillis(delayInMillis);
            }
        }
        if (currentRace != null) {
            final DynamicTrackedRace trackedRace = getTrackedRegatta().getExistingTrackedRace(currentRace);
            if (trackedRace != null) {
                TimePoint startOfTracking = clientParams.getRace().getTrackingStartTime();
                if (startOfTracking != null) {
                    trackedRace.setStartOfTrackingReceived(simulator == null ? startOfTracking : simulator
                            .advance(startOfTracking));
                }
                TimePoint endOfTracking = clientParams.getRace().getTrackingEndTime();
                if (endOfTracking != null) {
                    trackedRace.setEndOfTrackingReceived(simulator == null ? endOfTracking : simulator
                            .advance(endOfTracking));
                }
                TimePoint raceStartTime = clientParams.getRace().getStartTime();
                if (raceStartTime != null) {
                    trackedRace.setStartTimeReceived(raceStartTime);
                }
            }
        }
    }

    @Override
    public Triple<URL, URI, URI> getID() {
        return urls;
    }

    @Override
    public WindStore getWindStore() {
        return windStore;
    }

    @Override
    public RacesHandle getRacesHandle() {
        return new RaceHandleImpl(domainFactory, tractracEvent, getTrackedRegatta(), this);
    }
    
    @Override
    public Set<RaceDefinition> getRaces() {
        return races;
    }
    
    protected void addListenersForStoredDataAndStartController(Iterable<TypeController> listenersForStoredData) {
        for (TypeController listener : listenersForStoredData) {
            getController().add(listener);
        }
        startController();
    }
    
    @Override
    public com.sap.sailing.domain.base.Regatta getRegatta() {
        return regatta;
    }
    
    /**
     * Called when the {@link #storedDataEnd()} event was received. Adds the listeners
     * returned to the {@link #getController() controller}, presumably for live data.
     * This default implementation returns an empty iterable. Subclasses may override
     * to return more.
     */
    protected Iterable<TypeController> getListenersForLiveData() {
        return Collections.emptySet();
    }

    protected void startController() {
        ioThread.start();
    }
    
    @Override
    public void stop() throws InterruptedException {
        stop(/* stop receivers preemtively */ true);
    }

    private void stop(boolean stopReceiversPreemtively) throws InterruptedException {
        synchronized (this) {
            while (controlPointPositionPoller == null) {
                wait(); // constructor will notify all waiters once the controlPointPositionPoller is set
            }
        }
        controlPointPositionPoller.cancel(/* mayInterruptIfRunning */ false);
        new Thread("TracTrac Controller Stopper for "+getID()) {
            public void run() {
                controller.stop(/* abortStored */ true);
                try {
                    ioThread.join();
                    if (ioThread.isAlive()) {
                        logger.severe("Tractrac IO thread in tracker "+getID()+" for race(s) "+getRaces()+" joined but is still active. Very strange.");
                    } else {
                        logger.info("Joined TracTrac IO thread in tracker "+getID()+" for race(s) "+getRaces());
                    }
                    lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.FINISHED, /* will be ignored */ 1.0);
                    updateStatusOfTrackedRaces();
                } catch (InterruptedException e) {
                    logger.log(Level.INFO, "Interrupted while trying to join TracTrac DataController thread for "+getID());
                } // wait no more than three seconds
            }
        }.start();
        for (Receiver receiver : receivers) {
            if (stopReceiversPreemtively) {
                receiver.stopPreemptively();
            } else {
                receiver.stopAfterProcessingQueuedEvents();
            }
        }
    }

    protected DataController getController() {
        return controller;
    }

    @Override
    public void liveDataConnected() {
        logger.info("Live data connected in tracker "+getID()+" for race(s) "+getRaces());
    }

    @Override
    public void liveDataDisconnected() {
        logger.info("Live data disconnected in tracker "+getID()+" for race(s) "+getRaces());
    }

    @Override
    public void stopped() {
        logger.info("stopped TracTrac tracking in tracker "+getID()+" for "+getRaces());
        lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1.0);
        updateStatusOfTrackedRaces();
        // don't stop the tracker (see bug 1517) as it seems that the storedData... callbacks are unreliable, and
        // we have seen many more fixes been transmitted after having received stopped()
    }

    /**
     * Propagates {@link #lastStatus} to all tracked races to which this tracker writes.
     * 
     * @see #updateStatusOfTrackedRace(DynamicTrackedRace)
     */
    private void updateStatusOfTrackedRaces() {
        for (RaceDefinition race : getRaces()) {
            DynamicTrackedRace trackedRace = getTrackedRegatta().getExistingTrackedRace(race);
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
            trackedRace.setStatus(status);
        }
    }

    @Override
    public void storedDataBegin() {
        logger.info("Stored data begin in tracker "+getID()+" for race(s) "+getRaces());
        lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, 0);
        updateStatusOfTrackedRaces();
    }

    @Override
    public void storedDataEnd() {
        logger.info("Stored data end in tracker "+getID()+" for race(s) "+getRaces());
        if (isLiveTracking) {
            lastStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 1);
            updateStatusOfTrackedRaces();
        }
    }

    @Override
    public void storedDataProgress(float progress) {
        logger.info("Stored data progress in tracker "+getID()+" for race(s) "+getRaces()+": "+progress);
        Integer counter = 0;
        final Pair<Integer, Float> lastProgressPair = lastProgressPerID.get(getID());
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
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                } else {
                    counter += 1;
                }
            } 
        }
        lastStatus = new TrackedRaceStatusImpl(progress==1.0 ? TrackedRaceStatusEnum.TRACKING : TrackedRaceStatusEnum.LOADING, progress);
        lastProgressPerID.put(getID(), new Pair<Integer, Float>(counter, progress));
        updateStatusOfTrackedRaces();
    }

    @Override
    public void storedDataError(String arg0) {
        logger.warning("Error with stored data in tracker "+getID()+" for race(s) "+getRaces()+": "+arg0);
    }

    @Override
    public void liveDataConnectError(String arg0) {
        logger.warning("Error with live data in tracker "+getID()+" for race(s) "+getRaces()+": "+arg0);
    }

    @Override
    public void addRaceDefinition(RaceDefinition race, DynamicTrackedRace trackedRace) {
        races.add(race);
        updateStatusOfTrackedRace(trackedRace);
    }

}
