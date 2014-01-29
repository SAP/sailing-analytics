package com.sap.sailing.datamining.function.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.datamining.factories.FunctionFactory;
import com.sap.sailing.datamining.function.Function;
import com.sap.sailing.datamining.test.function.test_classes.DataTypeWithContext;
import com.sap.sailing.datamining.test.function.test_classes.DataTypeWithContextImpl;
import com.sap.sailing.datamining.test.function.test_classes.SimpleClassWithMarkedMethods;
import com.sap.sailing.datamining.test.util.FunctionTestsUtil;

public class TestMethodWrappingFunctionInvocation {
    
    private Function<String> getRegattaName;
    private Function<Integer> increment;

    @Before
    public void setUpFunctions() {
        getRegattaName = FunctionFactory.createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(DataTypeWithContext.class, "getRegattaName"));
        increment = FunctionFactory.createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "increment", int.class));
    }

    @Test
    public void testInvocationWithNoParameters() {
        DataTypeWithContext dataEntry = new DataTypeWithContextImpl("Regatta Name", "Race Name", 7);
        assertThat(getRegattaName.invoke(dataEntry), is(dataEntry.getRegattaName()));
    }

    @Test
    public void testInvocationWithParameters() {
        SimpleClassWithMarkedMethods instance = new SimpleClassWithMarkedMethods();
        int valueToIncrement = 10;
        assertThat(increment.invoke(instance, valueToIncrement), is(instance.increment(valueToIncrement)));
    }
    
    @Test
    public void testInvocationWithWrongParameters() {
        DataTypeWithContext dataEntry = new DataTypeWithContextImpl("Regatta Name", "Race Name", 7);
        assertThat(getRegattaName.invoke(dataEntry, "Wrong Parameter"), is(nullValue()));

        SimpleClassWithMarkedMethods instance = new SimpleClassWithMarkedMethods();
        assertThat(increment.invoke(instance), is(nullValue()));
    }

}
