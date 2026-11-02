package com.sap.sailing.server.gateway.test.jaxrs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.UUID;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.server.gateway.deserialization.impl.BoatJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.Helpers;
import com.sap.sse.common.impl.RGBColor;
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

    @Test
    public void testCreateBoat() throws Exception {
        final String newBoatId = UUID.randomUUID().toString();
        final Response response = boatsResource.createBoat("Black", "1", "#000000", "J/70", newBoatId);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        final String jsonString = StreamingOutputUtil.getEntityAsString(response.getEntity());
        final DynamicBoat boat = BoatJsonDeserializer.create(racingEventService.getBaseDomainFactory())
                .deserialize(Helpers.toJSONObjectSafe(JSONValue.parse(jsonString)));
        assertEquals(newBoatId, boat.getId().toString());
        assertEquals("Black", boat.getName());
        assertEquals("1", boat.getSailID());
        assertEquals("J/70", boat.getBoatClass().getName());
        assertEquals(new RGBColor("#000000").getAsHtml(), boat.getColor().getAsHtml());
        final DynamicBoat storedBoat = racingEventService.getCompetitorAndBoatStore().getExistingBoatByIdAsString(newBoatId);
        assertNotNull(storedBoat);
        assertEquals("Black", storedBoat.getName());
        assertEquals(new RGBColor("#000000").getAsHtml(), storedBoat.getColor().getAsHtml());
    }

    @Test
    public void testCreateBoatWithoutIdUsesSailIdAsDefaultName() throws Exception {
        final Response response = boatsResource.createBoat(null, "GER 7", null, boatClassName, null);
        assertEquals(Status.OK.getStatusCode(), response.getStatus());
        final JSONObject jsonObject = Helpers.toJSONObjectSafe(JSONValue.parse(StreamingOutputUtil.getEntityAsString(response.getEntity())));
        final DynamicBoat boat = BoatJsonDeserializer.create(racingEventService.getBaseDomainFactory()).deserialize(jsonObject);
        assertEquals("GER 7", boat.getName());
        assertNotNull(racingEventService.getCompetitorAndBoatStore().getExistingBoatByIdAsString(boat.getId().toString()));
    }

    @Test
    public void testCreateBoatWithExistingIdIsAConflict() throws Exception {
        final Response response = boatsResource.createBoat("Other boat", "GER 2", null, boatClassName, id);
        assertEquals(Status.CONFLICT.getStatusCode(), response.getStatus());
        assertEquals(boatName, racingEventService.getCompetitorAndBoatStore().getExistingBoatByIdAsString(id).getName());
    }

    @Test
    public void testCreateBoatRejectsMissingBoatClassAndInvalidColor() throws Exception {
        assertEquals(Status.BAD_REQUEST.getStatusCode(), boatsResource.createBoat("Red", "4", "#ff0000", null, null).getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), boatsResource.createBoat("Red", "4", "red", boatClassName, null).getStatus());
        assertEquals(Status.BAD_REQUEST.getStatusCode(), boatsResource.createBoat(null, null, null, boatClassName, null).getStatus());
    }
}
