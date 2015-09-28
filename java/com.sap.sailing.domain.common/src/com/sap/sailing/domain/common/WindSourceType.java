package com.sap.sailing.domain.common;

/**
 * Possible sources for wind data. Used to key and select between different {@link WindTrack}s. Literals
 * are given in descending order of precedence. Particularly, the {@link #COURSE_BASED} source should
 * really only be used if nothing else is known about the wind.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public enum WindSourceType {
    /**
     * Manually entered via a web form or received through a REST service call, e.g., from BeTomorrow's estimation
     */
    WEB(true, 0.9, /* useSpeed */ true),
    
    /**
     * Measured using wind sensors
     */
    EXPEDITION(true, 0.9, /* useSpeed */ true),
    
    /**
     * Estimates wind conditions by analyzing the boat tracks; may not have results for all time points, e.g.,
     * because at a given time point all boats may sail on the same tack and hence no averaging between the
     * two tacks is possible. This is the more likely to happen the smaller the fleet tracked is.
     */
    TRACK_BASED_ESTIMATION(false, 0.5, /* useSpeed */ false),

    /**
     * Inferred from the race course layout if the course is known to have its first leg be an upwind leg. This
     * source has very low confidence and must be superseded by any other wind source.
     */
    COURSE_BASED(false, 0.01, /* useSpeed */ false),
    
    /**
     * Wind estimation combined from all other wind sources, using <code>TrackedRace.getWind(...)</code>, based on
     * confidences
     */
    COMBINED(false, 0.9, /* useSpeed */ true),
    
    /**
     * Manually entered by the race committee over the app. As the race committee measures the wind several times over races for documentation purposes,
     * their measures are stored in the race log.
     * 
     */
    RACECOMMITTEE(false, 0.9, /* useSpeed */ true);
    
    private final boolean canBeStored;
    
    private final double baseConfidence;
    
    /**
     * If <code>false</code>, the speeds of this wind source are not meaningful / defined. For example, the {@link #COURSE_BASED}
     * wind source type doesn't provide any clues as to the wind speed, so it shouldn't be shown in charts nor used when
     * combining wind sources into one.
     */
    private final boolean useSpeed;
    
    private WindSourceType(boolean canBeStored, double baseConfidence, boolean useSpeed) {
        this.canBeStored = canBeStored;
        this.baseConfidence = baseConfidence;
        this.useSpeed = useSpeed;
    }
    
    public boolean canBeStored() {
        return canBeStored;
    }
    
    public double getBaseConfidence() {
        return baseConfidence;
    }
    
    public boolean useSpeed() {
        return useSpeed;
    }
    
}
