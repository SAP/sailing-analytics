package com.sap.sailing.domain.common.test;

import org.junit.Test;

import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;

import static org.junit.Assert.*;

import static org.hamcrest.Matchers.*;

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

}
