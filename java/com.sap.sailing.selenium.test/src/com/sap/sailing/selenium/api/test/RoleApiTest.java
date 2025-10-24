package com.sap.sailing.selenium.api.test;

import static com.sap.sailing.selenium.api.core.ApiContext.SECURITY_CONTEXT;
import static com.sap.sailing.selenium.api.core.ApiContext.createAdminApiContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Collection;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.selenium.api.core.ApiContext;
import com.sap.sailing.selenium.api.event.RoleApi;
import com.sap.sailing.selenium.api.event.RoleApi.RoleDefinition;
import com.sap.sailing.selenium.core.SeleniumTestCase;
import com.sap.sailing.selenium.test.AbstractSeleniumTest;
import com.sap.sse.common.Util;

public class RoleApiTest extends AbstractSeleniumTest {

    private final RoleApi roleApi = new RoleApi();

    @BeforeEach
    public void setUp() {
        clearState(getContextRoot(), /* headless */ true);
    }

    @SeleniumTestCase
    public void testCreateAndGetRole() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);

        final String testRoleName = "test role 123";
        final RoleDefinition roleCreated = roleApi.createRole(adminCtx, testRoleName);

        assertEquals(testRoleName, roleCreated.getName(), "Responded role name of createRole is different!");
        assertNotNull(roleCreated.getId(), "UUID cannot be null!");
        assertTrue(Util.isEmpty(roleCreated.getPermissions()), "Permissions should be empty!");

        final RoleDefinition roleGet = roleApi.getRole(adminCtx, roleCreated.getId());

        assertEquals(roleCreated.getName(), roleGet.getName(), "Responded role name of getRole is different!");
        assertEquals(roleCreated.getId(), roleGet.getId(), "UUID must be the same!");
        assertEquals(roleCreated.getPermissions(), roleGet.getPermissions(), "Permissions changed!");

        roleApi.deleteRole(adminCtx, roleCreated.getId());
    }

    @SeleniumTestCase
    public void testDeleteRole() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);

        final String testRoleName = "test role 123";
        final RoleDefinition roleCreated = roleApi.createRole(adminCtx, testRoleName);

        assertNotNull(roleCreated.getId(), "UUID cannot be null!");

        final RoleDefinition roleGet = roleApi.getRole(adminCtx, roleCreated.getId());

        assertEquals(roleCreated.getId(), roleGet.getId(), "UUID must be the same!");

        roleApi.deleteRole(adminCtx, roleCreated.getId());

        try {
            roleApi.getRole(adminCtx, roleCreated.getId());
            fail("Expected exception since role should not be parsable!");
        } catch (RuntimeException e) {
            assertTrue(e.getMessage().contains("No role with id"), "Unrelated error");
        }
    }

    @SeleniumTestCase
    public void testUpdateRole() {
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);

        final String testRoleName = "test role 123";
        final RoleDefinition roleCreated = roleApi.createRole(adminCtx, testRoleName);

        assertNotNull(roleCreated.getId(), "UUID cannot be null!");

        final RoleDefinition roleGet = roleApi.getRole(adminCtx, roleCreated.getId());

        assertEquals(roleCreated.getId(), roleGet.getId(), "UUID must be the same!");

        final Collection<String> permissions = new ArrayList<>();
        permissions.add("COMPETITOR,EVENT:READ");
        permissions.add("BOAT:READ");

        final String updatedRoleName = roleCreated.getName() + "_changed";
        final String roleUpdate = roleApi.updateRole(adminCtx, roleCreated.getId(), permissions, updatedRoleName);

        assertNull(roleUpdate, "Empty string expected.");

        final RoleDefinition roleGetAfterUpdate = roleApi.getRole(adminCtx, roleCreated.getId());

        assertEquals(updatedRoleName, roleGetAfterUpdate.getName(), "Responded role name of getRole is different!");
        assertEquals(roleCreated.getId(), roleGetAfterUpdate.getId(), "UUID must be the same!");

        // |A| = |B| ∧ ∀a∈ A: a∈ B --> A and B have the same elements, ignoring order
        assertEquals(permissions.size(), Util.size(roleGetAfterUpdate.getPermissions()), "Permissions changed!");
        for (String permission : roleGetAfterUpdate.getPermissions()) {
            assertTrue(permissions.contains(permission), "Permissions missing!");
        }

        roleApi.deleteRole(adminCtx, roleCreated.getId());
    }

    @SeleniumTestCase
    public void createRoleWithDuplicateNameShouldFailTest() {
        final String roleName = "DUPLICATE_CHECK_ROLE";
        final ApiContext adminCtx = createAdminApiContext(getContextRoot(), SECURITY_CONTEXT);
        roleApi.createRole(adminCtx, roleName);
        try {
            roleApi.createRole(adminCtx, roleName);
            fail();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
