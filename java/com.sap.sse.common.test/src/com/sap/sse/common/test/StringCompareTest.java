package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;

public class StringCompareTest {
    @Test
    public void testStringCompare() {
        assertTrue(Util.equalsWithNull("a", "A", /* ignoreCase */ true));
        assertTrue(Util.equalsWithNull("a", "a", /* ignoreCase */ true));
        assertFalse(Util.equalsWithNull("a", "A", /* ignoreCase */ false));
        assertTrue(Util.equalsWithNull(null, null, /* ignoreCase */ true));
        assertFalse(Util.equalsWithNull("a", null, /* ignoreCase */ true));
        assertTrue(Util.equalsWithNull(null, null, /* ignoreCase */ false));
        assertFalse(Util.equalsWithNull("a", null, /* ignoreCase */ false));
        assertFalse(Util.equalsWithNull("A", null, /* ignoreCase */ true));
        assertTrue(Util.equalsWithNull(null, null, /* ignoreCase */ false));
        assertFalse(Util.equalsWithNull("A", null, /* ignoreCase */ false));
        assertFalse(Util.equalsWithNull(null, "b", /* ignoreCase */ true));
        assertFalse(Util.equalsWithNull(null, "B", /* ignoreCase */ true));
    }
}
