package com.sap.sailing.server.operationaltransformation;

import java.util.UUID;

import com.sap.sailing.server.interfaces.RacingEventService;

/**
 * Updates the persisted health state for a video URL attached to an event.
 */
public class UpdateEventVideoHealth extends UpdateEventMediaHealth {
    private static final long serialVersionUID = 4187320561438792034L;

    public UpdateEventVideoHealth(UUID eventId, String videoUrl, boolean missing, boolean missingMailNotificationSent) {
        super(eventId, videoUrl, missing, missingMailNotificationSent);
    }

    @Override
    public Void internalApplyTo(RacingEventService toState) {
        toState.updateEventVideoHealth(getId(), mediaUrl, missing, missingMailNotificationSent);
        return null;
    }
}
