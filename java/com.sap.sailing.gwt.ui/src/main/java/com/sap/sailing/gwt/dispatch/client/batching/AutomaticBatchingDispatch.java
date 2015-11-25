package com.sap.sailing.gwt.dispatch.client.batching;

import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.dispatch.client.Action;
import com.sap.sailing.gwt.dispatch.client.DispatchAsync;
import com.sap.sailing.gwt.dispatch.client.DispatchContext;
import com.sap.sailing.gwt.dispatch.client.Result;

public class AutomaticBatchingDispatch<CTX extends DispatchContext> implements DispatchAsync<CTX> {

    private static final Logger LOG = Logger.getLogger(AutomaticBatchingDispatch.class.getName());

    private final DispatchAsync<CTX> executionContext;

    private DispatchCallStack<CTX> currentCallstack;

    /**
     * Creates a batching dispatch service using the given dispatch implementation.
     * 
     * The underlying dispatch service is responsible for the context propagation.
     * 
     * @param service
     *            the underlying service implementation to use
     * 
     */
    public AutomaticBatchingDispatch(DispatchAsync<CTX> service) {
        this.executionContext = service;
    }

    /**
     * Execute action in a batch. Automatically triggers execution when GWT gives control back to browser in the event
     * loop.
     */
    @Override
    public <R extends Result, A extends Action<R, CTX>> void execute(A action, AsyncCallback<R> callback) {
        if(action instanceof NonBatchableAction) {
            executionContext.execute(action, callback);
            return;
        }
        if (this.currentCallstack == null) {
            this.currentCallstack = new DispatchCallStack<CTX>();
            this.triggerExecution();
        }
        this.currentCallstack.addCall(action, callback);
    };

    /**
     * Schedules the execution of the newly created {@link DispatchCallStack} at the end of the event loop.
     * 
     * Uses {@link Scheduler} as underlying scheduling API.
     */
    private void triggerExecution() {
        Scheduler.get().scheduleFinally(new ScheduledCommand() {

            @Override
            public void execute() {
                LOG.fine("executing batch callstack ");

                final DispatchCallStack<CTX> callStackInExecution = AutomaticBatchingDispatch.this.currentCallstack;
                AutomaticBatchingDispatch.this.currentCallstack = null;
                AutomaticBatchingDispatch.this.executionContext.execute(callStackInExecution.getBatchAction(),
                        new AsyncCallback<BatchResult>() {

                            @Override
                            public void onFailure(Throwable caught) {
                                LOG.finest("Failure on call execution: " + caught.getMessage());
                                callStackInExecution.fail(caught);
                            }

                            @Override
                            public void onSuccess(BatchResult result) {
                                callStackInExecution.processResult(result);
                            }
                        });
            }
        });
    }

}
