package com.sap.sse.datamining;

import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutorService;

import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.components.management.AggregationProcessorDefinitionProvider;
import com.sap.sse.datamining.components.management.DataRetrieverChainDefinitionProvider;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.functions.FunctionProvider;
import com.sap.sse.datamining.impl.components.DataRetrieverLevel;
import com.sap.sse.datamining.shared.DataMiningSession;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.shared.impl.dto.QueryResultDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;


public interface DataMiningServer {
    
    public ExecutorService getExecutorService();
    public ResourceBundleStringMessages getStringMessages();

    public Date getComponentsChangedTimepoint();

    public FunctionProvider getFunctionProvider();
    public Iterable<Function<?>> getAllStatistics();
    public Iterable<Function<?>> getFunctionsFor(Class<?> sourceType);
    public Iterable<Function<?>> getStatisticsFor(Class<?> sourceType);
    public Iterable<Function<?>> getDimensionsFor(Class<?> sourceType);
    public Map<DataRetrieverLevel<?, ?>, Iterable<Function<?>>> getDimensionsMappedByLevelFor(
            DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition);
    public Map<DataRetrieverLevel<?, ?>, Iterable<Function<?>>> getReducedDimensionsMappedByLevelFor(
            DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition);
    /**
     * @return The first function, that matches the given DTO or <code>null</code>
     */
    public Function<?> getFunctionForDTO(FunctionDTO functionDTO);

    public DataRetrieverChainDefinitionProvider getDataRetrieverChainDefinitionProvider();
    public Iterable<DataRetrieverChainDefinition<?, ?>> getDataRetrieverChainDefinitions();
    public <DataSourceType> Iterable<DataRetrieverChainDefinition<DataSourceType, ?>> getDataRetrieverChainDefinitionsBySourceType(
            Class<DataSourceType> dataSourceType);
    public <DataType> Iterable<DataRetrieverChainDefinition<?, DataType>> getDataRetrieverChainDefinitionsByDataType(
            Class<DataType> dataType);
    public <DataSourceType, DataType> Iterable<DataRetrieverChainDefinition<DataSourceType, DataType>> getDataRetrieverChainDefinitions(
            Class<DataSourceType> dataSourceType, Class<DataType> retrievedDataType);
    public <DataSourceType, DataType> DataRetrieverChainDefinition<DataSourceType, DataType> getDataRetrieverChainDefinition(UUID id);
    
    public <DataSourceType> Query<Set<Object>> createDimensionValuesQuery(DataRetrieverChainDefinition<DataSourceType, ?> dataRetrieverChainDefinition, int retrieverLevel,
            Iterable<Function<?>> dimensions, Map<Integer, Map<Function<?>, Collection<?>>> filterSelection, Locale locale);
    public <DataSourceType, DataType, ExtractedType, ResultType> StatisticQueryDefinition<DataSourceType, DataType, ExtractedType, ResultType> getQueryDefinitionForDTO(StatisticQueryDefinitionDTO queryDefinitionDTO);
    public <DataSourceType, ResultType> Query<ResultType> createQuery(StatisticQueryDefinition<DataSourceType, ?, ?, ResultType> queryDefinition);
    public <ResultType> QueryResult<ResultType> runNewQueryAndAbortPreviousQueries(DataMiningSession session, Query<ResultType> query);
    
    public AggregationProcessorDefinitionProvider getAggregationProcessorProvider();
    public <ExtractedType> Iterable<AggregationProcessorDefinition<? super ExtractedType, ?>> getAggregationProcessorDefinitions(Class<ExtractedType> extractedType);
    public <ExtractedType> AggregationProcessorDefinition<? super ExtractedType, ?> getAggregationProcessorDefinition(Class<ExtractedType> extractedType, String aggregationNameMessageKey);
    public <ExtractedType, ResultType> AggregationProcessorDefinition<ExtractedType, ResultType> getAggregationProcessorDefinitionForDTO(AggregationProcessorDefinitionDTO aggregatorDefinitionDTO);
    
    public <ResultType> QueryResultDTO<ResultType> convertToDTO(QueryResult<ResultType> result);
    
}
