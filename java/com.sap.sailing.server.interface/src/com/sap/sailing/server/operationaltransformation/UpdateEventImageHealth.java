package com.sap.sailing.server.operationaltransformation;

import java.util.UUID;

import com.sap.sailing.server.interfaces.RacingEventService;

/**
 * Updates the persisted health state for an image URL attached to an event.
 */
public class UpdateEventImageHealth extends UpdateEventMediaHealth {
    private static final long serialVersionUID = 6923230063351853605L;

    public UpdateEventImageHealth(UUID eventId, String imageUrl, boolean missing, boolean missingMailNotificationSent) {
        super(eventId, imageUrl, missing, missingMailNotificationSent);
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) {
        toState.updateEventImageHealth(getId(), mediaUrl, missing, missingMailNotificationSent);
        return null;
    }
}
