package com.sap.sailing.domain.abstractlog.race.analyzing.impl;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogDependentStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.impl.BaseRaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventComparator;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.ReadonlyRacingProcedure;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.util.impl.ArrayListNavigableSet;

/**
 * Returns the status and the clock {@link TimePoint} at which it was calculated. That time point is obtained
 * from the {@link Clock} passed to this analyzer at construction time. It is queried at the time the analysis
 * takes place ({@link #performAnalysis()}.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RaceStatusAnalyzer extends RaceLogAnalyzer<Pair<RaceLogRaceStatus, TimePoint>> {
    public interface Clock {
        TimePoint now();
    }
    
    public final static class StandardClock implements Clock {
        @Override
        public TimePoint now() {
            return MillisecondsTimePoint.now();
        }
    }
    
    private final RaceLogResolver resolver;
    private final Clock clock;
    private final ReadonlyRacingProcedure racingProcedure;
    
    public RaceStatusAnalyzer(RaceLogResolver resolver, RaceLog raceLog, ReadonlyRacingProcedure racingProcedure) {
        this(resolver, raceLog, new StandardClock(), racingProcedure);
    }
    
    public RaceStatusAnalyzer(RaceLogResolver resolver, RaceLog raceLog, Clock clock, ReadonlyRacingProcedure racingProcedure) {
        super(raceLog);
        this.resolver = resolver;
        this.clock = clock;
        this.racingProcedure = racingProcedure;
    }

    @Override
    protected Pair<RaceLogRaceStatus, TimePoint> performAnalysis() {
        ArrayListNavigableSet<RaceLogRaceStatusEvent> statusEvents = new ArrayListNavigableSet<>(
                RaceLogRaceStatusEventComparator.INSTANCE);
        for (RaceLogEvent event : getPassEvents()) {
            if (event instanceof RaceLogRaceStatusEvent) {
                statusEvents.add((RaceLogRaceStatusEvent) event);
            }
        }
        final TimePoint now = clock.now();
        final EventDispatcher eventDispatcher = new EventDispatcher(now, racingProcedure);
        RaceLogRaceStatus result = RaceLogRaceStatus.UNSCHEDULED;
        for (RaceLogRaceStatusEvent event : statusEvents.descendingSet()) {
            event.accept(eventDispatcher);
            result = eventDispatcher.nextStatus;
            break;
        }
        return new Pair<>(result, now);
    }
    
    private class EventDispatcher extends BaseRaceLogEventVisitor {

        private final TimePoint now;
        private final ReadonlyRacingProcedure racingProcedure;
        public RaceLogRaceStatus nextStatus;
        
        public EventDispatcher(TimePoint now, ReadonlyRacingProcedure racingProcedure) {
            this.now = now;
            this.racingProcedure = racingProcedure;
            this.nextStatus = RaceLogRaceStatus.UNKNOWN;
        }

        @Override
        public void visit(RaceLogStartTimeEvent event) {
            TimePoint startTime = event.getStartTime();
            setRaceLogStatusBasedOnStartTime(startTime);
        }

        private void setRaceLogStatusBasedOnStartTime(TimePoint startTime) {
            if (startTime == null) {
                nextStatus = RaceLogRaceStatus.PRESCHEDULED;
            } else {
                if (racingProcedure.isStartphaseActive(startTime, now)) {
                    nextStatus = RaceLogRaceStatus.STARTPHASE;
                } else if (now.before(startTime)) {
                    nextStatus = RaceLogRaceStatus.SCHEDULED;
                } else {
                    nextStatus = RaceLogRaceStatus.RUNNING;
                }
            }
        }

        @Override
        public void visit(RaceLogRaceStatusEvent event) {
            nextStatus = event.getNextStatus();
        }

        @Override
        public void visit(RaceLogDependentStartTimeEvent event) {
            DependentStartTimeResolver startTimeResolver = new DependentStartTimeResolver(resolver);
            TimePoint startTime = startTimeResolver.resolve(event).getStartTime();
            setRaceLogStatusBasedOnStartTime(startTime);
        }
    }
}