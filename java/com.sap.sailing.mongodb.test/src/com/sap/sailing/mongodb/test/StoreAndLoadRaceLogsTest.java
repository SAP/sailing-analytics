package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.impl.RaceLogIdentifierImpl;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class StoreAndLoadRaceLogsTest extends AbstractMongoDBTest {

    protected MongoObjectFactoryImpl mongoFactory = (MongoObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getMongoObjectFactory(getMongoService());
    protected DomainObjectFactoryImpl domainFactory = (DomainObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);

    public StoreAndLoadRaceLogsTest() throws UnknownHostException, MongoException {
        super();
    }

    @Test
    public void testStoreAndLoadSingleRaceLog() {
        RaceLogIdentifier identifier = createIdentifier("A.A", "B.B", "C.C");

        RaceLogFlagEvent event1 = createRaceLogFlagEvent(UUID.randomUUID());
        RaceLogFlagEvent event2 = createRaceLogFlagEvent(UUID.randomUUID());
        Document dbEvent1 = mongoFactory.storeRaceLogEntry(identifier, event1);
        mongoFactory.getRaceLogCollection().insertOne(dbEvent1);
        Document dbEvent2 = mongoFactory.storeRaceLogEntry(identifier, event2);
        mongoFactory.getRaceLogCollection().insertOne(dbEvent2);

        RaceLog raceLog = domainFactory.loadRaceLog(identifier);
        try {
            raceLog.lockForRead();
            assertEquals(2, Util.size(raceLog.getRawFixes()));
        } finally {
            raceLog.unlockAfterRead();
        }
    }

    @Test
    public void testStoreAndLoadCollidingRaceLogs() {
        // RaceLog A.A B.B C.C
        RaceLogIdentifier identifier = createIdentifier("A.A", "B.B", "C.C");

        RaceLogFlagEvent event1 = createRaceLogFlagEvent(UUID.randomUUID());
        RaceLogFlagEvent event2 = createRaceLogFlagEvent(UUID.randomUUID());
        Document dbEvent1 = mongoFactory.storeRaceLogEntry(identifier, event1);
        mongoFactory.getRaceLogCollection().insertOne(dbEvent1);
        Document dbEvent2 = mongoFactory.storeRaceLogEntry(identifier, event2);
        mongoFactory.getRaceLogCollection().insertOne(dbEvent2);

        // RaceLog A.A.B B.C C
        RaceLogIdentifier collidingIdentifier = createIdentifier("A.A.B", "B.C", "C");

        RaceLogFlagEvent event3 = createRaceLogFlagEvent(UUID.randomUUID());
        Document dbEvent3 = mongoFactory.storeRaceLogEntry(collidingIdentifier, event3);
        mongoFactory.getRaceLogCollection().insertOne(dbEvent3);

        RaceLog raceLog = domainFactory.loadRaceLog(identifier);
        try {
            raceLog.lockForRead();
            assertEquals(2, Util.size(raceLog.getRawFixes()));
        } finally {
            raceLog.unlockAfterRead();
        }
    }

    private RaceLogIdentifier createIdentifier(String groupName, String raceColumnName, String fleetName) {
        RegattaLikeIdentifier regattaLike = mock(RegattaLikeIdentifier.class);
        when(regattaLike.getName()).thenReturn(groupName);
        Fleet fleet = mock(Fleet.class);
        when(fleet.getName()).thenReturn(fleetName);
        return new RaceLogIdentifierImpl(regattaLike, raceColumnName, fleet);
    }

    private RaceLogFlagEvent createRaceLogFlagEvent(Serializable id) {
        return new RaceLogFlagEventImpl(MillisecondsTimePoint.now(), new MillisecondsTimePoint(42), new LogEventAuthorImpl("Test author", /* priority */ 1), id,
                42, Flags.ALPHA, Flags.BRAVO, true);
    }

}
