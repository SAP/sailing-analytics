package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

/**
 * Demonstrates how unmodifiable collections' hashCode and equals is useless because it's based on identity and not on
 * content.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class UnmodifiableCollectionsEqualsHashCodeTest {
    @Test
    public void testInequalityOfUnmodifiableCollection() {
        List<String> l1 = Arrays.asList(new String[] { "abc", "def" });
        List<String> l2 = new ArrayList<>(l1);
        Iterable<String> u1_1 = Collections.unmodifiableCollection(l1);
        Iterable<String> u1_2 = Collections.unmodifiableCollection(l1);
        Iterable<String> u2_1 = Collections.unmodifiableCollection(l2);
        assertEquals(l1, l2);
        assertFalse(u1_1.equals(u1_2));
        assertFalse(u1_1.equals(u2_1));
    }

    @Test
    public void testInequalityOfUnmodifiableList() {
        List<String> l1 = Arrays.asList(new String[] { "abc", "def" });
        List<String> l2 = new ArrayList<>(l1);
        Iterable<String> u1_1 = Collections.unmodifiableList(l1);
        Iterable<String> u1_2 = Collections.unmodifiableList(l1);
        Iterable<String> u2_1 = Collections.unmodifiableList(l2);
        assertEquals(l1, l2);
        assertEquals(u1_1, u1_2);
        assertEquals(u1_1, u2_1);
    }

    @Test
    public void testInequalityOfUnmodifiableSet() {
        Set<String> s1 = new HashSet<>(Arrays.asList(new String[] { "abc", "def" }));
        Set<String> s2 = new HashSet<>(s1);
        Iterable<String> u1_1 = Collections.unmodifiableSet(s1);
        Iterable<String> u1_2 = Collections.unmodifiableSet(s1);
        Iterable<String> u2_1 = Collections.unmodifiableSet(s2);
        assertEquals(s1, s2);
        assertEquals(u1_1, u1_2);
        assertEquals(u1_1, u2_1);
    }
}
