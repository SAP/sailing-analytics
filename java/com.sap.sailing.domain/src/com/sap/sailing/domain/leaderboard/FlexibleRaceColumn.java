package com.sap.sailing.domain.leaderboard;

import com.sap.sailing.domain.base.RaceColumn;

public interface FlexibleRaceColumn extends RaceColumn {
    void setName(String newName);
    void setIsMedalRace(boolean isMedalRace);
}
