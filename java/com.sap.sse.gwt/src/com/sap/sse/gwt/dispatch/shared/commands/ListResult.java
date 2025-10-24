package com.sap.sse.gwt.dispatch.shared.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sap.sse.gwt.shared.DTO;

public class ListResult<T extends DTO> implements CollectionResult<T> {
    private ArrayList<T> values = new ArrayList<>();
    
    public ListResult() {
    }

    public ListResult(Collection<T> values) {
        super();
        this.values.addAll(values);
    }
    
    public void addValue(T value) {
        if (value != null) {
            this.values.add(value);
        }
    }
    
    public List<T> getValues() {
        return values;
    }
    
    public boolean isEmpty() {
        return values.isEmpty();
    }
}
