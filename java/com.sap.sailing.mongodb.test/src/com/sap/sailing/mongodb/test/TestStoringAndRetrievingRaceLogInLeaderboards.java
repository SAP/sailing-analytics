package com.sap.sailing.mongodb.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.Serializable;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.DBObject;
import com.mongodb.MongoException;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.persistence.MongoFactory;
import com.sap.sailing.domain.persistence.MongoRaceLogStoreFactory;
import com.sap.sailing.domain.persistence.impl.FieldNames;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoUtils;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogCourseAreaChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogEventFactory;
import com.sap.sailing.domain.racelog.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.racelog.RaceLogFinishPositioningListChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;
import com.sap.sailing.domain.racelog.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogPassChangeEvent;
import com.sap.sailing.domain.racelog.RaceLogPathfinderEvent;
import com.sap.sailing.domain.racelog.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.racelog.RaceLogStartProcedureChangedEvent;
import com.sap.sailing.domain.racelog.RaceLogStartTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogStore;
import com.sap.sailing.domain.racelog.RaceLogWindFixEvent;
import com.sap.sailing.domain.racelog.impl.RaceLogEventAuthorImpl;
import com.sap.sailing.domain.tracking.Wind;

public class TestStoringAndRetrievingRaceLogInLeaderboards extends RaceLogMongoDBTest {

    String leaderboardName = "TestLeaderboard";
    final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
    FlexibleLeaderboardImpl leaderboard = null;
    private RaceLogEventAuthor author = new RaceLogEventAuthorImpl("Test Author", 1);
    
    public TestStoringAndRetrievingRaceLogInLeaderboards() throws UnknownHostException, MongoException {
        super();
    }

    @Before
    public void setUp() {
        now = MillisecondsTimePoint.now();
        mongoObjectFactory = MongoFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        domainObjectFactory = MongoFactory.INSTANCE.getDomainObjectFactory(getMongoService());

        RaceLogStore raceLogStore = MongoRaceLogStoreFactory.INSTANCE.getMongoRaceLogStore(mongoObjectFactory, domainObjectFactory);

        leaderboard = new FlexibleLeaderboardImpl(raceLogStore, leaderboardName, new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), null);

        Fleet defaultFleet = leaderboard.getFleet(null);
        leaderboard.addRaceColumn(raceColumnName, /* medalRace */ false, defaultFleet);
    }

    private void addAndStoreRaceLogEvent(FlexibleLeaderboard leaderboard, String raceColumnName, RaceLogEvent event) {
        Fleet defaultFleet = leaderboard.getFleet(null);
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        raceColumn.getRaceLog(defaultFleet).add(event);

        mongoObjectFactory.storeLeaderboard(leaderboard);
    }

    private RaceLog retrieveRaceLog() {
        Leaderboard loadedLeaderboard = domainObjectFactory.loadLeaderboard(leaderboardName, /* regattaRegistry */ null);
        Fleet loadedDefaultFleet = loadedLeaderboard.getFleet(null);

        return loadedLeaderboard.getRaceColumnByName(raceColumnName).getRaceLog(loadedDefaultFleet);
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogProtestStartTimeEvent() {        
        RaceLogProtestStartTimeEvent expectedEvent = RaceLogEventFactory.INSTANCE.createProtestStartTimeEvent(now, author, 0, MillisecondsTimePoint.now());

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, expectedEvent);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogProtestStartTimeEvent actualEvent = (RaceLogProtestStartTimeEvent) loadedEvent;
            assertEquals(expectedEvent.getTimePoint(), actualEvent.getTimePoint());
            assertEquals(expectedEvent.getPassId(), actualEvent.getPassId());
            assertEquals(expectedEvent.getId(), actualEvent.getId());
            assertEquals(expectedEvent.getProtestStartTime(), actualEvent.getProtestStartTime());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogStartProcedureChangedEvent() {        
        RaceLogStartProcedureChangedEvent expectedEvent = RaceLogEventFactory.INSTANCE.createStartProcedureChangedEvent(now, author, 0, RacingProcedureType.ESS);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, expectedEvent);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogStartProcedureChangedEvent actualEvent = (RaceLogStartProcedureChangedEvent) loadedEvent;
            assertEquals(expectedEvent.getTimePoint(), actualEvent.getTimePoint());
            assertEquals(expectedEvent.getPassId(), actualEvent.getPassId());
            assertEquals(expectedEvent.getId(), actualEvent.getId());
            assertEquals(expectedEvent.getStartProcedureType(), actualEvent.getStartProcedureType());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogGateLineOpeningTimeEvent() {        
        RaceLogGateLineOpeningTimeEvent expectedEvent = RaceLogEventFactory.INSTANCE.createGateLineOpeningTimeEvent(now, author, 0, 1234l);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, expectedEvent);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogGateLineOpeningTimeEvent actualEvent = (RaceLogGateLineOpeningTimeEvent) loadedEvent;
            assertEquals(expectedEvent.getTimePoint(), actualEvent.getTimePoint());
            assertEquals(expectedEvent.getPassId(), actualEvent.getPassId());
            assertEquals(expectedEvent.getId(), actualEvent.getId());
            assertEquals(expectedEvent.getGateLineOpeningTime(), actualEvent.getGateLineOpeningTime());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogPathfinderEvent() {        
        RaceLogPathfinderEvent expectedEvent = RaceLogEventFactory.INSTANCE.createPathfinderEvent(now, author, 0, "GER 20");

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, expectedEvent);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogPathfinderEvent actualEvent = (RaceLogPathfinderEvent) loadedEvent;
            assertEquals(expectedEvent.getTimePoint(), actualEvent.getTimePoint());
            assertEquals(expectedEvent.getPassId(), actualEvent.getPassId());
            assertEquals(expectedEvent.getId(), actualEvent.getId());
            assertEquals(expectedEvent.getPathfinderId(), actualEvent.getPathfinderId());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogPassChangeEvent() {        
        RaceLogPassChangeEvent event = RaceLogEventFactory.INSTANCE.createPassChangeEvent(now, author, 0);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogPassChangeEvent passEvent = (RaceLogPassChangeEvent) loadedEvent;
            assertEquals(event.getTimePoint(), passEvent.getTimePoint());
            assertEquals(event.getPassId(), passEvent.getPassId());
            assertEquals(event.getId(), passEvent.getId());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogFinishPositioningConfirmedEvent() {   
        Competitor storedCompetitor = DomainFactory.INSTANCE.createCompetitor(UUID.randomUUID(), "SAP Extreme Sailing Team", null, null);
        List<Triple<Serializable, String, MaxPointsReason>> storedPositioningList = new ArrayList<Triple<Serializable, String, MaxPointsReason>>();
        storedPositioningList.add(new Triple<Serializable, String, MaxPointsReason>(storedCompetitor.getId(), storedCompetitor.getName(), MaxPointsReason.NONE));
        
        RaceLogFinishPositioningConfirmedEvent event = RaceLogEventFactory.INSTANCE.createFinishPositioningConfirmedEvent(now, author, 0, storedPositioningList);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningConfirmedEvent loadedConfirmedEvent = (RaceLogFinishPositioningConfirmedEvent) loadedEvent;
            assertEquals(event.getTimePoint(), loadedConfirmedEvent.getTimePoint());
            assertEquals(event.getPassId(), loadedConfirmedEvent.getPassId());
            assertEquals(event.getId(), loadedConfirmedEvent.getId());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithBackwardsCompatibleRaceLogFinishPositioningConfirmedEvent() {
        RaceLogFinishPositioningConfirmedEvent event = RaceLogEventFactory.INSTANCE.createFinishPositioningConfirmedEvent(now, author, 0, null);

        createAndStoreOldRaceLogFinishPositioningConfirmedEventDBEntry();

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningConfirmedEvent loadedConfirmedEvent = (RaceLogFinishPositioningConfirmedEvent) loadedEvent;
            assertEquals(now, loadedConfirmedEvent.getTimePoint());
            assertEquals(0, loadedConfirmedEvent.getPassId());
            assertEquals(0, loadedConfirmedEvent.getInvolvedBoats().size());
            assertNull(event.getPositionedCompetitorsIDsNamesMaxPointsReasons()); 
            assertNull(loadedConfirmedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    private void createAndStoreOldRaceLogFinishPositioningConfirmedEventDBEntry() {
        Fleet defaultFleet = leaderboard.getFleet(null);
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        mongoObjectFactory.storeLeaderboard(leaderboard);
        
        DBObject result = new BasicDBObject();
        result.put(FieldNames.TIME_AS_MILLIS.name(), now.asMillis());
        result.put(FieldNames.RACE_LOG_EVENT_CREATED_AT.name(), now.asMillis());
        result.put(FieldNames.RACE_LOG_EVENT_ID.name(), UUID.randomUUID());
        result.put(FieldNames.RACE_LOG_EVENT_PASS_ID.name(), 0);
        result.put(FieldNames.RACE_LOG_EVENT_INVOLVED_BOATS.name(), new BasicDBList());
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFinishPositioningConfirmedEvent.class.getSimpleName());
        
        DBObject raceLogResult = new BasicDBObject();
        raceLogResult.put(FieldNames.RACE_LOG_IDENTIFIER.name(), MongoUtils.escapeDollarAndDot(raceColumn.getRaceLogIdentifier(defaultFleet).getDeprecatedIdentifier()));       
        raceLogResult.put(FieldNames.RACE_LOG_EVENT.name(), result);
        
        MongoObjectFactoryImpl factoryImpl = (MongoObjectFactoryImpl) mongoObjectFactory;
        factoryImpl.getRaceLogCollection().insert(raceLogResult);
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogFinishPositioningListChangeEvent() {
        Competitor storedCompetitor = DomainFactory.INSTANCE.createCompetitor(UUID.randomUUID(), "SAP Extreme Sailing Team", null, null);
        List<Triple<Serializable, String, MaxPointsReason>> storedPositioningList = new ArrayList<Triple<Serializable, String, MaxPointsReason>>();
        storedPositioningList.add(new Triple<Serializable, String, MaxPointsReason>(storedCompetitor.getId(), storedCompetitor.getName(), MaxPointsReason.NONE));
        
        RaceLogFinishPositioningListChangedEvent event = RaceLogEventFactory.INSTANCE.createFinishPositioningListChangedEvent(now, author, 0, storedPositioningList);
        
        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningListChangedEvent loadedPositioningEvent = (RaceLogFinishPositioningListChangedEvent) loadedEvent;
            assertEquals(event.getTimePoint(), loadedPositioningEvent.getTimePoint());
            assertEquals(event.getPassId(), loadedPositioningEvent.getPassId());
            assertEquals(event.getId(), loadedPositioningEvent.getId());
            assertEquals(event.getInvolvedBoats().size(), loadedPositioningEvent.getInvolvedBoats().size());
            assertEquals(event.getPositionedCompetitorsIDsNamesMaxPointsReasons().size(), loadedPositioningEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons().size());
            assertEquals(event.getPositionedCompetitorsIDsNamesMaxPointsReasons().get(0).getA(), loadedPositioningEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons().get(0).getA());
            assertEquals(event.getPositionedCompetitorsIDsNamesMaxPointsReasons().get(0).getB(), loadedPositioningEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons().get(0).getB());
            assertEquals(event.getPositionedCompetitorsIDsNamesMaxPointsReasons().get(0).getC().name(), loadedPositioningEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons().get(0).getC().name());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogCourseAreaChangeEvent() {
        final UUID uuid = UUID.randomUUID();
        RaceLogCourseAreaChangedEvent event = RaceLogEventFactory.INSTANCE.createCourseAreaChangedEvent(now, author, 0, uuid);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogCourseAreaChangedEvent courseAreaEvent = (RaceLogCourseAreaChangedEvent) loadedEvent;
            assertEquals(event.getTimePoint(), courseAreaEvent.getTimePoint());
            assertEquals(event.getPassId(), courseAreaEvent.getPassId());
            assertEquals(event.getId(), courseAreaEvent.getId());
            assertEquals(event.getCourseAreaId(), courseAreaEvent.getCourseAreaId());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogRaceStatusEvent() {

        RaceLogRaceStatusEvent event = RaceLogEventFactory.INSTANCE.createRaceStatusEvent(now, author, 0, RaceLogRaceStatus.SCHEDULED);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogRaceStatusEvent statusEvent = (RaceLogRaceStatusEvent) loadedEvent;
            assertEquals(event.getTimePoint(), statusEvent.getTimePoint());
            assertEquals(event.getPassId(), statusEvent.getPassId());
            assertEquals(event.getId(), statusEvent.getId());
            assertEquals(event.getNextStatus(), statusEvent.getNextStatus());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogStartTimeEvent() {

        RaceLogStartTimeEvent event = RaceLogEventFactory.INSTANCE.createStartTimeEvent(now, author, 0, now);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogStartTimeEvent timeEvent = (RaceLogStartTimeEvent) loadedEvent;
            assertEquals(event.getTimePoint(), timeEvent.getTimePoint());
            assertEquals(event.getPassId(), timeEvent.getPassId());
            assertEquals(event.getId(), timeEvent.getId());
            assertEquals(event.getNextStatus(), timeEvent.getNextStatus());
            assertEquals(event.getStartTime(), timeEvent.getStartTime());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogFlagEvent() {

        RaceLogFlagEvent event = RaceLogEventFactory.INSTANCE.createFlagEvent(now, author, 0, Flags.FIRSTSUBSTITUTE, Flags.NONE, true);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFlagEvent flagEvent = (RaceLogFlagEvent) loadedEvent;
            assertEquals(event.getTimePoint(), flagEvent.getTimePoint());
            assertEquals(event.getPassId(), flagEvent.getPassId());
            assertEquals(event.getId(), flagEvent.getId());
            assertEquals(event.getUpperFlag(), flagEvent.getUpperFlag());
            assertEquals(event.getLowerFlag(), flagEvent.getLowerFlag());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogCourseDesignChangedEvent() {
        CourseBase course = createCourseBase();
        RaceLogCourseDesignChangedEvent event = RaceLogEventFactory.INSTANCE.createCourseDesignChangedEvent(now, author, 0, course);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogCourseDesignChangedEvent courseDesignEvent = (RaceLogCourseDesignChangedEvent) loadedEvent;
            assertEquals(event.getTimePoint(), courseDesignEvent.getTimePoint());
            assertEquals(event.getPassId(), courseDesignEvent.getPassId());
            assertEquals(event.getId(), courseDesignEvent.getId());
            compareCourseData(event.getCourseDesign(), courseDesignEvent.getCourseDesign());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogWindFixEvent() {
        Wind wind = createWindFix();
        RaceLogWindFixEvent event = RaceLogEventFactory.INSTANCE.createWindFixEvent(now, author, 0, wind);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogWindFixEvent windEvent = (RaceLogWindFixEvent) loadedEvent;
            assertEquals(event.getTimePoint(), windEvent.getTimePoint());
            assertEquals(event.getPassId(), windEvent.getPassId());
            assertEquals(event.getId(), windEvent.getId());
            compareWind(event.getWindFix(), windEvent.getWindFix());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

}
