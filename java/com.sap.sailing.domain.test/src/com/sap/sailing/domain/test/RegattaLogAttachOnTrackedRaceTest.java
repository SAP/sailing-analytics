package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.regatta.RegattaLog;
import com.sap.sailing.domain.abstractlog.regatta.impl.RegattaLogImpl;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.racelog.impl.EmptyRaceLogStore;
import com.sap.sailing.domain.regattalike.RegattaLikeIdentifier;
import com.sap.sailing.domain.regattalog.RegattaLogStore;
import com.sap.sailing.domain.test.mock.MockedTrackedRace;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Util;

public class RegattaLogAttachOnTrackedRaceTest {
    
    private class MyMockedRegattaLogStore implements RegattaLogStore {
        private Map<RegattaLikeIdentifier, RegattaLog> regattaLogs = new HashMap<>();
        @Override
        public RegattaLog getRegattaLog(RegattaLikeIdentifier regattaLikeId, boolean ignoreCache) {
            if(!regattaLogs.containsKey(regattaLikeId)) {
                regattaLogs.put(regattaLikeId, new RegattaLogImpl(regattaLikeId));
            }
            return regattaLogs.get(regattaLikeId);
        }

        @Override
        public void removeRegattaLog(RegattaLikeIdentifier regattaLikeId) {
        }

        @Override
        public void addImportedRegattaLog(RegattaLog regattaLog, RegattaLikeIdentifier identifier) {
        }

        @Override
        public void clear() {
        }
    }
    
    private class MyMockedTrackedRace extends MockedTrackedRace {
        private static final long serialVersionUID = -5450144964435682919L;
        
        private RegattaLog regattaLog;
        @Override
        public void attachRegattaLog(RegattaLog regattaLog) {
            this.regattaLog = regattaLog;
        }
        
        @Override
        public Iterable<RegattaLog> getAttachedRegattaLogs() {
            return Collections.singletonList(regattaLog);
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
        leaderboard = new FlexibleLeaderboardImpl(EmptyRaceLogStore.INSTANCE, new MyMockedRegattaLogStore(), "", null, new LowPoint(),
                /* no course area */ (CourseArea) null);
    }
    
    @Test
    public void testAttachRegattaLogIsCalledWhenTrackedRaceIsSetToARaceColumn() {
        RaceColumn column = leaderboard.addRaceColumn("R1", false);
        TrackedRace trackedRace = new MyMockedTrackedRace();
        Fleet defaultFleet = Util.get(column.getFleets(), 0);
        column.setTrackedRace(defaultFleet, trackedRace);
        assertEquals(1, Util.size(trackedRace.getAttachedRegattaLogs()));
        assertSame(leaderboard.getRegattaLog(), Util.get(trackedRace.getAttachedRegattaLogs(), 0));
    }

}
