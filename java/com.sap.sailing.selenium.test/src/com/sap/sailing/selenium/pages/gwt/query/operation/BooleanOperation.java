package com.sap.sailing.selenium.pages.gwt.query.operation;

import java.util.List;

import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.Operation;
import com.sap.sailing.selenium.pages.gwt.query.expr.BooleanExpression;
import com.sap.sailing.selenium.pages.gwt.query.operation.bool.Not;

public class BooleanOperation extends BooleanExpression implements Operation<Boolean> {
    
    private final Operation<Boolean> operationMixin;
    
    @SuppressWarnings("unchecked")
    public BooleanOperation(Operation<Boolean> mixin) {
        super(mixin);
        
        this.operationMixin = (Operation<Boolean>) this.mixin;
    }
    
    
    @Override
    public Expression<?> getArgument(int index) {
        return this.operationMixin.getArgument(index);
    }

    @Override
    public List<Expression<?>> getArguments() {
        return this.operationMixin.getArguments();
    }
    
    @Override
    public BooleanExpression not() {
        if (this.mixin instanceof Not && getArgument(0) instanceof BooleanExpression) {
            return (BooleanExpression) getArgument(0);
        }
        
        return super.not();
    }
}
