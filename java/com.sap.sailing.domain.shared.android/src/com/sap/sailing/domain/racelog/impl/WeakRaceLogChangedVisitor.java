package com.sap.sailing.domain.racelog.impl;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogChangedListener;
import com.sap.sailing.domain.racelog.RaceLogEvent;

/**
 * A listener that unregisters itself from the race log's listeners if its notification target is no longer strongly
 * referenced.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public class WeakRaceLogChangedVisitor extends AbstractRaceLogChangedVisitor {
    private static final Logger logger = Logger.getLogger(WeakRaceLogChangedVisitor.class.getName());
    private static ReferenceQueue<? super RaceLogChangedListener> queue = new ReferenceQueue<RaceLogChangedListener>();
    private static Map<Reference<?>, WeakRaceLogChangedVisitor> referenceToVisitor = new HashMap<Reference<?>, WeakRaceLogChangedVisitor>();
    static {
        new Thread(WeakRaceLogChangedVisitor.class.getSimpleName() + " weak reference cleaner") {
            @Override
            public void run() {
                while (true) {
                    try {
                        Reference<?> ref = queue.remove();
                        WeakRaceLogChangedVisitor visitor = referenceToVisitor.get(ref);
                        visitor.removeListener();
                    } catch (InterruptedException e) {
                        logger.log(Level.SEVERE, "Error trying to clean weak reference", e);
                    }
                }
            }
        }.start();
    }
    
    private final WeakReference<RaceLogChangedListener> listenerRef;
    private final RaceLog removeFromThisRaceLogWhenListenerNoLongerStronglyReferenced;
    
    public WeakRaceLogChangedVisitor(RaceLog removeFromThisRaceLogWhenListenerNoLongerStronglyReferenced, RaceLogChangedListener listener) {
        this.removeFromThisRaceLogWhenListenerNoLongerStronglyReferenced = removeFromThisRaceLogWhenListenerNoLongerStronglyReferenced;
        listenerRef = new WeakReference<RaceLogChangedListener>(listener, queue);
    }
    
    @Override
    protected void notifyListenerAboutEventAdded(RaceLogEvent event) {
        RaceLogChangedListener listener = listenerRef.get();
        if (listener == null) {
            removeListener();
        } else {
            listener.eventAdded(event);
        }
    }

    private void removeListener() {
        removeFromThisRaceLogWhenListenerNoLongerStronglyReferenced.removeListener(this);
    }

}
