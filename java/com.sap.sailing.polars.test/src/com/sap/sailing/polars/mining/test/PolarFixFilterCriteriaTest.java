package com.sap.sailing.polars.mining.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.NavigableSet;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.polars.mining.PolarFixFilterCriteria;
import com.sap.sse.common.TimePoint;

public class PolarFixFilterCriteriaTest {
    
    @Test
    public void testLeadingCompetitorsFilterForReplayRaces() {
        TrackedRace mockedTrackedRace = mock(TrackedRace.class);
        when(mockedTrackedRace.isLive(any(TimePoint.class))).thenReturn(false);
        RaceDefinition mockedRaceDefinition = mock(RaceDefinition.class);
        when(mockedTrackedRace.getRace()).thenReturn(mockedRaceDefinition);
        Course mockedCourse = mock(Course.class);
        when(mockedRaceDefinition.getCourse()).thenReturn(mockedCourse);
        Waypoint mockedFinish = mock(Waypoint.class);
        when(mockedCourse.getLastWaypoint()).thenReturn(mockedFinish);
        
        List<MarkPassing> markPassings = new ArrayList<>();
        List<Competitor> competitors = new ArrayList<>();
        for(int i = 0; i < 31; i++) {
            Competitor mockedCompetitor = mock(Competitor.class);
            competitors.add(mockedCompetitor);
            MarkPassing mockedMarkPassing = mock(MarkPassing.class);
            when(mockedMarkPassing.getCompetitor()).thenReturn(mockedCompetitor);
            markPassings.add(mockedMarkPassing);
        }
        
        when(mockedTrackedRace.getMarkPassingsInOrder(mockedFinish)).thenReturn(markPassings);
        when(mockedRaceDefinition.getCompetitors()).thenReturn(competitors);
        
        //If pct is 0, only winner should be included
        assertTrue(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(0), 0));
        for(int i = 1; i < 31; i++) {
            assertFalse(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0));
        }
        
        //If pct is 0.1, only 3 should be included (floor)
        for (int i = 0; i < 3; i++) {
            assertTrue(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0.1));
        }
        for(int i = 3; i < 31; i++) {
            assertFalse(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0.1));
        }
        
        //If pct is 0.15, only 5 should be included (ceil)
        for (int i = 0; i < 5; i++) {
            assertTrue(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0.15));
        }
        for(int i = 5; i < 31; i++) {
            assertFalse(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0.15));
        }
        
        //If pct is 1, all 31 should be included
        for (int i = 0; i < 31; i++) {
            assertTrue(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 1));
        }
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testLeadingCompetitorsFilterForLiveRaces() {
        TrackedRace mockedTrackedRace = mock(TrackedRace.class);
        when(mockedTrackedRace.isLive(any(TimePoint.class))).thenReturn(true);
        RaceDefinition mockedRaceDefinition = mock(RaceDefinition.class);
        when(mockedTrackedRace.getRace()).thenReturn(mockedRaceDefinition);
        MarkPassing mockedLastMarkPassing = mock(MarkPassing.class);
        NavigableSet<MarkPassing> mockedCompetitorsMarkPassings = (NavigableSet<MarkPassing>) mock(NavigableSet.class);
        when(mockedCompetitorsMarkPassings.last()).thenReturn(mockedLastMarkPassing);
        when(mockedTrackedRace.getMarkPassings(any(Competitor.class))).thenReturn(mockedCompetitorsMarkPassings);
        
        Waypoint mockedLastWayPoint = mock(Waypoint.class);
        when(mockedLastMarkPassing.getWaypoint()).thenReturn(mockedLastWayPoint);
        
        List<MarkPassing> markPassings = new ArrayList<>();
        List<Competitor> competitors = new ArrayList<>();
        for(int i = 0; i < 31; i++) {
            Competitor mockedCompetitor = mock(Competitor.class);
            competitors.add(mockedCompetitor);
            MarkPassing mockedMarkPassing = mock(MarkPassing.class);
            when(mockedMarkPassing.getCompetitor()).thenReturn(mockedCompetitor);
            markPassings.add(mockedMarkPassing);
        }
        
        when(mockedTrackedRace.getMarkPassingsInOrder(mockedLastWayPoint)).thenReturn(markPassings);
        when(mockedRaceDefinition.getCompetitors()).thenReturn(competitors);
        
        //If pct is 0, only winner should be included
        assertTrue(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(0), 0));
        for(int i = 1; i < 31; i++) {
            assertFalse(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0));
        }
        
        //If pct is 0.1, only 3 should be included (floor)
        for (int i = 0; i < 3; i++) {
            assertTrue(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0.1));
        }
        for(int i = 3; i < 31; i++) {
            assertFalse(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0.1));
        }
        
        //If pct is 0.15, only 5 should be included (ceil)
        for (int i = 0; i < 5; i++) {
            assertTrue(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0.15));
        }
        for(int i = 5; i < 31; i++) {
            assertFalse(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 0.15));
        }
        
        //If pct is 1, all 31 should be included
        for (int i = 0; i < 31; i++) {
            assertTrue(PolarFixFilterCriteria.isInLeadingCompetitors(mockedTrackedRace, competitors.get(i), 1));
        }
    }

}
