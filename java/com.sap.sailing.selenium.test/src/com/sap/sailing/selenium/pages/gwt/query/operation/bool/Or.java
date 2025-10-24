package com.sap.sailing.selenium.pages.gwt.query.operation.bool;

import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.operation.PredicateOperation;

public class Or extends PredicateOperation {

    public Or(Expression<Boolean> left, Expression<Boolean> rigth) {
        super(left, rigth);
    }

    @Override
    public Boolean evaluate(Object argument) {
        Expression<Boolean> leftExpression = getArg(0);
        Boolean leftValue = leftExpression.evaluate(argument);
        
        Expression<Boolean> rigthExpression = getArg(1);
        Boolean rigthValue = rigthExpression.evaluate(argument);
        
        return Boolean.valueOf(leftValue.booleanValue() | rigthValue.booleanValue());
    }

}
