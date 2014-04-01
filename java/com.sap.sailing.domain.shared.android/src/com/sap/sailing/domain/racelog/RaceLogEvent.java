package com.sap.sailing.domain.racelog;

import java.io.Serializable;
import java.util.List;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Timed;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WithID;

/**
 * <p>
 * A {@link RaceLogEvent} is an entry in a race's log (typically a {@link RaceLog}.
 * </p>
 * 
 * <p>
 * Such an entry may influence the state of a race (scheduled, started, finished, ...), contain information about the
 * current displayed flags (AP, N, X, P, etc.) of a race or indicate some other change regarding the race. See
 * subclasses of {@link RaceLogEvent} for a full list of possibilities.
 * </p>
 * 
 * <p>
 * A {@link RaceLogEvent} may be associated to a certain pass. For a race a new pass is initiated whenever a new start
 * time is proposed (e.g. after the race was aborted).
 * </p>
 * 
 * <p>
 * Each {@link RaceLogEvent} carries two timestamps.
 * 
 * The timestamp received with {@link RaceLogEvent#getCreatedAt()} denotes the point in time the event was created.
 * 
 * The timestamp received with {@link RaceLogEvent#getLogicalTimePoint()} denotes the logical point in time the event
 * has occurred (sometimes called 'event time'). This timestamp might be different from
 * {@link RaceLogEvent#getCreatedAt()}. For example setting the start time of race to a point in time in the past,
 * results in a {@link RaceLogEvent} returning "now" on {@link RaceLogEvent#getCreatedAt()} and such a point in time
 * that a complete startphase could have occured on {@link RaceLogEvent#getTimePoint()}.
 * 
 * To ensure an ordering there should be no two {@link RaceLogEvent}s returning the same point in time on
 * {@link RaceLogEvent#getCreatedAt()} <b>per pass</b>.
 * 
 * The {@link RaceLogEvent}'s {@link Timed} interface redirects to {@link RaceLogEvent#getCreatedAt()} (was
 * {@link RaceLogEvent#getLogicalTimePoint()}!).
 * </p>
 * 
 * <p>
 * Race log events have an author that has a name and a priority assigned. This can be used to represent multiple
 * concurrent race log authors such as a device used on a starting vessel, another device used at the finish line and
 * yet another device used on shore. In case more than one device make a statement about something, such as the start
 * time or the start procedure, the statement from the device with the highest priority needs to take precedence.
 * </p>
 */
public interface RaceLogEvent extends Timed, WithID, Serializable {

    /**
     * Gets the {@link TimePoint} this event was created at.
     */
    TimePoint getCreatedAt();

    /**
     * Gets the event's logical timestamp.
     * 
     * @return
     */
    TimePoint getLogicalTimePoint();

    /**
     * Gets the event's pass identifier.
     * 
     * Each {@link RaceLogEvent} is associated to a certain pass. A pass is every attempt to start and run a race. A new
     * pass is initiated when a new start time is proposed (e.g. after the race was aborted). There might be certain
     * event type that are not strictly bound to its pass.
     * 
     */
    int getPassId();

    /**
     * Gets the list of associated {@link Competitor}s.
     * 
     * A {@link RaceLogEvent} might be associated with a list of competitors, which are somehow relevant for this kind
     * of event. An example is a list of competitors who are marked for an individual recall.
     */
    List<Competitor> getInvolvedBoats();

    /**
     * Visitor pattern to implement certain {@link RaceLogEvent} subclass specific behavior.
     */
    void accept(RaceLogEventVisitor visitor);

    /**
     * Gets the {@link RaceLogEventAuthor} author of this event.
     */
    RaceLogEventAuthor getAuthor();

    /**
     * Gets a short info about the most relevant data of the event.
     * @return
     */
    String getShortInfo();
}
