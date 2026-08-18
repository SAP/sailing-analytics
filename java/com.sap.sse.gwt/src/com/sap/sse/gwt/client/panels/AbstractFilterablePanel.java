package com.sap.sse.gwt.client.panels;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.user.cellview.client.AbstractCellTable;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.ColumnSortEvent;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.SingleSelectionModel;
import com.sap.sse.common.Util;
import com.sap.sse.common.filter.AbstractKeywordFilter;
import com.sap.sse.common.filter.AbstractListFilter;
import com.sap.sse.common.filter.Filter;
import com.sap.sse.gwt.client.StringMessages;
import com.sap.sse.gwt.client.celltable.RefreshableSelectionModel;

/**
 * This Panel contains a text box and optionally a check box. Text entered into the text box filters the
 * {@link CellTable} passed to the constructor by adjusting the cell table's {@link ListDataProvider}'s contents using
 * the {@link AbstractListFilter#applyFilter(String, List)} and then sorting the table again according the the sorting
 * criteria currently active (the sorting is the only reason why the {@link CellTable} actually needs to be known to an
 * instance of this class). To be initiated the method {@link #getSearchableStrings(Object)} has to be defined, which
 * gets those Strings from a <code>T</code> that should be considered when filtering, e.g. name or boatClass. The cell
 * table can be sorted independently from the text box (e.g., after adding new objects) by calling the method
 * {@link #updateAll(Iterable)} which then runs the filter over the new selection.
 * <p>
 * 
 * If a filter function has been {@link #setUpdatePermissionFilterForCheckbox(Function) set}, an additional checkbox
 * will be displayed which toggles the application of the filter function. It is intended to be used as a
 * permission-based filter, toggling between showing only objects the user can change (UPDATE permission) and all
 * objects the user can READ. This assumption is currently encoded in the default
 * {@link StringMessages#hideElementsWithoutUpdateRights() text label used for the checkbox}, but there are currently no
 * constraints regarding the actual implementation of the filter function.
 * <p>
 * 
 * Note that this panel does <em>not</em> contain the table that it filters. With this, this class's clients are free to
 * position the table wherever they want, not necessarily related to the text box provided by this panel in any specific
 * way.
 * <p>
 * 
 * It is recommended to use the {@link #getAllListDataProvider()} as the data provider for the table's selection model.
 * This way, when the filter reduces the elements displayed in the table the selection will still refer to all elements
 * and will not be modified solely by the act of filtering.
 * <p>
 * 
 * Provides methods for filtering and selection of table items.
 * 
 * @param <T>
 * @author Nicolas Klose, Axel Uhl
 * 
 */
public abstract class AbstractFilterablePanel<T> extends HorizontalPanel {
    protected ListDataProvider<T> all;
    protected final ListDataProvider<T> filtered;
    protected final TextBox textBox;
    protected final CheckBox showOnlyObjectsWithUpdatePermissionCheckBox;
    
    private final Set<Filter<T>> filters = new HashSet<>();
    private final CheckboxEnablableFilter<T> checkboxFilterForUpdatableObjectsOnly;

    /**
     * Tells whether the {@link #showOnlyObjectsWithUpdatePermissionCheckBox} has already been added to this panel
     */
    private boolean showOnlyObjectsWithUpdatePermissionCheckBoxAdded = false;
    
    private List<String> select;
    private String selectExact;
    private boolean executeSelectOnRefresh;
    private boolean executeSelect;

    static class CheckboxEnablableFilter<T> implements Filter<T> {
        private final CheckBox checkbox;
        private Filter<T> filterToApply;

        public CheckboxEnablableFilter(CheckBox checkbox) {
            this.checkbox = checkbox;
        }

        @Override
        public boolean matches(T object) {
            final boolean result;
            if (filterToApply == null) {
                result = true;
            } else if (!this.checkbox.getValue()) {
                result = true;
            } else {
                result = filterToApply.matches(object);
            }
            return result;
        }

        @Override
        public String getName() {
            return filterToApply.getName();
        }

        public void setFilterToApply(Filter<T> filterToApply) {
            this.filterToApply = filterToApply;
        }
    }

    protected final AbstractKeywordFilter<T> filterer = new AbstractKeywordFilter<T>() {
        @Override
        public Iterable<String> getStrings(T t) {
            return getSearchableStrings(t);
        }
    };

    /**
     * This filter is used to find exact matches. 
     * It is used when the parameter selectExact is given in an {@link #select(List, String) select} statement.
     */
    protected final AbstractKeywordFilter<T> selectionFilter = new AbstractKeywordFilter<T>() {
        @Override
        public Iterable<String> getStrings(T t) {
            return getSearchableStrings(t);
        }
    };
    
    /**
     * @param all
     *            the sequence of all objects that may be displayed in the table and from which the filter may choose.
     *            This panel keeps a copy, so modifications to the <code>all</code> object do not reflect in the table
     *            contents. Use {@link #updateAll(Iterable)} instead to update the sequence of available objects.
     * @param drawTextBox
     *            if {@code true}, the default text box will be shown that can be used to provide the filter text; if
     *            {@code false}, the box may optionally be added later using the {@link #addDefaultTextBox()} method.
     *            This way, subclasses may choose to add other filter elements before the default text filter box.
     *            Filtering will still use the text box contents, even if the box is ultimately not shown; but under
     *            normal circumstances the text box will be empty in this case, not making filtering any stricter.
     * @param executeSelectionOnRefresh
     *            Used for table items selection by URL-parameters. If true, the table items will wait to be selected
     *            until the table list items have been refreshed.
     * @param filterCheckboxLabel
     *            The text to use for the label of the filter checkbox which will be displayed if a
     *            {@link #setUpdatePermissionFilterForCheckbox(Function) filter is set}.
     */
    public AbstractFilterablePanel(Iterable<T> all, final ListDataProvider<T> filtered,
            boolean drawTextBox, final StringMessages stringMessages, boolean executeSelectOnRefresh,
            String filterCheckboxLabel) {
        this.executeSelectOnRefresh = executeSelectOnRefresh;
        filters.add(filterer);
        setSpacing(5);
        this.all = new ListDataProvider<>();
        this.filtered = filtered;
        this.textBox = new TextBox();
        this.textBox.ensureDebugId("FilterTextBox");
        this.showOnlyObjectsWithUpdatePermissionCheckBox = new CheckBox(filterCheckboxLabel);
        showOnlyObjectsWithUpdatePermissionCheckBox.setValue(true);
        checkboxFilterForUpdatableObjectsOnly = new CheckboxEnablableFilter<>(showOnlyObjectsWithUpdatePermissionCheckBox);
        filters.add(checkboxFilterForUpdatableObjectsOnly);
        this.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        setAll(all);
        if (drawTextBox) {
            addDefaultTextBox();
        }       
    }

    /**
     * Like {@link #AbstractFilterablePanel(Iterable, ListDataProvider, boolean, StringMessages, boolean)}, but
     * defaults the {@code filterCheckboxLabel} parameter to {@link StringMessages#hideElementsWithoutUpdateRights()}.
     */
    public AbstractFilterablePanel(Iterable<T> all, final ListDataProvider<T> filtered,
            boolean drawTextBox, final StringMessages stringMessages, boolean executeSelectOnRefresh) {
        this(all, filtered, drawTextBox, stringMessages, executeSelectOnRefresh, stringMessages.hideElementsWithoutUpdateRights());
    }

    public AbstractFilterablePanel(Iterable<T> all, final ListDataProvider<T> filtered,
            boolean drawTextBox, final StringMessages stringMessages) {
        this(all, filtered, drawTextBox, stringMessages, true);
    }
    
    public AbstractFilterablePanel(Iterable<T> all, final ListDataProvider<T> filtered,
            final StringMessages stringMessages) {
        this(all, filtered, stringMessages, /* selectOnRefresh */ true);
    }
    
    public AbstractFilterablePanel(Iterable<T> all, final ListDataProvider<T> filtered,
            final StringMessages stringMessages, boolean selectOnRefresh) {
        this(all, filtered, /* show default filter text box */ true, stringMessages, selectOnRefresh);
    }

    public AbstractFilterablePanel(Iterable<T> all, ListDataProvider<T> filtered, StringMessages stringMessages,
            String filterCheckboxLabel) {
        this(all, filtered, /* draw text box */ true, stringMessages, /* selectOnRefresh */ true, filterCheckboxLabel);
    }

    private void setAll(Iterable<? extends T> all) {
        this.all.getList().clear();
        if (all != null) {
            for (T t : all) {
                this.all.getList().add(t);
            }
        }
    }
    
    public void addFilter(Filter<T> filterToAdd) {
        filters.add(filterToAdd);
    }

    private void setKeywordsFilterSplitValue(String value) {
        filterer.setKeywords(Util.splitAlongWhitespaceRespectingDoubleQuotedPhrases(value));
    }
    
    private void resetKeywordsFilterSplitValue() {
        setKeywordsFilterSplitValue(getTextBox().getValue());
    }
    
    /**
     * Subclasses must implement this to extract the strings from an object of type <code>T</code> based on which the
     * filter performs its filtering
     * 
     * @param t
     *            the object from which to extract the searchable strings
     * @return the searchable strings
     */
    public abstract Iterable<String> getSearchableStrings(T t);

    /**
     * Updates the set of all objects to be shown in the table and applies the search filter to update the table view.
     */
    public void updateAll(Iterable<? extends T> all) {
        setAll(all);
        filter();
        if (executeSelectOnRefresh && executeSelect) {
            select();
        }
    }
    
    private boolean isSelectParameterSet() {
        return selectExact != null || (select != null && !select.isEmpty());
    }
    
    /**
     * Adds an object and applies the search filter.
     */
    public void add(T object) {
        all.getList().add(object);
        filter();
    }

    /**
     * Adds an object at a certain position and applies the search filter.
     */
    public void add(int index, T object) {
        all.getList().add(index, object);
        filter();
    }

    /**
     * Returns the index of the first occurrence of the specified element in this list, or -1 if this list does not
     * contain the element.
     */
    public int indexOf(T object) {
        return all.getList().indexOf(object);
    }

    /**
     * Removes an object and applies the search filter.
     */
    public void remove(T object) {
        select(object, false);
        all.getList().remove(object);
        filter();
    }

    public void addAll(Iterable<T> objects) {
        Util.addAll(objects, all.getList());
        filter();
    }

    /**
     * Would better be called {@code clear()}, but {@link #clear} is already the method inherited from {@link Panel}...
     * This method removes all entries from this filterable panel. The effect is the same as invoking
     * <code>removeAll(all)</code> with <code>all</code> being a copy of what you get when calling {@link #getAll()}.
     */
    public void removeAll() {
        deselectAll();
        all.getList().clear();
        filter();
    }

    public void removeAll(Iterable<T> objects) {
        objects.forEach(o->select(o, false)); // clear those objects removed from the selection
        Util.removeAll(objects, all.getList());
        filter();
    }

    public void filter() {
        filtered.getList().clear();
        retainElementsInFilteredThatPassFilter();
        Scheduler.get().scheduleFinally(()->{
            filtered.flush();
            sort();
        });
    }

    /**
     * Filters and / or selects table items. Used for URL-parameter filtering.
     * If field {@link #executeSelectOnRefresh executeSelectOnRefresh} is set to true, selection will be executed immediately.
     * otherwise, selection will be postponed to be called the next time the table items are refreshed. 
     * 
     * @param filterParameters defines the parameters for filtering and selection
     */
    public void filterAndSelect(FilterAndSelectParameters filterParameters) {
        if (filterParameters.isAnyParameterSet()) {
            if (filterParameters.getFilterString() != null) {
                search(filterParameters.getFilterString());
            } 
            if (!executeSelectOnRefresh) {
                select(filterParameters.getSelectList(), filterParameters.getSelectExact());
            } else {
                setSelectParameters(filterParameters);
            }
        }
    }
    
    public abstract AbstractCellTable<T> getCellTable();
   
    protected void retainElementsInFilteredThatPassFilter() {
        List<T> filteredElements = new ArrayList<>();
        for (T t : all.getList()) {
            if (matches(t)) {
                filteredElements.add(t);
            }
        }
        Util.addAll(filteredElements, filtered.getList());
    }
    
    private void select() {
        select(select, selectExact);       
    }
    
    /**
     * Executes table item selection.
     * Distinguishes between singleSelection tables and multiselection tables.
     * 
     * @param selections list of selection parameters. Will use same logic as the filter mechanism to find items for selection
     * @param selectExact parameter that is used to select only exact matches in the table.
     * 
     */
    private void select(List<String> selections, String selectExact) {
        if (!isSelectParameterSet()) {
            deselectAll();
        }
        else if (isSingleSelect()) {
            selectSingle(selections, selectExact);
        } else {
            selectMultiple(selections, selectExact);
        }        
        resetSelectParameters();
    }
    
    private void deselectAll() {
        for (T t : all.getList()) {
            select(t, false);      
        }  
    }
    
    private void setSelectParameters(FilterAndSelectParameters filterParameters) {
        this.selectExact = filterParameters.getSelectExact();
        this.select = filterParameters.getSelectList();
        this.executeSelect = true;
    }
    
    private void resetSelectParameters() {
        if (select != null) {
            select.clear();
        }
        selectExact = null;
        executeSelect = false;
    }
    
    /**
     * @return true if select logic for single-selection tables should be used.
     */
    private boolean isSingleSelect() {
        return filtered.getDataDisplays().stream().allMatch(dataDisplay -> 
        dataDisplay.getSelectionModel() instanceof SingleSelectionModel);
    }
    
    /**
     * Selection logic for multi-selection tables.
     * Selects all matches for the given selection parameters.
     * 
     * @param selections list of selection parameters. Will use same logic as the filter mechanism to select items
     * @param selectExact parameter that will be used to find and select exactly matching items in the table
     */
    private void selectMultiple(List<String> selections, String selectExact) {        
        selectionFilter.setKeywords(selectExact == null ? new ArrayList<>() : Arrays.asList(selectExact));        
        for (T t : all.getList()) {
            boolean matchFound = false;
            for (String selection : selections) {
                setKeywordsFilterSplitValue(selection);
                matchFound = matches(t);
                if (matchFound) {
                    break;
                }
            }
            if (selectExact != null) {
                matchFound = matchFound || matchesExactly(t);
            }
            select(t, matchFound);
        } 
        resetKeywordsFilterSplitValue();
    } 
    
    /**
     * Selection logic for single-selection tables.
     * Will not select more than one item in the table. First, it will select an exactly matching table item.
     * If none can be found, the first item matching one of the strings in the selections-list will be selected.
     * 
     * @param selections list of selection parameters. Will use same logic as the filter mechanism to select items
     * @param selectExact parameter that will be used to find and select exactly matching items in the table
     */
    private void selectSingle(List<String> selections, String selectExact) {    
        final boolean selected = singleSelectExact(selectExact);  
        if (!selected) {
            try {
                for (final T t : all.getList()) {
                    for (final String selection : selections) {
                        setKeywordsFilterSplitValue(selection);
                        if (matches(t)) {
                            select(t);
                            return;
                        }
                    }
                } 
            } finally {
                resetKeywordsFilterSplitValue();
            }
        } 
    }  
    
    private void select(T item) {
        select(item, true);
    }
    
    private void select(T item, boolean select) {
        getCellTable().getSelectionModel().setSelected(item, select);
    }
    
    /**
     * Selects exactly matching items 
     * @param selectExact parameter that will be used to find and select exactly matching items in the table
     * @return true if an exactly matching item has been found and selected.
     */
    private boolean singleSelectExact(String selectExact) {
        final boolean result;
        if (selectExact == null) {
            result = false;
        } else {
            selectionFilter.setKeywords(Arrays.asList(selectExact));
            boolean found = false;
            for (T t : all.getList()) {
                if (matchesExactly(t)) {
                    select(t);
                    found = true;
                    break;
                }
            }    
            result = found;
        }
        return result;
    }
    
    private boolean matchesExactly(T t) {       
       return selectionFilter.matchesExactly(t);
    }
    
    private boolean matches(T t) {
        for (Filter<T> filter : filters) {
            if (!filter.matches(t)) {
                return false;
            }
        }
        return true;
    }

    protected void sort() {
        if (getCellTable() != null) {
            ColumnSortEvent.fire(getCellTable(), getCellTable().getColumnSortList());
        }
   }

    public void addDefaultTextBox() {
        add(getTextBox());
        getTextBox().addKeyUpHandler(new KeyUpHandler() {
            @Override
            public void onKeyUp(KeyUpEvent event) {
                resetKeywordsFilterSplitValue();
                filter();
            }
        });
    }

    public void addShowOnlyObjectsWithUpdatePermissionCheckBoxIfNecessary() {
        if (!showOnlyObjectsWithUpdatePermissionCheckBoxAdded) {
            add(getShowOnlyObjectsWithUpdatePermissionCheckBox());
            getShowOnlyObjectsWithUpdatePermissionCheckBox().addClickHandler(e -> filter());
            showOnlyObjectsWithUpdatePermissionCheckBoxAdded = true;
        }
    }

    public void search(String searchString) {
        getTextBox().setText(searchString);
        setKeywordsFilterSplitValue(searchString);
        filter();
    }

    public TextBox getTextBox() {
        return textBox;
    }

    public CheckBox getShowOnlyObjectsWithUpdatePermissionCheckBox() {
        return showOnlyObjectsWithUpdatePermissionCheckBox;
    }

    /**
     * Defines the filter function to apply when the {@link #showOnlyObjectsWithUpdatePermissionCheckBox} checkbox is
     * ticked.
     */
    public void setUpdatePermissionFilterForCheckbox(Function<T, Boolean> filterFunction) {
        this.checkboxFilterForUpdatableObjectsOnly.setFilterToApply(new Filter<T>() {
            @Override
            public boolean matches(T object) {
                return filterFunction.apply(object);
            }

            @Override
            public String getName() {
                return "Update Permission Filter";
            }
        });
        addShowOnlyObjectsWithUpdatePermissionCheckBoxIfNecessary();
    }

    public Iterable<T> getAll() {
        return all.getList();
    }

    /**
     * You can use this method to get the {@link ListDataProvider} that represents the all data structure. On this
     * {@link ListDataProvider} a {@link RefreshableSelectionModel} can be registered. So the selection can also be
     * maintained when the data is filtered.
     * 
     * @return The original all data structure. It's no copy.
     */
    public ListDataProvider<T> getAllListDataProvider() {
        return all;
    }
}