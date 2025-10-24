package com.sap.sse.replication;

import java.io.ObjectOutputStream;
import java.io.Serializable;

import com.sap.sse.ServerInfo;
import com.sap.sse.operationaltransformation.Operation;

/**
 * An operational transformation {@link Operation} is expected to return the target state after applying the operation.
 * This operation type offers the possibility to let an operation return a result value from its
 * {@link #internalApplyTo} method while in the context of operational transformation the operation will still return
 * the target state.<p>
 * 
 * Operations that are instance of a class implementing this interface inherit the serializability declared by this
 * interface. Multiple operation instances may be serialized into the same {@link ObjectOutputStream} (see also
 * bug 5741). Keep this in mind when choosing how to represent the operation's internals. For example, it is not a
 * good idea to have the operation reference a mutable object with the intention to transport a new "to-be" state
 * this way. If multiple such state changes end up in the same stream of operations, the mutable object will be
 * serialized to the stream only once, in one (probably the first, thus oldest) state, and all subsequent operations
 * referencing that same object will serialize only a handle referencing the object state already written. With this,
 * you may lose state changes. Hence, make sure to either clone such objects before storing the clone in the operation,
 * or---even better---use specialized operations describing the specific state change to apply. For example, instead
 * of serializing an object with all its attributes, rather implement an operation per attribute that updates only
 * that attribute's value. It may be more tedious, but it keeps your serialized operations small, is therefore more
 * bandwidth-efficient in the replication architecture and less prone to accidentally dropping state changes by
 * representing the same object in the stream only once.
 * 
 * @param <S>
 *            type of state to which the operation can be applied
 * @param <R>
 *            result type, or in other words, the return type of the {@link #internalApplyTo(Object)} operation
 * 
 * @author Axel Uhl (D043530)
 *
 */
@FunctionalInterface
public interface OperationWithResult<S, R> extends Operation<S>, Serializable {
    /**
     * Performs the actual operation, applying it to the <code>toState</code> service. The operation's result is
     * returned.<p>
     * 
     * <b>NOTE:</b> If invoking this method triggers replication of the operation's effects, this object must
     * return <code>false</code> from the {@link #isRequiresExplicitTransitiveReplication()} method to avoid
     * replication duplication in transitive replication scenarios.
     */
    R internalApplyTo(S toState) throws Exception;
    
    /**
     * Tells whether this operation requires explicit transitive replication to other replicas when received by a
     * replica. This is the case for all operations whose {@link #internalApplyTo(Object)} method will not trigger
     * replication. An example for an operation that does <em>not</em> require explicit transitive replication is the
     * insertion of a GPS fix into a competitor's track because the insertion of the fix, as implemented by
     * {@link #internalApplyTo} will trigger the replication.
     * <p>
     * 
     * This default implementation returns <code>true</code>. Implementing classes whose
     * {@link #internalApplyTo(Object)} operation implicitly triggers replication must override this to return
     * <code>false</code>.
     */
    default boolean isRequiresExplicitTransitiveReplication() {
        return true;
    }
    
    /**
     * @return the class to use for keeping statistics; this allows wrapper classes to report the actual inner class
     */
    default Class<?> getClassForLogging() {
        return getClass();
    }

    /**
     * Ignores the actual result of {@link #internalApplyTo(Object)} and returns <code>toState</code> which
     * for the operational transformation algorithm is the "next state reached."
     */
    @Override
    default S applyTo(S toState) {
        try {
            internalApplyTo(toState);
            return toState;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns {@code null} by default; however, operations that originated on a different server and were sent
     * to their primary will return their {@link ServerInfo#getName() server name}. This can be used, e.g., for
     * debug output.
     */
    default String getOriginServerName() {
        return null;
    }
}
