package com.sap.sailing.gwt.ui.datamining;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.datamining.shared.SailingDataMiningSerializationDummy;
import com.sap.sse.datamining.shared.DataMiningSession;
import com.sap.sse.datamining.shared.SSEDataMiningSerializationDummy;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.shared.impl.dto.QueryResultDTO;

public interface DataMiningServiceAsync {

    public void getComponentsChangedTimepoint(AsyncCallback<Date> asyncCallback);

    void getAllStatistics(String localeInfoName, AsyncCallback<Iterable<FunctionDTO>> callback);
    void getStatisticsFor(DataRetrieverChainDefinitionDTO currentRetrieverChainDefinition, String localeName,
            AsyncCallback<Iterable<FunctionDTO>> asyncCallback);

    void getAggregatorDefinitionsFor(FunctionDTO extractionFunction, String localeInfoName,
            AsyncCallback<Iterable<AggregationProcessorDefinitionDTO>> asyncCallback);

    void getDimensionsFor(FunctionDTO statisticToCalculate, String localeInfoName, AsyncCallback<Iterable<FunctionDTO>> callback);
    void getDimensionsFor(DataRetrieverChainDefinitionDTO dataRetrieverChainDefinitionDTO, String localeInfoName,
            AsyncCallback<Iterable<FunctionDTO>> callback);
    
    void getDataRetrieverChainDefinitions(String localeInfoName, AsyncCallback<Iterable<DataRetrieverChainDefinitionDTO>> asyncCallback);
    void getDataRetrieverChainDefinitionsFor(FunctionDTO statisticToCalculate, String localeInfoName,
            AsyncCallback<Iterable<DataRetrieverChainDefinitionDTO>> callback);

    void getDimensionValuesFor(DataMiningSession session, DataRetrieverChainDefinitionDTO dataRetrieverChainDefinitionDTO, int retrieverLevel,
            Iterable<FunctionDTO> dimensionDTOs, Map<Integer, Map<FunctionDTO, Collection<?>>> filterSelectionDTO,
            String localeInfoName, AsyncCallback<QueryResultDTO<Set<Object>>> callback);

    <ResultType extends Number> void runQuery(DataMiningSession session, StatisticQueryDefinitionDTO queryDefinition, AsyncCallback<QueryResultDTO<ResultType>> callback);
    
    /**
     * This method does nothing, but is needed to ensure, that some classes for the data mining
     * (like {@link GenericGroupKey}) is added to the GWT serialization policy.<br />
     * This is necessary, because the type is somehow hidden from GWT. For Further information
     * look at bug 1503.<br />
     */
    void pseudoMethodSoThatSomeSSEDataMiningClassesAreAddedToTheGWTSerializationPolicy(AsyncCallback<SSEDataMiningSerializationDummy> asyncCallback);
    
    /**
     * This method does nothing, but is needed to ensure, that some classes for the data mining
     * (like {@link GenericGroupKey}) is added to the GWT serialization policy.<br />
     * This is necessary, because the type is somehow hidden from GWT. For Further information
     * look at bug 1503.<br />
     */
    void pseudoMethodSoThatSomeSailingDataMiningClassesAreAddedToTheGWTSerializationPolicy(AsyncCallback<SailingDataMiningSerializationDummy> asyncCallback);

}
