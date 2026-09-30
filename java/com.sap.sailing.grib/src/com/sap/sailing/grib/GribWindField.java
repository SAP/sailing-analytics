package com.sap.sailing.grib;

import java.io.IOException;

import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sse.common.Bounds;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;

/**
 * A wind field that is based on some GRIB data and can be queried using a {@link TimePoint} and a {@link Position}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface GribWindField {
    /**
     * @return the geographical bounds within which there is wind data in this field
     */
    Bounds getBounds();
    
    /**
     * @param timePoint
     *            at which time to query the wind; assuming that the wind field may have wind data for several time
     *            points, the data from the closest time point is used. Time and space distance as well as the general
     *            reliability of the service of the underlying GRIB file are combined into the confidence value of the
     *            wind vector returned.
     * @param position
     *            a position that is {@link Bounds#contains(Position) contained} in the {@link #getBounds() bounds} of
     *            this field
     * @return the wind data valid at the requested time point and position, with a confidence that expresses the
     *         general confidence in the GRIB source as well as the time-based confidence. For example, if asking
     *         further into the future than the source knows, confidence is reduced.
     */
    WindWithConfidence<TimePoint> getWind(TimePoint timePoint, Position position) throws IOException;

    /**
     * The time range covered by the underlying GRIB data.
     */
    TimeRange getTimeRange();

    Iterable<Wind> getAllWindFixes() throws IOException;
}
