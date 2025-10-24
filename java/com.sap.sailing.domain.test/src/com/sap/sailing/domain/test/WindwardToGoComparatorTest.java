package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Comparator;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class WindwardToGoComparatorTest {
    @BeforeEach
    public void setUp() {
        
    }
    
    @Test
    public void testInsertEqualElementsIntoSortedSet() {
        TreeSet<Integer> ti = new TreeSet<Integer>(new Comparator<Integer>() {
            @Override
            public int compare(Integer o1, Integer o2) {
                return 0;
            }
        });
        ti.add(Integer.valueOf(1));
        ti.add(Integer.valueOf(2));
        assertEquals(1, ti.size());
    }

    @Test
    public void testInsertEqualElementsIntoSortedSetWithoutComparator() {
        TreeSet<Integer> ti = new TreeSet<Integer>();
        ti.add(Integer.valueOf(1));
        ti.add(Integer.valueOf(2));
        assertEquals(2, ti.size());
    }
}
