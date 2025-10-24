package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFlagEventImpl;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.racelog.Flags;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.operationaltransformation.OperationalTransformer;
import com.sap.sse.operationaltransformation.Peer;
import com.sap.sse.operationaltransformation.Peer.Role;
import com.sap.sse.operationaltransformation.PeerImpl;


public class RaceLogInRaceColumnTest {

    private static final String LEADERBOARDNAME = "TESTBOARD";

    private RacingEventService racingEventServiceServer;
    private Peer<RacingEventServiceOperation<?>, RacingEventService> server;
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);

    @BeforeEach
    public void setUp() {
        MongoDBService.INSTANCE.getDB().drop();
        racingEventServiceServer = new RacingEventServiceImpl();
        OperationalTransformer<RacingEventService, RacingEventServiceOperation<?>> transformer = new OperationalTransformer<>();
        server = new PeerImpl<>(transformer, racingEventServiceServer, Role.SERVER);
    }
    
    @AfterEach
    public void tearDown() {
        server.shutdown();
    }

    @Test
    public void testAddEventToSingleRaceLog() throws InterruptedException {
        String raceColumnName = "myRaceColumn";
        RacingEventServiceOperation<FlexibleLeaderboard> addLeaderboardOp =
                new CreateFlexibleLeaderboard(
                        LEADERBOARDNAME, LEADERBOARDNAME, new int[] { 5 }, new LowPoint(), null);
        server.apply(addLeaderboardOp);
        RacingEventServiceOperation<RaceColumn> addLeaderboardColumn = 
                new AddColumnToLeaderboard(
                        raceColumnName, LEADERBOARDNAME, true);
        server.apply(addLeaderboardColumn);
        TimePoint t1 = MillisecondsTimePoint.now();
        RaceLogEvent rlEvent = new RaceLogFlagEventImpl(
                t1, author, 0, Flags.CLASS, Flags.NONE, true);
        Leaderboard leaderboard = racingEventServiceServer.getLeaderboardByName(LEADERBOARDNAME);
        RaceColumn raceColumn = leaderboard.getRaceColumnByName(raceColumnName);
        Fleet fleet = raceColumn.getFleetByName("Default");
        RaceLog raceLog = raceColumn.getRaceLog(fleet);
        raceLog.add(rlEvent);
        // TODO: Why do we have to sleep?
        Thread.sleep(1000);
        try {
            raceLog.lockForRead();
            assertEquals(rlEvent, raceLog.getFirstRawFix());
        } finally {
            raceLog.unlockAfterRead();
        }
    }
}
