package com.sap.sailing.landscape.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Live content found in one application replica set. */
public final class ReplicaSetLiveContent implements Serializable {
    private static final long serialVersionUID = -8106650879852011910L;
    private final String replicaSetName;
    private final List<EventLiveContent> eventsWithLiveContent;

    public ReplicaSetLiveContent(final String replicaSetName,
            final Iterable<EventLiveContent> eventsWithLiveContent) {
        this.replicaSetName = replicaSetName;
        final List<EventLiveContent> eventsWithLiveContentCopy = new ArrayList<>();
        eventsWithLiveContent.forEach(eventsWithLiveContentCopy::add);
        this.eventsWithLiveContent = Collections.unmodifiableList(eventsWithLiveContentCopy);
    }

    public String getReplicaSetName() {
        return replicaSetName;
    }

    public List<EventLiveContent> getEventsWithLiveContent() {
        return eventsWithLiveContent;
    }
}
