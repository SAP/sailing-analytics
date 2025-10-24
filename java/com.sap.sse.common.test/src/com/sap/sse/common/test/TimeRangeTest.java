package com.sap.sse.common.test;

import static com.sap.sse.common.test.TimeTestHelpers.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.TimeRangeImpl;

public class TimeRangeTest {
    private void liesWithin(long fromOuter, long toOuter, long fromInner, long toInner) {
        TimeRange outer = create(fromOuter, toOuter);
        TimeRange inner = create(fromInner, toInner);
        
        Assertions.assertTrue(inner.liesWithin(outer));
        Assertions.assertTrue(outer.includes(inner));
        Assertions.assertFalse(inner.includes(outer));
        Assertions.assertFalse(outer.liesWithin(inner));
    }
    
    @Test
    public void liesWithin() {
        liesWithin(0, 100, 10, 90);
        liesWithin(0, 100, 0, 90);
        liesWithin(0, 100, 10, 100);
    }
    
    @Test
    public void includesTimePoint() {
        TimeRange range = create(0, 100);
        Assertions.assertTrue(range.includes(create(0)));
        Assertions.assertTrue(range.includes(create(1)));
        Assertions.assertTrue(range.includes(create(99)));
        Assertions.assertFalse(range.includes(create(100))); // exclusive end
        Assertions.assertFalse(range.includes(create(101)));
    }
    
    @Test
    public void empty() {
        assertTrue(create(100, 100).isEmpty());
        assertFalse(create(100, 101).isEmpty());
        assertFalse(create(null, 100l).isEmpty());
        assertFalse(create(100l, null).isEmpty());
    }
    @Test
    public void intersects() {
        TimeRange one = create(0, 50);
        TimeRange two = create(20, 70);
        
        Assertions.assertFalse(one.liesWithin(two));
        Assertions.assertFalse(two.liesWithin(one));
        Assertions.assertTrue(one.intersects(two));
        Assertions.assertTrue(two.intersects(one));
    }
    
    @Test
    public void openRanges() {    	
    	TimeRange one = new TimeRangeImpl(new MillisecondsTimePoint(0), null);
    	TimeRange two = create(5, 10);
    	TimeRange three = new TimeRangeImpl(null, new MillisecondsTimePoint(15));
    	
    	assertTrue(two.liesWithin(one));
    	assertTrue(two.liesWithin(three));
    	assertTrue(one.intersects(three));
    	assertTrue(one.endsAfter(three));
    	assertFalse(one.startsBefore(three));
    }
    
    @Test
    public void extendWithTimePoint() {
        TimeRange one = create(5, 10);
        assertFalse(one.includes(create(10))); // end is exclusive
        TimePoint two = create(20);
        TimeRange extension = one.extend(two);
        assertTrue(extension.includes(one));
        assertTrue(extension.includes(two));
        assertTrue(extension.includes(create(5)));
        assertTrue(extension.includes(create(10)));
    }
    
    @Test
    public void extendWithTimeRange() {
        TimeRange one = create(5, 10);
        assertFalse(one.includes(create(10))); // end is exclusive
        TimeRange two = create(20, 30);
        assertFalse(two.includes(create(30))); // end is exclusive
        TimeRange extension = one.extend(two);
        assertTrue(extension.includes(one));
        assertTrue(extension.includes(two));
        assertTrue(extension.includes(create(5)));
        assertTrue(extension.includes(create(10)));
        assertFalse(extension.includes(create(30)));
    }
    
    @Test
    public void union() {
    	TimeRange one = create(5, 10);
    	TimeRange two = create(7, 12);
    	
    	TimeRange union = one.union(two);
    	assertEquals(5, union.from().asMillis());
    	assertEquals(12, union.to().asMillis());
    	
    	two = create(11, 16);
    	assertNull(one.union(two));
    	
    	assertEquals(create(5, 20), one.union(create(10, 20)));
    	assertEquals(create(5, 10), create(5, 10).union(create(5, 10)));
    	assertEquals(create(5, 10), create(5, 10).union(create(6, 7)));
    	assertEquals(create(5, 10), create(5, 10).union(create(6, 6)));
    	
    	two = create(7, TimePoint.EndOfTime.asMillis());
    	union = two.union(one);
    	TimeRange union2 = one.union(two);
    	assertEquals(5, union.from().asMillis());
    	assertEquals(5, union2.from().asMillis());
    	assertEquals(TimePoint.EndOfTime.asMillis(), union.to().asMillis());
    	assertEquals(TimePoint.EndOfTime.asMillis(), union2.to().asMillis());
    	
    	one = create(TimePoint.BeginningOfTime.asMillis(), 10);
    	union = two.union(one);
    	assertTrue(union.hasOpenBeginning());
    	assertTrue(union.hasOpenEnd());
    }
    
    @Test
    public void intersection() {
    	TimeRange one = create(5, 10);
    	TimeRange two = create(7, 12);
    	
    	TimeRange intersection = one.intersection(two);
    	assertEquals(7, intersection.from().asMillis());
    	assertEquals(10, intersection.to().asMillis());
    	
    	two = create(11, 16);
    	assertNull(one.intersection(two));
    	
    	two = create(7, Long.MAX_VALUE);
    	intersection = two.intersection(one);
    	assertEquals(7, intersection.from().asMillis());
    	assertEquals(10, intersection.to().asMillis());
    	
    	one = create(0, Long.MAX_VALUE);
    	two = create(Long.MIN_VALUE, 10);
    	intersection = two.intersection(one);
    	assertEquals(0, intersection.from().asMillis());
    	assertEquals(10, intersection.to().asMillis());
    }
    
    @Test
    public void startAfterTest() {
        assertTrue(create(100, 200).startsAtOrAfter(create(100)));
        assertTrue(create(100, 200).startsAtOrAfter(create(99)));
        assertFalse(create(100, 200).startsAtOrAfter(create(101)));
        assertTrue(create(100, 200).startsAfter(create(50, 100)));
        assertFalse(create(100, 200).startsAfter(create(50, 150)));
        assertFalse(create(100, 200).startsAfter(create(50l, null))); // can't start after a range that extends until the end of time
    }
    
    @Test
    public void touches() {
        assertTrue(create(100, 200).touches(create(100, 200))); // touches is reflexive
        assertTrue(create(100, 200).touches(create(50, 150)));
        assertTrue(create(100, 200).touches(create(50, 100)));
        assertTrue(create(100, 200).touches(create(200, 350)));
        assertFalse(create(100, 200).touches(create(201, 250)));
        assertFalse(create(100, 200).touches(create(50, 99)));
    }
    
    @Test
    public void timeDifferenceTest() {
        TimeRange one = create(5, 10);
        assertEquals(2, one.timeDifference(new MillisecondsTimePoint(3)).asMillis());
        assertEquals(0, one.timeDifference(new MillisecondsTimePoint(7)).asMillis());
        assertEquals(2, one.timeDifference(new MillisecondsTimePoint(12)).asMillis());
    }
    
    @Test
    public void testEndOfTime() {
        assertTrue(TimePoint.EndOfTime.after(TimePoint.BeginningOfTime));
        assertTrue(TimePoint.EndOfTime.after(MillisecondsTimePoint.now()));
    }
    
    @Test
    public void testSubtractionWithMinuendCompletelyWithin() {
        testSubtraction(5, 10, 7, 8, /* expected */ 5, 7, 8, 10);
    }

    @Test
    public void testSubtractionWithMinuendStartingAtFrom() {
        testSubtraction(5, 10, 5, 8, /* expected */ 8, 10);
    }

    @Test
    public void testSubtractionWithMinuendEndingAtTo() {
        testSubtraction(5, 10, 7, 10, /* expected */ 5, 7);
    }

    @Test
    public void testSubtractionWithMinuendContainingAll() {
        testSubtraction(5, 10, 4, 11 /* expected empty */);
    }

    @Test
    public void testSubtractionWithMinuendOverlappingAtFrom() {
        testSubtraction(5, 10, 4, 7, /* expected */ 7, 10);
    }

    @Test
    public void testSubtractionWithMinuendOverlappingAtto() {
        testSubtraction(5, 10, 7, 12, /* expected */ 5, 7);
    }
    
    @Test
    public void testExclusiveEnd() {
        assertFalse(create(100, 200).includes(create(200)));
        assertTrue(create(100, 200).includes(create(199)));
    }

    @Test
    public void testEndsBeforeWithExclusiveEnd() {
        assertTrue(create(100, 200).endsBefore(create(200)));
        assertTrue(create(100, 200).endsBefore(create(201)));
        assertFalse(create(100, 200).endsBefore(create(199)));
    }
    
    @Test
    public void testEndsAfterWithExclusiveEnd() {
        assertFalse(create(100, 200).endsAfter(create(100, 200)));
        assertFalse(create(100, 200).endsAfter(create(100, 201)));
        assertTrue(create(100, 200).endsAfter(create(100, 199)));
    }
    
    private void testSubtraction(long allFrom, long allTo, long minuendFrom, long minuendTo, long... expected) {
        TimeRange all = create(allFrom, allTo);
        TimeRange minuend = create(minuendFrom, minuendTo);
        MultiTimeRange diff = all.subtract(minuend);
        assertEquals(expected.length/2, Util.size(diff), "Expected to obtain "+expected.length/2+" time ranges but got "+Util.size(diff));
        int i=0;
        for (final TimeRange timeRangeFromDiff : diff) {
            assertEquals(create(expected[2*i]),
                    timeRangeFromDiff.from(), "expected "+i+"-th time range's \"from\" time point with millis "+expected[2*i]+" but got "+timeRangeFromDiff.from().asMillis());
            assertEquals(create(expected[2*i+1]),
                    timeRangeFromDiff.to(), "expected "+i+"-th time range's \"to\" time point with millis "+expected[2*i+1]+" but got "+timeRangeFromDiff.to().asMillis());
            i++;
        }
    }
    
    @Test
    public void testBeginningOfTimeBeforeEndOfTime() {
        assertTrue(TimePoint.BeginningOfTime.before(TimePoint.EndOfTime));
    }
    
    @Test
    public void testTimeRangeWithNullStartEndEndIsValid() {
        assertNotNull(TimeRange.create(null, null));
    }
}
