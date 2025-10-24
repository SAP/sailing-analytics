package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SECURITY_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.SHARED_SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static com.sap.sailing.selenium.api.core.ApiContext.createApiContext;
import static com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage.goToPage;
import static java.lang.System.currentTimeMillis;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.LongAdder;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import org.json.JSONException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.coursetemplate.CourseConfiguration;
import com.sap.sailing.selenium.api.coursetemplate.CourseConfigurationApi;
import com.sap.sailing.selenium.api.coursetemplate.CourseTemplate;
import com.sap.sailing.selenium.api.coursetemplate.CourseTemplateApi;
import com.sap.sailing.selenium.api.coursetemplate.DeviceMapping;
import com.sap.sailing.selenium.api.coursetemplate.FreestyleProperties;
import com.sap.sailing.selenium.api.coursetemplate.MarkConfiguration;
import com.sap.sailing.selenium.api.coursetemplate.MarkProperties;
import com.sap.sailing.selenium.api.coursetemplate.MarkPropertiesApi;
import com.sap.sailing.selenium.api.coursetemplate.MarkRole;
import com.sap.sailing.selenium.api.coursetemplate.MarkRoleApi;
import com.sap.sailing.selenium.api.coursetemplate.MarkTemplate;
import com.sap.sailing.selenium.api.coursetemplate.MarkTemplateApi;
import com.sap.sailing.selenium.api.coursetemplate.Positioning;
import com.sap.sailing.selenium.api.coursetemplate.RepeatablePart;
import com.sap.sailing.selenium.api.coursetemplate.WaypointTemplate;
import com.sap.sailing.selenium.api.coursetemplate.WaypointWithMarkConfiguration;
import com.sap.sailing.selenium.api.event.EventApi;
import com.sap.sailing.selenium.api.event.LeaderboardApi;
import com.sap.sailing.selenium.api.event.Mark;
import com.sap.sailing.selenium.api.event.MarkApi;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.api.helper.CourseTemplateDataFactory;
import com.sap.sailing.selenium.api.regatta.Course;
import com.sap.sailing.selenium.api.regatta.RaceColumn;
import com.sap.sailing.selenium.api.regatta.RegattaApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.pages.adminconsole.AdminConsolePage;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

public class CourseConfigurationTest extends AbstractSeleniumTest {
    private static final String DONALDS_PASSWORD = "dai:LKJ][][''sy0815";

    private static final Logger logger = Logger.getLogger(CourseConfigurationTest.class.getName());
    
    private final CourseConfigurationApi courseConfigurationApi = new CourseConfigurationApi();
    private final CourseTemplateApi courseTemplateApi = new CourseTemplateApi();
    private final MarkTemplateApi markTemplateApi = new MarkTemplateApi();
    private final MarkPropertiesApi markPropertiesApi = new MarkPropertiesApi();
    private final MarkRoleApi markRoleApi = new MarkRoleApi();
    private final EventApi eventApi = new EventApi();
    private final RegattaApi regattaApi = new RegattaApi();
    private final LeaderboardApi leaderboardApi = new LeaderboardApi();
    private final MarkApi markApi = new MarkApi();
    private final SecurityApi securityApi = new SecurityApi();
    private ApiContext ctx;
    private ApiContext sharedServerCtx;

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
        ctx = createAdminApiContext(getContextRoot(), SERVER_CONTEXT);
        sharedServerCtx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
    }

    private void assertConsistentCourseConfiguration(final CourseConfiguration courseConfiguration) {
        for (final WaypointWithMarkConfiguration wp : courseConfiguration.getWaypoints()) {
            for (final String mcId : wp.getMarkConfigurationIds()) {
                boolean found = false;
                for (final MarkConfiguration mc : courseConfiguration.getMarkConfigurations()) {
                    if (mc.getId().equals(mcId)) {
                        found = true;
                    }
                }
                assertTrue(found, "No markconfiguration with id " + mcId + " for waypoint");
            }
        }
    }

    private void assertCourseConfigurationCompared(final ApiContext ctx,
            final CourseConfiguration srcCourseConfiguration, final CourseConfiguration trgtCourseConfiguration) {
        final Iterable<MarkConfiguration> srcMarkConfigurations = srcCourseConfiguration.getMarkConfigurations();
        final Iterable<MarkConfiguration> trgtMarkConfigurations = trgtCourseConfiguration.getMarkConfigurations();
        final Iterable<WaypointWithMarkConfiguration> srcWaypoints = srcCourseConfiguration.getWaypoints();
        final Iterable<WaypointWithMarkConfiguration> trgtWaypoints = trgtCourseConfiguration.getWaypoints();
        assertEquals(Util.size(srcMarkConfigurations), Util.size(trgtMarkConfigurations),
                "number of markconfiguration is different");
        assertEquals(Util.size(srcWaypoints), Util.size(trgtWaypoints), "number of waypoints is different");
        final Map<MarkConfiguration, String> markConfigurationToNameMap = new HashMap<>();
        for (final MarkConfiguration markConfiguration : srcMarkConfigurations) {
            final UUID markTemplateId = markConfiguration.getMarkTemplateId();
            FreestyleProperties srcAppearance = markConfiguration.getEffectiveProperties();
            if (srcAppearance == null) {
                srcAppearance = markConfiguration.getFreestyleProperties();
            }
            Positioning srcPositioning = markConfiguration.getPositioning();
            boolean found = false;
            for (final MarkConfiguration trgtMarkConfiguration : trgtMarkConfigurations) {
                final FreestyleProperties trgtAppearance = trgtMarkConfiguration.getEffectiveProperties();
                if (srcAppearance == null && trgtAppearance == null) {
                    return;
                }
                final boolean matchByName = srcAppearance != null && trgtAppearance != null
                        && srcAppearance.getName().equals(trgtAppearance.getName());
                final boolean matchByTemplateId = markTemplateId != null
                        && markTemplateId.equals(trgtMarkConfiguration.getMarkTemplateId());
                final boolean matchByMarkId = markConfiguration.getMarkId() != null
                        && markConfiguration.getMarkId().equals(trgtMarkConfiguration.getMarkId());
                // final boolean matchByMarkId =
                if (matchByName || matchByTemplateId || matchByMarkId) {
                    found = true;
                    final String identifier = matchByName ? srcAppearance.getName()
                            : matchByTemplateId ? markTemplateId.toString()
                                    : matchByMarkId ? markConfiguration.getMarkId().toString() : "unknown";
                    final String msgIdentifier = matchByName ? "markconfiguration with name " + srcAppearance.getName()
                            : matchByTemplateId ? "markconfiguration with markTemplateID " + markTemplateId
                                    : matchByMarkId ? "markconfiguration with markId " + markConfiguration.getMarkId()
                                            : "unknown";
                    markConfigurationToNameMap.put(markConfiguration, identifier);
                    if (markTemplateId != null && trgtMarkConfiguration.getMarkTemplateId() != null) {
                        assertEquals(markTemplateId, trgtMarkConfiguration.getMarkTemplateId(),
                                "markTemplateId is different for " + msgIdentifier);
                    }
                    final String srcAssociatedRoleId = markConfiguration.getAssociatedRoleId();
                    if (srcAssociatedRoleId != null) {
                        assertEquals(srcAssociatedRoleId, trgtMarkConfiguration.getAssociatedRoleId(),
                                "associated role is different for " + msgIdentifier);
                    }
                    if (matchByName) {
                        assertEquals(srcAppearance.getShortName(), trgtAppearance.getShortName(),
                                "shortName is different for " + msgIdentifier);
                    }
                    List<DeviceMapping> trgtDeviceMappings = trgtMarkConfiguration.getDeviceMappings();
                    boolean hasNonPingDeviceIdentifier = !trgtDeviceMappings.isEmpty()
                            && !trgtDeviceMappings.get(0).getType().equals("PING");
                    if (srcPositioning != null) {
                        if (srcPositioning.getDeviceId() != null) {
                            assertEquals("smartphoneUUID",
                                    trgtDeviceMappings.get(0).getType(), "tracking device was not properly mapped for " + msgIdentifier);
                        } else if (srcPositioning.getLatitudeDeg() != null && srcPositioning.getLongitudeDeg() != null) {
                            assertEquals(srcPositioning.getLatitudeDeg(), trgtMarkConfiguration.getLastKnownPosition().getLatDeg(),
                                    0.0001, "position.lat is different for " + msgIdentifier);
                            assertEquals(srcPositioning.getLongitudeDeg(),
                                    trgtMarkConfiguration.getLastKnownPosition().getLngDeg(), 0.0001, "position.lng is different for " + msgIdentifier);
                        }
                    }
                    if (markConfiguration.isStoreToInventory()) {
                        final UUID markPropertiesId = trgtMarkConfiguration.getMarkPropertiesId();
                        assertNotNull(markPropertiesId);
                        final MarkProperties markProperties = markPropertiesApi.getMarkProperties(ctx,
                                markPropertiesId);
                        assertNotNull(markProperties);
                        if (matchByName) {
                            assertEquals(srcAppearance.getName(), markProperties.getName());
                            assertEquals(srcAppearance.getShortName(), markProperties.getShortName());
                        }
                        assertEquals(markProperties.hasDevice(), hasNonPingDeviceIdentifier, "device association differs for "+msgIdentifier);
                    }
                }
            }
            assertTrue(found, "No matching markconfiguration found for markconfiguration " + markConfiguration.getId());
        }
        for (final WaypointWithMarkConfiguration srcWaypoint : srcWaypoints) {
            final String controlPointName = srcWaypoint.getControlPointName();
            if (controlPointName != null) {
                boolean found = false;
                for (final WaypointWithMarkConfiguration trgtWaypoint : trgtWaypoints) {
                    if (controlPointName.equals(trgtWaypoint.getControlPointName())) {
                        found = true;
                        assertEquals(srcWaypoint.getControlPointShortName(),
                                trgtWaypoint.getControlPointShortName(), "Waypoint controlpoint shortname is different");
                        assertEquals(Util.size(srcWaypoint.getMarkConfigurationIds()),
                                Util.size(trgtWaypoint.getMarkConfigurationIds()),
                                "Waypoint number of markconfigurations is different");
                        final LongAdder srcIndex = new LongAdder();
                        for (final String srcMarkConfigurationId : srcWaypoint.getMarkConfigurationIds()) {
                            srcIndex.increment();
                            srcMarkConfigurations.forEach(srcMc -> {
                                if (srcMc.getId().equals(srcMarkConfigurationId)) {
                                    final LongAdder trgtIndex = new LongAdder();
                                    for (final String trgtMarkConfigurationId : trgtWaypoint
                                            .getMarkConfigurationIds()) {
                                        trgtIndex.increment();
                                        trgtMarkConfigurations.forEach(trgtMc -> {
                                            if (trgtMc.getId().equals(trgtMarkConfigurationId)) {
                                                if (markConfigurationToNameMap.get(srcMc)
                                                        .equals(trgtMc.getEffectiveProperties().getName())) {
                                                    assertEquals(srcIndex.intValue(),
                                                            trgtIndex.intValue(), "Waypoint position of markconfigurations is wrong");
                                                }
                                            }
                                        });
                                    }
                                }
                            });
                        }
                    }
                }
                assertTrue(found, "Waypoint with control point name " + controlPointName + " not found");
            }
        }
        final RepeatablePart srcRepeatablePart = srcCourseConfiguration.getRepeatablePart();
        if (srcRepeatablePart != null) {
            final RepeatablePart tgrtRepeatablePart = trgtCourseConfiguration.getRepeatablePart();
            assertEquals(srcRepeatablePart.getZeroBasedIndexOfRepeatablePartStart(),
                    Integer.valueOf(tgrtRepeatablePart.getZeroBasedIndexOfRepeatablePartStart()),
                    "repeatablePart.start is different");
            assertEquals(srcRepeatablePart.getZeroBasedIndexOfRepeatablePartEnd(), tgrtRepeatablePart.getZeroBasedIndexOfRepeatablePartEnd(),
                    "repeatablePart.end is different");
        }
    }

    @SeleniumTestCase
    public void testCreateCourseFromCourseTemplateWithoutChanges() {
        final int numberOfLaps = 2;
        final CourseTemplateDataFactory ctdf = new CourseTemplateDataFactory(sharedServerCtx);
        final Map<MarkRole, MarkTemplate> associatedRoles = new HashMap<>();
        associatedRoles.put(ctdf.sbRole, ctdf.sb);
        associatedRoles.put(ctdf.peRole, ctdf.pe);
        associatedRoles.put(ctdf.b1Role, ctdf.b1);
        associatedRoles.put(ctdf.b4sRole, ctdf.b4s);
        associatedRoles.put(ctdf.b4pRole, ctdf.b4p);
        final CourseTemplate createdCourseTemplate = courseTemplateApi.createCourseTemplate(sharedServerCtx,
                ctdf.constructCourseTemplate(new Pair<>(1, 3), numberOfLaps, associatedRoles));
        CourseConfiguration courseConfiguration = courseConfigurationApi.createCourseConfigurationFromCourseTemplate(
                ctx, createdCourseTemplate.getId(), /* optionalRegattaName */ null, /* tags */ null, /* optionalNumberOfLaps */ null);
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, 1)[0];
        courseConfigurationApi.createCourse(ctx, courseConfiguration, regattaName, race.getRaceName(), "Default");
        CourseConfiguration createdCourseAsConfiguration = courseConfigurationApi
                .createCourseConfigurationFromCourse(ctx, regattaName, race.getRaceName(), "Default", null);
        assertEquals(6, Util.size(createdCourseAsConfiguration.getMarkConfigurations()));
        assertEquals(createdCourseTemplate.getId(), createdCourseAsConfiguration.getOptionalCourseTemplateId());
        assertCourseConfigurationCompared(ctx, courseConfiguration, createdCourseAsConfiguration);
        assertConsistentCourseConfiguration(createdCourseAsConfiguration);
        assertEquals(numberOfLaps, createdCourseAsConfiguration.getNumberOfLaps());
    }

    @SeleniumTestCase
    public void testReconstructionOfLapsForCourseBasedOnTemplate() {
        final CourseTemplateDataFactory ctdf = new CourseTemplateDataFactory(sharedServerCtx);
        final CourseTemplate createdCourseTemplate = courseTemplateApi.createCourseTemplate(sharedServerCtx,
                ctdf.constructCourseTemplate(new Pair<>(1, 3), null));
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, 1)[0];
        for (int numberOfLaps = 1; numberOfLaps <= 3; numberOfLaps++) {
            CourseConfiguration courseConfiguration = courseConfigurationApi
                    .createCourseConfigurationFromCourseTemplate(ctx, createdCourseTemplate.getId(), regattaName,
                            /* tags */ null, /* optionalNumberOfLaps */ null);
            courseConfiguration.setNumberOfLaps(numberOfLaps);
            courseConfigurationApi.createCourse(ctx, courseConfiguration, regattaName, race.getRaceName(), "Default");
            CourseConfiguration createdCourseAsConfiguration = courseConfigurationApi
                    .createCourseConfigurationFromCourse(ctx, regattaName, race.getRaceName(), "Default", null);
            assertEquals(createdCourseTemplate.getId(), createdCourseAsConfiguration.getOptionalCourseTemplateId());
            assertEquals(numberOfLaps, createdCourseAsConfiguration.getNumberOfLaps());
        }
    }

    @SeleniumTestCase
    public void testWithoutRepeatablePart() {
        final int numberOfLaps = 2;
        final CourseTemplateDataFactory ctdf = new CourseTemplateDataFactory(sharedServerCtx);
        final CourseTemplate template = courseTemplateApi.createCourseTemplate(sharedServerCtx,
                ctdf.constructCourseTemplate(/* repeatable part */ null, numberOfLaps));
        final int templateWaypoints = Util.size(template.getWaypoints());
        // create course configuration from template without repeatable part and expect same number of waypoints and no repeatable part
        final CourseConfiguration courseConfiguration = courseConfigurationApi.createCourseConfigurationFromCourseTemplate(ctx,
                template.getId(), /* optionalRegattaName */ null, /* tags */ null, /* optionalNumberOfLaps */ null);
        assertEquals(templateWaypoints, Util.size(courseConfiguration.getWaypoints()));

        assertNull(courseConfiguration.getRepeatablePart(), "repeatable part of course configuration is not null");
        assertEquals(numberOfLaps, courseConfiguration.getNumberOfLaps());
        // create a course and make sure it has no repeatable part
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, null, 1)[0];
        final CourseConfiguration course = courseConfigurationApi.createCourse(ctx, courseConfiguration, "test", race.getRaceName(), "Default");
        assertEquals(templateWaypoints, Util.size(course.getWaypoints()));
        assertNull(course.getRepeatablePart(), "repeatable part of course is not null");
        assertEquals(-1, course.getNumberOfLaps()); // the course template does not specify a repeatable part
    }

    @SeleniumTestCase
    public void testWithRepeatablePartOfSizeOneAndDifferentNumbersOfLaps() {
        final int defaultNumberOfLaps = 2;
        final CourseTemplateDataFactory ctdf = new CourseTemplateDataFactory(sharedServerCtx);
        final Pair<Integer, Integer> repeatablePart = new Pair<>(1, 2);
        final CourseTemplate template = courseTemplateApi.createCourseTemplate(sharedServerCtx,
                ctdf.constructCourseTemplate(repeatablePart, defaultNumberOfLaps));
        final int templateWaypoints = Util.size(template.getWaypoints()); // no multiplication happening based on the default number of laps
        assertEquals(ctdf.waypointSequence.size(), templateWaypoints);
        // create course configuration from template with repeatable part of size 1 and expect it to occur once for the default two laps:
        final CourseConfiguration courseConfigurationWithDefaultNumberOfLaps = courseConfigurationApi.createCourseConfigurationFromCourseTemplate(ctx,
                template.getId(), /* optionalRegattaName */ null, /* tags */ null, /* optionalNumberOfLaps */ null);
        final int lengthOfRepeatablePart = repeatablePart.getB()-repeatablePart.getA();
        final int expectedNumberOfWaypointsWithTwoLaps = ctdf.waypointSequence.size(); // default of two laps makes one occurrence which is what the waypoint sequence has
        assertEquals(expectedNumberOfWaypointsWithTwoLaps, Util.size(courseConfigurationWithDefaultNumberOfLaps.getWaypoints()));
        // create course configuration from template with repeatable part of size 1 and ask for only one lap; the repeatable part must still occur once:
        final CourseConfiguration courseConfigurationWithOneLap = courseConfigurationApi.createCourseConfigurationFromCourseTemplate(ctx,
                template.getId(), /* optionalRegattaName */ null, /* tags */ null, /* optionalNumberOfLaps */ 1);
        final int expectedNumberOfWaypointsWithOneLap = ctdf.waypointSequence.size(); // expecting one configuration for each original waypoint in the template although no occurrence of the repeatable part
        assertEquals(expectedNumberOfWaypointsWithOneLap, Util.size(courseConfigurationWithOneLap.getWaypoints()));
        // create course configuration from template with repeatable part of size 1 and ask for only one lap; the repeatable part must still occur once:
        final CourseConfiguration courseConfigurationWithThreeLaps = courseConfigurationApi.createCourseConfigurationFromCourseTemplate(ctx,
                template.getId(), /* optionalRegattaName */ null, /* tags */ null, /* optionalNumberOfLaps */ 3);
        final int expectedNumberOfWaypointsWithThreeLaps = ctdf.waypointSequence.size()+1*lengthOfRepeatablePart; // expecting one additional occurrence of the repeatable part
        assertEquals(expectedNumberOfWaypointsWithThreeLaps, Util.size(courseConfigurationWithThreeLaps.getWaypoints()));
    }

    @SeleniumTestCase
    public void testCreateCourseWithLessNoOfLapsThanRepeatablePartOccurences() {
        createCourseFromTemplateBasedCourseConfiguration(/* numberOfLaps */ 10, /* numberOfLapsForCOurse */ 8,
                /* repeatable part start */ 1, /* repeatable part end */ 3, true);
    }

    @SeleniumTestCase
    public void testCreateCourseWithMoreNoOfLapsThanRepeatablePartOccurences() {
        final int numberOfLaps = 2;
        final int numberOfLapsForCourse = 50;
        final int repeatablePartStart = 1;
        final int repeatablePartEnd = 3;
        final int repeatablePartLength = repeatablePartEnd - repeatablePartStart;
        final Pair<CourseConfiguration, CourseConfiguration> result = createCourseFromTemplateBasedCourseConfiguration(
                numberOfLaps, numberOfLapsForCourse, repeatablePartStart, repeatablePartEnd, true);
        // get repeatable part sequence from template configuration
        List<WaypointWithMarkConfiguration> repeatablePartWpsA = new ArrayList<>(
                repeatablePartEnd - repeatablePartStart);
        for (int i = repeatablePartStart; i < repeatablePartEnd; i++) {
            repeatablePartWpsA.add(Util.get(result.getA().getWaypoints(), i));
        }
        // compare each waypoint of the course configuration against the repeatable part of the template configuration
        for (int lap = numberOfLaps; lap < numberOfLapsForCourse; lap++) {
            for (int i = repeatablePartStart; i < repeatablePartEnd; i++) {
                final WaypointWithMarkConfiguration wpA = repeatablePartWpsA.get(i - repeatablePartStart);
                final WaypointWithMarkConfiguration wpB = Util.get(result.getB().getWaypoints(),
                        (lap - 1) * repeatablePartLength + i);
                assertEquals(wpA.getPassingInstruction(), wpB.getPassingInstruction());
                assertEquals(Util.get(Util.get(result.getB().getWaypoints(), i).getMarkConfigurationIds(), 0),
                        Util.get(wpB.getMarkConfigurationIds(), 0));
            }
        }
    }

    @SeleniumTestCase
    public void testCreateCourseLineup() {
        createCourseFromTemplateBasedCourseConfiguration(/* numberOfLaps */ 2, /* numberOfLapsForCourse */ 2,
                /* repeatable part start */ 0, /* repeatable part end */ 1, /* strict */ true);
        createCourseFromTemplateBasedCourseConfiguration(/* numberOfLaps */ 2, /* numberOfLapsForCourse */ 1,
                /* repeatable part start */ 0, /* repeatable part end */ 5, true);
        createCourseFromTemplateBasedCourseConfiguration(/* numberOfLaps */ 10, /* numberOfLapsForCourse */ 1,
                /* repeatable part start */ 0, /* repeatable part end */ 5, true);
        createCourseFromTemplateBasedCourseConfiguration(/* numberOfLaps */ 10000, /* numberOfLapsForCourse */ 20000,
                /* repeatable part start */ 0, /* repeatable part end */ 5, true);
    }

    /**
     * Creates a course based on a coursetemplate. This validates that the waypoint numbers are correct for the
     * following rule
     * 
     * #waypoints = #laps * sizeOfRepeatingPart + #templateWaypoints - sizeOfRepeatingPart
     * 
     * @param numberOfLaps
     *            number of laps to construct the course template
     * @param numberOfLapsForCourse
     *            number of laps for constructing the course
     * @param zeroBasedIndexOfRepeatablePartStart
     *            start index of repeatable part
     * @param zeroBasedIndexOfRepeatablePartEnd
     *            end index of repeatable part
     * @param strict
     *            if false, no asserts are checked
     * @return Pair of (A) source course configuration based on the template and (B) the result course configuration of
     *         the created regatta course
     */
    private Pair<CourseConfiguration, CourseConfiguration> createCourseFromTemplateBasedCourseConfiguration(
            final int numberOfLaps, final int numberOfLapsForCourse, final int zeroBasedIndexOfRepeatablePartStart,
            final int zeroBasedIndexOfRepeatablePartEnd, final boolean strict) {
        final CourseTemplateDataFactory ctdf = new CourseTemplateDataFactory(sharedServerCtx);
        final CourseTemplate template = courseTemplateApi.createCourseTemplate(sharedServerCtx,
                ctdf.constructCourseTemplate(
                        new Pair<>(zeroBasedIndexOfRepeatablePartStart, zeroBasedIndexOfRepeatablePartEnd),
                        numberOfLaps));
        // create course configuration from template
        final CourseConfiguration courseConfiguration = courseConfigurationApi
                .createCourseConfigurationFromCourseTemplate(ctx, template.getId(), /* optionalRegattaName */ null,
                        /* tags */ null, /* optional number of laps */ null);
        if (strict) {
            assertEquals(courseConfiguration.getRepeatablePart().getZeroBasedIndexOfRepeatablePartStart().intValue(),
                    zeroBasedIndexOfRepeatablePartStart);
            assertEquals(courseConfiguration.getRepeatablePart().getZeroBasedIndexOfRepeatablePartEnd().intValue(),
                    zeroBasedIndexOfRepeatablePartEnd);
            final int expectedNumberOfWaypoints = (numberOfLaps - 1)
                    * (zeroBasedIndexOfRepeatablePartEnd - zeroBasedIndexOfRepeatablePartStart)
                    + Util.size(template.getWaypoints())
                    - (zeroBasedIndexOfRepeatablePartEnd - zeroBasedIndexOfRepeatablePartStart);
            assertEquals(expectedNumberOfWaypoints, Util.size(courseConfiguration.getWaypoints()));
        }

        // set number of laps in the course configuration an create a course out of it
        courseConfiguration.setNumberOfLaps(numberOfLapsForCourse);
        final String regattaName = UUID.randomUUID().toString();
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, null, 1)[0];
        final CourseConfiguration course = courseConfigurationApi.createCourse(ctx, courseConfiguration, regattaName,
                race.getRaceName(), "Default");
        if (strict) {
            final int expectedNumberOfWaypointsOfCourse = (numberOfLapsForCourse - 1)
                    * (zeroBasedIndexOfRepeatablePartEnd - zeroBasedIndexOfRepeatablePartStart)
                    + Util.size(template.getWaypoints())
                    - (zeroBasedIndexOfRepeatablePartEnd - zeroBasedIndexOfRepeatablePartStart);
            assertEquals(expectedNumberOfWaypointsOfCourse, Util.size(course.getWaypoints()));
        }
        return new Pair<>(courseConfiguration, course);
    }

    @SeleniumTestCase
    public void testMarkPropertiesWithPositioning() throws JSONException {
        final double MP_LAT_DEG = 49.097487;
        final double MP_LNG_DEG = 8.648631;
        final MarkProperties mp1 = markPropertiesApi.createMarkProperties(sharedServerCtx, "mpWithPos", "mpWithPos",
                /* deviceUuid */ null, "#ffffff", "shape", "pattern", MarkType.LANDMARK.name(),
                /* tags */ Collections.emptyList(), MP_LAT_DEG, MP_LNG_DEG);
        assertEquals(MP_LAT_DEG, mp1.getLatDeg().doubleValue(), .1);
        assertEquals(MP_LNG_DEG, mp1.getLonDeg().doubleValue(), .1);
        final MarkConfiguration mc1 = MarkConfiguration.createMarkPropertiesBased(mp1.getId(),
                markRoleApi.createMarkRole(sharedServerCtx, "role_mc1_1", /* shortName */ null).getId());
        final WaypointWithMarkConfiguration wp1 = new WaypointWithMarkConfiguration("landmark", "landmark",
                PassingInstruction.Single_Unknown, Arrays.asList(mc1.getId()));
        final CourseConfiguration courseConfiguration = new CourseConfiguration("test1", Arrays.asList(mc1),
                Arrays.asList(wp1));
        final String regattaName = "Regatta1";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, null, 1)[0];
        final CourseConfiguration course = courseConfigurationApi.createCourse(ctx, courseConfiguration, regattaName,
                race.getRaceName(), "Default");
        final MarkConfiguration mc1_1 = course.getMarkConfigurationByEffectiveName("mpWithPos");
        assertEquals(MP_LAT_DEG, mc1_1.getLastKnownPosition().getLatDeg(), .1);
        assertEquals(MP_LNG_DEG, mc1_1.getLastKnownPosition().getLngDeg(), .1);
        for (final DeviceMapping dm : mc1_1.getDeviceMappings()) {
            boolean hasPingDevice = false;
            if ("PING".equals(dm.getType())) {
                hasPingDevice = true;
                assertEquals(MP_LAT_DEG, dm.getLastKnownPosition().getLatDeg(), .1);
                assertEquals(MP_LNG_DEG, dm.getLastKnownPosition().getLngDeg(), .1);
            }
            assertTrue(hasPingDevice);
        }
    }

    @SeleniumTestCase
    public void testMarkPropertiesWithPositiongWithMarkTemplateUsage() throws JSONException {
        final double MP_LAT_DEG = 49.097487;
        final double MP_LNG_DEG = 8.648631;
        final String regattaName2 = "Regatta2";
        eventApi.createEvent(ctx, regattaName2, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName2, null, 1)[0];

        // Creating a mark configuration based on a new mark template
        final MarkTemplate mtb2 = markTemplateApi.createMarkTemplate(sharedServerCtx, "mark template 1", "mt1",
                "#FFFFFF", "Cylinder", "Checkered", MarkType.BUOY.name());
        final MarkConfiguration b2 = MarkConfiguration.createMarkTemplateBased(mtb2.getId(),
                markRoleApi.createMarkRole(sharedServerCtx, "role_b2", /* shortName */ "mc1").getId());
        b2.setStoreToInventory(true);

        // Creating a new course configuration with the created mark configuration
        final WaypointWithMarkConfiguration wp3 = new WaypointWithMarkConfiguration(null, null,
                PassingInstruction.Single_Unknown, Arrays.asList(b2.getId()));
        final CourseConfiguration courseConfiguration = new CourseConfiguration("my-course", Arrays.asList(b2),
                Arrays.asList(wp3));

        // Creating a course template and set mark properties position
        final CourseConfiguration courseTemplate = courseConfigurationApi.createCourseTemplate(ctx, courseConfiguration,
                regattaName2);
        for (MarkProperties mp : markPropertiesApi.getAllMarkProperties(sharedServerCtx, Collections.emptyList())) {
            if (mp.getShortName().equals("mt1")) {
                markPropertiesApi.updateMarkPropertiesPositioning(sharedServerCtx, mp.getId(), /* deviceUuid */ null,
                        MP_LAT_DEG, MP_LNG_DEG);
            }
        }

        // Creating a course from the course template
        final CourseConfiguration course = courseConfigurationApi.createCourse(ctx, courseTemplate, regattaName2,
                race.getRaceName(), "Default");

        final MarkConfiguration mc1_1 = course.getMarkConfigurationByEffectiveName("mark template 1");
        assertEquals(MP_LAT_DEG, mc1_1.getLastKnownPosition().getLatDeg(), .1);
        assertEquals(MP_LNG_DEG, mc1_1.getLastKnownPosition().getLngDeg(), .1);
        for (final DeviceMapping dm : mc1_1.getDeviceMappings()) {
            boolean hasPingDevice = false;
            if ("PING".equals(dm.getType())) {
                hasPingDevice = true;
                assertEquals(MP_LAT_DEG, dm.getLastKnownPosition().getLatDeg(), .1);
                assertEquals(MP_LNG_DEG, dm.getLastKnownPosition().getLngDeg(), .1);
            }
            assertTrue(hasPingDevice);
        }
    }

    @Disabled
    //TODO: Need to clarify this test. The two course template don't seem to get merged by
    // MarkRole. Perhaps the cause is, that the MarkTemplate/MarkRole-map of the first
    // MarkTemplate is passed to the creation of the second one, so the MarkTemplate-IDs are
    // different.
    public void testDifferentCourseTemplatesWithCommonRolesInRegatta() {
        final CourseTemplateDataFactory ctdf = new CourseTemplateDataFactory(sharedServerCtx);
        final CourseTemplate createdCourseTemplate = courseTemplateApi.createCourseTemplate(sharedServerCtx,
                ctdf.constructCourseTemplate(new Pair<>(1, 3), /* numberOfLaps */ 2));
        logger.info(createdCourseTemplate.getJson().toJSONString());
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, /* numberOfRaces */ 1)[0];
        // Create a course based on one of the templates
        CourseConfiguration courseConfiguration = courseConfigurationApi.createCourseConfigurationFromCourseTemplate(
                ctx, createdCourseTemplate.getId(), regattaName, /* tags */ null, /* optionalNumberOfLaps */ null);
        logger.info(courseConfiguration.getJson().toJSONString());
        final CourseConfiguration createdCourse = courseConfigurationApi.createCourse(ctx, courseConfiguration,
                regattaName, race.getRaceName(), "Default");
        final Map<MarkRole, MarkTemplate> roleTemplateMap = new HashMap<>();
        courseConfiguration.getMarkConfigurations().forEach(mc -> {
            if (mc.getMarkTemplateId() != null && mc.getAssociatedRoleId() != null) {
                roleTemplateMap.put(markRoleApi.getMarkRole(sharedServerCtx, UUID.fromString(mc.getAssociatedRoleId())), markTemplateApi.getMarkTemplate(sharedServerCtx, mc.getMarkTemplateId()));
            }
        });
        roleTemplateMap.entrySet().forEach(e -> logger.info("XXXXXXXXXXXXXXXXXXXXXX" + e.getKey().getShortName() + " " + e.getValue().getName()));
        final CourseTemplateDataFactory ctdf2 = new CourseTemplateDataFactory(sharedServerCtx);
        final CourseTemplate createdCourseTemplate2 = courseTemplateApi.createCourseTemplate(sharedServerCtx,
                ctdf2.constructCourseTemplate(new Pair<>(1, 3), 3, roleTemplateMap));
        logger.info(createdCourseTemplate2.getJson().toJSONString());
        CourseConfiguration courseConfigurationBasedOnOtherTemplate = courseConfigurationApi
                .createCourseConfigurationFromCourseTemplate(ctx, createdCourseTemplate2.getId(), regattaName,
                        /* tags */ null, /* optionalNumberOfLaps */ null);
        logger.info(courseConfigurationBasedOnOtherTemplate.getJson().toJSONString());
        // All marks being part of the course sequence are required to be matched by role.
        // The single spare mark can not be matched by a role because no role was assigned to it.
        // This means a new spare mark will be suggested to be created.
        assertEquals(Util.size(createdCourse.getMarkConfigurations()) + 1,
                Util.size(courseConfigurationBasedOnOtherTemplate.getMarkConfigurations()));
        // TODO check that the marks are associated to the same role/position in the sequence as in the originally saved course.
    }

    @SeleniumTestCase
    public void testCreateCourseFromFreestyleConfigurationWithPositioning() {
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, 1)[0];
        MarkConfiguration sb = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_sb", /* shortName */ null).getId(), "startboat", "sb",
                null, null, null, null, Collections.emptySet());
        sb.setFixedPosition(5.5, 7.1);
        MarkConfiguration pe = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_pe", /* shortName */ null).getId(), "pin end", "pe",
                null, null, null, null, Collections.emptySet());
        UUID randomDeviceId = UUID.randomUUID();
        pe.setTrackingDeviceId(randomDeviceId);
        MarkConfiguration bl = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_bl", /* shortName */ null).getId(), "1", null,
                "#0000FF", null, null, null, Collections.emptySet());
        WaypointWithMarkConfiguration wp1 = new WaypointWithMarkConfiguration("start/end", "s/e",
                PassingInstruction.Line, Arrays.asList(sb.getId(), pe.getId()));
        WaypointWithMarkConfiguration wp2 = new WaypointWithMarkConfiguration(null, null, PassingInstruction.Port,
                Arrays.asList(bl.getId()));
        CourseConfiguration courseConfiguration = new CourseConfiguration("my-freestyle-course",
                Arrays.asList(sb, pe, bl), Arrays.asList(wp1, wp2, wp1));
        courseConfigurationApi.createCourse(ctx, courseConfiguration, regattaName, race.getRaceName(), "Default");
        CourseConfiguration createdCourseAsConfiguration = courseConfigurationApi
                .createCourseConfigurationFromCourse(ctx, regattaName, race.getRaceName(), "Default", null);
        assertConsistentCourseConfiguration(createdCourseAsConfiguration);
        assertCourseConfigurationCompared(sharedServerCtx, courseConfiguration, createdCourseAsConfiguration);
    }

    @SeleniumTestCase
    public void testCreateCourseTemplateWithPositioningIncluded() {
        MarkConfiguration sb = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_sb", /* shortName */ null).getId(), "startboat", "sb",
                null, null, null, null, Collections.emptySet());
        sb.setFixedPosition(5.5, 7.1);
        MarkConfiguration pe = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_pe", /* shortName */ null).getId(), "pin end", "pe",
                null, null, null, null, Collections.emptySet());
        UUID randomDeviceId = UUID.randomUUID();
        pe.setTrackingDeviceId(randomDeviceId);
        MarkConfiguration bl = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_bl", /* shortName */ null).getId(), "1", null,
                "#0000FF", null, null, null, Collections.emptySet());
        WaypointWithMarkConfiguration wp1 = new WaypointWithMarkConfiguration("start/end", "s/e",
                PassingInstruction.Line, Arrays.asList(sb.getId(), pe.getId()));
        WaypointWithMarkConfiguration wp2 = new WaypointWithMarkConfiguration(null, null, PassingInstruction.Port,
                Arrays.asList(bl.getId()));

        CourseConfiguration courseConfiguration = new CourseConfiguration("my-freestyle-course",
                Arrays.asList(sb, pe, bl), Arrays.asList(wp1, wp2, wp1));

        CourseConfiguration courseConfigurationResult = courseConfigurationApi.createCourseTemplate(ctx,
                courseConfiguration, null);

        assertConsistentCourseConfiguration(courseConfigurationResult);
        assertCourseConfigurationCompared(sharedServerCtx, courseConfiguration, courseConfigurationResult);
    }

    @SeleniumTestCase
    public void testCreateCourseTemplateWithPositiongUpdated() {
        final String regattaName = "test";
        final double longDeg = 7.1, updatedLongDeg = 8.4;
        final double latDeg = 5.5, updatedLatDeg = 6.7;

        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, 1)[0];

        final MarkTemplate mt1 = markTemplateApi.createMarkTemplate(sharedServerCtx, "mc1", "mc1", "#FFFFFF", "Cylinder",
                "Checkered", MarkType.BUOY.name());
        final MarkConfiguration mc1 = MarkConfiguration.createMarkTemplateBased(mt1.getId(),
                markRoleApi.createMarkRole(sharedServerCtx, "role_mt1", /* shortName */ null).getId());

        final MarkProperties mp1 = markPropertiesApi.createMarkProperties(sharedServerCtx, "mc2", "mc2", /* deviceUuid */ null,
                "#FF0000", "shape", "pattern", "STARTBOAT", Collections.emptyList(), 1.0, 1.0);
        final MarkConfiguration mc2 = MarkConfiguration.createMarkPropertiesBased(mp1.getId(),
                markRoleApi.createMarkRole(sharedServerCtx, "role_mp1", /* shortName */ null).getId());
        mc2.setFixedPosition(latDeg, longDeg); // overrides the 1.0 / 1.0 as provided by the mp1 MarkProperties object

        final MarkConfiguration mc3 = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_mp2", /* shortName */ null).getId(), "mc3", "mc3",
                "#0000FF", null, null, null, Collections.emptySet());
        mc3.setFixedPosition(latDeg, longDeg);

        final UUID deviceId = UUID.randomUUID();
        final MarkProperties mp4 = markPropertiesApi.createMarkProperties(sharedServerCtx, "mc4", "mc4", deviceId.toString(),
                "#FF0000", "shape", "pattern", "STARTBOAT", Collections.emptyList(), null, null);
        final MarkConfiguration mc4 = MarkConfiguration.createMarkPropertiesBased(mp4.getId(),
                markRoleApi.createMarkRole(sharedServerCtx, "role_mp4", /* shortName */ null).getId());
        // mc4.setStoreToInventory(true);

        final MarkProperties mp5 = markPropertiesApi.createMarkProperties(sharedServerCtx, "mc5", "mc5", null, "#FF0000", "shape",
                "pattern", "STARTBOAT", Collections.emptyList(), 1.0, 1.0);
        final MarkConfiguration mc5 = MarkConfiguration.createMarkPropertiesBased(mp5.getId(),
                markRoleApi.createMarkRole(sharedServerCtx, "role_mc5", /* shortName */ null).getId());

        final WaypointWithMarkConfiguration wp1 = new WaypointWithMarkConfiguration("start/end", "s/e",
                PassingInstruction.Line, Arrays.asList(mc1.getId(), mc2.getId()));
        final WaypointWithMarkConfiguration wp2 = new WaypointWithMarkConfiguration("start/end", "s/e",
                PassingInstruction.Line, Arrays.asList(mc3.getId(), mc4.getId()));
        final WaypointWithMarkConfiguration wp3 = new WaypointWithMarkConfiguration(null, null,
                PassingInstruction.Starboard, Arrays.asList(mc5.getId()));

        final CourseConfiguration courseConfiguration = new CourseConfiguration("my-course",
                Arrays.asList(mc1, mc2, mc3, mc4, mc5), Arrays.asList(wp1, wp2, wp3));
        logger.info("initial: " + courseConfiguration.getJson());

        final CourseConfiguration createdCourse = courseConfigurationApi.createCourse(ctx, courseConfiguration,
                regattaName, race.getRaceName(), "Default");
        logger.info("createdCourse: " + createdCourse.getJson());

        final MarkConfiguration mc1a = createdCourse.getMarkConfigurationByEffectiveName("mc1");
        assertTrue(mc1a.getDeviceMappings().isEmpty());
        assertNull(mc1a.getLastKnownPosition());
        mc1a.setFixedPosition(updatedLatDeg, updatedLongDeg);

        final MarkConfiguration mc2a = createdCourse.getMarkConfigurationByEffectiveName("mc2");
        // mc2a.getCurrentTrackingDeviceId() may describe the virtual PING device and hence shouldn't be expected to be null
        assertEquals(latDeg, mc2a.getLastKnownPosition().getLatDeg(), 0.0);
        assertEquals(longDeg, mc2a.getLastKnownPosition().getLngDeg(), 0.0);
        mc2a.setTrackingDeviceId(deviceId);

        final MarkConfiguration mc3a = createdCourse.getMarkConfigurationByEffectiveName("mc3");
        assertEquals(latDeg, mc3a.getLastKnownPosition().getLatDeg(), 0.0);
        assertEquals(longDeg, mc3a.getLastKnownPosition().getLngDeg(), 0.0);
        mc3a.unsetPositioning();

        final MarkConfiguration mc4a = createdCourse.getMarkConfigurationByEffectiveName("mc4");
        assertNull(mc4a.getLastKnownPosition());
        mc4a.setFixedPosition(updatedLatDeg, updatedLongDeg);

        final MarkConfiguration mc5a = createdCourse.getMarkConfigurationByEffectiveName("mc5");
        // mc5a.getCurrentTrackingDeviceId() may describe the virtual PING device and hence shouldn't be expected to be null
        assertEquals(mc5a.getLastKnownPosition().getLatDeg(), 1.0, 0.0);
        assertEquals(mc5a.getLastKnownPosition().getLngDeg(), 1.0, 0.0);
        mc5a.setFixedPosition(updatedLatDeg, updatedLongDeg);
        mc5a.setStoreToInventory(true);

        logger.info("createdCourseChanged: " + createdCourse);

        CourseConfiguration updatedCourse = courseConfigurationApi.createCourse(ctx, createdCourse, regattaName,
                race.getRaceName(), "Default");
        logger.info("updatedCourse: " + updatedCourse);

        final CourseConfiguration loadedCourse = courseConfigurationApi.createCourseConfigurationFromCourse(ctx,
                regattaName, race.getRaceName(), "Default", null);
        logger.info(loadedCourse.getJson().toJSONString());
        assertEquals(Util.size(createdCourse.getMarkConfigurations()), Util.size(loadedCourse.getMarkConfigurations()));

        final MarkConfiguration mc1b = loadedCourse.getMarkConfigurationByEffectiveName("mc1");
        assertNotNull(mc1b.getLastKnownPosition());
        assertEquals(updatedLatDeg, mc1b.getLastKnownPosition().getLatDeg(), .0);
        assertEquals(updatedLongDeg, mc1b.getLastKnownPosition().getLngDeg(), .0);

        final MarkConfiguration mc2b = loadedCourse.getMarkConfigurationByEffectiveName("mc2");
        assertFalse(mc2b.getDeviceMappings().isEmpty());
        assertEquals(latDeg, mc2b.getLastKnownPosition().getLatDeg(), 0.0001);
        assertEquals(longDeg, mc2b.getLastKnownPosition().getLngDeg(), 0.0001);

        final MarkConfiguration mc3b = loadedCourse.getMarkConfigurationByEffectiveName("mc3");
        // TODO: following assertfails, but mc3a.unsetPositioning(); was called (position: null); but what's the intented semantics? If a tracking device is associated and a request does provide a null positioning, shall this terminate an existing device mapping? Starting at which time?
//        assertNull(mc3b.getCurrentTrackingDeviceId());
        // there is still the last known position caused by the ping:
        assertEquals(latDeg, mc3b.getLastKnownPosition().getLatDeg(), 0.0);
        assertEquals(longDeg, mc3b.getLastKnownPosition().getLngDeg(), 0.0);

        final MarkConfiguration mc4b = loadedCourse.getMarkConfigurationByEffectiveName("mc4");
        assertNotNull(mc4b.getLastKnownPosition());
        assertEquals(updatedLatDeg, mc4b.getLastKnownPosition().getLatDeg(), .0);
        assertEquals(updatedLongDeg, mc4b.getLastKnownPosition().getLngDeg(), .0);

        // final MarkConfiguration mc5b = loadedCourse.getMarkConfigurationByEffectiveName("mc5");
        // TODO: position is not updated when mc5a.setStoreToInventory(true), so following asserts fail
        // but the mark property is updated
        // assertEquals(updatedLatDeg, mc5b.getEffectivePositioning().getLatitudeDeg().doubleValue(), 0.0);
        // assertEquals(updatedLongDeg, mc5b.getEffectivePositioning().getLongitudeDeg().doubleValue(), 0.0);
        final MarkProperties reloadedMp5 = markPropertiesApi.getMarkProperties(sharedServerCtx, mp5.getId());
        assertEquals(updatedLatDeg, reloadedMp5.getLatDeg().doubleValue(), .0);
        assertEquals(updatedLongDeg, reloadedMp5.getLonDeg().doubleValue(), .0);
    }

    @SeleniumTestCase
    public void testCreateCourseAndReloadWithAdmin() {
        testCreateCourseAndReload(ctx, sharedServerCtx);
    }

    @SeleniumTestCase
    public void testCreateCourseAndReloadWithUser() {
        clearState(getContextRoot());
        super.setUp();
        final AdminConsolePage adminConsole = goToPage(getWebDriver(), getContextRoot());
        adminConsole.goToLocalServerPanel().setSelfServiceServer(true);
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
        securityApi.createUser(adminCtx, "donald", "Donald Duck", null, DONALDS_PASSWORD);
        final ApiContext ctx = createApiContext(getContextRoot(), SERVER_CONTEXT, "donald", DONALDS_PASSWORD);
        final ApiContext sharedServerCtx = createApiContext(getContextRoot(), SHARED_SERVER_CONTEXT, "donald", DONALDS_PASSWORD);
        testCreateCourseAndReload(ctx, sharedServerCtx);
    }

    public void testCreateCourseAndReload(final ApiContext ctx, final ApiContext sharedServerCtx) {
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, 1)[0];
        final String pinEndName = "Start/Finish Pin";
        MarkConfiguration sfp = MarkConfiguration.createFreestyle(null, null, null, pinEndName, "SFP", null, null, null,
                MarkType.BUOY.name(), Collections.emptySet());
        sfp.setFixedPosition(47.159776, 27.5891346);
        sfp.setStoreToInventory(true);
        MarkConfiguration sfb = MarkConfiguration.createFreestyle(null, null, null, "Start/Finish Boat", "SFB", null,
                null, null, MarkType.STARTBOAT.name(), Collections.emptySet());
        sfp.setStoreToInventory(true);
        WaypointWithMarkConfiguration wp1 = new WaypointWithMarkConfiguration("Start", "S", PassingInstruction.Gate,
                Arrays.asList(sfp.getId(), sfb.getId()));
        WaypointWithMarkConfiguration wp2 = new WaypointWithMarkConfiguration("Finish", "F", PassingInstruction.Gate,
                Arrays.asList(sfp.getId(), sfb.getId()));
        CourseConfiguration courseConfiguration = new CourseConfiguration("my-freestyle-course",
                Arrays.asList(sfp, sfb), Arrays.asList(wp1, wp2));
        CourseConfiguration createdCourseConfiguration = courseConfigurationApi.createCourse(ctx, courseConfiguration,
                regattaName, race.getRaceName(), "Default");
        assertCourseConfigurationCompared(sharedServerCtx, courseConfiguration, createdCourseConfiguration);
        CourseConfiguration reloadedCourseConfiguration = courseConfigurationApi
                .createCourseConfigurationFromCourse(ctx, regattaName, race.getRaceName(), "Default", null);
        assertCourseConfigurationCompared(sharedServerCtx, courseConfiguration, reloadedCourseConfiguration);
        MarkConfiguration startboatConfigurationResult = reloadedCourseConfiguration
                .getMarkConfigurationByEffectiveName(pinEndName);
        assertNotNull(startboatConfigurationResult.getMarkPropertiesId());
        assertNotNull(startboatConfigurationResult.getMarkId());
        Mark markResult = leaderboardApi.getMark(ctx, regattaName, startboatConfigurationResult.getMarkId());
        assertEquals(startboatConfigurationResult.getMarkPropertiesId(), markResult.getOriginatingMarkPropertiesId());
        MarkProperties createdMarkProperties = markPropertiesApi.getMarkProperties(sharedServerCtx,
                startboatConfigurationResult.getMarkPropertiesId());
        assertEquals(startboatConfigurationResult.getMarkPropertiesId(), createdMarkProperties.getId());
        assertEquals(pinEndName, createdMarkProperties.getName());
        leaderboardApi.startRaceLogTracking(ctx, regattaName, race.getRaceName(), "Default");
        CourseConfiguration reloadedCourseConfigurationAfterTrackingStarted = courseConfigurationApi
                .createCourseConfigurationFromCourse(ctx, regattaName, race.getRaceName(), "Default", null);
        assertCourseConfigurationCompared(sharedServerCtx, courseConfiguration, reloadedCourseConfigurationAfterTrackingStarted);
        Course course = regattaApi.getCourse(ctx, regattaName, race.getRaceName(), "Default");
        assertEquals(courseConfiguration.getName(), course.getName());
    }

    @SeleniumTestCase
    public void testGetEmptyCourseWithPredefinedMarks() {
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn[] races = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, 2);
        final RaceColumn race1 = races[0];
        final RaceColumn race2 = races[1];

        String markName = "some mark";
        MarkConfiguration mark = MarkConfiguration.createFreestyle(null, null, null, markName, null, null, null, null,
                MarkType.BUOY.name(), Collections.emptySet());

        WaypointWithMarkConfiguration wp = new WaypointWithMarkConfiguration(null, null, PassingInstruction.Starboard,
                Arrays.asList(mark.getId()));

        CourseConfiguration courseConfiguration = new CourseConfiguration("my-freestyle-course", Arrays.asList(mark),
                Arrays.asList(wp));

        courseConfigurationApi.createCourse(ctx, courseConfiguration, regattaName, race1.getRaceName(), "Default");

        CourseConfiguration courseConfigurationForRace2 = courseConfigurationApi.createCourseConfigurationFromCourse(
                ctx, regattaName, race2.getRaceName(), "Default", Collections.emptySet());

        assertEquals(1, Util.size(courseConfigurationForRace2.getMarkConfigurations()));
        assertEquals(0, Util.size(courseConfigurationForRace2.getWaypoints()));

        MarkConfiguration previouslyDefinedMark = courseConfigurationForRace2.getMarkConfigurations().iterator().next();
        assertEquals(markName, previouslyDefinedMark.getEffectiveProperties().getName());
    }

    @SeleniumTestCase
    public void testCreateCourseConfigurationFromTemplate() {
        final List<MarkRole> markRolesInWpt1 = new ArrayList<>();
        final List<MarkTemplate> markTemplates = new ArrayList<>();
        MarkRole mr1 = markRoleApi.createMarkRole(sharedServerCtx, "mark role 1", "mr1");
        markRolesInWpt1.add(mr1);
        MarkTemplate mt1 = markTemplateApi.createMarkTemplate(sharedServerCtx, "mark role 1", "mr1", "#FFFFFF", "Cylinder",
                "Checkered", MarkType.BUOY.name());
        markTemplates.add(mt1);
        final Map<MarkRole, MarkTemplate> roleMapping = new HashMap<>();
        roleMapping.put(mr1, mt1);
        final List<WaypointTemplate> waypointTemplates = new ArrayList<>();
        WaypointTemplate wpt1 = new WaypointTemplate("wpt1", PassingInstruction.FixedBearing, markRolesInWpt1);
        waypointTemplates.add(wpt1);
        final List<String> tags = new ArrayList<>();
        CourseTemplate courseTemplate = new CourseTemplate("test", "t", markTemplates, roleMapping, waypointTemplates, null,
                tags, null, null);
        CourseTemplate srcCourseTemplate = courseTemplateApi.createCourseTemplate(sharedServerCtx, courseTemplate);
        CourseConfiguration courseConfiguration = courseConfigurationApi.createCourseConfigurationFromCourseTemplate(
                ctx, srcCourseTemplate.getId(), /* optionalRegattaName */ null, /* tags */ null, /* optionalNumberOfLaps */ null);
        assertNotNull(courseConfiguration);
        assertConsistentCourseConfiguration(courseConfiguration);
    }

    @SeleniumTestCase
    public void testCreateCourseConfigurationWithStoreToInventoryWithAdmin() {
        testCreateCourseConfigurationWithStoreToInventory(ctx, sharedServerCtx);
    }

    @SeleniumTestCase
    public void testCreateCourseConfigurationWithStoreToInventoryWithUser() {
        clearState(getContextRoot());
        super.setUp();
        final AdminConsolePage adminConsole = goToPage(getWebDriver(), getContextRoot());
        adminConsole.goToLocalServerPanel().setSelfServiceServer(true);
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
        securityApi.createUser(adminCtx, "donald", "Donald Duck", null, DONALDS_PASSWORD);
        final ApiContext ctx = createApiContext(getContextRoot(), SERVER_CONTEXT, "donald", DONALDS_PASSWORD);
        final ApiContext sharedServerCtx = createApiContext(getContextRoot(), SHARED_SERVER_CONTEXT, "donald", DONALDS_PASSWORD);
        testCreateCourseConfigurationWithStoreToInventory(ctx, sharedServerCtx);
    }

    private void testCreateCourseConfigurationWithStoreToInventory(final ApiContext ctx, final ApiContext sharedServerCtx) {
        final String STARTBOAT_NAME = "startboat";
        MarkConfiguration sb = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_sb", /* shortName */ null).getId(), STARTBOAT_NAME,
                "sb", null, null, null, null, Collections.emptySet());
        final double startBoatLatDeg = 5.5;
        final double startBoatLonDeg = 7.1;
        sb.setFixedPosition(startBoatLatDeg, startBoatLonDeg);
        sb.setStoreToInventory(true);
        MarkConfiguration pe = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_pe", /* shortName */ null).getId(), "pin end", "pe",
                null, null, null, null, Collections.emptySet());
        pe.setStoreToInventory(true);
        UUID randomDeviceId = UUID.randomUUID();
        pe.setTrackingDeviceId(randomDeviceId);
        MarkConfiguration bl = MarkConfiguration.createFreestyle(null, null,
                markRoleApi.createMarkRole(sharedServerCtx, "role_bl", /* shortName */ null).getId(), "1", null,
                "#0000FF", null, null, null, Collections.emptySet());
        MarkTemplate mtb2 = markTemplateApi.createMarkTemplate(sharedServerCtx, "mark template 1", "mt1", "#FFFFFF", "Cylinder",
                "Checkered", MarkType.BUOY.name());
        MarkConfiguration b2 = MarkConfiguration.createMarkTemplateBased(mtb2.getId(),
                markRoleApi.createMarkRole(sharedServerCtx, "role_b2", /* shortName */ null).getId());
        b2.setStoreToInventory(true);
        eventApi.createEvent(ctx, "testregatta", "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, "testregatta", null, 1)[0];
        Mark mark = markApi.addMarkToRegatta(ctx, "testregatta", "mymark");
        markApi.addMarkFix(ctx, "testregatta", race.getRaceName(), "Default", mark.getMarkId(),
                /* markTemplateId */ UUID.randomUUID(), /* markPropertiesId */ UUID.randomUUID(), 9.12, .599,
                currentTimeMillis());
        MarkConfiguration b3 = MarkConfiguration.createMarkBased(mark.getMarkId(),
                markRoleApi.createMarkRole(sharedServerCtx, "role_b3", /* shortName */ null).getId());
        b3.setStoreToInventory(true);
        WaypointWithMarkConfiguration wp1 = new WaypointWithMarkConfiguration("start/end", "s/e",
                PassingInstruction.Line, Arrays.asList(sb.getId(), pe.getId()));
        WaypointWithMarkConfiguration wp2 = new WaypointWithMarkConfiguration(null, null, PassingInstruction.Port,
                Arrays.asList(bl.getId()));
        WaypointWithMarkConfiguration wp3 = new WaypointWithMarkConfiguration(null, null,
                PassingInstruction.Single_Unknown, Arrays.asList(b2.getId()));
        WaypointWithMarkConfiguration wp4 = new WaypointWithMarkConfiguration(null, null,
                PassingInstruction.Single_Unknown, Arrays.asList(b3.getId()));
        CourseConfiguration courseConfiguration = new CourseConfiguration("my-course",
                Arrays.asList(sb, pe, bl, b2, b3), Arrays.asList(wp1, wp2, wp1, wp3, wp4));
        CourseConfiguration courseConfigurationResult = courseConfigurationApi.createCourseTemplate(ctx,
                courseConfiguration, "testregatta");
        assertCourseConfigurationCompared(sharedServerCtx, courseConfiguration, courseConfigurationResult);
        Set<UUID> expectedMarkProperties = new HashSet<>();
        courseConfigurationResult.getMarkConfigurations().forEach(mc -> {
            if (mc.getMarkPropertiesId() != null) {
                expectedMarkProperties.add(mc.getMarkPropertiesId());
            }
        });
        Iterable<MarkProperties> allMarkProperties = markPropertiesApi.getAllMarkProperties(sharedServerCtx,
                Collections.emptySet());
        Set<UUID> availableMarkProperties = new HashSet<>();
        allMarkProperties.forEach(mp -> availableMarkProperties.add(mp.getId()));
        assertEquals(expectedMarkProperties, availableMarkProperties);
        final double newLatStartBoat = 123.4;
        final double newLonStartBoat = 89.0;
        UUID startBoatMarkPropertiesId = null;
        for (final MarkProperties mp : allMarkProperties) {
            // for the start boat verify that the fixed positioning has been copied to the MarkProperties object
            if (mp.getName().equals(STARTBOAT_NAME)) {
                startBoatMarkPropertiesId = mp.getId();
                assertEquals(mp.getLatDeg(), startBoatLatDeg, 0.0);
                assertEquals(mp.getLonDeg(), startBoatLonDeg, 0.0);
            }
        }
        // prepare for updating a new position specification for the start boat, but at first not requesting store to inventory:
        sb.setFixedPosition(newLatStartBoat, newLonStartBoat);
        sb.setMarkPropertiesId(startBoatMarkPropertiesId);
        sb.setStoreToInventory(false);
        courseConfigurationApi.createCourseTemplate(ctx, courseConfiguration, "testregatta");
        // now ensure that the MarkProperties object for the start boat has not been updated with the new position:
        final MarkProperties newStartBoatMp = markPropertiesApi.getMarkProperties(sharedServerCtx, startBoatMarkPropertiesId);
        assertEquals(newStartBoatMp.getLatDeg(), startBoatLatDeg, 0.0);
        assertEquals(newStartBoatMp.getLonDeg(), startBoatLonDeg, 0.0);
        // repeat the action, this time with storeToInventory==true
        sb.setStoreToInventory(true);
        courseConfigurationApi.createCourseTemplate(ctx, courseConfiguration, "testregatta");
        // now ensure that the MarkProperties object for the start boat *has* been updated with the new position:
        final MarkProperties newNewStartBoatMp = markPropertiesApi.getMarkProperties(sharedServerCtx, startBoatMarkPropertiesId);
        assertEquals(newNewStartBoatMp.getLatDeg(), newLatStartBoat, 0.0);
        assertEquals(newNewStartBoatMp.getLonDeg(), newLonStartBoat, 0.0);
    }

    @SeleniumTestCase
    public void testCreateCourseConfigurationFromCourse() {
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, 1)[0];
        leaderboardApi.startRaceLogTracking(ctx, regattaName, race.getRaceName(), "Default");
        CourseConfiguration createdCourseConfiguration = courseConfigurationApi.createCourseConfigurationFromCourse(ctx,
                regattaName, race.getRaceName(), "Default", /* tags */ null);
        assertConsistentCourseConfiguration(createdCourseConfiguration);
    }

    @SeleniumTestCase
    public void testCreateCourseFromCourseConfiguration() {
        final String regattaName = "test";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, /* prefix */ null, 1)[0];
        final CourseConfiguration simpleCourseConfiguration = createSimpleCourseConfiguration(sharedServerCtx);
        assertConsistentCourseConfiguration(simpleCourseConfiguration);
        CourseConfiguration createdCourseConfiguration = courseConfigurationApi.createCourse(ctx,
                simpleCourseConfiguration, regattaName, race.getRaceName(), "Default");
        assertConsistentCourseConfiguration(createdCourseConfiguration);
        assertCourseConfigurationCompared(sharedServerCtx, simpleCourseConfiguration, createdCourseConfiguration);
    }

    @SeleniumTestCase
    public void testCreateCourseTemplateFromCourseConfiguration() {
        final CourseConfiguration simpleCourseConfiguration = createSimpleCourseConfiguration(sharedServerCtx);
        logger.info(simpleCourseConfiguration.getJson().toJSONString());
        final CourseConfiguration createdCourseConfiguration = courseConfigurationApi.createCourseTemplate(ctx,
                simpleCourseConfiguration, /* optionalRegattaName */ null);
        assertConsistentCourseConfiguration(createdCourseConfiguration);
        assertCourseConfigurationCompared(sharedServerCtx, simpleCourseConfiguration, createdCourseConfiguration);
    }
    
    @SeleniumTestCase
    public void createCourseConfigurationWithFreestyleUpdateForExistingMarkProperty() {
        final double MP_LAT_DEG = 49.097487;
        final double MP_LNG_DEG = 8.648631;
        final UUID markPropertiesId = markPropertiesApi.createMarkProperties(sharedServerCtx, "mpWithPos", "mpWithPos",
                /* deviceUuid */ null, "#ffffff", "shape", "pattern", MarkType.LANDMARK.name(),
                 Collections.emptyList(), MP_LAT_DEG, MP_LNG_DEG).getId();
        final Set<String> tags = new HashSet<>();
        tags.add("tag1");
        final MarkConfiguration mc1 = MarkConfiguration.createFreestyle(null, markPropertiesId, null, null, null,
                null, null, null, null, tags);
        mc1.setStoreToInventory(true);
        final WaypointWithMarkConfiguration wp1 = new WaypointWithMarkConfiguration("landmark", "landmark",
                PassingInstruction.Single_Unknown, Arrays.asList(mc1.getId()));
        final CourseConfiguration courseConfiguration = new CourseConfiguration("test1", Arrays.asList(mc1),
                Arrays.asList(wp1));
        final String regattaName = "Regatta1";
        eventApi.createEvent(ctx, regattaName, "", CompetitorRegistrationType.CLOSED, "");
        final RaceColumn race = regattaApi.addRaceColumn(ctx, regattaName, null, 1)[0];
        courseConfigurationApi.createCourse(ctx, courseConfiguration, regattaName,
                race.getRaceName(), "Default");
        MarkProperties markProperties = markPropertiesApi.getMarkProperties(sharedServerCtx, markPropertiesId);
        Set<String> updatedTags = markProperties.getTags();
        assertEquals(tags, updatedTags);
    }

    private CourseConfiguration createSimpleCourseConfiguration(final ApiContext ctx) {
        List<MarkConfiguration> markConfigurations = new ArrayList<MarkConfiguration>();
        MarkTemplate markTemplate = markTemplateApi.createMarkTemplate(ctx, "test", "test", "#ffffff", "shape",
                "pattern", MarkType.LANDMARK.name());
        MarkConfiguration markConfiguration = MarkConfiguration.createMarkTemplateBased(markTemplate.getId(),
                markRoleApi.createMarkRole(ctx, "test", /* shortName */ null).getId());
        markConfigurations.add(markConfiguration);
        List<String> markConfigurationIds = markConfigurations.stream().map(mc -> mc.getId())
                .collect(Collectors.toList());
        List<WaypointWithMarkConfiguration> waypoints = new ArrayList<>();
        WaypointWithMarkConfiguration waypoint = new WaypointWithMarkConfiguration(null, null, PassingInstruction.Line,
                markConfigurationIds);
        waypoints.add(waypoint);
        return new CourseConfiguration("test-course", markConfigurations, waypoints);
    }
}
