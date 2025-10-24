package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class SubstringTest {
    @Test
    public void substringAfterEndTest() {
        final String abc = "abc?";
        assertEquals("", abc.substring(abc.indexOf("?")+1));
    }
}
