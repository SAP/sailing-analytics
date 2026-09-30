package com.sap.sailing.domain.maneuverdetection.impl;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.maneuverdetection.ApproximatedFixesCalculator;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;

public class ApproximatedFixesCalculatorImpl implements ApproximatedFixesCalculator {

    protected final TrackedRace trackedRace;
    protected final Competitor competitor;

    public ApproximatedFixesCalculatorImpl(TrackedRace trackedRace, Competitor competitor) {
        this.trackedRace = trackedRace;
        this.competitor = competitor;
    }

    @Override
    public Iterable<GPSFixMoving> approximate(TimePoint earliestStart, TimePoint latestEnd) {
        return trackedRace.approximate(competitor,
                earliestStart,
                latestEnd);
    }

}
