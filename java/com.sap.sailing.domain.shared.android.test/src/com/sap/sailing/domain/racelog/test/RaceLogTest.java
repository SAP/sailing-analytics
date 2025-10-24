package com.sap.sailing.domain.racelog.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogEventVisitor;
import com.sap.sailing.domain.abstractlog.race.RaceLogFlagEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogStartTimeEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogStartTimeEventImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class RaceLogTest {
    
    private RaceLog raceLog;
    
    private static class MockRaceLogEventForSorting extends RaceLogEventImpl {
        public MockRaceLogEventForSorting(long createdAtMillis) {
            super(new MillisecondsTimePoint(createdAtMillis),
                    MillisecondsTimePoint.now(),
                    /* author */ new LogEventAuthorImpl("Dummy Author", /* priority */ 0), /* ID */ UUID.randomUUID(), /* pInvolvedBoats */ null, /* pass ID */ 1);
        }

        private static final long serialVersionUID = 4928452859543831451L;

        @Override
        public void accept(RaceLogEventVisitor visitor) {
        }
        
    }
    
    @BeforeEach
    public void setUp() {
        raceLog = new RaceLogImpl("testlock", "test-identifier");
    }
    
    @Test
    public void testThrowsOnNoLock() {
        try {
            raceLog.getRawFixes();
            fail("Should throw IllegalStateException when lock is not acquired");
        } catch (IllegalStateException e) {
            // expected
        }
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

    private RaceLog merge(long[] intoTimePoints, long[] mergeTimePoints) {
        RaceLog into = new RaceLogImpl(UUID.randomUUID());
        RaceLog merge = new RaceLogImpl(UUID.randomUUID());
        for (long l : intoTimePoints) {
            into.add(new MockRaceLogEventForSorting(l));
        }
        for (long l : mergeTimePoints) {
            merge.add(new MockRaceLogEventForSorting(l));
        }
        into.merge(merge);
        return into;
    }
    
    @Test
    public void testSimpleMergeOfEmptyLog() {
        RaceLog into = merge(new long[] { 123, 234 }, new long[] {});
        into.lockForRead();
        try {
            assertEquals(2, Util.size(into.getRawFixes()));
            assertBefore(into, 123, 234);
        } finally {
            into.unlockAfterRead();
        }
    }
    
    /**
     * Asserts that both, events with creation time stamp millis <code>eventOne</code> and <code>eventTwo</code> occur
     * in <code>log</code> and the one with <code>eventOne</code> occurs before the one with <code>eventTwo</code> in
     * the log's raw fixes iteration order.
     */
    private void assertBefore(RaceLog log, long eventOne, long eventTwo) {
        log.lockForRead();
        boolean foundEventOne = false;
        try {
            for (RaceLogEvent e : log.getRawFixes()) {
                if (e.getCreatedAt().asMillis() == eventOne) {
                    foundEventOne = true;
                } else if (e.getCreatedAt().asMillis() == eventTwo) {
                    if (foundEventOne) {
                        break; // all is good
                    } else {
                        fail("Found "+eventTwo+" before "+eventOne+" but expected to find them in the opposite order");
                    }
                }
            }
        } finally {
            log.unlockAfterRead();
        }
    }

    @Test
    public void testSimpleMergeIntoEmptyLog() {
        RaceLog into = merge(new long[0], new long[] { 123, 234 });
        into.lockForRead();
        try {
            assertEquals(2, Util.size(into.getRawFixes()));
            assertBefore(into, 123, 234);
        } finally {
            into.unlockAfterRead();
        }
    }
    
    @Test
    public void testSimpleMergeOfLogWithOneElementToTheMiddle() {
        RaceLog into = merge(new long[] { 123, 234 }, new long[] { 200 });
        into.lockForRead();
        try {
            assertEquals(3, Util.size(into.getRawFixes()));
            assertBefore(into, 123, 200);
            assertBefore(into, 200, 234);
        } finally {
            into.unlockAfterRead();
        }
    }
    
    @Test
    public void testSimpleMergeOfLogWithOneElementToTheBeginning() {
        RaceLog into = merge(new long[] { 123, 234 }, new long[] { 100 });
        into.lockForRead();
        try {
            assertEquals(3, Util.size(into.getRawFixes()));
            assertBefore(into, 100, 123);
            assertBefore(into, 123, 234);
        } finally {
            into.unlockAfterRead();
        }
    }
    
    @Test
    public void testSimpleMergeOfLogWithOneElementToTheEnd() {
        RaceLog into = merge(new long[] { 123, 234 }, new long[] { 345 });
        into.lockForRead();
        try {
            assertEquals(3, Util.size(into.getRawFixes()));
            assertBefore(into, 123, 234);
            assertBefore(into, 234, 345);
        } finally {
            into.unlockAfterRead();
        }
    }
    
    @Test
    public void testSimpleMergeOfLogWithThreeElementsAroundTwoOthers() {
        RaceLog into = merge(new long[] { 123, 234 }, new long[] { 100, 200, 345 });
        into.lockForRead();
        try {
            assertEquals(5, Util.size(into.getRawFixes()));
            assertBefore(into, 100, 123);
            assertBefore(into, 123, 200);
            assertBefore(into, 200, 234);
            assertBefore(into, 234, 345);
        } finally {
            into.unlockAfterRead();
        }
    }
    
    @Test
    public void testSimpleMergeOfLogWithSequencesOfElementsAroundTwoOthers() {
        RaceLog into = merge(new long[] { 123, 234 }, new long[] { 100, 101, 200, 201, 345, 346 });
        into.lockForRead();
        try {
            assertEquals(8, Util.size(into.getRawFixes()));
            assertBefore(into, 100, 101);
            assertBefore(into, 101, 123);
            assertBefore(into, 123, 200);
            assertBefore(into, 200, 201);
            assertBefore(into, 201, 234);
            assertBefore(into, 234, 345);
            assertBefore(into, 234, 346);
        } finally {
            into.unlockAfterRead();
        }
    }
    
    @Test
    public void testSimpleMergeOfLogWithTwoElementsAroundSequenceOfOthers() {
        RaceLog into = merge(new long[] { 100, 101, 200, 201, 345, 346 }, new long[] { 123, 234 });
        into.lockForRead();
        try {
            assertEquals(8, Util.size(into.getRawFixes()));
            assertBefore(into, 100, 101);
            assertBefore(into, 101, 123);
            assertBefore(into, 123, 200);
            assertBefore(into, 200, 201);
            assertBefore(into, 201, 234);
            assertBefore(into, 234, 345);
            assertBefore(into, 234, 346);
        } finally {
            into.unlockAfterRead();
        }
    }
    
    @Test
    public void testAddEventDifferentPassButSameTimePoint() {
        RaceLogEvent eventOne = mock(RaceLogEvent.class);
        RaceLogEvent eventTwo = mock(RaceLogEvent.class);
        
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(mock(AbstractLogEventAuthor.class));
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getId()).thenReturn("a");
        when(eventTwo.getPassId()).thenReturn(1);
        when(eventTwo.getAuthor()).thenReturn(mock(AbstractLogEventAuthor.class));
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(0));
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
        AbstractLogEventAuthor author = mock(AbstractLogEventAuthor.class);
        
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(author);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getId()).thenReturn("a");
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(author);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(0));
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
        AbstractLogEventAuthor author = mock(AbstractLogEventAuthor.class);
        
        when(eventOne.getPassId()).thenReturn(0);
        when(eventOne.getAuthor()).thenReturn(author);
        when(eventOne.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(0));
        when(eventOne.getId()).thenReturn("a");
        when(eventTwo.getPassId()).thenReturn(0);
        when(eventTwo.getAuthor()).thenReturn(author);
        when(eventTwo.getCreatedAt()).thenReturn(new MillisecondsTimePoint(0));
        when(eventTwo.getLogicalTimePoint()).thenReturn(new MillisecondsTimePoint(0));
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
        AbstractLogEventAuthor minorAuthor = mock(AbstractLogEventAuthor.class);
        AbstractLogEventAuthor majorAuthor = mock(AbstractLogEventAuthor.class);
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
        AbstractLogEventAuthor minorAuthor = mock(AbstractLogEventAuthor.class);
        AbstractLogEventAuthor majorAuthor = mock(AbstractLogEventAuthor.class);
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
    private AbstractLogEventAuthor author = new LogEventAuthorImpl("Test Author", 1);
    
    @Test
    public void testAddingEventsFromMultipleClients() {
        RaceLog raceLog = new RaceLogImpl("RaceLogTest", "test-identifier");
        UUID client1Id = UUID.randomUUID();
        UUID client2Id = UUID.randomUUID();
        final TimePoint now = MillisecondsTimePoint.now();
        RaceLogStartTimeEvent startTimeEvent1 = new RaceLogStartTimeEventImpl(now, author, 1, now.plus(1), /* courseAreaId */ null);
        Iterable<RaceLogEvent> empty = raceLog.add(startTimeEvent1, client1Id);
        assertTrue(Util.isEmpty(empty));
        RaceLogStartTimeEvent startTimeEvent2 = new RaceLogStartTimeEventImpl(now.plus(2), author, 1, now.plus(3), /* courseAreaId */ null);
        Iterable<RaceLogEvent> nonEmpty = raceLog.add(startTimeEvent2, client2Id);
        assertEquals(1, Util.size(nonEmpty));
        assertSame(startTimeEvent1, nonEmpty.iterator().next());
        RaceLogStartTimeEvent startTimeEvent3 = new RaceLogStartTimeEventImpl(now.plus(4), author, 1, now.plus(5), /* courseAreaId */ null);
        Iterable<RaceLogEvent> nonEmpty2 = raceLog.add(startTimeEvent3, client1Id);
        assertEquals(1, Util.size(nonEmpty2));
        assertSame(startTimeEvent2, nonEmpty2.iterator().next());
        
    }
    
    @Test()
    public void testGetFirstFixAfterNew() {
        
        TimePoint createdAtEventOne = new MillisecondsTimePoint(100);
        RaceLogEvent eventOne = mock(RaceLogEvent.class);
        when(eventOne.getAuthor()).thenReturn(mock(AbstractLogEventAuthor.class));
        when(eventOne.getCreatedAt()).thenReturn(createdAtEventOne);
        when(eventOne.getTimePoint()).thenReturn(createdAtEventOne);
        raceLog.add(eventOne);
        
        TimePoint createdAtEventTwo = new MillisecondsTimePoint(99);
        RaceLogEvent eventTwo = mock(RaceLogEvent.class);
        when(eventTwo.getAuthor()).thenReturn(mock(AbstractLogEventAuthor.class));
        when(eventTwo.getCreatedAt()).thenReturn(createdAtEventTwo);
        when(eventTwo.getTimePoint()).thenReturn(createdAtEventTwo);
        raceLog.add(eventTwo);
        
        assertNull(raceLog.getFirstFixAfter(new MillisecondsTimePoint(100)));
        assertEquals(eventOne, raceLog.getFirstFixAfter(new MillisecondsTimePoint(99)));
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
