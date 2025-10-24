package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;

public class IterableConcatTest {
    @Test
    public void emptyTest() {
        assertTrue(Util.isEmpty(Util.concat(Collections.emptySet())));
    }
    
    @Test
    public void testWithSingleNullIterable() {
        assertTrue(Util.isEmpty(Util.concat(Collections.singleton(null))));
    }
    
    @Test
    public void testWithTwoNullIterables() {
        assertTrue(Util.isEmpty(Util.concat(Arrays.asList(null, null))));
    }
    
    @Test
    public void testWithSingleIterable() {
        final Iterable<Integer> list = Arrays.asList(1, 2, 3, 4);
        assertTrue(Util.equals(list, Util.concat(Collections.singleton(list))));
    }

    @Test
    public void testWithSingleIterableSurroundedByTwoNullIterables() {
        final Iterable<Integer> list = Arrays.asList(1, 2, 3, 4);
        assertTrue(Util.equals(list, Util.concat(Arrays.asList(null, list, null))));
    }

    @Test
    public void testWithTwoIterables() {
        final Iterable<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        final Iterable<Integer> list2 = Arrays.asList(9, 8, 7, 6);
        final List<Integer> expectedResult = new ArrayList<>();
        Util.addAll(list1, expectedResult);
        Util.addAll(list2, expectedResult);
        assertTrue(Util.equals(expectedResult, Util.concat(Arrays.asList(list1, list2))));
    }

    @Test
    public void testWithTwoIterablesWithNullBetween() {
        final Iterable<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        final Iterable<Integer> list2 = Arrays.asList(9, 8, 7, 6);
        final List<Integer> expectedResult = new ArrayList<>();
        Util.addAll(list1, expectedResult);
        Util.addAll(list2, expectedResult);
        assertTrue(Util.equals(expectedResult, Util.concat(Arrays.asList(list1, null, list2))));
    }

    @Test
    public void testWithTwoIterablesWithEmptyIterableBetween() {
        final Iterable<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        final Iterable<Integer> list2 = Arrays.asList(9, 8, 7, 6);
        final List<Integer> expectedResult = new ArrayList<>();
        Util.addAll(list1, expectedResult);
        Util.addAll(list2, expectedResult);
        assertTrue(Util.equals(expectedResult, Util.concat(Arrays.asList(list1, new ArrayList<>(), list2))));
    }

    @Test
    public void testWithTwiceSameIterable() {
        final Iterable<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        final List<Integer> expectedResult = new ArrayList<>();
        Util.addAll(list1, expectedResult);
        Util.addAll(list1, expectedResult);
        assertTrue(Util.equals(expectedResult, Util.concat(Arrays.asList(list1, list1))));
    }

    @Test
    public void testIteratingTwice() {
        final Iterable<Integer> list1 = Arrays.asList(1, 2, 3, 4);
        final List<Integer> expectedResult = new ArrayList<>();
        Util.addAll(list1, expectedResult);
        Util.addAll(list1, expectedResult);
        final Iterable<Integer> concat = Util.concat(Arrays.asList(list1, list1));
        assertTrue(Util.equals(expectedResult, concat));
        assertTrue(Util.equals(expectedResult, concat));
    }
}
