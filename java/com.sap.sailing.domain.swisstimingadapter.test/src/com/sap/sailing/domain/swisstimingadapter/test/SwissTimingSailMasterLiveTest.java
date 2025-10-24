package com.sap.sailing.domain.swisstimingadapter.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.net.UnknownHostException;
import java.text.ParseException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.common.TrackedRaceStatusEnum;
import com.sap.sailing.domain.swisstimingadapter.Competitor;
import com.sap.sailing.domain.swisstimingadapter.Course;
import com.sap.sailing.domain.swisstimingadapter.Fix;
import com.sap.sailing.domain.swisstimingadapter.Mark;
import com.sap.sailing.domain.swisstimingadapter.Race;
import com.sap.sailing.domain.swisstimingadapter.RaceStatus;
import com.sap.sailing.domain.swisstimingadapter.RacingStatus;
import com.sap.sailing.domain.swisstimingadapter.SailMasterConnector;
import com.sap.sailing.domain.swisstimingadapter.SailMasterListener;
import com.sap.sailing.domain.swisstimingadapter.StartList;
import com.sap.sailing.domain.swisstimingadapter.SwissTimingFactory;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;

@Disabled("This test doesn't work as long as the server doesn't play an actual race")
public class SwissTimingSailMasterLiveTest implements SailMasterListener {
    private int rpdCounter;
    private SailMasterConnector connector;

    @BeforeEach
    public void connect() throws InterruptedException, ParseException {
        connector = SwissTimingFactory.INSTANCE.getOrCreateSailMasterConnector("gps.sportresult.com", 40300, "W4702", /* raceDataUrl */ null, "R2", "Women 470 Race 2", null /* boat class*/, /* SwissTimingRaceTracker */ null);
    }
    
    @AfterEach
    public void stopConnector() throws IOException {
        connector.stop();
    }
    
    @Test
    public void testReceiveRPDEvents() throws UnknownHostException, IOException, InterruptedException {
        connector.addSailMasterListener(this);
        connector.disableRacePositionData();
        connector.enableRacePositionData();
        synchronized (this) {
            wait(10000); // receive 10s worth of events and expect to have received some data
        }
        connector.removeSailMasterListener(this);
        assertTrue(rpdCounter > 5);
    }
    
    @Test
    public void testGetRaces() throws UnknownHostException, IOException, InterruptedException {
        Race race = connector.getRace();
        assertEquals("Women 470 Race 2", race.getDescription());
        assertEquals("W4702", race.getRaceID());
    }

    @Test
    public void testGetCourse() throws UnknownHostException, IOException, InterruptedException {
        Race race = connector.getRace();
        Course course = connector.getCourse(race.getRaceID());
        assertNotNull(course);
        Iterable<Mark> marks = course.getMarks();
        assertEquals(7, Util.size(marks));
    }
    
    @Test
    public void testGetStartList() throws UnknownHostException, IOException, InterruptedException {
        Race race = connector.getRace();
        StartList startList = connector.getStartList(race.getRaceID());
        Iterable<Competitor> competitors = startList.getCompetitors();
        assertEquals(race.getRaceID(), startList.getRaceID());
        assertEquals(46, Util.size(competitors));
    }
    
    @Test
    public void testGetStartTime() throws UnknownHostException, IOException, ParseException, InterruptedException {
        TimePoint startTime = connector.getStartTime();
        assertNotNull(startTime);
    }
    
    @Test
    public void testGetClockAtMark() throws UnknownHostException, IOException, InterruptedException, ParseException {
        Race race = connector.getRace();
        List<com.sap.sse.common.Util.Triple<Integer, TimePoint, String>> clockAtMark = connector.getClockAtMark(race.getRaceID());
        assertFalse(clockAtMark.isEmpty());
    }
    
    @Test
    public void testGetCurrentBoatSpeed() throws UnknownHostException, IOException, InterruptedException {
        Race race = connector.getRace();
        for (Competitor competitor : connector.getStartList(race.getRaceID()).getCompetitors()) {
            Speed currentBoatSpeed = connector.getCurrentBoatSpeed(race.getRaceID(), competitor.getBoatID());
            assertNotNull(currentBoatSpeed);
        }
    }
    
    @Test
    public void testGetDistanceBetweenBoats() throws UnknownHostException, IOException, InterruptedException {
        Race race = connector.getRace();
        Iterator<Competitor> competitorIter = connector.getStartList(race.getRaceID()).getCompetitors().iterator();
        competitorIter.next();
        competitorIter.next();
        Competitor competitor1 = competitorIter.next();
        for (Competitor competitor2 : connector.getStartList(race.getRaceID()).getCompetitors()) {
            System.out.print("d");
            Distance distance = connector.getDistanceBetweenBoats(race.getRaceID(), competitor1.getBoatID(),
                    competitor2.getBoatID());
            Distance reverseDistance = connector.getDistanceBetweenBoats(race.getRaceID(), competitor2.getBoatID(),
                    competitor1.getBoatID());
            if (distance != null && reverseDistance != null) {
                assertEquals(distance.getMeters(), reverseDistance.getMeters(), 5);
            }
        }
    }
    
    @Test
    public void testGetDistanceToMark() throws UnknownHostException, IOException, InterruptedException {
        Race race = connector.getRace();
        Course course = connector.getCourse(race.getRaceID());
        for (Competitor competitor : connector.getStartList(race.getRaceID()).getCompetitors()) {
            for (int i = 0; i < Util.size(course.getMarks()); i++) {
                Distance distance = connector.getDistanceToMark(race.getRaceID(), i, competitor.getBoatID());
                System.out.print("d");
                if (distance != null) {
                    assertTrue(distance.getMeters() > 0);
                }
            }
        }
    }
    
    @Test
    public void testGetMarkPassingTimes() throws UnknownHostException, IOException, InterruptedException {
        Race race = connector.getRace();
        Course course = connector.getCourse(race.getRaceID());
        int numberOfMarks = Util.size(course.getMarks());
        Iterable<Competitor> competitors = connector.getStartList(race.getRaceID()).getCompetitors();
        for (Competitor competitor : competitors) {
            Map<Integer, com.sap.sse.common.Util.Pair<Integer, Long>> markPassingTimes = connector.getMarkPassingTimesInMillisecondsSinceRaceStart(race.getRaceID(), competitor.getBoatID());
            for (Integer markIndex : markPassingTimes.keySet()) {
                com.sap.sse.common.Util.Pair<Integer, Long> rankAndTime = markPassingTimes.get(markIndex);
                for (int i=0; i<numberOfMarks; i++) {
                    if (i != markIndex && markPassingTimes.containsKey(numberOfMarks)) {
                        if (markPassingTimes.get(i).getB() != null && rankAndTime.getB() != null) {
                            // previous marks must have been passed before subsequent marks
                            assertTrue(markPassingTimes.get(i).getB() < rankAndTime.getB());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void receivedRacePositionData(String raceID, RaceStatus raceStatus, RacingStatus racingStatus, TimePoint timePoint,
            TimePoint startTime, Long millisecondsSinceRaceStart, Integer nextMarkIndexForLeader,
            Distance distanceToNextMarkForLeader, Collection<Fix> fixes) {
        System.out.print(".");
        rpdCounter++;
    }

    @Override
    public void receivedTimingData(String raceID, String boatID,
            List<com.sap.sse.common.Util.Triple<Integer, Integer, Long>> markIndicesRanksAndTimesSinceStartInMilliseconds) {
    }

    @Override
    public void receivedClockAtMark(String raceID,
            List<com.sap.sse.common.Util.Triple<Integer, TimePoint, String>> markIndicesTimePointsAndBoatIDs) {
    }

    @Override
    public void receivedStartList(String raceID, StartList startList) {
    }

    @Override
    public void receivedCourseConfiguration(String raceID, Course course) {
    }

    @Override
    public void receivedAvailableRaces(Iterable<Race> races) {
    }

    @Override
    public void storedDataProgress(String raceID, double progress, TrackedRaceStatusEnum statusAfterLoadingComplete) {
    }

    @Override
    public void receivedWindData(String raceID, int zeroBasedMarkIndex, double windDirectionTrueDegrees,
            double windSpeedInKnots) {
    }
}
