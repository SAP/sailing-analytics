package com.sap.sailing.domain.swisstimingadapter.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

public class SequenceNumberParsingTest {
    @Test
    public void testNoSequenceNumber() {
        SailMasterMessageImpl message = new SailMasterMessageImpl("RAC|0");
        assertNull(message.getSequenceNumber());
        assertEquals("RAC|0", message.getMessage());
    }

    @Test
    public void testValidSequenceNumber() {
        SailMasterMessageImpl message = new SailMasterMessageImpl("4711|RAC|0");
        assertNotNull(message.getSequenceNumber());
        assertEquals(4711l, message.getSequenceNumber().longValue());
        assertEquals("RAC|0", message.getMessage());
    }
}
