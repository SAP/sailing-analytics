package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.leaderboard.impl.LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal;

public class LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinalTest {
    private Regatta regatta;
    private LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal scoringScheme;
    private RaceColumnInSeries quarterFinal;
    private Series quarterFinalSeries;
    private RaceColumnInSeries semiFinal;
    private Series semiFinalSeries;
    private RaceColumnInSeries grandFinal;
    private Series grandFinalSeries;
    private HashMap<Competitor, Integer> competitorsRankedByOpeningSeries;
    private Competitor c1, c2, c3, c4, c5;

    @BeforeEach
    public void setUp() {
        regatta = mock(Regatta.class);
        scoringScheme = new LowPointWithEliminatingMedalSeriesPromotingOneToFinalAndTwoToSemifinal();
        quarterFinal = mock(RaceColumnInSeries.class);
        quarterFinalSeries = mockMedalSeries(regatta, quarterFinal);
        semiFinal = mock(RaceColumnInSeries.class);
        semiFinalSeries = mockMedalSeries(regatta, semiFinal);
        grandFinal = mock(RaceColumnInSeries.class);
        grandFinalSeries = mockMedalSeries(regatta, grandFinal);
        when(regatta.getSeries()).thenAnswer(new Answer<Iterable<Series>>() {
            @Override
            public Iterable<Series> answer(InvocationOnMock invocation) throws Throwable {
                return Arrays.asList(quarterFinalSeries, semiFinalSeries, grandFinalSeries);
            }
        });
        when(quarterFinal.getSeries()).thenReturn(quarterFinalSeries);
        when(semiFinal.getSeries()).thenReturn(semiFinalSeries);
        when(grandFinal.getSeries()).thenReturn(grandFinalSeries);
        c1 = mock(Competitor.class);
        c2 = mock(Competitor.class);
        c3 = mock(Competitor.class);
        c4 = mock(Competitor.class);
        c5 = mock(Competitor.class);
        competitorsRankedByOpeningSeries = new HashMap<>();
        competitorsRankedByOpeningSeries.put(c1, 1);
        competitorsRankedByOpeningSeries.put(c2, 2);
        competitorsRankedByOpeningSeries.put(c3, 3);
        competitorsRankedByOpeningSeries.put(c4, 4);
        competitorsRankedByOpeningSeries.put(c5, 5);
    }

    @Test
    public void testCountingCompetitorsBetterThanThoseSailingInSeries() {
        assertEquals(0, scoringScheme.getNumberOfCompetitorsBetterThanThoseSailingInSeries(grandFinalSeries));
        assertEquals(1, scoringScheme.getNumberOfCompetitorsBetterThanThoseSailingInSeries(semiFinalSeries));
        assertEquals(3, scoringScheme.getNumberOfCompetitorsBetterThanThoseSailingInSeries(quarterFinalSeries));
    }
    
    @Test
    public void testCountingCompetitorsAdvancingFromOpeningSeries() {
        assertEquals(1, scoringScheme.getNumberOfCompetitorsAdvancingFromOpeningSeriesToOrThroughSeries(grandFinalSeries));
        assertEquals(3, scoringScheme.getNumberOfCompetitorsAdvancingFromOpeningSeriesToOrThroughSeries(semiFinalSeries));
        assertEquals(3, scoringScheme.getNumberOfCompetitorsAdvancingFromOpeningSeriesToOrThroughSeries(quarterFinalSeries));
    }
    
    @Test
    public void testQuarterFinalParticipationForPromotedCompetitors() {
        assertTrue(scoringScheme.isParticipatingInMedalRace(c1, /* competitorMedalRaceScore */ null, quarterFinal, ()->competitorsRankedByOpeningSeries));
        assertTrue(scoringScheme.isParticipatingInMedalRace(c2, /* competitorMedalRaceScore */ null, quarterFinal, ()->competitorsRankedByOpeningSeries));
        assertTrue(scoringScheme.isParticipatingInMedalRace(c3, /* competitorMedalRaceScore */ null, quarterFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c4, /* competitorMedalRaceScore */ null, quarterFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c5, /* competitorMedalRaceScore */ null, quarterFinal, ()->competitorsRankedByOpeningSeries));
    }
    
    @Test
    public void testSemiFinalParticipationForPromotedCompetitors() {
        assertTrue(scoringScheme.isParticipatingInMedalRace(c1, /* competitorMedalRaceScore */ null, semiFinal, ()->competitorsRankedByOpeningSeries));
        assertTrue(scoringScheme.isParticipatingInMedalRace(c2, /* competitorMedalRaceScore */ null, semiFinal, ()->competitorsRankedByOpeningSeries));
        assertTrue(scoringScheme.isParticipatingInMedalRace(c3, /* competitorMedalRaceScore */ null, semiFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c4, /* competitorMedalRaceScore */ null, semiFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c5, /* competitorMedalRaceScore */ null, semiFinal, ()->competitorsRankedByOpeningSeries));
    }
    
    @Test
    public void testGrandFinalParticipationForPromotedCompetitors() {
        assertTrue(scoringScheme.isParticipatingInMedalRace(c1, /* competitorMedalRaceScore */ null, grandFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c2, /* competitorMedalRaceScore */ null, grandFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c3, /* competitorMedalRaceScore */ null, grandFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c4, /* competitorMedalRaceScore */ null, grandFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c5, /* competitorMedalRaceScore */ null, grandFinal, ()->competitorsRankedByOpeningSeries));
    }
    
    @Test
    public void testGrandFinalParticipationForScoringCompetitors() {
        assertTrue(scoringScheme.isParticipatingInMedalRace(c1, /* competitorMedalRaceScore */ 3.0, grandFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c2, /* competitorMedalRaceScore */ null, grandFinal, ()->competitorsRankedByOpeningSeries));
        assertTrue(scoringScheme.isParticipatingInMedalRace(c3, /* competitorMedalRaceScore */ 1.0, grandFinal, ()->competitorsRankedByOpeningSeries));
        assertTrue(scoringScheme.isParticipatingInMedalRace(c4, /* competitorMedalRaceScore */ 2.0, grandFinal, ()->competitorsRankedByOpeningSeries));
        assertFalse(scoringScheme.isParticipatingInMedalRace(c5, /* competitorMedalRaceScore */ null, grandFinal, ()->competitorsRankedByOpeningSeries));
    }
    
    @Test
    public void testMedalRaceParticipationForScoringCompetitors() {
        assertTrue(scoringScheme.isParticipatingInMedalRace(c1, /* competitorMedalRaceScore */ 4.0, quarterFinal, ()->competitorsRankedByOpeningSeries));
        assertTrue(scoringScheme.isParticipatingInMedalRace(c4, /* competitorMedalRaceScore */ 2.0, quarterFinal, ()->competitorsRankedByOpeningSeries));
        assertTrue(scoringScheme.isParticipatingInMedalRace(c5, /* competitorMedalRaceScore */ 3.0, quarterFinal, ()->competitorsRankedByOpeningSeries));
    }
    
    private Series mockMedalSeries(Regatta regatta, RaceColumnInSeries... raceColumns) {
        final Iterable<RaceColumnInSeries> medalSeriesColumns = Arrays.asList(raceColumns);
        final Series medalSeries = mock(Series.class);
        when(medalSeries.getRegatta()).thenReturn(regatta);
        when(medalSeries.isMedal()).thenReturn(true);
        when(medalSeries.getRaceColumns()).thenAnswer(new Answer<Iterable<RaceColumnInSeries>>() {
            @Override
            public Iterable<RaceColumnInSeries> answer(InvocationOnMock invocation) throws Throwable {
                return medalSeriesColumns;
            }
        });
        return medalSeries;
    }
}
