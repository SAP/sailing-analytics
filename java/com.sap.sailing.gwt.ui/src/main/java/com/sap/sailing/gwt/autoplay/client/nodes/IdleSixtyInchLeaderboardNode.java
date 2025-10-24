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
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayFailureEvent;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.autoplay.client.nodes.base.FiresPlaceNode;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboardsixty.IdleSixtyInchLeaderboardPlace;
import com.sap.sailing.gwt.settings.client.leaderboard.MultiRaceLeaderboardSettings;
import com.sap.sailing.gwt.settings.client.leaderboard.RaceColumnSelectionStrategies;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.CompetitorSelectionProvider;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sailing.gwt.ui.leaderboard.MultiRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.SixtyInchLeaderboardStyle;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.async.MarkedAsyncCallback;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class IdleSixtyInchLeaderboardNode extends FiresPlaceNode {
    private static final Logger logger = Logger.getLogger(IdleSixtyInchLeaderboardNode.class.getName());
    private final AutoPlayClientFactory cf;
    private Timer timer;
    private boolean overallLeaderBoard;
    private Consumer<Integer> durationConsumer;

    public IdleSixtyInchLeaderboardNode(AutoPlayClientFactory cf, boolean overallLeaderBoard) {
        super(IdleSixtyInchLeaderboardNode.class.getName());
        this.cf = cf;
        this.overallLeaderBoard = overallLeaderBoard;
    }

    @Override
    public void onStart() {
        List<DetailType> overallDetails = new ArrayList<>();
        //TODO Enable this, once Leaderboards can actually show the OverallRank see Bug 4588
//        if (this.overallLeaderBoard) {
//            overallDetails.add(DetailType.OVERALL_RANK);
//        } else {
            overallDetails.add(DetailType.REGATTA_RANK);
//        }

        List<DetailType> raceDetails = new ArrayList<>();
        // raceDetails.add(DetailType.RACE_RANK);

        timer = new Timer(
                // perform the first request as "live" but don't by default auto-play
                PlayModes.Live, PlayStates.Playing,
                /* delayBetweenAutoAdvancesInMilliseconds */ LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS);
        String leaderBoard = cf.getAutoPlayCtxSignalError().getContextDefinition().getLeaderboardName();

        CompetitorSelectionProvider provider = new CompetitorSelectionModel(true);
        cf.getSailingService().getLeaderboard(leaderBoard, new AsyncCallback<StrippedLeaderboardDTO>() {
            
            @Override
            public void onSuccess(StrippedLeaderboardDTO strippedLeaderboardDTO) {
                final MultiRaceLeaderboardSettings leaderboardSettings = new MultiRaceLeaderboardSettings(
                        /* maneuverDetailsToShow */ null, /* legDetailsToShow */ null, raceDetails, overallDetails,
                        /* namesOfRaceColumnsToShow */ null, /* numberOfLastRacesToShow */5,
                        /* delayBetweenAutoAdvancesInMilliseconds */ null, RaceColumnSelectionStrategies.LAST_N,
                        /* showAddedScores */ true, /* showCompetitorShortNameColumn */ true,
                        /* showCompetitorFullNameColumn */ false, /* showCompetitorBoatInfoColumn */ false,
                        /* isCompetitorNationalityColumnVisible */ true,
                        new SecurityChildSettingsContext(strippedLeaderboardDTO, new PaywallResolverImpl(cf.getUserService(), cf.getSubscriptionServiceFactory())));
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
            
            @Override
            public void onFailure(Throwable caught) {
                getBus().fireEvent(new AutoPlayFailureEvent(caught, "Error loading Leaderboard with name " + leaderBoard));
            }
        });
        
    }

    private void startWithLeaderbaord(final MultiRaceLeaderboardSettings leaderboardSettings,
            CompetitorSelectionProvider provider, String leaderboardName) {
        MultiRaceLeaderboardPanel leaderboardPanel = new MultiRaceLeaderboardPanel(null, null,
                cf.getSailingService(() -> leaderboardName), new AsyncActionsExecutor(), leaderboardSettings, false,
                provider, timer, leaderboardName, cf.getErrorReporter(), StringMessages.INSTANCE, false, null,
                false, null, false, true, false, false, false, new SixtyInchLeaderboardStyle(true),
                FlagImageResolverImpl.get(), Arrays.asList(DetailType.values()), cf);

        IdleSixtyInchLeaderboardPlace place = new IdleSixtyInchLeaderboardPlace(leaderboardPanel, provider,
                durationConsumer);
        setPlaceToGo(place);
        firePlaceChangeAndStartTimer();
        final String title, subtitle;
        if (this.overallLeaderBoard) {
            title = leaderboardName;
            subtitle = null;
        } else {
            title = cf.getAutoPlayCtxSignalError().getEvent().getName();
            subtitle = leaderboardName;
        }
        getBus().fireEvent(new AutoPlayHeaderEvent(title, subtitle));
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
