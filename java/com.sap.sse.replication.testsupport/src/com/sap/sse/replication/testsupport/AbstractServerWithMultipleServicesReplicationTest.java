package com.sap.sse.replication.testsupport;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

public abstract class AbstractServerWithMultipleServicesReplicationTest {
    protected final Set<AbstractServerReplicationTestSetUp<?, ?>> testSetUps = new HashSet<>();

    @BeforeEach
    public void setUp() throws Exception {
        for (AbstractServerReplicationTestSetUp<?, ?> testSetUp : testSetUps) {
            testSetUp.setUp();
        }
    }
    
    @AfterEach
    public void tearDown() throws Exception {
        for (AbstractServerReplicationTestSetUp<?, ?> testSetUp : testSetUps) {
            testSetUp.tearDown();
        }
    }
}
