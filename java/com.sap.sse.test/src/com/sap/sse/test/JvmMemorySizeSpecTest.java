package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sse.util.JvmUtils;

public class JvmMemorySizeSpecTest {
    @Test
    public void testPlainBytes() {
        assertEquals(1, (int) JvmUtils.getMegabytesFromJvmSize(""+1024*1024).get());
    }

    @Test
    public void testMegsLowercase() {
        assertEquals(1024, (int) JvmUtils.getMegabytesFromJvmSize("1024m").get());
    }

    @Test
    public void testMegsUppercase() {
        assertEquals(1024, (int) JvmUtils.getMegabytesFromJvmSize("1024M").get());
    }

    @Test
    public void testGigsLowercase() {
        assertEquals(1024, (int) JvmUtils.getMegabytesFromJvmSize("1g").get());
    }

    @Test
    public void testGigsUppercase() {
        assertEquals(10240, (int) JvmUtils.getMegabytesFromJvmSize("10G").get());
    }
    
    @Test
    public void testRoundingUp() {
        assertEquals(1, (int) JvmUtils.getMegabytesFromJvmSize("100").get());
        assertEquals(1, (int) JvmUtils.getMegabytesFromJvmSize(""+1024*1023).get());
        assertEquals(2, (int) JvmUtils.getMegabytesFromJvmSize(""+1024*1025).get());
    }
}