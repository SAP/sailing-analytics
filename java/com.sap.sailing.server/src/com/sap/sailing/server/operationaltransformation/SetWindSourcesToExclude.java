package com.sap.sailing.server.operationaltransformation;

import com.sap.sailing.domain.common.EventAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class SetWindSourcesToExclude extends AbstractRaceOperation<Void> {
    private static final long serialVersionUID = 7639288885720509529L;
    private final Iterable<WindSource> windSourcesToExclude;
    
    public SetWindSourcesToExclude(EventAndRaceIdentifier raceIdentifier, Iterable<WindSource> windSourcesToExclude) {
        super(raceIdentifier);
        this.windSourcesToExclude = windSourcesToExclude;
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

    @Override
    public Void internalApplyTo(RacingEventService toState) {
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) toState.getExistingTrackedRace(getRaceIdentifier());
        if (trackedRace != null) {
            trackedRace.setWindSourcesToExclude(windSourcesToExclude);
        }
        return null;
    }

}
