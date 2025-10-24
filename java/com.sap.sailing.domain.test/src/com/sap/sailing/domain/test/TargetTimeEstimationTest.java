package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Leg;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.SpeedWithBearingWithConfidence;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.SpeedWithBearingWithConfidenceImpl;
import com.sap.sailing.domain.base.impl.SpeedWithConfidenceImpl;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.NauticalMileDistance;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.polars.PolarDataService;
import com.sap.sailing.domain.tracking.MarkPositionAtTimePointCache;
import com.sap.sailing.domain.tracking.TrackedLeg;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tracking.impl.MarkPositionAtTimePointCacheImpl;
import com.sap.sailing.domain.tracking.impl.TrackedLegImpl;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TargetTimeEstimationTest {
    @SuppressWarnings("unchecked") // the problem here is any(Set.class) which cannot infer the type arguments
    @Test
    public void simpleReachTargetTimeEstimation() throws NotEnoughDataHasBeenAddedException, NoWindException {
        // Setup mock objects
        PolarDataService mockedPolars = mock(PolarDataService.class);
        BoatClass mockedBoatClass = mock(BoatClass.class);
        Position centerOfCourse = new DegreePosition(54.432800, 10.193655);
        TimePoint timepoint = new MillisecondsTimePoint(1431426491696l);
        Bearing windBearing = new DegreeBearingImpl(90);
        SpeedWithBearing windSpeedWithBearing = new KnotSpeedWithBearingImpl(10, windBearing);
        Wind wind = new WindImpl(centerOfCourse, timepoint, windSpeedWithBearing);
        Position startOfLeg = new DegreePosition(54.434648, 10.193312);
        Position endOfLeg = new DegreePosition(54.430454, 10.193226);
        Bearing legBearing = startOfLeg.getBearingGreatCircle(endOfLeg);
        SpeedWithConfidence<Void> boatSpeedWithConfidence = new SpeedWithConfidenceImpl<Void>(new KnotSpeedImpl(12), 1, null);
        DynamicTrackedRaceImpl trackedRace = mock(DynamicTrackedRaceImpl.class);
        WindSource source = mock(WindSource.class);
        Set<WindSource> sources = Collections.singleton(source);
        when(trackedRace.getWindSources(WindSourceType.TRACK_BASED_ESTIMATION)).thenReturn(sources);
        when(trackedRace.getWindSources(WindSourceType.MANEUVER_BASED_ESTIMATION)).thenReturn(sources);
        when(trackedRace.getWind(any(Position.class), eq(timepoint))).thenReturn(wind);
        when(trackedRace.getWind(any(Position.class), eq(timepoint), any(Set.class))).thenReturn(wind);
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getBoatClass()).thenReturn(mockedBoatClass);
        when(trackedRace.getRace()).thenReturn(race);
        Leg leg = mock(Leg.class);
        Waypoint from = mock(Waypoint.class);
        Waypoint to = mock(Waypoint.class);
        when(leg.getFrom()).thenReturn(from);
        when(leg.getTo()).thenReturn(to);
        when(trackedRace.getApproximatePosition(from, timepoint)).thenReturn(startOfLeg);
        when(trackedRace.getApproximatePosition(to, timepoint)).thenReturn(endOfLeg);
        when(trackedRace.getApproximatePosition(from, timepoint)).thenReturn(startOfLeg);
        when(trackedRace.getApproximatePosition(to, timepoint)).thenReturn(endOfLeg);
        when(trackedRace.getApproximatePosition(eq(from), eq(timepoint), any(MarkPositionAtTimePointCacheImpl.class))).thenReturn(startOfLeg);
        when(trackedRace.getApproximatePosition(eq(to), eq(timepoint), any(MarkPositionAtTimePointCacheImpl.class))).thenReturn(endOfLeg);
        when(trackedRace.getApproximatePosition(eq(from), eq(timepoint), any(MarkPositionAtTimePointCacheImpl.class))).thenReturn(startOfLeg);
        when(trackedRace.getApproximatePosition(eq(to), eq(timepoint), any(MarkPositionAtTimePointCacheImpl.class))).thenReturn(endOfLeg);
        when(mockedPolars.getSpeed(mockedBoatClass, wind, legBearing.getDifferenceTo(windBearing.reverse()))).thenReturn(boatSpeedWithConfidence);
        HashSet<Competitor> competitors = new HashSet<Competitor>();
        TrackedLeg trackedLeg = new TrackedLegImpl(trackedRace, leg, competitors);
        // Actual test of functionality
        Duration duration = trackedLeg.getEstimatedTimeAndDistanceToComplete(mockedPolars, timepoint,
                new MarkPositionAtTimePointCacheImpl(trackedRace, timepoint)).getExpectedDuration();
        assertEquals(75494, duration.asMillis(), 100);
    }
    
    @SuppressWarnings("unchecked") // the problem here is any(Set.class) which cannot infer the type arguments
    @Test
    public void simpleUpwindTargetTimeEstimation() throws NotEnoughDataHasBeenAddedException, NoWindException {
        // Setup mock objects
        PolarDataService mockedPolars = mock(PolarDataService.class);
        BoatClass mockedBoatClass = mock(BoatClass.class);
        Position centerOfCourse = new DegreePosition(54.432800, 10.193655);
        TimePoint timepoint = new MillisecondsTimePoint(1431426491696l);
        Bearing windBearing = new DegreeBearingImpl(225); // wind from 045deg (NE)
        SpeedWithBearing windSpeedWithBearing = new KnotSpeedWithBearingImpl(10, windBearing);
        Wind wind = new WindImpl(centerOfCourse, timepoint, windSpeedWithBearing);
        Distance legLength = new NauticalMileDistance(1);
        Position startOfLeg = new DegreePosition(54.430454, 10.193226);
        Position endOfLeg = startOfLeg.translateGreatCircle(new DegreeBearingImpl(40), legLength); // leg end is 1NM approximately to the NE from start, so 5deg off perfect upwind
        DynamicTrackedRaceImpl trackedRace = mock(DynamicTrackedRaceImpl.class);
        when(trackedRace.getCenterOfCourse(timepoint)).thenReturn(centerOfCourse);
        when(trackedRace.getWind(any(Position.class), eq(timepoint))).thenReturn(wind);
        when(trackedRace.getWind(any(Position.class), eq(timepoint), any(Set.class))).thenReturn(wind);
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getBoatClass()).thenReturn(mockedBoatClass);
        when(trackedRace.getRace()).thenReturn(race);
        Leg leg = mock(Leg.class);
        Waypoint from = mock(Waypoint.class);
        Waypoint to = mock(Waypoint.class);
        when(leg.getFrom()).thenReturn(from);
        when(leg.getTo()).thenReturn(to);
        when(trackedRace.getApproximatePosition(eq(from), eq(timepoint))).thenReturn(startOfLeg);
        when(trackedRace.getApproximatePosition(eq(to), eq(timepoint))).thenReturn(endOfLeg);
        when(trackedRace.getApproximatePosition(eq(from), eq(timepoint), any(MarkPositionAtTimePointCache.class))).thenReturn(startOfLeg);
        when(trackedRace.getApproximatePosition(eq(to), eq(timepoint), any(MarkPositionAtTimePointCache.class))).thenReturn(endOfLeg);
        final double speedInKnots = 6.0;
        SpeedWithBearing speedWithBearingPort = new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(-45));
        SpeedWithBearingWithConfidence<Void> boatSpeedWithBearingWithConfidencePort = new SpeedWithBearingWithConfidenceImpl<Void>(speedWithBearingPort, 1, null);
        when(mockedPolars.getAverageSpeedWithTrueWindAngle(mockedBoatClass, wind, LegType.UPWIND, Tack.PORT)).thenReturn(boatSpeedWithBearingWithConfidencePort);
        SpeedWithBearing speedWithBearingStarboard = new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(45));
        SpeedWithBearingWithConfidence<Void> boatSpeedWithBearingWithConfidenceStarboard = new SpeedWithBearingWithConfidenceImpl<Void>(speedWithBearingStarboard, 1, null);
        when(mockedPolars.getAverageSpeedWithTrueWindAngle(mockedBoatClass, wind, LegType.UPWIND, Tack.STARBOARD)).thenReturn(boatSpeedWithBearingWithConfidenceStarboard);
        HashSet<Competitor> competitors = new HashSet<Competitor>();
        TrackedLeg trackedLeg = new TrackedLegImpl(trackedRace, leg, competitors);
        // Actual test of functionality
        Duration duration = trackedLeg.getEstimatedTimeAndDistanceToComplete(mockedPolars, timepoint,
                new MarkPositionAtTimePointCacheImpl(trackedRace, timepoint)).getExpectedDuration();
        // redundantly calculate expected result as follows: sailing a certain, somewhat shorter, distance on STARBOARD tack (wind from starboard)
        // at COG 355, then tacking to PORT tack, sailing COG 85deg for a bit longer to the end of the leg.
        // This happens to be a right triangle with the hypothenusis being the 1NM leg's great circle segment.
        // The one angle is 40deg, the other 50. So the length of the first tack is 1NM*sin(40/180*PI), the
        // other 1NM*sin(50/180*PI). This distance is sailed at a speedInKnots.
        Distance distanceToSail = legLength.scale(Math.sin(40./180.*Math.PI)).add(legLength.scale(Math.sin(50./180.*Math.PI)));
        Duration durationToSail = distanceToSail.atSpeed(speedWithBearingPort);
        assertEquals(durationToSail.asMillis(), duration.asMillis(), 200);
    }
    
    @SuppressWarnings("unchecked") // the problem here is any(Set.class) which cannot infer the type arguments
    @Test
    public void simpleDownwindTargetTimeEstimation() throws NotEnoughDataHasBeenAddedException, NoWindException {
        // Setup mock objects
        PolarDataService mockedPolars = mock(PolarDataService.class);
        BoatClass mockedBoatClass = mock(BoatClass.class);
        Position centerOfCourse = new DegreePosition(54.432800, 10.193655);
        TimePoint timepoint = new MillisecondsTimePoint(1431426491696l);
        Bearing windBearing = new DegreeBearingImpl(60);
        SpeedWithBearing windSpeedWithBearing = new KnotSpeedWithBearingImpl(12, windBearing); // wind from 240deg
        Wind wind = new WindImpl(centerOfCourse, timepoint, windSpeedWithBearing);
        Position startOfLeg = new DegreePosition(54.430454, 10.193226);
        Bearing legBearing = new DegreeBearingImpl(40); // 20deg off perfect downwind, off to the left
        Distance legLength = new NauticalMileDistance(1);
        Position endOfLeg = startOfLeg.translateGreatCircle(legBearing, legLength);
        DynamicTrackedRaceImpl trackedRace = mock(DynamicTrackedRaceImpl.class);
        when(trackedRace.getCenterOfCourse(timepoint)).thenReturn(centerOfCourse);
        when(trackedRace.getWind(any(Position.class), eq(timepoint))).thenReturn(wind);
        when(trackedRace.getWind(any(Position.class), eq(timepoint), any(Set.class))).thenReturn(wind);
        RaceDefinition race = mock(RaceDefinition.class);
        when(race.getBoatClass()).thenReturn(mockedBoatClass);
        when(trackedRace.getRace()).thenReturn(race);
        Leg leg = mock(Leg.class);
        Waypoint from = mock(Waypoint.class);
        Waypoint to = mock(Waypoint.class);
        when(leg.getFrom()).thenReturn(from);
        when(leg.getTo()).thenReturn(to);
        when(trackedRace.getApproximatePosition(eq(from), eq(timepoint))).thenReturn(startOfLeg);
        when(trackedRace.getApproximatePosition(eq(to), eq(timepoint))).thenReturn(endOfLeg);
        when(trackedRace.getApproximatePosition(eq(from), eq(timepoint), any(MarkPositionAtTimePointCache.class))).thenReturn(startOfLeg);
        when(trackedRace.getApproximatePosition(eq(to), eq(timepoint), any(MarkPositionAtTimePointCache.class))).thenReturn(endOfLeg);
        final double speedInKnots = 11.0;
        SpeedWithBearing speedWithBearingPort = new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(150));
        SpeedWithBearingWithConfidence<Void> boatSpeedWithBearingWithConfidencePort = new SpeedWithBearingWithConfidenceImpl<Void>(speedWithBearingPort, 1, null);
        when(mockedPolars.getAverageSpeedWithTrueWindAngle(mockedBoatClass, wind, LegType.DOWNWIND, Tack.PORT)).thenReturn(boatSpeedWithBearingWithConfidencePort);
        SpeedWithBearing speedWithBearingStarboard = new KnotSpeedWithBearingImpl(speedInKnots, new DegreeBearingImpl(-150));
        SpeedWithBearingWithConfidence<Void> boatSpeedWithBearingWithConfidenceStarboard = new SpeedWithBearingWithConfidenceImpl<Void>(speedWithBearingStarboard, 1, null);
        when(mockedPolars.getAverageSpeedWithTrueWindAngle(mockedBoatClass, wind, LegType.DOWNWIND, Tack.STARBOARD)).thenReturn(boatSpeedWithBearingWithConfidenceStarboard);
        HashSet<Competitor> competitors = new HashSet<Competitor>();
        TrackedLeg trackedLeg = new TrackedLegImpl(trackedRace, leg, competitors);
        // Actual test of functionality
        Duration duration = trackedLeg.getEstimatedTimeAndDistanceToComplete(mockedPolars, timepoint,
                new MarkPositionAtTimePointCacheImpl(trackedRace, timepoint)).getExpectedDuration();
        // redundantly calculate expected result as follows: sailing a certain, somewhat shorter, distance on STARBOARD tack (wind from starboard)
        // at COG 090deg, then gybing to PORT tack, sailing COG 030deg for a bit longer to the end of the leg.
        // Short distance on starboard tack is "a", longer distance on port tack is "b".
        // b*sin(10/180*PI) = a*sin(50/180*PI)
        //   and
        // b*cos(10/180*PI) + a*cos(50/180*PI) = legLength
        //   with this:
        // a = legLength / (sin(50/180*PI)*cos(10/180*PI)/sin(10/180*PI) + cos(50/180*PI))
        //   and hence:
        // b = legLength / (sin(50/180*PI)*cos(10/180*PI)/sin(10/180*PI) + cos(50/180*PI)) * sin(50/180*PI) / sin(10/180*PI)
        Distance a = legLength.scale(1. / (Math.sin(50./180.*Math.PI)*Math.cos(10./180.*Math.PI)/Math.sin(10./180.*Math.PI) + Math.cos(50./180.*Math.PI)));
        Distance b = a.scale(Math.sin(50./180.*Math.PI)/Math.sin(10./180.*Math.PI));
        Distance distanceToSail = a.add(b);
        Duration durationToSail = distanceToSail.atSpeed(speedWithBearingPort);
        assertEquals(durationToSail.asMillis(), duration.asMillis(), 100);
    }
}
