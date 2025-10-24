package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class DoubleCoercionTest {
    @Test
    public void testLongTimesDoubleCoercion() {
        assertEquals(12l, (long) (15l * 0.5 + 0.5 * 10l));
    }
}
