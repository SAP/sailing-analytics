package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * See the use of bit shift operations in <code>IncrementalLeaderboardDTO</code>
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class BitShiftTest {
    @Test
    public void bitShiftWithLongTest() {
        long l = 1l<<63;
        assertTrue((l & 1l<<63) != 0);
    }
}
