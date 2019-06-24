package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.UUID;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;

import com.sap.sailing.domain.base.Event;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sailing.server.gateway.jaxrs.AbstractSailingServerResource;
import com.sap.sailing.server.impl.preferences.model.SailingPreferences;
import com.sap.sailing.server.impl.preferences.model.TrackedElementWithDeviceId;
import com.sap.sailing.server.impl.preferences.model.TrackedEventPreference;
import com.sap.sailing.server.impl.preferences.model.TrackedEventPreferences;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.impl.User;

@Path("/v1/events/trackedevents/")
public class TrackedEventsResource extends AbstractSailingServerResource {

    private static final String KEY_EVENT_ID = "eventId";
    private static final String KEY_EVENT_IS_ARCHIVED = "isArchived";
    private static final String KEY_EVENT_NAME = "name";
    private static final String KEY_EVENT_START = "start";
    private static final String KEY_EVENT_AND = "end";
    private static final String KEY_EVENT_REGISTERED_DEVICE_IDS = "registeredDeviceIds";
    private static final String KEY_EVENT_BASE_URL = "url";
    private static final String KEY_EVENT_IS_OWNER = "isOwner";
    private static final String KEY_TRACKED_ELEMENT_ID = "deviceId";
    private static final String KEY_TRACKED_ELEMENT_COMPETITOR_ID = "competitorId";
    private static final String KEY_TRACKED_ELEMENT_BOAT_ID = "boatId";
    private static final String KEY_TRACKED_ELEMENT_MARK_ID = "markId";

    @GET
    @Produces("application/json;charset=UTF-8")
    public Response getTrackedEvents(@QueryParam(KEY_EVENT_IS_ARCHIVED) String isArchived) {

        final User currentUser = getSecurityService().getCurrentUser();
        final ResponseBuilder builder;

        // check if user logged in
        // TODO: handle requests for anonymous users
        if (currentUser != null) {

            final TrackedEventPreferences prefs = getSecurityService().getPreferenceObject(currentUser.getName(),
                    SailingPreferences.TRACKED_EVENTS_PREFERENCES);

            final JSONArray result = new JSONArray();

            final boolean showArchived = Boolean.parseBoolean(isArchived);
            if (prefs != null) {
                for (final TrackedEventPreference pref : prefs.getTrackedEvents()) {

                    if (!showArchived && pref.getIsArchived()) {
                        // skip, if event is archived and should be filtered out
                        continue;
                    }

                    final Event event = getService().getEvent(pref.getEventId());
                    final OwnershipAnnotation ownership = getSecurityService().getOwnership(event.getIdentifier());
                    final boolean isOwner = currentUser == null ? false
                            : currentUser.equals(ownership.getAnnotation().getUserOwner());

                    final JSONObject jsonEvent = new JSONObject();
                    jsonEvent.put(KEY_EVENT_ID, event.getId().toString());
                    jsonEvent.put(KEY_EVENT_NAME, event.getName());
                    jsonEvent.put(KEY_EVENT_START, event.getStartDate().toString());
                    jsonEvent.put(KEY_EVENT_AND, event.getEndDate().toString());

                    final JSONArray deviceIdsWithTrackedElementJson = new JSONArray();

                    for(TrackedElementWithDeviceId trackedElement : pref.getTrackedElements()) {
                        JSONObject trackedElementJson = new JSONObject();
                        trackedElementJson.put(KEY_TRACKED_ELEMENT_ID, trackedElement.getDeviceId());
                        if(trackedElement.getTrackedCompetitorId() != null) {
                            trackedElementJson.put(KEY_TRACKED_ELEMENT_COMPETITOR_ID, trackedElement.getTrackedCompetitorId().toString());
                        } else if (trackedElement.getTrackedBoatId() != null) {
                            trackedElementJson.put(KEY_TRACKED_ELEMENT_BOAT_ID,
                                    trackedElement.getTrackedBoatId().toString());
                        }
                        else if (trackedElement.getTrackedMarkId() != null) {
                            trackedElementJson.put(KEY_TRACKED_ELEMENT_MARK_ID,
                                    trackedElement.getTrackedMarkId().toString());
                        }
                    }

                    jsonEvent.put(KEY_EVENT_REGISTERED_DEVICE_IDS, deviceIdsWithTrackedElementJson);
                    // jsonEvent.put("imageUrl", event.getImages().)
                    jsonEvent.put(KEY_EVENT_BASE_URL, pref.getBaseUrl());
                    jsonEvent.put(KEY_EVENT_IS_ARCHIVED, pref.getIsArchived());
                    jsonEvent.put(KEY_EVENT_IS_OWNER, isOwner);
                    result.add(jsonEvent);
                }
            }
            String json = result.toJSONString();
            builder = Response.ok(json).header("Content-Type", MediaType.APPLICATION_JSON + ";charset=UTF-8");
        } else {
            builder = Response.status(Status.UNAUTHORIZED);
        }
        return builder.build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateTrackedEvents(String jsonBody) {
        ResponseBuilder responseBuilder;
        final User currentUser = getSecurityService().getCurrentUser();

        if (currentUser != null) {

            try {
                final Object requestBody = JSONValue.parseWithException(jsonBody);
                final JSONObject requestObject = Helpers.toJSONObjectSafe(requestBody);
                final String eventId = (String) requestObject.get(KEY_EVENT_ID);
                final Boolean archived = (Boolean) requestObject.get(KEY_EVENT_IS_ARCHIVED);
                try {
                    final UUID uuid = UUID.fromString(eventId);
                    final boolean isArchived = archived;

                    TrackedEventPreferences prefs = getSecurityService().getPreferenceObject(currentUser.getName(),
                            SailingPreferences.TRACKED_EVENTS_PREFERENCES);

                    if (prefs == null) {
                        prefs = new TrackedEventPreferences();
                    }

                    Collection<TrackedEventPreference> prefsNew = new ArrayList<>();
                    Iterator<TrackedEventPreference> it = prefs.getTrackedEvents().iterator();
                    while (it.hasNext()) {
                        final TrackedEventPreference pref = it.next();
                        if (pref.getEventId().equals(uuid)) {
                            prefsNew.add(new TrackedEventPreference(pref, isArchived));
                        } else {
                            prefsNew.add(pref);
                        }
                    }

                    getSecurityService().setPreferenceObject(currentUser.getName(),
                            SailingPreferences.TRACKED_EVENTS_PREFERENCES, prefsNew);
                    responseBuilder = Response.status(Status.ACCEPTED);
                } catch (IllegalArgumentException | ClassCastException e) {
                    responseBuilder = Response.status(Status.BAD_REQUEST)
                            .entity("Invalid or missing attributes in JSON body.");
                }
            } catch (ParseException | JsonDeserializationException e) {
                responseBuilder = Response.status(Status.BAD_REQUEST).entity("Invalid JSON body in request.");
            }
        } else {
            responseBuilder = Response.status(Status.UNAUTHORIZED);
        }
        return responseBuilder.build();
    }
}
