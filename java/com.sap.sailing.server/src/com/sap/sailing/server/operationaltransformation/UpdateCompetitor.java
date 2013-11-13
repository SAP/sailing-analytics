package com.sap.sailing.server.operationaltransformation;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class UpdateCompetitor extends AbstractRacingEventServiceOperation<Competitor> {
    private static final long serialVersionUID = 1172181354320184263L;
    private final String idAsString;
    private final String newName;
    private final String newSailId;
    private final Nationality newNationality;
    
    /**
     * @param idAsString Identified the competitor to update
     * @param newNationality if <code>null</code>, the competitor obtains the "NONE" nationality, usually represented by a white flag
     */
    public UpdateCompetitor(String idAsString, String newName, String newSailId, Nationality newNationality) {
        super();
        this.idAsString = idAsString;
        this.newName = newName;
        this.newSailId = newSailId;
        this.newNationality = newNationality;
    }

    @Override
    public Competitor internalApplyTo(RacingEventService toState) throws Exception {
        return toState.getBaseDomainFactory().getCompetitorStore().updateCompetitor(idAsString, newName, newSailId, newNationality);
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<?> serverOp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<?> clientOp) {
        // TODO Auto-generated method stub
        return null;
    }

}
