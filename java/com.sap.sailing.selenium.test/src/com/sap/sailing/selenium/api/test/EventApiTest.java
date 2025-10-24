package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.domain.common.CompetitorRegistrationType.CLOSED;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.EventApi.Event;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class EventApiTest extends AbstractSeleniumTest {

    private final EventApi eventApi = new EventApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(),  /* headless */ true);
    }

    @SeleniumTestCase
    public void createAndGetEventTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), ApiContext.SERVER_CONTEXT);

        final String eventName = "<ppp> loggingsession";

        final Event createdEvent = eventApi.createEvent(ctx, eventName, "75QMNATIONALEKREUZER", CLOSED, "default");
        assertNotNull(createdEvent.getId(), "create: event.eventid is missing");
        assertNotNull(createdEvent.getStartDate(), "create: event.eventstartdate is missing");
        assertNotNull(createdEvent.getEndDate(), "create: event.eventenddate is missing");
        assertEquals(eventName, createdEvent.getName(), "create: event.eventname is different");
        assertEquals(eventName, createdEvent.get("regatta"), "create: event.reagattaname is different");
        assertEquals(eventName, createdEvent.get("leaderboard"), "create: event.leaderboard is different");

        final Event foundEvent = eventApi.getEvent(ctx, (String) createdEvent.getId());
        assertNotNull(foundEvent.getId(), "read: event.id is missing");
        assertEquals(eventName, foundEvent.getName(), "read: event.name is different");
        assertEquals(eventName, foundEvent.get("description"), "read: event.description is different");
        assertEquals(null, (String) foundEvent.get("officialWebsiteURL"),
                "read: event.officialWebsiteURL is different");
        assertEquals(removeTrailingSlash(getContextRoot()), removeTrailingSlash(foundEvent.get("baseURL")), "read: event.baseUrl is different");
        assertNotNull(foundEvent.getStartDate(), "read: event.startDate is missing");
        assertNotNull(foundEvent.getEndDate(), "read: event.endDate is missing");
        assertEquals(0, ((JSONArray) foundEvent.get("images")).size(), "read: event.images should be empty");
        assertEquals(0, ((JSONArray) foundEvent.get("videos")).size(), "read: event.videos should be empty");
        assertEquals(0, ((JSONArray) foundEvent.get("sailorsInfoWebsiteURLs")).size(),
                "read: event.sailorsInfoWebsiteURLs");

        JSONObject foundEventVenue = (JSONObject) foundEvent.get("venue");
        assertEquals("default", foundEventVenue.get("name"), "read: event.venue.name is different");

        JSONArray foundEventVenueCourseAreas = (JSONArray) foundEventVenue.get("courseAreas");
        JSONObject foundEventVenueCourseAreasFirst = (JSONObject) foundEventVenueCourseAreas.get(0);
        assertEquals("Default", foundEventVenueCourseAreasFirst.get("name"),
                "read: event.venue.courseAreas[0].name is different");
        assertNotNull(foundEventVenueCourseAreasFirst.get("id"), "read: event.venue.courseAreas[0].id is null");

        JSONArray foundEventLeaderBoardGroups = (JSONArray) foundEvent.get("leaderboardGroups");
        JSONObject foundEventLeaderBoardGroupsFirst = (JSONObject) foundEventLeaderBoardGroups.get(0);
        assertNotNull(foundEventLeaderBoardGroupsFirst.get("id"), "read: event.leaderboardGroups[0].id is null");
        assertEquals(eventName, foundEventLeaderBoardGroupsFirst.get("name"),
                "read: event.leaderboardGroups[0].name is different");
        assertEquals(eventName, foundEventLeaderBoardGroupsFirst.get("description"),
                "read: event.leaderboardGroups[0].description is different");
        assertEquals(false, foundEventLeaderBoardGroupsFirst.get("hasOverallLeaderboard"),
                "read: event.leaderboardGroups[0].hasOverallLeaderboard is different");
    }
    
    private String removeTrailingSlash(String url) {
        return url.endsWith("/")?url.substring(0, url.length()-1):url;
    }
}
