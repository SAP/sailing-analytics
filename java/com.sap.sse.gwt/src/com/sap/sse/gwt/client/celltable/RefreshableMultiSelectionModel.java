package com.sap.sse.gwt.client.celltable;

import java.util.List;

import com.google.gwt.view.client.ListDataProvider;
import com.google.gwt.view.client.MultiSelectionModelWithSelectedIterable;
import com.google.gwt.view.client.ProvidesKey;
import com.google.gwt.view.client.SelectionChangeEvent;
import com.sap.sse.common.Util;

/**
 * This {@link RefreshableMultiSelectionModel} implements the {@link RefreshableSelectionModel} interface. So it
 * register it self as a display on the {@link ListDataProvider} and reacts on the changes of {@link ListDataProvider}.
 * When the {@link ListDataProvider} is changed this {@link RefreshableMultiSelectionModel selection model} will refresh
 * the selection according to the {@link ListDataProvider} changes. To make this class work correct it is very important
 * to set the {@link ListDataProvider}, otherwise it won't work.
 * <p>
 * For more details on the update process read the {@link RefreshableSelectionModel} Javadoc and see the methods
 * {@link RefreshableMultiSelectionModel#refreshSelectionModel(Iterable)} and
 * {@link RefreshableMultiSelectionModel#setRowData(int, List)}.<p>
 * 
 * TODO try to factor out the commonalities with RefreshableSingleSelectionModel into a delegate
 * 
 * @author Lukas Furmanek
 * @param <T>
 *            the type of entries
 */
public class RefreshableMultiSelectionModel<T> extends MultiSelectionModelWithSelectedIterable<T>
        implements RefreshableSelectionModel<T> {
    final EntityIdentityComparator<T> comp;
    private boolean dontCheckSelectionState = false;
    private final ListDataProvider<T> listDataProvider;

    /**
     * @param comp
     *            {@link EntityIdentityComparator} to compare the identity of the objects; if not <code>null</code>, this will
     *            also be used to determine the selection "keys" using a {@link ProvidesKey} implementation based on this
     *            comparator.
     * @param listDataProvider
     *            {@link ListDataProvider} to add this {@link RefreshableSingleSelectionModel selectionmodel} as an
     *            display on {@link ListDataProvider}
     */
    public RefreshableMultiSelectionModel(final EntityIdentityComparator<T> comp, ListDataProvider<T> listDataProvider) {
        super(/* keyProvider */ comp == null ? null : new ProvidesKey<T>() {
            @Override
            public Object getKey(T item) {
                return new EntityIdentityWrapper<T>(item, comp);
            }
        });
        this.comp = comp;
        this.listDataProvider = listDataProvider;
        this.listDataProvider.addDataDisplay(new HasDataAdapter<T>(this, listDataProvider));
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
     * Checks if all items that are currently selected also appear in the visible items list.
     * 
     * @param visibleItemList
     *            can be obtained by calling {@link com.google.gwt.user.cellview.client.CellTable#getVisibleItems()}
     *            method.
     * @return <code>true</code> if the list of visible items does not contain every item of the current selected item set.
     */
    public boolean itemIsSelectedButNotVisible(List<T> visibleItemList) {
        for (T item : getSelectedElements()) {
            if (!visibleItemList.contains(item)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks the old selection state of the object. If it was selected before, the old version will be replaced with
     * the new one. In all other cases this method behave same as
     * <code>super.setSelected(T item, boolean selected)</code>.
     * <p>
     * When the {@link EntityIdentityComparator} is null this method also behaves like the <code>super</code> method
     */
    @Override
    public void setSelected(T item, boolean selected) {
        if (getEntityIdentityComparator() == null || dontCheckSelectionState || item == null || Util.isEmpty(getSelectedElements())) {
            super.setSelected(item, selected);
        } else {
            T wasSelectedBefore = null;
            for (T it : getSelectedElements()) {
                if (getEntityIdentityComparator().representSameEntity(it, item)) {
                    wasSelectedBefore = it;
                    break;
                }
            }
            if (wasSelectedBefore != null) {
                super.setSelected(wasSelectedBefore, false);
                super.setSelected(item, selected);
            } else {
                super.setSelected(item, selected);
            }
        }
    }

    /**
     * Refreshes the {@link RefreshableMultiSelectionModel} with the <code>newObjects</code>. All objects from the
     * current selection that {@link EntityIdentityComparator#representSameEntity(Object, Object) represent the same
     * entity} as an object from <code>newObjects</code> will be reselected. All others are de-selected. That means if a
     * selected object is not contained in <code>newObjects</code> the object wouldn't be selected anymore. If this
     * selection model has no {@link EntityIdentityComparator} set, this method will use the {@link #equals(Object)}
     * method to compare. If an object is reselected it will be replaced with the new version of it.
     * <p>
     *
     * When the selection is refreshed this method triggers a
     * {@link SelectionChangeEvent.Handler#onSelectionChange(SelectionChangeEvent) onSelectionChangedEvent} using
     * {@link AbstractSelectionModel#fireEvent(com.google.gwt.event.shared.GwtEvent)}.
     * 
     * @param newObjects
     *            the new objects to refresh the {@link RefreshableMultiSelectionModel selection model}. When this
     *            selection model is used together with {@link com.sap.sse.gwt.client.panels.AbstractFilterablePanel},
     *            this must be the <em>unfiltered</em> list (i.e. the {@code all} {@link ListDataProvider}, not the
     *            {@code filtered} one), so that items hidden by the current filter are not incorrectly deselected.
     *            {@link HasDataAdapter} ensures this by passing {@code listDataProvider.getList()} — which is the
     *            {@code all} provider registered in the {@code AbstractFilterablePanel} constructor.
     */
    @Override
    public void refreshSelectionModel(Iterable<T> newObjects) {
        if (!dontCheckSelectionState) { // avoid endless recursions
            dontCheckSelectionState = true;
            try {
                if (!isEmpty()) {
                    for (T it : newObjects) {
                        if (isSelected(it)) {
                            setSelected(it, true); // this updates matching elements in the selection model
                        }
                    }
                    // Deselect items that are no longer present in newObjects (e.g. because they were deleted).
                    // newObjects comes from getAllListDataProvider() (the unfiltered list), so absence here means
                    // true deletion, not just a filter hiding the item.
                    // getSelectedElements() already returns a snapshot copy, so no additional copy is needed here.
                    for (final T selected : getSelectedElements()) {
                        boolean foundInNew = false;
                        for (final T candidate : newObjects) {
                            if (comp != null ? comp.representSameEntity(selected, candidate) : selected.equals(candidate)) {
                                foundInNew = true;
                                break;
                            }
                        }
                        if (!foundInNew) {
                            super.setSelected(selected, false);
                        }
                    }
                    SelectionChangeEvent.fire(this);
                }
            } finally {
                dontCheckSelectionState = false;
            }
        }
    }
}