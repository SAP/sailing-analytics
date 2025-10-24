package com.sap.sse.gwt.client.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

/**
 * Some simple tests for the {@link AsyncActionsExecutor}.
 *  
 * @author Simon Marcel Pamies
 */
public class AsyncActionsExecutorTest {
    private class AsyncActionWithExecutionCounter implements AsyncAction<Void> {
        private int executionCounter = 0;
        
        private final boolean doSucceed;
        private final boolean doFail;
        
        public AsyncActionWithExecutionCounter(boolean doSucceed, boolean doFail) {
            this.doFail = doFail;
            this.doSucceed = doSucceed;
        }
        
        @Override
        public void execute(AsyncCallback<Void> callback) {
            if (doSucceed) {
                callback.onSuccess(null);
            }
            if (doFail) {
                callback.onFailure(new RuntimeException());
            }
            executionCounter++;
        }
        
        public int getExecutionCounter() {
            return executionCounter;
        }
    }
    
    private AsyncCallback<Void> defaultCallback = new AsyncCallback<Void>() {
        @Override
        public void onFailure(Throwable caught) {
        }

        @Override
        public void onSuccess(Void result) {
        }
    }; 
    
    private AsyncActionsExecutor executor;

    @Test
    public void testThatANonTriggeringActionLeadsToPendingCall() {
        executor = new AsyncActionsExecutor(/*maxPendingCalls*/6, /*maxPendingCallsPerType*/5,
                /*durationAfterToResetQueue*/Duration.ONE_SECOND);
        // add a job that will never execute successfully
        AsyncAction<Void> nonTriggeringAction = new AsyncActionWithExecutionCounter(/*doSucceed*/false, /*doFail*/false);
        executor.execute(nonTriggeringAction, defaultCallback);
        // now the executor should have one pending action
        assertNotNull(executor.getNumberOfPendingActionsPerType(nonTriggeringAction.getClass().getName()));
        assertEquals(1, executor.getNumberOfPendingActionsPerType(nonTriggeringAction.getClass().getName()));
    }
    
    @Test
    public void testTriggeringActionsLeadingToPendingQueue() {
        executor = new AsyncActionsExecutor(/*maxPendingCalls*/6, /*maxPendingCallsPerType*/6,
                /*durationAfterToResetQueue*/Duration.ONE_HOUR);
        // test completing jobs leading to empty pending queue
        AsyncAction<Void> successfulAction = new AsyncActionWithExecutionCounter(/*doSucceed*/true, /*doFail*/false);
        executor.execute(successfulAction, defaultCallback);
        assertEquals(0, executor.getNumberOfPendingActionsPerType(successfulAction.getClass().getName()));
        AsyncAction<Void> failingAction = new AsyncActionWithExecutionCounter(/*doSucceed*/false, /*doFail*/true);
        executor.execute(failingAction, defaultCallback);
        assertEquals(0, executor.getNumberOfPendingActionsPerType(failingAction.getClass().getName()));
        // test that max pending calls is being used
        AsyncAction<Void> nonTriggeringAction = new AsyncActionWithExecutionCounter(/*doSucceed*/false, /*doFail*/false);
        for (int i=0;i<=11;i++) {
            executor.execute(nonTriggeringAction, defaultCallback);
        }
        assertEquals(6, executor.getNumberOfPendingActions());
        assertEquals(6, executor.getNumberOfPendingActionsPerType(nonTriggeringAction.getClass().getName()));
    }
    
    @Test
    public void testAutomaticReleaseOfActionsImmediatelyAndAfterSomeTime() {
        executor = new AsyncActionsExecutor(/*maxPendingCalls*/6, /*maxPendingCallsPerType*/6,
                /*durationAfterToResetQueue*/Duration.ONE_SECOND.times(2));
        // execute action that does not get through but then retries
        AsyncActionWithExecutionCounter nonTriggeringAction = new AsyncActionWithExecutionCounter(/*doSucceed*/false, /*doFail*/false);
        assertEquals(0, nonTriggeringAction.getExecutionCounter());
        TimePoint startOfExecution = MillisecondsTimePoint.now();
        executor.execute(nonTriggeringAction, defaultCallback);
        assertEquals(1, nonTriggeringAction.getExecutionCounter());
        for (int i=0;i<=4;i++) {
            executor.execute(nonTriggeringAction, defaultCallback);
        }
        assertEquals(6, nonTriggeringAction.getExecutionCounter());
        assertEquals(6, executor.getNumberOfPendingActions());
        assertEquals(6, executor.getNumberOfPendingActionsPerType(nonTriggeringAction.getClass().getName()));
        // now the action should be retriggered - we can not use Thread.sleep() here as it is not
        // compilable by the GWT compiler. Using a hack to wait a little bit.
        while (MillisecondsTimePoint.now().minus(Duration.ONE_SECOND.times(3)).before(startOfExecution)) {
        }
        executor.execute(nonTriggeringAction, defaultCallback);
        assertEquals(7, nonTriggeringAction.getExecutionCounter());
        assertEquals(1, executor.getNumberOfPendingActions());
        assertEquals(1, executor.getNumberOfPendingActionsPerType(nonTriggeringAction.getClass().getName()));
    }
}
