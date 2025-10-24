package com.sap.sailing.domain.markpassingcalculation.impl;

import java.util.NavigableSet;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.markpassingcalculation.Candidate;

public class MostProbableCandidatesInSmallTimeRangeFilterTest extends AbstractCandidateFilterTestSupport {
    private MostProbableCandidatesInSmallTimeRangeFilter filter;
    
    @BeforeEach
    public void setUp() {
        super.setUp();
        filter = new MostProbableCandidatesInSmallTimeRangeFilter(
                candidateComparator, start, end);
    }

    @Test
    public void testGetTimeWiseContiguousDistanceCandidates() {
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousCandidates(competitorCandidates, c3, /* includeStartFrom */ true);
        assertContainsExactly(result, c1, c2, c3, c4, c5);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesOnRightBorder() {
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousCandidates(competitorCandidates, c5, /* includeStartFrom */ true);
        assertContainsExactly(result, c1, c2, c3, c4, c5);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesOnLeftBorder() {
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousCandidates(competitorCandidates, c6, /* includeStartFrom */ true);
        assertContainsExactly(result, c6, c7, c8, c9, c10);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesInMiddleOfGapNotInserted() {
        final Candidate middleOfGap = candidate(c5.getTimePoint().plus(c5.getTimePoint().until(c6.getTimePoint()).divide(2)), "gap");
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousCandidates(competitorCandidates, middleOfGap,
                /* includeStartFrom */ true);
        assertContainsExactly(result, c1, c2, c3, c4, c5, middleOfGap, c6, c7, c8, c9, c10);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesInMiddleOfGapNotInsertedDontIncludeStartFrom() {
        final Candidate middleOfGap = candidate(c5.getTimePoint().plus(c5.getTimePoint().until(c6.getTimePoint()).divide(2)), "gap");
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousCandidates(competitorCandidates, middleOfGap,
                /* includeStartFrom */ false);
        assertContainsExactly(result, c1, c2, c3, c4, c5, c6, c7, c8, c9, c10);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesInMiddleOfGapInserted() {
        final Candidate middleOfGap = candidate(c5.getTimePoint().plus(c5.getTimePoint().until(c6.getTimePoint()).divide(2)), "gap");
        competitorCandidates.add(middleOfGap);
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousCandidates(competitorCandidates, middleOfGap,
                /* includeStartFrom */ true);
        assertContainsExactly(result, c1, c2, c3, c4, c5, middleOfGap, c6, c7, c8, c9, c10);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesInMiddleOfWidenedGap() {
        competitorCandidates.remove(c5);
        competitorCandidates.remove(c6);
        final Candidate middleOfGap = candidate(c5.getTimePoint().plus(c5.getTimePoint().until(c6.getTimePoint()).divide(2)), "gap");
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousCandidates(competitorCandidates, middleOfGap,
                /* includeStartFrom */ true);
        assertContainsExactly(result, middleOfGap);
    }
}
