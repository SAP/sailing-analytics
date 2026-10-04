package com.sap.sse.gwt.client.celltable;

import java.util.List;

import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.gwt.view.client.SingleSelectionModel;

/**
 * This {@link RefreshableSingleSelectionModel} implements the {@link RefreshableSelectionModel} interface. So it
 * register it self as a display on the {@link ListDataProvider} and reacts on the changes of {@link ListDataProvider}.
 * When the {@link ListDataProvider} is changed this {@link RefreshableSingleSelectionModel selectionmodel} will refresh
 * the selection according to the {@link ListDataProvider} changes. To make this class work correct it is very important
 * to set the {@link ListDataProvider}, otherwise it won't work.
 * <p>
 * For more details on the update process read the {@link RefreshableSelectionModel} Javadoc and see the methods
 * {@link RefreshableSingleSelectionModel#refreshSelectionModel(Iterable)} and
 * {@link RefreshableSingleSelectionModel#setRowData(int, List)}.<p>
 * 
 * TODO try to factor out the commonalities with RefreshableMultiSelectionModel into a delegate
 * 
 * @author Lukas Furmanek
 * @param <T>
 *            the type of entries
 */
public class RefreshableSingleSelectionModel<T> extends SingleSelectionModel<T> implements RefreshableSelectionModel<T> {
    private final EntityIdentityComparator<T> comp;
    private boolean dontcheckSelectionState = false;
    
    /**
     * @param comp
     *            {@link EntityIdentityComparator} to compare the identity of the objects
     * @param listDataProvider
     *            {@link ListDataProvider} to add this {@link RefreshableSingleSelectionModel selectionmodel} as an
     *            display on {@link ListDataProvider}
     */
    public RefreshableSingleSelectionModel(final EntityIdentityComparator<T> comp, ListDataProvider<T> listDataProvider) {
        super(comp == null ? null : new ProvidesKey<T>() {
                    @Override
                    public Object getKey(T item) {
                        return new EntityIdentityWrapper<T>(item, comp);
                    }
                });
        this.comp = comp;
        listDataProvider.addDataDisplay(new HasDataAdapter<T>(this, listDataProvider));
    }
    
    /**
     * @return the {@link EntityIdentityComparator} for the {@link RefreshableSingleSelectionModel}. If the
     *         {@link EntityIdentityComparator} is not set this method will return <code>null</code>.
     */
    @Override
    public EntityIdentityComparator<T> getEntityIdentityComparator() {
        return comp;
    }
    
    /**
     * Checks the old selection state of the object. If it was selected before, the old version will be replaced with the
     * new one. In all other cases this method behave same as <code>super.setSelected(T item, boolean selected)</code>.
     * <p>
     * When the {@link EntityIdentityComparator} is null this method also behaves like the <code>super</code> method
     */
    @Override
    public void setSelected(T item, boolean selected) {
        if (getEntityIdentityComparator() == null || dontcheckSelectionState || item == null || getSelectedObject() == null) {
            super.setSelected(item, selected);
        } else {
            if (getEntityIdentityComparator().representSameEntity(getSelectedObject(), item)) {
                super.setSelected(getSelectedObject(), false); // This old version of item will be deleted with the next clear()
            }
            super.setSelected(item, selected);
        }
    }
    
    /**
     * Checks if the currently selected object is also visible.
     * 
     * @param visibleItemList
     *            can be obtained by calling {@link com.google.gwt.user.cellview.client.CellTable#getVisibleItems()}
     *            method.
     * @return <code>true</code> if the list of visible items does not contain the selected item.
     */
    public boolean itemIsSelectedButNotVisible(List<T> visibleItemList) {
       return !visibleItemList.contains(getSelectedObject());
    }

    /**
     * Refreshes the {@link RefreshableSingleSelectionModel} with the <code>newObjects</code>. If the currently selected
     * object {@link EntityIdentityComparator#representSameEntity(Object, Object) represents the same entity} as an
     * object from <code>newObjects</code> it will be reselected. All others are de-selected. That means if a selected
     * object is not contained (based on {@link EntityIdentityComparator#representSameEntity(Object, Object)}) in
     * <code>newObjects</code> the object wouldn't be selected anymore. If this selection model has no
     * {@link EntityIdentityComparator} set, this method will use the {@link #equals(Object)} method to compare. If an
     * object is reselected it will be replaced with the new version of it.
     * <p>
     *
     * When the selection is refreshed this method triggers a
     * {@link SelectionChangeEvent.Handler#onSelectionChange(SelectionChangeEvent) onSelectionChangedEvent} using
     * {@link AbstractSelectionModel#fireEvent(com.google.gwt.event.shared.GwtEvent)}.
     * 
     * @param newObjects
     *            the new objects to refresh the {@link RefreshableSingleSelectionModel selectionmodel}
     */
    @Override
    public void refreshSelectionModel(Iterable<T> newObjects) {
        if (!dontcheckSelectionState) { // avoid endless recursion
            try {
                // avoid a new selection state check in setSelected
                dontcheckSelectionState = true;
                final T selected = getSelectedObject();
                if (selected != null) {
                    for (final T it : newObjects) {
                        if (getEntityIdentityComparator() == null ? selected.equals(it) : getEntityIdentityComparator().representSameEntity(selected, it)) {
                            setSelected(it, true);
                            break;
                        }
                    }
                }
                // elements that were selected before and that don't have a corresponding element in newObjects
                // will just be left alone; they will probably remain in selectedSet, and they were probably not in
                // newObjects because a filter removed them. But when they re-appear, e.g., because the filter is
                // removed, the elements will naturally be selected again.
                SelectionChangeEvent.fire(this);
            } finally {
                dontcheckSelectionState = false;
            }
        }
    }
}