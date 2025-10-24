package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collection;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.RaceLogAnalyzer;

public abstract class PassAwareRaceLogAnalyzerTest<AnalyzerType extends RaceLogAnalyzer<ResultType>, ResultType>
        extends RaceLogAnalyzerTest<AnalyzerType> {
    
    protected class TargetPair extends com.sap.sse.common.Util.Pair<Collection<? extends RaceLogEvent>, ResultType> {
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
    protected abstract TargetPair getTargetEventsAndResultForPassAwareTests(int passId, AbstractLogEventAuthor author);
    
    /**
     * Most of the times you do not need to implement this (only when your ResultType is a primitive). 
     */
    protected TargetPair getBlockingEventsAndResultForPassAwareTests(int passId, AbstractLogEventAuthor author) {
        return getTargetEventsAndResultForPassAwareTests(passId, author);
    }
    
    @Test
    public void testPassAwareWrongPass() {
        AbstractLogEventAuthor author = mock(AbstractLogEventAuthor.class);
        TargetPair pair = getTargetEventsAndResultForPassAwareTests(0, author);
        ResultType nonExpectedResult = pair.getB();
        RaceLogEvent blockingEvent = createEvent(RaceLogEvent.class, 1, 1, UUID.randomUUID(), author);
        
        pair.addAll(raceLog);
        raceLog.add(blockingEvent);

        assertNotSame(nonExpectedResult, analyzer.analyze());
    }
    
    @Test
    public void testPassAwareCorrectPass() {
        AbstractLogEventAuthor author = mock(AbstractLogEventAuthor.class);
        RaceLogEvent minorEvent = createEvent(RaceLogEvent.class, 0, 0, UUID.randomUUID(), author);
        TargetPair pair = getTargetEventsAndResultForPassAwareTests(1, author);
        ResultType expectedResult = pair.getB();
        pair.addAll(raceLog);
        raceLog.add(minorEvent);
        assertEquals(expectedResult, analyzer.analyze());
    }
    
    @Test
    public void testPassAwareBlockedByMajorAuthor() {
        AbstractLogEventAuthor minorAuthor = mock(AbstractLogEventAuthor.class);
        AbstractLogEventAuthor majorAuthor = mock(AbstractLogEventAuthor.class);
        when(minorAuthor.compareTo(majorAuthor)).thenReturn(-1);
        when(majorAuthor.compareTo(minorAuthor)).thenReturn(1);
        TargetPair minorPair = getTargetEventsAndResultForPassAwareTests(0, minorAuthor);
        minorPair.addAll(raceLog);
        TargetPair majorPair = getBlockingEventsAndResultForPassAwareTests(0, majorAuthor);
        majorPair.addAll(raceLog);
        assertNotSame(minorPair.getB(), analyzer.analyze());
        assertEquals(majorPair.getB(), analyzer.analyze());
    }
    
    @Test
    public void testPassAwareHidingMinorAuthor() {
        AbstractLogEventAuthor minorAuthor = mock(AbstractLogEventAuthor.class);
        AbstractLogEventAuthor majorAuthor = mock(AbstractLogEventAuthor.class);
        when(minorAuthor.compareTo(majorAuthor)).thenReturn(-1);
        when(majorAuthor.compareTo(minorAuthor)).thenReturn(1);
        
        TargetPair majorPair = getTargetEventsAndResultForPassAwareTests(0, majorAuthor);
        majorPair.addAll(raceLog);
        
        TargetPair minorPair = getBlockingEventsAndResultForPassAwareTests(0, minorAuthor);
        minorPair.addAll(raceLog);

        assertEquals(majorPair.getB(), analyzer.analyze());
        assertNotSame(minorPair.getB(), analyzer.analyze());
    }
}
