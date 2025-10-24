package com.sap.sse.replication.testsupport;

import java.io.IOException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.replication.Replicable;
import com.sap.sse.replication.ReplicationMasterDescriptor;
import com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp.ReplicationServiceTestImpl;

public abstract class AbstractServerWithSingleServiceReplicationTest<ReplicableInterface extends Replicable<?, ?>, ReplicableImpl extends ReplicableInterface> {
    protected final AbstractServerReplicationTestSetUp<ReplicableInterface, ReplicableImpl> testSetUp;
    protected ReplicableImpl replica;
    protected ReplicableImpl master;
    protected ReplicationServiceTestImpl<ReplicableInterface> replicaReplicator;
    protected ReplicationMasterDescriptor  masterDescriptor;
    
    public AbstractServerWithSingleServiceReplicationTest(AbstractServerReplicationTestSetUp<ReplicableInterface, ReplicableImpl> testSetUp) {
        this.testSetUp = testSetUp;
    }

    @BeforeEach
    public void setUp() throws Exception {
        testSetUp.setUp();
        setUpDescriptorsFromTestSetup();
    }

    private void setUpDescriptorsFromTestSetup() {
        master = testSetUp.getMaster();
        replica = testSetUp.getReplica();
        replicaReplicator = testSetUp.getReplicaReplicator();
        masterDescriptor = testSetUp.getMasterDescriptor();
    }
    
    protected Pair<ReplicationServiceTestImpl<ReplicableInterface>, ReplicationMasterDescriptor> basicSetUp(boolean dropDB,
            ReplicableImpl master, ReplicableImpl replica) throws Exception {
        final Pair<ReplicationServiceTestImpl<ReplicableInterface>, ReplicationMasterDescriptor> result = testSetUp
                .basicSetUp(dropDB, master, replica);
        setUpDescriptorsFromTestSetup();
        return result;
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        testSetUp.tearDown();
    }
    
    protected ReplicableImpl createNewMaster() throws Exception {
        return testSetUp.createNewMaster(testSetUp.getSecurityServiceTrackerMock());
    }
    
    protected ReplicableImpl createNewReplica() throws Exception {
        return testSetUp.createNewReplica(testSetUp.getSecurityServiceTrackerMock());
    }
    
    protected void persistenceSetUp(boolean dropDB) {
        testSetUp.persistenceSetUp(dropDB);
    }
    
    protected void persistenceTearDown() {
        testSetUp.persistenceTearDown();
    }
    
    public void stopReplicatingToMaster() throws IOException {
        testSetUp.stopReplicatingToMaster();
    }
}
