package com.sap.sailing.datamining.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.sap.sailing.datamining.ExtractionWorker;
import com.sap.sse.datamining.shared.GroupKey;

public abstract class AbstractExtractionWorker<DataType, ExtractedType> extends AbstractComponentWorker<Map<GroupKey, Collection<ExtractedType>>>
                                                                        implements ExtractionWorker<DataType, ExtractedType> {

    private Map<GroupKey, Collection<DataType>> data;

    @Override
    public void setDataToExtractFrom(Map<GroupKey, Collection<DataType>> data) {
        this.data = data;
    }

    @Override
    protected Map<GroupKey, Collection<ExtractedType>> doWork() {
        Map<GroupKey, Collection<ExtractedType>> extractedData = new HashMap<GroupKey, Collection<ExtractedType>>();
        for (Entry<GroupKey, Collection<DataType>> dataEntry : data.entrySet()) {
            GroupKey key = dataEntry.getKey();
            if (!extractedData.containsKey(key)) {
                extractedData.put(key, new ArrayList<ExtractedType>());
            };
            
            for (DataType dateElement : dataEntry.getValue()) {
                ExtractedType extractedDataElement = extract(dateElement);
                if (extractedDataElement != null) {
                    extractedData.get(key).add(extractedDataElement);
                }
            }
        }
        return extractedData;
    }

    protected abstract ExtractedType extract(DataType dataEntry);

}
