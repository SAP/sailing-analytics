package com.sap.sailing.gwt.ui.shared;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map.Entry;
import java.util.Set;

import com.google.gwt.core.shared.GwtIncompatible;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.domain.common.racelog.tracking.TrackingTimesRevocationErrorCode;
import com.sap.sailing.domain.racelogtracking.TrackingTimesRevocationReport;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

public class TrackingTimesRevocationReportDTO implements IsSerializable {
    private LinkedHashMap<Pair<RaceColumnDTO, FleetDTO>, HashSet<RaceLogEventDTO>> revokedEvents;
    private LinkedHashMap<Pair<RaceColumnDTO, FleetDTO>, Pair<TimePoint, TimePoint>> notRevokedBecauseOfMissingStartOrFinishTime;
    private ArrayList<Pair<RaceColumnDTO, FleetDTO>> notRevokedBecauseNotForTracking;
    private TrackingTimesRevocationErrorCode errorCode;

    @Deprecated
    TrackingTimesRevocationReportDTO() {
        // for GWT serialization
    }
    
    @GwtIncompatible
    public TrackingTimesRevocationReportDTO(TrackingTimesRevocationErrorCode errorCode) {
        this.errorCode = errorCode;
    }
    
    @GwtIncompatible
    public TrackingTimesRevocationReportDTO(TrackingTimesRevocationReport report) {
        this.errorCode = report.getErrorCode();
        this.revokedEvents = new LinkedHashMap<>();
        for (final Entry<Pair<RaceColumn, Fleet>, Set<RaceLogEvent>> e : report.getRevokedEvents().entrySet()) {
            revokedEvents.put(
                    new Pair<>(toDTO(e.getKey().getA()), toDTO(e.getKey().getB())),
                    new HashSet<>(e.getValue().stream().map(this::toDTO)
                            .collect(java.util.stream.Collectors.toSet())));
        }
        this.notRevokedBecauseOfMissingStartOrFinishTime = new LinkedHashMap<>();
        for (final Entry<Pair<RaceColumn, Fleet>, Pair<TimePoint, TimePoint>> e : report.getNotRevokedBecauseOfMissingStartOfFinishTime().entrySet()) {
            notRevokedBecauseOfMissingStartOrFinishTime.put(
                    new Pair<>(toDTO(e.getKey().getA()), toDTO(e.getKey().getB())),
                    e.getValue());
        }
        this.notRevokedBecauseNotForTracking = new ArrayList<>();
        for (final Pair<RaceColumn, Fleet> e : report.getNotRevokedBecauseNotForTracking()) {
            notRevokedBecauseNotForTracking.add(new Pair<>(toDTO(e.getA()), toDTO(e.getB())));
        }
    }
    
    /**
     * @return {@code null} means no error
     */
    public TrackingTimesRevocationErrorCode getErrorCode() {
        return errorCode;
    }

    public HashMap<Pair<RaceColumnDTO, FleetDTO>, HashSet<RaceLogEventDTO>> getRevokedEvents() {
        return revokedEvents;
    }

    public HashMap<Pair<RaceColumnDTO, FleetDTO>, Pair<TimePoint, TimePoint>> getNotRevokedBecauseOfMissingStartOrFinishTime() {
        return notRevokedBecauseOfMissingStartOrFinishTime;
    }

    public ArrayList<Pair<RaceColumnDTO, FleetDTO>> getNotRevokedBecauseNotForTracking() {
        return notRevokedBecauseNotForTracking;
    }

    @GwtIncompatible
    private FleetDTO toDTO(Fleet fleet) {
        return new FleetDTO(fleet.getName(), fleet.getOrdering(), fleet.getColor());
    }

    @GwtIncompatible
    private RaceColumnDTO toDTO(final RaceColumn raceColumn) {
        return new RaceColumnDTO(raceColumn.getName(), raceColumn.isOneAlwaysStaysOne());
    }

    @GwtIncompatible
    private RaceLogEventDTO toDTO(RaceLogEvent raceLogEvent) {
        return new RaceLogEventDTO(raceLogEvent.getPassId(), raceLogEvent.getAuthor().getName(),
                raceLogEvent.getAuthor().getPriority(),
                raceLogEvent.getCreatedAt() != null ? raceLogEvent.getCreatedAt().asDate() : null,
                raceLogEvent.getLogicalTimePoint() != null ? raceLogEvent.getLogicalTimePoint().asDate() : null,
                raceLogEvent.getClass().getSimpleName(), raceLogEvent.getShortInfo());
    }
}
