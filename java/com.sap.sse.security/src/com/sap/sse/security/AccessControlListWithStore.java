package com.sap.sse.security;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AccessControlListWithStore implements AccessControlList {
    private static final long serialVersionUID = -5709064967680495227L;
    
    private final String id;
    
    private final UserStore userStore;
    
    /**
     * Maps from UserGroup name to its permissions
     */
    private final Map<String, Set<String>> permissionMap;
    
    public AccessControlListWithStore(String id, Map<String, Set<String>> permissionMap, UserStore userStore) {
        this.id = id;
        this.permissionMap = permissionMap;
        this.userStore = userStore;
    }
    
    public AccessControlListWithStore(String id, UserStore userStore) {
        this(id, new HashMap<>(), userStore);
    }
    
    @Override
    public boolean hasPermission(String username, String permission) {
        for (Map.Entry<String, Set<String>> entry : permissionMap.entrySet()) {
            UserGroup group = userStore.getUserGroupByName(entry.getKey());
            if (group.contains(username) && entry.getValue().contains(permission)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getName() {
        return id;
    }

    @Override
    public Serializable getId() {
        return getName();
    }

    @Override
    public Map<String, Set<String>> getPermissionMap() {
        return permissionMap;
    }
}
