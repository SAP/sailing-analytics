package com.sap.sailing.gwt.settings.client.spectator;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;

public class SpectatorContextDefinition extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = -3422076606157458337L;
    
    private transient StringSetting leaderboardGroupName;
    
    private transient StringSetting leaderboardGroupId;
    
    public SpectatorContextDefinition() {
        super();
    }
    
    public SpectatorContextDefinition(String leaderboardGroupId) {
        this();
        this.leaderboardGroupId.setValue(leaderboardGroupId);
    }
    
    @Override
    protected void addChildSettings() {
        leaderboardGroupName = new StringSetting("leaderboardGroupName", this);
        leaderboardGroupId = new StringSetting("leaderboardGroupId", this);
    }
    
    public String getLeaderboardGroupName() {
        return leaderboardGroupName.getValue();
    }
    
    public String getLeaderboardGroupId() {
        return leaderboardGroupId.getValue();
    }
}
