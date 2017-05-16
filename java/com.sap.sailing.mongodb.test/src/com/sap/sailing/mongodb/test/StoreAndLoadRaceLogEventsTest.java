package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseAreaChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogPassChangeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRevokeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseAreaChangeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPassChangeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRevokeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogDenoteForTrackingEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.RaceLogRegisterCompetitorEvent;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogDenoteForTrackingEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.FieldNames;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.tracking.test.mock.MockSmartphoneImeiServiceFinderFactory;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class StoreAndLoadRaceLogEventsTest extends AbstractMongoDBTest {

    protected MongoObjectFactoryImpl mongoFactory = (MongoObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getMongoObjectFactory(getMongoService(), new MockSmartphoneImeiServiceFinderFactory());
    protected DomainObjectFactoryImpl domainFactory = (DomainObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE,
                    new MockSmartphoneImeiServiceFinderFactory());

    protected RaceLogIdentifier logIdentifier;

    protected TimePoint expectedEventTime = new MillisecondsTimePoint(42);
    protected Serializable expectedId = UUID.randomUUID();
    protected List<Competitor> expectedInvolvedBoats = Collections.emptyList();
    protected int expectedPassId = 42;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);

    public StoreAndLoadRaceLogEventsTest() throws UnknownHostException, MongoException {
        super();
    }

    @Before
    public void setUp() {
        logIdentifier = mock(RaceLogIdentifier.class);
        when(logIdentifier.getIdentifier()).thenReturn(
                new com.sap.sse.common.Util.Triple<String, String, String>("a", "b", UUID.randomUUID().toString()));
    }

    public void assertBaseFields(RaceLogEvent expectedEvent, RaceLogEvent actualEvent) {
        assertNotNull(actualEvent);
        assertEquals(expectedEvent.getCreatedAt(), actualEvent.getCreatedAt());
        assertEquals(expectedEvent.getLogicalTimePoint(), actualEvent.getLogicalTimePoint());
        assertEquals(expectedEvent.getId(), actualEvent.getId());
        assertEquals(expectedEvent.getInvolvedBoats().size(), Util.size(actualEvent.getInvolvedBoats()));
        assertEquals(expectedEvent.getPassId(), actualEvent.getPassId());
    }

    @Test
    public void testStoreEventWithoutAuthorLoadsCompatibilityAuthor() {
        RaceLogFlagEvent expectedEvent = new RaceLogFlagEventImpl(MillisecondsTimePoint.now(), expectedEventTime, null,
                expectedId, expectedPassId, Flags.NONE, Flags.NONE, true);

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogFlagEvent actualEvent = loadEvent(dbObject);

        assertNull(expectedEvent.getAuthor());
        assertNotNull(actualEvent.getAuthor());
        assertEquals(AbstractLogEventAuthor.PRIORITY_COMPATIBILITY, actualEvent.getAuthor().getPriority());
        assertEquals(AbstractLogEventAuthor.NAME_COMPATIBILITY, actualEvent.getAuthor().getName());
    }

    @Test
    public void testStoreAndLoadFlagEvent() {
        Flags upperFlag = Flags.ALPHA;
        Flags lowerFlag = Flags.BRAVO;
        boolean isDisplayed = true;

        RaceLogFlagEvent expectedEvent = new RaceLogFlagEventImpl(MillisecondsTimePoint.now(), expectedEventTime,
                author, expectedId, expectedPassId, upperFlag, lowerFlag, isDisplayed);

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogFlagEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(upperFlag, actualEvent.getUpperFlag());
        assertEquals(lowerFlag, actualEvent.getLowerFlag());
        assertEquals(isDisplayed, actualEvent.isDisplayed());
    }

    @Test
    public void testStoreAndLoadCourseAreaChangedEvent() {
        Serializable courseAreaId = UUID.randomUUID();

        RaceLogCourseAreaChangedEvent expectedEvent = new RaceLogCourseAreaChangeEventImpl(MillisecondsTimePoint.now(),
                expectedEventTime, author, expectedId, expectedPassId, courseAreaId);

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogCourseAreaChangedEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(courseAreaId, actualEvent.getCourseAreaId());
    }

    @Test
    public void testStoreAndLoadPassChangeEvent() {
        RaceLogPassChangeEvent expectedEvent = new RaceLogPassChangeEventImpl(MillisecondsTimePoint.now(),
                expectedEventTime, author, expectedId, expectedPassId);

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogPassChangeEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
    }

    @Test
    public void testStoreAndLoadRaceStatusEvent() {
        RaceLogRaceStatus status = RaceLogRaceStatus.SCHEDULED;
        RaceLogRaceStatusEvent expectedEvent = new RaceLogRaceStatusEventImpl(MillisecondsTimePoint.now(),
                expectedEventTime, author, expectedId, expectedPassId, status);

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogRaceStatusEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(status, actualEvent.getNextStatus());
    }

    @Test
    public void testStoreAndLoadStartTimeEvent() {
        TimePoint startTime = new MillisecondsTimePoint(1337);
        RaceLogStartTimeEvent expectedEvent = new RaceLogStartTimeEventImpl(MillisecondsTimePoint.now(),
                expectedEventTime, author, expectedId, expectedPassId, startTime, RaceLogRaceStatus.SCHEDULED);

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogStartTimeEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(startTime, actualEvent.getStartTime());
    }

    @Test
    public void testStoreAndLoadDenoteForTrackingEvent() {
        String raceName = "race";
        BoatClass boatClass = DomainFactory.INSTANCE.getOrCreateBoatClass("49er");
        UUID raceId = UUID.randomUUID();
        RaceLogDenoteForTrackingEvent expectedEvent = new RaceLogDenoteForTrackingEventImpl(
                expectedEventTime, expectedEventTime, author, expectedId, expectedPassId,
                raceName, boatClass, raceId);

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogDenoteForTrackingEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(boatClass, actualEvent.getBoatClass());
        assertEquals(raceName, actualEvent.getRaceName());
        assertEquals(raceId, actualEvent.getRaceId());
    }

    @Test
    public void testStoreAndLoadRevokeEvent() {
        UUID revokedEventId = UUID.randomUUID();
        String revokedEventType = "type";
        String revokedEventShortInfo = "short info";
        String reason = "reason";
        RaceLogRevokeEvent expectedEvent = new RaceLogRevokeEventImpl(expectedEventTime, expectedEventTime, author,
                expectedId, expectedPassId, revokedEventId, revokedEventType, revokedEventShortInfo, reason);

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogRevokeEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(revokedEventId, actualEvent.getRevokedEventId());
        assertEquals(revokedEventType, actualEvent.getRevokedEventType());
        assertEquals(revokedEventShortInfo, actualEvent.getRevokedEventShortInfo());
        assertEquals(reason, actualEvent.getReason());
    }

    @Test
    public void testStoreAndLoadRegisterCompetitorEvent() {
        RaceLogRegisterCompetitorEvent expectedEvent = new RaceLogRegisterCompetitorEventImpl(expectedEventTime,
                expectedEventTime, author, expectedId, expectedPassId, DomainFactory.INSTANCE.getOrCreateCompetitor(
                        "comp", "comp", "c", null, null, null, null,
                        /* timeOnTimeFactor */null, /* timeOnDistanceAllowancePerNauticalMile */null, null));

        DBObject dbObject = mongoFactory.storeRaceLogEntry(logIdentifier, expectedEvent);
        RaceLogRegisterCompetitorEvent actualEvent = loadEvent(dbObject);

        assertBaseFields(expectedEvent, actualEvent);
        assertEquals(expectedEvent.getCompetitor(), actualEvent.getCompetitor());
    }

    /**
     * Will always wait a couple of milliseconds to ensure that {@link RaceLogEvent#getCreatedAt()} has passed.
     */
    @SuppressWarnings("unchecked")
    private <T extends RaceLogEvent> T loadEvent(DBObject dbObject) {
        try {
            Thread.sleep(2);
        } catch (InterruptedException ie) {
            fail(ie.toString());
        }
        RaceLogEvent dbEvent = domainFactory
                .loadRaceLogEvent((DBObject) dbObject.get(FieldNames.RACE_LOG_EVENT.name()));
        T actualEvent = (T) dbEvent;
        return actualEvent;
    }
}
