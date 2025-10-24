package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.server.gateway.deserialization.impl.BoatJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sse.rest.StreamingOutputUtil;

public class BoatResourceTest extends AbstractJaxRsApiTest {
    private final String id = "af855a56-9726-4a9c-a77e-da955bd289be";
    private final String boatName = "My boat";
    private final String boatClassName = "49er";
    private final String sailId = "GER 1";
    private final BoatClass boatClass = new BoatClassImpl(boatClassName, true);

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        racingEventService.getBaseDomainFactory().getOrCreateBoat(id, boatName, boatClass, sailId, null, /* storePersistently */ true);
    }

    @Test
    public void testGetBoatAsJson() throws Exception {
        String jsonString = StreamingOutputUtil.getEntityAsString(boatsResource.getBoat(id, null, null).getEntity());
        BoatJsonDeserializer boatJsonDeserializer = BoatJsonDeserializer.create(racingEventService.getBaseDomainFactory());
        JSONObject jsonObject = Helpers.toJSONObjectSafe(JSONValue.parse(jsonString));
        DynamicBoat boat = boatJsonDeserializer.deserialize(jsonObject);
        assertTrue(boat.getId().equals(id));
        assertTrue(boat.getName().equals(boatName));
        assertTrue(boat.getSailID().equals(sailId));
        assertTrue(boat.getSailID().equals(sailId));
        assertTrue(boat.getBoatClass().getName().equals(boatClassName));
    }

}
