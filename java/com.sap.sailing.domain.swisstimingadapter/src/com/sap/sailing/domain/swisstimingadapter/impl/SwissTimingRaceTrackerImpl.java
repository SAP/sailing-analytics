package com.sap.sailing.domain.swisstimingadapter.impl;

import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Optional;
import java.util.TreeMap;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorAndBoatStore;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintRegistry;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.shared.tracking.impl.TrackingConnectorInfoImpl;
import com.sap.sailing.domain.swisstimingadapter.Course;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.Fix;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceStatus;
import com.sap.sailing.domain.swisstimingadapter.RacingStatus;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterListener;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingAdapter;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingRaceTracker;
import com.sap.sailing.domain.tracking.AbstractRaceTrackerImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.RaceTrackingHandler.DefaultRaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sailing.domain.tracking.impl.UpdateHandler;
import com.sap.sse.common.Distance;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import difflib.PatchFailedException;

public class SwissTimingRaceTrackerImpl extends AbstractRaceTrackerImpl<SwissTimingTrackingConnectivityParameters>
        implements SwissTimingRaceTracker, SailMasterListener, TrackingDataLoader {

    private static final Logger logger = Logger.getLogger(SwissTimingRaceTrackerImpl.class.getName());
    
    private final SailMasterConnector connector;
    private final String raceID;
    private final String raceName;
    private final String raceDescription;
    private final BoatClass boatClass;
    private final DomainFactory domainFactory;
    private final com.sap.sse.common.Util.Triple<String, String, Integer> id;
    private final Regatta regatta;
    private final WindStore windStore;
    private final boolean startListFromManage2Sail;
    private final boolean useInternalMarkPassingAlgorithm;

    /**
     * Starts out as <code>null</code> and is set when the race definition has been created. When this happens, this object is
     * {@link Object#notifyAll() notified}.
     */
    private volatile RaceDefinition race;
    
    private Course course;
    private StartList startList;
    private Map<String, Competitor> competitorsByBoatId;
    private DynamicTrackedRace trackedRace;
    private final DynamicTrackedRegatta trackedRegatta;

    private boolean loggedIgnore;
    private final long delayToLiveInMillis;
    
    /**
     * For TMD messages received when there was no start time set, this message queue stores those TMD messages. When a start
     * time is received, it will be sent to the queue which in turn will re-apply all not yet applied TMD messages again to this
     * tracker by calling {@link #receivedTimingData(String, String, List)}.
     */
    private final TMDMessageQueue tmdMessageQueue;

    private final RaceLogAndTrackedRaceResolver raceLogResolver;
    
    private final MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry;

    /**
     * If set to a non-{@code null}, non-{@link String#isEmpty() empty} value, updates about start time changes, course
     * changes, etc. will be sent as REST requests to this URL, using the {@link #updateUsername} /
     * {@link #updatePassword} as credentials.
     */
    private final String updateURL;

    private final String updateUsername;

    private final String updatePassword;

    private final RaceTrackingHandler raceTrackingHandler;

    private final TrackedRegattaRegistry trackedRegattaRegistry;
    
    protected SwissTimingRaceTrackerImpl(RaceLogStore raceLogStore, RegattaLogStore regattaLogStore,
            WindStore windStore, DomainFactory domainFactory, SwissTimingFactory factory,
            TrackedRegattaRegistry trackedRegattaRegistry, RaceLogAndTrackedRaceResolver raceLogResolver,
            SwissTimingTrackingConnectivityParameters connectivityParams, RaceTrackingHandler raceTrackingHandler,
            MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry)
            throws InterruptedException, UnknownHostException, IOException, ParseException, URISyntaxException {
        this(/* regatta */ null, windStore, domainFactory, factory, trackedRegattaRegistry, raceLogStore,
                regattaLogStore, raceLogResolver, connectivityParams, raceTrackingHandler, markPassingRaceFingerprintRegistry);
    }

    protected SwissTimingRaceTrackerImpl(Regatta regatta, WindStore windStore, DomainFactory domainFactory,
            SwissTimingFactory factory, TrackedRegattaRegistry trackedRegattaRegistry, RaceLogStore raceLogStore,
            RegattaLogStore regattaLogStore, RaceLogAndTrackedRaceResolver raceLogResolver,
            SwissTimingTrackingConnectivityParameters connectivityParams, RaceTrackingHandler raceTrackingHandler, MarkPassingRaceFingerprintRegistry markPassingRaceFingerprintRegistry)
            throws InterruptedException, UnknownHostException, IOException, ParseException, URISyntaxException {
        super(connectivityParams);
        this.raceLogResolver = raceLogResolver;
        this.raceTrackingHandler = raceTrackingHandler;
        this.tmdMessageQueue = new TMDMessageQueue(this);
        this.trackedRegattaRegistry = trackedRegattaRegistry;
        this.markPassingRaceFingerprintRegistry = markPassingRaceFingerprintRegistry;
        final Regatta effectiveRegatta;
        // Try to find a pre-associated event based on the Race ID
        if (regatta == null) {
            effectiveRegatta = trackedRegattaRegistry.getRememberedRegattaForRace(connectivityParams.getRaceID());
        } else {
            effectiveRegatta = regatta;
        }
        // if regatta is still null, no previous assignment of any of the races in this TracTrac event to a Regatta was
        // found; in this case, create a default regatta based on the TracTrac event data
        this.regatta = effectiveRegatta == null ? domainFactory.getOrCreateDefaultRegatta(raceLogStore, regattaLogStore,
                connectivityParams.getRaceID(), connectivityParams.getBoatClass(), trackedRegattaRegistry) : effectiveRegatta;
                this.domainFactory = domainFactory;
        this.raceID = connectivityParams.getRaceID();
        // start out with an empty course, so we don't depend on receiving the CCG message before the timeout
        this.course = new CourseImpl(this.raceID, /* start with empty marks list */ Collections.emptyList());
        this.raceName = connectivityParams.getRaceName();
        this.startList = connectivityParams.getStartList();
        this.startListFromManage2Sail = connectivityParams.getStartList() != null;
        this.raceDescription = connectivityParams.getRaceDescription();
        this.boatClass = connectivityParams.getBoatClass();
        this.windStore = windStore;
        this.id = createID(connectivityParams.getRaceID(), connectivityParams.getHostname(), connectivityParams.getPort());
        trackedRegatta = trackedRegattaRegistry.getOrCreateTrackedRegatta(this.regatta);
        this.delayToLiveInMillis = connectivityParams.getDelayToLiveInMillis();
        this.competitorsByBoatId = new HashMap<String, Competitor>();
        this.useInternalMarkPassingAlgorithm = connectivityParams.isUseInternalMarkPassingAlgorithm();
        this.updateURL = connectivityParams.getUpdateURL();
        this.updateUsername = connectivityParams.getUpdateUsername();
        this.updatePassword = connectivityParams.getUpdatePassword();
        if (connectivityParams.getStartList() != null) {
            createRaceDefinition(course);
        }
        // Ensure this is called last, otherwise the connector starts running without having a complete racetracker
        this.connector = factory.getOrCreateSailMasterConnector(connectivityParams.getHostname(),
                connectivityParams.getPort(), connectivityParams.getRaceID(), getRaceDataUrl(connectivityParams),
                connectivityParams.getRaceName(), connectivityParams.getRaceDescription(), connectivityParams.getBoatClass(), this);
    }

    /**
     * If a valid hostname/port combination is set in the connectivity parameters, {@code null} is returned. Otherwise,
     * a {@link URL} is constructed from the {@link SwissTimingTrackingConnectivityParameters#getManage2SailEventUrl()}
     * by stripping all URL parameters and replacing the last path element which is expected to be a file name for the
     * {@code .json} file by the {race-id}.log combination, expecting the race data to reside in the same folder as
     * the JSON file describing the Manage2Sail event.
     */
    private URL getRaceDataUrl(SwissTimingTrackingConnectivityParameters connectivityParams) throws MalformedURLException {
        final URL result;
        if (connectivityParams.getHostname() == null) {
            final java.net.URL manage2SailEventURL = new URL(connectivityParams.getManage2SailEventUrl());
            final String path = manage2SailEventURL.getPath();
            final String logFile = path.substring(0, path.lastIndexOf('/')+1) + connectivityParams.getRaceID()+".log";
            if (manage2SailEventURL.getPort() != -1) {
                result = new java.net.URL(manage2SailEventURL.getProtocol(), manage2SailEventURL.getHost(), manage2SailEventURL.getPort(), logFile);
            } else {
                result = new java.net.URL(manage2SailEventURL.getProtocol(), manage2SailEventURL.getHost(), logFile);
            }
        } else {
            result = null;
        }
        return result;
    }

    @Override
    public DynamicTrackedRegatta getTrackedRegatta() {
        return trackedRegatta;
    }

    static com.sap.sse.common.Util.Triple<String, String, Integer> createID(String raceID, String hostname, int port) {
        return new com.sap.sse.common.Util.Triple<String, String, Integer>(raceID, hostname, port);
    }

    @Override
    protected void onStop(boolean preemptive, boolean willBeRemoved) throws MalformedURLException, IOException, InterruptedException {
        if (isTrackedRaceStillReachable()) {
            TrackedRaceStatus newStatus = new TrackedRaceStatusImpl(willBeRemoved ? TrackedRaceStatusEnum.REMOVED : TrackedRaceStatusEnum.FINISHED, 1.0);
            trackedRace.onStatusChanged(this, newStatus);
        }
        connector.removeSailMasterListener(this);
    }

    @Override
    public RaceDefinition getRace() {
        return race;
    }

    @Override
    public RaceHandle getRaceHandle() {
        return new RaceHandle() {
            @Override
            public Regatta getRegatta() {
                return SwissTimingRaceTrackerImpl.this.getRegatta();
            }

            @Override
            public RaceDefinition getRace() {
                synchronized (SwissTimingRaceTrackerImpl.this) {
                    while (race == null) {
                        try {
                            SwissTimingRaceTrackerImpl.this.wait();
                        } catch (InterruptedException e) {
                            logger.log(Level.SEVERE, "Interrupted wait", e);
                        }
                    }
                }
                return race;
            }

            @Override
            public RaceDefinition getRace(long timeoutInMilliseconds) {
                long start = System.currentTimeMillis();
                synchronized (SwissTimingRaceTrackerImpl.this) {
                    RaceDefinition preResult = race;
                    boolean interrupted = false;
                    while ((System.currentTimeMillis()-start < timeoutInMilliseconds) && !interrupted && preResult == null) {
                        try {
                            long timeToWait = timeoutInMilliseconds - (System.currentTimeMillis() - start);
                            if (timeToWait > 0) {
                                SwissTimingRaceTrackerImpl.this.wait(timeToWait);
                            }
                            preResult = race;
                        } catch (InterruptedException e) {
                            interrupted = true;
                        }
                    }
                    final RaceDefinition result;
                    if (preResult == null) {
                        result = null;
                    } else {
                        result = preResult;
                    }
                    return result;
                }
            }

            @Override
            public DynamicTrackedRegatta getTrackedRegatta() {
                return SwissTimingRaceTrackerImpl.this.getTrackedRegatta();
            }

            @Override
            public RaceTracker getRaceTracker() {
                return SwissTimingRaceTrackerImpl.this;
            }
        };
    }

    @Override
    public WindStore getWindStore() {
        return windStore;
    }

    @Override
    public Regatta getRegatta() {
        return regatta;
    }

    @Override
    public com.sap.sse.common.Util.Triple<String, String, Integer> getID() {
        return id; 
    }

    @Override
    public void receivedRacePositionData(String raceID, RaceStatus raceStatus, RacingStatus racingStatus, TimePoint timePoint,
            TimePoint startTime, Long millisecondsSinceRaceStart, Integer nextMarkIndexForLeader,
            Distance distanceToNextMarkForLeader, Collection<Fix> fixes) {
        assert this.raceID.equals(raceID);
        if (isTrackedRaceStillReachable()) {
            if (this.raceID.equals(raceID)) {
                if (startTime != null) {
                    trackedRace.setStartTimeReceived(startTime);
                    tmdMessageQueue.validStartTimeReceived();
                }
                for (Fix fix : fixes) {
                    GPSFixMoving gpsFix = domainFactory.createGPSFix(timePoint, fix);
                    switch (fix.getTrackerType()) {
                    case BUOY:
                    case COMMITTEE:
                    case JURY:
                    case TIMINGSCORING:
                    case UNIDENTIFIED:
                        Serializable trackerID = fix.getTrackedObjectId();
                        Mark mark = domainFactory.getOrCreateMark(trackerID, /* description */ null);
                        trackedRace.recordFix(mark, gpsFix);
                        break;
                    case COMPETITOR:
                        Serializable competitorId = fix.getTrackedObjectId();
                    	Competitor competitor = getDomainFactory().getBaseDomainFactory().getExistingCompetitorById(competitorId);
                        if (competitor == null) {
                            // TODO: read startlist again from Manage2Sail
                            // use competitorStore.isCompetitorToUpdateDuringGetOrCreate(result)
                        }
                	if (competitor != null) {
                            DynamicGPSFixTrack<Competitor, GPSFixMoving> competitorTrack = trackedRace.getTrack(competitor);
                            competitorTrack.addGPSFix(gpsFix);
                    	} else {
                            logger.info("Unknown competitor " + competitorId + " found for race with id " + raceID);
                    	}
                        break;
                    default:
                        logger.info("Unknown tracker type " + fix.getTrackerType());
                    }
                }
            }
        } else {
            if (!loggedIgnore) {
                logger.info("Ignoring race position data " + fixes + " for SwissTiming race " + raceID
                        + " because tracked race is no longer reachable. Was the race removed but is still tracked? "+
                        "(Future occurrences of this message will be suppressed)");
                loggedIgnore = true;
            }
        }

    }

    @Override
    public void receivedTimingData(String raceID, String competitorIdAsString,
            List<com.sap.sse.common.Util.Triple<Integer, Integer, Long>> markIndicesRanksAndTimesSinceStartInMilliseconds) {
        assert this.raceID.equals(raceID);
        if (!useInternalMarkPassingAlgorithm) {
            if (isTrackedRaceStillReachable()) {
                final Competitor competitor = domainFactory.getBaseDomainFactory().getExistingCompetitorById(UUID.fromString(competitorIdAsString));
                if (competitor == null) {
                    logger.info("Received timing data for boat ID " + competitorIdAsString + " in race " + raceID
                            + " but couldn't find a competitor with that boat ID in this race. Ignoring.");
                } else {
                    // the list of mark indices and time stamps is partial and usually only shows the last mark passing;
                    // we need to use this to *update* the competitor's mark passings list, not *replace* it
                    TreeMap<Integer, MarkPassing> markPassingsByMarkIndex = new TreeMap<Integer, MarkPassing>();
                    // now fill with the already existing mark passings for the competitor identified by boatID...
                    NavigableSet<MarkPassing> markPassings = trackedRace.getMarkPassings(competitor);
                    trackedRace.lockForRead(markPassings);
                    try {
                        for (MarkPassing markPassing : markPassings) {
                            markPassingsByMarkIndex.put(
                                    trackedRace.getRace().getCourse().getIndexOfWaypoint(markPassing.getWaypoint()),
                                    markPassing);
                        }
                    } finally {
                        trackedRace.unlockAfterRead(markPassings);
                    }
                    // ...and then overwrite those for which we received "new evidence"
                    for (com.sap.sse.common.Util.Triple<Integer, Integer, Long> markIndexRankAndTimeSinceStartInMilliseconds : markIndicesRanksAndTimesSinceStartInMilliseconds) {
                        Waypoint waypoint = Util.get(trackedRace.getRace().getCourse().getWaypoints(),
                                markIndexRankAndTimeSinceStartInMilliseconds.getA());
                        // If the rank and time information is empty, we interpret this by clearing the mark rounding if
                        // any (see also bug 1911):
                        if (markIndexRankAndTimeSinceStartInMilliseconds.getC() == null) {
                            markPassingsByMarkIndex.remove(markIndexRankAndTimeSinceStartInMilliseconds.getA());
                        } else {
                            // update mark passing only if we have a start time; guessed start times don't make sense and
                            // for the start line would lead subsequent calls to getStartOfRace() return that guessed
                            // start time which then cannot be identified as "guessed" anymore...
                            if (trackedRace.getStartOfRace() != null) {
                                final TimePoint startTime = trackedRace.getStartOfRace();
                                MillisecondsTimePoint timePoint = new MillisecondsTimePoint(startTime.asMillis()
                                        + markIndexRankAndTimeSinceStartInMilliseconds.getC());
                                MarkPassing markPassing = domainFactory.createMarkPassing(timePoint, waypoint, competitor);
                                markPassingsByMarkIndex.put(markIndexRankAndTimeSinceStartInMilliseconds.getA(),
                                        markPassing);
                            } else {
                                logger.warning("Received mark passing with time relative to start of race "
                                        + trackedRace.getRace().getName()
                                        + " before having received a race start time."
                                        + " Queueing message for re-application when a start time has been received.");
                                tmdMessageQueue.enqueue(raceID, competitorIdAsString,
                                        markIndicesRanksAndTimesSinceStartInMilliseconds);
                            }
                        }
                    }
                    trackedRace.updateMarkPassings(competitor, markPassingsByMarkIndex.values());
                }
            } else {
                if (!loggedIgnore) {
                    logger.info("Ignoring timing data "
                            + markIndicesRanksAndTimesSinceStartInMilliseconds
                            + " for SwissTiming race "
                            + raceID
                            + " because tracked race is no longer reachable. Was the race removed but is still tracked? "
                            + "(Future occurrences of this message will be suppressed)");
                    loggedIgnore = true;
                }
            }
        }
    }

    @Override
    public void storedDataProgress(String raceID, double progress, TrackedRaceStatusEnum statusAfterLoadingComplete) {
        assert this.raceID.equals(raceID);
        if (isTrackedRaceStillReachable()) {
            final TrackedRaceStatusImpl newStatus;
            if (progress == 0.0) {
                newStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.PREPARED, 0.0);
            } else if (progress == 1.0) {
                newStatus = new TrackedRaceStatusImpl(statusAfterLoadingComplete, progress);
            } else {
                newStatus = new TrackedRaceStatusImpl(TrackedRaceStatusEnum.LOADING, progress);
            }
            trackedRace.onStatusChanged(this, newStatus);
            if (newStatus.getStatus() == TrackedRaceStatusEnum.FINISHED) {
                try {
                    trackedRegattaRegistry.stopTracker(getRegatta(), this);
                } catch (IOException | InterruptedException e) {
                    logger.log(Level.SEVERE, "Error trying to stop the tracker "+this, e);
                }
            }
        }
    }

    @Override
    public void receivedClockAtMark(String raceID,
            List<com.sap.sse.common.Util.Triple<Integer, TimePoint, String>> markIndicesTimePointsAndBoatIDs) {
        // Ignored because it's covered by TMD. Mail from Kai Hahndorf of 2011-11-15T12:42:00Z:
        // "Die TMD werden immer gesendet. Das CAM Protokoll ist nur fuer unsere TV-Grafik wichtig, da damit die Rueckstandsuhr gestartet wird."
    }

    @Override
    public void receivedStartList(String raceID, StartList startList) throws URISyntaxException {
    	// ignore STL messages if the startlist has been already provided by Manage2Sail  
    	if (!startListFromManage2Sail && this.raceID.equals(raceID)) {
            StartList oldStartList = this.startList;
            this.startList = startList;
            if (oldStartList == null && course != null) {
                createRaceDefinition(course);
            } else if (trackedRace != null) {
                final Map<Competitor, Boat> competitorsAndTheirBoats = domainFactory.createCompetitorsAndBoats(startList, raceID, boatClass, new DefaultRaceTrackingHandler());
                if (!Util.setEquals(competitorsAndTheirBoats.keySet(), getRace().getCompetitors())) {
                    try {
                        trackedRegattaRegistry.updateRaceCompetitors(getRegatta(), getRace());
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }
    }

    @Override
    public void receivedWindData(String raceID, int zeroBasedMarkIndex, double windDirectionTrueDegrees, double windSpeedInKnots) {
        if (this.raceID.equals(raceID)) {
            DynamicTrackedRace trackedRace = getTrackedRace();
            if (trackedRace == null) {
                logger.warning("Received wind data at mark " + zeroBasedMarkIndex + ": " + windDirectionTrueDegrees + "deg at "
                        + windSpeedInKnots + "kts but didn't find tracked race; ignoring");
            } else {
                Waypoint wp = Util.get(trackedRace.getRace().getCourse().getWaypoints(), zeroBasedMarkIndex);
                final TimePoint timePoint = connector.getLastRPDMessageTimePoint();
                Position waypointPosition = trackedRace.getApproximatePosition(wp, timePoint);
                Wind wind = new WindImpl(waypointPosition, timePoint, new KnotSpeedWithBearingImpl(windSpeedInKnots, new DegreeBearingImpl(windDirectionTrueDegrees)));
                WindSource windSource = new WindSourceWithAdditionalID(WindSourceType.RACECOMMITTEE, "@"+zeroBasedMarkIndex);
                trackedRace.recordWind(wind, windSource);
            }
        }
    }

    private void createRaceDefinition(Course course) throws URISyntaxException {
        assert this.raceID.equals(raceID);
        assert startList != null;
        assert course != null;
        // now we can create the RaceDefinition and most other things
        final Race swissTimingRace = new RaceImpl(raceID, raceName, raceDescription, boatClass);
        try {
            synchronized (this) {
                race = domainFactory.createRaceDefinition(regatta, swissTimingRace, startList, course,
                        raceTrackingHandler);
                this.notifyAll();
            }
            // temp
            CompetitorAndBoatStore competitorStore = domainFactory.getBaseDomainFactory().getCompetitorAndBoatStore();
            for (com.sap.sailing.domain.swisstimingadapter.Competitor c : startList.getCompetitors()) {
                Competitor existingCompetitor = competitorStore.getExistingCompetitorByIdAsString(c.getIdAsString());
                if (existingCompetitor != null) {
                    competitorsByBoatId.put(c.getBoatID(), existingCompetitor);
                }
            }
            trackedRace = raceTrackingHandler.createTrackedRace(getTrackedRegatta(), race, Collections.<Sideline> emptyList(), windStore,
                    delayToLiveInMillis,
                    WindTrack.DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND,
                    /* time over which to average speed */ race.getBoatClass().getApproximateManeuverDurationInMilliseconds(),
                    new DynamicRaceDefinitionSet() {
                @Override
                public void addRaceDefinition(RaceDefinition race, DynamicTrackedRace trackedRace) {
                    // we already know our single RaceDefinition
                    assert SwissTimingRaceTrackerImpl.this.race == race;
                }
            }, useInternalMarkPassingAlgorithm, raceLogResolver,
                    /* Not needed because the RaceTracker is not active on a replica */ Optional.empty(),
                    new TrackingConnectorInfoImpl(SwissTimingAdapter.NAME, SwissTimingAdapter.DEFAULT_URL,/*no api connection to query the webUrl*/ null), markPassingRaceFingerprintRegistry);
            addUpdateHandlers();
            notifyRaceCreationListeners();
            logger.info("Created SwissTiming RaceDefinition and TrackedRace for "+race.getName());
        } catch (Exception exception) {
            logger.log(Level.WARNING,
                    "Error while creating race " + raceName + " for regatta " + trackedRegatta.getRegatta(), exception);
            try {
                if (race == null) {
                    trackedRegattaRegistry.stopTracker(regatta, this);
                } else {
                    trackedRegattaRegistry.stopTracking(regatta, race);
                }
            } catch (Exception e) {
                logger.log(Level.INFO,
                        "Something else went wrong while trying to notify the TrackedRegattaRegistry that the race "
                                + " could not be added to the the regatta " + trackedRegatta.getRegatta(),
                                e);
            }
        }
    }

    /**
     * If a non-{@code null}, non-{@link String#isEmpty() empty} {@link #updateURL} is set, adds {@link UpdateHandler}s
     * to the race that will forward changes to the race such as start time changes or course changes to the URL as REST
     * requests.
     */
    private void addUpdateHandlers() throws URISyntaxException {
        getDomainFactory().addUpdateHandlers(updateURL, updateUsername, updatePassword,
                /* TODO using the regatta ID as the "eventId" for now... */ regatta.getId(), trackedRace.getRace(), trackedRace);
    }
    
    /**
     * Checks if {@link #getRegatta()} still contains the {@link RaceDefinition} obtained when calling
     * {@link TrackedRace#getRace()} on {@link #trackedRace} and if the {@link #getTrackedRegatta() tracked regatta} for
     * {@link #getRegatta()} still contains {@link #trackedRace}. This is the precondition for updating the
     * {@link #trackedRace} with data received from the trackers.
     */
    private boolean isTrackedRaceStillReachable() {
        return trackedRace != null && Util.contains(getRegatta().getAllRaces(), trackedRace.getRace())
                && getTrackedRegatta().getExistingTrackedRace(trackedRace.getRace()) == trackedRace;
    }

    @Override
    public void receivedCourseConfiguration(String raceID, Course course) throws URISyntaxException {
        Course oldCourse = this.course;
        if (trackedRace == null) {
            if (oldCourse == null && startList != null) {
                createRaceDefinition(course);
                this.course = course;
            }
        } else {
            if (isTrackedRaceStillReachable()) {
                try {
                    domainFactory.updateCourseWaypoints(trackedRace.getRace().getCourse(), course.getMarks());
                    this.course = course;
                } catch (PatchFailedException e) {
                    logger.info("Internal error trying to update course: " + e.getMessage());
                    logger.throwing(SwissTimingRaceTrackerImpl.class.getName(), "receivedCourseConfiguration", e);
                }
            } else {
                if (!loggedIgnore) {
                    logger.info("Ignoring course configuration "+course+" for SwissTiming race "+raceID+
                            " because tracked race is no longer reachable. Was the race removed but is still tracked? "+
                            "(Future occurrences of this message will be suppressed)");
                    loggedIgnore = true;
                }
            }
        }
    }


    @Override
    public void receivedAvailableRaces(Iterable<Race> races) {
        // don't care
    }
    
    protected DynamicTrackedRace getTrackedRace() {
        return trackedRace;
    }

    public DomainFactory getDomainFactory() {
        return domainFactory;
    }

    public BoatClass getBoatClass() {
        return boatClass;
    }
}
