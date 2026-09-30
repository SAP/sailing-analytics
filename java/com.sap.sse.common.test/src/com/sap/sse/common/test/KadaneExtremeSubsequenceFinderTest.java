package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Random;
import java.util.TreeSet;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.common.scalablevalue.KadaneExtremeSubsequenceFinder;
import com.sap.sse.common.scalablevalue.ScalableDouble;
import com.sap.sse.testutils.Measurement;
import com.sap.sse.testutils.MeasurementCase;
import com.sap.sse.testutils.MeasurementXMLFile;

public abstract class KadaneExtremeSubsequenceFinderTest {
    private static final double EPSILON = 0.00000001;
    private static final Logger logger = Logger.getLogger(KadaneExtremeSubsequenceFinderTest.class.getName());

    protected KadaneExtremeSubsequenceFinder<Double, Double, ScalableDouble> finder;
    private static Random random = new Random();
    private static final MeasurementXMLFile performanceReport = new MeasurementXMLFile(KadaneExtremeSubsequenceFinderTest.class);
    
    @Test
    public void testSimplePositiveSequence() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        assertEquals(6.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(2, finder.getEndIndexOfMaxSumSequence());
    }

    @Test
    public void testSimplePositiveSequenceWithInsertInTheMiddle() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(3));
        finder.add(1, new ScalableDouble(2));
        assertEquals(6.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(2, finder.getEndIndexOfMaxSumSequence());
    }

    @Test
    public void testSimpleSequenceWithPositiveAndNegative() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(-4));
        finder.add(new ScalableDouble(5));
        finder.add(new ScalableDouble(6));
        finder.add(new ScalableDouble(-5));
        assertEquals(13.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(5, finder.getEndIndexOfMaxSumSequence());
    }
    
    @Test
    public void testSimplePositiveSequenceWithLaterNegativeInsertInTheMiddle() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(4, finder.getEndIndexOfMaxSumSequence());
        finder.add(3, new ScalableDouble(-7));
        assertEquals(9.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(4, finder.getStartIndexOfMaxSumSequence());
        assertEquals(5, finder.getEndIndexOfMaxSumSequence());
        assertTrue(Util.equals(Arrays.asList(new ScalableDouble(4), new ScalableDouble(5)), ()->finder.getSubSequenceWithMaxSum()));
    }
    
    @Test
    public void testRemoveFromMiddleOfPositiveSequence() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(4, finder.getEndIndexOfMaxSumSequence());
        finder.remove(2);
        assertEquals(12.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(3, finder.getEndIndexOfMaxSumSequence());
    }
    
    /**
     * A white-box test for how we remove from the {@link TreeSet} that holds the best max sum sub-sequence end node.
     * Should the wrong node be removed, the structure would become inconsistent and hold a node as "best" that no
     * longer exists.
     */
    @Test
    public void testTwoEquallyGoodPositiveSequencesThenRemovingFromOne() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        // now a "very negative" one that starts a new positive sequence after it
        finder.add(new ScalableDouble(-100));
        // now the second positive sequence with equal max sum as the first one
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertTrue(finder.getStartIndexOfMaxSumSequence() == 0 || finder.getStartIndexOfMaxSumSequence() == 6);
        assertTrue(finder.getEndIndexOfMaxSumSequence() == 4 || finder.getEndIndexOfMaxSumSequence() == 10);
        finder.remove(3);
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(5, finder.getStartIndexOfMaxSumSequence());
        assertEquals(9, finder.getEndIndexOfMaxSumSequence());
        assertTrue(Util.equals(Arrays.asList(new ScalableDouble(1), new ScalableDouble(2), new ScalableDouble(3), new ScalableDouble(4), new ScalableDouble(5)), ()->finder.getSubSequenceWithMaxSum()));
        finder.remove(8); // removes the 4.0 from the second sequence, resulting again in two equal max sum sub-sequences:
        assertEquals(11.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertTrue(finder.getStartIndexOfMaxSumSequence() == 0 || finder.getStartIndexOfMaxSumSequence() == 5);
        assertTrue(finder.getEndIndexOfMaxSumSequence() == 3 || finder.getEndIndexOfMaxSumSequence() == 8);
        finder.remove(7); // removes the 3.0 from the second sequence
        assertEquals(11.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(3, finder.getEndIndexOfMaxSumSequence());
    }
    
    @Test
    public void testRemoveFromBeginningOfPositiveSequence() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(5));
        assertEquals(15.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(4, finder.getEndIndexOfMaxSumSequence());
        finder.remove(0);
        finder.remove(0);
        finder.remove(0);
        assertEquals(9.0, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(1, finder.getEndIndexOfMaxSumSequence());
    }
    
    @Test
    public void performanceTestWithRandomRemove() throws IOException {
        final MeasurementCase performanceMeasurement = performanceReport.addCase("PerformanceTestWithRandomRemove");
        final int NODES = 10000;
        for (int i=0; i<NODES; i++) {
            finder.add(new ScalableDouble(random.nextDouble()-0.5));
        }
        assertEquals(NODES, finder.size());
        finder.resetStats();
        for (int i=0; i<NODES/2; i++) {
            finder.remove(random.nextInt(finder.size()));
        }
        assertEquals(NODES-NODES/2, finder.size());
        performanceMeasurement.addMeasurement(new Measurement("minChangePropagationCount", finder.getAverageMinChangePropagationSteps()));
        performanceMeasurement.addMeasurement(new Measurement("maxChangePropagationCount", finder.getAverageMaxChangePropagationSteps()));
        logger.info("Stats after random remove: " + finder.toString());
    }

    @Test
    public void performanceTestWithRemoveFromBeginning() throws IOException {
        final MeasurementCase performanceMeasurement = performanceReport.addCase("PerformanceTestWithRemoveFromBeginning");
        final int NODES = 10000;
        for (int i=0; i<NODES; i++) {
            finder.add(new ScalableDouble(random.nextDouble()-0.5));
        }
        assertEquals(NODES, finder.size());
        finder.resetStats();
        for (int i=0; i<NODES/2; i++) {
            finder.remove(0);
        }
        assertEquals(NODES-NODES/2, finder.size());
        performanceMeasurement.addMeasurement(new Measurement("minChangePropagationCount", finder.getAverageMinChangePropagationSteps()));
        performanceMeasurement.addMeasurement(new Measurement("maxChangePropagationCount", finder.getAverageMaxChangePropagationSteps()));
        logger.info("Stats after removing from beginning: " + finder.toString());
    }

    @Test
    public void performanceTestWithPruneFromBeginning() throws IOException {
        final MeasurementCase performanceMeasurement = performanceReport.addCase("PerformanceTestWithPruneFromBeginning");
        final int NODES = 10000;
        for (int i=0; i<NODES; i++) {
            finder.add(new ScalableDouble(random.nextDouble()-0.5));
        }
        assertEquals(NODES, finder.size());
        finder.resetStats();
        finder.removeFirst(NODES/2);
        assertEquals(NODES-NODES/2, finder.size());
        assertNotEquals(-1, finder.getStartIndexOfMaxSumSequence());
        assertNotEquals(-1, finder.getEndIndexOfMaxSumSequence());
        assertNotEquals(-1, finder.getStartIndexOfMinSumSequence());
        assertNotEquals(-1, finder.getEndIndexOfMinSumSequence());
        performanceMeasurement.addMeasurement(new Measurement("minChangePropagationCount", finder.getAverageMinChangePropagationSteps()));
        performanceMeasurement.addMeasurement(new Measurement("maxChangePropagationCount", finder.getAverageMaxChangePropagationSteps()));
        logger.info("Stats after pruning from beginning: " + finder.toString());
    }
    
    @Test
    public void testSingleNegativeNumber() {
        finder.add(new ScalableDouble(1));
        finder.add(new ScalableDouble(2));
        finder.add(new ScalableDouble(3));
        finder.add(new ScalableDouble(4));
        finder.add(new ScalableDouble(-38.001708984375));
        finder.removeFirst(4);
        assertEquals(-38.001708984375, finder.getMaxSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMaxSumSequence());
        assertEquals(0, finder.getEndIndexOfMaxSumSequence());
        assertEquals(-38.001708984375, finder.getMinSum().divide(1.0), EPSILON);
        assertEquals(0, finder.getStartIndexOfMinSumSequence());
        assertEquals(0, finder.getEndIndexOfMinSumSequence());
    }
    
    @AfterAll
    public static void writeMeasurements() throws IOException {
        performanceReport.write();
    }
}
