package com.sap.sailing.domain.markpassingcalculation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Comparator;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.junit.jupiter.api.BeforeEach;

import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.shared.tracking.impl.TimedComparator;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class AbstractCandidateFilterTestSupport {
    protected Candidate c1, c2, c3, c4, c5, c6, c7, c8, c9, c10;
    protected NavigableSet<Candidate> competitorCandidates;
    protected Comparator<Candidate> candidateComparator;
    protected Candidate start;
    protected Candidate end;
    
    @BeforeEach
    public void setUp() {
        final TimePoint now = MillisecondsTimePoint.now();
        start = new CandidateImpl(0, now.minus(Duration.ONE_HOUR), 1.0, null);
        end = new CandidateImpl(6, now.plus(Duration.ONE_HOUR), 1.0, null);
        candidateComparator = (c1, c2)->TimedComparator.INSTANCE.compare(c1, c2);
        competitorCandidates = new TreeSet<>(candidateComparator);
        final Duration gapless = MostProbableCandidatesInSmallTimeRangeFilter.CANDIDATE_FILTER_TIME_WINDOW.divide(2);
        c1 = candidate(now, "c1");
        c2 = candidate(c1.getTimePoint().plus(gapless), "c2");
        c3 = candidate(c2.getTimePoint().plus(gapless), "c3");
        c4 = candidate(c3.getTimePoint().plus(gapless), "c4");
        c5 = candidate(c4.getTimePoint().plus(gapless), "c5");
        // add a cap here that is greater than the threshold but not twice as long;
        // this way, adding one in the middle of this gap is the interesting case
        c6 = candidate(c5.getTimePoint().plus(MostProbableCandidatesInSmallTimeRangeFilter.CANDIDATE_FILTER_TIME_WINDOW.times(1.5)), "c6");
        c7 = candidate(c6.getTimePoint().plus(gapless), "c7");
        c8 = candidate(c7.getTimePoint().plus(gapless), "c8");
        c9 = candidate(c8.getTimePoint().plus(gapless), "c9");
        c10= candidate(c9.getTimePoint().plus(gapless), "c10");
        competitorCandidates.add(c1);
        competitorCandidates.add(c2);
        competitorCandidates.add(c3);
        competitorCandidates.add(c4);
        competitorCandidates.add(c5);
        competitorCandidates.add(c6);
        competitorCandidates.add(c7);
        competitorCandidates.add(c8);
        competitorCandidates.add(c9);
        competitorCandidates.add(c10);
    }

    protected void assertContainsExactly(Iterable<Candidate> candidates, Candidate... candidatesExpected) {
        assertEquals(candidatesExpected.length,
                Util.size(candidates), ""+candidates+"'s size does not match that of "+Arrays.toString(candidatesExpected)+"; ");
        assertContains(candidates, candidatesExpected);
    }
    
    protected void assertContains(Iterable<Candidate> candidates, Candidate...candidatesExpected) {
        for (final Candidate candidateExpected : candidatesExpected) {
            assertTrue(Util.contains(candidates, candidateExpected),
                    "Expected "+candidateExpected+" to be in "+candidates+" but it wasn't; ");
        }
    }

    protected Candidate candidate(TimePoint timePoint, String name) {
        return candidate(timePoint, name, /* waypoint */ null);
    }

    protected Candidate candidate(TimePoint timePoint, String name, Waypoint waypoint) {
        return new CandidateImpl(1, timePoint, 1.0, waypoint) {
            private static final long serialVersionUID = 8547646939054562751L;
            @Override public String toString() { return name; }
        };
    }
}
