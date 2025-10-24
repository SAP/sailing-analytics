package com.sap.sailing.domain.racelog.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEventComparator;
import com.sap.sse.common.Timed;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceLogEventComparatorTest {

    private RaceLogEvent eventOne;
    private RaceLogEvent eventTwo;
    private AbstractLogEventAuthor author;
    
    private RaceLogEventComparator comparator;
    
    @BeforeEach
    public void setUp() {
        eventOne = mock(RaceLogEvent.class);
        eventTwo = mock(RaceLogEvent.class);
        author = mock(AbstractLogEventAuthor.class);
        
        comparator = new RaceLogEventComparator();
    }
    
    @Test
    public void testEqualsOnSame() {
        when(eventOne.getAuthor()).thenReturn(mock(AbstractLogEventAuthor.class));
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        when(eventOne.getId()).thenReturn("a");
        
        int result = comparator.compare(eventOne, eventOne);
        assertEquals(0, result);
    }
    
    @Test
    public void testSamePassAndSameAuthorAndSameTimeAndSameId() {
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(author);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        when(eventOne.getId()).thenReturn("a");
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(author);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getId()).thenReturn("a");
        
        int result = comparator.compare(eventOne, eventTwo);
        assertEquals(0, result);
    }
    
    @Test
    public void testSamePassAndSameAuthorAndSameTimeButDifferentId() {
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(author);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        when(eventOne.getId()).thenReturn("a");
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(author);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getId()).thenReturn("b");
        
        int result = comparator.compare(eventOne, eventTwo);
        assertTrue(result < 0);
    }
    
    @Test
    public void testSamePassAndSameAuthorButLowerHigherTime() {
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(author);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(author);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(2));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(2));
        
        int result = comparator.compare(eventOne, eventTwo);
        assertTrue(result < 0);
    }
    
    @Test
    public void testSamePassAndSameAuthorButHigherLowerTime() {
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(author);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(2));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(2));
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(author);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        
        int result = comparator.compare(eventOne, eventTwo);
        assertTrue(result > 0);
    }
    
    @Test
    public void testSamePassButLowerHigherAuthor() {
        AbstractLogEventAuthor minorAuthor = mock(AbstractLogEventAuthor.class);
        AbstractLogEventAuthor majorAuthor = mock(AbstractLogEventAuthor.class);
        when(minorAuthor.compareTo(majorAuthor)).thenReturn(-1);
        when(majorAuthor.compareTo(minorAuthor)).thenReturn(1);
        
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(minorAuthor);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(majorAuthor);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        
        int result = comparator.compare(eventOne, eventTwo);
        assertTrue(result < 0);
    }
    
    @Test
    public void testSamePassButHigherLowerAuthor() {
        AbstractLogEventAuthor minorAuthor = mock(AbstractLogEventAuthor.class);
        AbstractLogEventAuthor majorAuthor = mock(AbstractLogEventAuthor.class);
        when(minorAuthor.compareTo(majorAuthor)).thenReturn(-1);
        when(majorAuthor.compareTo(minorAuthor)).thenReturn(1);
        
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(majorAuthor);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(minorAuthor);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        
        int result = comparator.compare(eventOne, eventTwo);
        assertTrue(result > 0);
    }
    
    @Test
    public void testPassLowerHigher() {
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(2));
        when(eventTwo.getPassId()).thenReturn(1);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        
        int result = comparator.compare(eventOne, eventTwo);
        assertTrue(result < 0);
    }
    
    @Test
    public void testPassHigherLower() {
        when(eventOne.getPassId()).thenReturn(1);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(2));
        
        int result = comparator.compare(eventOne, eventTwo);
        assertTrue(result > 0);
    }
    
    @Test
    public void testFallbackForTimed() {
        when(eventOne.getPassId()).thenReturn(1);
        when(eventOne.getTimePoint()).thenReturn(new MillisecondsTimePoint(1));
        Timed timed = mock(Timed.class);
        when(timed.getTimePoint()).thenReturn(new MillisecondsTimePoint(2));
        
        int result = comparator.compare(eventOne, timed);
        assertTrue(result < 0);
    }
    
}
