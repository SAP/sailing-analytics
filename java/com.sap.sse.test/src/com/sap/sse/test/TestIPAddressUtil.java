package com.sap.sse.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sse.util.IPAddressUtil;

public class TestIPAddressUtil {
    @Test
    public void testIPv4Matching() {
        assertTrue(IPAddressUtil.isIPv4AddressLiteral("1.2.3.4"));
        assertTrue(IPAddressUtil.isIPv4AddressLiteral("123.234.33.0"));
        assertFalse(IPAddressUtil.isIPv4AddressLiteral("0.2.3.4"));
        assertTrue(IPAddressUtil.isIPv4AddressLiteral("255.255.255.255"));
        assertFalse(IPAddressUtil.isIPv4AddressLiteral("java.sun.com"));
    }

    @Test
    public void testIPv6Matching() {
        assertTrue(IPAddressUtil.isIPv6AddressLiteral("2001:db8:3333:4444:5555:6666:7777:8888"));
        assertTrue(IPAddressUtil.isIPv6AddressLiteral("2001:db8:3333:4444:CCCC:DDDD:EEEE:FFFF"));
        assertTrue(IPAddressUtil.isIPv6AddressLiteral("::"));
        assertTrue(IPAddressUtil.isIPv6AddressLiteral("2001:db8::"));
        assertTrue(IPAddressUtil.isIPv6AddressLiteral("::1234:5678"));
        assertTrue(IPAddressUtil.isIPv6AddressLiteral("2001:db8::1234:5678"));
        assertFalse(IPAddressUtil.isIPv6AddressLiteral("java.sun.com"));
        assertFalse(IPAddressUtil.isIPv6AddressLiteral(":"));
    }
}
