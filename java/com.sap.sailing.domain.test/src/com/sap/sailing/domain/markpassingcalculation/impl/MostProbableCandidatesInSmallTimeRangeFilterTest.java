package com.sap.sailing.domain.markpassingcalculation.impl;

import java.util.NavigableSet;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.markpassingcalculation.Candidate;

public class MostProbableCandidatesInSmallTimeRangeFilterTest extends AbstractCandidateFilterTestSupport {
    private MostProbableCandidatesInSmallTimeRangeFilter filter;
    
    @Before
    public void setUp() {
        super.setUp();
        filter = new MostProbableCandidatesInSmallTimeRangeFilter(
                candidateComparator, start, end);
    }

    @Test
    public void testGetTimeWiseContiguousDistanceCandidates() {
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousDistanceCandidates(competitorCandidates, c3, /* includeStartFrom */ true);
        assertContains(result, c1, c2, c3, c4, c5);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesOnRightBorder() {
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousDistanceCandidates(competitorCandidates, c5, /* includeStartFrom */ true);
        assertContains(result, c1, c2, c3, c4, c5);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesOnLeftBorder() {
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousDistanceCandidates(competitorCandidates, c6, /* includeStartFrom */ true);
        assertContains(result, c6, c7, c8, c9, c10);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesInMiddleOfGapNotInserted() {
        final Candidate middleOfGap = candidate(c5.getTimePoint().plus(c5.getTimePoint().until(c6.getTimePoint()).divide(2)), "gap");
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousDistanceCandidates(competitorCandidates, middleOfGap,
                /* includeStartFrom */ true);
        assertContains(result, c1, c2, c3, c4, c5, middleOfGap, c6, c7, c8, c9, c10);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesInMiddleOfGapNotInsertedDontIncludeStartFrom() {
        final Candidate middleOfGap = candidate(c5.getTimePoint().plus(c5.getTimePoint().until(c6.getTimePoint()).divide(2)), "gap");
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousDistanceCandidates(competitorCandidates, middleOfGap,
                /* includeStartFrom */ false);
        assertContains(result, c1, c2, c3, c4, c5, c6, c7, c8, c9, c10);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesInMiddleOfGapInserted() {
        final Candidate middleOfGap = candidate(c5.getTimePoint().plus(c5.getTimePoint().until(c6.getTimePoint()).divide(2)), "gap");
        competitorCandidates.add(middleOfGap);
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousDistanceCandidates(competitorCandidates, middleOfGap,
                /* includeStartFrom */ true);
        assertContains(result, c1, c2, c3, c4, c5, middleOfGap, c6, c7, c8, c9, c10);
    }
    
    @Test
    public void testGetTimeWiseContiguousDistanceCandidatesInMiddleOfWidenedGap() {
        competitorCandidates.remove(c5);
        competitorCandidates.remove(c6);
        final Candidate middleOfGap = candidate(c5.getTimePoint().plus(c5.getTimePoint().until(c6.getTimePoint()).divide(2)), "gap");
        final NavigableSet<Candidate> result = filter.getTimeWiseContiguousDistanceCandidates(competitorCandidates, middleOfGap,
                /* includeStartFrom */ true);
        assertContains(result, middleOfGap);
    }
}
