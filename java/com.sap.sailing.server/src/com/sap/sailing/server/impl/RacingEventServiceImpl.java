package com.sap.sailing.server.impl;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.EventListener;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.impl.EventImpl;
import com.sap.sailing.domain.common.DefaultLeaderboardName;
import com.sap.sailing.domain.common.EventAndRaceIdentifier;
import com.sap.sailing.domain.common.EventIdentifier;
import com.sap.sailing.domain.common.EventName;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.RaceInLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.ResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.impl.ScoreCorrectionImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterMessage;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.persistence.SwissTimingAdapterPersistence;
import com.sap.sailing.domain.tracking.DynamicTrackedEvent;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceChangeListener;
import com.sap.sailing.domain.tracking.RaceListener;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tracking.TrackedEvent;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.Wind;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTracker;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedEventImpl;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.JSONService;
import com.sap.sailing.domain.tractracadapter.RaceRecord;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.expeditionconnector.ExpeditionListener;
import com.sap.sailing.expeditionconnector.ExpeditionWindTrackerFactory;
import com.sap.sailing.expeditionconnector.UDPExpeditionReceiver;
import com.sap.sailing.mongodb.MongoDBService;
import com.sap.sailing.operationaltransformation.Operation;
import com.sap.sailing.server.OperationExecutionListener;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;
import com.sap.sailing.server.operationaltransformation.RecordBuoyGPSFix;
import com.sap.sailing.server.operationaltransformation.RecordCompetitorGPSFix;
import com.sap.sailing.server.operationaltransformation.RecordWindFix;
import com.sap.sailing.server.operationaltransformation.RemoveWindFix;
import com.sap.sailing.server.operationaltransformation.UpdateMarkPassings;
import com.sap.sailing.server.operationaltransformation.UpdateWindAveragingTime;

public class RacingEventServiceImpl implements RacingEventService, EventListener {
    private static final Logger logger = Logger.getLogger(RacingEventServiceImpl.class.getName());

    /**
     * A scheduler for the periodic checks of the paramURL documents for the advent of {@link ControlPoint}s
     * with static position information otherwise not available through <code>MarkPassingReceiver</code>'s events.
     */
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    
    private final DomainFactory tractracDomainFactory;
    
    private final com.sap.sailing.domain.swisstimingadapter.DomainFactory swissTimingDomainFactory;
    
    private final ExpeditionWindTrackerFactory windTrackerFactory;
    
    protected final Map<String, Event> eventsByName;
    
    private final Map<RaceDefinition, CourseChangeReplicator> courseListeners;
    
    protected final Map<Event, Set<RaceTracker>> raceTrackersByEvent;
    
    /**
     * Remembers the trackers by paramURL/liveURI/storedURI to avoid duplication
     */
    protected final Map<Object, RaceTracker> raceTrackersByID;
    
    /**
     * Leaderboards managed by this racing event service
     */
    private final Map<String, Leaderboard> leaderboardsByName;
    
    private final Map<String, LeaderboardGroup> leaderboardGroupsByName;
    
    private Set<DynamicTrackedEvent> eventsObservedForDefaultLeaderboard = new HashSet<DynamicTrackedEvent>();
    
    private final MongoObjectFactory mongoObjectFactory;
    
    private final DomainObjectFactory domainObjectFactory;
    
    private final SwissTimingFactory swissTimingFactory;
    
    private final SwissTimingAdapterPersistence swissTimingAdapterPersistence;

    private final Map<Event, DynamicTrackedEvent> eventTrackingCache;
    
    private final Set<OperationExecutionListener> operationExecutionListeners;

    public RacingEventServiceImpl() {
        this(MongoFactory.INSTANCE.getDefaultDomainObjectFactory(), MongoFactory.INSTANCE.getDefaultMongoObjectFactory());
    }
    
    private RacingEventServiceImpl(DomainObjectFactory domainObjectFactory, MongoObjectFactory mongoObjectFactory) {
        tractracDomainFactory = DomainFactory.INSTANCE;
        this.domainObjectFactory = domainObjectFactory;
        this.mongoObjectFactory = mongoObjectFactory;
        swissTimingFactory = SwissTimingFactory.INSTANCE;
        swissTimingDomainFactory = com.sap.sailing.domain.swisstimingadapter.DomainFactory.INSTANCE;
        swissTimingAdapterPersistence = SwissTimingAdapterPersistence.INSTANCE;
        windTrackerFactory = ExpeditionWindTrackerFactory.getInstance();
        eventsByName = new HashMap<String, Event>();
        eventTrackingCache = new HashMap<Event, DynamicTrackedEvent>();
        raceTrackersByEvent = new HashMap<Event, Set<RaceTracker>>();
        raceTrackersByID = new HashMap<Object, RaceTracker>();
        leaderboardGroupsByName = new HashMap<String, LeaderboardGroup>();
        leaderboardsByName = new HashMap<String, Leaderboard>();
        operationExecutionListeners = new HashSet<OperationExecutionListener>();
        courseListeners = new HashMap<RaceDefinition, CourseChangeReplicator>();
        // Add one default leaderboard that aggregates all races currently tracked by this service.
        // This is more for debugging purposes than for anything else.
        addLeaderboard(DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME, new int[] { 5, 8 });
        loadStoredLeaderboardsAndGroups();
    }
    
    public RacingEventServiceImpl(MongoDBService mongoDBService) {
        this(MongoFactory.INSTANCE.getDomainObjectFactory(mongoDBService), MongoFactory.INSTANCE.getMongoObjectFactory(mongoDBService));
    }
    
    private void loadStoredLeaderboardsAndGroups() {
        // Loading all leaderboard groups and putting the contained leaderboards
        for (LeaderboardGroup leaderboardGroup : domainObjectFactory.getAllLeaderboardGroups()) {
            leaderboardGroupsByName.put(leaderboardGroup.getName(), leaderboardGroup);
            for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                leaderboardsByName.put(leaderboard.getName(), leaderboard);
            }
        }
        // Loading the remaining leaderboards
        for (Leaderboard leaderboard : domainObjectFactory.getLeaderboardsNotInGroup()) {
            leaderboardsByName.put(leaderboard.getName(), leaderboard);
        }
    }
    
    @Override
    public Leaderboard addLeaderboard(String name, int[] discardThresholds) {
        Leaderboard result = new LeaderboardImpl(name, new ScoreCorrectionImpl(), new ResultDiscardingRuleImpl(
                discardThresholds));
        synchronized (leaderboardsByName) {
            if (leaderboardsByName.containsKey(name)) {
                throw new IllegalArgumentException("Leaderboard with name "+name+" already exists");
            }
            leaderboardsByName.put(name, result);
        }
        mongoObjectFactory.storeLeaderboard(result);
        return result;
    }
    
    @Override
    public void addColumnToLeaderboard(String columnName, String leaderboardName, boolean medalRace) {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            leaderboard.addRaceColumn(columnName, medalRace);
            updateStoredLeaderboard(leaderboard);
        } else {
            throw new IllegalArgumentException("Leaderboard named " + leaderboardName + " not found");
        }
    }

    @Override
    public void moveLeaderboardColumnUp(String leaderboardName, String columnName) {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            leaderboard.moveRaceColumnUp(columnName);
            updateStoredLeaderboard(leaderboard);
        } else {
            throw new IllegalArgumentException("Leaderboard named " + leaderboardName + " not found");
        }
    }

    @Override
    public void moveLeaderboardColumnDown(String leaderboardName, String columnName) {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            leaderboard.moveRaceColumnDown(columnName);
            updateStoredLeaderboard(leaderboard);
        } else {
            throw new IllegalArgumentException("Leaderboard named " + leaderboardName + " not found");
        }
    }

    @Override
    public void removeLeaderboardColumn(String leaderboardName, String columnName) {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            leaderboard.removeRaceColumn(columnName);
            updateStoredLeaderboard(leaderboard);
        } else {
            throw new IllegalArgumentException("Leaderboard named "+leaderboardName+" not found");
        }
    }

    @Override
    public void renameLeaderboardColumn(String leaderboardName, String oldColumnName, String newColumnName) {
        Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
        if (leaderboard != null) {
            leaderboard.getRaceColumnByName(oldColumnName).setName(newColumnName);
            updateStoredLeaderboard(leaderboard);
        } else {
            throw new IllegalArgumentException("Leaderboard named "+leaderboardName+" not found");
        }
    }

    @Override
    public void renameLeaderboard(String oldName, String newName) {
        synchronized (leaderboardsByName) {
            if (!leaderboardsByName.containsKey(oldName)) {
                throw new IllegalArgumentException("No leaderboard with name "+oldName+" found");
            }
            if (leaderboardsByName.containsKey(newName)) {
                throw new IllegalArgumentException("Leaderboard with name "+newName+" already exists");
            }
            Leaderboard toRename = leaderboardsByName.remove(oldName);
            toRename.setName(newName);
            leaderboardsByName.put(newName, toRename);
            mongoObjectFactory.renameLeaderboard(oldName, newName);
        }
    }
    
    @Override
    public void updateStoredLeaderboard(Leaderboard leaderboard) {
        mongoObjectFactory.storeLeaderboard(leaderboard);
        syncGroupsAfterLeaderboardChange(leaderboard, true);
    }
    
    /**
     * Checks all groups, if they contain a leaderboard with the name of the <code>updatedLeaderboard</code> and
     * replaces the one in the group with the updated one.<br />
     * This synchronizes things like the RaceIdentifier in the leaderboard columns.
     * 
     * @param updatedLeaderboard
     */
    private void syncGroupsAfterLeaderboardChange(Leaderboard updatedLeaderboard, boolean doDatabaseUpdate) {
        boolean groupNeedsUpdate = false;
        synchronized (leaderboardGroupsByName) {
            for (LeaderboardGroup leaderboardGroup : leaderboardGroupsByName.values()) {
                for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    if (leaderboard.getName().equals(updatedLeaderboard.getName())) {
                        int index = leaderboardGroup.getIndexOf(leaderboard);
                        leaderboardGroup.removeLeaderboard(leaderboard);
                        leaderboardGroup.addLeaderboardAt(updatedLeaderboard, index);
                        groupNeedsUpdate = true;
                        // TODO we assume that the leaderboard names are unique, so we can break the inner loop here
                        break;
                    }
                }
                
                if (doDatabaseUpdate && groupNeedsUpdate) {
                    mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);
                }
                groupNeedsUpdate = false;
            }
        }
    }
    
    @Override
    public void removeLeaderboard(String leaderboardName) {
        synchronized (leaderboardsByName) {
            leaderboardsByName.remove(leaderboardName);
        }
        mongoObjectFactory.removeLeaderboard(leaderboardName);
        
        syncGroupsAfterLeaderboardRemove(leaderboardName, true);
    }
    
    /**
     * Checks all groups, if they contain a leaderboard with the <code>removedLeaderboardName</code> and removes it from the group.
     * @param removedLeaderboardName
     */
    private void syncGroupsAfterLeaderboardRemove(String removedLeaderboardName, boolean doDatabaseUpdate) {
        boolean groupNeedsUpdate = false;
        synchronized (leaderboardGroupsByName) {
            for (LeaderboardGroup leaderboardGroup : leaderboardGroupsByName.values()) {
                for (Leaderboard leaderboard : leaderboardGroup.getLeaderboards()) {
                    if (leaderboard.getName().equals(removedLeaderboardName)) {
                        leaderboardGroup.removeLeaderboard(leaderboard);
                        groupNeedsUpdate = true;
                        // TODO we assume that the leaderboard names are unique, so we can break the inner loop here
                        break;
                    }
                }
                
                if (doDatabaseUpdate && groupNeedsUpdate) {
                    mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);
                }
                groupNeedsUpdate = false;
            }
        }
    }
    
    @Override
    public Leaderboard getLeaderboardByName(String name) {
        synchronized (leaderboardsByName) {
            return leaderboardsByName.get(name);
        }
    }
    
    @Override
    public Map<String, Leaderboard> getLeaderboards() {
        synchronized (leaderboardsByName) {
            return Collections.unmodifiableMap(new HashMap<String, Leaderboard>(leaderboardsByName));
        }
    }

    private DomainFactory getDomainFactory() {
        return tractracDomainFactory;
    }
    
    @Override
    public SwissTimingFactory getSwissTimingFactory() {
        return swissTimingFactory;
    }
    
    @Override
    public synchronized Iterable<Event> getAllEvents() {
        return Collections.unmodifiableCollection(new ArrayList<Event>(eventsByName.values()));
    }
    
    @Override
    public boolean isRaceBeingTracked(RaceDefinition r) {
        for (Set<RaceTracker> trackers : raceTrackersByEvent.values()) {
            for (RaceTracker tracker : trackers) {
                if (tracker.getRaces() != null && tracker.getRaces().contains(r)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public Event getEventByName(String name) {
        return eventsByName.get(name);
    }

    @Override
    public synchronized Event addEvent(URL jsonURL, URI liveURI, URI storedURI, WindStore windStore, long timeoutInMilliseconds) throws Exception {
        JSONService jsonService = getDomainFactory().parseJSONURL(jsonURL);
        Event event = null;
        for (RaceRecord rr : jsonService.getRaceRecords()) {
            URL paramURL = rr.getParamURL();
            event = addTracTracRace(paramURL, liveURI, storedURI, windStore, timeoutInMilliseconds).getEvent();
        }
        return event;
    }

    @Override
    public Event createEvent(String eventName, String boatClassName, boolean boatClassTypicallyStartsUpwind) {
        Event event = new EventImpl(eventName, com.sap.sailing.domain.base.DomainFactory.INSTANCE.getOrCreateBoatClass(
                boatClassName, boatClassTypicallyStartsUpwind));
        eventsByName.put(event.getName(), event);
        event.addEventListener(this);
        return event;
    }

    @Override
    public Pair<String, List<RaceRecord>> getTracTracRaceRecords(URL jsonURL) throws IOException, ParseException, org.json.simple.parser.ParseException, URISyntaxException {
        JSONService jsonService = getDomainFactory().parseJSONURL(jsonURL);
        return new Pair<String, List<RaceRecord>>(jsonService.getEventName(), jsonService.getRaceRecords());
    }
    
    @Override
    public List<com.sap.sailing.domain.swisstimingadapter.RaceRecord> getSwissTimingRaceRecords(String hostname,
            int port, boolean canSendRequests) throws InterruptedException, UnknownHostException, IOException, ParseException {
        List<com.sap.sailing.domain.swisstimingadapter.RaceRecord> result = new ArrayList<com.sap.sailing.domain.swisstimingadapter.RaceRecord>();
//        SailMasterConnector swissTimingConnector = swissTimingFactory.getOrCreateSailMasterConnector(hostname, port, swissTimingAdapterPersistence,
//                canSendRequests);
        //TODO remove after testing and enable the upper code instead
        SailMasterConnector swissTimingConnector = swissTimingFactory.getOrCreateSailMasterLiveSimulatorConnector(hostname, port, swissTimingAdapterPersistence,
                canSendRequests);
        //
        for (Race race : swissTimingConnector.getRaces()) {
            TimePoint startTime = swissTimingConnector.getStartTime(race.getRaceID());
            result.add(new com.sap.sailing.domain.swisstimingadapter.RaceRecord(race.getRaceID(), race.getDescription(),
                    startTime==null?null:startTime.asDate()));
        }
        return result;
    }

    @Override
    public synchronized RacesHandle addSwissTimingRace(String raceID, String hostname, int port,
            boolean canSendRequests, WindStore windStore, long timeoutInMilliseconds) throws Exception {
        return addRace(
                swissTimingDomainFactory.createTrackingConnectivityParameters(hostname, port, raceID, canSendRequests,
                        swissTimingFactory, swissTimingDomainFactory, windStore, swissTimingAdapterPersistence),
                windStore, timeoutInMilliseconds);
    }

    @Override
    public synchronized RacesHandle addTracTracRace(URL paramURL, URI liveURI, URI storedURI, WindStore windStore,
            long timeoutInMilliseconds) throws Exception {
        return addRace(getDomainFactory().createTrackingConnectivityParameters(paramURL, liveURI, storedURI, /* startOfTracking */ null,
                /* endOfTracking */ null, windStore), windStore, timeoutInMilliseconds);
    }
    
    @Override
    public void addRace(EventIdentifier addToEvent, RaceDefinition raceDefinition) {
        Event event = getEvent(addToEvent);
        event.addRace(raceDefinition); // will trigger the raceAdded operation because this service is listening on all its events
    }
    
    @Override
    public void raceAdded(Event event, RaceDefinition raceDefinition) {
        final CourseChangeReplicator listener = new CourseChangeReplicator(this, event, raceDefinition);
        courseListeners.put(raceDefinition, listener);
        raceDefinition.getCourse().addCourseListener(listener);
    }

    @Override
    public void raceRemoved(Event event, RaceDefinition raceDefinition) {
        raceDefinition.getCourse().removeCourseListener(courseListeners.remove(raceDefinition));
    }

    @Override
    public synchronized RacesHandle addRace(RaceTrackingConnectivityParameters params, WindStore windStore,
            long timeoutInMilliseconds) throws Exception {
        RaceTracker tracker = raceTrackersByID.get(params.getTrackerID());
        if (tracker == null) {
            tracker = params.createRaceTracker(this);
            raceTrackersByID.put(params.getTrackerID(), tracker);
            Set<RaceTracker> trackers = raceTrackersByEvent.get(tracker.getEvent());
            if (trackers == null) {
                trackers = new HashSet<RaceTracker>();
                raceTrackersByEvent.put(tracker.getEvent(), trackers);
            }
            trackers.add(tracker);
            // TODO we assume here that the event name is unique which necessesitates adding the boat class name to it in EventImpl constructor
            String eventName = tracker.getEvent().getName();
            Event eventWithName = eventsByName.get(eventName);
            // TODO we assume here that the event name is unique which necessesitates adding the boat class name to it in EventImpl constructor
            if (eventWithName != null) {
                if (eventWithName != tracker.getEvent()) {
                    if (Util.isEmpty(eventWithName.getAllRaces())) {
                        // probably, tracker removed the last races from the old event and created a new one
                        eventsByName.put(eventName, tracker.getEvent());
                        tracker.getEvent().addEventListener(this);
                    } else {
                        throw new RuntimeException("Internal error. Two Event objects with equal name "+eventName);
                    }
                }
            } else {
                eventsByName.put(eventName, tracker.getEvent());
                tracker.getEvent().addEventListener(this);
            }
        } else {
            WindStore existingTrackersWindStore = tracker.getWindStore();
            if (!existingTrackersWindStore.equals(windStore)) {
                logger.warning("Wind store mismatch. Requested wind store: "+windStore+
                        ". Wind store in use by existing tracker: "+existingTrackersWindStore);
            }
        }
        if (timeoutInMilliseconds != -1) {
            scheduleAbortTrackerAfterInitialTimeout(tracker, timeoutInMilliseconds);
        }
        return tracker.getRacesHandle();
    }
    
    @Override
    public synchronized TrackedRace createTrackedRace(EventAndRaceIdentifier raceIdentifier, WindStore windStore,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed) {
        DynamicTrackedEvent trackedEvent = getTrackedEvent(getEvent(raceIdentifier));
        RaceDefinition race = getRace(raceIdentifier);
        return trackedEvent.createTrackedRace(race, windStore, millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed,
                /* raceDefinitionSetToUpdate */ null);
    }
    
    @Override
    public synchronized RacesHandle addTracTracRace(URL paramURL, URI liveURI, URI storedURI,
            TimePoint startOfTracking, TimePoint endOfTracking, WindStore windStore,
            long timeoutInMilliseconds) throws Exception {
        return addRace(getDomainFactory().createTrackingConnectivityParameters(paramURL, liveURI, storedURI, startOfTracking,
                endOfTracking, windStore), windStore, timeoutInMilliseconds);
    }

    private void ensureEventIsObservedForDefaultLeaderboardAndAutoLeaderboardLinking(DynamicTrackedEvent trackedEvent) {
        synchronized (eventsObservedForDefaultLeaderboard) {
            if (!eventsObservedForDefaultLeaderboard.contains(trackedEvent)) {
                trackedEvent.addRaceListener(new RaceAdditionListener());
                eventsObservedForDefaultLeaderboard.add(trackedEvent);
            }
        }
    }
    
    /**
     * A listener class used to ensure that when a tracked race is added to any {@link TrackedEvent} managed by this
     * service, the service adds the tracked race to the default leaderboard and links it to the leaderboard columns
     * that were previously connected to it. Additionally, a {@link RaceChangeListener} is added to the {@link TrackedRace}
     * which is responsible for triggering the replication of all relevant changes to the tracked race. When a tracked
     * race is removed, the {@link TrackedRaceReplicator} that was added as listener to that tracked race is removed again.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class RaceAdditionListener implements RaceListener, Serializable {
        private static final long serialVersionUID = 1036955460477000265L;
        
        private final Map<TrackedRace, TrackedRaceReplicator> trackedRaceReplicators;

        public RaceAdditionListener() {
            this.trackedRaceReplicators = new HashMap<TrackedRace, TrackedRaceReplicator>();
        }

        @Override
        public void raceRemoved(TrackedRace trackedRace) {
            TrackedRaceReplicator trackedRaceReplicator = trackedRaceReplicators.remove(trackedRace);
            if (trackedRaceReplicator != null) {
                trackedRace.removeListener(trackedRaceReplicator);
            }
        }

        @Override
        public void raceAdded(TrackedRace trackedRace) {
            linkRaceToConfiguredLeaderboardColumns(trackedRace);
            leaderboardsByName.get(DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME).addRace(trackedRace,
                    trackedRace.getRace().getName(), /* medalRace */false);
            TrackedRaceReplicator trackedRaceReplicator = new TrackedRaceReplicator(trackedRace.getRaceIdentifier());
            trackedRaceReplicators.put(trackedRace, trackedRaceReplicator);
            trackedRace.addListener(trackedRaceReplicator);
        }
    }
    
    private class TrackedRaceReplicator implements RaceChangeListener {
        private final EventAndRaceIdentifier raceIdentifier;

        public TrackedRaceReplicator(EventAndRaceIdentifier raceIdentifier) {
            this.raceIdentifier = raceIdentifier;
        }

        @Override
        public void windDataReceived(Wind wind, WindSource windSource) {
            replicate(new RecordWindFix(raceIdentifier, windSource, wind));
            
        }

        @Override
        public void windDataRemoved(Wind wind, WindSource windSource) {
            replicate(new RemoveWindFix(raceIdentifier, windSource, wind));
        }

        @Override
        public void windAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
            replicate(new UpdateWindAveragingTime(raceIdentifier, newMillisecondsOverWhichToAverage));
        }

        @Override
        public void competitorPositionChanged(GPSFixMoving fix, Competitor competitor) {
            replicate(new RecordCompetitorGPSFix(raceIdentifier, competitor, fix));
            
        }

        @Override
        public void buoyPositionChanged(GPSFix fix, Buoy buoy) {
            replicate(new RecordBuoyGPSFix(raceIdentifier, buoy, fix));
        }

        @Override
        public void markPassingReceived(MarkPassing oldMarkPassing, MarkPassing markPassing) {
            replicate(new UpdateMarkPassings(raceIdentifier, markPassing.getCompetitor(), Collections.singleton(markPassing)));
        }

        @Override
        public void speedAveragingChanged(long oldMillisecondsOverWhichToAverage, long newMillisecondsOverWhichToAverage) {
            replicate(new UpdateWindAveragingTime(raceIdentifier, newMillisecondsOverWhichToAverage));
        }
    }

    /**
     * Based on the <code>trackedRace</code>'s {@link TrackedRace#getRaceIdentifier() race identifier}, the tracked race
     * is (re-)associated to all {@link RaceInLeaderboard race columns} that currently have no
     * {@link RaceInLeaderboard#getTrackedRace() tracked race assigned} and whose
     * {@link RaceInLeaderboard#getRaceIdentifier() race identifier} equals that of <code>trackedRace</code>.
     */
    private void linkRaceToConfiguredLeaderboardColumns(TrackedRace trackedRace) {
        boolean leaderboardHasChanged = false;
        RaceIdentifier trackedRaceIdentifier = trackedRace.getRaceIdentifier();
        for (Leaderboard leaderboard : getLeaderboards().values()) {
            for (RaceInLeaderboard column : leaderboard.getRaceColumns()) {
                if (trackedRaceIdentifier.equals(column.getRaceIdentifier()) && column.getTrackedRace() == null) {
                    column.setTrackedRace(trackedRace);
                    leaderboardHasChanged = true;
                }
            }
            if (leaderboardHasChanged) {
                //Update the corresponding groups, to keep them in sync
                syncGroupsAfterLeaderboardChange(leaderboard, /*doDatabaseUpdate*/ false);
            }
        }
    }

    @Override
    public synchronized void stopTracking(Event event) throws MalformedURLException, IOException, InterruptedException {
        if (raceTrackersByEvent.containsKey(event)) {
            for (RaceTracker raceTracker : raceTrackersByEvent.get(event)) {
                for (RaceDefinition race : raceTracker.getRaces()) {
                    stopTrackingWind(event, race);
                }
                raceTracker.stop(); // this also removes the TrackedRace from trackedEvent
                raceTrackersByID.remove(raceTracker.getID());
            }
            raceTrackersByEvent.remove(event);
        }
    }
    
    @Override
    public synchronized void stopTrackingAndRemove(Event event) throws MalformedURLException, IOException, InterruptedException {
        stopTracking(event);
        if (event != null) {
            if (event.getName() != null) {
                eventsByName.remove(event.getName());
                event.removeEventListener(this);
            }
            for (RaceDefinition race : event.getAllRaces()) {
                stopTrackingWind(event, race);
                // remove from default leaderboard
                Leaderboard defaultLeaderboard = getLeaderboardByName(DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME);
                defaultLeaderboard.removeRaceColumn(race.getName());
            }
        }
    }

    /**
     * The tracker will initially try to connect to the TracTrac infrastructure to obtain basic race master data. If
     * this fails after some timeout, to avoid garbage and lingering threads, the task scheduled by this method will
     * check after the timeout expires if race master data was successfully received. If so, the tracker continues
     * normally. Otherwise, the tracker is shut down orderly by {@link Receiver#stopPreemptively() stopping} all
     * receivers and {@link DataController#stop(boolean) stopping} the TracTrac controller for this tracker.
     * 
     * @return the scheduled task, in case the caller wants to {@link ScheduledFuture#cancel(boolean) cancel} it, e.g.,
     *         when the tracker is stopped or has successfully received the race
     */
    private ScheduledFuture<?> scheduleAbortTrackerAfterInitialTimeout(final RaceTracker tracker, final long timeoutInMilliseconds) {
        ScheduledFuture<?> task = getScheduler().schedule(new Runnable() {
            @Override public void run() {
                if (tracker.getRaces() == null || tracker.getRaces().isEmpty()) {
                    try {
                        Event event = tracker.getEvent();
                        logger.log(Level.SEVERE, "RaceDefinition for a race in event "+event.getName()+" not obtained within "+
                                timeoutInMilliseconds+"ms. Aborting tracker for this race.");
                        Set<RaceTracker> trackersForEvent = raceTrackersByEvent.get(event);
                        if (trackersForEvent != null) {
                            trackersForEvent.remove(tracker);
                        }
                        tracker.stop();
                        raceTrackersByID.remove(tracker.getID());
                        if (trackersForEvent == null || trackersForEvent.isEmpty()) {
                            stopTracking(event);
                        }
                    } catch (Exception e) {
                        logger.throwing(RacingEventServiceImpl.class.getName(), "scheduleAbortTrackerAfterInitialTimeout", e);
                        e.printStackTrace();
                    }
                }
            }
        }, /* delay */ timeoutInMilliseconds, /* unit */ TimeUnit.MILLISECONDS);
        return task;
    }

    @Override
    public synchronized void stopTracking(Event event, RaceDefinition race) throws MalformedURLException, IOException, InterruptedException {
        logger.info("Stopping tracking for "+race+"...");
        if (raceTrackersByEvent.containsKey(event)) {
            Iterator<RaceTracker> trackerIter = raceTrackersByEvent.get(event).iterator();
            while (trackerIter.hasNext()) {
                RaceTracker raceTracker = trackerIter.next();
                if (raceTracker.getRaces() != null && raceTracker.getRaces().contains(race)) {
                    logger.info("Found tracker to stop for races "+raceTracker.getRaces());
                    raceTracker.stop(); // this also removes the TrackedRace from trackedEvent
                    // do not remove the tracker from raceTrackersByEvent, because it should still exist there, but with the state "non-tracked"
                    trackerIter.remove();
                    raceTrackersByID.remove(raceTracker.getID());
                }
            }
        } else {
            logger.warning("Didn't find any trackers for event "+event);
        }
        stopTrackingWind(event, race);
        // if the last tracked race was removed, remove the entire event
        if (raceTrackersByEvent.get(event).isEmpty()) {
            stopTracking(event);
        }
    }

    @Override
    public synchronized void removeEvent(Event event) throws MalformedURLException, IOException, InterruptedException {
        for (RaceDefinition race : event.getAllRaces()) {
            removeRace(event, race);
        }
    }
    
    @Override
    public synchronized void removeRace(Event event, RaceDefinition race) throws MalformedURLException,
            IOException, InterruptedException {
        logger.info("Removing the race + " + race + "...");
        stopAllTrackersForWhichRaceIsLastReachable(event, race);
        stopTrackingWind(event, race);
        TrackedRace trackedRace = getExistingTrackedRace(event, race);
        if (trackedRace != null) {
            TrackedEvent trackedEvent = getTrackedEvent(event);
            if (trackedEvent != null) {
                trackedEvent.removeTrackedRace(trackedRace);
            }
            if (Util.isEmpty(trackedEvent.getTrackedRaces())) {
                removeTrackedEvent(event);
            }
            for (Leaderboard leaderboard : getLeaderboards().values()) {
                boolean changed = false;
                for (RaceInLeaderboard raceColumn : leaderboard.getRaceColumns()) {
                    if (raceColumn.getTrackedRace() == trackedRace) {
                        raceColumn.setTrackedRace(null); // but leave the RaceIdentifier on the race column untouched, e.g., for later re-load
                        changed = true;
                    }
                }
                if (changed) {
                    updateStoredLeaderboard(leaderboard);
                }
            }
        }
        // remove the race from the event
        event.removeRace(race);
        if (Util.isEmpty(event.getAllRaces())) {
            eventsByName.remove(event.getName());
            event.removeEventListener(this);
        }
    }

    /**
     * Doesn't stop any wind trackers
     */
    private void stopAllTrackersForWhichRaceIsLastReachable(Event event, RaceDefinition race)
            throws MalformedURLException, IOException, InterruptedException {
        if (raceTrackersByEvent.containsKey(event)) {
            Iterator<RaceTracker> trackerIter = raceTrackersByEvent.get(event).iterator();
            while (trackerIter.hasNext()) {
                RaceTracker raceTracker = trackerIter.next();
                if (raceTracker.getRaces() != null && raceTracker.getRaces().contains(race)) {
                    boolean foundReachableRace = false;
                    for (RaceDefinition raceTrackedByTracker : raceTracker.getRaces()) {
                        if (raceTrackedByTracker != race && isReachable(event, raceTrackedByTracker)) {
                            foundReachableRace = true;
                            break;
                        }
                    }
                    if (!foundReachableRace) {
                        // firstly stop the tracker
                        raceTracker.stop();
                        // remove it from the raceTrackers by Event
                        trackerIter.remove();
                        raceTrackersByID.remove(raceTracker.getID());
                        // if the last tracked race was removed, remove the entire event
                        if (raceTrackersByEvent.get(event).isEmpty()) {
                            stopTracking(event);
                        }
                    }
                }
            }
        }
    }

    private boolean isReachable(Event event, RaceDefinition race) {
        return Util.contains(event.getAllRaces(), race);
    }

    @Override
    public void startTrackingWind(Event event, RaceDefinition race,
            boolean correctByDeclination) throws SocketException {
        windTrackerFactory.createWindTracker(getOrCreateTrackedEvent(event), race, correctByDeclination);
    }

    @Override
    public synchronized void stopTrackingWind(Event event, RaceDefinition race) throws SocketException, IOException {
        WindTracker windTracker = windTrackerFactory.getExistingWindTracker(race);
        if (windTracker != null) {
            windTracker.stop();
        }
    }

    @Override
    public synchronized Iterable<Triple<Event, RaceDefinition, String>> getWindTrackedRaces() {
        List<Triple<Event, RaceDefinition, String>> result = new ArrayList<Triple<Event, RaceDefinition, String>>();
        for (Event event : getAllEvents()) {
            for (RaceDefinition race : event.getAllRaces()) {
                WindTracker windTracker = windTrackerFactory.getExistingWindTracker(race);
                if (windTracker != null) {
                    result.add(new Triple<Event, RaceDefinition, String>(event, race, windTracker.toString()));
                }
            }
        }
        return result;
    }

    @Override
    public TrackedRace getTrackedRace(Event e, RaceDefinition r) {
        return getOrCreateTrackedEvent(e).getTrackedRace(r);
    }
    
    private TrackedRace getExistingTrackedRace(Event e, RaceDefinition r) {
        return getOrCreateTrackedEvent(e).getExistingTrackedRace(r);
    }
    
    @Override
    public DynamicTrackedEvent getOrCreateTrackedEvent(Event event) {
        synchronized (eventTrackingCache) {
            DynamicTrackedEvent result = eventTrackingCache.get(event);
            if (result == null) {
                result = new DynamicTrackedEventImpl(event);
                eventTrackingCache.put(event, result);
                ensureEventIsObservedForDefaultLeaderboardAndAutoLeaderboardLinking(result);
            }
            return result;
        }
    }

    @Override
    public DynamicTrackedEvent getTrackedEvent(com.sap.sailing.domain.base.Event event) {
        return eventTrackingCache.get(event);
    }

    @Override
    public void removeTrackedEvent(Event event) {
        eventTrackingCache.remove(event);
    }

    @Override
    public void storeSwissTimingDummyRace(String racMessage, String stlMessage, String ccgMessage){
        SailMasterMessage racSMMessage = swissTimingFactory.createMessage(racMessage, null);
        SailMasterMessage stlSMMessage = swissTimingFactory.createMessage(stlMessage, null);
        SailMasterMessage ccgSMMessage = swissTimingFactory.createMessage(ccgMessage, null);
        if (swissTimingAdapterPersistence.getRace(stlSMMessage.getRaceID()) != null) {
            throw new IllegalArgumentException("Race with raceID \"" + stlSMMessage.getRaceID() + "\" already exists.");
        }
        else {
            swissTimingAdapterPersistence.storeSailMasterMessage(racSMMessage);
            swissTimingAdapterPersistence.storeSailMasterMessage(stlSMMessage);
            swissTimingAdapterPersistence.storeSailMasterMessage(ccgSMMessage);
        }
    }

    @Override
    public Event getEvent(EventName eventName) {
        return (Event) eventsByName.get(eventName.getEventName());
    }

    @Override
    public Event getEvent(EventIdentifier eventIdentifier) {
        return (Event) eventIdentifier.getEvent(this);
    }
    
    @Override
    public TrackedRace getTrackedRace(RaceIdentifier raceIdentifier) {
        return (TrackedRace) raceIdentifier.getTrackedRace(this);
    }

    @Override
    public TrackedRace getExistingTrackedRace(RaceIdentifier raceIdentifier) {
        Event event = getEventByName(raceIdentifier.getEventName());
        TrackedRace trackedRace = null;
        if (event != null) {
            RaceDefinition race = event.getRaceByName(raceIdentifier.getRaceName());
            trackedRace = getOrCreateTrackedEvent(event).getExistingTrackedRace(race);
        }
        return trackedRace;
    }

    @Override
    public RaceDefinition getRace(EventAndRaceIdentifier eventNameAndRaceName) {
        RaceDefinition result = null;
        Event event = getEvent(eventNameAndRaceName);
        if (event != null) {
            result = event.getRaceByName(eventNameAndRaceName.getRaceName());
        }
        return result;
    }

    @Override
    public Map<String, LeaderboardGroup> getLeaderboardGroups() {
        synchronized (leaderboardGroupsByName) {
            return Collections.unmodifiableMap(new HashMap<String, LeaderboardGroup>(leaderboardGroupsByName));
        }
    }

    @Override
    public LeaderboardGroup getLeaderboardGroupByName(String groupName) {
        synchronized (leaderboardGroupsByName) {
            return leaderboardGroupsByName.get(groupName);
        }
    }

    @Override
    public LeaderboardGroup addLeaderboardGroup(String groupName, String description, List<String> leaderboardNames) {
        ArrayList<Leaderboard> leaderboards = new ArrayList<>();
        synchronized (leaderboardsByName) {
            for (String leaderboardName : leaderboardNames) {
                Leaderboard leaderboard = leaderboardsByName.get(leaderboardName);
                if (leaderboard == null) {
                    throw new IllegalArgumentException("No leaderboard with name " + leaderboardName + " found");
                } else {
                    leaderboards.add(leaderboard);
                }
            }
        }
        LeaderboardGroup result = new LeaderboardGroupImpl(groupName, description, leaderboards);
        synchronized (leaderboardGroupsByName) {
            if (leaderboardGroupsByName.containsKey(groupName)) {
                throw new IllegalArgumentException("Leaderboard group with name " + groupName + " already exists");
            }
            leaderboardGroupsByName.put(groupName, result);
        }
        mongoObjectFactory.storeLeaderboardGroup(result);
        return result;
    }

    @Override
    public void removeLeaderboardGroup(String groupName) {
        synchronized (leaderboardGroupsByName) {
            leaderboardGroupsByName.remove(groupName);
        }
        mongoObjectFactory.removeLeaderboardGroup(groupName);
    }

    @Override
    public void renameLeaderboardGroup(String oldName, String newName) {
        synchronized (leaderboardGroupsByName) {
            if (!leaderboardGroupsByName.containsKey(oldName)) {
                throw new IllegalArgumentException("No leaderboard group with name " + oldName + " found");
            }
            if (leaderboardGroupsByName.containsKey(newName)) {
                throw new IllegalArgumentException("Leaderboard group with name " + newName + " already exists");
            }
            LeaderboardGroup toRename = leaderboardGroupsByName.remove(oldName);
            toRename.setName(newName);
            leaderboardGroupsByName.put(newName, toRename);
            mongoObjectFactory.renameLeaderboardGroup(oldName, newName);
        }
    }

    @Override
    public void updateLeaderboardGroup(String oldName, String newName, String description, List<String> leaderboardNames) {
        if (!oldName.equals(newName)) {
            renameLeaderboardGroup(oldName, newName);
        }
        LeaderboardGroup group = getLeaderboardGroupByName(newName);
        if (!description.equals(group.getDescription())) {
            group.setDescriptiom(description);
        }
        group.clearLeaderboards();
        for (String leaderboardName : leaderboardNames) {
            Leaderboard leaderboard = getLeaderboardByName(leaderboardName);
            if (leaderboard != null) {
                group.addLeaderboard(leaderboard);
            }
        }
        mongoObjectFactory.storeLeaderboardGroup(group);
    }

    @Override
    public void updateStoredLeaderboardGroup(LeaderboardGroup leaderboardGroup) {
        mongoObjectFactory.storeLeaderboardGroup(leaderboardGroup);
    }
    
    @Override
    public void addExpeditionListener(ExpeditionListener listener, boolean validMessagesOnly) throws SocketException {
        UDPExpeditionReceiver receiver = windTrackerFactory.getOrCreateWindReceiverOnDefaultPort();
        receiver.addListener(listener, validMessagesOnly);
    }

    @Override
    public void removeExpeditionListener(ExpeditionListener listener) {
        UDPExpeditionReceiver receiver;
        try {
            receiver = windTrackerFactory.getOrCreateWindReceiverOnDefaultPort();
            receiver.removeListener(listener);
        } catch (SocketException e) {
            logger.info("Failed to remove expedition listener "+listener+
                    "; exception while trying to retrieve wind receiver: "+e.getMessage());
        }
    }

    private ScheduledExecutorService getScheduler() {
        return scheduler;
    }

    /**
     * Currently, the operation is executed by immediately {@link Operation#internalApplyTo(Object) applying} it to this
     * service object.<p>
     * 
     * Future implementations of this method will need to also replicate the effects of the operation to all replica
     * of this service known.
     */
    @Override
    public <T> T apply(RacingEventServiceOperation<T> operation) {
        try {
            T result = operation.internalApplyTo(this);
            replicate(operation);
            return result;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    <T> void replicate(RacingEventServiceOperation<T> operation) {
        for (OperationExecutionListener listener : operationExecutionListeners) {
            listener.executed(operation);
        }
    }

    @Override
    public void addOperationExecutionListener(OperationExecutionListener listener) {
        operationExecutionListeners.add(listener);
    }

    @Override
    public void removeOperationExecutionListener(OperationExecutionListener listener) {
        operationExecutionListeners.remove(listener);
    }

    @Override
    public void serializeForInitialReplication(ObjectOutputStream oos) throws IOException {
        oos.writeObject(eventsByName);
        oos.writeObject(eventsObservedForDefaultLeaderboard);
        oos.writeObject(eventTrackingCache);
        oos.writeObject(leaderboardGroupsByName);
        oos.writeObject(leaderboardsByName);
    }

    @SuppressWarnings("unchecked") // the type-parameters in the casts of the de-serialized collection objects can't be checked
    @Override
    public synchronized void initiallyFillFrom(ObjectInputStream ois) throws IOException, ClassNotFoundException {
        eventsByName.clear();
        eventsByName.putAll((Map<String, Event>) ois.readObject());
        eventsObservedForDefaultLeaderboard.clear();
        for (DynamicTrackedEvent trackedEventToObserve : (Set<DynamicTrackedEvent>) ois.readObject()) {
            ensureEventIsObservedForDefaultLeaderboardAndAutoLeaderboardLinking(trackedEventToObserve);
        }
        eventTrackingCache.clear();
        eventTrackingCache.putAll((Map<Event, DynamicTrackedEvent>) ois.readObject());
        leaderboardGroupsByName.clear();
        leaderboardGroupsByName.putAll((Map<String, LeaderboardGroup>) ois.readObject());
        leaderboardsByName.clear();
        leaderboardsByName.putAll((Map<String, Leaderboard>) ois.readObject());
    }

}
