package com.sap.sailing.domain.persistence.racelog.tracking;

import java.net.UnknownHostException;

import com.mongodb.MongoException;
import com.mongodb.ReadConcern;
import com.mongodb.WriteConcern;
import com.mongodb.client.ClientSession;
import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.racelog.tracking.impl.MongoSensorFixStoreFactoryImpl;
import com.sap.sse.common.TypeBasedServiceFinderFactory;

public interface MongoSensorFixStoreFactory {
    MongoSensorFixStoreFactory INSTANCE = new MongoSensorFixStoreFactoryImpl();

    /**
     * Gets the default Mongo store based on the properties mongo.hostname[=localhost], mongo.port[=27017]
     * and mongo.dbName[=&lt;contents of constant {@link #DEFAULT_DB_NAME}&gt;] specified in the bundle context
     */
    MongoSensorFixStore getMongoGPSFixStore(MongoObjectFactory mongoObjectFactory, DomainObjectFactory domainObjectFactory,
            TypeBasedServiceFinderFactory serviceFinderFactory)
                    throws UnknownHostException, MongoException;

    /**
     * Like {@link #getMongoGPSFixStore(MongoObjectFactory, DomainObjectFactory, TypeBasedServiceFinderFactory)}, but
     * additionally the write concern for the MongoDB writes can be controlled which otherwise usually defaults
     * to {@link WriteConcern#UNACKNOWLEDGED} for the fixes as well as the metadata.
     */
    MongoSensorFixStore getMongoGPSFixStore(MongoObjectFactory mongoObjectFactory, DomainObjectFactory domainObjectFactory,
            TypeBasedServiceFinderFactory serviceFinderFactory, ReadConcern readConcern, WriteConcern writeConcern)
                    throws UnknownHostException, MongoException;

    /**
     * Creates a store whose explicit sessions provide causal consistency with the chosen read/write concerns.
     * Database operations synchronize on each supplied session; the same session may serve fixes and metadata.
     * A {@code null} session adds no locking, preserving parallel updates for independent devices.<p>
     *
     * Sessions remain caller-owned. Direct driver operations must use the same session monitor. Before dropping
     * collections or closing sessions, stop fix producers and call {@link MongoSensorFixStore#getNumberOfFixes(DeviceIdentifier)}
     * for each device to finish its pending metadata updates. Do not hold the session monitor while calling store
     * methods: their metadata waits and application callbacks deliberately run outside that monitor.
     */
    MongoSensorFixStore getMongoGPSFixStore(MongoObjectFactory mongoObjectFactory,
            DomainObjectFactory domainObjectFactory, TypeBasedServiceFinderFactory serviceFinderFactory,
            ReadConcern readConcern, WriteConcern writeConcern, ClientSession clientSession,
            ClientSession metadataCollectionClientSession) throws UnknownHostException, MongoException;

}