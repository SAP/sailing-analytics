package com.sap.sailing.server.operationaltransformation;

import java.util.UUID;

import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Position;

/** Updates course-area attributes while preserving their IDs and existing references. */
public class UpdateCourseAreas extends AbstractRacingEventServiceOperation<Void> {
    private static final long serialVersionUID = 2352147822083355347L;
    private final UUID eventId;
    private final UUID[] courseAreaIds;
    private final String[] courseAreaNames;
    private final Position[] centerPositions;
    private final Distance[] radiuses;

    public UpdateCourseAreas(final UUID eventId, final UUID[] courseAreaIds, final String[] courseAreaNames,
            final Position[] centerPositions, final Distance[] radiuses) {
        this.eventId = eventId;
        this.courseAreaIds = courseAreaIds.clone();
        this.courseAreaNames = courseAreaNames.clone();
        this.centerPositions = centerPositions.clone();
        this.radiuses = radiuses.clone();
    }

    @Override
    public Void internalApplyTo(final RacingEventService toState) throws Exception {
        toState.updateCourseAreasWithoutReplication(eventId, courseAreaIds, courseAreaNames, centerPositions, radiuses);
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(final RacingEventServiceOperation<?> serverOp) {
        return serverOp;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(final RacingEventServiceOperation<?> clientOp) {
        return clientOp;
    }
}
