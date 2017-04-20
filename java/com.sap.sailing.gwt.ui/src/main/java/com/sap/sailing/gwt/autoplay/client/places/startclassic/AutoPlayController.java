package com.sap.sailing.gwt.autoplay.client.places.startclassic;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.LeaderboardWithHeaderPerspective;
import com.sap.sailing.gwt.autoplay.client.shared.leaderboard.LeaderboardWithHeaderPerspectiveSettings;
import com.sap.sailing.gwt.settings.client.raceboard.RaceBoardPerspectiveOwnSettings;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.MediaServiceAsync;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sailing.gwt.ui.raceboard.RaceBoardPanel;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.RaceboardDataDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.shared.perspective.PerspectiveCompositeSettings;
import com.sap.sse.gwt.client.useragent.UserAgentDetails;
import com.sap.sse.security.ui.client.UserService;

public class AutoPlayController implements RaceTimesInfoProviderListener, LeaderboardUpdateListener {
    private static final int REFRESH_INTERVAL_IN_MILLIS_LEADERBOARD = 10000;
    private static final long REFRESH_INTERVAL_IN_MILLIS_RACEBOARD = 1000;
    private static final long WAIT_TIME_AFTER_END_OF_RACE_MIILIS = 60 * 1000; // 1 min  
    private final SailingServiceAsync sailingService;
    private final MediaServiceAsync mediaService;
    private final UserService userService;
    private final ErrorReporter errorReporter;
    private final UserAgentDetails userAgent;
    private final AsyncActionsExecutor asyncActionsExecutor;

    /**
     * We use two timers: one for the leaderboard, one for the race board. This way, we can stop one while the other continues.
     * This way, the other component stops its auto-updates.
     */
    private final Timer leaderboardTimer;
    private final Timer raceboardTimer;
    private final RaceTimesInfoProvider raceTimesInfoProvider;  
    private AutoPlayModes activeTvView;
    private boolean isInitialScreen = true;

    // leaderboard perspective related attributes
    private LeaderboardDTO currentLeaderboard;
   
    // raceboard perspective related attributes
    private RegattaAndRaceIdentifier currentLiveRace;
    
    private final PlayerView playerView;
    private final AutoPlayerContextDefinition autoPlayerConfiguration;
    private PerspectiveCompositeSettings<AutoplayPerspectiveOwnSettings> settings;
    private AutoplayPerspectiveLifecycle autoplayLifecycle;

    public AutoPlayController(SailingServiceAsync sailingService, MediaServiceAsync mediaService,
            UserService userService, ErrorReporter errorReporter, AutoPlayerContextDefinition autoPlayerConfiguration,
            PerspectiveCompositeSettings<AutoplayPerspectiveOwnSettings> settings,
            UserAgentDetails userAgent, PlayerView playerView, AutoplayPerspectiveLifecycle autoplayLifecycle) {
        this.sailingService = sailingService;
        this.mediaService = mediaService;
        this.userService = userService;
        this.errorReporter = errorReporter;
        this.autoPlayerConfiguration = autoPlayerConfiguration;
        this.userAgent = userAgent;
        this.playerView = playerView;
        this.settings = settings;
        this.autoplayLifecycle = autoplayLifecycle;

        asyncActionsExecutor = new AsyncActionsExecutor();
        currentLeaderboard = null;
        leaderboardTimer = new Timer(PlayModes.Live, /* delayBetweenAutoAdvancesInMilliseconds */ LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS);
        leaderboardTimer
                .setLivePlayDelayInMillis(settings.getPerspectiveOwnSettings().getTimeToSwitchBeforeRaceStart());
        leaderboardTimer.setRefreshInterval(REFRESH_INTERVAL_IN_MILLIS_LEADERBOARD);
        leaderboardTimer.play();
        raceboardTimer = new Timer(PlayModes.Live, /* delayBetweenAutoAdvancesInMilliseconds */1000l);
        raceboardTimer.setLivePlayDelayInMillis(settings.getPerspectiveOwnSettings().getTimeToSwitchBeforeRaceStart());
        raceboardTimer.setRefreshInterval(REFRESH_INTERVAL_IN_MILLIS_RACEBOARD);
        raceboardTimer.play();

        raceTimesInfoProvider = new RaceTimesInfoProvider(sailingService, asyncActionsExecutor, errorReporter, new ArrayList<RegattaAndRaceIdentifier>(), 3000l);
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(this);
    }
    
    private void showLeaderboard() {
        if (activeTvView != AutoPlayModes.Leaderboard) {
            
            playerView.clearContent();
            boolean withFullscreenButton = settings.getPerspectiveOwnSettings().isFullscreen() && isInitialScreen;

            PerspectiveCompositeSettings<LeaderboardWithHeaderPerspectiveSettings> leaderboardSettings = settings
                    .findSettingsByComponentId(autoplayLifecycle.getLeaderboardLifecycle().getComponentId());

            LeaderboardWithHeaderPerspective leaderboardPerspective = new LeaderboardWithHeaderPerspective(null,
                    null, autoplayLifecycle.getLeaderboardLifecycle(), leaderboardSettings, 
                    sailingService, userService, asyncActionsExecutor,
                    new CompetitorSelectionModel(/* hasMultiSelection */ true), leaderboardTimer,
                    autoPlayerConfiguration.getLeaderboardName(), errorReporter, StringMessages.INSTANCE,
                    withFullscreenButton);

            playerView.setContent(leaderboardPerspective);
            currentLiveRace = null;
            activeTvView = AutoPlayModes.Leaderboard;
            raceboardTimer.pause();
            leaderboardTimer.setPlayMode(PlayModes.Live);
            leaderboardPerspective.addLeaderboardUpdateListener(this);
            isInitialScreen = false;
        }
    }

    private void showRaceBoard() {
        if (activeTvView != AutoPlayModes.Raceboard) {
            
            AsyncCallback<RaceboardDataDTO> raceBoardDataCallback = new AsyncCallback<RaceboardDataDTO>() {
                @Override
                public void onSuccess(RaceboardDataDTO result) {
                    
                    playerView.clearContent();

                    PerspectiveCompositeSettings<RaceBoardPerspectiveOwnSettings> raceboardSettings = settings
                            .findSettingsByComponentId(autoplayLifecycle.getRaceboardLifecycle().getComponentId());

                    RaceBoardPanel raceboardPerspective = new RaceBoardPanel(null, null,
                            autoplayLifecycle.getRaceboardLifecycle(),
                            raceboardSettings,
                            sailingService, mediaService, userService, asyncActionsExecutor,
                            result.getCompetitorAndTheirBoats(), raceboardTimer, currentLiveRace, autoPlayerConfiguration.getLeaderboardName(), 
                            /** leaderboardGroupName */ null, /** eventId */ null, errorReporter,
                            StringMessages.INSTANCE, userAgent, raceTimesInfoProvider, true);

                    playerView.setContent(raceboardPerspective);

                    activeTvView = AutoPlayModes.Raceboard;
                    leaderboardTimer.pause();
                    raceboardTimer.setPlayMode(PlayModes.Live);
                    
                    isInitialScreen = false;
                }
                
                @Override
                public void onFailure(Throwable caught) {
                    errorReporter.reportError("Error while loading data for raceboard: " + caught.getMessage());
                }
            };
            
            sailingService.getRaceboardData(currentLiveRace.getRegattaName(), currentLiveRace.getRaceName(),
                    autoPlayerConfiguration.getLeaderboardName(), null, null, raceBoardDataCallback);
            
        }
    }

    public void updatePlayMode(AutoPlayModes tvView) {
        switch (tvView) {
            case Leaderboard:
                showLeaderboard();
                break;
            case Raceboard:
                showRaceBoard();
                break;
        }
    }
    
    @Override
    public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfo, long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest, long clientTimeWhenResponseWasReceived) {
        raceboardTimer.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest, clientTimeWhenResponseWasReceived);
        leaderboardTimer.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest, clientTimeWhenResponseWasReceived);
        if (currentLiveRace != null) {
            RaceTimesInfoDTO currentRaceTimes = raceTimesInfo.get(currentLiveRace);

            Date endOfRace = currentRaceTimes.endOfRace;
            if (endOfRace != null && raceboardTimer.getTime().getTime() > endOfRace.getTime() + WAIT_TIME_AFTER_END_OF_RACE_MIILIS
                && raceboardTimer.getPlayMode() == PlayModes.Live) {
                updatePlayMode(AutoPlayModes.Leaderboard);
            }
        } else {
            currentLiveRace = checkForLiveRace(raceboardTimer.getLiveTimePointInMillis());
            if (currentLiveRace != null) {
                updatePlayMode(AutoPlayModes.Raceboard);
            } else {
                updatePlayMode(AutoPlayModes.Leaderboard);
            }
        }
    }

    private RegattaAndRaceIdentifier checkForLiveRace(long serverTimePointAsMillis) {
        RegattaAndRaceIdentifier result = null;
        Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfos = raceTimesInfoProvider.getRaceTimesInfos();
        for (RaceColumnDTO race : currentLeaderboard.getRaceList()) {
            for (FleetDTO fleet : race.getFleets()) {
                RegattaAndRaceIdentifier raceIdentifier = race.getRaceIdentifier(fleet);
                if(raceIdentifier != null) {
                    RaceTimesInfoDTO raceTimes = raceTimesInfos.get(raceIdentifier);
                    if (raceTimes != null && raceTimes.startOfTracking != null && raceTimes.getStartOfRace() != null && raceTimes.endOfRace == null) {
                        long startTimeInMs = raceTimes.getStartOfRace().getTime();
                        long timeToSwitchBeforeRaceStartInMs = settings.getPerspectiveOwnSettings()
                                .getTimeToSwitchBeforeRaceStart() * 1000;
                        long delayToLiveInMs = raceTimes.delayToLiveInMs;
                        // the switch to the live race should happen at a defined timepoint before the race start (default is 3 min) 
                        if (serverTimePointAsMillis - delayToLiveInMs > startTimeInMs - timeToSwitchBeforeRaceStartInMs) {
                            return raceIdentifier;
                        }
                    }
                }
            }
        }
        return result;
    }
    
    private void updateRaceTimesInfoProvider() {
        boolean addedRaces = false;
        for (RaceColumnDTO race : currentLeaderboard.getRaceList()) {
            for (FleetDTO fleet : race.getFleets()) {
                RegattaAndRaceIdentifier raceIdentifier = race.getRaceIdentifier(fleet);
                if (raceIdentifier != null && !raceTimesInfoProvider.containsRaceIdentifier(raceIdentifier)) {
                    raceTimesInfoProvider.addRaceIdentifier(raceIdentifier, false);
                    addedRaces = true;
                }
            }
        }
        if (addedRaces) {
            raceTimesInfoProvider.forceTimesInfosUpdate();
        }
    }

    @Override
    public void updatedLeaderboard(LeaderboardDTO leaderboard) {
        this.currentLeaderboard = leaderboard;
        updateRaceTimesInfoProvider();
    }

    @Override
    public void currentRaceSelected(RaceIdentifier raceIdentifier, RaceColumnDTO raceColumn) {
        // no-op
    }
}
