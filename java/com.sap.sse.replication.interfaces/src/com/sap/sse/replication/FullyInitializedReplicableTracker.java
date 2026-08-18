package com.sap.sse.replication;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.osgi.framework.BundleContext;
import org.osgi.framework.Filter;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.sap.sse.replication.ReplicationService.ReplicationStartingListener;
import com.sap.sse.util.ServiceTrackerFactory;

/**
 * While a regular OSGi {@link ServiceTracker} would {@link ServiceTracker#waitForService(long) wait} for the service's
 * appearance in the OSGi registry and then return it, this specialization is aware of the {@link ReplicationService}
 * and understands the replication life cycle which can be in {@link ReplicationService#isReplicationStarting()
 * starting} mode, and furthermore each replicable can be in the process of handling its initial load. This tracker has
 * a specialized {@link #waitForService(long)} implementation that waits for the {@link ReplicationService} to not be in
 * state {@link ReplicationService#isReplicationStarting()}, waits for the replicable requested using the regular
 * (super-class) {@link ServiceTracker#waitForService(long)} method and asserts the {@link Replicable} to return is
 * not in the state {@link Replicable#isCurrentlyFillingFromInitialLoad()}.<p>
 * 
 * By analogy, the {@link #getService()} and related methods return only services for which the above rules apply.
 * In particular, {@link #getService()} will return {@code null} if the {@link ReplicationService} is currently in mode
 * {@link ReplicationService#isReplicationStarting()} or the replicable found by the regular OSGi tracker is currently
 * {@link Replicable#isCurrentlyFillingFromInitialLoad() receiving its initial load}.<p>
 * 
 * Note that {@link #waitForService(long)} must not be used during the activation of a {@link Replicable}'s bundle before
 * that {@link Replicable} has been registered with the OSGi registry. Otherwise, a deadlock can result because the waiting
 * {@link Replicable} will hold up the completion of the replication start-up and hence wait forever.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <R>
 *            the type of {@link Replicable} to track
 */
public class FullyInitializedReplicableTracker<R extends Replicable<?, ?>> extends ServiceTracker<R, R> {
    /**
     * Used to find the {@link ReplicationService}. If {@code null}, no attempt will be made to look
     * for the {@link ReplicationService}, and only the replicable's {@link Replicable#isCurrentlyFillingFromInitialLoad()}
     * result will be considered.
     */
    private final ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker;

    /**
     * Same as {@link #createAndOpen(BundleContext, Class, ServiceTrackerCustomizer)}, but using {@code null} for the customizer.
     */
    public static <R extends Replicable<?, ?>> FullyInitializedReplicableTracker<R> createAndOpen(BundleContext context, Class<R> clazz) {
        return createAndOpen(context, clazz, /* customizer */ null);
    }

    public static <R extends Replicable<?, ?>> FullyInitializedReplicableTracker<R> createAndOpen(BundleContext context, Class<R> clazz,
            ServiceTrackerCustomizer<R, R> customizer) {
        final FullyInitializedReplicableTracker<R> result = new FullyInitializedReplicableTracker<R>(context, clazz, customizer,
                ServiceTrackerFactory.createAndOpen(context, ReplicationService.class));
        result.open();
        return result;
    }
    
    public FullyInitializedReplicableTracker(BundleContext context, Class<R> clazz,
            ServiceTrackerCustomizer<R, R> customizer,
            ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker) {
        super(context, clazz, customizer);
        this.replicationServiceTracker = replicationServiceTracker;
    }

    public FullyInitializedReplicableTracker(BundleContext context, Filter filter,
            ServiceTrackerCustomizer<R, R> customizer,
            ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker) {
        super(context, filter, customizer);
        this.replicationServiceTracker = replicationServiceTracker;
    }

    public FullyInitializedReplicableTracker(BundleContext context, ServiceReference<R> reference,
            ServiceTrackerCustomizer<R, R> customizer,
            ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker) {
        super(context, reference, customizer);
        this.replicationServiceTracker = replicationServiceTracker;
    }

    public FullyInitializedReplicableTracker(BundleContext context, String clazz,
            ServiceTrackerCustomizer<R, R> customizer,
            ServiceTracker<ReplicationService, ReplicationService> replicationServiceTracker) {
        super(context, clazz, customizer);
        this.replicationServiceTracker = replicationServiceTracker;
    }
    
    /**
     * @param timeoutInMillis
     *            0 means indefinite waiting time
     * @return {@code true} if the {@link #replicationServiceTracker} is {@code null} or the {@link ReplicationService}
     *         was obtained successfully and has been in or has reached the state of not
     *         {@link ReplicationService#isReplicationStarting()} within the timeout provided. {@code false} otherwise.
     */
    private boolean waitForReplicationToBeInitialized(long timeoutInMillis) throws InterruptedException {
        final boolean result;
        if (replicationServiceTracker != null) {
            final CountDownLatch latch = new CountDownLatch(1); // counted down either by the direct check or the listener
            final ReplicationService replicationService = replicationServiceTracker.waitForService(timeoutInMillis);
            if (replicationService != null) {
                final ReplicationStartingListener replicationStartingListener = newIsReplicationStarting->{
                    if (!newIsReplicationStarting) {
                        latch.countDown();
                    }
                };
                replicationService.addReplicationStartingListener(replicationStartingListener);
                if (!replicationService.isReplicationStarting()) {
                    latch.countDown();
                }
                if (timeoutInMillis == 0) {
                    latch.await();
                    result = true;
                } else {
                    result = latch.await(timeoutInMillis, TimeUnit.MILLISECONDS);
                }
                replicationService.removeReplicationStartingListener(replicationStartingListener);
            } else {
                result = false; // replication service tracker was set, but the service didn't show up within the timeout period
            }
        } else {
            result = true;
        }
        return result;
    }

    /**
     * Waits for one service object tracked to appear for {@code timeoutInMillis} milliseconds (see
     * {@link #waitForService(long)}). If no such service object can be found before timing out, {@code null}
     * is returned. Once a service object has been retrieved and a non-{@code null} {@link #replicationServiceTracker}
     * has been provided at construction time, the {@link ReplicationService} is obtained from that tracker by
     * waiting for it at least {@code timeoutInMillis} milliseconds and then is asked to wait for the replication
     * to be fully initialized, so in particular having received and incorporated the initial load.
     * 
     * @param timeoutInMillis
     *            0 means indefinite wait time
     * @return {@code null} if no service was obtained from the registry in the timeout specified or the replication did
     *         not reach a fully initialized state in the timeout specified.
     */
    public R getInitializedService(long timeoutInMillis) throws InterruptedException {
        final R service = waitForService(timeoutInMillis);
        final R result;
        if (service != null) {
            if (waitForReplicationToBeInitialized(timeoutInMillis)) {
                result = service;
            } else {
                result = null;
            }
        } else {
            result = null;
        }
        return result;
    }
}
