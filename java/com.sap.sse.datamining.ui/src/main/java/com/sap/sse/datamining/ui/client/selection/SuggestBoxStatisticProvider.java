package com.sap.sse.datamining.ui.client.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Consumer;
import java.util.Objects;
import java.util.Set;

import com.google.gwt.event.dom.client.KeyCodes;
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
import com.sap.sse.common.Util;
import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.common.settings.Settings;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverChainDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.DataRetrieverLevelDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.datamining.ui.client.AbstractDataMiningComponent;
import com.sap.sse.datamining.ui.client.AggregatorDefinitionChangedListener;
import com.sap.sse.datamining.ui.client.DataMiningServiceAsync;
import com.sap.sse.datamining.ui.client.DataMiningSettingsControl;
import com.sap.sse.datamining.ui.client.DataMiningSettingsInfo;
import com.sap.sse.datamining.ui.client.DataMiningSettingsInfoManager;
import com.sap.sse.datamining.ui.client.DataRetrieverChainDefinitionChangedListener;
import com.sap.sse.datamining.ui.client.ExtractionFunctionChangedListener;
import com.sap.sse.datamining.ui.client.StatisticChangedListener;
import com.sap.sse.datamining.ui.client.StatisticProvider;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.shared.components.Component;
import com.sap.sse.gwt.client.shared.components.CompositeSettings;
import com.sap.sse.gwt.client.shared.components.CompositeTabbedSettingsDialogComponent;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.controls.AbstractObjectRenderer;
import com.sap.sse.gwt.client.shared.settings.ComponentContext;
import com.sap.sse.gwt.client.suggestion.AbstractListSuggestOracle;
import com.sap.sse.gwt.client.suggestion.CustomSuggestBox;

/**
 * A {@link StatisticProvider} that contains all statistics registered in the server. Each statistic is paired with the
 * corresponding {@link DataRetrieverChainDefinitionDTO}. This provides a more comfortable access to the available
 * statistics, without the need to specify the domain to analyze.
 * 
 * @author Lennart Hensler
 */
public class SuggestBoxStatisticProvider extends AbstractDataMiningComponent<CompositeSettings>
        implements StatisticProvider {

    private static final String STATISTIC_PROVIDER_ELEMENT_STYLE = "statisticProviderElement";

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
    private final List<AggregationProcessorDefinitionDTO> availableAggregators;
    
    private AggregationProcessorDefinitionDTO aggregatorToSelect;
    private Consumer<Iterable<String>> selectionCallback;

    public SuggestBoxStatisticProvider(Component<?> parent, ComponentContext<?> componentContext,
            DataMiningServiceAsync dataMiningService, ErrorReporter errorReporter,
            DataMiningSettingsControl settingsControl, DataMiningSettingsInfoManager settingsManager) {
        super(parent, componentContext);
        this.dataMiningService = dataMiningService;
        this.errorReporter = errorReporter;
        retrieverChainListeners = new HashSet<>();
        extractionFunctionListeners = new HashSet<>();
        aggregatorDefinitionListeners = new HashSet<>();
        isAwaitingReload = false;
        this.settingsManager = settingsManager;
        this.settingsControl = settingsControl;
        this.settingsControl.addSettingsComponent(this);
        settingsMap = new HashMap<>();
        retrieverLevelSettingsComponents = new ArrayList<>();

        mainPanel = new FlowPanel();
        Label label = new Label(getDataMiningStringMessages().calculateThe());
        label.addStyleName(STATISTIC_PROVIDER_ELEMENT_STYLE);
        label.addStyleName("queryProviderElementLabel");
        label.addStyleName("emphasizedLabel");
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
        extractionFunctionSuggestBox.getValueBox().addKeyUpHandler(e -> {
            int keyCode = e.getNativeEvent().getKeyCode();
            if (keyCode == KeyCodes.KEY_ESCAPE) {
                extractionFunctionSuggestBox.hideSuggestionList();
                extractionFunctionSuggestBox.setFocus(false);
            }
        });
        extractionFunctionSuggestBox.setLimit(Integer.MAX_VALUE);
        extractionFunctionSuggestBox.addStyleName(STATISTIC_PROVIDER_ELEMENT_STYLE);
        extractionFunctionSuggestBox.setWidth("540px");
        mainPanel.add(extractionFunctionSuggestBox);

        availableAggregators = new ArrayList<>();
        aggregatorListBox = createAggregatorListBox();
        aggregatorListBox.addStyleName(STATISTIC_PROVIDER_ELEMENT_STYLE);
        mainPanel.add(aggregatorListBox);
    }

    private ValueListBox<AggregationProcessorDefinitionDTO> createAggregatorListBox() {
        ValueListBox<AggregationProcessorDefinitionDTO> aggregatorListBox = new ValueListBox<AggregationProcessorDefinitionDTO>(
                new AbstractObjectRenderer<AggregationProcessorDefinitionDTO>() {
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
                        extractionFunctionSuggestBox.setSelectableValues(availableExtractionFunctions);
                    } else {
                        for (DataRetrieverChainDefinitionDTO retrieverChain : dataRetrieverChainDefinitions) {
                            if (retrieverChain.hasSettings()) {
                                settingsMap.put(retrieverChain, retrieverChain.getDefaultSettings());
                            }
                            dataMiningService.getStatisticsFor(retrieverChain, localeName,
                                new AsyncCallback<HashSet<FunctionDTO>>() {
                                    @Override
                                    public void onSuccess(HashSet<FunctionDTO> statistics) {
                                        collectStatistics(retrieverChain, statistics);
                                    }

                                    @Override
                                    public void onFailure(Throwable caught) {
                                        errorReporter.reportError("Error fetching the statistics for the retriever chain '"
                                                        + retrieverChain + "': " + caught.getMessage());
                                        collectStatistics(retrieverChain, Collections.emptySet());
                                    }
                                });
                        }
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    errorReporter
                            .reportError("Error fetching the retriever chain definitions: " + caught.getMessage());
                }
            });
    }

    private void collectStatistics(DataRetrieverChainDefinitionDTO retrieverChain,
            Iterable<FunctionDTO> extractionFunctions) {
        for (FunctionDTO extractionFunction : extractionFunctions) {
            availableExtractionFunctions.add(new ExtractionFunctionWithContext(retrieverChain, extractionFunction));
        }

        awaitingRetrieverChainStatistics--;
        if (awaitingRetrieverChainStatistics == 0) {
            Collections.sort(availableExtractionFunctions);
            extractionFunctionSuggestBox.setSelectableValues(availableExtractionFunctions);

            // TODO Do not pre-select the first element. The other UI components have to be able to handle "empty content"
            ExtractionFunctionWithContext currentValue = extractionFunctionSuggestBox.getExtractionFunction();
            ExtractionFunctionWithContext valueToBeSelected = availableExtractionFunctions.contains(currentValue)
                    ? currentValue : Util.first(availableExtractionFunctions);
            extractionFunctionSuggestBox.setExtractionFunction(valueToBeSelected);
        }
    }

    private void updateAggregators() {
        availableAggregators.clear();
        FunctionDTO extractionFunction = getExtractionFunction();
        if (extractionFunction == null) {
            updateListBox(aggregatorListBox, availableAggregators);
        } else {
            dataMiningService.getAggregatorDefinitionsFor(extractionFunction, LocaleInfo.getCurrentLocale().getLocaleName(),
                new AsyncCallback<HashSet<AggregationProcessorDefinitionDTO>>() {
                    @Override
                    public void onSuccess(HashSet<AggregationProcessorDefinitionDTO> aggregators) {
                        availableAggregators.addAll(aggregators);
                        Collections.sort(availableAggregators);
                        updateListBox(aggregatorListBox, availableAggregators);
                        
                        if (aggregatorToSelect != null) {
                            setAggregator(aggregatorToSelect, selectionCallback);
                            aggregatorToSelect = null;
                            selectionCallback = null;
                        }
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        errorReporter.reportError("Error fetching the aggregators for the extraction function'"
                                + extractionFunction + "': " + caught.getMessage());
                        aggregatorToSelect = null;
                        selectionCallback = null;
                    }
                });
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
        for (Entry<DataRetrieverLevelDTO, SerializableSettings> retrieverLevelSettings : settingsMap.get(retrieverChain)
                .entrySet()) {
            final DataRetrieverLevelDTO retrieverLevel = retrieverLevelSettings.getKey();
            final Class<?> settingsType = retrieverLevelSettings.getValue().getClass();
            DataMiningSettingsInfo settingsInfo = settingsManager.getSettingsInfo(settingsType);
            settingsComponents.add(new RetrieverLevelSettingsComponent(this, getComponentContext(), retrieverLevel,
                    settingsInfo.getId(), settingsInfo.getLocalizedName()) {
                @Override
                public SettingsDialogComponent<SerializableSettings> getSettingsDialogComponent(
                        SerializableSettings settings) {
                    return settingsManager.getSettingsInfo(settingsType)
                            .createSettingsDialogComponent(settingsMap.get(retrieverChain).get(retrieverLevel));
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
        Map<DataRetrieverLevelDTO, SerializableSettings> chainSettings = settingsMap
                .get(getDataRetrieverChainDefinition());
        for (Entry<String, Settings> settingsPerComponent : newSettings.getSettingsPerComponentId().entrySet()) {
            RetrieverLevelSettingsComponent component = (RetrieverLevelSettingsComponent) findComponentById(
                    settingsPerComponent.getKey());
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
        for (Entry<DataRetrieverLevelDTO, SerializableSettings> retrieverLevelSettings : settingsMap
                .get(getDataRetrieverChainDefinition()).entrySet()) {
            final DataRetrieverLevelDTO retrieverLevel = retrieverLevelSettings.getKey();
            final Class<?> settingsType = retrieverLevelSettings.getValue().getClass();
            DataMiningSettingsInfo settingsInfo = settingsManager.getSettingsInfo(settingsType);
            RetrieverLevelSettingsComponent c = new RetrieverLevelSettingsComponent(this, getComponentContext(),
                    retrieverLevel, settingsInfo.getId(),
                    settingsInfo.getLocalizedName()) {
                @Override
                public SettingsDialogComponent<SerializableSettings> getSettingsDialogComponent(
                        SerializableSettings settings) {
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
    public void applyQueryDefinition(StatisticQueryDefinitionDTO queryDefinition, Consumer<Iterable<String>> callback) {
        DataRetrieverChainDefinitionDTO retrieverChain = queryDefinition.getDataRetrieverChainDefinition();
        FunctionDTO extractionFunction = queryDefinition.getStatisticToCalculate();
        ExtractionFunctionWithContext statistic = new ExtractionFunctionWithContext(retrieverChain, extractionFunction);
        int index = availableExtractionFunctions.indexOf(statistic);
        if (index != -1) {
            setSettings(retrieverChain, queryDefinition.getRetrieverSettings());
            
            statistic = availableExtractionFunctions.get(index);
            DataRetrieverChainDefinitionDTO oldRetrieverChain = getDataRetrieverChainDefinition();
            extractionFunctionSuggestBox.setExtractionFunction(statistic);
            
            aggregatorToSelect = queryDefinition.getAggregatorDefinition();
            selectionCallback = callback;
            if (retrieverChain.equals(oldRetrieverChain)) {
                setAggregator(aggregatorToSelect, selectionCallback);
                aggregatorToSelect = null;
                selectionCallback = null;
            }
        } else {
            String errorMessage = getDataMiningStringMessages().statisticNotAvailable(
                    extractionFunction.getDisplayName());
            callback.accept(Collections.singleton(errorMessage));
        }
    }

    private void setSettings(DataRetrieverChainDefinitionDTO retrieverChain, HashMap<DataRetrieverLevelDTO, SerializableSettings> settings) {
        HashMap<DataRetrieverLevelDTO, SerializableSettings> newSettings = new HashMap<>();
        for (int level = 0; level < retrieverChain.getLevelAmount(); level++) {
            DataRetrieverLevelDTO retrieverLevel = retrieverChain.getRetrieverLevel(level);
            if (retrieverLevel.hasSettings()) {
                SerializableSettings levelSettings = settings.get(retrieverLevel);
                if (levelSettings == null) {
                    levelSettings = retrieverLevel.getDefaultSettings();
                }
                newSettings.put(retrieverLevel, levelSettings);
            }
        }
        settingsMap.put(retrieverChain, newSettings);
    }

    private void setAggregator(AggregationProcessorDefinitionDTO aggregator, Consumer<Iterable<String>> callback) {
        Iterable<String> callbackMessages = Collections.emptySet();
        if (availableAggregators.contains(aggregator)) {
            aggregatorListBox.setValue(aggregator, true);
        } else {
            String errorMessage = getDataMiningStringMessages()
                    .aggregatorNotAvailable(aggregator.getDisplayName());
            callbackMessages = Collections.singleton(errorMessage);
        }
        callback.accept(callbackMessages);
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
        return getDataMiningStringMessages().statisticProvider();
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

        public ExtractionFunctionWithContext(DataRetrieverChainDefinitionDTO retrieverChain,
                FunctionDTO extractionFunction) {
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

        @Override
        public int compareTo(ExtractionFunctionWithContext o) {
            String otherDisplayName = o.getExtractionFunction().getDisplayName();
            int comparedDisplayName = extractionFunction.getDisplayName().compareToIgnoreCase(otherDisplayName);
            if (comparedDisplayName != 0) {
                return comparedDisplayName;
            }
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
        private final ScrollableSuggestionDisplay display;
        
        private ExtractionFunctionWithContext extractionFunction;

        @SuppressWarnings("unchecked")
        public ExtractionFunctionSuggestBox() {
            super(new AbstractListSuggestOracle<ExtractionFunctionWithContext>() {
                @Override
                protected Iterable<String> getKeywordStrings(Iterable<String> queryTokens) {
                    String filterText = Util.first(queryTokens);
                    if (filterText == null) {
                        return queryTokens;
                    }
                    return Util.splitAlongWhitespaceRespectingDoubleQuotedPhrases(filterText);
                }

                @Override
                protected Iterable<String> getMatchingStrings(ExtractionFunctionWithContext value) {
                    return value.getMatchingStrings();
                }

                @Override
                protected String createSuggestionKeyString(ExtractionFunctionWithContext value) {
                    return value.getExtractionFunction().getDisplayName();
                }

                @Override
                protected String createSuggestionAdditionalDisplayString(ExtractionFunctionWithContext value) {
                    return value.getRetrieverChain().getName();
                }
            }, new ScrollableSuggestionDisplay());
            suggestOracle = (AbstractListSuggestOracle<ExtractionFunctionWithContext>) getSuggestOracle();
            display = (ScrollableSuggestionDisplay) getSuggestionDisplay();
            addSuggestionSelectionHandler(this::setExtractionFunction);
        }
        
        @Override
        public void hideSuggestionList() {
            display.hideSuggestions();
        }

        public void setSelectableValues(Collection<ExtractionFunctionWithContext> selectableValues) {
            suggestOracle.setSelectableValues(selectableValues);
        }

        public void setExtractionFunction(ExtractionFunctionWithContext extractionFunction) {
            if (!Objects.equals(this.extractionFunction, extractionFunction)) {
                this.extractionFunction = extractionFunction;
                setValue(extractionFunction.getExtractionFunction().getDisplayName(), false);
                onValueChange();
            }
            setFocus(false);
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
