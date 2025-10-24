package com.sap.sailing.domain.common.orc;

import java.io.Serializable;

import com.sap.sse.common.Speed;
import com.sap.sse.common.Timed;

/**
 * Provides information about the so-called "implied wind" of a race that is ranked by ORC Polar Curve Scoring
 * (PCS). Implied wind is generally determined by mapping a competitor's elapsed time "backwards" through the
 * polar curve function to obtain a wind speed at which the time allowance for the course sailed matches
 * with the time elapsed. In uses of ORC PCS scoring before 2015 this was the ranking criterion, whereas since 2015
 * the general rule is to take the maximum implied wind achieved by any competitor and use it to determine each
 * competitor's time allowance for the course sailed, then comparing their elapsed time to their allowance and
 * using the difference as the ranking criterion.<p>
 * 
 * In some cases it does not have to be the maximum implied wind of a race that is used to compute the time
 * allowances, but it could be another race's maximum implied wind, or it could be a fixed speed decided by
 * a race officer based on observations or gut feel.<p>
 * 
 * An instance of a class implementing this interface will tell the <em>effective</em> implied wind used for
 * scoring purposes, and it will explain where this implied wind value came from, namely either by observing
 * the corresponding race's maximum implied wind, or by fetching it from another race (which transitively could
 * again fetch it from somewhere else, and so on), or by explicitly fixing it at a constant value.<p>
 * 
 * This object is considered immutable and a snapshot taken from a race at {@link #getTimePoint a time point}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface ImpliedWind extends ImpliedWindSource, Serializable, Timed {
    // TODO after refactoring ImpliedWindSource, re-define this interface... it will be used to transport the result of asking a race column/fleet slot for its implied wind to the client and vice versa
    /**
     * The effective speed that is used as implied wind for ranking purposes. If {@link #getFixedImpliedWindSpeed()} returns
     * a non-{@code null} object then that will be the result also of calling this method. Otherwise, if
     * {@link #getLeaderboardAndRaceColumnAndFleetOfDefiningRace()} returns a non-{@code null} value, this method will return
     * the effective implied wind used by that other race. Otherwise, the effective implied wind is defined to be the
     * maximum implied wind in the race that this object describes.
     */
    Speed getEffectiveImpliedWind();
    
    /**
     * The maximum implied wind in the race described by this object, taken at the current time. If the race is still live,
     * this means that the result will most likely change over time.
     */
    Speed getOwnMaximumImpliedWind();
}
