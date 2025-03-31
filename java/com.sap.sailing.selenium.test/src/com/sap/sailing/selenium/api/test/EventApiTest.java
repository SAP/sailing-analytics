package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.domain.common.CompetitorRegistrationType.CLOSED;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.EventApi.Event;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class EventApiTest extends AbstractSeleniumTest {

    private final EventApi eventApi = new EventApi();

    @Before
    public void setUp() {
        clearState(getContextRoot(),  /* headless */ true);
    }

    @Test
    public void createAndGetEventTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), ApiContext.SERVER_CONTEXT);

        final String eventName = "<ppp> loggingsession";

        final Event createdEvent = eventApi.createEvent(ctx, eventName, "75QMNATIONALEKREUZER", CLOSED, "default");
        assertNotNull("create: event.eventid is missing", createdEvent.getId());
        assertNotNull("create: event.eventstartdate is missing", createdEvent.getStartDate());
        assertNotNull("create: event.eventenddate is missing", createdEvent.getEndDate());
        assertEquals("create: event.eventname is different", eventName, createdEvent.getName());
        assertEquals("create: event.reagattaname is different", eventName, createdEvent.get("regatta"));
        assertEquals("create: event.leaderboard is different", eventName, createdEvent.get("leaderboard"));

        final Event foundEvent = eventApi.getEvent(ctx, (String) createdEvent.getId());
        assertNotNull("read: event.id is missing", foundEvent.getId());
        assertEquals("read: event.name is different", eventName, foundEvent.getName());
        assertEquals("read: event.description is different", eventName, foundEvent.get("description"));
        assertEquals("read: event.officialWebsiteURL is different", null,
                (String) foundEvent.get("officialWebsiteURL"));
        assertEquals("read: event.baseUrl is different", removeTrailingSlash(getContextRoot()), removeTrailingSlash(foundEvent.get("baseURL")));
        assertNotNull("read: event.startDate is missing", foundEvent.getStartDate());
        assertNotNull("read: event.endDate is missing", foundEvent.getEndDate());
        assertEquals("read: event.images should be empty", 0, ((JSONArray) foundEvent.get("images")).size());
        assertEquals("read: event.videos should be empty", 0, ((JSONArray) foundEvent.get("videos")).size());
        assertEquals("read: event.sailorsInfoWebsiteURLs", 0,
                ((JSONArray) foundEvent.get("sailorsInfoWebsiteURLs")).size());

        JSONObject foundEventVenue = (JSONObject) foundEvent.get("venue");
        assertEquals("read: event.venue.name is different", "default", foundEventVenue.get("name"));

        JSONArray foundEventVenueCourseAreas = (JSONArray) foundEventVenue.get("courseAreas");
        JSONObject foundEventVenueCourseAreasFirst = (JSONObject) foundEventVenueCourseAreas.get(0);
        assertEquals("read: event.venue.courseAreas[0].name is different", "Default",
                foundEventVenueCourseAreasFirst.get("name"));
        assertNotNull("read: event.venue.courseAreas[0].id is null", foundEventVenueCourseAreasFirst.get("id"));

        JSONArray foundEventLeaderBoardGroups = (JSONArray) foundEvent.get("leaderboardGroups");
        JSONObject foundEventLeaderBoardGroupsFirst = (JSONObject) foundEventLeaderBoardGroups.get(0);
        assertNotNull("read: event.leaderboardGroups[0].id is null", foundEventLeaderBoardGroupsFirst.get("id"));
        assertEquals("read: event.leaderboardGroups[0].name is different", eventName,
                foundEventLeaderBoardGroupsFirst.get("name"));
        assertEquals("read: event.leaderboardGroups[0].description is different", eventName,
                foundEventLeaderBoardGroupsFirst.get("description"));
        assertEquals("read: event.leaderboardGroups[0].hasOverallLeaderboard is different", false,
                foundEventLeaderBoardGroupsFirst.get("hasOverallLeaderboard"));
    }
    
    private String removeTrailingSlash(String url) {
        return url.endsWith("/")?url.substring(0, url.length()-1):url;
    }
}
