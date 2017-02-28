package com.sap.sailing.gwt.settings.client.leaderboard;

import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;

/**
 * This is a special lifecycle that uses a different ID, which is required to allow a OverallLeaderboard and a normal
 * Leaderboard to coexist.
 */
public class OverallLeaderboardPanelLifecycle extends LeaderboardPanelLifecycle {

    public static final String ID = "olb";

    public OverallLeaderboardPanelLifecycle(AbstractLeaderboardDTO leaderboard, StringMessages stringMessages) {
        super(leaderboard, stringMessages);
    }

    @Override
    public String getComponentId() {
        return ID;
    }

    @Override
    public String getLocalizedShortName() {
        return StringMessages.INSTANCE.overallLeaderboard();
    }

}
