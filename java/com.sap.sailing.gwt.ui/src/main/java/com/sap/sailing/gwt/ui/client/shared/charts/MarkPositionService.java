package com.sap.sailing.gwt.ui.client.shared.charts;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.sap.sailing.gwt.ui.client.shared.charts.RaceIdentifierToLeaderboardRaceColumnAndFleetMapper.LeaderboardNameRaceColumnNameAndFleetName;
import com.sap.sailing.gwt.ui.shared.GPSFixDTO;
import com.sap.sailing.gwt.ui.shared.MarkDTO;
import com.sap.sse.common.Position;

public interface MarkPositionService {
    public static class MarkTrackDTO implements IsSerializable {
        private MarkDTO mark;
        private Iterable<GPSFixDTO> fixes;
        private boolean thinnedOut;
        
        MarkTrackDTO() {} // for GWT serialization
        
        public MarkTrackDTO(MarkDTO mark, Iterable<GPSFixDTO> fixes, boolean thinnedOut) {
            this.mark = mark;
            this.fixes = fixes;
            this.thinnedOut = thinnedOut;
        }

        public MarkDTO getMark() {
            return mark;
        }

        public Iterable<GPSFixDTO> getFixes() {
            return fixes;
        }

        public boolean isThinnedOut() {
            return thinnedOut;
        }
    }
    
    public static class MarkTracksDTO implements IsSerializable {
        private Iterable<MarkTrackDTO> tracks;
        
        MarkTracksDTO() {} // for GWT serialization
        
        public MarkTracksDTO(Iterable<MarkTrackDTO> tracks) {
            this.tracks = tracks;
        }

        public Iterable<MarkTrackDTO> getTracks() {
            return tracks;
        }
    }
    
    void getMarksInTrackedRace(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, AsyncCallback<Iterable<MarkDTO>> callback);
    
    void getMarkTrack(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, String markIdAsString, AsyncCallback<MarkTrackDTO> callback);
    
    /**
     * The service may decide whether a mark fix can be removed. It may, for example, be impossible to
     * cleanly remove a mark fix if a tracked race already exists and the mark fixes are already part of
     * the GPS fix track which currently does not support a remove operation. However, when only the
     * regatta log is the basis of the service and no tracked race exists yet, mark fixes may be removed
     * by revoking the device mappings.
     */
    void canRemoveMarkFix(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, MarkDTO mark, GPSFixDTO fix, AsyncCallback<Boolean> callback);
    
    void removeMarkFix(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, MarkDTO mark, GPSFixDTO fix, AsyncCallback<Void> callback);
    
    void addMarkFix(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, MarkDTO mark, GPSFixDTO newFix, AsyncCallback<Void> callback);
    
    void editMarkFix(LeaderboardNameRaceColumnNameAndFleetName raceIdentifier, MarkDTO mark, GPSFixDTO oldFix, Position newPosition, AsyncCallback<Void> callback);
}