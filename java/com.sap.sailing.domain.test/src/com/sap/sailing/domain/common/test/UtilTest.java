package com.sap.sailing.domain.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

public class UtilTest {
    private List<Integer> l;
    
    @BeforeEach
    public void setUp() {
        l = new ArrayList<>();
        l.add(1);
        l.add(2);
        l.add(null);
        l.add(4);
    }

    @Test
    public void testContainsWithNull() {
        assertTrue(Util.contains(l, 1));
        assertFalse(Util.contains(l, 5));
        assertTrue(Util.contains(l, null));
    }
    
    @Test
    public void testContainsWithoutNull() {
        l.remove(null);
        assertFalse(Util.contains(l, null));
    }
    
    @Test
    public void testIndexOfWithNull() {
        assertEquals(0, Util.indexOf(l, 1));
        assertEquals(2, Util.indexOf(l, null));
    }
    
    @Test
    public void testGroup() {
        Pair<String, String> a1 = new Pair<>("a", "a");
        Pair<String, String> a2 = new Pair<>("a", "b");
        Pair<String, String> a3 = new Pair<>("a", "c");
        Pair<String, String> b1 = new Pair<>("b", "a");
        Pair<String, String> b2 = new Pair<>("b", "b");
        Pair<String, String> c1 = new Pair<>("c", "a");

        Map<String, Iterable<Pair<String, String>>> expected = new HashMap<>();
        expected.put("a", new HashSet<Pair<String, String>>(Arrays.asList(a1, a2, a3)));
        expected.put("b", new HashSet<Pair<String, String>>(Arrays.asList(b1, b2)));
        expected.put("c", new HashSet<Pair<String, String>>(Collections.singleton(c1)));

        assertEquals(expected, Util.group(Arrays.asList(a1, b1, a2, c1, b2, a3), Pair::getA, HashSet::new));
    }
}
