package com.sap.sailing.datamining.function;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Test;

import com.sap.sailing.datamining.function.impl.MethodWrappingFunction;
import com.sap.sailing.datamining.function.impl.PartitionParallelExternalFunctionRetriever;
import com.sap.sailing.datamining.function.impl.PartitioningParallelMarkedFunctionRetriever;
import com.sap.sailing.datamining.function.impl.SimpleFunctionRegistry;
import com.sap.sailing.datamining.test.function.test_classes.ExternalLibraryClass;
import com.sap.sailing.datamining.test.function.test_classes.SimpleClassWithMarkedMethods;
import com.sap.sailing.datamining.test.util.FunctionTestsUtil;

public class TestSimpleFunctionRegistryRegistrations {

    @Test
    public void testSimpleRegistration() {
        Method dimension = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("dimension");
        
        FunctionRegistry registry = new SimpleFunctionRegistry();
        registry.register(dimension);
        
        Set<Function> expectedRegisteredFunctionsAsSet = new HashSet<>();
        expectedRegisteredFunctionsAsSet.add(new MethodWrappingFunction(dimension));
        Iterable<Function> expectedRegisteredFunctions = expectedRegisteredFunctionsAsSet;
        assertThat(registry.getAllRegisteredFunctions(), is(expectedRegisteredFunctions));
        assertThat(registry.getRegisteredFunctionsOf(SimpleClassWithMarkedMethods.class), is(expectedRegisteredFunctions));
    }
    
    @Test
    public void testRegistrationByFunctionRetrievers() {
        FunctionRegistry registry = new SimpleFunctionRegistry();
        registerMethodsOfTestClassesViaFunctionRetrieversTo(registry);

        Set<Function> expectedRegisteredFunctionsAsSet = new HashSet<>();
        expectedRegisteredFunctionsAsSet.addAll(FunctionTestsUtil.getMarkedMethodsOfSimpleClassWithMarkedMethod());
        expectedRegisteredFunctionsAsSet.addAll(FunctionTestsUtil.getMethodsOfExternalLibraryClass());
        Iterable<Function> expectedRegisteredFunctions = expectedRegisteredFunctionsAsSet;
        assertThat(registry.getAllRegisteredFunctions(), is(expectedRegisteredFunctions));
    }

    private void registerMethodsOfTestClassesViaFunctionRetrieversTo(FunctionRegistry registry) {
        Collection<Class<?>> classesToScan = new HashSet<>();
        classesToScan.add(SimpleClassWithMarkedMethods.class);
        ParallelFunctionRetriever markedFunctionRetriever = new PartitioningParallelMarkedFunctionRetriever(classesToScan, FunctionTestsUtil.getExecutor());
        registry.registerFunctionsRetrievedBy(markedFunctionRetriever);
        
        Collection<Class<?>> externalClasses = new HashSet<>();
        externalClasses.add(ExternalLibraryClass.class);
        ParallelFunctionRetriever externalFunctionRetriever = new PartitionParallelExternalFunctionRetriever(externalClasses, FunctionTestsUtil.getExecutor());
        registry.registerFunctionsRetrievedBy(externalFunctionRetriever);
    }
    
    @Test
    public void testTheMultipleRegistrationOfTheSameFunction() {
        Method dimension = FunctionTestsUtil.getMethodFromSimpleClassWithMarkedMethod("dimension");
        
        FunctionRegistry registry = new SimpleFunctionRegistry();
        registry.register(dimension);
        registry.register(dimension);
        registry.register(dimension);
        
        Set<Function> expectedRegisteredFunctionsAsSet = new HashSet<>();
        expectedRegisteredFunctionsAsSet.add(new MethodWrappingFunction(dimension));
        Iterable<Function> expectedRegisteredFunctions = expectedRegisteredFunctionsAsSet;
        assertThat(registry.getAllRegisteredFunctions(), is(expectedRegisteredFunctions));
    }

}
