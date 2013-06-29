package com.sap.sailing.gwt.ui.actions;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * An action that which will be executed a asynchronous remote call to the server
 * 
 * @param <Result>
 *            The type of the returned value of the call
 * @author c5163874
 */
public interface AsyncAction<Result> {
    void execute(AsyncActionsExecutor asyncActionsExecutor);
    
    Result getResult();

    AsyncCallback<Result> getCallback();

    AsyncCallback<Result> getWrapperCallback(AsyncActionsExecutor asyncActionsExecutor);

    String getType(); 
}
