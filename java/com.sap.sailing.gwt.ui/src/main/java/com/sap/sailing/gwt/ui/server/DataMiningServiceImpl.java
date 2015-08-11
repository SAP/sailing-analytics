package com.sap.sailing.gwt.ui.server;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.osgi.framework.BundleContext;
import org.osgi.util.tracker.ServiceTracker;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import com.sap.sailing.datamining.shared.SailingDataMiningSerializationDummy;
import com.sap.sailing.gwt.ui.datamining.DataMiningService;
import com.sap.sailing.server.RacingEventService;
import com.sap.sse.datamining.DataMiningServer;
import com.sap.sse.datamining.Query;
import com.sap.sse.datamining.StatisticQueryDefinition;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.data.QueryResult;
import com.sap.sse.datamining.factories.DataMiningDTOFactory;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.components.DataRetrieverTypeWithInformation;
import com.sap.sse.datamining.shared.DataMiningSession;
import com.sap.sse.datamining.shared.SSEDataMiningSerializationDummy;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.shared.impl.dto.LocalizedTypeDTO;
import com.sap.sse.datamining.shared.impl.dto.QueryResultDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class DataMiningServiceImpl extends RemoteServiceServlet implements DataMiningService {
    private static final long serialVersionUID = -7951930891674894528L;

    private final BundleContext context;
    
    private final ServiceTracker<DataMiningServer, DataMiningServer> dataMiningServerTracker;

    private final DataMiningDTOFactory dtoFactory;
    
    public DataMiningServiceImpl() {
        context = Activator.getDefault();
        dataMiningServerTracker = createAndOpenDataMiningServerTracker(context);
        dtoFactory = new DataMiningDTOFactory();
    }

    private ServiceTracker<DataMiningServer, DataMiningServer> createAndOpenDataMiningServerTracker(
            BundleContext context) {
        ServiceTracker<DataMiningServer, DataMiningServer> result = new ServiceTracker<DataMiningServer, DataMiningServer>(
                context, DataMiningServer.class.getName(), null);
        result.open();
        return result;
    }
    
    private DataMiningServer getDataMiningServer() {
        return dataMiningServerTracker.getService();
    }
    
    @Override
    public Date getComponentsChangedTimepoint() {
        return getDataMiningServer().getComponentsChangedTimepoint();
    }
    
    @Override
    public Iterable<FunctionDTO> getAllStatistics(String localeInfoName) {
        Iterable<Function<?>> statistics = getDataMiningServer().getAllStatistics();
        return functionsAsFunctionDTOs(statistics, localeInfoName);
    }
    
    @Override
    public Iterable<FunctionDTO> getStatisticsFor(DataRetrieverChainDefinitionDTO retrieverChainDefinition, String localeInfoName) {
        Class<?> retrievedDataType = getDataMiningServer().getDataRetrieverChainDefinition(retrieverChainDefinition.getId()).getRetrievedDataType();
        Iterable<Function<?>> statistics = getDataMiningServer().getStatisticsFor(retrievedDataType);
        return functionsAsFunctionDTOs(statistics, localeInfoName);
    }
    
    @Override
    public Iterable<AggregationProcessorDefinitionDTO> getAggregatorDefinitionsFor(FunctionDTO extractionFunction, String localeInfoName) {
        Class<?> returnType = getReturnType(extractionFunction);
        @SuppressWarnings("unchecked")
        Iterable<AggregationProcessorDefinition<?, ?>> definitions = 
                (Iterable<AggregationProcessorDefinition<?, ?>>)(Iterable<?>) getDataMiningServer().getAggregationProcessorDefinitions(returnType);
        return aggregatorDefinitionsAsDTOs(definitions, localeInfoName);
    }

    private Class<?> getReturnType(FunctionDTO extractionFunction) {
        return getDataMiningServer().getFunctionForDTO(extractionFunction).getReturnType();
    }

    private Iterable<AggregationProcessorDefinitionDTO> aggregatorDefinitionsAsDTOs(
            Iterable<AggregationProcessorDefinition<?, ?>> definitions, String localeInfoName) {
        ResourceBundleStringMessages stringMessages = getDataMiningServer().getStringMessages();
        Locale locale = ResourceBundleStringMessages.Util.getLocaleFor(localeInfoName);
        
        Collection<AggregationProcessorDefinitionDTO> definitionDTOs = new HashSet<>();
        for (AggregationProcessorDefinition<?,?> definition : definitions) {
            definitionDTOs.add(dtoFactory.createAggregationProcessorDefinitionDTO(definition, stringMessages, locale));
        }
        return definitionDTOs;
    }

    @Override
    public Iterable<FunctionDTO> getDimensionsFor(FunctionDTO statisticToCalculate, String localeInfoName) {
        Class<?> baseDataType = getBaseDataType(statisticToCalculate);
        Iterable<Function<?>> dimensions = getDataMiningServer().getDimensionsFor(baseDataType);
        return functionsAsFunctionDTOs(dimensions, localeInfoName);
    }
    
    @Override
    public Iterable<FunctionDTO> getDimensionsFor(DataRetrieverChainDefinitionDTO dataRetrieverChainDefinitionDTO, String localeInfoName) {
        DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition = getDataMiningServer().getDataRetrieverChainDefinition(dataRetrieverChainDefinitionDTO.getId());
        Iterable<Function<?>> dimensions = getDataMiningServer().getDimensionsFor(dataRetrieverChainDefinition);
        return functionsAsFunctionDTOs(dimensions, localeInfoName);
    }

    private Class<?> getBaseDataType(FunctionDTO statisticToCalculate) {
        Function<?> function = getDataMiningServer().getFunctionForDTO(statisticToCalculate);
        return function.getDeclaringType();
    }
    
    private Collection<FunctionDTO> functionsAsFunctionDTOs(Iterable<Function<?>> functions, String localeInfoName) {
        Locale locale = ResourceBundleStringMessages.Util.getLocaleFor(localeInfoName);
        ResourceBundleStringMessages ServerStringMessages = getDataMiningServer().getStringMessages();
        
        Collection<FunctionDTO> functionDTOs = new ArrayList<FunctionDTO>();
        for (Function<?> function : functions) {
            functionDTOs.add(dtoFactory.createFunctionDTO(function, ServerStringMessages, locale));
        }
        return functionDTOs;
    }
    
    @Override
    public Iterable<DataRetrieverChainDefinitionDTO> getDataRetrieverChainDefinitions(String localeInfoName) {
        Iterable<DataRetrieverChainDefinition<?, ?>> dataRetrieverChainDefinitions = getDataMiningServer().getDataRetrieverChainDefinitions();
        return dataRetrieverChainDefinitionsAsDTOs(dataRetrieverChainDefinitions, localeInfoName);
    }
    
    @Override
    public Iterable<DataRetrieverChainDefinitionDTO> getDataRetrieverChainDefinitionsFor(FunctionDTO statisticToCalculate, String localeInfoName) {
        Class<?> baseDataType = getBaseDataType(statisticToCalculate);
        @SuppressWarnings("unchecked")
        Iterable<DataRetrieverChainDefinition<?, ?>> dataRetrieverChainDefinitions = (Iterable<DataRetrieverChainDefinition<?, ?>>)(Iterable<?>)  getDataMiningServer().getDataRetrieverChainDefinitionsByDataType(baseDataType);
        return dataRetrieverChainDefinitionsAsDTOs(dataRetrieverChainDefinitions, localeInfoName);
    }
    
    private Collection<DataRetrieverChainDefinitionDTO> dataRetrieverChainDefinitionsAsDTOs(
            Iterable<DataRetrieverChainDefinition<?, ?>> dataRetrieverChainDefinitions, String localeInfoName) {
        Locale locale = ResourceBundleStringMessages.Util.getLocaleFor(localeInfoName);
        ResourceBundleStringMessages serverStringMessages = getDataMiningServer().getStringMessages();
        
        Collection<DataRetrieverChainDefinitionDTO> DTOs = new ArrayList<>();
        for (DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition : dataRetrieverChainDefinitions) {
            Collection<LocalizedTypeDTO> retrievedDataTypesChain = new ArrayList<>();
            for (DataRetrieverTypeWithInformation<?, ?> retrieverTypeWithInformation : dataRetrieverChainDefinition.getDataRetrieverTypesWithInformation()) {
                String typeName = retrieverTypeWithInformation.getRetrievedDataType().getSimpleName();
                String displayName = retrieverTypeWithInformation.getRetrievedDataTypeMessageKey() != null && !retrieverTypeWithInformation.getRetrievedDataTypeMessageKey().isEmpty() ?
                                        serverStringMessages.get(locale, retrieverTypeWithInformation.getRetrievedDataTypeMessageKey()) : 
                                        typeName;
                LocalizedTypeDTO localizedRetrievedDataType = new LocalizedTypeDTO(typeName, displayName);
                retrievedDataTypesChain.add(localizedRetrievedDataType);
            }
            DTOs.add(new DataRetrieverChainDefinitionDTO(dataRetrieverChainDefinition.getID(), dataRetrieverChainDefinition.getLocalizedName(locale, serverStringMessages),
                                                         dataRetrieverChainDefinition.getDataSourceType().getSimpleName(), retrievedDataTypesChain));
        }
        return DTOs;
    }

    @Override
    public QueryResultDTO<Set<Object>> getDimensionValuesFor(DataMiningSession session, DataRetrieverChainDefinitionDTO dataRetrieverChainDefinitionDTO,
            int retrieverLevel, Iterable<FunctionDTO> dimensionDTOs, Map<Integer, Map<FunctionDTO, Collection<?>>> filterSelectionDTO, String localeInfoName) {
        DataMiningServer dataMiningServer = getDataMiningServer();
        DataRetrieverChainDefinition<RacingEventService, ?> retrieverChainDefinition = dataMiningServer.getDataRetrieverChainDefinition(dataRetrieverChainDefinitionDTO.getId());
        Iterable<Function<?>> dimensions = functionDTOsAsFunctions(dimensionDTOs);
        Map<Integer, Map<Function<?>, Collection<?>>> filterSelection = filterSelectionDTOAsFilterSelection(filterSelectionDTO);
        Locale locale = ResourceBundleStringMessages.Util.getLocaleFor(localeInfoName);
        Query<Set<Object>> dimensionValuesQuery = dataMiningServer.createDimensionValuesQuery(retrieverChainDefinition, retrieverLevel, dimensions, filterSelection, locale);
        QueryResult<Set<Object>> result = dataMiningServer.runNewQueryAndAbortPreviousQueries(session, dimensionValuesQuery);
        return dataMiningServer.convertToDTO(result);
    }

    private Collection<Function<?>> functionDTOsAsFunctions(Iterable<FunctionDTO> functionDTOs) {
        List<Function<?>> functions = new ArrayList<>();
        DataMiningServer dataMiningServer = getDataMiningServer();
        for (FunctionDTO functionDTO : functionDTOs) {
            Function<?> function = dataMiningServer.getFunctionForDTO(functionDTO);
            if (function != null) {
                functions.add(function);
            }
        }
        return functions;
    }

    private Map<Integer, Map<Function<?>, Collection<?>>> filterSelectionDTOAsFilterSelection(
            Map<Integer, Map<FunctionDTO, Collection<?>>> filterSelectionDTO) {
        Map<Integer, Map<Function<?>, Collection<?>>> filterSelection = new HashMap<>();
        for (Integer retrieverLevel : filterSelectionDTO.keySet()) {
            Map<FunctionDTO, Collection<?>> retrievalLevelSelection = filterSelectionDTO.get(retrieverLevel);
            for (FunctionDTO dimensionDTO : retrievalLevelSelection.keySet()) {
                if (!retrievalLevelSelection.get(dimensionDTO).isEmpty()) {
                    Function<?> function = getDataMiningServer().getFunctionForDTO(dimensionDTO);
                    if (function != null) {
                        if (!filterSelection.containsKey(retrieverLevel)) {
                            filterSelection.put(retrieverLevel, new HashMap<Function<?>, Collection<?>>());
                        }
                        filterSelection.get(retrieverLevel).put(function, retrievalLevelSelection.get(dimensionDTO));
                    }
                }
            }
        }
        return filterSelection;
    }

    @Override
    public <ResultType> QueryResultDTO<ResultType> runQuery(DataMiningSession session, StatisticQueryDefinitionDTO queryDefinitionDTO) {
        DataMiningServer dataMiningServer = getDataMiningServer();
        StatisticQueryDefinition<RacingEventService, ?, ?, ResultType> queryDefinition = dataMiningServer.getQueryDefinitionForDTO(queryDefinitionDTO);
        Query<ResultType> query = dataMiningServer.createQuery(queryDefinition);
        QueryResult<ResultType> result = dataMiningServer.runNewQueryAndAbortPreviousQueries(session, query);
        return dataMiningServer.convertToDTO(result);
    }
    
    @Override
    public SSEDataMiningSerializationDummy pseudoMethodSoThatSomeSSEDataMiningClassesAreAddedToTheGWTSerializationPolicy() {
        return null;
    }
    
    @Override
    public SailingDataMiningSerializationDummy pseudoMethodSoThatSomeSailingDataMiningClassesAreAddedToTheGWTSerializationPolicy() {
        return null;
    }

}
