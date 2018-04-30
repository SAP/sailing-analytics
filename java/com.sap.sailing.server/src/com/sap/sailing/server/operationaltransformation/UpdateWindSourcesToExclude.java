package com.sap.sailing.server.operationaltransformation;

import java.util.logging.Logger;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class UpdateWindSourcesToExclude extends AbstractRaceOperation<Void> {
    private static final long serialVersionUID = 5599076261746041948L;
    private static final Logger logger = Logger.getLogger(UpdateWindSourcesToExclude.class.getName());
    private final Iterable<? extends WindSource> windSourcesToExclude;
    
    public UpdateWindSourcesToExclude(RegattaAndRaceIdentifier raceIdentifier, Iterable<? extends WindSource> windSourcesToExclude) {
        super(raceIdentifier);
        this.windSourcesToExclude = windSourcesToExclude;
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) throws Exception {
        // it's fair to not wait for the tracked race to arrive here because we're receiving a replication operation
        // and the synchronous race-creating operation must have been processed synchronously before this operation
        // could even have been received
        final DynamicTrackedRace trackedRace = toState.getExistingTrackedRace(getRaceIdentifier());
        if (trackedRace != null) {
            trackedRace.setWindSourcesToExclude(windSourcesToExclude);
        } else {
            logger.warning("Tracked race for "+getRaceIdentifier()+" has disappeared");
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
