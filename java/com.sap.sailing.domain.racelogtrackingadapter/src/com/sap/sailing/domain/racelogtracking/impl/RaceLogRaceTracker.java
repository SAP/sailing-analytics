package com.sap.sailing.domain.racelogtracking.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.abstractlog.AbstractLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEndOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.LastPublishedCourseDesignFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.TrackingTimesFinder;
import com.sap.sailing.domain.abstractlog.race.impl.BaseRaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogStartTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.analyzing.impl.RaceInformationFinder;
import com.sap.sailing.domain.abstractlog.race.tracking.analyzing.impl.RaceLogTrackingStateAnalyzer;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.RegattaLogDefineMarkEvent;
import com.sap.sailing.domain.abstractlog.regatta.impl.BaseRegattaLogEventVisitor;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.base.impl.RaceDefinitionImpl;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.abstractlog.TimePointSpecificationFoundInLog;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.domain.common.racelog.tracking.RaceNotCreatedException;
import com.sap.sailing.domain.racelog.tracking.GPSFixStore;
import com.sap.sailing.domain.racelogtracking.impl.logtracker.RaceLogGPSFixTracker;
import com.sap.sailing.domain.regattalike.IsRegattaLike;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackingDataLoader;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.WindTrack;
import com.sap.sailing.domain.tracking.impl.TrackedRaceStatusImpl;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

import difflib.PatchFailedException;

/**
 * Track a race using the data defined in the {@link RaceLog} and possibly the Leaderboards
 * {@link IsRegattaLike#getRegattaLog RegattaLog}. If the events suggest that the race is already in the
 * {@link RaceLogTrackingState#TRACKING} state, tracking commences immediately and existing fixes are loaded immediately
 * from the database.Thinkpad
 * <p>
 * Otherwise, the tracker waits until a {@link RaceLogStartTrackingEvent} is received to perform these tasks.
 * 
 * @author Fredrik Teschke
 */
public class RaceLogRaceTracker implements RaceTracker, TrackingDataLoader {
    
    private static final String LOGGER_AND_LOGAUTHOR_NAME = RaceLogRaceTracker.class.getName();
    private static final Logger logger = Logger.getLogger(LOGGER_AND_LOGAUTHOR_NAME);
    
    private final AbstractLogEventAuthor raceLogEventAuthor = new LogEventAuthorImpl(LOGGER_AND_LOGAUTHOR_NAME, 0);
    private final Map<AbstractLog<?, ?>, Object> visitors = new HashMap<AbstractLog<?, ?>, Object>();
    
    private final RaceLogConnectivityParams params;
    private final WindStore windStore;
    private final GPSFixStore gpsFixStore;
    private final DynamicTrackedRegatta regatta;
    private final RaceLogResolver raceLogResolver;

    private DynamicTrackedRace trackedRace;
    private RaceLogGPSFixTracker raceLogGPSFixTracker;

    public RaceLogRaceTracker(DynamicTrackedRegatta regatta, RaceLogConnectivityParams params, WindStore windStore,
            GPSFixStore gpsFixStore, RaceLogResolver raceLogResolver) {
        this.params = params;
        this.windStore = windStore;
        this.gpsFixStore = gpsFixStore;
        this.regatta = regatta;
        this.raceLogResolver = raceLogResolver;

        // add log listeners
        for (AbstractLog<?, ?> log : params.getLogHierarchy()) {
            if (log instanceof RaceLog) {
                RaceLogEventVisitor visitor = new BaseRaceLogEventVisitor() {
                    @Override
                    public void visit(RaceLogStartTrackingEvent event) {
                        RaceLogRaceTracker.this.onStartTrackingEvent(event);
                    };

                    @Override
                    public void visit(RaceLogCourseDesignChangedEvent event) {
                        RaceLogRaceTracker.this.onCourseDesignChangedEvent(event);
                    }
                    @Override
                    public void visit(RaceLogStartOfTrackingEvent event) {
                        RaceLogRaceTracker.this.onStartOfTrackingEvent(event);
                    }
                    @Override
                    public void visit(RaceLogEndOfTrackingEvent event) {
                        RaceLogRaceTracker.this.onEndOfTrackingEvent(event);
                    }
                    
                    @Override
                    public void visit(RaceLogStartTimeEvent event) {
                        trackedRace.updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
                    }
                    
                    @Override
                    public void visit(RaceLogRaceStatusEvent event) {
                        if (event.getNextStatus().equals(RaceLogRaceStatus.FINISHED)){
                            trackedRace.updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
                        }
                    }
                };
                visitors.put(log, visitor);
                ((RaceLog) log).addListener(visitor);
            } else if (log instanceof RegattaLog) {
                RegattaLogEventVisitor visitor = new BaseRegattaLogEventVisitor() {
                    @Override
                    public void visit(RegattaLogDefineMarkEvent event) {
                        RaceLogRaceTracker.this.onDefineMarkEvent(event);
                    }
                };
                visitors.put(log, visitor);
                ((RegattaLog) log).addListener(visitor);
            }
        }

        logger.info(String.format("Created race-log tracker for: %s %s %s", params.getLeaderboard(),
                params.getRaceColumn(), params.getFleet()));

        // load race for which tracking already started
        if (new RaceLogTrackingStateAnalyzer(params.getRaceLog()).analyze() == RaceLogTrackingState.TRACKING) {
            startTracking(null);
        }
    }

    @Override
    public void stop(boolean preemptive) {
        RaceLog raceLog = params.getRaceLog();
        final Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> trackingTimes = new TrackingTimesFinder(raceLog).analyze();
        if (trackingTimes == null || trackingTimes.getB() == null || trackingTimes.getB().getTimePoint() == null) {
            // seems the first time tracking for this race is stopped; enter "now" as end of tracking
            // into the race log
            raceLog.add(new RaceLogEndOfTrackingEventImpl(MillisecondsTimePoint.now(), raceLogEventAuthor, /* passId */ 0));
        }
        
        // mark passing calculator is automatically stopped, when the race status is set to {@link
        // TrackedRaceStatusEnum#FINISHED}
        trackedRace.onStatusChanged(this, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.FINISHED, 100));

        // remove listeners on logs
        for (Entry<AbstractLog<?, ?>, Object> visitor : visitors.entrySet()) {
            visitor.getKey().removeListener(visitor.getValue());
        }

        raceLogGPSFixTracker.stop();

        logger.info(String.format("Stopped tracking race-log race %s %s %s", params.getLeaderboard(),
                params.getRaceColumn(), params.getFleet()));
    }

    @Override
    public Regatta getRegatta() {
        return regatta.getRegatta();
    }

    @Override
    public Set<RaceDefinition> getRaces() {
        return trackedRace == null ? null : Collections.singleton(trackedRace.getRace());
    }

    @Override
    public Set<RegattaAndRaceIdentifier> getRaceIdentifiers() {
        return trackedRace == null ? null : Collections.singleton(trackedRace.getRaceIdentifier());
    }

    @Override
    public RaceHandle getRacesHandle() {
        return new RaceLogRacesHandle(this);
    }

    @Override
    public DynamicTrackedRegatta getTrackedRegatta() {
        return regatta;
    }

    @Override
    public WindStore getWindStore() {
        return windStore;
    }

    @Override
    public GPSFixStore getGPSFixStore() {
        return gpsFixStore;
    }

    @Override
    public Object getID() {
        return params.getRaceLog().getId();
    }
    
    /**
     * When a log is attached to it, the tracked race creates mark tracks for all marks either defined or with a device
     * mapped to it. When this tracker is running for a tracked race it has to mimic this behavior dynamically. When a
     * {@link RaceLogDefineMarkEvent} is received, the existence of the track for that mark in the {@link TrackedRace}
     * has to be ensured, also ensuring that the mark will exist in the mark tracks map key set.
     */
    private void onDefineMarkEvent(RegattaLogDefineMarkEvent event) {
        if (trackedRace != null) {
            trackedRace.getOrCreateTrack(event.getMark());
        }
    }

    private void onStartTrackingEvent(RaceLogStartTrackingEvent event) {
        if (trackedRace == null) {
            startTracking(event);
        }
    }
    
    private void onStartOfTrackingEvent(RaceLogStartOfTrackingEvent event) {
        if (trackedRace != null) {
            trackedRace.updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
        }
    }
    
    private void onEndOfTrackingEvent(RaceLogEndOfTrackingEvent event) {
        if (trackedRace != null) {
            trackedRace.updateStartAndEndOfTracking(/* waitForGPSFixesToLoad */ false);
        }
    }

    private void onCourseDesignChangedEvent(RaceLogCourseDesignChangedEvent event) {
        if (trackedRace != null) {
            CourseBase base = new LastPublishedCourseDesignFinder(params.getRaceLog(), /* onlyCoursesWithValidWaypointList */ true).analyze();
            List<Util.Pair<ControlPoint, PassingInstruction>> update = new ArrayList<>();
            for (Waypoint waypoint : base.getWaypoints()) {
                update.add(new Util.Pair<>(waypoint.getControlPoint(), waypoint.getPassingInstructions()));
            }
            try {
                trackedRace.getRace().getCourse().update(update, params.getDomainFactory());
            } catch (PatchFailedException e) {
                logger.log(Level.WARNING, "Could not update course for race " + trackedRace.getRace().getName());
            }
        }
    }

    private void startTracking(RaceLogStartTrackingEvent event) {
        RaceLog raceLog = params.getRaceLog();
        RaceColumn raceColumn = params.getRaceColumn();
        Fleet fleet = params.getFleet();
        RaceLogDenoteForTrackingEvent denoteEvent = new RaceInformationFinder(raceLog).analyze();
        final Pair<TimePointSpecificationFoundInLog, TimePointSpecificationFoundInLog> trackingTimes = new TrackingTimesFinder(raceLog).analyze();
        if (trackingTimes == null || trackingTimes.getA() == null || trackingTimes.getA().getTimePoint() == null) {
            // the start of tracking interval is unset or set to null; enter "now" as start of tracking into the race log
            raceLog.add(new RaceLogStartOfTrackingEventImpl(MillisecondsTimePoint.now(), raceLogEventAuthor, /* passId */ 0));
        }
        BoatClass boatClass = denoteEvent.getBoatClass();
        String raceName = denoteEvent.getRaceName();
        CourseBase courseBase = new LastPublishedCourseDesignFinder(raceLog, /* onlyCoursesWithValidWaypointList */ true).analyze();
        if (courseBase == null) {
            courseBase = new CourseDataImpl("Default course for " + raceName);
            logger.log(Level.FINE, "Using empty course in creation of race " + raceName);
        }
        final Course course = new CourseImpl(raceName + " course", courseBase.getWaypoints());
        if (raceColumn.getTrackedRace(fleet) != null) {
            if (event != null) {
                try {
                    raceLog.revokeEvent(params.getService().getServerAuthor(), event,
                            "could not start tracking because tracked race already exists");
                } catch (NotRevokableException e) {
                    logger.log(Level.WARNING, "Couldn't revoke event "+event, e);
                }
            }
            throw new RaceNotCreatedException(String.format("Race for racelog (%s) has already been created", raceLog));
        }
        Iterable<Competitor> competitors = raceColumn.getAllCompetitors(params.getFleet());
        Serializable raceId = denoteEvent.getRaceId();
        final RaceDefinition raceDef = new RaceDefinitionImpl(raceName, course, boatClass, competitors, raceId);
        Iterable<Sideline> sidelines = Collections.<Sideline> emptyList();
        // set race definition, so race is linked to leaderboard automatically
        regatta.getRegatta().addRace(raceDef);
        raceColumn.setRaceIdentifier(fleet, regatta.getRegatta().getRaceIdentifier(raceDef));
        trackedRace = regatta.createTrackedRace(raceDef, sidelines, windStore, gpsFixStore,
                params.getDelayToLiveInMillis(), WindTrack.DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND,
                boatClass.getApproximateManeuverDurationInMilliseconds(), null, /*useMarkPassingCalculator*/ true, raceLogResolver);
        trackedRace.onStatusChanged(this, new TrackedRaceStatusImpl(TrackedRaceStatusEnum.TRACKING, 0));

        raceLogGPSFixTracker = new RaceLogGPSFixTracker(regatta, trackedRace, gpsFixStore);
        logger.info(String.format("Started tracking race-log race (%s)", raceLog));
        // this wakes up all waiting race handles
        synchronized (this) {
            this.notifyAll();
        }
    }
}
