package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.UnknownHostException;
import java.util.Collections;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;

import com.mongodb.BasicDBList;
import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogDependentStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningListChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogPassChangeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogPathfinderEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartProcedureChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogDependentStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEventComparator;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningConfirmedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFinishPositioningListChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogGateLineOpeningTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPassChangeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPathfinderEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogProtestStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartProcedureChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogWindFixEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.TripleSerializer;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

public class TestStoringAndRetrievingRaceLogInLeaderboards extends RaceLogMongoDBTest {

    String leaderboardName = "TestLeaderboard";
    final int[] discardIndexResultsStartingWithHowManyRaces = new int[] { 5, 8 };
    FlexibleLeaderboardImpl leaderboard = null;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    
    public TestStoringAndRetrievingRaceLogInLeaderboards() throws UnknownHostException, MongoException {
        super();
    }

    @BeforeEach
    public void setUp() {
        now = MillisecondsTimePoint.now();
        mongoObjectFactory = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        domainObjectFactory = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(),
                DomainFactory.INSTANCE);
        leaderboard = new FlexibleLeaderboardImpl(getRaceLogStore(), getRegattaLogStore(), leaderboardName,
                new ThresholdBasedResultDiscardingRuleImpl(discardIndexResultsStartingWithHowManyRaces),
                new LowPoint(), /* courseAreaIds */ Collections.emptySet());
        leaderboard.addRaceColumn(raceColumnName, /* medalRace */false);
    }

    private void addAndStoreRaceLogEvent(FlexibleLeaderboard leaderboard, String raceColumnName, RaceLogEvent event) {
        Fleet defaultFleet = leaderboard.getFleet(null);
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        raceColumn.getRaceLog(defaultFleet).add(event);
        mongoObjectFactory.storeLeaderboard(leaderboard);
    }

    private RaceLog retrieveRaceLog() {
        Leaderboard loadedLeaderboard = domainObjectFactory.loadLeaderboard(leaderboardName, /* regattaRegistry */ null, /* leaderboardRegistry */ null);
        Fleet loadedDefaultFleet = loadedLeaderboard.getFleet(null);
        return loadedLeaderboard.getRaceColumnByName(raceColumnName).getRaceLog(loadedDefaultFleet);
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogProtestStartTimeEvent() {
        TimePoint now = MillisecondsTimePoint.now();
        TimeRange protestTime = new TimeRangeImpl(now, now.plus(Duration.ONE_MINUTE.times(90)));
        RaceLogProtestStartTimeEvent expectedEvent = new RaceLogProtestStartTimeEventImpl(now, author, 0, protestTime);
        addAndStoreRaceLogEvent(leaderboard, raceColumnName, expectedEvent);
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogProtestStartTimeEvent actualEvent = (RaceLogProtestStartTimeEvent) loadedEvent;
            assertEquals(expectedEvent.getLogicalTimePoint(), actualEvent.getLogicalTimePoint());
            assertEquals(expectedEvent.getPassId(), actualEvent.getPassId());
            assertEquals(expectedEvent.getId(), actualEvent.getId());
            assertEquals(expectedEvent.getProtestTime().from(), actualEvent.getProtestTime().from());
            assertEquals(expectedEvent.getProtestTime().to(), actualEvent.getProtestTime().to());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testThatRemoveRaceColumnAlsoRemovesPersistentRaceLog() {
        TimePoint now = MillisecondsTimePoint.now();
        TimeRange protestTime = new TimeRangeImpl(now, now.plus(Duration.ONE_MINUTE.times(90)));
        RaceLogProtestStartTimeEvent expectedEvent = new RaceLogProtestStartTimeEventImpl(now, author, 0, protestTime);
        Fleet defaultFleet = leaderboard.getFleet(null);
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        raceColumn.getRaceLog(defaultFleet).add(expectedEvent);
        //getLastError() is now deprecated - seems to run fine without (at least locally)
        //db.getLastError(); // sync DB
        leaderboard.removeRaceColumn(raceColumnName);
        //getLastError() is now deprecated - seems to run fine without (at least locally)
        //db.getLastError(); // sync DB
        leaderboard.addRaceColumn(raceColumnName, /* medalRace */false);
        // now assert that the race log is empty because the column was removed and so should have been the race log
        RaceLog loadedRaceLog = leaderboard.getRaceColumnByName(raceColumnName).getRaceLog(leaderboard.getRaceColumnByName(raceColumnName).getFleets().iterator().next());
        loadedRaceLog.lockForRead();
        try {
            assertEquals(0, Util.size(loadedRaceLog.getRawFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testThatRenameRaceColumnMigratesPersistentRaceLog() {
        RacingEventServiceImpl service = mock(RacingEventServiceImpl.class);
        when(service.getMongoObjectFactory()).thenReturn(mongoObjectFactory);
        when(service.getLeaderboardByName(leaderboardName)).thenReturn(leaderboard);
        doCallRealMethod().when(service).renameLeaderboardColumn(ArgumentMatchers.anyString(), ArgumentMatchers.anyString(), ArgumentMatchers.anyString());
        TimePoint now = MillisecondsTimePoint.now();
        TimeRange protestTime = new TimeRangeImpl(now, now.plus(Duration.ONE_MINUTE.times(90)));
        RaceLogProtestStartTimeEvent expectedEvent = new RaceLogProtestStartTimeEventImpl(now, author, 0, protestTime);
        Fleet defaultFleet = leaderboard.getFleet(null);
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        raceColumn.getRaceLog(defaultFleet).add(expectedEvent);
        //getLastError() is now deprecated - seems to run fine without (at least locally)
        //db.getLastError(); // sync DB to ensure event is stored
        final String newColumnName = "New "+raceColumnName;
        service.renameLeaderboardColumn(leaderboardName, raceColumnName, newColumnName);
        //getLastError() is now deprecated - seems to run fine without (at least locally)
        //db.getLastError(); // sync DB
        // now assert that the race log still holds the original race log event because the column was only renamed
        RaceLog renamedRaceLog = leaderboard.getRaceColumnByName(newColumnName).getRaceLog(leaderboard.getRaceColumnByName(newColumnName).getFleets().iterator().next());
        renamedRaceLog.lockForRead();
        try {
            assertEquals(1, Util.size(renamedRaceLog.getRawFixes()));
            assertSame(expectedEvent, renamedRaceLog.getRawFixes().iterator().next());
        } finally {
            renamedRaceLog.unlockAfterRead();
        }
        RaceLog reloadedRenamedRaceLog = domainObjectFactory.loadRaceLog(leaderboard.getRaceColumnByName(newColumnName)
                .getRaceLogIdentifier(leaderboard.getRaceColumnByName(newColumnName).getFleets().iterator().next()));
        reloadedRenamedRaceLog.lockForRead();
        try {
            assertEquals(1, Util.size(reloadedRenamedRaceLog.getRawFixes()));
            assertEquals(0, new RaceLogEventComparator().compare(expectedEvent, reloadedRenamedRaceLog.getRawFixes().iterator().next()));
        } finally {
            reloadedRenamedRaceLog.unlockAfterRead();
        }
        leaderboard.addRaceColumn(raceColumnName, /* medalRace */false);
        mongoObjectFactory.storeLeaderboard(leaderboard);
        // now assert that the race log is empty because the renamed column's persistent race log is expected to have
        // been removed
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            assertEquals(0, Util.size(loadedRaceLog.getRawFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogStartProcedureChangedEvent() {        
        RaceLogStartProcedureChangedEvent expectedEvent = new RaceLogStartProcedureChangedEventImpl(now, author, 0, RacingProcedureType.ESS);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, expectedEvent);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogStartProcedureChangedEvent actualEvent = (RaceLogStartProcedureChangedEvent) loadedEvent;
            assertEquals(expectedEvent.getLogicalTimePoint(), actualEvent.getLogicalTimePoint());
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
        RaceLogGateLineOpeningTimeEvent expectedEvent = new RaceLogGateLineOpeningTimeEventImpl(now, author, 0, 1234l, 54321l);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, expectedEvent);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogGateLineOpeningTimeEvent actualEvent = (RaceLogGateLineOpeningTimeEvent) loadedEvent;
            assertEquals(expectedEvent.getLogicalTimePoint(), actualEvent.getLogicalTimePoint());
            assertEquals(expectedEvent.getPassId(), actualEvent.getPassId());
            assertEquals(expectedEvent.getId(), actualEvent.getId());
            assertEquals(expectedEvent.getGateLineOpeningTimes(), actualEvent.getGateLineOpeningTimes());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogPathfinderEvent() {        
        RaceLogPathfinderEvent expectedEvent = new RaceLogPathfinderEventImpl(now, author, 0, "GER 20");

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, expectedEvent);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogPathfinderEvent actualEvent = (RaceLogPathfinderEvent) loadedEvent;
            assertEquals(expectedEvent.getLogicalTimePoint(), actualEvent.getLogicalTimePoint());
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
        RaceLogPassChangeEvent event = new RaceLogPassChangeEventImpl(now, author, 0);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogPassChangeEvent passEvent = (RaceLogPassChangeEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), passEvent.getLogicalTimePoint());
            assertEquals(event.getPassId(), passEvent.getPassId());
            assertEquals(event.getId(), passEvent.getId());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogFinishPositioningConfirmedEvent() {   
        Competitor storedCompetitor = DomainFactory.INSTANCE.getOrCreateCompetitor(UUID.randomUUID(), "SAP Extreme Sailing Team", "SAP",
                Color.RED, "someone@nowhere.de", null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat storedBoat = DomainFactory.INSTANCE.getOrCreateBoat(UUID.randomUUID(), "SAP Extreme Sailing Team",
                new BoatClassImpl("X40", false), "123", Color.RED, /* storePersistently */ true);
        CompetitorResults storedPositioningList = new CompetitorResultsImpl();
        storedPositioningList.add(new CompetitorResultImpl(storedCompetitor.getId(), storedCompetitor.getName(), storedCompetitor.getShortName(),
                storedBoat.getName(), storedBoat.getSailID(), /* rank */ 1, MaxPointsReason.NONE, /* score */ null, /* finishingTimePoint */null,
                /* comment */null, MergeState.OK));
        RaceLogFinishPositioningConfirmedEvent event = new RaceLogFinishPositioningConfirmedEventImpl(now, author, 0, storedPositioningList);
        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningConfirmedEvent loadedConfirmedEvent = (RaceLogFinishPositioningConfirmedEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), loadedConfirmedEvent.getLogicalTimePoint());
            assertEquals(event.getPassId(), loadedConfirmedEvent.getPassId());
            assertEquals(event.getId(), loadedConfirmedEvent.getId());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogFinishPositioningConfirmedEventWithContents() {   
        Competitor storedCompetitor = DomainFactory.INSTANCE.getOrCreateCompetitor(UUID.randomUUID(), "SAP Extreme Sailing Team", "SAP",
                Color.RED, "someone@nowhere.de", null, null,
                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat storedBoat = DomainFactory.INSTANCE.getOrCreateBoat(UUID.randomUUID(), "SAP Extreme Sailing Team",
                new BoatClassImpl("X40", false), "123", Color.RED, /* storePersistently */ true);
        CompetitorResults storedPositioningList = new CompetitorResultsImpl();
        final TimePoint finishingTimePoint = MillisecondsTimePoint.now();
        final String comment = "This is a comment";
        storedPositioningList.add(new CompetitorResultImpl(storedCompetitor.getId(), storedCompetitor.getName(), storedCompetitor.getShortName(),
                storedBoat.getName(), storedBoat.getSailID(), /* rank */ 1, MaxPointsReason.NONE, /* score */ 12.3,  finishingTimePoint, comment,
                MergeState.OK));
        RaceLogFinishPositioningConfirmedEvent event = new RaceLogFinishPositioningConfirmedEventImpl(now, author, 0, storedPositioningList);
        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningConfirmedEvent loadedConfirmedEvent = (RaceLogFinishPositioningConfirmedEvent) loadedEvent;
            CompetitorResults results = loadedConfirmedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons();
            assertEquals(1, Util.size(results));
            final CompetitorResult result = results.iterator().next();
            assertEquals(comment, result.getComment());
            assertEquals(12.3, result.getScore(), 0.000001);
            assertEquals(MaxPointsReason.NONE, result.getMaxPointsReason());
            assertEquals(1, result.getOneBasedRank());
            assertEquals(finishingTimePoint, result.getFinishingTime());
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithBackwardsCompatibleRaceLogFinishPositioningConfirmedEvent() {
        RaceLogFinishPositioningConfirmedEvent event = new RaceLogFinishPositioningConfirmedEventImpl(now, author, 0, null);
        createAndStoreOldRaceLogFinishPositioningConfirmedEventDBEntry();
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningConfirmedEvent loadedConfirmedEvent = (RaceLogFinishPositioningConfirmedEvent) loadedEvent;
            assertEquals(now, loadedConfirmedEvent.getLogicalTimePoint());
            assertEquals(0, loadedConfirmedEvent.getPassId());
            assertEquals(0, loadedConfirmedEvent.getInvolvedCompetitors().size());
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
        
        Document result = new Document();
        result.put(FieldNames.TIME_AS_MILLIS.name(), now.asMillis());
        result.put(FieldNames.RACE_LOG_EVENT_CREATED_AT.name(), now.asMillis());
        result.put(FieldNames.RACE_LOG_EVENT_ID.name(), UUID.randomUUID());
        result.put(FieldNames.RACE_LOG_EVENT_PASS_ID.name(), 0);
        result.put(FieldNames.RACE_LOG_EVENT_INVOLVED_BOATS.name(), new BasicDBList());
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFinishPositioningConfirmedEvent.class.getSimpleName());
        Document raceLogResult = new Document();
        raceLogResult.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceColumn.getRaceLogIdentifier(defaultFleet).getIdentifier()));       
        raceLogResult.put(FieldNames.RACE_LOG_EVENT.name(), result);
        MongoObjectFactoryImpl factoryImpl = (MongoObjectFactoryImpl) mongoObjectFactory;
        factoryImpl.getRaceLogCollection().insertOne(raceLogResult);
    }
    
    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogFinishPositioningListChangeEvent() {
        Competitor storedCompetitor = DomainFactory.INSTANCE.getOrCreateCompetitor(UUID.randomUUID(), "SAP Extreme Sailing Team", "SAP", Color.RED,
                "someone@nowhere.de", null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat storedBoat = DomainFactory.INSTANCE.getOrCreateBoat(UUID.randomUUID(), "SAP Extreme Sailing Team",
                new BoatClassImpl("X40", false), "123", Color.RED, /* storePersistently */ true);
        CompetitorResults storedPositioningList = new CompetitorResultsImpl();
        storedPositioningList.add(new CompetitorResultImpl(storedCompetitor.getId(), storedCompetitor.getName(), storedCompetitor.getShortName(),
                storedBoat.getName(), storedBoat.getSailID(), /* rank */ 1, MaxPointsReason.NONE, /* score */ null, /* finishingTimePoint */null,
                /* comment */null, MergeState.OK));
        RaceLogFinishPositioningListChangedEvent event = new RaceLogFinishPositioningListChangedEventImpl(now, author, 0, storedPositioningList);
        
        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningListChangedEvent loadedPositioningEvent = (RaceLogFinishPositioningListChangedEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), loadedPositioningEvent.getLogicalTimePoint());
            assertEquals(event.getPassId(), loadedPositioningEvent.getPassId());
            assertEquals(event.getId(), loadedPositioningEvent.getId());
            assertEquals(event.getInvolvedCompetitors().size(), loadedPositioningEvent.getInvolvedCompetitors().size());
            final CompetitorResults expectedCompetiorResults = event.getPositionedCompetitorsIDsNamesMaxPointsReasons();
            final CompetitorResults loadedCompetitorResults = loadedPositioningEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons();
            assertCompetitorResultsEqual(expectedCompetiorResults, loadedCompetitorResults);
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

    @Test
    public void testStoreAndRetrieveSimpleLeaderboardWithRaceLogRaceStatusEvent() {

        RaceLogRaceStatusEvent event = new RaceLogRaceStatusEventImpl(now, author, 0, RaceLogRaceStatus.SCHEDULED);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogRaceStatusEvent statusEvent = (RaceLogRaceStatusEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), statusEvent.getLogicalTimePoint());
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

        RaceLogStartTimeEvent event = new RaceLogStartTimeEventImpl(now, author, 0, now, /* courseAreaId */ null);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogStartTimeEvent timeEvent = (RaceLogStartTimeEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), timeEvent.getLogicalTimePoint());
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

        RaceLogFlagEvent event = new RaceLogFlagEventImpl(now, author, 0, Flags.FIRSTSUBSTITUTE, Flags.NONE, true);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFlagEvent flagEvent = (RaceLogFlagEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), flagEvent.getLogicalTimePoint());
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
        RaceLogCourseDesignChangedEvent event = new RaceLogCourseDesignChangedEventImpl(now, author, 0, course, CourseDesignerMode.ADMIN_CONSOLE);

        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);

        RaceLog loadedRaceLog = retrieveRaceLog();

        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogCourseDesignChangedEvent courseDesignEvent = (RaceLogCourseDesignChangedEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), courseDesignEvent.getLogicalTimePoint());
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
        RaceLogWindFixEvent event = new RaceLogWindFixEventImpl(now, author, 0, wind, /* isMagnetic */ false);
        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogWindFixEvent windEvent = (RaceLogWindFixEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), windEvent.getLogicalTimePoint());
            assertEquals(event.getPassId(), windEvent.getPassId());
            assertEquals(event.getId(), windEvent.getId());
            compareWind(event.getWindFix(), windEvent.getWindFix());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }

    @Test
    public void storeDependentStartTimeEvent() throws UnknownHostException, MongoException, InterruptedException {
        TimePoint now = MillisecondsTimePoint.now();
        final String PARENT = "parent";
        final String COLUMN = "column";
        final String FLEET = "fleet";
        final SimpleRaceLogIdentifier srli = new SimpleRaceLogIdentifierImpl(PARENT, COLUMN, FLEET);
        RaceLogDependentStartTimeEvent event = new RaceLogDependentStartTimeEventImpl(
                now, author, 0, srli, Duration.ONE_MINUTE, /* courseAreaId */ null);
        addAndStoreRaceLogEvent(leaderboard, raceColumnName, event);
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogDependentStartTimeEvent dependentStartTimeEvent = (RaceLogDependentStartTimeEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), dependentStartTimeEvent.getLogicalTimePoint());
            assertEquals(event.getPassId(), dependentStartTimeEvent.getPassId());
            assertEquals(event.getId(), dependentStartTimeEvent.getId());
            assertEquals(event.getStartTimeDifference(), dependentStartTimeEvent.getStartTimeDifference());
            assertEquals(event.getDependentOnRaceIdentifier(), dependentStartTimeEvent.getDependentOnRaceIdentifier());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
}
