package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.BearingChangeAnalyzer;
import com.sap.sse.common.impl.DegreeBearingImpl;

public class BearingChangeAnalyzerTest {
    private final BearingChangeAnalyzer bearingChangeAnalyzer = BearingChangeAnalyzer.INSTANCE;
    
    @Test
    public void testSimpleNonCrossingZeroDegreesCase() {
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), 10, new DegreeBearingImpl(15),
                new DegreeBearingImpl(10)));
    }

    @Test
    public void testDoubleNonCrossingZeroDegreesCase() {
        assertEquals(2, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), 370, new DegreeBearingImpl(15),
                new DegreeBearingImpl(10)));
    }

    @Test
    public void testNegativeDoubleNonCrossingZeroDegreesCase() {
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), 370, new DegreeBearingImpl(15),
                new DegreeBearingImpl(16)));
    }

    @Test
    public void testNegativeSimpleNonCrossingZeroDegreesCase() {
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), 10, new DegreeBearingImpl(15),
                new DegreeBearingImpl(17)));
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), 10, new DegreeBearingImpl(15),
                new DegreeBearingImpl(3)));
    }

    @Test
    public void testForwardCrossingZeroDegreesCase() {
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), 10, new DegreeBearingImpl(5),
                new DegreeBearingImpl(0)));
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), 10, new DegreeBearingImpl(5),
                new DegreeBearingImpl(359)));
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), 10, new DegreeBearingImpl(5),
                new DegreeBearingImpl(1)));
    }

    @Test
    public void testNegativeForwardCrossingZeroDegreesCase() {
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), 10, new DegreeBearingImpl(5),
                new DegreeBearingImpl(354)));
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), 10, new DegreeBearingImpl(5),
                new DegreeBearingImpl(6)));
    }

    @Test
    public void testBackwardNonCrossingZeroDegreesCase() {
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), -350, new DegreeBearingImpl(5),
                new DegreeBearingImpl(180)));
    }

    @Test
    public void testDoubleBackwardNonCrossingZeroDegreesCase() {
        assertEquals(2, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), -710, new DegreeBearingImpl(5),
                new DegreeBearingImpl(180)));
    }

    @Test
    public void testNegativeDoubleBackwardNonCrossingZeroDegreesCase() {
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), -710, new DegreeBearingImpl(5),
                new DegreeBearingImpl(3)));
    }

    @Test
    public void testNegativeBackwardNonCrossingZeroDegreesCase() {
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), -350, new DegreeBearingImpl(5),
                new DegreeBearingImpl(359)));
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), -350, new DegreeBearingImpl(5),
                new DegreeBearingImpl(0)));
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(355), -350, new DegreeBearingImpl(5),
                new DegreeBearingImpl(1)));
    }

    @Test
    public void testBackwardCrossingZeroDegreesCase() {
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), -10, new DegreeBearingImpl(355),
                new DegreeBearingImpl(0)));
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), -10, new DegreeBearingImpl(355),
                new DegreeBearingImpl(359)));
        assertEquals(1, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), -10, new DegreeBearingImpl(355),
                new DegreeBearingImpl(1)));
    }

    @Test
    public void testNegativeBackwardCrossingZeroDegreesCase() {
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), -10, new DegreeBearingImpl(355),
                new DegreeBearingImpl(180)));
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), -10, new DegreeBearingImpl(355),
                new DegreeBearingImpl(354)));
        assertEquals(0, bearingChangeAnalyzer.didPass(new DegreeBearingImpl(5), -10, new DegreeBearingImpl(355),
                new DegreeBearingImpl(6)));
    }
}
