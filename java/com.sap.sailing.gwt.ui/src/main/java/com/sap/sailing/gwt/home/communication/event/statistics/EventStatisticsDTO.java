package com.sap.sailing.gwt.home.communication.event.statistics;

import com.google.gwt.core.shared.GwtIncompatible;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.gwt.home.communication.event.SimpleCompetitorDTO;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Speed;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util.Triple;
import com.sap.sse.gwt.shared.DTO;

public class EventStatisticsDTO implements DTO {
    private int regattasFoughtCount;
    private int competitorsCount;
    private int racesRunCount;
    private int trackedRacesCount;
    private long numberOfGPSFixes;
    private long numberOfWindFixes;
    private SimpleCompetitorDTO fastestCompetitor;
    private Double fastestCompetitorSpeedInKnots;
    private Distance totalDistanceTraveled;

    @SuppressWarnings("unused")
    private EventStatisticsDTO() {
    }

    @GwtIncompatible
    public EventStatisticsDTO(int regattasFoughtCount, int competitorsCount, int racesRunCount, int trackedRacesCount,
            long numberOfGPSFixes, long numberOfWindFixes,
            Triple<Competitor, Speed, TimePoint> maxSpeed, Distance totalDistanceTraveled) {
        super();
        this.regattasFoughtCount = regattasFoughtCount;
        this.competitorsCount = competitorsCount;
        this.racesRunCount = racesRunCount;
        this.trackedRacesCount = trackedRacesCount;
        this.numberOfGPSFixes = numberOfGPSFixes;
        this.numberOfWindFixes = numberOfWindFixes;
        if (maxSpeed != null) {
            this.fastestCompetitor = new SimpleCompetitorDTO(maxSpeed.getA());
            this.fastestCompetitorSpeedInKnots = maxSpeed.getB().getKnots();
        }
        this.totalDistanceTraveled = totalDistanceTraveled;
    }

    public int getRegattasFoughtCount() {
        return regattasFoughtCount;
    }

    public void setRegattasFoughtCount(int regattasFoughtCount) {
        this.regattasFoughtCount = regattasFoughtCount;
    }

    public int getCompetitorsCount() {
        return competitorsCount;
    }

    public void setCompetitorsCount(int competitorsCount) {
        this.competitorsCount = competitorsCount;
    }

    public int getRacesRunCount() {
        return racesRunCount;
    }

    public void setRacesRunCount(int racesRunCount) {
        this.racesRunCount = racesRunCount;
    }

    public int getTrackedRacesCount() {
        return trackedRacesCount;
    }

    public void setTrackedRacesCount(int trackedRacesCount) {
        this.trackedRacesCount = trackedRacesCount;
    }

    public long getNumberOfGPSFixes() {
        return numberOfGPSFixes;
    }

    public long getNumberOfWindFixes() {
        return numberOfWindFixes;
    }
    
    public SimpleCompetitorDTO getFastestCompetitor() {
        return fastestCompetitor;
    }
    
    public Double getFastestCompetitorSpeedInKnots() {
        return fastestCompetitorSpeedInKnots;
    }

    public Distance getTotalDistanceTraveled() {
        return totalDistanceTraveled;
    }
}
