package com.sap.sse.shared.android.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;

import org.junit.jupiter.api.Test;

import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

public class ArrayListNavigableSetTest {
    @Test
    public void testEqualityOfArrayListNavigableSet() {
        ArrayListNavigableSet<Integer> a = new ArrayListNavigableSet<>(Comparator.naturalOrder());
        ArrayListNavigableSet<Integer> b = new ArrayListNavigableSet<>(Comparator.naturalOrder());
        a.add(1);
        a.add(42);
        b.add(42);
        b.add(1);
        assertEquals(a, b);
    }
}
