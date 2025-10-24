package com.sap.sailing.domain.racelog.analyzing.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.AbstractLogEventAuthor;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult;
import com.sap.sailing.domain.abstractlog.race.CompetitorResult.MergeState;
import com.sap.sailing.domain.abstractlog.race.CompetitorResults;
import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogFinishPositioningConfirmedEvent;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.AbstractFinishPositioningListFinder.CompetitorResultsAndTheirCreationTimePoints;
import com.sap.sailing.domain.abstractlog.race.analyzing.impl.ConfirmedFinishPositioningListFinder;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultImpl;
import com.sap.sailing.domain.abstractlog.race.impl.CompetitorResultsImpl;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sse.common.Util;

public class ConfirmedFinishPositioningListFinderTest extends
        PassAwareRaceLogAnalyzerTest<ConfirmedFinishPositioningListFinder, CompetitorResultsAndTheirCreationTimePoints> {
    
    @Override
    protected ConfirmedFinishPositioningListFinder createAnalyzer(RaceLog raceLog) {
        return new ConfirmedFinishPositioningListFinder(raceLog);
    }

    @Override
    protected TargetPair getTargetEventsAndResultForPassAwareTests(int passId, AbstractLogEventAuthor author) {
        RaceLogFinishPositioningConfirmedEvent event = createEvent(RaceLogFinishPositioningConfirmedEvent.class, 1, passId, author);
        when(event.getPositionedCompetitorsIDsNamesMaxPointsReasons()).thenReturn(mockCompetitorResults());
        return new TargetPair(Arrays.asList(event), new CompetitorResultsAndTheirCreationTimePoints(
                event.getPositionedCompetitorsIDsNamesMaxPointsReasons(), new HashMap<>()));
    }
    
    private CompetitorResults mockCompetitorResults(CompetitorResult... results) {
        final CompetitorResultsImpl result = new CompetitorResultsImpl();
        for (final CompetitorResult r : results) {
            result.add(r);
        }
        return result;
    }

    @Test
    public void testNullForNone() {
        RaceLogEvent event1 = createEvent(RaceLogEvent.class, 1);
        raceLog.add(event1);
        assertNull(analyzer.analyze().getCompetitorResults());
    }

    @Test
    public void testMostRecent() {
        RaceLogFinishPositioningConfirmedEvent event1 = createEvent(RaceLogFinishPositioningConfirmedEvent.class, 1);
        RaceLogFinishPositioningConfirmedEvent event2 = createEvent(RaceLogFinishPositioningConfirmedEvent.class, 2);
        final CompetitorResultImpl oldResultC1 = new CompetitorResultImpl("Comp1", "Comp 1", "Comp 1 Short", "Comp 1 Boat", "Comp 1 Boat Id", 1, MaxPointsReason.NONE, 1.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResultImpl oldResultC2 = new CompetitorResultImpl("Comp2", "Comp 2", "Comp 2 Short", "Comp 2 Boat", "Comp 2 Boat Id", 2, MaxPointsReason.NONE, 2.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResults olderResults = mockCompetitorResults(oldResultC1, oldResultC2);
        final CompetitorResultImpl newResultC2 = new CompetitorResultImpl("Comp2", "Comp 2", "Comp 3 Short", "Comp 3 Boat", "Comp 3 Boat Id", 2, MaxPointsReason.DNF, 3.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResults newerResults = mockCompetitorResults(newResultC2);
        when(event1.getPositionedCompetitorsIDsNamesMaxPointsReasons()).thenReturn(olderResults);
        when(event2.getPositionedCompetitorsIDsNamesMaxPointsReasons()).thenReturn(newerResults);
        raceLog.add(event1);
        raceLog.add(event2);
        assertCompetitorResultsEquals(mockCompetitorResults(oldResultC1, newResultC2), analyzer.analyze().getCompetitorResults());
    }
    
    @Test
    public void testMixtureOfPartialResultsOfDifferentPriority() {
        RaceLogFinishPositioningConfirmedEvent event1 = createEvent(/* priority */ 1, RaceLogFinishPositioningConfirmedEvent.class, 1);
        RaceLogFinishPositioningConfirmedEvent event2 = createEvent(/* priority */ 1, RaceLogFinishPositioningConfirmedEvent.class, 2);
        RaceLogFinishPositioningConfirmedEvent event3 = createEvent(/* priority */ 2, RaceLogFinishPositioningConfirmedEvent.class, 3);
        RaceLogFinishPositioningConfirmedEvent event4 = createEvent(/* priority */ 1, RaceLogFinishPositioningConfirmedEvent.class, 4);
        final CompetitorResultImpl e1ResultC1 = new CompetitorResultImpl("Comp1", "Comp 1", "Comp 1 Short", "Comp 1 Boat", "Comp 1 Boat Id", 1, MaxPointsReason.DNC, 6.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResultImpl e1ResultC2 = new CompetitorResultImpl("Comp2", "Comp 2", "Comp 2 Short", "Comp 2 Boat", "Comp 2 Boat Id", 2, MaxPointsReason.NONE, 2.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResults e1Results = mockCompetitorResults(e1ResultC1, e1ResultC2);
        final CompetitorResultImpl e2ResultC2 = new CompetitorResultImpl("Comp2", "Comp 2", "Comp 2 Short", "Comp 2 Boat", "Comp 2 Boat Id", 2, MaxPointsReason.DNF, 3.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResults e2Results = mockCompetitorResults(e2ResultC2);
        final CompetitorResultImpl e3ResultC2 = new CompetitorResultImpl("Comp2", "Comp 2", "Comp 2 Short", "Comp 2 Boat", "Comp 2 Boat Id", 2, MaxPointsReason.DSQ, 6.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResultImpl e3ResultC3 = new CompetitorResultImpl("Comp3", "Comp 3", "Comp 3 Short", "Comp 3 Boat", "Comp 3 Boat Id", 2, MaxPointsReason.DNF, 3.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResults e3Results = mockCompetitorResults(e3ResultC2, e3ResultC3);
        final CompetitorResultImpl e4ResultC1 = new CompetitorResultImpl("Comp1", "Comp 1", "Comp 1 Short", "Comp 1 Boat", "Comp 1 Boat Id", 1, /* maxPointsReason */ null, /* score */ null, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResultImpl e4ResultC4 = new CompetitorResultImpl("Comp4", "Comp 4", "Comp 4 Short", "Comp 4 Boat", "Comp 4 Boat Id", 2, MaxPointsReason.DNF, 3.0, /* finishing time */ null, /* comment */ null, MergeState.OK);
        final CompetitorResults e4Results = mockCompetitorResults(e4ResultC1, e4ResultC4);
        when(event1.getPositionedCompetitorsIDsNamesMaxPointsReasons()).thenReturn(e1Results);
        when(event2.getPositionedCompetitorsIDsNamesMaxPointsReasons()).thenReturn(e2Results);
        when(event3.getPositionedCompetitorsIDsNamesMaxPointsReasons()).thenReturn(e3Results);
        when(event4.getPositionedCompetitorsIDsNamesMaxPointsReasons()).thenReturn(e4Results);
        // insert in more or less random order
        raceLog.add(event4);
        raceLog.add(event2);
        raceLog.add(event1);
        raceLog.add(event3);
        // e1ResultC1 is expected to be overruled by e4ResultC1 because the latter has the same priority and comes later
        // e1ResultC2 is expected to be overruled by e2ResultC2 due to same priority but later time; and e3ResultC2 has less priority
        // e3ResultC3 and e4ResultC4 stand by themselves; they are not overruled by anything
        assertCompetitorResultsEquals(mockCompetitorResults(e4ResultC1, e2ResultC2, e3ResultC3, e4ResultC4), analyzer.analyze().getCompetitorResults());
    }

    /**
     * Results are considered equal if they contain equal sets of {@link CompetitorResult}s. 
     */
    private void assertCompetitorResultsEquals(CompetitorResults o1, CompetitorResults o2) {
        Set<CompetitorResult> o1Results = new HashSet<>();
        Util.addAll(o1, o1Results);
        Set<CompetitorResult> o2Results = new HashSet<>();
        Util.addAll(o2, o2Results);
        assertEquals(o1Results, o2Results);
    }
}
