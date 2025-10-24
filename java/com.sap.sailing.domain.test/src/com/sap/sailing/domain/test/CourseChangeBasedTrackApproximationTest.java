package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.impl.CourseChangeBasedTrackApproximation;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsDurationImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class CourseChangeBasedTrackApproximationTest {
    private DynamicGPSFixTrack<Competitor, GPSFixMoving> track;
    private CourseChangeBasedTrackApproximation approximation;

    @BeforeEach
    public void setUp() throws InterruptedException {
        final CompetitorWithBoat competitor = TrackBasedTest.createCompetitorWithBoat("Someone");
        track = new DynamicGPSFixMovingTrackImpl<Competitor>(competitor,
                /* millisecondsOverWhichToAverage */5000, /* lossless compaction */true);
        approximation = new CourseChangeBasedTrackApproximation(track, competitor.getBoat().getBoatClass());
    }
    
    @Test
    public void simpleTackRecognition() {
        final GPSFixMoving start = fix(10000l, 0, 0, 5, 0);
        track.add(start);
        GPSFixMoving next = start;
        for (int i=0; i<20; i++) {
            track.add(next = travel(next, 1000 /*ms*/, 5 /* knots */, 0 /*deg COG*/));
        }
        final TimePoint startOfTurn = next.getTimePoint().minus(1000); // the turn will be considered started at the previous fix
        // now turn to port over three fixes:
        track.add(next = travel(next, 1000, 4, 340));
        next = travel(next, 1000, 3, 290);
        track.add(next);
        track.add(next = travel(next, 1000, 3, 270));
        final TimePoint endOfTurn = next.getTimePoint();
        for (int i=0; i<20; i++) {
            track.add(next = travel(next, 1000 /*ms*/, 5 /* knots */, 270 /*deg COG*/));
        }
        
        Iterable<GPSFixMoving> candidates = approximation.approximate(start.getTimePoint(), next.getTimePoint());
        assertTrue(Util.size(candidates) >= 1);
        for (final GPSFixMoving candidate : candidates) {
            assertTrue(!candidate.getTimePoint().before(startOfTurn) &&
                    !candidate.getTimePoint().after(endOfTurn));
        }
    }

    private GPSFixMoving fix(long timepoint, double lat, double lon, double speedInKnots, double cogDeg) {
        return new GPSFixMovingImpl(new DegreePosition(lat, lon), new MillisecondsTimePoint(timepoint), new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(cogDeg)), /* optionalTrueHeading */ null);
    }
    
    private GPSFixMoving travel(GPSFixMoving fix, long durationInMillis, double speedInKnots, double cogDeg) {
        return new GPSFixMovingImpl(fix.getPosition().translateGreatCircle(new DegreeBearingImpl(cogDeg), new KnotSpeedImpl(speedInKnots).travel(new MillisecondsDurationImpl(durationInMillis))),
                fix.getTimePoint().plus(durationInMillis), new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(cogDeg)), /* optionalTrueHeading */ null);
    }
    
}
