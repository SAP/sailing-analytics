package com.sap.sailing.gwt.ui.datamining.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.i18n.client.LocaleInfo;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ValueListBox;
import com.google.gwt.user.client.ui.Widget;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.controls.AbstractObjectRenderer;
import com.sap.sailing.gwt.ui.datamining.DataMiningServiceAsync;
import com.sap.sailing.gwt.ui.datamining.GroupingChangedListener;
import com.sap.sailing.gwt.ui.datamining.GroupingProvider;
import com.sap.sailing.gwt.ui.datamining.StatisticProvider;
import com.sap.sse.common.settings.SerializableSettings;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.AggregationProcessorDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.FunctionDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;

public class MultiDimensionalGroupingProvider implements GroupingProvider {
    
    private static final String GROUPING_PROVIDER_ELEMENT_STYLE = "groupingProviderElement";
    
    private final StringMessages stringMessages;
    private final DataMiningServiceAsync dataMiningService;
    private final ErrorReporter errorReporter;
    private final Set<GroupingChangedListener> listeners;
    
    private final FlowPanel mainPanel;
    private final List<ValueListBox<FunctionDTO>> dimensionToGroupByBoxes;

    private boolean isAwaitingReload;
    private FunctionDTO currentStatisticToCalculate;
    private final List<FunctionDTO> availableDimensions;

    public MultiDimensionalGroupingProvider(StringMessages stringMessages, DataMiningServiceAsync dataMiningService, ErrorReporter errorReporter,
                                            StatisticProvider statisticProvider) {
        this.stringMessages = stringMessages;
        this.dataMiningService = dataMiningService;
        this.errorReporter = errorReporter;
        listeners = new HashSet<GroupingChangedListener>();
        currentStatisticToCalculate = null;
        availableDimensions = new ArrayList<>();
        isAwaitingReload = false;
        dimensionToGroupByBoxes = new ArrayList<ValueListBox<FunctionDTO>>();
        
        mainPanel = new FlowPanel();
        Label groupByLabel = new Label(this.stringMessages.groupBy());
        groupByLabel.addStyleName(GROUPING_PROVIDER_ELEMENT_STYLE);
        mainPanel.add(groupByLabel);

        ValueListBox<FunctionDTO> firstDimensionToGroupByBox = createDimensionToGroupByBox();
        addDimensionToGroupByBoxAndUpdateAcceptableValues(firstDimensionToGroupByBox);
        statisticProvider.addStatisticChangedListener(this);
    }
    
    @Override
    public void awaitReloadComponents() {
        isAwaitingReload = true;
    }
    
    @Override
    public boolean isAwatingReload() {
        return isAwaitingReload;
    }
    
    @Override
    public void reloadComponents() {
        isAwaitingReload = false;
        updateAvailableDimensions();
    }
    
    @Override
    public void statisticChanged(FunctionDTO newStatisticToCalculate, AggregationProcessorDefinitionDTO newAggregatorDefinition) {
        if (!Objects.equals(currentStatisticToCalculate, newStatisticToCalculate)) {
            currentStatisticToCalculate = newStatisticToCalculate;
            if (!isAwaitingReload) {
                updateAvailableDimensions();
            }
        }
    }

    private void updateAvailableDimensions() {
        if (currentStatisticToCalculate != null) {
            dataMiningService.getDimensionsFor(currentStatisticToCalculate, LocaleInfo.getCurrentLocale()
                    .getLocaleName(), new AsyncCallback<HashSet<FunctionDTO>>() {
                @Override
                public void onSuccess(HashSet<FunctionDTO> dimensions) {
                    clearAvailableDimensionsAndGroupByBoxes();
                    for (FunctionDTO dimension : dimensions) {
                        availableDimensions.add(dimension);
                    }
                    ValueListBox<FunctionDTO> firstDimensionToGroupByBox = createDimensionToGroupByBox();
                    addDimensionToGroupByBoxAndUpdateAcceptableValues(firstDimensionToGroupByBox);
                    if (!availableDimensions.isEmpty()) {
                        Collections.sort(availableDimensions);
                        firstDimensionToGroupByBox.setValue(availableDimensions.iterator().next(), true);
                    } else {
                        notifyListeners();
                    }
                }

                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error fetching the dimensions from the server: " + caught.getMessage());
                }
            });
        } else {
            clearAvailableDimensionsAndGroupByBoxes();
            ValueListBox<FunctionDTO> firstDimensionToGroupByBox = createDimensionToGroupByBox();
            addDimensionToGroupByBoxAndUpdateAcceptableValues(firstDimensionToGroupByBox);
        }
    }

    private void clearAvailableDimensionsAndGroupByBoxes() {
        for (ValueListBox<FunctionDTO> dimensionToGroupByBox : dimensionToGroupByBoxes) {
            mainPanel.remove(dimensionToGroupByBox);
        }
        dimensionToGroupByBoxes.clear();
        availableDimensions.clear();
    }

    private ValueListBox<FunctionDTO> createDimensionToGroupByBox() {
        ValueListBox<FunctionDTO> dimensionToGroupByBox = new ValueListBox<FunctionDTO>(new AbstractObjectRenderer<FunctionDTO>() {
            @Override
            protected String convertObjectToString(FunctionDTO function) {
                return function.getDisplayName();
            }
            
        });
        dimensionToGroupByBox.addStyleName(GROUPING_PROVIDER_ELEMENT_STYLE);
        dimensionToGroupByBox.addValueChangeHandler(new ValueChangeHandler<FunctionDTO>() {
            private boolean firstChange = true;

            @Override
            public void onValueChange(ValueChangeEvent<FunctionDTO> event) {
                if (firstChange && event.getValue() != null) {
                    ValueListBox<FunctionDTO> newDimensionToGroupByBox = createDimensionToGroupByBox();
                    addDimensionToGroupByBox(newDimensionToGroupByBox);
                    firstChange = false;
                } else if (event.getValue() == null) {
                    Widget changedDimensionToGroupByBox = (Widget) event.getSource();
                    mainPanel.remove(changedDimensionToGroupByBox);
                    dimensionToGroupByBoxes.remove(changedDimensionToGroupByBox);
                }
                updateAcceptableValues();
                notifyListeners();
            }
        });
        return dimensionToGroupByBox;
    }

    private void addDimensionToGroupByBoxAndUpdateAcceptableValues(ValueListBox<FunctionDTO> dimensionToGroupByBox) {
        addDimensionToGroupByBox(dimensionToGroupByBox);
        updateAcceptableValues();
    }

    private void addDimensionToGroupByBox(ValueListBox<FunctionDTO> dimensionToGroupByBox) {
        mainPanel.add(dimensionToGroupByBox);
        dimensionToGroupByBoxes.add(dimensionToGroupByBox);
    }

    private void updateAcceptableValues() {
        for (ValueListBox<FunctionDTO> dimensionToGroupByBox : dimensionToGroupByBoxes) {
            List<FunctionDTO> acceptableValues = new ArrayList<FunctionDTO>(availableDimensions);
            acceptableValues.removeAll(getDimensionsToGroupBy());
            if (dimensionToGroupByBox.getValue() != null) {
                acceptableValues.add(dimensionToGroupByBox.getValue());
            }
            Collections.sort(acceptableValues);
            acceptableValues.add(null);
            dimensionToGroupByBox.setAcceptableValues(acceptableValues);
        }
    }

    @Override
    public Collection<FunctionDTO> getDimensionsToGroupBy() {
        Collection<FunctionDTO> dimensionsToGroupBy = new ArrayList<FunctionDTO>();
        for (ValueListBox<FunctionDTO> dimensionListBox : dimensionToGroupByBoxes) {
            if (dimensionListBox.getValue() != null) {
                dimensionsToGroupBy.add(dimensionListBox.getValue());
            }
        }
        return dimensionsToGroupBy;
    }

    @Override
    public String getCustomGrouperScriptText() {
        return "";
    }

    @Override
    public void applyQueryDefinition(StatisticQueryDefinitionDTO queryDefinition) {
        int index = 0;
        for (FunctionDTO dimension : queryDefinition.getDimensionsToGroupBy()) {
            dimensionToGroupByBoxes.get(index).setValue(dimension, true);
            index++;
        }
    }

    @Override
    public void addGroupingChangedListener(GroupingChangedListener listener) {
        listeners.add(listener);
    }
    
    private void notifyListeners() {
        for (GroupingChangedListener listener : listeners) {
            listener.groupingChanged();
        }
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.groupingProvider();
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
    public boolean hasSettings() {
        return false;
    }

    @Override
    public SettingsDialogComponent<SerializableSettings> getSettingsDialogComponent() {
        return null;
    }

    @Override
    public void updateSettings(SerializableSettings newSettings) { }
    
    @Override
    public String getDependentCssClassName() {
        return "multiDimensionalGroupingProvider";
    }

}
