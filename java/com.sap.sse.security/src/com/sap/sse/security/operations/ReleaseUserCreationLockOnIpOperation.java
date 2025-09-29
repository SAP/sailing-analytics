package com.sap.sse.security.operations;

import com.sap.sse.security.impl.ReplicableSecurityService;

public class ReleaseUserCreationLockOnIpOperation implements SecurityOperation<Void> {
    private static final long serialVersionUID = 8729427754960969395L;
    protected final String ip;

    public ReleaseUserCreationLockOnIpOperation(final String ip) {
        this.ip = ip;
    }

    @Override
    public Void internalApplyTo(ReplicableSecurityService toState) throws Exception {
        toState.internalReleaseUserCreationLockOnIp(ip);
        return null;
    }
}
