package com.sap.sailing.domain.common.racelog.tracking;

public class NoCorrespondingServiceRegisteredException extends RuntimeException {
    public String type;
    public String serviceInterface;
    private static final long serialVersionUID = -358955216089477585L;
    
    protected NoCorrespondingServiceRegisteredException() {}

    public NoCorrespondingServiceRegisteredException(String message, String type, String serviceInterface) {
        super(message);
        this.type = type;
        this.serviceInterface = serviceInterface;
    }

    public String getDeviceType() {
        return type;
    }

    @Override
    public String toString() {
        return super.toString() + " (Service: " + serviceInterface + ", Type: " + type + ")";
    }
}
