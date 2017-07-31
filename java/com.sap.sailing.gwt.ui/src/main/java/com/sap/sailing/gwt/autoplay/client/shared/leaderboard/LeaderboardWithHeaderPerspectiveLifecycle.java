package com.sap.sailing.gwt.autoplay.client.shared.leaderboard;

import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.autoplay.client.shared.header.SAPHeaderComponentLifecycle;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardPanelLifecycle;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.perspective.AbstractPerspectiveLifecycle;

/**
 * A special stand alone Leaderboard with an SAP Header is handled by this lifecycle
 *
 */
public class LeaderboardWithHeaderPerspectiveLifecycle extends AbstractPerspectiveLifecycle<LeaderboardWithHeaderPerspectiveSettings> {
    
    private final SAPHeaderComponentLifecycle sapHeaderLifecycle;
    private final MultiRaceLeaderboardPanelLifecycle leaderboardPanelLifecycle;
    private final StringMessages stringMessages;
    
    public static final String ID = "lbwh";

    public LeaderboardWithHeaderPerspectiveLifecycle(AbstractLeaderboardDTO leaderboard, StringMessages stringMessages) {
        this.stringMessages = stringMessages;
        this.leaderboardPanelLifecycle = new MultiRaceLeaderboardPanelLifecycle(leaderboard, stringMessages);
        this.sapHeaderLifecycle = new SAPHeaderComponentLifecycle(stringMessages.leaderboard() +  ": " +
                (leaderboard.getDisplayName() == null ? leaderboard.name : leaderboard.getDisplayName()),
                        stringMessages);
        
        addLifeCycle(leaderboardPanelLifecycle);
        addLifeCycle(sapHeaderLifecycle);
    }
    
    @Override
    public LeaderboardWithHeaderPerspectiveSettings createPerspectiveOwnDefaultSettings() {
        return new LeaderboardWithHeaderPerspectiveSettings();
    }

    @Override
    public SettingsDialogComponent<LeaderboardWithHeaderPerspectiveSettings> getPerspectiveOwnSettingsDialogComponent(LeaderboardWithHeaderPerspectiveSettings perspectiveSettings) {
        return new LeaderboardPerspectiveSettingsDialogComponent(perspectiveSettings, stringMessages);
    }

    @Override
    public String getLocalizedShortName() {
        return stringMessages.leaderboard() + " " + stringMessages.page();
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    public SAPHeaderComponentLifecycle getSapHeaderLifecycle() {
        return sapHeaderLifecycle;
    }

    public MultiRaceLeaderboardPanelLifecycle getLeaderboardPanelLifecycle() {
        return leaderboardPanelLifecycle;
    }

    @Override
    public String getComponentId() {
        return ID;
    }

    @Override
    protected LeaderboardWithHeaderPerspectiveSettings extractOwnUserSettings(
            LeaderboardWithHeaderPerspectiveSettings settings) {
        return settings;
    }

    @Override
    protected LeaderboardWithHeaderPerspectiveSettings extractOwnDocumentSettings(
            LeaderboardWithHeaderPerspectiveSettings settings) {
        return settings;
    }
}
