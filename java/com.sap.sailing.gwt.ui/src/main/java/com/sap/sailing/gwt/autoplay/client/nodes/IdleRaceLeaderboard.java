package com.sap.sailing.gwt.autoplay.client.nodes;

import java.util.Arrays;

import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoplayPerspectiveLifecycle;
import com.sap.sailing.gwt.autoplay.client.app.AutoplayPerspectiveOwnSettings;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.autoplay.client.nodes.base.FiresPlaceNode;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboard.LeaderboardPlace;
import com.sap.sailing.gwt.autoplay.client.shared.header.SAPHeaderComponentLifecycle;
import com.sap.sailing.gwt.autoplay.client.shared.header.SAPHeaderComponentSettings;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.LeaderboardWithZoomingPerspectiveSettings;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.MultiRaceLeaderboardWithZoomingPerspective;
import com.sap.sailing.gwt.autoplay.client.utils.AutoplayHelper;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;

public class IdleRaceLeaderboard extends FiresPlaceNode {
    private static final int REFRESH_INTERVAL_IN_MILLIS_LEADERBOARD = 10000;
    private final AutoPlayClientFactory cf;
    private final Timer leaderboardTimer;

    public IdleRaceLeaderboard(AutoPlayClientFactory cf) {
        super(IdleRaceLeaderboard.class.getName());
        this.cf = cf;
        leaderboardTimer = new Timer(PlayModes.Live,
                /* delayBetweenAutoAdvancesInMilliseconds */ LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS);
        leaderboardTimer.setRefreshInterval(REFRESH_INTERVAL_IN_MILLIS_LEADERBOARD);
    }

    public void onStart() {
        leaderboardTimer.setTime(MillisecondsTimePoint.now().asMillis());
        leaderboardTimer.play();
        PerspectiveCompositeSettings<AutoplayPerspectiveOwnSettings> settings = cf.getAutoPlayCtxSignalError()
                .getAutoplaySettings();
        AutoplayPerspectiveLifecycle autoplayLifecycle = cf.getAutoPlayCtxSignalError().getAutoplayLifecycle();
        boolean withFullscreenButton = settings.getPerspectiveOwnSettings().isFullscreen();
        PerspectiveCompositeSettings<LeaderboardWithZoomingPerspectiveSettings> leaderboardSettings = settings
                .findSettingsByComponentId(autoplayLifecycle.getLeaderboardLifecycle().getComponentId());
        StringMessages stringMessages = StringMessages.INSTANCE;
        SAPHeaderComponentLifecycle sapHeaderLifecycle = autoplayLifecycle.getLeaderboardLifecycle().getSapHeaderLifecycle();
        SAPHeaderComponentSettings headerSettings = leaderboardSettings.findSettingsByComponentId(sapHeaderLifecycle.getComponentId());
        MultiRaceLeaderboardWithZoomingPerspective leaderboardPerspective = new MultiRaceLeaderboardWithZoomingPerspective(null, null,
                autoplayLifecycle.getLeaderboardLifecycle(), leaderboardSettings, cf.getSailingService(),
                cf.getUserService(), AutoplayHelper.asyncActionsExecutor,
                new CompetitorSelectionModel(/* hasMultiSelection */ true), leaderboardTimer,
                cf.getAutoPlayCtxSignalError().getContextDefinition().getLeaderboardName(), cf.getErrorReporter(), stringMessages,
                withFullscreenButton, Arrays.asList(DetailType.values()), cf);
        setPlaceToGo(new LeaderboardPlace(leaderboardPerspective));
        getBus().fireEvent(new AutoPlayHeaderEvent(headerSettings.getTitle(), ""));
        firePlaceChangeAndStartTimer();
    };

    @Override
    public void onStop() {
        leaderboardTimer.pause();
        leaderboardTimer.reset();
    }
}
