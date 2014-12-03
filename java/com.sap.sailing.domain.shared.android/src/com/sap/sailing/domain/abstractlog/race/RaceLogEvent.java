package com.sap.sailing.domain.abstractlog.race;

import java.util.List;

import com.sap.sailing.domain.abstractlog.AbstractLogEvent;
import com.sap.sailing.domain.base.Competitor;

public interface RaceLogEvent extends AbstractLogEvent<RaceLogEventVisitor> {

    /**
     * Gets the event's pass identifier.
     * 
     * Each {@link RaceLogEvent} is associated to a certain pass. A pass is every attempt to start and run a race. A
     * new pass is initiated when a new start time is proposed (e.g. after the race was aborted). There might be certain
     * event type that are not strictly bound to its pass.
     * 
     */
    int getPassId();

    /**
     * Gets the list of associated {@link Competitor}s.
     * 
     * A {@link RaceLogEvent} might be associated with a list of competitors, which are somehow relevant for this
     * kind of event. An example is a list of competitors who are marked for an individual recall.
     */
    List<Competitor> getInvolvedBoats();
}
