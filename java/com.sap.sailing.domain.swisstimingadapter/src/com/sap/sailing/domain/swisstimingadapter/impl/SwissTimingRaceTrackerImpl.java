package com.sap.sailing.domain.swisstimingadapter.impl;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.Util;
import com.sap.sailing.domain.common.Util.Triple;
import com.sap.sailing.domain.swisstimingadapter.Course;
import com.sap.sailing.domain.swisstimingadapter.DomainFactory;
import com.sap.sailing.domain.swisstimingadapter.Fix;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceSpecificMessageLoader;
import com.sap.sailing.domain.swisstimingadapter.RaceStatus;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterListener;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingRaceTracker;
import com.sap.sailing.domain.tracking.AbstractRaceTrackerImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicRaceDefinitionSet;
import com.sap.sailing.domain.tracking.DynamicTrackedEvent;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RacesHandle;
import com.sap.sailing.domain.tracking.TrackedEventRegistry;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;

import difflib.PatchFailedException;

public class SwissTimingRaceTrackerImpl extends AbstractRaceTrackerImpl implements SwissTimingRaceTracker, SailMasterListener {
    private static final Logger logger = Logger.getLogger(SwissTimingRaceTrackerImpl.class.getName());
    
    private final SailMasterConnector connector;
    private final String raceID;
    private final RaceSpecificMessageLoader messageLoader;
    private final DomainFactory domainFactory;
    private final Triple<String, String, Integer> id;
    private final Event event;
    private final WindStore windStore;

    private RaceDefinition race;
    private Course course;
    private StartList startList;
    private DynamicTrackedRace trackedRace;

    private boolean loggedIgnore;
    
    protected SwissTimingRaceTrackerImpl(String raceID, String hostname, int port, WindStore windStore,
            DomainFactory domainFactory, SwissTimingFactory factory, RaceSpecificMessageLoader messageLoader,
            TrackedEventRegistry trackedEventRegistry, boolean canSendRequests) throws InterruptedException,
            UnknownHostException, IOException, ParseException {
        super();
        this.connector = factory.getOrCreateSailMasterConnector(hostname, port, messageLoader, canSendRequests);
        this.domainFactory = domainFactory;
        this.raceID = raceID;
        this.messageLoader = messageLoader;
        this.windStore = windStore;
        this.id = new Triple<String, String, Integer>(raceID, hostname, port);
        connector.addSailMasterListener(raceID, this);
        event = domainFactory.getOrCreateEvent(raceID);
        setTrackedEvent(trackedEventRegistry.getOrCreateTrackedEvent(event));
        connector.trackRace(raceID);
    }

    @Override
    public void stop() throws MalformedURLException, IOException, InterruptedException {
        connector.removeSailMasterListener(raceID, this);
    }

    @Override
    public Set<RaceDefinition> getRaces() {
        return race==null?null:Collections.singleton(race);
    }

    @Override
    public RacesHandle getRacesHandle() {
        return new RacesHandle() {
            @Override
            public Event getEvent() {
                return SwissTimingRaceTrackerImpl.this.getEvent();
            }

            @Override
            public Set<RaceDefinition> getRaces() {
                return Collections.singleton(race);
            }

            @Override
            public Set<RaceDefinition> getRaces(long timeoutInMilliseconds) {
                return Collections.singleton(race);
            }

            @Override
            public DynamicTrackedEvent getTrackedEvent() {
                return SwissTimingRaceTrackerImpl.this.getTrackedEvent();
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
    public Event getEvent() {
        return event;
    }

    @Override
    public Triple<String, String, Integer> getID() {
        return id; 
    }

    @Override
    public void receivedRacePositionData(String raceID, RaceStatus status, TimePoint timePoint, TimePoint startTime,
            Long millisecondsSinceRaceStart, Integer nextMarkIndexForLeader, Distance distanceToNextMarkForLeader,
            Collection<Fix> fixes) {
        assert this.raceID.equals(raceID);
        if (isTrackedRaceStillReachable()) {
            if (this.raceID.equals(raceID)) {
                if (startTime != null) {
                    trackedRace.setStartTimeReceived(startTime);
                }
                for (Fix fix : fixes) {
                    GPSFixMoving gpsFix = domainFactory.createGPSFix(timePoint, fix);
                    switch (fix.getTrackerType()) {
                    case BUOY:
                    case COMMITTEE:
                    case JURY:
                    case TIMINGSCORING:
                    case UNIDENTIFIED:
                        String trackerID = fix.getBoatID();
                        Buoy buoy = domainFactory.getOrCreateBuoy(trackerID);
                        DynamicGPSFixTrack<Buoy, GPSFix> buoyTrack = trackedRace.getOrCreateTrack(buoy);
                        buoyTrack.addGPSFix(gpsFix);
                        break;
                    case COMPETITOR:
                        Competitor competitor = domainFactory.getCompetitorByBoatID(fix.getBoatID());
                        DynamicGPSFixTrack<Competitor, GPSFixMoving> competitorTrack = trackedRace.getTrack(competitor);
                        competitorTrack.addGPSFix(gpsFix);
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
    public void receivedTimingData(String raceID, String boatID,
            List<Triple<Integer, Integer, Long>> markIndicesRanksAndTimesSinceStartInMilliseconds) {
        assert this.raceID.equals(raceID);
        if (isTrackedRaceStillReachable()) {
            Competitor competitor = domainFactory.getCompetitorByBoatID(boatID);
            // the list of mark indices and time stamps is partial and usually only shows the last mark passing;
            // we need to use this to *update* the competitor's mark passings list, not *replace* it
            TreeMap<Integer, MarkPassing> markPassingsByMarkIndex = new TreeMap<Integer, MarkPassing>();
            // now fill with the already existing mark passings for the competitor identified by boatID...
            for (MarkPassing markPassing : trackedRace.getMarkPassings(competitor)) {
                markPassingsByMarkIndex.put(
                        trackedRace.getRace().getCourse().getIndexOfWaypoint(markPassing.getWaypoint()), markPassing);
            }
            // ...and then overwrite those for which we received "new evidence"
            for (Triple<Integer, Integer, Long> markIndexRankAndTimeSinceStartInMilliseconds : markIndicesRanksAndTimesSinceStartInMilliseconds) {
                Waypoint waypoint = Util.get(trackedRace.getRace().getCourse().getWaypoints(),
                        markIndexRankAndTimeSinceStartInMilliseconds.getA());
                MillisecondsTimePoint timePoint = trackedRace.getStart() == null ? null : new MillisecondsTimePoint(
                        trackedRace.getStart().asMillis() + markIndexRankAndTimeSinceStartInMilliseconds.getC());
                MarkPassing markPassing = domainFactory.createMarkPassing(timePoint, waypoint,
                        domainFactory.getCompetitorByBoatID(boatID));
                markPassingsByMarkIndex.put(markIndexRankAndTimeSinceStartInMilliseconds.getA(), markPassing);
            }
            trackedRace.updateMarkPassings(competitor, markPassingsByMarkIndex.values());
        } else {
            if (!loggedIgnore) {
                logger.info("Ignoring timing data " + markIndicesRanksAndTimesSinceStartInMilliseconds + " for SwissTiming race " + raceID
                        + " because tracked race is no longer reachable. Was the race removed but is still tracked? "+
                        "(Future occurrences of this message will be suppressed)");
                loggedIgnore = true;
            }
        }

    }

    @Override
    public void receivedClockAtMark(String raceID,
            List<Triple<Integer, TimePoint, String>> markIndicesTimePointsAndBoatIDs) {
        // Ignored because it's covered by TMD. Mail from Kai Hahndorf of 2011-11-15T12:42:00Z:
        // "Die TMD werden immer gesendet. Das CAM Protokoll ist nur f�r unsere TV-Grafik wichtig, da damit die R�ckstandsuhr gestartet wird."
    }

    @Override
    public void receivedStartList(String raceID, StartList startList) {
        this.startList = startList;
        if (course != null) {
            createRaceDefinition(raceID);
        }
    }

    private void createRaceDefinition(String raceID) {
        assert this.raceID.equals(raceID);
        assert startList != null;
        assert course != null;
        // now we can create the RaceDefinition and most other things
        Race swissTimingRace = messageLoader.getRace(raceID);
        race = domainFactory.createRaceDefinition(event, swissTimingRace, startList, course);
        trackedRace = getTrackedEvent().createTrackedRace(race, windStore,
                WindTrack.DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND,
                /* time over which to average speed */ race.getBoatClass().getApproximateManeuverDurationInMilliseconds(),
                new DynamicRaceDefinitionSet() {
                    @Override
                    public void addRaceDefinition(RaceDefinition race) {
                        // we already know our single RaceDefinition
                        assert SwissTimingRaceTrackerImpl.this.race == race;
                    }
                });
        logger.info("Created SwissTiming RaceDefinition and TrackedRace for "+race.getName());
    }
    
    /**
     * Checks if {@link #getEvent()} still contains the {@link RaceDefinition} obtained when calling
     * {@link TrackedRace#getRace()} on {@link #trackedRace} and if the {@link #getTrackedEvent() tracked event} for
     * {@link #getEvent()} still contains {@link #trackedRace}. This is the precondition for updating the
     * {@link #trackedRace} with data received from the trackers.
     */
    private boolean isTrackedRaceStillReachable() {
        return Util.contains(getEvent().getAllRaces(), trackedRace.getRace()) &&
                getTrackedEvent().getExistingTrackedRace(trackedRace.getRace()) == trackedRace;
    }

    @Override
    public void receivedCourseConfiguration(String raceID, Course course) {
        this.course = course;
        if (trackedRace == null) {
            if (startList != null) {
                createRaceDefinition(raceID);
            }
        } else {
            if (isTrackedRaceStillReachable()) {
                try {
                    domainFactory.updateCourseWaypoints(trackedRace.getRace().getCourse(), course.getMarks());
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
}
