package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogEndOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartOfTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEndOfTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartOfTrackingEventImpl;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogEndOfTrackingEventDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogStartOfTrackingEventDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.NationalityJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PersonJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TeamJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogEndOfTrackingEventSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogStartOfTrackingEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;
import com.sap.sse.shared.json.JsonDeserializer;
import com.sap.sse.shared.json.JsonSerializer;

public class TrackingTimesEventSerializerTest {
    private JsonSerializer<RaceLogEvent> createStartOfTrackingEventSerializer() {
        return new RaceLogStartOfTrackingEventSerializer(createCompetitorSerializer());
    }

    private JsonSerializer<RaceLogEvent> createEndOfTrackingEventSerializer() {
        return new RaceLogEndOfTrackingEventSerializer(createCompetitorSerializer());
    }

    private JsonDeserializer<RaceLogEvent> createStartOfTrackingEventDeserializer() {
        return new RaceLogStartOfTrackingEventDeserializer(createCompetitorDeserializer());
    }

    private JsonDeserializer<DynamicCompetitor> createCompetitorDeserializer() {
        return new CompetitorJsonDeserializer(DomainFactory.INSTANCE.getCompetitorAndBoatStore());
    }

    private JsonDeserializer<RaceLogEvent> createEndOfTrackingEventDeserializer() {
        return new RaceLogEndOfTrackingEventDeserializer(createCompetitorDeserializer());
    }

    private CompetitorJsonSerializer createCompetitorSerializer() {
        return new CompetitorJsonSerializer(new TeamJsonSerializer(
                new PersonJsonSerializer(new NationalityJsonSerializer())), new BoatJsonSerializer(new BoatClassJsonSerializer()), /* serializeNonPublicFields */ false);
    }

    @Test
    public void testNullStartOfTrackingTimeAttributeDeserialization() throws JsonDeserializationException {
        final RaceLogStartOfTrackingEvent event = new RaceLogStartOfTrackingEventImpl(/* startOfTracking */null,
                new LogEventAuthorImpl("Axel", 0), /* passId */1);
        JSONObject json = createStartOfTrackingEventSerializer().serialize(event);
        RaceLogEvent deserialized = createStartOfTrackingEventDeserializer().deserialize(json);
        assertNull(deserialized.getLogicalTimePoint());
    }
    
    @Test
    public void testNullEndOfTrackingTimeAttributeDeserialization() throws JsonDeserializationException {
        final RaceLogEndOfTrackingEvent event = new RaceLogEndOfTrackingEventImpl(/* endOfTracking */null,
                new LogEventAuthorImpl("Axel", 0), /* passId */1);
        JSONObject json = createStartOfTrackingEventSerializer().serialize(event);
        RaceLogEvent deserialized = createEndOfTrackingEventDeserializer().deserialize(json);
        assertNull(deserialized.getLogicalTimePoint());
    }
    
    @Test
    public void testNullStartOfTrackingTimeAttribute() {
        final RaceLogStartOfTrackingEvent event = new RaceLogStartOfTrackingEventImpl(/* startOfTracking */null,
                new LogEventAuthorImpl("Axel", 0), /* passId */1);
        JSONObject json = createStartOfTrackingEventSerializer().serialize(event);
        assertNull(json.get(RaceLogStartOfTrackingEventSerializer.FIELD_TIMESTAMP));
    }

    @Test
    public void testNullEndOfTrackingTimeAttribute() {
        final RaceLogEndOfTrackingEvent event = new RaceLogEndOfTrackingEventImpl(/* endOfTracking */null,
                new LogEventAuthorImpl("Axel", 0), /* passId */1);
        JSONObject json = createEndOfTrackingEventSerializer().serialize(event);
        assertNull(json.get(RaceLogEndOfTrackingEventSerializer.FIELD_TIMESTAMP));
    }

    @Test
    public void testNonNullStartOfTrackingTimeAttribute() {
        final TimePoint now = MillisecondsTimePoint.now();
        final RaceLogStartOfTrackingEvent event = new RaceLogStartOfTrackingEventImpl(/* startOfTracking */now,
                new LogEventAuthorImpl("Axel", 0), /* passId */1);
        JSONObject json = createStartOfTrackingEventSerializer().serialize(event);
        assertEquals(now.asMillis(), json.get(RaceLogStartOfTrackingEventSerializer.FIELD_TIMESTAMP));
    }

    @Test
    public void testNonNullEndOfTrackingTimeAttribute() {
        final TimePoint now = MillisecondsTimePoint.now();
        final RaceLogEndOfTrackingEvent event = new RaceLogEndOfTrackingEventImpl(/* endOfTracking */now,
                new LogEventAuthorImpl("Axel", 0), /* passId */1);
        JSONObject json = createEndOfTrackingEventSerializer().serialize(event);
        assertEquals(now.asMillis(), json.get(RaceLogEndOfTrackingEventSerializer.FIELD_TIMESTAMP));
    }
}
