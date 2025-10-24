package com.sap.sse.gwt.dispatch.shared.commands;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import com.sap.sse.gwt.shared.DTO;

public class StringsResult implements DTO, Result {

    private ArrayList<String> values = new ArrayList<>();

    public StringsResult() {
    }

    public StringsResult(Collection<String> values) {
        this.values.addAll(values);
    }

    public void addValue(String value) {
        if (value != null) {
            this.values.add(value);
        }
    }

    public List<String> getValues() {
        return values;
    }
    
    public boolean isEmpty() {
        return this.values.isEmpty();
    }

}
