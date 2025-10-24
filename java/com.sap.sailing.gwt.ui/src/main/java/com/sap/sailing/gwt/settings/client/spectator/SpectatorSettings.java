package com.sap.sailing.gwt.settings.client.spectator;

import com.sap.sailing.gwt.settings.client.raceboard.RaceBoardPerspectiveOwnSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.BooleanSetting;
import com.sap.sse.common.settings.generic.StringSetting;

public class SpectatorSettings extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = -883043012807297001L;
    
    private transient StringSetting viewMode;
    private transient BooleanSetting canReplayDuringLiveRaces;
    private transient BooleanSetting showMapControls;
    private transient BooleanSetting showRaceDetails;
    private transient BooleanSetting embedded;
    
    public SpectatorSettings() {
        super();
    }
    
    public SpectatorSettings(boolean showRaceDetails) {
        this();
        this.showRaceDetails.setValue(showRaceDetails);
    }
    
    @Override
    protected void addChildSettings() {
        viewMode = new StringSetting("viewMode", this);
        canReplayDuringLiveRaces = new BooleanSetting(RaceBoardPerspectiveOwnSettings.PARAM_CAN_REPLAY_DURING_LIVE_RACES, this, false);
        showMapControls = new BooleanSetting(RaceMapSettings.PARAM_SHOW_MAPCONTROLS, this, true);
        showRaceDetails = new BooleanSetting("showRaceDetails", this, true);
        embedded = new BooleanSetting("embedded", this, false);
    }
    
    public String getViewMode() {
        return viewMode.getValue();
    }
    
    public boolean isCanReplayDuringLiveRaces() {
        return Boolean.TRUE.equals(canReplayDuringLiveRaces.getValue());
    }
    
    public boolean isShowMapControls() {
        return Boolean.TRUE.equals(showMapControls.getValue());
    }
    
    public boolean isShowRaceDetails() {
        return Boolean.TRUE.equals(showRaceDetails.getValue());
    }
    
    public boolean isEmbedded() {
        return Boolean.TRUE.equals(embedded.getValue());
    }
}
