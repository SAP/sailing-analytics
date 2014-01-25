package com.sap.sailing.datamining.test.function.test_classes;

import com.sap.sailing.datamining.annotations.Dimension;

public class DataTypeWithContextProcessor {
    
    @Dimension("regattaAndRaceName")
    public String getRegattaAndRaceName(DataTypeWithContext dataEntry) {
        return dataEntry.getRegattaName() + " - " + dataEntry.getRaceName();
    }

}
