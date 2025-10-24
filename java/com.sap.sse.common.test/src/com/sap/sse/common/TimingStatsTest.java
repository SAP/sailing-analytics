package com.sap.sse.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TimingStatsTest {
    private final Duration YOUNG = Duration.ONE_SECOND;
    private final Duration OLD = YOUNG.times(2);
    private final Duration SHORT = Duration.ONE_HOUR;
    private final Duration LONG = SHORT.times(2);

    @Test
    public void testOneAndTwoElements() {
        final TimePoint now = MillisecondsTimePoint.now();
        final TimingStats timingStats = new TimingStats(now.minus(Duration.ONE_HOUR), YOUNG, OLD);
        {
            timingStats.recordTiming(now, now.minus(YOUNG).minus(Duration.ONE_MILLISECOND), SHORT);
            final Map<Duration, Pair<Duration, Integer>> stats = timingStats.getAverageDurations(now);
            assertNull(stats.get(YOUNG).getA());
            assertEquals(1, stats.get(OLD).getB().intValue());
            assertEquals(SHORT, stats.get(OLD).getA());
        }
        // add another one later, such that the new one is expected to be YOUNG, and the formerly OLD one is expected to still be old
        {
            final TimePoint newNow = now.plus(YOUNG.divide(2));
            timingStats.recordTiming(newNow, newNow.minus(Duration.ONE_MILLISECOND), LONG);
            final Map<Duration, Pair<Duration, Integer>> newStats = timingStats.getAverageDurations(newNow);
            assertEquals(LONG, newStats.get(YOUNG).getA());
            assertEquals(SHORT.plus(LONG).divide(2).asMillis(), newStats.get(OLD).getA().asMillis());
        }
        // now advance the time by adding another one, causing the oldest one to drop out of the frame
        {
            final TimePoint newNow = now.plus(OLD).plus(Duration.ONE_MILLISECOND);
            timingStats.recordTiming(newNow, newNow.minus(Duration.ONE_MILLISECOND), LONG.times(2));
            // the first entry added is OLD+YOUNG+2ms old, dropping out
            // the second entry added is OLD-YOUNG/2+2ms old, ending up in OLD and not in YOUNG
            // the new entry added is 1ms old, ending up in YOUNG and OLD
            // So we expect YOUNG to represent one entry: 2*LONG
            // and OLD to represent two entries: 2*LONG + LONG with average 1.5*LONG
            final Map<Duration, Pair<Duration, Integer>> newStats = timingStats.getAverageDurations(newNow);
            assertEquals(LONG.times(2), newStats.get(YOUNG).getA());
            assertEquals(LONG.times(2).plus(LONG).divide(2).asMillis(), newStats.get(OLD).getA().asMillis());
        }
        // simply advance the time to force all elements to drop out
        {
            final Map<Duration, Pair<Duration, Integer>> newStats = timingStats.getAverageDurations(now.plus(OLD.times(4)));
            assertNull(newStats.get(YOUNG).getA());
            assertEquals(0, newStats.get(YOUNG).getB().intValue());
            assertNull(newStats.get(OLD).getA());
            assertEquals(0, newStats.get(OLD).getB().intValue());
        }
    }
    
    @Test
    public void testEntryAtBorder() {
        final TimePoint now = MillisecondsTimePoint.now();
        final TimingStats timingStats = new TimingStats(now.minus(Duration.ONE_HOUR), YOUNG, OLD);
        timingStats.recordTiming(now, now.minus(YOUNG), SHORT);
        final Map<Duration, Pair<Duration, Integer>> stats = timingStats.getAverageDurations(now);
        assertEquals(SHORT, stats.get(YOUNG).getA());
        assertEquals(SHORT, stats.get(OLD).getA());
    }
}
