package com.sap.sailing.server.operationaltransformation;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;

public class StopTrackingRace extends AbstractRaceOperation<Void> {
    private static final long serialVersionUID = -6074168525544219596L;

    public StopTrackingRace(RegattaAndRaceIdentifier raceIdentifier) {
        super(raceIdentifier);
    }

    /**
     * {@link #internalApplyTo(RacingEventService)} does not replicate the effects
     */
    @Override
    public boolean isRequiresExplicitTransitiveReplication() {
        return true;
    }
    
    @Override
    public Void internalApplyTo(RacingEventService toState) throws Exception {
        final Regatta regatta = toState.getRegatta(getRaceIdentifier());
        if (regatta != null) {
            final RaceDefinition r = toState.getRace(getRaceIdentifier());
            if (r != null) {
                toState.stopTracking(regatta, r);
            }
        }
        return null;
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
