package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.orc.RaceLogORCImpliedWindSourceEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogORCImpliedWindSourceEventDeserializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.ImpliedWindSourceSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogORCImpliedWindSourceEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonSerializer;

public class ORCSetImpliedWindSourceEventSerializerTest extends AbstractEventSerializerTest<RaceLogORCImpliedWindSourceEvent> {

    private final long expectedStartTimeTimestamp = 2013;

    @Override
    protected RaceLogORCImpliedWindSourceEvent createMockEvent() {
        TimePoint startTime = mock(TimePoint.class);
        when(startTime.asMillis()).thenReturn(expectedStartTimeTimestamp);
        RaceLogORCImpliedWindSourceEvent event = mock(RaceLogORCImpliedWindSourceEvent.class);
        when(event.getImpliedWindSource()).thenReturn(null);
        return event;
    }

    @Override
    protected JsonSerializer<RaceLogEvent> createSerializer(JsonSerializer<Competitor> competitorSerializer) {
        return new RaceLogORCImpliedWindSourceEventSerializer(competitorSerializer);
    }

    @Test
    public void testNullImpliedWindSourceAttribute() {
        final JSONObject json = serializer.serialize(event);
        assertNull(((JSONObject) json.get(RaceLogORCImpliedWindSourceEventSerializer.ORC_IMPLIED_WIND_SOURCE))
                .get(ImpliedWindSourceSerializer.ORC_IMPLIED_WIND_SOURCE_TYPE));
    }
    
    @Test
    public void testNullImpliedWindSourceDeserialization() throws JsonDeserializationException {
        final JSONObject json = serializer.serialize(event);
        final RaceLogEvent result = new RaceLogORCImpliedWindSourceEventDeserializer(null).deserialize(json);
        assertNull(((RaceLogORCImpliedWindSourceEvent) result).getImpliedWindSource());
    }
}
