package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.regatta.Competitor;
import com.sap.sailing.selenium.api.regatta.CompetitorsApi;
import com.sap.sailing.selenium.api.regatta.RegattaApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class CompetitorApiTest extends AbstractSeleniumTest {

    private static final String EVENT_NAME = "Competitor Test Regatta";
    private static final String BOAT_CLASS = "GC 32";
    private static final String COMPETITOR_NAME = "Donald Duck";

    private final EventApi eventApi = new EventApi();
    private final RegattaApi regattaApi = new RegattaApi();
    private final CompetitorsApi competitorApi = new CompetitorsApi();

    private ApiContext adminCtx;

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot());
        super.setUp();
        adminCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(adminCtx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "Some special place");
    }

    @SeleniumTestCase
    public void testGetCompetitorWithOldSerializer() {
        final Competitor competitor = regattaApi.createAndAddCompetitor(adminCtx, EVENT_NAME, BOAT_CLASS, "test@test",
                COMPETITOR_NAME, "USA");
        final Competitor competitorReloaded = competitorApi.getCompetitor(adminCtx, competitor.getId());
        assertEquals(competitor.getId(), competitorReloaded.getId());
        assertEquals(competitor.getName(), competitorReloaded.getName());
        assertEquals(competitor.getNationality(), competitorReloaded.getNationality());
        assertEquals(competitor.getColor(), competitorReloaded.getColor());
        assertEquals(competitor.getFlagImageUri(), competitorReloaded.getFlagImageUri());
        assertEquals(competitor.getTeam().getTeamImageUri(), competitorReloaded.getTeam().getTeamImageUri());
        assertEquals(competitor.getShortName(), competitorReloaded.getShortName());
        assertEquals(competitor.getTimeOnTimeFactor(), competitorReloaded.getTimeOnTimeFactor());
        assertEquals(competitor.getTimeOnDistanceAllowanceInSecondsPerNauticalMile(),
                competitorReloaded.getTimeOnDistanceAllowanceInSecondsPerNauticalMile());
        assertEquals(competitor.getNationalityISO2(), competitorReloaded.getNationalityISO2());
        assertEquals(competitor.getNationalityISO3(), competitorReloaded.getNationalityISO3());
    }

    @SeleniumTestCase
    public void testUpdateCompetitor() {
        final Competitor competitor = regattaApi.createAndAddCompetitor(adminCtx, EVENT_NAME, BOAT_CLASS, "test@test",
                COMPETITOR_NAME, "USA");
        Competitor competitorU1 = competitorApi.updateCompetitor(adminCtx, competitor.getId(),
                mapOf("displayColor", "#ABCDEF"));
        assertEquals(competitor.getId(), competitorU1.getId());
        assertEquals("#ABCDEF", competitorU1.getColor());
        assertEquals(competitor.getName(), competitorU1.getName());
        Competitor competitorU2 = competitorApi.updateCompetitor(adminCtx, competitor.getId(),
                mapOf("name", "Dagobert", "shortName", "D"));
        assertEquals("#ABCDEF", competitorU2.getColor());
        assertEquals("Dagobert", competitorU2.getName());
        assertEquals("D", competitorU2.getShortName());
        Competitor competitorU3 = competitorApi.updateCompetitor(adminCtx, competitor.getId(), mapOf("nationality",
                "GER", "flagImageUri", "http://flagimage.url", "teamImageUri", "http://teamimage.url"));
        assertEquals("#ABCDEF", competitorU3.getColor());
        assertEquals("Dagobert", competitorU3.getName());
        assertEquals("D", competitorU2.getShortName());
        assertEquals("GER", competitorU3.getNationality());
        assertEquals("http://flagimage.url", competitorU3.getFlagImageUri());
        assertEquals("http://teamimage.url", competitorU3.getTeam().getTeamImageUri());
        Competitor competitorU4 = competitorApi.updateCompetitor(adminCtx, competitor.getId(),
                mapOf("timeOnTimeFactor", 2.4, "timeOnDistanceAllowanceInSecondsPerNauticalMile", 10000));
        assertEquals("#ABCDEF", competitorU4.getColor());
        assertEquals("Dagobert", competitorU4.getName());
        assertEquals("D", competitorU2.getShortName());
        assertEquals("GER", competitorU4.getNationality());
        assertEquals("http://flagimage.url", competitorU4.getFlagImageUri());
        assertEquals("http://teamimage.url", competitorU4.getTeam().getTeamImageUri());
        assertEquals(2.4, competitorU4.getTimeOnTimeFactor());
        assertEquals(10000.0, competitorU4.getTimeOnDistanceAllowanceInSecondsPerNauticalMile());
        Competitor competitorU5 = competitorApi.updateCompetitor(adminCtx, competitor.getId(),
                mapOf("searchTag", "atag"));
        assertEquals("#ABCDEF", competitorU5.getColor());
        assertEquals("Dagobert", competitorU5.getName());
        assertEquals("D", competitorU2.getShortName());
        assertEquals("GER", competitorU5.getNationality());
        assertEquals("http://flagimage.url", competitorU5.getFlagImageUri());
        assertEquals("http://teamimage.url", competitorU5.getTeam().getTeamImageUri());
        assertEquals(2.4, competitorU5.getTimeOnTimeFactor());
        assertEquals(10000.0, competitorU5.getTimeOnDistanceAllowanceInSecondsPerNauticalMile());
        // check final state by reloading competitor
        Competitor competitorReloaded = competitorApi.getCompetitor(adminCtx, competitor.getId());
        assertEquals("#ABCDEF", competitorReloaded.getColor());
        assertEquals("Dagobert", competitorReloaded.getName());
        assertEquals("D", competitorReloaded.getShortName());
        assertEquals("GER", competitorReloaded.getNationality());
        assertEquals("http://flagimage.url", competitorReloaded.getFlagImageUri());
        assertEquals("http://teamimage.url", competitorReloaded.getTeam().getTeamImageUri());
        assertEquals(2.4, competitorReloaded.getTimeOnTimeFactor());
        assertEquals(10000.0, competitorU5.getTimeOnDistanceAllowanceInSecondsPerNauticalMile());
    }

    @SeleniumTestCase
    public void testUpdateCompetitorValuesToNull() {
        final Competitor competitor = regattaApi.createAndAddCompetitor(adminCtx, EVENT_NAME, BOAT_CLASS, "test@test",
                COMPETITOR_NAME, "USA");
        Competitor competitorU1 = competitorApi.updateCompetitor(adminCtx, competitor.getId(),
                mapOf("name", "Dagobert", "shortName", "D", "email", "test@test2", "nationality", "GER", "flagImageUri",
                        "http://flagimage.url", "teamImageUri", "http://teamimage.url", "timeOnTimeFactor", 2.4, "displayColor", "#ABCDEF",
                        "timeOnDistanceAllowanceInSecondsPerNauticalMile", 10000, "searchTag", "atag"));
        assertNotNull(competitorU1.getName());
        assertNotNull(competitorU1.getShortName());
        assertNotNull(competitorU1.getFlagImageUri());
        assertNotNull(competitorU1.getTeam().getTeamImageUri());
        assertNotNull(competitorU1.getNationality());
        assertNotNull(competitorU1.getTimeOnTimeFactor());
        assertNotNull(competitorU1.getTimeOnDistanceAllowanceInSecondsPerNauticalMile());
        assertNotNull(competitorU1.getColor());
        Competitor competitorU2 = competitorApi.updateCompetitor(adminCtx, competitor.getId(),
                mapOf("name", null, "shortName", null, "email", null, "nationality", null, "flagImageUri", null,
                        "teamImageUri", null, "timeOnTimeFactor", null, "displayColor", null, 
                        "timeOnDistanceAllowanceInSecondsPerNauticalMile", null, "searchTag", null));
        assertNull(competitorU2.getName());
        assertNull(competitorU2.getShortName());
        assertNull(competitorU2.getFlagImageUri());
        assertNull(competitorU2.getTeam().getTeamImageUri());
        assertNull(competitorU2.getTimeOnTimeFactor());
        assertNull(competitorU2.getTimeOnDistanceAllowanceInSecondsPerNauticalMile());
        assertNull(competitorU2.getColor());
    }

    private Map<String, Object> mapOf(Object... args) {
        assertTrue(args.length % 2 == 0);
        Map<String, Object> map = new HashMap<>();
        for (int i = 0; i < args.length; i += 2) {
            map.put((String) args[i], args[i + 1]);
        }
        return map;
    }
}
