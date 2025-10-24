package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SHARED_SERVER_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.coursetemplate.MarkRole;
import com.sap.sailing.selenium.api.coursetemplate.MarkRoleApi;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;

public class MarkRoleTest extends AbstractSeleniumTest {

    private final MarkRoleApi markRoleApi = new MarkRoleApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
    }

    @SeleniumTestCase
    public void createMarkRoleAdminTest() {
        final String roleName1 = "role_sb";
        final String roleName2 = "role_pe";
        final ApiContext ctx = createAdminApiContext(getContextRoot(), SHARED_SERVER_CONTEXT);
        final MarkRole markRole1 = markRoleApi.createMarkRole(ctx, roleName1, /* shortName */ null);
        assertNotNull(markRole1.getId());
        assertEquals(roleName1, markRole1.getName());
        final MarkRole markRoleReloaded = markRoleApi.getMarkRole(ctx, markRole1.getId());
        assertNotNull(markRoleReloaded.getId());
        assertEquals(roleName1, markRoleReloaded.getName());
        final MarkRole markRole2 = markRoleApi.createMarkRole(ctx, roleName2, /* shortName */ null);
        final Iterable<MarkRole> markRoles = markRoleApi.getAllMarkRoles(ctx);
        for (MarkRole markRole : markRoles) {
            assertTrue(markRole.getId().equals(markRole1.getId()) || markRole.getId().equals(markRole2.getId()));
        }
    }
}
