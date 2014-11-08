package com.sap.sailing.server.operationaltransformation;

import java.util.UUID;

import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class SetDataImportDeleteProgressFromMapTimer extends AbstractRacingEventServiceOperation<Void> {

    private static final long serialVersionUID = 2134445141548315303L;
    private UUID importOperationId;

    public SetDataImportDeleteProgressFromMapTimer(UUID importOperationId) {
        this.importOperationId = importOperationId;
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) throws Exception {
        toState.setDataImportDeleteProgressFromMapTimerWithoutReplication(importOperationId);
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<Void> serverOp) {
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<Void> clientOp) {
        return null;
    }

}
