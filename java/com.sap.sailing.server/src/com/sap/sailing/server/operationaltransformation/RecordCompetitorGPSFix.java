package com.sap.sailing.server.operationaltransformation;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class RecordCompetitorGPSFix extends AbstractRaceOperation<Void> {
    private static final long serialVersionUID = 5847067037829132465L;
    private final Competitor competitor;
    private final GPSFixMoving gpsFix;
    
    public RecordCompetitorGPSFix(RegattaAndRaceIdentifier raceIdentifier, Competitor competitor, GPSFixMoving gpsFix) {
        super(raceIdentifier);
        this.competitor = competitor;
        this.gpsFix = gpsFix;
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) throws Exception {
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) toState.getTrackedRace(getRaceIdentifier());
        trackedRace.recordFix(competitor, gpsFix);
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
