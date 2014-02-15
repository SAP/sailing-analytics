package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import java.net.UnknownHostException;

import com.mongodb.MongoException;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.racelog.tracking.MongoGPSFixStore;
import com.sap.sailing.domain.persistence.racelog.tracking.MongoGPSFixStoreFactory;

public class MongoGPSFixStoreFactoryImpl implements MongoGPSFixStoreFactory {	
	@Override
	public MongoGPSFixStore getMongoGPSFixStore(MongoObjectFactory mongoObjectFactory,
			DomainObjectFactory domainObjectFactory) throws UnknownHostException, MongoException {
		return new MongoGPSFixStoreImpl(mongoObjectFactory, domainObjectFactory);
	}
}
