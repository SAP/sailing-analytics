package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Iterator;
import java.util.Map.Entry;

import org.junit.jupiter.api.Test;

import java.util.Set;

import com.sap.sse.concurrent.ConcurrentWeakHashMap;

/**
 * Tests the class {@link ConcurrentWeakHashMap}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ConcurrentWeakHashMapTest {
    @Test
    public void testPutAndGet() {
        ConcurrentWeakHashMap<String, String> m = new ConcurrentWeakHashMap<>();
        m.put("a", "b");
        assertEquals("b", m.get("a"));
    }

    @Test
    public void testPutAndIterate() {
        ConcurrentWeakHashMap<String, String> m = new ConcurrentWeakHashMap<>();
        m.put("a", "b");
        assertEquals("b", m.values().iterator().next());
        assertEquals("a", m.keySet().iterator().next());
        assertEquals("a", m.entrySet().iterator().next().getKey());
    }

    @Test
    public void testPutAndPutAgain() {
        ConcurrentWeakHashMap<Integer, String> m = new ConcurrentWeakHashMap<>();
        Integer one1 = new Integer(1);
        Integer one2 = new Integer(1);
        assertNotSame(one1, one2);
        m.put(one1, "a");
        String replaced = m.put(one2, "b");
        assertEquals("a", replaced);
        assertEquals(1, m.size());
        assertEquals(m.keySet().iterator().next(), one2);
    }

    @Test
    public void testRemoveWithSame() {
        ConcurrentWeakHashMap<Integer, String> m = new ConcurrentWeakHashMap<>();
        Integer one = Integer.valueOf(1);
        m.put(one, "a");
        m.remove(one);
        assertTrue(m.isEmpty());
    }

    @Test
    public void testRemoveImpactOnKeySetAndEntrySet() {
        ConcurrentWeakHashMap<Integer, String> m = new ConcurrentWeakHashMap<>();
        Integer one = Integer.valueOf(1);
        m.put(one, "a");
        Set<Integer> keySet = m.keySet();
        Set<Entry<Integer, String>> entrySet = m.entrySet();
        assertEquals(1, keySet.size());
        assertEquals(1, entrySet.size());
        m.remove(one);
        assertTrue(m.isEmpty());
        assertTrue(keySet.isEmpty());
        assertTrue(entrySet.isEmpty());
    }

    @Test
    public void testRemoveWithEqual() {
        ConcurrentWeakHashMap<Integer, String> m = new ConcurrentWeakHashMap<>();
        Integer one1 = new Integer(1);
        Integer one2 = new Integer(1);
        assertNotSame(one1, one2);
        m.put(one1, "a");
        m.remove(one2);
        assertTrue(m.isEmpty());
    }
    
    @Test
    public void removeThrougKeySet() {
        ConcurrentWeakHashMap<Integer, String> m = new ConcurrentWeakHashMap<>();
        Integer one1 = Integer.valueOf(1);
        m.put(one1, "a");
        final Set<Integer> keySet = m.keySet();
        Iterator<Integer> keySetIter = keySet.iterator();
        keySetIter.next();
        keySetIter.remove();
        assertTrue(m.isEmpty());
        assertTrue(keySet.isEmpty());
        assertTrue(m.entrySet().isEmpty());
    }

    @Test
    public void testPutAndGetWithSubsequentGC() {
        Object key = createKeyObject();
        ConcurrentWeakHashMap<Object, String> m = new ConcurrentWeakHashMap<>();
        m.put(key, "b");
        assertEquals("b", m.get(key));
        key = null;
        forceEnqueuableWeakReferencesToBeEnqueued();
        assertTrue(m.isEmpty());
    }
    
    private void forceEnqueuableWeakReferencesToBeEnqueued() {
        System.gc();
        try {
            long[] hugeArrayThatWontGetAllocated = new long[Integer.MAX_VALUE];
            assertTrue(hugeArrayThatWontGetAllocated.length > 0);
        } catch (OutOfMemoryError e) {
            // Ignore OME
        }
        try {
            Thread.sleep(50); // give GC some time to finish enqueuing and finalizing all weakly-referenced objects
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Use a separate method for this, assuming that the compiler won't put the object on the using method's stack
     * @return
     */
    private Object createKeyObject() {
        return new Object();
    }
}
