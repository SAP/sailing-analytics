package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.server.gateway.serialization.coursedata.impl.MarkJsonSerializer;
import com.sap.sse.common.impl.AbstractColor;

public class MarksJsonSerializerTest {
    
    private MarkJsonSerializer serializer;
    
    private Mark mark;
    
    @BeforeEach
    public void setUp() {
        serializer = new MarkJsonSerializer();
        
        mark = mock(Mark.class);
    }

    @Test
    public void testName() {
        when(mark.getName()).thenReturn("NAME");
        when(mark.getColor()).thenReturn(AbstractColor.getCssColor("RED"));
        when(mark.getId()).thenReturn("IIIIDDDD");
        when(mark.getPattern()).thenReturn("SHINY");
        when(mark.getShape()).thenReturn("ROUND");
        when(mark.getType()).thenReturn(MarkType.BUOY);
        
        JSONObject result = serializer.serialize(mark);

        assertEquals("NAME", result.get(MarkJsonSerializer.FIELD_NAME));
        assertEquals(AbstractColor.getCssColor("RED").getAsHtml(), result.get(MarkJsonSerializer.FIELD_COLOR));
        assertEquals("IIIIDDDD", result.get(MarkJsonSerializer.FIELD_ID));
        assertEquals("SHINY", result.get(MarkJsonSerializer.FIELD_PATTERN));
        assertEquals("ROUND", result.get(MarkJsonSerializer.FIELD_SHAPE));
        assertEquals(MarkType.BUOY, MarkType.valueOf(result.get(MarkJsonSerializer.FIELD_TYPE).toString()));
    }
}
