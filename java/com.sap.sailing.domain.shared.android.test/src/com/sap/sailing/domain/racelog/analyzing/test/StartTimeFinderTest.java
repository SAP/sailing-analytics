package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogResolver;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinder;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.StartTimeFinderResult;
import com.sap.sse.common.TimePoint;

public class StartTimeFinderTest extends PassAwareRaceLogAnalyzerTest<StartTimeFinder, StartTimeFinderResult> {

    @Override
    protected StartTimeFinder createAnalyzer(RaceLog raceLog) {
        return new StartTimeFinder(mock(RaceLogResolver.class), raceLog);
    }

    @Override
    protected TargetPair getTargetEventsAndResultForPassAwareTests(int passId, AbstractLogEventAuthor author) {
        RaceLogStartTimeEvent event = createEvent(RaceLogStartTimeEvent.class, 1, passId, author);
        when(event.getStartTime()).thenReturn(mock(TimePoint.class));
        return new TargetPair(Arrays.asList(event), new StartTimeFinderResult(event.getStartTime(),
                /* startTimeDiff */ null, /* courseAreaId */ null));
    }

    @Test
    public void testNullForNone() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        raceLog.add(event1);
        assertNull(analyzer.analyze().getStartTime());
    }

    @Test
    public void testMostRecent() {
        RaceLogStartTimeEvent event1 = createEvent(RaceLogStartTimeEvent.class, 1);
        when(event1.getStartTime()).thenReturn(mock(TimePoint.class));
        RaceLogStartTimeEvent event2 = createEvent(RaceLogStartTimeEvent.class, 2);
        when(event2.getStartTime()).thenReturn(mock(TimePoint.class));
        raceLog.add(event1);
        raceLog.add(event2);
        assertEquals(event2.getStartTime(), analyzer.analyze().getStartTime());
    }
}
