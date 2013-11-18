package com.sap.sailing.domain.racelog.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.Before;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.RaceLogEvent;
import com.sap.sailing.domain.racelog.RaceLogEventAuthor;
import com.sap.sailing.domain.racelog.RaceLogEventFactory;
import com.sap.sailing.domain.racelog.RaceLogEventVisitor;
import com.sap.sailing.domain.racelog.RaceLogFlagEvent;
import com.sap.sailing.domain.racelog.RaceLogStartTimeEvent;
import com.sap.sailing.domain.racelog.impl.RaceLogEventAuthorImpl;
import com.sap.sailing.domain.racelog.impl.RaceLogImpl;

public class RaceLogTest {
    
    private RaceLog raceLog;
    
    @Before
    public void setUp() {
        raceLog = new RaceLogImpl("testlock", "test-identifier");
    }
    
    @Test(expected = IllegalStateException.class)
    public void testThrowsOnNoLock() {
        raceLog.getRawFixes();
    }
    
    @Test
    public void testEmptyOnInitialize() {
        raceLog.lockForRead();
        assertEquals(0, Util.size(raceLog.getRawFixes()));
        raceLog.unlockAfterRead();
    }
    
    public void testIdentifier() {
        assertEquals("test-identifier", raceLog.getId());
    }
    
    @Test
    public void testAddEvent() {
        RaceLogEvent event = mock(RaceLogEvent.class);
        boolean isAdded = raceLog.add(event);
        
        raceLog.lockForRead();
        assertTrue(isAdded);
        assertEquals(event, Util.get(raceLog.getRawFixes(), 0));
        raceLog.unlockAfterRead();
    }
    
    @Test
    public void testAddEventDifferentPassButSameTimePoint() {
        RaceLogEvent eventOne = mock(RaceLogEvent.class);
        RaceLogEvent eventTwo = mock(RaceLogEvent.class);
        
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(mock(RaceLogEventAuthor.class));
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getId()).thenReturn("a");
        when(eventTwo.getPassId()).thenReturn(1);
        when(eventTwo.getAuthor()).thenReturn(mock(RaceLogEventAuthor.class));
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventTwo.getId()).thenReturn("b");
        
        assertTrue(raceLog.add(eventOne));
        assertTrue(raceLog.add(eventTwo));
        
        raceLog.lockForRead();
        assertTrue(Util.contains(raceLog.getRawFixes(), eventOne));
        assertTrue(Util.contains(raceLog.getRawFixes(), eventTwo));
        raceLog.unlockAfterRead();
    }
    
    @Test
    public void testAddEventSamePassAndSameAuthorAndSameTimePointButDifferentId() {
        RaceLogEvent eventOne = mock(RaceLogEvent.class);
        RaceLogEvent eventTwo = mock(RaceLogEvent.class);
        RaceLogEventAuthor author = mock(RaceLogEventAuthor.class);
        
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(author);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getId()).thenReturn("a");
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(author);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventTwo.getId()).thenReturn("b");
        
        assertTrue(raceLog.add(eventOne));
        assertTrue(raceLog.add(eventTwo));
        
        raceLog.lockForRead();
        assertTrue(Util.contains(raceLog.getRawFixes(), eventOne));
        assertTrue(Util.contains(raceLog.getRawFixes(), eventTwo));
        raceLog.unlockAfterRead();
    }
    
    @Test
    public void testWontAddEventSamePassAndSameAuthorAndSameTimePointAndSameId() {
        RaceLogEvent eventOne = mock(RaceLogEvent.class);
        RaceLogEvent eventTwo = mock(RaceLogEvent.class);
        RaceLogEventAuthor author = mock(RaceLogEventAuthor.class);
        
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(author);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getId()).thenReturn("a");
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(author);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventTwo.getId()).thenReturn("a");
        
        assertTrue(raceLog.add(eventOne));
        assertFalse(raceLog.add(eventTwo));
        
        raceLog.lockForRead();
        assertTrue(Util.contains(raceLog.getRawFixes(), eventOne));
        assertEquals(1, Util.size(raceLog.getRawFixes()));
        raceLog.unlockAfterRead();
    }
    
    @Test
    public void testAddListener() {
        RaceLogFlagEvent event = mock(RaceLogFlagEvent.class);
        RaceLogEventVisitor listener = mock(RaceLogEventVisitor.class);
        doAnswer(new VisitFlagEventAnswer()).when(event).accept(listener);
        
        raceLog.addListener(listener);
        raceLog.add(event);
        
        verify(listener).visit(event);
    }
    
    @Test
    public void testAddAndRemoveListener() {
        RaceLogFlagEvent event = mock(RaceLogFlagEvent.class);
        RaceLogEventVisitor listener = mock(RaceLogEventVisitor.class);
        doAnswer(new VisitFlagEventAnswer()).when(event).accept(listener);
        
        raceLog.addListener(listener);
        raceLog.removeListener(listener);
        raceLog.add(event);
        
        verify(listener, never()).visit(event);
    }
    
    @Test
    public void testAddListenerWhileNotifying() {
        RaceLogFlagEvent event = mock(RaceLogFlagEvent.class);
        RaceLogEventVisitor listener = mock(RaceLogEventVisitor.class);
        final RaceLogEventVisitor dynamicListener = mock(RaceLogEventVisitor.class);
        doAnswer(new Answer<Void>() {
            @Override
            public Void answer(InvocationOnMock invocation) throws Throwable {
                raceLog.addListener(dynamicListener);
                return null;
            }
        }).when(event).accept(listener);
        
        raceLog.addListener(listener);
        raceLog.add(event);
        
        assertEquals(2, Util.size(raceLog.getAllListeners()));
        assertTrue(Util.contains(raceLog.getAllListeners(), listener));
        assertTrue(Util.contains(raceLog.getAllListeners(), dynamicListener));
        verify(event).accept(listener);
        verify(event, never()).accept(dynamicListener);
    }
    
    @Test
    public void testRawFixesDescending() {
        RaceLogEvent event1 = mock(RaceLogEvent.class);
        RaceLogEvent event2 = mock(RaceLogEvent.class);
        RaceLogEvent event3 = mock(RaceLogEvent.class);
        RaceLogEventAuthor minorAuthor = mock(RaceLogEventAuthor.class);
        RaceLogEventAuthor majorAuthor = mock(RaceLogEventAuthor.class);
        when(minorAuthor.compareTo(majorAuthor)).thenReturn(-1);
        when(majorAuthor.compareTo(minorAuthor)).thenReturn(1);
        
        when(event1.getAuthor()).thenReturn(majorAuthor);
        when(event1.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(event2.getAuthor()).thenReturn(minorAuthor);
        when(event2.getCreatedAt()).thenReturn(new MillisecondsTimePoint(2));
        when(event3.getAuthor()).thenReturn(minorAuthor);
        when(event3.getCreatedAt()).thenReturn(new MillisecondsTimePoint(3));
        
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);
        
        raceLog.lockForRead();
        assertEquals(event1, Util.get(raceLog.getRawFixesDescending(), 0));
        assertEquals(event3, Util.get(raceLog.getRawFixesDescending(), 1));
        assertEquals(event2, Util.get(raceLog.getRawFixesDescending(), 2));
        raceLog.unlockAfterRead();
    }
    
    @Test
    public void testFixesDescending() {
        RaceLogEvent event1 = mock(RaceLogEvent.class);
        RaceLogEvent event2 = mock(RaceLogEvent.class);
        RaceLogEvent event3 = mock(RaceLogEvent.class);
        RaceLogEvent event4 = mock(RaceLogEvent.class);
        RaceLogEventAuthor minorAuthor = mock(RaceLogEventAuthor.class);
        RaceLogEventAuthor majorAuthor = mock(RaceLogEventAuthor.class);
        when(minorAuthor.compareTo(majorAuthor)).thenReturn(-1);
        when(majorAuthor.compareTo(minorAuthor)).thenReturn(1);
        
        when(event1.getPassId()).thenReturn(1);
        when(event1.getAuthor()).thenReturn(majorAuthor);
        when(event1.getCreatedAt()).thenReturn(new MillisecondsTimePoint(1));
        when(event2.getPassId()).thenReturn(1);
        when(event2.getAuthor()).thenReturn(minorAuthor);
        when(event2.getCreatedAt()).thenReturn(new MillisecondsTimePoint(2));
        when(event3.getPassId()).thenReturn(1);
        when(event3.getAuthor()).thenReturn(minorAuthor);
        when(event3.getCreatedAt()).thenReturn(new MillisecondsTimePoint(3));
        when(event4.getPassId()).thenReturn(0);
        when(event4.getAuthor()).thenReturn(majorAuthor);
        when(event4.getCreatedAt()).thenReturn(new MillisecondsTimePoint(4));
        
        raceLog.add(event1);
        raceLog.add(event2);
        raceLog.add(event3);
        raceLog.add(event4);
        
        raceLog.lockForRead();
        assertEquals(3, Util.size(raceLog.getFixesDescending()));
        assertEquals(event1, Util.get(raceLog.getFixesDescending(), 0));
        assertEquals(event3, Util.get(raceLog.getFixesDescending(), 1));
        assertEquals(event2, Util.get(raceLog.getFixesDescending(), 2));
        raceLog.unlockAfterRead();
    }
    private RaceLogEventAuthor author = new RaceLogEventAuthorImpl("Test Author", 1);
    
    @Test
    public void testAddingEventsFromMultipleClients() {
        RaceLog raceLog = new RaceLogImpl("RaceLogTest", "test-identifier");
        UUID client1Id = UUID.randomUUID();
        UUID client2Id = UUID.randomUUID();
        final MillisecondsTimePoint now = MillisecondsTimePoint.now();
        RaceLogStartTimeEvent startTimeEvent1 = RaceLogEventFactory.INSTANCE.createStartTimeEvent(now, author, 1, now.plus(1));
        Iterable<RaceLogEvent> empty = raceLog.add(startTimeEvent1, client1Id);
        assertTrue(Util.isEmpty(empty));
        RaceLogStartTimeEvent startTimeEvent2 = RaceLogEventFactory.INSTANCE.createStartTimeEvent(now.plus(2), author, 1, now.plus(3));
        Iterable<RaceLogEvent> nonEmpty = raceLog.add(startTimeEvent2, client2Id);
        assertEquals(1, Util.size(nonEmpty));
        assertSame(startTimeEvent1, nonEmpty.iterator().next());
        RaceLogStartTimeEvent startTimeEvent3 = RaceLogEventFactory.INSTANCE.createStartTimeEvent(now.plus(4), author, 1, now.plus(5));
        Iterable<RaceLogEvent> nonEmpty2 = raceLog.add(startTimeEvent3, client1Id);
        assertEquals(1, Util.size(nonEmpty2));
        assertSame(startTimeEvent2, nonEmpty2.iterator().next());
        
    }
    
    @Test
    public void testGetFirstFixAfter() {
        TimePoint timePoint = new MillisecondsTimePoint(1);
        
        RaceLogEvent eventOne = mock(RaceLogEvent.class);
        when(eventOne.getAuthor()).thenReturn(mock(RaceLogEventAuthor.class));
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(100));
        when(eventOne.getTimePoint()).thenReturn(timePoint);
        raceLog.add(eventOne);
        
        assertNull(raceLog.getFirstFixAfter(timePoint));
        assertEquals(eventOne, raceLog.getFirstFixAfter(timePoint.minus(1)));
    }
    
    private class VisitFlagEventAnswer implements Answer<Object> {
        @Override
        public Object answer(InvocationOnMock invocation) throws Throwable {
            RaceLogEventVisitor visitor = (RaceLogEventVisitor) invocation.getArguments()[0];
            RaceLogFlagEvent flagEvent = (RaceLogFlagEvent) invocation.getMock();
            visitor.visit(flagEvent);
            return null;
        }
    }

}
