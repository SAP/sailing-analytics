package com.sap.sailing.aiagent.impl;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.base.impl.RaceColumnListenerWithDefaultAction;
import com.sap.sailing.domain.leaderboard.HasRaceColumns;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.tracking.TrackedRace;
import com.sap.sse.common.Util.Pair;

/**
 * Can be used to observe the {@link RaceColumn} structure of a {@link Leaderboard} and the {@link TrackedRace}s linked
 * to them and to ensure that all tracked races linked to any race column of the race column container have a
 * {@link RaceListener} observing it.
 * <p>
 * 
 * When race columns are added to the container observed, and those race columns already have tracked races linked, a
 * new {@link RaceListener} will be added to those tracked races. If a tracked race is connected to an existing race
 * column the {@link RaceListener} is added as a listener to that tracked race. Conversely, when race columns or tracked
 * races get removed/disconnected, a {@link RaceListener} registered for this tracked race earlier by thie
 * {@link RaceColumnListener} is unregistered as a listener from those objects again.
 * 
 * @author Axel Uhl (d043530)
 *
 */
public class RaceColumnListener implements RaceColumnListenerWithDefaultAction {
    private static final long serialVersionUID = -366609838503904875L;
    
    private final ConcurrentMap<Pair<RaceColumn, Fleet>, RaceListener> raceListeners;
    
    private final AIAgentImpl aiAgent;
    
    private final Leaderboard leaderboard;
    
    /**
     * {@link HasRaceColumns#addRaceColumnListener(com.sap.sailing.domain.base.RaceColumnListener) Registers} the new
     * object as a race column listener on {@code raceColumnContainer} and registers the {@code raceListener} on all
     * {@link TrackedRace}s found attached to any of the race columns in the container. When the container's race
     * columns change or tracked races get attached / detached to/from its race columns, this listener adjusts the
     * observer relationship of the {@code raceListener} on those tracked races.
     */
    public RaceColumnListener(Leaderboard leaderboard, final AIAgentImpl aiAgent) {
        this.aiAgent = aiAgent;
        this.raceListeners = new ConcurrentHashMap<>();
        this.leaderboard = leaderboard;
        leaderboard.addRaceColumnListener(this);
        leaderboard.getRaceColumns().forEach(raceColumn->raceColumn.getFleets().forEach((Fleet fleet)->Optional.ofNullable(raceColumn.getTrackedRace(fleet)).ifPresent(trackedRace->
            addNewRaceListenerToTrackedRace(leaderboard, aiAgent, raceColumn, fleet, trackedRace))));
    }

    /**
     * Marks this listener as "transient" (not to be serialized, e.g., during replication)
     */
    @Override
    public boolean isTransient() {
        return true;
    }

    private void addNewRaceListenerToTrackedRace(Leaderboard leaderboard, final AIAgentImpl aiAgent,
            RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        final RaceListener raceListener = new RaceListener(aiAgent, leaderboard, raceColumn, fleet, trackedRace);
        trackedRace.addListener(raceListener);
        raceListeners.put(new Pair<>(raceColumn, fleet), raceListener);
    }
    
    public void removeListener() {
        leaderboard.removeRaceColumnListener(this);
        leaderboard.getRaceColumns().forEach(raceColumn->raceColumn.getFleets().forEach((Fleet fleet)->Optional.ofNullable(raceColumn.getTrackedRace(fleet)).ifPresent(trackedRace->
            removeRaceListenerFromTrackedRace(raceColumn, fleet, trackedRace))));
    }

    @Override
    public void trackedRaceLinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        addNewRaceListenerToTrackedRace(leaderboard, aiAgent, raceColumn, fleet, trackedRace);
    }

    @Override
    public void trackedRaceUnlinked(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        removeRaceListenerFromTrackedRace(raceColumn, fleet, trackedRace);
    }

    private void removeRaceListenerFromTrackedRace(RaceColumn raceColumn, Fleet fleet, TrackedRace trackedRace) {
        final RaceListener raceListener = raceListeners.remove(new Pair<>(raceColumn, fleet));
        if (raceListener != null) {
            trackedRace.removeListener(raceListener);
        }
    }

    @Override
    public void raceColumnAddedToContainer(RaceColumn raceColumn) {
        raceColumn.getFleets().forEach((Fleet fleet)->Optional.ofNullable(raceColumn.getTrackedRace(fleet)).ifPresent(trackedRace->
            addNewRaceListenerToTrackedRace(leaderboard, aiAgent, raceColumn, fleet, trackedRace)));
    }

    @Override
    public void raceColumnRemovedFromContainer(RaceColumn raceColumn) {
        raceColumn.getFleets().forEach((Fleet fleet)->Optional.ofNullable(raceColumn.getTrackedRace(fleet)).ifPresent(trackedRace->
            removeRaceListenerFromTrackedRace(raceColumn, fleet, trackedRace)));
    }

    @Override
    public void defaultAction() {
    }
}
