package com.sap.sailing.domain.abstractlog.impl;

import com.sap.sailing.domain.abstractlog.AbstractLog;
import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.abstractlog.BaseLogAnalyzer;
import com.sap.sailing.domain.abstractlog.race.RaceLog;

/**
 * Finds the most recent {@link RaceLog#getUnrevokedEventsDescending() valid unrevoked event} in the log which is an
 * {@code instanceof} {@link #ofType}.
 * 
 * @author Fredrik Teschke
 *
 */
public class LastEventOfTypeFinder<LogT extends AbstractLog<EventT, VisitorT>, EventT extends AbstractLogEvent<VisitorT>,
VisitorT> extends BaseLogAnalyzer<LogT, EventT, VisitorT, EventT> {
    private final Class<?> ofType;
    private final boolean onlyUnrevoked;

    public LastEventOfTypeFinder(LogT log, boolean onlyUnrevoked, Class<?> ofType) {
        super(log);
        this.ofType = ofType;
        this.onlyUnrevoked = onlyUnrevoked;
    }

    @Override
    protected EventT performAnalysis() {
        final Iterable<EventT> set = onlyUnrevoked ? getLog().getUnrevokedEventsDescending() : getAllEventsDescending();
        for (final EventT event : set) {
            if (ofType.isAssignableFrom(event.getClass())) {
                return event;
            }
        }
        return null;
    }
}
