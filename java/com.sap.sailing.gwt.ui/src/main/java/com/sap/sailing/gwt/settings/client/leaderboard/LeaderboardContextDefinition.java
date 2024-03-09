package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.UUID;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.UUIDSetting;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;

public class LeaderboardContextDefinition extends AbstractGenericSerializableSettings<SecurityChildSettingsContext> {
    private static final long serialVersionUID = 6699950643747065960L;
    
    private transient UUIDSetting eventId;
    private transient StringSetting leaderboardName;
    private transient StringSetting displayName;

    public LeaderboardContextDefinition() {
        super(null);
    }
    
    public LeaderboardContextDefinition(String leaderboardName, String displayName) {
        this();
        this.leaderboardName.setValue(leaderboardName);
        this.displayName.setValue(displayName);
    }

    @Override
    protected void addChildSettings(SecurityChildSettingsContext context) {
        eventId = new UUIDSetting("eventId", this);
        leaderboardName = new StringSetting("name", this);
        displayName = new StringSetting("displayName", this);
    }
    public UUID getEventId() {
        return eventId.getValue();
    }
    public String getLeaderboardName() {
        return leaderboardName.getValue();
    }
    public String getDisplayName() {
        return displayName.getValue();
    }
}
