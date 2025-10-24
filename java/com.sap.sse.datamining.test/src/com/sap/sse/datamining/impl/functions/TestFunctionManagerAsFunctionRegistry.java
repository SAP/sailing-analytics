package com.sap.sse.datamining.impl.functions;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.components.management.AggregationProcessorDefinitionRegistry;
import com.sap.sse.datamining.components.management.DataRetrieverChainDefinitionRegistry;
import com.sap.sse.datamining.components.management.DataSourceProviderRegistry;
import com.sap.sse.datamining.components.management.QueryDefinitionDTORegistry;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.DataMiningServerImpl;
import com.sap.sse.datamining.impl.components.management.AggregationProcessorDefinitionManager;
import com.sap.sse.datamining.impl.components.management.DataRetrieverChainDefinitionManager;
import com.sap.sse.datamining.impl.components.management.DataSourceProviderManager;
import com.sap.sse.datamining.impl.components.management.QueryDefinitionDTOManager;
import com.sap.sse.datamining.test.data.Test_HasContextWithDeadConnectorEnd;
import com.sap.sse.datamining.test.data.Test_HasLegOfCompetitorContext;
import com.sap.sse.datamining.test.data.Test_HasRaceContext;
import com.sap.sse.datamining.test.data.impl.Test_ExternalLibraryClass;
import com.sap.sse.datamining.test.util.ConcurrencyTestsUtil;
import com.sap.sse.datamining.test.util.ExpectedFunctionRegistryUtil;
import com.sap.sse.datamining.test.util.TestsUtil;
import com.sap.sse.datamining.test.util.components.management.OpenFunctionManager;


public class TestFunctionManagerAsFunctionRegistry {
    
    private static ExpectedFunctionRegistryUtil expectedFunctionRegistryUtil;
    
    private Set<Class<?>> internalClassesToScan;
    private HashSet<Class<?>> externalClassesToScan;

    @BeforeAll
    public static void intializeExpectedFunctions() throws NoSuchMethodException, SecurityException {
        expectedFunctionRegistryUtil = new ExpectedFunctionRegistryUtil();
    }
    
    @BeforeEach
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
        OpenFunctionManager functionManager = new OpenFunctionManager();
        DataSourceProviderRegistry dataSourceProviderRegistry = new DataSourceProviderManager();
        DataRetrieverChainDefinitionRegistry dataRetrieverChainDefinitionRegistry = new DataRetrieverChainDefinitionManager();
        AggregationProcessorDefinitionRegistry aggregationProcessorDefinitionRegistry = new AggregationProcessorDefinitionManager();
        QueryDefinitionDTORegistry queryDefinitionRegistry = new QueryDefinitionDTOManager();
        ModifiableDataMiningServer server = new DataMiningServerImpl(ConcurrencyTestsUtil.getSharedExecutor(), functionManager,
                                                                     dataSourceProviderRegistry,
                                                                     dataRetrieverChainDefinitionRegistry,
                                                                     aggregationProcessorDefinitionRegistry,
                                                                     queryDefinitionRegistry);
        
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
        assertThat(functionManager.getDimensions(), is(expectedDimensions));
        
        Collection<Function<?>> expectedStatistics = expectedFunctionRegistryUtil.getExpectedStatisticsFor(Test_HasLegOfCompetitorContext.class);
        assertThat(server.getAllStatistics(), is(expectedStatistics));
        
        Collection<Function<?>> expectedExternalFunctions = expectedFunctionRegistryUtil.getExpectedExternalFunctionsFor(Test_ExternalLibraryClass.class);
        assertThat(functionManager.getExternalFunctions(), is(expectedExternalFunctions));
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
