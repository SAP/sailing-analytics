package com.sap.sailing.domain.leaderboard.meta;

import java.util.Collections;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.RaceColumnListener;
import com.sap.sailing.domain.base.impl.SimpleAbstractRaceColumn;
import com.sap.sailing.domain.common.RaceIdentifier;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * All {@link RaceColumnListener} events received from the underlying leaderboard's race columns
 * are forwarded to this object's {@link RaceColumnListener}s.
 * 
 * @author Axel Uhl
 *
 */
public class MetaLeaderboardColumn extends SimpleAbstractRaceColumn implements RaceColumn, RaceColumnListener {
    private static final long serialVersionUID = 3092096133388262955L;
    private final Leaderboard leaderboard;
    private final Fleet metaFleet;
    
    public MetaLeaderboardColumn(Leaderboard leaderboard, Fleet metaFleet) {
        super();
        this.leaderboard = leaderboard;
        this.metaFleet = metaFleet;
        leaderboard.addRaceColumnListener(this);
    }

    Leaderboard getLeaderboard() {
        return leaderboard;
    }
    
    @Override
    public String getName() {
        return leaderboard.getName();
    }

    @Override
    public void addRaceColumnListener(RaceColumnListener listener) {
    }

    @Override
    public void removeRaceColumnListener(RaceColumnListener listener) {
    }

    @Override
    public Iterable<? extends Fleet> getFleets() {
        return Collections.singleton(metaFleet);
    }

    @Override
    public Fleet getFleetByName(String fleetName) {
        return fleetName.equals(metaFleet.getName()) ? metaFleet : null;
    }

    @Override
    public Fleet getFleetOfCompetitor(Competitor competitor) {
        return metaFleet;
    }

    @Override
    public void setTrackedRace(Fleet fleet, TrackedRace race) {
    }

    @Override
    public boolean hasTrackedRaces() {
        return false;
    }

    @Override
    public TrackedRace getTrackedRace(Fleet fleet) {
        return null;
    }

    @Override
    public TrackedRace getTrackedRace(Competitor competitor) {
        return null;
    }

    @Override
    public RaceIdentifier getRaceIdentifier(Fleet fleet) {
        return null;
    }

    @Override
    public void setRaceIdentifier(Fleet fleet, RaceIdentifier raceIdentifier) {
    }

    @Override
    public boolean isMedalRace() {
        return false;
    }

    @Override
    public void releaseTrackedRace(Fleet fleet) {
    }

    @Override
    public void trackedRaceLinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        getRaceColumnListeners().notifyListenersAboutTrackedRaceLinked(raceColumn, fleet, trackedRace);
    }

    @Override
    public void trackedRaceUnlinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        getRaceColumnListeners().notifyListenersAboutTrackedRaceUnlinked(raceColumn, fleet, trackedRace);
    }

    @Override
    public void isMedalRaceChanged(RaceColumn raceColumn, boolean newIsMedalRace) {
        getRaceColumnListeners().notifyListenersAboutIsMedalRaceChanged(raceColumn, newIsMedalRace);
    }

    @Override
    public boolean canAddRaceColumnToContainer(RaceColumn raceColumn) {
        return getRaceColumnListeners().canAddRaceColumnToContainer(raceColumn);
    }

    @Override
    public void raceColumnAddedToContainer(RaceColumn raceColumn) {
        getRaceColumnListeners().notifyListenersAboutRaceColumnAddedToContainer(raceColumn);
    }

    @Override
    public void raceColumnRemovedFromContainer(RaceColumn raceColumn) {
        getRaceColumnListeners().notifyListenersAboutRaceColumnRemovedFromContainer(raceColumn);
    }

    @Override
    public void raceColumnMoved(RaceColumn raceColumn, int newIndex) {
        getRaceColumnListeners().notifyListenersAboutRaceColumnMoved(raceColumn, newIndex);
    }

    @Override
    public void factorChanged(RaceColumn raceColumn, Double oldFactor, Double newFactor) {
        getRaceColumnListeners().notifyListenersAboutFactorChanged(raceColumn, oldFactor, newFactor);
    }

    @Override
    public boolean isTransient() {
        return false;
    }
}
