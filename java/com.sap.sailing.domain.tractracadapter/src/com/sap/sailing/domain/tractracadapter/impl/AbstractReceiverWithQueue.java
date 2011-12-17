package com.sap.sailing.domain.tractracadapter.impl;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.tracking.DynamicTrackedEvent;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedEvent;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tractracadapter.DomainFactory;
import com.sap.sailing.domain.tractracadapter.Receiver;
import com.sap.sailing.util.Util.Triple;
import com.tractrac.clientmodule.Event;
import com.tractrac.clientmodule.Race;

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
    
    private final LinkedBlockingQueue<Triple<A, B, C>> queue;
    private final DomainFactory domainFactory;
    private final com.tractrac.clientmodule.Event tractracEvent;
    private final DynamicTrackedEvent trackedEvent;
    private Thread thread;

    /**
     * used by {@link #stopAfterNotReceivingEventsForSomeTime(long)} and {@link #run()} to check if an event was received
     * during the timeout period.
     */
    private boolean receivedEventSinceDuringTimeout;
    
    public AbstractReceiverWithQueue(DomainFactory domainFactory, Event tractracEvent, DynamicTrackedEvent trackedEvent) {
        super();
        this.tractracEvent = tractracEvent;
        this.trackedEvent = trackedEvent;
        this.domainFactory = domainFactory;
        this.queue = new LinkedBlockingQueue<Triple<A, B, C>>();
    }
    
    protected synchronized void setAndStartThread(Thread thread) {
        this.thread = thread;
        thread.start();
    }
    
    protected DomainFactory getDomainFactory() {
        return domainFactory;
    }
    
    protected com.tractrac.clientmodule.Event getTracTracEvent() {
        return tractracEvent;
    }
    
    protected DynamicTrackedEvent getTrackedEvent() {
        return trackedEvent;
    }
    
    public void stopPreemptively() {
        // mark the end and hence terminate the thread by adding a null/null/null event to the queue
        queue.clear();
        stopAfterProcessingQueuedEvents();
    }
    
    @Override
    public void stopAfterProcessingQueuedEvents() {
        queue.add(new Triple<A, B, C>(null, null, null));
    }
    
    @Override
    public void stopAfterNotReceivingEventsForSomeTime(final long timeoutInMilliseconds) {
        receivedEventSinceDuringTimeout = false;
        TracTracRaceTrackerImpl.scheduler.schedule(new Runnable() {
            public void run() {
                if (!receivedEventSinceDuringTimeout) {
                    logger.info("Stopping receiver "+this+" of class "+this.getClass().getName()+
                                " after not having received an event during "+timeoutInMilliseconds+"ms");
                    stopAfterProcessingQueuedEvents();
                } else {
                    TracTracRaceTrackerImpl.scheduler.schedule(this, timeoutInMilliseconds, TimeUnit.MILLISECONDS);
                }
            }
        }, timeoutInMilliseconds, TimeUnit.MILLISECONDS);
    }

    protected void enqueue(Triple<A, B, C> event) {
        queue.add(event);
    }
    
    private boolean isStopEvent(Triple<A, B, C> event) {
        return event.getA() == null && event.getB() == null && event.getC() == null;
    }

    @Override
    public void run() {
        Triple<A, B, C> event = null;
        while (event == null || !isStopEvent(event)) {
            try {
                event = queue.take();
                if (!isStopEvent(event)) {
                    receivedEventSinceDuringTimeout = true;
                    handleEvent(event);
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
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

    protected abstract void handleEvent(Triple<A, B, C> event);

    /**
     * Tries to find a {@link TrackedRace} for <code>race</code> in the {@link com.sap.sailing.domain.base.Event} corresponding
     * to {@link #tractracEvent}, as keyed by the {@link #domainFactory}. If the {@link RaceDefinition} for <code>race</code>
     * is not found in the {@link com.sap.sailing.domain.base.Event}, <code>null</code> is returned. If the {@link TrackedRace}
     * for <code>race</code> isn't found in the {@link TrackedEvent}, <code>null</code> is returned, too.
     */
    protected DynamicTrackedRace getTrackedRace(Race race) {
        DynamicTrackedRace result = null;
        RaceDefinition raceDefinition = getDomainFactory().getAndWaitForRaceDefinition(race);
        com.sap.sailing.domain.base.Event domainEvent = getDomainFactory().getOrCreateEvent(getTracTracEvent());
        if (domainEvent.getRaceByName(raceDefinition.getName()) != null) {
            result = trackedEvent.getTrackedRace(raceDefinition);
        }
        return result;
    }
}
