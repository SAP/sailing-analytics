package com.sap.sailing.selenium.api.event;

import static com.sap.sailing.selenium.api.core.ApiRequest.Context.SAILING;

import java.util.Map;
import java.util.TreeMap;

import org.json.simple.JSONObject;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.ApiRequest;
import com.sap.sailing.selenium.api.core.JsonWrapper;
import com.sun.jersey.api.representation.Form;

public class EventApi {

    private static final String CREATE_EVENT_URL = "/api/v1/events/createEvent";
    private static final String LIST_EVENTS = "/api/v1/events";

    public Event createEvent(ApiContext ctx, String eventName, String boatclassname,
            CompetitorRegistrationType competitorRegistrationType, String venuename) {
        Map<String, String> formParams = new TreeMap<>();
        formParams.put("boatclassname", boatclassname);
        formParams.put("competitorRegistrationType", competitorRegistrationType.name());
        formParams.put("eventName", eventName);
        formParams.put("venuename", venuename);
        EventForCreate event = new EventForCreate(ctx.post(CREATE_EVENT_URL, null, formParams));
        return event;
    }

    public static ApiRequest<Form, EventForCreate> create(String eventName, String boatclassname,
            CompetitorRegistrationType competitorRegistrationType, String venuename) {
        return SAILING.post(CREATE_EVENT_URL).formParam("boatclassname", boatclassname)
                .formParam("competitorRegistrationType", competitorRegistrationType.name())
                .formParam("eventName", eventName).formParam("venuename", venuename)
                .wrapJsonResult(EventForCreate.class);
    }

    public Event getEvent(ApiContext ctx, String eventId) {
        Event event = new Event(ctx.get(LIST_EVENTS + "/" + eventId));
        return event;
    }

    public static class Event extends JsonWrapper {

        public Event(JSONObject json) {
            super(json);
        }

        public String getId() {
            return get("id");
        }

        public String getName() {
            return get("name");
        }

        public String getSecret() {
            return null;
        }

        public Long getStartDate() {
            return get("startDate");
        }

        public Long getEndDate() {
            return get("endDate");
        }
    }

    public static class EventForCreate extends Event {

        public EventForCreate(JSONObject json) {
            super(json);
        }

        public String getId() {
            return get("eventid");
        }

        public String getName() {
            return get("eventname");
        }

        public String getSecret() {
            return get("registrationSecret");
        }

        public Long getStartDate() {
            return get("eventstartdate");
        }

        public Long getEndDate() {
            return (Long) get("eventenddate");
        }
    }
}
