package com.sap.sse.replication;

import java.io.Serializable;
import java.util.UUID;

import com.sap.sse.ServerInfo;
import com.sap.sse.common.WithID;

/**
 * Wraps an operation and delegates all methods to it. In addition, provides an {@link #id ID} which is used
 * as the basis for implementing {@link #equals(Object)} and {@link #hashCode()}. This way, an operation can
 * be uniquely identified after serializing it back and forth.
 * 
 * @author Axel Uhl (D043530)
 *
 * @param <S>
 * @param <R>
 */
public class OperationWithResultWithIdWrapper<S, R> implements OperationWithResult<S, R>, WithID {
    private static final long serialVersionUID = -5435955633510008283L;
    private final Serializable id;
    private final OperationWithResult<S, R> delegate;
    private final String originServerName;

    /**
     * Creates a new UUID for this wrapper operation
     */
    public OperationWithResultWithIdWrapper(OperationWithResult<S, R> delegate) {
        this(delegate, UUID.randomUUID());
    }

    /**
     * Allows the caller to define the ID for this wrapper.
     */
    protected OperationWithResultWithIdWrapper(OperationWithResult<S, R> delegate, Serializable id) {
        super();
        this.id = id;
        this.delegate = delegate;
        this.originServerName = delegate.getOriginServerName() == null ? ServerInfo.getName() : delegate.getOriginServerName();
    }

    /**
     * @return the {@link #delegate}'s class for keeping statistics
     */
    @Override
    public Class<?> getClassForLogging() {
        return delegate.getClassForLogging();
    }
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((id == null) ? 0 : id.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        OperationWithResultWithIdWrapper<?, ?> other = (OperationWithResultWithIdWrapper<?, ?>) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        return true;
    }

    @Override
    public boolean isRequiresExplicitTransitiveReplication() {
        return delegate.isRequiresExplicitTransitiveReplication();
    }
    
    @Override
    public boolean requiresSynchronousExecution() {
        return delegate.requiresSynchronousExecution();
    }
    
    @Override
    public S applyTo(S toState) {
        return delegate.applyTo(toState);
    }

    @Override
    public R internalApplyTo(S toState) throws Exception {
        return delegate.internalApplyTo(toState);
    }

    @Override
    public Serializable getId() {
        return id;
    }
    
    @Override
    public String getOriginServerName() {
        return originServerName;
    }

    @Override
    public String toString() {
        return ""+delegate+" with ID "+getId()+(originServerName==null?"":(" from server \""+getOriginServerName()+"\""));
    }
}
