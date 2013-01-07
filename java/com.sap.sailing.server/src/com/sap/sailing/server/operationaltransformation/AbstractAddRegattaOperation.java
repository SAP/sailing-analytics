package com.sap.sailing.server.operationaltransformation;

import java.io.Serializable;

import com.sap.sailing.domain.base.Regatta;

public abstract class AbstractAddRegattaOperation extends AbstractRacingEventServiceOperation<Regatta> {
    private static final long serialVersionUID = 4596134166642095364L;
    private final String baseEventName;
    private final String boatClassName;
    private final Serializable id;
    
    public AbstractAddRegattaOperation(String regattaName, String boatClassName, Serializable id) {
        super();
        this.baseEventName = regattaName;
        this.boatClassName = boatClassName;
        this.id = id;
    }

    protected String getBaseEventName() {
        return baseEventName;
    }

    protected String getBoatClassName() {
        return boatClassName;
    }
    
    protected Serializable getId() {
        return id;
    }
}
