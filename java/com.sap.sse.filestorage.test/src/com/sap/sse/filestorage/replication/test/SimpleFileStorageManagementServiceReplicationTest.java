package com.sap.sse.filestorage.replication.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.NoCorrespondingServiceRegisteredException;
import com.sap.sse.filestorage.FileStorageService;
import com.sap.sse.filestorage.test.util.Util;
import com.sap.sse.filestorage.testsupport.DummyFileStorageService;

public class SimpleFileStorageManagementServiceReplicationTest extends
        AbstractFileStorageManagementServiceReplicationTest {

    @Test
    public void testReplicationOfExistingActiveServiceAndProperties() throws InterruptedException,
            IllegalAccessException {
        FileStorageService serviceOnMaster = master.getActiveFileStorageService();

        // see if initial load worked
        FileStorageService serviceOnReplica = replica.getActiveFileStorageService();
        assertThat("active service on replica same as on master after initial load", serviceOnReplica.getName(),
                equalTo(serviceOnMaster.getName()));
        assertThat("service property on replica set after initial load",
                Util.findProperty(serviceOnReplica, DummyFileStorageService.PROPERTY_NAME).getValue(), equalTo("123"));

        // now change things and see if replication works
        master.setActiveFileStorageService(null);
        master.setFileStorageServiceProperty(serviceOnMaster, DummyFileStorageService.PROPERTY_NAME, "456");

        replicaReplicator.waitUntilQueueIsEmpty();
        Thread.sleep(3000);

        Assertions.assertThrows(NoCorrespondingServiceRegisteredException.class, ()->replica.getActiveFileStorageService());
        assertThat("service property on replica set after replication",
                Util.findProperty(serviceOnReplica, DummyFileStorageService.PROPERTY_NAME).getValue(), equalTo("456"));
    }
}
