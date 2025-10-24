package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.Collections;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

/**
 * This is a special lifecycle that uses a different ID, which is required to allow a MultiLeaderboard and a normal
 * Leaderboard to coexist.
 */
public class MultipleMultiLeaderboardPanelLifecycle extends MultiRaceLeaderboardPanelLifecycle {
    public static final String MID = "mlb";

    public MultipleMultiLeaderboardPanelLifecycle(StringMessages stringMessages, Iterable<DetailType> availableDetailTypes, 
            PaywallResolver paywallResolver, AbstractLeaderboardDTO leaderboardDTO) {
        super(leaderboardDTO, Collections.emptyList(), false, stringMessages, availableDetailTypes, paywallResolver);
    }

    @Override
    public String getComponentId() {
        return MID;
    }

    @Override
    public MultiRaceLeaderboardSettings extractDocumentSettings(MultiRaceLeaderboardSettings currentLeaderboardSettings) {
        SecurityChildSettingsContext context = new SecurityChildSettingsContext(leaderboardDTO, paywallResolver);
        return currentLeaderboardSettings.withNamesOfRaceColumnsToShowDefaultsAndValues(Collections.emptyList(), context);
    }

    @Override
    public String getLocalizedShortName() {
        return StringMessages.INSTANCE.regattaLeaderboards();
    }
}
