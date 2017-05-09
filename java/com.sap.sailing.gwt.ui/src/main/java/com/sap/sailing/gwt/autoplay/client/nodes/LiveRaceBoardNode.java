package com.sap.sailing.gwt.autoplay.client.nodes;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoplayPerspectiveLifecycle;
import com.sap.sailing.gwt.autoplay.client.app.AutoplayPerspectiveOwnSettings;
import com.sap.sailing.gwt.autoplay.client.nodes.base.FiresPlaceNode;
import com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.raceboard.LiveRaceWithRaceboardPlace;
import com.sap.sailing.gwt.autoplay.client.utils.AutoplayHelper;
import com.sap.sailing.gwt.settings.client.raceboard.RaceBoardPerspectiveOwnSettings;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardPanel;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceboardDataDTO;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.security.ui.client.UserService;

public class LiveRaceBoardNode extends FiresPlaceNode implements RaceTimesInfoProviderListener {
    private static final int EFRESH_INTERVAL_IN_MILLIS_LEADERBOARD = 10000;
    private static final long REFRESH_INTERVAL_IN_MILLIS_RACEBOARD = 1000;
    private static final long LIS = 60 * 1000; // 1 min
    private final AutoPlayClientFactory cf;
    private final Timer raceboardTimer;
    private final RaceTimesInfoProvider raceTimesInfoProvider;


    public LiveRaceBoardNode(AutoPlayClientFactory cf) {

        this.cf = cf;
        SailingServiceAsync sailingService = cf.getSailingService();

        PerspectiveCompositeSettings<AutoplayPerspectiveOwnSettings> settings = cf.getSlideCtx().getAutoplaySettings();
        raceboardTimer = new Timer(PlayModes.Live, /* delayBetweenAutoAdvancesInMilliseconds */1000l);
        raceboardTimer.setLivePlayDelayInMillis(settings.getPerspectiveOwnSettings().getTimeToSwitchBeforeRaceStart());
        raceboardTimer.setRefreshInterval(REFRESH_INTERVAL_IN_MILLIS_RACEBOARD);
        raceboardTimer.play();
        raceTimesInfoProvider = new RaceTimesInfoProvider(sailingService, AutoplayHelper.asyncActionsExecutor,
                cf.getErrorReporter(),
                new ArrayList<RegattaAndRaceIdentifier>(), 3000l);
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(this);

    }

    public void onStart() {
        raceTimesInfoProvider.addRaceIdentifier(cf.getSlideCtx().getLifeRace(), true);

        PerspectiveCompositeSettings<AutoplayPerspectiveOwnSettings> settings = cf.getSlideCtx().getAutoplaySettings();
        AutoplayPerspectiveLifecycle autoplayLifecycle = cf.getSlideCtx().getAutoplayLifecycle();
        UserService userService = cf.getUserService();
        SailingServiceAsync sailingService = cf.getSailingService();
        MediaServiceAsync mediaService = cf.getMediaService();

        AsyncCallback<RaceboardDataDTO> raceBoardDataCallback = new AsyncCallback<RaceboardDataDTO>() {
            @Override
            public void onSuccess(RaceboardDataDTO result) {

                PerspectiveCompositeSettings<RaceBoardPerspectiveOwnSettings> raceboardSettings = settings
                        .findSettingsByComponentId(autoplayLifecycle.getRaceboardLifecycle().getComponentId());
                RaceBoardPanel raceboardPerspective = new RaceBoardPanel(null, null,
                        autoplayLifecycle.getRaceboardLifecycle(), raceboardSettings, sailingService, mediaService,
                        userService, AutoplayHelper.asyncActionsExecutor, result.getCompetitorAndTheirBoats(),
                        raceboardTimer,
                        cf.getSlideCtx().getLifeRace(), cf.getSlideCtx().getSettings().getLeaderboardName(),
                        /** leaderboardGroupName */
                        null, /** eventId */
                        null, cf.getErrorReporter(), StringMessages.INSTANCE, null, raceTimesInfoProvider, true);
                setPlaceToGo(new LiveRaceWithRaceboardPlace(raceboardPerspective));
                firePlaceChangeAndStartTimer();
            }

            @Override
            public void onFailure(Throwable caught) {
                cf.getErrorReporter().reportError("Error while loading data for raceboard: " + caught.getMessage());
            }
        };
        sailingService.getRaceboardData(cf.getSlideCtx().getLifeRace().getRegattaName(),
                cf.getSlideCtx().getLifeRace().getRaceName(), cf.getSlideCtx().getSettings().getLeaderboardName(), null,
                null, raceBoardDataCallback);
    };

    @Override
    public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfo,
            long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest, long clientTimeWhenResponseWasReceived) {
        raceboardTimer.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest,
                clientTimeWhenResponseWasReceived);
    }

    @Override
    public void onStop() {
        raceboardTimer.pause();
        raceboardTimer.reset();
    }
}
