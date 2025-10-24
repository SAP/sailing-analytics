package com.sap.sse.aicore;

import com.sap.sse.common.WithID;

public interface Deployment extends WithID {
    @Override
    String getId();
    
    String getModelName();
}
