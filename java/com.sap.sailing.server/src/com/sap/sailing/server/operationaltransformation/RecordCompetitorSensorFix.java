package com.sap.sailing.server.operationaltransformation;

import java.io.Serializable;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.tracking.SensorFix;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class RecordCompetitorSensorFix extends AbstractRaceOperation<Void> {
    private static final long serialVersionUID = -7092704633177037511L;
    private final Serializable competitorID;
    private final SensorFix fix;
    private final String trackName;
    
    public RecordCompetitorSensorFix(RegattaAndRaceIdentifier raceIdentifier, Competitor competitor, String trackName, SensorFix fix) {
        super(raceIdentifier);
        this.trackName = trackName;
        this.competitorID = competitor.getId();
        this.fix = fix;
    }

    /**
     * Operations of this type can be run in parallel to other operations; subsequent operations do not have to wait
     * for this operation's completion.
     */
    @Override
    public boolean requiresSynchronousExecution() {
        return false;
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) throws Exception {
        DynamicTrackedRace trackedRace = (DynamicTrackedRace) toState.getTrackedRace(getRaceIdentifier());
        Competitor competitor = trackedRace.getRace().getCompetitorById(competitorID);
        trackedRace.recordSensorFix(competitor, trackName, fix, /* onlyWhenInTrackingTimeInterval */ false); // record the fix in any case
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
