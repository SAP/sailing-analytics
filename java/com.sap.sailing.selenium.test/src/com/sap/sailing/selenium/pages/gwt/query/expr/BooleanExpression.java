package com.sap.sailing.selenium.pages.gwt.query.expr;

import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.Predicate;
import com.sap.sailing.selenium.pages.gwt.query.operation.BooleanOperation;
import com.sap.sailing.selenium.pages.gwt.query.operation.bool.And;
import com.sap.sailing.selenium.pages.gwt.query.operation.bool.Not;
import com.sap.sailing.selenium.pages.gwt.query.operation.bool.Or;

public class BooleanExpression extends SimpleExpression<Boolean> implements Predicate {
    
    public BooleanExpression(Expression<Boolean> mixin) {
        super(mixin);
    }
    
    @Override
    public BooleanExpression not() {
        return new BooleanOperation(new Not(this));
    }
    
    /**
     * Get an intersection of this and the given expression
     *
     * @param right right hand side of the union
     * @return this && right
     */
    @SuppressWarnings("unchecked")
    public BooleanExpression and(Predicate right) {
        if (right == null)
            return this;
        
        return new BooleanOperation(new And((Expression<Boolean>) this.mixin, right));
    }
    
    /**
     * 
     *
     * @param right right hand side of the union
     * @return this || right
     */
    @SuppressWarnings("unchecked")
    public BooleanExpression or(Predicate right) {
        if (right == null)
            return this;
        
        return new BooleanOperation(new Or((Expression<Boolean>) this.mixin, right));
    }
    
    public BooleanExpression isTrue() {
        return eq(Boolean.TRUE);
    }

    /**
     * Get a this == false expression
     *
     * @return
     *   Returns true if this expression evaluates to false.
     */
    public BooleanExpression isFalse() {
        return eq(Boolean.FALSE);
    }
    
    @Override
    public BooleanExpression eq(Boolean right) {
        if (right == null) {
            throw new IllegalArgumentException("eq(null) is not allowed. Use isNull() instead");
        }
        
        return eq(new ConstantExpression<>(right));
    }
}
