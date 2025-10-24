package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Team;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.DynamicBoat;
import com.sap.sailing.domain.base.impl.DynamicCompetitor;
import com.sap.sailing.domain.base.impl.TransientCompetitorAndBoatStoreImpl;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorAndBoatJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorAndBoatJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.shared.json.JsonDeserializationException;

public class CompetitorJsonSerializerTest {

    private Competitor competitor;
    private CompetitorJsonSerializer serializer;
    private CompetitorJsonDeserializer deserializer;

    @BeforeEach
    public void setUp() {
        competitor = mock(Competitor.class);
        mockTeam(competitor);
        serializer = CompetitorJsonSerializer.create();
        // use a separate competitor store for the de-serializer
        deserializer = CompetitorJsonDeserializer.create(new DomainFactoryImpl(new TransientCompetitorAndBoatStoreImpl(), /* raceLogResolver */ null));
    }

    private void mockTeam(final Competitor competitor) {
        final Team team = mock(Team.class);
        when(team.getSailors()).thenReturn(Collections.emptySet());
        when(team.getName()).thenReturn("The Team");
        when(competitor.getTeam()).thenReturn(team);
    }
    
    /**
     * This test case fails based on bug 1599.
     */
    @Test
    public void testIntegerId() throws JsonDeserializationException, ParseException {
        Integer expectedId = Integer.valueOf(123);
        when(competitor.getId()).thenReturn(expectedId);
        JSONObject result = serializer.serialize(competitor);
        assertEquals(
                expectedId, 
                result.get(CompetitorJsonSerializer.FIELD_ID));
        Competitor deserializedCompetitor = deserializer.deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertEquals(expectedId, deserializedCompetitor.getId());
    }

    @Test
    public void testStringId() throws JsonDeserializationException, ParseException {
        String expectedId = "123";
        when(competitor.getId()).thenReturn(expectedId);
        JSONObject result = serializer.serialize(competitor);
        assertEquals(
                expectedId, 
                result.get(CompetitorJsonSerializer.FIELD_ID));
        Competitor deserializedCompetitor = deserializer.deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertEquals(expectedId, deserializedCompetitor.getId());
    }

    @Test
    public void testCompetitorWithBoat() throws JsonDeserializationException, ParseException {
        final CompetitorWithBoat competitorWithBoat = mock(CompetitorWithBoat.class);
        mockTeam(competitorWithBoat);
        String expectedId = "123";
        when(competitorWithBoat.getId()).thenReturn(expectedId);
        final DynamicBoat boat = mock(DynamicBoat.class);
        when(competitorWithBoat.hasBoat()).thenReturn(true);
        when(competitorWithBoat.getBoat()).thenReturn(boat);
        when(boat.getId()).thenReturn(UUID.randomUUID());
        when(boat.getSailID()).thenReturn("1233");
        when(boat.getBoatClass()).thenReturn(DomainFactory.INSTANCE.getOrCreateBoatClass("Tornado"));
        JSONObject result = serializer.serialize(competitorWithBoat);
        final Competitor deserializedCompetitor = deserializer.deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertTrue(deserializedCompetitor.hasBoat());
        assertTrue(deserializedCompetitor instanceof CompetitorWithBoat);
        final CompetitorWithBoat deserializedCompetitorWithBoat = (CompetitorWithBoat) deserializedCompetitor;
        assertEquals(boat.getId(), deserializedCompetitorWithBoat.getBoat().getId());
        assertEquals(boat.getSailID(), deserializedCompetitorWithBoat.getBoat().getSailID());
    }

    @Test
    public void testCompetitorAndBoatWithBoat() throws JsonDeserializationException, ParseException {
        final CompetitorWithBoat competitorWithBoat = mock(CompetitorWithBoat.class);
        mockTeam(competitorWithBoat);
        String expectedId = "123";
        when(competitorWithBoat.getId()).thenReturn(expectedId);
        final DynamicBoat boat = mock(DynamicBoat.class);
        when(competitorWithBoat.hasBoat()).thenReturn(true);
        when(competitorWithBoat.getBoat()).thenReturn(boat);
        when(boat.getId()).thenReturn(UUID.randomUUID());
        when(boat.getSailID()).thenReturn("1233");
        when(boat.getBoatClass()).thenReturn(DomainFactory.INSTANCE.getOrCreateBoatClass("Tornado"));
        JSONObject result = CompetitorAndBoatJsonSerializer.create(/* serializeNonPublicCompetitorFields */ false).serialize(new Pair<>(competitorWithBoat, boat));
        final Pair<DynamicCompetitor, Boat> deserializedCompetitorAndBoat = CompetitorAndBoatJsonDeserializer
                .create(new DomainFactoryImpl(new TransientCompetitorAndBoatStoreImpl(), /* raceLogResolver */ null))
                .deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertTrue(deserializedCompetitorAndBoat.getA().hasBoat());
        assertTrue(deserializedCompetitorAndBoat.getA() instanceof CompetitorWithBoat);
        final CompetitorWithBoat deserializedCompetitorWithBoat = (CompetitorWithBoat) deserializedCompetitorAndBoat.getA();
        assertSame(deserializedCompetitorWithBoat.getBoat(), deserializedCompetitorAndBoat.getB());
        assertEquals(boat.getId(), deserializedCompetitorWithBoat.getBoat().getId());
        assertEquals(boat.getSailID(), deserializedCompetitorWithBoat.getBoat().getSailID());
    }

    @Test
    public void testCompetitorAndBoatWithoutBoat() throws JsonDeserializationException, ParseException {
        String expectedId = "1234";
        when(competitor.getId()).thenReturn(expectedId);
        when(competitor.hasBoat()).thenReturn(false);
        final Boat boat = mock(Boat.class);
        when(boat.getId()).thenReturn(UUID.randomUUID());
        when(boat.getSailID()).thenReturn("12334");
        when(boat.getBoatClass()).thenReturn(DomainFactory.INSTANCE.getOrCreateBoatClass("Tornado"));
        JSONObject result = CompetitorAndBoatJsonSerializer.create(/* serializeNonPublicCompetitorFields */ false).serialize(new Pair<>(competitor, boat));
        final Pair<DynamicCompetitor, Boat> deserializedCompetitorAndBoat = CompetitorAndBoatJsonDeserializer
                .create(new DomainFactoryImpl(new TransientCompetitorAndBoatStoreImpl(), /* raceLogResolver */ null))
                .deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertFalse(deserializedCompetitorAndBoat.getA().hasBoat());
        assertFalse(deserializedCompetitorAndBoat.getA() instanceof CompetitorWithBoat);
        assertEquals("12334", deserializedCompetitorAndBoat.getB().getSailID());
    }

    @Test
    public void testCompetitorAndBoatWithNullBoat() throws JsonDeserializationException, ParseException {
        final CompetitorWithBoat competitorWithBoat = mock(CompetitorWithBoat.class);
        mockTeam(competitorWithBoat);
        String expectedId = "1234";
        when(competitorWithBoat.getId()).thenReturn(expectedId);
        when(competitorWithBoat.hasBoat()).thenReturn(false);
        when(competitorWithBoat.getBoat()).thenReturn(null);
        final Boat boat = mock(Boat.class);
        when(boat.getId()).thenReturn(UUID.randomUUID());
        when(boat.getSailID()).thenReturn("1233");
        when(boat.getBoatClass()).thenReturn(DomainFactory.INSTANCE.getOrCreateBoatClass("Tornado"));
        JSONObject result = CompetitorAndBoatJsonSerializer.create(/* serializeNonPublicCompetitorFields */ false).serialize(new Pair<>(competitorWithBoat, boat));
        final Pair<DynamicCompetitor, Boat> deserializedCompetitorAndBoat = CompetitorAndBoatJsonDeserializer
                .create(new DomainFactoryImpl(new TransientCompetitorAndBoatStoreImpl(), /* raceLogResolver */ null))
                .deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertFalse(deserializedCompetitorAndBoat.getA().hasBoat());
        assertFalse(deserializedCompetitorAndBoat.getA() instanceof CompetitorWithBoat);
        assertEquals("1233", deserializedCompetitorAndBoat.getB().getSailID());
    }

    @Test
    public void testCompetitorWithNullBoat() throws JsonDeserializationException, ParseException {
        final CompetitorWithBoat competitorWithBoat = mock(CompetitorWithBoat.class);
        mockTeam(competitorWithBoat);
        String expectedId = "1234";
        when(competitorWithBoat.getId()).thenReturn(expectedId);
        when(competitorWithBoat.hasBoat()).thenReturn(false);
        when(competitorWithBoat.getBoat()).thenReturn(null);
        JSONObject result = serializer.serialize(competitorWithBoat);
        Competitor deserializedCompetitor = deserializer.deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertFalse(deserializedCompetitor.hasBoat());
        assertFalse(deserializedCompetitor instanceof CompetitorWithBoat);
    }

    @Test
    public void testUUIDId() throws JsonDeserializationException, ParseException {
        UUID expectedId = UUID.randomUUID();
        when(competitor.getId()).thenReturn(expectedId);

        JSONObject result = serializer.serialize(competitor);
        Competitor deserializedCompetitor = deserializer.deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertEquals(expectedId, deserializedCompetitor.getId());
    }

    @Test
    public void testLongId() throws JsonDeserializationException, ParseException {
        Long expectedId = Long.valueOf(123);
        when(competitor.getId()).thenReturn(expectedId);

        JSONObject result = serializer.serialize(competitor);
        assertEquals(
                expectedId, 
                result.get(CompetitorJsonSerializer.FIELD_ID));
        Competitor deserializedCompetitor = deserializer.deserialize((JSONObject) new JSONParser().parse(result.toString()));
        assertEquals(expectedId, deserializedCompetitor.getId());
    }
}
