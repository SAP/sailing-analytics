package com.sap.sse.filestorage.replication.test;

import java.io.IOException;
import java.net.MalformedURLException;

import com.sap.sse.common.impl.SingleTypeBasedServiceFinderImpl;
import com.sap.sse.filestorage.FileStorageManagementService;
import com.sap.sse.filestorage.FileStorageService;
import com.sap.sse.filestorage.impl.EmptyFileStorageServicePropertyStoreImpl;
import com.sap.sse.filestorage.impl.FileStorageManagementServiceImpl;
import com.sap.sse.filestorage.testsupport.DummyFileStorageService;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.replication.FullyInitializedReplicableTracker;
import com.sap.sse.replication.testsupport.AbstractServerWithSingleServiceReplicationTest;
import com.sap.sse.security.SecurityService;

public abstract class AbstractFileStorageManagementServiceReplicationTest extends
        AbstractServerWithSingleServiceReplicationTest<FileStorageManagementService, FileStorageManagementServiceImpl> {
    public AbstractFileStorageManagementServiceReplicationTest() {
        super(new ServerReplicationTestSetUp());
    }

    private static class ServerReplicationTestSetUp extends com.sap.sse.replication.testsupport.AbstractServerReplicationTestSetUp<FileStorageManagementService, FileStorageManagementServiceImpl> {
        private MongoDBService mongoDBService;

        @Override
        protected void persistenceSetUp(boolean dropDB) {
            mongoDBService = MongoDBService.INSTANCE;
            if (dropDB) {
                mongoDBService.getDB().drop();
            }
        }

        protected FileStorageManagementServiceImpl createNewService() {
            return new FileStorageManagementServiceImpl(
                    new SingleTypeBasedServiceFinderImpl<>(new DummyFileStorageService(),
                            DummyFileStorageService.NAME), EmptyFileStorageServicePropertyStoreImpl.INSTANCE);
        }

        @Override
        public FileStorageManagementServiceImpl createNewMaster(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock) throws MalformedURLException, IOException,
        InterruptedException {
            FileStorageManagementServiceImpl result = createNewService();
            FileStorageService serviceOnMaster = result.getAvailableFileStorageServices()[0];
            result.setActiveFileStorageService(serviceOnMaster);
            result.setFileStorageServiceProperty(serviceOnMaster, DummyFileStorageService.PROPERTY_NAME, "123");
            return result;
        }

        @Override
        public FileStorageManagementServiceImpl createNewReplica(FullyInitializedReplicableTracker<SecurityService> securityServiceTrackerMock) {
            return createNewService();
        }

        @Override
        protected void persistenceTearDown() {
            mongoDBService.getDB().drop();
        }
    }
}
