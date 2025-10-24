package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SHARED_SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.coursetemplate.MarkTemplate;
import com.sap.sailing.selenium.api.coursetemplate.MarkTemplateApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class MarkTemplateTest extends AbstractSeleniumTest {

    private static final String MARK_TEMPLATE_NAME = "testname";
    private static final String MARK_TEMPLATE_SHORTNAME = "testshortname";
    private static final String MARK_TEMPLATE_COLOR = "#FF0000";
    private static final String MARK_TEMPLATE_SHAPE = "shape";
    private static final String MARK_TEMPLATE_PATTERN = "pattern";
    private static final String MARK_TEMPLATE_TYPE = "STARTBOAT";

    private final MarkTemplateApi markTemplateApi = new MarkTemplateApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
    }

    @SeleniumTestCase
    public void createMarkTemplateWithDeviceUuidTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        MarkTemplate markTemplate = markTemplateApi.createMarkTemplate(ctx, MARK_TEMPLATE_NAME, MARK_TEMPLATE_SHORTNAME,
                MARK_TEMPLATE_COLOR, MARK_TEMPLATE_SHAPE, MARK_TEMPLATE_PATTERN, MARK_TEMPLATE_TYPE);
        assertNotNull(markTemplate, "read: no MarkTemplate returnded");
        assertDefaultValues(markTemplate);
    }

    private void assertDefaultValues(MarkTemplate markTemplate) {
        assertNotNull(markTemplate.getId(), "read: MarkTemplate.id is missing");
        assertEquals(MARK_TEMPLATE_NAME, markTemplate.getName(), "read: MarkTemplate.name is different");
        assertEquals(MARK_TEMPLATE_SHORTNAME, markTemplate.getShortName(), "read: MarkTemplate.shortName is different");
        assertEquals(MARK_TEMPLATE_COLOR, markTemplate.getColor(), "read: MarkTemplate.color is different");
        assertEquals(MARK_TEMPLATE_SHAPE, markTemplate.getShape(), "read: MarkTemplate.shape is different");
        assertEquals(MARK_TEMPLATE_PATTERN, markTemplate.getPattern(), "read: MarkTemplate.pattern is different");
        assertEquals(MARK_TEMPLATE_TYPE, markTemplate.getMarkType().name(), "read: MarkTemplate.type is different");
    }

    @SeleniumTestCase
    public void createAndGetMarkTemplateTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        MarkTemplate createdMarkTemplate = markTemplateApi.createMarkTemplate(ctx, MARK_TEMPLATE_NAME,
                MARK_TEMPLATE_SHORTNAME, MARK_TEMPLATE_COLOR, MARK_TEMPLATE_SHAPE, MARK_TEMPLATE_PATTERN,
                MARK_TEMPLATE_TYPE);
        MarkTemplate foundMarkTemplate = markTemplateApi.getMarkTemplate(ctx, createdMarkTemplate.getId());
        assertDefaultValues(foundMarkTemplate);
    }

    @Disabled
    public void createSeveralMarkTemplateAndGetAllTest() {
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        MarkTemplate markTemplate1 = markTemplateApi.createMarkTemplate(ctx, MARK_TEMPLATE_NAME,
                MARK_TEMPLATE_SHORTNAME, MARK_TEMPLATE_COLOR, MARK_TEMPLATE_SHAPE, MARK_TEMPLATE_PATTERN,
                MARK_TEMPLATE_TYPE);
        MarkTemplate markTemplate2 = markTemplateApi.createMarkTemplate(ctx, MARK_TEMPLATE_NAME,
                MARK_TEMPLATE_SHORTNAME, MARK_TEMPLATE_COLOR, MARK_TEMPLATE_SHAPE, MARK_TEMPLATE_PATTERN,
                MARK_TEMPLATE_TYPE);

        final Iterable<MarkTemplate> markTemplateResult = markTemplateApi.getAllMarkTemplates(ctx);
        for (MarkTemplate markTemplate : markTemplateResult) {
            assertTrue(markTemplate.getId().equals(markTemplate1.getId())
                    || markTemplate.getId().equals(markTemplate2.getId()));
        }
    }
}
