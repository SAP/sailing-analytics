package com.sap.sailing.domain.orc;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveCourse;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLeg;
import com.sap.sailing.domain.common.orc.ORCPerformanceCurveLegTypes;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveCourseImpl;
import com.sap.sailing.domain.common.orc.impl.ORCPerformanceCurveLegImpl;
import com.sap.sailing.domain.orc.impl.ORCPerformanceCurveLegAdapter;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.WindLegTypeAndLegBearingAndORCPerformanceCurveCache;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TestORCPerformanceCurveCourse {
    @Test
    public void testSubcourseOfSimpleORCCourse() {
        double accuracy = 0.000000001;
        List<ORCPerformanceCurveLeg> legs = new ArrayList<>();
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(0)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(90)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(120)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(180)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(0)));
        ORCPerformanceCurveCourse course = new ORCPerformanceCurveCourseImpl(legs);
        // case 0: no leg finished, 40.0% of current leg
        ORCPerformanceCurveCourse subcourse0 = course.subcourse(0, 0.4);
        assertEquals(0.4, subcourse0.getTotalLength().getNauticalMiles(), accuracy);
        // case 1: first leg finished, 0.0% of current leg
        ORCPerformanceCurveCourse subcourse1 = course.subcourse(1, 0);
        assertEquals(1, subcourse1.getTotalLength().getNauticalMiles(), accuracy);
        // case 2: first leg finished, 12.5% of current leg
        ORCPerformanceCurveCourse subcourse2 = course.subcourse(1, 0.125);
        assertEquals(1.125, subcourse2.getTotalLength().getNauticalMiles(), accuracy);
        // special case: didn't start, equals to no legs finished and 0.0% of current leg
        ORCPerformanceCurveCourse subcourseSpecial1 = course.subcourse(0, 0);
        assertEquals(0, subcourseSpecial1.getTotalLength().getNauticalMiles(), accuracy);
        // special case: number of finished legs is higher then number of actual legs
        ORCPerformanceCurveCourse subcourseSpecial2 = course.subcourse(10,0);
        assertEquals(5, subcourseSpecial2.getTotalLength().getNauticalMiles(), accuracy);
    }
    
    @Test
    public void testSubcourseOfComplexCourse() {
        double accuracy = 0.000000001;
        List<ORCPerformanceCurveLeg> legs = new ArrayList<>();
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(3.78), new DegreeBearingImpl(0)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1.45), new DegreeBearingImpl(90)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(5), new DegreeBearingImpl(120)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(10.23), new DegreeBearingImpl(180)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2.57), new DegreeBearingImpl(0)));
        ORCPerformanceCurveCourse course = new ORCPerformanceCurveCourseImpl(legs);
        // case 0: no leg finished, 40.0% of current leg
        ORCPerformanceCurveCourse subcourse0 = course.subcourse(0, 0.4);
        assertEquals(1.512, subcourse0.getTotalLength().getNauticalMiles(), accuracy);
        // case 1: first leg finished, 0.0% of current leg
        ORCPerformanceCurveCourse subcourse1 = course.subcourse(1, 0);
        assertEquals(3.78, subcourse1.getTotalLength().getNauticalMiles(), accuracy);
        // case 2: first leg finished, 12.5% of current leg
        ORCPerformanceCurveCourse subcourse2 = course.subcourse(1, 0.125);
        assertEquals(3.96125, subcourse2.getTotalLength().getNauticalMiles(), accuracy);
        // special case: didn't start, equals to no legs finished and 0.0% of current leg
        ORCPerformanceCurveCourse subcourseSpecial1 = course.subcourse(0, 0);
        assertEquals(0, subcourseSpecial1.getTotalLength().getNauticalMiles(), accuracy);
        // special case: number of finished legs is higher then number of actual legs
        ORCPerformanceCurveCourse subcourseSpecial2 = course.subcourse(10,0);
        assertEquals(23.03, subcourseSpecial2.getTotalLength().getNauticalMiles(), accuracy);
    }
    
    @Test
    public void testSubcourseOfCourseWithMixedLegTypes() {
        double accuracy = 0.000000001;
        List<ORCPerformanceCurveLeg> legs = new ArrayList<>();
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(0)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2), ORCPerformanceCurveLegTypes.LONG_DISTANCE));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(3), ORCPerformanceCurveLegTypes.CIRCULAR_RANDOM));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(4), new DegreeBearingImpl(0)));
        ORCPerformanceCurveCourse course = new ORCPerformanceCurveCourseImpl(legs);
        // case 0: no leg finished, 40.0% of current leg
        ORCPerformanceCurveCourse subcourse0 = course.subcourse(0, 0.4);
        assertEquals(0.4, subcourse0.getTotalLength().getNauticalMiles(), accuracy);
        // case 1: first leg finished, 0.0% of current leg
        ORCPerformanceCurveCourse subcourse1 = course.subcourse(1, 0);
        assertEquals(1, subcourse1.getTotalLength().getNauticalMiles(), accuracy);
        // case 2: first leg finished, 12.5% of current leg
        ORCPerformanceCurveCourse subcourse2 = course.subcourse(1, 0.125);
        assertEquals(1.25, subcourse2.getTotalLength().getNauticalMiles(), accuracy);
        assertEquals(ORCPerformanceCurveLegTypes.LONG_DISTANCE, Util.last(subcourse2.getLegs()).getType());
        assertEquals(0, subcourse2.getLegs().iterator().next().getTwa().getDegrees(), accuracy);
        // special case: didn't start, equals to no legs finished and no leg started yet, so zero legs
        ORCPerformanceCurveCourse subcourseSpecial1 = course.subcourse(0, 0);
        assertEquals(0, subcourseSpecial1.getTotalLength().getNauticalMiles(), accuracy);
        assertEquals(0, Util.size(subcourseSpecial1.getLegs()));
        // special case: number of finished legs is higher then number of actual legs
        ORCPerformanceCurveCourse subcourseSpecial2 = course.subcourse(10, 0);
        assertEquals(10, subcourseSpecial2.getTotalLength().getNauticalMiles(), accuracy);
        assertEquals(ORCPerformanceCurveLegTypes.TWA, Util.get(subcourseSpecial2.getLegs(), 0).getType());
        assertEquals(ORCPerformanceCurveLegTypes.LONG_DISTANCE, Util.get(subcourseSpecial2.getLegs(), 1).getType());
        assertEquals(ORCPerformanceCurveLegTypes.CIRCULAR_RANDOM, Util.get(subcourseSpecial2.getLegs(), 2).getType());
        assertEquals(ORCPerformanceCurveLegTypes.TWA, Util.get(subcourseSpecial2.getLegs(), 3).getType());
    }
    
    @Test
    public void testSubcourseOfSubcourse() {
        double accuracy = 0.000000001;
        List<ORCPerformanceCurveLeg> legs = new ArrayList<>();
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(1), new DegreeBearingImpl(0)));
        legs.add(new ORCPerformanceCurveLegImpl(new NauticalMileDistance(2), new DegreeBearingImpl(0)));
        ORCPerformanceCurveCourse course = new ORCPerformanceCurveCourseImpl(legs);
        // case 0: no leg finished, 40.0% of current leg
        ORCPerformanceCurveCourse subcourse0 = course.subcourse(0, 0.4);
        assertEquals(0.4, subcourse0.getTotalLength().getNauticalMiles(), accuracy);
        // case 1: first leg finished, 0.0% of current leg
        ORCPerformanceCurveCourse subcourse1 = subcourse0.subcourse(0, 0.1);
        assertEquals(0.04, subcourse1.getTotalLength().getNauticalMiles(), accuracy);
    }

    @Test
    public void testSubcourseOfSubcourseWithTrackedLegAdapter() {
        double accuracy = 0.000000001;
        Position p1 = new DegreePosition(1, 2);
        TimePoint t1 = MillisecondsTimePoint.now();
        SpeedWithBearing s1 = new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(3));
        Wind w1 = new WindImpl(p1, t1, s1);
        List<ORCPerformanceCurveLeg> legs = new ArrayList<>();
        final TrackedLeg trackedLeg1 = Mockito.mock(TrackedLeg.class);
        TrackedRace trackedRace = Mockito.mock(TrackedRace.class);
        Mockito.when(trackedRace.getWindWithConfidence(ArgumentMatchers.any(), ArgumentMatchers.any()))
                .thenReturn(new WindWithConfidenceImpl<Util.Pair<Position, TimePoint>>(w1, 1.0,
                        new Util.Pair<Position, TimePoint>(new DegreePosition(1.0, 1.0), MillisecondsTimePoint.now()),
                        false));
        Mockito.when(trackedLeg1.getEquidistantReferenceTimePoints(10)).thenReturn(Collections.singleton(new MillisecondsTimePoint(new Date(1))));
        Mockito.when(trackedLeg1.getEquidistantSectionsOfLeg(ArgumentMatchers.any(), ArgumentMatchers.anyInt())).thenReturn(Collections.singleton(new DegreePosition(1.0d, 1.0d)));
        Mockito.when(trackedLeg1.getTrackedRace()).thenReturn(trackedRace);
        Mockito.when(trackedLeg1.getWindwardDistance(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(WindLegTypeAndLegBearingAndORCPerformanceCurveCache.class))).thenReturn(new NauticalMileDistance(1));
        final TrackedLeg trackedLeg2 = Mockito.mock(TrackedLeg.class);
        Mockito.when(trackedLeg2.getWindwardDistance(ArgumentMatchers.any(), ArgumentMatchers.any(),
                ArgumentMatchers.any(WindLegTypeAndLegBearingAndORCPerformanceCurveCache.class))).thenReturn(new NauticalMileDistance(2));
        legs.add(new ORCPerformanceCurveLegAdapter(trackedLeg1) {
            private static final long serialVersionUID = 2173629869089646863L;

            @Override
            public Bearing getTwa() {
                return new DegreeBearingImpl(0);
            }

            @Override
            public ORCPerformanceCurveLegTypes getType() {
                return ORCPerformanceCurveLegTypes.TWA;
            }
        });
        legs.add(new ORCPerformanceCurveLegAdapter(trackedLeg2) {
            private static final long serialVersionUID = 5651091430433706403L;

            @Override
            public Bearing getTwa() {
                return new DegreeBearingImpl(0);
            }

            @Override
            public ORCPerformanceCurveLegTypes getType() {
                return ORCPerformanceCurveLegTypes.TWA;
            }
        });
        ORCPerformanceCurveCourse course = new ORCPerformanceCurveCourseImpl(legs);
        // case 0: no leg finished, 40.0% of current leg
        ORCPerformanceCurveCourse subcourse0 = course.subcourse(0, 0.4);
        assertEquals(0.4, subcourse0.getTotalLength().getNauticalMiles(), accuracy);
        // case 1: first leg finished, 0.0% of current leg
        ORCPerformanceCurveCourse subcourse1 = subcourse0.subcourse(0, 0.1);
        assertEquals(0.04, subcourse1.getTotalLength().getNauticalMiles(), accuracy);
    }
}
