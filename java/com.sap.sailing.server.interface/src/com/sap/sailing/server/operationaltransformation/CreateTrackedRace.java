package com.sap.sailing.server.operationaltransformation;

import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.shared.tracking.TrackingConnectorInfo;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRegatta;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.domain.tracking.impl.EmptyWindStore;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;

/**
 * Creates a tracked race for a race identifier by a {@link RaceIdentifier}. The operation assumes that the
 * {@link RaceDefinition}, therefore the {@link Regatta} as well as the {@link TrackedRegatta} into which the
 * new {@link TrackedRace} will be composed already exist and that the {@link TrackedRace} does not yet
 * exist. The operation is intended only for execution on a replica, not on the master.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class CreateTrackedRace extends AbstractRaceOperation<DynamicTrackedRace> {
    private static final long serialVersionUID = 7164691177800524484L;
    private final long millisecondsOverWhichToAverageWind;
    private final long millisecondsOverWhichToAverageSpeed;
    private final long delayToLiveInMillis;
    private final TrackingConnectorInfo trackingConnectorInfo;
    
    /**
     * If a {@link WindStore} is provided to this command, it will be used for the construction of the tracked race.
     * However, after de-serialization, the wind store will always be <code>null</code>, causing the use of an
     * {@link EmptyWindStore}.
     */
    private transient final WindStore windStore;
    
    /**
     * @param windStore
     *            if <code>null</code>, an {@link EmptyWindStore} will be used. Note that the {@link #windStore} field
     *            won't be serialized. A receiver of this operation will therefore always use an {@link EmptyWindStore}.
     */
    public CreateTrackedRace(RegattaAndRaceIdentifier raceIdentifier, WindStore windStore, long delayToLiveInMillis,
            long millisecondsOverWhichToAverageWind, long millisecondsOverWhichToAverageSpeed,
            TrackingConnectorInfo trackingConnectorInfo) {
        super(raceIdentifier);
        this.windStore = windStore;
        this.delayToLiveInMillis = delayToLiveInMillis;
        this.millisecondsOverWhichToAverageWind = millisecondsOverWhichToAverageWind;
        this.millisecondsOverWhichToAverageSpeed = millisecondsOverWhichToAverageSpeed;
        this.trackingConnectorInfo = trackingConnectorInfo;
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
    public DynamicTrackedRace internalApplyTo(RacingEventService toState) {
        return toState.createTrackedRace(getRaceIdentifier(), windStore == null ? EmptyWindStore.INSTANCE : windStore,
                delayToLiveInMillis, millisecondsOverWhichToAverageWind, millisecondsOverWhichToAverageSpeed,
                /* useMarkPassingCalculator */ false, trackingConnectorInfo);
        // no separate mark passing calculations in replica;
        // Mark passings are computed on master and are replicated separately.
        // See UpdateMarkPassings
    }
}
