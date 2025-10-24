package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AbortingFlagFinder;
import com.sap.sailing.domain.common.racelog.Flags;

public class AbortingFlagFinderTest extends RaceLogAnalyzerTest<AbortingFlagFinder> {

    @Override
    protected AbortingFlagFinder createAnalyzer(RaceLog raceLog) {
        return new AbortingFlagFinder(raceLog);
    }

    @Test
    public void testNullOnEmpty() {
        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testNullOnNoPreviousPass() {
        int passId = 0;
        RaceLogFlagEvent event1 = createAbortingEvent(passId);
        raceLog.add(event1);
        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testNullOnPreviousPassNotAborted() {
        int passId = 0;
        RaceLogFlagEvent event1 = createNonAbortingEvent(passId);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 1, passId + 1, UUID.randomUUID());
        raceLog.add(event1);
        raceLog.add(event2);
        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testNullOnPassBeforePreviousAborted() {
        int passId = 0;
        RaceLogFlagEvent event1 = createAbortingEvent(passId);
        RaceLogFlagEvent event2 = createNonAbortingEvent(passId + 1);
        RaceLogFlagEvent event3 = createEvent(RaceLogFlagEvent.class, 1, passId + 2, UUID.randomUUID());
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);
        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testPreviousPassAborted() {
        int passId = 0;
        RaceLogFlagEvent event1 = createAbortingEvent(passId);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 1, passId + 1, UUID.randomUUID());
        raceLog.add(event1);
        raceLog.add(event2);
        assertEquals(event1, analyzer.analyze());
    }
    
    @Test
    public void testPreviousPassAbortedReturnsMostRecentAbortEvent() {
        int passId = 0;
        RaceLogFlagEvent event1 = createAbortingEvent(passId, 1);
        RaceLogFlagEvent event2 = createAbortingEvent(passId, 2);
        RaceLogFlagEvent event3 = createEvent(RaceLogFlagEvent.class, 1, passId + 1, UUID.randomUUID());
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);
        assertEquals(event2, analyzer.analyze());
    }
    
    @Test
    public void testPreviousPassAndPassBeforePreviousAborted() {
        int passId = 0;
        RaceLogFlagEvent event1 = createAbortingEvent(passId);
        RaceLogFlagEvent event2 = createAbortingEvent(passId + 1);
        RaceLogFlagEvent event3 = createEvent(RaceLogFlagEvent.class, 1, passId + 2, UUID.randomUUID());
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);
        assertEquals(event2, analyzer.analyze());
    }
    
    private static RaceLogFlagEvent createAbortingEvent(int passId) {
        return createAbortingEvent(passId, 1);
    }
    
    private static RaceLogFlagEvent createAbortingEvent(int passId, int time) {
        RaceLogFlagEvent event = createEvent(RaceLogFlagEvent.class, time, passId, UUID.randomUUID());
        when(event.getUpperFlag()).thenReturn(Flags.AP);
        when(event.isDisplayed()).thenReturn(true);
        return event;
    }
    
    private static RaceLogFlagEvent createNonAbortingEvent(int passId) {
        RaceLogFlagEvent event = createEvent(RaceLogFlagEvent.class, 1, passId, UUID.randomUUID());
        when(event.getUpperFlag()).thenReturn(Flags.AP);
        when(event.isDisplayed()).thenReturn(false);
        return event;
    }

}
