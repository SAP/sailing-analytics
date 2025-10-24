package com.sap.sailing.selenium.pages.gwt.query.operation.bool;

import java.util.Objects;

import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.operation.PredicateOperation;

public class Eq extends PredicateOperation {

    public Eq(Expression<?> left, Expression<?> rigth) {
        super(left, rigth);
    }

    @Override
    public Boolean evaluate(Object argument) {
        Expression<?> leftExpression = getArgument(0);
        Object leftValue = leftExpression.evaluate(argument);
        
        Expression<?> rigthExpression = getArgument(1);
        Object rigthValue = rigthExpression.evaluate(argument);
        
        return Boolean.valueOf(Objects.equals(leftValue, rigthValue));
    }

}
