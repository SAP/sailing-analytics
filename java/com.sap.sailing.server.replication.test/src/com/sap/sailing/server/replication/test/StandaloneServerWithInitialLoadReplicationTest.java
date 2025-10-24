package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.impl.SailingServerConfigurationImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.operationaltransformation.UpdateServerConfiguration;
import com.sap.sailing.server.replication.test.ConnectionResetAndReconnectTest.MasterReplicationDescriptorMock;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.replication.ReplicationMasterDescriptor;

public class StandaloneServerWithInitialLoadReplicationTest extends AbstractServerReplicationTest {
    private static final Logger logger = Logger.getLogger(StandaloneServerWithInitialLoadReplicationTest.class.getName());
    
    public StandaloneServerWithInitialLoadReplicationTest() {
        super(new AbstractServerReplicationTest.ServerReplicationTestSetUp() {
            /**
             * Don't start replication immediately during set-up
             */
            @Override
            public void setUp() throws Exception {
                try {
                    Pair<ReplicationServiceTestImpl<RacingEventService>, ReplicationMasterDescriptor> result = basicSetUp(
                            /* dropDB */true,
                            /* master=null means create a new one */null, /* replica=null means create a new one */null);
                    masterDescriptor = MasterReplicationDescriptorMock.from(result.getB());
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Exception during set up", e);
                    tearDown();
                }
            }
        });
    }
    
    /**
     * See https://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=5600
     */
    @Test
    public void testInitialLoadReplicationOfStandaloneServer() throws Exception {
        master.apply(new UpdateServerConfiguration(new SailingServerConfigurationImpl(/* isStandalone */ true)));
        Thread.sleep(1000); // wait 1s for JMS to deliver any recovered messages; there should be none
        assertFalse(replica.getSailingServerConfiguration().isStandaloneServer());
        testSetUp.getReplicaReplicator().startToReplicateFrom(testSetUp.getMasterDescriptor());
        assertFalse(replica.isCurrentlyFillingFromInitialLoad());
        assertTrue(replica.getSailingServerConfiguration().isStandaloneServer());
    }
}
