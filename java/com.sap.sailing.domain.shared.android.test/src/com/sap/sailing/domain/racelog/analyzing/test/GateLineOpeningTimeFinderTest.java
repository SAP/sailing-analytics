package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.junit.Test;

import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogGateLineOpeningTimeEvent;
import com.sap.sailing.domain.racelog.RaceLogGateLineOpeningTimeEvent.GateLineOpeningTimes;
import com.sap.sailing.domain.racelog.analyzing.impl.GateLineOpeningTimeFinder;

public class GateLineOpeningTimeFinderTest extends PassAwareRaceLogAnalyzerTest<GateLineOpeningTimeFinder, GateLineOpeningTimes> {

    @Override
    protected GateLineOpeningTimeFinder createAnalyzer(RaceLog raceLog) {
        return new GateLineOpeningTimeFinder(raceLog);
    }

    @Override
    protected TargetPair getTargetEventsAndResultForPassAwareTests(int passId, RaceLogEventAuthor author) {
        RaceLogGateLineOpeningTimeEvent event = createEvent(RaceLogGateLineOpeningTimeEvent.class, 1, passId, author);
        when(event.getGateLineOpeningTimes()).thenReturn(new GateLineOpeningTimes(2, 3));
        return new TargetPair(Arrays.asList(event), new GateLineOpeningTimes(2, 3));
    }
    
    @Test
    public void testNullForNone() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        raceLog.add(event1);
        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testMostRecent() {
        RaceLogGateLineOpeningTimeEvent event1 = createEvent(RaceLogGateLineOpeningTimeEvent.class, 1);
        when(event1.getGateLineOpeningTimes()).thenReturn(new GateLineOpeningTimes(1, 2));
        RaceLogGateLineOpeningTimeEvent event2 = createEvent(RaceLogGateLineOpeningTimeEvent.class, 2);
        when(event2.getGateLineOpeningTimes()).thenReturn(new GateLineOpeningTimes(2, 3));
        
        raceLog.add(event1);
        raceLog.add(event2);

        assertEquals(event2.getGateLineOpeningTimes(), analyzer.analyze());
    }
}
