package com.sap.sailing.gwt.settings.client.leaderboard;

import java.util.List;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sse.common.settings.generic.support.SettingsUtil;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolver;

public class MultiRaceLeaderboardPanelLifecycle
        extends AbstractMultiRaceLeaderboardPanelLifecycle<MultiRaceLeaderboardSettings> {

    public MultiRaceLeaderboardPanelLifecycle(AbstractLeaderboardDTO leaderboard, StringMessages stringMessages,
            Iterable<DetailType> availableDetailTypes, PaywallResolver paywallResolver) {
        super(leaderboard, stringMessages, availableDetailTypes, paywallResolver);
    }

    protected MultiRaceLeaderboardPanelLifecycle(AbstractLeaderboardDTO leaderboard, List<String> namesOfRaceColumns, boolean canBoatInfoBeShown,
            StringMessages stringMessages, Iterable<DetailType> availableDetailTypes, PaywallResolver paywallResolver) {
        super(leaderboard, namesOfRaceColumns, canBoatInfoBeShown, stringMessages, availableDetailTypes, paywallResolver);
    }

    @Override
    public MultiRaceLeaderboardSettings extractUserSettings(MultiRaceLeaderboardSettings currentLeaderboardSettings) {
        // All settings except namesOfRaceColumnsToShow are used for the user settings
        return currentLeaderboardSettings.withNamesOfRaceColumnsToShowDefaultsAndValues(namesOfRaceColumns,
                new SecurityChildSettingsContext(leaderboardDTO, paywallResolver));
    }

    @Override
    public MultiRaceLeaderboardSettingsDialogComponent getSettingsDialogComponent(
            MultiRaceLeaderboardSettings settings) {
        return new MultiRaceLeaderboardSettingsDialogComponent(settings, namesOfRaceColumns, stringMessages,
                availableDetailTypes, canBoatInfoBeShown, paywallResolver, leaderboardDTO);
    }

    @Override
    public MultiRaceLeaderboardSettings createDefaultSettings() {
        MultiRaceLeaderboardSettings leaderboardSettings = new MultiRaceLeaderboardSettings(namesOfRaceColumns,
                new SecurityChildSettingsContext(leaderboardDTO, paywallResolver));
        SettingsUtil.copyDefaultsFromValues(leaderboardSettings, leaderboardSettings);
        return leaderboardSettings;
    }
}
