package com.sap.sailing.domain.racelog.tracking.impl;

import java.util.UUID;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.Regatta;
import com.sap.sailing.domain.common.racelog.tracking.RaceNotCreatedException;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.racelog.RaceLog;
import com.sap.sailing.domain.racelog.tracking.GPSFixStore;
import com.sap.sailing.domain.racelog.tracking.analyzing.impl.RaceInformationFinder;
import com.sap.sailing.domain.racelog.tracking.analyzing.impl.RaceLogTrackingStateAnalyzer;
import com.sap.sailing.domain.tracking.DynamicTrackedRegatta;
import com.sap.sailing.domain.tracking.RaceTracker;
import com.sap.sailing.domain.tracking.RaceTrackingConnectivityParameters;
import com.sap.sailing.domain.tracking.TrackedRegattaRegistry;
import com.sap.sailing.domain.tracking.WindStore;
import com.sap.sailing.server.RacingEventService;

public class RaceLogConnectivityParams implements RaceTrackingConnectivityParameters {
	private final RacingEventService service;
    private final RaceLog raceLog;
    private final RaceColumn raceColumn;
    private final Fleet fleet;
    private final Leaderboard leaderboard;
    private final long delayToLiveInMillis;
    private final Regatta regatta;

    public RaceLogConnectivityParams(RacingEventService service, Regatta regatta,
    		RaceLog raceLog, RaceColumn raceColumn, Fleet fleet, Leaderboard leaderboard, long delayToLiveInMillis)
    	throws RaceNotCreatedException {
    	this.service = service;
    	this.regatta = regatta;
        this.raceLog = raceLog;
        this.raceColumn = raceColumn;
        this.fleet = fleet;
        this.leaderboard = leaderboard;
        this.delayToLiveInMillis = delayToLiveInMillis;
        
        if (! new RaceLogTrackingStateAnalyzer(raceLog).analyze().isForTracking()) {
        	throw new RaceNotCreatedException(String.format("Racelog (%s) is not denoted for tracking", raceLog));
        }
    }

	@Override
	public RaceTracker createRaceTracker(
			TrackedRegattaRegistry trackedRegattaRegistry, WindStore windStore, GPSFixStore gpsFixStore) {
		return createRaceTracker(regatta, trackedRegattaRegistry, windStore, gpsFixStore);
	}

	@Override
	public RaceTracker createRaceTracker(Regatta regatta,
			TrackedRegattaRegistry trackedRegattaRegistry, WindStore windStore, GPSFixStore gpsFixStore) {
		if (regatta == null) {
			BoatClass boatClass = new RaceInformationFinder(raceLog).analyze().getB();
			regatta = service.getOrCreateDefaultRegatta("RaceLog-tracking default Regatta", boatClass.getName(), UUID.randomUUID());
		}
		if (regatta == null) {
			throw new RaceNotCreatedException("No regatta for race-log tracked race");
		}
		DynamicTrackedRegatta trackedRegatta = trackedRegattaRegistry.getOrCreateTrackedRegatta(regatta);
		return new RaceLogRaceTracker(trackedRegatta, this, windStore, gpsFixStore);
	}

    @Override
    public Object getTrackerID() {
        return raceLog.getId();
    }

    @Override
    public long getDelayToLiveInMillis() {
        return delayToLiveInMillis;
    }

    public RaceLog getRaceLog() {
        return raceLog;
    }

    public RaceColumn getRaceColumn() {
        return raceColumn;
    }

    public Fleet getFleet() {
        return fleet;
    }

    public Leaderboard getLeaderboard() {
        return leaderboard;
    }
}
