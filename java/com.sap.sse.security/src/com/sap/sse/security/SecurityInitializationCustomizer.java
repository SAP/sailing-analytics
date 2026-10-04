package com.sap.sse.security;

@FunctionalInterface
public interface SecurityInitializationCustomizer {
    void customizeSecurityService(SecurityService securityService);
}
