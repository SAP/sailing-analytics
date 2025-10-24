package com.sap.sse.filestorage.propertystore.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.TypeBasedServiceFinder;
import com.sap.sse.common.impl.SingleTypeBasedServiceFinderImpl;
import com.sap.sse.filestorage.FileStorageService;
import com.sap.sse.filestorage.FileStorageServiceProperty;
import com.sap.sse.filestorage.FileStorageServicePropertyStore;
import com.sap.sse.filestorage.impl.FileStorageManagementServiceImpl;
import com.sap.sse.filestorage.impl.MongoFileStorageServicePropertyStoreImpl;
import com.sap.sse.filestorage.test.util.Util;
import com.sap.sse.filestorage.testsupport.DummyFileStorageService;
import com.sap.sse.mongodb.MongoDBConfiguration;

public class MongoStoreTest {
    private MongoDBConfiguration config = MongoDBConfiguration.getDefaultTestConfiguration();

    @BeforeEach
    public void setup() {
        config.getService().getDB().drop();
    }

    @AfterEach
    public void tearDown() {
        config.getService().getDB().drop();
    }

    @Test
    public void testStoreAndReadProperties() {
        FileStorageServicePropertyStore store = new MongoFileStorageServicePropertyStoreImpl(config.getService());

        store.writeProperty("s1", "p1", "1");
        store.writeProperty("s1", "p2", "2");
        store.writeProperty("s2", "p3", "3");
        store.writeActiveService("active");

        Map<String, String> properties = store.readAllProperties("s1");
        assertThat("two properties read for service 1", properties.size(), equalTo(2));
        assertThat("property 1 is read correctly for service 1", properties.get("p1"), equalTo("1"));

        properties = store.readAllProperties("s2");
        assertThat("one property read for service 2", properties.size(), equalTo(1));

        assertThat("active service written", store.readActiveServiceName(), equalTo("active"));
        
        //change some things
        store.writeProperty("s1", "p1", "10");
        store.writeActiveService("newActive");
        properties = store.readAllProperties("s1");
        assertThat("new property 1 is read correctly for service 1", properties.get("p1"), equalTo("10"));
        assertThat("new active service correct", store.readActiveServiceName(), equalTo("newActive"));
    }

    @Test
    public void testManagementServiceUsesStore() {
        FileStorageServicePropertyStore store = new MongoFileStorageServicePropertyStoreImpl(config.getService());

        FileStorageService service = new DummyFileStorageService();
        TypeBasedServiceFinder<FileStorageService> serviceFinder = new SingleTypeBasedServiceFinderImpl<>(
                service, DummyFileStorageService.NAME);
        FileStorageManagementServiceImpl mgmtService = new FileStorageManagementServiceImpl(serviceFinder, store);

        mgmtService.setActiveFileStorageService(service);
        mgmtService.setFileStorageServiceProperty(service, DummyFileStorageService.PROPERTY_NAME, "123");

        // now setup new services
        service = new DummyFileStorageService();
        serviceFinder = new SingleTypeBasedServiceFinderImpl<>(service, DummyFileStorageService.NAME);
        mgmtService = new FileStorageManagementServiceImpl(serviceFinder, store);

        assertThat("active service is read from DB", mgmtService.getActiveFileStorageService(), equalTo(service));

        // simulate OSGi service addition
        mgmtService.onServiceAdded(service);
        FileStorageServiceProperty property = Util.findProperty(service, DummyFileStorageService.PROPERTY_NAME);
        assertThat("stored property set for service", property.getValue(), equalTo("123"));
    }
}
