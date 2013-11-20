package com.sap.sailing.domain.igtimiadapter.oauth;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

public class RestApiApplication extends Application {
    public RestApiApplication() {
    }

    @Override
    public Set<Class<?>> getClasses() {
        return new HashSet<Class<?>>(Arrays.asList(new Class<?>[] { Callback.class }));
    }
}
