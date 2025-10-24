package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import static com.sap.sse.common.Util.padPositiveValue;

public class NumberPaddingTest {
    @Test
    public void testValueLessThanOne() {
        assertEquals(".1", padPositiveValue(0.1, 0, 1, /* round */ false));
    }

    @Test
    public void testValueGreaterThanOne() {
        assertEquals("1.1", padPositiveValue(1.1, 0, 1, /* round */ false));
    }

    @Test
    public void testValueGreaterThanTen() {
        assertEquals("10.1", padPositiveValue(10.1, 0, 1, /* round */ false));
    }

    @Test
    public void testValueGreaterThanTenWithOneDigitLeftOfDecimalRequested() {
        assertEquals("10.1", padPositiveValue(10.1, 1, 1, /* round */ false));
    }

    @Test
    public void testTruncation() {
        assertEquals("10.1", padPositiveValue(10.179, 0, 1, /* round */ false));
    }

    @Test
    public void testRounding() {
        assertEquals("10.2", padPositiveValue(10.179, 0, 1, /* round */ true));
    }

    @Test
    public void testRoundingCloseToInteger() {
        assertEquals("11.0", padPositiveValue(10.9999999999, 0, 1, /* round */ true));
        assertEquals("11.0", padPositiveValue(11.0000000001, 0, 1, /* round */ true));
    }

    @Test
    public void testLeftPad() {
        assertEquals("0010.2", padPositiveValue(10.179, 4, 1, /* round */ true));
    }

    @Test
    public void testRightPad() {
        assertEquals("10.1790", padPositiveValue(10.179, 0, 4, /* round */ true));
    }
}
