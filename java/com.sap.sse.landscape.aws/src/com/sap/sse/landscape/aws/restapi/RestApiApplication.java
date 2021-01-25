package com.sap.sse.landscape.aws.restapi;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.sap.sse.security.jaxrs.ShiroAuthorizationExceptionTo401ResponseMapper;

public class RestApiApplication extends Application {
        
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();
        // RESTlets
        classes.add(LandscapeResource.class);
        
        // Exception Mapper
        classes.add(ShiroAuthorizationExceptionTo401ResponseMapper.class);
        return classes;
    }
}
