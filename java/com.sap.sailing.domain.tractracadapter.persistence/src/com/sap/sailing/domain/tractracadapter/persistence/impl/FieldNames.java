package com.sap.sailing.domain.tractracadapter.persistence.impl;

public enum FieldNames {
    // TracTrac configuration parameters:
    TT_CONFIG_CREATOR_NAME,
    TT_CONFIG_NAME,
    TT_CONFIG_JSON_URL,
    TT_CONFIG_LIVE_DATA_URI,
    TT_CONFIG_STORED_DATA_URI,
    TT_CONFIG_COURSE_DESIGN_UPDATE_URI,
    @Deprecated
    TT_CONFIG_TRACTRAC_USERNAME,
    
    @Deprecated
    TT_CONFIG_TRACTRAC_PASSWORD,
    
    TT_CONFIG_TRACTRAC_API_TOKEN;
}
