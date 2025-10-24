package com.sap.sailing.selenium.pages.gwt.query.operation;

import java.util.List;

import com.google.common.collect.ImmutableList;
import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.Operation;
import com.sap.sailing.selenium.pages.gwt.query.expr.ImmutableExpression;

public abstract class SimpleOperation<T> extends ImmutableExpression<T> implements Operation<T> {
    
    private final ImmutableList<Expression<?>> arguments;

    protected SimpleOperation(Class<? extends T> type, Expression<?>... args) {
        this(type, ImmutableList.copyOf(args));
    }

    protected SimpleOperation(Class<? extends T> type, ImmutableList<Expression<?>> arguments) {
        super(type);
        
        this.arguments = arguments;
    }

    @Override
    public final Expression<?> getArgument(int i) {
        return this.arguments.get(i);
    }

    @Override
    public final List<Expression<?>> getArguments() {
        return this.arguments;
    }

    @Override
    public final boolean equals(Object object) {
        if (object == this) {
            return true;
        }
        
        if (!(object instanceof Operation<?>)) {
            return false;
        }
        
        Operation<?> other = (Operation<?>) object;
        
        if(!this.arguments.equals(other.getArguments()))
            return false;
        
        return  other.getType().equals(getType());
    }

    @Override
    public int hashCode() {
        // TODO: Implement a better hashCode
        return super.hashCode();
    }
    
    @SuppressWarnings("unchecked")
    protected final <O> Expression<O> getArg(int i) {
        return (Expression<O>) this.arguments.get(i);
    }
}
