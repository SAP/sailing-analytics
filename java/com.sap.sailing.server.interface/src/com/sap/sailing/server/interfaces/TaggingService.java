package com.sap.sailing.server.interfaces;

import java.util.List;

import org.apache.shiro.authz.AuthorizationException;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.common.tagging.TagAlreadyExistsException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sse.common.TimePoint;

// TODO: use document settings id for tags/tag-buttons/... as race identifier
/**
 * This service is used to perform all CRUD operations on {@link TagDTO tags} and is used by the
 * {@link com.sap.sailing.server.gateway.jaxrs.api.TagsResource REST API} for mobile apps as well as the GWT client via
 * the {@link com.sap.sailing.gwt.ui.server.SailingServiceImpl SailingService}.
 * 
 * @author Henri Kohlberg
 */
public interface TaggingService {

    /**
     * Saves given properties as tag for given race. Checks if all parameters are valid and all required parameters are
     * set.
     * 
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param tag
     *            title of tag, must <b>NOT</b> be <code>null</code>
     * @param comment
     *            optional comment of tag
     * @param hiddenInfo TODO
     * @param imageURL
     *            optional image URL of tag
     * @param resizedImageURL
     *            optional resized image URL of tag image
     * @param visibleForPublic
     *            when set to <code>true</code>, tag will be saved in
     *            {@link com.sap.sailing.domain.abstractlog.race.RaceLog RaceLog} (visible for every user), otherwise
     *            tag will be saved in {@link com.sap.sse.security.interfaces.UserStore UserStore} (only visible for the creator)
     * @param raceTimepoint
     *            timepoint in race when user created tag, must <b>NOT</b> be <code>null</code>
     * @throws AuthorizationException
     *             thrown if user is not logged in or is missing permissions
     * @throws IllegalArgumentException
     *             thrown if one of the required parameters has an invalid value
     * @throws RaceLogNotFoundException
     *             thrown if racelog cannot be found (e.g. when <code>leaderboardName</code>,
     *             <code>raceColumnName</code> or <code>fleetName</code> are missing)
     * @throws ServiceNotFoundException
     *             thrown if security service cannot be found
     * @throws TagAlreadyExistsException
     *             thrown if tag already exists
     */
    void addTag(String leaderboardName, String raceColumnName, String fleetName, String tag, String comment,
            String hiddenInfo, String imageURL, String resizedImageURL, boolean visibleForPublic, TimePoint raceTimepoint) throws AuthorizationException,
            IllegalArgumentException, RaceLogNotFoundException, ServiceNotFoundException, TagAlreadyExistsException;

    /**
     * Removes public {@link TagDTO tag} from {@link com.sap.sailing.domain.abstractlog.race.RaceLog RaceLog} and
     * private {@link TagDTO tag} from {@link com.sap.sse.security.interfaces.UserStore UserStore}.
     * 
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param tag
     *            tag to remove
     * @throws AuthorizationException
     *             thrown if user is not logged in or is missing permissions
     * @throws IllegalArgumentException
     *             thrown if one of the required parameters has an invalid value
     * @throws NotRevokableException
     *             thrown if tag is public and not revokable from racelog
     * @throws RaceLogNotFoundException
     *             thrown if racelog cannot be found (e.g. when <code>leaderboardName</code>,
     *             <code>raceColumnName</code> or <code>fleetName</code> are missing)
     * @throws ServiceNotFoundException
     *             thrown if security service cannot be found
     */
    void removeTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tag)
            throws AuthorizationException, IllegalArgumentException, NotRevokableException, RaceLogNotFoundException,
            ServiceNotFoundException;

    /**
     * Updates given <code>tagToUpdate</code> with the given parameters <code>tag</code>, <code>comment</code>,
     * <code>imageURL</code> and <code>isPublic</code>.
     * 
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param tagToUpdate
     *            tag to be updated
     * @param tag
     *            new tag title
     * @param comment
     *            new comment
     * @param hiddenInfo TODO
     * @param imageURL
     *            optional image URL of tag
     * @param resizedImageURL
     *            optional resized image URL of tag image
     * @param visibleForPublic
     *            new privacy status
     * @throws AuthorizationException
     *             thrown if user is not logged in or is missing permissions
     * @throws IllegalArgumentException
     *             thrown if one of the required parameters has an invalid value
     * @throws NotRevokableException
     *             thrown if tag is public and not revokable from racelog
     * @throws TagAlreadyExistsException
     *             thrown if tag already exists
     * @throws RaceLogNotFoundException
     *             thrown if racelog cannot be found (e.g. when <code>leaderboardName</code>,
     *             <code>raceColumnName</code> or <code>fleetName</code> are missing)
     * @throws ServiceNotFoundException
     *             thrown if security service cannot be found
     */
    void updateTag(String leaderboardName, String raceColumnName, String fleetName, TagDTO tagToUpdate, String tag,
            String comment, String hiddenInfo, String imageURL, String resizedImageURL, boolean visibleForPublic)
            throws AuthorizationException, IllegalArgumentException, NotRevokableException, RaceLogNotFoundException,
            ServiceNotFoundException, TagAlreadyExistsException;

    /**
     * Returns all private and public tags of specified race since timepoint <code>searchSince</code>.
     * 
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param searchSince
     *            tags will only be returned if they got created after this {@link Timepoint time point}. Set this value
     *            to <code>null</code> to return all found tags independant of their creation date.
     * @param returnRevokedTags
     *            if set to <code>true</code> only valid tags will be returned, if set to <code>false</code> revoked
     *            tags will be returned too
     * @return list of {@link TagDTO tags}, empty list in case an error occurs or there are no tags available but
     *         <b>never null</b>!
     */
    List<TagDTO> getTags(String leaderboardName, String raceColumnName, String fleetName, TimePoint searchSince,
            boolean returnRevokedTags) throws RaceLogNotFoundException, ServiceNotFoundException;
    
    /**
     * Same as {@link #getTags(String, String, String, TimePoint, boolean)}, only that instead of the object names
     * for leaderboard, race column and fleet the objects themselves are passed, accelerating the search for the
     * matching {@link RaceLog} and therefore accelerating the search for public tags.
     */
    List<TagDTO> getTags(Leaderboard leaderboard, RaceColumn raceColumn, Fleet fleet, TimePoint searchSince,
            boolean returnRevokedTags) throws RaceLogNotFoundException, ServiceNotFoundException;

    /**
     * Returns all public tags for the specified race.
     * 
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @return list of {@link TagDTO tags}, empty list in case an error occurs or there are no tags available but
     *         <b>never null</b>!
     * @param searchSince
     *            tags will only be returned if they got created after this {@link TimePoint time point}. Set this value
     *            to <code>null</code> to return all found tags independent of their creation date.
     * @param returnRevokedTags
     *            if set to <code>true</code> only valid tags will be returned, if set to <code>false</code> revoked
     *            tags will be returned also
     * @throws RaceLogNotFoundException
     *             thrown if racelog cannot be found (e.g. when <code>leaderboardName</code>,
     *             <code>raceColumnName</code> or <code>fleetName</code> are missing)
     */
    List<TagDTO> getPublicTags(String leaderboardName, String raceColumnName, String fleetName, TimePoint searchSince,
            boolean returnRevokedTags) throws RaceLogNotFoundException;
    
    /**
     * Returns all public tags since the given <code>searchSince</code> for the specified race.
     * 
     * @param raceIdentifier
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param searchSince
     *            tags will only be returned if they got created after this time point
     * @return list of {@link TagDTO tags}, empty list in case an error occurs or there are no tags available but
     *         <b>never null</b>!
     * @throws RaceLogNotFoundException
     */
    List<TagDTO> getPublicTags(RegattaAndRaceIdentifier raceIdentifier, TimePoint searchSince);

    /**
     * Returns all private tags of current user for the specified race.
     * 
     * @param leaderboardName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param raceColumnName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @param fleetName
     *            required to identify {@link RaceLog}, must <b>NOT</b> be <code>null</code>
     * @return list of {@link TagDTO tags}, empty list in case there is no logged-in user or an error occurs or there
     *         are no tags available but <b>never null</b>!
     * @throws ServiceNotFoundException
     *             thrown if security service cannot be found
     */
    List<TagDTO> getPrivateTags(String leaderboardName, String raceColumnName, String fleetName)
            throws ServiceNotFoundException;
}
