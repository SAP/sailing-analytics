package com.sap.sailing.server.operationaltransformation;

import java.util.UUID;

import com.sap.sailing.server.interfaces.RacingEventServiceOperation;

/**
 * Updates the persisted health state for a media URL attached to an event.
 */
public abstract class UpdateEventMediaHealth extends AbstractEventOperation<Void> {
    private static final long serialVersionUID = 2814759036548190723L;

    protected final String mediaUrl;
    protected final boolean missing;
    protected final boolean missingMailNotificationSent;

    public UpdateEventMediaHealth(UUID eventId, String mediaUrl, boolean missing, boolean missingMailNotificationSent) {
        super(eventId);
        this.mediaUrl = mediaUrl;
        this.missing = missing;
        this.missingMailNotificationSent = missingMailNotificationSent;
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<?> serverOp) {
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<?> clientOp) {
        return null;
    }
}
