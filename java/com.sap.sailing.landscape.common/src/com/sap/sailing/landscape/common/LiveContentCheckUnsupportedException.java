package com.sap.sailing.landscape.common;

/**
 * Raised when a replica set's live-content state cannot be determined because the server does not support the
 * live-content query&mdash;for example, when the server predates the introduction of that endpoint and responds with an
 * error status instead of a live-content report. Callers should treat an affected replica set conservatively (as
 * potentially serving live content) rather than mistaking the failure for a "no live content" answer.
 */
public final class LiveContentCheckUnsupportedException extends Exception {
    private static final long serialVersionUID = 4640006497364585983L;
    private final String replicaSetName;

    public LiveContentCheckUnsupportedException(final String replicaSetName, final String reason) {
        super("Could not determine live-content state of replica set " + replicaSetName + ": " + reason);
        this.replicaSetName = replicaSetName;
    }

    public String getReplicaSetName() {
        return replicaSetName;
    }
}
