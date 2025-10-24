package com.sap.sailing.gwt.autoplay.client.nodes;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.sap.sailing.domain.common.RegattaAndRaceIdentifier;
import com.sap.sailing.gwt.autoplay.client.app.AutoPlayClientFactory;
import com.sap.sailing.gwt.autoplay.client.app.AutoplayPerspectiveOwnSettings;
import com.sap.sailing.gwt.autoplay.client.events.FailureEvent;
import com.sap.sailing.gwt.autoplay.client.nodes.base.AutoPlayNode;
import com.sap.sailing.gwt.autoplay.client.nodes.base.RootNodeBase;
import com.sap.sailing.gwt.autoplay.client.nodes.base.RootNodeState;

public class RootNodeClassic extends RootNodeBase {
    private static final Logger LOGGER = Logger.getLogger(RootNodeClassic.class.getName());
    private final AutoPlayNode idle;
    private final AutoPlayNode live;
    private final IdlePreEventNode preEvent;

    public RootNodeClassic(AutoPlayClientFactory cf) {
        super(RootNodeClassic.class.getName(), cf);
        this.idle = new IdleRaceLeaderboard(cf);
        this.live = new LiveRaceBoardNode(cf);
        this.preEvent = new IdlePreEventNode(cf);
    }

    protected boolean processStateTransition(RegattaAndRaceIdentifier currentPreLiveRace,
            RegattaAndRaceIdentifier currentLiveRace, RootNodeState goingTo, RootNodeState comingFrom) {
        getClientFactory().getAutoPlayCtxSignalError().updateLiveRace(currentPreLiveRace, currentLiveRace);
        switch (goingTo) {
        case PRE_EVENT:
            transitionTo(preEvent);
            break;
        case AFTER_LIVE:
        case IDLE:
            transitionTo(idle);
            break;
        case PRE_RACE:
        case LIVE:
            transitionTo(live);
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
            LOGGER.log(Level.WARNING, "error hook called", event.getCaught());
        }
        transitionTo(idle);
    }

    @Override
    protected long getSwitchBeforeRaceStartInMillis() {
        AutoplayPerspectiveOwnSettings ownSettings = getClientFactory().getAutoPlayCtxSignalError().getAutoplaySettings().getPerspectiveOwnSettings();
        return ownSettings.getTimeToSwitchBeforeRaceStartInSeconds() * 1000;
    }

    @Override
    protected long getWaitTimeAfterRaceEndInMillis() {
        AutoplayPerspectiveOwnSettings ownSettings = getClientFactory().getAutoPlayCtxSignalError().getAutoplaySettings().getPerspectiveOwnSettings();
        return ownSettings.getWaitTimeAfterRaceEndInSeconds() * 1000;
    }

}
