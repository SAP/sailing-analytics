package com.sap.sailing.gwt.autoplay.client.places.screens.afterliveraceloop.boats;

import java.util.Arrays;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.AcceptsOneWidget;
import com.google.gwt.user.client.ui.Label;
import com.sap.sailing.domain.common.DetailType;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.domain.common.dto.CompetitorDTO;
import com.sap.sailing.domain.common.dto.LeaderboardDTO;
import com.sap.sailing.domain.common.dto.RaceColumnDTO;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayPresenterConfigured;
import com.sap.sailing.gwt.autoplay.client.events.AutoPlayHeaderEvent;
import com.sap.sailing.gwt.settings.client.leaderboard.SingleRaceLeaderboardSettings;
import com.sap.sailing.gwt.ui.client.FlagImageResolverImpl;
import com.sap.sailing.gwt.ui.client.LeaderboardUpdateListener;
import com.sap.sailing.gwt.ui.client.RaceCompetitorSelectionModel;
import com.sap.sailing.gwt.ui.client.SailingServiceAsync;
import com.sap.sailing.gwt.ui.client.StringMessages;
import com.sap.sailing.gwt.ui.leaderboard.LeaderboardEntryPoint;
import com.sap.sailing.gwt.ui.leaderboard.SingleRaceLeaderboardPanel;
import com.sap.sailing.gwt.ui.leaderboard.SixtyInchLeaderboardStyle;
import com.sap.sailing.gwt.ui.shared.StrippedLeaderboardDTO;
import com.sap.sse.common.Distance;
import com.sap.sse.common.Duration;
import com.sap.sse.gwt.client.ErrorReporter;
import com.sap.sse.gwt.client.Notification;
import com.sap.sse.gwt.client.Notification.NotificationType;
import com.sap.sse.gwt.client.async.AsyncActionsExecutor;
import com.sap.sse.gwt.client.player.Timer;
import com.sap.sse.gwt.client.player.Timer.PlayModes;
import com.sap.sse.gwt.client.player.Timer.PlayStates;
import com.sap.sse.security.ui.client.SecurityChildSettingsContext;
import com.sap.sse.security.ui.client.premium.PaywallResolverImpl;

public class RaceEndWithBoatsPresenterImpl extends AutoPlayPresenterConfigured<AbstractRaceEndWithImagesTop3Place>
        implements RaceEndWithBoatsView.NextRaceWithBoatsPresenter {
    protected static final int SWITCH_COMPETITOR_DELAY = 2000;
    private RaceEndWithBoatsView view;
    private SingleRaceLeaderboardPanel leaderboardPanel;
    private RaceCompetitorSelectionModel competitorSelectionProvider;
    private Timer timer;

    public RaceEndWithBoatsPresenterImpl(AbstractRaceEndWithImagesTop3Place place, AutoPlayClientFactory clientFactory,
            RaceEndWithBoatsView slide1ViewImpl) {
        super(place, clientFactory);
        this.view = slide1ViewImpl;
    }

    @Override
    public void startConfigured(AcceptsOneWidget panel) {
        final SailingServiceAsync sailingService = getClientFactory().getSailingService();
        final ErrorReporter errorReporter = getClientFactory().getErrorReporter();
        view.startingWith(this, panel);
        final RegattaAndRaceIdentifier liveRace = getPlace().getLastRace();
        if (liveRace == null) {
            panel.setWidget(new Label("No raceIdentifier specified"));
            return;
        }
        getEventBus().fireEvent(new AutoPlayHeaderEvent(getSlideCtx().getContextDefinition().getLeaderboardName(),
                getPlace().getLastRace().getRaceName()));
        sailingService.getLeaderboardWithSecurity(getSlideCtx().getContextDefinition().getLeaderboardName(), new AsyncCallback<StrippedLeaderboardDTO>() {
            @Override
            public void onSuccess(StrippedLeaderboardDTO leaderboardAsSecurityDTO) {
                SecurityChildSettingsContext context = new SecurityChildSettingsContext(leaderboardAsSecurityDTO,
                        new PaywallResolverImpl(getClientFactory().getUserService(), 
                                getClientFactory().getSubscriptionServiceFactory()));
                final SingleRaceLeaderboardSettings leaderboardSettings = new SingleRaceLeaderboardSettings(
                        /* maneuverDetailsToShow */ null, /* legDetailsToShow */ null, /* raceDetailsToShow */ null,
                        /* overallDetailsToShow */ null, /* delayBetweenAutoAdvancesInMilliseconds */ null,
                        /* showAddedScores */ false, /* showCompetitorShortNameColumn */ true,
                        /* showCompetitorFullNameColumn */ false, /* isCompetitorNationalityColumnVisible */ false,
                        /* showCompetitorBoatInfoColumn */ false, /* showRaceRankColumn */ true, context);
                competitorSelectionProvider = new RaceCompetitorSelectionModel(/* hasMultiSelection */ false);
                timer = new com.sap.sse.gwt.client.player.Timer(PlayModes.Live,
                        PlayStates.Paused,
                        /* delayBetweenAutoAdvancesInMilliseconds */ LeaderboardEntryPoint.DEFAULT_REFRESH_INTERVAL_MILLIS);
                leaderboardPanel = new SingleRaceLeaderboardPanel(null, null, sailingService, new AsyncActionsExecutor(), leaderboardSettings,
                        false, liveRace, competitorSelectionProvider, timer, null,
                        getSlideCtx().getContextDefinition().getLeaderboardName(), errorReporter, StringMessages.INSTANCE, 
                        false, null, false, null, false, true, false, false, false, new SixtyInchLeaderboardStyle(true),
                        FlagImageResolverImpl.get(), Arrays.asList(DetailType.values()), getClientFactory());
                int competitorCount = getPlace().getStatistic().getCompetitors();
                Distance distance  = getPlace().getStatistic().getDistance();
                Duration duration = getPlace().getStatistic().getDuration();
                view.setStatistic(competitorCount,distance,duration);
                view.setLeaderBoard(leaderboardPanel);
                leaderboardPanel.addLeaderboardUpdateListener(new LeaderboardUpdateListener() {
                    @Override
                    public void updatedLeaderboard(LeaderboardDTO leaderboard) {
                        determinePlacement(liveRace);
                    }
                    
                    @Override
                    public void currentRaceSelected(RaceIdentifier raceIdentifier, RaceColumnDTO raceColumn) {
                    }
                });
            }
            
            @Override
            public void onFailure(Throwable caught) {
                Notification.notify(StringMessages.INSTANCE.errorLoadingLeaderboard(getSlideCtx().getContextDefinition().getLeaderboardName(), caught.getMessage()), NotificationType.ERROR);
            }
        });
    }

    private void determinePlacement(RegattaAndRaceIdentifier lifeRace) {
        //TODO this is not multifleet capable currently, think about alternatives
        List<RaceColumnDTO> lifeRaceResult = leaderboardPanel.getLeaderboard().getRaceList();
        RaceColumnDTO raceColumn = null;
        for(RaceColumnDTO column:lifeRaceResult){
            if(column.containsRace(lifeRace)){
                raceColumn = column;
            }
        }
        List<CompetitorDTO> sortedCompetitors = leaderboardPanel.getLeaderboard()
                .getCompetitorsFromBestToWorst(raceColumn);

        if (sortedCompetitors.size() >= 3) {
            view.setFirst(sortedCompetitors.get(0));
            view.setSecond(sortedCompetitors.get(1));
            view.setThird(sortedCompetitors.get(2));
        }
    }

    @Override
    public void onStop() {
        if(timer!= null){
            timer.pause();
        }
    }
}
