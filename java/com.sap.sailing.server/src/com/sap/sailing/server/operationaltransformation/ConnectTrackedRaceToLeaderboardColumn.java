package com.sap.sailing.server.operationaltransformation;

import java.util.logging.Logger;

import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RegattaLeaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceOperation;

public class ConnectTrackedRaceToLeaderboardColumn extends AbstractLeaderboardColumnOperation<Boolean> {
    private static final Logger logger = Logger.getLogger(ConnectTrackedRaceToLeaderboardColumn.class.getName());
    private static final long serialVersionUID = -1336511401516212508L;
    private final RegattaAndRaceIdentifier raceToConnect;
    private final String fleetName;
    
    public ConnectTrackedRaceToLeaderboardColumn(String leaderboardName, String columnName, String fleetName, RegattaAndRaceIdentifier raceToConnect) {
        super(leaderboardName, columnName);
        this.raceToConnect = raceToConnect;
        this.fleetName = fleetName;
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
    public Boolean internalApplyTo(RacingEventService toState) {
        boolean success = false;
        Leaderboard leaderboard = toState.getLeaderboardByName(getLeaderboardName());
        if (leaderboard != null) {
            RaceColumn raceColumn = leaderboard.getRaceColumnByName(getColumnName());
            if (raceColumn != null) {
                TrackedRace trackedRace = toState.getExistingTrackedRace(raceToConnect);
                if (trackedRace != null) {
                    raceColumn.setTrackedRace(raceColumn.getFleetByName(fleetName), trackedRace);
                } else {
                    raceColumn.setRaceIdentifier(raceColumn.getFleetByName(fleetName), raceToConnect);
                }
                success = true;
                updateDB(toState, leaderboard, raceColumn);
            } else {
                final String leaderboardAsString = leaderboard.getName() + (leaderboard instanceof RegattaLeaderboard ?
                        (" for regatta "+((RegattaLeaderboard) leaderboard).getRegatta().getName()+
                                " ("+((RegattaLeaderboard) leaderboard).getRegatta().hashCode()+")"): "");
                logger.info("unable to find race column "+getColumnName()+" in leaderboard "+leaderboardAsString);
            }
        } else {
            logger.info("unable to find leaderboard "+getLeaderboardName()+" in server "+toState);
        }
        return success;
    }

}
