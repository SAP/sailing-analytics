package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;

public class IsOnlyAddingTest {
    @Test
    public void testAddingOne() {
        final List<Integer> a = Arrays.asList(1, 2, 3);
        final List<Integer> b = new ArrayList<>(a);
        b.add(1, 5);
        assertTrue(Util.isOnlyAdding(b, a));
        assertFalse(Util.isOnlyAdding(a, b));
    }

    @Test
    public void testAddingDuplicate() {
        final List<Integer> a = Arrays.asList(1, 2, 3);
        final List<Integer> b = Arrays.asList(1, 3, 2, 3);
        assertTrue(Util.isOnlyAdding(b, a));
        assertFalse(Util.isOnlyAdding(a, b));
    }

    @Test
    public void testAddingTwo() {
        final List<Integer> a = Arrays.asList(1, 2, 3);
        final List<Integer> b = new ArrayList<>(a);
        b.add(2, 5);
        b.add(1, 9);
        assertTrue(Util.isOnlyAdding(b, a));
        assertFalse(Util.isOnlyAdding(a, b));
    }

    @Test
    public void testRemovingOne() {
        final List<Integer> a = Arrays.asList(1, 2, 3);
        final List<Integer> b = new ArrayList<>(a);
        b.remove(1);
        assertFalse(Util.isOnlyAdding(b, a));
    }

    @Test
    public void testChangingOrder() {
        final List<Integer> a = Arrays.asList(1, 2, 3);
        final List<Integer> b = Arrays.asList(1, 3, 2);
        assertFalse(Util.isOnlyAdding(b, a));
    }
}
