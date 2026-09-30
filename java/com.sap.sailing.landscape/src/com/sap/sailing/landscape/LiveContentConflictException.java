package com.sap.sailing.landscape;

import com.sap.sailing.landscape.common.LiveContentCheckResult;
import com.sap.sse.common.Util;

/** Raised before a landscape operation would affect a replica set containing live tracked races. */
public final class LiveContentConflictException extends Exception {
    private static final long serialVersionUID = 291018425020458180L;
    private final LiveContentCheckResult liveContentCheckResult;

    public LiveContentConflictException(final LiveContentCheckResult liveContentCheckResult) {
        super("The operation would affect " + Util.size(liveContentCheckResult.getReplicaSetsWithLiveContent())
                + " application replica set(s) containing live tracked races and "
                + Util.size(liveContentCheckResult.getUndeterminedReplicaSetNames())
                + " application replica set(s) whose live-content state could not be determined");
        this.liveContentCheckResult = liveContentCheckResult;
    }

    public LiveContentCheckResult getLiveContentCheckResult() {
        return liveContentCheckResult;
    }
}
