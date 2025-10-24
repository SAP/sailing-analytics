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
import com.sap.sailing.domain.abstractlog.race.RaceLogProtestStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.ProtestTimeFinder;
import com.sap.sse.common.TimeRange;

public class ProtestStartTimeFinderTest extends PassAwareRaceLogAnalyzerTest<ProtestTimeFinder, TimeRange> {

    @Override
    protected ProtestTimeFinder createAnalyzer(RaceLog raceLog) {
        return new ProtestTimeFinder(raceLog);
    }

    @Override
    protected TargetPair getTargetEventsAndResultForPassAwareTests(int passId, AbstractLogEventAuthor author) {
        RaceLogProtestStartTimeEvent event = createEvent(RaceLogProtestStartTimeEvent.class, 1, passId, author);
        when(event.getProtestTime()).thenReturn(mock(TimeRange.class));
        return new TargetPair(Arrays.asList(event), event.getProtestTime());
    }
    
    @Test
    public void testNullForNone() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        raceLog.add(event1);
        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testMostRecent() {
        RaceLogProtestStartTimeEvent event1 = createEvent(RaceLogProtestStartTimeEvent.class, 1);
        when(event1.getProtestTime()).thenReturn(mock(TimeRange.class));
        RaceLogProtestStartTimeEvent event2 = createEvent(RaceLogProtestStartTimeEvent.class, 2);
        when(event2.getProtestTime()).thenReturn(mock(TimeRange.class));
        
        raceLog.add(event1);
        raceLog.add(event2);

        assertEquals(event2.getProtestTime(), analyzer.analyze());
    }
}
