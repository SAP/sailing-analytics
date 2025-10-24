package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogWindFixEventImpl;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.PositionJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.WindJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogWindFixEventDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.NationalityJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PersonJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PositionJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TeamJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.WindJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogWindFixEventSerializer;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;

public class RaceLogWindFixEventSerializerTest {

    private RaceLogWindFixEventSerializer serializer;
    private RaceLogWindFixEventDeserializer deserializer;
    private RaceLogWindFixEvent event;
    private TimePoint now;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    
    @BeforeEach
    public void setUp() {
        SharedDomainFactory<?> factory = DomainFactory.INSTANCE;
        serializer = new RaceLogWindFixEventSerializer(new CompetitorJsonSerializer(new TeamJsonSerializer(
                new PersonJsonSerializer(new NationalityJsonSerializer())), new BoatJsonSerializer(new BoatClassJsonSerializer()), /* serializeNonPublicFields */ false), new WindJsonSerializer(
                new PositionJsonSerializer()));
        deserializer = new RaceLogWindFixEventDeserializer(new CompetitorJsonDeserializer(factory.getCompetitorAndBoatStore(), /* team deserializer */ null, /* boat deserializer */ null),
                new WindJsonDeserializer(new PositionJsonDeserializer()));
        now = MillisecondsTimePoint.now();
        event = new RaceLogWindFixEventImpl(now, author, 0, createWindFix(), /* isMagnetic */ false);
    }

    @Test
    public void testSerializeAndDeserializeRaceLogWindFixEvent() throws JsonDeserializationException {
        JSONObject jsonWindFixEvent = serializer.serialize(event);
        RaceLogWindFixEvent deserializedEvent = (RaceLogWindFixEvent) deserializer.deserialize(jsonWindFixEvent);
        assertEquals(event.getId(), deserializedEvent.getId());
        assertEquals(event.getPassId(), deserializedEvent.getPassId());
        assertEquals(event.getLogicalTimePoint(), deserializedEvent.getLogicalTimePoint());
        assertEquals(0, Util.size(event.getInvolvedCompetitors()));
        assertEquals(0, Util.size(deserializedEvent.getInvolvedCompetitors()));
        compareWind(event.getWindFix(), deserializedEvent.getWindFix());
    }

    private void compareWind(Wind serializedWindFix, Wind deserializedWindFix) {
        assertEquals(serializedWindFix.getTimePoint(), deserializedWindFix.getTimePoint());
        assertNotNull(serializedWindFix.getPosition());
        assertNotNull(deserializedWindFix.getPosition());
        assertEquals(serializedWindFix.getPosition().getLatDeg(), deserializedWindFix.getPosition().getLatDeg(), 0);
        assertEquals(serializedWindFix.getPosition().getLngDeg(), deserializedWindFix.getPosition().getLngDeg(), 0);
        assertEquals(serializedWindFix.getKnots(), deserializedWindFix.getKnots(), 0);
        assertNotNull(serializedWindFix.getBearing());
        assertNotNull(deserializedWindFix.getBearing());
        assertEquals(serializedWindFix.getBearing().getDegrees(), deserializedWindFix.getBearing().getDegrees(), 0);
    }

    private Wind createWindFix() {
        Position position = new DegreePosition(23.0313, 2.2344);
        Bearing bearing = new DegreeBearingImpl(25.5);
        SpeedWithBearing speedBearing = new KnotSpeedWithBearingImpl(10.4, bearing);
        return new WindImpl(position, now, speedBearing);
    }
}
