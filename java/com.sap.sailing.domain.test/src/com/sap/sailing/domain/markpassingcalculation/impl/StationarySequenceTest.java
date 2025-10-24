package com.sap.sailing.domain.markpassingcalculation.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.NavigableSet;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.test.TrackBasedTest;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class StationarySequenceTest extends AbstractCandidateFilterTestSupport {
    private StationarySequence stationarySequence;
    private DynamicGPSFixTrack<Competitor, GPSFixMoving> track;
    private final static int FIXES_BETWEEN_CANDIDATES = 3;
    
    @BeforeEach
    public void setUp() {
        super.setUp();
        final TimePoint now = MillisecondsTimePoint.now();
        final Waypoint w3 = new WaypointImpl(new ControlPointWithTwoMarksImpl(
                UUID.randomUUID(), new MarkImpl(UUID.randomUUID(), "3p"),
                new MarkImpl(UUID.randomUUID(), "3s"), "Leeward Gate", "Leeward Gate"));
        final Waypoint w5 = new WaypointImpl(new ControlPointWithTwoMarksImpl(
                UUID.randomUUID(), new MarkImpl(UUID.randomUUID(), "Committee Boat"),
                new MarkImpl(UUID.randomUUID(), "Pin"), "Start/Finish", "Start/Finish"));
        c1 = candidate(now, "c1", w3);
        c2 = candidate(c1.getTimePoint().plus(Duration.ONE_SECOND.times(10)), "c2", w5);
        c3 = candidate(c2.getTimePoint().plus(Duration.ONE_SECOND.times(10)), "c3", w3);
        competitorCandidates.clear();
        competitorCandidates.add(c1);
        competitorCandidates.add(c2);
        competitorCandidates.add(c3);
        track = new DynamicGPSFixMovingTrackImpl<Competitor>(TrackBasedTest.createCompetitorWithBoat("Someone"), /* millisecondsOverWhichToAverage */ 15000);
        // construct a track such that the three candidates end up within a single stationary sequence:
        Position position = new DegreePosition(0, 0);
        final SpeedWithBearing verySmallSpeed = new KnotSpeedWithBearingImpl(StationarySequence.CANDIDATE_FILTER_DISTANCE.scale(0.5/ /* number of candidates */ 10. / (double) FIXES_BETWEEN_CANDIDATES).
                inTime(c1.getTimePoint().until(c10.getTimePoint())).getKnots(), new DegreeBearingImpl(0));
        TimePoint previousTimePoint = c1.getTimePoint();
        for (final Candidate candidate : new Candidate[] { c2, c3 }) {
            final Duration durationBetweenFixes = previousTimePoint.until(candidate.getTimePoint()).divide(FIXES_BETWEEN_CANDIDATES);
            TimePoint timePoint = previousTimePoint;
            for (int i=0; i<FIXES_BETWEEN_CANDIDATES; i++) {
                if (previousTimePoint != null) {
                    position = verySmallSpeed.travelTo(position, durationBetweenFixes);
                }
                track.add(new GPSFixMovingImpl(position, timePoint, verySmallSpeed, /* optionalTrueHeading */ null));
                timePoint = timePoint.plus(durationBetweenFixes);
            }
            previousTimePoint = candidate.getTimePoint();
        }
        stationarySequence = new StationarySequence(c1, candidateComparator, track);
        TreeSet<Candidate> candidatesEffectivelyAdded = new TreeSet<>(candidateComparator);
        TreeSet<Candidate> candidatesEffectivelyRemoved = new TreeSet<>(candidateComparator);
        for (final Candidate candidate : new Candidate[] { c2, c3 }) {
            assertTrue(stationarySequence.tryToExtendAfterLast(candidate, candidatesEffectivelyAdded, candidatesEffectivelyRemoved));
        }
    }

    /**
     * See bug 5086; asserts that for a candidate in the middle of a sequence based on a
     * small bounding box but outside of a time interval from the borders in-between candidates
     * are still returned if they are the only one for a given waypoint.
     */
    @Test
    public void testOneCandidatePerWaypointInStationarySequence() {
        assertEquals(3, Util.size(stationarySequence.getAllCandidates()));
        assertTrue(Util.contains(stationarySequence.getValidCandidates(), c2));
    }
    
    /**
     * Asserts that when a fix at one end of the bounding box is replaced by a fix that is on the other side of the bounding box
     * and would, if both were considered by the bounding box, let the bounding box exceed limits; but if the fix replaced is
     * no longer considered, the new fix would leave the bounding box within limits, so no split must happen, and all candidates
     * previously contained must remain within the sequence.
     */
    @Test
    public void testFixReplacementThatKeepsBoundingBoxWithinLimits() {
        assertEquals(3, Util.size(stationarySequence.getAllCandidates()));
        // the track travels north (towards 0deg); let's travel west, then replace by a fix
        // that travels east:
        final TimePoint timePointForNewFix = c2.getTimePoint().plus(c2.getTimePoint().until(c3.getTimePoint()).times(0.5));
        final Position originalPosition = track.getEstimatedPosition(timePointForNewFix, /* extrapolate */ false);
        final Position westPosition = originalPosition.translateGreatCircle(new DegreeBearingImpl(270), StationarySequence.CANDIDATE_FILTER_DISTANCE.scale(0.5));
        final Position eastPosition = originalPosition.translateGreatCircle(new DegreeBearingImpl(90), StationarySequence.CANDIDATE_FILTER_DISTANCE.scale(0.5));
        final GPSFixMovingImpl westFix = new GPSFixMovingImpl(westPosition, timePointForNewFix, originalPosition.getSpeedWithBearingToReachOnGreatCircle(westPosition, c2.getTimePoint().until(timePointForNewFix)), /* optionalTrueHeading */ null);
        track.add(westFix);
        final Set<Candidate> candidatesEffectivelyAdded = new HashSet<>();
        final Set<Candidate> candidatesEffectivelyRemoved = new HashSet<>();
        final NavigableSet<StationarySequence> stationarySequenceSetToUpdate = new TreeSet<>((ss1, ss2)->candidateComparator.compare(ss1.getFirst(), ss2.getFirst()));
        final StationarySequence resultForWestFix = stationarySequence.tryToAddFix(westFix, candidatesEffectivelyAdded, candidatesEffectivelyRemoved, stationarySequenceSetToUpdate, /* isReplacement */ false);
        assertNull(resultForWestFix); // no split should have been necessary
        assertEquals(3, Util.size(stationarySequence.getAllCandidates())); // and all candidates should still be part of the sequence
        assertTrue(candidatesEffectivelyAdded.isEmpty());
        assertTrue(candidatesEffectivelyRemoved.isEmpty());
        final GPSFixMovingImpl eastFix = new GPSFixMovingImpl(eastPosition, timePointForNewFix, originalPosition.getSpeedWithBearingToReachOnGreatCircle(eastPosition, c2.getTimePoint().until(timePointForNewFix)), /* optionalTrueHeading */ null);
        track.add(eastFix, /* replace */ true);
        final StationarySequence resultForEastFix = stationarySequence.tryToAddFix(eastFix, candidatesEffectivelyAdded, candidatesEffectivelyRemoved, stationarySequenceSetToUpdate, /* isReplacement */ true);
        assertNull(resultForEastFix); // again no split should have been necessary
        assertEquals(3, Util.size(stationarySequence.getAllCandidates())); // and all candidates should still be part of the sequence
        assertTrue(candidatesEffectivelyAdded.isEmpty());
        assertTrue(candidatesEffectivelyRemoved.isEmpty());
    }
}
