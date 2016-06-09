package com.sap.sailing.domain.common.test;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Calendar;
import java.util.GregorianCalendar;

import org.junit.Test;

import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TimePointTest {
    @Test
    public void compare() {
        TimePoint one = new MillisecondsTimePoint(Long.MIN_VALUE);
        TimePoint two = new MillisecondsTimePoint(Long.MAX_VALUE);
        assertTrue(one.before(two));
        assertTrue(two.after(one));
    }
    
    @Test
    public void compareEquals() {
        TimePoint one = new MillisecondsTimePoint(Long.MIN_VALUE);
        TimePoint two = new MillisecondsTimePoint(Long.MIN_VALUE);
        assertThat(one.compareTo(two), is(0));
        assertFalse(one.before(two));
        assertFalse(two.after(one));
    }
    
    @Test
    public void testEqualForSame() {
        TimePoint t = new MillisecondsTimePoint(Long.MIN_VALUE);
        assertThat(t, is(t));
    }

    @Test
    public void testEqualForEuqal() {
        TimePoint t1 = new MillisecondsTimePoint(Long.MIN_VALUE);
        TimePoint t2 = new MillisecondsTimePoint(Long.MIN_VALUE);
        assertThat(t1, is(t2));
        assertThat(t2, is(t1));
    }

    @Test
    public void testEqualForNull() {
        TimePoint t = new MillisecondsTimePoint(Long.MIN_VALUE);
        assertFalse(t.equals(null));
    }

    @Test
    public void testEqualForString() {
        TimePoint t = new MillisecondsTimePoint(Long.MIN_VALUE);
        assertFalse(t.equals("s"));
    }

    @Test
    public void nearestFullMinute() {
        Calendar cal = new GregorianCalendar();
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(2016, 5, 15, 10, 12, 33); // 10:12:33 on June 15, 2016 in the local time zone
        final TimePoint start = new MillisecondsTimePoint(cal.getTime());
        cal.set(2016, 5, 15, 10, 8, 35); // 10:08:35 on June 15, 2016 in the local time zone
        final TimePoint t1 = new MillisecondsTimePoint(cal.getTime());
        cal.set(2016, 5, 15, 10, 8, 31); // 10:08:31 on June 15, 2016 in the local time zone
        final TimePoint t2 = new MillisecondsTimePoint(cal.getTime());
        cal.set(2016, 5, 15, 10, 15, 31); // 10:15:31 on June 15, 2016 in the local time zone
        final TimePoint t3 = new MillisecondsTimePoint(cal.getTime());
        cal.set(2016, 5, 15, 10, 15, 37); // 10:15:31 on June 15, 2016 in the local time zone
        final TimePoint t4 = new MillisecondsTimePoint(cal.getTime());
        assertEquals(new MillisecondsTimePoint(new GregorianCalendar(2016, 5, 15, 10, 12, 35).getTime()), start.getNearestModuloOneMinute(t1));
        assertEquals(new MillisecondsTimePoint(new GregorianCalendar(2016, 5, 15, 10, 12, 31).getTime()), start.getNearestModuloOneMinute(t2));
        assertEquals(new MillisecondsTimePoint(new GregorianCalendar(2016, 5, 15, 10, 12, 31).getTime()), start.getNearestModuloOneMinute(t3));
        assertEquals(new MillisecondsTimePoint(new GregorianCalendar(2016, 5, 15, 10, 12, 37).getTime()), start.getNearestModuloOneMinute(t4));
        // and now at least on milliseconds example
        cal.set(Calendar.MILLISECOND, 123);
        final TimePoint t5 = new MillisecondsTimePoint(cal.getTime());
        final Calendar t5Compare = new GregorianCalendar(2016, 5, 15, 10, 12, 37);
        t5Compare.set(Calendar.MILLISECOND, 123);
        assertEquals(new MillisecondsTimePoint(t5Compare.getTime()), start.getNearestModuloOneMinute(t5));
    }
}
