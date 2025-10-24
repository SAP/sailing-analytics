package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogStartTimeEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.shared.json.JsonSerializer;

public class StartTimeEventSerializerTest extends RaceStatusEventSerializerTest {

    private final long expectedStartTimeTimestamp = 2013;

    @Override
    protected RaceLogStartTimeEvent createMockEvent() {
        TimePoint startTime = mock(TimePoint.class);
        when(startTime.asMillis()).thenReturn(expectedStartTimeTimestamp);

        RaceLogStartTimeEvent event = mock(RaceLogStartTimeEvent.class);
        when(event.getStartTime()).thenReturn(startTime);
        setupMockEvent(event);
        
        return event;
    }

    @Override
    protected JsonSerializer<RaceLogEvent> createSerializer(JsonSerializer<Competitor> competitorSerializer) {
        return new RaceLogStartTimeEventSerializer(competitorSerializer);
    }

    @Test
    public void testStartTimeAttribute() {
        JSONObject json = serializer.serialize(event);

        assertEquals(
                expectedStartTimeTimestamp,
                json.get(RaceLogStartTimeEventSerializer.FIELD_START_TIME));
    }

    @Test
    public void testClassAttribute() {
        JSONObject json = serializer.serialize(event);

        assertEquals(
                RaceLogStartTimeEventSerializer.VALUE_CLASS,
                json.get(RaceLogStartTimeEventSerializer.FIELD_CLASS));
    }

}
