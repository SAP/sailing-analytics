package com.sap.sailing.domain.tractracadapter.impl;

import java.net.URI;
import java.util.concurrent.atomic.AtomicInteger;

import com.tractrac.model.lib.api.event.IEvent;
import com.tractrac.subscription.lib.api.IEventSubscriber;
import com.tractrac.subscription.lib.api.SubscriberInitializationException;
import com.tractrac.subscription.lib.api.SubscriptionLocator;
import com.tractrac.subscription.lib.api.competitor.ICompetitorsListener;
import com.tractrac.subscription.lib.api.event.IConnectionStatusListener;
import com.tractrac.subscription.lib.api.event.IEventMessageListener;
import com.tractrac.subscription.lib.api.event.IServerTimeListener;
import com.tractrac.subscription.lib.api.map.IMapItemsListener;
import com.tractrac.subscription.lib.api.race.IRacesListener;
import com.tractrac.subscription.lib.api.race.IStartStopTimesChangeListener;

/**
 * A wrapper around a {@link IEventSubscriber} that can be shared across many {@link TracTracRaceTrackerImpl} instances
 * that each invoke {@link #start} and {@link #stop()} symmetrically. This wrapper manages a counter (as an
 * {@link AtomicInteger}) such that {@link #start} will only delegate to the instance wrapped if the counter is 0;
 * likewise, {@link #stop} will delegate only if the counter goes to 0.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class EventSubscriberWrapper implements IEventSubscriber {
    private IEventSubscriber delegate;
    private final IEvent tractracEvent;
    private final URI liveURI;
    private final URI storedURI;
    private int startCounter;
    
    public EventSubscriberWrapper(IEvent tractracEvent, URI liveURI, URI storedURI) throws SubscriberInitializationException {
        this.tractracEvent = tractracEvent;
        this.liveURI = liveURI;
        this.storedURI = storedURI;
        this.startCounter = 0;
        this.delegate = createEventSubscriber();
    }

    private IEventSubscriber createEventSubscriber() throws SubscriberInitializationException {
        return SubscriptionLocator.getSusbcriberFactory().createEventSubscriber(tractracEvent, liveURI, storedURI);
    }

    @Override
    public void subscribeConnectionStatus(IConnectionStatusListener listener) {
        delegate.subscribeConnectionStatus(listener);
    }

    @Override
    public void unsubscribeConnectionStatus(IConnectionStatusListener listener) {
        delegate.unsubscribeConnectionStatus(listener);
    }

    @Override
    public synchronized void start() {
        if (startCounter++ == 0) {
            delegate.start();
        }
    }

    @Override
    public synchronized void stop() {
        if (--startCounter == 0) {
            delegate.stop();
            try {
                delegate = createEventSubscriber();
            } catch (SubscriberInitializationException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public boolean isRunning() {
        return delegate.isRunning();
    }

    @Override
    public void subscribeMapItems(IMapItemsListener listener) {
        delegate.subscribeMapItems(listener);
    }

    @Override
    public void unsubscribeMapItems(IMapItemsListener listener) {
        delegate.unsubscribeMapItems(listener);
    }

    @Override
    public void subscribeEventTimesChanges(IStartStopTimesChangeListener listener) {
        delegate.subscribeEventTimesChanges(listener);
    }

    @Override
    public void unsubscribeEventTimesChanges(IStartStopTimesChangeListener listener) {
        delegate.unsubscribeEventTimesChanges(listener);
    }

    @Override
    public void subscribeEventMessages(IEventMessageListener listener) {
        delegate.subscribeEventMessages(listener);
    }

    @Override
    public void unsubscribeEventMessages(IEventMessageListener listener) {
        delegate.unsubscribeEventMessages(listener);
    }

    @Override
    public void subscribeServerTime(IServerTimeListener serverTimeListener) {
        delegate.subscribeServerTime(serverTimeListener);
    }

    @Override
    public void unsubscribeServerTime(IServerTimeListener serverTimeListener) {
        delegate.unsubscribeServerTime(serverTimeListener);
    }

    @Override
    public void subscribeRaces(IRacesListener listener) {
        delegate.subscribeRaces(listener);
    }

    @Override
    public void unsubscribeRaces(IRacesListener listener) {
        delegate.unsubscribeRaces(listener);
    }

    @Override
    public void subscribeCompetitors(ICompetitorsListener listener) {
        delegate.subscribeCompetitors(listener);
    }

    @Override
    public void unsubscribeCompetitors(ICompetitorsListener listener) {
        delegate.unsubscribeCompetitors(listener);
    }
}
