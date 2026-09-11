package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.apache.shiro.SecurityUtils;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.landscape.common.EventLiveContent;
import com.sap.sailing.landscape.common.LiveContentCheckResult;
import com.sap.sailing.landscape.common.RaceLiveContent;
import com.sap.sailing.landscape.common.ReplicaSetLiveContent;
import com.sap.sailing.server.gateway.serialization.impl.LiveContentCheckResultJsonSerializer;
import com.sap.sailing.shared.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sse.ServerInfo;
import com.sap.sse.common.TimePoint;
import com.sap.sse.landscape.common.shared.SecuredLandscapeTypes;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

@Path(LiveContentResource.V1_LIVE_CONTENT)
public final class LiveContentResource extends AbstractSailingServerResource {
    public static final String V1_LIVE_CONTENT = "/v1/livecontent";
    public static final String CHECKED_AT_MILLIS_QUERY_PARAM = "checkedAtMillis";

    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getLiveContent(@QueryParam(CHECKED_AT_MILLIS_QUERY_PARAM) final Long checkedAtMillis) {
        SecurityUtils.getSubject().checkPermission(SecuredLandscapeTypes.LANDSCAPE.getStringPermissionForTypeRelativeIdentifier(
                SecuredLandscapeTypes.LandscapeActions.MANAGE, new TypeRelativeObjectIdentifier("AWS")));
        final TimePoint checkedAt = checkedAtMillis == null ? TimePoint.now() : TimePoint.of(checkedAtMillis);
        final List<EventLiveContent> eventsWithLiveContent = new ArrayList<>();
        for (final Event event : getService().getAllEvents()) {
            if (mayBeLiveAt(event, checkedAt)) {
                final List<RaceLiveContent> racesWithLiveContent = new ArrayList<>();
                for (final TrackedRace trackedRace : getService().getAllTrackedRacesForEventTrackingAt(event,
                        checkedAt)) {
                    racesWithLiveContent.add(new RaceLiveContent(trackedRace.getRaceIdentifier().getRegattaName(),
                            trackedRace.getRaceIdentifier().getRaceName(), trackedRace.getStartOfTracking().asMillis(),
                            trackedRace.getEndOfTracking() == null ? null : trackedRace.getEndOfTracking().asMillis()));
                }
                racesWithLiveContent.sort(Comparator.comparing(RaceLiveContent::getRegattaName)
                        .thenComparing(RaceLiveContent::getRaceName));
                if (!racesWithLiveContent.isEmpty()) {
                    eventsWithLiveContent.add(new EventLiveContent(event.getId().toString(), event.getName(),
                            event.getStartDate() == null ? null : event.getStartDate().asMillis(),
                            event.getEndDate() == null ? null : event.getEndDate().asMillis(), racesWithLiveContent));
                }
            }
        }
        eventsWithLiveContent.sort(Comparator.comparing(EventLiveContent::getEventName)
                .thenComparing(EventLiveContent::getEventId));
        final List<ReplicaSetLiveContent> replicaSetsWithLiveContent = new ArrayList<>();
        if (!eventsWithLiveContent.isEmpty()) {
            replicaSetsWithLiveContent.add(new ReplicaSetLiveContent(ServerInfo.getName(), eventsWithLiveContent));
        }
        final LiveContentCheckResult result = new LiveContentCheckResult(checkedAt.asMillis(),
                replicaSetsWithLiveContent);
        return Response.ok(streamingOutput(new LiveContentCheckResultJsonSerializer().serialize(result))).build();
    }

    private boolean mayBeLiveAt(final Event event, final TimePoint checkedAt) {
        return (event.getStartDate() == null || !event.getStartDate().after(checkedAt))
                && (event.getEndDate() == null || !event.getEndDate().before(checkedAt));
    }
}
