package com.sap.sailing.server.operationaltransformation;

import java.util.Date;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard.Entry;
import com.sap.sailing.domain.leaderboard.RaceColumn;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class UpdateLeaderboardMaxPointsReason extends AbstractLeaderboardColumnOperation<Pair<Integer, Integer>> {
    private static final long serialVersionUID = -492130952256848047L;
    private final String competitorIdAsString;
    private final MaxPointsReason newMaxPointsReason;
    private final Date date;
    
    public UpdateLeaderboardMaxPointsReason(String leaderboardName, String columnName, String competitorIdAsString,
            MaxPointsReason newMaxPointsReason, Date date) {
        super(leaderboardName, columnName);
        this.competitorIdAsString = competitorIdAsString;
        this.newMaxPointsReason = newMaxPointsReason;
        this.date = date;
    }

    @Override
    public RacingEventServiceOperation<?> transformClientOp(RacingEventServiceOperation<?> serverOp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public RacingEventServiceOperation<?> transformServerOp(RacingEventServiceOperation<?> clientOp) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public Pair<Integer, Integer> internalApplyTo(RacingEventService toState) throws NoWindException {
        TimePoint timePoint = new MillisecondsTimePoint(date);
        Leaderboard leaderboard = toState.getLeaderboardByName(getLeaderboardName());
        if (leaderboard != null) {
            Competitor competitor = leaderboard.getCompetitorByIdAsString(competitorIdAsString);
            if (competitor != null) {
                RaceColumn raceColumn = leaderboard.getRaceColumnByName(getColumnName());
                if (raceColumn == null) {
                    throw new IllegalArgumentException("Didn't find race "+getColumnName()+" in leaderboard "+getLeaderboardName());
                }
                leaderboard.getScoreCorrection().setMaxPointsReason(competitor, raceColumn, newMaxPointsReason);
                toState.updateStoredLeaderboard(leaderboard);
                Entry updatedEntry = leaderboard.getEntry(competitor, raceColumn, timePoint);
                return new Pair<Integer, Integer>(updatedEntry.getNetPoints(), updatedEntry.getTotalPoints());
            } else {
                throw new IllegalArgumentException("Didn't find competitor with ID "+competitorIdAsString+" in leaderboard "+getLeaderboardName());
            }
        } else {
            throw new IllegalArgumentException("Didn't find leaderboard "+getLeaderboardName());
        }
    }

}
