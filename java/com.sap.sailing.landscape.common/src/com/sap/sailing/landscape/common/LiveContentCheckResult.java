package com.sap.sailing.landscape.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.sap.sse.common.TimePoint;

/**
 * Describes live tracked races found in one or more application replica sets at one instant in time. An empty
 * {@link #getReplicaSetsWithLiveContent() list} means that no live content was found.
 */
public final class LiveContentCheckResult implements Serializable {
    private static final long serialVersionUID = 3100475095484004645L;
    private final TimePoint checkedAt;
    private final List<ReplicaSetLiveContent> replicaSetsWithLiveContent;

    public LiveContentCheckResult(final TimePoint checkedAt,
            final Iterable<ReplicaSetLiveContent> replicaSetsWithLiveContent) {
        this.checkedAt = checkedAt;
        final List<ReplicaSetLiveContent> replicaSetsWithLiveContentCopy = new ArrayList<>();
        replicaSetsWithLiveContent.forEach(replicaSetsWithLiveContentCopy::add);
        this.replicaSetsWithLiveContent = Collections.unmodifiableList(replicaSetsWithLiveContentCopy);
    }

    public TimePoint getCheckedAt() {
        return checkedAt;
    }

    public List<ReplicaSetLiveContent> getReplicaSetsWithLiveContent() {
        return replicaSetsWithLiveContent;
    }

    public boolean hasLiveContent() {
        return !replicaSetsWithLiveContent.isEmpty();
    }
}
