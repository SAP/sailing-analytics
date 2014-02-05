package com.sap.sailing.datamining.test.util;

import java.util.Collection;

import com.sap.sailing.datamining.Dimension;
import com.sap.sailing.datamining.impl.MultiDimensionalGroupingWorker;
import com.sap.sse.datamining.shared.GroupKey;
import com.sap.sse.datamining.shared.impl.GenericGroupKey;

public class OpenGrouper<DataType> extends MultiDimensionalGroupingWorker<DataType, String> {

    public OpenGrouper(Collection<Dimension<DataType, String>> dimensions) {
        super(dimensions);
    }

    @Override
    protected GroupKey createGroupKeyFor(DataType dataEntry, Dimension<DataType, String> dimension) {
        return new GenericGroupKey<String>(dimension.getDimensionValueFrom(dataEntry));
    }
    
    public GroupKey getGroupKey(DataType dataEntry) {
        return getGroupKeyFor(dataEntry);
    }
    
}