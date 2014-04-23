package com.sap.sailing.domain.racelogtracking;

import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotableForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.racelog.tracking.DefineMarkEvent;
import com.sap.sailing.domain.racelog.tracking.DenoteForTrackingEvent;
import com.sap.sailing.domain.racelog.tracking.GPSFixStore;
import com.sap.sailing.domain.racelog.tracking.StartTrackingEvent;
import com.sap.sailing.domain.racelogtracking.impl.RaceLogRaceTracker;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventService;

public interface RaceLogTrackingAdapter {    
    /**
     * Performs the necessary steps to ensure that the race is tracked (aka that a {@link TrackedRace}
     * is created from the data in this {@code RaceLog}).<p>
     * The following steps are performed to achieve this: 
     * <ul>
     * <li>Is the racelog denoted for tracking? If not, throw exception.</li>
     * <li>Is a {@link RaceLogRaceTracker} already listening for this racelog? If not, add one.</li>
     * <li>Is a {@link StartTrackingEvent} present in the racelog? If not, add one</li>
     * </ul>
     */
    void startTracking(RacingEventService service, Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet)
            throws NotDenotedForRaceLogTrackingException, Exception;
    
    RaceLogTrackingState getRaceLogTrackingState(RacingEventService service, RaceColumn raceColumn, Fleet fleet);

    /**
     * Is a {@link RaceLogRaceTracker} already listening on this {@code raceLog}?
     */
    boolean isRaceLogRaceTrackerAttached(RacingEventService service, RaceLog raceLog);

    /**
     * Denotes the {@link RaceLog} for racelog-tracking, by inserting a {@link DenoteForTrackingEvent}.
     * 
     * @throws NotDenotableForRaceLogTrackingException
     *             Fails, if no {@link RaceLog}, or a non-empty {@link RaceLog}, or one with attached
     *             {@link TrackedRace} is found already in place.
     *             Also fails, if the {@code leaderboard} is not a {@link RegattaLeaderboard}.
     */
    void denoteRaceForRaceLogTracking(RacingEventService service, Leaderboard leaderboard, RaceColumn raceColumn,
            Fleet fleet, String raceName) throws NotDenotableForRaceLogTrackingException;

    /**
     * Denotes the entire {@link Leaderboard} for racelog-tracking, by calling the
     * {@link #denoteRaceForRaceLogTracking(RacingEventService, Leaderboard, RaceColumn, Fleet, String)} method for each
     * {@link RaceLog}.<p>
     * 
     * Will fail if there one or more of the attached {@code RaceLogs} are not empty.<p>
     * 
     * Also, a listener is registered, that denotes every {@link RaceLog} that is added at a later time as well.
     */
    void denoteLeaderboardForRaceLogTracking(RacingEventService service, Leaderboard leaderboard)
            throws NotDenotableForRaceLogTrackingException;
    
    /**
     * Add a fix to the {@link GPSFixStore}, and create a mapping with a virtual device for exactly that timepoint.
     */
    void pingMark(RaceLog raceLogToAddTo, Mark mark, GPSFix gpsFix, RacingEventService service);
    
    /**
     * Duplicate the course in the newest {@link RaceLogCourseDesignChangedEvent} in {@code from} race log to the
     * {@code to} race log.
     * The {@link Mark}s and {@link ControlPoint}s are duplicated and not reused. This also inserts the necessary
     * {@link DefineMarkEvent}s into the {@code to} race log.
     */
    void copyCourseToOtherRaceLog(RaceLog from, RaceLog to, SharedDomainFactory baseDomainFactory, RacingEventService service);
}
