package com.sap.sse.datamining.impl.functions;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.test.data.impl.ContainerElement;
import com.sap.sse.datamining.test.data.impl.ContainerElementImpl;
import com.sap.sse.datamining.test.data.impl.MarkedContainer;
import com.sap.sse.datamining.test.data.impl.MarkedContainerImpl;
import com.sap.sse.datamining.test.util.FunctionTestsUtil;
import com.sap.sse.datamining.test.util.TestsUtil;

public class TestCompoundFunction {

    private Function<?> getContainerElementFunction;
    private Function<?> getNameFunction;
    
    private Function<String> compoundFunction;
    
    @Before
    public void initializeCompoundFunction() throws ClassCastException, NoSuchMethodException, SecurityException {
        List<Function<?>> functions = new ArrayList<>();
        getContainerElementFunction = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(MarkedContainer.class.getMethod("getContainerElement", new Class<?>[0]));
        functions.add(getContainerElementFunction);
        getNameFunction = FunctionTestsUtil.getFunctionFactory().createMethodWrappingFunction(ContainerElement.class.getMethod("getName", new Class<?>[0]));
        functions.add(getNameFunction);
        
        compoundFunction = FunctionTestsUtil.getFunctionFactory().createCompoundFunction(functions);
    }

    @Test(expected=IllegalArgumentException.class)
    public void testConstruction() {
        List<Function<?>> functions = new ArrayList<>();
        functions.add(getContainerElementFunction);
        new ConcatenatingCompoundFunction<String>(functions, String.class);
    }
    
    @Test
    public void testInitialValues() {
        assertThat(compoundFunction.isDimension(), is(true));
        //This is a workaround, because the assertion, that to types are equal checks for an instance of the given class instead.
        assertThat("The actual declaring class did'nt match the expected one.",
                compoundFunction.getDeclaringType().equals(MarkedContainer.class), is(true));
        assertThat(compoundFunction.getSimpleName(), is("getContainerElement -> getName"));
        assertThat(compoundFunction.getLocalizedName(Locale.ENGLISH, TestsUtil.getTestStringMessages()), is("Name"));
        assertThat(compoundFunction.getResultDecimals(), is(0));
    }
    
    @Test
    public void testInvocation() {
        String containerElementName = "TestElement";
        ContainerElement element = new ContainerElementImpl(containerElementName);
        MarkedContainer container = new MarkedContainerImpl(element);
        
        String result = compoundFunction.tryToInvoke(container);
        assertThat(result, is(containerElementName));
    }

}
