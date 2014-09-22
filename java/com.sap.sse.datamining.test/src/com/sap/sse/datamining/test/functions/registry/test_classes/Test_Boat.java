package com.sap.sse.datamining.test.functions.registry.test_classes;

import com.sap.sse.datamining.shared.annotations.Dimension;

public interface Test_Boat {
    
    @Dimension(messageKey="SailID")
    public String getSailID();

}
