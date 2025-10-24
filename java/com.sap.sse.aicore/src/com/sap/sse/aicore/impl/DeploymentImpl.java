package com.sap.sse.aicore.impl;

import com.sap.sse.aicore.Deployment;

public class DeploymentImpl implements Deployment {
    private final String id;
    private final String modelName;
    
    public DeploymentImpl(String id, String modelName) {
        super();
        this.id = id;
        this.modelName = modelName;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getModelName() {
        return modelName;
    }
}
