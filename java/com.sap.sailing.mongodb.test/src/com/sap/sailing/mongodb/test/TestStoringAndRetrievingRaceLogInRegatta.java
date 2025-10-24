package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.Test;

import com.mongodb.BasicDBList;
import com.mongodb.MongoException;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
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
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
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
import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.TripleSerializer;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

public class TestStoringAndRetrievingRaceLogInRegatta extends AbstractTestStoringAndRetrievingRaceLogInRegatta {
    public TestStoringAndRetrievingRaceLogInRegatta() throws UnknownHostException, MongoException {
        super();
    }

    private void addAndStoreRaceLogEvent(Regatta regatta, String raceColumnName, RaceLogEvent event) {
        Series series = regatta.getSeriesByName(seriesName);
        Fleet fleet = series.getFleetByName(yellowFleetName);
        RaceColumn raceColumn = series.getRaceColumnByName(raceColumnName);
        raceColumn.getRaceLog(fleet).add(event);
        
        mongoObjectFactory.storeRegatta(regatta);
    }

    private RaceLog retrieveRaceLog() {
        Regatta loadedRegatta = domainObjectFactory.loadRegatta(regatta.getName(), /* trackedRegattaRegistry */ null);
        Series loadedSeries = loadedRegatta.getSeriesByName(seriesName);
        Fleet loadedFleet = loadedSeries.getFleetByName(yellowFleetName);
       
        return loadedSeries.getRaceColumnByName(raceColumnName).getRaceLog(loadedFleet);
    }
    
    @Override
    protected List<Fleet> createQualifyingFleets() {
        List<Fleet> qualifyingFleets = new ArrayList<Fleet>();
        qualifyingFleets.add(new FleetImpl(yellowFleetName));
        return qualifyingFleets;
    }
    
    @Test
    public void testStoreAndRetrieveRegattaWithRaceLogProtestStartTimeEvent() {
        TimePoint now = MillisecondsTimePoint.now();
        TimeRange protestTime = new TimeRangeImpl(now, now.plus(Duration.ONE_MINUTE.times(90)));
        RaceLogProtestStartTimeEvent expectedEvent = new RaceLogProtestStartTimeEventImpl(now, author, 0, protestTime);
        addAndStoreRaceLogEvent(regatta, raceColumnName, expectedEvent);
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
    public void testStoreAndRetrieveRegattaWithRaceLogStartProcedureChangedEvent() {        
        RaceLogStartProcedureChangedEvent expectedEvent = new RaceLogStartProcedureChangedEventImpl(now, author, 0, RacingProcedureType.ESS);
        addAndStoreRaceLogEvent(regatta, raceColumnName, expectedEvent);
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
    public void testStoreAndRetrieveRegattaWithRaceLogPathfinderEvent() {        
        RaceLogPathfinderEvent expectedEvent = new RaceLogPathfinderEventImpl(now, author, 0, "GER 20");

       addAndStoreRaceLogEvent(regatta, raceColumnName, expectedEvent);

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
    public void testStoreAndRetrieveRegattaWithRaceLogGateLineOpeningTimeEvent() {        
        RaceLogGateLineOpeningTimeEvent expectedEvent = new RaceLogGateLineOpeningTimeEventImpl(now, author, 0, 1234l, 54321l);

       addAndStoreRaceLogEvent(regatta, raceColumnName, expectedEvent);

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
    public void testStoreAndRetrieveRegattaWithRaceLogPassChangeEvent() {        
        RaceLogPassChangeEvent event = new RaceLogPassChangeEventImpl(now, author, 0);

        addAndStoreRaceLogEvent(regatta, raceColumnName, event);

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
    public void testStoreAndRetrieveRegattaWithRaceLogFinishPositioningListChangeEvent() {
        Competitor storedCompetitor = DomainFactory.INSTANCE.getOrCreateCompetitor(UUID.randomUUID(), "SAP Extreme Sailing Team", "SAP", Color.RED, 
                "someone@nowhere.de", null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat storedBoat = DomainFactory.INSTANCE.getOrCreateBoat(UUID.randomUUID(), "SAP Extreme Sailing Team",
                new BoatClassImpl("X40", false), "123", Color.RED, /* storePersistently */ true);
        CompetitorResults storedPositioningList = new CompetitorResultsImpl();
        storedPositioningList.add(new CompetitorResultImpl(storedCompetitor.getId(), storedCompetitor.getName(), storedCompetitor.getShortName(),
                storedBoat.getName(), storedBoat.getSailID(), /* rank */ 1, MaxPointsReason.NONE, /* score */ null, /* finishingTime */ null,
                /* comment */ null, MergeState.OK));
        RaceLogFinishPositioningListChangedEvent event = new RaceLogFinishPositioningListChangedEventImpl(now, author, 0, storedPositioningList);
        addAndStoreRaceLogEvent(regatta, raceColumnName, event);
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningListChangedEvent loadedPositioningEvent = (RaceLogFinishPositioningListChangedEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), loadedPositioningEvent.getLogicalTimePoint());
            assertEquals(event.getPassId(), loadedPositioningEvent.getPassId());
            assertEquals(event.getId(), loadedPositioningEvent.getId());
            assertEquals(event.getInvolvedCompetitors().size(), loadedPositioningEvent.getInvolvedCompetitors().size());
            assertCompetitorResultsEqual(event.getPositionedCompetitorsIDsNamesMaxPointsReasons(), loadedPositioningEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveRegattaWithRaceLogFinishPositioningConfirmedEvent() {   
        Competitor storedCompetitor = DomainFactory.INSTANCE.getOrCreateCompetitor(UUID.randomUUID(), "SAP Extreme Sailing Team", "SAP", Color.RED, 
                "someone@nowhere.de", null, null, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null, /* storePersistently */ true);
        Boat storedBoat = DomainFactory.INSTANCE.getOrCreateBoat(UUID.randomUUID(), "SAP Extreme Sailing Team",
                new BoatClassImpl("X40", false), "123", Color.RED, /* storePersistently */ true);
        CompetitorResults storedPositioningList = new CompetitorResultsImpl();
        storedPositioningList.add(new CompetitorResultImpl(storedCompetitor.getId(), storedCompetitor.getName(), storedCompetitor.getShortName(),
                storedBoat.getName(), storedBoat.getSailID(), /* rank */ 1, MaxPointsReason.NONE, /* score */ null, /* finishingTime */ null,
                /* comment */ null, MergeState.OK));
        RaceLogFinishPositioningConfirmedEvent event = new RaceLogFinishPositioningConfirmedEventImpl(now, author, 0, storedPositioningList);
        addAndStoreRaceLogEvent(regatta, raceColumnName, event);
        RaceLog loadedRaceLog = retrieveRaceLog();
        loadedRaceLog.lockForRead();
        try {
            RaceLogEvent loadedEvent = loadedRaceLog.getFirstRawFix();
            RaceLogFinishPositioningConfirmedEvent loadedConfirmedEvent = (RaceLogFinishPositioningConfirmedEvent) loadedEvent;
            assertEquals(event.getLogicalTimePoint(), loadedConfirmedEvent.getLogicalTimePoint());
            assertEquals(event.getPassId(), loadedConfirmedEvent.getPassId());
            assertEquals(event.getId(), loadedConfirmedEvent.getId());
            assertEquals(event.getInvolvedCompetitors().size(), loadedConfirmedEvent.getInvolvedCompetitors().size());
            assertCompetitorResultsEqual(event.getPositionedCompetitorsIDsNamesMaxPointsReasons(), loadedConfirmedEvent.getPositionedCompetitorsIDsNamesMaxPointsReasons());
            assertEquals(1, Util.size(loadedRaceLog.getFixes()));
        } finally {
            loadedRaceLog.unlockAfterRead();
        }
    }
    
    @Test
    public void testStoreAndRetrieveRegattaWithBackwardsCompatibleRaceLogFinishPositioningConfirmedEvent() {
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
        Series series = regatta.getSeriesByName(seriesName);
        Fleet fleet = series.getFleetByName(yellowFleetName);
        RaceColumn raceColumn = series.getRaceColumnByName(raceColumnName);
        mongoObjectFactory.storeRegatta(regatta);
        
        Document result = new Document();
        result.put(FieldNames.TIME_AS_MILLIS.name(), now.asMillis());
        result.put(FieldNames.RACE_LOG_EVENT_CREATED_AT.name(), now.asMillis());
        result.put(FieldNames.RACE_LOG_EVENT_ID.name(), UUID.randomUUID());
        result.put(FieldNames.RACE_LOG_EVENT_PASS_ID.name(), 0);
        result.put(FieldNames.RACE_LOG_EVENT_INVOLVED_BOATS.name(), new BasicDBList());
        result.put(FieldNames.RACE_LOG_EVENT_CLASS.name(), RaceLogFinishPositioningConfirmedEvent.class.getSimpleName());
        Document raceLogResult = new Document();
        raceLogResult.put(FieldNames.RACE_LOG_IDENTIFIER.name(), TripleSerializer.serialize(raceColumn.getRaceLogIdentifier(fleet).getIdentifier()));       
        raceLogResult.put(FieldNames.RACE_LOG_EVENT.name(), result);
        MongoObjectFactoryImpl factoryImpl = (MongoObjectFactoryImpl) mongoObjectFactory;
        factoryImpl.getRaceLogCollection().insertOne(raceLogResult);
    }

    @Test
    public void testStoreAndRetrieveRegattaWithRaceLogRaceStatusEvent() {

        RaceLogRaceStatusEvent event = new RaceLogRaceStatusEventImpl(now, author, 0, RaceLogRaceStatus.SCHEDULED);

        addAndStoreRaceLogEvent(regatta, raceColumnName, event);

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
    public void testStoreAndRetrieveRegattaWithRaceLogStartTimeEvent() {

        RaceLogStartTimeEvent event = new RaceLogStartTimeEventImpl(now, author, 0, now, /* courseAreaId */ null);

        addAndStoreRaceLogEvent(regatta, raceColumnName, event);

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
    public void testStoreAndRetrieveRegattaWithRaceLogFlagEvent() {

        RaceLogFlagEvent event = new RaceLogFlagEventImpl(now, author, 0, Flags.FIRSTSUBSTITUTE, Flags.NONE, true);

        addAndStoreRaceLogEvent(regatta, raceColumnName, event);

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
    public void testStoreAndRetrieveRegattaWithRaceLogCourseDesignChangedEvent() {
        CourseBase course = createCourseBase();
        RaceLogCourseDesignChangedEvent event = new RaceLogCourseDesignChangedEventImpl(now, author, 0, course, CourseDesignerMode.ADMIN_CONSOLE);

        addAndStoreRaceLogEvent(regatta, raceColumnName, event);

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
    public void testStoreAndRetrieveRegattaWithRaceLogWindFixEvent() {
        Wind wind = createWindFix();
        RaceLogWindFixEvent event = new RaceLogWindFixEventImpl(now, author, 0, wind, /* isMagnetic */ false);

        addAndStoreRaceLogEvent(regatta, raceColumnName, event);

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

}
