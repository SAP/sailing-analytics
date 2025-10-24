package com.sap.sse.util;

public class ParseHttpParameterException extends Exception {
    private static final long serialVersionUID = 7040169741090353200L;
    private final String parameterName;
    
    public ParseHttpParameterException(String parameterName, String message, Throwable ex) {
        super(message, ex);
        this.parameterName = parameterName;
    }

    public ParseHttpParameterException(String parameterName, String message) {
        super(message);
        this.parameterName = parameterName;
    }

    public String getParameterName() {
        return parameterName;
    }    

}
