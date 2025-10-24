package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.WindFixesFinder;
import com.sap.sailing.domain.common.Wind;

public class WindFixesFinderTest extends RaceLogAnalyzerTest<WindFixesFinder> {

    @Override
    protected WindFixesFinder createAnalyzer(RaceLog raceLog) {
        return new WindFixesFinder(raceLog);
    }
    
    @Test
    public void testEmptyForNone() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        
        raceLog.add(event1);
        
        List<RaceLogWindFixEvent> windList = analyzer.analyze();
        assertNotNull(windList);
        assertEquals(0, windList.size());
    }
    
    @Test
    public void testWindFixList() {
        RaceLogFlagEvent event1 = createEvent(RaceLogFlagEvent.class, 1);
        RaceLogWindFixEvent event2 = createEvent(RaceLogWindFixEvent.class, 2);
        RaceLogWindFixEvent event3 = createEvent(RaceLogWindFixEvent.class, 3);
        when(event2.getWindFix()).thenReturn(mock(Wind.class));
        when(event3.getWindFix()).thenReturn(mock(Wind.class));
        
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);

        List<RaceLogWindFixEvent> windList = analyzer.analyze();
        assertNotNull(windList);
        assertEquals(2, windList.size());
        assertEquals(event3.getWindFix(), windList.get(0).getWindFix());
        assertEquals(event2.getWindFix(), windList.get(1).getWindFix());
    }
}
