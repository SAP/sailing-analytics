package com.sap.sailing.aiagent.gateway.impl;

import java.util.Collections;
import java.util.UUID;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.apache.shiro.SecurityUtils;
import org.osgi.util.tracker.ServiceTracker;

import com.sap.sailing.aiagent.interfaces.AIAgent;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.shared.server.gateway.jaxrs.SharedAbstractSailingServerResource;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.Util;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes;
import com.sap.sse.security.shared.impl.SecuredSecurityTypes.ServerActions;

@Path(RestApiApplication.API + RestApiApplication.V1 + AIAgentResource.AI_AGENT)
public class AIAgentResource extends SharedAbstractSailingServerResource {
    protected static final String AI_AGENT = "/aiagent";

    private AIAgent getAIAgent() {
        @SuppressWarnings("unchecked")
        ServiceTracker<AIAgent, AIAgent> tracker = (ServiceTracker<AIAgent, AIAgent>) getServletContext()
                .getAttribute(RestServletContainer.AI_AGENT_TRACKER_NAME);
        return tracker.getService();
    }

    /**
     * The calling subject must have the {@link SecuredDomainType#AI_AGENT AI_AGENT}:{@link DefaultActions#UPDATE
     * UPDATE} permission on the AI agent identified by the local {@link ServerInfo#getName() server name}, as well as
     * the {@link SecuredDomainType#EVENT EVENT}:{@link DefaultActions#UPDATE UPDATE} permission on the event identified
     * by the {@code eventUUID}.
     */
    @Path("/startcommenting/{eventUUID}")
    @POST
    @Produces("application/json;charset=UTF-8")
    public Response startCommentingEvent(@PathParam("eventUUID") String eventUUID) {
        final Response response;
        SecurityUtils.getSubject().checkPermission(
                SecuredSecurityTypes.SERVER.getStringPermissionForTypeRelativeIdentifier(ServerActions.CONFIGURE_AI_AGENT,
                        new TypeRelativeObjectIdentifier(ServerInfo.getName())));
        final AIAgent aiAgent = getAIAgent();
        if (aiAgent != null) {
            final RacingEventService racingEventService = getService();
            final Event event = Util.first(racingEventService.getEventsSelectively(/* include */ true, Collections.singleton(UUID.fromString(eventUUID))));
            if (event != null) {
                getSecurityService().checkCurrentUserUpdatePermission(event);
                aiAgent.startCommentingOnEvent(event);
                response = Response.ok().build();
            } else {
                response = Response
                        .status(Status.NOT_FOUND)
                        .entity("Event with the requested ID not found")
                        .build();
            }
        } else {
            response = Response
                    .status(Status.NOT_FOUND)
                    .entity("AI Agent not found; perhaps no credentials provided?")
                    .build();
        }
        return response;
    }
    
    /**
     * The calling subject must have the {@link SecuredDomainType#AI_AGENT AI_AGENT}:{@link DefaultActions#UPDATE
     * UPDATE} permission on the AI agent identified by the local {@link ServerInfo#getName() server name}, as well as
     * the {@link SecuredDomainType#EVENT EVENT}:{@link DefaultActions#UPDATE UPDATE} permission on the event identified
     * by the {@code eventUUID}.
     */
    @Path("/stopcommenting/{eventUUID}")
    @POST
    @Produces("application/json;charset=UTF-8")
    public Response stopCommentingEvent(@PathParam("eventUUID") String eventUUID) {
        final Response response;
        SecurityUtils.getSubject().checkPermission(
                SecuredSecurityTypes.SERVER.getStringPermissionForTypeRelativeIdentifier(ServerActions.CONFIGURE_AI_AGENT,
                        new TypeRelativeObjectIdentifier(ServerInfo.getName())));
        final AIAgent aiAgent = getAIAgent();
        if (aiAgent != null) {
            final RacingEventService racingEventService = getService();
            final Event event = Util.first(racingEventService.getEventsSelectively(/* include */ true, Collections.singleton(UUID.fromString(eventUUID))));
            if (event != null) {
                getSecurityService().checkCurrentUserUpdatePermission(event);
                aiAgent.stopCommentingOnEvent(event);
                response = Response.ok().build();
            } else {
                response = Response
                        .status(Status.NOT_FOUND)
                        .entity("Event with the requested ID not found")
                        .build();
            }
        } else {
            response = Response
                    .status(Status.NOT_FOUND)
                    .entity("AI Agent not found; perhaps no credentials provided?")
                    .build();
        }
        return response;
    }
    
}
