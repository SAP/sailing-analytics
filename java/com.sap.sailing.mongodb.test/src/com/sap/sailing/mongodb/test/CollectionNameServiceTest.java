package com.sap.sailing.mongodb.test;

import org.junit.Test;

import com.sap.sse.mongodb.AlreadyRegisteredException;
import com.sap.sse.mongodb.MongoDBService;

public class CollectionNameServiceTest {

    @Test
    public void shouldWork() throws AlreadyRegisteredException {
        MongoDBService service = MongoDBService.INSTANCE;
        service.registerExclusively(TestInterface.class, "a");
        service.registerExclusively(TestInterface.class, "b");
        service.registerExclusively(TestInterface.class, "B");
        service.registerExclusively(TestInterface.class, "B");
    }

    @Test(expected=AlreadyRegisteredException.class)
    public void shouldNotWork() throws AlreadyRegisteredException {
        MongoDBService service = MongoDBService.INSTANCE;
        service.registerExclusively(TestInterface.class, "b");
        service.registerExclusively(com.sap.sailing.mongodb.test.needadifferentpackage.TestInterface.class, "b");
    }
}
