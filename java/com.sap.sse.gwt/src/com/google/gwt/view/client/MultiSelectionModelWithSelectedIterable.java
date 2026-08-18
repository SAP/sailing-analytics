package com.google.gwt.view.client;

import java.util.ArrayList;
import java.util.Map;

public class MultiSelectionModelWithSelectedIterable<T> extends MultiSelectionModel<T> {
    public MultiSelectionModelWithSelectedIterable() {
        super();
    }

    public MultiSelectionModelWithSelectedIterable(ProvidesKey<T> keyProvider, Map<Object, T> selectedSet,
            Map<Object, SelectionChange<T>> selectionChanges) {
        super(keyProvider, selectedSet, selectionChanges);
    }

    public MultiSelectionModelWithSelectedIterable(ProvidesKey<T> keyProvider) {
        super(keyProvider);
    }

    /**
     * Produces a snapshot of the elements currently selected. In contrast to {@link #getSelectedSet()},
     * this method does not need to hash and compare all elements using {@link Object#hashCode()} and
     * {@link Object#equals(Object)} which may be expensive methods. Iteration order is undefined.
     */
    public Iterable<T> getSelectedElements() {
        return new ArrayList<>(selectedSet.values());
    }
    
    public boolean isEmpty() {
        return selectedSet.isEmpty();
    }
}
