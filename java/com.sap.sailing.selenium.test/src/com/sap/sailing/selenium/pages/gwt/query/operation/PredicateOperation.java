package com.sap.sailing.selenium.pages.gwt.query.operation;

import com.google.common.collect.ImmutableList;
import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.Predicate;
import com.sap.sailing.selenium.pages.gwt.query.operation.bool.Not;

public abstract class PredicateOperation extends SimpleOperation<Boolean> implements Predicate {
    
    public PredicateOperation(Expression<?> one) {
        this(ImmutableList.<Expression<?>>of(one));
    }
    
    public PredicateOperation(Expression<?> one, Expression<?> two) {
        this(ImmutableList.of(one, two));
    }
    
    public PredicateOperation(Expression<?>... args) {
        this(ImmutableList.copyOf(args));
    }
    
    public PredicateOperation(ImmutableList<Expression<?>> args) {
        super(Boolean.class, args);
    }
    
    @Override
    public Predicate not() {
        return new Not(this);
    }
}
