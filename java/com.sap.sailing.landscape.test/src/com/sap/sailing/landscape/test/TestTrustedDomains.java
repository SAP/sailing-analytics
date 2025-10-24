package com.sap.sailing.landscape.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.landscape.common.SharedLandscapeConstants;

public class TestTrustedDomains {
    @Test
    public void testSapSailingCom() {
        assertTrue(SharedLandscapeConstants.isTrustedDomain("sapsailing.com"));
    }

    @Test
    public void testSapSailingComSubdomain() {
        assertTrue(SharedLandscapeConstants.isTrustedDomain("wind.sapsailing.com"));
    }

    @Test
    public void testSapSailingComSubSubdomain() {
        assertTrue(SharedLandscapeConstants.isTrustedDomain("test.wind.sapsailing.com"));
    }

    @Test
    public void testSapSailingComInDomain() {
        assertFalse(SharedLandscapeConstants.isTrustedDomain("wind.sapsailing.com.something"));
    }

    @Test
    public void testDotAtEnd() {
        assertFalse(SharedLandscapeConstants.isTrustedDomain("sapsailing.com."));
    }

    @Test
    public void testDotAtBeginning() {
        assertFalse(SharedLandscapeConstants.isTrustedDomain(".sapsailing.com."));
    }

    @Test
    public void testIncorrectDomain() {
        assertFalse(SharedLandscapeConstants.isTrustedDomain("sap_sailing.com."));
    }

    @Test
    public void testLocalhost() {
        assertTrue(SharedLandscapeConstants.isTrustedDomain("127.0.0.1"));
        assertTrue(SharedLandscapeConstants.isTrustedDomain("localhost"));
    }
}
