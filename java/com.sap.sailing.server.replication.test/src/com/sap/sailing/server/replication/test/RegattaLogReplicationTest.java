package com.sap.sailing.server.replication.test;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEvent;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLogEventVisitor;
import com.sap.sailing.domain.abstractlog.regatta.events.impl.RegattaLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.impl.RegattaImpl;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RegattaLogReplicationTest extends
        AbstractLogReplicationTest<RegattaLog, RegattaLogEvent, RegattaLogEventVisitor> {
    
    private RegattaLogEvent regattaLogEvent;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    
    private TimePoint t(long millis) {
        return new MillisecondsTimePoint(millis);
    }
    
    @BeforeEach
    public void createEvent() throws Exception {
        final CompetitorWithBoat competitor = TrackBasedTest.createCompetitorWithBoat("Test Competitor");
        regattaLogEvent = new RegattaLogRegisterCompetitorEventImpl(t(0), t(0), author, 0, competitor);
    }
    
    @Test
    public void testRegattaLogEmptyOnInitialLoad() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        Regatta regatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RegattaLog masterLog = regatta.getRegattaLog();
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RegattaLog replicaLog = getReplicaLog(regatta);
        addAndValidateEventIds(masterLog, replicaLog);
    }
    
    @Test
    public void testRegattaLogStateOnInitialLoad() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        Regatta regatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RegattaLog masterLog = regatta.getRegattaLog();
        masterLog.add(regattaLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RegattaLog replicaLog = getReplicaLog(regatta);
        addAndValidateEventIds(masterLog, replicaLog);
    }
    
    @Test
    public void testRaceEventReplicationOnEmptyRegatta() throws Exception {
        final String regattaName = "Test";
        final String seriesName = "Default";
        final String fleetName = "Default";
        Regatta masterRegatta = setupRegatta(RegattaImpl.getDefaultName(regattaName, BOAT_CLASS_NAME_49er), seriesName, fleetName, BOAT_CLASS_NAME_49er);
        RegattaLog masterLog = masterRegatta.getRegattaLog();
        masterLog.add(regattaLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RegattaLog replicaLog = getReplicaLog(masterRegatta);
        addAndValidateEventIds(masterLog, replicaLog, regattaLogEvent);
    }
    
    @Test
    public void testRaceEventReplicationOnEmptyFlexibleLeaderboard() throws Exception {
        final String leaderboardName = "Test";
        FlexibleLeaderboard masterLeaderboard = setupFlexibleLeaderboard(leaderboardName);
        RegattaLog masterLog = masterLeaderboard.getRegattaLog();
        masterLog.add(regattaLogEvent);
        replicaReplicator.startToReplicateFrom(masterDescriptor);
        RegattaLog replicaLog = getReplicaLog(masterLeaderboard);
        addAndValidateEventIds(masterLog, replicaLog, regattaLogEvent);
    }


    private RegattaLog getReplicaLog(Leaderboard leaderboard) {
        return ((FlexibleLeaderboard) replica.getLeaderboardByName(leaderboard.getName())).getRegattaLog();
    }

    private RegattaLog getReplicaLog(Regatta masterRegatta) {
        return replica.getRegatta(masterRegatta.getRegattaIdentifier()).getRegattaLog();
    }
}
