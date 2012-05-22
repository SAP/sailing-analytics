package com.sap.sailing.domain.leaderboard;

import com.sap.sailing.domain.base.Fleet;

import com.sap.sailing.domain.base.Series;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * A leaderboard that allows its clients to flexibly modify the race columns arranged in this leaderboard without the need to adhere
 * to the constraints of a {@link Regatta} with its {@link Series} and {@link Fleet}s.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public interface FlexibleLeaderboard extends Leaderboard {
    /**
     * Moves the column with the name <code>name</code> up. 
     * @param name The name of the column to move.
     */
    void moveRaceColumnUp(String name);
    
    /**
     * Moves the column with the name <code>name</code> down. 
     * @param name The name of the column to move.
     */
    void moveRaceColumnDown(String name);

    /**
     * Adds a new {@link RaceColumn} that has no {@link TrackedRace} associated yet to this leaderboard.
     * 
     * @param name
     *            the name for the new race column such that none of the columns in {@link #getRaceColumns()}
     *            has that name yet; otherwise, an message will be logged, no race column will be added and
     *            the existing column will be returned for robustness reasons
     * @param medalRace
     *            tells if the column to add represents a medal race which has double score and cannot be discarded
     * @param fleets
     *            the fleets to add to the {@link RaceColumn} created. If no fleets are specified, a single default
     *            fleet will be assigned to the race column created.
     * 
     * @return the race column in the leaderboard used to represent the tracked <code>race</code>
     */
    RaceColumn addRaceColumn(String name, boolean medalRace, Fleet... fleets);
    
    /**
     * Adds a tracked race to this leaderboard. If a {@link RaceColumn} with name <code>columnName</code> already exists
     * in this leaderboard, <code>race</code> is {@link RaceColumn#setTrackedRace(Fleet, TrackedRace) set as its tracked
     * race} and <code>medalRace</code> is ignored. Otherwise, a new {@link RaceColumn} column, with <code>race</code>
     * as its tracked race, is created and added to this leaderboard.
     * 
     * @param medalRace
     *            tells if the column to add represents a medal race which has double score and cannot be discarded;
     *            ignored if the column named <code>columnName</code> already exists
     * 
     * @param fleets
     *            the fleets to add to the {@link RaceColumn} created. If no fleets are specified, a single default
     *            fleet will be assigned to the race column created.
     * 
     * @return the race column in the leaderboard used to represent the tracked <code>race</code>
     */
    RaceColumn addRace(TrackedRace race, String columnName, boolean medalRace, Fleet fleet);

    void removeRaceColumn(String columnName);

    void updateIsMedalRace(String raceName, boolean isMedalRace);
    
}
