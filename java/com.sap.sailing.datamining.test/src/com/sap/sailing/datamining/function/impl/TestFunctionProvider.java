package com.sap.sailing.datamining.function.impl;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.datamining.DataMiningStringMessages;
import com.sap.sailing.datamining.factories.FunctionFactory;
import com.sap.sailing.datamining.function.Function;
import com.sap.sailing.datamining.function.FunctionProvider;
import com.sap.sailing.datamining.function.FunctionRegistry;
import com.sap.sailing.datamining.function.ParallelFunctionRetriever;
import com.sap.sailing.datamining.shared.dto.FunctionDTO;
import com.sap.sailing.datamining.test.function.test_classes.DataTypeWithContext;
import com.sap.sailing.datamining.test.function.test_classes.DataTypeWithContextImpl;
import com.sap.sailing.datamining.test.function.test_classes.DataTypeWithContextProcessor;
import com.sap.sailing.datamining.test.function.test_classes.ExtendingInterface;
import com.sap.sailing.datamining.test.function.test_classes.ExternalLibraryClass;
import com.sap.sailing.datamining.test.function.test_classes.SimpleClassWithMarkedMethods;
import com.sap.sailing.datamining.test.util.FunctionTestsUtil;
import com.sap.sailing.datamining.test.util.StringMessagesForTests;

public class TestFunctionProvider {
    
    private static final DataMiningStringMessages stringMessages = new StringMessagesForTests();
    
    private FunctionRegistry functionRegistry;
    
    @Before
    public void initializeFunctionRegistry() {
        functionRegistry = new SimpleFunctionRegistry();
        
        Collection<Class<?>> classesToScan = new HashSet<>();
        classesToScan.add(SimpleClassWithMarkedMethods.class);
        classesToScan.add(DataTypeWithContext.class);
        classesToScan.add(DataTypeWithContextProcessor.class);
        classesToScan.add(ExtendingInterface.class);
        ParallelFunctionRetriever markedFunctionRetriever = new PartitioningParallelMarkedFunctionRetriever(classesToScan, FunctionTestsUtil.getExecutor());
        functionRegistry.registerFunctionsRetrievedBy(markedFunctionRetriever);
        
        Collection<Class<?>> externalClasses = new HashSet<>();
        externalClasses.add(ExternalLibraryClass.class);
        ParallelFunctionRetriever externalFunctionRetriever = new PartitionParallelExternalFunctionRetriever(externalClasses, FunctionTestsUtil.getExecutor());
        functionRegistry.registerFunctionsRetrievedBy(externalFunctionRetriever);
    }

    @Test
    public void testGetDimensionsForType() {
        FunctionProvider functionProvider = new RegistryFunctionsProvider(functionRegistry);
        
        Collection<Function<?>> expectedDimensions = FunctionTestsUtil.getDimensionsFor(DataTypeWithContext.class);
        
        Collection<Function<?>> providedDimensions = new HashSet<>(functionProvider.getDimenionsFor(DataTypeWithContext.class));
        assertThat(providedDimensions, is(expectedDimensions));
        
        providedDimensions = new HashSet<>(functionProvider.getDimenionsFor(DataTypeWithContextImpl.class));
        assertThat(providedDimensions, is(expectedDimensions));
    }
    
    @Test
    public void testGetFunctionsForType() {
        FunctionProvider functionProvider = new RegistryFunctionsProvider(functionRegistry);
        Method getRegattaAndRaceName = FunctionTestsUtil.getMethodFromClass(DataTypeWithContextProcessor.class, "getRegattaAndRaceName", DataTypeWithContext.class);
        
        Collection<Function<?>> expectedFunctions = FunctionTestsUtil.getMarkedMethodsOfDataTypeWithContextAndItsSupertypes();
        expectedFunctions.add(FunctionFactory.createMethodWrappingFunction(getRegattaAndRaceName));
        
        Collection<Function<?>> providedFunctions = new HashSet<>(functionProvider.getFunctionsFor(DataTypeWithContext.class));
        assertThat(providedFunctions, is(expectedFunctions));
        
        expectedFunctions = FunctionTestsUtil.getMarkedMethodsOfDataTypeWithContextImplAndItsSupertypes();
        expectedFunctions.add(FunctionFactory.createMethodWrappingFunction(getRegattaAndRaceName));
        
        providedFunctions = new HashSet<>(functionProvider.getFunctionsFor(DataTypeWithContextImpl.class));
        assertThat(providedFunctions, is(expectedFunctions));
    }
    
    @Test
    public void testGetFunctionForDTO() {
        Method getRegattaNameMethod = FunctionTestsUtil.getMethodFromClass(DataTypeWithContext.class, "getRegattaName");
        Function<Object> getRegattaName = FunctionFactory.createMethodWrappingFunction(getRegattaNameMethod);
        FunctionDTO getRegattaNameDTO = getRegattaName.asDTO(Locale.ENGLISH, stringMessages);
        
        FunctionProvider functionProvider = new RegistryFunctionsProvider(functionRegistry);
        @SuppressWarnings("unchecked") // Hamcrest requires type matching of actual and expected type, so the Functions have to be specific (without <?>)
        Function<Object> providedFunction = (Function<Object>) functionProvider.getFunctionFor(getRegattaNameDTO);
        assertThat(providedFunction, is(getRegattaName));
    }
    
    @Test
    public void testGetFunctionForUnregisteredDTO() {
        Method incrementMethod = FunctionTestsUtil.getMethodFromClass(SimpleClassWithMarkedMethods.class, "increment", int.class);
        Function<Object> increment = FunctionFactory.createMethodWrappingFunction(incrementMethod);
        FunctionDTO incrementDTO = increment.asDTO(Locale.ENGLISH, stringMessages);
        
        FunctionProvider functionProvider = new RegistryFunctionsProvider(functionRegistry);
        assertThat(functionProvider.getFunctionFor(incrementDTO), is(nullValue()));
    }
    
    @Test
    public void testGetFunctionForNullDTO() {
        FunctionProvider functionProvider = new RegistryFunctionsProvider(functionRegistry);
        assertThat(functionProvider.getFunctionFor(null), is(nullValue()));
    }

}
