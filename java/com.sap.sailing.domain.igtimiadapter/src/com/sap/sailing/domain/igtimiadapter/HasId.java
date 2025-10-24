package com.sap.sailing.domain.igtimiadapter;

import java.io.Serializable;

public interface HasId extends Serializable {
    /**
     * 0 is not a valid ID but is used as a marker to ask for an auto-generated ID
     */
    long getId();
}
