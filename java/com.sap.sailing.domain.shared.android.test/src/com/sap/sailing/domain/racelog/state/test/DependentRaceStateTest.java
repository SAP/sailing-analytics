package com.sap.sailing.domain.racelog.state.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult.ResolutionFailed;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogDependentStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogPassChangeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.abstractlog.race.state.RaceState;
import com.sap.sailing.domain.abstractlog.race.state.RaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.ReadonlyRaceState;
import com.sap.sailing.domain.abstractlog.race.state.impl.BaseRaceStateChangedListener;
import com.sap.sailing.domain.abstractlog.race.state.impl.RaceStateImpl;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.RacingProcedureFactory;
import com.sap.sailing.domain.abstractlog.race.state.racingprocedure.impl.RacingProcedureFactoryImpl;
import com.sap.sailing.domain.base.configuration.ConfigurationLoader;
import com.sap.sailing.domain.base.configuration.RegattaConfiguration;
import com.sap.sailing.domain.base.configuration.impl.EmptyRegattaConfiguration;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class DependentRaceStateTest {
    private static class RaceStateImplWithPublicRaceStateToObserve extends RaceStateImpl {
        public RaceStateImplWithPublicRaceStateToObserve(RaceLogResolver raceLogResolver, RaceLog raceLog,
                AbstractLogEventAuthor author, RacingProcedureFactory procedureFactory) {
            super(raceLogResolver, raceLog, author, procedureFactory);
        }

        @Override
        public ReadonlyRaceState getRaceStateToObserve() {
            return super.getRaceStateToObserve();
        }
    }

    private RaceLog raceLogA;
    private RaceLog raceLogB;
    private RaceLog raceLogC;

    private RaceState stateA;
    private RaceStateImplWithPublicRaceStateToObserve stateB;
    private RaceState stateC;

    private RaceStateChangedListener listenerA;
    private RaceStateChangedListener listenerB;
    private RaceStateChangedListener listenerC;

    private AbstractLogEventAuthor author;
    private ConfigurationLoader<RegattaConfiguration> configuration;

    private TimePoint nowMock;

    private RaceLogResolver raceLogResolver;

    @BeforeEach
    public void setUp() {
        author = new LogEventAuthorImpl("Test", 1);
        configuration = new EmptyRegattaConfiguration();
        raceLogA = new RaceLogImpl("raceLogA");
        raceLogB = new RaceLogImpl("raceLogB");
        raceLogC = new RaceLogImpl("raceLogC");
        listenerA = mock(RaceStateChangedListener.class);
        listenerB = mock(RaceStateChangedListener.class);
        listenerC = mock(RaceStateChangedListener.class);
        nowMock = mock(TimePoint.class);
        raceLogResolver = new RaceLogResolver() {
            @Override
            public RaceLog resolve(SimpleRaceLogIdentifier identifier) {
                if (identifier.getRegattaLikeParentName().equals("A")) {
                    return raceLogA;
                } else if (identifier.getRegattaLikeParentName().equals("B")) {
                    return raceLogB;
                } else {
                    return raceLogC;
                }
            }
        };
        stateA = new RaceStateImpl(raceLogResolver, raceLogA, author, new RacingProcedureFactoryImpl(author, configuration));
        stateB = new RaceStateImplWithPublicRaceStateToObserve(raceLogResolver, raceLogB, author, new RacingProcedureFactoryImpl(author, configuration));
        stateC = new RaceStateImpl(raceLogResolver, raceLogC, author, new RacingProcedureFactoryImpl(author, configuration));
        stateA.addChangedListener(listenerA);
        stateB.addChangedListener(listenerB);
        stateC.addChangedListener(listenerC);
    }

    @Test
    public void testCorrectAmountOfStartTimeChanges() {
        raceLogC.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("B", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        raceLogB.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("A", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        TimePoint now = MillisecondsTimePoint.now();
        raceLogA.add(new RaceLogStartTimeEventImpl(now, now, author, "12", 12, new MillisecondsTimePoint(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        verify(listenerC, times(3)).onStartTimeChanged(stateC);
        verify(listenerB, times(2)).onStartTimeChanged(stateB);
        verify(listenerA, times(1)).onStartTimeChanged(stateA);
    }

    /**
     * See bug 4197
     */
    @Test
    public void testCyclicStartTimeDependencyDoesNotLeadToEndlessRecursion() {
        raceLogC.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("B", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        raceLogB.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("A", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        raceLogA.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("C", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        assertEquals(ResolutionFailed.CYCLIC_DEPENDENCY, stateA.getStartTimeFinderResult().getResolutionFailed());
    }

    @Test
    public void testInitialRegistrationOfRaceState() {
        final MillisecondsDurationImpl delta = new MillisecondsDurationImpl(5000);
        raceLogB.add(new RaceLogDependentStartTimeEventImpl(nowMock, author, 12,
                new SimpleRaceLogIdentifierImpl("A", "", ""), delta, /* courseAreaId */ null));
        final RaceState stateB2 = new RaceStateImpl(raceLogResolver, raceLogB, author, new RacingProcedureFactoryImpl(
                author, configuration));
        final TimePoint[] bTime = new TimePoint[1];
        stateB2.addChangedListener(new BaseRaceStateChangedListener() {
            @Override
            public void onStartTimeChanged(ReadonlyRaceState state) {
                bTime[0] = state.getStartTime();
            }
        });
        TimePoint now = MillisecondsTimePoint.now();
        raceLogA.add(new RaceLogStartTimeEventImpl(now, author, 12, now, /* courseAreaId */ null));
        assertEquals(now.plus(delta), bTime[0]);
    }

    @Test
    public void testCorrectAmountOfStartTimeChanges2() {
        raceLogC.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("B", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        raceLogB.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("A", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        TimePoint now = MillisecondsTimePoint.now();
        raceLogA.add(new RaceLogStartTimeEventImpl(now, now, author, "12", 12, new MillisecondsTimePoint(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        raceLogB.add(new RaceLogStartTimeEventImpl(now, now, author, "12", 12, new MillisecondsTimePoint(20000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        verify(listenerA, times(1)).onStartTimeChanged(stateA);
        verify(listenerB, times(3)).onStartTimeChanged(stateB);
        verify(listenerC, times(4)).onStartTimeChanged(stateC);
    }
    
    /**
     * See bug 3740; when the current pass has an absolute start time, adding a relative start time
     * to a prior pass late must not lead to the race state observing the race state of the race that
     * the no longer valid dependent start time specification depended upon.
     */
    @Test
    public void testLateDeliveryOfDependentStartTimeForPriorPass() {
        TimePoint now = MillisecondsTimePoint.now();
        raceLogA.add(new RaceLogStartTimeEventImpl(now, now, author, /* ID */ "1", /* pass */ 0, new MillisecondsTimePoint( 5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        raceLogB.add(new RaceLogStartTimeEventImpl(now, now, author, /* ID */ "2", /* pass */ 0, new MillisecondsTimePoint(10000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        // now B enters a new pass:
        raceLogB.add(new RaceLogPassChangeEventImpl(now.plus(1000), author, /* pass */ 1));
        // this is expected to reset B's start time:
        assertNull(stateB.getStartTime());
        // now B receives an absolute start time definition for pass 1 (the second pass):
        raceLogB.add(new RaceLogStartTimeEventImpl(now.plus(1000), now.plus(1000), author, /* ID */ "3", /* pass */ 1, new MillisecondsTimePoint(20000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        assertEquals(new MillisecondsTimePoint( 5000), stateA.getStartTime());
        assertEquals(new MillisecondsTimePoint(20000), stateB.getStartTime());
        // here comes a late event for B's pass 0, setting a relative start time; it is expected to not
        // have any effect on B's current start time because B is already in pass 1; furthermore, it is
        // expected to not have an effect on the observing relationship. No observer relationship is to
        // be established from B to A; in particular, updating A's start time is expected to leave B's
        // start time unmodified.
        raceLogB.add(new RaceLogDependentStartTimeEventImpl(now.plus(500), now.plus(500), author, /* ID */ "4", /* pass */ 0,
                new SimpleRaceLogIdentifierImpl("A", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        
        // verify that B's race state is *not* observing A's race state
        assertNull(stateB.getRaceStateToObserve());
        
        // B's absolute start time in pass 1 is expected to remain unchanged
        assertEquals(new MillisecondsTimePoint(20000), stateB.getStartTime());
        // moving A's start time is expected to NOT move B's start time:
        raceLogA.add(new RaceLogStartTimeEventImpl(now.plus(10000), now.plus(10000), author, /* ID */ "5", /* pass */ 0,
                /* new start time */ new MillisecondsTimePoint(20000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        assertEquals(new MillisecondsTimePoint(20000), stateA.getStartTime());
        assertEquals(new MillisecondsTimePoint(20000), stateB.getStartTime()); // no update to B's start time expected
    }
    
    /**
     * See bug 3740; the current pass is set up such that it has a relative start time; then we assume
     * that an absolute start time for a prior pass is delivered late. Now it's important that this does
     * not lead to the dependent race state stopping to observe the race state it depends upon.
     */
    @Test
    public void testLateDeliveryOfAbsoluteStartTimeForPrioPass() {
        TimePoint now = MillisecondsTimePoint.now();
        raceLogA.add(new RaceLogStartTimeEventImpl(now, now, author, /* ID */ "1", /* pass */ 0, new MillisecondsTimePoint( 5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        raceLogB.add(new RaceLogStartTimeEventImpl(now, now, author, /* ID */ "2", /* pass */ 0, new MillisecondsTimePoint(10000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        // now B enters a new pass:
        raceLogB.add(new RaceLogPassChangeEventImpl(now.plus(1000), author, /* pass */ 1));
        // this is expected to reset B's start time:
        assertNull(stateB.getStartTime());
        // now B receives a start time definition relative to that of A:
        raceLogB.add(new RaceLogDependentStartTimeEventImpl(now.plus(2000), now.plus(2000), author, /* ID */ "3", /* pass */ 1,
                new SimpleRaceLogIdentifierImpl("A", "", ""), new MillisecondsDurationImpl(5000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        assertEquals(new MillisecondsTimePoint( 5000), stateA.getStartTime());
        assertEquals(stateA.getStartTime().plus(5000), stateB.getStartTime()); // B is expected to have a start time 5s after that of A due to its relative start time
        // moving A's start time is expected to move B's start time:
        raceLogA.add(new RaceLogStartTimeEventImpl(now.plus(10000), now.plus(10000), author, /* ID */ "4", /* pass */ 0,
                /* new start time */ new MillisecondsTimePoint(20000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        assertEquals(new MillisecondsTimePoint(20000), stateA.getStartTime());
        assertEquals(stateA.getStartTime().plus(5000), stateB.getStartTime()); // B is expected to have a start time 5s after that of A due to its relative start time
        // here comes a late event for B's pass 0, updating the absolute start time; it is expected to not
        // have any effect on B's current start time because B is already in pass 1; furthermore, it is
        // expected to not have an effect on the observing relationship: when updating A's start time
        // later, B's relative start time is expected to still update.
        raceLogB.add(new RaceLogStartTimeEventImpl(now.plus(10), now.plus(10), author, /* ID */ "5", /* pass */ 0, new MillisecondsTimePoint(11000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        assertEquals(stateA.getStartTime().plus(5000), stateB.getStartTime()); // B is expected to still have a start time 5s after that of A due to its relative start time
        // move A's start time to then check that B's still relative start time has moved as well, testing the
        // continued existence of the observer relationship:
        raceLogA.add(new RaceLogStartTimeEventImpl(now.plus(11000), now.plus(11000), author, /* ID */ "6", /* pass */ 0,
                /* new start time */ new MillisecondsTimePoint(30000), RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        assertEquals(new MillisecondsTimePoint(30000), stateA.getStartTime());
        assertEquals(stateA.getStartTime().plus(5000), stateB.getStartTime()); // B is expected to still have a start time 5s after that of A due to its relative start time
    }
}
