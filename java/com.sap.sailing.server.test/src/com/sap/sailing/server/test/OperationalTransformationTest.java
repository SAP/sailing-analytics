package com.sap.sailing.server.test;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.common.DefaultLeaderboardName;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.mongodb.MongoDBService;
import com.sap.sailing.operationaltransformation.Peer;
import com.sap.sailing.operationaltransformation.Peer.Role;
import com.sap.sailing.operationaltransformation.PeerImpl;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboard;
import com.sap.sailing.server.operationaltransformation.MoveLeaderboardColumnUp;
import com.sap.sailing.server.operationaltransformation.OperationalTransformer;
import com.sap.sailing.server.operationaltransformation.RacingEventServiceOperation;
import com.sap.sailing.server.operationaltransformation.RemoveLeaderboard;

public class OperationalTransformationTest {
    private static final String LEADERBOARDNAME = "TESTBOARD";

    private RacingEventService racingEventServiceServer;
    private RacingEventService racingEventServiceReplica;
    private Peer<RacingEventServiceOperation, RacingEventService> server;
    private Peer<RacingEventServiceOperation, RacingEventService> replica;

    @Before
    public void setUp() {
        // ensure that leaderboards will be loaded from and stored to the test database which is 
        MongoDBService.INSTANCE.getDB().dropDatabase();
        racingEventServiceServer = new RacingEventServiceImpl();
        racingEventServiceReplica = new RacingEventServiceImpl();
        OperationalTransformer transformer = new OperationalTransformer();
        server = new PeerImpl<>(transformer, racingEventServiceServer, Role.SERVER);
        replica = new PeerImpl<>(transformer, racingEventServiceReplica, Role.CLIENT);
        // wire the peers:
        server.addPeer(replica);
        replica.addPeer(server);
    }

    @Test
    public void testAddLeaderboard() {
        RacingEventServiceOperation addLeaderboardOp = new CreateLeaderboard(LEADERBOARDNAME, new int[] { 5 });
        server.apply(addLeaderboardOp);
        server.waitForNotRunning();
        replica.waitForNotRunning();
        Map<String, Leaderboard> replicaLeaderboards = racingEventServiceReplica.getLeaderboards();
        assertEquals(2, replicaLeaderboards.size()); // expected to include the default leaderboard
        assertEquals(new HashSet<String>(Arrays.asList(new String[] { DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME, LEADERBOARDNAME })),
                replicaLeaderboards.keySet());
        assertEquals(racingEventServiceServer.getLeaderboards().keySet(), replicaLeaderboards.keySet());
    }

    @Test
    public void testAddColumnToLeaderboardOnClientAndRemoveLeaderboardOnServer() throws InterruptedException {
        RacingEventServiceOperation addLeaderboardColumn = new AddColumnToLeaderboard(
                "newColumn", DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME, /* medalRace */ true);
        server.apply(addLeaderboardColumn);
        RacingEventServiceOperation removeDefaultLeaderboard = new RemoveLeaderboard(DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME);
        replica.apply(removeDefaultLeaderboard);
        replica.waitForNotRunning();
        server.waitForNotRunning();
        assertEquals(0, racingEventServiceReplica.getLeaderboards().size());
        assertEquals(0, racingEventServiceServer.getLeaderboards().size());
    }

    @Test
    public void testAddOneColumnOnEachSideThenMoveOneUpOnServerAndRemoveLeaderboardOnClient() throws InterruptedException {
        RacingEventServiceOperation addLeaderboardColumnOnServer = new AddColumnToLeaderboard(
                "newColumn1", DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME, /* medalRace */ true);
        server.apply(addLeaderboardColumnOnServer);
        RacingEventServiceOperation addLeaderboardColumnOnReplica = new AddColumnToLeaderboard(
                "newColumn2", DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME, /* medalRace */ true);
        replica.apply(addLeaderboardColumnOnReplica);
        replica.waitForNotRunning();
        server.waitForNotRunning();
        RacingEventServiceOperation moveUpNewColumn2 = new MoveLeaderboardColumnUp(DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME, "newColumn2");
        server.apply(moveUpNewColumn2);
        RacingEventServiceOperation removeDefaultLeaderboard = new RemoveLeaderboard(DefaultLeaderboardName.DEFAULT_LEADERBOARD_NAME);
        replica.apply(removeDefaultLeaderboard);
        replica.waitForNotRunning();
        server.waitForNotRunning();
        assertEquals(0, racingEventServiceReplica.getLeaderboards().size());
        assertEquals(0, racingEventServiceServer.getLeaderboards().size());
    }
}
