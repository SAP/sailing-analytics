package com.sap.sailing.server.gateway.serialization.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Team;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.TransientCompetitorStoreImpl;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorJsonDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;

public class CompetitorJsonSerializerTest {

    private Competitor competitor;
    private CompetitorJsonSerializer serializer;
    private CompetitorJsonDeserializer deserializer;

    @Before
    public void setUp() {
        competitor = mock(Competitor.class);
        mockTeam(competitor);
        serializer = CompetitorJsonSerializer.create();
        // use a separate competitor store for the de-serializer
        deserializer = CompetitorJsonDeserializer.create(new DomainFactoryImpl(new TransientCompetitorStoreImpl(), /* raceLogResolver */ null));
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
        final Boat boat = mock(Boat.class);
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
