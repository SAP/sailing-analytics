package com.sap.sailing.gwt.settings.client.leaderboardedit;

import com.sap.sse.common.settings.generic.AbstractGenericSerializableSettings;
import com.sap.sse.common.settings.generic.StringSetting;

public class EditableLeaderboardContextDefinition extends AbstractGenericSerializableSettings {

    private static final long serialVersionUID = -2634678952043877600L;
    
    private transient StringSetting leaderboardName;
    
    public EditableLeaderboardContextDefinition() {
        super();
    }
    
    public EditableLeaderboardContextDefinition(String leaderboardName) {
        this();
        this.leaderboardName.setValue(leaderboardName);
    }
    
    @Override
    protected void addChildSettings() {
        leaderboardName = new StringSetting("name", this);
    }
    
    public String getLeaderboardName() {
        return leaderboardName.getValue();
    }
}
