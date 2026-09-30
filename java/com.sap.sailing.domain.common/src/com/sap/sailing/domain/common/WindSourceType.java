package com.sap.sailing.domain.common;

import com.sap.sailing.domain.common.impl.WindSourceWithAdditionalID;

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
    WEB(true, 0.9, /* useSpeed */ true, /* observed */ true),
    
    /**
     * Measured using wind sensors
     */
    EXPEDITION(true, 0.9, /* useSpeed */ true, /* observed */ true),
    
    /**
     * Estimates wind conditions by analyzing the boat tracks; may not have results for all time points, e.g.,
     * because at a given time point all boats may sail on the same tack and hence no averaging between the
     * two tacks is possible. This is the more likely to happen the smaller the fleet tracked is.
     */
    TRACK_BASED_ESTIMATION(false, 0.1, /* useSpeed */ false, /* observed */ false),
    
    /**
     * Estimates wind conditions by analyzing the maneuvers of all boat tracks closely interoperating with incremental
     * maneuver detector.
     */
    MANEUVER_BASED_ESTIMATION(false, 0.001, /* useSpeed */ false, /* observed */ false),

    /**
     * Inferred from the race course layout if the course is known to have its first leg be an upwind leg. This
     * source has very low confidence and must be superseded by any other wind source.
     */
    COURSE_BASED(false, 0.01, /* useSpeed */ false, /* observed */ false),
    
    /**
     * Wind estimation combined from all other wind sources, using <code>TrackedRace.getWind(...)</code>, based on
     * confidences
     */
    COMBINED(false, 0.9, /* useSpeed */ true, /* observed */ false),
    
    /**
     * Manually entered by the race committee over the app. As the race committee measures the wind several times over races for documentation purposes,
     * their measures are stored in the race log.
     * 
     */
    RACECOMMITTEE(false, 0.9, /* useSpeed */ true, /* observed */ true),
    
    /**
     * Like {@link #COMBINED}, only that when combined with an ID into a {@link WindSourceWithAdditionalID} then the
     * position for the wind is taken to be the middle of the tracked leg whose number is identified by the ID.
     */
    LEG_MIDDLE(/* can be stored */ false, /* base confidence */ 0.9, /* useSpeed */ true, /* observed */ false),
    
    /**
     * A wind measurement spot as provided by https://www.windfinder.com; when used with a
     * {@link WindSourceWithAdditionalID}, the {@link WindSourceWithAdditionalID#getId() ID} is
     * the WindFinder's "Spot ID" such as "10044N" for Kiel Lighthouse.
     */
    WINDFINDER(/* can be stored */ true, /* base confidence */ 0.3, /* useSpeed */ true, /* observed */ true);
    
    private final boolean canBeStored;
    
    private final double baseConfidence;
    
    /**
     * If <code>false</code>, the speeds of this wind source are not meaningful / defined. For example, the {@link #COURSE_BASED}
     * wind source type doesn't provide any clues as to the wind speed, so it shouldn't be shown in charts nor used when
     * combining wind sources into one.
     */
    private final boolean useSpeed;
    
    /**
     * A flag to indicate if the wind data is observed or an estimation.
     * <p>
     * This flag should be set to {@code true} when the data originates from a
     * definitive source, such as a direct sensor measurement, a manual user entry,
     * or an official confirmation from race direction.
     * <p>
     * It should be set to {@code false} if the data is an approximation,
     * such as a value that has been calculated or estimated
     * from other data points.
     */
    private final boolean isObserved;
    
    private WindSourceType(boolean canBeStored, double baseConfidence, boolean useSpeed, boolean isObserved) {
        this.canBeStored = canBeStored;
        this.baseConfidence = baseConfidence;
        this.useSpeed = useSpeed;
        this.isObserved = isObserved;
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
    
    public boolean isObserved() {
        return isObserved;
    }
}