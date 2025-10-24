package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SHARED_SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static java.util.UUID.randomUUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.HttpException;
import com.sap.sailing.selenium.api.coursetemplate.MarkProperties;
import com.sap.sailing.selenium.api.coursetemplate.MarkPropertiesApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class MarkPropertiesTest extends AbstractSeleniumTest {

    private static final String MARK_PROPERTIES_NAME = "testname";
    private static final String MARK_PROPERTIES_SHORTNAME = "testshortname";
    private static final String MARK_PROPERTIES_COLOR = "#FF0000";
    private static final String MARK_PROPERTIES_SHAPE = "shape";
    private static final String MARK_PROPERTIES_PATTERN = "pattern";
    private static final String MARK_PROPERTIES_TYPE = "STARTBOAT";
    private static final List<String> MARK_PROPERTIES_TAGS;
    private static final Double MARK_PROPERTIES_LATDEG = 41.456;
    private static final Double MARK_PROPERTIES_LONDEG = 9.123;

    static {
        final List<String> tags = new ArrayList<>();
        tags.add("tag1");
        MARK_PROPERTIES_TAGS = tags;
    }

    private final MarkPropertiesApi markPropertiesApi = new MarkPropertiesApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
    }

    @SeleniumTestCase
    public void createMarkPropertyWithDeviceUuidTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        final UUID deviceUuid = randomUUID();
        MarkProperties markProperties = markPropertiesApi.createMarkProperties(ctx, MARK_PROPERTIES_NAME,
                MARK_PROPERTIES_SHORTNAME, deviceUuid.toString(), MARK_PROPERTIES_COLOR, "shape", "pattern",
                MARK_PROPERTIES_TYPE, MARK_PROPERTIES_TAGS, null, null);
        assertNotNull(markProperties, "read: no MarkProperties returnded");
        assertDefaultValues(markProperties);
        assertTrue(markProperties.hasDevice());
    }

    @SeleniumTestCase
    public void createAndGetMarkPropertiesWithoutDeviceUuidTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        MarkProperties createdMarkProperties = markPropertiesApi.createMarkProperties(ctx, MARK_PROPERTIES_NAME,
                MARK_PROPERTIES_SHORTNAME, null, MARK_PROPERTIES_COLOR, MARK_PROPERTIES_SHAPE, MARK_PROPERTIES_PATTERN,
                MARK_PROPERTIES_TYPE, MARK_PROPERTIES_TAGS, MARK_PROPERTIES_LATDEG, MARK_PROPERTIES_LONDEG);
        assertNotNull(createdMarkProperties, "create: no MarkProperties returnded");
        assertNotNull(createdMarkProperties.getId(), "create: MarkProperties.id is missing");

        MarkProperties foundMarkProperties = markPropertiesApi.getMarkProperties(ctx, createdMarkProperties.getId());
        assertDefaultValues(foundMarkProperties);
        assertEquals(MARK_PROPERTIES_LATDEG, foundMarkProperties.getLatDeg(), "read: MarkProperties.latDeg is different");
        assertEquals(MARK_PROPERTIES_LONDEG, foundMarkProperties.getLonDeg(), "read: MarkProperties.lonDeg is different");
    }

    @SeleniumTestCase
    public void createSeveralMarkPropertiesAndGetAll() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        MarkProperties markProperties1 = markPropertiesApi.createMarkProperties(ctx, MARK_PROPERTIES_NAME,
                MARK_PROPERTIES_SHORTNAME, null, MARK_PROPERTIES_COLOR, MARK_PROPERTIES_SHAPE, MARK_PROPERTIES_PATTERN,
                MARK_PROPERTIES_TYPE, MARK_PROPERTIES_TAGS, MARK_PROPERTIES_LATDEG, MARK_PROPERTIES_LONDEG);
        MarkProperties markProperties2 = markPropertiesApi.createMarkProperties(ctx, MARK_PROPERTIES_NAME,
                MARK_PROPERTIES_SHORTNAME, null, MARK_PROPERTIES_COLOR, MARK_PROPERTIES_SHAPE, MARK_PROPERTIES_PATTERN,
                MARK_PROPERTIES_TYPE, MARK_PROPERTIES_TAGS, MARK_PROPERTIES_LATDEG, MARK_PROPERTIES_LONDEG);

        final Iterable<MarkProperties> markPropertiesResult = markPropertiesApi.getAllMarkProperties(ctx,
                MARK_PROPERTIES_TAGS);
        for (MarkProperties markProperties : markPropertiesResult) {
            assertTrue(markProperties.getId().equals(markProperties1.getId())
                    || markProperties.getId().equals(markProperties2.getId()));
        }
    }

    @Disabled
    public void createAndUpdateMarkProperties() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        MarkProperties createdMarkProperties = markPropertiesApi.createMarkProperties(ctx, MARK_PROPERTIES_NAME,
                MARK_PROPERTIES_SHORTNAME, null, MARK_PROPERTIES_COLOR, MARK_PROPERTIES_SHAPE, MARK_PROPERTIES_PATTERN,
                MARK_PROPERTIES_TYPE, MARK_PROPERTIES_TAGS, MARK_PROPERTIES_LATDEG, MARK_PROPERTIES_LONDEG);
        final UUID deviceUuid = randomUUID();
        MarkProperties updatedMarkProperties = markPropertiesApi.updateMarkPropertiesPositioning(ctx,
                createdMarkProperties.getId(), deviceUuid, 1.0, 2.0);
        assertTrue(1.0 == updatedMarkProperties.getLatDeg().doubleValue(), "read: MarkProperties.latDeg is different");
        assertTrue(2.0 == updatedMarkProperties.getLonDeg().doubleValue(), "read: MarkProperties.lonDeg is different");
    }

    private void assertDefaultValues(MarkProperties markProperties) {
        assertNotNull(markProperties.getId(), "read: MarkProperties.id is missing");
        assertEquals(MARK_PROPERTIES_NAME, markProperties.getName(), "read: MarkProperties.name is different");
        assertEquals(MARK_PROPERTIES_SHORTNAME, markProperties.getShortName(),
                "read: MarkProperties.shortName is different");
        assertEquals(MARK_PROPERTIES_COLOR, markProperties.getColor(), "read: MarkProperties.color is different");
        assertEquals(MARK_PROPERTIES_SHAPE, markProperties.getShape(), "read: MarkProperties.shape is different");
        assertEquals(MARK_PROPERTIES_PATTERN, markProperties.getPattern(), "read: MarkProperties.pattern is different");
        assertEquals(MARK_PROPERTIES_TYPE, markProperties.getMarkType().name(),
                "read: MarkProperties.type is different");
    }

    @SeleniumTestCase
    public void createAndDeleteMarkPropertiesTest() {
        assertThrows(HttpException.NotFound.class, () -> {
            final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
            MarkProperties createdMarkProperties = markPropertiesApi.createMarkProperties(ctx, MARK_PROPERTIES_NAME,
                    MARK_PROPERTIES_SHORTNAME, null, MARK_PROPERTIES_COLOR, MARK_PROPERTIES_SHAPE, MARK_PROPERTIES_PATTERN,
                    MARK_PROPERTIES_TYPE, MARK_PROPERTIES_TAGS, MARK_PROPERTIES_LATDEG, MARK_PROPERTIES_LONDEG);
            assertNotNull(createdMarkProperties, "create: no MarkProperties returnded");
            assertNotNull(createdMarkProperties.getId(), "create: MarkProperties.id is missing");
    
            markPropertiesApi.deleteMarkProperties(ctx, createdMarkProperties.getId());
            markPropertiesApi.getMarkProperties(ctx, createdMarkProperties.getId());
        });
    }

    @SeleniumTestCase
    public void testExclusionOfDeviceUuidAndFixedPositioning() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        MarkProperties createdMarkProperties = markPropertiesApi.createMarkProperties(ctx, MARK_PROPERTIES_NAME,
                MARK_PROPERTIES_SHORTNAME, null, MARK_PROPERTIES_COLOR, MARK_PROPERTIES_SHAPE, MARK_PROPERTIES_PATTERN,
                MARK_PROPERTIES_TYPE, MARK_PROPERTIES_TAGS, MARK_PROPERTIES_LATDEG, MARK_PROPERTIES_LONDEG);
        assertEquals(MARK_PROPERTIES_LATDEG, createdMarkProperties.getLatDeg());
        assertEquals(MARK_PROPERTIES_LONDEG, createdMarkProperties.getLonDeg());
        assertFalse(createdMarkProperties.hasDevice());

        final UUID deviceUuid = UUID.randomUUID();
        MarkProperties updatedMarkProperties = markPropertiesApi.updateMarkProperties(ctx,
                createdMarkProperties.getId(), MARK_PROPERTIES_NAME, MARK_PROPERTIES_SHORTNAME, deviceUuid.toString(),
                MARK_PROPERTIES_COLOR, MARK_PROPERTIES_SHAPE, MARK_PROPERTIES_PATTERN, MARK_PROPERTIES_TYPE,
                MARK_PROPERTIES_TAGS, null, null);
        assertNull(updatedMarkProperties.getLatDeg());
        assertNull(updatedMarkProperties.getLonDeg());
        assertTrue(updatedMarkProperties.hasDevice());
    }
    
    @SeleniumTestCase
    public void testOverlapOfDeviceUuidAndFixedPositioning() {
        assertThrows(HttpException.class, () -> {
            final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
            final UUID deviceUuid = UUID.randomUUID();
            markPropertiesApi.createMarkProperties(ctx, MARK_PROPERTIES_NAME,
                    MARK_PROPERTIES_SHORTNAME, deviceUuid.toString(), MARK_PROPERTIES_COLOR, MARK_PROPERTIES_SHAPE, MARK_PROPERTIES_PATTERN,
                    MARK_PROPERTIES_TYPE, MARK_PROPERTIES_TAGS, MARK_PROPERTIES_LATDEG, MARK_PROPERTIES_LONDEG);
        });
    }
}
