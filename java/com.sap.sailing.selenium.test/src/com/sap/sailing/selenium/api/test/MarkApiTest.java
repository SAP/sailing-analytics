package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SECURITY_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static com.sap.sailing.selenium.api.core.ApiContext.createApiContext;
import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.HttpException;
import com.sap.sailing.selenium.api.coursetemplate.CourseConfiguration;
import com.sap.sailing.selenium.api.coursetemplate.CourseConfigurationApi;
import com.sap.sailing.selenium.api.coursetemplate.MarkConfiguration;
import com.sap.sailing.selenium.api.coursetemplate.WaypointWithMarkConfiguration;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.LeaderboardApi;
import com.sap.sailing.selenium.api.event.Mark;
import com.sap.sailing.selenium.api.event.MarkApi;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.api.regatta.Course;
import com.sap.sailing.selenium.api.regatta.RaceColumn;
import com.sap.sailing.selenium.api.regatta.Regatta;
import com.sap.sailing.selenium.api.regatta.RegattaApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class MarkApiTest extends AbstractSeleniumTest {

    private static final String DONALDS_PASSWORD = "da)(&$0897KFDisy0815";
    private static String EVENT_NAME = "MarkApiTestEvent";
    private static String BOAT_CLASS = "Flying Dutchman";
    private static String FLEET = "Default";

    private final EventApi eventApi = new EventApi();
    private final RegattaApi regattaApi = new RegattaApi();
    private final MarkApi markApi = new MarkApi();
    private final LeaderboardApi leaderboardApi = new LeaderboardApi();
    private final CourseConfigurationApi courseConfigurationApi = new CourseConfigurationApi();
    private final SecurityApi securityApi = new SecurityApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
    }

    @SeleniumTestCase
    public void testAddMarkToRegatta() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);

        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
        final Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        final Mark mark = markApi.addMarkToRegatta(ctx, regatta.getName(), "Startboat");

        assertNotNull(mark, "Mark result should not be null");
        assertNotNull(mark.getMarkId(), "Id of created mark should not be null");
    }

    /**
     * Negative test scenarios. Checks all common 'not found' errors.
     */
    @SeleniumTestCase
    public void testRevokeMarkOnRegattaMarkNotFound() {
        assertThrows(HttpException.class, () -> {
            final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
            eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
            final Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
            final RaceColumn race = regattaApi.addRaceColumn(ctx, EVENT_NAME, null, 1)[0];
            try {
                markApi.revokeMarkOnRegatta(ctx, "blah", race.getRaceName(), FLEET, UUID.randomUUID());
                assertFalse(true);
            } catch (HttpException e) {
                assertTrue(e.getMessage().contains("Could not find a regatta with name"));
            }
            try {
                markApi.revokeMarkOnRegatta(ctx, regatta.getName(), "Blub", FLEET, UUID.randomUUID());
                assertFalse(true);
            } catch (HttpException e) {
                assertTrue(e.getMessage().contains("Could not find a race with raceColumn"));
            }
            try {
                markApi.revokeMarkOnRegatta(ctx, regatta.getName(), race.getRaceName(), "Blume", UUID.randomUUID());
                assertFalse(true);
            } catch (HttpException e) {
                assertTrue(e.getMessage().contains("Could not find a race with raceColumn"));
            }
            try {
                markApi.revokeMarkOnRegatta(ctx, regatta.getName(), race.getRaceName(), FLEET, UUID.randomUUID());
                assertFalse(true);
            } catch (HttpException e) {
                assertTrue(e.getMessage().contains("Could not find a mark with id"));
                throw e;
            }
            assertFalse(true);
        });
    }

    /**
     * Test to check if mark is already used by a tracking of a race.
     * Expected result: HTTP exception, that mark is already used in a tacked race.
     */
    @SeleniumTestCase
    public void testRevokeMarkOnRegattaAlreadyTracked() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
        final Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        final Mark mark = markApi.addMarkToRegatta(ctx, regatta.getName(), "Startboat");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, EVENT_NAME, null, 1)[0];
        leaderboardApi.startRaceLogTracking(ctx, regatta.getName(), race.getRaceName(), FLEET);
        try {
            markApi.revokeMarkOnRegatta(ctx, regatta.getName(), race.getRaceName(), FLEET, mark.getMarkId());
            assertFalse(true);
        } catch (HttpException e) {
            assertTrue(e.getMessage().contains(" is already tracked"));
        }
    }

    /**
     * Positive rest case. Revoke mark on regatte without errors.
     */
    @SeleniumTestCase
    public void testRevokeMarkOnRegatta() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
        final Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        final Mark mark = markApi.addMarkToRegatta(ctx, regatta.getName(), "Startboat");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, EVENT_NAME, null, 1)[0];
        markApi.revokeMarkOnRegatta(ctx, regatta.getName(),
                race.getRaceName(), FLEET, mark.getMarkId());
    }

    /**
     * Creates a regatta with two races. Create a course for 2nd race with test mark inside.
     * Try to revoke the mark on 1st race. 
     * Expected result is a HTTP exception, that the is mark already in use in 2nd race.
     */
    @SeleniumTestCase
    public void testRevokeMarkAlreadyUsedInCourse() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
        final Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        final Mark mark = markApi.addMarkToRegatta(ctx, regatta.getName(), "Testboat");
        final RaceColumn[] races = regattaApi.addRaceColumn(ctx, EVENT_NAME, null, 2);

        final String pinEndName = "Start/Finish Pin";
        final MarkConfiguration sfp = MarkConfiguration.createFreestyle(null, null, null, pinEndName, "SFP", null, null,
                null, MarkType.BUOY.name(), Collections.emptySet());
        sfp.setFixedPosition(47.159776, 27.5891346);
        sfp.setStoreToInventory(true);
        final MarkConfiguration sfb = MarkConfiguration.createFreestyle(null, null, null, "Start/Finish Boat", "SFB",
                null, null, null, MarkType.STARTBOAT.name(), Collections.emptySet());
        sfp.setStoreToInventory(true);
        final MarkConfiguration sft = MarkConfiguration.createMarkBased(mark.getMarkId(), null);
        sft.setFixedPosition(47.159776, 37.5891346);
        final WaypointWithMarkConfiguration wp1 = new WaypointWithMarkConfiguration("Start", "S",
                PassingInstruction.Gate, Arrays.asList(sfp.getId(), sfb.getId()));
        final WaypointWithMarkConfiguration wp2 = new WaypointWithMarkConfiguration("Finish", "F",
                PassingInstruction.Gate, Arrays.asList(sfp.getId(), sfb.getId()));
        final WaypointWithMarkConfiguration wp3 = new WaypointWithMarkConfiguration("Testboat", "T",
                PassingInstruction.Gate, Arrays.asList(sft.getId()));
        final CourseConfiguration courseConfiguration = new CourseConfiguration("my-course",
                Arrays.asList(sfp, sft, sfb), Arrays.asList(wp1, wp2, wp3));
        courseConfigurationApi.createCourse(ctx, courseConfiguration, regatta.getName(), races[1].getRaceName(), FLEET);

        final Course course = regattaApi.getCourse(ctx, regatta.getName(), races[1].getRaceName(), FLEET);
        assertEquals(courseConfiguration.getName(), course.getName());
        try {
            markApi.revokeMarkOnRegatta(ctx, regatta.getName(), races[0].getRaceName(), FLEET, mark.getMarkId());
            assertFalse(true);
        } catch (HttpException e) {
            assertTrue(e.getMessage().contains(" is already used in race R2/" + FLEET));
        }
    }

    /**
     * Test to check if revoke also works on fixed marks. In parallel it checks if only specific mark is removed
     * from regatta/race/fleet.
     */
    @SeleniumTestCase
    public void testRevokeMarkWithFixOnRegatta() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
        final Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        // create mark 1
        final Mark mark1 = markApi.addMarkToRegatta(ctx, regatta.getName(), "Startboat1");
        // create mark 2
        final Mark mark2 = markApi.addMarkToRegatta(ctx, regatta.getName(), "Startboat2");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, EVENT_NAME, null, 1)[0];
        // add a fix to mark 1
        markApi.addMarkFix(ctx, EVENT_NAME, race.getRaceName(), FLEET, mark1.getMarkId(), /* markTemplateId */ null,
                /* markPropertiesId */ null, 9.12, .599, currentTimeMillis());
        // revoke mark 1
        markApi.revokeMarkOnRegatta(ctx, regatta.getName(), race.getRaceName(), FLEET, mark1.getMarkId());

        leaderboardApi.startRaceLogTracking(ctx, regatta.getName(), race.getRaceName(), FLEET);
        final CourseConfiguration reloadedCourseConfigurationAfterTrackingStarted = courseConfigurationApi
                .createCourseConfigurationFromCourse(ctx, regatta.getName(), race.getRaceName(), FLEET, null);

        final MarkConfiguration markConfiguration1 = reloadedCourseConfigurationAfterTrackingStarted
                .getMarkConfigurationByEffectiveName("Startboat1");
        MarkConfiguration markConfiguration2 = reloadedCourseConfigurationAfterTrackingStarted
                .getMarkConfigurationByEffectiveName("Startboat2");

        // check that mark 1 was revoked (and therefore configuration for mark 1 is not existing)
        assertNull(markConfiguration1);
        // check that mark 2 is still there (and therefore a configuration)
        assertEquals(markConfiguration2.getMarkId(), mark2.getMarkId());
    }

    @SeleniumTestCase
    public void testAddMarkFix() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);

        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
        final Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        final Mark mark = markApi.addMarkToRegatta(ctx, regatta.getName(), "Startboat");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, EVENT_NAME, null, 1)[0];
        markApi.addMarkFix(ctx, EVENT_NAME, race.getRaceName(), FLEET, mark.getMarkId(), /* markTemplateId */ null,
                /* markPropertiesId */ null, 9.12, .599, currentTimeMillis());
    }

    @SeleniumTestCase
    public void testAddMarkFixWithMarkTemplateAndMarkProperties() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);

        eventApi.createEvent(ctx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
        final Regatta regatta = regattaApi.getRegatta(ctx, EVENT_NAME);
        final Mark mark = markApi.addMarkToRegatta(ctx, regatta.getName(), "Startboat");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, EVENT_NAME, null, 1)[0];
        markApi.addMarkFix(ctx, EVENT_NAME, race.getRaceName(), FLEET, mark.getMarkId(),
                /* markTemplateId */ UUID.randomUUID(), /* markPropertiesId */ UUID.randomUUID(), 9.12, .599,
                currentTimeMillis());
    }

    @SeleniumTestCase
    public void testAddMarkToRegattaForNonExistingEvent() {
        assertThrows(HttpException.NotFound.class, () -> {
            final ApiContext ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
            markApi.addMarkToRegatta(ctx, "NONEVENT", "Startboat");
        });
    }
    
    @SeleniumTestCase
    public void testAddMarkToRegattaWithoutPermission() {
        assertThrows(HttpException.Unauthorized.class, () -> {
            final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
            final ApiContext ownerCtx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
            securityApi.createUser(adminSecurityCtx, "donald", "Donald Duck", null, DONALDS_PASSWORD);
            final ApiContext readerCtx = createApiContext(getContextRoot(), SERVER_CONTEXT, "donald", DONALDS_PASSWORD);
            eventApi.createEvent(ownerCtx, EVENT_NAME, BOAT_CLASS, CompetitorRegistrationType.CLOSED, "default");
            markApi.addMarkToRegatta(readerCtx, EVENT_NAME, "Startboat");
        });
    }
}
