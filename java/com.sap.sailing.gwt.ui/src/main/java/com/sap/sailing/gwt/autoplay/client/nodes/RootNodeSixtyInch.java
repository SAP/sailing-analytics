package com.sap.sailing.gwt.autoplay.client.nodes;

import com.google.gwt.user.client.Command;
import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.events.FailureEvent;
import com.sap.sailing.gwt.autoplay.client.nodes.base.AutoPlayLoopNode;
import com.sap.sailing.gwt.autoplay.client.nodes.base.AutoPlayNodeBase;
import com.sap.sailing.gwt.autoplay.client.nodes.base.AutoPlaySequenceNode;
import com.sap.sailing.gwt.autoplay.client.nodes.base.RootNodeBase;
import com.sap.sailing.gwt.autoplay.client.nodes.base.RootNodeState;

public class RootNodeSixtyInch extends RootNodeBase {

    private final AutoPlayLoopNode idleLoop;
    private final AutoPlayLoopNode preLiveRaceLoop;
    private final AutoPlayNodeBase liveRaceLoop;
    private final AutoPlaySequenceNode afterLiveRaceLoop;
    protected boolean afterRaceFinished = true;
    private AutoPlayLoopNode preEvent;

    public RootNodeSixtyInch(AutoPlayClientFactory cf) {
        super(RootNodeSixtyInch.class.getName(), cf);
        this.idleLoop = new AutoPlayLoopNode("IdleLoop", 30, new IdleUpNextNode(cf),
                new IdleSixtyInchLeaderboardNode(cf, true), new IdleSixtyInchLeaderboardNode(cf, false),
                new VideoNode(cf));
        this.preEvent = new AutoPlayLoopNode("PreEvent", 30, new IdlePreEventNode(cf), new VideoNode(cf));
        this.preLiveRaceLoop = new AutoPlayLoopNode("PreLiveRaceLoop", 90,
                new PreLiveRaceLeaderboardWithCompetitorsNode(cf), new PreLiveRaceWithRacemapNode(cf));
        // We currently show only the race map in live state. Therefore we do not need loop support here.
        this.liveRaceLoop = new LiveRaceWithRacemapNode(cf);
        this.afterLiveRaceLoop = new AutoPlaySequenceNode("AfterLiveRaceLoop", 30,
                new RaceEndWithCompetitorsBoatsNode(cf), new RaceEndWithCompetitorsFlagsNode(cf));
        afterLiveRaceLoop.setOnSequenceEnd(new Command() {
            @Override
            public void execute() {
                afterRaceFinished = true;
            }
        });
    }

    protected boolean processStateTransition(RegattaAndRaceIdentifier currentPreLiveRace,
            RegattaAndRaceIdentifier currentLiveRace, RootNodeState goingTo, RootNodeState comingFrom) {
        // veto transitions, if in AFTER_LIVE and it did not finish yet
        if (comingFrom == RootNodeState.AFTER_LIVE && !afterRaceFinished) {
            return true;
        }
        getClientFactory().getAutoPlayCtxSignalError().updateLiveRace(currentPreLiveRace, currentLiveRace);
        switch (goingTo) {
        case PRE_EVENT:
            transitionTo(preEvent);
            break;
        case IDLE:
            transitionTo(idleLoop);
            break;
        case PRE_RACE:
            transitionTo(preLiveRaceLoop);
            break;
        case LIVE:
            transitionTo(liveRaceLoop);
            break;
        case AFTER_LIVE:
            transitionTo(afterLiveRaceLoop);
            afterRaceFinished = false;
            break;
        }
        return false;
    }

    protected void processFailure(FailureEvent event) {
        AutoPlayClientFactory cf = getClientFactory();
        if (!cf.isConfigured()) {
            backToConfig();
            return;
        }
        if (event.getCaught() != null) {
            event.getCaught().printStackTrace();
        }
        transitionTo(idleLoop);
    }

    @Override
    protected long getSwitchBeforeRaceStartInMillis() {
        return 3*60*1000;
    }

    @Override
    protected long getWaitTimeAfterRaceEndInMillis() {
        return 60*1000;
    }
}
