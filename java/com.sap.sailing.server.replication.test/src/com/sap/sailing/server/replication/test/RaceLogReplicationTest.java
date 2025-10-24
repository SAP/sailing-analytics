package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.UUID;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogCourseDesignChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogCourseDesignChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.CourseBase;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.CourseDataImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.CourseDesignerMode;
import com.sap.sailing.domain.common.MarkType;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.AddColumnToSeries;
import com.sap.sailing.server.operationaltransformation.CreateRegattaLeaderboard;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.AbstractColor;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceLogReplicationTest extends AbstractLogReplicationTest<RaceLog, RaceLogEvent, RaceLogEventVisitor> {
    
    private RaceLogEvent raceLogEvent;
    private RaceLogEvent anotherRaceLogEvent;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    
    @BeforeEach
    public void createEvents() throws Exception {
        raceLogEvent = new RaceLogRaceStatusEventImpl(new MillisecondsTimePoint(1), author, 42, RaceLogRaceStatus.UNKNOWN);
        anotherRaceLogEvent = new RaceLogRaceStatusEventImpl(new MillisecondsTimePoint(2), author, 42, RaceLogRaceStatus.UNKNOWN);
    }
    
    @Test
    public void testRaceLogEmptyOnInitialLoad() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        Regatta regatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RaceLog masterLog = setupRaceColumn(regatta, seriesName, raceColumnName, fleetName);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(seriesName, fleetName, raceColumnName, regatta);
        addAndValidateEventIds(masterLog, replicaLog);
    }
    
    @Test
    public void testRaceLogStateOnInitialLoad() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        Regatta masterRegatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RaceLog masterLog = setupRaceColumn(masterRegatta, seriesName, raceColumnName, fleetName);
        masterLog.add(raceLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(seriesName, fleetName, raceColumnName, masterRegatta);
        addAndValidateEventIds(masterLog, replicaLog);
    }
    
    @Test
    public void testRaceEventReplicationOnEmptyRegatta() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        Regatta masterRegatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RaceLog masterLog = setupRaceColumn(masterRegatta, seriesName, raceColumnName, fleetName);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(seriesName, fleetName, raceColumnName, masterRegatta);
        addAndValidateEventIds(masterLog, replicaLog, raceLogEvent);
    }
    
    @Test
    public void testRaceEventReplicationOnEmptyFlexibleLeaderboard() throws Exception {
        final String leaderboardName = "Test";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        FlexibleLeaderboard masterLeaderboard = setupFlexibleLeaderboard(leaderboardName);
        RaceLog masterLog = setupRaceColumn(leaderboardName, fleetName, raceColumnName);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(fleetName, raceColumnName, masterLeaderboard);
        addAndValidateEventIds(masterLog, replicaLog, raceLogEvent);
    }

    @Test
    public void testRaceEventReplicationOnRegatta() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        Regatta masterRegatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RaceLog masterLog = setupRaceColumn(masterRegatta, seriesName, raceColumnName, fleetName);
        masterLog.add(raceLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(seriesName, fleetName, raceColumnName, masterRegatta);
        addAndValidateEventIds(masterLog, replicaLog, anotherRaceLogEvent);
    }
    
    @Test
    public void testRaceEventReplicationCourseDesignOnRegatta() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        Regatta masterRegatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RaceLog masterLog = setupRaceColumn(masterRegatta, seriesName, raceColumnName, fleetName);
        raceLogEvent = new RaceLogCourseDesignChangedEventImpl(MillisecondsTimePoint.now(), author, 43, createCourseData(), CourseDesignerMode.ADMIN_CONSOLE);
        masterLog.add(raceLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(seriesName, fleetName, raceColumnName, masterRegatta);
        anotherRaceLogEvent = new RaceLogCourseDesignChangedEventImpl(MillisecondsTimePoint.now(), author, 43, createCourseData(), CourseDesignerMode.ADMIN_CONSOLE);
        addAndValidateEventIds(masterLog, replicaLog, anotherRaceLogEvent);
        compareReplicatedCourseDesignEvent(replicaLog, (RaceLogCourseDesignChangedEvent) anotherRaceLogEvent);
    }
    
    @Test
    public void testRaceEventReplicationOnFlexibleLeaderboard() throws Exception {
        final String leaderboardName = "Test";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        FlexibleLeaderboard masterLeaderboard = setupFlexibleLeaderboard(leaderboardName);
        RaceLog masterLog = setupRaceColumn(leaderboardName, fleetName, raceColumnName);
        masterLog.add(raceLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(fleetName, raceColumnName, masterLeaderboard);
        addAndValidateEventIds(masterLog, replicaLog, anotherRaceLogEvent);
    }
    
    @Test
    public void testRaceEventReplicationCourseDesignOnFlexibleLeaderboard() throws Exception {
        final String leaderboardName = "Test";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        FlexibleLeaderboard masterLeaderboard = setupFlexibleLeaderboard(leaderboardName);
        RaceLog masterLog = setupRaceColumn(leaderboardName, fleetName, raceColumnName);
        raceLogEvent = new RaceLogCourseDesignChangedEventImpl(MillisecondsTimePoint.now(), author, 43, createCourseData(), CourseDesignerMode.ADMIN_CONSOLE);
        masterLog.add(raceLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(fleetName, raceColumnName, masterLeaderboard);
        anotherRaceLogEvent = new RaceLogCourseDesignChangedEventImpl(MillisecondsTimePoint.now(), author, 43, createCourseData(), CourseDesignerMode.ADMIN_CONSOLE);
        addAndValidateEventIds(masterLog, replicaLog, anotherRaceLogEvent);
        compareReplicatedCourseDesignEvent(replicaLog, (RaceLogCourseDesignChangedEvent) anotherRaceLogEvent);
    }
    
    private void compareReplicatedCourseDesignEvent(RaceLog replicaLog, RaceLogCourseDesignChangedEvent courseDesignChangedEvent) {
        replicaLog.lockForRead();
        try {
            RaceLogCourseDesignChangedEvent replicatedEvent = (RaceLogCourseDesignChangedEvent) replicaLog.getLastRawFix();
            assertEquals(courseDesignChangedEvent.getId(), replicatedEvent.getId());
            assertEquals(courseDesignChangedEvent.getPassId(), replicatedEvent.getPassId());
            assertEquals(courseDesignChangedEvent.getCreatedAt(), replicatedEvent.getCreatedAt());
            assertEquals(courseDesignChangedEvent.getLogicalTimePoint(), replicatedEvent.getLogicalTimePoint());
            assertEquals(Util.size(courseDesignChangedEvent.getInvolvedCompetitors()), Util.size(replicatedEvent.getInvolvedCompetitors()));
            compareCourseBase(courseDesignChangedEvent.getCourseDesign(), replicatedEvent.getCourseDesign());
        } finally {
            replicaLog.unlockAfterRead();
        }
    }
    
    /**
     * See bug 1666; a race log reload operation may not properly have been replicating. This tests asserts that when a race log event
     * has been added to the DB and then race log is re-loaded on the master, the events added through the DB also show up on the replica.
     */
    @Test
    public void testRaceLogReloadReplication() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        final String raceColumnName = "R1";
        Regatta masterRegatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RaceLog masterLog = setupRaceColumn(masterRegatta, seriesName, raceColumnName, fleetName);
        masterLog.add(raceLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RaceLog replicaLog = getReplicaLog(seriesName, fleetName, raceColumnName, masterRegatta);
        addAndValidateEventIds(masterLog, replicaLog);
        
        final Series series = masterRegatta.getSeries().iterator().next();
        final TimePoint approximateRaceStatusEventCreationTimePoint = MillisecondsTimePoint.now();
        final RaceLogRaceStatusEvent raceStatusEvent = new RaceLogRaceStatusEventImpl(approximateRaceStatusEventCreationTimePoint, author, 42,
                RaceLogRaceStatus.UNKNOWN);
        addEventToDB(series.getRaceColumnByName(raceColumnName).getRaceLogIdentifier(series.getFleetByName(fleetName)), raceStatusEvent, regattaName, raceColumnName, fleetName);
        RegattaLeaderboard leaderboard = master.apply(new CreateRegattaLeaderboard(
                masterRegatta.getRegattaIdentifier(), /* leaderboardDisplayName */ null, new int[0]));
        master.reloadRaceLog(leaderboard.getName(), raceColumnName, fleetName);
        Thread.sleep(3000);
        final RaceLog reloadedMasterLog = leaderboard.getRaceColumnByName(raceColumnName).getRaceLog(series.getFleetByName(fleetName));
        final RaceLogEvent lastEventReadFromMasterLog = reloadedMasterLog.getFirstRawFixAtOrAfter(approximateRaceStatusEventCreationTimePoint);
        assertNotNull(lastEventReadFromMasterLog);
        assertEqualsOnId(reloadedMasterLog, getReplicaLog(seriesName, fleetName, raceColumnName, masterRegatta));
    }

    private RaceLog getReplicaLog(final String fleetName, final String raceColumnName, Leaderboard leaderboard) {
        Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboard.getName());
        RaceColumn replicaColumn = replicaLeaderboard.getRaceColumnByName(raceColumnName);
        Fleet replicaFleet = replicaColumn.getFleetByName(fleetName);
        return replicaColumn.getRaceLog(replicaFleet);
    }

    private RaceLog getReplicaLog(final String seriesName, final String fleetName, final String raceColumnName,
            Regatta masterRegatta) {
        Regatta replicaRegatta = replica.getRegatta(masterRegatta.getRegattaIdentifier());
        RaceColumn replicaColumn = replicaRegatta.getSeriesByName(seriesName).getRaceColumnByName(raceColumnName);
        Fleet replicaFleet = replicaColumn.getFleetByName(fleetName);
        return replicaColumn.getRaceLog(replicaFleet);
    }

    private RaceLog setupRaceColumn(final Regatta regatta, final String seriesName, 
            final String raceColumnName, final String fleetName) {
        AddColumnToSeries addColumnOperation = new AddColumnToSeries(regatta.getRegattaIdentifier(), seriesName, raceColumnName);
        RaceColumn raceColumn = master.apply(addColumnOperation);
        Fleet masterFleet = raceColumn.getFleetByName(fleetName);
        return raceColumn.getRaceLog(masterFleet);
    }

    private RaceLog setupRaceColumn(final String leaderboardName, final String fleetName, final String raceColumnName) {
        AddColumnToLeaderboard addColumnOperation = new AddColumnToLeaderboard(raceColumnName, leaderboardName, false);
        RaceColumn masterRaceColumn = master.apply(addColumnOperation);
        Fleet masterFleet = masterRaceColumn.getFleetByName(fleetName);
        RaceLog masterLog = masterRaceColumn.getRaceLog(masterFleet);
        return masterLog;
    }
    
    protected CourseBase createCourseData() {
        CourseBase course = new CourseDataImpl("Test Course");
        course.addWaypoint(0, new WaypointImpl(new ControlPointWithTwoMarksImpl(UUID.randomUUID(), 
                new MarkImpl(UUID.randomUUID(), "Black", MarkType.BUOY, AbstractColor.getCssColor("black"), "round", "circle"),
                new MarkImpl(UUID.randomUUID(), "Green", MarkType.BUOY, AbstractColor.getCssColor("green"), "round", "circle"),
                "Upper gate", "Upper gate")));
        course.addWaypoint(1, new WaypointImpl(new MarkImpl(UUID.randomUUID(), "White", MarkType.BUOY, AbstractColor.getCssColor("white"), "conical", "bold"), PassingInstruction.Port));
        
        return course;
    }
    
    protected void compareCourseBase(CourseBase masterCourse, CourseBase replicatedCourse) {
        assertEquals(masterCourse.getFirstWaypoint().getPassingInstructions(), PassingInstruction.None);
        assertEquals(replicatedCourse.getFirstWaypoint().getPassingInstructions(), PassingInstruction.None);
        Assertions.assertTrue(masterCourse.getFirstWaypoint().getControlPoint() instanceof ControlPointWithTwoMarks);
        Assertions.assertTrue(replicatedCourse.getFirstWaypoint().getControlPoint() instanceof ControlPointWithTwoMarks);
        
        ControlPointWithTwoMarks masterGate = (ControlPointWithTwoMarks) masterCourse.getFirstWaypoint().getControlPoint();
        ControlPointWithTwoMarks replicatedGate = (ControlPointWithTwoMarks) replicatedCourse.getFirstWaypoint().getControlPoint();
        
        assertEquals(masterGate.getId(), replicatedGate.getId());
        assertEquals(masterGate.getName(), replicatedGate.getName());
        
        compareMarks(masterGate.getLeft(), replicatedGate.getLeft());
        compareMarks(masterGate.getRight(), replicatedGate.getRight());
        
        assertEquals(masterCourse.getLastWaypoint().getPassingInstructions(), PassingInstruction.Port);
        assertEquals(replicatedCourse.getLastWaypoint().getPassingInstructions(), PassingInstruction.Port);
        Assertions.assertTrue(masterCourse.getLastWaypoint().getControlPoint() instanceof Mark);
        Assertions.assertTrue(replicatedCourse.getLastWaypoint().getControlPoint() instanceof Mark);
        
        Mark masterMark = (Mark) masterCourse.getLastWaypoint().getControlPoint();
        Mark replicatedMark = (Mark) replicatedCourse.getLastWaypoint().getControlPoint();
        compareMarks(masterMark, replicatedMark);
    }
    
    private void compareMarks(Mark masterMark, Mark replicatedMark) {
        assertEquals(masterMark.getId(), replicatedMark.getId());
        assertEquals(masterMark.getColor(), replicatedMark.getColor());
        assertEquals(masterMark.getName(), replicatedMark.getName());
        assertEquals(masterMark.getPattern(), replicatedMark.getPattern());
        assertEquals(masterMark.getShape(), replicatedMark.getShape());
        assertEquals(masterMark.getType(), replicatedMark.getType());
    }
    
}
