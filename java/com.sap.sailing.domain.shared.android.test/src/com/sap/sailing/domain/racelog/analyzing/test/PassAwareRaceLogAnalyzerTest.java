package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.UUID;

import org.junit.Test;

import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.analyzing.impl.RaceLogAnalyzer;

public abstract class PassAwareRaceLogAnalyzerTest<AnalyzerType extends RaceLogAnalyzer<ResultType>, ResultType>
        extends RaceLogAnalyzerTest<AnalyzerType> {
    
    protected class TargetPair extends Util.Pair<Collection<? extends RaceLogEvent>, ResultType> {
        private static final long serialVersionUID = 7961879340006633439L;

        public TargetPair(Collection<? extends RaceLogEvent> a, ResultType b) {
            super(a, b);
        }
        
        public void addAll(RaceLog destination) {
            for (RaceLogEvent event : getA()) {
                destination.add(event);
            }
        }
        
    }
    

    /**
     * Implement in your test to drive some pass- and author-specific tests.
     * 
     * @param passId pass to be used by your events
     * @param author author to be used by your events
     * @return {@link TargetPair}
     */
    protected abstract TargetPair getTargetEventsAndResultForPassAwareTests(int passId, RaceLogEventAuthor author);
    
    /**
     * Most of the times you do not need to implement this (only when your ResultType is a primitive). 
     */
    protected TargetPair getBlockingEventsAndResultForPassAwareTests(int passId, RaceLogEventAuthor author) {
        return getTargetEventsAndResultForPassAwareTests(passId, author);
    }
    
    @Test
    public void testPassAwareWrongPass() {
        RaceLogEventAuthor author = mock(RaceLogEventAuthor.class);
        TargetPair pair = getTargetEventsAndResultForPassAwareTests(0, author);
        ResultType nonExpectedResult = pair.getB();
        RaceLogEvent blockingEvent = createEvent(RaceLogEvent.class, 1, 1, UUID.randomUUID(), author);
        
        pair.addAll(raceLog);
        raceLog.add(blockingEvent);

        assertNotSame(nonExpectedResult, analyzer.analyze());
    }
    
    @Test
    public void testPassAwareCorrectPass() {
        RaceLogEventAuthor author = mock(RaceLogEventAuthor.class);
        RaceLogEvent minorEvent = createEvent(RaceLogEvent.class, 0, 0, UUID.randomUUID(), author);
        TargetPair pair = getTargetEventsAndResultForPassAwareTests(1, author);
        ResultType expectedResult = pair.getB();
        
        pair.addAll(raceLog);
        raceLog.add(minorEvent);

        assertSame(expectedResult, analyzer.analyze());
    }
    
    @Test
    public void testPassAwareBlockedByMajorAuthor() {
        RaceLogEventAuthor minorAuthor = mock(RaceLogEventAuthor.class);
        RaceLogEventAuthor majorAuthor = mock(RaceLogEventAuthor.class);
        when(minorAuthor.compareTo(majorAuthor)).thenReturn(-1);
        when(majorAuthor.compareTo(minorAuthor)).thenReturn(1);
        
        TargetPair minorPair = getTargetEventsAndResultForPassAwareTests(0, minorAuthor);
        minorPair.addAll(raceLog);
        
        TargetPair majorPair = getBlockingEventsAndResultForPassAwareTests(0, majorAuthor);
        majorPair.addAll(raceLog);
        
        assertNotSame(minorPair.getB(), analyzer.analyze());
        assertSame(majorPair.getB(), analyzer.analyze());
    }
    
    @Test
    public void testPassAwareHidingMinorAuthor() {
        RaceLogEventAuthor minorAuthor = mock(RaceLogEventAuthor.class);
        RaceLogEventAuthor majorAuthor = mock(RaceLogEventAuthor.class);
        when(minorAuthor.compareTo(majorAuthor)).thenReturn(-1);
        when(majorAuthor.compareTo(minorAuthor)).thenReturn(1);
        
        TargetPair majorPair = getTargetEventsAndResultForPassAwareTests(0, majorAuthor);
        majorPair.addAll(raceLog);
        
        TargetPair minorPair = getBlockingEventsAndResultForPassAwareTests(0, minorAuthor);
        minorPair.addAll(raceLog);

        assertSame(majorPair.getB(), analyzer.analyze());
        assertNotSame(minorPair.getB(), analyzer.analyze());
    }
}
