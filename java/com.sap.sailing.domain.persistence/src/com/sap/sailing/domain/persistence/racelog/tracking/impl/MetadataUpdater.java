package com.sap.sailing.domain.persistence.racelog.tracking.impl;

import java.util.concurrent.Callable;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.DeviceIdentifier;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Timed;
import com.sap.sse.common.TransformationException;
import com.sap.sse.util.ThreadPoolUtil;

/**
 * Manages the actual updates to the {@link MetadataCollection}, batching the update statements to the MongoDB by
 * {@link MetadataUpdate#merge(MetadataUpdate) combining} update requests
 * {@link #enqueueMetadataUpdate(DeviceIdentifier, Object, int, TimeRange, Timed) enqueued} with this updater in case an
 * update is currently running.
 * <p>
 * 
 * When no update task is currently running (indicated by {@link #runningUpdateTask} being {@code null}, this updater
 * will schedule a task with the {@link #executor} when a metadata update is
 * {@link #enqueueMetadataUpdate(DeviceIdentifier, Object, int, TimeRange, Timed) enqueued} and assign it to the
 * {@link #runningUpdateTask} field. The update enqueued is stored in the {@link #nextUpdate} field. The task scheduled
 * with the executor will pick it up, assign it to the {@link #currentUpdate} field, and the {@link #nextUpdate} field
 * is cleared. The update is then applied by invoking {@link MetadataCollection#update(MetadataUpdate)} on the
 * {@link #metadataCollection} for the {@link #currentUpdate}. When the update has been processed successfully,
 * {@link #updatesProcessed} is incremented and waiters on this object are all notified. Then, the running task looks
 * again for another update in the {@link #nextUpdate} field. If nothing is found, {@link #currentUpdate} is cleared the
 * task is terminated, and {@link #runningUpdateTask} is cleared. Otherwise, the process repeats as described above,
 * with the {@link #nextUpdate} being assigned to {@link #currentUpdate}, {@link #nextUpdate} being cleared, and so on.
 * <p>
 * 
 * If an update is enqueued while there already is another update waiting as {@link #nextUpdate} to be processed by the
 * {@link #runningUpdateTask}, the new update request is {@link MetadataUpdate#merge(MetadataUpdate) merged} with
 * the request already enqueued before the running update task will pick up that merged update request with the merge
 * result being set to the {@link #nextUpdate} field.
 * <p>
 * 
 * All manipulations to {@link #runningUpdateTask}, {@link #currentUpdate}, {@link #nextUpdate} and {@link #updatesProcessed}
 * happen under this object's monitor ({@code synchronized}).<p>
 * 
 * The {@link #waitForPendingUpdates()} method can be used to wait until the pending update request(s) up to the point
 * when {@link #waitForPendingUpdates()} is called have been sent to MongoDB.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class MetadataUpdater {
    private static final Logger logger = Logger.getLogger(MetadataUpdater.class.getName());
    private final ScheduledExecutorService executor;
    private final DeviceIdentifier forDevice;
    private final MetadataCollection metadataCollection;
    private Future<?> runningUpdateTask;
    private MetadataUpdate<?> nextUpdate;
    private MetadataUpdate<?> currentUpdate;
    
    /**
     * A counter for the updates processed. Can be used for synchronization purposes, e.g., by {@link #waitForPendingUpdates()}.
     */
    private long updatesProcessed;

    MetadataUpdater(ScheduledExecutorService executor, MetadataCollection metadataCollection, DeviceIdentifier forDevice) {
        super();
        this.executor = executor;
        this.metadataCollection = metadataCollection;
        this.forDevice = forDevice;
    }
    
    /**
     * Uses the {@link ThreadPoolUtil#getDefaultBackgroundTaskThreadPoolExecutor() default background thread pool
     * executor} for updating the metadata.
     * 
     * @param forDevice
     *            used to validate the constraint that all updates delivered to this updater must be for that device
     */
    MetadataUpdater(MetadataCollection metadataCollection, DeviceIdentifier forDevice) {
        this(ThreadPoolUtil.INSTANCE.getDefaultBackgroundTaskThreadPoolExecutor(), metadataCollection, forDevice);
    }
    
    synchronized <FixT extends Timed> void enqueueMetadataUpdate(DeviceIdentifier device, final Object dbDeviceId,
            final int nrOfTotalFixes, TimeRange fixesTimeRange, FixT latestFix) throws TransformationException {
        final MetadataUpdate<FixT> update = new MetadataUpdate<>(device, dbDeviceId, nrOfTotalFixes, fixesTimeRange, latestFix);
        if (runningUpdateTask == null) { // nothing running
            setNextUpdate(update);   // store the update request
            scheduleUpdate();        // and launch the task processing it
        } else {                           // an update task is already running
            if (getNextUpdate() == null) { // and there is no next update scheduled yet;
                setNextUpdate(update);     // store it, and the running task will pick it up
            } else {                       // there already is another metadata update scheduled
                final MetadataUpdate<FixT> theNextUpdate = getNextUpdate();
                setNextUpdate(theNextUpdate.merge(update)); // merge the current update request with the one already scheduled
            }
        }
    }
    
    /**
     * Schedules a task that under this object's monitor ({@code synchronized}) obtains the {@link #getNextUpdate() next update}
     * and if one is found, {@link MetadataCollection#update(MetadataUpdate) updates} the MongoDB metadata collection accordingly.
     * Before terminating, this object's monitor is acquired again, and if then another {@link #getNextUpdate() next update} is
     * found, it is applied again; otherwise, the task ends.
     */
    private synchronized <FixT extends Timed> void scheduleUpdate() {
        assert runningUpdateTask == null && nextUpdate != null;
        runningUpdateTask = executor.submit((Callable<Void>) ()->{
            logger.fine(()->"Starting metadata updater task for device "+forDevice);
            boolean currentUpdateNullInSynchronizedBlock; // the "finished" marker, outside of synchronized blocks equivalent to runningUpdateTask == null
            synchronized (MetadataUpdater.this) {
                currentUpdate = getNextUpdate();
                // need to remember the currentUpdate null status from within the synchronized block;
                // if runningUpdateTask is set to null because currentUpdate was null, and the synchronized
                // block is left, another thread may call scheduleUpdate and start and set a new runningUpdateTask
                // with a new currentUpdate; if this thread then sees currentUpdate != null, it would erroneously
                // continue to run, on the same currentUpdate that has scheduled a new runningUpdateTask
                currentUpdateNullInSynchronizedBlock = currentUpdate == null;
                if (currentUpdateNullInSynchronizedBlock) {
                    runningUpdateTask = null;
                } else {
                    setNextUpdate(null);
                }
            }
            do {
                if (!currentUpdateNullInSynchronizedBlock) {
                    boolean success = false;
                    int retryCount = 3;
                    do {
                        try {
                            metadataCollection.update(currentUpdate);
                            success = true;
                        } catch (Exception e) {
                            logger.severe("Unable to write update "+currentUpdate+" to the metadata collection for device "+forDevice+
                                    ": "+e.getMessage()+"; retrying "+retryCount+" more times.");
                            Thread.sleep(3000);
                        }
                    } while (!success && retryCount-- > 0);
                    synchronized (MetadataUpdater.this) {
                        currentUpdate = getNextUpdate();
                        currentUpdateNullInSynchronizedBlock = currentUpdate == null;
                        if (currentUpdateNullInSynchronizedBlock) {
                            runningUpdateTask = null;
                        } else {
                            setNextUpdate(null);
                        }
                        updatesProcessed++; // we increment even in case of repeated failure to unblock waiters
                        MetadataUpdater.this.notifyAll();
                    }
                }
            } while (!currentUpdateNullInSynchronizedBlock);
            logger.fine(()->"Terminating metadata updater task for device "+forDevice);
            return null;
        });
    }

    private <FixT extends Timed> void setNextUpdate(MetadataUpdate<FixT> update) {
        assert update == null || update.getDevice().equals(forDevice);
        nextUpdate = update;
    }

    @SuppressWarnings("unchecked")
    private synchronized <FixT extends Timed> MetadataUpdate<FixT> getNextUpdate() {
        return (MetadataUpdate<FixT>) nextUpdate;
    }

    /**
     * An update may either currently be in progress, on its way to MongoDB, with no {@link #nextUpdate} in the queue,
     * or an update may have been queued in {@link #nextUpdate} which will be picked up by an already running update
     * task and will then be in progress, or non {@link #runningUpdateTask} is active in which case the method returns
     * immediately.
     */
    void waitForPendingUpdates() {
        synchronized (this) {
            if (runningUpdateTask != null) {
                final long waitUntilTheseManyUpdatesProcessed = getNextUpdate() == null ?
                        (currentUpdate == null ? updatesProcessed : updatesProcessed+1) :
                        (currentUpdate == null ? updatesProcessed+1 : updatesProcessed + 2);
                while (updatesProcessed < waitUntilTheseManyUpdatesProcessed) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        logger.log(Level.WARNING, "Interrupted while waiting for pending updates; continuing to wait...", e);
                    }
                }
            }
        }
    }
}
