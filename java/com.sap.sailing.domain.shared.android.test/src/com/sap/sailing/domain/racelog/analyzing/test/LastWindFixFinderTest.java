package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogWindFixEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.LastWindFixFinder;
import com.sap.sailing.domain.common.Wind;

public class LastWindFixFinderTest extends RaceLogAnalyzerTest<LastWindFixFinder> {

    @Override
    protected LastWindFixFinder createAnalyzer(RaceLog raceLog) {
        return new LastWindFixFinder(raceLog);
    }
    
    @Test
    public void testNullForNone() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        raceLog.add(event1);
        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testMostRecent() {
        RaceLogWindFixEvent event1 = createEvent(RaceLogWindFixEvent.class, 1);
        RaceLogWindFixEvent event2 = createEvent(RaceLogWindFixEvent.class, 2);
        when(event2.getWindFix()).thenReturn(mock(Wind.class));
        
        raceLog.add(event1);
        raceLog.add(event2);

        assertEquals(event2.getWindFix(), analyzer.analyze());
    }
}
