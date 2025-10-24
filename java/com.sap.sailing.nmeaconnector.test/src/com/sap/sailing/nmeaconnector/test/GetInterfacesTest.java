package com.sap.sailing.nmeaconnector.test;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import net.sf.marineapi.nmea.event.AbstractSentenceListener;

/**
 * Asserting that Class.getInterfaces() does not consider transitively implemented
 * interfaces. Based on this observation it seems advisable to use
 * {@link Class#isAssignableFrom(Class)} instead of a contains check with the
 * directly implemented interfaces when using the {@link AbstractSentenceListener}
 * pattern. See commit 746440cae18dd55bf46df8207a03da70466f9e02.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class GetInterfacesTest {
    private interface I2 {}
    private interface I1 extends I2 {}
    private static class A implements I1 {}
    
    @Test
    public void testTransitiveInterface() {
        assertFalse(Arrays.asList(A.class.getInterfaces()).contains(I2.class));
    }
}
