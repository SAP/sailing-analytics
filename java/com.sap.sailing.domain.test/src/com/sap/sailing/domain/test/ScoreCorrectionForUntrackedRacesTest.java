package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.analyzing.impl.RegisteredCompetitorsAnalyzer;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogRegisterCompetitorEventImpl;
import com.sap.sailing.domain.abstractlog.race.tracking.impl.RaceLogUseCompetitorsFromRaceLogEventImpl;
import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumnInSeries;
import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.base.impl.SeriesImpl;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.impl.ScoreCorrectionImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;

/**
 * See bug6168; tests fleet-specific check for score corrections in race columns that suggest that this
 * fleet has finished the race.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class ScoreCorrectionForUntrackedRacesTest extends StoredTrackBasedTest {
    private RaceColumnInSeries f1;
    private RaceColumnInSeries f2;
    private Series series;
    private Fleet gold;
    private Fleet silver;
    private RegattaLog regattaLog;
    private RaceLog goldRaceLogF1;
    private RaceLog goldRaceLogF2;
    private RaceLog silverRaceLogF1;
    private RaceLog silverRaceLogF2;
    private SettableScoreCorrection scoreCorrection;
    private CompetitorWithBoat c1;
    private CompetitorWithBoat c2;
    private AbstractLogEventAuthor author;
    private Leaderboard leaderboard;
    
    @BeforeEach
    public void setUp() {
        leaderboard = mock(Leaderboard.class);
        c1 = createCompetitorWithBoat("C1");
        c2 = createCompetitorWithBoat("C2");
        author = new LogEventAuthorImpl("Test", 1);
        gold = new FleetImpl("Gold", 1);
        silver = new FleetImpl("Silver", 2);
        series = new SeriesImpl("Default", /* isMedal */ false, /* isFleetsCanRunInParallel */ true, Arrays.asList(gold, silver), Arrays.asList("F1", "F2"), /* trackedRegattaRegistry */ null);
        f1 = mock(RaceColumnInSeries.class);
        when(f1.getName()).thenReturn("F1");
        when(f1.getSeries()).thenReturn(series);
        when(f1.getKey(c1)).thenReturn(new Pair<>(c1, f1));
        when(f1.getKey(c2)).thenReturn(new Pair<>(c2, f1));
        goldRaceLogF1 = new RaceLogImpl(UUID.randomUUID());
        silverRaceLogF1 = new RaceLogImpl(UUID.randomUUID());
        when(f1.getRaceLog(gold)).thenReturn(goldRaceLogF1);
        when(f1.getRaceLog(silver)).thenReturn(silverRaceLogF1);
        f2 = mock(RaceColumnInSeries.class);
        when(f2.getName()).thenReturn("F2");
        when(f2.getSeries()).thenReturn(series);
        when(f2.getKey(c1)).thenReturn(new Pair<>(c1, f2));
        when(f2.getKey(c2)).thenReturn(new Pair<>(c2, f2));
        goldRaceLogF2 = new RaceLogImpl(UUID.randomUUID());
        silverRaceLogF2 = new RaceLogImpl(UUID.randomUUID());
        when(f2.getRaceLog(gold)).thenReturn(goldRaceLogF2);
        when(f2.getRaceLog(silver)).thenReturn(silverRaceLogF2);
        goldRaceLogF1.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(TimePoint.now(), author, TimePoint.now(), UUID.randomUUID(), 0));
        goldRaceLogF1.add(new RaceLogRegisterCompetitorEventImpl(TimePoint.now(), author, /* passId */ 0, c1));
        goldRaceLogF2.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(TimePoint.now(), author, TimePoint.now(), UUID.randomUUID(), 0));
        goldRaceLogF2.add(new RaceLogRegisterCompetitorEventImpl(TimePoint.now(), author, /* passId */ 0, c1));
        silverRaceLogF1.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(TimePoint.now(), author, TimePoint.now(), UUID.randomUUID(), 0));
        silverRaceLogF1.add(new RaceLogRegisterCompetitorEventImpl(TimePoint.now(), author, /* passId */ 0, c2));
        silverRaceLogF2.add(new RaceLogUseCompetitorsFromRaceLogEventImpl(TimePoint.now(), author, TimePoint.now(), UUID.randomUUID(), 0));
        silverRaceLogF2.add(new RaceLogRegisterCompetitorEventImpl(TimePoint.now(), author, /* passId */ 0, c2));
        regattaLog = new RegattaLogImpl(UUID.randomUUID());
        when(f1.getAllCompetitors(gold)).thenReturn(new RegisteredCompetitorsAnalyzer(goldRaceLogF1, regattaLog).analyze());
        when(f1.getAllCompetitors(silver)).thenReturn(new RegisteredCompetitorsAnalyzer(silverRaceLogF1, regattaLog).analyze());
        when(f2.getAllCompetitors(gold)).thenReturn(new RegisteredCompetitorsAnalyzer(goldRaceLogF2, regattaLog).analyze());
        when(f2.getAllCompetitors(silver)).thenReturn(new RegisteredCompetitorsAnalyzer(silverRaceLogF2, regattaLog).analyze());
        scoreCorrection = new ScoreCorrectionImpl(leaderboard);
        scoreCorrection.correctScore(c1, f1, 1.0);
        scoreCorrection.correctScore(c1, f2, 1.0);
        scoreCorrection.correctScore(c2, f1, 1.0);
    }
    
    @Test
    public void testNumberOfRacesWithCorrections() {
        assertTrue(scoreCorrection.hasCorrectionForNonTrackedFleet(f1, gold));
        assertTrue(scoreCorrection.hasCorrectionForNonTrackedFleet(f1, silver));
        assertTrue(scoreCorrection.hasCorrectionForNonTrackedFleet(f2, gold));
        assertFalse(scoreCorrection.hasCorrectionForNonTrackedFleet(f2, silver));
    }
}
