package com.sap.sailing.landscape;

import com.sap.sailing.landscape.common.LiveContentCheckResult;

/** Raised before a landscape operation would affect a replica set containing live tracked races. */
public final class LiveContentConflictException extends Exception {
    private static final long serialVersionUID = 291018425020458180L;
    private final LiveContentCheckResult liveContentCheckResult;

    public LiveContentConflictException(final LiveContentCheckResult liveContentCheckResult) {
        super("The operation would affect " + liveContentCheckResult.getReplicaSetsWithLiveContent().size()
                + " application replica set(s) containing live tracked races");
        this.liveContentCheckResult = liveContentCheckResult;
    }

    public LiveContentCheckResult getLiveContentCheckResult() {
        return liveContentCheckResult;
    }
}
