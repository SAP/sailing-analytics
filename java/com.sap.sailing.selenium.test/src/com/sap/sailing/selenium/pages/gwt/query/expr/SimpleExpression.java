package com.sap.sailing.selenium.pages.gwt.query.expr;

import java.util.Arrays;
import java.util.Collection;

import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.operation.BooleanOperation;
import com.sap.sailing.selenium.pages.gwt.query.operation.bool.Eq;
import com.sap.sailing.selenium.pages.gwt.query.operation.collection.In;
import com.sap.sailing.selenium.pages.gwt.query.operation.object.IsNotNull;
import com.sap.sailing.selenium.pages.gwt.query.operation.object.IsNull;

/**
 * <p>SimpleExpression is the base class for scalar Expression implementations.</p>
 * 
 * @author
 *   D049941
 * @param <T>
 *   The type the expression is bound to.
 */
public class SimpleExpression<T> implements Expression<T> {
    protected final Expression<? extends T> mixin;
    
    public SimpleExpression(Expression<? extends T> mixin) {
        this.mixin = mixin;
    }
        
    @Override
    public final Class<? extends T> getType() {
        return this.mixin.getType();
    }

    /**
     * Creates a <code>this is null</code> expression
     *
     * @return
     */
    public BooleanExpression isNull() {
        return new BooleanOperation(new IsNull(this.mixin));
    }

    /**
     * Create a <code>this is not null</code> expression
     *
     * @return
     */
    public BooleanExpression isNotNull() {
        return new BooleanOperation(new IsNotNull(this.mixin));
    }
    
    /**
     * Get a <code>this == right</code> expression
     * 
     * <p>Use expr.isNull() instead of expr.eq(null)</p>
     *
     * @param right rhs of the comparison
     * @return
     */
    public BooleanExpression eq(T right) {
        if (right == null)
            throw new IllegalArgumentException("eq(null) is not allowed. Use isNull() instead");
        
        return eq(new ConstantExpression<>(right));
    }

    /**
     * Get a <code>this == right</code> expression
     *
     * @param right rhs of the comparison
     * @return
     */
    public BooleanExpression eq(Expression<? super T> right) {
        return new BooleanOperation(new Eq(this.mixin, right));
    }
    
    /**
     * Get a <code>this in right</code> expression
     *
     * @param right rhs of the comparison
     * @return
     */
    public BooleanExpression in(Collection<? extends T> right) {
        if (right.size() == 1) {
            return eq(right.iterator().next());
        }
        
        return new BooleanOperation(new In(this.mixin, new ConstantExpression<Collection<?>>(right)));
    }
    
    /**
     * Get a <code>this in right</code> expression
     *
     * @param right rhs of the comparison
     * @return
     */
    @SuppressWarnings("unchecked")
    public BooleanExpression in(T... right) {
        return in(Arrays.asList(right));
    }
    
    /**
     * Get a <code>this in right</code> expression
     *
     * @param right rhs of the comparison
     * @return
     */
    public BooleanExpression notIn(Collection<? extends T> right) {
        return in(right).not();
    }
    
    /**
     * Get a <code>this in right</code> expression
     *
     * @param right rhs of the comparison
     * @return
     */
    @SuppressWarnings("unchecked")
    public BooleanExpression notIn(T... right) {
        return notIn(Arrays.asList(right));
    }
    
    @Override
    public final int hashCode() {
        return this.mixin.hashCode();
    }
    
    @Override
    public final String toString() {
        return this.mixin.toString();
    }

    @Override
    public T evaluate(Object argument) {
        return this.mixin.evaluate(argument);
    }
    
    @SuppressWarnings("unchecked")
    protected Expression<T> getMixin() {
        return (Expression<T>) this.mixin;
    }
}
