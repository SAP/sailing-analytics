package com.sap.sse.security.userstore.mongodb;

import java.util.Map;
import java.util.UUID;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.security.UserStore;
import com.sap.sse.security.shared.AccessControlListAnnotation;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.Role;
import com.sap.sse.security.shared.RoleDefinition;
import com.sap.sse.security.shared.SecurityUser;
import com.sap.sse.security.shared.Tenant;
import com.sap.sse.security.shared.User;
import com.sap.sse.security.shared.UserGroup;
import com.sap.sse.security.shared.UserManagementException;

public interface DomainObjectFactory {
    Iterable<AccessControlListAnnotation> loadAllAccessControlLists(UserStore userStore);
    
    Iterable<OwnershipAnnotation> loadAllOwnerships(UserStore userStore);
    
    Iterable<RoleDefinition> loadAllRoleDefinitions();
    
    /**
     * Loads user groups and tenants from the persistent store. The users {@link UserGroup#getUsers() contained} therein
     * are proxies and must be replaced by the caller once the real {@link SecurityUser} objects have been loaded from
     * the store. The proxies only have the correct {@link SecurityUser#getName() name} field set which also acts as the
     * {@link SecurityUser#getId() user ID}. {@link UserGroup#remove(SecurityUser)} and
     * {@link UserGroup#add(SecurityUser)} have to be used for this process.
     * <p>
     * 
     * {@link Tenant}s are special {@link UserGroup}s whose content is stored in the same DB collection as the
     * {@link UserGroup} objects, but in addition their ID is kept in a separate table, thus marking them as tenants.
     * 
     * @return those {@link UserGroup}s that are not {@link Tenant}s in the first component of the pair; all
     *         {@link Tenant}s found in the second component
     */
    Pair<Iterable<UserGroup>, Iterable<Tenant>> loadAllUserGroupsAndTenantsWithProxyUsers();
    
    /**
     * @param defaultTenantForRoleMigration
     *            when a string-based role is found on the user object it will be mapped to a {@link Role} object
     *            pointing to an equal-named {@link RoleDefinition} from the {@code roleDefinitionsById} map, with a
     *            {@link Role#getQualifiedForTenant() tenant qualification} as defined by this parameter; if this
     *            parameter is {@code null}, role migration will throw an exception.
     * @return the user objects returned have dummy objects for their {@link SecurityUser#getDefaultTenant() default
     *         tenant} and for their {@link SecurityUser#getRoles() roles} attribute which need to be replaced by the
     *         caller once the {@link Tenant} objects and all user objects have been loaded from the DB. The only field
     *         that is set correctly in those dummy {@link Tenant} objects is their {@link Tenant#getId() ID} field.
     *         The {@link Role} objects returned from the {@link SecurityUser#getRoles()} method can be expected to have
     *         valid {@link Role#getRoleDefinition() role definitions} attached; for the {@link Role#getQualifiedForTenant()}
     *         and {@link Role#getQualifiedForUser()} fields callers can only expect valid IDs to be set; those objects need
     *         to be resolved against the full set of tenants and users loaded at a later point in time.
     */
    Iterable<User> loadAllUsers(Map<UUID, RoleDefinition> roleDefinitionsById, Tenant defaultTenantForRoleMigration,
            Map<UUID, Tenant> tenants) throws UserManagementException;
    
    Map<String, Object> loadSettings();
    
    Map<String, Class<?>> loadSettingTypes();
    
    Map<String, Map<String, String>> loadPreferences();


}
