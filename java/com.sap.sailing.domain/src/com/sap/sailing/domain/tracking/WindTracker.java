package com.sap.sailing.domain.tracking;

/**
 * Receives wind information and forwards it to a {@link TrackedRace}'s {@link TrackedRace#getOrCreateWindTrack(WindSource) wind
 * track}.
 * 
 * @author Axel Uhl (D043530)
 * 
 */
public interface WindTracker {

    void stop();

}
