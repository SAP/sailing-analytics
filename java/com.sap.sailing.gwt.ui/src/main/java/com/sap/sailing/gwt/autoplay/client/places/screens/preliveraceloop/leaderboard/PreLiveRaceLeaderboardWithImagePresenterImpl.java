package com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard;

import java.util.ArrayList;
import java.util.Arrays;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.LeaderboardRowDTO;
import com.sap.sailing.gwt.autoplay.client.app.AnimationPanel;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayPresenterConfigured;
import com.sap.sailing.gwt.settings.client.leaderboard.SingleRaceLeaderboardSettings;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.SixtyInchLeaderboardStyle;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class PreLiveRaceLeaderboardWithImagePresenterImpl
        extends AutoPlayPresenterConfigured<AbstractPreRaceLeaderboardWithImagePlace>
        implements PreLeaderboardWithImageView.Slide1Presenter {
    protected static final int SWITCH_COMPETITOR_DELAY = 2000;
    private int selected = -1;
    private PreLeaderboardWithImageView view;
    private SingleRaceLeaderboardPanel leaderboardPanel;
    private Timer selectionTimer;
    private RaceCompetitorSelectionModel competitorSelectionProvider;
    ArrayList<CompetitorDTO> compList = new ArrayList<>();
    private com.sap.sse.gwt.client.player.Timer timer;

    public PreLiveRaceLeaderboardWithImagePresenterImpl(AbstractPreRaceLeaderboardWithImagePlace place,
            AutoPlayClientFactory clientFactory, PreLeaderboardWithImageView slide1ViewImpl) {
        super(place, clientFactory);
        this.view = slide1ViewImpl;
        selectionTimer = new Timer() {
            @Override
            public void run() {
                selectNext();
            }
        };
    }

    protected void selectNext() {
        compList.clear();
        // sync with Leaderboard sorting
        for (LeaderboardRowDTO item : leaderboardPanel.getLeaderboardTable().getVisibleItems()) {
            compList.add(item.competitor);
        }
        if (compList.isEmpty()) {
            // no data loaded yet
            return;
        }
        if (selected >= 0) {
            CompetitorDTO lastSelected = compList.get(selected);
            competitorSelectionProvider.setSelected(lastSelected, false);
        }
        selected++;
        // overflow, restart
        if (selected > compList.size() - 1) {
            selected = 0;
        }
        CompetitorDTO newSelected = compList.get(selected);
        competitorSelectionProvider.setSelected(newSelected, true);
        view.onCompetitorSelect(newSelected);
        Scheduler.get().scheduleDeferred(new ScheduledCommand() {
            @Override
            public void execute() {
                if (selected == 0) {
                    view.scrollLeaderBoardToTop();
                } else {
                    leaderboardPanel.scrollRowIntoView(selected);
                }
                selectionTimer.schedule(SWITCH_COMPETITOR_DELAY);
            }
        });
    }

    @Override
    public void startConfigured(AcceptsOneWidget panel) {
        SailingServiceAsync sailingService = getClientFactory().getSailingService();
        ErrorReporter errorReporter = getClientFactory().getErrorReporter();
        view.startingWith(this, panel);
        view.nextRace(getSlideCtx().getPreLiveRace());
        RegattaAndRaceIdentifier liveRace = getSlideCtx().getPreLiveRace();
        SecurityChildSettingsContext context = new SecurityChildSettingsContext(leaderboardPanel.getLeaderboard(),
                new PaywallResolverImpl(getClientFactory().getUserService(),
                        getClientFactory().getSubscriptionServiceFactory()));
        final SingleRaceLeaderboardSettings leaderboardSettings = new SingleRaceLeaderboardSettings(
                /* maneuverDetailsToShow */ null, /* legDetailsToShow */ null, /* raceDetailsToShow */ null,
                /* overallDetailsToShow */ null, /* delayBetweenAutoAdvancesInMilliseconds */ null,
                /* showAddedScores */ false, /* showCompetitorShortNameColumn */ true,
                /* showCompetitorFullNameColumn */ false, /* isCompetitorNationalityColumnVisible */ false,
                /* showCompetitorBoatInfoColumn */ false, /* showRaceRankColumn */ false, context);
        competitorSelectionProvider = new RaceCompetitorSelectionModel(/* hasMultiSelection */ false);
        timer = new com.sap.sse.gwt.client.player.Timer(
                // perform the first request as "live" but don't by default auto-play
                PlayModes.Live, PlayStates.Playing,
                /* delayBetweenAutoAdvancesInMilliseconds */ LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS);
        leaderboardPanel = new SingleRaceLeaderboardPanel(null, null, sailingService, new AsyncActionsExecutor(),
                leaderboardSettings, false, liveRace, competitorSelectionProvider, timer, null,
                getSlideCtx().getContextDefinition().getLeaderboardName(), errorReporter, StringMessages.INSTANCE,
                false, null, false, null, false, true, false, false, false, new SixtyInchLeaderboardStyle(false),
                FlagImageResolverImpl.get(), Arrays.asList(DetailType.values()), getClientFactory());
        view.setLeaderBoard(leaderboardPanel);
        selectionTimer.schedule(AnimationPanel.DELAY + AnimationPanel.ANIMATION_DURATION);
    }

    @Override
    public void onStop() {
        timer.pause();
        selectionTimer.cancel();
        view.onStop();
    }
}
