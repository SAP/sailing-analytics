package com.sap.sailing.gwt.autoplay.client.app;

import com.google.gwt.activity.shared.Activity;
import com.google.gwt.activity.shared.ActivityMapper;
import com.google.gwt.place.shared.Place;
import com.sap.sailing.gwt.autoplay.client.places.autoplaystart.AutoPlayStartPlace;
import com.sap.sailing.gwt.autoplay.client.places.autoplaystart.AutoPlayStartPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.autoplaystart.AutoPlayStartViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.afterliveraceloop.boats.RaceEndWithBoatsPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.afterliveraceloop.boats.RaceEndWithBoatsViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.afterliveraceloop.boats.RaceEndWithCompetitorsBoatsPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.afterliveraceloop.flags.RaceEndWithCompetitorFlagsPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.idleupnext.IdleNextUpViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.idleupnext.IdleUpNextPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.idleupnext.IdleUpNextPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboard.LeaderboardPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboard.LeaderboardPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboard.LeaderboardViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboardsixty.IdleSixtyInchLeaderboardPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboardsixty.IdleSixtyInchLeaderboardPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.leaderboardsixty.IdleSixtyInchLeaderboardViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.video.VideoPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.video.VideoPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.idleloop.video.VideoViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.raceboard.LiveRaceWithRaceboardPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.raceboard.LiveRaceWithRaceboardPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.raceboard.LiveRaceWithRaceboardViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.racemapwithleaderboard.LiveRaceWithRacemapAndLeaderBoardPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.racemapwithleaderboard.LiveRaceWithRacemapAndLeaderBoardPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.liveraceloop.racemapwithleaderboard.LiveRaceWithRacemapAndLeaderBoardViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.preevent.IdlePreEventPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.preevent.IdlePreEventPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.preevent.IdlePreEventViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.competitors.PreRaceCompetitorsFlagsPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.competitors.PreRaceCompetitorsImagePlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.competitors.PreRaceCompetitorsPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.competitors.PreRaceCompetitorsViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard.PreLiveRaceLeaderboardWithImagePresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard.PreLiveRaceLeaderboardWithImageViewImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard.PreRaceLeaderboardWithCompetitorPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.leaderboard.PreRaceLeaderboardWithFlagPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.racemap.PreRaceRacemapPlace;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.racemap.PreRaceRacemapPresenterImpl;
import com.sap.sailing.gwt.autoplay.client.places.screens.preliveraceloop.racemap.PreRaceRacemapViewImpl;
import com.sap.sailing.gwt.autoplay.client.utils.CompetitorImageProvider;
import com.sap.sailing.gwt.autoplay.client.utils.FlagImageProvider;

public class AutoPlayActivityMapperImpl implements ActivityMapper {
    private final AutoPlayClientFactory clientFactory;

    public AutoPlayActivityMapperImpl(AutoPlayClientFactory clientFactory) {
        super();
        this.clientFactory = clientFactory;
    }

    @Override
    public Activity getActivity(Place place) {
        if (place instanceof AutoPlayStartPlace) {
            return new AutoPlayStartPresenterImpl((AutoPlayStartPlace) place, clientFactory,
                    new AutoPlayStartViewImpl());
        } else if (place instanceof LiveRaceWithRaceboardPlace) {
            return new LiveRaceWithRaceboardPresenterImpl((LiveRaceWithRaceboardPlace) place, clientFactory,
                    new LiveRaceWithRaceboardViewImpl());
        } else if (place instanceof LeaderboardPlace) {
            return new LeaderboardPresenterImpl((LeaderboardPlace) place, clientFactory, new LeaderboardViewImpl());
        }
        if (place instanceof PreRaceLeaderboardWithCompetitorPlace) {
            return new PreLiveRaceLeaderboardWithImagePresenterImpl((PreRaceLeaderboardWithCompetitorPlace) place,
                    clientFactory, new PreLiveRaceLeaderboardWithImageViewImpl(new CompetitorImageProvider(), clientFactory.getUserService()));
        }
        if (place instanceof PreRaceLeaderboardWithFlagPlace) {
            return new PreLiveRaceLeaderboardWithImagePresenterImpl((PreRaceLeaderboardWithFlagPlace) place,
                    clientFactory, new PreLiveRaceLeaderboardWithImageViewImpl(new FlagImageProvider(), clientFactory.getUserService()));
        }
        if (place instanceof IdleUpNextPlace) {
            return new IdleUpNextPresenterImpl((IdleUpNextPlace) place, clientFactory, new IdleNextUpViewImpl());
        }
        if (place instanceof IdlePreEventPlace) {
            return new IdlePreEventPresenterImpl((IdlePreEventPlace) place, clientFactory, new IdlePreEventViewImpl());
        }
        if (place instanceof VideoPlace) {
            return new VideoPresenterImpl((VideoPlace) place, clientFactory, new VideoViewImpl());
        }
        if (place instanceof PreRaceCompetitorsFlagsPlace) {
            return new PreRaceCompetitorsPresenterImpl((PreRaceCompetitorsFlagsPlace) place, clientFactory,
                    new PreRaceCompetitorsViewImpl(new FlagImageProvider()));
        }
        if (place instanceof PreRaceCompetitorsImagePlace) {
            return new PreRaceCompetitorsPresenterImpl((PreRaceCompetitorsImagePlace) place, clientFactory,
                    new PreRaceCompetitorsViewImpl(new CompetitorImageProvider()));
        }
        if (place instanceof PreRaceRacemapPlace) {
            return new PreRaceRacemapPresenterImpl((PreRaceRacemapPlace) place, clientFactory,
                    new PreRaceRacemapViewImpl());
        }
        if (place instanceof LiveRaceWithRacemapAndLeaderBoardPlace) {
            return new LiveRaceWithRacemapAndLeaderBoardPresenterImpl((LiveRaceWithRacemapAndLeaderBoardPlace) place,
                    clientFactory, new LiveRaceWithRacemapAndLeaderBoardViewImpl());
        }
        if (place instanceof RaceEndWithCompetitorsBoatsPlace) {
            return new RaceEndWithBoatsPresenterImpl((RaceEndWithCompetitorsBoatsPlace) place, clientFactory,
                    new RaceEndWithBoatsViewImpl(new CompetitorImageProvider(), clientFactory.getUserService()));
        }
        if (place instanceof RaceEndWithCompetitorFlagsPlace) {
            return new RaceEndWithBoatsPresenterImpl((RaceEndWithCompetitorFlagsPlace) place, clientFactory,
                    new RaceEndWithBoatsViewImpl(new FlagImageProvider(), clientFactory.getUserService()));
        }
        if (place instanceof IdleSixtyInchLeaderboardPlace) {
            return new IdleSixtyInchLeaderboardPresenterImpl((IdleSixtyInchLeaderboardPlace) place, clientFactory,
                    new IdleSixtyInchLeaderboardViewImpl());
        }
        return null;
    }
}
