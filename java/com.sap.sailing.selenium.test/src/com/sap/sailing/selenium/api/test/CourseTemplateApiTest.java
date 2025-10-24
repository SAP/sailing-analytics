package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SECURITY_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.SHARED_SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static com.sap.sailing.selenium.api.core.ApiContext.createApiContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.core.HttpException;
import com.sap.sailing.selenium.api.coursetemplate.CourseTemplate;
import com.sap.sailing.selenium.api.coursetemplate.CourseTemplateApi;
import com.sap.sailing.selenium.api.coursetemplate.MarkRole;
import com.sap.sailing.selenium.api.coursetemplate.MarkRoleApi;
import com.sap.sailing.selenium.api.coursetemplate.MarkTemplate;
import com.sap.sailing.selenium.api.event.SecurityApi;
import com.sap.sailing.selenium.api.helper.CourseTemplateDataFactory;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

public class CourseTemplateApiTest extends AbstractSeleniumTest {
    private static final String DONALDS_PASSWORD = "dai982734(*^(sy0815";
    private final CourseTemplateApi courseTemplateApi = new CourseTemplateApi();
    private final MarkRoleApi markRoleApi = new MarkRoleApi();
    private ApiContext ctx;
    private CourseTemplateDataFactory ctdf;

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
        ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        ctdf = new CourseTemplateDataFactory(ctx);
    }

    @SeleniumTestCase
    public void createSimpleCourseTemplateTest() {
        final CourseTemplate courseTemplateToSave = ctdf.constructCourseTemplate();
        final CourseTemplate createdCourseTemplate = courseTemplateApi.createCourseTemplate(ctx, courseTemplateToSave);
        assertEquals(courseTemplateToSave.getName(), createdCourseTemplate.getName());
        assertEquals(ctdf.waypointSequence.size(), Util.size(createdCourseTemplate.getWaypoints()));
    }

    @SeleniumTestCase
    public void cantUseOthersMarkTemplatesTest() {
        final ApiContext adminSecurityCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
        new SecurityApi().createUser(adminSecurityCtx, "donald", "Donald Duck", null, DONALDS_PASSWORD);
        final ApiContext otherUserCtx = createApiContext(getContextRoot(), SHARED_SERVER_CONTEXT, "donald", DONALDS_PASSWORD);

        try {
            courseTemplateApi.createCourseTemplate(otherUserCtx, ctdf.constructCourseTemplate());
            fail();
        } catch (Exception e) {
            final String expected = "Subject does not have permission [MARK_ROLE:READ:";
            assertTrue(e.getMessage().contains(expected), "Expected "+e.getMessage()+" to contain "+expected);
        }
    }

    @SeleniumTestCase
    public void createCourseTemplateWithRepeatablePartTest() {
        final Pair<Integer, Integer> repeatablePart = new Pair<>(1, 3);
        final CourseTemplate courseTemplateToSave = ctdf.constructCourseTemplate(repeatablePart,
                /* defaultNumberOfLaps */null);

        final CourseTemplate createdCourseTemplate = courseTemplateApi.createCourseTemplate(ctx, courseTemplateToSave);

        assertEquals(repeatablePart, createdCourseTemplate.getOptionalRepeatablePart());
    }

    @SeleniumTestCase
    public void createCourseTemplateWithInvalidRepeatablePartTest() {
        final Pair<Integer, Integer> repeatablePart = new Pair<>(1, 6);
        final CourseTemplate courseTemplateToSave = ctdf.constructCourseTemplate(repeatablePart,
                /* defaultNumberOfLaps */null);
        try {
            courseTemplateApi.createCourseTemplate(ctx, courseTemplateToSave);
            fail();
        } catch (Exception e) {
            assertTrue(e.getMessage().contains("Repeatable part (1, 6) is out of range for sequence of length 5"));
        }
    }

    @SeleniumTestCase
    public void createCourseTemplateWithRoleMappingTest() {
        final Map<MarkRole, MarkTemplate> associatedRoles = new HashMap<>();
        associatedRoles.put(ctdf.sbRole, ctdf.sb);
        associatedRoles.put(ctdf.peRole, ctdf.pe);
        associatedRoles.put(ctdf.b1Role, ctdf.b1);
        associatedRoles.put(ctdf.b4sRole, ctdf.b4s);
        associatedRoles.put(ctdf.b4pRole, ctdf.b4p);
        final CourseTemplate createdCourseTemplate = courseTemplateApi.createCourseTemplate(ctx,
                ctdf.constructCourseTemplate(null, /* defaultNumberOfLaps */null, associatedRoles));
        assertEquals(new TreeSet<>(associatedRoles.keySet()),
                new TreeSet<>(createdCourseTemplate.getRoleMapping().keySet().stream()
                        .map(r -> markRoleApi.getMarkRole(ctx, r.getAssociatedMarkRoleId())).collect(Collectors.toSet())));
    }

    @SeleniumTestCase
    public void createCourseTemplateWithImplicitRoleMappingTest() {
        final CourseTemplate createdCourseTemplate = courseTemplateApi.createCourseTemplate(ctx,
                ctdf.constructCourseTemplate());
        assertEquals(
                new HashSet<>(Arrays.asList(ctdf.sb.getName(), ctdf.pe.getName(), ctdf.b1.getName(), ctdf.b4s.getName(),
                        ctdf.b4p.getName())),
                new HashSet<>(createdCourseTemplate.getRoleMapping().keySet().stream()
                        .map(r -> markRoleApi.getMarkRole(ctx, r.getAssociatedMarkRoleId()).getName())
                        .collect(Collectors.toSet())));
    }

    @SeleniumTestCase
    public void createCourseTemplateWithPartialRoleMappingTest() {
        final Map<MarkRole, MarkTemplate> associatedRoles = new HashMap<>();
        associatedRoles.put(ctdf.b1Role, ctdf.b1);
        associatedRoles.put(ctdf.b4sRole, ctdf.b4s);
        associatedRoles.put(ctdf.b4pRole, ctdf.b4p);
        try {
            courseTemplateApi.createCourseTemplate(ctx, ctdf.constructCourseTemplate(null, /* defaultNumberOfLaps */null, associatedRoles));
            fail("Expected HttpException because all mark roles require a mark template to be assigned");
        } catch (HttpException e) {
            assertEquals(400, e.getHttpStatusCode());
        }
    }
}
