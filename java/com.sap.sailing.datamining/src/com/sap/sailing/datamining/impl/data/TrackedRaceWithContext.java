package com.sap.sailing.datamining.impl.data;

import java.util.Calendar;

import com.sap.sailing.datamining.data.HasLeaderboardContext;
import com.sap.sailing.datamining.data.HasTrackedRaceContext;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.tracking.GPSFix;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.GPSFixTrack;
import com.sap.sailing.domain.tracking.LineDetails;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class TrackedRaceWithContext implements HasTrackedRaceContext {

    private final HasLeaderboardContext leaderboardContext;
    private final Regatta regatta;
    private final Fleet fleet;
    private final TrackedRace trackedRace;
    
    private Integer year;
    private boolean yearHasBeenInitialized;

    public TrackedRaceWithContext(HasLeaderboardContext leaderboardContext, Regatta regatta, Fleet fleet, TrackedRace trackedRace) {
        this.leaderboardContext = leaderboardContext;
        this.regatta = regatta;
        this.fleet = fleet;
        this.trackedRace = trackedRace;
    }
    
    @Override
    public HasLeaderboardContext getLeaderboardContext() {
        return leaderboardContext;
    }
    
    @Override
    public Regatta getRegatta() {
        return regatta;
    }
    
    @Override
    public CourseArea getCourseArea() {
        return getRegatta().getDefaultCourseArea();
    }
    
    @Override
    public BoatClass getBoatClass() {
        return getRegatta().getBoatClass();
    }
    
    @Override
    public TrackedRace getTrackedRace() {
        return trackedRace;
    }

    @Override
    public Fleet getFleet() {
        return fleet;
    }
    
    @Override
    public RaceDefinition getRace() {
        return getTrackedRace().getRace();
    }

    @Override
    public Integer getYear() {
        if (!yearHasBeenInitialized) {
            year = calculateYear();
            yearHasBeenInitialized = true;
        }
        return year;
    }

    private Integer calculateYear() {
        TimePoint startOfRace = getTrackedRace().getStartOfRace();
        TimePoint time = startOfRace != null ? startOfRace : getTrackedRace().getStartOfTracking();
        if (time == null) {
            year = 0;
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(time.asDate());
        return calendar.get(Calendar.YEAR);
    }
    
    @Override
    public NauticalSide getAdvantageousEndOfLine() {
        LineDetails startLine = getTrackedRace().getStartLine(getTrackedRace().getStartOfRace());
        return startLine.getAdvantageousSideWhileApproachingLine();
    }

    @Override
    public Boolean isTracked() {
        return getTrackedRace().hasStarted(MillisecondsTimePoint.now());
    }

    @Override
    public int getNumberOfCompetitorFixes() {
        int number = 0;
        for (Competitor competitor : getRace().getCompetitors()) {
            GPSFixTrack<Competitor, GPSFixMoving> track = getTrackedRace().getTrack(competitor);
            track.lockForRead();
            try {
                number += Util.size(track.getRawFixes());
            } finally {
                track.unlockAfterRead();
            }
        }
        return number;
    }

    @Override
    public int getNumberOfMarkFixes() {
        int number = 0;
        for (Mark mark : getTrackedRace().getMarks()) {
            GPSFixTrack<Mark, GPSFix> track = getTrackedRace().getTrack(mark);
            track.lockForRead();
            try {
                number += Util.size(track.getRawFixes());
            } finally {
                track.unlockAfterRead();
            }
        }
        return number;
    }
    
    // Convenience methods for race dependent calculation to avoid code duplication
    public Double getRelativeScoreForCompetitor(Competitor competitor) {
        Double rankAtFinish = getRankAtFinishForCompetitor(competitor);
        if (rankAtFinish == null) {
            return null;
        }
        return rankAtFinish / Util.size(getTrackedRace().getRace().getCompetitors());
    }
    
    @Override
    public Double getRankAtFinishForCompetitor(Competitor competitor) {
        int rank = getTrackedRace().getRank(competitor, getTrackedRace().getEndOfTracking());
        return rank == 0 ? null : Double.valueOf(rank);
    }

}