package com.sap.sailing.domain.racelog.analyzing.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;

/**
 * Analyzation returns the most recent {@link RaceLogFlagEvent}s.
 * 
 * If there is no {@link RaceLogFlagEvent} in the current pass, <code>null</code> is returned. Otherwise a {@link List}
 * of {@link RaceLogFlagEvent} is returned containing all {@link RaceLogEvent}s with the most recent {@link TimePoint}.
 * 
 */
public class LastFlagsFinder extends RaceLogAnalyzer<List<RaceLogFlagEvent>> {

    public LastFlagsFinder(RaceLog raceLog) {
        super(raceLog);
    }

    @Override
    protected List<RaceLogFlagEvent> performAnalysis() {

        Iterator<RaceLogEvent> iterator = getPassEventsDescending().iterator();
        RaceLogFlagEvent flagEvent = getNextFlagEvent(iterator);
        if (flagEvent != null) {
            return collectAllWithSameTimePoint(iterator, flagEvent);
        }
        return null;
    }

    private List<RaceLogFlagEvent> collectAllWithSameTimePoint(Iterator<RaceLogEvent> iterator,
            RaceLogFlagEvent flagEvent) {
        List<RaceLogFlagEvent> result = new ArrayList<RaceLogFlagEvent>();
        TimePoint lastFlagsTime = flagEvent.getTimePoint();

        while (flagEvent.getTimePoint().equals(lastFlagsTime)) {
            result.add(flagEvent);
            flagEvent = getNextFlagEvent(iterator);
            if (flagEvent == null) {
                break;
            }
        }

        return result;
    }

    private RaceLogFlagEvent getNextFlagEvent(Iterator<RaceLogEvent> iterator) {
        while (iterator.hasNext()) {
            RaceLogEvent event = iterator.next();
            if (event instanceof RaceLogFlagEvent) {
                return (RaceLogFlagEvent) event;
            }
        }
        return null;
    }

    /**
     * Use this method the obtain the most "interesting" {@link RaceLogFlagEvent} of the {@link LastFlagsFinder}'s
     * analyzation result.
     * 
     * If an empty list or <code>null</code> is passed, this method returns <code>null</code>. Otherwise the (most
     * recent) {@link RaceLogFlagEvent} with the highest ID is returned favoring events with
     * {@link RaceLogFlagEvent#isDisplayed()} returning <code>true</code>.
     * 
     * @param events
     *            result of {@link LastFlagsFinder#analyze()}.
     * @return {@link RaceLogFlagEvent} or <code>null</code>.
     */
    public static RaceLogFlagEvent getMostRecent(List<RaceLogFlagEvent> events) {
        if (events == null || events.isEmpty()) {
            return null;
        }

        List<RaceLogFlagEvent> sortedEvents = new ArrayList<RaceLogFlagEvent>(events);
        Collections.sort(sortedEvents, new Comparator<RaceLogFlagEvent>() {
            @Override
            public int compare(RaceLogFlagEvent left, RaceLogFlagEvent right) {
                int result = right.getTimePoint().compareTo(left.getTimePoint());
                if (result == 0) {
                    result = Boolean.valueOf(right.isDisplayed()).compareTo(left.isDisplayed());
                    if (result == 0) {
                        result = right.getId().toString().compareTo(left.getId().toString());
                    }
                }
                return result;
            }
        });

        return sortedEvents.get(0);
    }

}
