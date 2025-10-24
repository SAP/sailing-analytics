package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.mongodb.MongoException;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.configuration.DeviceConfiguration;
import com.sap.sailing.domain.base.configuration.impl.DeviceConfigurationImpl;
import com.sap.sailing.domain.base.configuration.impl.RegattaConfigurationImpl;
import com.sap.sailing.domain.persistence.PersistenceFactory;
import com.sap.sailing.domain.persistence.impl.DomainObjectFactoryImpl;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sse.common.Util;

public class StoreAndLoadDeviceConfigurationsTest extends AbstractMongoDBTest {

    public StoreAndLoadDeviceConfigurationsTest() throws UnknownHostException, MongoException {
        super();
    }

    protected MongoObjectFactoryImpl mongoFactory = (MongoObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getMongoObjectFactory(getMongoService());
    protected DomainObjectFactoryImpl domainFactory = (DomainObjectFactoryImpl) PersistenceFactory.INSTANCE
            .getDomainObjectFactory(getMongoService(), DomainFactory.INSTANCE);

    @Test
    public void testStoreEmptyConfiguration() {
        DeviceConfiguration configuration = new DeviceConfigurationImpl(new RegattaConfigurationImpl(), UUID.randomUUID(), "");
        mongoFactory.storeDeviceConfiguration(configuration);
        Iterable<DeviceConfiguration> configurations = domainFactory.loadAllDeviceConfigurations();
        assertEquals(1, Util.size(configurations));
        DeviceConfiguration entry = configurations.iterator().next();
        assertNull(entry.getAllowedCourseAreaNames());
        assertNull(entry.getByNameCourseDesignerCourseNames());
        assertNull(entry.getResultsMailRecipient());
    }
    
    @Test
    public void testStoreConfiguration() {
        DeviceConfigurationImpl configuration = new DeviceConfigurationImpl(new RegattaConfigurationImpl(), UUID.randomUUID(), "");
        configuration.setAllowedCourseAreaNames(Arrays.asList("a","b"));
        configuration.setByNameDesignerCourseNames(Arrays.asList("a", "c"));
        configuration.setResultsMailRecipient("abc");
        mongoFactory.storeDeviceConfiguration(configuration);
        Iterable<DeviceConfiguration> configurations = domainFactory.loadAllDeviceConfigurations();
        assertEquals(1, Util.size(configurations));
        DeviceConfiguration entry = configurations.iterator().next();
        assertNotNull(entry.getAllowedCourseAreaNames());
        assertEquals(2, entry.getAllowedCourseAreaNames().size());
        assertEquals(2, entry.getByNameCourseDesignerCourseNames().size());
        assertEquals(configuration.getByNameCourseDesignerCourseNames(), entry.getByNameCourseDesignerCourseNames());
        assertEquals("abc", entry.getResultsMailRecipient());
    }
    
    @Test
    public void testRemoveConfiguration() {
        DeviceConfiguration configuration = new DeviceConfigurationImpl(new RegattaConfigurationImpl(), UUID.randomUUID(), "");
        mongoFactory.storeDeviceConfiguration(configuration);
        Iterable<DeviceConfiguration> configurations = domainFactory.loadAllDeviceConfigurations();
        assertEquals(1, Util.size(configurations));
        mongoFactory.removeDeviceConfiguration(configuration.getId());
        configurations = domainFactory.loadAllDeviceConfigurations();
        assertEquals(0, Util.size(configurations));
    }
}
