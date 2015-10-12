package com.sap.sse.datamining.factories;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.components.AggregationProcessorDefinition;
import com.sap.sse.datamining.components.DataRetrieverChainDefinition;
import com.sap.sse.datamining.functions.Function;
import com.sap.sse.datamining.impl.components.DataRetrieverLevel;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.shared.impl.dto.LocalizedTypeDTO;
import com.sap.sse.i18n.ResourceBundleStringMessages;

public class DataMiningDTOFactory {

    /**
     * Creates the corresponding DTO for the given {@link Function}, without localization.<br>
     * The display name of the resulting DTO is the {@link Function#getSimpleName() simple name} of the given function.
     */
    public FunctionDTO createFunctionDTO(Function<?> function) {
        return createFunctionDTO(function, function.getSimpleName());
    }
    
    /**
     * Creates the corresponding DTO for the given function, with the retrieved string message for the given locale and the
     * contained message key as display name. The message key is provided with the {@link Dimension} or {@link Connector}
     * annotation.<br>
     * If the function has no message key, the function name is used as display name.
     */
    public FunctionDTO createFunctionDTO(Function<?> function, ResourceBundleStringMessages stringMessages, Locale locale) {
        return createFunctionDTO(function, function.getLocalizedName(locale, stringMessages));
    }
    
    private FunctionDTO createFunctionDTO(Function<?> function, String displayName) {
        String functionName = function.getSimpleName();
        String sourceTypeName = function.getDeclaringType().getName();
        String returnTypeName = function.getReturnType().getName();
        List<String> parameterTypeNames = getParameterTypeNames(function);
        return new FunctionDTO(function.isDimension(), functionName, sourceTypeName, returnTypeName, parameterTypeNames, displayName, function.getOrdinal());
    }

    private List<String> getParameterTypeNames(Function<?> function) {
        List<String> parameterTypeNames = new ArrayList<>();
        for (Class<?> parameterType : function.getParameters()) {
            parameterTypeNames.add(parameterType.getName());
        }
        return parameterTypeNames;
    }

    /**
     * Creates the corresponding DTO for the given {@link AggregationProcessorDefinition aggregator definition}, without localization.<br>
     * The display name of the resulting DTO is the message key of the given aggregation definition.
     */
    public AggregationProcessorDefinitionDTO createAggregationProcessorDefinitionDTO(AggregationProcessorDefinition<?, ?> aggregatorDefinition) {
        return createAggregationProcessorDefinitionDTO(aggregatorDefinition, aggregatorDefinition.getAggregationNameMessageKey());
    }


    /**
     * Creates the corresponding localized DTO for the given {@link AggregationProcessorDefinition aggregator definition}.
     */
    public AggregationProcessorDefinitionDTO createAggregationProcessorDefinitionDTO(AggregationProcessorDefinition<?, ?> aggregatorDefinition,
                                                                                     ResourceBundleStringMessages stringMessages, Locale locale) {
        return createAggregationProcessorDefinitionDTO(aggregatorDefinition, stringMessages.get(locale, aggregatorDefinition.getAggregationNameMessageKey()));
    }

    private AggregationProcessorDefinitionDTO createAggregationProcessorDefinitionDTO(AggregationProcessorDefinition<?, ?> aggregatorDefinition, String displayName) {
        return new AggregationProcessorDefinitionDTO(aggregatorDefinition.getAggregationNameMessageKey(),
                                                     aggregatorDefinition.getExtractedType().getName(),
                                                     aggregatorDefinition.getAggregatedType().getName(),
                                                     displayName);
    }

    public DataRetrieverChainDefinitionDTO createDataRetrieverChainDefinitionDTO(DataRetrieverChainDefinition<?, ?> dataRetrieverChainDefinition,
                                                                                 ResourceBundleStringMessages stringMessages, Locale locale) {
        ArrayList<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<>();
        for (DataRetrieverLevel<?, ?> retrieverLevel : dataRetrieverChainDefinition.getDataRetrieverLevels()) {
            retrieverLevels.add(createDataRetrieverLevelDTO(retrieverLevel, stringMessages, locale));
        }
        return new DataRetrieverChainDefinitionDTO(dataRetrieverChainDefinition.getID(),
                dataRetrieverChainDefinition.getLocalizedName(locale, stringMessages), dataRetrieverChainDefinition
                        .getDataSourceType().getName(), retrieverLevels);
   }

    public DataRetrieverLevelDTO createDataRetrieverLevelDTO(DataRetrieverLevel<?, ?> retrieverLevel,
                                                             ResourceBundleStringMessages stringMessages, Locale locale) {
        String typeName = retrieverLevel.getRetrievedDataType().getName();
        String displayName = retrieverLevel.getRetrievedDataTypeMessageKey() != null
                && !retrieverLevel.getRetrievedDataTypeMessageKey().isEmpty() ? stringMessages
                .get(locale, retrieverLevel.getRetrievedDataTypeMessageKey()) : typeName;
        LocalizedTypeDTO localizedRetrievedDataType = new LocalizedTypeDTO(typeName, displayName);
        return new DataRetrieverLevelDTO(retrieverLevel.getLevel(),
                                         retrieverLevel.getRetrieverType().getName(),
                                         localizedRetrievedDataType, retrieverLevel.getDefaultSettings());
    }

}
