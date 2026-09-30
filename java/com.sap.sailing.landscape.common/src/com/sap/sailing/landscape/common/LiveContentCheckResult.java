package com.sap.sailing.landscape.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sse.common.TimePoint;

/**
 * Describes live tracked races found in one or more application replica sets at one instant in time. An empty
 * {@link #getReplicaSetsWithLiveContent() list} means that no live content was found among the replica sets that could
 * actually be inspected. Replica sets whose live-content state could <em>not</em> be determined&mdash;for example
 * because the server predates the availability of the live-content endpoint and therefore could not answer the
 * query&mdash;are reported separately in {@link #getUndeterminedReplicaSetNames()} so that callers can treat them
 * conservatively (as potentially serving live content) rather than mistaking them for replica sets that were verified
 * to be idle.
 */
public final class LiveContentCheckResult implements Serializable {
    private static final long serialVersionUID = -6357657054503530857L;
    private final TimePoint checkedAt;
    private final Iterable<ReplicaSetLiveContent> replicaSetsWithLiveContent;
    private final Iterable<String> undeterminedReplicaSetNames;

    public LiveContentCheckResult(final TimePoint checkedAt,
            final Iterable<ReplicaSetLiveContent> replicaSetsWithLiveContent) {
        this(checkedAt, replicaSetsWithLiveContent, Collections.emptyList());
    }

    public LiveContentCheckResult(final TimePoint checkedAt,
            final Iterable<ReplicaSetLiveContent> replicaSetsWithLiveContent,
            final Iterable<String> undeterminedReplicaSetNames) {
        this.checkedAt = checkedAt;
        final List<ReplicaSetLiveContent> replicaSetsWithLiveContentCopy = new ArrayList<>();
        replicaSetsWithLiveContent.forEach(replicaSetsWithLiveContentCopy::add);
        this.replicaSetsWithLiveContent = Collections.unmodifiableList(replicaSetsWithLiveContentCopy);
        final List<String> undeterminedReplicaSetNamesCopy = new ArrayList<>();
        undeterminedReplicaSetNames.forEach(undeterminedReplicaSetNamesCopy::add);
        this.undeterminedReplicaSetNames = Collections.unmodifiableList(undeterminedReplicaSetNamesCopy);
    }

    public TimePoint getCheckedAt() {
        return checkedAt;
    }

    public Iterable<ReplicaSetLiveContent> getReplicaSetsWithLiveContent() {
        return replicaSetsWithLiveContent;
    }

    /**
     * Names of replica sets whose live-content state could not be determined (e.g., because their server does not offer
     * the live-content query yet). Callers that block operations on live content should treat these as potentially
     * serving live content rather than as verified to be idle.
     */
    public Iterable<String> getUndeterminedReplicaSetNames() {
        return undeterminedReplicaSetNames;
    }

    public boolean hasLiveContent() {
        return replicaSetsWithLiveContent.iterator().hasNext();
    }

    public boolean hasUndeterminedReplicaSets() {
        return undeterminedReplicaSetNames.iterator().hasNext();
    }
}
