package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.LastFlagsFinder;

public class LastFlagsFinderTest extends RaceLogAnalyzerTest<LastFlagsFinder> {

    @Override
    protected LastFlagsFinder createAnalyzer(RaceLog raceLog) {
        return new LastFlagsFinder(raceLog);
    }

    @Test
    public void testNullForNoFlag() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        raceLog.add(event1);

        assertNull(analyzer.analyze());
    }
    
    @Test
    public void testSingleLast() {
        RaceLogFlagEvent event1 = createEvent(RaceLogFlagEvent.class, 1);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 2);
        
        raceLog.add(event1);
        raceLog.add(event2);

        List<RaceLogFlagEvent> result = analyzer.analyze();
        assertEquals(1, result.size());
        assertEquals(event2, result.get(0));
    }
    
    @Test
    public void testSingleInBetween() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 2);
        RaceLogEvent event3 = createEvent(RaceLogEvent.class, 3);
        
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);

        List<RaceLogFlagEvent> result = analyzer.analyze();
        assertEquals(1, result.size());
        assertEquals(event2, result.get(0));
    }
    
    @Test
    public void testTwoLast() {
        RaceLogFlagEvent event1 = createEvent(RaceLogFlagEvent.class, 1);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 2, "a");
        RaceLogFlagEvent event3 = createEvent(RaceLogFlagEvent.class, 2, "b");
        
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);

        List<RaceLogFlagEvent> result = analyzer.analyze();
        assertEquals(2, result.size());
        assertEquals(event3, result.get(0));
        assertEquals(event2, result.get(1));
    }    
    
    @Test
    public void testTwoLastWithInBetween() {
        RaceLogFlagEvent event1 = createEvent(RaceLogFlagEvent.class, 1);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 2, "a");
        RaceLogEvent event3 = createEvent(RaceLogEvent.class, 2, "b");
        RaceLogFlagEvent event4 = createEvent(RaceLogFlagEvent.class, 2, "c");
        
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);
        raceLog.add(event4);

        List<RaceLogFlagEvent> result = analyzer.analyze();
        assertEquals(2, result.size());
        assertEquals(event4, result.get(0));
        assertEquals(event2, result.get(1));
    }
    
    @Test
    public void testMoreThanTwoLast() {
        RaceLogFlagEvent event1 = createEvent(RaceLogFlagEvent.class, 1);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 2, "a");
        RaceLogFlagEvent event3 = createEvent(RaceLogFlagEvent.class, 2, "b");
        RaceLogFlagEvent event4 = createEvent(RaceLogFlagEvent.class, 2, "c");
        
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);
        raceLog.add(event4);

        List<RaceLogFlagEvent> result = analyzer.analyze();
        assertEquals(3, result.size());
        assertEquals(event4, result.get(0));
        assertEquals(event3, result.get(1));
        assertEquals(event2, result.get(2));
    }
    
    @Test
    public void testGetMostRecentDisplayedNullOnNull() {
        assertNull(LastFlagsFinder.getMostRecent(null));
    }
    
    @Test
    public void testGetMostRecentDisplayedNullOnEmpty() {
        assertNull(LastFlagsFinder.getMostRecent(new ArrayList<RaceLogFlagEvent>()));
    }
    
    @Test
    public void testGetMostRecentDisplayedDisplayedOverRemoved() {
        List<RaceLogFlagEvent> events = new ArrayList<RaceLogFlagEvent>();
        RaceLogFlagEvent event1 = createEvent(RaceLogFlagEvent.class, 1);
        when(event1.isDisplayed()).thenReturn(false);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 1);
        when(event2.isDisplayed()).thenReturn(true);
        
        events.add(event1);
        events.add(event2);
        
        assertEquals(event2, LastFlagsFinder.getMostRecent(events));
        
        // order independent?
        Collections.reverse(events);
        assertEquals(event2, LastFlagsFinder.getMostRecent(events));
    }
    
    @Test
    public void testGetMostRecentDisplayedMultipleDisplayedOverRemoved() {
        List<RaceLogFlagEvent> events = new ArrayList<RaceLogFlagEvent>();
        RaceLogFlagEvent event1 = createEvent(RaceLogFlagEvent.class, 1);
        when(event1.isDisplayed()).thenReturn(false);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 1, "a");
        when(event2.isDisplayed()).thenReturn(true);
        RaceLogFlagEvent event3 = createEvent(RaceLogFlagEvent.class, 1, "b");
        when(event3.isDisplayed()).thenReturn(true);
        
        events.add(event1);
        events.add(event2);
        events.add(event3);
        
        assertEquals(event3, LastFlagsFinder.getMostRecent(events));
    }
    
    @Test
    public void testGetMostRecentDisplayedMultipleRemoved() {
        List<RaceLogFlagEvent> events = new ArrayList<RaceLogFlagEvent>();
        RaceLogFlagEvent event1 = createEvent(RaceLogFlagEvent.class, 1, "a");
        when(event1.isDisplayed()).thenReturn(false);
        RaceLogFlagEvent event2 = createEvent(RaceLogFlagEvent.class, 1, "b");
        when(event2.isDisplayed()).thenReturn(false);
        
        events.add(event1);
        events.add(event2);
        
        assertEquals(event2, LastFlagsFinder.getMostRecent(events));
    }
}
