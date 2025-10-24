package com.sap.sse.common.test;

import static com.sap.sse.common.test.TimeTestHelpers.create;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.MultiTimeRange;
import com.sap.sse.common.TimeRange;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.MultiTimeRangeImpl;

public class MultiTimeRangeTest {
    private MultiTimeRange createMulti(long... startAndEndSequence) {
        assert startAndEndSequence != null;
        assert startAndEndSequence.length % 2 == 0;
        List<TimeRange> list = new ArrayList<>();
        for (int i=0; i<startAndEndSequence.length; i+=2) {
            list.add(create(startAndEndSequence[i], startAndEndSequence[i+1]));
        }
        return new MultiTimeRangeImpl(list);
    }
    
    @Test
    public void testEmptyMultiTimeRange() {
        assertTrue(createMulti().isEmpty());
        assertTrue(createMulti(100, 200).intersection(create(200, 300)).isEmpty());
        assertTrue(createMulti(100, 100).isEmpty());
    }
    
    @Test
    public void equality() {
        assertEquals(createMulti(300, 400, 400, 500, 200, 350), createMulti(400, 500, 300, 400, 200, 350));
        assertEquals(createMulti(300, 400, 400, 500, 200, 350), createMulti(200, 500));
        assertFalse(createMulti(300, 400, 400, 500, 200, 350).equals(createMulti(300, 600)));
        assertFalse(createMulti(300, 400).equals(createMulti(70, 500)));
        assertEquals(createMulti(500, 600, 300, 400), createMulti(300, 400, 500, 600, 300, 400));
        assertEquals(createMulti(500, 600, 300, 400), createMulti(300, 400, 500, 600));
    }
    @Test
    public void testAdd() {
        MultiTimeRange mtr = createMulti(100, 200);
        MultiTimeRange multi = mtr.union(createMulti(300, 400));
        assertEquals(2, Util.size(multi));
        assertEquals(mtr.iterator().next(), multi.iterator().next());
        assertTrue(multi.includes(create(150)));
        assertTrue(multi.includes(create(350)));
        assertFalse(multi.includes(create(250)));
        assertFalse(multi.includes(create(200))); // exclusive first time range
        assertFalse(multi.includes(create(400))); // exclusive last time range
    }
    
    @Test
    public void testMinimality() {
        assertEquals(1, Util.size(createMulti(300, 400, 400, 500, 200, 350)));
        assertEquals(create(200), Util.first(createMulti(300, 400, 400, 500, 200, 350)).from());
        assertEquals(create(500), Util.last(createMulti(300, 400, 400, 500, 200, 350)).to());

        assertEquals(2, Util.size(createMulti(500, 600, 300, 400)));
        assertEquals(create(300), Util.first(createMulti(500, 600, 300, 400)).from());
        assertEquals(create(600), Util.last(createMulti(500, 600, 300, 400)).to());
        assertTrue(createMulti(500, 600, 300, 400).includes(create(350)));
        assertTrue(createMulti(500, 600, 300, 400).includes(create(550)));
        assertFalse(createMulti(500, 600, 300, 400).includes(create(450)));
        assertFalse(createMulti(500, 600, 300, 400).includes(create(400)));
        assertFalse(createMulti(500, 600, 300, 400).includes(create(600)));
    }
    
    @Test
    public void intersection() {
        assertEquals(createMulti(100, 200, 300, 400), createMulti(100, 200, 300, 400).intersection(createMulti(100, 400)));
        assertEquals(createMulti(100, 400), createMulti(100, 350, 300, 400).intersection(createMulti(100, 400)));
        assertTrue(createMulti(100, 200).intersection(createMulti()).isEmpty());
        assertTrue(createMulti(100, 200).intersection(createMulti(300, 400)).isEmpty());
        assertTrue(createMulti(100, 200).intersection(createMulti(200, 400)).isEmpty());
        assertEquals(createMulti(100, 101), createMulti(100, 200).intersection(createMulti(50, 101)));
        assertEquals(createMulti(120, 200, 300, 380), createMulti(100, 380).intersection(createMulti(120, 200, 300, 400)));
        assertEquals(createMulti(100, 150, 160, 180, 190, 200, 300, 350, 370, 400), createMulti(100, 200, 300, 400).intersection(createMulti(50, 150, 160, 180, 190, 220, 280, 350, 370, 410)));
    }
    
    @Test
    public void intersects() {
        assertTrue(createMulti(100, 200, 300, 400).intersects(createMulti(50, 150, 160, 180, 190, 220, 280, 350, 370, 410)));
        assertFalse(createMulti(100, 200).intersects(createMulti(220, 300)));
        assertFalse(createMulti(100, 200).intersects(createMulti(200, 300)));
        assertTrue(createMulti(100, 200, 300, 400).intersects(createMulti(150, 350)));
        assertTrue(createMulti(100, 200, 300, 400).intersects(createMulti(350, 370)));
        assertFalse(createMulti(100, 200, 300, 400).intersects(createMulti(250, 270)));
    }
    
    @Test
    public void subtract() {
        assertEquals(createMulti(100, 120, 180, 200), createMulti(100, 200).subtract(createMulti(120, 180)));
        assertTrue(createMulti(100, 200).subtract(createMulti(100, 200)).isEmpty());
        assertEquals(createMulti(100, 120), createMulti(100, 200).subtract(createMulti(120, 200)));
        assertEquals(createMulti(100, 120, 300, 400), createMulti(100, 200, 300, 400).subtract(createMulti(120, 250)));
        assertEquals(createMulti(100, 120, 180, 200, 300, 400), createMulti(100, 200, 300, 400).subtract(createMulti(120, 180)));
        assertEquals(createMulti(100, 120, 300, 400), createMulti(100, 200, 300, 400).subtract(createMulti(120, 300)));
        assertEquals(createMulti(100, 120, 350, 400), createMulti(100, 200, 300, 400).subtract(createMulti(120, 350)));
        assertEquals(createMulti(100, 120, 350, 360, 370, 400), createMulti(100, 200, 300, 400).subtract(createMulti(120, 350, 360, 370)));
    }
    
    @Test
    public void testOrderingWithOf() {
        final MillisecondsTimePoint _10 = new MillisecondsTimePoint(10);
        final MillisecondsTimePoint _20 = new MillisecondsTimePoint(20);
        final MillisecondsTimePoint _100 = new MillisecondsTimePoint(100);
        final MillisecondsTimePoint _200 = new MillisecondsTimePoint(200);
        final MultiTimeRange mtr = MultiTimeRange.of(TimeRange.create(_100, _200), TimeRange.create(_10, _20));
        assertEquals(TimeRange.create(_10, _20), Util.get(mtr, 0));
        assertEquals(TimeRange.create(_100, _200), Util.get(mtr, 1));
    }
    
    @Test
    public void testOrderingWithUnion() {
        final MillisecondsTimePoint _10 = new MillisecondsTimePoint(10);
        final MillisecondsTimePoint _20 = new MillisecondsTimePoint(20);
        final MillisecondsTimePoint _100 = new MillisecondsTimePoint(100);
        final MillisecondsTimePoint _200 = new MillisecondsTimePoint(200);
        final MultiTimeRange mtr = MultiTimeRange.of(TimeRange.create(_100, _200)).union(TimeRange.create(_10, _20));
        assertEquals(TimeRange.create(_10, _20), Util.get(mtr, 0));
        assertEquals(TimeRange.create(_100, _200), Util.get(mtr, 1));
    }
    
    @Test
    public void testEmptyTimeRangeRemoval() {
        final MillisecondsTimePoint _10 = new MillisecondsTimePoint(10);
        final MillisecondsTimePoint _100 = new MillisecondsTimePoint(100);
        final MillisecondsTimePoint _200 = new MillisecondsTimePoint(200);
        final MultiTimeRange mtr = MultiTimeRange.of(TimeRange.create(_100, _200)).union(TimeRange.create(_10, _10));
        assertEquals(1, Util.size(mtr));
        assertEquals(TimeRange.create(_100, _200), Util.get(mtr, 0));
    }
}
