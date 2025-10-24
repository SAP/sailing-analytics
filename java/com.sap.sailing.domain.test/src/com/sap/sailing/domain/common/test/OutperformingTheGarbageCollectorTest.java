package com.sap.sailing.domain.common.test;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

/**
 * Used to force specific garbage collection properties together with the G1GC implementation. By adjusting the
 * {@link #MAX_THREADPOOL_SIZE} to, say, 500, it is possible to generate a lot of garbage and hold on to it while they
 * are executing slowly in the massively oversized thread pool. This will cause the temporary objects to get promoted
 * into the GC's old generation where the mixed GC runs are too slow to keep up, ultimately forcing a Full GC.
 * See bug 3864 for more details. Use the launch configuration {@code OutperformingTheGarbageCollectorTest.launch}
 * in this project.<p>
 * 
 * The configuration is pretty chaotic, with fairly unpredictable impact of changing the constants
 * {@link #NUMBER_OF_OBJECTS_TO_CREATE_INITIALLY} and {@link #NUMBER_OF_FOLLOW_UP_OBJECTS_TO_CREATE}. On a four-core
 * machine, 100 threads with 50,000,000 initial objects to create and 100,000,000 follow-up objects to create
 * we reach a state where Full GC strikes.
 * 
 * @author Axel Uhl (D043530)
 *
 */
@Disabled("Unignore if you want to run GC tests")
public class OutperformingTheGarbageCollectorTest {
    private static final Logger logger = Logger.getLogger(OutperformingTheGarbageCollectorTest.class.getName());
    private static final int MAX_THREADPOOL_SIZE = 100;
    private static final int BATCH_SIZE = 100000;
    private static final int NUMBER_OF_OBJECTS_TO_CREATE_INITIALLY = 50000000;
    private static final int NUMBER_OF_FOLLOW_UP_OBJECTS_TO_CREATE = 100000000;
    private final Random random = new Random();
    private final ThreadPoolExecutor executor = new ThreadPoolExecutor(MAX_THREADPOOL_SIZE, MAX_THREADPOOL_SIZE, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>());
    private final ThreadLocal<HashMap<Integer, String>> mapTemporarilyHoldingGarbage = ThreadLocal.withInitial(HashMap::new);
    private int numberOfFollowUpTasksCreated;
    
    @Test
    public void testOutperformingTheGarbageCollector() throws InterruptedException {
        for (int i=0; i<NUMBER_OF_OBJECTS_TO_CREATE_INITIALLY/BATCH_SIZE; i++) {
            executor.execute(()->produceSomeContentAndRemoveSomeOtherContent());
        }
        logger.info("Done with producing tasks; waiting for task queue to become empty.");
        while (!executor.getQueue().isEmpty()) {
            logger.info("Queue still has "+executor.getQueue().size()+" elements;  "+
                    numberOfFollowUpTasksCreated+" of "+NUMBER_OF_FOLLOW_UP_OBJECTS_TO_CREATE/BATCH_SIZE+
                    " follow-up tasks produced; waiting...");
            Thread.sleep(1000);
        }
        logger.info("Task queue empty; terminating");
    }
    
    /**
     * Produces {@link #BATCH_SIZE} map entries and then turns them into garbage again by removing them again from the map.
     * If follow-up tasks for less than {@link #NUMBER_OF_FOLLOW_UP_OBJECTS_TO_CREATE} have been created, enqueue another one.
     */
    private void produceSomeContentAndRemoveSomeOtherContent() {
        for (int i=0; i<BATCH_SIZE; i++) {
            mapTemporarilyHoldingGarbage.get().put(random.nextInt(Integer.MAX_VALUE), ""+random.nextDouble());
        }
        final Iterator<Entry<Integer, String>> i = mapTemporarilyHoldingGarbage.get().entrySet().iterator();
        int count = 0;
        try {
            while (i.hasNext() && count++ < BATCH_SIZE) {
                i.next();
                i.remove();
            }
        } catch (NoSuchElementException e) {
            // ignore because some other thread may already have removed the last element, and that's OK then
            logger.log(Level.INFO, "Very strange", e);
        }
        if (numberOfFollowUpTasksCreated < NUMBER_OF_FOLLOW_UP_OBJECTS_TO_CREATE/BATCH_SIZE) {
            numberOfFollowUpTasksCreated++;
            executor.execute(()->produceSomeContentAndRemoveSomeOtherContent());
        }
    }
}
