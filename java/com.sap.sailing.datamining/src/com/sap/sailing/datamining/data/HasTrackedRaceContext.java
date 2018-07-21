package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;

public interface HasTrackedRaceContext {
    
    @Connector(scanForStatistics=false)
    public HasLeaderboardContext getLeaderboardContext();
    
    public TrackedRace getTrackedRace();
    
    @Connector(messageKey="Regatta", ordinal=0)
    public Regatta getRegatta();
    
    @Connector(messageKey="CourseArea", ordinal=3)
    public CourseArea getCourseArea();

    @Connector(messageKey="RaceColumn", ordinal=4)
    public RaceColumn getRaceColumn();

    @Connector(messageKey="Fleet", ordinal=5)
    public Fleet getFleet();
    
    @Connector(messageKey="Race", ordinal=6)
    public RaceDefinition getRace();
    
    @Dimension(messageKey="Year", ordinal=2)
    public Integer getYear();
    
    @Dimension(messageKey="AdvantageousEndOfLine", ordinal=7)
    public NauticalSide getAdvantageousEndOfLine();
    
    @Dimension(messageKey="MedalRace", ordinal=8)
    public Boolean isMedalRace();
    
    @Dimension(messageKey="IsTracked", ordinal=9)
    public Boolean isTracked();
    
    @Statistic(messageKey="NumberOfCompetitorFixes", resultDecimals=0, ordinal=0)
    public int getNumberOfCompetitorFixes();
    
    @Statistic(messageKey="NumberOfMarkFixes", resultDecimals=0, ordinal=1)
    public int getNumberOfMarkFixes();
    
    // Convenience methods for race dependent calculation to avoid code duplication
    public Double getRelativeScoreForCompetitor(Competitor competitor);
    
    public Double getRankAtFinishForCompetitor(Competitor competitor);
    
}