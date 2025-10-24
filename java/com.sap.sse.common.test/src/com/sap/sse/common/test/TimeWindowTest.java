package com.sap.sse.common.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.TimeWindow;
import com.sap.sse.common.Timed;
import com.sap.sse.common.Util;
import com.sap.sse.common.Util.Pair;

public class TimeWindowTest {
    private static class TimedString implements Timed {
        private static final long serialVersionUID = -3970312764704801199L;
        final TimePoint timePoint;
        final String string;
        public TimedString(TimePoint timePoint, String string) {
            super();
            this.timePoint = timePoint;
            this.string = string;
        }
        public TimePoint getTimePoint() {
            return timePoint;
        }
        public String getString() {
            return string;
        }
        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((string == null) ? 0 : string.hashCode());
            result = prime * result + ((timePoint == null) ? 0 : timePoint.hashCode());
            return result;
        }
        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            TimedString other = (TimedString) obj;
            if (string == null) {
                if (other.string != null)
                    return false;
            } else if (!string.equals(other.string))
                return false;
            if (timePoint == null) {
                if (other.timePoint != null)
                    return false;
            } else if (!timePoint.equals(other.timePoint))
                return false;
            return true;
        }
        @Override
        public String toString() {
            return "TimedString [timePoint=" + timePoint + ", string=" + string + "]";
        }
    }
    
    private TimedString t(long millis, String s) {
        return new TimedString(TimePoint.of(millis), s);
    }
    
    @Test
    public void testSimpleSequence() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(1, "A"),
                t(2, "B"),
                t(3, "C")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertEquals(Arrays.asList(new Pair<>("A", "B"),
                                   new Pair<>("A", "C"),
                                   new Pair<>("B", "C")), stringPairs);
    }

    @Test
    public void testSimpleSequenceWithEqualElementsNoEquivalenceRelation() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(1, "A"),
                t(1, "A"),
                t(1, "A")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertEquals(Arrays.asList(new Pair<>("A", "A"),
                                   new Pair<>("A", "A"),
                                   new Pair<>("A", "A")), stringPairs);
    }

    @Test
    public void testSimpleSequenceWithEqualElementsWithEquivalenceRelation() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(1, "A"),
                t(1, "A"),
                t(1, "A")).iterator(), Duration.ofMillis(5), ts->ts);
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertTrue(stringPairs.isEmpty());
    }

    @Test
    public void testSimpleSequenceWithEqualAndDifferentElementsWithEquivalenceRelation() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(1, "A"),
                t(1, "A"),
                t(1, "A"),
                t(1, "B"),
                t(1, "B"),
                t(1, "B")).iterator(), Duration.ofMillis(5), ts->ts);
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertEquals(Arrays.asList(new Pair<>("A", "B")), stringPairs);
    }

    @Test
    public void testEqualTimePoints() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(1, "A"),
                t(1, "B")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertTrue(Arrays.asList(new Pair<>("A", "B")).equals(stringPairs) || Arrays.asList(new Pair<>("B", "A")).equals(stringPairs));
    }

    private List<Pair<String, String>> getStringPairs(final TimeWindow<TimedString> timeWindow) {
        final List<Pair<String, String>> stringPairs = new ArrayList<>();
        Util.addAll(Util.map(()->timeWindow, tsPair->new Pair<>(tsPair.getA().getString(), tsPair.getB().getString())), stringPairs);
        return stringPairs;
    }

    @Test
    public void testLongerSequence() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(1, "A"),
                t(2, "B"),
                t(3, "C"),
                t(4, "D"),
                t(5, "E"),
                t(6, "F"),
                t(7, "G")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertEquals(Arrays.asList(
                new Pair<>("A", "B"),
                new Pair<>("A", "C"),
                new Pair<>("A", "D"),
                new Pair<>("A", "E"),
                new Pair<>("A", "F"),
                new Pair<>("B", "C"),
                new Pair<>("B", "D"),
                new Pair<>("B", "E"),
                new Pair<>("B", "F"),
                new Pair<>("B", "G"),
                new Pair<>("C", "D"),
                new Pair<>("C", "E"),
                new Pair<>("C", "F"),
                new Pair<>("C", "G"),
                new Pair<>("D", "E"),
                new Pair<>("D", "F"),
                new Pair<>("D", "G"),
                new Pair<>("E", "F"),
                new Pair<>("E", "G"),
                new Pair<>("F", "G")), stringPairs);
    }

    @Test
    public void testInterruptedSequence() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(1, "A"),
                t(2, "B"),
                t(3, "C"),
                t(100, "E"),
                t(101, "F"),
                t(102, "G")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertEquals(Arrays.asList(new Pair<>("A", "B"),
                                   new Pair<>("A", "C"),
                                   new Pair<>("B", "C"),
                                   new Pair<>("E", "F"),
                                   new Pair<>("E", "G"),
                                   new Pair<>("F", "G")), stringPairs);
    }

    @Test
    public void testEmptySequence() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Collections.<TimedString>emptySet().iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertTrue(stringPairs.isEmpty());
    }
    
    @Test
    public void testOneElementSequence() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Collections.singleton(t(100, "A")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertTrue(stringPairs.isEmpty());
    }
    
    @Test
    public void testTwoIsolatedElements() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(100, "A"),
                t(200, "B")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertTrue(stringPairs.isEmpty());
    }
    
    @Test
    public void testTwoElementsWithinWindow() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(100, "A"),
                t(101, "B")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertEquals(Arrays.asList(new Pair<>("A", "B")), stringPairs);
    }
    
    @Test
    public void testTwoSequencesWithOneElementBetweenThem() {
        final TimeWindow<TimedString> w = new TimeWindow<>(Arrays.asList(
                t(1, "A"),
                t(2, "B"),
                t(3, "C"),
                t(50, "D"),
                t(100, "E"),
                t(101, "F"),
                t(102, "G")).iterator(), Duration.ofMillis(5));
        final List<Pair<String, String>> stringPairs = getStringPairs(w);
        assertEquals(Arrays.asList(new Pair<>("A", "B"),
                                   new Pair<>("A", "C"),
                                   new Pair<>("B", "C"),
                                   new Pair<>("E", "F"),
                                   new Pair<>("E", "G"),
                                   new Pair<>("F", "G")), stringPairs);
    }
}
