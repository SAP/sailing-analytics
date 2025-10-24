package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.SimpleRaceLogIdentifier;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogDependentStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.SimpleRaceLogIdentifierImpl;
import com.sap.sailing.domain.common.racelog.RaceLogRaceStatus;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class DependentStartTimeFinderTest {

    private RaceLog raceLogA;
    private RaceLog raceLogB;
    private RaceLog raceLogC;

    private AbstractLogEventAuthor author;

    private TimePoint nowMock;

    private RaceLogResolver raceLogResolver;

    @BeforeEach
    public void setUp() {
        author = new LogEventAuthorImpl("Test", 1);
        raceLogA = new RaceLogImpl("raceLogA");
        raceLogB = new RaceLogImpl("raceLogB");
        raceLogC = new RaceLogImpl("raceLogC");
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
    }

    @Test
    public void testDependentStartTimeUpdate() {
        final MillisecondsDurationImpl cAfterB = new MillisecondsDurationImpl(5000);
        raceLogC.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("B", "", ""), cAfterB, RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));

        StartTimeFinder finder = new StartTimeFinder(raceLogResolver, raceLogC);
        // Tests for correct behaviour, when race depending on has no start time set
        assertNull(finder.analyze().getStartTime());

        final MillisecondsDurationImpl bAfterA = new MillisecondsDurationImpl(6000);
        raceLogB.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("A", "", ""), bAfterA, RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));

        finder = new StartTimeFinder(raceLogResolver, raceLogB);
        StartTimeFinderResult result = finder.analyze();

        List<SimpleRaceLogIdentifier> expectedDependingOnRaces = new ArrayList<>();
        expectedDependingOnRaces.add(new SimpleRaceLogIdentifierImpl("A", "", ""));
        assertNull(result.getStartTime());
        assertEquals(bAfterA, result.getStartTimeDiff());
        assertEquals(expectedDependingOnRaces, result.getDependingOnRaces());
        // assertEquals(5000, result.getStartTimeDiff().asMillis());

        finder = new StartTimeFinder(raceLogResolver, raceLogC);
        // Tests for correct behaviour, when race depending on has a DependentStartTimeEvent and its parent start time
        // is unknown
        expectedDependingOnRaces = new ArrayList<>();
        expectedDependingOnRaces.add(new SimpleRaceLogIdentifierImpl("B", "", ""));
        expectedDependingOnRaces.add(new SimpleRaceLogIdentifierImpl("A", "", ""));

        result = finder.analyze();
        assertNull(result.getStartTime());
        assertEquals(cAfterB, result.getStartTimeDiff());
        assertEquals(expectedDependingOnRaces, result.getDependingOnRaces());

        TimePoint now = MillisecondsTimePoint.now();
        raceLogA.add(new RaceLogStartTimeEventImpl(now, now, author, "12", 12, new MillisecondsTimePoint(5000),
                RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));

        finder = new StartTimeFinder(raceLogResolver, raceLogB);
        result = finder.analyze();
        assertEquals(11000, result.getStartTime().asMillis());
        assertEquals(6000, result.getStartTimeDiff().asMillis());

        finder = new StartTimeFinder(raceLogResolver, raceLogC);
        result = finder.analyze();
        assertEquals(16000, result.getStartTime().asMillis());
        assertEquals(5000, result.getStartTimeDiff().asMillis());

        // Test correct behaviour, when middle element changes
        raceLogB.add(new RaceLogStartTimeEventImpl(now, now, author, "12", 12, new MillisecondsTimePoint(15000),
                RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));
        finder = new StartTimeFinder(raceLogResolver, raceLogA);
        result = finder.analyze();
        assertEquals(5000, result.getStartTime().asMillis());
        assertNull(result.getStartTimeDiff());

        finder = new StartTimeFinder(raceLogResolver, raceLogB);
        result = finder.analyze();
        assertEquals(15000, result.getStartTime().asMillis());
        assertNull(result.getStartTimeDiff());

        finder = new StartTimeFinder(raceLogResolver, raceLogC);
        result = finder.analyze();
        assertEquals(20000, result.getStartTime().asMillis());
        assertEquals(5000, result.getStartTimeDiff().asMillis());
    }

    @Test
    public void testDependentStartTimeCycle() {
        final MillisecondsDurationImpl cAfterB = new MillisecondsDurationImpl(6000);
        raceLogC.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("B", "", ""), cAfterB, RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));

        final MillisecondsDurationImpl bAfterA = new MillisecondsDurationImpl(7000);
        raceLogB.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("A", "", ""), bAfterA, RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));

        final MillisecondsDurationImpl aAfterC = new MillisecondsDurationImpl(8000);
        raceLogA.add(new RaceLogDependentStartTimeEventImpl(nowMock, nowMock, author, "12", 12,
                new SimpleRaceLogIdentifierImpl("C", "", ""), aAfterC, RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));

        // Check that all resolve to null in case of a cycle
        StartTimeFinder finder = new StartTimeFinder(raceLogResolver, raceLogC);
        assertNull(finder.analyze().getStartTime());
        assertEquals(cAfterB, finder.analyze().getStartTimeDiff());

        finder = new StartTimeFinder(raceLogResolver, raceLogB);
        assertNull(finder.analyze().getStartTime());
        assertEquals(bAfterA, finder.analyze().getStartTimeDiff());

        finder = new StartTimeFinder(raceLogResolver, raceLogA);
        assertNull(finder.analyze().getStartTime());
        assertEquals(aAfterC, finder.analyze().getStartTimeDiff());

        // Check that all resolve correctly after changing some element in cycle
        TimePoint now = MillisecondsTimePoint.now();
        raceLogB.add(new RaceLogStartTimeEventImpl(now, now, author, "12", 12, new MillisecondsTimePoint(5000),
                RaceLogRaceStatus.SCHEDULED, /* courseAreaId */ null));

        // now A -> C -> B
        finder = new StartTimeFinder(raceLogResolver, raceLogA);
        // Tests for correct behaviour, when race depending on has a DependentStartTimeEvent and its parent start time
        // is unknown
        List<SimpleRaceLogIdentifier> expectedDependingOnRaces;
        expectedDependingOnRaces = new ArrayList<>();
        expectedDependingOnRaces.add(new SimpleRaceLogIdentifierImpl("C", "", ""));
        expectedDependingOnRaces.add(new SimpleRaceLogIdentifierImpl("B", "", ""));

        StartTimeFinderResult result = finder.analyze();
        assertEquals(19000, result.getStartTime().asMillis());
        assertEquals(8000, result.getStartTimeDiff().asMillis());
        assertEquals(expectedDependingOnRaces, result.getDependingOnRaces());

        // now C -> B
        finder = new StartTimeFinder(raceLogResolver, raceLogC);
        // Tests for correct behaviour, when race depending on has a DependentStartTimeEvent and its parent start time
        // is unknown
        expectedDependingOnRaces = new ArrayList<>();
        expectedDependingOnRaces.add(new SimpleRaceLogIdentifierImpl("B", "", ""));

        result = finder.analyze();
        assertEquals(11000, result.getStartTime().asMillis());
        assertEquals(6000, result.getStartTimeDiff().asMillis());
        assertEquals(expectedDependingOnRaces, result.getDependingOnRaces());

        // now B
        finder = new StartTimeFinder(raceLogResolver, raceLogB);
        // Tests for correct behaviour, when race depending on has a DependentStartTimeEvent and its parent start time
        // is unknown
        expectedDependingOnRaces = new ArrayList<>();

        result = finder.analyze();
        assertEquals(5000, result.getStartTime().asMillis());
        assertNull(result.getStartTimeDiff());
        assertEquals(expectedDependingOnRaces, result.getDependingOnRaces());
    }
}
