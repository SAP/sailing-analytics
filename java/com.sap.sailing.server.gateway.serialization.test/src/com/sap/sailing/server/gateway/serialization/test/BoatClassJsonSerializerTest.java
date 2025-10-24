package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;

public class BoatClassJsonSerializerTest {

    protected BoatClass boatClass;
    protected BoatClassJsonSerializer serializer;

    @BeforeEach
    public void setUp() {
        boatClass = mock(BoatClass.class);
        serializer = new BoatClassJsonSerializer();
    }

    @Test
    public void testName() {
        String expectedName = "Cruiser";
        when(boatClass.getName()).thenReturn(expectedName);

        JSONObject result = serializer.serialize(boatClass);

        assertEquals(
                expectedName, 
                result.get(BoatClassJsonSerializer.FIELD_NAME));
    }

}
