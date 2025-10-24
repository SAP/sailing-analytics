package com.sap.sailing.domain.tractracadapter.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.LoadingQueueDoneCallBack;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Triple;
import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.model.lib.api.event.IRace;
import com.tractrac.model.lib.api.map.IMapItem;
import com.tractrac.model.lib.api.map.IPositionedItem;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.IRaceSubscriber;

/**
 * Some event receiver that can be executed in a thread because it's a runnable, and
 * manages a queue of events received. The events are expected to be triplets.<p>
 * 
 * The receiver can be stopped in different ways. 
 * 
 * @author Axel Uhl (d043530)
 */
public abstract class AbstractReceiverWithQueue<A, B, C> implements Runnable, Receiver {
    private static Logger logger = Logger.getLogger(AbstractReceiverWithQueue.class.getName());

    /**
     * The timeout after which to re-try looking for the race definition to have appeared in
     * {@link #getTrackedRace(IRace)} in case an infinite timeout (-1) was specified. During this
     * short break, {@link #getTrackedRace(IRace)} will check for {@link #hasBeenStoppedPreemptively()}
     * and will stop trying to look for the race if this received was stopped preemptively.
     */
    private static final long RETRY_TIMEOUT_IN_MILLIS = Duration.ONE_MINUTE.asMillis();

    private final LinkedBlockingDeque<Util.Triple<A, B, C>> queue;
    private final DomainFactory domainFactory;
    private final IEvent tractracEvent;
    private final IEventSubscriber eventSubscriber;
    private final IRaceSubscriber raceSubscriber;
    private final DynamicTrackedRegatta trackedRegatta;
    private final Simulator simulator;
    private final Thread thread;
    private final Map<Util.Triple<A, B, C>, Set<LoadingQueueDoneCallBack>> loadingQueueDoneCallBacks;
    private final long timeoutInMilliseconds;

    /**
     * used by {@link #stopAfterNotReceivingEventsForSomeTime(long)} and {@link #run()} to check if an event was received
     * during the timeout period.
     */
    private boolean receivedEventDuringTimeout;
    
    /**
     * Set by {@link #stopPreemptively()}. Invocations of {@link #handleEvent(Triple)} waiting for something
     * should wait repeatedly with some timeout, such as, say, one minute, and keep re-trying until what they
     * are waiting for has appeared, or this flag has been set in which case they can abort and assume they
     * received a stop event.
     */
    private boolean stoppedPreemptively;
    
    public AbstractReceiverWithQueue(DomainFactory domainFactory, IEvent tractracEvent,
            DynamicTrackedRegatta trackedRegatta, Simulator simulator, IEventSubscriber eventSubscriber, IRaceSubscriber raceSubscriber,
            long timeoutInMilliseconds) {
        super();
        this.eventSubscriber = eventSubscriber;
        this.raceSubscriber = raceSubscriber;
        this.tractracEvent = tractracEvent;
        this.trackedRegatta = trackedRegatta;
        this.domainFactory = domainFactory;
        this.simulator = simulator;
        this.queue = new LinkedBlockingDeque<Util.Triple<A, B, C>>();
        this.thread = new Thread(this, getClass().getName());
        this.loadingQueueDoneCallBacks = new HashMap<>();
        this.timeoutInMilliseconds = timeoutInMilliseconds;
    }
    
    protected IEventSubscriber getEventSubscriber() {
        return eventSubscriber;
    }

    protected IRaceSubscriber getRaceSubscriber() {
        return raceSubscriber;
    }

    protected synchronized void startThread() {
        thread.start();
    }
    
    protected DomainFactory getDomainFactory() {
        return domainFactory;
    }
    
    protected IEvent getTracTracEvent() {
        return tractracEvent;
    }
    
    protected DynamicTrackedRegatta getTrackedRegatta() {
        return trackedRegatta;
    }
    
    public void stopPreemptively() {
        // mark the end and hence terminate the thread by adding a null/null/null event to the queue
        queue.clear();
        stopAfterProcessingQueuedEvents();
        stoppedPreemptively = true;
    }
    
    protected boolean hasBeenStoppedPreemptively() {
        return stoppedPreemptively;
    }
    
    @Override
    public void stopAfterProcessingQueuedEvents() {
        queue.add(new Util.Triple<A, B, C>(null, null, null));
    }
    
    protected Simulator getSimulator() {
        return simulator;
    }
    
    protected abstract void unsubscribe();
    
    @Override
    public void stopAfterNotReceivingEventsForSomeTime(final long timeoutInMilliseconds) {
        receivedEventDuringTimeout = false;
        TracTracRaceTrackerImpl.scheduler.schedule(new Runnable() {
            public void run() {
                if (!receivedEventDuringTimeout) {
                    logger.info("Stopping receiver "+AbstractReceiverWithQueue.this+
                                " of class "+AbstractReceiverWithQueue.this.getClass().getName()+
                                " after not having received an event during "+timeoutInMilliseconds+"ms");
                    stopAfterProcessingQueuedEvents();
                } else {
                    logger.info("Rescheduling the stopping of receiver "+AbstractReceiverWithQueue.this+
                            " for another "+timeoutInMilliseconds+"ms");
                    receivedEventDuringTimeout = false;
                    TracTracRaceTrackerImpl.scheduler.schedule(this, timeoutInMilliseconds, TimeUnit.MILLISECONDS);
                }
            }
        }, timeoutInMilliseconds, TimeUnit.MILLISECONDS);
    }

    protected void enqueue(Util.Triple<A, B, C> event) {
        queue.add(event);
        receivedEventDuringTimeout = true;
    }
    
    private boolean isStopEvent(Util.Triple<A, B, C> event) {
        return event.getA() == null && event.getB() == null && event.getC() == null;
    }

    @Override
    public void run() {
        Util.Triple<A, B, C> event = null;
        while (event == null || !isStopEvent(event)) {
            try {
                final Set<LoadingQueueDoneCallBack> callBacks;
                event = queue.take();
                synchronized (loadingQueueDoneCallBacks) {
                    if (getSimulator() != null) {
                        // when simulator is running, loading is considered finished and all callbacks will
                        // be satisfied instantly
                        callBacks = new HashSet<>();
                        for (Set<LoadingQueueDoneCallBack> set : loadingQueueDoneCallBacks.values()) {
                            callBacks.addAll(set);
                        }
                        loadingQueueDoneCallBacks.clear();
                    } else {
                        // otherwise, check only if there are callbacks that registered at the event
                        // currently consumed and notify if any are found
                        callBacks = loadingQueueDoneCallBacks.remove(event);
                    }
                }
                if (!isStopEvent(event)) {
                    handleEvent(event);
                }
                if (callBacks != null) {
                    for (LoadingQueueDoneCallBack callback : callBacks) {
                        callback.loadingQueueDone(this);
                    }
                }
            } catch (InterruptedException e) {
                logger.log(Level.INFO, "Interrupted while taking element from queue", e);
            } catch (Exception e) {
                // before this thread terminates abnormally, at least log it:
                logger.log(Level.SEVERE, ""+this+" is terminating abnormally; the race will probably be left at LOADING (100%).", e);
                throw e;
            }
        }
        unsubscribe();
    }

    @Override
    public synchronized void join() throws InterruptedException {
        if (thread != null) {
            thread.join();
        }
    }

    @Override
    public synchronized void join(long timeoutInMilliseconds) throws InterruptedException {
        if (thread != null) {
            thread.join(timeoutInMilliseconds);
        }
    }

    protected abstract void handleEvent(Util.Triple<A, B, C> event);

    /**
     * Tries to find a {@link TrackedRace} for <code>race</code> in the {@link com.sap.sailing.domain.base.Regatta}
     * corresponding to {@link #tractracEvent}, as keyed by the {@link #domainFactory}. Waits for
     * {@link #timeoutInMilliseconds} milliseconds for the {@link RaceDefinition} to show up, or infinitely if
     * {@link #timeoutInMilliseconds} is -1, except this receiver is {@link #hasBeenStoppedPreemptively() stopped
     * preemptively}. If the race doesn't show up under these boundary conditions, <code>null</code> is returned. If the
     * {@link RaceDefinition} for <code>race</code> is not found in the {@link com.sap.sailing.domain.base.Regatta},
     * <code>null</code> is returned. If the {@link TrackedRace} for <code>race</code> isn't found in the
     * {@link TrackedRegatta}, <code>null</code> is returned, too.
     */
    protected DynamicTrackedRace getTrackedRace(IRace race) {
        DynamicTrackedRace result = null;
        final long effectiveTimeoutInMilliseconds = timeoutInMilliseconds == -1 ? RETRY_TIMEOUT_IN_MILLIS : timeoutInMilliseconds;
        RaceDefinition raceDefinition;
        while ((raceDefinition = getDomainFactory().getAndWaitForRaceDefinition(race.getId(), effectiveTimeoutInMilliseconds)) == null &&
                timeoutInMilliseconds == -1 && !hasBeenStoppedPreemptively());
        if (raceDefinition != null) {
            com.sap.sailing.domain.base.Regatta domainRegatta = trackedRegatta.getRegatta();
            if (domainRegatta.getRaceByName(raceDefinition.getName()) != null) {
                result = trackedRegatta.getTrackedRace(raceDefinition);
            }
        }
        return result;
    }
    
    @Override
    public void callBackWhenLoadingQueueIsDone(LoadingQueueDoneCallBack callback) {
        synchronized (loadingQueueDoneCallBacks) {
            // Synchronization: If the following peekLast() call returns an element that is consumed just before the
            // callback is added to loadingQueueDoneCallbacks then the callback will still be triggered because the
            // run() method also synchronizes on loadingQueueDoneCallBacks before checking for any call back to be
            // triggered for the event just consumed. Worst case: the event is taken from the queue but not yet
            // handled. In this case the peekLast() call will already return null and execute the loadingQueueDone(this)
            // callback already although the event's handling has not completed yet.
            Triple<A, B, C> lastInQueue = queue.peekLast();
            // when simulator is attached, consider loading already done; the simulator simulates "live" tracking
            if (lastInQueue == null || getSimulator() != null) {
                callback.loadingQueueDone(this);
            } else {
                Util.addToValueSet(loadingQueueDoneCallBacks, lastInQueue, callback);
            }
        }
    }
    
    protected void ensureAllSingleMarksOfCourseAreaAreCreated(final IRace tractracRace) {
        for (final IMapItem tractracControlPoint : getDomainFactory().getControlsForCourseArea(getTracTracEvent(),
                tractracRace.getCourseArea())) {
            if (!tractracControlPoint.isMultiple()) {
                final IPositionedItem mark = tractracControlPoint.getPositionedItems().get(0);
                getDomainFactory().getOrCreateMark(mark);
            }
        }
    }
    
    @Override
    public String toString() {
        return "Receiver "+getClass().getName()+" for regatta "+getTrackedRegatta().getRegatta().getName()+
                " in event "+getTracTracEvent().getName()+" with ID "+getTracTracEvent().getId();
    }
}
