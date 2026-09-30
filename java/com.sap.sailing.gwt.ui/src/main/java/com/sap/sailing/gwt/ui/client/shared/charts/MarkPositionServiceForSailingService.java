package com.sap.sailing.gwt.ui.client.shared.charts;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.gwt.ui.client.SailingServiceWriteAsync;
import com.sap.sailing.gwt.ui.client.shared.charts.RaceIdentifierToLeaderboardRaceColumnAndFleetMapper.LeaderboardNameRaceColumnNameAndFleetName;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sse.common.Position;

public class MarkPositionServiceForSailingService implements MarkPositionService {
    private final SailingServiceWriteAsync sailingServiceWrite;
    
    public MarkPositionServiceForSailingService(SailingServiceWriteAsync sailingServiceWrite) {
        super();
        this.sailingServiceWrite = sailingServiceWrite;
    }
    
    @Override
    public void getMarksInTrackedRace(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, AsyncCallback<Iterable<MarkDTO>> callback) {
        sailingServiceWrite.getMarksInTrackedRace(raceIdentifier.getLeaderboardName(), raceIdentifier.getRaceColumnName(), raceIdentifier.getFleetName(), callback);
    }

    @Override
    public void getMarkTrack(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, String markIdAsString,
            AsyncCallback<MarkTrackDTO> callback) {
        sailingServiceWrite.getMarkTrack(raceIdentifier.getLeaderboardName(), raceIdentifier.getRaceColumnName(), raceIdentifier.getFleetName(), markIdAsString, callback);
    }

    @Override
    public void canRemoveMarkFix(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, MarkDTO mark,
            GPSFixDTO fix, AsyncCallback<Boolean> callback) {
        sailingServiceWrite.canRemoveMarkFix(raceIdentifier.getLeaderboardName(), raceIdentifier.getRaceColumnName(), raceIdentifier.getFleetName(), mark.getIdAsString(), fix, callback);
    }

    @Override
    public void removeMarkFix(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, MarkDTO mark, GPSFixDTO fix, AsyncCallback<Void> callback) {
        sailingServiceWrite.removeMarkFix(raceIdentifier.getLeaderboardName(), raceIdentifier.getRaceColumnName(), raceIdentifier.getFleetName(), mark.getIdAsString(), fix, callback);
    }

    @Override
    public void addMarkFix(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, MarkDTO mark, GPSFixDTO newFix, AsyncCallback<Void> callback) {
        sailingServiceWrite.addMarkFix(raceIdentifier.getLeaderboardName(), raceIdentifier.getRaceColumnName(), raceIdentifier.getFleetName(), mark.getIdAsString(), newFix, callback);
    }

    @Override
    public void editMarkFix(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, MarkDTO mark, GPSFixDTO oldFix,
            Position newPosition, AsyncCallback<Void> callback) {
        sailingServiceWrite.editMarkFix(raceIdentifier.getLeaderboardName(), raceIdentifier.getRaceColumnName(), raceIdentifier.getFleetName(), mark.getIdAsString(), oldFix, newPosition, callback);
    }
}
