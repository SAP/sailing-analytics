package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.operationaltransformation.MoveLeaderboardColumnUp;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.operationaltransformation.OperationalTransformer;
import com.sap.sse.operationaltransformation.Peer;
import com.sap.sse.operationaltransformation.Peer.Role;
import com.sap.sse.operationaltransformation.PeerImpl;

public class OperationalTransformationTest {
    private static final String LEADERBOARDNAME = "TESTBOARD";

    private RacingEventService racingEventServiceServer;
    private RacingEventService racingEventServiceReplica;
    private Peer<RacingEventServiceOperation<?>, RacingEventService> server;
    private Peer<RacingEventServiceOperation<?>, RacingEventService> replica;

    @BeforeEach
    public void setUp() {
        // ensure that leaderboards will be loaded from and stored to the test database which is 
        MongoDBService.INSTANCE.getDB().drop();
        racingEventServiceServer = new RacingEventServiceImpl();
        racingEventServiceReplica = new RacingEventServiceImpl();
        OperationalTransformer<RacingEventService, RacingEventServiceOperation<?>> transformer = new OperationalTransformer<>();
        server = new PeerImpl<>(transformer, racingEventServiceServer, Role.SERVER);
        replica = new PeerImpl<>(transformer, racingEventServiceReplica, Role.CLIENT);
        // wire the peers:
        server.addPeer(replica);
        replica.addPeer(server);
    }
    
    @AfterEach
    public void tearDown() {
        server.shutdown();
        replica.shutdown();
    }

    @Test
    public void testAddLeaderboard() {
        RacingEventServiceOperation<FlexibleLeaderboard> addLeaderboardOp = new CreateFlexibleLeaderboard(LEADERBOARDNAME,null, new int[] { 5 },
                new LowPoint(), null);
        server.apply(addLeaderboardOp);
        server.waitForNotRunning();
        replica.waitForNotRunning();
        Map<String, Leaderboard> replicaLeaderboards = racingEventServiceReplica.getLeaderboards();
        assertEquals(1, replicaLeaderboards.size());
        assertEquals(new HashSet<String>(Arrays.asList(new String[] { LEADERBOARDNAME })),
                replicaLeaderboards.keySet());
        assertEquals(racingEventServiceServer.getLeaderboards().keySet(), replicaLeaderboards.keySet());
    }

    @Test
    public void testAddColumnToLeaderboardOnServerAndRemoveLeaderboardOnClient() throws InterruptedException {
        RacingEventServiceOperation<FlexibleLeaderboard> addLeaderboardOp = new CreateFlexibleLeaderboard(LEADERBOARDNAME,null, new int[] { 5 },
                new LowPoint(), null);
        server.apply(addLeaderboardOp);
        RacingEventServiceOperation<RaceColumn> addLeaderboardColumn = new AddColumnToLeaderboard(
                "newColumn", LEADERBOARDNAME, /* medalRace */ true);
        server.apply(addLeaderboardColumn);
        RacingEventServiceOperation<Void> removeDefaultLeaderboard = new RemoveLeaderboard(LEADERBOARDNAME);
        replica.apply(removeDefaultLeaderboard);
        replica.waitForNotRunning();
        server.waitForNotRunning();
        assertEquals(0, racingEventServiceReplica.getLeaderboards().size());
        assertEquals(0, racingEventServiceServer.getLeaderboards().size());
    }

    @Test
    public void testAddOneColumnOnEachSideThenMoveOneUpOnServerAndRemoveLeaderboardOnClient() throws InterruptedException {
        RacingEventServiceOperation<FlexibleLeaderboard> addLeaderboardOp = new CreateFlexibleLeaderboard(LEADERBOARDNAME,null, new int[] { 5 },
                new LowPoint(), null);
        server.apply(addLeaderboardOp);
        RacingEventServiceOperation<RaceColumn> addLeaderboardColumnOnServer = new AddColumnToLeaderboard(
                "newColumn1", LEADERBOARDNAME, /* medalRace */ true);
        server.apply(addLeaderboardColumnOnServer);
        RacingEventServiceOperation<RaceColumn> addLeaderboardColumnOnReplica = new AddColumnToLeaderboard(
                "newColumn2", LEADERBOARDNAME, /* medalRace */ true);
        replica.apply(addLeaderboardColumnOnReplica);
        replica.waitForNotRunning();
        server.waitForNotRunning();
        RacingEventServiceOperation<Void> moveUpNewColumn2 = new MoveLeaderboardColumnUp(LEADERBOARDNAME, "newColumn2");
        server.apply(moveUpNewColumn2);
        RacingEventServiceOperation<Void> removeDefaultLeaderboard = new RemoveLeaderboard(LEADERBOARDNAME);
        replica.apply(removeDefaultLeaderboard);
        replica.waitForNotRunning();
        server.waitForNotRunning();
        assertEquals(0, racingEventServiceReplica.getLeaderboards().size());
        assertEquals(0, racingEventServiceServer.getLeaderboards().size());
    }
}
