package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import javax.ws.rs.core.UriInfo;

import org.apache.shiro.authz.AuthorizationException;
import org.json.simple.JSONArray;

import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.racelog.RaceLogServletConstants;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.TagAlreadyExistsException;
import com.sap.sailing.server.interfaces.TaggingService;
import com.sap.sailing.server.tagging.TagDTODeSerializer;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

@Path("/v1/{" + RaceLogServletConstants.PARAMS_LEADERBOARD_NAME + "}/{"
        + RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME + "}/{" + RaceLogServletConstants.PARAMS_RACE_FLEET_NAME
        + "}/tags")
public class TagsResource extends AbstractSailingServerResource {

    private static final Logger logger = Logger.getLogger(TagsResource.class.getName());
    private static final String APPLICATION_JSON_UTF8 = MediaType.APPLICATION_JSON + ";charset=UTF-8";
    private static final String TEXT_PLAIN_UTF8 = MediaType.TEXT_PLAIN + ";charset=UTF-8";

    private final TagDTODeSerializer serializer;

    public TagsResource() {
        serializer = new TagDTODeSerializer();
    }

    /**
     * Loads public tags from {@link com.sap.sailing.domain.abstractlog.race.RaceLog RaceLog} and private tags from
     * {@link com.sap.sse.security.interfaces.UserStore UserStore} in case current user is logged in. Only loads tags when
     * parameters <code>leaderboardName</code>, <code>raceColumnName</code> and <code>fleetName</code> can identify
     * {@link com.sap.sailing.domain.abstractlog.race.RaceLog RaceLog}.
     * 
     * @param visibility
     *            <code>public</code> will load only public tags, <code>private</code> will load only private tags,
     *            <code>both</code> (default) will load public and private tags
     * @param revoked
     *            <code>true</code> will also return already deleted public tags, <code>false</code> (default) will only
     *            return non-revoked tags
     * @return status 200 (including tags) if request was successful, otherwise 400 (bad request) or 500 (internal
     *         server error)
     */
    // TODO: Add searchSince
    @GET
    @Produces({ APPLICATION_JSON_UTF8, TEXT_PLAIN_UTF8 })
    public Response getTags(@PathParam(RaceLogServletConstants.PARAMS_LEADERBOARD_NAME) String leaderboardName,
            @PathParam(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME) String raceColumnName,
            @PathParam(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME) String fleetName,
            @DefaultValue("both") @QueryParam("visibility") String visibility) {
        Response response;
        final TaggingService taggingService = getService().getTaggingService();
        final List<TagDTO> tags = new ArrayList<TagDTO>();
        final boolean lookForPublic = visibility.equalsIgnoreCase("both") || visibility.equalsIgnoreCase("public");
        final boolean lookForPrivate = visibility.equalsIgnoreCase("both") || visibility.equalsIgnoreCase("private");
        try {
            if (lookForPublic) {
                Util.addAll(taggingService.getPublicTags(leaderboardName, raceColumnName, fleetName, null, false), tags);
            }
            if (lookForPrivate) {
                try {
                    Util.addAll(taggingService.getPrivateTags(leaderboardName, raceColumnName, fleetName), tags);
                } catch (AuthorizationException e) {
                    // do nothing when user is not logged in
                }
            }
            JSONArray jsonTags = serializer.serialize(tags);
            response = Response.ok(streamingOutput(jsonTags)).type(APPLICATION_JSON_UTF8).build();
        } catch (RaceLogNotFoundException e) {
            response = Response.status(Status.BAD_REQUEST).type(TEXT_PLAIN_UTF8).build();
        } catch (Exception e) {
            logger.warning("Could not load tags! " + e.getMessage());
            response = Response.status(Status.INTERNAL_SERVER_ERROR).type(TEXT_PLAIN_UTF8).build();
        }
        return response;
    }

    /**
     * Saves new tag.
     * 
     * @param tag
     *            may not be empty
     * @param visible
     *            default is <code>false</code>
     * 
     * @return status 201 (created) if creation was successful, otherwise 400 (bad request) 401 (unauthorized) or 500
     *         (internal server error)
     * @see TaggingService#addTag(String, String, String, String, String, String, boolean, TimePoint)
     */
    @POST
    @Produces({ TEXT_PLAIN_UTF8 })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response createTag(@Context UriInfo uriInfo,
            @PathParam(RaceLogServletConstants.PARAMS_LEADERBOARD_NAME) String leaderboardName,
            @PathParam(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME) String raceColumnName,
            @PathParam(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME) String fleetName, @FormParam("tag") String tag,
            @FormParam("comment") String comment, @FormParam("hiddenInfo") String hiddenInfo, @FormParam("image") String imageURL,
            @FormParam("resizedImage") String resizedImageURL, @FormParam("public") boolean visibleForPublic,
            @FormParam("raceTimepoint") long raceTimepoint) {
        Response response;
        final TaggingService taggingService = getService().getTaggingService();
        try {
            if (visibleForPublic) {
                getSecurityService().checkCurrentUserUpdatePermission(getService().getLeaderboardByName(leaderboardName));
            }
            taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, hiddenInfo, imageURL,
                    resizedImageURL, visibleForPublic, new MillisecondsTimePoint(raceTimepoint));
            // replication works either through SecurityService preferences for private tags,
            // or through RaceLog replication
            response = Response.created(uriInfo.getRequestUri()).build();
        } catch (IllegalArgumentException | RaceLogNotFoundException | TagAlreadyExistsException e) {
            response = Response.status(Status.BAD_REQUEST).type(TEXT_PLAIN_UTF8).build();
        } catch (AuthorizationException e) {
            response = Response.status(Status.UNAUTHORIZED).type(TEXT_PLAIN_UTF8).build();
        } catch (Exception e) {
            logger.warning("Could not save tag! " + e.getMessage());
            response = Response.status(Status.INTERNAL_SERVER_ERROR).type(TEXT_PLAIN_UTF8).build();
        }
        return response;
    }

    /**
     * Removes tag given as json string.
     * 
     * @return status 204 (no content) if deletion was successful, otherwise 400 (bad request), 401 (unauthorized) or
     *         500 (internal server error)
     * @see TagDTO
     * @see TaggingService#removeTag(String, String, String, TagDTO)
     */
    @DELETE
    @Produces({ TEXT_PLAIN_UTF8 })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response deleteTag(@PathParam(RaceLogServletConstants.PARAMS_LEADERBOARD_NAME) String leaderboardName,
            @PathParam(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME) String raceColumnName,
            @PathParam(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME) String fleetName,
            @FormParam("tag_json") String tagJson) {
        Response response;
        TagDTO tagToRemove = serializer.deserializeTag(tagJson);
        TaggingService taggingService = getService().getTaggingService();
        try {
            taggingService.removeTag(leaderboardName, raceColumnName, fleetName, tagToRemove); // FIXME what about replication???
            response = Response.noContent().build();
        } catch (IllegalArgumentException | NotRevokableException | RaceLogNotFoundException e) {
            response = Response.status(Status.BAD_REQUEST).type(TEXT_PLAIN_UTF8).build();
        } catch (AuthorizationException e) {
            response = Response.status(Status.UNAUTHORIZED).type(TEXT_PLAIN_UTF8).build();
        } catch (Exception e) {
            logger.warning("Could not remove tag! " + e.getMessage());
            response = Response.status(Status.INTERNAL_SERVER_ERROR).type(TEXT_PLAIN_UTF8).build();
        }
        return response;
    }

    /**
     * Updates tag. When optional parameters <code>tag</code>, <code>comment</code>, <code>image</code> or
     * <code>public</code> are missing, old values of <code>tagJson</code> will be used for the missing attributes
     * instead.
     * 
     * @param tag
     *            may not be empty
     * 
     * @return status 204 (no content) if update was successful, otherwise 400 (bad request), 401 (unauthorized) or 500
     *         (internal server error)
     * @see TagDTO
     * @see TaggingService#updateTag(String, String, String, TagDTO, String, String, String, boolean)
     */
    @PUT
    @Produces({ TEXT_PLAIN_UTF8 })
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response updateTag(@PathParam(RaceLogServletConstants.PARAMS_LEADERBOARD_NAME) String leaderboardName,
            @PathParam(RaceLogServletConstants.PARAMS_RACE_COLUMN_NAME) String raceColumnName,
            @PathParam(RaceLogServletConstants.PARAMS_RACE_FLEET_NAME) String fleetName,
            @FormParam("tag_json") String tagJson, @FormParam("tag") String tagParam,
            @FormParam("comment") String commentParam, @FormParam("hiddenInfo") String hiddenInfo, @FormParam("image") String imageURLParam,
            @FormParam("resizedImage") String resizedImageURLParam, @FormParam("public") String visibleForPublicParam) {
        Response response;
        final TagDTO tagToUpdate = serializer.deserializeTag(tagJson);
        final TaggingService taggingService = getService().getTaggingService();
        // only call update method when any of the parameters needs to be changed
        if (tagParam != null || commentParam != null || imageURLParam != null || visibleForPublicParam != null) {
            // keep old values when no new values are provided
            String tag = (tagParam == null ? tagToUpdate.getTag() : tagParam);
            String comment = (commentParam == null ? tagToUpdate.getComment() : commentParam);
            String imageURL = (imageURLParam == null ? tagToUpdate.getImageURL() : imageURLParam);
            String resizedImageURL = (resizedImageURLParam == null ? tagToUpdate.getResizedImageURL()
                    : resizedImageURLParam);
            boolean visibleForPublic = (visibleForPublicParam == null ? tagToUpdate.isVisibleForPublic()
                    : visibleForPublicParam.equalsIgnoreCase("true") ? true : false);
            try {
                if (visibleForPublic) {
                    getSecurityService().checkCurrentUserUpdatePermission(getService().getLeaderboardByName(leaderboardName));
                }
                taggingService.updateTag(leaderboardName, raceColumnName, fleetName, tagToUpdate, tag, comment,
                        hiddenInfo, imageURL, resizedImageURL, visibleForPublic); // FIXME what about replication???
                response = Response.noContent().build();
            } catch (IllegalArgumentException | NotRevokableException | RaceLogNotFoundException
                    | TagAlreadyExistsException e) {
                response = Response.status(Status.BAD_REQUEST).type(TEXT_PLAIN_UTF8).build();
            } catch (AuthorizationException e) {
                response = Response.status(Status.UNAUTHORIZED).type(TEXT_PLAIN_UTF8).build();
            } catch (Exception e) {
                logger.warning("Could not update tag! " + e.getMessage());
                response = Response.status(Status.INTERNAL_SERVER_ERROR).type(TEXT_PLAIN_UTF8).build();
            }
        } else {
            response = Response.noContent().build();
        }
        return response;
    }
}
