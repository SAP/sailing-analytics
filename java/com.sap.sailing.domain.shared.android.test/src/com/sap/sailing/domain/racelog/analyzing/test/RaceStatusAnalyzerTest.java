package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogRaceStatusEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartProcedureChangedEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceStatusAnalyzer;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceStatusAnalyzer.Clock;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogRaceStatusEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartProcedureChangedEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.ReadonlyRaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedure;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl.ReadonlyRacingProcedureFactory;
import com.sap.sailing.domain.base.configuration.impl.EmptyRegattaConfiguration;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sailing.domain.common.racelog.RacingProcedureType;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceStatusAnalyzerTest extends PassAwareRaceLogAnalyzerTest<RaceStatusAnalyzer, Pair<RaceLogRaceStatus, TimePoint>> {

    private RacingProcedure racingProcedure = mock(RacingProcedure.class);
    private TimePoint now = MillisecondsTimePoint.now();
    private Clock constantTimeClock = ()->now;
    
    @Override
    protected RaceStatusAnalyzer createAnalyzer(RaceLog raceLog) {
        return new RaceStatusAnalyzer(mock(RaceLogResolver.class), raceLog, constantTimeClock, racingProcedure);
    }

    @Override
    protected TargetPair getTargetEventsAndResultForPassAwareTests(int passId, AbstractLogEventAuthor author) {
        RaceLogRaceStatusEvent event = createEvent(RaceLogRaceStatusEvent.class, 1, passId, author);
        when(event.getNextStatus()).thenReturn(RaceLogRaceStatus.RUNNING);
        doAnswer(new StatusVisitorAnswer()).when(event).accept(any(RaceLogEventVisitor.class));
        return new TargetPair(Arrays.asList(event), new Pair<>(event.getNextStatus(), now));
    }
    
    @Override
    protected TargetPair getBlockingEventsAndResultForPassAwareTests(
            int passId, AbstractLogEventAuthor author) {
        RaceLogRaceStatusEvent event = createEvent(RaceLogRaceStatusEvent.class, 1, passId, author);
        when(event.getNextStatus()).thenReturn(RaceLogRaceStatus.FINISHING);
        doAnswer(new StatusVisitorAnswer()).when(event).accept(any(RaceLogEventVisitor.class));
        return new TargetPair(Arrays.asList(event), new Pair<>(event.getNextStatus(), now));
    }
    
    @Test
    public void testUnscheduldedForNone() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        raceLog.add(event1);
        assertEquals(RaceLogRaceStatus.UNSCHEDULED, analyzer.analyze().getA());
    }

    @Test
    public void testMostRecent() {
        LogEventAuthorImpl author = new LogEventAuthorImpl("author", 0);
        RaceLogRaceStatusEvent event1 = new RaceLogRaceStatusEventImpl(
                new MillisecondsTimePoint(1), author, /* passId */ 1, RaceLogRaceStatus.FINISHING);
        RaceLogRaceStatusEvent event2 = new RaceLogRaceStatusEventImpl(
                new MillisecondsTimePoint(2), author, /* passId */ 1, RaceLogRaceStatus.FINISHED);
        raceLog.add(event1);
        raceLog.add(event2);
        assertEquals(event2.getNextStatus(), analyzer.analyze().getA());
    }
    
    @Test
    public void testFinishingWithFinishedInFuture() {
        now = new MillisecondsTimePoint(2);
        LogEventAuthorImpl author = new LogEventAuthorImpl("author", 0);
        RaceLogRaceStatusEvent event1 = new RaceLogRaceStatusEventImpl(
                new MillisecondsTimePoint(1), author, /* passId */ 1, RaceLogRaceStatus.FINISHING);
        RaceLogRaceStatusEvent event2 = new RaceLogRaceStatusEventImpl(
                new MillisecondsTimePoint(3), author, /* passId */ 1, RaceLogRaceStatus.FINISHED);
        raceLog.add(event1);
        raceLog.add(event2);
        assertEquals(event1.getNextStatus(), analyzer.analyze().getA());
    }
    
    @Test
    public void testStartphaseNotYetActive() {
        RaceLogStartTimeEvent event = createStartTimeEvent(MillisecondsTimePoint.now().plus(20000).asMillis(), true);
        raceLog.add(event);
        assertEquals(RaceLogRaceStatus.SCHEDULED, analyzer.analyze().getA());
    }
    
    @Test
    public void testStartphaseActive() {
        when(racingProcedure.isStartphaseActive(any(TimePoint.class), any(TimePoint.class))).thenReturn(true);
        
        RaceLogStartTimeEvent event = createStartTimeEvent(MillisecondsTimePoint.now().plus(20000).asMillis(), true);
        raceLog.add(event);
        
        assertEquals(RaceLogRaceStatus.STARTPHASE, analyzer.analyze().getA());
    }
    
    @Test
    public void testStartTimePassed() {
        when(racingProcedure.isStartphaseActive(any(TimePoint.class), any(TimePoint.class))).thenReturn(false);
        
        RaceLogStartTimeEvent event = createStartTimeEvent(0, false);
        raceLog.add(event);
        
        assertEquals(RaceLogRaceStatus.RUNNING, analyzer.analyze().getA());
    }
    
    @Test
    public void testClock() {
        final TimePoint startTime = MillisecondsTimePoint.now(); 
        analyzer = new RaceStatusAnalyzer(mock(RaceLogResolver.class), raceLog, new RaceStatusAnalyzer.Clock() {
            @Override
            public TimePoint now() {
                return startTime.minus(1);
            }
        }, racingProcedure);
        when(racingProcedure.isStartphaseActive(any(TimePoint.class), any(TimePoint.class))).thenReturn(false);
        RaceLogStartTimeEvent event = createStartTimeEvent(startTime);
        raceLog.add(event);
        assertEquals(RaceLogRaceStatus.SCHEDULED, analyzer.analyze().getA());
    }

    @Test
    public void testRaceStateStatusUpdateWithoutNewRaceLogEvent() throws InterruptedException {
        final TimePoint[] timeForClock = { MillisecondsTimePoint.now() };
        final RaceStatusAnalyzer.Clock clock = new RaceStatusAnalyzer.Clock() {
            @Override
            public TimePoint now() {
                return timeForClock[0];
            }
        };
        final int analyzeAndUpdateCachedRaceStatusCalls[] = new int[1];
        ReadonlyRaceState raceState = new ReadonlyRaceStateImpl(mock(RaceLogResolver.class), raceLog,
                new SimpleRaceLogIdentifierImpl("regatta", "column", "fleet"), clock, new ReadonlyRacingProcedureFactory(
                new EmptyRegattaConfiguration()), Collections.emptyMap()) {
                    @Override
                    protected void analyzeAndUpdateCachedRaceStatus() {
                        analyzeAndUpdateCachedRaceStatusCalls[0]++;
                        super.analyzeAndUpdateCachedRaceStatus();
                    }
        };
        RaceLogStartProcedureChangedEvent startProcedureEvent = new RaceLogStartProcedureChangedEventImpl(
                timeForClock[0], mock(AbstractLogEventAuthor.class), /* passId */ 0, RacingProcedureType.RRS26);
        raceLog.add(startProcedureEvent);
        final TimePoint startTime = timeForClock[0].plus(Duration.ONE_MINUTE.times(10));
        RaceLogStartTimeEvent event = createStartTimeEvent(startTime);
        raceLog.add(event);
        assertEquals(startTime, raceState.getStartTime());
        assertEquals(RaceLogRaceStatus.SCHEDULED, raceState.getStatus());
        timeForClock[0] = timeForClock[0].plus(Duration.ONE_MINUTE.times(9)); // one minute before start; we should be in STARTPHASE
        assertEquals(RaceLogRaceStatus.STARTPHASE, raceState.getStatus());
        timeForClock[0] = timeForClock[0].plus(Duration.ONE_MINUTE.times(2)); // one minute after start; we should be RUNNING
        assertEquals(RaceLogRaceStatus.RUNNING, raceState.getStatus());
        timeForClock[0] = timeForClock[0].plus(Duration.ONE_MINUTE.times(2)); // three minutes after start; we should still be RUNNING, without recalculating
        final int analyzeAndUpdateCachedRaceStatusCallsBefore = analyzeAndUpdateCachedRaceStatusCalls[0];
        assertEquals(RaceLogRaceStatus.RUNNING, raceState.getStatus());
        assertEquals(analyzeAndUpdateCachedRaceStatusCalls[0], analyzeAndUpdateCachedRaceStatusCallsBefore);
    }

    @Test
    @Override
    public void testPassAwareHidingMinorAuthor() {
        // makes no sense here as the status events are not of the same type - redirect to testStartSetByStartVesselAndFinishSetByShoreControl()
        testStartSetByStartVesselAndFinishSetByShoreControl();
    }

    public void testStartSetByStartVesselAndFinishSetByShoreControl() {
        AbstractLogEventAuthor authorStartVessel = new LogEventAuthorImpl("Race Officer on Finish Vessel", 1);
        AbstractLogEventAuthor authorShoreControl = new LogEventAuthorImpl("Shore Control", 2);
        
        RaceLogRaceStatusEvent event1 = createEvent(RaceLogRaceStatusEvent.class, 1, 1 /** passId*/, authorStartVessel);
        when(event1.getNextStatus()).thenReturn(RaceLogRaceStatus.SCHEDULED);
        RaceLogRaceStatusEvent event2 = createEvent(RaceLogRaceStatusEvent.class, 2, 1 /** passId*/, authorShoreControl);
        when(event2.getNextStatus()).thenReturn(RaceLogRaceStatus.FINISHING);
        doAnswer(new StatusVisitorAnswer()).when(event2).accept(any(RaceLogEventVisitor.class));
        
        raceLog.add(event1);
        raceLog.add(event2);

        assertEquals(event2.getNextStatus(), analyzer.analyze().getA());
    }

    private static RaceLogStartTimeEvent createStartTimeEvent(long startTimeAsMillis, boolean isGreater) {
        TimePoint startTime = mock(TimePoint.class);
        when(startTime.asMillis()).thenReturn(startTimeAsMillis);
        when(startTime.compareTo(any(TimePoint.class))).thenReturn(isGreater ? 1 : -1);
        RaceLogStartTimeEvent event = createEvent(RaceLogStartTimeEvent.class, 1);
        when(event.getStartTime()).thenReturn(startTime);
        doAnswer(new StartTimeVisitorAnswer()).when(event).accept(any(RaceLogEventVisitor.class));
        return event;
    }
    
    private static RaceLogStartTimeEvent createStartTimeEvent(TimePoint startTime) {
        return new RaceLogStartTimeEventImpl(startTime, new LogEventAuthorImpl("Author", /* priority */ 0), /* pass */ 0, startTime, /* courseAreaId */ null);
    }
    
    private static class StartTimeVisitorAnswer implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            RaceLogStartTimeEvent event = (RaceLogStartTimeEvent) invocation.getMock();
            RaceLogEventVisitor visitor = (RaceLogEventVisitor) invocation.getArguments()[0];
            visitor.visit(event);
            return null;
        }
    }
    
    private static class StatusVisitorAnswer implements Answer<Void> {
        @Override
        public Void answer(InvocationOnMock invocation) throws Throwable {
            RaceLogRaceStatusEvent event = (RaceLogRaceStatusEvent) invocation.getMock();
            RaceLogEventVisitor visitor = (RaceLogEventVisitor) invocation.getArguments()[0];
            visitor.visit(event);
            return null;
        }
    }
}
