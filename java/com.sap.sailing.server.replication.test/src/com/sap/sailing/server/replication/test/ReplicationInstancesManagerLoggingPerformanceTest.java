package com.sap.sailing.server.replication.test;

import static org.junit.Assert.assertEquals;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Collections;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.AbstractLeaderboardGroupOperation;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sse.replication.ReplicaDescriptor;
import com.sap.sse.replication.impl.ReplicaDescriptorImpl;
import com.sap.sse.replication.impl.ReplicationInstancesManager;

public class ReplicationInstancesManagerLoggingPerformanceTest {
    private ReplicationInstancesManager replicationInstanceManager;
    private AbstractLeaderboardGroupOperation<LeaderboardGroup> operation;
    private ReplicaDescriptor replica;

    @Before
    public void setUp() throws UnknownHostException {
        replicationInstanceManager = new ReplicationInstancesManager();
        replica = new ReplicaDescriptorImpl(InetAddress.getLocalHost(), UUID.randomUUID(), "", /* replicableIds */ new String[] { "Humba" });
        replicationInstanceManager.registerReplica(replica);
        UUID newGroupid = UUID.randomUUID();
        operation = new CreateLeaderboardGroup(newGroupid, "Test Leaderboard Group",
                "Description of Test Leaderboard Group", /* displayName */ null,
                /* displayGroupsInReverseOrder */ false,
                Arrays.asList(new String[] { "Default Leaderboard" }), /* overallLeaderboardDiscardThresholds */ null, /* overallLeaderboardScoringSchemeType */ null);
    }
    
    @Test
    public void testLoggingPerformance() {
        final int count = 10000000;
        for (int i=0; i<count; i++) {
            replicationInstanceManager.log(Collections.<Class<?>>singletonList(operation.getClass()), 0l);
        }
        assertEquals(count, replicationInstanceManager.getStatistics(replica).get(operation.getClass()).intValue());
        assertEquals(1.0, replicationInstanceManager.getAverageNumberOfOperationsPerMessage(replica), 0.0000001);
    }
    
    @Test
    public void testLoggingAverages() {
        final int count = 1000;
        for (int i=0; i<count; i++) {
            replicationInstanceManager.log(Arrays.asList(new Class<?>[] { operation.getClass(), operation.getClass() }), 0l);
        }
        assertEquals(2*count, replicationInstanceManager.getStatistics(replica).get(operation.getClass()).intValue());
        assertEquals(2.0, replicationInstanceManager.getAverageNumberOfOperationsPerMessage(replica), 0.0000001);
    }
}
