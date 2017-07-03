package com.sap.sailing.gwt.autoplay.client.app;

import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.LeaderboardWithHeaderPerspectiveLifecycle;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardPerspectiveLifecycle;
import com.sap.sse.gwt.client.shared.components.SettingsDialogComponent;
import com.sap.sse.gwt.client.shared.perspective.AbstractPerspectiveLifecycle;

/**
 * This lifecycle contains the necessary child lifecycles to allow AutoPlay to create and use both, a Leaderboard for
 * non life, and a RaceBoard for life races.
 */
public class AutoplayPerspectiveLifecycle extends AbstractPerspectiveLifecycle<AutoplayPerspectiveOwnSettings> {
    public static final String ID = "ap";
    private LeaderboardWithHeaderPerspectiveLifecycle leaderboardLifecycle;
    private RaceBoardPerspectiveLifecycle raceboardLifecycle;

    public AutoplayPerspectiveLifecycle(AbstractLeaderboardDTO leaderboard) {
        leaderboardLifecycle = new LeaderboardWithHeaderPerspectiveLifecycle(leaderboard, StringMessages.INSTANCE);
        raceboardLifecycle = new RaceBoardPerspectiveLifecycle(leaderboard, StringMessages.INSTANCE);
        addLifeCycle(leaderboardLifecycle);
        addLifeCycle(raceboardLifecycle);
    }

    @Override
    public AutoplayPerspectiveOwnSettings createPerspectiveOwnDefaultSettings() {
        return new AutoplayPerspectiveOwnSettings();
    }

    @Override
    public SettingsDialogComponent<AutoplayPerspectiveOwnSettings> getPerspectiveOwnSettingsDialogComponent(
            AutoplayPerspectiveOwnSettings settings) {
        return new AutoplaySettingsDialogComponent(settings);
    }

    public LeaderboardWithHeaderPerspectiveLifecycle getLeaderboardLifecycle() {
        return leaderboardLifecycle;
    }

    public RaceBoardPerspectiveLifecycle getRaceboardLifecycle() {
        return raceboardLifecycle;
    }

    @Override
    public String getLocalizedShortName() {
        return StringMessages.INSTANCE.autoplayConfiguration();
    }

    @Override
    public String getComponentId() {
        return ID;
    }

    @Override
    public boolean hasSettings() {
        return true;
    }

    @Override
    protected AutoplayPerspectiveOwnSettings extractOwnUserSettings(AutoplayPerspectiveOwnSettings settings) {
        return settings;
    }

    @Override
    protected AutoplayPerspectiveOwnSettings extractOwnDocumentSettings(AutoplayPerspectiveOwnSettings settings) {
        return settings;
    }

}
