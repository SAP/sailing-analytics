package com.sap.sse.gwt.dispatch.client;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class ResultWrapper<R extends Result> implements IsSerializable {
    
    private Date currentServerTime = new Date();
    
    private R result;
    
    @SuppressWarnings("unused")
    private ResultWrapper() {
    }

    public ResultWrapper(R result) {
        super();
        this.result = result;
    }
    
    public Date getCurrentServerTime() {
        return currentServerTime;
    }
    
    public R getResult() {
        return result;
    }
}
