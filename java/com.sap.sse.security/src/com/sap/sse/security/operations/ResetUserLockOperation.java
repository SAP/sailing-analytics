package com.sap.sse.security.operations;

import com.sap.sse.common.TimedLock;
import com.sap.sse.security.impl.ReplicableSecurityService;

public class ResetUserLockOperation implements SecurityOperation<Void> {
    private static final long serialVersionUID = -6267523788529623080L;
    protected final TimedLock timedLock;
    protected final String username;
    
    public ResetUserLockOperation(String username, TimedLock timedLock) {
        this.username = username;
        this.timedLock = timedLock;
    }

    @Override
    public Void internalApplyTo(ReplicableSecurityService toState) throws Exception {
        toState.internalResetUserTimedLock(username);
        return null;
    }

}
