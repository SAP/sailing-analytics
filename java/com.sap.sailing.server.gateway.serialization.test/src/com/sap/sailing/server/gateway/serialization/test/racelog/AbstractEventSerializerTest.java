package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.server.gateway.serialization.racelog.impl.BaseRaceLogEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.shared.json.JsonSerializer;

public abstract class AbstractEventSerializerTest<EventType extends RaceLogEvent> {

    protected final UUID expectedId = UUID.randomUUID();
    protected final long expectedCreatedAtTimestamp = 42l;
    protected final long expectedLogicalTimestamp = 1337l;
    protected final int expectedPassId = 42;

    private JsonSerializer<Competitor> competitorSerializer;
    protected JsonSerializer<RaceLogEvent> serializer;
    protected EventType event;
    protected final AbstractLogEventAuthor author = new LogEventAuthorImpl("Test author", 1);

    protected abstract EventType createMockEvent();

    protected abstract JsonSerializer<RaceLogEvent> createSerializer(JsonSerializer<Competitor> competitorSerializer);

    /**
     * Be sure to call this from subclasses as last statement!
     */
    @BeforeEach
    public void setUp() {
        serializer = createSerializer(competitorSerializer);
        event = createMockEvent();

        TimePoint createdAtTimePoint = mock(TimePoint.class);
        when(createdAtTimePoint.asMillis()).thenReturn(expectedCreatedAtTimestamp);
        TimePoint logicalTimePoint = mock(TimePoint.class);
        when(logicalTimePoint.asMillis()).thenReturn(expectedLogicalTimestamp);

        when(event.getId()).thenReturn(expectedId);
        when(event.getCreatedAt()).thenReturn(createdAtTimePoint);
        when(event.getLogicalTimePoint()).thenReturn(logicalTimePoint);
        when(event.getPassId()).thenReturn(expectedPassId);
        final List<Competitor> emptyList = Collections.<Competitor> emptyList();
        when(event.getInvolvedCompetitors()).thenReturn(emptyList);
        when(event.getAuthor()).thenReturn(author);
    }

    @Test
    public void testSerializeBaseAttributes() {
        JSONObject json = serializer.serialize(event);

        assertEquals(expectedId, UUID.fromString(json.get(BaseRaceLogEventSerializer.FIELD_ID).toString()));
        assertEquals(expectedCreatedAtTimestamp, json.get(BaseRaceLogEventSerializer.FIELD_CREATED_AT));
        assertEquals(expectedLogicalTimestamp, json.get(BaseRaceLogEventSerializer.FIELD_TIMESTAMP));
        assertEquals(expectedPassId, json.get(BaseRaceLogEventSerializer.FIELD_PASS_ID));
    }

    @Test
    public void testIsParseable() throws ParseException {
        JSONObject json = serializer.serialize(event);
        String value = json.toJSONString();
        JSONValue.parseWithException(value);
    }

}
