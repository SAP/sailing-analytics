package com.sap.sailing.domain.racelogtracking.test;

import static com.sap.sse.common.Util.size;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;

import java.util.Collections;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDefineMarkEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceCompetitorMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogDeviceMarkMappingEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Sideline;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.MongoSensorFixStoreImpl;
import com.sap.sailing.domain.racelog.RaceLogAndTrackedRaceResolver;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.racelog.tracking.SensorFixStore;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockDeviceAndSessionIdentifierWithGPSFixesDeserializer;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sailing.domain.racelogtracking.impl.SmartphoneImeiIdentifierImpl;
import com.sap.sailing.domain.ranking.OneDesignRankingMetric;
import com.sap.sailing.domain.regattalog.impl.EmptyRegattaLogStore;
import com.sap.sailing.domain.shared.tracking.Track;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRegattaImpl;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.gateway.deserialization.impl.DeviceAndSessionIdentifierWithGPSFixesDeserializer;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBService;

public class AbstractGPSFixStoreTest extends RaceLogTrackingTestHelper {
    protected RacingEventService service;
    protected final  MockSmartphoneImeiServiceFinderFactory serviceFinderFactory = new MockSmartphoneImeiServiceFinderFactory();
    DeviceAndSessionIdentifierWithGPSFixesDeserializer deserializer =
            new MockDeviceAndSessionIdentifierWithGPSFixesDeserializer();
    protected final DeviceIdentifier device = new SmartphoneImeiIdentifierImpl("a");
    protected RegattaLog regattaLog;
    protected SensorFixStore store;
    protected final BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");
    protected final Competitor comp = DomainFactory.INSTANCE.getOrCreateCompetitor("comp", "comp", null, null, null, null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
    protected final Boat boat = DomainFactory.INSTANCE.getOrCreateBoat("boat", "boat", boatClass, "GER 234", null, /* storePersistently */ true);
    protected final Mark mark = DomainFactory.INSTANCE.getOrCreateMark("mark");
    private ClientSession clientSession, metadataCollectionClientSession;

    protected GPSFixMoving createFix(long millis, double lat, double lng, double knots, double degrees) {
        return new GPSFixMovingImpl(new DegreePosition(lat, lng),
                new MillisecondsTimePoint(millis), new KnotSpeedWithBearingImpl(knots, new DegreeBearingImpl(degrees)), /* optionalTrueHeading */ null);
    }
    
    protected GPSFix createFix(long millis, double lat, double lng) {
        return new GPSFixImpl(new DegreePosition(lat, lng),
                new MillisecondsTimePoint(millis));
    }

    @BeforeEach
    public void setServiceAndRaceLog() {
        service = new RacingEventServiceImpl(null, null, serviceFinderFactory);
        raceLog = new RaceLogImpl("racelog");
        regattaLog = new RegattaLogImpl("regattalog");
        clientSession = MongoDBService.INSTANCE.startCausallyConsistentSession();
        metadataCollectionClientSession = MongoDBService.INSTANCE.startCausallyConsistentSession();
        dropPersistedData();
        store = new MongoSensorFixStoreImpl(service.getMongoObjectFactory(), service.getDomainObjectFactory(),
                serviceFinderFactory, ReadConcern.MAJORITY, WriteConcern.MAJORITY, clientSession, metadataCollectionClientSession);
    }

    @AfterEach
    public void after() {
        dropPersistedData();
        clientSession.close();
    }

    private void dropPersistedData() {
        MongoObjectFactoryImpl mongoOF = (MongoObjectFactoryImpl) service.getMongoObjectFactory();
        mongoOF.getGPSFixCollection(clientSession).withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
        mongoOF.getGPSFixMetadataCollection().withWriteConcern(WriteConcern.MAJORITY).drop(metadataCollectionClientSession);
        mongoOF.getRaceLogCollection().withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
        mongoOF.getRegattaLogCollection().withWriteConcern(WriteConcern.MAJORITY).drop(clientSession);
    }

    protected void map(RegattaLog regattaLog, Competitor comp, DeviceIdentifier device, long from, long to) {
        regattaLog.add(new RegattaLogDeviceCompetitorMappingEventImpl(MillisecondsTimePoint.now(),
                MillisecondsTimePoint.now(), author, UUID.randomUUID(), comp, device, new MillisecondsTimePoint(from),
                new MillisecondsTimePoint(to)));
    }

    protected void map(Competitor comp, DeviceIdentifier device, long from, long to) {
        map(regattaLog, comp, device, from, to);
    }

    protected void map(Mark mark, DeviceIdentifier device, long from, long to) {
        regattaLog.add(new RegattaLogDeviceMarkMappingEventImpl(MillisecondsTimePoint.now(),
                MillisecondsTimePoint.now(), author, UUID.randomUUID(), mark, device, new MillisecondsTimePoint(from),
                new MillisecondsTimePoint(to)));
    }
    
    protected void defineMarksOnRegattaLog(Mark... marks) {
        for (int i = 0; i < marks.length; i++) {
            regattaLog.add(new RegattaLogDefineMarkEventImpl(new MillisecondsTimePoint(i + 1), author,
                    new MillisecondsTimePoint(1), 0, marks[i]));
        }
    }

    protected DynamicTrackedRaceImpl createDynamicTrackedRace(BoatClass boatClass, RaceDefinition raceDefinition) {
        DynamicTrackedRegatta regatta = new DynamicTrackedRegattaImpl(new RegattaImpl(EmptyRaceLogStore.INSTANCE,
                EmptyRegattaLogStore.INSTANCE, RegattaImpl.getDefaultName("regatta", boatClass.getName()), boatClass,
                /* canBoatsOfCompetitorsChangePerRace */ true, CompetitorRegistrationType.CLOSED, /* startDate */ null,
                /* endDate */null, null, null, "a", null, /* registrationLinkSecret */ UUID.randomUUID().toString()));
        return new DynamicTrackedRaceImpl(regatta, raceDefinition, Collections.<Sideline> emptyList(),
                EmptyWindStore.INSTANCE, 0, 0, 0, /* useMarkPassingCalculator */ false, OneDesignRankingMetric::new,
                mock(RaceLogAndTrackedRaceResolver.class), /* trackingConnectorInfo */ null, /* markPassingRaceFingerprintRegistry */ null);
    }

    protected void testNumberOfRawFixes(Track<?> track, long expected) {
        track.lockForRead();
        try {
            assertEquals(expected, size(track.getRawFixes()));
        } finally {
            track.unlockAfterRead();
        }
    }
}
