package com.sap.sse.datamining.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sse.common.Base64Utils;
import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.datamining.DataSourceProvider;
import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.Query;
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
import com.sap.sse.datamining.components.management.QueryDefinitionDTOProvider;
import com.sap.sse.datamining.components.management.QueryDefinitionDTORegistry;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.factories.QueryFactory;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.components.DataRetrieverLevel;
import com.sap.sse.datamining.impl.components.management.AbstractMemoryMonitorAction;
import com.sap.sse.datamining.impl.components.management.QueryManagerMemoryMonitor;
import com.sap.sse.datamining.impl.components.management.RuntimeMemoryInfoProvider;
import com.sap.sse.datamining.impl.components.management.StrategyPerQueryTypeManager;
import com.sap.sse.datamining.shared.DataMiningSession;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.PredefinedQueryIdentifier;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;
import com.sap.sse.i18n.impl.CompoundResourceBundleStringMessages;
import com.sap.sse.shared.classloading.JoinedClassLoader;
import com.sap.sse.util.ObjectInputStreamResolvingAgainstCache;

public class DataMiningServerImpl implements ModifiableDataMiningServer {
    private static final Logger logger = Logger.getLogger(DataMiningServerImpl.class.getName());
    
    private static final long MEMORY_CHECK_PERIOD = 5;
    private static final TimeUnit MEMORY_CHECK_PERIOD_UNIT = TimeUnit.SECONDS;
    
    private final Set<ClassLoader> dataMiningClassLoaders;
    
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
    private final QueryDefinitionDTORegistry queryDefinitionRegistry;

    public DataMiningServerImpl(ExecutorService executorService, FunctionRegistry functionRegistry,
                                DataSourceProviderRegistry dataSourceProviderRegistry,
                                DataRetrieverChainDefinitionRegistry dataRetrieverChainDefinitionRegistry,
                                AggregationProcessorDefinitionRegistry aggregationProcessorDefinitionRegistry,
                                QueryDefinitionDTORegistry queryDefinitionRegistry) {
        dataMiningClassLoaders = new HashSet<ClassLoader>();
        dataMiningClassLoaders.add(this.getClass().getClassLoader());
        this.stringMessages = new CompoundResourceBundleStringMessages();
        this.executorService = executorService;
        componentsChangedTimepoint = new Date();
        this.queryFactory = new QueryFactory();
        dataMiningQueryManager = new StrategyPerQueryTypeManager();
        memoryMonitor = new QueryManagerMemoryMonitor(new RuntimeMemoryInfoProvider(Runtime.getRuntime()), dataMiningQueryManager,
                                                      createMemoryMonitorActions(), MEMORY_CHECK_PERIOD, MEMORY_CHECK_PERIOD_UNIT);
        this.functionRegistry = functionRegistry;
        this.dataSourceProviderRegistry = dataSourceProviderRegistry;
        this.dataRetrieverChainDefinitionRegistry = dataRetrieverChainDefinitionRegistry;
        this.aggregationProcessorDefinitionRegistry = aggregationProcessorDefinitionRegistry;
        this.queryDefinitionRegistry = queryDefinitionRegistry;
    }
    
    private Iterable<MemoryMonitorAction> createMemoryMonitorActions() {
        Collection<MemoryMonitorAction> actions = new ArrayList<>();
        actions.add(new AbstractMemoryMonitorAction(0.10, /* 10GB minFreeMemoryInBytes */ 10l*1024*1024*1024) {
            @Override
            public void performAction() {
                memoryMonitor.logWarning("Yellow Alert free memory is below " + (100*getThreshold()) + "%!");
                final int numberOfRunningQueries = dataMiningQueryManager.getNumberOfRunningQueries();
                if (numberOfRunningQueries > 0) {
                    memoryMonitor.logWarning("Aborting random query.");
                    dataMiningQueryManager.abortRandomQuery();
                } else {
                    memoryMonitor.logWarning("Can't abort random query, because none are running.");
                }
            }
        });
        actions.add(new AbstractMemoryMonitorAction(0.05, /* 5GB minFreeMemoryInBytes */ 5l*1024*1024*1024) {
            @Override
            public void performAction() {
                memoryMonitor.logSevere("Red Alert free memory is below " + (100*getThreshold()) + "%!");
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
    public JoinedClassLoader getJoinedClassLoader() {
        return new JoinedClassLoader(dataMiningClassLoaders);
    }
    
    /**
     * @return the {@link StatisticQueryDefinitionDTO} from a base 64 string deserialized with java serialization,
     *         considering the {@link #getJoinedClassLoader() joined class loader} that offers all classes of all
     *         bundles that currently provide data mining components to this server
     */
    @Override
    public StatisticQueryDefinitionDTO fromBase64String(final String string) {
        byte[] bytes;
        try {
            bytes = Base64Utils.fromBase64(string);
        } catch (IllegalArgumentException e) {
            return null;
        }
        final ClassLoader oldThreadContextClassLoader = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(getJoinedClassLoader());
        try (final ObjectInputStream in = new ObjectInputStreamResolvingAgainstCache<Object>(
                new ByteArrayInputStream(bytes), /* dummy "cache" */ new Object(), /* resolve listener */ null, /* classLoaderCache */ new HashMap<>()) {}) {
            Object o = in.readObject();
            if (o instanceof StatisticQueryDefinitionDTO) {
                return (StatisticQueryDefinitionDTO) o;
            }
        } catch (IOException | ClassNotFoundException e) {
            logger.log(Level.SEVERE, "Could not load query", e);
        } finally {
            Thread.currentThread().setContextClassLoader(oldThreadContextClassLoader);
        }
        return null;
    }

    
    @Override
    public void addDataMiningBundleClassLoader(ClassLoader classLoader) {
        logger.info("Adding data mining bundle class loader "+classLoader);
        dataMiningClassLoaders.add(classLoader);
    }
    
    @Override
    public void removeDataMiningBundleClassLoader(ClassLoader classLoader) {
        logger.info("Removing data mining bundle class loader "+classLoader);
        dataMiningClassLoaders.remove(classLoader);
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
    public Function<?> getFunctionForDTO(FunctionDTO functionDTO) {
        return functionRegistry.getFunctionForDTO(functionDTO, getJoinedClassLoader());
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
    public <DataSourceType, DataType> DataRetrieverChainDefinition<DataSourceType, DataType> getDataRetrieverChainDefinitionForDTO(DataRetrieverChainDefinitionDTO retrieverChainDTO) {
        return dataRetrieverChainDefinitionRegistry.getForDTO(retrieverChainDTO, getJoinedClassLoader());
    }

    @Override
    public AggregationProcessorDefinitionProvider getAggregationProcessorProvider() {
        return aggregationProcessorDefinitionRegistry;
    }

    @Override
    public <ExtractedType, ResultType> AggregationProcessorDefinition<ExtractedType, ResultType> getAggregationProcessorDefinitionForDTO(AggregationProcessorDefinitionDTO aggregatorDefinitionDTO) {
        return aggregationProcessorDefinitionRegistry.getForDTO(aggregatorDefinitionDTO, getJoinedClassLoader());
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
    public QueryDefinitionDTOProvider getQueryDefinitionDTOProvider() {
        return queryDefinitionRegistry;
    }
    
    @Override
    public QueryDefinitionDTORegistry getQueryDefinitionDTORegistry() {
        return queryDefinitionRegistry;
    }
    
    @Override
    public void registerPredefinedQueryDefinition(PredefinedQueryIdentifier identifier, StatisticQueryDefinitionDTO queryDefinition) {
        boolean componentsChanged = queryDefinitionRegistry.register(identifier, queryDefinition);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public void unregisterPredefinedQueryDefinition(PredefinedQueryIdentifier identifier, StatisticQueryDefinitionDTO queryDefinition) {
        boolean componentsChanged = queryDefinitionRegistry.unregister(identifier, queryDefinition);
        if (componentsChanged) {
            updateComponentsChangedTimepoint();
        }
    }
    
    @Override
    public <DataSourceType, DataType, ExtractedType, ResultType> StatisticQueryDefinition<DataSourceType, DataType, ExtractedType, ResultType> getQueryDefinitionForDTO(StatisticQueryDefinitionDTO queryDefinitionDTO) {
        ModifiableStatisticQueryDefinition<DataSourceType, DataType, ExtractedType, ResultType> queryDefinition = null;
        
        Locale locale = ResourceBundleStringMessages.Util.getLocaleFor(queryDefinitionDTO.getLocaleInfoName());
        DataRetrieverChainDefinition<DataSourceType, DataType> retrieverChain = getDataRetrieverChainDefinitionForDTO(queryDefinitionDTO.getDataRetrieverChainDefinition());
        @SuppressWarnings("unchecked")
        Function<ExtractedType> statisticToCalculate = (Function<ExtractedType>) getFunctionForDTO(queryDefinitionDTO.getStatisticToCalculate());
        
        if (locale != null && retrieverChain != null && statisticToCalculate != null) {
            AggregationProcessorDefinition<ExtractedType, ResultType> aggregatorDefinition = getAggregationProcessorDefinitionForDTO(queryDefinitionDTO.getAggregatorDefinition());
            queryDefinition = new ModifiableStatisticQueryDefinition<>(locale, retrieverChain, statisticToCalculate, aggregatorDefinition);
            
            Map<DataRetrieverLevelDTO, SerializableSettings> retrieverSettings = queryDefinitionDTO.getRetrieverSettings();
            Map<DataRetrieverLevelDTO, HashMap<FunctionDTO, HashSet<? extends Serializable>>> filterSelection = queryDefinitionDTO.getFilterSelection();
            for (DataRetrieverLevelDTO retrieverLevelDTO : queryDefinitionDTO.getDataRetrieverChainDefinition().getRetrieverLevels()) {
                if (retrieverSettings.containsKey(retrieverLevelDTO)) {
                    queryDefinition.setRetrieverSettings(retrieverChain.getDataRetrieverLevel(retrieverLevelDTO.getLevel()), retrieverSettings.get(retrieverLevelDTO));
                }
                
                if (filterSelection.containsKey(retrieverLevelDTO)) {
                    for (Entry<FunctionDTO, HashSet<? extends Serializable>> levelSpecificFilterSelectionEntry : filterSelection.get(retrieverLevelDTO).entrySet()) {
                        Function<?> dimensionToFilterBy = getFunctionForDTO(levelSpecificFilterSelectionEntry.getKey());
                        if (dimensionToFilterBy != null) {
                            queryDefinition.setFilterSelection(retrieverChain.getDataRetrieverLevel(retrieverLevelDTO.getLevel()), dimensionToFilterBy, levelSpecificFilterSelectionEntry.getValue());
                        }
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
    
}
