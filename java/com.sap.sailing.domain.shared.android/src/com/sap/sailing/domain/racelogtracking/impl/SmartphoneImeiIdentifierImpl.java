package com.sap.sailing.domain.racelogtracking.impl;

import com.sap.sailing.domain.racelogtracking.SmartphoneImeiIdentifier;


public class SmartphoneImeiIdentifierImpl implements SmartphoneImeiIdentifier {
    private static final long serialVersionUID = -1830014229310181702L;

    private final String imei;

    public SmartphoneImeiIdentifierImpl(String imei) {
        this.imei = imei;
    }

    public String getImei() {
        return imei;
    }

    @Override
    public String getIdentifierType() {
        return TYPE;
    }

    @Override
    public String toString() {
        return "IMEI " + imei;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof SmartphoneImeiIdentifierImpl) {
            return ((SmartphoneImeiIdentifierImpl) obj).imei.equals(imei);
        }
        return false;
    }
    
    @Override
    public int hashCode() {
        return imei.hashCode();
    }
    
    @Override
    public String getStringRepresentation() {
        return imei;
    }
}
