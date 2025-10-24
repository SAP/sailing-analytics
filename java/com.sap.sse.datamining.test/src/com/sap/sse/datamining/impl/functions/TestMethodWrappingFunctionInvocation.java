package com.sap.sse.datamining.impl.functions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.test.data.impl.DataTypeWithContext;
import com.sap.sse.datamining.test.data.impl.DataTypeWithContextImpl;
import com.sap.sse.datamining.test.data.impl.SimpleClassWithMarkedMethods;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;

public class TestMethodWrappingFunctionInvocation {
    
    private Function<String> getRegattaName;
    private Function<Integer> increment;

    @BeforeEach
    public void setUpFunctions() {
        getRegattaName = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(DataTypeWithContext.class, "getRegattaName"));
        increment = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "increment", int.class));
    }

    @Test
    public void testInvocationWithNoParameters() {
        DataTypeWithContext dataEntry = new DataTypeWithContextImpl("Regatta Name", "Race Name", 7);
        assertThat(getRegattaName.tryToInvoke(dataEntry), is(dataEntry.getRegattaName()));
    }

    @Test
    public void testInvocationWithParameters() {
        SimpleClassWithMarkedMethods instance = new SimpleClassWithMarkedMethods();
        final int valueToIncrement = 10;
        assertThat(increment.tryToInvoke(instance, () -> {return new Object[] {valueToIncrement};} ), is(instance.increment(valueToIncrement)));
    }
    
    @Test
    public void testInvocationWithWrongParameters() {
        DataTypeWithContext dataEntry = new DataTypeWithContextImpl("Regatta Name", "Race Name", 7);
        // the one parameter is ignored; see bug 4034; in case of concatenated functions, excess parameters are ignored
        // and would only be used for those methods that need it; therefore, we receive a valid result here
        assertThat(getRegattaName.tryToInvoke(dataEntry, () -> {return new Object[0];} ), is("Regatta Name"));
        SimpleClassWithMarkedMethods instance = new SimpleClassWithMarkedMethods();
        // invoking a method that expects an int parameter with no ParameterProvider is expected to return null ("error")
        assertThat(increment.tryToInvoke(instance), is(nullValue()));
    }

}
