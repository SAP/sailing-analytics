package com.sap.sailing.server.impl;

import com.sap.sailing.server.RacingEventService;
import com.sap.sailing.server.RacingEventServiceMXBean;

public class RacingEventServiceMXBeanImpl implements RacingEventServiceMXBean {
    private final RacingEventService racingEventService;

    protected RacingEventServiceMXBeanImpl(RacingEventService racingEventService) {
        super();
        this.racingEventService = racingEventService;
    }
    
    private RacingEventService getRacingEventService() {
        return racingEventService;
    }

    @Override
    public int getNumberOfLeaderboards() {
        return getRacingEventService().getLeaderboards().size();
    }

    @Override
    public int getNumberOfTrackedRacesToRestore() {
        return getRacingEventService().getNumberOfTrackedRacesToRestore();
    }

    @Override
    public int getNumberOfTrackedRacesRestored() {
        return getRacingEventService().getNumberOfTrackedRacesRestored();
    }
}
