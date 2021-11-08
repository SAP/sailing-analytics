package com.sap.sailing.domain.regattalike;

import java.io.Serializable;
import java.util.Optional;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.CompetitorRegistrationType;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sse.common.Duration;

/**
 * A domain object that is regatta-like (in other words: similar to a regatta).
 * This is usually a {@link Regatta}. In some special cases however a {@link FlexibleLeaderboard} is used to semantically represent
 * a Regatta - e.g. if a special series uses individual races from other events/regattas.
 * 
 * @author Fredrik Teschke
 *
 */
public interface IsRegattaLike extends Serializable {
    /**
     * @return The RegattaLog associated with this {@code Leaderboard}. Where this log actually lives may be different
     * for different {@code Leaderboard} implementations (i.e. a {@link RegattaLeaderboard} returns the {@code RegattaLog}
     * of its {@link Regatta#getRegattaLog Regatta}, whereas a {@link FlexibleLeaderboard} creates its own {@code RegattaLog}).
     */
    RegattaLog getRegattaLog();
    
    /**
     * Indicates whether the competitors use the same boat for the whole regatta or change the boat used during the competition
     * @return true when the competitors change their boats, false otherwise
     */
    boolean canBoatsOfCompetitorsChangePerRace(); 
    
    /**
     * Get type of comeptitor registration.  
     * @return type of competitor registration
     */
    CompetitorRegistrationType getCompetitorRegistrationType();
    
    RaceColumn getRaceColumnByName(String raceColumnName);
    
    RegattaLikeIdentifier getRegattaLikeIdentifier();
    
    void addListener(RegattaLikeListener listener);
    
    void removeListener(RegattaLikeListener listener);
    
    /**
     * The time-on-time handicap factor for the <code>competitor</code>. If <code>null</code>, no time-on-time handicap
     * factor is defined for the <code>competitor</code>. For a simple time-on-time scheme, the factor is multiplied
     * with the actual time sailed to obtain the corrected time. For a "performance line" system, afterwards an
     * additional distance-based allowance may be subtracted from the resulting time.
     * <p>
     * 
     * The default factor is obtained from the {@link Competitor#getTimeOnTimeFactor() competitor}. This regatta can
     * make specific arrangements and override this default value for this competitor in its {@link #getRegattaLog()
     * regatta log}, accommodating for the fact that in between regattas competitors may get a new, different rating,
     * resulting in different handicaps for different regattas.
     * 
     * @param changeCallback
     *            an optional callback listener that will be {@link Runnable#run() invoked} when the
     *            {@code competitor}'s time-on-time factor changes for any reason. Only a weak reference will be
     *            established to the {@link Runnable} so that if the callback or the objects it references become
     *            eligible for garbage collection then any registration that is made internally in order to satisfy the
     *            callback will automatically be cleared.
     * 
     * @see #getTimeOnDistanceAllowancePerNauticalMile(Competitor, Optional)
     */
    Double getTimeOnTimeFactor(Competitor competitor, Optional<Runnable> changeCallback);
    
    /**
     * The time-on-distance time allowance for the <code>competitor</code>, used in handicap regattas. If
     * <code>null</code>, no time-on-distance allowance is defined for the <code>competitor</code>. For a simple
     * time-on-distance scheme, the factor is multiplied with the (windward) course length and the result is subtracted
     * from the actual time sailed to obtain the corrected time. For a "performance line" system, the actual time sailed
     * will be multiplied with the {@link #getTimeOnTimeFactor(Competitor, Optional) time-on-time factor} before
     * subtracting the time-on-distance allowance.
     * <p>
     * 
     * The default allowance is obtained from the {@link Competitor#getTimeOnDistanceAllowancePerNauticalMile()
     * competitor}. This regatta can make specific arrangements and override this default value for this competitor in
     * its {@link #getRegattaLog() regatta log}, accommodating for the fact that in between regattas competitors may get
     * a new, different rating, resulting in different handicaps for different regattas.
     * 
     * @param changeCallback
     *            an optional callback listener that will be {@link Runnable#run() invoked} when the
     *            {@code competitor}'s time-on-distance allowance changes for any reason. Only a weak reference will be
     *            established to the {@link Runnable} so that if the callback or the objects it references become
     *            eligible for garbage collection then any registration that is made internally in order to satisfy the
     *            callback will automatically be cleared.
     * 
     * @see #getTimeOnTimeFactor(Competitor, Optional)
     */
    Duration getTimeOnDistanceAllowancePerNauticalMile(Competitor competitor, Optional<Runnable> changeCallback);
    
    void setFleetsCanRunInParallelToTrue();
}