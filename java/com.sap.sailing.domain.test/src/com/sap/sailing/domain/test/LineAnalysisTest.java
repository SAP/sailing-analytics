package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.common.Bearing;
import com.sap.sailing.domain.common.Distance;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MeterDistance;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.DynamicTrackedRace;
import com.sap.sailing.domain.tracking.LineDetails;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class LineAnalysisTest extends TrackBasedTest {
    private TimePoint now;
    private DynamicTrackedRace trackedRace;
    private Competitor competitor;

    @Before
    public void setUp() {
        competitor = createCompetitor("Test");
        final Iterable<Competitor> competitorListWithOneCompetitor = Collections.singletonList(competitor);
        now = MillisecondsTimePoint.now();
        trackedRace = createTestTrackedRace("Test Regatta", "Test Race", "505", competitorListWithOneCompetitor, now, /* useMarkPassingCalculator */ false);
    }
    
    @Test
    public void testLinesAnalysisWithNoAdvantage() {
        final LineDetails startLine = trackedRace.getStartLine(now);
        assertEquals(0, startLine.getAdvantage().getMeters(), 0.001);
        final LineDetails finishLine = trackedRace.getFinishLine(now);
        assertEquals(0, finishLine.getAdvantage().getMeters(), 0.001);
    }

    @Test
    public void testStartLineAnalysisWithAdvantageOnStarboard() {
        Mark right = getMark("Right lee gate buoy");
        final DynamicGPSFixTrack<Mark, GPSFix> rightMarkTrack = trackedRace.getOrCreateTrack(right);
        Position rightMarkAtNow = rightMarkTrack.getFirstRawFix().getPosition();
        TimePoint aBitAfterNow = now.plus(1000);
        final MeterDistance advantage = new MeterDistance(10);
        rightMarkTrack.addGPSFix(new GPSFixImpl(rightMarkAtNow.translateGreatCircle(new DegreeBearingImpl(0), advantage), aBitAfterNow));
        final LineDetails startLine = trackedRace.getStartLine(aBitAfterNow);
        assertEquals(advantage.getMeters(), startLine.getAdvantage().getMeters(), 0.1);
        assertEquals(NauticalSide.STARBOARD, startLine.getAdvantageousSideWhileApproachingLine());
        final LineDetails finishLine = trackedRace.getFinishLine(aBitAfterNow);
        assertEquals(advantage.getMeters(), finishLine.getAdvantage().getMeters(), 0.1);
        assertEquals(NauticalSide.PORT, finishLine.getAdvantageousSideWhileApproachingLine());
    }
    
    @Test
    public void testDistanceToLineWithOrthogonalProjectionOnLine() {
        Mark left = getMark("Left lee gate buoy");
        Mark right = getMark("Right lee gate buoy");
        final Position leftPos = trackedRace.getOrCreateTrack(left).getEstimatedPosition(now, /* extrapolate */ false);
        final Position rightPos = trackedRace.getOrCreateTrack(right).getEstimatedPosition(now, /* extrapolate */ false);
        final Bearing lineBearingFromLeftToRight = leftPos.getBearingGreatCircle(rightPos);
        final Bearing awayFromCourseSideOrthogonalToLine = lineBearingFromLeftToRight.add(new DegreeBearingImpl(90));
        final Position lineMiddle = leftPos.translateGreatCircle(lineBearingFromLeftToRight, leftPos.getDistance(rightPos).scale(0.5));
        final Distance distanceBehindLine = new MeterDistance(10);
        final Position behindMiddleOfLine = lineMiddle.translateGreatCircle(awayFromCourseSideOrthogonalToLine, distanceBehindLine);
        trackedRace.getTrack(competitor).add(new GPSFixMovingImpl(behindMiddleOfLine, now, new KnotSpeedWithBearingImpl(5, new DegreeBearingImpl(45))));
        assertEquals(distanceBehindLine.getMeters(), trackedRace.getDistanceToStartLine(competitor, now).getMeters(), 0.000001);
    }

    @Test
    public void testDistanceToLineWithOrthogonalProjectionOutsideLine() {
        Mark left = getMark("Left lee gate buoy");
        Mark right = getMark("Right lee gate buoy");
        final Position leftPos = trackedRace.getOrCreateTrack(left).getEstimatedPosition(now, /* extrapolate */ false);
        final Position rightPos = trackedRace.getOrCreateTrack(right).getEstimatedPosition(now, /* extrapolate */ false);
        final Bearing lineBearingFromLeftToRight = leftPos.getBearingGreatCircle(rightPos);
        final Bearing awayFromCourseSideOrthogonalToLine = lineBearingFromLeftToRight.add(new DegreeBearingImpl(90));
        final Position outsideOfLine = rightPos.translateGreatCircle(lineBearingFromLeftToRight, new MeterDistance(30)); // 30m right of start boat
        final Distance distanceBehindLine = new MeterDistance(10);
        final Position behindOutsideOfLine = outsideOfLine.translateGreatCircle(awayFromCourseSideOrthogonalToLine, distanceBehindLine);
        trackedRace.getTrack(competitor).add(new GPSFixMovingImpl(behindOutsideOfLine, now, new KnotSpeedWithBearingImpl(5, new DegreeBearingImpl(45))));
        // the distance is that of the hypotenuse formed by the two cathesuses of 10m and 30m length, respectively:
        assertEquals(Math.sqrt(10.*10.+30.*30.), trackedRace.getDistanceToStartLine(competitor, now).getMeters(), 0.001);
    }

    private Mark getMark(String name) {
        for (Mark mark : trackedRace.getMarks()) {
            if (mark.getName().equals(name)) {
                return mark;
            }
        }
        return null;
    }
}
