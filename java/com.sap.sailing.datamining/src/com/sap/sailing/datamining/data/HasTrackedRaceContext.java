package com.sap.sailing.datamining.data;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.LegType;
import com.sap.sailing.domain.common.NauticalSide;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Bearing;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.datamining.annotations.Connector;
import com.sap.sse.datamining.annotations.Dimension;
import com.sap.sse.datamining.annotations.Statistic;

public interface HasTrackedRaceContext {
    
    @Connector(scanForStatistics=false)
    public HasLeaderboardContext getLeaderboardContext();
    
    /**
     * May be {@code null}
     */
    public TrackedRace getTrackedRace();
    
    /**
     * May be {@code null}
     */
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
    
    @Statistic(messageKey="AdvantageOfStarboardSideOfStartLine")
    public Distance getAdvantageOfStarboardSideOfStartline();
    
    @Statistic(messageKey="TrueWindAngleOfStartLineSeenFromStarboardSide")
    public Bearing getTrueWindAngleOfStartLineFromStarboardSide();
    
    @Statistic(messageKey="StartLineLength")
    public Distance getStartLineLength();
    
    @Statistic(messageKey="FinishLineLength")
    public Distance getFinishLineLength();
    
    @Statistic(messageKey="BiasAtStartOfRace", resultDecimals=2)
    Double getStartLineBiasInMetersAtStartOfRace();
    
    @Statistic(messageKey="Bias30SecondsAfterRaceStart", resultDecimals=2)
    Double getStartLineBiasInMeters30SecondsAfterRaceStart();
    
    @Dimension(messageKey="MedalRace", ordinal=8)
    public Boolean isMedalRace();
    
    @Dimension(messageKey="IsTracked", ordinal=9)
    public Boolean isTracked();
    
    @Statistic(messageKey="RaceDuration", ordinal=0)
    public Duration getDuration();
    
    @Statistic(messageKey="NumberOfCompetitorFixes", resultDecimals=0, ordinal=1)
    public int getNumberOfCompetitorFixes();
    
    @Statistic(messageKey="NumberOfMarkFixes", resultDecimals=0, ordinal=2)
    public int getNumberOfMarkFixes();
    
    @Statistic(messageKey="NumberOfWindFixes", resultDecimals=0, ordinal=1)
    public int getNumberOfWindFixes();
    
    @Dimension(messageKey="Day")
    public String getDayAsISO();
    
    @Dimension(messageKey="TypeOfFirstLeg")
    public LegType getTypeOfFirstLeg() throws NoWindException;

    // Convenience methods for race dependent calculation to avoid code duplication
    public Double getRelativeScoreForCompetitor(Competitor competitor);
    
    public Integer getRankAtFinishForCompetitor(Competitor competitor);
}