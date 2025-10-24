package com.sap.sailing.gwt.autoplay.client.utils;

import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.web.bindery.event.shared.EventBus;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.AbstractLeaderboardDTO;
import com.sap.sailing.domain.common.dto.BoatDTO;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.FleetDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.home.communication.SailingDispatchSystem;
import com.sap.sailing.gwt.ui.client.CompetitorColorProvider;
import com.sap.sailing.gwt.ui.client.CompetitorColorProviderImpl;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProvider;
import com.sap.sailing.gwt.ui.client.RaceTimesInfoProviderListener;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.DefaultQuickFlagDataProvider;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceCompetitorSet;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMap;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapHelpLinesSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapLifecycle;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapResources;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceMapZoomSettings.ZoomTypes;
import com.sap.sailing.gwt.ui.raceboard.AbstractQuickFlagDataProvider;
import com.sap.sailing.gwt.ui.shared.EventDTO;
import com.sap.sailing.gwt.ui.shared.LeaderboardGroupDTO;
import com.sap.sailing.gwt.ui.shared.RaceTimesInfoDTO;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Util.Pair;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.security.shared.dto.SecuredDTO;
import com.sap.sse.security.ui.client.UserService;
import com.sap.sse.security.ui.client.premium.PaywallResolver;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;
import com.sap.sse.security.ui.client.subscription.SubscriptionServiceFactory;

public class AutoplayHelper {
    private static final Logger LOGGER = Logger.getLogger(AutoplayHelper.class.getName());
    private static final RaceMapResources raceMapResources = GWT.create(RaceMapResources.class);
    private static Timer fastCurrentTimeProvider = new Timer(PlayModes.Live,
            /* delayBetweenAutoAdvancesInMilliseconds */1000l);
    private static Date startOfLiveRace;
    public static final AsyncActionsExecutor asyncActionsExecutor = new AsyncActionsExecutor();
    /**
     * If a racestart is longer ago, the race is never considered live, even if all other checks pass
     */
    private static final long NEGATIVE_SANITY_CHECK = -24 * 60 * 60 * 1000;

    public static long durationOfCurrentLiveRaceRunning() {
        if (startOfLiveRace != null) {
            return fastCurrentTimeProvider.getLiveTimePointInMillis() - startOfLiveRace.getTime();
        } else {
            return 0;
        }
    }

    public static void getLiveRace(SailingServiceAsync sailingService, ErrorReporter errorReporter, EventDTO event,
            String leaderBoardName, SailingDispatchSystem dispatch, long waitTimeAfterRaceEndInMillis,
            long switchBeforeRaceStartInMillis, AsyncCallback<Pair<Long, RegattaAndRaceIdentifier>> callback) {
        if (fastCurrentTimeProvider.getRefreshInterval() != 1000) {
            fastCurrentTimeProvider.setRefreshInterval(1000);
        }
        // only update once (very high timer to retry, meanwhile the forceUpdate will terminate the provider)
        RaceTimesInfoProvider raceTimesInfoProvider = new RaceTimesInfoProvider(sailingService, asyncActionsExecutor,
                errorReporter, new ArrayList<RegattaAndRaceIdentifier>(), 10000);
        StrippedLeaderboardDTO selectedLeaderboard = getSelectedLeaderboard(event, leaderBoardName);
        if (selectedLeaderboard != null) {
            for (RaceColumnDTO race : selectedLeaderboard.getRaceList()) {
                for (FleetDTO fleet : race.getFleets()) {
                    RegattaAndRaceIdentifier raceIdentifier = race.getRaceIdentifier(fleet);
                    if (raceIdentifier != null && !raceTimesInfoProvider.containsRaceIdentifier(raceIdentifier)) {
                        raceTimesInfoProvider.addRaceIdentifier(raceIdentifier, false);
                    }
                }
            }
        }
        if (raceTimesInfoProvider.getRaceIdentifiers().isEmpty()) {
            LOGGER.warning(
                    "No raceidentifier was found. Can not determine current live race. Check event configuration in case you expect a race to be in live state.");
            callback.onSuccess(null);
        }
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(new RaceTimesInfoProviderListener() {
            @Override
            public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfo,
                    long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest,
                    long clientTimeWhenResponseWasReceived) {
                raceTimesInfoProvider.removeRaceTimesInfoProviderListener(this);
                fastCurrentTimeProvider.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest,
                        clientTimeWhenResponseWasReceived);
                Pair<Long, RegattaAndRaceIdentifier> timeToStartAndRaceIdentifier = checkForLiveRace(
                        selectedLeaderboard, serverTimeDuringRequest, raceTimesInfoProvider,
                        waitTimeAfterRaceEndInMillis, switchBeforeRaceStartInMillis);
                callback.onSuccess(timeToStartAndRaceIdentifier);
                // kill old provider!
                raceTimesInfoProvider.terminate();
            }
        });
        raceTimesInfoProvider.forceTimesInfosUpdate();
    }

    /**
     * functional sideeffect free method for getting a leaderboard from an event based on the name
     */
    public static StrippedLeaderboardDTO getSelectedLeaderboard(EventDTO event, String leaderBoardName) {
        for (LeaderboardGroupDTO leaderboardGroup : event.getLeaderboardGroups()) {
            for (StrippedLeaderboardDTO leaderboard : leaderboardGroup.getLeaderboards()) {
                if (leaderboard.getName().equals(leaderBoardName)) {
                    return leaderboard;
                }
            }
        }
        return null;
    }

    /**
     * Side effect free method to get a live race from a timesProvider and a leaderboard.
     * 
     * @return the time to the start of the live race in milliseconds, and the identifier of the live race; or
     *         {@code null} if no live race is found
     */
    public static Pair<Long, RegattaAndRaceIdentifier> checkForLiveRace(AbstractLeaderboardDTO currentLeaderboard,
            Date serverTimeDuringRequest, RaceTimesInfoProvider raceTimesInfoProvider,
            long waitTimeAfterRaceEndInMillis, long switchBeforeRaceStartInMillis) {
        Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfos = raceTimesInfoProvider.getRaceTimesInfos();
        for (RaceColumnDTO race : currentLeaderboard.getRaceList()) {
            for (FleetDTO fleet : race.getFleets()) {
                RegattaAndRaceIdentifier raceIdentifier = race.getRaceIdentifier(fleet);
                if (raceIdentifier != null) {
                    RaceTimesInfoDTO raceTimes = raceTimesInfos.get(raceIdentifier);
                    boolean notNullInRequiredValues = raceTimes != null && raceTimes.startOfTracking != null
                            && raceTimes.getStartOfRace() != null;
                    Date endTime = raceTimes.getFinishedTime() != null ? raceTimes.getFinishedTime() : raceTimes.getEndOfRace();
                    boolean raceHasNotEndedOrOnlyRecentlyEnded = endTime == null
                            || serverTimeDuringRequest.getTime()
                                    - raceTimes.delayToLiveInMs < endTime.getTime()
                                            + waitTimeAfterRaceEndInMillis;
                    if (notNullInRequiredValues && raceHasNotEndedOrOnlyRecentlyEnded) {
                        long startTimeInMs = raceTimes.getStartOfRace().getTime();
                        long startIn = startTimeInMs - serverTimeDuringRequest.getTime() - raceTimes.delayToLiveInMs;
                        if (startIn <= switchBeforeRaceStartInMillis && startIn > NEGATIVE_SANITY_CHECK) {
                            startOfLiveRace = raceTimes.getStartOfRace();
                            return new Pair<Long, RegattaAndRaceIdentifier>(startIn, raceIdentifier);
                        }
                    }
                }
            }
        }
        startOfLiveRace = null;
        return null;
    }

    /**
     * Struct like Returnvalue class for a racemap. <b> The user is required to pause the timer and terminate the
     * RaceTimeProvider after use! </b>
     */
    public static class RVWrapper {
        public final RaceTimesInfoProvider creationTimeProvider;
        public final Timer raceboardTimer;
        public final RaceMap raceboardPerspective;
        public final RaceCompetitorSelectionModel csel;

        public RVWrapper(RaceMap raceboardPerspective, RaceCompetitorSelectionModel competitorSelectionProvider,
                Timer raceboardTimer, RaceTimesInfoProvider creationTimeProvider) {
            this.raceboardPerspective = raceboardPerspective;
            this.csel = competitorSelectionProvider;
            this.raceboardTimer = raceboardTimer;
            this.creationTimeProvider = creationTimeProvider;
        }
    }

    public static void create(SailingServiceAsync sailingService, UserService userService, ErrorReporter errorReporter,
            String leaderBoardName, UUID eventId, EventDTO event, EventBus eventBus,
            SailingDispatchSystem sailingDispatchSystem, RegattaAndRaceIdentifier regattaAndRaceIdentifier,
            AsyncCallback<RVWrapper> callback, SubscriptionServiceFactory subscriptionServiceFactory) {
        LOGGER.info("Creating map for " + regattaAndRaceIdentifier);
        Timer creationTimer = new Timer(PlayModes.Live, /* delayBetweenAutoAdvancesInMilliseconds */1000l);

        creationTimer.setLivePlayDelayInMillis(1000);
        creationTimer.setRefreshInterval(1000);

        StrippedLeaderboardDTO selectedLeaderboard = AutoplayHelper.getSelectedLeaderboard(event, leaderBoardName);

        sailingService.getCompetitorsOfLeaderboard(leaderBoardName, new AsyncCallback<Iterable<CompetitorDTO>>() {
            @Override
            public void onSuccess(Iterable<CompetitorDTO> competitors) {
                RaceTimesInfoProvider creationTimeProvider = new RaceTimesInfoProvider(sailingService,
                        asyncActionsExecutor, errorReporter, new ArrayList<RegattaAndRaceIdentifier>(), 1000l);

                continouslyLoadRaceTimeData(selectedLeaderboard, new RaceTimesInfoProviderListener() {
                    boolean mapAlreadyCreated;

                    @Override
                    public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfo,
                            long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest,
                            long clientTimeWhenResponseWasReceived) {
                        // keep the creationTime updated upon liveChanges!
                        creationTimer.adjustClientServerOffset(clientTimeWhenRequestWasSent, serverTimeDuringRequest,
                                clientTimeWhenResponseWasReceived);
                        creationTimer
                                .setLivePlayDelayInMillis(raceTimesInfo.get(regattaAndRaceIdentifier).delayToLiveInMs);
                        creationTimer.play();
                        if (regattaAndRaceIdentifier != null) {
                            if (!mapAlreadyCreated) {
                                mapAlreadyCreated = true;
                                sailingService.getCompetitorBoats(regattaAndRaceIdentifier,
                                        new AsyncCallback<Map<CompetitorDTO, BoatDTO>>() {
                                            @Override
                                            public void onSuccess(
                                                    Map<CompetitorDTO, BoatDTO> competitorsAndTheirBoats) {
                                                createRaceMapIfNotExist(regattaAndRaceIdentifier, selectedLeaderboard,
                                                        competitorsAndTheirBoats, competitors, sailingService,
                                                        userService, AutoplayHelper.asyncActionsExecutor, errorReporter,
                                                        creationTimer, callback, clientTimeWhenResponseWasReceived,
                                                        serverTimeDuringRequest, clientTimeWhenRequestWasSent,
                                                        raceTimesInfo, creationTimeProvider,
                                                        new DefaultQuickFlagDataProvider(), subscriptionServiceFactory);
                                            }
                                            @Override
                                            public void onFailure(Throwable caught) {
                                                creationTimeProvider.terminate();
                                                creationTimer.pause();
                                                callback.onFailure(
                                                        new IllegalStateException("Error getting Competitor Boats"));
                                            }
                                        });
                            }
                        } else {
                            creationTimeProvider.terminate();
                            creationTimer.pause();
                            callback.onFailure(new IllegalStateException("No Live Race Found"));
                        }
                    }
                }, creationTimeProvider);
            }

            @Override
            public void onFailure(Throwable caught) {
                callback.onFailure(new IllegalStateException("Error getting Competitors"));
            }
        });
    }

    protected static void continouslyLoadRaceTimeData(AbstractLeaderboardDTO selectedLeaderboard,
            RaceTimesInfoProviderListener callback, RaceTimesInfoProvider raceTimesInfoProvider) {
        for (RaceColumnDTO race : selectedLeaderboard.getRaceList()) {
            for (FleetDTO fleet : race.getFleets()) {
                RegattaAndRaceIdentifier raceIdentifier = race.getRaceIdentifier(fleet);
                if (raceIdentifier != null && !raceTimesInfoProvider.containsRaceIdentifier(raceIdentifier)) {
                    raceTimesInfoProvider.addRaceIdentifier(raceIdentifier, false);
                }
            }
        }
        raceTimesInfoProvider.addRaceTimesInfoProviderListener(new RaceTimesInfoProviderListener() {
            @Override
            public void raceTimesInfosReceived(Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfo,
                    long clientTimeWhenRequestWasSent, Date serverTimeDuringRequest,
                    long clientTimeWhenResponseWasReceived) {
                callback.raceTimesInfosReceived(raceTimesInfo, clientTimeWhenRequestWasSent, serverTimeDuringRequest,
                        clientTimeWhenResponseWasReceived);
            }
        });
    }

    private static void createRaceMapIfNotExist(RegattaAndRaceIdentifier currentLiveRace,
            StrippedLeaderboardDTO selectedLeaderboard, Map<CompetitorDTO, BoatDTO> competitorsAndTheirBoats,
            Iterable<CompetitorDTO> competitors, SailingServiceAsync sailingService, UserService userService,
            AsyncActionsExecutor asyncActionsExecutor, ErrorReporter errorReporter, Timer raceboardTimer,
            AsyncCallback<RVWrapper> callback, long clientTimeWhenResponseWasReceived, Date serverTimeDuringRequest,
            long clientTimeWhenRequestWasSent, Map<RegattaAndRaceIdentifier, RaceTimesInfoDTO> raceTimesInfos,
            RaceTimesInfoProvider creationTimeProvider, AbstractQuickFlagDataProvider provider,
            SubscriptionServiceFactory subscriptionServiceFactory) {
        userService.createEssentialSecuredDTOByIdAndType(currentLiveRace.getPermissionType(), currentLiveRace.getName(),
                currentLiveRace.getTypeRelativeObjectIdentifier(), new AsyncCallback<SecuredDTO>() {
                    @Override
                    public void onSuccess(SecuredDTO raceDto) {
                        ArrayList<ZoomTypes> typesToConsiderOnZoom = new ArrayList<>();
                        // Other zoom types such as BOATS, TAILS or WINDSENSORS are not currently used as default zoom types.
                        typesToConsiderOnZoom.add(ZoomTypes.BUOYS);
                        typesToConsiderOnZoom.add(ZoomTypes.BOATS);
                        RaceMapZoomSettings autoFollowRace = new RaceMapZoomSettings(typesToConsiderOnZoom, true);
                        final PaywallResolver paywallResolver = new PaywallResolverImpl(userService, subscriptionServiceFactory);
                        RaceMapSettings settings = new RaceMapSettings(autoFollowRace, new RaceMapHelpLinesSettings(), false, 15,
                                100000l, false, RaceMapSettings.DEFAULT_BUOY_ZONE_RADIUS, false, true, false, false, false, false,
                                RaceMapSettings.getDefaultManeuvers(), false, false, /* startCountDownFontSizeScaling */ 1.5,
                                /* showManeuverLossVisualization */ false, /* showSatelliteLayer */ false, /* showWindLadder */ false,
                                paywallResolver, raceDto);
                        RaceMapLifecycle raceMapLifecycle = new RaceMapLifecycle(StringMessages.INSTANCE,
                                paywallResolver, raceDto);
                        final CompetitorColorProvider colorProvider = new CompetitorColorProviderImpl(currentLiveRace,
                                competitorsAndTheirBoats);
                        RaceCompetitorSelectionModel competitorSelectionProvider = new RaceCompetitorSelectionModel(
                                /* hasMultiSelection */ true, colorProvider, competitorsAndTheirBoats);
                        for (Entry<CompetitorDTO, BoatDTO> entry : competitorsAndTheirBoats.entrySet()) {
                            competitorSelectionProvider.setBoat(entry.getKey(), entry.getValue());
                        }
                        competitorSelectionProvider.setCompetitors(competitors);
                        RaceMap raceboardPerspective = new RaceMap(null, null, raceMapLifecycle, settings,
                                sailingService, asyncActionsExecutor, errorReporter, raceboardTimer,
                                competitorSelectionProvider, new RaceCompetitorSet(competitorSelectionProvider),
                                StringMessages.INSTANCE, currentLiveRace, raceMapResources, false, provider,
                                paywallResolver, /* isSimulationEnabled */false);
                        raceboardPerspective.raceTimesInfosReceived(raceTimesInfos, clientTimeWhenRequestWasSent,
                                serverTimeDuringRequest, clientTimeWhenResponseWasReceived);
                        raceboardTimer.setPlayMode(PlayModes.Live);
                        // wait for one update
                        raceboardPerspective.onResize();
                        callback.onSuccess(new RVWrapper(raceboardPerspective, competitorSelectionProvider,
                                raceboardTimer, creationTimeProvider));
                    }

                    @Override
                    public void onFailure(Throwable caught) {
                        LOGGER.log(Level.SEVERE, "Cannot create essential raceDTO", caught);
                    }
                });
    }

}
