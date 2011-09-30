package com.sap.sailing.domain.swisstimingadapter.impl;

import com.sap.sailing.domain.swisstimingadapter.Race;

public class RaceImpl implements Race {
    private final String description;
    private final String raceId;
    
    
    public RaceImpl(String description, String raceId) {
        super();
        this.description = description;
        this.raceId = raceId;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getRaceID() {
        return raceId;
    }

}
