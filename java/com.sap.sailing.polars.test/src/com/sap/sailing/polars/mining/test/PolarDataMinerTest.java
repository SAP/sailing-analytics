package com.sap.sailing.polars.mining.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.SpeedWithConfidence;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.PolarSheetGenerationSettings;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.PolarSheetGenerationSettingsImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSpeedSteppingWithMaxDistance;
import com.sap.sailing.domain.common.polars.NotEnoughDataHasBeenAddedException;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.domain.tracking.TrackedRaceStatus;
import com.sap.sailing.domain.tracking.WindWithConfidence;
import com.sap.sailing.domain.tracking.impl.DynamicGPSFixMovingTrackImpl;
import com.sap.sailing.domain.tracking.impl.WindWithConfidenceImpl;
import com.sap.sailing.polars.mining.BearingClusterGroup;
import com.sap.sailing.polars.mining.CubicRegressionPerCourseProcessor;
import com.sap.sailing.polars.mining.PolarDataMiner;
import com.sap.sailing.polars.mining.SpeedRegressionPerAngleClusterProcessor;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.datamining.data.ClusterGroup;

public class PolarDataMinerTest {

    private static final int MILLISECONDS_OVER_WHICH_TO_AVERAGE_SPEED = 30;
    private static final double EPSILON = 1E-4;
    
    public static PolarSheetGenerationSettings createTestPolarSettings() {
        List<Double> levelList = new ArrayList<Double>();
        for (double levelValue = 0.5; levelValue < 35; levelValue = levelValue + 0.5) {
            levelList.add(levelValue);
        }
        double[] levels = new double[levelList.size()];
        int i=0;
        for (Double level : levelList) {
            levels[i++] = level;
        }
        WindSpeedSteppingWithMaxDistance windStepping = new WindSpeedSteppingWithMaxDistance(levels, 0.5);
        return new PolarSheetGenerationSettingsImpl(50, 0.1, 20, 20, 0.1, true, true, 2, 0.05, true, windStepping,
                false, 3);
    }
    
    private ClusterGroup<Bearing> createAngleClusterGroup() {
        return new BearingClusterGroup(0, 180, 5);
    }

    @Disabled("The test did work before DataMining was used for polars; maybe rework in the future...")
    @Test
    public void testGrouping() throws InterruptedException, TimeoutException, NoSuchMethodException,
            NotEnoughDataHasBeenAddedException {
        PolarSheetGenerationSettings settings = createTestPolarSettings();
        ClusterGroup<Bearing> angleClusterGroup = createAngleClusterGroup();
        PolarDataMiner miner = new PolarDataMiner(settings,
                new CubicRegressionPerCourseProcessor(),
                new SpeedRegressionPerAngleClusterProcessor(angleClusterGroup), angleClusterGroup);

        BoatClass mockedBoatClass = mock(BoatClass.class);
        
        when(mockedBoatClass.getName()).thenReturn("49ER");

        GPSFixMoving fix1_1 = createMockedFix(13, 00, 54.431952, 10.186767, 45, 10.5);
        GPSFixMoving fix1_2 = createMockedFix(13, 30, 54.485034, 10.538303, 44.8, 10.6);
        Competitor competitor1 = mock(Competitor.class);
        
        GPSFixMoving fix2_1 = createMockedFix(13, 00, 54.443942, 10.172739, 42.2, 10);
        GPSFixMoving fix2_2 = createMockedFix(13, 30, 54.425394, 10.177404, 41.8, 10.1);
        Competitor competitor2 = mock(Competitor.class);
        
        GPSFixMoving fix3_1 = createMockedFix(13, 45, 54.873740, 10.193648, 43.2, 10);
        Competitor competitor3 = mock(Competitor.class);

        Map<Competitor, Set<GPSFixMoving>> fixesPerCompetitor = new HashMap<Competitor, Set<GPSFixMoving>>();
        
        Set<GPSFixMoving> setForCompetitor1 = new HashSet<GPSFixMoving>();
        setForCompetitor1.add(fix1_1);
        setForCompetitor1.add(fix1_2);
        fixesPerCompetitor.put(competitor1, setForCompetitor1);
        
        Set<GPSFixMoving> setForCompetitor2 = new HashSet<GPSFixMoving>();
        setForCompetitor2.add(fix2_1);
        setForCompetitor2.add(fix2_2);
        fixesPerCompetitor.put(competitor2, setForCompetitor2);
        
        Set<GPSFixMoving> setForCompetitor3 = new HashSet<GPSFixMoving>();
        setForCompetitor2.add(fix3_1);
        fixesPerCompetitor.put(competitor3, setForCompetitor3);
        

        TrackedRace trackedRace = createMockedTrackedRace(fixesPerCompetitor, mockedBoatClass);
        
        for (Entry<Competitor, Set<GPSFixMoving>> entry : fixesPerCompetitor.entrySet()) {
            Competitor competitor = entry.getKey();
            Set<GPSFixMoving> fixSet = entry.getValue();
            for (GPSFixMoving fix : fixSet) {
                miner.addFix(fix, competitor, trackedRace);
            }
        }
        
        int millisLeft = 500000;
        while (miner.isCurrentlyActiveAndOrHasQueue() && millisLeft > 0) {
            Thread.sleep(100);
            millisLeft = millisLeft - 100;
            if (miner.isCurrentlyActiveAndOrHasQueue() && millisLeft <= 0) {
                throw new TimeoutException();
            }
        }

        SpeedWithConfidence<Void> estimatedSpeed1 = miner.estimateBoatSpeed(mockedBoatClass, new KnotSpeedImpl(15),
                new DegreeBearingImpl(44.9));
        assertThat(estimatedSpeed1, is(notNullValue()));
        assertThat(estimatedSpeed1.getObject().getKnots(), is(closeTo(10.55, EPSILON)));

        SpeedWithConfidence<Void> estimatedSpeed2 = miner.estimateBoatSpeed(mockedBoatClass, new KnotSpeedImpl(15),
                new DegreeBearingImpl(42));
        assertThat(estimatedSpeed2, is(notNullValue()));
        assertThat(estimatedSpeed2.getObject().getKnots(), is(closeTo(10.05, EPSILON)));

        SpeedWithConfidence<Void> estimatedSpeed3 = miner.estimateBoatSpeed(mockedBoatClass, new KnotSpeedImpl(15),
                new DegreeBearingImpl(42.8));
        assertThat(estimatedSpeed3, is(notNullValue()));
        assertThat(estimatedSpeed3.getObject().getKnots(), is(closeTo(10, EPSILON)));
    }

    private GPSFixMoving createMockedFix(int hour, int minute, double lat, double lng, double bearingRaw, double speed) {
        GPSFixMoving fix = mock(GPSFixMoving.class);
        when(fix.getPosition()).thenReturn(new DegreePosition(lat, lng));
        Calendar cal = Calendar.getInstance();
        cal.set(2014, 4, 3, hour, minute);
        TimePoint fixTimePoint = new MillisecondsTimePoint(cal.getTime());
        when(fix.getTimePoint()).thenReturn(fixTimePoint);
        Bearing bearing = new DegreeBearingImpl(bearingRaw);
        SpeedWithBearing speedWithBearing = new KnotSpeedWithBearingImpl(speed, bearing);
        when(fix.getSpeed()).thenReturn(speedWithBearing);
        return fix;
    }

    private TrackedRace createMockedTrackedRace(Map<Competitor, Set<GPSFixMoving>> fixesPerCompetitor,
            BoatClass mockedBoatClass) {
        TrackedRace trackedRace = mock(TrackedRace.class);

        for (Entry<Competitor, Set<GPSFixMoving>> competitorEntry : fixesPerCompetitor.entrySet()) {
            Competitor competitor = competitorEntry.getKey();
            DynamicGPSFixTrack<Competitor, GPSFixMoving> track = new DynamicGPSFixMovingTrackImpl<Competitor>(
                    competitor, MILLISECONDS_OVER_WHICH_TO_AVERAGE_SPEED);
            when(trackedRace.getTrack(competitor)).thenReturn(track);
            for (GPSFixMoving fix : competitorEntry.getValue()) {
                track.add(fix);
            }
        }
        Calendar cal = Calendar.getInstance();
        cal.set(2014, 4, 3, 12, 00);
        TimePoint startOfRace = new MillisecondsTimePoint(cal.getTime());

        cal.set(2014, 4, 3, 15, 00);
        TimePoint endOfRace = new MillisecondsTimePoint(cal.getTime());
        Waypoint finishWaypoint = mock(Waypoint.class);
        createWayPoint(fixesPerCompetitor, trackedRace, endOfRace, finishWaypoint);
        Waypoint startWaypoint = mock(Waypoint.class);
        createWayPoint(fixesPerCompetitor, trackedRace, startOfRace, startWaypoint);
        RaceDefinition mockedRaceDefinition = createMockedRaceDefinition(startWaypoint, finishWaypoint);
        when(mockedRaceDefinition.getBoatClass()).thenReturn(mockedBoatClass);
        when(trackedRace.getRace()).thenReturn(mockedRaceDefinition);

        for (Competitor competitor : fixesPerCompetitor.keySet()) {
            MarkPassing markpassing = createMockedStartMarkPassing();
            when(trackedRace.getMarkPassing(eq(competitor), eq(startWaypoint))).thenReturn(markpassing);
        }
        TrackedRaceStatus status = mock(TrackedRaceStatus.class);
        when(status.getStatus()).thenReturn(TrackedRaceStatusEnum.FINISHED);
        when(trackedRace.getStatus()).thenReturn(status);


        when(trackedRace.getStartOfRace()).thenReturn(startOfRace);
        when(trackedRace.getEndOfRace()).thenReturn(endOfRace);
        
        Bearing windBearing = new DegreeBearingImpl(180);
        SpeedWithBearing windSpeed = new KnotSpeedWithBearingImpl(15, windBearing);
        
        Wind wind = new WindImpl(new DegreePosition(54.431952, 10.186767), startOfRace, windSpeed);
        WindWithConfidence<Pair<Position, TimePoint>> windWithConfidence = new WindWithConfidenceImpl<>(wind, 0.5, null, false);
        // Always return same wind
        when(trackedRace.getWind(any(Position.class), any(TimePoint.class))).thenReturn(wind);
        when(trackedRace.getWind(any(Position.class), any(TimePoint.class), any())).thenReturn(wind);
        when(trackedRace.getWindWithConfidence(any(Position.class), any(TimePoint.class), any())).thenReturn(windWithConfidence);

        return trackedRace;
    }

    private void createWayPoint(Map<Competitor, Set<GPSFixMoving>> fixesPerCompetitor, TrackedRace trackedRace,
            TimePoint endOfRace, Waypoint waypoint) {
        List<MarkPassing> markPassings = new ArrayList<MarkPassing>();
        Iterator<Competitor> competitorIterator = fixesPerCompetitor.keySet().iterator();
        while (competitorIterator.hasNext()) {
            MarkPassing markPassing = mock(MarkPassing.class);
            when(markPassing.getTimePoint()).thenReturn(endOfRace);
            Competitor competitor = competitorIterator.next();
            when(markPassing.getCompetitor()).thenReturn(competitor);
            when(trackedRace.getMarkPassing(competitor, waypoint)).thenReturn(markPassing);
            markPassings.add(markPassing);
        }
        when(trackedRace.getMarkPassingsInOrder(waypoint)).thenReturn(markPassings);
    }

    private MarkPassing createMockedStartMarkPassing() {
        Calendar cal = Calendar.getInstance();
        cal.set(2014, 4, 3, 12, 15);
        TimePoint startOfRaceForCompetitor = new MillisecondsTimePoint(cal.getTime());

        MarkPassing passing = mock(MarkPassing.class);
        when(passing.getTimePoint()).thenReturn(startOfRaceForCompetitor);
        return passing;
    }

    private RaceDefinition createMockedRaceDefinition(Waypoint startWaypoint, Waypoint finishWaypoint) {
        RaceDefinition raceDefinition = mock(RaceDefinition.class);
        Course mockedCourse = createMockedCourse();
        when(mockedCourse.getLastWaypoint()).thenReturn(finishWaypoint);
        when(mockedCourse.getFirstWaypoint()).thenReturn(startWaypoint);
        when(raceDefinition.getCourse()).thenReturn(mockedCourse);
        BoatClass mockedBoatClass = mock(BoatClass.class);
        when(mockedBoatClass.getManeuverDegreeAngleThreshold()).thenReturn(20.0);
        when(raceDefinition.getBoatClass()).thenReturn(mockedBoatClass);
        return raceDefinition;
    }

    private Course createMockedCourse() {
        Course course = mock(Course.class);
        return course;
    }

}
