package com.sap.sailing.landscape.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Describes live tracked races found in one or more application replica sets at one instant in time. An empty
 * {@link #getReplicaSetsWithLiveContent() list} means that no live content was found.
 */
public final class LiveContentCheckResult implements Serializable {
    private static final long serialVersionUID = 4259503344851123760L;
    private final long checkedAtMillis;
    private final List<ReplicaSetLiveContent> replicaSetsWithLiveContent;

    public LiveContentCheckResult(final long checkedAtMillis,
            final Iterable<ReplicaSetLiveContent> replicaSetsWithLiveContent) {
        this.checkedAtMillis = checkedAtMillis;
        final List<ReplicaSetLiveContent> replicaSetsWithLiveContentCopy = new ArrayList<>();
        replicaSetsWithLiveContent.forEach(replicaSetsWithLiveContentCopy::add);
        this.replicaSetsWithLiveContent = Collections.unmodifiableList(replicaSetsWithLiveContentCopy);
    }

    public long getCheckedAtMillis() {
        return checkedAtMillis;
    }

    public List<ReplicaSetLiveContent> getReplicaSetsWithLiveContent() {
        return replicaSetsWithLiveContent;
    }

    public boolean hasLiveContent() {
        return !replicaSetsWithLiveContent.isEmpty();
    }
}
