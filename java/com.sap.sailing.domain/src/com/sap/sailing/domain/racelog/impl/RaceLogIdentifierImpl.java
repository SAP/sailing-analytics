package com.sap.sailing.domain.racelog.impl;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Triple;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogIdentifierTemplate;

public class RaceLogIdentifierImpl implements RaceLogIdentifier {
    private static final long serialVersionUID = -1933109112840405951L;
    
    private final String raceColumnName;
    private final String fleetName;
    
    private final RaceLogIdentifierTemplate template;
    
    public RaceLogIdentifierImpl(RaceLogIdentifierTemplate template, String raceColumnName, Fleet fleet) {
        this.template = template;
        this.raceColumnName = raceColumnName;
        this.fleetName = fleet.getName();
    }

    @Override
    public Triple<String, String, String> getIdentifier() {
        return new Util.Triple<String, String, String>(template.getParentObjectName(), raceColumnName, fleetName);
    }

    @Override
    public RaceLogIdentifierTemplate getTemplate() {
        return template;
    }

    @Override
    public String getRaceColumnName() {
        return raceColumnName;
    }

    @Override
    public String getFleetName() {
        return fleetName;
    }

}
