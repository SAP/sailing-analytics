package com.sap.sailing.domain.tractracadapter.impl;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.maptrack.client.io.TypeController;
import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.impl.DegreePosition;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.tracking.AbstractRaceTrackerImpl;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.TrackedEvent;
import com.sap.sailing.domain.tracking.TrackedEventRegistry;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.domain.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.domain.tractracadapter.TracTracRaceTracker;
import com.sap.sailing.util.Util.Triple;
import com.tractrac.clientmodule.ControlPoint;
import com.tractrac.clientmodule.Event;
import com.tractrac.clientmodule.Race;
import com.tractrac.clientmodule.data.DataController;
import com.tractrac.clientmodule.data.DataController.Listener;
import com.tractrac.clientmodule.setup.KeyValue;

public class TracTracRaceTrackerImpl extends AbstractRaceTrackerImpl implements Listener, TracTracRaceTracker, DynamicRaceDefinitionSet {
    private static final Logger logger = Logger.getLogger(TracTracRaceTrackerImpl.class.getName());
    
    /**
     * A scheduler for the periodic checks of the paramURL documents for the advent of {@link ControlPoint}s
     * with static position information otherwise not available through {@link MarkPassingReceiver}'s events.
     */
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final Event tractracEvent;
    private final com.sap.sailing.domain.base.Event domainEvent;
    private final Thread ioThread;
    private final DataController controller;
    private final Set<Receiver> receivers;
    private final DomainFactory domainFactory;
    private final WindStore windStore;
    private final Set<RaceDefinition> races;
    
    /**
     * paramURL, liveURI and storedURI for TracTrac connection
     */
    private final Triple<URL, URI, URI> urls;
    private final ScheduledFuture<?> controlPointPositionPoller;

    /**
     * Creates a race tracked for the specified URL/URIs and starts receiving all available existing and future push
     * data from there. Receiving continues until {@link #stop()} is called.
     * <p>
     * 
     * A race tracker uses the <code>paramURL</code> for the TracTrac Java client to register for push data about one
     * race. The {@link DomainFactory} is asked to retrieve an existing or create a new {@link com.sap.sailing.domain.base.Event}
     * based on the TracTrac event. The {@link RaceDefinition} for the race, however, isn't created until the {@link Course} has been
     * received. Therefore, the {@link RaceCourseReceiver} will create the {@link RaceDefinition} and will add it to the
     * {@link com.sap.sailing.domain.base.Event}.
     * <p>
     * 
     * The link to the {@link RaceDefinition} is created in the {@link DomainFactory} when the
     * {@link RaceCourseReceiver} creates the {@link TrackedRace} object. Starting then, the {@link DomainFactory} will
     * respond with the {@link RaceDefinition} when its {@link DomainFactory#getRaces(Event)} is called with the TracTrac
     * {@link Event} as argument that is used for its tracking.
     * <p>
     * @param windStore
     *            Provides the capability to obtain the {@link WindTrack}s for the different wind sources. A trivial
     *            implementation is {@link EmptyWindStore} which simply provides new, empty tracks. This is always
     *            available but loses track of the wind, e.g., during server restarts.
     * @param trackedEventRegistry used to create the {@link TrackedEvent} for the domain event
     */
    protected TracTracRaceTrackerImpl(DomainFactory domainFactory, URL paramURL, URI liveURI, URI storedURI,
            WindStore windStore, TrackedEventRegistry trackedEventRegistry) throws URISyntaxException,
            MalformedURLException, FileNotFoundException {
        urls = new Triple<URL, URI, URI>(paramURL, liveURI, storedURI);
        this.races = new HashSet<RaceDefinition>();
        this.windStore = windStore;
        this.domainFactory = domainFactory;
        // Read event data from configuration file
        tractracEvent = KeyValue.setup(paramURL);
        controlPointPositionPoller = scheduleControlPointPositionPoller(paramURL);
        // can happen that TracTrac event is null (occurs when there is no Internet connection)
        // so lets raise some meaningful exception
        if (tractracEvent == null) {
            throw new RuntimeException("Connection failed. Could not connect to " + paramURL);
        }
        
        // Initialize data controller using live and stored data sources
        controller = new DataController(liveURI, storedURI, this);
        // Start live and stored data streams
        ioThread = new Thread(controller, "io");
        for (Race tractracRace : tractracEvent.getRaceList()) {
            // removeRace may detach the domain event from the domain factory if that
            // removed the last race; therefore, it's important to getOrCreate the
            // domainEvent *after* calling removeRace
            domainFactory.removeRace(tractracEvent, tractracRace, trackedEventRegistry);
        }
        domainEvent = domainFactory.getOrCreateEvent(tractracEvent);
        setTrackedEvent(trackedEventRegistry.getOrCreateTrackedEvent(domainEvent));
        receivers = new HashSet<Receiver>();
        Set<TypeController> typeControllers = new HashSet<TypeController>();
        for (Receiver receiver : domainFactory.getUpdateReceivers(getTrackedEvent(), tractracEvent, windStore, this)) {
            receivers.add(receiver);
            for (TypeController typeController : receiver.getTypeControllersAndStart()) {
                typeControllers.add(typeController);
            }
        }
        addListenersForStoredDataAndStartController(typeControllers);
    }
    
    /**
     * Control points may get added late in the race. If they don't have a tracker installed, their position
     * will be static. This position can be retrieved from {@link ControlPoint#getLat1()} etc. This method registers
     * a task with {@link #scheduler} that regularly polls the <code>paramURL</code> to see if any new control points
     * have arrived or positions for existing control points have been received. Any new information in this
     * direction will be entered into the {@link TrackedRace} for the {@link #getRaces() race} tracked by this
     * tracker.
     * 
     * @param paramURL points to the document describing the race's metadata which will periodically be downloaded
     * @return the task to cancel in case the tracker wants to terminate the poller
     */
    private ScheduledFuture<?> scheduleControlPointPositionPoller(final URL paramURL) {
        ScheduledFuture<?> task = scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override public void run() {
                Set<RaceDefinition> raceDefinitions = getRaces();
                if (raceDefinitions != null && !raceDefinitions.isEmpty()) {
                    logger.info("fetching paramURL to check for new ControlPoint positions...");
                    Event event = KeyValue.setup(paramURL);
                    for (ControlPoint controlPoint : event.getControlPointList()) {
                        com.sap.sailing.domain.base.ControlPoint domainControlPoint = domainFactory.getControlPoint(controlPoint);
                        boolean first = true;
                        for (Buoy buoy : domainControlPoint.getBuoys()) {
                            for (RaceDefinition raceDefinition : raceDefinitions) {
                                DynamicTrackedRace trackedRace = getTrackedEvent().getExistingTrackedRace(
                                        raceDefinition);
                                DynamicTrack<Buoy, GPSFix> buoyTrack = trackedRace.getOrCreateTrack(buoy);
                                if (buoyTrack.getFirstRawFix() == null) {
                                    buoyTrack.addGPSFix(new GPSFixImpl(new DegreePosition(first ? controlPoint
                                            .getLat1() : controlPoint.getLat2(), first ? controlPoint.getLon1()
                                            : controlPoint.getLon2()), MillisecondsTimePoint.now()));
                                }
                            }
                            first = false;
                        }
                    }
                }
            }
        }, /* initialDelay */ 30000, /* delay */ 15000, /* unit */ TimeUnit.MILLISECONDS);
        return task;
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
    public RaceHandle getRaceHandle() {
        return new RaceHandleImpl(domainFactory, tractracEvent, getTrackedEvent(), this);
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
    public com.sap.sailing.domain.base.Event getEvent() {
        return domainEvent;
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
    public void stop() throws MalformedURLException, IOException, InterruptedException {
        controlPointPositionPoller.cancel(/* mayInterruptIfRunning */ false);
        controller.stop(/* abortStored */ true);
        for (Receiver receiver : receivers) {
            receiver.stopPreemptively();
        }
        ioThread.join(3000); // wait no more than three seconds
        logger.info("Joined TracTrac IO thread for race(s) "+getRaces());
        super.stop();
    }

    protected DataController getController() {
        return controller;
    }

    @Override
    public void liveDataConnected() {
        logger.info("Live data connected for race(s) "+getRaces());
    }

    @Override
    public void liveDataDisconnected() {
        logger.info("Live data disconnected for race(s) "+getRaces());
    }

    @Override
    public void stopped() {
        logger.info("stopped TracTrac tracking for "+getRaces());
    }

    @Override
    public void storedDataBegin() {
        logger.info("Stored data begin for race(s) "+getRaces());
    }

    @Override
    public void storedDataEnd() {
        logger.info("Stored data end for race(s) "+getRaces());
    }

    @Override
    public void storedDataProgress(float progress) {
        logger.info("Stored data progress for race(s) "+getRaces()+": "+progress);
        
    }

    @Override
    public void storedDataError(String arg0) {
        logger.warning("Error with stored data for race(s) "+getRaces()+": "+arg0);
    }

    @Override
    public void liveDataConnectError(String arg0) {
        logger.warning("Error with live data for race(s) "+getRaces()+": "+arg0);
    }

    @Override
    public void addRaceDefinition(RaceDefinition race) {
        races.add(race);
    }
}
