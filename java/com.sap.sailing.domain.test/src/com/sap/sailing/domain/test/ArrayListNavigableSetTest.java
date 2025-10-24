package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sse.shared.util.impl.ArrayListNavigableSet;

public class ArrayListNavigableSetTest {
    @Test
    public void testReplacementInHashSet() {
        Set<Integer> hashSet = new HashSet<Integer>();
        testSet(hashSet);
    }

    @Test
    public void testReplacementInArrayListNavigableSet() {
        Set<Integer> arrayListNavigableSet = new ArrayListNavigableSet<Integer>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return o1.compareTo(o2);
            }
        });
        testSet(arrayListNavigableSet);
    }

    private void testSet(Set<Integer> hashSet) {
        Integer i1 = new Integer(1);
        Integer i2 = new Integer(1);
        assertNotSame(i1, i2);
        assertEquals(i1, i2);
        hashSet.add(i1);
        boolean replaced = hashSet.add(i2);
        assertFalse(replaced); // because s1 and s2 are equal
        Integer integerFromSet = hashSet.iterator().next();
        assertSame(i1, integerFromSet); // supposedly not replaced
    }
    
    @Test
    public void testMultiAdd() {
        ArrayListNavigableSet<Integer> arrayListNavigableSet = new ArrayListNavigableSet<Integer>(Comparator.naturalOrder());        
        final List<Integer> elements = Arrays.asList(1, 2, 3, 4, 6);
        arrayListNavigableSet.addAll(elements);
        assertEquals(5, arrayListNavigableSet.size());
        assertEquals(new HashSet<>(elements), arrayListNavigableSet);
        boolean notContained;
        notContained = arrayListNavigableSet.add(0);
        assertEquals(Integer.valueOf(0), arrayListNavigableSet.first());
        assertTrue(notContained);
        notContained = arrayListNavigableSet.add(7);
        assertEquals(Integer.valueOf(7), arrayListNavigableSet.last());
        assertTrue(notContained);
        notContained = arrayListNavigableSet.add(4);
        assertEquals(7, arrayListNavigableSet.size());
        assertFalse(notContained);
        notContained = arrayListNavigableSet.add(5);
        assertTrue(notContained);
        assertEquals(8, arrayListNavigableSet.size());
        int last = -1;
        Iterator<Integer> i = arrayListNavigableSet.iterator();
        while (i.hasNext()) {
            int next = i.next();
            assertTrue(next > last);
        }
    }
}
