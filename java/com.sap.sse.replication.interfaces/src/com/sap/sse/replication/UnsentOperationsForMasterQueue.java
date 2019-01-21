package com.sap.sse.replication;

/**
 * Objects of this type can queue operations whose delivery to the master server has failed temporarily.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface UnsentOperationsForMasterQueue {
    /**
     * When this replica was unable to send an operation to the master, e.g., for connectivity issues or the
     * master currently not being available, passing the operation to this method will enqueue the operation
     * for later re-send attempts. Implementations have to make sure that operations are queued and sent in the
     * order in which they are passed to this method. Therefore, implementations should {@code synchronize} on this
     * object.
     * 
     * @param sender the object to use to try to send the operation to the master server upon the next attempt
     */
    <S, O extends OperationWithResult<S, ?>, T> void retrySendingLater(OperationWithResult<S, T> operationWithResultWithIdWrapper, OperationsToMasterSender<S, O> sender);
}
