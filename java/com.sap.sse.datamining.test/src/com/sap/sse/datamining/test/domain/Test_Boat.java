package com.sap.sse.datamining.test.domain;

import com.sap.sse.datamining.annotations.Dimension;

public interface Test_Boat {
    
    @Dimension(messageKey="SailID")
    public String getSailID();

}
