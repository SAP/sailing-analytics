package com.sap.sailing.gwt.autoplay.client.nodes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.autoplay.client.nodes.base.FiresPlaceNode;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboardsixty.IdleSixtyInchLeaderBoardPlace;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.RaceColumnSelectionStrategies;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sailing.gwt.ui.leaderboard.MultiRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.SixtyInchLeaderBoardStyle;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;

public class IdleSixtyInchLeaderBoardNode extends FiresPlaceNode {
    private static final Logger logger = Logger.getLogger(IdleSixtyInchLeaderBoardNode.class.getName());
    private final AutoPlayClientFactory cf;
    private Timer timer;
    private boolean overallLeaderBoard;
    private Consumer<Integer> durationConsumer;

    public IdleSixtyInchLeaderBoardNode(AutoPlayClientFactory cf, boolean overallLeaderBoard) {
        super(IdleSixtyInchLeaderBoardNode.class.getName());
        this.cf = cf;
        this.overallLeaderBoard = overallLeaderBoard;
    }

    @Override
    public void onStart() {
        List<DetailType> overallDetails = new ArrayList<>();
        overallDetails.add(DetailType.OVERALL_RANK);
        overallDetails.add(DetailType.REGATTA_RANK);

        List<DetailType> raceDetails = new ArrayList<>();
        // raceDetails.add(DetailType.RACE_RANK);
        final MultiRaceLeaderboardSettings leaderboardSettings = new MultiRaceLeaderboardSettings(
                /* maneuverDetailsToShow */ null, /* legDetailsToShow */ null, raceDetails, overallDetails,
                /* namesOfRaceColumnsToShow */ null, /* numberOfLastRacesToShow */5,
                /* delayBetweenAutoAdvancesInMilliseconds */ null, RaceColumnSelectionStrategies.LAST_N,
                /* showAddedScores */ true, /* showCompetitorShortNameColumn */ true,
                /* showCompetitorFullNameColumn */ false, /* showCompetitorBoatInfoColumn */ false,
                /* isCompetitorNationalityColumnVisible */ true);

        timer = new Timer(
                // perform the first request as "live" but don't by default auto-play
                PlayModes.Live, PlayStates.Playing,
                /* delayBetweenAutoAdvancesInMilliseconds */ LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS);
        String leaderBoard = cf.getAutoPlayCtxSignalError().getContextDefinition().getLeaderboardName();

        CompetitorSelectionProvider provider = new CompetitorSelectionModel(true);

        if (overallLeaderBoard) {
            cf.getSailingService().getOverallLeaderboardNamesContaining(leaderBoard,
                    new MarkedAsyncCallback<List<String>>(new AsyncCallback<List<String>>() {
                        @Override
                        public void onSuccess(List<String> result) {
                            if (result.isEmpty()) {
                                durationConsumer.accept(0);
                            } else {
                                startWithLeaderbaord(leaderboardSettings, provider, result.get(0));
                            }
                        }

                        @Override
                        public void onFailure(Throwable caught) {
                            logger.log(Level.SEVERE, "Remote call for Leaderboard loading failed", caught);
                        }
                    }));
        } else {
            startWithLeaderbaord(leaderboardSettings, provider, leaderBoard);
        }
    }

    private void startWithLeaderbaord(final MultiRaceLeaderboardSettings leaderboardSettings,
            CompetitorSelectionProvider provider, String leaderboardName) {
        MultiRaceLeaderboardPanel leaderboardPanel = new MultiRaceLeaderboardPanel(null, null, cf.getSailingService(),
                new AsyncActionsExecutor(), leaderboardSettings, false, provider, timer, null, leaderboardName,
                cf.getErrorReporter(), StringMessages.INSTANCE, false, null, false, null, false, true, false, false,
                false, new SixtyInchLeaderBoardStyle(true), FlagImageResolverImpl.get(),
                Arrays.asList(DetailType.values()));

        IdleSixtyInchLeaderBoardPlace place = new IdleSixtyInchLeaderBoardPlace(leaderboardPanel, provider,
                durationConsumer);
        setPlaceToGo(place);
        firePlaceChangeAndStartTimer();
        getBus().fireEvent(new AutoPlayHeaderEvent(cf.getAutoPlayCtxSignalError().getEvent().getName(),
                cf.getAutoPlayCtxSignalError().getContextDefinition().getLeaderboardName()));
    }

    @Override
    public void onStop() {
        if (timer != null) {
            timer.pause();
        }
        super.onStop();
    }

    @Override
    public void customDurationHook(Consumer<Integer> consumer) {
        this.durationConsumer = consumer;
        // ten minutes should be enough to show all competitors
        consumer.accept(10 * 60);
    }
    

}
