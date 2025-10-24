package com.sap.sailing.gwt.autoplay.client.configs;

import java.util.UUID;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.EnumSetting;
import com.sap.sse.common.settings.generic.StringSetting;
import com.sap.sse.common.settings.generic.StringToEnumConverter;
import com.sap.sse.common.settings.generic.UUIDSetting;

public class AutoPlayContextDefinitionImpl extends AbstractGenericSerializableSettings implements AutoPlayContextDefinition {
    private static final long serialVersionUID = 2880854263616658272L;

    private transient EnumSetting<AutoPlayType> type;
    private transient UUIDSetting eventUUID;
    private transient StringSetting leaderboardName;

    public AutoPlayContextDefinitionImpl(AutoPlayType type, UUID eventUUID, String leaderboardName) {
        this();
        this.eventUUID.setValue(eventUUID);
        this.leaderboardName.setValue(leaderboardName);
        this.type.setValue(type);
    }

    public AutoPlayContextDefinitionImpl() {
        super();
    }

    @Override
    protected void addChildSettings() {
        type = new EnumSetting<AutoPlayType>("autoplayType", this, AutoPlayType.CLASSIC,
                new StringToEnumConverter<AutoPlayType>() {
                    @Override
                    public AutoPlayType fromString(String stringValue) {
                        return AutoPlayType.valueOf(stringValue);
                    }
                });
        eventUUID = new UUIDSetting("eventId", this);
        leaderboardName = new StringSetting("name", this);
    }

    /* (non-Javadoc)
     * @see com.sap.sailing.gwt.autoplay.client.app.classic.AutoPlaySettings#getEventId()
     */
    @Override
    public UUID getEventId() {
        return eventUUID.getValue();
    }

    /* (non-Javadoc)
     * @see com.sap.sailing.gwt.autoplay.client.app.classic.AutoPlaySettings#getLeaderboardName()
     */
    @Override
    public String getLeaderboardName() {
        return leaderboardName.getValue();
    }

    @Override
    public AutoPlayType getType() {
        return type.getValue();
    }

    @Override
    public String toString() {
        return "AutoPlayContextDefinitionImpl [type=" + type + ", eventUUID=" + eventUUID + ", leaderboardName="
                + leaderboardName + "]";
    }
}
