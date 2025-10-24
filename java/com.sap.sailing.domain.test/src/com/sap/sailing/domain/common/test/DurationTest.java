package com.sap.sailing.domain.common.test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.DurationFormatter;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.common.impl.SecondsDurationImpl;

public class DurationTest {
    @Test
    public void testDurationPlus() {
        TimePoint t1 = new MillisecondsTimePoint(123);
        Duration d = new MillisecondsDurationImpl(234);
        TimePoint t2 = t1.plus(d);
        assertEquals(123+234, t2.asMillis());
    }
    
    @Test
    public void testDurationMinus() {
        TimePoint t1 = new MillisecondsTimePoint(234);
        Duration d = new MillisecondsDurationImpl(123);
        TimePoint t2 = t1.minus(d);
        assertEquals(234-123, t2.asMillis());
    }

    @Test
    public void testDurationUntil() {
        TimePoint t1 = new MillisecondsTimePoint(123);
        TimePoint t2 = new MillisecondsTimePoint(234);
        Duration d = t1.until(t2);
        assertEquals(234-123, d.asMillis());
    }

    @Test
    public void testNegativeDuration() {
        Duration d = new MillisecondsDurationImpl(100);
        Duration d2 = d.minus(200).plus(300);
        assertEquals(200, d2.asMillis());
    }

    @Test
    public void testDurationFractions() {
        DurationFormatter fmt = DurationFormatter.getInstance(/*shortMode*/false, /*nonNegativeDuration*/false);
        Duration oneSecond = Duration.ONE_SECOND;
        assertEquals(0, oneSecond.asDays(), 0.2);
        assertEquals(0, oneSecond.asHours(), 0.2);
        assertEquals(0, oneSecond.asMinutes(), 0.2);
        assertEquals(1, oneSecond.asSeconds(), 0);
        assertEquals(1000, oneSecond.asMillis());
        Duration oneHour = Duration.ONE_HOUR;
        assertEquals(0, oneHour.asDays(), 0);
        assertEquals(1, oneHour.asHours(), 0);
        assertEquals(60, oneHour.asMinutes(), 0);
        assertEquals(3600, oneHour.asSeconds(), 0);
        assertEquals(3600*1000, oneHour.asMillis());
        assertEquals("1 hour", fmt.format(oneHour));
        Duration oneDay = Duration.ONE_DAY;
        assertEquals(24, oneDay.asHours(), 0);
        assertEquals(1, oneDay.asDays(), 0);
        assertEquals(24*60, oneDay.asMinutes(), 0);
        assertEquals(3600*24, oneDay.asSeconds(), 0);
        assertEquals(3600*24*1000, oneDay.asMillis());
        assertEquals("1 day", fmt.format(oneDay));
        Duration someMinutes = Duration.ONE_MINUTE.times(5).plus(Duration.ONE_SECOND.times(40));
        assertEquals(0, someMinutes.asDays(), 0);
        assertEquals(0, someMinutes.asHours(), (5.+40./60.)/60.);
        assertEquals(5.6, someMinutes.asMinutes(), 0.1);
        assertEquals(340, someMinutes.asSeconds(), 0.1);
        assertEquals(340000, someMinutes.asMillis());
        assertEquals("5:40 min", fmt.format(someMinutes));
        fmt = DurationFormatter.getInstance(/*shortMode*/true, /*nonNegativeDuration*/false);
        assertEquals("5:40", fmt.format(someMinutes));
        Duration someSeconds = new MillisecondsDurationImpl(2400);
        assertEquals(0, someSeconds.asDays(), 0.1);
        assertEquals(0, someSeconds.asHours(), 0.1);
        assertEquals(0, someSeconds.asMinutes(), 0.1);
        assertEquals(2.4, someSeconds.asSeconds(), 0.1);
        assertEquals(2400, someSeconds.asMillis());
    }
    
    @Test
    public void testOperationsMilliseconds() { 
        Duration someSeconds = new MillisecondsDurationImpl(2400).minus(1000);
        assertEquals(0, someSeconds.asDays(), 0.1);
        assertEquals(0, someSeconds.asHours(), 0.1);
        assertEquals(0, someSeconds.asMinutes(), 0.1);
        assertEquals(1.4, someSeconds.asSeconds(), 0.1);
        assertEquals(1400, someSeconds.asMillis());
        Duration oneDay = Duration.ONE_DAY.plus(Duration.ONE_HOUR);
        assertEquals(25, oneDay.asHours(), 0.001);
        assertEquals(1, oneDay.asDays(), 0.01);
        assertEquals(25*60, oneDay.asMinutes(), 0.1);
        assertEquals(3600*25, oneDay.asSeconds(), 0.1);
        assertEquals(3600*25*1000, oneDay.asMillis());
    }
    
    @Test
    public void testOperationsSeconds() { 
        Duration someSeconds = new SecondsDurationImpl(2.4).minus(1000);
        assertEquals(0, someSeconds.asDays(), 0.1);
        assertEquals(0, someSeconds.asHours(), 0.1);
        assertEquals(0, someSeconds.asMinutes(), 0.1);
        assertEquals(1.4, someSeconds.asSeconds(), 0.1);
        assertEquals(1400, someSeconds.asMillis(), 0.1);
        Duration oneDay = Duration.ONE_DAY.plus(Duration.ONE_HOUR);
        assertEquals(25, oneDay.asHours(), 0.001);
        assertEquals(1, oneDay.asDays(), 0.01);
        assertEquals(25*60, oneDay.asMinutes(), 0.1);
        assertEquals(3600*25, oneDay.asSeconds(), 0.1);
        assertEquals(3600*25*1000, oneDay.asMillis());
    }

    @Test
    public void compare() {
        assertTrue(MillisecondsDurationImpl.ONE_MINUTE.compareTo(MillisecondsDurationImpl.ONE_HOUR) < 0);
        assertTrue(MillisecondsDurationImpl.ONE_HOUR.compareTo(MillisecondsDurationImpl.ONE_MINUTE) > 0);
    }
    
    @Test
    public void compareEquals() {
        assertThat(MillisecondsDurationImpl.ONE_HOUR.compareTo(MillisecondsDurationImpl.ONE_HOUR), is(0));
    }
    
    @Test
    public void testEqualForSame() {
        assertThat(MillisecondsDurationImpl.ONE_HOUR, is(MillisecondsDurationImpl.ONE_HOUR));
    }

    @Test
    public void testEqualForEuqal() {
        Duration t1 = MillisecondsDurationImpl.ONE_HOUR;
        Duration t2 = new MillisecondsDurationImpl(t1.asMillis());
        assertThat(t1, is(t2));
        assertThat(t2, is(t1));
    }

    @Test
    public void testEqualForNull() {
        assertFalse(MillisecondsDurationImpl.ONE_HOUR.equals(null));
    }

    @SuppressWarnings("unlikely-arg-type") // this is what we're testing here
    @Test
    public void testEqualForString() {
        assertFalse(MillisecondsDurationImpl.ONE_HOUR.equals("s"));
    }

}
