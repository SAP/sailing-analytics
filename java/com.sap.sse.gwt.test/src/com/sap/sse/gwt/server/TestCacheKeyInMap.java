package com.sap.sse.gwt.server;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.ref.ReferenceQueue;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.CacheableRPCResult;

/**
 * The {@link CacheKey} class uses weak references that may be cleared. This test is to ensure that handling
 * {@link CacheKey} objects in hashed structures such as {@link HashMap}s works properly across clearing the reference
 * held by the key.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class TestCacheKeyInMap {
    private HashMap<CacheKey, String> map;
    private ReferenceQueue<CacheableRPCResult> queue;

    @BeforeEach
    public void setUp() {
        map = new HashMap<>();
        queue = new ReferenceQueue<>();
    }
    
    @Test
    public void testNonClearedReference() {
        final CacheableRPCResult cachedObject = new CacheableRPCResult() {};
        final CacheKey key = new CacheKey(/* serialization policy */ null, cachedObject, queue);
        final String value = "abc";
        map.put(key, value);
        assertSame(value, map.get(new CacheKey(/* serialization policy */ null, cachedObject, /* queue */ null)));
    }
    
    @Test
    public void testClearingReferenceAfterCaching() {
        final CacheableRPCResult cachedObject = new CacheableRPCResult() {};
        final CacheKey key = new CacheKey(/* serialization policy */ null, cachedObject, queue);
        final String value = "abc";
        map.put(key, value);
        key.clear();
        assertSame(value, map.get(key));
    }

    @Test
    public void testClearingReferenceBeforeCaching() {
        final CacheableRPCResult cachedObject = new CacheableRPCResult() {};
        final CacheKey key = new CacheKey(/* serialization policy */ null, cachedObject, queue);
        key.clear();
        final String value = "abc";
        map.put(key, value);
        assertSame(value, map.get(key));
    }

    @Test
    public void testRemoveAfterCaching() {
        final CacheableRPCResult cachedObject = new CacheableRPCResult() {};
        final CacheKey key = new CacheKey(/* serialization policy */ null, cachedObject, queue);
        final String value = "abc";
        map.put(key, value);
        key.clear();
        assertSame(value, map.remove(key));
        assertTrue(map.isEmpty());
    }
}
