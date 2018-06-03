package com.sap.sailing.gwt.ui.datamining.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.common.client.suggestion.AbstractListSuggestOracle;
import com.sap.sailing.gwt.common.client.suggestion.CustomSuggestBox;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.AbstractObjectRenderer;
import com.sap.sailing.gwt.ui.datamining.AggregatorDefinitionChangedListener;
import com.sap.sailing.gwt.ui.datamining.DataMiningServiceAsync;
import com.sap.sailing.gwt.ui.datamining.DataMiningSettingsControl;
import com.sap.sailing.gwt.ui.datamining.DataMiningSettingsInfo;
import com.sap.sailing.gwt.ui.datamining.DataMiningSettingsInfoManager;
import com.sap.sailing.gwt.ui.datamining.DataRetrieverChainDefinitionChangedListener;
import com.sap.sailing.gwt.ui.datamining.ExtractionFunctionChangedListener;
import com.sap.sailing.gwt.ui.datamining.StatisticChangedListener;
import com.sap.sailing.gwt.ui.datamining.StatisticProvider;
import com.sap.sse.common.Util;
import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.shared.components.AbstractComponent;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.CompositeSettings;
import com.sap.sse.gwt.client.shared.components.CompositeTabbedSettingsDialogComponent;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;

/**
 * A {@link StatisticProvider} that contains all statistics registered in the server.
 * Each statistic is paired with the corresponding {@link DataRetrieverChainDefinitionDTO}.
 * This provides a more comfortable access to the available statistics, without the need to
 * specify the domain to analyze.
 * 
 * @author Lennart Hensler
 */
public class SuggestBoxStatisticProvider extends AbstractComponent<CompositeSettings> implements StatisticProvider {
    
    private static final String STATISTIC_PROVIDER_ELEMENT_STYLE = "statisticProviderElement";

    private final StringMessages stringMessages;
    private final DataMiningServiceAsync dataMiningService;
    private final ErrorReporter errorReporter;
    private final Set<DataRetrieverChainDefinitionChangedListener> retrieverChainListeners;
    private final Set<ExtractionFunctionChangedListener> extractionFunctionListeners;
    private final Set<AggregatorDefinitionChangedListener> aggregatorDefinitionListeners;
    private boolean isAwaitingReload;
    private int awaitingRetrieverChainStatistics;
    
    private final DataMiningSettingsInfoManager settingsManager;
    private final DataMiningSettingsControl settingsControl;
    private final Map<DataRetrieverChainDefinitionDTO, HashMap<DataRetrieverLevelDTO, SerializableSettings>> settingsMap;
    private final List<Component<?>> retrieverLevelSettingsComponents;
    
    private final FlowPanel mainPanel;
    private final List<ExtractionFunctionWithContext> availableExtractionFunctions;
    private final ExtractionFunctionSuggestBox extractionFunctionSuggestBox;
    private final ValueListBox<AggregationProcessorDefinitionDTO> aggregatorListBox;

    public SuggestBoxStatisticProvider(Component<?> parent, ComponentContext<?> componentContext,
            StringMessages stringMessages, DataMiningServiceAsync dataMiningService,
            ErrorReporter errorReporter, DataMiningSettingsControl settingsControl) {
        super(parent, componentContext);
        this.stringMessages = stringMessages;
        this.dataMiningService = dataMiningService;
        this.errorReporter = errorReporter;
        retrieverChainListeners = new HashSet<>();
        extractionFunctionListeners = new HashSet<>();
        aggregatorDefinitionListeners = new HashSet<>();
        isAwaitingReload = false;
        
        settingsManager = new DataMiningSettingsInfoManager();
        this.settingsControl = settingsControl;
        this.settingsControl.addSettingsComponent(this);
        settingsMap = new HashMap<>();
        retrieverLevelSettingsComponents = new ArrayList<>();
        
        mainPanel = new FlowPanel();
        Label label = new Label(this.stringMessages.calculateThe());
        label.addStyleName(STATISTIC_PROVIDER_ELEMENT_STYLE);
        mainPanel.add(label);
        
        availableExtractionFunctions = new ArrayList<>();
        extractionFunctionSuggestBox = new ExtractionFunctionSuggestBox() {
            @Override
            protected void onValueChange() {
                notifyRetrieverChainListeners();
                notifyExtractionFunctionListeners();
                updateAggregators();
            }
        };
        extractionFunctionSuggestBox.getValueBox().addFocusHandler(e -> {
            extractionFunctionSuggestBox.getValueBox().selectAll();
            extractionFunctionSuggestBox.showSuggestionList();
        });
        extractionFunctionSuggestBox.setLimit(Integer.MAX_VALUE);
        extractionFunctionSuggestBox.addStyleName(STATISTIC_PROVIDER_ELEMENT_STYLE);
        extractionFunctionSuggestBox.setWidth("540px");
        mainPanel.add(extractionFunctionSuggestBox);
        
        aggregatorListBox = createAggregatorListBox();
        aggregatorListBox.addStyleName(STATISTIC_PROVIDER_ELEMENT_STYLE);
        mainPanel.add(aggregatorListBox);
    }

    private ValueListBox<AggregationProcessorDefinitionDTO> createAggregatorListBox() {
        ValueListBox<AggregationProcessorDefinitionDTO> aggregatorListBox = new ValueListBox<AggregationProcessorDefinitionDTO>(new AbstractObjectRenderer<AggregationProcessorDefinitionDTO>() {
            @Override
            protected String convertObjectToString(AggregationProcessorDefinitionDTO nonNullObject) {
                return nonNullObject.getDisplayName();
            }
        });
        aggregatorListBox.addValueChangeHandler(new ValueChangeHandler<AggregationProcessorDefinitionDTO>() {
            @Override
            public void onValueChange(ValueChangeEvent<AggregationProcessorDefinitionDTO> event) {
                notifyAggregatorDefinitionListeners();
            }
        });
        return aggregatorListBox;
    }

    @Override
    public void awaitReloadComponents() {
        isAwaitingReload = true;
    }
    
    @Override
    public boolean isAwaitingReload() {
        return isAwaitingReload;
    }
    
    @Override
    public void reloadComponents() {
        isAwaitingReload = false;
        updateContent();
    }
    
    private void updateContent() {
        final String localeName = LocaleInfo.getCurrentLocale().getLocaleName();
        dataMiningService.getDataRetrieverChainDefinitions(localeName,
            new AsyncCallback<ArrayList<DataRetrieverChainDefinitionDTO>>() {
                @Override
                public void onSuccess(ArrayList<DataRetrieverChainDefinitionDTO> dataRetrieverChainDefinitions) {
                    settingsMap.clear();
                    awaitingRetrieverChainStatistics = dataRetrieverChainDefinitions.size();
                    availableExtractionFunctions.clear();
                    if (awaitingRetrieverChainStatistics == 0) {
                        extractionFunctionSuggestBox.setSelectableValues(Collections.emptyList());
                    } else {
                        for (DataRetrieverChainDefinitionDTO retrieverChain : dataRetrieverChainDefinitions) {
                            if (retrieverChain.hasSettings()) {
                                settingsMap.put(retrieverChain, retrieverChain.getDefaultSettings());
                            }
                            dataMiningService.getStatisticsFor(retrieverChain, localeName, new AsyncCallback<HashSet<FunctionDTO>>() {
                                @Override
                                public void onSuccess(HashSet<FunctionDTO> statistics) {
                                    collectStatistics(retrieverChain, statistics);
                                }
                                @Override
                                public void onFailure(Throwable caught) {
                                    errorReporter.reportError("Error fetching the statistics for the retriever chain '" +
                                                              retrieverChain + "': " + caught.getMessage());
                                    collectStatistics(retrieverChain, Collections.emptySet());
                                }
                            });
                        }
                    }
                }
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error fetching the retriever chain definitions: " + caught.getMessage());
                }
            }
        );
    }
    
    private void collectStatistics(DataRetrieverChainDefinitionDTO retrieverChain, Iterable<FunctionDTO> extractionFunctions) {
        for (FunctionDTO extractionFunction : extractionFunctions) {
            availableExtractionFunctions.add(new ExtractionFunctionWithContext(retrieverChain, extractionFunction));
        }
        
        awaitingRetrieverChainStatistics--;
        if (awaitingRetrieverChainStatistics == 0) {
            Collections.sort(availableExtractionFunctions);
            Map<String, ExtractionFunctionWithContext> displayDuplicates = new HashMap<>();
            for (ExtractionFunctionWithContext statistic : availableExtractionFunctions) {
                ExtractionFunctionWithContext duplicate = displayDuplicates.get(statistic.getExtractionFunction().getDisplayName());
                if (duplicate == null) {
                    displayDuplicates.put(statistic.getExtractionFunction().getDisplayName(), statistic);
                } else {
                    statistic.setVerbose(true);
                    duplicate.setVerbose(true);
                }
            }
            extractionFunctionSuggestBox.setSelectableValues(availableExtractionFunctions);
            
            // TODO Do not pre-select the first element. The other UI components have to able to handle "empty content"
            ExtractionFunctionWithContext currentValue = extractionFunctionSuggestBox.getExtractionFunction();
            ExtractionFunctionWithContext valueToBeSelected = availableExtractionFunctions.contains(currentValue) ? currentValue: Util.first(availableExtractionFunctions);
            extractionFunctionSuggestBox.getValueBox().setValue(valueToBeSelected.getDisplayString(), false);
            extractionFunctionSuggestBox.setExtractionFunction(valueToBeSelected);
        }
    }
    
    private void updateAggregators() {
        FunctionDTO extractionFunction = getExtractionFunction();
        if (extractionFunction == null) {
            updateListBox(aggregatorListBox, Collections.emptyList());
        } else {
            dataMiningService.getAggregatorDefinitionsFor(extractionFunction, LocaleInfo.getCurrentLocale().getLocaleName(),
                new AsyncCallback<HashSet<AggregationProcessorDefinitionDTO>>() {
                    @Override
                    public void onSuccess(HashSet<AggregationProcessorDefinitionDTO> aggregators) {
                        List<AggregationProcessorDefinitionDTO> aggregatorsList = new ArrayList<>(aggregators);
                        Collections.sort(aggregatorsList);
                        updateListBox(aggregatorListBox, aggregatorsList);
                    }
                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error fetching the aggregators for the extraction function'" +
                                                  extractionFunction + "': " + caught.getMessage());
                    }
                }
            );
        }
    }
    
    private <T> void updateListBox(ValueListBox<T> listBox, Collection<T> acceptableValues) {
        T currentValue = listBox.getValue();
        T valueToBeSelected = acceptableValues.contains(currentValue) ? currentValue : Util.first(acceptableValues);
        
        listBox.setValue(valueToBeSelected, true);
        listBox.setAcceptableValues(acceptableValues);
    }

    @Override
    public HashMap<DataRetrieverLevelDTO, SerializableSettings> getRetrieverSettings() {
        if (settingsMap.containsKey(getDataRetrieverChainDefinition())) {
            return settingsMap.get(getDataRetrieverChainDefinition());
        } else {
            return new HashMap<>();
        }
    }

    @Override
    public boolean hasSettings() {
        return getDataRetrieverChainDefinition().hasSettings();
    }

    @Override
    public SettingsDialogComponent<CompositeSettings> getSettingsDialogComponent(CompositeSettings settings) {
        retrieverLevelSettingsComponents.clear();
        retrieverLevelSettingsComponents.addAll(createSettingsComponentsFor(getDataRetrieverChainDefinition()));
        return new CompositeTabbedSettingsDialogComponent(retrieverLevelSettingsComponents);
    }

    private List<Component<?>> createSettingsComponentsFor(final DataRetrieverChainDefinitionDTO retrieverChain) {
        List<Component<?>> settingsComponents = new ArrayList<>();
        for (Entry<DataRetrieverLevelDTO, SerializableSettings> retrieverLevelSettings : settingsMap.get(retrieverChain).entrySet()) {
            final DataRetrieverLevelDTO retrieverLevel = retrieverLevelSettings.getKey();
            final Class<?> settingsType = retrieverLevelSettings.getValue().getClass();
            DataMiningSettingsInfo settingsInfo = settingsManager.getSettingsInfo(settingsType);
            settingsComponents.add(new RetrieverLevelSettingsComponent(this, getComponentContext(),
                    retrieverLevel, settingsInfo.getId(), settingsInfo.getLocalizedName(stringMessages)) {
                @Override
                public SettingsDialogComponent<SerializableSettings> getSettingsDialogComponent(SerializableSettings settings) {
                    return settingsManager.getSettingsInfo(settingsType).createSettingsDialogComponent(settingsMap.get(retrieverChain).get(retrieverLevel));
                }
                @Override
                public void updateSettings(SerializableSettings newSettings) {
                    settingsMap.get(retrieverChain).put(retrieverLevel, newSettings);
                }
            });
        }
        return settingsComponents;
    }

    @Override
    public void updateSettings(CompositeSettings newSettings) {
        Map<DataRetrieverLevelDTO, SerializableSettings> chainSettings = settingsMap.get(getDataRetrieverChainDefinition());
        for (Entry<String, Settings> settingsPerComponent : newSettings.getSettingsPerComponentId().entrySet()) {
            RetrieverLevelSettingsComponent component = (RetrieverLevelSettingsComponent) findComponentById(settingsPerComponent.getKey());
            SerializableSettings settings = (SerializableSettings) settingsPerComponent.getValue();
            chainSettings.put(component.getRetrieverLevel(), settings);
        }
        notifyRetrieverChainListeners();
    }

    private Component<?> findComponentById(String componentId) {
        for (Component<?> component : retrieverLevelSettingsComponents) {
            if (component.getId().equals(componentId)) {
                return component;
            }
        }
        return null;
    }
    
    @Override 
    public CompositeSettings getSettings() {
        Map<String, Settings> settings = new HashMap<>();
        for (Entry<DataRetrieverLevelDTO, SerializableSettings> retrieverLevelSettings : settingsMap.get(getDataRetrieverChainDefinition()).entrySet()) {
            final DataRetrieverLevelDTO retrieverLevel = retrieverLevelSettings.getKey();
            final Class<?> settingsType = retrieverLevelSettings.getValue().getClass();
            DataMiningSettingsInfo settingsInfo = settingsManager.getSettingsInfo(settingsType);
            RetrieverLevelSettingsComponent c = new RetrieverLevelSettingsComponent(
                    this, getComponentContext(), retrieverLevel,
                    settingsInfo.getId(), settingsInfo.getLocalizedName(stringMessages)) {
                @Override
                public SettingsDialogComponent<SerializableSettings> getSettingsDialogComponent(SerializableSettings settings) {
                    return null;
                }
                @Override
                public void updateSettings(SerializableSettings newSettings) {
                }
            };
            settings.put(c.getId(), c.hasSettings() ? c.getSettings() : null);
        }
        
        return new CompositeSettings(settings);
    }
    
    @Override
    public void applyQueryDefinition(StatisticQueryDefinitionDTO queryDefinition) {
        DataRetrieverChainDefinitionDTO retrieverChain = queryDefinition.getDataRetrieverChainDefinition();
        FunctionDTO extractionFunction = queryDefinition.getStatisticToCalculate();
        ExtractionFunctionWithContext statistic = new ExtractionFunctionWithContext(retrieverChain, extractionFunction);
        int index = availableExtractionFunctions.indexOf(statistic);
        if (index != -1) {
            statistic = availableExtractionFunctions.get(index);
        } else {
            String displayName = extractionFunction.getDisplayName();
            for (ExtractionFunctionWithContext availableStatistic : availableExtractionFunctions) {
                if (availableStatistic.getExtractionFunction().getDisplayName().equals(displayName)) {
                    statistic.setVerbose(true);
                    availableStatistic.setVerbose(true);
                }
            }
            availableExtractionFunctions.add(statistic);
            extractionFunctionSuggestBox.setSelectableValues(availableExtractionFunctions);
        }
        
        extractionFunctionSuggestBox.getValueBox().setValue(statistic.getDisplayString(), false);
        extractionFunctionSuggestBox.setExtractionFunction(statistic);
        aggregatorListBox.setValue(queryDefinition.getAggregatorDefinition());
    }
    
    @Override
    public void addStatisticChangedListener(StatisticChangedListener listener) {
        retrieverChainListeners.add(listener);
        extractionFunctionListeners.add(listener);
        aggregatorDefinitionListeners.add(listener);
    }
    
    @Override
    public void addDataRetrieverChainDefinitionChangedListener(DataRetrieverChainDefinitionChangedListener listener) {
        retrieverChainListeners.add(listener);
    }
    
    private void notifyRetrieverChainListeners() {
        DataRetrieverChainDefinitionDTO dataRetrieverChainDefinition = getDataRetrieverChainDefinition();
        for (DataRetrieverChainDefinitionChangedListener listener : retrieverChainListeners) {
            listener.dataRetrieverChainDefinitionChanged(dataRetrieverChainDefinition);
        }
    }
    
    @Override
    public void addExtractionFunctionChangedListener(ExtractionFunctionChangedListener listener) {
        extractionFunctionListeners.add(listener);
    }

    private void notifyExtractionFunctionListeners() {
        FunctionDTO extractionFunction = getExtractionFunction();
        for (ExtractionFunctionChangedListener listener : extractionFunctionListeners) {
            listener.extractionFunctionChanged(extractionFunction);
        }
    }
    
    @Override
    public void addAggregatorDefinitionChangedListener(AggregatorDefinitionChangedListener listener) {
        aggregatorDefinitionListeners.add(listener);
    }

    private void notifyAggregatorDefinitionListeners() {
        AggregationProcessorDefinitionDTO aggregatorDefinition = getAggregatorDefinition();
        for (AggregatorDefinitionChangedListener listener : aggregatorDefinitionListeners) {
            listener.aggregatorDefinitionChanged(aggregatorDefinition);
        }
    }

    @Override
    public DataRetrieverChainDefinitionDTO getDataRetrieverChainDefinition() {
        ExtractionFunctionWithContext extractionFunction = extractionFunctionSuggestBox.getExtractionFunction();
        return extractionFunction == null ? null : extractionFunction.getRetrieverChain();
    }
    
    @Override
    public FunctionDTO getExtractionFunction() {
        ExtractionFunctionWithContext extractionFunction = extractionFunctionSuggestBox.getExtractionFunction();
        return extractionFunction == null ? null : extractionFunction.getExtractionFunction();
    }
    
    @Override
    public AggregationProcessorDefinitionDTO getAggregatorDefinition() {
        return aggregatorListBox.getValue();
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.statisticProvider();
    }

    @Override
    public Widget getEntryWidget() {
        return mainPanel;
    }

    @Override
    public boolean isVisible() {
        return mainPanel.isVisible();
    }

    @Override
    public void setVisible(boolean visibility) {
        mainPanel.setVisible(visibility);
    }
    
    @Override
    public String getDependentCssClassName() {
        return "globalStatisticsProvider";
    }

    @Override
    public String getId() {
        return "GlobalStatisticProvider";
    }
    
    private class ExtractionFunctionWithContext implements Comparable<ExtractionFunctionWithContext> {
        
        private final DataRetrieverChainDefinitionDTO retrieverChain;
        private final FunctionDTO extractionFunction;
        private final Collection<String> matchingStrings;
        private boolean verbose;
        
        public ExtractionFunctionWithContext(DataRetrieverChainDefinitionDTO retrieverChain, FunctionDTO extractionFunction) {
            this.retrieverChain = retrieverChain;
            this.extractionFunction = extractionFunction;
            matchingStrings = new ArrayList<>(2);
            matchingStrings.add(retrieverChain.getName());
            matchingStrings.add(extractionFunction.getDisplayName());
        }

        public DataRetrieverChainDefinitionDTO getRetrieverChain() {
            return retrieverChain;
        }

        public FunctionDTO getExtractionFunction() {
            return extractionFunction;
        }
        
        public Iterable<String> getMatchingStrings() {
            return matchingStrings;
        }

        public boolean isVerbose() {
            return verbose;
        }

        public void setVerbose(boolean verbose) {
            this.verbose = verbose;
        }
        
        public String getDisplayString() {
            StringBuilder builder = new StringBuilder(extractionFunction.getDisplayName());
            if (isVerbose()) {
                builder.append(" (").append(stringMessages.basedOn()).append(" ")
                       .append(retrieverChain.getName()).append(")");
            }
            return builder.toString();
        }
        
        @Override
        public int compareTo(ExtractionFunctionWithContext o) {
            int comparedDisplayName = extractionFunction.getDisplayName().compareTo(o.getExtractionFunction().getDisplayName());
            if (comparedDisplayName != 0) return comparedDisplayName;
            
            return retrieverChain.compareTo(o.getRetrieverChain());
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((extractionFunction == null) ? 0 : extractionFunction.hashCode());
            result = prime * result + ((retrieverChain == null) ? 0 : retrieverChain.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            ExtractionFunctionWithContext other = (ExtractionFunctionWithContext) obj;
            if (extractionFunction == null) {
                if (other.extractionFunction != null)
                    return false;
            } else if (!extractionFunction.equals(other.extractionFunction))
                return false;
            if (retrieverChain == null) {
                if (other.retrieverChain != null)
                    return false;
            } else if (!retrieverChain.equals(other.retrieverChain))
                return false;
            return true;
        }
        
    }
    
    private abstract class ExtractionFunctionSuggestBox extends CustomSuggestBox<ExtractionFunctionWithContext> {
        
        private final AbstractListSuggestOracle<ExtractionFunctionWithContext> suggestOracle;
        private ExtractionFunctionWithContext extractionFunction;
        
        @SuppressWarnings("unchecked")
        public ExtractionFunctionSuggestBox() {
            super(new AbstractListSuggestOracle<ExtractionFunctionWithContext>() {

                @Override
                protected Iterable<String> getMatchingStrings(ExtractionFunctionWithContext value) {
                    return value.getMatchingStrings();
                }

                @Override
                protected String createSuggestionKeyString(ExtractionFunctionWithContext value) {
                    return value.getDisplayString();
                }

                @Override
                protected String createSuggestionAdditionalDisplayString(ExtractionFunctionWithContext value) {
                    return null;
                }
            }, new ScrollableSuggestionDisplay());
            suggestOracle = (AbstractListSuggestOracle<ExtractionFunctionWithContext>) getSuggestOracle();
            addSuggestionSelectionHandler(this::setExtractionFunction);
        }
        
        public void setSelectableValues(Collection<ExtractionFunctionWithContext> selectableValues) {
            suggestOracle.setSelectableValues(selectableValues);
        }
        
        public void setExtractionFunction(ExtractionFunctionWithContext extractionFunction) {
            if (!Objects.equals(this.extractionFunction, extractionFunction)) {
                this.extractionFunction = extractionFunction;
                onValueChange();
            }
        }
        
        public ExtractionFunctionWithContext getExtractionFunction() {
            return extractionFunction;
        }
        
        protected abstract void onValueChange();
        
    }
    
    private static class ScrollableSuggestionDisplay extends DefaultSuggestionDisplay {
        
        public ScrollableSuggestionDisplay() {
            PopupPanel popupPanel = getPopupPanel();
            popupPanel.addStyleName("statisticSuggestBoxPopup");
        }
        
        @Override
        protected void moveSelectionUp() {
            super.moveSelectionUp();
            scrollSelectedItemIntoView();
        }
        
        @Override
        protected void moveSelectionDown() {
            super.moveSelectionDown();
            scrollSelectedItemIntoView();
        }

        private void scrollSelectedItemIntoView() {
            getSelectedMenuItem().getElement().scrollIntoView();
        }
        
        private native MenuItem getSelectedMenuItem() /*-{
            var menu = this.@com.google.gwt.user.client.ui.SuggestBox.DefaultSuggestionDisplay::suggestionMenu;
            return menu.@com.google.gwt.user.client.ui.MenuBar::selectedItem;
        }-*/;
        
    }

}
