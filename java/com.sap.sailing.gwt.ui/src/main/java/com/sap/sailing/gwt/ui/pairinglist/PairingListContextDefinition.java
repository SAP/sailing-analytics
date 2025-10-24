package com.sap.sailing.gwt.ui.pairinglist;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;

public class PairingListContextDefinition extends AbstractGenericSerializableSettings {
    
    private static final long serialVersionUID = 6206953942932913058L;
    
    private transient StringSetting leaderboardName;

    public PairingListContextDefinition() {
        super();
    }
    
    public PairingListContextDefinition(String leaderboardName) {
        this();
        this.leaderboardName.setValue(leaderboardName);
    }
    
    @Override
    protected void addChildSettings() {
        this.leaderboardName = new StringSetting("leaderboardName", this);
    }
    
    public String getLeaderboardName() {
        return this.leaderboardName.getValue();
    }
}
