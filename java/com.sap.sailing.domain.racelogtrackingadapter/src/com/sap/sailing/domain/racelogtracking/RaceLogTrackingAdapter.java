package com.sap.sailing.domain.racelogtracking;

import java.util.Locale;
import java.util.Set;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogStartTrackingEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotableForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.racelogtracking.impl.RaceLogRaceTracker;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.mail.MailException;

public interface RaceLogTrackingAdapter {
    /**
     * Performs the necessary steps to ensure that the race is tracked (aka that a {@link TrackedRace} is created from
     * the data in this {@code RaceLog}).
     * <p>
     * The following steps are performed to achieve this:
     * <ul>
     * <li>Is the racelog denoted for tracking? If not, throw exception.</li>
     * <li>Is a {@link RaceLogRaceTracker} already listening for this racelog? If not, add one.</li>
     * <li>Is a {@link RaceLogStartTrackingEvent} present in the racelog? If not, add one</li>
     * </ul>
     */
    RaceHandle startTracking(RacingEventService service, Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet, boolean trackWind, boolean correctWindDirectionByMagneticDeclination)
            throws NotDenotedForRaceLogTrackingException, Exception;

    RaceLogTrackingState getRaceLogTrackingState(RacingEventService service, RaceColumn raceColumn, Fleet fleet);

    /**
     * Is a {@link RaceLogRaceTracker} already listening on this {@code raceLog}?
     */
    boolean isRaceLogRaceTrackerAttached(RacingEventService service, RaceLog raceLog);

    /**
     * Denotes the {@link RaceLog} for racelog-tracking, by inserting a {@link RaceLogDenoteForTrackingEvent}.
     * 
     * @return {@code true} if the race was not yet denoted for race log tracking and now has successfully been denoted
     *         so
     * 
     * @throws NotDenotableForRaceLogTrackingException
     *             Fails, if no {@link RaceLog}, or a non-empty {@link RaceLog}, or one with attached
     *             {@link TrackedRace} is found already in place. Also fails, if the {@code leaderboard} is not a
     *             {@link RegattaLeaderboard}.
     */
    boolean denoteRaceForRaceLogTracking(RacingEventService service, Leaderboard leaderboard, RaceColumn raceColumn,
            Fleet fleet, String raceName) throws NotDenotableForRaceLogTrackingException;

    /**
     * Revoke the {@link RaceLogDenoteForTrackingEvent}, and if it exists the {@link RaceLogStartTrackingEvent}. This
     * does not affect existing an {@link RaceLogRaceTracker} or {@link TrackedRace} for this {@code RaceLog}.
     */
    void removeDenotationForRaceLogTracking(RacingEventService service, RaceLog raceLog);

    /**
     * Denotes the entire {@link Leaderboard} for racelog-tracking, by calling the
     * {@link #denoteRaceForRaceLogTracking(RacingEventService, Leaderboard, RaceColumn, Fleet, String)} method for each
     * {@link RaceLog}.
     */
    void denoteAllRacesForRaceLogTracking(RacingEventService service, Leaderboard leaderboard, String prefix)
            throws NotDenotableForRaceLogTrackingException;

    /**
     * Add a fix to the {@link GPSFixStore}, and create a mapping with a virtual device for exactly that time point
     * in the {@code regattaLogToAddTo}, mapping the virtual device to the {@code mark}.
     */
    void pingMark(RegattaLog regattaLogToAddTo, Mark mark, GPSFix gpsFix, RacingEventService service);

    /**
     * Invite competitors for tracking via the Tracking App by sending out emails.
     * 
     * @throws MailException
     */
    void inviteCompetitorsForTrackingViaEmail(Event event, Leaderboard leaderboard,
            String serverUrlWithoutTrailingSlash, Set<Competitor> competitors, String iOSAppUrl, String androidAppUrl,
            Locale locale) throws MailException;

    /**
     * Invite buoy tenders for buoy pinging via the Buoy Tender App by sending out emails.
     * 
     * @param appUrl
     * @throws MailException
     */
    void inviteBuoyTenderViaEmail(Event event, Leaderboard leaderboard, String serverUrlWithoutTrailingSlash,
            String emails, String iOSAppUrl, String androidAppUrl, Locale locale) throws MailException;

    /**
     * Copy the course in the newest {@link RaceLogCourseDesignChangedEvent} in {@code from} race log to the {@code to}
     * race logs. The {@link Mark}s and {@link ControlPoint}s are reused and not duplicated.
     */
    void copyCourse(RaceLog fromRaceLog, Set<RaceLog> toRaceLogs, SharedDomainFactory baseDomainFactory,
            RacingEventService service);

    void copyCompetitors(RaceColumn fromRaceColumn, Fleet fromFleet, Iterable<Pair<RaceColumn, Fleet>> toRaces);
}
