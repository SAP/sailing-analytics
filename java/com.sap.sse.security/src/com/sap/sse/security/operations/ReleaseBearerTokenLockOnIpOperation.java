package com.sap.sse.security.operations;

import com.sap.sse.security.impl.ReplicableSecurityService;

public class ReleaseBearerTokenLockOnIpOperation implements SecurityOperation<Void> {
    private static final long serialVersionUID = 5839571828359473821L;
    protected final String ip;

    public ReleaseBearerTokenLockOnIpOperation(final String ip) {
        this.ip = ip;
    }

    @Override
    public Void internalApplyTo(ReplicableSecurityService toState) throws Exception {
        toState.internalReleaseBearerTokenLockOnIp(ip);
        return null;
    }
}
