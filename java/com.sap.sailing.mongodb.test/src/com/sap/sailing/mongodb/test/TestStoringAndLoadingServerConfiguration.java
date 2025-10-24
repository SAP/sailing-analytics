package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.net.UnknownHostException;

import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.SailingServerConfiguration;
import com.sap.sailing.domain.base.impl.SailingServerConfigurationImpl;
import com.sap.sailing.domain.persistence.DomainObjectFactory;
import com.sap.sailing.domain.persistence.MongoObjectFactory;
import com.sap.sailing.domain.persistence.PersistenceFactory;

public class TestStoringAndLoadingServerConfiguration extends AbstractMongoDBTest {
    public TestStoringAndLoadingServerConfiguration() throws UnknownHostException, MongoException {
        super();
    }

    @Test
    public void testLoadStoreServerConfigurationFirstTime() {
        boolean defaultValueOfIsStandaloneServer = false;
        
        // if we have never stored a server configuration before a default configuration should be created 
        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        SailingServerConfiguration loadedServerConfiguration = dof.loadServerConfiguration();
        assertNotNull(loadedServerConfiguration);
        assertEquals(defaultValueOfIsStandaloneServer, loadedServerConfiguration.isStandaloneServer());
    }
    
    @Test
    public void testLoadStoreServerConfiguration() {
        boolean isStandaloneServer = false;
        MongoObjectFactory mof = PersistenceFactory.INSTANCE.getMongoObjectFactory(getMongoService());
        
        SailingServerConfiguration serverConfiguration = new SailingServerConfigurationImpl(isStandaloneServer);  
        mof.storeServerConfiguration(serverConfiguration);
        
        DomainObjectFactory dof = PersistenceFactory.INSTANCE.getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);
        SailingServerConfiguration loadedServerConfiguration = dof.loadServerConfiguration();
        assertNotNull(loadedServerConfiguration);
        assertEquals(isStandaloneServer, loadedServerConfiguration.isStandaloneServer());
        
        isStandaloneServer = true;
        serverConfiguration.setStandaloneServer(isStandaloneServer);
        mof.storeServerConfiguration(serverConfiguration);
        loadedServerConfiguration = dof.loadServerConfiguration();
        assertNotNull(loadedServerConfiguration);
        assertEquals(isStandaloneServer, loadedServerConfiguration.isStandaloneServer());
    }
}
