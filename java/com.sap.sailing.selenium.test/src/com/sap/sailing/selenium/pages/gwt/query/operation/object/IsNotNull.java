package com.sap.sailing.selenium.pages.gwt.query.operation.object;

import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.operation.PredicateOperation;

public class IsNotNull extends PredicateOperation {

    public IsNotNull(Expression<?> argument) {
        super(argument);
    }

    @Override
    public Boolean evaluate(Object argument) {
        Expression<?> leftExpression = getArgument(0);
        Object leftValue = leftExpression.evaluate(argument);
        
        return Boolean.valueOf(leftValue != null);
    }

}
