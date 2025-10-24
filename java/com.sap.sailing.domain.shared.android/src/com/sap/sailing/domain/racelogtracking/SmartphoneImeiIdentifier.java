package com.sap.sailing.domain.racelogtracking;

import com.sap.sailing.domain.common.DeviceIdentifier;


public interface SmartphoneImeiIdentifier extends DeviceIdentifier {
    String TYPE = "smartphoneImei";
    
    String getImei();
}
