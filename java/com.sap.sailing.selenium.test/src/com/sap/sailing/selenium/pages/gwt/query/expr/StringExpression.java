package com.sap.sailing.selenium.pages.gwt.query.expr;

import com.sap.sailing.selenium.pages.gwt.query.Expression;
import com.sap.sailing.selenium.pages.gwt.query.operation.BooleanOperation;
import com.sap.sailing.selenium.pages.gwt.query.operation.string.Matches;

public class StringExpression extends ComparableExpression<String> {
    
    public StringExpression(Expression<String> mixin) {
        super(mixin);
    }
    
//    public BooleanExpression isEmpty()
//    public BooleanExpression isNotEmpty()
//    
//    public NumberExpression<Integer> length()
    
    /**
     * Return true if this String matches the given regular expression
     * 
     * @param regex
     * @return 
     * @see java.lang.String#matches(String)
     */
    public BooleanExpression matches(Expression<String> regex) {
        return new BooleanOperation(new Matches(getMixin(), regex));
    }

    /**
     * Return true if this String matches the given regular expression
     * 
     * @param regex
     * @return 
     * @see java.lang.String#matches(String)
     */
    public BooleanExpression matches(String regex) {
        return matches(ConstantExpression.create(regex));
    }
    
//    public StringExpression append(Expression<String> string)
//    public StringExpression append(String string)
//    public BooleanExpression contains(Expression<String> string)
//    public BooleanExpression contains(String string)
//    public BooleanExpression startsWith(Expression<String> string)
//    public BooleanExpression startsWith(String string)
//    public BooleanExpression endsWith(Expression<String> string)
//    public BooleanExpression endsWith(String string)
}
