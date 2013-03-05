package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.simple.JSONObject;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;
import com.sap.sailing.server.gateway.serialization.JsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogFlagEventSerializer;

public class FlagEventSerializerTest extends AbstractEventSerializerTest<RaceLogFlagEvent> {


    private Flags expectedUpperFlag = Flags.FOXTROTT;
    private Flags expectedLowerFlag = Flags.FOXTROTT;
    private boolean expectedDisplayed = true;

    @Override
    protected RaceLogFlagEvent createMockEvent() {
        RaceLogFlagEvent event = mock(RaceLogFlagEvent.class);

        when(event.getUpperFlag()).thenReturn(expectedUpperFlag);
        when(event.getLowerFlag()).thenReturn(expectedLowerFlag);
        when(event.isDisplayed()).thenReturn(expectedDisplayed);

        return event;
    }
    
    @Override
    protected JsonSerializer<RaceLogEvent> createSerializer(JsonSerializer<Competitor> competitorSerializer) {
        return new RaceLogFlagEventSerializer(competitorSerializer);
    }

    @Test
    public void testFlagStatusAttributes() {
        JSONObject json = serializer.serialize(event);

        assertEquals(
                expectedUpperFlag,
                Flags.valueOf(json.get(RaceLogFlagEventSerializer.FIELD_UPPER_FLAG).toString()));
        assertEquals(
                expectedLowerFlag,
                Flags.valueOf(json.get(RaceLogFlagEventSerializer.FIELD_LOWER_FLAG).toString()));
        assertEquals(
                expectedDisplayed,
                json.get(RaceLogFlagEventSerializer.FIELD_DISPLAYED));
    }

    @Test
    public void testClassAttribute() {
        JSONObject json = serializer.serialize(event);

        assertEquals(
                RaceLogFlagEventSerializer.VALUE_CLASS,
                json.get(RaceLogFlagEventSerializer.FIELD_CLASS));
    }

}
