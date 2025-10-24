package com.sap.sailing.gwt.home.communication.eventview;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;

import com.sap.sailing.gwt.home.communication.event.EventSeriesReferenceDTO;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Util;
import com.sap.sse.gwt.shared.DTO;

public class RegattaMetadataDTO extends RegattaReferenceDTO implements HasRegattaMetadata {
    private int raceCount;
    private int competitorsCount;
    private String boatClass;
    private ArrayList<String> leaderboardGroupNames;
    private String defaultCourseAreaName;
    private ArrayList<String> courseAreaIdsAsStrings;
    private Date startDate;
    private Date endDate;
    private RegattaState state;
    private boolean flexibleLeaderboard;
    private RaceDataInfo raceDataInfo;
    private Distance buoyZoneRadius;
    private EventSeriesReferenceDTO seriesReference;
    
    public RegattaMetadataDTO() {
    }

    @Override
    public int getRaceCount() {
        return raceCount;
    }

    public void setRaceCount(int raceCount) {
        this.raceCount = raceCount;
    }

    @Override
    public int getCompetitorsCount() {
        return competitorsCount;
    }

    public void setCompetitorsCount(int competitorsCount) {
        this.competitorsCount = competitorsCount;
    }

    @Override
    public String getBoatClass() {
        return boatClass;
    }

    public void setBoatClass(String boatClass) {
        this.boatClass = boatClass;
    }

    @Override
    public Iterable<String> getLeaderboardGroupNames() {
        if (leaderboardGroupNames == null) {
            leaderboardGroupNames = new ArrayList<>();
        }
        return Collections.unmodifiableList(leaderboardGroupNames);
    }

    public void addLeaderboardGroupName(String leaderboardGroupName) {
        if (leaderboardGroupNames == null) {
            leaderboardGroupNames = new ArrayList<>();
        }
        leaderboardGroupNames.add(leaderboardGroupName);
    }

    @Override
    public Date getStartDate() {
        return startDate;
    }

    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    @Override
    public Date getEndDate() {
        return endDate;
    }

    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @Override
    public RegattaState getState() {
        return state;
    }

    public void setState(RegattaState state) {
        this.state = state;
    }

    public String getDefaultCourseAreaName() {
        return defaultCourseAreaName;
    }

    public void setDefaultCourseAreaName(String defaultCourseAreaName) {
        this.defaultCourseAreaName = defaultCourseAreaName;
    }

    public boolean isFlexibleLeaderboard() {
        return flexibleLeaderboard;
    }

    public void setFlexibleLeaderboard(boolean flexibleLeaderboard) {
        this.flexibleLeaderboard = flexibleLeaderboard;
    }

    public Iterable<String> getCourseAreaIdsAsStrings() {
        return courseAreaIdsAsStrings;
    }

    public void setCourseAreaIdsAsStrings(Iterable<String> courseAreaIdsAsStrings) {
        this.courseAreaIdsAsStrings = new ArrayList<>();
        Util.addAll(courseAreaIdsAsStrings, this.courseAreaIdsAsStrings);
    }
    
    public RaceDataInfo getRaceDataInfo() {
        return raceDataInfo;
    }
    
    public void setRaceDataInfo(RaceDataInfo raceDataInfo) {
        this.raceDataInfo = raceDataInfo;
    }

    public Distance getBuoyZoneRadius() {
        return buoyZoneRadius;
    }

    public void setBuoyZoneRadius(Distance buoyZoneRadius) {
        this.buoyZoneRadius = buoyZoneRadius;
    }
    
    public EventSeriesReferenceDTO getSeriesReference() {
        return seriesReference;
    }
    public void setSeriesReference(EventSeriesReferenceDTO seriesReference) {
        this.seriesReference = seriesReference;
    }

    /**
     * Holder class for flags, which inform about GPS, wind, video or audio data availability. 
     */
    public static class RaceDataInfo implements DTO {
        private boolean hasGPSData, hasWindData, hasVideoData, hasAudioData;
        
        protected RaceDataInfo() {
        }
        
        public RaceDataInfo(boolean hasGPSData, boolean hasWindData, boolean hasVideoData, boolean hasAudioData) {
            this.hasGPSData = hasGPSData;
            this.hasWindData = hasWindData;
            this.hasVideoData = hasVideoData;
            this.hasAudioData = hasAudioData;
        }

        public boolean hasData() {
            return hasGPSData || hasWindData || hasVideoData || hasAudioData;
        }

        public boolean hasGPSData() {
            return hasGPSData;
        }

        public boolean hasWindData() {
            return hasWindData;
        }

        public boolean hasVideoData() {
            return hasVideoData;
        }

        public boolean hasAudioData() {
            return hasAudioData;
        }
    }
}
