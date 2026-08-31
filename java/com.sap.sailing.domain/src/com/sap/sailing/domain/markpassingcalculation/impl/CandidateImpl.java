package com.sap.sailing.domain.markpassingcalculation.impl;

import java.util.Comparator;

import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.AbstractTimePoint;

public class CandidateImpl implements Candidate {
    private static final long serialVersionUID = -4626280455738918911L;
    private final Waypoint w;
    private final boolean timePointAsMillisIsNull;
    private final long timePointAsMillis;
    private final double probability;
    private final Integer oneBasedIndexOfWaypoint;
    private final Comparator<TimePoint> nullSafeTimePointComparator;
    
    public class CandidateTimePoint extends AbstractTimePoint implements TimePoint {
        private static final long serialVersionUID = 8156956989028606884L;

        @Override
        public long asMillis() {
            return timePointAsMillis;
        }
    }

    public CandidateImpl(int oneBasedIndexOfWaypoint, TimePoint p, double probability, Waypoint w) {
        this.w = w;
        this.timePointAsMillisIsNull = p == null;
        this.timePointAsMillis = p == null ? 0 : p.asMillis();
        this.probability = probability;
        this.oneBasedIndexOfWaypoint = oneBasedIndexOfWaypoint;
        this.nullSafeTimePointComparator = Comparator.nullsLast(Comparator.naturalOrder());
    }

    @Override
    public int getOneBasedIndexOfWaypoint() {
        return oneBasedIndexOfWaypoint;
    }

    @Override
    public TimePoint getTimePoint() {
        return timePointAsMillisIsNull ? null : new CandidateTimePoint();
    }

    @Override
    public long getTimePointAsMillis() {
        return timePointAsMillisIsNull ? -1 : timePointAsMillis;
    }

    @Override
    public Double getProbability() {
        return probability;
    }

    @Override
    public Waypoint getWaypoint() {
        return w;
    }

    public String toString() {
        return "Candidate for waypoint " + getOneBasedIndexOfWaypoint() + " with probability " +
                    getProbability() + " and timepoint " + getTimePoint();
    }

    @Override
    public int compareTo(CandidateImpl arg0) {
        return compareTo((Candidate) arg0);
    }

    @Override
    public int compareTo(Candidate arg0) {
        return getOneBasedIndexOfWaypoint() != arg0.getOneBasedIndexOfWaypoint() ? Integer.valueOf(
                getOneBasedIndexOfWaypoint()).compareTo(arg0.getOneBasedIndexOfWaypoint())
                : getTimePoint() != arg0.getTimePoint() ? nullSafeTimePointComparator.compare(getTimePoint(), arg0.getTimePoint()) :
                    getProbability().compareTo(arg0.getProbability());
    }
}
