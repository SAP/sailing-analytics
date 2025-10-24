package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;

public class ArraySortTest {
    @Test
    public void simpleArraySortTest() {
        double[] keys   = { 1, 3, 2 };
        double[] values = { 4, 6, 5 };
        Util.sort(keys, values);
        assertTrue(Arrays.equals(new double[] { 1, 2, 3 }, keys));
        assertTrue(Arrays.equals(new double[] { 4, 5, 6 }, values));
    }

    @Test
    public void threeArraySortTest() {
        double[] keys    = { 1, 3, 2 };
        double[] values1 = { 4, 6, 5 };
        double[] values2 = { 7, 9, 8 };
        Util.sort(keys, values1, values2);
        assertTrue(Arrays.equals(new double[] { 1, 2, 3 }, keys));
        assertTrue(Arrays.equals(new double[] { 4, 5, 6 }, values1));
        assertTrue(Arrays.equals(new double[] { 7, 8, 9 }, values2));
    }

    @Test
    public void threeArrayErraticSortTest() {
        double[] keys    = { 1, 3, 2 };
        double[] values1 = { 4, 5, 6 };
        double[] values2 = { 7, 8, 9 };
        Util.sort(keys, values1, values2);
        assertTrue(Arrays.equals(new double[] { 1, 2, 3 }, keys));
        assertTrue(Arrays.equals(new double[] { 4, 6, 5 }, values1));
        assertTrue(Arrays.equals(new double[] { 7, 9, 8 }, values2));
    }
}
