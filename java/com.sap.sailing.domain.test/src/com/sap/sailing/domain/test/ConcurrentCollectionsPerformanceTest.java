package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util;
import com.sap.sse.concurrent.ConcurrentHashBag;

/**
 * Measures the performance of different concurrent collections types and shows their add and remove behavior for
 * different collection sizes and different numbers of concurrently accessing threads. The collections used must be able
 * to hold the same element multiple times, keeping count, not necessarily order.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ConcurrentCollectionsPerformanceTest {
    private static final int COUNT = 10000;
    
    @Test
    public void testConcurrentLinkedQueue() {
        Collection<Object> c = new ConcurrentLinkedQueue<Object>();
        System.out.println("ConcurrentLinkedQueue" + runWith(c));
    }

    @Test
    public void testConcurrentHashMap() {
        Collection<Object> c = new ConcurrentHashBag<Object>();
        System.out.println("ConcurrentHashBag" + runWith(c));
    }

    private com.sap.sse.common.Util.Pair<Long, Long> runWith(Collection<Object> c) {
        List<Object> l = new ArrayList<>(COUNT);
        for (int i=0; i<COUNT; i++) {
            l.add(new Object());
        }
        // add all objects a second time to ensure
        for (Object o : new ArrayList<Object>(l)) {
            l.add(o);
        }
        long startInsert = System.currentTimeMillis();
        for (Object o : l) {
            c.add(o);
        }
        long endInsert = System.currentTimeMillis();
        assertEquals(l.size(), c.size());
        Collections.shuffle(l);
        long startRemove = System.currentTimeMillis();
        for (Object o : l) {
            c.remove(o);
        }
        long endRemove = System.currentTimeMillis();
        return new Util.Pair<Long, Long>(endInsert-startInsert, endRemove-startRemove);
    }
}
