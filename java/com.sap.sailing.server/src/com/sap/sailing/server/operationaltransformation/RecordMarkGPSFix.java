package com.sap.sailing.server.operationaltransformation;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public abstract class RecordMarkGPSFix extends AbstractRaceOperation<Void> {
    private static final long serialVersionUID = -2149936580623244814L;
    private final GPSFix fix;
    
    public RecordMarkGPSFix(RegattaAndRaceIdentifier raceIdentifier, GPSFix fix) {
        super(raceIdentifier);
        this.fix = fix;
    }

    protected GPSFix getFix() {
        return fix;
    }

    protected DynamicTrackedRace getTrackedRace(RacingEventService toState) {
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) toState.getTrackedRace(getRaceIdentifier());
        return trackedRace;
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<Void> serverOp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<Void> clientOp) {
        // TODO Auto-generated method stub
        return null;
    }
}
