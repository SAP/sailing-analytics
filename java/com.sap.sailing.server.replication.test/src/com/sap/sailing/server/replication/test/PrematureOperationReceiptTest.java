package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.io.IOException;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.impl.ReplicationReceiverImpl;

public class PrematureOperationReceiptTest extends AbstractServerReplicationTest {
    private static ReplicationReceiverImpl replicator;
    
    public PrematureOperationReceiptTest() {
        super(new ServerReplicationTestSetUp());
    }

    private static class ServerReplicationTestSetUp extends AbstractServerReplicationTest.ServerReplicationTestSetUp {
        @Override
        public void setUp() throws Exception {
            Pair<ReplicationServiceTestImpl<RacingEventService>, ReplicationMasterDescriptor> result =
                    basicSetUp(/* dropDB */ true, /* master=null means create a new one */ null, /* replica=null means create a new one */ null);
            replicator = replicaReplicator.startToReplicateFromButDontYetFetchInitialLoad(result.getB(), /* startReplicatorSuspended */ true);
        }
    }

    /**
     * If JMS replication starts and the initial load hasn't yet been fully received and installed into the server,
     * the JMS-transported operations may not yet be applicable on the replica's side. This test forces such a situation
     * by first creating a leaderboard, then starting the replication but holding back the fetching of the initial load,
     * then creates a race column in this leaderboard which will be sent as an operation through JMS to the replica which
     * hasn't yet received the initial load with the leaderboard. Trying to apply will fail unless the replicator is
     * started in suspended mode and resumed only after the initial load was successfully installed.
     */
    @Test
    public void testRaceColumnInLeaderboardReplicationAfterInitialLoad() throws InterruptedException, ClassNotFoundException, IOException, IllegalAccessException {
        final String leaderboardName = "My new leaderboard";
        final int[] discardThresholds = new int[] { 17, 23 };
        CreateFlexibleLeaderboard createTestLeaderboard = new CreateFlexibleLeaderboard(leaderboardName, null, discardThresholds, new LowPoint(), null);
        assertNull(master.getLeaderboardByName(leaderboardName));
        replicaReplicator.initialLoad(); // serialize the master state before the operation has been applied
        master.apply(createTestLeaderboard);
        final Leaderboard masterLeaderboard = master.getLeaderboardByName(leaderboardName);
        assertNotNull(masterLeaderboard);
        Thread.sleep(1000); // wait 1s for JMS to deliver the message and the message to be applied
        {
            final Leaderboard nonExistingReplicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
            assertNull(nonExistingReplicaLeaderboard); // because replicator is still suspended
        }
        {
            final Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
            assertNull(replicaLeaderboard); // because replicator is still suspended
        }
        replicator.setSuspended(false);
        synchronized (replicator) {
            while (!replicator.isQueueEmptyOrStopped()) {
                replicator.wait();
            }
        }
        {
            final Leaderboard replicaLeaderboard = replica.getLeaderboardByName(leaderboardName);
            assertNotNull(replicaLeaderboard);
        }
    }

}
