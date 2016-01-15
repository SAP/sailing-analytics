package com.sap.sailing.domain.racelogtracking.impl;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

import org.osgi.framework.ServiceReference;

import com.google.zxing.WriterException;
import com.sap.sailing.domain.abstractlog.impl.LastEventOfTypeFinder;
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
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.common.RegattaIdentifier;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.racelog.tracking.CompetitorRegistrationOnRaceLogDisabledException;
import com.sap.sailing.domain.common.racelog.tracking.DeviceMappingConstants;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotableForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.NotDenotedForRaceLogTrackingException;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogRaceTrackerExistsException;
import com.sap.sailing.domain.common.racelog.tracking.RaceLogTrackingState;
import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.racelogtracking.PingDeviceIdentifierImpl;
import com.sap.sailing.domain.racelogtracking.RaceLogTrackingAdapter;
import com.sap.sailing.domain.tracking.RaceHandle;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.mail.MailException;
import com.sap.sse.mail.MailService;
import com.sap.sse.qrcode.QRCodeGenerationUtil;
import com.sap.sse.util.impl.NonGwtUrlHelper;

public class RaceLogTrackingAdapterImpl implements RaceLogTrackingAdapter {
    private static final Logger logger = Logger.getLogger(RaceLogTrackingAdapterImpl.class.getName());

    /**
     * The URL prefix that the iOS app will recognize as a deep link and pass anything after this prefix on
     * to the app for analysis
     */
    private static final String IOS_DEEP_LINK_PREFIX = "comsapsailingtracker://";

    private final DomainFactory domainFactory;
    private final long delayToLiveInMillis;

    public RaceLogTrackingAdapterImpl(DomainFactory domainFactory) {
        this.domainFactory = domainFactory;
        this.delayToLiveInMillis = TrackedRace.DEFAULT_LIVE_DELAY_IN_MILLISECONDS;
    }

    @Override
    public RaceHandle startTracking(RacingEventService service, Leaderboard leaderboard, RaceColumn raceColumn,
            Fleet fleet) throws NotDenotedForRaceLogTrackingException, Exception {
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
            result = addTracker(service, regatta, leaderboard, raceColumn, fleet, -1);
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
    private RaceHandle addTracker(RacingEventService service, RegattaIdentifier regattaToAddTo,
            Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet, long timeoutInMilliseconds)
            throws RaceLogRaceTrackerExistsException, Exception {
        RaceLog raceLog = raceColumn.getRaceLog(fleet);
        assert !isRaceLogRaceTrackerAttached(service, raceLog) : new RaceLogRaceTrackerExistsException(
                leaderboard.getName() + " - " + raceColumn.getName() + " - " + fleet.getName());

        Regatta regatta = regattaToAddTo == null ? null : service.getRegatta(regattaToAddTo);
        RaceLogConnectivityParams params = new RaceLogConnectivityParams(service, regatta, raceColumn, fleet,
                leaderboard, delayToLiveInMillis, domainFactory);
        return service.addRace(regattaToAddTo, params, timeoutInMilliseconds);
    }

    @Override
    public void denoteRaceForRaceLogTracking(RacingEventService service, Leaderboard leaderboard,
            RaceColumn raceColumn, Fleet fleet, String raceName) throws NotDenotableForRaceLogTrackingException {
        BoatClass boatClass = null;
        if (leaderboard instanceof RegattaLeaderboard) {
            RegattaLeaderboard rLeaderboard = (RegattaLeaderboard) leaderboard;
            boatClass = rLeaderboard.getRegatta().getBoatClass();
        } else {
            if (!Util.isEmpty(raceColumn.getAllCompetitors(fleet))) {
                boatClass = findDominatingBoatClass(raceColumn.getAllCompetitors(fleet));
            } else if (!Util.isEmpty(raceColumn.getAllCompetitors())) {
                boatClass = findDominatingBoatClass(raceColumn.getAllCompetitors());
            } else if (!Util.isEmpty(leaderboard.getAllCompetitors())) {
                boatClass = findDominatingBoatClass(leaderboard.getAllCompetitors());
            } else {
                throw new NotDenotableForRaceLogTrackingException("Couldn't infer boat class, no competitors on race and leaderboard");
            }
        }
        if (raceName == null) {
            raceName = leaderboard.getName() + " " + raceColumn.getName() + " " + fleet.getName();
        }
        RaceLog raceLog = raceColumn.getRaceLog(fleet);
        assert raceLog != null : new NotDenotableForRaceLogTrackingException("No RaceLog found in place");
        if (new RaceLogTrackingStateAnalyzer(raceLog).analyze().isForTracking()) {
            throw new NotDenotableForRaceLogTrackingException("Already denoted for tracking");
        }
        RaceLogEvent event = new RaceLogDenoteForTrackingEventImpl(MillisecondsTimePoint.now(),
                service.getServerAuthor(), raceLog.getCurrentPassId(), raceName, boatClass, UUID.randomUUID());
        raceLog.add(event);
    }
    
    // implemented somewhere in domainFactory? 
    private BoatClass findDominatingBoatClass(Iterable<Competitor> allCompetitors) {
        HashMap<BoatClass, Integer> occurenceCount = new HashMap<>();
        for (Competitor competitor : allCompetitors) {
            BoatClass boatclass = competitor.getBoat().getBoatClass();
            if (occurenceCount.containsKey(boatclass) ) {
                int value = occurenceCount.get(boatclass);
                occurenceCount.put(boatclass, value + 1);
            } else {
                occurenceCount.put(boatclass, 1);
            }
        }

        Entry<BoatClass, Integer> mostFrequentEntry = occurenceCount.entrySet().iterator().next();
        for (Entry<BoatClass, Integer> entry : occurenceCount.entrySet()) {
            if (mostFrequentEntry.getValue() < entry.getValue()){
                mostFrequentEntry = entry;
            }
        }
        return mostFrequentEntry.getKey();
    }

    @Override
    public void denoteAllRacesForRaceLogTracking(final RacingEventService service, final Leaderboard leaderboard)
            throws NotDenotableForRaceLogTrackingException {
        for (RaceColumn column : leaderboard.getRaceColumns()) {
            for (Fleet fleet : column.getFleets()) {
                denoteRaceForRaceLogTracking(service, leaderboard, column, fleet, null);
            }
        }
    }

    @Override
    public boolean isRaceLogRaceTrackerAttached(RacingEventService service, RaceLog raceLog) {
        return service.getRaceTrackerById(raceLog.getId()) != null;
    }

    @Override
    public RaceLogTrackingState getRaceLogTrackingState(RacingEventService service, RaceColumn raceColumn, Fleet fleet) {
        return new RaceLogTrackingStateAnalyzer(raceColumn.getRaceLog(fleet)).analyze();
    }

    @Override
    public void copyCourse(RaceLog fromRaceLog, Set<RaceLog> toRaceLogs, SharedDomainFactory baseDomainFactory,
            RacingEventService service) {
        CourseBase course = new LastPublishedCourseDesignFinder(fromRaceLog).analyze();
        final Set<Mark> marks = new HashSet<>();
        if (course != null) {
            course.getWaypoints().forEach(wp -> Util.addAll(wp.getMarks(), marks));
        }

        for (RaceLog toRaceLog : toRaceLogs) {
            if (new RaceLogTrackingStateAnalyzer(toRaceLog).analyze().isForTracking()) {
                if (course != null) {
                    CourseBase newCourse = new CourseDataImpl("Copy of \"" + course.getName() + "\"");
                    TimePoint now = MillisecondsTimePoint.now();
                    int i = 0;
                    for (Waypoint oldWaypoint : course.getWaypoints()) {
                        newCourse.addWaypoint(i++, oldWaypoint);
                    }

                    int passId = toRaceLog.getCurrentPassId();
                    RaceLogEvent newCourseEvent = new RaceLogCourseDesignChangedEventImpl(now,
                            service.getServerAuthor(), passId, newCourse);
                    toRaceLog.add(newCourseEvent);
                }

            }
        }
    }

    @Override
    public void copyCompetitors(final RaceColumn fromRaceColumn, final Fleet fromFleet, final Iterable<Pair<RaceColumn, Fleet>> toRaces) {
        Iterable<Competitor> competitorsToCopy = fromRaceColumn.getAllCompetitors(fromFleet);
        for (Pair<RaceColumn, Fleet> toRace : toRaces) {
            final RaceColumn toRaceColumn = toRace.getA();
            final Fleet toFleet = toRace.getB();
            try {
                if (toRaceColumn.isCompetitorRegistrationInRacelogEnabled(toFleet)) {
                    toRaceColumn.registerCompetitors(competitorsToCopy, toFleet);
                } else {
                    toRaceColumn.enableCompetitorRegistrationOnRaceLog(toFleet);
                    toRaceColumn.registerCompetitors(competitorsToCopy, toFleet);
                }
            } catch (CompetitorRegistrationOnRaceLogDisabledException e1) {
                // cannot happen as we explicitly checked successfully before, or enabled it when the check failed; still produce a log documenting this strangeness:
                logger.log(Level.WARNING, "Internal error: race column "+toRaceColumn.getName()+" does not accept competitor registration although it should", e1);
            }
        }
    }

    @Override
    public void pingMark(RegattaLog log, Mark mark, GPSFix gpsFix, RacingEventService service) {
        final PingDeviceIdentifierImpl device = new PingDeviceIdentifierImpl();
        final TimePoint timePoint = gpsFix.getTimePoint();
        final RegattaLogEvent mapping = new RegattaLogDeviceMarkMappingEventImpl(timePoint,
                timePoint, service.getServerAuthor(), UUID.randomUUID(), mark, device, timePoint, timePoint);
        log.add(mapping);
        try {
            service.getGPSFixStore().storeFix(device, gpsFix);
        } catch (TransformationException | NoCorrespondingServiceRegisteredException e) {
            logger.log(Level.WARNING, "Could not ping mark " + mark);
        }
    }

    @Override
    public void removeDenotationForRaceLogTracking(RacingEventService service, RaceLog raceLog) {
        RaceLogEvent denoteForTrackingEvent = new LastEventOfTypeFinder<>(raceLog, true,
                RaceLogDenoteForTrackingEvent.class).analyze();
        RaceLogEvent startTrackingEvent = new LastEventOfTypeFinder<>(raceLog, true, RaceLogStartTrackingEvent.class)
                .analyze();
        try {
            raceLog.revokeEvent(service.getServerAuthor(), denoteForTrackingEvent, "remove denotation");
            raceLog.revokeEvent(service.getServerAuthor(), startTrackingEvent,
                    "reset start time upon removing denotation");
        } catch (NotRevokableException e) {
            logger.log(Level.WARNING, "could not remove denotation by adding RevokeEvents", e);
        }
    }

    private MailService getMailService() {
        ServiceReference<MailService> ref = Activator.getContext().getServiceReference(MailService.class);
        if (ref == null) {
            logger.warning("No file storage management service registered");
            return null;
        }
        return Activator.getContext().getService(ref);
    }

    @Override
    public void inviteCompetitorsForTrackingViaEmail(Event event, Leaderboard leaderboard,
            String serverUrlWithoutTrailingSlash, Set<Competitor> competitors, Locale locale) throws MailException {
        StringBuilder occuredExceptions = new StringBuilder();

        for (Competitor competitor : competitors) {
            final String toAddress = competitor.getEmail();
            if (toAddress != null) {
                String leaderboardName = leaderboard.getName();
                String competitorName = competitor.getName();

                String url = DeviceMappingConstants.getDeviceMappingForRegattaLogUrl(serverUrlWithoutTrailingSlash,
                        event.getId().toString(), leaderboardName, DeviceMappingConstants.URL_COMPETITOR_ID_AS_STRING,
                        competitor.getId().toString(), NonGwtUrlHelper.INSTANCE);
                try {
                    sendInvitationEmail(locale, toAddress, leaderboardName, competitorName, url);
                } catch (MailException e) {
                    occuredExceptions.append(e.getMessage() + "\r\n");
                }
            }
        }
        if (!(occuredExceptions.length() == 0)) {
            throw new MailException(occuredExceptions.toString());
        }
    }

    private void sendInvitationEmail(Locale locale, final String toAddress, String leaderboardName, String invitee,
            String url) throws MailException {
        String subject = String.format("%s %s",
                RaceLogTrackingI18n.STRING_MESSAGES.get(locale, "trackingInvitationFor"), invitee);

        // taken from http://www.tutorialspoint.com/javamail_api/javamail_api_send_inlineimage_in_email.htm
        BodyPart messageTextPart = new MimeBodyPart();
        String htmlText = String.format("<h1>%s %s</h1>" + "<p>%s <b>%s</b></p>"
                + "<img src=\"cid:image\" title=\"%s\"><br/><b>%s</b>: <a href=\"%s\">%s</a><br/><b>%s</b>: <a href=\"%s\">%s</a>",
                RaceLogTrackingI18n.STRING_MESSAGES.get(locale, "welcomeTo"), leaderboardName,
                RaceLogTrackingI18n.STRING_MESSAGES.get(locale, "scanQRCodeOrVisitUrlToRegisterAs"), invitee,
                url, RaceLogTrackingI18n.STRING_MESSAGES.get(locale, "iOSUsers"),
                IOS_DEEP_LINK_PREFIX+url, RaceLogTrackingI18n.STRING_MESSAGES.get(locale, "alternativelyVisitThisLink"),
                RaceLogTrackingI18n.STRING_MESSAGES.get(locale, "androidUsers"),
                url, RaceLogTrackingI18n.STRING_MESSAGES.get(locale, "alternativelyVisitThisLink"));

        try {
            messageTextPart.setContent(htmlText, "text/html");

            BodyPart messageImagePart = new MimeBodyPart();
            InputStream imageIs = QRCodeGenerationUtil.create(url, 250);
            DataSource imageDs = new ByteArrayDataSource(imageIs, "image/png");
            messageImagePart.setDataHandler(new DataHandler(imageDs));
            messageImagePart.setHeader("Content-ID", "<image>");

            MimeMultipart multipart = new MimeMultipart();
            multipart.addBodyPart(messageTextPart);
            multipart.addBodyPart(messageImagePart);

            getMailService().sendMail(toAddress, subject, multipart);
        } catch (MessagingException | MailException | WriterException | IOException e) {
            logger.log(Level.SEVERE, "Error trying to send mail to " + invitee + " with e-mail address " + toAddress, e);
            throw new MailException(e.getMessage());
        }
    }

    @Override
    public void inviteBuoyTenderViaEmail(Event event, Leaderboard leaderboard, String serverUrlWithoutTrailingSlash,
            String emails, Locale locale) throws MailException {

        StringBuilder occuredExceptions = new StringBuilder();

        String[] emailArray = emails.split(",");
        String leaderboardName = leaderboard.getName();

        String eventId = event.getId().toString();

        // http://<host>/buoy-tender/checkin?event_id=<event-id>&leaderboard_name=<leaderboard-name>
        String url = DeviceMappingConstants.getBuoyTenderInvitationUrl(serverUrlWithoutTrailingSlash, leaderboardName,
                eventId, NonGwtUrlHelper.INSTANCE);
        for (String toAddress : emailArray) {
            try {
                sendInvitationEmail(locale, toAddress, leaderboardName,
                        RaceLogTrackingI18n.STRING_MESSAGES.get(locale, "buoyTender"), url);
            } catch (MailException e) {
                occuredExceptions.append(e.getMessage() + "\r\n");
            }
        }

        if (!(occuredExceptions.length() == 0)) {
            throw new MailException(occuredExceptions.toString());
        }
    }
}
