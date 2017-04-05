package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;

/**
 * Different tracking providers require different sets of arguments to start tracking a race.
 * This interface represents the functionality required to start tracking a race, agnostic of the
 * particular parameters and ways of launching the connector. Use {@link #createRaceTracker()} to
 * create the tracker that starts tracking the race identified by these tracking parameters.<p>
 * 
 * The parameters also tell whether wind tracking shall be activated for this race (see {@link #isTrackWind()}).
 * This property can be {@link #setTrackWind(boolean) updated}, making sense in case tracking a live race
 * is stopped in a running server, stopping the wind tracker and therefore not requiring wind tracking
 * to be activated anymore when the race is restored during server restart.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public interface RaceTrackingConnectivityParameters {
    /**
     * Starts a {@link RaceTracker} using the connectivity parameters provided by this object. As no specific
     * {@link Regatta} is provided, this will first look up a regatta for the race from
     * {@link TrackedRegattaRegistry#getRememberedRegattaForRace(java.io.Serializable)} and if not found will look up or
     * create a default regatta based on race data such as an event name and the boat class.
     * 
     * @param timeoutInMilliseconds
     *            gives the tracker a possibility to abort tracking the race after so many milliseconds of
     *            unsuccessfully waiting for the connection to be established. Support is optional for
     *            implementations, and there is no exact specification what must have happened before
     *            this timeout in order for tracking to continue. So, consider this as a "hint" to the
     *            tracker.
     */
    RaceTracker createRaceTracker(TrackedRegattaRegistry trackedRegattaRegistry, WindStore windStore,
            RaceLogResolver raceLogResolver, long timeoutInMilliseconds) throws Exception;
    
    /**
     * Starts a {@link RaceTracker}, associating the resulting races with the {@link Regatta} passed as argument
     * instead of using the tracker's domain factory to obtain a default {@link Regatta} object for the tracking
     * parameters. This is particularly useful if a predefined regatta with {@link Series} and {@link Fleet}s
     * is to be used.
     * @param timeoutInMilliseconds TODO
     */
    RaceTracker createRaceTracker(Regatta regatta, TrackedRegattaRegistry trackedRegattaRegistry, WindStore windStore,
            RaceLogResolver raceLogResolver, long timeoutInMilliseconds) throws Exception;
    
    /**
     * Deliver an ID object equal to that of the {@link RaceTracker#getID()} delivered by the {@link RaceTracker}
     * that will be created from these parameters by calling {@link #createRaceTracker(TrackedRegattaRegistry)}.
     */
    Object getTrackerID();

    /** 
     * Gets the configured delay time to the 'live' timepoint for this tracker
     */
    public long getDelayToLiveInMillis();
    
    /**
     * A non-{@code null} unique type identifier that helps finding services that need to deal with connectivity
     * parameters in a type-specific way
     */
    public String getTypeIdentifier();

    boolean isTrackWind();
    
    void setTrackWind(boolean trackWind);
    
    boolean isCorrectWindDirectionByMagneticDeclination();
}
