package com.sap.sailing.domain.igtimiadapter.gateway.impl;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import com.sap.sse.rest.StreamingOutputUtil;

@Path(RestApiApplication.API + RestApiApplication.V1 + RiotServerListersResource.SERVER_LISTERS)
public class RiotServerListersResource extends StreamingOutputUtil {
    protected static final String SERVER_LISTERS = "/server_listers";
    protected static final String WEB_SOCKETS = "/web_sockets";
    
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path(WEB_SOCKETS)
    public Response getEndpoints(@Context HttpServletRequest request, @Context UriInfo uriInfo) throws MalformedURLException, URISyntaxException {
        final JSONObject result = new JSONObject();
        final JSONArray webSocketServers = new JSONArray();
        result.put("web_socket_servers", webSocketServers);
        final URI requestUri = uriInfo.getRequestUri();
        final String xForwardedProtoHeader = request.getHeader("X-Forwarded-Proto");
        webSocketServers.add(new URI(((xForwardedProtoHeader != null && xForwardedProtoHeader.equals("https"))
                || requestUri.getScheme().equals("https")) ? "wss" : "ws",
                /* user info */ null, requestUri.getHost(),
                xForwardedProtoHeader != null && xForwardedProtoHeader.equals("https") ? 443 : requestUri.getPort(),
                // preserve the Web Context Root (probably "/igtimi"), then append the WEB_SOCKET_PATH
                requestUri.getPath().substring(0, requestUri.getPath().indexOf('/', 1))+RestApiApplication.WEB_SOCKET_PATH,
                /* query */ null, /* fragment */ null).toString());
        return Response.ok(streamingOutput(result)).build();
    }
}
