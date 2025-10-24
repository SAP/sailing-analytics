package com.sap.sailing.domain.abstractlog.race.analyzing.impl;

import java.util.ConcurrentModificationException;

import com.sap.sailing.domain.abstractlog.BaseLogAnalyzer;
import com.sap.sailing.domain.abstractlog.impl.AbstractLogImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.shared.tracking.Track;

public abstract class RaceLogAnalyzer<ResultType> extends BaseLogAnalyzer
        <RaceLog, RaceLogEvent, RaceLogEventVisitor, ResultType> {

    public RaceLogAnalyzer(RaceLog raceLog) {
        super(raceLog);
    }

    /**
     * Callers that want to iterate over the collection returned need to use {@link Track#lockForRead()} and
     * {@link Track#unlockAfterRead()} on the underlying {@link #getLog() log} to avoid
     * {@link ConcurrentModificationException}s.
     */
    protected Iterable<RaceLogEvent> getPassEvents() {
        return log.getFixes();
    }

    /**
     * Callers that want to iterate over the collection returned need to use {@link Track#lockForRead()} and
     * {@link Track#unlockAfterRead()} on the underlying {@link #getLog() log} to avoid
     * {@link ConcurrentModificationException}s.
     */
    protected Iterable<RaceLogEvent> getPassEventsDescending() {
        return log.getFixesDescending();
    }

    /**
     * Callers that want to iterate over the collection returned need to use {@link Track#lockForRead()} and
     * {@link Track#unlockAfterRead()} on the underlying {@link #getLog() log} to avoid
     * {@link ConcurrentModificationException}s.
     */
    protected Iterable<RaceLogEvent> getPassUnrevokedEvents() {
        return new AbstractLogImpl.FilteredPartialNavigableSetView<>(log.getUnrevokedEvents(), new RaceLog.PassValidator(log.getCurrentPassId()));
    }

    /**
     * Callers that want to iterate over the collection returned need to use {@link Track#lockForRead()} and
     * {@link Track#unlockAfterRead()} on the underlying {@link #getLog() log} to avoid
     * {@link ConcurrentModificationException}s.
     */
    protected Iterable<RaceLogEvent> getPassUnrevokedEventsDescending() {
        return new AbstractLogImpl.FilteredPartialNavigableSetView<>(log.getUnrevokedEventsDescending(), new RaceLog.PassValidator(log.getCurrentPassId()));
    }
}
