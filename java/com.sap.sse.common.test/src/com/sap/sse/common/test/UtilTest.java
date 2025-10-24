package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;

public class UtilTest {
    @Test
    public void testFilterIterable() {
        final Iterable<Integer> ints = Arrays.asList(1, 2, 3, 9, 8, 7, 4, 5, 6);
        assertEquals(Arrays.asList(1, 2, 3, 4, 5, 6), Util.asList(Util.filter(ints, i->i<7)));
    }

    @Test
    public void testFilterLast() {
        final Iterable<Integer> ints = Arrays.asList(1, 2, 3, 9, 8, 7, 4, 5, 6);
        assertEquals(Arrays.asList(1, 2, 3, 9, 8, 7, 4, 5), Util.asList(Util.filter(ints, i->i!=6)));
    }

    @Test
    public void testFilterFirst() {
        final Iterable<Integer> ints = Arrays.asList(1, 2, 3, 9, 8, 7, 4, 5, 6);
        assertEquals(Arrays.asList(2, 3, 9, 8, 7, 4, 5, 6), Util.asList(Util.filter(ints, i->i!=1)));
    }

    @Test
    public void testFilterNone() {
        final Iterable<Integer> ints = Arrays.asList(1, 2, 3, 9, 8, 7, 4, 5, 6);
        assertEquals(ints, Util.asList(Util.filter(ints, i->true)));
    }

    @Test
    public void testFilterAll() {
        final Iterable<Integer> ints = Arrays.asList(1, 2, 3, 9, 8, 7, 4, 5, 6);
        assertTrue(Util.isEmpty(Util.asList(Util.filter(ints, i->false))));
    }
}
