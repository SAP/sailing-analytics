package com.sap.sse.gwt.client.async;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;


/**
 * A executor class making the actual remote call for an {@link AsyncAction}. The class is managing the number of
 * executed actions in order to prevent a server overload. If the amount of actions to be executed exceeds a defined
 * threshold the execution of those actions will be dropped.
 * 
 * @author c5163874, Simon Marcel Pamies
 */
public class AsyncActionsExecutor {
    private final static Logger logger = Logger.getLogger(AsyncActionsExecutor.class.getName());
    
    private class ExecutionJob<T> implements AsyncCallback<T> {
        private final AsyncAction<T> action;
        private final String category;
        private final AsyncCallback<T> callback;
        
        public ExecutionJob(AsyncAction<T> action, String category, AsyncCallback<T> callback) {
            this.action = action;
            this.category = category;
            this.callback = callback;
        }
        
        public String getCategory() {
            return (this.category == null ? MarkedAsyncCallback.CATEGORY_GLOBAL : this.category);
        }
        
        public String getType() {
            return this.action.getClass().getName();
        }
        
        public void execute() {
            this.action.execute(new MarkedAsyncCallback<T>(this, getCategory()));
        }
        
        @Override
        public void onSuccess(T result) {
            try {
                this.callback.onSuccess(result);
            } finally {
                AsyncActionsExecutor.this.callCompleted(this);
            }
        }
        
        @Override
        public void onFailure(Throwable caught) {
            try {
                logger.warning("Execution failure for action of type " + getType() + ", category "+getCategory());
                this.callback.onFailure(caught);
            } finally {
                AsyncActionsExecutor.this.callCompleted(this);
            }
        }
    }
    
    private static final int DEFAULT_MAX_PENDING_CALLS = 10;
    private static final int DEFAULT_MAX_PENDING_CALLS_PER_TYPE = 4;
    
    /**
     * It can happen that the network connection breaks for some time. During that time
     * no events would be send out and most probably time out. If events time out and do
     * not get sent then it numPendingCalls will never be decreased. That leads to
     * the execute method dropping all new events forever.
     * 
     * In order to be able to recover from such situations the execute method should
     * accept new events if, for a certain duration, no events have been sent out.
     */
    private static final Duration DURATION_AFTER_TO_RESET_QUEUE = Duration.ONE_MINUTE;
    
    private final int maxPendingCalls;
    private final int maxPendingCallsPerType;
    private final Duration durationAfterToResetQueue;
    private final Map<String, Integer> actionsPerTypeCounter;
    private final Map<String, ExecutionJob<?>> lastRequestedActionsNotBeingSentOut;
    private final Map<String, TimePoint> timePointOfTypeLastBeingExecuted;
    private final Map<String, Set<Runnable>> runAfterLastActionForCategoryReturned;
    private final Set<Runnable> runAfterLastActionReturned;
    
    private int numPendingCalls = 0;
    private TimePoint timePointOfFirstExecutorInit = null;

    public AsyncActionsExecutor() {
        this(/*maxPendingCalls*/DEFAULT_MAX_PENDING_CALLS, /*maxPendingCallsPerType*/DEFAULT_MAX_PENDING_CALLS_PER_TYPE,
                /*durationAfterToResetQueue*/DURATION_AFTER_TO_RESET_QUEUE);
    }
    
    public AsyncActionsExecutor(int maxPendingCalls, int maxPendingCallsPerType, Duration durationAfterToResetQueue) {
        if (maxPendingCalls < maxPendingCallsPerType) {
            throw new RuntimeException("The number of max pending calls can not be lower than the number of max pending calls per type.");
        }
        this.runAfterLastActionForCategoryReturned = new HashMap<>();
        this.runAfterLastActionReturned = new HashSet<>();
        this.maxPendingCalls = maxPendingCalls;
        this.maxPendingCallsPerType = maxPendingCallsPerType;
        this.durationAfterToResetQueue = durationAfterToResetQueue;
        this.actionsPerTypeCounter = new HashMap<>();
        this.lastRequestedActionsNotBeingSentOut = new HashMap<>();
        this.timePointOfTypeLastBeingExecuted = new HashMap<>();
        this.timePointOfFirstExecutorInit = MillisecondsTimePoint.now(); // triggering duration to reset
    }
    
    /**
     * If there are no calls for the {@code category} whose results are still outstanding, the {@code callback}
     * is invoked immediately. Otherwise, the {@code callback} is stored and will be invoked once there are
     * no more outstanding responses for the {@code category}.
     */
    public void runAfterLastActionReturned(String category, Runnable callback) {
        if (getNumberOfPendingActionsPerType(category) == 0) {
            callback.run();
        } else {
            Util.addToValueSet(runAfterLastActionForCategoryReturned, category, callback);
        }
    }
    
    /**
     * If there are no calls for this executor whose results are still outstanding, the {@code callback}
     * is invoked immediately. Otherwise, the {@code callback} is stored and will be invoked once there are
     * no more outstanding responses for this executor.
     */
    public void runAfterLastActionReturned(Runnable callback) {
        if (getNumberOfPendingActions() == 0) {
            callback.run();
        } else {
            runAfterLastActionReturned.add(callback);
        }
    }
    
    public <T> void execute(AsyncAction<T> action, AsyncCallback<T> callback) {
        execute(action, MarkedAsyncCallback.CATEGORY_GLOBAL, callback);
    }
    
    public <T> void execute(AsyncAction<T> action, String category, AsyncCallback<T> callback) {
        execute(new ExecutionJob<T>(action, category, callback));
    }
    
    public int getNumberOfPendingActionsPerType(String type) {
        int result = 0;
        result = actionsPerTypeCounter.getOrDefault(type, 0);
        return result;
    }
    
    public int getNumberOfPendingActions() {
        return numPendingCalls;
    }
    
    private void execute(ExecutionJob<?> job) {
        Integer numActionsOfType = actionsPerTypeCounter.get(job.getType());
        if (numActionsOfType == null) {
            numActionsOfType = Integer.valueOf(0);
        }
        if (numPendingCalls >= maxPendingCalls || (numActionsOfType >= maxPendingCallsPerType)) {
            TimePoint now = MillisecondsTimePoint.now();
            TimePoint timePointToInspectForResetDecision = timePointOfTypeLastBeingExecuted.get(job.getType()) != null ?
                    timePointOfTypeLastBeingExecuted.get(job.getType()) : timePointOfFirstExecutorInit;
            if (timePointToInspectForResetDecision != null &&
                    now.minus(durationAfterToResetQueue).after(timePointToInspectForResetDecision)) {
                // reset the number of pending calls
                numPendingCalls = 0;
                // reset number of pending actions per type - 0 is fine as checkForEmptyCallQueue
                // will check for a number less than maxPendingCallsPerType to send out the
                // last job pending for a given type
                for (String jobPendingTypeKey : lastRequestedActionsNotBeingSentOut.keySet()) {
                    actionsPerTypeCounter.put(jobPendingTypeKey, 0);
                }
                numActionsOfType = 0;
            } else {
                logger.info("Dropping action of type " + job.getType() + ", category "+job.getCategory());
                /* don't put the call into the execution queue, but save it as the last one of each type
                 * after each successful execution of a job checkForEmptyCallQueue will check if there
                 * are other jobs of that type that need execution and execute the last one thus
                 * emptying the lastRequestedActionsQueue.
                 * */
                lastRequestedActionsNotBeingSentOut.put(job.getType(), job);
                return;
            }
        }
        actionsPerTypeCounter.put(job.getType(), numActionsOfType+1);
        numPendingCalls++;
        job.execute();
    }

    private void callCompleted(ExecutionJob<?> job) {
        String type = job.getType();
        Integer numActionsPerType = actionsPerTypeCounter.get(type);
        if (numActionsPerType != null && numActionsPerType > 0) {
            actionsPerTypeCounter.put(type, numActionsPerType-1);
            if (numActionsPerType-1 == 0) {
                final Set<Runnable> callbacks = runAfterLastActionForCategoryReturned.get(type);
                if (callbacks != null) {
                    callbacks.forEach(callback->callback.run());
                }
            }

        }
        numPendingCalls--;
        if (numPendingCalls == 0) {
            runAfterLastActionReturned.forEach(callback->callback.run());
        }
        timePointOfTypeLastBeingExecuted.put(type, MillisecondsTimePoint.now());
        checkForEmptyCallQueue(type);
    }

    private void checkForEmptyCallQueue(String type) {
        Integer numActionsPerType = actionsPerTypeCounter.get(type);
        if (numActionsPerType != null && numActionsPerType < maxPendingCallsPerType && lastRequestedActionsNotBeingSentOut.containsKey(type)) {
            ExecutionJob<?> lastRequestedAction = lastRequestedActionsNotBeingSentOut.remove(type);
            logger.info("Executing last queued action of type: " + lastRequestedAction.getType());
            execute(lastRequestedAction);
        }
    }
}
