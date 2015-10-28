package com.sap.sse.datamining;

import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.components.management.AggregationProcessorDefinitionRegistry;
import com.sap.sse.datamining.components.management.DataRetrieverChainDefinitionRegistry;
import com.sap.sse.datamining.components.management.FunctionRegistry;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public interface ModifiableDataMiningServer extends DataMiningServer {

    public void addDataMiningBundleClassLoader(ClassLoader classLoader);
    public void removeDataMiningBundleClassLoader(ClassLoader classLoader);

    public void addStringMessages(ResourceBundleStringMessages stringMessages);
    public void removeStringMessages(ResourceBundleStringMessages stringMessages);

    public FunctionRegistry getFunctionRegistry();
    public void registerAllClasses(Iterable<Class<?>> classesToScan);
    public void registerAllWithExternalFunctionPolicy(Iterable<Class<?>> externalClassesToScan);
    public void unregisterAllFunctionsOf(Iterable<Class<?>> classesToUnregister);
    
    public void registerDataSourceProvider(DataSourceProvider<?> dataSourceProvider);
    public void unregisterDataSourceProvider(DataSourceProvider<?> dataSourceProvider);
    
    public DataRetrieverChainDefinitionRegistry getDataRetrieverChainDefinitionRegistry();
    public void registerDataRetrieverChainDefinition(DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition);
    public void unregisterDataRetrieverChainDefinition(DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition);
    
    public AggregationProcessorDefinitionRegistry getAggregationProcessorRegistry();
    public void registerAggregationProcessor(AggregationProcessorDefinition<?, ?> aggregationProcessorDefinition);
    public void unregisterAggregationProcessor(AggregationProcessorDefinition<?, ?> aggregationProcessorDefinition);
    
}
