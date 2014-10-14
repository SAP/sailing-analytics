package com.sap.sse.security;

public enum DefaultRoles {
    ADMIN("admin");
    
    private DefaultRoles(String rolename) {
        this.rolename = rolename;
    }
    
    public String getRolename() {
        return rolename;
    }
    
    private final String rolename;
}
