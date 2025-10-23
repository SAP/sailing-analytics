package com.sap.sailing.domain.swisstimingadapter.persistence;

import com.sap.sailing.domain.swisstimingadapter.SwissTimingArchiveConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingConfiguration;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sailing.domain.swisstimingadapter.persistence.impl.SwissTimingAdapterPersistenceImpl;
import com.sap.sse.mongodb.MongoDBService;

public interface SwissTimingAdapterPersistence {

    SwissTimingAdapterPersistence INSTANCE = new SwissTimingAdapterPersistenceImpl(MongoDBService.INSTANCE, SwissTimingFactory.INSTANCE);

    Iterable<SwissTimingConfiguration> getSwissTimingConfigurations();

    Iterable<SwissTimingArchiveConfiguration> getSwissTimingArchiveConfigurations();
    
    void createSwissTimingArchiveConfiguration(SwissTimingArchiveConfiguration swissTimingArchiveConfiguration);

    void updateSwissTimingArchiveConfiguration(SwissTimingArchiveConfiguration swissTimingArchiveConfiguration);

    void deleteSwissTimingArchiveConfiguration(SwissTimingArchiveConfiguration swissTimingArchiveConfiguration);

    void deleteSwissTimingConfiguration(String creatorName, String jsonURL);

    void updateSwissTimingConfiguration(SwissTimingConfiguration createSwissTimingConfiguration, boolean isApiTokenAvailable);

    void createSwissTimingConfiguration(SwissTimingConfiguration createSwissTimingConfiguration);
}
