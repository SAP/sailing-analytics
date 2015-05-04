package com.sap.sse.datamining.impl.functions;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.sap.sse.datamining.DataRetrieverChainDefinitionRegistry;
import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.DataMiningServerImpl;
import com.sap.sse.datamining.impl.SimpleDataRetrieverChainDefinitionRegistry;
import com.sap.sse.datamining.test.functions.registry.test_contexts.Test_HasContextWithDeadConnectorEnd;
import com.sap.sse.datamining.test.functions.registry.test_contexts.Test_HasLegOfCompetitorContext;
import com.sap.sse.datamining.test.functions.registry.test_contexts.Test_HasRaceContext;
import com.sap.sse.datamining.test.functions.test_classes.Test_ExternalLibraryClass;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.ExpectedFunctionRegistryUtil;
import com.sap.sse.datamining.test.util.OpenFunctionManager;
import com.sap.sse.datamining.test.util.TestsUtil;


public class TestFunctionManagerAsFunctionRegistry {
    
    private static ExpectedFunctionRegistryUtil expectedFunctionRegistryUtil;
    
    private Set<Class<?>> internalClassesToScan;
    private HashSet<Class<?>> externalClassesToScan;

    @BeforeClass
    public static void intializeExpectedFunctions() throws NoSuchMethodException, SecurityException {
        expectedFunctionRegistryUtil = new ExpectedFunctionRegistryUtil();
    }
    
    @Before
    public void initializeClassesToScan() {
        internalClassesToScan = new HashSet<>();
        internalClassesToScan.add(Test_HasLegOfCompetitorContext.class);
        internalClassesToScan.add(Test_HasRaceContext.class);
        internalClassesToScan.add(Test_HasContextWithDeadConnectorEnd.class);
        
        externalClassesToScan = new HashSet<>();
        externalClassesToScan.add(Test_ExternalLibraryClass.class);
    }
    
    @Test
    public void testRegistration() throws NoSuchMethodException, SecurityException {
        OpenFunctionManager functionRegistry = new OpenFunctionManager();
        DataRetrieverChainDefinitionRegistry retrieverChainRegistry = new SimpleDataRetrieverChainDefinitionRegistry();
        ModifiableDataMiningServer server = new DataMiningServerImpl(ConcurrencyTestsUtil.getExecutor(), functionRegistry, functionRegistry, retrieverChainRegistry);

        Date beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerAllClasses(internalClassesToScan);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(true));
        
        beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerAllWithExternalFunctionPolicy(externalClassesToScan);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(true));
        
        Collection<Function<?>> expectedDimensions = new HashSet<>();
        expectedDimensions.addAll(expectedFunctionRegistryUtil.getExpectedDimensionsFor(Test_HasLegOfCompetitorContext.class));
        expectedDimensions.addAll(expectedFunctionRegistryUtil.getExpectedDimensionsFor(Test_HasRaceContext.class));
        assertThat(functionRegistry.getDimensions(), is(expectedDimensions));
        
        Collection<Function<?>> expectedStatistics = expectedFunctionRegistryUtil.getExpectedStatisticsFor(Test_HasLegOfCompetitorContext.class);
        assertThat(server.getAllStatistics(), is(expectedStatistics));
        
        Collection<Function<?>> expectedExternalFunctions = expectedFunctionRegistryUtil.getExpectedExternalFunctionsFor(Test_ExternalLibraryClass.class);
        assertThat(functionRegistry.getExternalFunctions(), is(expectedExternalFunctions));
    }
    
    @Test
    public void testMultipleRegistrationAndUnregistrationOfTheSameClasses() {
        ModifiableDataMiningServer server = TestsUtil.createNewServer();

        Date beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerAllClasses(internalClassesToScan);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(true));
        beforeRegistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.registerAllClasses(internalClassesToScan);
        assertThat(server.getComponentsChangedTimepoint().after(beforeRegistration), is(false));

        Date beforeUnregistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterAllFunctionsOf(internalClassesToScan);
        assertThat(server.getComponentsChangedTimepoint().after(beforeUnregistration), is(true));
        beforeUnregistration = new Date();
        ConcurrencyTestsUtil.sleepFor(10);
        server.unregisterAllFunctionsOf(internalClassesToScan);
        assertThat(server.getComponentsChangedTimepoint().after(beforeUnregistration), is(false));
    }
    
    @Test
    public void testUnregistration() {
        OpenFunctionManager registry = new OpenFunctionManager();
        registry.registerAllClasses(internalClassesToScan);
        
        Collection<Function<?>> expectedDimensions = new HashSet<>();
        expectedDimensions.addAll(expectedFunctionRegistryUtil.getExpectedDimensionsFor(Test_HasLegOfCompetitorContext.class));
        expectedDimensions.addAll(expectedFunctionRegistryUtil.getExpectedDimensionsFor(Test_HasRaceContext.class));
        assertThat(registry.getDimensions(), is(expectedDimensions));
        
        Set<Class<?>> classesToUnregister = new HashSet<>();
        classesToUnregister.add(Test_HasLegOfCompetitorContext.class);
        assertThat(registry.unregisterAllFunctionsOf(classesToUnregister), is(true));
        
        expectedDimensions = expectedFunctionRegistryUtil.getExpectedDimensionsFor(Test_HasRaceContext.class);
        assertThat(registry.getDimensions(), is(expectedDimensions));

        registry.registerAllClasses(internalClassesToScan);
        classesToUnregister = new HashSet<>();
        classesToUnregister.add(Test_HasRaceContext.class);
        assertThat(registry.unregisterAllFunctionsOf(classesToUnregister), is(true));
        
        expectedDimensions = expectedFunctionRegistryUtil.getExpectedDimensionsFor(Test_HasLegOfCompetitorContext.class);
        assertThat(registry.getDimensions(), is(expectedDimensions));
    }

}
