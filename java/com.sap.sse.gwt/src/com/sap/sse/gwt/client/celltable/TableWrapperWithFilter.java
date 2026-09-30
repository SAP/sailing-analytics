package com.sap.sse.gwt.client.celltable;

import java.util.Collections;
import java.util.Optional;
import java.util.function.Function;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.StringMessages;
import com.sap.sse.gwt.client.panels.LabeledAbstractFilterablePanel;

/**
 * A {@link CellTable} wrapped in a {@link VerticalPanel}, with a {@link LabeledAbstractFilterablePanel} above it which
 * may or may not have a filter checkbox enabled if a subclass
 * {@link LabeledAbstractFilterablePanel#setUpdatePermissionFilterForCheckbox(java.util.function.Function) sets a filter
 * function} to apply when the checkbox is activated.<p>
 * 
 * {@link #refresh(Iterable) Refreshing} the contents of the table displayed here will update the set of <em>all</em>
 * objects underlying the filter. The filtering / sorting will then again be applied to update the model of the actual
 * cell table displaying the filtered data set.<p>
 * 
 * The table starts out with an empty data set.
 * 
 * @author Axel Uhl (d043530)
 */
public abstract class TableWrapperWithFilter<T, S extends RefreshableSelectionModel<T>, SM extends StringMessages, TR extends CellTableWithCheckboxResources>
extends TableWrapper<T, S, SM, TR> {
    private final LabeledAbstractFilterablePanel<T> filterPanel;
    
    /**
     * See
     * {@link #TableWrapperWithFilter(StringMessages, ErrorReporter, boolean, boolean, Optional, CellTableWithCheckboxResources, Optional, Optional, String)}
     * Default table resources of type {@link CellTableWithCheckboxResources} will be used by this constructor variant.
     */
    public TableWrapperWithFilter(SM stringMessages, ErrorReporter errorReporter, boolean multiSelection,
            boolean enablePager, Optional<EntityIdentityComparator<T>> entityIdentityComparator,
            Optional<Function<T, Boolean>> updatePermissionFilterForCheckbox, Optional<String> filterLabel,
            String filterCheckboxLabel) {
        this(stringMessages, errorReporter, multiSelection, enablePager, entityIdentityComparator,
                GWT.create(CellTableWithCheckboxResources.class), updatePermissionFilterForCheckbox, filterLabel,
                filterCheckboxLabel);
    }

    /**
     * @param multiSelection
     *            must fit the {@code S} selection model type; a {@link RefreshableSingleSelectionModel} shall be used
     *            if {@code multiSelection} is {@code false}, whereas a {@link RefreshableMultiSelectionModel} shall be
     *            used otherwise.
     * @param entityIdentityComparator
     *            if provided, specifies how row objects of type {@code T} shall be compared with each other to
     *            determine whether the entities they represent are the same. For example, if two distinct DTOs have
     *            equal attribute values for an "ID" field they may be considered "identical" by this definition. If not
     *            provided, Java object identity will be used to determine "identity" and this will not allow you to
     *            replace one object for another while keeping selection even if the replacing object represents the
     *            same entity.
     * @param updatePermissionFilterForCheckbox
     *            if provided, a checkbox will be shown next to the filter text box that will by default apply the
     *            filter condition encoded by this parameter's function; when the checkbox is un-ticked, all objects
     *            passing the text filter will be shown instead.
     * @param filterLabel
     *            If provided, this replaces the regular (i18n-treated) "Filter by" label shown in front of the filter
     *            text box
     * @param filterCheckboxLabel
     *            if a non-empty {@code updatePermissionFilterForCheckbox} is provided then this parameter must be
     *            non-{@code null}, providing the label text for the checkbox used to toggle the filter
     */
    public TableWrapperWithFilter(SM stringMessages, ErrorReporter errorReporter, boolean multiSelection,
            boolean enablePager, Optional<EntityIdentityComparator<T>> entityIdentityComparator, TR tableRes,
            Optional<Function<T, Boolean>> updatePermissionFilterForCheckbox, Optional<String> filterLabel,
            String filterCheckboxLabel) {
        super(stringMessages, errorReporter, multiSelection, enablePager, entityIdentityComparator.orElse(null),
                tableRes);
        filterPanel = new LabeledAbstractFilterablePanel<T>(new Label(filterLabel.orElse(stringMessages.filterBy())),
                Collections.emptySet(), dataProvider, stringMessages, filterCheckboxLabel) {
            @Override
            public Iterable<String> getSearchableStrings(T t) {
                return TableWrapperWithFilter.this.getSearchableStrings(t);
            }

            @Override
            public AbstractCellTable<T> getCellTable() {
                return TableWrapperWithFilter.this.getTable();
            }
        };
        registerSelectionModelOnNewDataProvider(filterPanel.getAllListDataProvider());
        updatePermissionFilterForCheckbox.ifPresent(getFilterPanel()::setUpdatePermissionFilterForCheckbox);
        mainPanel.insert(filterPanel, 0);
    }
    
    @Override
    public void refresh(Iterable<T> newItems) {
        getFilterPanel().updateAll(newItems);
    }
    
    @Override
    public void refresh() {
        getFilterPanel().filter();
    }

    @Override
    public void add(T t) {
        getFilterPanel().add(t);
    }

    @Override
    public void remove(T t) {
        getFilterPanel().remove(t);
    }

    @Override
    public void clear() {
        getFilterPanel().removeAll();
    }

    public LabeledAbstractFilterablePanel<T> getFilterPanel() {
        return filterPanel;
    }

    protected abstract Iterable<String> getSearchableStrings(T t);
}
