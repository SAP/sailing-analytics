package com.sap.sailing.server.testsupport;

import java.net.UnknownHostException;
import java.util.Map;
import java.util.Set;

import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.impl.DataImportProgressImpl;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.racelog.tracking.MongoSensorFixStoreFactory;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.DataImportLockWithProgress;
import com.sap.sse.common.TypeBasedServiceFinderFactory;

/**
 * Use an empty anonymous inner subclass for instantiation to ensure that you'll have the right classpath for
 * your bundle in order to carry out de-serializations in the correct context. Example:
 * {@code new RacingEventServiceImplMock(){}}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class RacingEventServiceImplMock extends RacingEventServiceImpl {
    private DataImportLockWithProgress lock;

    public RacingEventServiceImplMock() {
        super(/* clearPersistentCompetitorStore */ false, /* sensorFixStore */ null, null, /* restoreTrackedRaces */ false);
    }
    
    public RacingEventServiceImplMock(DataImportProgressImpl dataImportProgressImpl,
            TypeBasedServiceFinderFactory serviceFinderFactory) throws UnknownHostException, MongoException {
        super(null, MongoSensorFixStoreFactory.INSTANCE.getMongoGPSFixStore(
                PersistenceFactory.INSTANCE
                .getDefaultMongoObjectFactory(serviceFinderFactory), PersistenceFactory.INSTANCE
                .getDefaultDomainObjectFactory(), serviceFinderFactory, ReadConcern.MAJORITY, WriteConcern.MAJORITY), serviceFinderFactory);
        lock = new DataImportLockWithProgress();
        lock.addProgress(dataImportProgressImpl.getOperationId(), dataImportProgressImpl);
    }

    public RacingEventServiceImplMock(DataImportProgressImpl dataImportProgressImpl) {
        lock = new DataImportLockWithProgress();
        lock.addProgress(dataImportProgressImpl.getOperationId(), dataImportProgressImpl);
    }

    public Map<Regatta, Set<RaceTracker>> getRaceTrackersByRegattaMap() {
        return raceTrackersByRegatta;
    }

    public Map<Object, RaceTracker> getRaceTrackersByIDMap() {
        return raceTrackersByID;
    }

    public Map<String, Regatta> getRegattasByName() {
        return regattasByName;
    }

    @Override
    public DataImportLockWithProgress getDataImportLock() {
        return lock;
    }
}
