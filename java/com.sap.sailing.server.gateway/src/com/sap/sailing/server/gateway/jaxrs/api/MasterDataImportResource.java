package com.sap.sailing.server.gateway.jaxrs.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.authz.UnauthorizedException;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.DataImportProgress;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.landscape.common.SharedLandscapeConstants;
import com.sap.sailing.server.gateway.dto.MasterDataImportResultImpl;
import com.sap.sailing.server.gateway.interfaces.MasterDataImportResult;
import com.sap.sailing.server.gateway.serialization.impl.DataImportProgressJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.MasterDataImportResultJsonSerializer;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.util.RemoteServerUtil;

@Path(MasterDataImportResource.V1_MASTERDATAIMPORT)
public class MasterDataImportResource extends AbstractSailingServerResource {
    public static final String PROGRESS_TRACKING_UUID = "progressTrackingUuid";
    public static final String V1_MASTERDATAIMPORT = "/v1/masterdataimport";
    public static final String PROGRESS = "/progress";
    public static final String REMOTE_SERVER_BEARER_TOKEN_FORM_PARAM = "remoteServerBearerToken";
    public static final String REMOTE_SERVER_PASSWORD_FORM_PARAM = "remoteServerPassword";
    public static final String REMOTE_SERVER_USERNAME_FORM_PARAM = "remoteServerUsername";
    public static final String REMOTE_SERVER_URL_FORM_PARAM = "remoteServerUrl";
    public static final String PROGRESS_TRACKING_UUID_FORM_PARAM = "progressTrackingUUID";
    private static final Logger logger = Logger.getLogger(MasterDataImportResource.class.getName());
    
    public MasterDataImportResource() {
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces("application/json;charset=UTF-8")
    public Response importMasterData(@FormParam(REMOTE_SERVER_URL_FORM_PARAM) String remoteServerUrlAsString,
            @FormParam(REMOTE_SERVER_USERNAME_FORM_PARAM) String remoteServerUsername,
            @FormParam(REMOTE_SERVER_PASSWORD_FORM_PARAM) String remoteServerPassword,
            @FormParam(REMOTE_SERVER_BEARER_TOKEN_FORM_PARAM) String remoteServerBearerToken,
            @FormParam(MasterDataImportResultJsonSerializer.LEADERBOARDGROUP_UUID_FORM_PARAM) List<UUID> requestedLeaderboardGroupIds,
            @FormParam(MasterDataImportResultJsonSerializer.OVERRIDE_FORM_PARAM) @DefaultValue("false") Boolean override,
            @FormParam(MasterDataImportResultJsonSerializer.COMPRESS_FORM_PARAM) @DefaultValue("true") Boolean compress,
            @FormParam(MasterDataImportResultJsonSerializer.EXPORT_WIND_FORM_PARAM) @DefaultValue("true") Boolean exportWind,
            @FormParam(MasterDataImportResultJsonSerializer.EXPORT_DEVICE_CONFIGS_FORM_PARAM) @DefaultValue("false") Boolean exportDeviceConfigs,
            @FormParam(MasterDataImportResultJsonSerializer.EXPORT_TRACKED_RACES_AND_START_TRACKING_FORM_PARAM) @DefaultValue("true") Boolean exportTrackedRacesAndStartTracking,
            @FormParam(PROGRESS_TRACKING_UUID_FORM_PARAM) String progressTrackingUuid) throws MalformedURLException {
        Response response = null;
        if (!Util.hasLength(remoteServerUrlAsString)) {
            response = badRequest("Remote server URL parameter "+REMOTE_SERVER_URL_FORM_PARAM+" must be present and non-empty");
        } else {
            final URL url = RemoteServerUtil.createBaseUrl(remoteServerUrlAsString);
            if (!SharedLandscapeConstants.isTrustedDomain(url.getHost())) {
                response = badRequest("Untrusted domain for "+url);
            } else if (!validateAuthenticationParameters(remoteServerUsername, remoteServerPassword, remoteServerBearerToken)) {
                response = badRequest("Specify "+REMOTE_SERVER_USERNAME_FORM_PARAM+" and "+REMOTE_SERVER_PASSWORD_FORM_PARAM+" or alternatively "+REMOTE_SERVER_BEARER_TOKEN_FORM_PARAM+" or none of them.");
            } else {
                final UUID importMasterDataUid = progressTrackingUuid == null ? UUID.randomUUID() : UUID.fromString(progressTrackingUuid);
                try {
                    getSecurityService().checkCurrentUserServerPermission(ServerActions.CAN_IMPORT_MASTERDATA);
                    final Map<LeaderboardGroup, ? extends Iterable<Event>> eventsForLeaderboardGroups = getService()
                            .importMasterData(remoteServerUrlAsString,
                                    requestedLeaderboardGroupIds.toArray(new UUID[requestedLeaderboardGroupIds.size()]),
                                    override, compress, exportWind, exportDeviceConfigs, remoteServerUsername,
                                    remoteServerPassword, remoteServerBearerToken, exportTrackedRacesAndStartTracking,
                                    importMasterDataUid);
                    final MasterDataImportResult result = new MasterDataImportResultImpl(
                            eventsForLeaderboardGroups, remoteServerUrlAsString, override, exportWind,
                            exportDeviceConfigs, exportTrackedRacesAndStartTracking);
                    final JSONObject jsonResponse = new MasterDataImportResultJsonSerializer().serialize(result);
                    response = Response.ok(streamingOutput(jsonResponse)).build();
                } catch (UnauthorizedException e) {
                    response = Response.status(Status.UNAUTHORIZED).build();
                    logger.warning(e.getMessage() + " for user: " + getSecurityService().getCurrentUser());
                } catch (IllegalArgumentException e) {
                    response = Response.status(Status.BAD_REQUEST).entity(e.getMessage()).type(MediaType.TEXT_PLAIN).build();
                    logger.warning(e.getMessage());
                } catch (Throwable e) {
                    response = Response.status(Status.INTERNAL_SERVER_ERROR).entity(e.getMessage())
                            .type(MediaType.TEXT_PLAIN).build();
                    logger.severe(e.toString());
                }
            }
        }
        return response;
    }
    
    @GET
    @Path(PROGRESS)
    @Produces("application/json;charset=UTF-8")
    public Response getProgress(@QueryParam(PROGRESS_TRACKING_UUID) String progressTrackingUuid) {
        Response response;
        try {
            getSecurityService().checkCurrentUserServerPermission(ServerActions.CAN_IMPORT_MASTERDATA);
            final DataImportProgress progress = getService().getDataImportLock().getProgress(UUID.fromString(progressTrackingUuid));
            if (progress == null) {
                response = Response.status(Status.NOT_FOUND)
                        .entity("\"No progress found for progess tracking UUID "+progressTrackingUuid+"\"")
                        .type(MediaType.APPLICATION_JSON).build();
            } else {
                final JSONObject jsonResponse = new DataImportProgressJsonSerializer().serialize(progress);
                response = Response.ok(streamingOutput(jsonResponse)).build();
            }
        } catch (UnauthorizedException e) {
            response = Response.status(Status.UNAUTHORIZED).build();
            logger.warning(e.getMessage() + " for user: " + getSecurityService().getCurrentUser());
        }
        return response;
    }
}
