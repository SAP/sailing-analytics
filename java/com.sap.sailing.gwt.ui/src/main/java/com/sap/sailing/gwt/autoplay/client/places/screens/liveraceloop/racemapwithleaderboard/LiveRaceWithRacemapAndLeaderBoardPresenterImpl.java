package com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.racemapwithleaderboard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.WindSource;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.gwt.autoplay.client.app.AnimationPanel;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayPresenterConfigured;
import com.sap.sailing.gwt.autoplay.client.utils.AutoplayHelper;
import com.sap.sailing.gwt.settings.client.leaderboard.SingleRaceLeaderboardSettings;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.client.shared.racemap.RaceCompetitorSet;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.SixtyInchLeaderBoardStyle;
import com.sap.sailing.gwt.ui.raceboard.QuickRanksDTOFromLeaderboardDTOProvider;
import com.sap.sailing.gwt.ui.shared.WindDTO;
import com.sap.sailing.gwt.ui.shared.WindTrackInfoDTO;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;

public class LiveRaceWithRacemapAndLeaderBoardPresenterImpl
        extends AutoPlayPresenterConfigured<LiveRaceWithRacemapAndLeaderBoardPlace>
        implements LiveRaceWithRacemapAndLeaderBoardView.Slide7Presenter {
    protected static final int SWITCH_COMPETITOR_DELAY = 2000;
    private static final Logger LOGGER = Logger.getLogger(LiveRaceWithRacemapAndLeaderBoardPresenterImpl.class.getName());
    private LiveRaceWithRacemapAndLeaderBoardView view;
    private Timer selectionTimer;
    private SingleRaceLeaderboardPanel leaderboardPanel;
    private int selected = -1;
    ArrayList<CompetitorDTO> compList = new ArrayList<>();
    private com.sap.sse.gwt.client.player.Timer timer;

    public LiveRaceWithRacemapAndLeaderBoardPresenterImpl(LiveRaceWithRacemapAndLeaderBoardPlace place,
            AutoPlayClientFactory clientFactory, LiveRaceWithRacemapAndLeaderBoardView LifeRaceWithRacemapViewImpl) {
        super(place, clientFactory);
        this.view = LifeRaceWithRacemapViewImpl;
        selectionTimer = new Timer() {
            @Override
            public void run() {
                selectNext();
            }
        };
    }

    protected void selectNext() {
        try {
            compList.clear();
            // sync with Leaderboard sorting
            for (LeaderboardRowDTO item : leaderboardPanel.getLeaderboardTable().getVisibleItems()) {
                compList.add(item.competitor);
                getPlace().getRaceMapSelectionProvider().setSelected(item.competitor, false);
            }
            // wait for data in leaderboard, if empty no need to proceed
            if (compList.isEmpty()) {
                selectionTimer.schedule(SWITCH_COMPETITOR_DELAY);
                return;
            }
            selected++;
            // overflow, restart
            if (selected > compList.size() - 1) {
                selected = 0;
            }
            CompetitorDTO marked = compList.get(selected);
            getPlace().getRaceMapSelectionProvider().setSelected(marked, true);
            onSelect(marked);
            Scheduler.get().scheduleDeferred(new ScheduledCommand() {
                @Override
                public void execute() {
                    leaderboardPanel.scrollRowIntoView(selected);
                    if (selected == 0) {
                        view.ensureMapVisibility();
                    }
                }
            });
        } catch (Exception e) {
            // ensure that the loop keeps running, no matter if errors occur
            LOGGER.log(Level.WARNING, "error in leaderboard loop" , e);
            selected = 0;
        }
        selectionTimer.schedule(SWITCH_COMPETITOR_DELAY);

        updateStatistics();
    }

    private void updateStatistics() {
        String windSpeed = null;
        if (getPlace().getRaceMap().getLastCombinedWindTrackInfoDTO() != null) {
            for (WindSource windSource : getPlace().getRaceMap()
                    .getLastCombinedWindTrackInfoDTO().windTrackInfoByWindSource.keySet()) {
                WindTrackInfoDTO windTrackInfoDTO = getPlace().getRaceMap()
                        .getLastCombinedWindTrackInfoDTO().windTrackInfoByWindSource.get(windSource);
                switch (windSource.getType()) {
                case COMBINED:
                    if (!windTrackInfoDTO.windFixes.isEmpty()) {
                        WindDTO windDTO = windTrackInfoDTO.windFixes.get(0);
                        double speedInKnots = windDTO.dampenedTrueWindSpeedInKnots;
                        NumberFormat numberFormat = NumberFormat.getFormat("0.0");
                        windSpeed = numberFormat.format(speedInKnots) + " " + StringMessages.INSTANCE.knotsUnit();
                    }
                    break;
                default:
                }
            }
        }

        
        
        List<LeaderboardRowDTO> sortedCompetitors = leaderboardPanel.getLeaderboardTable().getVisibleItems();
        if (sortedCompetitors.size() > 0) {
            if (getPlace().getStatistic() == null) {
                view.setStatistic(windSpeed, null, AutoplayHelper.durationOfCurrentLiveRaceRunning());
            } else {
                view.setStatistic(windSpeed, getPlace().getStatistic().getDistance(),
                        AutoplayHelper.durationOfCurrentLiveRaceRunning());
            }
        }
    }

    private void onSelect(CompetitorDTO marked) {
        view.onCompetitorSelect(marked);
    }

    @Override
    public void startConfigured(AcceptsOneWidget panel) {
        if (getPlace().getError() != null) {
            view.showErrorNoLive(this, panel, getPlace().getError());
            return;
        }
        SailingServiceAsync sailingService = getClientFactory().getSailingService();
        ErrorReporter errorReporter = getClientFactory().getErrorReporter();
        RegattaAndRaceIdentifier liveRace = getSlideCtx().getLiveRace();
        ArrayList<String> racesToShow = null;
        if (liveRace != null) {
            racesToShow = new ArrayList<>();
            racesToShow.add(liveRace.getRaceName());
        } else {
            view.showErrorNoLive(this, panel, new IllegalStateException("No race is live"));
            return;
        }
        final SingleRaceLeaderboardSettings leaderboardSettings = new SingleRaceLeaderboardSettings(
                /* maneuverDetailsToShow */ null, /* legDetailsToShow */ null, /* raceDetailsToShow */ null,
                /* overallDetailsToShow */ null, /* delayBetweenAutoAdvancesInMilliseconds */ null,
                /* showAddedScores */ false, /* showCompetitorShortNameColumn */ true,
                /* showCompetitorFullNameColumn */ false, /* isCompetitorNationalityColumnVisible */ false,
                /* showCompetitorBoatInfoColumn */ false, /* showRaceRankColumn */ true);
        timer = new com.sap.sse.gwt.client.player.Timer(
                // perform the first request as "live" but don't by default auto-play
                PlayModes.Live, PlayStates.Playing,
                /* delayBetweenAutoAdvancesInMilliseconds */ LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS);
        leaderboardPanel = new SingleRaceLeaderboardPanel(null, null, sailingService, new AsyncActionsExecutor(),
                leaderboardSettings, false, liveRace, getPlace().getRaceMapSelectionProvider(), timer, null,
                getSlideCtx().getContextDefinition().getLeaderboardName(), errorReporter, StringMessages.INSTANCE, 
                false, null, false, null, false, true, false, false, false, new SixtyInchLeaderBoardStyle(true),
                FlagImageResolverImpl.get(), Arrays.asList(DetailType.values()));
        getPlace().getRaceMap().setQuickRanksDTOProvider(new QuickRanksDTOFromLeaderboardDTOProvider(
                new RaceCompetitorSet(getPlace().getRaceMapSelectionProvider()), liveRace));
        view.startingWith(this, panel, getPlace().getRaceMap(), leaderboardPanel);
        selectionTimer.schedule(SWITCH_COMPETITOR_DELAY + AnimationPanel.ANIMATION_DURATION + AnimationPanel.DELAY);
    }

    @Override
    public void onStop() {
        if (timer != null) {
            timer.pause();
        }
        if(getPlace().getRaceboardTimer() != null){
            getPlace().getRaceboardTimer().pause();
        }
        if(getPlace().getTimeProvider() != null){
            getPlace().getTimeProvider().terminate();
        }
        selectionTimer.cancel();
        view.onStop();
    }
}
