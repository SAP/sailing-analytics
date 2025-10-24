package com.sap.sailing.util.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.Iterator;
import java.util.Random;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.concurrent.ConcurrentHashBag;

/**
 * Initiated by bug 2502 (http://bugzilla.sapsailing.com/bugzilla/show_bug.cgi?id=2502), this test tries to reproduce a
 * race condition that can lead to negative sizes and therefore negative array indices.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class ConcurrentHashBagTest {
    final static Random random = new Random();
    private ConcurrentHashBag<String> bag;
    
    @BeforeEach
    public void setUp() {
        bag = new ConcurrentHashBag<>();
    }
    
    /**
     * Trying to provoke a negative size() value using two different keys
     */
    @Test
    public void testMassConcurrentInsertAndRemove() throws InterruptedException {
        final boolean[] sawNegativeSize = new boolean[1];
        final boolean[] stopped = new boolean[1];
        final Runnable adderRemover = () -> {
            while (!stopped[0]) {
                bag.add("a");
                bag.add("b");
                bag.remove("a");
                bag.remove("b");
            }
        };
        Thread t1 = new Thread(adderRemover);
        Thread t2 = new Thread(adderRemover);
        Thread t3 = new Thread(adderRemover);
        Thread t4 = new Thread(adderRemover);
        Thread t5 = new Thread(() -> {
            while (!stopped[0]) {
                if (bag.size() < 0) {
                    sawNegativeSize[0] = true;
                }
            }
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        Thread.sleep(1000);
        stopped[0] = true;
        assertFalse(sawNegativeSize[0]);
    }

    /**
     * Trying to provoke a negative size() value using only one key
     */
    @Test
    public void testMassConcurrentInsertAndRemoveWithSingleKey() throws InterruptedException {
        final boolean[] sawNegativeSize = new boolean[1];
        final boolean[] stopped = new boolean[1];
        final Runnable adderRemover = () -> {
            while (!stopped[0]) {
                bag.add("a");
                bag.remove("a");
            }
        };
        Thread t1 = new Thread(adderRemover);
        Thread t2 = new Thread(adderRemover);
        Thread t3 = new Thread(adderRemover);
        Thread t4 = new Thread(adderRemover);
        Thread t5 = new Thread(() -> {
            while (!stopped[0]) {
                if (bag.size() < 0) {
                    sawNegativeSize[0] = true;
                }
            }
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        Thread.sleep(1000);
        stopped[0] = true;
        assertFalse(sawNegativeSize[0]);
    }

    /**
     * Trying to provoke a negative size() value using only one key
     */
    @Test
    public void testMassConcurrentInsertAndRemoveWithSingleKeyRemovingMoreThanAdded() throws InterruptedException {
        final boolean[] sawNegativeSize = new boolean[1];
        final boolean[] stopped = new boolean[1];
        final Runnable aAdderRemover = () -> {
            while (!stopped[0]) {
                bag.add("a");
                bag.remove("a");
                bag.remove("a");
            }
        };
        final Runnable randomAdderRemover = () -> {
            while (!stopped[0]) {
                final String s = ""+((char)(((int) 'A')+random.nextInt(26)))+((char)(((int)'A')+random.nextInt(26)));
                bag.add(s);
            }
        };
        Thread t1 = new Thread(aAdderRemover);
        Thread t2 = new Thread(randomAdderRemover);
        Thread t3 = new Thread(randomAdderRemover);
        Thread t4 = new Thread(randomAdderRemover);
        Thread t5 = new Thread(() -> {
            while (!stopped[0]) {
                if (bag.size() < 0) {
                    sawNegativeSize[0] = true;
                }
            }
        });
        t1.start();
        t2.start();
        t3.start();
        t4.start();
        t5.start();
        Thread.sleep(1000);
        stopped[0] = true;
        assertFalse(sawNegativeSize[0]);
    }

    private static class BooleanHolder {
        public volatile boolean b;
    }
    
    @Test
    public void testMassConcurrentInsertAndRemoveWithRandomizedReentrance() throws InterruptedException {
        final ConcurrentHashBag<Thread> bag = new ConcurrentHashBag<>();

        final int NUMBER_OF_THREADS = 500;
        final boolean[] sawNegativeSize = new boolean[1];
        final BooleanHolder stopped = new BooleanHolder();
        final Runnable aAdderRemover = () -> {
            final Thread currentThread = Thread.currentThread();
            while (!stopped.b) {
                bag.add(Thread.currentThread());
                bag.remove(currentThread);
            }
        };
        final Runnable randomAdderRemover = () -> {
            final Thread currentThread = Thread.currentThread();
            while (!stopped.b) {
                final int reentranceLevel = 1+random.nextInt(2);
                for (int i=0; i<reentranceLevel; i++) {
                    bag.add(currentThread);
                }
                for (int i=0; i<reentranceLevel; i++) {
                    bag.remove(currentThread);
                }
            }
        };
        final Thread[] threads = new Thread[NUMBER_OF_THREADS];
        Thread t1 = new Thread(aAdderRemover);
        t1.start();
        for (int i=0; i<NUMBER_OF_THREADS; i++) {
            Thread t2 = new Thread(randomAdderRemover);
            threads[i] = t2;
            t2.start();
        }
        Thread t5 = new Thread(() -> {
            while (!stopped.b) {
                if (bag.size() < 0) {
                    sawNegativeSize[0] = true;
                }
            }
        });
        t5.start();
        Thread.sleep(2000);
        stopped.b = true;
        assertFalse(sawNegativeSize[0]);
        t1.join();
        t5.join();
        for (int i=0; i<NUMBER_OF_THREADS; i++) {
            threads[i].join();
        }
    }
    
    @Test
    public void testIteratorRemove() {
        bag.add("123");
        bag.add("234");
        bag.add("345");
        bag.add("234");
        bag.add("345");
        bag.add("0");
        final int sizeBeforeRemove = bag.size();
        for (Iterator<String> i=bag.iterator(); i.hasNext(); ) {
            String next = i.next();
            if (next.equals("123")) {
                i.remove();
            }
        }
        assertFalse(bag.contains("123"));
        assertEquals(sizeBeforeRemove-1, bag.size());
    }

    @Test
    public void testIteratorRemoveFirstOfMultiple() {
        bag.add("123");
        bag.add("234");
        bag.add("345");
        bag.add("234");
        bag.add("345");
        bag.add("0");
        final int sizeBeforeRemove = bag.size();
        for (Iterator<String> i=bag.iterator(); i.hasNext(); ) {
            String next = i.next();
            if (next.equals("234")) {
                i.remove();
                break;
            }
        }
        assertTrue(bag.contains("234"));
        assertEquals(sizeBeforeRemove-1, bag.size());
    }
    
    @Test
    public void testIteratorRemoveThrowsExceptionBeforeNext() {
        bag.add("123");
        bag.add("234");
        bag.add("345");
        bag.add("234");
        bag.add("345");
        bag.add("0");
        Iterator<String> i=bag.iterator();
        try {
            i.remove();
            fail("Expected IllegalArgumentException");
        } catch (IllegalStateException e) {
            // this is expected
        }
    }

    @Test
    public void testIteratorRemoveThrowsExceptionAfterRemoveWithoutNext() {
        bag.add("123");
        bag.add("234");
        bag.add("345");
        bag.add("234");
        bag.add("345");
        bag.add("0");
        Iterator<String> i=bag.iterator();
        i.next();
        i.remove();
        try {
            i.remove();
            fail("Expected IllegalArgumentException");
        } catch (IllegalStateException e) {
            // this is expected
        }
    }
}