package com.sap.sailing.server.operationaltransformation;

import java.io.Serializable;

import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class RenameEvent extends AbstractEventOperation<Void> {
    private static final long serialVersionUID = 4516794459150387632L;
    private final String newEventName;

    public RenameEvent(Serializable id, String newEventName) {
        super(id);
        this.newEventName = newEventName;
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
        toState.renameEvent(getId(), newEventName);
        return null;
    }
}
