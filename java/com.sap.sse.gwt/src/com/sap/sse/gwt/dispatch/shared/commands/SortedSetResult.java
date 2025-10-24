package com.sap.sse.gwt.dispatch.shared.commands;

import java.util.Collection;
import java.util.Set;
import java.util.TreeSet;

import com.sap.sse.gwt.shared.DTO;

public class SortedSetResult<T extends DTO> implements CollectionResult<T> {
    private TreeSet<T> values = new TreeSet<>();
    
    public SortedSetResult() {
    }

    public SortedSetResult(Collection<T> values) {
        this.values.addAll(values);
    }
    
    /**
     * Adds the {@code value} to this result if {@code value} is not {@code null}. If an object equal
     * to {@code value} is already contained in this result, no action is taken.
     */
    public void addValue(T value) {
        if (value != null) {
            this.values.add(value);
        }
    }
    
    public Set<T> getValues() {
        return values;
    }
    
    public boolean isEmpty() {
        return values.isEmpty();
    }
}
