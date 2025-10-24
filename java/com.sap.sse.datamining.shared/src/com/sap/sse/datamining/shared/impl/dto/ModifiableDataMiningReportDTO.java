package com.sap.sse.datamining.shared.impl.dto;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.datamining.shared.data.ReportParameterToDimensionFilterBindings;
import com.sap.sse.datamining.shared.dto.DataMiningReportDTO;
import com.sap.sse.datamining.shared.dto.FilterDimensionIdentifier;
import com.sap.sse.datamining.shared.dto.FilterDimensionParameter;
import com.sap.sse.datamining.shared.dto.StatisticQueryDefinitionDTO;
import com.sap.sse.datamining.shared.impl.dto.parameters.ParameterModelListener;
import com.sap.sse.datamining.shared.impl.dto.parameters.ValueListFilterParameter;

public class ModifiableDataMiningReportDTO implements DataMiningReportDTO {
    private static final long serialVersionUID = -6512175470789118223L;
    
    /**
     * Handled by identity; remove and contains checks don't use query equality because modifiable queries can change
     * their equality/hashCode over their life cycle
     */
    private ArrayList<ModifiableStatisticQueryDefinitionDTO> queryDefinitions;
    private HashSet<FilterDimensionParameter> parameters;
    private IdentityHashMap<StatisticQueryDefinitionDTO, HashMap<FilterDimensionIdentifier, FilterDimensionParameter>> parameterUsages;
    
    @SuppressWarnings("unused") // only to force the type to be instantiable for GWT serialization
    private ParameterModelListener _serializationDummy = null;
    
    /**
     * Objects of this type entertain one value change listener for each parameter in {@link #getParameters()} so
     * that when a parameter value changes, all {@link #getQueryDefinitions() queries in this report} using this
     * parameter will have their {@link StatisticQueryDefinitionDTO#getFilterSelection() filter selections} adjusted
     * accordingly.
     */
    private HashMap<FilterDimensionParameter, ParameterModelListener> parameterValueChangeListeners;
    
    private transient Set<ParameterModelListener> parameterModelListeners;
    
    /**
     * Creates an empty report with no queries and hence no parameter usages.
     */
    public ModifiableDataMiningReportDTO() {
        this(Collections.emptySet(), Collections.emptySet());
    }
    
    public ModifiableDataMiningReportDTO(Iterable<ModifiableStatisticQueryDefinitionDTO> queryDefinitions, Iterable<FilterDimensionParameter> parameters) {
        this.queryDefinitions = new ArrayList<>();
        this.parameterValueChangeListeners = new HashMap<>();
        Util.addAll(queryDefinitions, this.queryDefinitions);
        this.parameters = new HashSet<>();
        Util.addAll(parameters, this.parameters);
        parameterModelListeners = new HashSet<>();
        parameterUsages = new IdentityHashMap<>();
    }

    /**
     * Based on all recorded {@link #getParameterUsages(FilterDimensionParameter) usages}, the corresponding queries'
     * {@link StatisticQueryDefinitionDTO#getFilterSelection() filter selections} are adjusted to reflect the new
     * parameter's {@link FilterDimensionParameter#getValues() value set}.
     */
    void adjustAllDimensionFiltersInQueriesUsingParameter(FilterDimensionParameter parameter) {
        for (final Pair<StatisticQueryDefinitionDTO, FilterDimensionIdentifier> usage : getParameterUsages(parameter)) {
            // the following cast is safe because addQueryDefinition accepts only ModifiableStatisticQueryDefinitionDTO objects
            final ModifiableStatisticQueryDefinitionDTO query = (ModifiableStatisticQueryDefinitionDTO) usage.getA();
            final DataRetrieverLevelDTO retrieverLevel = usage.getB().getRetrieverLevel();
            final HashMap<DataRetrieverLevelDTO, HashMap<FunctionDTO, HashSet<? extends Serializable>>> selection = query.getFilterSelection();
            final HashMap<FunctionDTO, HashSet<? extends Serializable>> selectionForRetrieverLevel = selection.get(retrieverLevel);
            // note that there may be left-over parameter usages from dimension filters that were removed when they were bound to a parameter
            if (selectionForRetrieverLevel != null) {
                selectionForRetrieverLevel.put(usage.getB().getDimensionFunction(), createHashSetFromIterable(parameter.getValues()));
                query.setFilterSelectionFor(retrieverLevel, selectionForRetrieverLevel);
                query.setQueryChangedSinceLastRun(true);
            }
        }
    }
    
    private <T extends Serializable> HashSet<T> createHashSetFromIterable(Iterable<T> iterable) {
        final HashSet<T> result = new HashSet<>();
        Util.addAll(iterable, result);
        return result;
    }
    
    @Override
    public Iterable<StatisticQueryDefinitionDTO> getQueryDefinitions() {
        return new ArrayList<>(queryDefinitions);
    }
    
    @Override
    public void addQueryDefinition(int index, ModifiableStatisticQueryDefinitionDTO queryDefinition) {
        if (!queryDefinitions.stream().anyMatch(qd->qd==queryDefinition)) {
            queryDefinitions.add(index, queryDefinition);
        }
    }
    
    /**
     * Removes the query definition from this reports (searched by its identity, not equality) and adjusts the
     * {@link #parameterUsages} accordingly, removing all usages within the query removed.
     * 
     * @param queryDefinition handled by identity, not equality.
     */
    @Override
    public int removeQueryDefinition(StatisticQueryDefinitionDTO queryDefinition) {
        parameterUsages.remove(queryDefinition);
        int index = 0;
        int result = -1;
        for (final Iterator<ModifiableStatisticQueryDefinitionDTO> i=queryDefinitions.iterator(); i.hasNext(); index++) {
            if (i.next() == queryDefinition) {
                result = index;
                i.remove();
                break;
            }
        }
        return result;
    }

    @Override
    public Iterable<FilterDimensionParameter> getParameters() {
        return parameters;
    }

    @Override
    public Iterable<Pair<StatisticQueryDefinitionDTO, FilterDimensionIdentifier>> getParameterUsages(FilterDimensionParameter parameter) {
        final List<Pair<StatisticQueryDefinitionDTO, FilterDimensionIdentifier>> result = new ArrayList<>();
        for (final Entry<StatisticQueryDefinitionDTO, HashMap<FilterDimensionIdentifier, FilterDimensionParameter>> e : parameterUsages.entrySet()) {
            for (final FilterDimensionIdentifier f : e.getValue().keySet()) {
                if (e.getValue().get(f) == parameter) {
                    result.add(new Pair<>(e.getKey(), f));
                }
            }
        }
        return result;
    }
    
    @Override
    public ReportParameterToDimensionFilterBindings getParameterUsages(StatisticQueryDefinitionDTO query) {
        return new ReportParameterToDimensionFilterBindings(parameterUsages.get(query));
    }

    @Override
    public FilterDimensionParameter getUsedParameter(StatisticQueryDefinitionDTO query, FilterDimensionIdentifier filterDimensionIdentifier) {
        final FilterDimensionParameter result;
        final HashMap<FilterDimensionIdentifier, FilterDimensionParameter> parameterUsagesInQuery = parameterUsages.get(query);
        if (parameterUsagesInQuery != null) {
            result = parameterUsagesInQuery.get(filterDimensionIdentifier);
        } else {
            result = null;
        }
        return result;
    }
    
    @Override
    public FilterDimensionParameter createParameter(String name, String typeName, Iterable<? extends Serializable> values) {
        final FilterDimensionParameter parameter = new ValueListFilterParameter(name, typeName, values);
        final ParameterValueChangeListener valueChangeListener = new ParameterValueChangeListener(this);
        parameter.addParameterModelListener(valueChangeListener);
        parameterValueChangeListeners.put(parameter, valueChangeListener);
        parameters.add(parameter);
        getParameterModelListeners().forEach(l->l.parameterAdded(this, parameter));
        return parameter;
    }
    
    @Override
    public void addParameterUsage(StatisticQueryDefinitionDTO query, FilterDimensionIdentifier filterDimensionIdentifier, FilterDimensionParameter parameter) {
        if (!Util.contains(getParameters(), parameter)) {
            throw new IllegalArgumentException("Parameter "+parameter.getName()+" is not part of this report");
        }
        if (!Util.stream(getQueryDefinitions()).anyMatch(q->q==query)) {
            throw new IllegalArgumentException("Query "+query+" is not part of this report");
        }
        parameterUsages.computeIfAbsent(query, k->new HashMap<>()).put(filterDimensionIdentifier, parameter);
    }
    
    @Override
    public void removeParameterUsage(StatisticQueryDefinitionDTO query, FilterDimensionIdentifier filterDimensionIdentifier,
            FilterDimensionParameter parameter) {
        if (parameterUsages.containsKey(query)) {
            parameterUsages.get(query).remove(filterDimensionIdentifier);
        }
    }

    @Override
    public void removeParameter(FilterDimensionParameter parameter) {
        final ParameterModelListener valueChangeListener = parameterValueChangeListeners.get(parameter);
        if (valueChangeListener != null) {
            parameter.removeParameterModelListener(valueChangeListener);
            parameterValueChangeListeners.remove(parameter);
        }
        parameters.remove(parameter);
        getParameterModelListeners().forEach(l->l.parameterRemoved(this, parameter));
        removeParameterUsages(parameter);
    }
    
    private void removeParameterUsages(FilterDimensionParameter parameter) {
        for (final Entry<StatisticQueryDefinitionDTO, HashMap<FilterDimensionIdentifier, FilterDimensionParameter>> parametersUsedInQuery : parameterUsages.entrySet()) {
            for (final Iterator<Entry<FilterDimensionIdentifier, FilterDimensionParameter>> parameterUsageInQueryIter = parametersUsedInQuery.getValue().entrySet().iterator(); parameterUsageInQueryIter.hasNext(); ) {
                final Entry<FilterDimensionIdentifier, FilterDimensionParameter> parameterUsageInQuery = parameterUsageInQueryIter.next();
                if (parameterUsageInQuery.getValue() == parameter) {
                    parameterUsageInQueryIter.remove();
                }
            }
        }
    }

    private Set<ParameterModelListener> getParameterModelListeners() {
        if (parameterModelListeners == null) { // transient; may have been nulled by de-serialization
            parameterModelListeners = new HashSet<>();
        }
        return parameterModelListeners;
    }

    @Override
    public void addParamterModelListener(ParameterModelListener listener) {
        getParameterModelListeners().add(listener);
    }

    @Override
    public void removeParameterModelListener(ParameterModelListener listener) {
        getParameterModelListeners().remove(listener);
    }
}
