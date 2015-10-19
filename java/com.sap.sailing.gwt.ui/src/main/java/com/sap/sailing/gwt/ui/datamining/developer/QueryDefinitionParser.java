package com.sap.sailing.gwt.ui.datamining.developer;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.google.gwt.safehtml.shared.OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml;
import com.google.gwt.safehtml.shared.SafeHtml;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.shared.impl.dto.LocalizedTypeDTO;
import com.sap.sse.datamining.shared.impl.dto.ModifiableStatisticQueryDefinitionDTO;

public class QueryDefinitionParser {
    
    public SafeHtml parseToDetailsAsSafeHtml(StatisticQueryDefinitionDTO queryDefinition) {
        String safeHtml = parseToDetails(queryDefinition, new HtmlBuilder());
        return new OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(safeHtml);
    }
    
    public String parseToDetailsAsText(StatisticQueryDefinitionDTO queryDefinition) {
        return parseToDetails(queryDefinition, new TextBuilder());
    }
    
    private String parseToDetails(StatisticQueryDefinitionDTO queryDefinition, Builder builder) {
        builder.appendText("Locale: " + queryDefinition.getLocaleInfoName()).appendLineBreak();
        
        DataRetrieverChainDefinitionDTO chainDefinition = queryDefinition.getDataRetrieverChainDefinition();
        builder.appendLineBreak()
               .appendText("Retrieval: " + chainDefinition).appendLineBreak()
               .appendTab(1).appendText("Levels: ").appendLineBreak();
        for (int levelIndex = 0; levelIndex < chainDefinition.getLevelAmount(); levelIndex++) {
            builder.appendTab(2).appendText(levelIndex + ": " + chainDefinition.getRetrieverLevel(levelIndex)).appendLineBreak();
        }
         
        HashMap<DataRetrieverLevelDTO, SerializableSettings> retrieverSettings = queryDefinition.getRetrieverSettings();
        if (!retrieverSettings.isEmpty()) {
            // TODO Display retriever settings
        }
        
        HashMap<DataRetrieverLevelDTO,HashMap<FunctionDTO,HashSet<? extends Serializable>>> filterSelection = queryDefinition.getFilterSelection();
        if (!filterSelection.isEmpty()) {
            builder.appendLineBreak().appendText("Filter Selection:").appendLineBreak();
            List<DataRetrieverLevelDTO> retrieverLevels = new ArrayList<>(filterSelection.keySet());
            Collections.sort(retrieverLevels);
            for (DataRetrieverLevelDTO retrieveLevel : retrieverLevels) {
                builder.appendTab(1).appendText("Level " + retrieveLevel.getLevel() + ": ").appendLineBreak();
                HashMap<FunctionDTO, HashSet<? extends Serializable>> levelFilterSelection = filterSelection.get(retrieveLevel);
                for (FunctionDTO dimension : levelFilterSelection.keySet()) {
                    builder.appendTab(2).appendText(dimension.toString()).appendLineBreak()
                           .appendTab(3).appendText("Values: ");
                    boolean first = true;
                    for (Serializable value : levelFilterSelection.get(dimension)) {
                        if (!first) {
                            builder.appendText(", ");
                        }
                        builder.appendText(value.toString());
                        first = false;
                    }
                    builder.appendLineBreak();
                }
            }
        }

        builder.appendLineBreak()
               .appendText("Group By:").appendLineBreak();
        ArrayList<FunctionDTO> dimensionsToGroupBy = queryDefinition.getDimensionsToGroupBy();
        for (int index = 0; index < dimensionsToGroupBy.size(); index++) {
            builder.appendTab(1).appendText(index + ": " + dimensionsToGroupBy.get(index)).appendLineBreak();
        }
        
        builder.appendLineBreak()
               .appendText("Statistic: " + queryDefinition.getStatisticToCalculate()).appendLineBreak();
        builder.appendText("Aggregator: " + queryDefinition.getAggregatorDefinition());
        
        return builder.toString();
    }

    public SafeHtml parseToCodeAsSafeHtml(StatisticQueryDefinitionDTO queryDefinition) {
        String safeHtml = parseToCode(queryDefinition, new HtmlBuilder());
        return new OnlyToBeUsedInGeneratedCodeStringBlessedAsSafeHtml(safeHtml);
    }
    
    public String parseToCodeAsText(StatisticQueryDefinitionDTO queryDefinition) {
        return parseToCode(queryDefinition, new TextBuilder());
    }
    
    private String parseToCode(StatisticQueryDefinitionDTO queryDefinition, Builder builder) {
        String functionClassName = FunctionDTO.class.getSimpleName();
        String aggregatorClassName = AggregationProcessorDefinitionDTO.class.getSimpleName();
        String retrieverLevelClassName = DataRetrieverLevelDTO.class.getSimpleName();
        String arrayListClassName = ArrayList.class.getSimpleName();
        String localizedTypeClassName = LocalizedTypeDTO.class.getSimpleName();
        String retrieverChainClassName = DataRetrieverChainDefinitionDTO.class.getSimpleName();
        String queryDefinitionClassName = ModifiableStatisticQueryDefinitionDTO.class.getSimpleName();
        String hashMapClassName = HashMap.class.getSimpleName();
        String hashSetClassName = HashSet.class.getSimpleName();
        String serializableClassName = Serializable.class.getSimpleName();
        
        DataRetrieverChainDefinitionDTO retrieverChain = queryDefinition.getDataRetrieverChainDefinition();

        // Extraction Function
        String statisticVariable = "statistic";
        builder.appendText(functionToCode(statisticVariable, queryDefinition.getStatisticToCalculate())).appendLineBreak();

        // Aggregator Definition
        String aggregatorVariable = "aggregator";
        AggregationProcessorDefinitionDTO aggregator = queryDefinition.getAggregatorDefinition();
        builder.appendText(aggregatorClassName + " " + aggregatorVariable + " = new " + aggregatorClassName + "(" + literal(aggregator.getMessageKey()) + ", " +
                           literal(aggregator.getExtractedTypeName()) + ", " + literal(aggregator.getAggregatedTypeName()) + ", \"\");").appendLineBreak()
               .appendLineBreak();
        
        // Retriever Levels and Retriever Chain Definition
        String retrieverLevelsVariable = "retrieverLevels";
        builder.appendText(arrayListClassName + "<" + retrieverLevelClassName + "> " + retrieverLevelsVariable + " = new " + arrayListClassName + "<>();").appendLineBreak();
        for (DataRetrieverLevelDTO retrieverLevel : retrieverChain.getRetrieverLevels()) {
            LocalizedTypeDTO retrievedType = retrieverLevel.getRetrievedDataType();
            String retrievedTypeAsCode = "new " + localizedTypeClassName + "(" + literal(retrievedType.getTypeName()) + ", " + literal(retrievedType.getDisplayName()) + ")";
            String retrieverSettingsAsCode = "null";
            // TODO Handle the retriever level settings
            builder.appendText(retrieverLevelsVariable + ".add(new " + retrieverLevelClassName + "(" + retrieverLevel.getLevel() + ", " +
                               literal(retrieverLevel.getRetrieverTypeName()) + ", " + retrievedTypeAsCode + ", " + retrieverSettingsAsCode + "));").appendLineBreak();
        }
        String retrieverChainVariable = "retrieverChain";
        builder.appendText(retrieverChainClassName + " " + retrieverChainVariable + " = new " + retrieverChainClassName + "(\"\", " + 
                           literal(retrieverChain.getDataSourceTypeName()) + ", " + retrieverLevelsVariable + ");").appendLineBreak()
               .appendLineBreak();
        
        // Query Definition instantiation
        String queryDefinitionVariable = "queryDefinition";
        builder.appendText(queryDefinitionClassName + " " + queryDefinitionVariable + " = new " + queryDefinitionClassName + "(" + literal(queryDefinition.getLocaleInfoName()) + 
                           ", " + statisticVariable + ", " + aggregatorVariable + ", " + retrieverChainVariable + ");").appendLineBreak()
               .appendLineBreak();
        
        // Filter Selection per Retriever Level separated by two line breaks
        for (DataRetrieverLevelDTO retrieverLevel : retrieverChain.getRetrieverLevels()) {
            HashMap<FunctionDTO, HashSet<? extends Serializable>> levelFilterSelection = queryDefinition.getFilterSelection().get(retrieverLevel);
            if (levelFilterSelection != null && !levelFilterSelection.isEmpty()) {
                String levelFilterSelectionVariable = "retrieverlevel" + retrieverLevel.getLevel() + "_FilterSelection";
                builder.appendText(hashMapClassName + "<" + functionClassName + ", " + hashSetClassName + "<? extends " + serializableClassName +
                                   ">> " + levelFilterSelectionVariable + " = new " + hashMapClassName + "<>();").appendLineBreak();
                
                int filterDimensionCounter = 0;
                for (FunctionDTO filterDimension : levelFilterSelection.keySet()) {
                    String filterDimensionVariable = "filterDimension" + filterDimensionCounter;
                    builder.appendText(functionToCode(filterDimensionVariable, filterDimension)).appendLineBreak();
                    
                    String dimensionFilterSelectionVariable = filterDimensionVariable + "_Selection";
                    builder.appendText(hashSetClassName + "<" + serializableClassName + "> " + dimensionFilterSelectionVariable +
                                       " = new " + hashSetClassName + "<>();").appendLineBreak();
                    for (Serializable filterValue : levelFilterSelection.get(filterDimension)) {
                        builder.appendText(dimensionFilterSelectionVariable + ".add(" + literal(filterValue) + ");").appendLineBreak();
                    }
                    
                    builder.appendText(levelFilterSelectionVariable + ".put(" + filterDimensionVariable + ", " + dimensionFilterSelectionVariable + ");").appendLineBreak();
                    filterDimensionCounter++;
                }
                
                builder.appendText(queryDefinitionVariable + ".setFilterSelectionFor(" + retrieverChainVariable + ".getRetrieverLevel(" +
                                   retrieverLevel.getLevel() + "), " + levelFilterSelectionVariable + ");").appendLineBreak()
                       .appendLineBreak();
            }
            
        }

        // Dimensions to Group By separated by two line breaks
        int dimensionToGroupByCounter = 0;
        boolean first = true;
        for (FunctionDTO dimensionToGroupBy : queryDefinition.getDimensionsToGroupBy()) {
            if (!first) {
                builder.appendLineBreak().appendLineBreak();
            }
            
            String dimensionToGroupByVariable = "dimensionToGroupBy" + dimensionToGroupByCounter;
            builder.appendText(functionToCode(dimensionToGroupByVariable, dimensionToGroupBy)).appendLineBreak();
            builder.appendText(queryDefinitionVariable + ".appendDimensionToGroupBy(" + dimensionToGroupByVariable + ");");
            
            first = false;
            dimensionToGroupByCounter++;
        }
        
        return builder.toString();
    }
    
    private String functionToCode(String functionVariable, FunctionDTO function) {
        String functionClassName = FunctionDTO.class.getSimpleName();
        return functionClassName + " " + functionVariable + " = new " + functionClassName + "(" + function.isDimension() + ", " + literal(function.getFunctionName()) + ", " +
               literal(function.getSourceTypeName()) + ", " + literal(function.getReturnTypeName()) + ", " + functionParametersToCode(function) + ", \"\", 0);";
    }
    
    private String functionParametersToCode(FunctionDTO function) {
        List<String> parameterTypeNames = function.getParameterTypeNames();
        if (parameterTypeNames.isEmpty()) {
            return "new " + ArrayList.class.getSimpleName() + "<" + String.class.getSimpleName() + ">()";
        }
        
        Arrays.asList("Test", "test");
        StringBuilder builder = new StringBuilder(Arrays.class.getSimpleName() + ".asList(");
        boolean first = true;
        for (String parameterTypeName : parameterTypeNames) {
            if (!first) {
                builder.append(", ");
            }
            builder.append("\"" + parameterTypeName + "\"");
            first = false;
        }
        builder.append(")");
        return builder.toString();
    }
    
    private static String literal(Serializable value) {
        if (value instanceof String) {
            return literal((String) value);
        }
        if (value instanceof Integer) {
            return value.toString();
        }
        throw new IllegalArgumentException("Can't create literal for values of type " + value.getClass().getName());
    }
    
    private static String literal(String value) {
        return "\"" + value + "\"";
    }

    private static final String HTML_LINE_BREAK = "<br />";
    private static final String HTML_TAB = "&emsp;";
    
    private static final String TEXT_LINE_BREAK = "\n";
    private static final String TEXT_TAB = "\t";
    
    private interface Builder {
        
        Builder appendText(String text);
        Builder appendLineBreak();
        Builder appendTab(int tabAmount);
        
        String toString();
        
    }
    
    private class HtmlBuilder implements Builder {
        
        private final SafeHtmlBuilder builder;
        public HtmlBuilder() {
            builder = new SafeHtmlBuilder();
        }

        @Override
        public Builder appendText(String text) {
            builder.appendEscaped(text);
            return this;
        }
        @Override
        public Builder appendLineBreak() {
            builder.appendHtmlConstant(HTML_LINE_BREAK);
            return this;
        }
        @Override
        public Builder appendTab(int tabAmount) {
            builder.appendHtmlConstant(TAB(HTML_TAB, tabAmount));
            return this;
        }
        
        @Override
        public String toString() {
            return builder.toSafeHtml().asString();
        }
        
    }
    
    private class TextBuilder implements Builder {
        
        private final StringBuilder builder;
        public TextBuilder() {
            builder = new StringBuilder();
        }

        @Override
        public Builder appendText(String text) {
            builder.append(text);
            return this;
        }
        @Override
        public Builder appendLineBreak() {
            builder.append(TEXT_LINE_BREAK);
            return this;
        }
        @Override
        public Builder appendTab(int tabAmount) {
            builder.append(TAB(TEXT_TAB, tabAmount));
            return this;
        }
        
        @Override
        public String toString() {
            return builder.toString();
        }
        
    }
    
    private static String TAB(String tabCharacter, int tabAmount) {
        StringBuilder tabBuilder = new StringBuilder();
        for (int i = 0; i < tabAmount; i++) {
            tabBuilder.append(tabCharacter);
        }
        return tabBuilder.toString();
    }

}
