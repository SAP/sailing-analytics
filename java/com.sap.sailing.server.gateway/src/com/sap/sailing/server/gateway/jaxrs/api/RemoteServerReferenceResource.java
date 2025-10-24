package com.sap.sailing.server.gateway.jaxrs.api;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.shiro.authz.UnauthorizedException;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sailing.domain.base.RemoteSailingServerReference;
import com.sap.sailing.server.gateway.serialization.impl.RemoteSailingServerReferenceJsonSerializer;
import com.sap.sailing.server.operationaltransformation.AddRemoteSailingServerReference;
import com.sap.sailing.server.operationaltransformation.RemoveRemoteSailingServerReference;
import com.sap.sailing.server.operationaltransformation.UpdateSailingServerReference;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;
import com.sap.sse.security.util.RemoteServerUtil;

@Path (RemoteServerReferenceResource.V1_REMOTESERVERREFERENCE)
public class RemoteServerReferenceResource extends AbstractSailingServerResource {
    public static final String REMOVE = "/remove";
    public static final String UPDATE = "/update";
    public static final String ADD = "/add";
    public static final String V1_REMOTESERVERREFERENCE = "/v1/remoteserverreference";
    public static final Logger logger = Logger.getLogger(RemoteServerReferenceResource.class.getName());
    public static final String REMOTE_SERVER_URL = "remoteServerUrl";
    public static final String REMOTE_SERVER_NAME = "remoteServerName";
    public static final String REMOTE_SERVER_EVENT_IDS = "eventIds";
    public static final String REMOTE_SERVER_IS_INCLUDE = "include";
    
    public RemoteServerReferenceResource() {
    }
    
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getRemoteServerReferences() {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
        final JSONArray result = new JSONArray();
        for (final Entry<String, RemoteSailingServerReference> e : getService().getAllRemoteServerReferences().entrySet()) {
            final JSONObject serverRefJson = serializeRemoteServerReference(e.getValue());
            result.add(serverRefJson);
        }
        return Response.ok(streamingOutput(result)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
    }

    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("/{"+REMOTE_SERVER_NAME+"}")
    public Response getRemoteServerReferences(@PathParam(REMOTE_SERVER_NAME) String serverName) {
        getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
        final RemoteSailingServerReference remoteServerReferenceByName = getService().getRemoteServerReferenceByName(serverName);
        final Response response;
        if (remoteServerReferenceByName == null) {
            response = Response.status(Status.NOT_FOUND).entity("Couldn't find remote server reference named "+serverName).build();
        } else {
            final JSONObject serverRefJson = serializeRemoteServerReference(remoteServerReferenceByName);
            response = Response.ok(streamingOutput(serverRefJson)).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
        }
        return response;
    }

    private JSONObject serializeRemoteServerReference(final RemoteSailingServerReference reference) {
        final JSONObject serverRefJson = new RemoteSailingServerReferenceJsonSerializer().serialize(reference);
        return serverRefJson;
    }
    
    @PUT
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path(UPDATE)
    public Response updateRemoteServerReference(
            @FormParam(REMOTE_SERVER_NAME) String remoteServerName,
            @FormParam(REMOTE_SERVER_IS_INCLUDE) Boolean include,
            @FormParam(REMOTE_SERVER_EVENT_IDS) Set<String> eventIds) {
        Response response = null;
        if (!Util.hasLength(remoteServerName)) {
            response = badRequest(REMOTE_SERVER_NAME + " form parameter must not be empty.");
        } else {
            try {
                getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
                final RemoteSailingServerReference existingReference = getService().getRemoteServerReferenceByName(remoteServerName);
                if (existingReference == null) {
                    response = Response.status(Status.NOT_FOUND).entity("No server reference by name "+StringEscapeUtils.escapeHtml(remoteServerName)+" found").build();
                } else {
                    final RemoteSailingServerReference serverRef = getService()
                            .apply(new UpdateSailingServerReference(remoteServerName,
                                    include == null ? existingReference.isInclude() : include,
                                    eventIds.stream().map(idString->UUID.fromString(idString)).collect(Collectors.toSet())));
                    final JSONObject jsonResponse = serializeRemoteServerReference(serverRef);
                    response = Response.ok(streamingOutput(jsonResponse))
                            .header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
                }
            } catch (UnauthorizedException e) {
                response = returnUnauthorized(e);
            } catch (Throwable e) {
                response = returnInternalServerError(e);
            }
        }
        return response;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path(ADD)
    public Response addRemoteServerReference(
            @FormParam(REMOTE_SERVER_URL) String remoteServerUrlAsString,
            @FormParam(REMOTE_SERVER_NAME) String remoteServerName,
            @FormParam(REMOTE_SERVER_IS_INCLUDE) @DefaultValue("false") Boolean include) {
        Response response = null;
        if (!Util.hasLength(remoteServerUrlAsString) || !Util.hasLength(remoteServerName)) {
            response = badRequest("Both, " + REMOTE_SERVER_URL + " and " + REMOTE_SERVER_NAME
                    + " form parameters must not be empty.");
        } else {
            try {
                final URL remoteServerUrl = RemoteServerUtil.createBaseUrl(remoteServerUrlAsString);
                getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
                final RemoteSailingServerReference serverRef = getService()
                        .apply(new AddRemoteSailingServerReference(remoteServerName, remoteServerUrl, include));
                final JSONObject jsonResponse = serializeRemoteServerReference(serverRef);
                response = Response.ok(streamingOutput(jsonResponse))
                        .header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
            } catch (UnauthorizedException e) {
                response = returnUnauthorized(e);
            } catch (MalformedURLException e) {
                response = badRequest("Malformed URL. See server logs for details.");
                logger.warning(e.getMessage() + " for URL: " + remoteServerUrlAsString);
            } catch (Throwable e) {
                response = returnInternalServerError(e);
            }
        }
        return response;
    }

    @POST
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Path(REMOVE)
    public Response removeRemoteServerReference(@FormParam(REMOTE_SERVER_NAME) String remoteServerName, @FormParam(REMOTE_SERVER_URL) String remoteServerUrlAsString) {
        Response response = null;
        if (!Util.hasLength(remoteServerName) && !Util.hasLength(remoteServerUrlAsString)) {
            response = badRequest("No remote server name provided in form parameter "+REMOTE_SERVER_NAME+
                    " nore any remote server URL provided in form parameter "+REMOTE_SERVER_URL);
        } else {
            try {
                getSecurityService().checkCurrentUserServerPermission(ServerActions.CONFIGURE_REMOTE_INSTANCES);
                final RemoteSailingServerReference serverRef;
                if (remoteServerName != null) {
                    serverRef = getService().getRemoteServerReferenceByName(remoteServerName);
                } else {
                    serverRef = getService().getRemoteServerReferenceByUrl(new URL(remoteServerUrlAsString));
                }
                if (serverRef == null) {
                    response = Response.status(Status.NOT_FOUND)
                            .entity("\"remoteServerName: \\\"" + StringEscapeUtils.escapeHtml(remoteServerName) +
                                    "\\\", remoteServerUrl: \\\""+StringEscapeUtils.escapeHtml(remoteServerUrlAsString)+"\\\" doesn't exist on this server.\"")
                            .build();
                } else {
                    getService().apply(new RemoveRemoteSailingServerReference(serverRef.getName()));
                    final JSONObject jsonResponse = serializeRemoteServerReference(serverRef);
                    response = Response.ok(streamingOutput(jsonResponse))
                            .header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8").build();
                }
            } catch (UnauthorizedException e) {
                response = returnUnauthorized(e);
            } catch (Throwable e) {
                response = returnInternalServerError(e);
            }
        }
        return response;
    }
}
