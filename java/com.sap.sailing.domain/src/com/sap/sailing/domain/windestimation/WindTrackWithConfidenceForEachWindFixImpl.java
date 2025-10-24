package com.sap.sailing.domain.windestimation;

import java.util.Map;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.WindTrackImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * Wind track implementation which uses the provided map with {@link WindWithConfidence}-instances to derive the
 * confidence for each wind fix within this wind track.
 * 
 * @author Vladislav Chumak (D069712)
 *
 */
public class WindTrackWithConfidenceForEachWindFixImpl extends WindTrackImpl {

    private static final long serialVersionUID = 8135949043175649443L;
    private final Map<Pair<Position, TimePoint>, WindWithConfidence<Pair<Position, TimePoint>>> windTrackWithConfidences;

    public WindTrackWithConfidenceForEachWindFixImpl(long millisecondsOverWhichToAverage, double baseConfidence,
            boolean useSpeed, String nameForReadWriteLock, boolean losslessCompaction,
            Map<Pair<Position, TimePoint>, WindWithConfidence<Pair<Position, TimePoint>>> windTrackWithConfidences) {
        super(millisecondsOverWhichToAverage, baseConfidence, useSpeed, nameForReadWriteLock, losslessCompaction);
        this.windTrackWithConfidences = windTrackWithConfidences;
    }

    @Override
    protected double getConfidenceOfInternalWindFixUnsynchronized(Wind windFix) {
        WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence = windTrackWithConfidences
                .get(new Pair<>(windFix.getPosition(), windFix.getTimePoint()));
        return windWithConfidence == null ? 0.0 :
            (super.getConfidenceOfInternalWindFixUnsynchronized(windFix) * windWithConfidence.getConfidence());
    }

    @Override
    public void lockForWrite() {
        super.lockForWrite();
    }

    @Override
    public void unlockAfterWrite() {
        super.unlockAfterWrite();
    }

}
