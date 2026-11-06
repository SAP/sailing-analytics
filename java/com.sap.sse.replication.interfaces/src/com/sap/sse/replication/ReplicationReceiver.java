package com.sap.sse.replication;

import java.util.Map;

public interface ReplicationReceiver {
    /**
     * The string with which every replication message starts, followed by the protocol version and the stringified
     * {@link Replicable} ID. Messages not starting with this indicator, such as messages in the legacy protocol which
     * started with the replicable ID directly, are rejected by the receiver. The replicable ID is usually a
     * fully-qualified class name. It must not be equal to this version indicator string which has the value
     * {@code "***"}.
     */
    static String VERSION_INDICATOR = "***";

    /**
     * If the replicator is suspended, messages received are queued.
     */
    boolean isSuspended();

    boolean isBeingStopped();

    /**
     * @return the number of unprocessed messages in the inbound message queue
     */
    int getMessageQueueSize() throws IllegalAccessException;

    /**
     * @return the number of queued {@link OperationWithResult operations}, keyed by the stringified IDs of the
     *         replicables to which the queue belongs
     */
    Map<String, Integer> getOperationQueueSizes();
    
    /**
     * @return <code>true</code> if all queues for all replicables are empty
     */
    boolean isQueueEmptyOrStopped() throws IllegalAccessException;

}
