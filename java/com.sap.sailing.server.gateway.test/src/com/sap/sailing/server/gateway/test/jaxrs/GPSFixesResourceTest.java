package com.sap.sailing.server.gateway.test.jaxrs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.UUID;

import javax.ws.rs.core.Response;

import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.common.racelog.tracking.TransformationException;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneUuidServiceFinderFactory;
import com.sap.sailing.domain.racelogtracking.DeviceIdentifier;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneUUIDIdentifierImpl;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.gateway.jaxrs.api.GPSFixesResource;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sse.common.NoCorrespondingServiceRegisteredException;

public class GPSFixesResourceTest {
    public static final String FIXES_JSON = "{\n" + 
            "  \"deviceUuid\" : \"af855a56-9726-4a9c-a77e-da955bd289bf\",\n" + 
            "  \"fixes\" : [\n" + 
            "    {\n" + 
            "      \"timestamp\" : 14144160080000,\n" + 
            "      \"latitude\" : 54.325246,\n" + 
            "      \"longitude\" : 10.148556,\n" + 
            "      \"speed\" : 3.61,\n" + 
            "      \"course\" : 258.11,\n" + 
            "    },\n" + 
            "    {\n" + 
            "      \"timestamp\" : 14144168490000,\n" + 
            "      \"latitude\" : 55.12456,\n" + 
            "      \"longitude\" : 8.03456,\n" + 
            "      \"speed\" : 5.1,\n" + 
            "      \"course\" : 14.2,\n" + 
            "    }\n" + 
            "  ]\n" + 
            "}";
    
    private RacingEventService service;
    
    @Before
    public void setup() {
        service = new RacingEventServiceImpl(/* clearPersistentCompetitorStore */ true,
                new MockSmartphoneUuidServiceFinderFactory(), /* restoreTrackedRaces */ false,
                /* anniversaryCalculator */ null);
        service.getMongoObjectFactory().getDatabase().dropDatabase();
    }
    
    @Test
    public void deserialize() throws ParseException, TransformationException, NoCorrespondingServiceRegisteredException {

        GPSFixesResource resource = new GPSFixesResource() {
            public RacingEventService getService() {
                return service;
            };
        };
        Response response = resource.postFixes(FIXES_JSON);
        assertThat("response is ok", response.getStatus(), equalTo(Response.Status.OK.getStatusCode()));
        
        DeviceIdentifier device = new SmartphoneUUIDIdentifierImpl(UUID.fromString("af855a56-9726-4a9c-a77e-da955bd289bf"));
        assertThat("all fixes stored", service.getSensorFixStore().getNumberOfFixes(device), equalTo(2L));
    }
}
