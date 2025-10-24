package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.server.gateway.serialization.impl.CourseAreaJsonSerializer;

public class CourseAreaJsonSerializerTest {

    protected final UUID expectedId = UUID.randomUUID();
    protected final String expectedName = "Cruiser";

    protected CourseArea courseArea;
    protected CourseAreaJsonSerializer serializer;

    @BeforeEach
    public void setUp() {
        courseArea = mock(CourseArea.class);
        serializer = new CourseAreaJsonSerializer();

        when(courseArea.getName()).thenReturn(expectedName);
        when(courseArea.getId()).thenReturn(expectedId);
    }

    @Test
    public void testBasicAttributes() {
        JSONObject result = serializer.serialize(courseArea);

        assertEquals(
                expectedName, 
                result.get(CourseAreaJsonSerializer.FIELD_NAME));
        assertEquals(
                expectedId, 
                UUID.fromString(result.get(CourseAreaJsonSerializer.FIELD_ID).toString()));
    }
}
