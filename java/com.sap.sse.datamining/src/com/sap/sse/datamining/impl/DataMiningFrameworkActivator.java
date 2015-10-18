package com.sap.sse.datamining.impl;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;
import org.osgi.util.tracker.ServiceTracker;
import org.osgi.util.tracker.ServiceTrackerCustomizer;

import com.sap.sse.datamining.DataMiningBundleService;
import com.sap.sse.datamining.DataMiningPredefinedQueryService;
import com.sap.sse.datamining.DataMiningServer;
import com.sap.sse.datamining.DataSourceProvider;
import com.sap.sse.datamining.ModifiableDataMiningServer;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.components.management.AggregationProcessorDefinitionRegistry;
import com.sap.sse.datamining.components.management.DataRetrieverChainDefinitionRegistry;
import com.sap.sse.datamining.components.management.DataSourceProviderRegistry;
import com.sap.sse.datamining.components.management.FunctionRegistry;
import com.sap.sse.datamining.components.management.QueryDefinitionDTORegistry;
import com.sap.sse.datamining.impl.components.aggregators.ParallelGroupedDataCountAggregationProcessor;
import com.sap.sse.datamining.impl.components.aggregators.ParallelGroupedNumberDataAverageAggregationProcessor;
import com.sap.sse.datamining.impl.components.aggregators.ParallelGroupedNumberDataMaxAggregationProcessor;
import com.sap.sse.datamining.impl.components.aggregators.ParallelGroupedNumberDataMedianAggregationProcessor;
import com.sap.sse.datamining.impl.components.aggregators.ParallelGroupedNumberDataMinAggregationProcessor;
import com.sap.sse.datamining.impl.components.aggregators.ParallelGroupedNumberDataSumAggregationProcessor;
import com.sap.sse.datamining.impl.components.management.AggregationProcessorDefinitionManager;
import com.sap.sse.datamining.impl.components.management.DataRetrieverChainDefinitionManager;
import com.sap.sse.datamining.impl.components.management.DataSourceProviderManager;
import com.sap.sse.datamining.impl.components.management.FunctionManager;
import com.sap.sse.datamining.impl.components.management.QueryDefinitionDTOManager;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.i18n.impl.ResourceBundleStringMessagesImpl;

public class DataMiningFrameworkActivator implements BundleActivator {
    private static final Logger logger = Logger.getLogger(DataMiningFrameworkActivator.class.getName());
    private static final String STRING_MESSAGES_BASE_NAME = "stringmessages/StringMessages";
    private static final int THREAD_POOL_SIZE = Math.max(Runtime.getRuntime().availableProcessors(), 3);
    
    private static DataMiningFrameworkActivator INSTANCE;

    private ServiceTracker<DataMiningBundleService, DataMiningBundleService> dataMiningBundleServiceTracker;
    private ServiceTracker<DataMiningPredefinedQueryService, DataMiningPredefinedQueryService> dataMiningPredefinedQueryServiceTracker;
    private final Collection<ServiceRegistration<?>> serviceRegistrations;
    
    private final ModifiableDataMiningServer dataMiningServer;
    
    public DataMiningFrameworkActivator() {
        dataMiningServer = createDataMiningServer();
        serviceRegistrations = new HashSet<>();
    }

    private ModifiableDataMiningServer createDataMiningServer() {
        ExecutorService executor = new DataMiningExecutorService(THREAD_POOL_SIZE);
        FunctionRegistry functionRegistry = new FunctionManager();
        DataSourceProviderRegistry dataSourceProviderRegistry = new DataSourceProviderManager();
        DataRetrieverChainDefinitionRegistry dataRetrieverChainDefinitionRegistry = new DataRetrieverChainDefinitionManager();
        AggregationProcessorDefinitionRegistry aggregationProcessorDefinitionRegistry = new AggregationProcessorDefinitionManager();
        QueryDefinitionDTORegistry queryDefinitionRegistry = new QueryDefinitionDTOManager();
        ModifiableDataMiningServer dataMiningServer = new DataMiningServerImpl(executor, functionRegistry,
                                                                               dataSourceProviderRegistry,
                                                                               dataRetrieverChainDefinitionRegistry,
                                                                               aggregationProcessorDefinitionRegistry,
                                                                               queryDefinitionRegistry);
        dataMiningServer.addStringMessages(new ResourceBundleStringMessagesImpl(STRING_MESSAGES_BASE_NAME, this.getClass().getClassLoader()));
        for (AggregationProcessorDefinition<?, ?> aggregationProcessorDefinition : getDefaultAggregationProcessors()) {
            dataMiningServer.registerAggregationProcessor(aggregationProcessorDefinition);
        }
        return dataMiningServer;
    }

    private Iterable<AggregationProcessorDefinition<?, ?>> getDefaultAggregationProcessors() {
        Collection<AggregationProcessorDefinition<?, ?>> defaultAggregationProcessors = new HashSet<>();
        defaultAggregationProcessors.add(ParallelGroupedDataCountAggregationProcessor.getDefinition());
        defaultAggregationProcessors.add(ParallelGroupedNumberDataAverageAggregationProcessor.getDefinition());
        defaultAggregationProcessors.add(ParallelGroupedNumberDataMaxAggregationProcessor.getDefinition());
        defaultAggregationProcessors.add(ParallelGroupedNumberDataMedianAggregationProcessor.getDefinition());
        defaultAggregationProcessors.add(ParallelGroupedNumberDataMinAggregationProcessor.getDefinition());
        defaultAggregationProcessors.add(ParallelGroupedNumberDataSumAggregationProcessor.getDefinition());
        return defaultAggregationProcessors;
    }

    @Override
    public void start(BundleContext context) throws Exception {
        INSTANCE = this;
        
        dataMiningBundleServiceTracker = new ServiceTracker<>(context, DataMiningBundleService.class, new ServiceTrackerCustomizer<DataMiningBundleService, DataMiningBundleService>() {
            @Override
            public DataMiningBundleService addingService(ServiceReference<DataMiningBundleService> reference) {
                DataMiningBundleService dataMiningBundleService = context.getService(reference);
                registerDataMiningBundle(dataMiningBundleService);
                return dataMiningBundleService;
            }
            @Override
            public void modifiedService(ServiceReference<DataMiningBundleService> reference, DataMiningBundleService service) { }
            @Override
            public void removedService(ServiceReference<DataMiningBundleService> reference, DataMiningBundleService dataMiningBundleService) {
                unregisterDataMiningBundle(dataMiningBundleService);
            }
        });
        dataMiningBundleServiceTracker.open();
        
        dataMiningPredefinedQueryServiceTracker = new ServiceTracker<>(context, DataMiningPredefinedQueryService.class, new ServiceTrackerCustomizer<DataMiningPredefinedQueryService, DataMiningPredefinedQueryService>() {
            @Override
            public DataMiningPredefinedQueryService addingService(ServiceReference<DataMiningPredefinedQueryService> reference) {
                DataMiningPredefinedQueryService predefinedQueryService = context.getService(reference);
                for (Entry<String, StatisticQueryDefinitionDTO> predefinedQueryEntry : predefinedQueryService.getPredefinedQueries().entrySet()) {
                    dataMiningServer.registerPredefinedQueryDefinition(predefinedQueryEntry.getKey(), predefinedQueryEntry.getValue());
                }
                return predefinedQueryService;
            }
            @Override
            public void modifiedService(ServiceReference<DataMiningPredefinedQueryService> reference, DataMiningPredefinedQueryService service) { }
            @Override
            public void removedService(ServiceReference<DataMiningPredefinedQueryService> reference, DataMiningPredefinedQueryService predefinedQueryService) {
                for (Entry<String, StatisticQueryDefinitionDTO> predefinedQueryEntry : predefinedQueryService.getPredefinedQueries().entrySet()) {
                    dataMiningServer.unregisterPredefinedQueryDefinition(predefinedQueryEntry.getKey(), predefinedQueryEntry.getValue());
                }
            }
        });
        dataMiningPredefinedQueryServiceTracker.open();

        serviceRegistrations.add(context.registerService(DataMiningServer.class, dataMiningServer, null));
    }

    private void registerDataMiningBundle(DataMiningBundleService dataMiningBundleService) {
        logger.info("Registering data mining bundle " + dataMiningBundleService);
        dataMiningServer.addDataMiningBundleClassLoader(dataMiningBundleService.getClassLoader());
        dataMiningServer.addStringMessages(dataMiningBundleService.getStringMessages());
        dataMiningServer.registerAllClasses(dataMiningBundleService.getClassesWithMarkedMethods());
        for (DataSourceProvider<?> dataSourceProvider : dataMiningBundleService.getDataSourceProviders()) {
            dataMiningServer.registerDataSourceProvider(dataSourceProvider);
        }
        for (DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition : dataMiningBundleService.getDataRetrieverChainDefinitions()) {
            dataMiningServer.registerDataRetrieverChainDefinition(dataRetrieverChainDefinition);
        }
        for (AggregationProcessorDefinition<?, ?> aggregationProcessorDefinition : dataMiningBundleService.getAggregationProcessorDefinitions()) {
            dataMiningServer.registerAggregationProcessor(aggregationProcessorDefinition);
        }
    }

    private void unregisterDataMiningBundle(DataMiningBundleService dataMiningBundleService) {
        logger.info("Unregistering data mining bundle " + dataMiningBundleService);
        dataMiningServer.removeDataMiningBundleClassLoader(dataMiningBundleService.getClassLoader());
        dataMiningServer.removeStringMessages(dataMiningBundleService.getStringMessages());
        dataMiningServer.unregisterAllFunctionsOf(dataMiningBundleService.getClassesWithMarkedMethods());
        for (DataSourceProvider<?> dataSourceProvider : dataMiningBundleService.getDataSourceProviders()) {
            dataMiningServer.unregisterDataSourceProvider(dataSourceProvider);
        }
        for (DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition : dataMiningBundleService.getDataRetrieverChainDefinitions()) {
            dataMiningServer.unregisterDataRetrieverChainDefinition(dataRetrieverChainDefinition);
        }
        for (AggregationProcessorDefinition<?, ?> aggregationProcessorDefinition : dataMiningBundleService.getAggregationProcessorDefinitions()) {
            dataMiningServer.unregisterAggregationProcessor(aggregationProcessorDefinition);
        }
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        dataMiningBundleServiceTracker.close();
        dataMiningPredefinedQueryServiceTracker.close();
        
        for (ServiceRegistration<?> serviceRegistration : serviceRegistrations) {
            context.ungetService(serviceRegistration.getReference());
        }
    }
    
    public static DataMiningFrameworkActivator getDefault() {
        if (INSTANCE == null) {
            INSTANCE = new DataMiningFrameworkActivator(); // probably non-OSGi case, as in test execution
        }
        return INSTANCE;
    }

}
