package com.sap.sse.operationaltransformation;

/**
 * Accepts change operations from clients and maintains a shared state. If new
 * clients connect, the full state can be sent to the client for initial
 * synchronization. When a change {@link Operation} is received from a client
 * that operation will be based on some state. Since that state the
 * server may have applied one or more change operations already. In order to
 * synchronize the requesting client, the server transforms all in-between
 * change operations for the requesting client and sends those transformation
 * results back to the requesting client. The client's operation, in turn, is
 * disseminated to all other clients for them to merge into their local replica
 * of the global state.
 * 
 * @author Axel Uhl D043530
 * 
 * @param <O> the type of operations that can be applied to the states
 * @param <S> the type of the states on which the operations perform changes
 */
public interface Peer<O extends Operation<S>, S> {
    
    /**
     * Enumeration representing the two valid roles a peer can have. There must
     * be exactly one server, multiple clients are allowed.
     * 
     * @author Jan Karstens D046040
     * 
     */
    public enum Role {
	SERVER, CLIENT
    }

    /**
     * Registers a peer with this peer for replication. From this point on, this
     * peer will remember which operations it has forwarded to that <code>peer</code> for
     * later transformation jobs for operations coming in from that <code>peer</code>.<p>
     * 
     * The <code>peer</code> is expected to set the state returned by this operation
     * as its initial state. 
     * 
     * @return the current server state; the calling client can use this as its
     *         initial state
     */
    S addPeer(Peer<O, S> peer);

    /**
     * Usually, the framework handles calls to this method and framework users
     * should therefore not need to call this method.
     * <p>
     * 
     * Synchronizes an <tt>operation</tt> from the <tt>source</tt> peer to this
     * peer. The <tt>source</tt> peer specifies how many operations from this
     * peer it already merged into its operations sequence before having
     * executed <tt>operation</tt>.
     * <p>
     * 
     * The operation has to be transformed using this peer's operational
     * transformation and using the sequence of operations applied to the
     * operation's start state up to this peer's current state. This results in
     * the operation that needs to be applied to the current state to produce
     * the next state.
     * <p>
     * 
     * After applying this possibly transformed operation to this peer's current
     * state and thus producing a new current state, all registered peers except
     * <tt>source</tt> are notified about the change by sending them the
     * transformed operation.
     * 
     */
    void apply(Peer<O, S> source, O operation, int numberOfOperationsSourceHasMergedFromThis);
    
    /**
     * Applies a change operation locally to the current peer's state and update all known
     * peers by sending them the operation.
     */
    void apply(O operation);
    
    /**
     * Tells this peer that the <tt>source</tt> peer has successfully merged
     * <tt>numberOfMergedOperations</tt> altogether coming from this peer
     * into its operation sequence. This peer can take this as an opportunity
     * to clean up the queue and remove the transformed copies of all operations
     * up to number <tt>numberOfMergedOperations-1</tt>.
     */
    void confirm(Peer<O, S> source, int numberOfMergedOperations);
    
    /**
     * Tells the current state in which the peer is. This is the state constructed by
     * the sequence of calls to {@link #apply(Operation)} and the merged in notifications
     * from other peers.
     */
    S getCurrentState();

    /**
     * Blocks until the queue of tasks that works on achieving consistency across peers
     * has completely drained. This can be useful for testing purposes when you want to
     * wait until consistency has been reached across clients and server.
     */
    void waitForNotRunning();

    /**
     * Shuts any executor service, e.g., the one used for merging incoming operations,
     * down. This is an irreversible operation; this peer cannot be used anymore afterwards.
     */
    void shutdown();
}
