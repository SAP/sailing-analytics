package com.sap.sailing.domain.racelog.impl;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.racelog.RaceLogIdentifier;
import com.sap.sailing.domain.racelog.RaceLogIdentifierTemplate;
import com.sap.sailing.domain.racelog.RaceLogIdentifierTemplateResolver;

public class RaceLogOnRegattaIdentifier implements RaceLogIdentifierTemplate {
    private static final long serialVersionUID = 516941514546654375L;
    
    private final String regattaName;
    private final String raceColumnName;
    
    public RaceLogOnRegattaIdentifier(Regatta regatta, String raceColumnName) {
        this.regattaName = regatta.getName();
        this.raceColumnName = raceColumnName;
    }
    
    @Override
    public RaceLogIdentifier compileRaceLogIdentifier(Fleet fleet) {
        return new RaceLogIdentifierImpl(this, raceColumnName, fleet);
    }

    @Override
    public void resolve(RaceLogIdentifierTemplateResolver resolver) {
        resolver.resolveOnRegattaIdentifierAndReplicate(this);
    }

    @Override
    public String getParentObjectName() {
        return regattaName;
    }

    @Override
    public String getRaceColumnName() {
        return raceColumnName;
    }

}
