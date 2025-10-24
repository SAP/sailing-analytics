package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.Collections;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.test.mock.MockedTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Util;

public class RaceLogAttachOnTrackedRaceTest {
    
    private class MyMockedTrackedRace extends MockedTrackedRace {
        private static final long serialVersionUID = -5450144964435682919L;
        
        private RaceLog raceLog;
        
        @Override
        public RaceLog detachRaceLog(Serializable identifier) {
            final RaceLog result = this.raceLog;
            this.raceLog = null;
            return result;
        }
        
        @Override
        public void attachRaceLog(RaceLog raceLog) {
            this.raceLog = raceLog;
        }
        
        @Override
        public RaceLog getRaceLog(Serializable identifier) {
            return raceLog;
        }
        
        @Override
        public RaceDefinition getRace() {
            final RaceDefinition result = mock(RaceDefinition.class);
            when(result.getCompetitors()).thenReturn(Collections.emptySet());
            return result;
        }
    }
    
    private FlexibleLeaderboard leaderboard;
    
    @BeforeEach
    public void setUp() {
        leaderboard = new FlexibleLeaderboardImpl("", null, new LowPoint(), null);
    }
    
    @Test
    public void testAttachDefaultFleet() {
        RaceColumn column = leaderboard.addRaceColumn("R1", false);
        TrackedRace trackedRace = new MyMockedTrackedRace();
        Fleet defaultFleet = Util.get(column.getFleets(), 0);
        column.setTrackedRace(defaultFleet, trackedRace);
        assertSame(column.getRaceLog(defaultFleet), trackedRace.getRaceLog(column.getRaceLogIdentifier(defaultFleet)));
    }
    
    @Test
    public void testAttachAndDetach() {
        RaceColumn column = leaderboard.addRaceColumn("R1", false);
        TrackedRace trackedRace = new MyMockedTrackedRace();
        Fleet fleet = Util.get(column.getFleets(), 0);
        column.setTrackedRace(fleet, trackedRace);
        column.setTrackedRace(fleet, null);
        assertNull(trackedRace.getRaceLog(column.getRaceLogIdentifier(fleet)));
    }
    
    @Test
    public void testAttachToDefaultFleet() {
        RaceColumn column = leaderboard.addRaceColumn("R1", false);
        TrackedRace trackedRace = new MyMockedTrackedRace();
        Fleet defaultFleet = leaderboard.getFleet(null);
        column.setTrackedRace(defaultFleet, trackedRace);
        assertSame(column.getRaceLog(defaultFleet), trackedRace.getRaceLog(column.getRaceLogIdentifier(defaultFleet)));
    }
    
    @Test
    public void testReattach() {
        RaceColumn column = leaderboard.addRaceColumn("R1", false);
        Fleet fleet = Util.get(column.getFleets(), 0);
        TrackedRace firstRace = new MyMockedTrackedRace();
        TrackedRace secondRace = new MyMockedTrackedRace();
        column.setTrackedRace(fleet, firstRace);
        column.setTrackedRace(fleet, secondRace);
        assertNull(firstRace.getRaceLog(column.getRaceLogIdentifier(fleet)));
        assertSame(column.getRaceLog(fleet), secondRace.getRaceLog(column.getRaceLogIdentifier(fleet)));
    }

}
