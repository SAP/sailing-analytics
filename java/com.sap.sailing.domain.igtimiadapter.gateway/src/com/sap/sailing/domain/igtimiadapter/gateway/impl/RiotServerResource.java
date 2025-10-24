package com.sap.sailing.domain.igtimiadapter.gateway.impl;

import java.io.IOException;
import java.net.URISyntaxException;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.igtimiadapter.server.riot.RiotServer;
import com.sap.sse.rest.StreamingOutputUtil;

@Path(RestApiApplication.API + RestApiApplication.V1 + RiotServerResource.SERVER)
public class RiotServerResource extends StreamingOutputUtil {
    protected static final String SERVER = "/server";
    
    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getServer() throws URISyntaxException, IOException {
        final RiotServer riot = Activator.getInstance().getRiotServer();
        final JSONObject result = new JSONObject();
        result.put("port", riot.getPort());
        return Response.ok(streamingOutput(result)).build();
    }
}
