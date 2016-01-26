package com.sap.sse.gwt.dispatch.client.rpcimpl;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.sap.sse.gwt.dispatch.client.Action;
import com.sap.sse.gwt.dispatch.client.DispatchSystemAsync;
import com.sap.sse.gwt.dispatch.client.DispatchContext;
import com.sap.sse.gwt.dispatch.client.Result;

/**
 * Simple dispatch implementation that uses GWT RPC internally to communicate with the server
 *
 * @param <CTX>
 */
public class SimpleDispatch<CTX extends DispatchContext> implements DispatchSystemAsync<CTX> {
    
    private final DispatchRPCAsync<CTX> dispatchRPC = GWT.create(DispatchRPC.class);

    private long clientServerOffset = 0;
    
    public SimpleDispatch(String dispatchRPCPath) {
        ((ServiceDefTarget) dispatchRPC).setServiceEntryPoint(dispatchRPCPath);
    }

    @Override
    public <R extends Result, A extends Action<R, CTX>> void execute(A action, final AsyncCallback<R> callback) {
        final RequestWrapper<R, A, CTX> requestWrapper = new RequestWrapper<R, A, CTX>(action, LocaleInfo
                .getCurrentLocale()
                .getLocaleName());
        final long clientTimeOnRequestStart = System.currentTimeMillis();
        dispatchRPC.execute(requestWrapper, new AsyncCallback<ResultWrapper<R>>() {

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(caught);
            }

            @Override
            public void onSuccess(ResultWrapper<R> result) {
                long clientTimeOnRequestEnd = System.currentTimeMillis();
                long latency = (clientTimeOnRequestEnd - clientTimeOnRequestStart) / 2;
                long currentServerTime = result.getCurrentServerTime().getTime() + latency;
                clientServerOffset = currentServerTime - clientTimeOnRequestEnd;
                callback.onSuccess(result.getResult());
            }
        });
    }

    public long getClientServerOffset() {
        return clientServerOffset;
    }
}
