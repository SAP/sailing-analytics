package com.sap.sailing.server.gateway.jaxrs.api;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import com.sap.sailing.server.gateway.jaxrs.exceptions.ExceptionManager;
import com.sap.sse.security.jaxrs.ShiroAuthorizationExceptionTo401ResponseMapper;

public class RestApiApplication extends Application {
    public Set<Class<?>> getClasses() {
        HashSet<Class<?>> classes = new HashSet<>();
        // RESTlets
        classes.add(LeaderboardGroupsResource.class);
        classes.add(EventsResource.class);
        classes.add(RegattasResource.class);
        classes.add(BoatsResource.class);
        classes.add(BoatClassesResource.class);
        classes.add(CountryCodesResource.class);
        classes.add(LeaderboardsResource.class);
        classes.add(LeaderboardsResourceV2.class);
        classes.add(PolarResource.class);
        classes.add(SearchResource.class);
        classes.add(GPSFixesResource.class);
        classes.add(CompetitorsResource.class);
        classes.add(FileStorageResource.class);
        classes.add(DataMiningResource.class);
        classes.add(PingResource.class);
        classes.add(TrackedRaceListResource.class);
        classes.add(StatisticsResource.class);
        classes.add(WindResource.class);
        classes.add(MarkRessource.class);
        classes.add(PreferencesResource.class);
        classes.add(TagsResource.class);
        classes.add(StatusResource.class);
        
        // Exception Mappers
        classes.add(ShiroAuthorizationExceptionTo401ResponseMapper.class);
        classes.add(ExceptionManager.class);
        return classes;
    }
}
