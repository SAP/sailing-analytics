package com.sap.sailing.declination;

import java.io.IOException;
import java.text.ParseException;

import com.sap.sailing.declination.impl.DeclinationServiceImpl;
import com.sap.sailing.declination.impl.NOAAImporter;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.Mile;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.CentralAngleDistance;
import com.sap.sse.common.TimePoint;

public interface DeclinationService {
    /**
     * A default implementation with a spatial default precision of 1 {@link Mile#METERS_PER_GEOGRAPHICAL_MILE
     * geographical mile} which equals the length of an arc with one degree on a meridian.
     */
    DeclinationService INSTANCE = new DeclinationServiceImpl(new CentralAngleDistance(1./180.*Math.PI), new NOAAImporter());
    
    /**
     * Obtains declination information with the default precision of this declination service in time and space
     * dimensions (e.g., one degree N/S and E/W and one year on the time axis). Particularly the time resolution may
     * benefit from an extrapolation using the {@link Declination#getAnnualChange() annual change} specification. Note
     * that the result is <em>not</em> corrected automatically by the {@link Declination#getAnnualChange() annual
     * change} of the result found for the position. The caller may do that using
     * {@link Declination#getBearingCorrectedTo(TimePoint)}.
     * 
     * @param timeoutForOnlineFetchInMilliseconds
     *            see {@link NOAAImporter#getDeclination(Position, TimePoint, long)}; if 0, waits forever; only needed
     *            if the pre-stored declinations don't fulfill the request up to the tolerance distance configured when
     *            constructing this service
     */
    Declination getDeclination(TimePoint timePoint, Position position, long timeoutForOnlineFetchInMilliseconds) throws IOException,
            ParseException;
    
    /**
     * Like {@link #getDeclination(TimePoint, Position, long)}, only that here the caller can specify a required spatial
     * precision which may be higher than the default precision. Increased precision may require an online lookup if no
     * stored value exists for this combination of time/position yet.
     * <p>
     * 
     * The time point precision is the default precision of this service. Note that the result is <em>not</em> corrected
     * automatically by the {@link Declination#getAnnualChange() annual change} of the result found for the position.
     * The caller may do that using {@link Declination#getBearingCorrectedTo(TimePoint)}.
     * 
     * @param timeoutForOnlineFetchInMilliseconds
     *            see {@link NOAAImporter#getDeclination(Position, TimePoint, long)}; if 0, waits forever; only needed
     *            if the pre-stored declinations don't fulfill the request up to the tolerance distance provided by the
     *            <code>maxDistance</code> parameter
     */
    Declination getDeclination(TimePoint timePoint, Position position, Distance maxDistance, long timeoutForOnlineFetchInMilliseconds) throws IOException,
            ParseException;
}
