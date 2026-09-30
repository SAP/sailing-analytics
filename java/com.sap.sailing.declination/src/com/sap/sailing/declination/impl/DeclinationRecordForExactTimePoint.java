package com.sap.sailing.declination.impl;

import com.sap.sailing.declination.Declination;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Position;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

/**
 * A declination record that does not know about any annual change because it has been computed precisely for
 * the time point and position requested. Therefore, no {@link #getBearingCorrectedTo(TimePoint) correction} to
 * any time point other than the one {@link #getTimePoint() requested} can be performed. An exception will
 * be thrown if that happens.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class DeclinationRecordForExactTimePoint extends AbstractDeclinationRecord implements Declination {
    private static final long serialVersionUID = -94512743120385233L;

    public DeclinationRecordForExactTimePoint(Position position, TimePoint timePoint, Bearing bearing) {
        super(position, timePoint, bearing);
    }

    @Override
    public Bearing getAnnualChange() {
        return null;
    }

    @Override
    public Bearing getBearingCorrectedTo(TimePoint timePoint) {
        if (Util.equalsWithNull(timePoint, getTimePoint())) {
            return getBearing();
        } else {
            throw new IllegalArgumentException("Declination computed precisely for " + getTimePoint()
                    + " cannot be corrected to any other time point " + timePoint
                    + " because we lack knowledge of the annual change here.");
        }
    }

}
