package com.sap.sailing.domain.tracking;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

public interface WindTrack extends DynamicTrack<Wind> {
    static final long DEFAULT_MILLISECONDS_OVER_WHICH_TO_AVERAGE_WIND = 30000;
    
    public static final Distance WIND_HALF_CONFIDENCE_DISTANCE = new MeterDistance(100);
    
    /**
     * The following is used as a "standard deviation" in a Gaussian normal distribution; in order
     * to still get positive confidences, a rather large duration of 30min is chosen here.
     */
    public static final Duration WIND_HALF_CONFIDENCE_DURATION = Duration.ONE_SECOND.times(5);

    /**
     * Estimates a wind force and direction based on tracked wind data.<p>
     * 
     * An implementation will typically put some averaging algorithm in place to avoid
     * "jumpy" measurements, making leaderboard and advantage line computations too edgy.
     * If a time-based averaging mechanism is used and <code>at</code> is more than the
     * averaging interval after the last known measurement then at least the last
     * measurement before <code>at</code> will be used to avoid ending up with no
     * estimate at all.<p>
     * 
     * If the track has no wind data at all, <code>null</code> will be returned.
     * Attention: The TimePoint of the returned Wind is NOT necessarily equal to the requested timepoint.  
     */
    Wind getAveragedWind(Position p, TimePoint at);
    
    WindWithConfidence<Util.Pair<Position, TimePoint>> getAveragedWindWithConfidence(Position p, TimePoint at);

    /**
     * A listener is notified whenever a new fix is added to this track
     */
    void addListener(WindListener listener);
    
    /**
     * Adds multiple {@link Wind} fixes in one call. This allows listeners to be notified with the
     * {@link WindListener#windDataReceived(Iterable)} callback method, taking action on all wind fixes actually added
     * at once. Only those fixes really added (not those where an equal {@link Wind} object was already contained in
     * this track} are passed to the {@link WindListener#windDataReceived(Iterable)} method.
     */
    void add(Iterable<Wind> fixesToAdd);

    void remove(Wind wind);

    void setMillisecondsOverWhichToAverage(long millisecondsOverWhichToAverage);

    long getMillisecondsOverWhichToAverageWind();
    
    /**
     * The {@link #getAveragedWindWithConfidence(Position, TimePoint)} method will usually try to produce a result even
     * if this track has only fixes that are spatially or time-wise not close to the position and time point requested,
     * having a lower confidence as an effect.
     * <p>
     * 
     * However, some wind tracks may choose to return
     * <code>null<code> from their {@link #getAveragedWindWithConfidence(Position, TimePoint)}
     * implementation if no fix can be produced or found within a certain range around the time point requested. If
     * such a time tick resolution exists, this method will tell the duration between two time ticks. Clients can
     * use this to determine how they should treat fixes in order to achieve far-reaching semantic consistency
     * with the server side.<p>
     * 
     * For example, if a client-side wind track records fixes coming from this track at a certain frequency,
     * the client can determine based on the result of this method whether the server had a value in the area
     * requested or not.
     */
    Duration getResolutionOutsideOfWhichNoFixWillBeReturned();
}
