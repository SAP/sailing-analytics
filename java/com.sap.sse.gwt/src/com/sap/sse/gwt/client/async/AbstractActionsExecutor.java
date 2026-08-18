package com.sap.sse.gwt.client.async;

import java.util.HashSet;
import java.util.Set;

/**
 * Can execute actions and keeps track of the number of pending actions.
 * Listeners can register to get informed about changes in the number of
 * pending actions.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public abstract class AbstractActionsExecutor {
    protected int numberOfPendingActions;
    
    public static interface Listener {
        void onNumberOfPendingCallsChanged(int newNumberOfPendingCalls);
    }
    
    private final Set<Listener> listeners;

    protected AbstractActionsExecutor() {
        this.listeners = new HashSet<>();
    }

    public void addListener(Listener listener) {
        listeners.add(listener);
    }
    
    public void removeListener(Listener listener) {
        listeners.remove(listener);
    }
    
    protected void notifyListeners() {
        listeners.forEach(l->l.onNumberOfPendingCallsChanged(numberOfPendingActions));
    }

}
