package com.sap.sailing.gwt.ui.datamining.selection;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.google.gwt.user.cellview.client.DataGrid;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sailing.datamining.shared.SharedDimension;
import com.sap.sailing.gwt.ui.client.shared.panels.AbstractFilterablePanel;

public abstract class SelectionTable<ContentType, ValueType> extends FlowPanel {
    
    private SharedDimension dimension;
    private Collection<ContentType> allData;

    private AbstractFilterablePanel<ContentType> filterPanel;
    private DataGrid<ContentType> table;
    private MultiSelectionModel<ContentType> selectionModel;
    private ListDataProvider<ContentType> dataProvider;
    
    public SelectionTable(String title, SharedDimension dimension) {
        this.dimension = dimension;
        allData = new ArrayList<ContentType>();
        
        table = new DataGrid<ContentType>();
        table.setAutoHeaderRefreshDisabled(true);
        table.setAutoFooterRefreshDisabled(true);
        
        table.addColumn(new TextColumn<ContentType>() {
            @Override
            public String getValue(ContentType content) {
                return SelectionTable.this.getValueAsString(content);
            }
        }, title);
        selectionModel = new MultiSelectionModel<ContentType>();
        table.setSelectionModel(selectionModel);
        
        dataProvider = new ListDataProvider<ContentType>(new ProvidesKey<ContentType>() {
            @Override
            public Object getKey(ContentType item) {
                return getValueAsString(item);
            }
        });
        dataProvider.addDataDisplay(table);
        
        filterPanel = new AbstractFilterablePanel<ContentType>(allData, table, dataProvider) {
            @Override
            public Iterable<String> getSearchableStrings(ContentType content) {
                Collection<String> searchableStrings = new ArrayList<String>();
                searchableStrings.add(getValueAsString(content));
                return searchableStrings;
            }
        };
        filterPanel.setWidth("100%");
        filterPanel.getTextBox().setWidth("90%");
        
        add(filterPanel);
        add(table);
    }

    public SharedDimension getDimension() {
        return dimension;
    }
    
    /**
     * Replaces the current content of the table with the new one, while preserving the current selection.<br />
     * Returns <code>true</code>, if:
     * <ul>
     *   <li>Previously selected elements have been removed</li>
     *   <li>Nothing was selected and elements have been added or removed</li>
     * </ul>
     * 
     * @param newContent The new content.
     * @return <code>true</code>, if query related elements has been changed.
     */
    @SuppressWarnings("unchecked") //You can't use instanceof for generic type parameters
    public boolean updateContent(Collection<?> newContent) {
        Collection<ContentType> specificNewContent = null;
        try {
            specificNewContent = (Collection<ContentType>) newContent;
        } catch (ClassCastException e) {
            return false;
        }
        
        Collection<ContentType> oldContent = new ArrayList<ContentType>(dataProvider.getList());
        Collection<ContentType> selection = selectionModel.getSelectedSet();
        
        dataProvider.getList().clear();
        dataProvider.getList().addAll(specificNewContent);
        allData.clear();
        allData.addAll(specificNewContent);
        filterPanel.updateAll(allData);
        
        return selection.isEmpty() ? elementsHaveBeenAddedOrRemoved(oldContent, specificNewContent) : selectedElementsHaveBeenRemoved(selection, specificNewContent);
    }
    
    private boolean selectedElementsHaveBeenRemoved(Collection<ContentType> selection, Collection<ContentType> newContent) {
        Set<Object> selectionKeys = getKeysFor(selection);
        Set<Object> newContentKeys = getKeysFor(newContent);
        return !newContentKeys.containsAll(selectionKeys);
    }

    private boolean elementsHaveBeenAddedOrRemoved(Collection<ContentType> oldContent, Collection<ContentType> newContent) {
        if (oldContent.size() != newContent.size()) {
            return true;
        }
        
        Set<Object> oldContentKeys = getKeysFor(oldContent);
        Set<Object> newContentKeys = getKeysFor(newContent);
        return !newContentKeys.containsAll(oldContentKeys);
    }

    private Set<Object> getKeysFor(Collection<ContentType> content) {
        Set<Object> newContentKeys = new HashSet<Object>();
        for (ContentType element : content) {
            newContentKeys.add(dataProvider.getKey(element));
        }
        return newContentKeys;
    }

    public Collection<?> getSelectionAsValues() {
        Collection<ValueType> selectionAsValues = new HashSet<ValueType>();
        for (ContentType content : selectionModel.getSelectedSet()) {
            selectionAsValues.add(getValue(content));
        }
        return selectionAsValues;
    }

    public void setSelection(Iterable<?> elements) {
        clearSelection();
        try {
            @SuppressWarnings("unchecked") //You can't use instanceof for generic type parameters
            Iterable<ContentType> elementsMatchingContent = (Iterable<ContentType>) elements;
            for (ContentType element : elementsMatchingContent) {
                selectionModel.setSelected(element, true);
            }
        } catch (ClassCastException e) {/*Ignore the elements, because they don't match the ContentType*/}
    }
    
    public void clearSelection() {
        selectionModel.clear();
    }

    public abstract ValueType getValue(ContentType content);
    
    public String getValueAsString(ContentType content) {
        return getValue(content).toString();
    }
    
    public void addSelectionChangeHandler(SelectionChangeEvent.Handler handler) {
        selectionModel.addSelectionChangeHandler(handler);
    }
    
    @Override
    public void setWidth(String width) {
        super.setWidth(width);
        table.setWidth(width);
    }
    
    @Override
    public void setHeight(String height) {
        super.setHeight(height);
        table.setHeight(height);
    }

}
