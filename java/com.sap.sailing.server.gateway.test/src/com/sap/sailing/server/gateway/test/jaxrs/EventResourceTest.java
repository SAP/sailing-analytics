package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.text.ParseException;
import java.util.Arrays;
import java.util.UUID;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.apache.commons.lang.RandomStringUtils;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.NotFoundException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.RankingMetrics;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.server.gateway.jaxrs.api.AbstractLeaderboardsResource;
import com.sap.sse.InvalidDateException;
import com.sap.sse.rest.StreamingOutputUtil;

public class EventResourceTest extends AbstractJaxRsApiTest {
    private String randomName; 
    private UriInfo uriInfo;
    
    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        uriInfo = mock(UriInfo.class);
        when(uriInfo.getBaseUri()).thenReturn(new URI("http://127.0.0.1:8888/"));
        randomName = randomName();
    }
    
    @Test
    public void testCreateEvent() throws Exception {
        Response eventResponse = createEvent();
        assertTrue(isValidCreateEventResponse(eventResponse));
        JSONObject objEvent = getEvent(getIdFromCreateEventResponse(eventResponse));
        assertFalse(hasDefaultLeaderboardGroup(objEvent));
    }
    
    @Test
    public void testCreateEventInBerlin() throws Exception {
        Response eventResponse = createEventAtLocation(new DegreePosition(52.514176, 13.411628));
        assertTrue(isValidCreateEventResponse(eventResponse));
        JSONObject objEvent = getEvent(getIdFromCreateEventResponse(eventResponse));
        assertFalse(hasDefaultLeaderboardGroup(objEvent));
        assertEquals("Nikolaiviertel", ((JSONObject) objEvent.get("venue")).get("name"));
    }
    
    @Test
    public void testCreateEventWithLeaderboardGroup() throws Exception {
        Response eventResponse = createEventWithLeaderboardGroup();
        assertTrue(isValidCreateEventResponse(eventResponse));
        JSONObject objEvent = getEvent(getIdFromCreateEventResponse(eventResponse));
        assertTrue(hasDefaultLeaderboardGroup(objEvent));
    }

    @Test
    public void testCreateEventWithLeaderboardGroupAndRegatta() throws Exception {
        Response eventResponse = createEventWithLeaderboardGroupAndRegatta();
        assert(isValidCreateEventResponse(eventResponse));
        JSONObject objEvent = getEvent(getIdFromCreateEventResponse(eventResponse));
        assertTrue(hasDefaultLeaderboardGroup(objEvent));
        assertTrue(hasAtLeastOneCourseArea(objEvent));
        JSONObject objRegatta = getRegatta(randomName);
        String strRegattaName = (String) objRegatta.get("name");
        String strRegattaCourseAreaId = (String) ((JSONArray) objRegatta.get("courseAreaIds")).get(0);
        assertTrue(strRegattaCourseAreaId.equals(getDefaultCourseAreaId(objEvent)));
        leaderBoardWithNameExists(strRegattaName);
        JSONArray leaderboardGroups = getLeaderboardGroups(objEvent);
        assertTrue(hasDefaultLeaderboardGroup(objEvent));
        JSONObject objDefaultLeaderboardGroup = (JSONObject) leaderboardGroups.get(0);
        String strDefaultLeaderboardGroupName = (String) objDefaultLeaderboardGroup.get("name");
        JSONObject objLeaderboardGroup = getLeaderboardGroup(strDefaultLeaderboardGroupName);
        JSONArray leaderboards  = (JSONArray) objLeaderboardGroup.get("leaderboards");
        assertTrue(containsObjectWithAttrbuteNameAndValue(leaderboards, "name", randomName));
    }

    @Test
    public void testCreateEventWithLeaderboardGroupAddRegatta() throws Exception {
        String eventName = randomName;
        Response eventResponse = createEventWithLeaderboardGroupAndRegatta();
        assertTrue(isValidCreateEventResponse(eventResponse));
        String strEventId = getIdFromCreateEventResponse(eventResponse);
        JSONObject regatta = getRegatta(eventName);
        String strRegattaCourseAreaId = (String) ((JSONArray) regatta.get("courseAreaIds")).get(0);
        JSONArray arrCourseAreas = getCourseAreasOfEvent(strEventId);
        assertTrue(arrCourseAreas.size() == 1);
        JSONObject objCourseArea = (JSONObject) arrCourseAreas.get(0);
        String strCourseAreaId = (String) objCourseArea.get("id");
        assertTrue(strCourseAreaId.equals(strRegattaCourseAreaId)); 
        String strRegattaName = (String) regatta.get("name");
        assertTrue(leaderBoardWithNameExists(strRegattaName));
        JSONObject objLeaderboardGroup = getLeaderboardGroup(eventName);
        JSONArray arrLeaderboards = (JSONArray) objLeaderboardGroup.get("leaderboards");
        assertTrue(containsObjectWithAttrbuteNameAndValue(arrLeaderboards, "name", strRegattaName));
        JSONObject leaderboard = getLeaderboardAsJsonObject(getLeaderboard(eventName));
        assertEquals("/leaderboard/" + eventName, leaderboard.get("shardingLeaderboardName"));
    }
    
    @Test
    public void testShardingNameInLeaderboardResponse() throws Exception {
        String originalRandomName = randomName;
        String eventName = randomName += "ä$";
        Response eventResponse = createEventWithLeaderboardGroupAndRegatta();
        assertTrue(isValidCreateEventResponse(eventResponse));
        JSONObject leaderboard = getLeaderboardAsJsonObject(getLeaderboard(eventName));
        assertEquals("/leaderboard/" + originalRandomName + "__", leaderboard.get("shardingLeaderboardName"));
    }
    
    @Test
    public void testCreateEventWithScoringShemeRankingMetricAndDiscards() throws Exception {
        String eventName = randomName;
        Response eventResponse = createEventWithScoringShemeRankingMetricAndDiscards();
        assertTrue(isValidCreateEventResponse(eventResponse));
        JSONObject regatta = getRegatta(eventName);
        String scoringSystem = (String) regatta.get("scoringSystem");
        assertEquals(ScoringSchemeType.HIGH_POINT.name(), scoringSystem);
        String rankingMetric = (String) regatta.get("rankingMetric");
        assertEquals(RankingMetrics.ORC_PERFORMANCE_CURVE.name(), rankingMetric);
        
        JSONObject leaderboard = getLeaderboardAsJsonObject(getLeaderboard(eventName));
        JSONArray discardIndices = (JSONArray) leaderboard.get("discardIndexResultsStartingWithHowManyRaces");
        assertEquals(2, discardIndices.size());
        assertEquals(2, ((Number) discardIndices.get(0)).intValue());
        assertEquals(4, ((Number) discardIndices.get(1)).intValue());
    }

    private Response createEventWithLeaderboardGroup() throws ParseException, NotFoundException, NumberFormatException,
            IOException, org.json.simple.parser.ParseException, InvalidDateException {
        return eventsResource.createEvent(uriInfo, randomName, randomName, /* startDateParam */ null,
                /* startDateAsMillis */ null, /* endDateParam */ null, /* endDateAsMillis */ null,
                /* venueNameParam */ randomName, /* venueLat */ null, /* venueLng */ null, /* isPublicParam */ null,
                /* officialWebsiteURLParam */ null, /* baseURLParam */ null, /* leaderboardGroupIdsListParam */ null,
                /* createLeaderboardGroupParam */ "true", /* createRegattaParam */ "false",
                /* boatClassNameParam */ null, /* numberOfRacesParam */ null, false, CompetitorRegistrationType.CLOSED.name(), null,
                /* rankingMetricParam */ null, /* scoringSchemeParam */ null, /* leaderboardDiscardThresholdsParam */ null);
    }

    private Response createEvent() throws ParseException, NotFoundException, NumberFormatException, IOException,
            org.json.simple.parser.ParseException, InvalidDateException {
        return eventsResource.createEvent(uriInfo, randomName, randomName, /* startDateParam */ null,
                /* startDateAsMillis */ null, /* endDateParam */ null, /* endDateAsMillis */ null,
                /* venueNameParam */ randomName, /* venueLat */ null, /* venueLng */ null, /* isPublicParam */ null,
                /* officialWebsiteURLParam */ null, /* baseURLParam */ null, /* leaderboardGroupIdsListParam */ null,
                /* createLeaderboardGroupParam */ "false", /* createRegattaParam */ "false",
                /* boatClassNameParam */ null, /* numberOfRacesParam */ null, false, CompetitorRegistrationType.CLOSED.name(), null,
                /* rankingMetricParam */ null, /* scoringSchemeParam */ null, /* leaderboardDiscardThresholdsParam */ null);
    }

    private Response createEventAtLocation(Position location) throws ParseException, NotFoundException,
            NumberFormatException, IOException, org.json.simple.parser.ParseException, InvalidDateException {
        return eventsResource.createEvent(uriInfo, randomName, randomName, /* startDateParam */ null,
                /* startDateAsMillis */ null, /* endDateParam */ null, /* endDateAsMillis */ null,
                /* venueNameParam */ null, /* venueLat */ "" + location.getLatDeg(),
                /* venueLng */ "" + location.getLngDeg(), /* isPublicParam */ null, /* officialWebsiteURLParam */ null,
                /* baseURLParam */ null, /* leaderboardGroupIdsListParam */ null,
                /* createLeaderboardGroupParam */ "false", /* createRegattaParam */ "false",
                /* boatClassNameParam */ null, /* numberOfRacesParam */ null, false, CompetitorRegistrationType.CLOSED.name(), null,
                /* rankingMetricParam */ null, /* scoringSchemeParam */ null, /* leaderboardDiscardThresholdsParam */ null);
    }

    private Response createEventWithLeaderboardGroupAndRegatta() throws ParseException, NotFoundException,
            NumberFormatException, IOException, org.json.simple.parser.ParseException, InvalidDateException {
        return eventsResource.createEvent(uriInfo, randomName, randomName, /* startDateParam */ null,
                /* startDateAsMillis */ null, /* endDateParam */ null, /* endDateAsMillis */ null,
                /* venueNameParam */ randomName, /* venueLat */ null, /* venueLng */ null, /* isPublicParam */ null,
                /* officialWebsiteURLParam */ null, /* baseURLParam */ null, /* leaderboardGroupIdsListParam */ null,
                /* createLeaderboardGroupParam */ "true", /* createRegattaParam */ "true",
                /* boatClassNameParam */ "A_CAT", /* numberOfRacesParam */ null, false, CompetitorRegistrationType.CLOSED.name(), null,
                /* rankingMetricParam */ null, /* scoringSchemeParam */ null, /* leaderboardDiscardThresholdsParam */ null);
    }
    
    private Response createEventWithScoringShemeRankingMetricAndDiscards() throws ParseException, NotFoundException,
    NumberFormatException, IOException, org.json.simple.parser.ParseException, InvalidDateException {
        return eventsResource.createEvent(uriInfo, randomName, randomName, /* startDateParam */ null,
                /* startDateAsMillis */ null, /* endDateParam */ null, /* endDateAsMillis */ null,
                /* venueNameParam */ randomName, /* venueLat */ null, /* venueLng */ null, /* isPublicParam */ null,
                /* officialWebsiteURLParam */ null, /* baseURLParam */ null, /* leaderboardGroupIdsListParam */ null,
                /* createLeaderboardGroupParam */ "true", /* createRegattaParam */ "true",
                /* boatClassNameParam */ "TP52", /* numberOfRacesParam */ "6", false, CompetitorRegistrationType.CLOSED.name(), null,
                /* rankingMetricParam */ RankingMetrics.ORC_PERFORMANCE_CURVE.name(), /* scoringSchemeParam */ ScoringSchemeType.HIGH_POINT.name(), /* leaderboardDiscardThresholdsParam */ Arrays.asList(2, 4));
    }
    
    private Response getLeaderboard(String name) {
        return leaderboardsResource.getLeaderboard(name, AbstractLeaderboardsResource.ResultStates.Final, null, null,
                /* competitorAndBoatIdsOnly */ false);
    }
    
    private boolean hasAtLeastOneCourseArea(JSONObject objEvent) {
        String strCourseAreaId = getDefaultCourseAreaId(objEvent);
        return validateUUID(strCourseAreaId);
    }

    private String getDefaultCourseAreaId(JSONObject objEvent) {
        JSONArray arrCourseAreas = getCourseAreas(objEvent);
        assertTrue(arrCourseAreas.size() == 1);
        JSONObject courseArea = (JSONObject) arrCourseAreas.get(0);
        String strCourseAreaId = (String) courseArea.get("id");
        return strCourseAreaId;
    }

    private boolean hasDefaultLeaderboardGroup(JSONObject objEvent) {
        String eventName = (String) objEvent.get("name");
        return containsObjectWithAttrbuteNameAndValue(getLeaderboardGroups(objEvent), "name", eventName);
    }
    
    private boolean containsObjectWithAttrbuteNameAndValue(JSONArray array, String attributeName, String value){
        return array.stream().filter(o -> ((JSONObject) o).get(attributeName).equals(value)).findFirst().isPresent();
    }

    private JSONObject getRegatta(String eventName) throws WebApplicationException, IOException {
        Response regattasResponse = regattasResource.getRegatta(eventName, null);
        return toJSONObject(StreamingOutputUtil.getEntityAsString(regattasResponse.getEntity()));
    }

    private boolean isValidCreateEventResponse(Response response) throws WebApplicationException, IOException {
        String id = getIdFromCreateEventResponse(response);
        return validateUUID(id);
    }
    
    private JSONArray getCourseAreasOfEvent(String strEventId) throws WebApplicationException, IOException {
        JSONObject objEvent = getEvent(strEventId);
        JSONArray arrCourseAreas = getCourseAreas(objEvent);
        return arrCourseAreas;
    }

    private JSONObject getEvent(String strEventId) throws WebApplicationException, IOException {
        String jsonEvent = getEventAsString(strEventId);
        JSONObject objEvent = toJSONObject(jsonEvent);
        return objEvent;
    }

    private boolean leaderBoardWithNameExists(String name) throws WebApplicationException, IOException {
        Response leaderboardResponse = getLeaderboard(name);
        JSONObject objLeaderboard = getLeaderboardAsJsonObject(leaderboardResponse);
        String strLeaderboardName = (String) objLeaderboard.get("name");
        return strLeaderboardName.equals(name);
    }

    private JSONObject getLeaderboardAsJsonObject(Response leaderboardResponse) throws WebApplicationException, IOException {
        String strLeaderboardGroup = StreamingOutputUtil.getEntityAsString(leaderboardResponse.getEntity());
        JSONObject objLeaderboardGroup = toJSONObject(strLeaderboardGroup);
        return objLeaderboardGroup;
    }

    private JSONObject getLeaderboardGroup(String strDefaultLeaderboardGroupName) throws WebApplicationException, IOException {
        Response leaderboardGroupsResponse = leaderboardGroupsResource.getLeaderboardGroup(strDefaultLeaderboardGroupName);
        return toJSONObject(StreamingOutputUtil.getEntityAsString(leaderboardGroupsResponse.getEntity()));
    }

    private JSONArray getLeaderboardGroups(JSONObject objEvent) {
        JSONArray arrLgs = (JSONArray) objEvent.get("leaderboardGroups");
        return arrLgs;
    }

    private JSONArray getCourseAreas(JSONObject objEvent) {
        JSONObject objVenue = (JSONObject) objEvent.get("venue");
        JSONArray arrCourseAreas = (JSONArray) objVenue.get("courseAreas");
        return arrCourseAreas;
    }

    private JSONObject toJSONObject(String strEvent) {
        return (JSONObject) JSONValue.parse(strEvent);
    }

    private boolean validateUUID(String eventId) {
        return UUID.fromString(eventId) != null;
    }

    private String randomName() {
        return RandomStringUtils.randomAlphanumeric(6).toUpperCase();
    }
    

    private String getIdFromCreateEventResponse(Response createEventResponse) throws WebApplicationException, IOException {
        return (String) toJSONObject(StreamingOutputUtil.getEntityAsString(createEventResponse.getEntity())).get("eventid");
    }

    private String getEventAsString(String eventId) throws WebApplicationException, IOException {
        return StreamingOutputUtil.getEntityAsString(eventsResource.getEvent(eventId, null).getEntity());
    }

}
