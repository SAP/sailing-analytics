package com.sap.sailing.domain.racelogtracking.impl;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.StreamSupport;

import javax.mail.MessagingException;

import org.osgi.framework.ServiceReference;

import com.sap.sailing.domain.abstractlog.impl.LastEventOfTypeFinder;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.LastPublishedCourseDesignFinder;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogStartTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.analyzing.impl.RaceLogTrackingStateAnalyzer;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogDenoteForTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogStartTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDefinedMarkAnalyzer;
import com.sap.sailing.domain.abstractlog.regatta.tracking.analyzing.impl.RegattaLogDeviceMarkMappingFinder;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.MailInvitationType;
import com.sap.sailing.domain.common.NotFoundException;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.racelog.tracking.CompetitorRegistrationOnRaceLogDisabledException;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotableForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogRaceTrackerExistsException;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.racelogtracking.DeviceMappingWithRegattaLogEvent;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.regattalike.LeaderboardThatHasRegattaLike;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.RaceTrackingHandler;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.mail.MailService;
import com.sap.sse.shared.util.impl.NonGwtUrlHelper;

public class RaceLogTrackingAdapterImpl implements RaceLogTrackingAdapter {
    private static final Logger logger = Logger.getLogger(RaceLogTrackingAdapterImpl.class.getName());

    private final DomainFactory domainFactory;
    private final long delayToLiveInMillis;

    public RaceLogTrackingAdapterImpl(DomainFactory domainFactory) {
        this.domainFactory = domainFactory;
        this.delayToLiveInMillis = TrackedRace.DEFAULT_LIVE_DELAY_IN_MILLISECONDS;
    }

    @Override
    public RaceHandle startTracking(RacingEventService service, Leaderboard leaderboard, RaceColumn raceColumn,
            Fleet fleet, boolean trackWind, boolean correctWindDirectionByMagneticDeclination,
            RaceTrackingHandler raceTrackingHandler)
            throws NotDenotedForRaceLogTrackingException, Exception {
        RaceLog raceLog = raceColumn.getRaceLog(fleet);
        RaceLogTrackingState raceLogTrackingState = new RaceLogTrackingStateAnalyzer(raceLog).analyze();
        if (!raceLogTrackingState.isForTracking()) {
            throw new NotDenotedForRaceLogTrackingException();
        }
        RegattaIdentifier regatta = ((RegattaLeaderboard) leaderboard).getRegatta().getRegattaIdentifier();
        if (raceLogTrackingState != RaceLogTrackingState.TRACKING) {
            RaceLogEvent event = new RaceLogStartTrackingEventImpl(MillisecondsTimePoint.now(),
                    service.getServerAuthor(), raceLog.getCurrentPassId());
            raceLog.add(event);
        }
        final RaceHandle result;
        if (!isRaceLogRaceTrackerAttached(service, raceLog)) {
            result = addTracker(service, regatta, leaderboard, raceColumn, fleet, -1, trackWind,
                    correctWindDirectionByMagneticDeclination, raceTrackingHandler);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Adds a {@link RaceLogRaceTracker}. If a {@link RaceLogStartTrackingEvent} is already present in the
     * {@code RaceLog} linked to the {@code raceColumn} and {@code fleet}, a {@code TrackedRace} is created immediately
     * and tracking begins. Otherwise, the {@code RaceLogRaceTracker} waits until a {@code StartTrackingEvent} is added
     * to perform these actions. The race first has to be denoted for racelog tracking.
     */
    private RaceHandle addTracker(RacingEventService service, RegattaIdentifier regattaToAddTo, Leaderboard leaderboard,
            RaceColumn raceColumn, Fleet fleet, long timeoutInMilliseconds, boolean trackWind,
            boolean correctWindDirectionByMagneticDeclination, RaceTrackingHandler raceTrackingHandler) throws RaceLogRaceTrackerExistsException, Exception {
        RaceLog raceLog = raceColumn.getRaceLog(fleet);
        assert !isRaceLogRaceTrackerAttached(service, raceLog) : new RaceLogRaceTrackerExistsException(
                leaderboard.getName() + " - " + raceColumn.getName() + " - " + fleet.getName());
        Regatta regatta = regattaToAddTo == null ? null : service.getRegatta(regattaToAddTo);
        RaceLogConnectivityParams params = new RaceLogConnectivityParams(service.getServerAuthor(), regatta, raceColumn, fleet,
                leaderboard, delayToLiveInMillis, domainFactory, trackWind, correctWindDirectionByMagneticDeclination);
        return service.addRace(regattaToAddTo, params, timeoutInMilliseconds, raceTrackingHandler);
    }

    @Override
    public boolean denoteRaceForRaceLogTracking(RacingEventService service, Leaderboard leaderboard,
            RaceColumn raceColumn, Fleet fleet, String raceName) throws NotDenotableForRaceLogTrackingException {
        final BoatClass boatClass;
        if (leaderboard instanceof RegattaLeaderboard) {
            RegattaLeaderboard rLeaderboard = (RegattaLeaderboard) leaderboard;
            boatClass = rLeaderboard.getRegatta().getBoatClass();
        } else {
            if (!Util.isEmpty(raceColumn.getAllCompetitorsAndTheirBoats(fleet).values())) {
                boatClass = findDominatingBoatClass(raceColumn.getAllCompetitorsAndTheirBoats(fleet).values());
            } else if (!Util.isEmpty(raceColumn.getAllCompetitorsAndTheirBoats().values())) {
                boatClass = findDominatingBoatClass(raceColumn.getAllCompetitorsAndTheirBoats().values());
            } else if (!Util.isEmpty(leaderboard.getAllCompetitors())) {
                boatClass = leaderboard.getBoatClass();
            } else {
                throw new NotDenotableForRaceLogTrackingException(
                        "Couldn't infer boat class, no competitors on race and leaderboard");
            }
        }
        final boolean result;
        if (raceName == null) {
            raceName = leaderboard.getName() + " " + raceColumn.getName() + " " + fleet.getName();
        }
        RaceLog raceLog = raceColumn.getRaceLog(fleet);
        if (raceLog == null) {
            throw new NotDenotableForRaceLogTrackingException("No RaceLog found in place");
        }
        if (new RaceLogTrackingStateAnalyzer(raceLog).analyze().isForTracking()) {
            result = false;
        } else {
            RaceLogEvent event = new RaceLogDenoteForTrackingEventImpl(MillisecondsTimePoint.now(),
                    service.getServerAuthor(), raceLog.getCurrentPassId(), raceName, boatClass, UUID.randomUUID());
            raceLog.add(event);
            result = true;
        }
        return result;
    }

    private BoatClass findDominatingBoatClass(Iterable<Boat> allBoats) {
        return Util.getDominantObject(() -> StreamSupport.stream(allBoats.spliterator(), /* parallel */ false)
                .map(b -> b.getBoatClass()).iterator());
    }

    @Override
    public void denoteAllRacesForRaceLogTracking(final RacingEventService service, final Leaderboard leaderboard,
            final String prefix) throws NotDenotableForRaceLogTrackingException {
        int fleetcount = 1;
        for (RaceColumn column : leaderboard.getRaceColumns()) {
            for (Fleet fleet : column.getFleets()) {
                if (prefix != null) {
                    denoteRaceForRaceLogTracking(service, leaderboard, column, fleet, prefix + fleetcount);
                } else {
                    denoteRaceForRaceLogTracking(service, leaderboard, column, fleet, null);
                }
                fleetcount++;
            }
        }
    }

    @Override
    public boolean isRaceLogRaceTrackerAttached(RacingEventService service, RaceLog raceLog) {
        return service.getRaceTrackerById(raceLog.getId()) != null;
    }

    @Override
    public RaceLogTrackingState getRaceLogTrackingState(RacingEventService service, RaceColumn raceColumn,
            Fleet fleet) {
        return new RaceLogTrackingStateAnalyzer(raceColumn.getRaceLog(fleet)).analyze();
    }

    @Override
    public void copyCourse(RaceLog fromRaceLog, LeaderboardThatHasRegattaLike fromLeaderboard, Set<RaceLog> toRaceLogs,
            LeaderboardThatHasRegattaLike toLeaderboard, boolean copyMarkDeviceMappings,
            SharedDomainFactory<?> baseDomainFactory, RacingEventService service, int priority) {
        CourseBase course = new LastPublishedCourseDesignFinder(fromRaceLog,
                /* onlyCoursesWithValidWaypointList */ true).analyze();
        final Set<Mark> marks = new HashSet<>();
        if (course != null) {
            course.getWaypoints().forEach(wp -> Util.addAll(wp.getMarks(), marks));
        }
        final RegattaLog toLeaderboardRegattaLog = toLeaderboard.getRegattaLike().getRegattaLog();
        final Collection<Mark> marksAlreadyDefinedInTarget = new RegattaLogDefinedMarkAnalyzer(toLeaderboardRegattaLog).analyze();
        for (final Mark mark : marks) {
            if (!marksAlreadyDefinedInTarget.contains(mark)) {
                toLeaderboardRegattaLog.add(new RegattaLogDefineMarkEventImpl(TimePoint.now(), service.getServerAuthor(), TimePoint.now(),
                        UUID.randomUUID(), mark));
            }
        }
        if (copyMarkDeviceMappings) {
            for (final Entry<Mark, List<DeviceMappingWithRegattaLogEvent<Mark>>> markDeviceMapping : new RegattaLogDeviceMarkMappingFinder(
                    fromLeaderboard.getRegattaLike().getRegattaLog()).analyze().entrySet()) {
                for (final DeviceMappingWithRegattaLogEvent<Mark> markDeviceMappingWithRegattaLogEvent : markDeviceMapping.getValue()) {
                    toLeaderboardRegattaLog.add(markDeviceMappingWithRegattaLogEvent.getRegattaLogEvent());
                }
            }
        }
        for (RaceLog toRaceLog : toRaceLogs) {
            if (new RaceLogTrackingStateAnalyzer(toRaceLog).analyze().isForTracking()) {
                if (course != null) {
                    CourseBase newCourse = new CourseDataImpl(course.getName(), course.getOriginatingCourseTemplateIdOrNull());
                    TimePoint now = MillisecondsTimePoint.now();
                    int i = 0;
                    for (Waypoint oldWaypoint : course.getWaypoints()) {
                        newCourse.addWaypoint(i++, oldWaypoint);
                    }
                    int passId = toRaceLog.getCurrentPassId();
                    RaceLogEvent newCourseEvent = new RaceLogCourseDesignChangedEventImpl(now,
                            new LogEventAuthorImpl(service.getServerAuthor().getName(), priority),
                            passId, newCourse, CourseDesignerMode.ADMIN_CONSOLE);
                    toRaceLog.add(newCourseEvent);
                }
            }
        }
    }

    @Override
    public void copyCompetitors(final RaceColumn fromRaceColumn, final Fleet fromFleet,
            final Iterable<Pair<RaceColumn, Fleet>> toRaces) {
        Map<Competitor, Boat> competitorsAndBoatsToCopy = fromRaceColumn.getAllCompetitorsAndTheirBoats(fromFleet);
        for (Pair<RaceColumn, Fleet> toRace : toRaces) {
            final RaceColumn toRaceColumn = toRace.getA();
            final Fleet toFleet = toRace.getB();
            try {
                if (toRaceColumn.isCompetitorRegistrationInRacelogEnabled(toFleet)) {
                    toRaceColumn.registerCompetitors(competitorsAndBoatsToCopy, toFleet);
                } else {
                    toRaceColumn.enableCompetitorRegistrationOnRaceLog(toFleet);
                    toRaceColumn.registerCompetitors(competitorsAndBoatsToCopy, toFleet);
                }
            } catch (CompetitorRegistrationOnRaceLogDisabledException e1) {
                // cannot happen as we explicitly checked successfully before, or enabled it when the check failed;
                // still produce a log documenting this strangeness:
                logger.log(Level.WARNING, "Internal error: race column " + toRaceColumn.getName()
                        + " does not accept competitor registration although it should", e1);
            }
        }
    }

    @Override
    public void pingMark(RegattaLog log, Mark mark, GPSFix gpsFix, RacingEventService service) {
        final PingDeviceIdentifierImpl device = new PingDeviceIdentifierImpl();
        final TimePoint timePoint = gpsFix.getTimePoint();
        final TimePoint now = MillisecondsTimePoint.now();
        final RegattaLogEvent mapping = new RegattaLogDeviceMarkMappingEventImpl(now, now, service.getServerAuthor(),
                UUID.randomUUID(), mark, device, timePoint, timePoint);
        log.add(mapping);
        try {
            service.getSensorFixStore().storeFix(device, gpsFix);
        } catch (NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not ping mark " + mark);
        }
    }

    @Override
    public void removeDenotationForRaceLogTracking(RacingEventService service, RaceLog raceLog) {
        RaceLogEvent denoteForTrackingEvent = new LastEventOfTypeFinder<>(raceLog, true, RaceLogDenoteForTrackingEvent.class).analyze();
        RaceLogEvent startTrackingEvent = new LastEventOfTypeFinder<>(raceLog, true, RaceLogStartTrackingEvent.class).analyze();
        if (denoteForTrackingEvent != null) {
            try {
                raceLog.revokeEvent(service.getServerAuthor(), denoteForTrackingEvent, "remove denotation");
                if (startTrackingEvent != null) {
                    raceLog.revokeEvent(service.getServerAuthor(), startTrackingEvent, "reset start tracking time upon removing denotation");
                }
            } catch (NotRevokableException e) {
                logger.log(Level.WARNING, "could not remove denotation by adding RevokeEvents", e);
            }
        }
    }

    private MailService getMailService() {
        ServiceReference<MailService> ref = Activator.getContext().getServiceReference(MailService.class);
        if (ref == null) {
            logger.warning("No mail service registered");
            return null;
        }
        return Activator.getContext().getService(ref);
    }

    @Override
    public void inviteCompetitorsForTrackingViaEmail(Event event, Leaderboard leaderboard, Regatta regatta,
            String serverUrlWithoutTrailingSlash, Set<Competitor> competitors, String legacyIOSAppUrl, String legacyAndroidAppUrl,
            Locale locale, MailInvitationType type) throws MailException {
        final StringBuilder occuredExceptions = new StringBuilder();
        for (final Competitor competitor : competitors) {
            final String toAddress = competitor.getEmail();
            if (toAddress != null && !toAddress.isEmpty()) {
                try {
                    final String url = DeviceMappingConstants.getDeviceMappingForRegattaLogUrl(
                            serverUrlWithoutTrailingSlash, event.getId().toString(), leaderboard.getName(),
                            DeviceMappingConstants.URL_COMPETITOR_ID_AS_STRING, competitor.getId().toString(),
                            regatta.getRegistrationLinkSecret(), NonGwtUrlHelper.INSTANCE);
                    RaceLogTrackingInvitationMailBuilder mail = getMailBuilder(type, locale);
                    mail.withSubject(competitor.getName())
                            .addEventLogo(event)
                            .addHeadline(event, leaderboard)
                            .addSailInSightIntroductoryText(competitor.getName())
                            .addSailInsightDeeplink(url, legacyIOSAppUrl, legacyAndroidAppUrl);
                    getMailService().sendMail(toAddress, mail.getSubject(), mail.getMultipartSupplier());
                } catch (MessagingException | MailException | IOException e) {
                    logger.log(Level.SEVERE, "Error while trying to send invitation mail to competitor"
                            + competitor.getName() + " with e-mail address " + toAddress + "!", e);
                    occuredExceptions.append(e.getMessage() + "\r\n");
                }
            }
        }
        if (!(occuredExceptions.length() == 0)) {
            throw new MailException(occuredExceptions.toString());
        }
    }

    @Override
    public void inviteBuoyTenderViaEmail(Event event, Leaderboard leaderboard, Regatta regatta,
            String serverUrlWithoutTrailingSlash, String emails, String legacyIOSAppUrl, String legacyAndroidAppUrl,
            Locale locale, MailInvitationType type) throws MailException {
        final StringBuilder occuredExceptions = new StringBuilder();
        final String[] emailArray = emails.split(",");
        // http://<host>/buoy-tender/checkin?event_id=<event-id>&leaderboard_name=<leaderboard-name>
        final String url = DeviceMappingConstants.getBuoyTenderInvitationUrl(serverUrlWithoutTrailingSlash,
                leaderboard.getName(), event.getId().toString(), regatta.getRegistrationLinkSecret(),
                NonGwtUrlHelper.INSTANCE);
        for (String toAddress : emailArray) {
            try {
                final String buoyTender = RaceLogTrackingI18n.buoyTender(locale);
                RaceLogTrackingInvitationMailBuilder mail = getMailBuilder(type, locale);
                mail.withSubject(buoyTender)
                .addEventLogo(event)
                .addHeadline(event, leaderboard)
                .addBuoyPingerIntroductoryText(buoyTender)
                        .addBuoyPingerDeeplink(url, legacyIOSAppUrl, legacyAndroidAppUrl);
                getMailService().sendMail(toAddress, mail.getSubject(), mail.getMultipartSupplier());
            } catch (MessagingException | MailException | IOException e) {
                logger.log(Level.SEVERE, "Error while trying to send invitation mail to buoy tender "
                        + "with e-mail address " + toAddress + "!", e);
                occuredExceptions.append(e.getMessage() + "\r\n");
            }
        }
        if (!(occuredExceptions.length() == 0)) {
            throw new MailException(occuredExceptions.toString());
        }
    }

    private RaceLogTrackingInvitationMailBuilder getMailBuilder(MailInvitationType type, Locale locale) {
        final RaceLogTrackingInvitationMailBuilder mail;
        switch (type) {
        case LEGACY:
            mail = new LegacyRaceLogTrackingInvitationMailBuilder(locale);
            break;
        case SailInsight1:
        case SailInsight2:
        case SailInsight3:
            mail = new BranchIORaceLogTrackingInvitationMailBuilder(locale, type);
            break;
        default:
            throw new IllegalArgumentException("Unhandled mail type");
        }
        return mail;
    }


    @Override
    public void copyPairingListFromOtherLeaderboard(RegattaLeaderboard sourceLeaderboard, RegattaLeaderboard targetLeaderboard,
            String fromRaceColumnName, String toRaceColumnInclusiveName) throws NotFoundException {
        final RaceColumn fromColumn = sourceLeaderboard.getRaceColumnByName(fromRaceColumnName);
        if (fromColumn == null) {
            throw new NotFoundException("Race column from which to start copying pairings "+fromRaceColumnName+" not found in source leaderboard "+sourceLeaderboard.getName());
        }
        final RaceColumn toColumn = sourceLeaderboard.getRaceColumnByName(toRaceColumnInclusiveName);
        if (toColumn == null) {
            throw new NotFoundException("Race column up to which to copy pairings "+toColumn+" not found in source leaderboard "+targetLeaderboard.getName());
        }
        // ensure all boats from source regatta are also registered on the target regatta
        final Iterable<Boat> targetBoats = targetLeaderboard.getAllBoats();
        for (final Boat sourceBoat : sourceLeaderboard.getAllBoats()) {
            if (!Util.contains(targetBoats, sourceBoat)) {
                // register the boat on the target leaderboard
                targetLeaderboard.registerBoat(sourceBoat);
            }
        }
        boolean inSequence = false;
        final Iterator<RaceColumn> targetRaceColumnIterator = targetLeaderboard.getRaceColumns().iterator();
        for (final RaceColumn sourceRaceColumn : sourceLeaderboard.getRaceColumns()) {
            // we're in a regatta leaderboard, so we can safely cast
            final RaceColumnInSeries sourceRaceColumnInSeries = (RaceColumnInSeries) sourceRaceColumn;
            if (sourceRaceColumn == fromColumn) {
                inSequence = true;
            }
            if (inSequence) {
                if (!targetRaceColumnIterator.hasNext()) {
                    throw new IllegalArgumentException("Target leaderboard "+targetLeaderboard.getName()+
                            " doesn't have enough race columns to copy pairings from source leaderboard");
                }
                copyPairingsFromRaceColumn(sourceLeaderboard, targetLeaderboard, sourceRaceColumnInSeries, (RaceColumnInSeries) targetRaceColumnIterator.next());
                if (sourceRaceColumn == toColumn) {
                    inSequence = false;
                }
            }
        }
    }

    /**
     * Copies the competitor-to-boat pairings from {@code sourceRaceColumn} to {@code targetRaceColumn}.
     * If there is a mismatch in the number of fleets, an {@link IllegalArgumentException} is thrown.
     * If competitors or boats are missing from the {@code targetLeaderboard}, they are registered on the
     * {@code targetLeaderboard}'s regatta.
     */
    private void copyPairingsFromRaceColumn(RegattaLeaderboard sourceLeaderboard, RegattaLeaderboard targetLeaderboard,
            RaceColumnInSeries sourceRaceColumn, RaceColumnInSeries targetRaceColumn) throws IllegalArgumentException {
        final Series sourceSeries = sourceRaceColumn.getSeries();
        final Series targetSeries = targetRaceColumn.getSeries();
        if (Util.size(sourceSeries.getFleets()) != Util.size(targetSeries.getFleets())) {
            throw new IllegalArgumentException("Source series has "+Util.size(sourceSeries.getFleets())+
                    " fleets, but target series has "+Util.size(targetSeries.getFleets()));
        }
        final Iterator<? extends Fleet> targetFleetIterator = targetSeries.getFleets().iterator();
        for (final Fleet sourceFleet : sourceRaceColumn.getFleets()) {
            final Fleet targetFleet = targetFleetIterator.next();
            final Iterable<Competitor> targetCompetitors = targetRaceColumn.getAllCompetitors(targetFleet);
            // ensure the target regatta has all competitors we need:
            for (final Competitor sourceCompetitor : sourceRaceColumn.getAllCompetitors(sourceFleet)) {
                if (!Util.contains(targetCompetitors, sourceCompetitor)) {
                    // register the competitor on the target leaderboard
                    targetLeaderboard.registerCompetitor(sourceCompetitor);
                }
            }
            copyCompetitors(sourceRaceColumn, sourceFleet, Collections.singleton(new Pair<>(targetRaceColumn, targetFleet)));
        }
    }
}
