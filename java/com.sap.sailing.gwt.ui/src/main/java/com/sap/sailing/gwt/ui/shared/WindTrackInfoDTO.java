package com.sap.sailing.gwt.ui.shared;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;

public class WindTrackInfoDTO implements IsSerializable {
    public List<WindDTO> windFixes;
    public long dampeningIntervalInMilliseconds;
    public double minWindConfidence;
    public double maxWindConfidence;
    
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
    public Duration resolutionOutsideOfWhichNoFixWillBeReturned;
    
    public WindTrackInfoDTO() {}
}
