package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bson.Document;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.orc.RaceLogORCCertificateAssignmentEvent;
import com.sap.sailing.domain.abstractlog.orc.impl.RaceLogORCCertificateAssignmentEventImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.orc.ORCCertificatesCollection;
import com.sap.sailing.domain.orc.impl.ORCCertificatesJsonImporter;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.shared.json.JsonDeserializationException;

/**
 * @author Daniel Lisunkin (i505543)
 */
public class TestStoringAndLoadingRegattaORCCertificateAssignmentEvent extends AbstractMongoDBTest {

    public TestStoringAndLoadingRegattaORCCertificateAssignmentEvent() throws UnknownHostException, MongoException {
        super();
    }

    protected TimePoint expectedEventTime = new MillisecondsTimePoint(750);
    protected Serializable expectedId = UUID.randomUUID();
    protected List<Competitor> expectedInvolvedBoats = Collections.emptyList();
    protected int expectedPassId = 42;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    private final Competitor competitor = new CompetitorImpl("comp", "Comp", "KYC", null, null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
    
    protected MongoObjectFactoryImpl mongoFactory = (MongoObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getMongoObjectFactory(getMongoService(), new MockSmartphoneImeiServiceFinderFactory());
    protected DomainObjectFactoryImpl domainFactory = (DomainObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE,
                    new MockSmartphoneImeiServiceFinderFactory());

    protected RaceLogIdentifier logIdentifier;
    private ORCCertificatesCollection importer;

    @BeforeEach
    public void setUp() throws FileNotFoundException, IOException, ParseException {
        logIdentifier = mock(RaceLogIdentifier.class);
        when(logIdentifier.getIdentifier()).thenReturn(
                new com.sap.sse.common.Util.Triple<String, String, String>("a", "b", UUID.randomUUID().toString()));
        DomainFactory.INSTANCE.getCompetitorAndBoatStore().clearCompetitors();
        importer = new ORCCertificatesJsonImporter().read(new FileInputStream(new File("resources/GER2019.json")));
    }

    @Test
    public void test() throws JsonDeserializationException, ParseException {
        RaceLogORCCertificateAssignmentEvent expectedEvent = new RaceLogORCCertificateAssignmentEventImpl(MillisecondsTimePoint.now(), expectedEventTime, author,
                expectedId, expectedPassId, importer.getCertificateById("GER20040783"), competitor);

        Document dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogORCCertificateAssignmentEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(expectedEvent.getBoatId(), actualEvent.getBoatId());
        assertEquals(expectedEvent.getCertificate().getSailNumber(), actualEvent.getCertificate().getSailNumber());
        assertEquals(expectedEvent.getCertificate().getGPHInSecondsToTheMile(), actualEvent.getCertificate().getGPHInSecondsToTheMile(), 0.00001);

        List<Speed> expectedBeatVMGPredictions = new ArrayList<>(expectedEvent.getCertificate().getBeatVMGPredictions().values());
        List<Double> expectedBeatVMGPredictionsInKnots = expectedBeatVMGPredictions.stream().map(s -> s.getKnots()).collect(Collectors.toList());
        List<Speed> actualBeatVMGPredictions = new ArrayList<>(actualEvent.getCertificate().getBeatVMGPredictions().values());
        List<Double> actualBeatVMGPredictionsInKnots = actualBeatVMGPredictions.stream().map(s -> s.getKnots()).collect(Collectors.toList());
        // Unfortunately we can't assure that the speeds have the same type and therefore we can't assure the same level of accuracy after possible conversions.
        // A normal comparison of the two HashMaps containing the values doesn't address this issue, so we need to compare the double values (in Knots) to ensure
        // the same level of accuracy.
        assertEquals(expectedBeatVMGPredictionsInKnots, actualBeatVMGPredictionsInKnots);
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
