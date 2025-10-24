package com.sap.sailing.gwt.settings.client.raceboard;

import java.util.UUID;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.UUIDSetting;

public class RaceboardContextDefinition extends AbstractGenericSerializableSettings {
    private static final long serialVersionUID = 2632481216072003426L;
    
    private StringSetting regattaName;
    private StringSetting raceName;
    private StringSetting leaderboardName;
    private StringSetting leaderboardGroupName;
    private UUIDSetting leaderboardGroupId;
    private UUIDSetting eventId;
    private StringSetting mode;

    public RaceboardContextDefinition() {
        super();
    }

    /**
     * If the {@code leaderboardGroupId} is provided as a non-{@code null} value, the {@code leaderboardGroupName}
     * parameter is ignored and not stored. Otherwise, the {@code leaderboardGroupName} is remembered.
     */
    public RaceboardContextDefinition(String regattaName, String raceName, String leaderboardName,
            String leaderboardGroupName, UUID leaderboardGroupId, UUID eventId, String mode) {
        this(regattaName, raceName, leaderboardName,
             // use the leaderboardGroupId if not null; only resort to leaderboardGroupName if ID is not provided
             leaderboardGroupId == null ? leaderboardGroupName : null, leaderboardGroupId, eventId, mode, /* selected competitor ID */ null);
    }

    public RaceboardContextDefinition(String regattaName, String raceName, String leaderboardName,
            String leaderboardGroupName, UUID leaderboardGroupId, UUID eventId, String mode, String selectedCompetitorId) {
        this();
        this.regattaName.setValue(regattaName);
        this.raceName.setValue(raceName);
        this.leaderboardName.setValue(leaderboardName);
        this.leaderboardGroupName.setValue(leaderboardGroupName);
        this.leaderboardGroupId.setValue(leaderboardGroupId);
        this.eventId.setValue(eventId);
        this.mode.setValue(mode);
    }

    @Override
    protected void addChildSettings() {
        regattaName = new StringSetting("regattaName", this);
        raceName = new StringSetting("raceName", this);
        leaderboardName = new StringSetting("leaderboardName", this);
        leaderboardGroupName = new StringSetting("leaderboardGroupName", this);
        leaderboardGroupId = new UUIDSetting("leaderboardGroupId", this);
        eventId = new UUIDSetting("eventId", this);
        mode = new StringSetting("mode", this);
    }

    public String getRegattaName() {
        return regattaName.getValue();
    }

    public String getRaceName() {
        return raceName.getValue();
    }

    public String getLeaderboardName() {
        return leaderboardName.getValue();
    }

    public String getLeaderboardGroupName() {
        return leaderboardGroupName.getValue();
    }
    
    public UUID getLeaderboardGroupId() {
        return leaderboardGroupId.getValue();
    }

    public UUID getEventId() {
        return eventId.getValue();
    }

    public String getMode() {
        return mode.getValue();
    }
}
