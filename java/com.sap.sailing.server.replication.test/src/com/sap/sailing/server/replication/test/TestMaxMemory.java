package com.sap.sailing.server.replication.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class TestMaxMemory {
    @Test
    public void testMaxMemory() {
        assertTrue(Runtime.getRuntime().maxMemory() > 1000l);
    }
}
