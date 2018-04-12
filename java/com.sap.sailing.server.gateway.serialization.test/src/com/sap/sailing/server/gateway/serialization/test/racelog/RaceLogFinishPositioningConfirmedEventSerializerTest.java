package com.sap.sailing.server.gateway.serialization.test.racelog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Iterator;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningConfirmedEventImpl;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.SharedDomainFactory;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.server.gateway.deserialization.JsonDeserializationException;
import com.sap.sailing.server.gateway.deserialization.impl.CompetitorJsonDeserializer;
import com.sap.sailing.server.gateway.deserialization.racelog.impl.RaceLogFinishPositioningConfirmedEventDeserializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatClassJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.BoatJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.CompetitorJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.NationalityJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.PersonJsonSerializer;
import com.sap.sailing.server.gateway.serialization.impl.TeamJsonSerializer;
import com.sap.sailing.server.gateway.serialization.racelog.impl.RaceLogFinishPositioningConfirmedEventSerializer;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceLogFinishPositioningConfirmedEventSerializerTest {

    private RaceLogFinishPositioningConfirmedEventSerializer serializer;
    private RaceLogFinishPositioningConfirmedEventDeserializer deserializer;
    private RaceLogFinishPositioningConfirmedEvent event;
    private TimePoint now;
    private CompetitorResults positioningList;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    
    @Before
    public void setUp() {
        SharedDomainFactory factory = DomainFactory.INSTANCE;
        serializer = new RaceLogFinishPositioningConfirmedEventSerializer(new CompetitorJsonSerializer(
                new TeamJsonSerializer(new PersonJsonSerializer(new NationalityJsonSerializer())), new BoatJsonSerializer(new BoatClassJsonSerializer())));
        deserializer = new RaceLogFinishPositioningConfirmedEventDeserializer(new CompetitorJsonDeserializer(factory.getCompetitorStore(), /* team deserializer */ null, /* boat deserializer */ null));
        now = MillisecondsTimePoint.now();
        positioningList = new CompetitorResultsImpl();
    }

    public static void assertCompetitorResultsEqual(final CompetitorResults expectedCompetitorResults, final CompetitorResults loadedCompetitorResults) {
        assertEquals(Util.size(expectedCompetitorResults), Util.size(loadedCompetitorResults));
        final Iterator<CompetitorResult> iExpected = expectedCompetitorResults.iterator();
        final Iterator<CompetitorResult> iLoaded = loadedCompetitorResults.iterator();
        while (iExpected.hasNext()) {
            assertEquals(iExpected.next(), iLoaded.next());
        }
    }

    @Test
    public void testSerializeAndDeserializeRaceLogFinishPositioningConfirmedEvent() throws JsonDeserializationException {
        positioningList.add(new CompetitorResultImpl(UUID.randomUUID(), "SAP Extreme", /* rank */ 1,
                MaxPointsReason.NONE, /* score */ null, /* finishingTime */ null, /* comment */ null, MergeState.OK));
        event = new RaceLogFinishPositioningConfirmedEventImpl(now, author, 0, positioningList);
        JSONObject jsonConfirmationEvent = serializer.serialize(event);
        RaceLogFinishPositioningConfirmedEvent deserializedEvent = (RaceLogFinishPositioningConfirmedEvent) deserializer.deserialize(jsonConfirmationEvent);
        assertEquals(event.getId(), deserializedEvent.getId());
        assertEquals(event.getPassId(), deserializedEvent.getPassId());
        assertEquals(event.getLogicalTimePoint(), deserializedEvent.getLogicalTimePoint());
        assertEquals(0, Util.size(event.getInvolvedCompetitors()));
        assertEquals(0, Util.size(deserializedEvent.getInvolvedCompetitors()));
        assertNotNull(event.getPositionedCompetitorsIDsNamesMaxPointsReasons());
        assertNotNull(deserializedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons());
        assertEquals(1, Util.size(event.getPositionedCompetitorsIDsNamesMaxPointsReasons()));
        assertCompetitorResultsEqual(event.getPositionedCompetitorsIDsNamesMaxPointsReasons(), deserializedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons());
    }

    @Test
    public void testSerializeAndDeserializeRaceLogFinishPositioningConfirmedEventWithoutPositioningBackwardsCompatible()
            throws JsonDeserializationException {
        event = new RaceLogFinishPositioningConfirmedEventImpl(now, author, 0, null);
        JSONObject jsonConfirmationEvent = serializer.serialize(event);
        RaceLogFinishPositioningConfirmedEvent deserializedEvent = (RaceLogFinishPositioningConfirmedEvent) deserializer.deserialize(jsonConfirmationEvent);
        assertEquals(event.getId(), deserializedEvent.getId());
        assertEquals(event.getPassId(), deserializedEvent.getPassId());
        assertEquals(event.getLogicalTimePoint(), deserializedEvent.getLogicalTimePoint());
        assertEquals(0, Util.size(event.getInvolvedCompetitors()));
        assertEquals(0, Util.size(deserializedEvent.getInvolvedCompetitors()));
        assertNull(event.getPositionedCompetitorsIDsNamesMaxPointsReasons());
        assertNotNull(deserializedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons());
        assertTrue(Util.isEmpty(deserializedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons()));
    }

    @Test
    public void testSerializeAndDeserializeRaceLogFinishPositioningConfirmedEventWithEmptyPositioning()
            throws JsonDeserializationException {
        event = new RaceLogFinishPositioningConfirmedEventImpl(now, author, 0, positioningList);
        JSONObject jsonConfirmationEvent = serializer.serialize(event);
        RaceLogFinishPositioningConfirmedEvent deserializedEvent = (RaceLogFinishPositioningConfirmedEvent) deserializer.deserialize(jsonConfirmationEvent);
        assertEquals(event.getId(), deserializedEvent.getId());
        assertEquals(event.getPassId(), deserializedEvent.getPassId());
        assertEquals(event.getLogicalTimePoint(), deserializedEvent.getLogicalTimePoint());
        assertEquals(0, Util.size(event.getInvolvedCompetitors()));
        assertEquals(0, Util.size(deserializedEvent.getInvolvedCompetitors()));
        assertNotNull(event.getPositionedCompetitorsIDsNamesMaxPointsReasons());
        assertNotNull(deserializedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons());
        assertTrue(Util.isEmpty(event.getPositionedCompetitorsIDsNamesMaxPointsReasons()));
        assertTrue(Util.isEmpty(deserializedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons()));
    }

}
