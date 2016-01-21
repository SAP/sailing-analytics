package com.sap.sse.gwt.dispatch.client.rpcimpl;

import com.google.gwt.user.client.rpc.RemoteService;
import com.sap.sse.gwt.dispatch.client.Action;
import com.sap.sse.gwt.dispatch.client.DispatchContext;
import com.sap.sse.gwt.dispatch.client.Result;
import com.sap.sse.gwt.dispatch.client.ResultWrapper;
import com.sap.sse.gwt.dispatch.client.exceptions.DispatchException;

public interface DispatchRPC<CTX extends DispatchContext> extends RemoteService {
    
    <R extends Result, A extends Action<R, CTX>> ResultWrapper<R> execute(
            RequestWrapper<R, A, CTX> action) throws DispatchException;

}
