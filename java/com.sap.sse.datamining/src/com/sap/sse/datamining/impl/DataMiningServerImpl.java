package com.sap.sse.datamining.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.datamining.DataSourceProvider;
import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.Query.QueryType;
import com.sap.sse.datamining.StatisticQueryDefinition;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.components.management.AggregationProcessorDefinitionProvider;
import com.sap.sse.datamining.components.management.AggregationProcessorDefinitionRegistry;
import com.sap.sse.datamining.components.management.DataRetrieverChainDefinitionProvider;
import com.sap.sse.datamining.components.management.DataRetrieverChainDefinitionRegistry;
import com.sap.sse.datamining.components.management.DataSourceProviderRegistry;
import com.sap.sse.datamining.components.management.FunctionProvider;
import com.sap.sse.datamining.components.management.FunctionRegistry;
import com.sap.sse.datamining.components.management.MemoryMonitor;
import com.sap.sse.datamining.components.management.MemoryMonitorAction;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.factories.QueryFactory;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.components.DataRetrieverLevel;
import com.sap.sse.datamining.impl.components.management.AbstractMemoryMonitorAction;
import com.sap.sse.datamining.impl.components.management.QueryManagerMemoryMonitor;
import com.sap.sse.datamining.impl.components.management.RuntimeMemorInfoProvider;
import com.sap.sse.datamining.impl.components.management.StrategyPerQueryTypeManager;
import com.sap.sse.datamining.shared.DataMiningSession;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.shared.impl.dto.QueryResultDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.CompoundResourceBundleStringMessages;

public class DataMiningServerImpl implements ModifiableDataMiningServer {
    
    private static final long MEMORY_CHECK_PERIOD = 5;
    private static final TimeUnit MEMORY_CHECK_PERIOD_UNIT = TimeUnit.SECONDS;
    
    private final CompoundResourceBundleStringMessages stringMessages;
    private final ExecutorService executorService;
    private Date componentsChangedTimepoint;
    
    private final QueryFactory queryFactory;
    private final StrategyPerQueryTypeManager dataMiningQueryManager;
    private final MemoryMonitor memoryMonitor;
    
    private final FunctionRegistry functionRegistry;
    private final DataSourceProviderRegistry dataSourceProviderRegistry;
    private final DataRetrieverChainDefinitionRegistry dataRetrieverChainDefinitionRegistry;
    private final AggregationProcessorDefinitionRegistry aggregationProcessorDefinitionRegistry;

    public DataMiningServerImpl(ExecutorService executorService, FunctionRegistry functionRegistry,
                                DataSourceProviderRegistry dataSourceProviderRegistry,
                                DataRetrieverChainDefinitionRegistry dataRetrieverChainDefinitionRegistry,
                                AggregationProcessorDefinitionRegistry aggregationProcessorDefinitionRegistry) {
        this.stringMessages = new CompoundResourceBundleStringMessages();
        this.executorService = executorService;
        componentsChangedTimepoint = new Date();
        this.queryFactory = new QueryFactory();
        dataMiningQueryManager = new StrategyPerQueryTypeManager();
        memoryMonitor = new QueryManagerMemoryMonitor(new RuntimeMemorInfoProvider(Runtime.getRuntime()), dataMiningQueryManager,
                                                      createMemoryMonitorActions(), MEMORY_CHECK_PERIOD, MEMORY_CHECK_PERIOD_UNIT);
        this.functionRegistry = functionRegistry;
        this.dataSourceProviderRegistry = dataSourceProviderRegistry;
        this.dataRetrieverChainDefinitionRegistry = dataRetrieverChainDefinitionRegistry;
        this.aggregationProcessorDefinitionRegistry = aggregationProcessorDefinitionRegistry;
    }
    
    private Iterable<MemoryMonitorAction> createMemoryMonitorActions() {
        Collection<MemoryMonitorAction> actions = new ArrayList<>();
        actions.add(new AbstractMemoryMonitorAction(0.10) {
            @Override
            public void performAction() {
                memoryMonitor.logWarning("Yellow Alert free memory is below " + getThreshold() + "%!");
                int numberOfRunningStatisticQueries = dataMiningQueryManager.getNumberOfRunningQueriesOfType(QueryType.STATISTIC);
                if (numberOfRunningStatisticQueries > 0) {
                    memoryMonitor.logWarning("Aborting random statistic query.");
                    dataMiningQueryManager.abortRandomQueryOfType(QueryType.STATISTIC);
                } else {
                    memoryMonitor.logWarning("Can't abort random statistic query, because none are running.");
                }
            }
        });
        actions.add(new AbstractMemoryMonitorAction(0.05) {
            @Override
            public void performAction() {
                memoryMonitor.logSevere("Red Alert free memory is below " + getThreshold() + "%!");
                if (dataMiningQueryManager.getNumberOfRunningQueries() > 0) {
                    memoryMonitor.logSevere("Aborting all queries.");
                    dataMiningQueryManager.abortAllQueries();
                } else {
                    memoryMonitor.logSevere("Can't abort all queries, because none are running.");
                }
            }
        });
        return actions;
    }

    @Override
    public Date getComponentsChangedTimepoint() {
        return componentsChangedTimepoint;
    }
    
    private void updateComponentsChangedTimepoint() {
        componentsChangedTimepoint = new Date();
    }

    @Override
    public ExecutorService getExecutorService() {
        return executorService;
    }
    
    @Override
    public ResourceBundleStringMessages getStringMessages() {
        return stringMessages;
    }

    @Override
    public void addStringMessages(ResourceBundleStringMessages stringMessages) {
        boolean componentsChanged = this.stringMessages.addStringMessages(stringMessages);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }

    @Override
    public void removeStringMessages(ResourceBundleStringMessages stringMessages) {
        boolean componentsChanged = this.stringMessages.removeStringMessages(stringMessages);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }

    @Override
    public FunctionRegistry getFunctionRegistry() {
        return functionRegistry;
    }
    
    @Override
    public void registerAllClasses(Iterable<Class<?>> classesToScan) {
        boolean componentsChanged = functionRegistry.registerAllClasses(classesToScan);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public void registerAllWithExternalFunctionPolicy(Iterable<Class<?>> externalClassesToScan) {
        boolean componentsChanged = functionRegistry.registerAllWithExternalFunctionPolicy(externalClassesToScan);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public void unregisterAllFunctionsOf(Iterable<Class<?>> classesToUnregister) {
        boolean componentsChanged = functionRegistry.unregisterAllFunctionsOf(classesToUnregister);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }

    @Override
    public FunctionProvider getFunctionProvider() {
        return functionRegistry;
    }

    @Override
    public Iterable<Function<?>> getAllStatistics() {
        return functionRegistry.getAllStatistics();
    }

    @Override
    public Iterable<Function<?>> getFunctionsFor(Class<?> sourceType) {
        return functionRegistry.getFunctionsFor(sourceType);
    }

    @Override
    public Iterable<Function<?>> getStatisticsFor(Class<?> sourceType) {
        return functionRegistry.getStatisticsFor(sourceType);
    }

    @Override
    public Iterable<Function<?>> getDimensionsFor(Class<?> sourceType) {
        return functionRegistry.getDimensionsFor(sourceType);
    }

    @Override
    public Map<DataRetrieverLevel<?, ?>, Iterable<Function<?>>> getDimensionsMappedByLevelFor(DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition) {
        return functionRegistry.getDimensionsMappedByLevelFor(dataRetrieverChainDefinition);
    }
    
    @Override
    public Map<DataRetrieverLevel<?, ?>, Iterable<Function<?>>> getReducedDimensionsMappedByLevelFor(
            DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition) {
        return functionRegistry.getReducedDimensionsMappedByLevelFor(dataRetrieverChainDefinition);
    }

    @Override
    public Function<?> getFunctionForDTO(FunctionDTO functionDTO) {
        return functionRegistry.getFunctionForDTO(functionDTO);
    }
    
    @Override
    public void registerDataSourceProvider(DataSourceProvider<?> dataSourceProvider) {
        boolean componentsChanged = dataSourceProviderRegistry.register(dataSourceProvider);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public void unregisterDataSourceProvider(DataSourceProvider<?> dataSourceProvider) {
        boolean componentsChanged = dataSourceProviderRegistry.unregister(dataSourceProvider);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public DataRetrieverChainDefinitionProvider getDataRetrieverChainDefinitionProvider() {
        return dataRetrieverChainDefinitionRegistry;
    }
    
    @Override
    public DataRetrieverChainDefinitionRegistry getDataRetrieverChainDefinitionRegistry() {
        return dataRetrieverChainDefinitionRegistry;
    }
    
    @Override
    public Iterable<DataRetrieverChainDefinition<?, ?>> getDataRetrieverChainDefinitions() {
        return dataRetrieverChainDefinitionRegistry.getAll();
    }
    
    @Override
    public void registerDataRetrieverChainDefinition(DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition) {
        boolean componentsChanged = dataRetrieverChainDefinitionRegistry.register(dataRetrieverChainDefinition);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public void unregisterDataRetrieverChainDefinition(DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition) {
        boolean componentsChanged = dataRetrieverChainDefinitionRegistry.unregister(dataRetrieverChainDefinition);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public <DataSourceType> Iterable<DataRetrieverChainDefinition<DataSourceType, ?>> getDataRetrieverChainDefinitionsBySourceType(
            Class<DataSourceType> dataSourceType) {
        return dataRetrieverChainDefinitionRegistry.getBySourceType(dataSourceType);
    }
    
    @Override
    public <DataType> Iterable<DataRetrieverChainDefinition<?, DataType>> getDataRetrieverChainDefinitionsByDataType(
            Class<DataType> dataType) {
        return dataRetrieverChainDefinitionRegistry.getByDataType(dataType);
    }

    @Override
    public <DataSourceType, DataType> Iterable<DataRetrieverChainDefinition<DataSourceType, DataType>> getDataRetrieverChainDefinitions(
            Class<DataSourceType> dataSourceType, Class<DataType> retrievedDataType) {
        return dataRetrieverChainDefinitionRegistry.get(dataSourceType, retrievedDataType);
    }

    @Override
    public <DataSourceType, DataType> DataRetrieverChainDefinition<DataSourceType, DataType> getDataRetrieverChainDefinition(UUID id) {
        return dataRetrieverChainDefinitionRegistry.get(id);
    }

    @Override
    public AggregationProcessorDefinitionProvider getAggregationProcessorProvider() {
        return aggregationProcessorDefinitionRegistry;
    }

    @Override
    public <ExtractedType> Iterable<AggregationProcessorDefinition<? super ExtractedType, ?>> getAggregationProcessorDefinitions(
            Class<ExtractedType> extractedType) {
        return aggregationProcessorDefinitionRegistry.getByExtractedType(extractedType);
    }
    
    @Override
    public <ExtractedType> AggregationProcessorDefinition<? super ExtractedType, ?> getAggregationProcessorDefinition(
            Class<ExtractedType> extractedType, String aggregationNameMessageKey) {
        return aggregationProcessorDefinitionRegistry.get(extractedType, aggregationNameMessageKey);
    }

    @Override
    public <ExtractedType, ResultType> AggregationProcessorDefinition<ExtractedType, ResultType> getAggregationProcessorDefinitionForDTO(AggregationProcessorDefinitionDTO aggregatorDefinitionDTO) {
        return aggregationProcessorDefinitionRegistry.getForDTO(aggregatorDefinitionDTO);
    }

    @Override
    public AggregationProcessorDefinitionRegistry getAggregationProcessorRegistry() {
        return aggregationProcessorDefinitionRegistry;
    }

    @Override
    public void registerAggregationProcessor(AggregationProcessorDefinition<?, ?> aggregationProcessorDefinition) {
        boolean componentsChanged = aggregationProcessorDefinitionRegistry.register(aggregationProcessorDefinition);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }

    @Override
    public void unregisterAggregationProcessor(AggregationProcessorDefinition<?, ?> aggregationProcessorDefinition) {
        boolean componentsChanged = aggregationProcessorDefinitionRegistry.unregister(aggregationProcessorDefinition);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public <DataSourceType, DataType, ExtractedType, ResultType> StatisticQueryDefinition<DataSourceType, DataType, ExtractedType, ResultType> getQueryDefinitionForDTO(StatisticQueryDefinitionDTO queryDefinitionDTO) {
        ModifiableStatisticQueryDefinition<DataSourceType, DataType, ExtractedType, ResultType> queryDefinition = null;
        
        Locale locale = ResourceBundleStringMessages.Util.getLocaleFor(queryDefinitionDTO.getLocaleInfoName());
        DataRetrieverChainDefinition<DataSourceType, DataType> retrieverChain = getDataRetrieverChainDefinition(queryDefinitionDTO.getDataRetrieverChainDefinition().getId());
        @SuppressWarnings("unchecked")
        Function<ExtractedType> statisticToCalculate = (Function<ExtractedType>) getFunctionForDTO(queryDefinitionDTO.getStatisticToCalculate());
        
        if (locale != null && retrieverChain != null && statisticToCalculate != null) {
            AggregationProcessorDefinition<ExtractedType, ResultType> aggregatorDefinition = getAggregationProcessorDefinitionForDTO(queryDefinitionDTO.getAggregatorDefinition());
            queryDefinition = new ModifiableStatisticQueryDefinition<>(locale, retrieverChain, statisticToCalculate, aggregatorDefinition);
            
            Map<DataRetrieverLevelDTO, SerializableSettings> retrieverSettings = queryDefinitionDTO.getRetrieverSettings();
            Map<DataRetrieverLevelDTO, HashMap<FunctionDTO, HashSet<? extends Serializable>>> filterSelection = queryDefinitionDTO.getFilterSelection();
            for (DataRetrieverLevel<?, ?> retrieverLevel : retrieverChain.getDataRetrieverLevels()) {
                if (retrieverSettings.containsKey(retrieverLevel)) {
                    queryDefinition.setRetrieverSettings(retrieverLevel, retrieverSettings.get(retrieverLevel));
                }
                
                if (filterSelection.containsKey(retrieverLevel)) {
                    for (Entry<FunctionDTO, HashSet<? extends Serializable>> levelSpecificFilterSelectionEntry : filterSelection.get(retrieverLevel).entrySet()) {
                        Function<?> dimensionToFilterBy = getFunctionForDTO(levelSpecificFilterSelectionEntry.getKey());
                        if (dimensionToFilterBy != null) {
                            queryDefinition.setFilterSelection(retrieverLevel, dimensionToFilterBy, levelSpecificFilterSelectionEntry.getValue());
                        }
                    }
                }
            }
            for (Entry<DataRetrieverLevelDTO, HashMap<FunctionDTO, HashSet<? extends Serializable>>> levelSpecificFilterSelection : queryDefinitionDTO.getFilterSelection().entrySet()) {
                DataRetrieverLevel<?, ?> retrieverLevel = retrieverChain.getDataRetrieverLevel(levelSpecificFilterSelection.getKey().getLevel());
                for (Entry<FunctionDTO, HashSet<? extends Serializable>> levelSpecificFilterSelectionEntry : levelSpecificFilterSelection.getValue().entrySet()) {
                    Function<?> dimensionToFilterBy = getFunctionForDTO(levelSpecificFilterSelectionEntry.getKey());
                    if (dimensionToFilterBy != null) {
                        queryDefinition.setFilterSelection(retrieverLevel, dimensionToFilterBy, levelSpecificFilterSelectionEntry.getValue());
                    }
                }
            }
             
            for (FunctionDTO dimensionToGroupByDTO : queryDefinitionDTO.getDimensionsToGroupBy()) {
                Function<?> dimensionToGroupBy = getFunctionForDTO(dimensionToGroupByDTO);
                if (dimensionToGroupBy != null) {
                    queryDefinition.addDimensionToGroupBy(dimensionToGroupBy);
                }
            }
        }
        
        return queryDefinition;
    }

    @Override
    public <DataSourceType, ResultType> Query<ResultType> createQuery(StatisticQueryDefinition<DataSourceType, ?, ?, ResultType> queryDefinition) {
        DataSourceProvider<DataSourceType> dataSourceProvider = getDataSourceProviderFor(queryDefinition.getDataRetrieverChainDefinition().getDataSourceType());
        return queryFactory.createQuery(dataSourceProvider.getDataSource(), queryDefinition, getStringMessages(), getExecutorService());
    }

    @Override
    public <DataSourceType> Query<HashSet<Object>> createDimensionValuesQuery(DataRetrieverChainDefinition<DataSourceType, ?> dataRetrieverChainDefinition, DataRetrieverLevel<?, ?> retrieverLevel,
            Iterable<Function<?>> dimensions, Map<DataRetrieverLevel<?, ?>, SerializableSettings> settings, Map<DataRetrieverLevel<?, ?>, Map<Function<?>, Collection<?>>> filterSelection, Locale locale) {
        DataSourceProvider<DataSourceType> dataSourceProvider = getDataSourceProviderFor(dataRetrieverChainDefinition.getDataSourceType());
        return queryFactory.createDimensionValuesQuery(dataSourceProvider.getDataSource(), dataRetrieverChainDefinition, retrieverLevel, dimensions, settings, filterSelection, locale, getStringMessages(), getExecutorService());
    }

    private <DataSourceType> DataSourceProvider<DataSourceType> getDataSourceProviderFor(Class<DataSourceType> dataSourceType) {
        DataSourceProvider<DataSourceType> dataSourceProvider = dataSourceProviderRegistry.get(dataSourceType);
        if (dataSourceProvider == null) {
            throw new NullPointerException("No DataSourceProvider found for '" + dataSourceType + "'");
        }
        return dataSourceProvider;
    }
    
    @Override
    public <ResultType> QueryResult<ResultType> runNewQueryAndAbortPreviousQueries(DataMiningSession session, Query<ResultType> query) {
        return dataMiningQueryManager.runNewAndAbortPrevious(session, query);
    }
    
    @Override
    public int getNumberOfRunningQueries() {
        return dataMiningQueryManager.getNumberOfRunningQueries();
    }
    
    @Override
    public <ResultType> QueryResultDTO<ResultType> convertToDTO(QueryResult<ResultType> result) {
        return new QueryResultDTO<ResultType>(result.getState(), result.getResultType(), result.getResults(), result.getAdditionalData());
    }
    
    
}
