package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCLegDataEvent;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCLegDataEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;

/**
 * @author Daniel Lisunkin (i505543)
 */
public class TestStoringAndLoadingRaceORCLegDataEvent extends AbstractMongoDBTest {

    protected TimePoint expectedEventTime = new MillisecondsTimePoint(750);
    protected Serializable expectedId = UUID.randomUUID();
    protected List<Competitor> expectedInvolvedBoats = Collections.emptyList();
    protected int expectedPassId = 42;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);

    protected MongoObjectFactoryImpl mongoFactory = (MongoObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getMongoObjectFactory(getMongoService(), new MockSmartphoneImeiServiceFinderFactory());
    protected DomainObjectFactoryImpl domainFactory = (DomainObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE,
                    new MockSmartphoneImeiServiceFinderFactory());

    protected RaceLogIdentifier logIdentifier;

    public TestStoringAndLoadingRaceORCLegDataEvent() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    public void setUp() {
        logIdentifier = mock(RaceLogIdentifier.class);
        when(logIdentifier.getIdentifier()).thenReturn(
                new com.sap.sse.common.Util.Triple<String, String, String>("a", "b", UUID.randomUUID().toString()));
        DomainFactory.INSTANCE.getCompetitorAndBoatStore().clearCompetitors();
    }

    @Test
    public void test() throws JsonDeserializationException, ParseException {
        RaceLogORCLegDataEvent expectedEvent = new RaceLogORCLegDataEventImpl(MillisecondsTimePoint.now(), expectedEventTime, author,
                expectedId, expectedPassId, 0, new DegreeBearingImpl(60), new NauticalMileDistance(1), ORCPerformanceCurveLegTypes.CIRCULAR_RANDOM);

        Document dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogORCLegDataEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(expectedEvent.getTwa(), actualEvent.getTwa());
        assertEquals(expectedEvent.getLength(), actualEvent.getLength());
        assertEquals(expectedEvent.getType(), actualEvent.getType());
    }

    public void assertBaseFields(RaceLogEvent expectedEvent, RaceLogEvent actualEvent) {
        assertNotNull(actualEvent);
        assertEquals(expectedEvent.getCreatedAt(), actualEvent.getCreatedAt());
        assertEquals(expectedEvent.getLogicalTimePoint(), actualEvent.getLogicalTimePoint());
        assertEquals(expectedEvent.getId(), actualEvent.getId());
        assertEquals(expectedEvent.getInvolvedCompetitors().size(), Util.size(actualEvent.getInvolvedCompetitors()));
        assertEquals(expectedEvent.getPassId(), actualEvent.getPassId());
    }

    /**
     * Will always wait a couple of milliseconds to ensure that {@link RaceLogEvent#getCreatedAt()} has passed.
     * @throws ParseException 
     * @throws JsonDeserializationException 
     */
    @SuppressWarnings("unchecked")
    private <T extends RaceLogEvent> T loadEvent(Document dbObject) throws JsonDeserializationException, ParseException {
        try {
            Thread.sleep(2);
        } catch (InterruptedException ie) {
            fail(ie.toString());
        }
        RaceLogEvent dbEvent = domainFactory.loadRaceLogEvent((Document) dbObject.get(FieldNames.RACE_LOG_EVENT.name())).getA();
        T actualEvent = (T) dbEvent;
        return actualEvent;
    }

}
