package com.sap.sailing.domain.markpassingcalculation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CourseImpl;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.common.util.IntHolder;

public class MarkPassingCalculatorTest {
    private static class MyMarkPassingCalculator extends MarkPassingCalculator {
        public MyMarkPassingCalculator(DynamicTrackedRace race, boolean doListen,
                boolean waitForInitialMarkPassingCalculation) {
            super(race, doListen, waitForInitialMarkPassingCalculation, /* markPassingRaceFingerprintRegistry */ null);
        }

        @Override
        protected void enqueueUpdate(StorePositionUpdateStrategy update) {
            super.enqueueUpdate(update);
        }
    }
    @Test
    public void testSuspendResume() throws InterruptedException {
        final boolean[] executed = new boolean[1];
        DynamicTrackedRegatta trackedRegatta = mock(DynamicTrackedRegatta.class);
        RaceDefinition race = mock(RaceDefinition.class);
        Regatta regatta = mock(Regatta.class);
        when(trackedRegatta.getRegatta()).thenReturn(regatta);
        DynamicTrackedRace trackedRace = mock(DynamicTrackedRace.class);
        when(trackedRace.getTrackedRegatta()).thenReturn(trackedRegatta);
        when(trackedRace.getRace()).thenReturn(race);
        when(race.getCompetitors()).thenReturn(Collections.emptyList());
        Course course = new CourseImpl("Course", Collections.emptyList());
        when(race.getCourse()).thenReturn(course);
        MyMarkPassingCalculator mpc = new MyMarkPassingCalculator(trackedRace, /* doListen */ true, /* waitForInitialMarkPassingCalculation */ true);
        mpc.suspend();
        mpc.enqueueUpdate(new StorePositionUpdateStrategy() {
            @Override
            public void storePositionUpdate(Map<Competitor, List<GPSFixMoving>> competitorFixes, Map<Competitor, List<GPSFixMoving>> competitorFixesThatReplacedExistingOnes,
                    Map<Mark, List<GPSFix>> markFixes, List<Waypoint> addedWaypoints, List<Waypoint> removedWaypoints,
                    IntHolder smallestChangedWaypointIndex,
                    List<Triple<Competitor, Integer, TimePoint>> fixedMarkPassings, List<Pair<Competitor, Integer>> removedMarkPassings,
                    List<Pair<Competitor, Integer>> suppressedMarkPassings, List<Competitor> unSuppressedMarkPassings,
                    CandidateFinder candidateFinder, CandidateChooser candidateChooser) {
                executed[0] = true;
            }
        });
        assertFalse(executed[0]);
        mpc.resume();
        mpc.waitUntilStopped(1000);
        assertTrue(executed[0]);
    }
}
