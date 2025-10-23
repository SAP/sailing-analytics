package com.sap.sailing.domain.tractracadapter.persistence;


import com.mongodb.DBObject;
import com.sap.sailing.domain.tractracadapter.TracTracConfiguration;
import com.sap.sailing.domain.tractracadapter.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sse.mongodb.MongoDBService;

/**
 * Offers methods to construct {@link DBObject MongoDB objects} from domain objects.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface MongoObjectFactory {
    MongoObjectFactory INSTANCE = new MongoObjectFactoryImpl(MongoDBService.INSTANCE.getDB());

    /**
     * Inserts/updates based on the {@link TracTracConfiguration#getName() name}. Any equally-named
     * config previously contained in the DB will be replaced / updated. Afterwards,
     * {@link DomainObjectFactory#getTracTracConfigurations()} called for the same <code>database</code>
     * will return an equal <code>tracTracConfiguration</code> in its results.
     */
    void createTracTracConfiguration(TracTracConfiguration tracTracConfiguration);

    /**
     * When the {@link TracTracConfiguration#getTracTracPassword() password} is {@code null} then it
     * will not be updated but left unchanged.
     */
    void updateTracTracConfiguration(TracTracConfiguration tracTracConfiguration, boolean isTracTracApiTokenAvailable);

    void deleteTracTracConfiguration(String creatorName, String jsonurl);

    void clear();
    
}
