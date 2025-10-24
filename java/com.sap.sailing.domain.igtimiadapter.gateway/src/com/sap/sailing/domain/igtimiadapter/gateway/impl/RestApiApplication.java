package com.sap.sailing.domain.igtimiadapter.gateway.impl;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.sap.sse.security.jaxrs.ShiroAuthorizationExceptionTo401ResponseMapper;

public class RestApiApplication extends Application {
    public static final String V1 = "/v1";
    public static final String API = "/api";
    public static final String WEB_SOCKET_PATH = "/websocket";

    public RestApiApplication() {
    }

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(new Class<?>[] {
            RiotDevicesResource.class,
            RiotResourcesResource.class,
            RiotDataAccessWindowsResource.class,
            RiotServerListersResource.class,
            RiotServerResource.class,
            ShiroAuthorizationExceptionTo401ResponseMapper.class }));
    }
}
