package com.sap.sailing.gwt.ui.shared;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;

/**
 * Captures a collection of score corrections to apply to a leaderboard. The leaderboard is identified by name,
 * and so are the race columns. The competitors are identified by their {@link CompetitorDTO#idAsString ID}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class BulkScoreCorrectionDTO implements IsSerializable {
    private String leaderboardName;
    private Map<String, Map<String, MaxPointsReason>> maxPointsUpdatesForRaceColumnByCompetitorIdAsString;
    private Map<String, Map<String, Double>> scoreUpdatesForRaceColumnByCompetitorIdAsString;

    public BulkScoreCorrectionDTO() {}
    
    public BulkScoreCorrectionDTO(String leaderboardName) {
        this.leaderboardName = leaderboardName;
        maxPointsUpdatesForRaceColumnByCompetitorIdAsString = new HashMap<String, Map<String,MaxPointsReason>>();
        scoreUpdatesForRaceColumnByCompetitorIdAsString = new HashMap<String, Map<String, Double>>();
    }
    
    public void addScoreUpdate(CompetitorDTO competitor, RaceColumnDTO raceColumn, double newScore) {
        Map<String, Double> map = scoreUpdatesForRaceColumnByCompetitorIdAsString.get(competitor.idAsString);
        if (map == null) {
            map = new HashMap<String, Double>();
            scoreUpdatesForRaceColumnByCompetitorIdAsString.put(competitor.idAsString, map);
        }
        map.put(raceColumn.name, newScore);
    }

    public void addMaxPointsReasonUpdate(CompetitorDTO competitor, RaceColumnDTO raceColumn, MaxPointsReason newReason) {
        Map<String, MaxPointsReason> map = maxPointsUpdatesForRaceColumnByCompetitorIdAsString.get(competitor.idAsString);
        if (map == null) {
            map = new HashMap<String, MaxPointsReason>();
            maxPointsUpdatesForRaceColumnByCompetitorIdAsString.put(competitor.idAsString, map);
        }
        map.put(raceColumn.name, newReason);
    }
    
    public Map<String, Map<String, MaxPointsReason>> getMaxPointsUpdatesForRaceColumnByCompetitorIdAsString() {
        return maxPointsUpdatesForRaceColumnByCompetitorIdAsString;
    }

    public Map<String, Map<String, Double>> getScoreUpdatesForRaceColumnByCompetitorIdAsString() {
        return scoreUpdatesForRaceColumnByCompetitorIdAsString;
    }

    public String getLeaderboardName() {
        return leaderboardName;
    }
}