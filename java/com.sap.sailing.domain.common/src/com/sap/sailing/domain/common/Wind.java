package com.sap.sailing.domain.common;

import com.sap.sse.common.Bearing;
import com.sap.sse.common.Timed;

/**
 * Records a wind observation made at a certain position at a given time with an observed speed and a bearing. Note that
 * while nautically wind is usually specified in the direction it's coming <em>from</em>, for computational purposes
 * internally we measure the wind in the direction <em>to</em> which it blows. Use {@link #getFrom} to get the "inverse"
 * bearing.
 * <p>
 * 
 * Note that sometimes a wind measurement is transmitted without its position. While this is useless for wind field
 * interpolation, it can still serve good uses when some wind direction is better than no wind direction at all. In this
 * case, {@link #getPosition()} will return <code>null</code>.
 * <p>
 * 
 * When processing wind in the scope of a {@link TrackedRace}, it should always use true, not magnetic, directions.
 * However, during capturing wind, e.g., on a committee boat, sometimes it is easier for users to capture wind using a
 * magnetic direction which needs to be corrected by the magnetic variation / declination in order to produce a true
 * direction. It depends on the context in which objects of this type are produced and used whether this represents a
 * true or magnetic wind direction. For conversion, use a {@link DeclinationService} instance.
 * <p>
 * 
 * For two {@link Wind} objects to be equal, they need to have equal {@link #getTimePoint() time points},
 * {@link #getPosition() positions}, as well as wind speeds and directions.
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface Wind extends Positioned, Timed, SpeedWithBearing {
    /**
     * Computes the inverse bearing telling the direction from where the wind blows.
     */
    Bearing getFrom();
}
