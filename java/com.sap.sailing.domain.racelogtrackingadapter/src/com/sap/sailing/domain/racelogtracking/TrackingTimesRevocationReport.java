package com.sap.sailing.domain.racelogtracking;

import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.racelog.tracking.TrackingTimesRevocationErrorCode;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

public interface TrackingTimesRevocationReport {
    /**
     * @return {@code null} means no error
     */
    TrackingTimesRevocationErrorCode getErrorCode();

    void revoked(RaceColumn raceColumn, Fleet fleet, RaceLogEvent event);

    void notRevokedBecauseOfMissingStartOrFinishTime(RaceColumn raceColumn, Fleet fleet, TimePoint startTime, TimePoint finishedTime);

    void notRevokedBecauseNotForTracking(RaceColumn raceColumn, Fleet fleet);

    Map<Pair<RaceColumn, Fleet>, Pair<TimePoint, TimePoint>> getNotRevokedBecauseOfMissingStartOfFinishTime();

    Map<Pair<RaceColumn, Fleet>, Set<RaceLogEvent>> getRevokedEvents();

    Iterable<Pair<RaceColumn, Fleet>> getNotRevokedBecauseNotForTracking();

}
