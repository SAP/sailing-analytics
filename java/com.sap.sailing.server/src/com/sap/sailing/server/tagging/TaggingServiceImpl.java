package com.sap.sailing.server.tagging;

import java.util.ArrayList;
import java.util.List;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogTagEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogTagEventImpl;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.security.Permission;
import com.sap.sailing.domain.common.security.Permission.Mode;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.common.tagging.TagAlreadyExistsException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.impl.Activator;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.TaggingService;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.SecurityService;
import com.sap.sse.util.ServiceTrackerFactory;

public class TaggingServiceImpl implements TaggingService {

    private final ServiceTracker<SecurityService, SecurityService> securityServiceTracker;
    private final RacingEventService racingService;
    private final TagDTODeSerializer serializer;

    public TaggingServiceImpl(RacingEventService racingService) {
        this.racingService = racingService;
        serializer = new TagDTODeSerializer();
        securityServiceTracker = ServiceTrackerFactory.createAndOpen(Activator.getContext(), SecurityService.class);
    }

    /**
     * Returns current username.
     * 
     * @throws AuthorizationException
     * @return username of current user
     */
    private String getCurrentUsername() throws AuthorizationException {
        Object principal = SecurityUtils.getSubject().getPrincipal();
        if (principal == null) {
            throw new AuthorizationException();
        }
        return principal.toString();
    }

    /**
     * Returns instance of {@link SecurityService} to access the user store.
     * 
     * @return instance of {@link SecurityService}
     * @throws ServiceNotFoundException
     */
    private SecurityService getSecurityService() throws ServiceNotFoundException {
        SecurityService securityService;
        // securityServiceTracker is null in non-OSGi environment (local JUnit tests) => use static reference of
        // security service, otherwise use OSGi service tracker
        if (securityServiceTracker != null) {
            securityService = securityServiceTracker.getService();
        } else {
            securityService = com.sap.sse.security.impl.Activator.getSecurityService();
        }
        if (securityService == null) {
            throw new ServiceNotFoundException("Security service not found!");
        }
        return securityService;
    }

    private void addPublicTag(String leaderboardName, String raceColumnName, String fleetName, String tag,
            String comment, String imageURL, String resizedImageURL, TimePoint raceTimepoint)
            throws RaceLogNotFoundException, TagAlreadyExistsException {
        // TODO: As soon as permission-vertical branch got merged into master, apply
        // new permission system at this permission check (see bug 4104, comment 9)
        // functionality: Check if user has the permission to add RaceLogEvents to RaceLog.
        SecurityUtils.getSubject()
                .checkPermission(Permission.LEADERBOARD.getStringPermissionForObjects(Mode.UPDATE, leaderboardName));
        RaceLog raceLog = racingService.getRaceLog(leaderboardName, raceColumnName, fleetName);
        if (raceLog == null) {
            throw new RaceLogNotFoundException();
        }
        // check if tag already exists
        List<TagDTO> publicTags = getPublicTags(leaderboardName, raceColumnName, fleetName, null, false);
        for (TagDTO publicTag : publicTags) {
            if (publicTag.equals(tag, comment, imageURL, resizedImageURL, true,
                    racingService.getServerAuthor().getName(), raceTimepoint)) {
                throw new TagAlreadyExistsException();
            }
        }
        raceLog.add(new RaceLogTagEventImpl(tag, comment, imageURL, resizedImageURL, raceTimepoint,
                racingService.getServerAuthor(), raceLog.getCurrentPassId()));
    }

    private void addPrivateTag(String leaderboardName, String raceColumnName, String fleetName, String tag,
            String comment, String imageURL, String resizedImageURL, TimePoint raceTimepoint)
            throws AuthorizationException, ServiceNotFoundException, TagAlreadyExistsException {
        SecurityService securityService = getSecurityService();
        String username = getCurrentUsername();
        TagDTODeSerializer serializer = new TagDTODeSerializer();
        String key = serializer.generateUniqueKey(leaderboardName, raceColumnName, fleetName);
        String privateTagsJson = securityService.getPreference(username, key);
        List<TagDTO> privateTags = serializer.deserializeTags(privateTagsJson);
        TagDTO tagToAdd = new TagDTO(tag, comment, imageURL, resizedImageURL, false, username, raceTimepoint,
                MillisecondsTimePoint.now());
        if (privateTags.contains(tagToAdd)) {
            throw new TagAlreadyExistsException();
        }
        privateTags.add(tagToAdd);
        securityService.setPreference(username, key, serializer.serializeTags(privateTags));
    }

    private void removePublicTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tag)
            throws AuthorizationException, NotRevokableException, RaceLogNotFoundException {
        RaceLog raceLog = racingService.getRaceLog(leaderboardName, raceColumnName, fleetName);
        if (raceLog == null) {
            throw new RaceLogNotFoundException();
        }
        ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(racingService, raceLog);
        Iterable<RaceLogTagEvent> foundTagEvents = raceState.getTagEvents();
        for (RaceLogTagEvent tagEvent : foundTagEvents) {
            if (tagEvent.getRevokedAt() != null) {
                continue;
            } else if (tagEvent.getTag().equals(tag.getTag()) && tagEvent.getComment().equals(tag.getComment())
                    && tagEvent.getImageURL().equals(tag.getImageURL())
                    && tagEvent.getUsername().equals(tag.getUsername())
                    && tagEvent.getLogicalTimePoint().equals(tag.getRaceTimepoint())) {

                // TODO: As soon as permission-vertical branch got merged into master, apply
                // new permission system at this permission check (see bug 4104, comment 9)
                // functionality: Check if user has the permission to delete tag from RaceLog (same user or
                // admin).
                Subject subject = SecurityUtils.getSubject();
                subject.checkPermission(
                        Permission.LEADERBOARD.getStringPermissionForObjects(Mode.UPDATE, leaderboardName));
                if (!((subject.getPrincipal() != null && subject.getPrincipal().equals(tag.getUsername()))
                        || subject.hasRole("admin"))) {
                    throw new AuthorizationException();
                }
                raceLog.revokeEvent(tagEvent.getAuthor(), tagEvent, "Revoked");
                break;
            }
        }
    }

    private void removePrivateTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tag)
            throws AuthorizationException, ServiceNotFoundException {
        String username = getCurrentUsername();
        List<TagDTO> privateTags = getPrivateTags(leaderboardName, raceColumnName, fleetName);
        privateTags.remove(tag);
        SecurityService securityService = getSecurityService();
        String key = serializer.generateUniqueKey(leaderboardName, raceColumnName, fleetName);
        if (username != null && securityService != null && key != null) {
            if (privateTags.isEmpty()) {
                securityService.unsetPreference(username, key);
            } else {
                securityService.setPreference(username, key, serializer.serializeTags(privateTags));
            }
        }
    }

    @Override
    public void addTag(String leaderboardName, String raceColumnName, String fleetName, String tag, String comment,
            String imageURL, String resizedImageURL, boolean visibleForPublic, TimePoint raceTimepoint)
            throws AuthorizationException, IllegalArgumentException, RaceLogNotFoundException, ServiceNotFoundException,
            TagAlreadyExistsException {
        // prefill optional parameters
        comment = comment == null ? "" : comment;
        imageURL = imageURL == null ? "" : imageURL;
        resizedImageURL = resizedImageURL == null ? "" : resizedImageURL;
        if (tag == null || tag.isEmpty()) {
            throw new IllegalArgumentException("Tag may not be empty!");
        }
        if (raceTimepoint == null || raceTimepoint.asMillis() == 0) {
            // TODO: Check if timepoint is near start/end of race (+/- x%)
            throw new IllegalArgumentException("Timepoint may not be empty!");
        }
        if (visibleForPublic) {
            addPublicTag(leaderboardName, raceColumnName, fleetName, tag, comment, imageURL, resizedImageURL,
                    raceTimepoint);
        } else {
            addPrivateTag(leaderboardName, raceColumnName, fleetName, tag, comment, imageURL, resizedImageURL,
                    raceTimepoint);
        }
    }

    @Override
    public void removeTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tag)
            throws AuthorizationException, IllegalArgumentException, NotRevokableException, RaceLogNotFoundException,
            ServiceNotFoundException {
        if (tag == null) {
            throw new IllegalArgumentException("Tag may not be empty!");
        }
        if (tag.isVisibleForPublic()) {
            removePublicTag(leaderboardName, raceColumnName, fleetName, tag);
        } else {
            removePrivateTag(leaderboardName, raceColumnName, fleetName, tag);
        }
    }

    @Override
    public void updateTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tagToUpdate,
            String tag, String comment, String imageURL, String resizedImageURL, boolean visibleForPublic)
            throws AuthorizationException, IllegalArgumentException, NotRevokableException, RaceLogNotFoundException,
            ServiceNotFoundException, TagAlreadyExistsException {
        String username = getCurrentUsername();
        if (username != null && tagToUpdate.getUsername().equals(username)) {
            removeTag(leaderboardName, raceColumnName, fleetName, tagToUpdate);
            addTag(leaderboardName, raceColumnName, fleetName, tag, comment, imageURL, resizedImageURL,
                    visibleForPublic, tagToUpdate.getRaceTimepoint());

        }
    }

    @Override
    public List<TagDTO> getTags(String leaderboardName, String raceColumnName, String fleetName, TimePoint searchSince,
            boolean returnRevokedTags) throws RaceLogNotFoundException, ServiceNotFoundException {
        final List<TagDTO> result = new ArrayList<TagDTO>();
        Util.addAll(getPublicTags(leaderboardName, raceColumnName, fleetName, searchSince, returnRevokedTags), result);
        Util.addAll(getPrivateTags(leaderboardName, raceColumnName, fleetName), result);
        return result;
    }

    @Override
    public List<TagDTO> getTags(Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet, TimePoint searchSince,
            boolean returnRevokedTags) throws RaceLogNotFoundException, ServiceNotFoundException {
        final List<TagDTO> result = new ArrayList<TagDTO>();
        Util.addAll(getPublicTags(raceColumn.getRaceLog(fleet), searchSince, returnRevokedTags), result);
        try {
            Util.addAll(getPrivateTags(leaderboard.getName(), raceColumn.getName(), fleet.getName()), result);
        } catch (AuthorizationException e) {
            // user is not logged in, may fail while unit testing because no user is logged in
        }
        return result;
    }

    @Override
    public List<TagDTO> getPublicTags(String leaderboardName, String raceColumnName, String fleetName,
            TimePoint searchSince, boolean returnRevokedTags) throws RaceLogNotFoundException {
        RaceLog raceLog = racingService.getRaceLog(leaderboardName, raceColumnName, fleetName);
        return getPublicTags(raceLog, searchSince, returnRevokedTags);
    }

    @Override
    public List<TagDTO> getPublicTags(RaceLog raceLog, TimePoint searchSince, boolean returnRevokedTags)
            throws RaceLogNotFoundException {
        final List<TagDTO> result = new ArrayList<TagDTO>();
        if (raceLog == null) {
            throw new RaceLogNotFoundException();
        }
        ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(racingService, raceLog);
        Iterable<RaceLogTagEvent> foundTagEvents = raceState.getTagEvents();
        for (RaceLogTagEvent tagEvent : foundTagEvents) {
            // check if revoked tags should be returned or tag is not revoked
            if (returnRevokedTags || (!returnRevokedTags && tagEvent.getRevokedAt() == null)) {
                // check if tag got created/revoked after searchSince or searchSince is null
                if (searchSince == null || (searchSince != null && tagEvent.getCreatedAt().after(searchSince)
                        && (tagEvent.getRevokedAt() == null
                                || (tagEvent.getRevokedAt() != null && tagEvent.getRevokedAt().after(searchSince))))) {
                    result.add(new TagDTO(tagEvent.getTag(), tagEvent.getComment(), tagEvent.getImageURL(),
                            tagEvent.getResizedImageURL(), true, tagEvent.getUsername(), tagEvent.getLogicalTimePoint(),
                            tagEvent.getCreatedAt(), tagEvent.getRevokedAt()));
                }
            }
        }
        return result;
    }

    @Override
    public List<TagDTO> getPublicTags(RegattaAndRaceIdentifier raceIdentifier, TimePoint searchSince) {
        final List<TagDTO> result = new ArrayList<TagDTO>();
        TrackedRace trackedRace = racingService.getExistingTrackedRace(raceIdentifier);
        Iterable<RaceLog> raceLogs = trackedRace.getAttachedRaceLogs();
        for (RaceLog raceLog : raceLogs) {
            ReadonlyRaceState raceState = ReadonlyRaceStateImpl.getOrCreate(racingService, raceLog);
            Iterable<RaceLogTagEvent> foundTagEvents = raceState.getTagEvents();
            for (RaceLogTagEvent tagEvent : foundTagEvents) {
                if ((searchSince == null && tagEvent.getRevokedAt() == null)
                        || (searchSince != null && tagEvent.getRevokedAt() == null
                                && tagEvent.getCreatedAt().after(searchSince))
                        || (searchSince != null && tagEvent.getRevokedAt() != null
                                && tagEvent.getRevokedAt().after(searchSince))) {
                    result.add(new TagDTO(tagEvent.getTag(), tagEvent.getComment(), tagEvent.getImageURL(),
                            tagEvent.getResizedImageURL(), true, tagEvent.getUsername(), tagEvent.getLogicalTimePoint(),
                            tagEvent.getCreatedAt(), tagEvent.getRevokedAt()));
                }
            }
        }
        return result;
    }

    @Override
    public List<TagDTO> getPrivateTags(String leaderboardName, String raceColumnName, String fleetName)
            throws AuthorizationException, ServiceNotFoundException {
        final List<TagDTO> result = new ArrayList<TagDTO>();
        if (SecurityUtils.getSubject().getPrincipal() != null) {
            String key = serializer.generateUniqueKey(leaderboardName, raceColumnName, fleetName);
            String privateTagsJson = getSecurityService().getPreference(getCurrentUsername(), key);
            List<TagDTO> privateTags = serializer.deserializeTags(privateTagsJson);
            result.addAll(privateTags);
        }
        return result;
    }
}
