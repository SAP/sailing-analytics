package com.sap.sailing.gwt.ui.shared;

import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Holds data about one competitor and all races represented by the owning {@link LeaderboardDTO leaderboard}.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class LeaderboardRowDTO implements IsSerializable {
    public CompetitorDTO competitor;
    public Map<String, LeaderboardEntryDTO> fieldsByRaceName;
    public Integer carriedPoints;
    
    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((carriedPoints == null) ? 0 : carriedPoints.hashCode());
        result = prime * result + ((competitor == null) ? 0 : competitor.hashCode());
        result = prime * result + ((fieldsByRaceName == null) ? 0 : fieldsByRaceName.hashCode());
        return result;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        LeaderboardRowDTO other = (LeaderboardRowDTO) obj;
        if (carriedPoints == null) {
            if (other.carriedPoints != null)
                return false;
        } else if (!carriedPoints.equals(other.carriedPoints))
            return false;
        if (competitor == null) {
            if (other.competitor != null)
                return false;
        } else if (!competitor.equals(other.competitor))
            return false;
        if (fieldsByRaceName == null) {
            if (other.fieldsByRaceName != null)
                return false;
        } else if (!fieldsByRaceName.equals(other.fieldsByRaceName))
            return false;
        return true;
    }
}
