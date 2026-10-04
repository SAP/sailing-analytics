package com.sap.sailing.domain.racelogtracking.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.racelog.tracking.TrackingTimesRevocationErrorCode;
import com.sap.sailing.domain.racelogtracking.TrackingTimesRevocationReport;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

public class TrackingTimesRevocationReportImpl implements TrackingTimesRevocationReport {
    private final Map<Pair<RaceColumn, Fleet>, Set<RaceLogEvent>> revokedEvents;
    private final Map<Pair<RaceColumn, Fleet>, Pair<TimePoint, TimePoint>> notRevokedBecauseOfMissingStartOrFinishTime;
    private final List<Pair<RaceColumn, Fleet>> notRevokedBecauseNotForTracking;
    private final TrackingTimesRevocationErrorCode errorCode;

    public TrackingTimesRevocationReportImpl(TrackingTimesRevocationErrorCode errorCode) {
        super();
        this.errorCode = errorCode;
        this.revokedEvents = new LinkedHashMap<>();
        this.notRevokedBecauseOfMissingStartOrFinishTime = new LinkedHashMap<>();
        this.notRevokedBecauseNotForTracking = new ArrayList<>();
    }
    
    @Override
    public TrackingTimesRevocationErrorCode getErrorCode() {
        return errorCode;
    }

    @Override
    public void revoked(RaceColumn raceColumn, Fleet fleet, RaceLogEvent event) {
        final Set<RaceLogEvent> set = revokedEvents.computeIfAbsent(new Pair<>(raceColumn, fleet), k->new HashSet<>());
        set.add(event);
    }

    @Override
    public void notRevokedBecauseOfMissingStartOrFinishTime(RaceColumn raceColumn, Fleet fleet, TimePoint startTime, TimePoint finishedTime) {
        notRevokedBecauseOfMissingStartOrFinishTime.put(new Pair<>(raceColumn, fleet), new Pair<>(startTime, finishedTime));
    }
    
    @Override
    public void notRevokedBecauseNotForTracking(RaceColumn raceColumn, Fleet fleet) {
        notRevokedBecauseNotForTracking.add(new Pair<>(raceColumn, fleet));
    }

    @Override
    public Map<Pair<RaceColumn, Fleet>, Pair<TimePoint, TimePoint>> getNotRevokedBecauseOfMissingStartOfFinishTime() {
        return Collections.unmodifiableMap(notRevokedBecauseOfMissingStartOrFinishTime);
    }

    @Override
    public Map<Pair<RaceColumn, Fleet>, Set<RaceLogEvent>> getRevokedEvents() {
        return Collections.unmodifiableMap(revokedEvents);
    }

    @Override
    public Iterable<Pair<RaceColumn, Fleet>> getNotRevokedBecauseNotForTracking() {
        return Collections.unmodifiableList(notRevokedBecauseNotForTracking);
    }
}
