package com.sap.sailing.server.trackfiles.common;

public class FormatNotSupportedException extends Exception {

    private static final long serialVersionUID = 9018109746837015286L;

    public FormatNotSupportedException(String e) {
        super(e);
    }

    public FormatNotSupportedException() {
        super();
    }

}
