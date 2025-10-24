package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.domain.common.CompetitorRegistrationType.CLOSED;
import static com.sap.sailing.domain.common.CompetitorRegistrationType.OPEN_UNMODERATED;
import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static com.sap.sailing.selenium.api.core.ApiContext.createApiContext;
import static com.sap.sailing.selenium.api.core.GpsFixMoving.createFix;
import static com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage.goToPage;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.DeviceStatus;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.EventApi.Event;
import com.sap.sailing.selenium.api.event.GpsFixApi;
import com.sap.sailing.selenium.api.event.LeaderboardApi;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.api.event.UserGroupApi;
import com.sap.sailing.selenium.api.event.UserGroupApi.UserGroup;
import com.sap.sailing.selenium.api.regatta.Competitor;
import com.sap.sailing.selenium.api.regatta.RaceColumn;
import com.sap.sailing.selenium.api.regatta.Regatta;
import com.sap.sailing.selenium.api.regatta.RegattaApi;
import com.sap.sailing.selenium.api.regatta.RegattaDeviceStatus;
import com.sap.sailing.selenium.api.regatta.RegattaDeviceStatus.CompetitorDeviceStatus;
import com.sap.sailing.selenium.api.regatta.RegattaRaces;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.SecurityService;

public class RegattaApiTest extends AbstractSeleniumTest {

    private static final String DAGOBERTS_PASSWORD = "daiHF(*&#($o97sy1337";
    private static final String DONALDS_PASSWORD = "daasdf][];l*&isy0815";
    private static String EVENT_NAME = "<ppp> loggingsession";
    private static String OTHER_EVENT_NAME = "<xxx> loggingsession";
    private static String BOAT_CLASS = "75QMNATIONALEKREUZER";

    private final EventApi eventApi = new EventApi();
    private final RegattaApi regattaApi = new RegattaApi();
    private final LeaderboardApi leaderboardApi = new LeaderboardApi();
    private final UserGroupApi usergroupApi = new UserGroupApi();
    private final GpsFixApi gpsFixApi = new GpsFixApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
        super.setUp();
    }

    @SeleniumTestCase
    public void testGetRegattaForCreatedEvent() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CLOSED, "default");
        Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        JSONArray series = (JSONArray) regatta.get("series");
        JSONObject serie = (JSONObject) series.get(0);
        JSONArray fleets = (JSONArray) serie.get("fleets");
        JSONObject trackedRaces = (JSONObject) serie.get("trackedRaces");
        assertEquals(EVENT_NAME, regatta.getName(), "read: regatta.name is different");
        assertNull(regatta.getStartDate(), "read: regatta.startDate should be null");
        assertNull(regatta.getEndDate(), "read: regatta.endDate should be null");
        assertEquals("LOW_POINT", regatta.getScoringSystem(), "read: regatta.scoringSystem is different");
        assertEquals(BOAT_CLASS, regatta.getBoatClass(), "read: regatta.boeatclass is different");
        assertFalse(regatta.getCourseAreaId().isEmpty(), "read: regatta.courseAreaId is missing");
        assertEquals(false, regatta.canBoatsOfCompetitorsChangePerRace(),
                "read: regatta.canBoatsOfCompetitorsChangePerRace should be false");
        assertEquals(CLOSED, regatta.getCompetitorRegistrationType(),
                "read: regatta.competitorRegistrationType is different");
        assertTrue(regatta.isUseStartTimeInference());
        assertFalse(regatta.isControlTrackingFromStartAndFinishTimes());
        assertEquals(1, series.size(), "read: reagtta.series should have 1 entry");
        assertEquals(1, fleets.size(), "read: reagtta.fleets should have 1 entry");
        assertNotNull(trackedRaces, "read: reagtta.trackedRaces is missing");
    }
    
    @SeleniumTestCase
    public void testUpdateRegatta() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        
        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CLOSED, "default");
        final TimePoint start = new MillisecondsTimePoint(1337);
        final TimePoint end = new MillisecondsTimePoint(2337);
        regattaApi.updateRegatta(ctx, EVENT_NAME, start, end,
                null, null, false, true, null, null);
        
        Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        
        assertEquals(start, regatta.getStartDate());
        assertEquals(end, regatta.getEndDate());
        assertFalse(regatta.isUseStartTimeInference());
        assertTrue(regatta.isControlTrackingFromStartAndFinishTimes());
    }

    @SeleniumTestCase
    public void testGetRacesForRegattaForCreateEvent() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);

        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CLOSED, "default");
        RegattaRaces regattaRaces = regattaApi.getRegattaRaces(ctx, EVENT_NAME);

        assertEquals(EVENT_NAME, regattaRaces.getRegattaName(), "read: regatta is different");
        //assertEquals("read: reagtta.series should have 0 entries", 0, regattaRaces.getRaces().length);
        
        RaceColumn[] raceColumns = regattaApi.addRaceColumn(ctx, EVENT_NAME, "R", 1);
        leaderboardApi.startRaceLogTracking(ctx, EVENT_NAME, raceColumns[0].getRaceName(), "Default");
        regattaRaces = regattaApi.getRegattaRaces(ctx, EVENT_NAME, r -> r.getRaces().length > 0);
        assertEquals(1, regattaRaces.getRaces().length, "read: reagtta.series should have 1 entries");
    }

    @SeleniumTestCase
    public void testCreateAndAddCompetitor() {
        final String competitorName = "Max Mustermann";
        final String competitorNationality = "USA";
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);

        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CLOSED, "default");
        Competitor competitor = regattaApi.createAndAddCompetitor(ctx, EVENT_NAME, BOAT_CLASS, "test@de",
                competitorName, competitorNationality);
        assertNotNull(competitor.getId(), "read: competitor.id is missing");
        assertEquals(competitorName, competitor.getName(), "read: competitor.name is different");
        assertEquals(competitorName, competitor.getShortName(), "read: competitor.shortName is different");
        assertEquals(competitorNationality, competitor.getNationality(), "read: competitor.nationality is different");
        assertNotNull(competitor.getTeam(), "read: competitor.team should not be emtpy");
        assertNotNull(competitor.getBoat(), "read: competitor.boat should not be empty");
        assertNotNull(competitor.getBoat().getBoatClass(), "read: competitor.boat.boatClass should not be empty");
        assertEquals(BOAT_CLASS, competitor.getBoat().getBoatClass().getName(),
                "read: competitor.boat.boatClass.name is differnet");
    }

    @SeleniumTestCase
    public void testGetCompetitors() {
        final String competitor1Name = "Max Mustermann";
        final String competitor2Name = "Hans Albatros";
        final String competitor1Nationality = "GER";
        final String competitor2Nationality = "USA";
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CLOSED, "default");
        final Competitor competitor1 = regattaApi.createAndAddCompetitor(ctx, EVENT_NAME, BOAT_CLASS, "test@de",
                competitor1Name, competitor1Nationality);
        assertNotNull(competitor1, "Competitor1 should not be null");
        final Competitor competitor2 = regattaApi.createAndAddCompetitor(ctx, EVENT_NAME, BOAT_CLASS, "test@de",
                competitor2Name, competitor2Nationality);
        assertNotNull(competitor2, "Competitor2 should not be null");
        final Competitor[] competitors = regattaApi.getCompetitors(ctx, EVENT_NAME);
        assertNotNull(competitors, "read: list of competitors should not be null");
        assertNotEquals(0, competitors.length, "read: list of competitors should not be empty");
        assertEquals(2, competitors.length, "read: list of competitors should contains 2 comeptitors");
        for (Competitor competitor : competitors) {
            assertTrue(competitor1Name.equals(competitor.getName()) || competitor2Name.equals(competitor.getName()),
                    "competitor name " + competitor.getName() + " is wrong");
        }
    }

    @SeleniumTestCase
    public void testAddRaceColumns() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);

        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CLOSED, "default");
        RaceColumn[] result = regattaApi.addRaceColumn(ctx, EVENT_NAME, "T", 5);
        assertEquals("Default", result[0].getSeriesName(), "read: racecolumn.seriesname is different");
        assertEquals("T1", result[0].getRaceName(), "read: racecolumn.racename is different");
        assertEquals("Default", result[4].getSeriesName(), "read: racecolumn.seriesname is different");
        assertEquals("T5", result[4].getRaceName(), "read: racecolumn.racename is different");
    }
    
    @SeleniumTestCase
    public void testRegisterExistingCompetitorWithSecret() {
        final AdminConsolePage adminConsole = goToPage(getWebDriver(), getContextRoot());
        adminConsole.goToLocalServerPanel().setSelfServiceServer(true);
        SecurityApi securityApi = new SecurityApi();
        final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), ApiContext.SECURITY_CONTEXT);
        securityApi.createUser(adminSecurityCtx, "donald", "Donald Duck", null, DONALDS_PASSWORD);
        final ApiContext userWithCompetitorCtx = createApiContext(getContextRoot(), SERVER_CONTEXT, "donald", DONALDS_PASSWORD);
        final ApiContext userWithCompetitorAndSecurityCtx = createApiContext(getContextRoot(),
                ApiContext.SECURITY_CONTEXT, "donald", DONALDS_PASSWORD);
        securityApi.createUser(adminSecurityCtx, "dagobert", "Dagobert Duck", null, DAGOBERTS_PASSWORD);
        final ApiContext userOwningEventCtx = createApiContext(getContextRoot(), SERVER_CONTEXT, "dagobert", DAGOBERTS_PASSWORD);
        // TODO we currently can't just create a competitor using the API. This is only possible by using a temporary Event/Regatta.
        eventApi.createEvent(userWithCompetitorCtx, EVENT_NAME, BOAT_CLASS, CLOSED, "default");
        Competitor competitor = regattaApi.createAndAddCompetitor(userWithCompetitorCtx, EVENT_NAME, BOAT_CLASS, null, "donald", "USA");
        // ensure the reader can actually see the competitor
        UserGroup group = usergroupApi.getUserGroupByName(userWithCompetitorAndSecurityCtx, "donald"+SecurityService.TENANT_SUFFIX);
        usergroupApi.addRoleToGroup(userWithCompetitorAndSecurityCtx, group.getGroupId(),
                UUID.fromString(/* sailing viewer role id */"c42948df-517b-45cb-9fa9-d1e79f18e115"), true);
        // This is the event to register the existing competitor for
        Event eventToRegisterExistingCompetitor = eventApi.createEvent(userOwningEventCtx, OTHER_EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.OPEN_UNMODERATED, "default");
        regattaApi.addCompetitor(userWithCompetitorCtx, OTHER_EVENT_NAME, competitor.getId(), Optional.of(eventToRegisterExistingCompetitor.getSecret()));
        Competitor[] competitors = regattaApi.getCompetitors(userOwningEventCtx, OTHER_EVENT_NAME);
        assertEquals(1, competitors.length);
        assertEquals(competitor.getId(), competitors[0].getId());
    }

    @SeleniumTestCase
    public void testRegisterExistingCompetitorWithBadSecret() {
        final AdminConsolePage adminConsole = goToPage(getWebDriver(), getContextRoot());
        adminConsole.goToLocalServerPanel().setSelfServiceServer(true);

        SecurityApi securityApi = new SecurityApi();
        final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), ApiContext.SECURITY_CONTEXT);
        securityApi.createUser(adminSecurityCtx, "donald", "Donald Duck", null, DONALDS_PASSWORD);
        final ApiContext userWithCompetitorCtx = createApiContext(getContextRoot(), SERVER_CONTEXT, "donald",
                DONALDS_PASSWORD);
        securityApi.createUser(adminSecurityCtx, "dagobert", "Dagobert Duck", null, DAGOBERTS_PASSWORD);
        final ApiContext userOwningEventCtx = createApiContext(getContextRoot(), SERVER_CONTEXT, "dagobert",
                DAGOBERTS_PASSWORD);

        final ApiContext userWithCompetitorAndSecurityCtx = createApiContext(getContextRoot(),
                ApiContext.SECURITY_CONTEXT, "donald", DONALDS_PASSWORD);

        // TODO we currently can't just create a competitor using the API. This is only possible by using a temporary
        // Event/Regatta.
        eventApi.createEvent(userWithCompetitorCtx, EVENT_NAME, BOAT_CLASS, CLOSED, "default");
        Competitor competitor = regattaApi.createAndAddCompetitor(userWithCompetitorCtx, EVENT_NAME, BOAT_CLASS, null,
                "donald", "USA");

        // ensure the reader can actually see the competitor
        UserGroup group = usergroupApi.getUserGroupByName(userWithCompetitorAndSecurityCtx, "donald"+SecurityService.TENANT_SUFFIX);
        usergroupApi.addRoleToGroup(userWithCompetitorAndSecurityCtx, group.getGroupId(),
                UUID.fromString(/* sailing viewer role id */"c42948df-517b-45cb-9fa9-d1e79f18e115"), true);

        // This is the event to register the existing competitor for
        Event eventToRegisterExistingCompetitor = eventApi.createEvent(userOwningEventCtx, OTHER_EVENT_NAME, BOAT_CLASS,
                CompetitorRegistrationType.OPEN_UNMODERATED, "default");
        try {
            regattaApi.addCompetitor(userWithCompetitorCtx, OTHER_EVENT_NAME, competitor.getId(),
                    Optional.of(eventToRegisterExistingCompetitor.getSecret() + "bad"));
            Assertions.fail("Expected error because of bad secret.");
        } catch (RuntimeException e) {
            Assertions.assertTrue(e.getMessage().contains("rc=401"), "Exepcted HTTP 401 - Unauthorized");
        }

        Competitor[] competitors = regattaApi.getCompetitors(userOwningEventCtx, OTHER_EVENT_NAME);

        assertEquals(0, competitors.length);
    }
    
    @SeleniumTestCase
    public void testTrackingDeviceStatus() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        Event event = eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, OPEN_UNMODERATED, "default");
        final UUID competitor1DeviceID = UUID.randomUUID();
        final Competitor competitor1 = regattaApi.createAndAddCompetitorWithSecret(ctx, EVENT_NAME, BOAT_CLASS, "test@de", "Max",
                "USA", event.getSecret(), competitor1DeviceID);
        final UUID competitor2DeviceID = UUID.randomUUID();
        final Competitor competitor2 = regattaApi.createAndAddCompetitorWithSecret(ctx, EVENT_NAME, BOAT_CLASS, "test@de",
                "Peter", "USA", event.getSecret(), competitor2DeviceID);

        final long timeMillisCompetitor1 = System.currentTimeMillis();
        gpsFixApi.postGpsFix(ctx, competitor1DeviceID, createFix(49.121, 8.5987, timeMillisCompetitor1, 10.0, 180.0));
        final long timeMillisCompetitor2 = System.currentTimeMillis() - 50;
        gpsFixApi.postGpsFix(ctx, competitor2DeviceID, createFix(49.120, 8.5988, timeMillisCompetitor2, 10.0, 180.0));
        final RegattaDeviceStatus trackingDeviceStatus = regattaApi.getTrackingDeviceStatus(ctx, EVENT_NAME);
        final List<CompetitorDeviceStatus> competitorsTrackingDeviceStatus = trackingDeviceStatus.getCompetitors();
        assertEquals(2, competitorsTrackingDeviceStatus.size());
        final Map<UUID, List<DeviceStatus>> statusByCompetitorUUID = competitorsTrackingDeviceStatus.stream()
                .collect(Collectors.toMap(c -> UUID.fromString(c.getCompetitorId()), c -> c.getDeviceStatuses()));
        assertTrue(statusByCompetitorUUID.containsKey(competitor1.getId()));
        assertTrue(statusByCompetitorUUID.containsKey(competitor2.getId()));
        List<DeviceStatus> competitor1DeviceStatuses = statusByCompetitorUUID.get(competitor1.getId());
        assertEquals(1, competitor1DeviceStatuses.size());
        DeviceStatus competitor1DeviceStatus = competitor1DeviceStatuses.iterator().next();
        assertEquals(competitor1DeviceID.toString(), competitor1DeviceStatus.getDeviceId());
        assertEquals(timeMillisCompetitor1, competitor1DeviceStatus.getLastGPSFix().getTime());
        List<DeviceStatus> competitor2DeviceStatuses = statusByCompetitorUUID.get(competitor2.getId());
        assertEquals(1, competitor1DeviceStatuses.size());
        DeviceStatus competitor2DeviceStatus = competitor2DeviceStatuses.iterator().next();
        assertEquals(competitor2DeviceID.toString(), competitor2DeviceStatus.getDeviceId());
        assertEquals(timeMillisCompetitor2, competitor2DeviceStatus.getLastGPSFix().getTime());
    }
}
