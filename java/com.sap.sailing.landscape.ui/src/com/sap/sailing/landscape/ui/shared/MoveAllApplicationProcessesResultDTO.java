package com.sap.sailing.landscape.ui.shared;

import java.io.Serializable;

/** Identifies the destination host created while moving all application processes away from another host. */
public final class MoveAllApplicationProcessesResultDTO implements Serializable {
    private static final long serialVersionUID = -3986005892785249498L;
    private final String newHostId;

    public MoveAllApplicationProcessesResultDTO(final String newHostId) {
        this.newHostId = newHostId;
    }

    public String getNewHostId() {
        return newHostId;
    }
}
