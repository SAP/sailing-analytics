package com.sap.sse.datamining.shared;

import java.io.Serializable;

import com.sap.sse.datamining.shared.impl.GenericGroupKey;

@SuppressWarnings("unused")
public final class SSEDataMiningSerializationDummy implements Serializable {
    private static final long serialVersionUID = 1L;
    
    private GenericGroupKey<String> groupKey;
    
    private SSEDataMiningSerializationDummy() { }
    
}
