package com.sap.sse.operationaltransformation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.util.ThreadPoolUtil;


/**
 * A very simple peer implementation for operational transformations that starts
 * out with a provided initial state and then receives operations which it applies
 * to its state. It manages a set of clients. Using the <tt>server</tt> constructor
 * parameter one can configure whether this peer is a client or a server.
 * 
 * @author Axel Uhl D043530
 *
 * @param <O> type of change operations to be processed by this server
 * @param <S> type of state managed by this server to which the operations can be applied
 */
public class PeerImpl<O extends Operation<S>, S> implements Peer<O, S> {
    private static final Logger logger = Logger.getLogger(PeerImpl.class.getName());
    
    private final String name;
    
    private volatile S currentState;
    
    /**
     * Tells if this peer acts in the server or client role. This is used to determine in which
     * direction to apply the operational transformation.
     */
    private final Role role;
    
    /**
     * Tells if this peer is currently actively running, with a certain number of pending / running requests
     */
    private volatile int scheduledOrRunning = 0;

    /**
     * Queues operations sent out to a peer in {@link #updatePeers(Operation, Peer)} and whose
     * merging has not yet been confirmed by that peer. Those messages are transformed when an
     * operation from that peer is applied. When applying an operation to the peer has been
     * confirmed, the first operation from the queue is removed because operations sent
     * from this peer to the remote peer are processed in order.
     */
    private final ConcurrentMap<Peer<O, S>, UnmergedOperationsQueue<O, S>> unmergedOperationsForPeer = new ConcurrentHashMap<>();
    
    /**
     * Remembers per peer how many of that peer's operations this peer has already
     * applied locally. Access happens while {@code synchronized} on {@code this} object's monitor.
     */
    private final Map<Peer<O, S>, Integer> numberOfMergedOperations = new HashMap<>();
    
    private final Transformer<S, O> transformer;
    
    /**
     * The background task handler which sends out the asynchronous updates to the peers.
     */
    private final ExecutorService merger;

    public PeerImpl(Transformer<S, O> transformer, S initialState, Role role) {
        this(/* name */ null, transformer, initialState, role);
    }
    
    public PeerImpl(String name, Transformer<S, O> transformer, S initialState, Role role) {
        this.transformer = transformer;
        currentState = initialState;
        this.role = role;
        this.merger = createMerger();
	this.name = name;
    }
    
    /**
     * Constructs a client peer and connects it to the <tt>server</tt> peer, establishing
     * the two-way link between client and server peers. The initial state is taken from
     * the server.
     */
    public PeerImpl(Transformer<S, O> transformer, Peer<O, S> server) {
        this(/* name */ null, transformer, server);
    }

    /**
     * Constructs a client peer with a name and connects it to the
     * <tt>server</tt> peer, establishing the two-way link between client and
     * server peers. The initial state is taken from the server.
     */
    public PeerImpl(String name, Transformer<S, O> transformer, Peer<O, S> server) {
        this(name, transformer, /* initial state */ null, Role.CLIENT);
        currentState = server.addPeer(this);
        addPeer(server);
    }
    
    @Override
    public void shutdown() {
	merger.shutdown();
    }

    private ExecutorService createMerger() {
        return ThreadPoolUtil.INSTANCE.createForegroundTaskThreadPoolExecutor(1,
                this.getClass().getName() + " " + name + " " + UUID.randomUUID());
    }

    private Transformer<S, O> getTransformer() {
        return transformer;
    }

    @Override
    public S addPeer(Peer<O, S> peer) {
	if (role == Role.CLIENT && getPeers().size() > 0) {
	    throw new RuntimeException("A client must be connected to at most one server");
	}
	unmergedOperationsForPeer.put(peer, new UnmergedOperationsQueue<>());
	numberOfMergedOperations.put(peer, 0);
	return getCurrentState();
    }

    private Collection<Peer<O, S>> getPeers() {
	return unmergedOperationsForPeer.keySet();
    }

    public S getCurrentState() {
	return currentState;
    }

    @Override
    public synchronized void apply(O operation) {
	taskScheduled();
        try {
            currentState = operation.applyTo(currentState);
            updatePeers(operation, /* except */null);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception during executing task in peer "+PeerImpl.this, e);
            throw e;
        } finally {
            taskFinished();
        }
    }

    @Override
    public synchronized void apply(final Peer<O, S> source, O operation,
	    final int numberOfOperationsSourceHasMergedFromThis) {
        taskScheduled();
        try {
            if (!getPeers().contains(source)) {
                throw new RuntimeException("Peer " + source + " not registered with peer " + this);
            }
            // Starting from base up to current, compute transformed operation sequence to send
            // to peer; this will create a sequence of states for the peer which eventually
            // leads up to a state that equals this peer's current state.
            O transformedOp = operation;
            UnmergedOperationsQueue<O, S> unmergedOperationsForSource = unmergedOperationsForPeer.get(source);
            int localOpNumber = numberOfOperationsSourceHasMergedFromThis;
            for (O unconfirmedOperation : unmergedOperationsForSource
                    .getUnmergedOperations(numberOfOperationsSourceHasMergedFromThis)) {
                if (role == Role.SERVER) {
                    final ClientServerOperationPair<S, O> pair;
                    if (transformedOp == null || unconfirmedOperation == null) {
                        pair = new ClientServerOperationPair<>(transformedOp, unconfirmedOperation);
                    } else {
                        pair = getTransformer().transform(transformedOp, unconfirmedOperation);
                    }
                    transformedOp = pair.getClientOp();
                    unmergedOperationsForSource.updateWithTransformed(localOpNumber, pair.getServerOp());
                } else {
                    final ClientServerOperationPair<S, O> pair;
                    if (transformedOp == null || unconfirmedOperation == null) {
                        pair = new ClientServerOperationPair<>(unconfirmedOperation, transformedOp);
                    } else {
                        pair = getTransformer().transform(unconfirmedOperation, transformedOp);
                    }
                    transformedOp = pair.getServerOp();
                    unmergedOperationsForSource.updateWithTransformed(localOpNumber, pair.getClientOp());
                }
                localOpNumber++;
            }
            if (transformedOp != null) {
                currentState = transformedOp.applyTo(currentState); // produce a new current state
            }
            final int numberOfMergedOperationsFromSource = numberOfMergedOperations.get(source) + 1;
            numberOfMergedOperations.put(source, numberOfMergedOperationsFromSource);
            // It's important that the following call to confirm is synchronized with the
            // sending of updates to the source peer. Therefore, the confirm call is executed
            // in the same serializing background thread:
            scheduleTask(new Runnable() {
                public void run() {
                    source.confirm(PeerImpl.this, numberOfMergedOperationsFromSource);
                }
            });
            updatePeers(transformedOp, /* except */source);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Exception during executing task in peer "+PeerImpl.this, e);
            throw e;
        } finally {
            taskFinished();
        }
    }

    /**
     * Propagates the changes described by <tt>operation</tt> to all registered peers (except the peer identified by
     * <tt>except</tt> if not <tt>null</tt>). For each peer, the operation is sent along to that peer's
     * {@link #apply(Peer, Operation, int)} method together with the {@link #numberOfMergedOperations number of
     * operations currently merged from that peer}.
     */
    private synchronized void updatePeers(final O operation, Peer<O, S> except) {
	for (final Peer<O, S> peer : getPeers()) {
	    if (except == null || !except.equals(peer)) {
		unmergedOperationsForPeer.get(peer).sentOutOperation(operation);
		// remember the number of merged operations while still in synchronized block
		final int numberOfMergedOpsForPeer = numberOfMergedOperations.get(peer);
		scheduleTask(new Runnable() {
                    public void run() {
                        /*
                         * Thoughts on locking: The peer runs the following apply(...) call synchronized. Therefore,
                         * it can't take other apply calls (local or from other peers) during that time. It may,
                         * though, have pending tasks in its updatePeers that can continue to run. This may include
                         * updates for this peer which would be received by this peer's apply(...) method.
                         */
                        peer.apply(PeerImpl.this, operation, numberOfMergedOpsForPeer);
                        // TODO what to do if apply throws a RuntimeException?
                    }
		});
	    }
	}
    }

    public String toString() {
	if (name != null) {
	    return name;
	} else {
	    return (role == Role.SERVER?"server":"client")+" "+super.toString();
	}
    }

    @Override
    public synchronized void confirm(Peer<O, S> source, int numberOfMergedOperations) {
	unmergedOperationsForPeer.get(source).confirm(numberOfMergedOperations);
    }

    /**
     * Records that a new task is scheduled by incrementing {@link #scheduledOrRunning} by one, then
     * schedules the <tt>runnable</tt> with the {@link #merger} executor service. The
     * <tt>runnable</tt> is wrapped such that when its execution has finished,
     * {@link #taskFinished()} will be called such that {@link #scheduledOrRunning} is decremented
     * properly again and waiting clients in {@link #waitForNotRunning()} can be unblocked.
     */
    private void scheduleTask(final Runnable runnable) {
	taskScheduled();
	merger.execute(new Runnable() {
	    public void run() {
	        try {
	            runnable.run();
	        } catch (Exception e) {
	            logger.log(Level.SEVERE, "Exception during executing task in peer "+PeerImpl.this, e);
	            throw e;
	        } finally {
	            taskFinished();
	        }
	    }
	});
    }

    private synchronized void taskScheduled() {
        logger.fine(""+this+" taskScheduled incrementing scheduleOrRunning from "+scheduledOrRunning+" to "+(scheduledOrRunning+1)); 
	scheduledOrRunning++;
    }

    /**
     * Must be called by all tasks submitted to the {@link #merger} when finished.
     * Unblocks {@link #waitForNotRunning()} if the merger's queue got empty by this.
     */
    private synchronized void taskFinished() {
        logger.fine(""+this+" taskFinished decrementing scheduleOrRunning from "+scheduledOrRunning+" to "+(scheduledOrRunning-1)); 
	scheduledOrRunning--;
	if (scheduledOrRunning == 0) {
	    logger.fine(""+this+" taskFinished notifying waiters"); 
	    notifyAll();
	}
    }

    @Override
    public synchronized void waitForNotRunning() {
	while (scheduledOrRunning > 0) {
	    try {
		wait();
		logger.fine(""+this+" waitForNotRunning scheduleOrRunning: "+scheduledOrRunning);
	    } catch (InterruptedException e) {
		// ignore interruption, try again
	    }
	}
    }
}
