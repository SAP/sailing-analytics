package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.shared.tracking.impl.PartialNavigableSetView;

public class PartialNavigableSetViewTest {
    private PartialNavigableSetView<Integer> fullSet;
    private PartialNavigableSetView<Integer> emptySet;
    private PartialNavigableSetView<Integer> evenSet;
    private PartialNavigableSetView<Integer> oddSet;
    private TreeSet<Integer> set;
    
    @BeforeEach
    public void setUp() {
        set = new TreeSet<Integer>();
        fullSet = new PartialNavigableSetView<Integer>(set) {
            @Override
            protected boolean isValid(Integer e) {
                return true;
            }
        };
        emptySet = new PartialNavigableSetView<Integer>(set) {
            @Override
            protected boolean isValid(Integer e) {
                return false;
            }
        };
        evenSet = new PartialNavigableSetView<Integer>(set) {
            @Override
            protected boolean isValid(Integer e) {
                return e % 2 == 0;
            }
        };
        oddSet = new PartialNavigableSetView<Integer>(set) {
            @Override
            protected boolean isValid(Integer e) {
                return e % 2 == 1;
            }
        };
    }
    
    @Test
    public void testThatRejectingAllAlwaysYieldsEmptySet() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertTrue(emptySet.isEmpty());
        set.add(4);
        assertTrue(emptySet.isEmpty());
    }

    @Test
    public void testThatRejectingAllAlwaysReturnsZeroSize() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(0, emptySet.size());
        set.add(4);
        assertEquals(0, emptySet.size());
    }

    @Test
    public void testThatRejectingNoneNeverReturnsEmptySet() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertFalse(fullSet.isEmpty());
        set.add(4);
        assertFalse(fullSet.isEmpty());
    }
    
    @Test
    public void testFullSetIterator() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        Iterator<Integer> iter = fullSet.iterator();
        for (int i=1; i<7; i++) {
            assertTrue(iter.hasNext());
            assertEquals(Integer.valueOf(i), iter.next());
        }
        assertFalse(iter.hasNext());
    }
    
    @Test
    public void testEvenSetIterator() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        Iterator<Integer> iter = evenSet.iterator();
        for (int i=2; i<7; i+=2) {
            assertTrue(iter.hasNext());
            assertEquals(Integer.valueOf(i), iter.next());
        }
        assertFalse(iter.hasNext());
    }
    
    @Test
    public void testOddSetIterator() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        Iterator<Integer> iter = oddSet.iterator();
        for (int i=1; i<7; i+=2) {
            assertTrue(iter.hasNext());
            assertEquals(Integer.valueOf(i), iter.next());
        }
        assertFalse(iter.hasNext());
    }
    
    @Test
    public void testEmptySetIterator() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        Iterator<Integer> iter = emptySet.iterator();
        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("Expected NoSuchElementException on iterator that has no next");
        } catch (NoSuchElementException e) {
            // expected
        }
    }
    
    @Test
    public void testFullSetDescendingIterator() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        Iterator<Integer> iter = fullSet.descendingIterator();
        for (int i=6; i>=1; i--) {
            assertTrue(iter.hasNext());
            assertEquals(Integer.valueOf(i), iter.next());
        }
        assertFalse(iter.hasNext());
    }
    
    @Test
    public void testEmptySetDescendingIterator() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        Iterator<Integer> iter = emptySet.descendingIterator();
        assertFalse(iter.hasNext());
        try {
            iter.next();
            fail("Expected NoSuchElementException on iterator that has no next");
        } catch (NoSuchElementException e) {
            // expected
        }
    }
    
    @Test
    public void testEvenSetDescendingIterator() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        Iterator<Integer> iter = evenSet.descendingIterator();
        for (int i=6; i>=1; i-=2) {
            assertTrue(iter.hasNext());
            assertEquals(Integer.valueOf(i), iter.next());
        }
        assertFalse(iter.hasNext());
    }
    
    @Test
    public void testOddSetDescendingIterator() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        set.add(6);
        Iterator<Integer> iter = oddSet.descendingIterator();
        for (int i=5; i>=1; i-=2) {
            assertTrue(iter.hasNext());
            assertEquals(Integer.valueOf(i), iter.next());
        }
        assertFalse(iter.hasNext());
    }
    
    @Test
    public void testThatRejectingNoneAlwaysReturnsFullSize() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(3, fullSet.size());
        set.add(4);
        assertEquals(4, fullSet.size());
    }
    
    @Test
    public void testEvenSetSize() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(1, evenSet.size());
        set.add(4);
        assertEquals(2, evenSet.size());
        set.add(5);
        assertEquals(2, evenSet.size());
    }

    @Test
    public void testOddSetSize() {
        set.add(1);
        set.add(2);
        set.add(3);
        assertEquals(2, oddSet.size());
        set.add(4);
        assertEquals(2, oddSet.size());
        set.add(5);
        assertEquals(3, oddSet.size());
    }

    @Test
    public void testOddHeadSetSize() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        assertTrue(oddSet.headSet(1).isEmpty());
        assertEquals(0, oddSet.headSet(1).size());
        assertEquals(1, oddSet.headSet(2).size());
        assertEquals(1, oddSet.headSet(3).size());
        assertEquals(2, oddSet.headSet(4).size());
        assertEquals(2, oddSet.headSet(5).size());
        assertEquals(3, oddSet.headSet(6).size());
        assertEquals(3, oddSet.headSet(7).size());
    }

    @Test
    public void testOddTailSetSize() {
        set.add(1);
        set.add(2);
        set.add(3);
        set.add(4);
        set.add(5);
        assertEquals(3, oddSet.tailSet(0).size());
        assertEquals(3, oddSet.tailSet(1).size());
        assertEquals(2, oddSet.tailSet(2).size());
        assertEquals(2, oddSet.tailSet(3).size());
        assertEquals(1, oddSet.tailSet(4).size());
        assertEquals(1, oddSet.tailSet(5).size());
        assertEquals(0, oddSet.tailSet(6).size());
        assertTrue(oddSet.tailSet(6).isEmpty());
    }

}
