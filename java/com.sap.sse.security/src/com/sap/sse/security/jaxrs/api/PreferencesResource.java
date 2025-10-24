package com.sap.sse.security.jaxrs.api;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;
import org.json.simple.parser.ParseException;

import com.sap.sse.security.jaxrs.AbstractSecurityResource;

@Path("/restsecurity/preferences")
public class PreferencesResource extends AbstractSecurityResource {
    @GET
    @Produces("application/json;charset=UTF-8")
    @Path("{settingsKey}")
    public Response getPreference(@PathParam("settingsKey") String settingsKey) throws ParseException {
        Response response = null;
        if (SecurityUtils.getSubject().isAuthenticated()) {
            String username = SecurityUtils.getSubject().getPrincipal().toString();
            String settings = getSecurityService().getPreference(username, settingsKey);
            if (settings == null) {
                response = Response.noContent().build();
            } else {
                response = Response.ok(settings).build();
            }
        } else {
            response = Response.status(401).build();
        }
        return response;
    }

    @PUT
    @Consumes(MediaType.APPLICATION_JSON)
    @Path("{settingsKey}")
    public Response putPreference(@PathParam("settingsKey") String settingsKey, String json) throws ParseException {
        Response response = null;
        if (SecurityUtils.getSubject().isAuthenticated()) {
            String username = SecurityUtils.getSubject().getPrincipal().toString();
            getSecurityService().setPreference(username, settingsKey, json);
            response = Response.ok().build();
        } else {
            response = Response.status(401).build();
        }
        return response;
    }

    @DELETE
    @Produces("application/json;charset=UTF-8")
    @Path("{settingsKey}")
    public Response deletePreference(@PathParam("settingsKey") String settingsKey) throws ParseException {
        Response response = null;
        if (SecurityUtils.getSubject().isAuthenticated()) {
            String username = SecurityUtils.getSubject().getPrincipal().toString();
            getSecurityService().unsetPreference(username, settingsKey);
            response = Response.ok().build();
        } else {
            response = Response.status(401).build();
        }
        return response;
    }
}