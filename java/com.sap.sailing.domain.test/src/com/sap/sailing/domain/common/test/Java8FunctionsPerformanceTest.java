package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Compare Java 8 stream and function performance to direct method call performance
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class Java8FunctionsPerformanceTest {
    private static final Logger logger = Logger.getLogger(Java8FunctionsPerformanceTest.class.getName());
    private List<Integer> intList;
    private final static int SIZE = 10000000;
    
    @BeforeEach
    public void setUp() {
        intList = new ArrayList<Integer>(SIZE);
        for (int i=0; i<SIZE; i++) {
            intList.add(i);
        }
    }
    
    @Test
    public void intArraySumWithIterator() {
        long start = System.currentTimeMillis();
        long sum = 0;
        for (Integer i : intList) {
            sum += i;
        }
        long end = System.currentTimeMillis();
        assertEquals(((long) SIZE)*((long) SIZE-1l)/2, sum);
        logger.info("took "+(end-start)+"ms");
    }

    @Test
    public void intArraySumWithStream() {
        long start = System.currentTimeMillis();
        final long[] sum = new long[1];
        intList.stream().forEach((i) -> sum[0] += i);
        long end = System.currentTimeMillis();
        assertEquals(((long) SIZE)*((long) SIZE-1l)/2, sum[0]);
        logger.info("took "+(end-start)+"ms");
    }

    @Test
    public void intArraySumWithForEach() {
        long start = System.currentTimeMillis();
        final long[] sum = new long[1];
        intList.forEach((i) -> sum[0] += i);
        long end = System.currentTimeMillis();
        assertEquals(((long) SIZE)*((long) SIZE-1l)/2, sum[0]);
        logger.info("took "+(end-start)+"ms");
    }

    @Test
    public void intArraySumWithIntRangeStream() {
        IntStream is = IntStream.range(0, SIZE);
        long start = System.currentTimeMillis();
        final long[] sum = new long[1];
        is.forEach(i -> sum[0] += i);
        long end = System.currentTimeMillis();
        assertEquals(((long) SIZE)*((long) SIZE-1l)/2, sum[0]);
        logger.info("took "+(end-start)+"ms");
    }

    @Test
    public void intSum() {
        long start = System.currentTimeMillis();
        long sum = 0;
        for (int i=0; i<SIZE; i++) {
            sum += i;
        }
        long end = System.currentTimeMillis();
        assertEquals(((long) SIZE)*((long) SIZE-1l)/2, sum);
        logger.info("took "+(end-start)+"ms");
    }
}
