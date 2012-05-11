package com.sap.sailing.domain.leaderboard;

import java.util.Map;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.MaxPointsReason;
import com.sap.sailing.domain.common.Named;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * A leaderboard is used to display the results of one or more {@link TrackedRace races}. It manages the competitors'
 * scores and can aggregate them, e.g., to show the overall regatta standings. In addition to the races, a "carry"
 * column may be used to carry results of races not displayed in the leaderboard into the calculations.
 * <p>
 * 
 * While a single {@link TrackedRace} can tell about the ranks in which according to the tracking information the
 * competitors crossed the finish line, the leaderboard may overlay this information with disqualifications, changes in
 * results because the finish-line tracking was inaccurate, jury penalties and discarded results (depending on the
 * regatta rules, the worst zero, one or more races of each competitor are discarded from the aggregated points).
 * <p>
 * 
 * @author Axel Uhl (d043530)
 * 
 */
public interface Leaderboard extends Named {
    /**
     * If the leaderboard is a "matrix" with the cells being defined by a competitor / race "coordinate,"
     * then this interface defines the structure of the "cells."
     * 
     * @author Axel Uhl (d043530)
     *
     */
    public interface Entry {
        int getTrackedPoints();
        int getNetPoints() throws NoWindException;
        int getTotalPoints() throws NoWindException;
        MaxPointsReason getMaxPointsReason();
        boolean isDiscarded() throws NoWindException;
        /**
         * Tells if the net points have been corrected by a {@link ScoreCorrection}
         */
        boolean isNetPointsCorrected();
    }
    
    /**
     * Obtains the unique set of {@link Competitor} objects from all {@link TrackedRace}s currently linked to this
     * leaderboard.
     */
    Iterable<Competitor> getCompetitors();
    
    Entry getEntry(Competitor competitor, RaceInLeaderboard race, TimePoint timePoint) throws NoWindException;
    
    /**
     * Tells the number of points carried over from previous races not tracked by this leaderboard for
     * the <code>competitor</code>. Returns <code>0</code> if there is no carried points definition for
     * <code>competitor</code>.
     */
    int getCarriedPoints(Competitor competitor);

    /**
     * Shorthand for {@link TrackedRace#getRank(Competitor, com.sap.sailing.domain.common.TimePoint)} with the
     * additional logic that in case the <code>race</code> hasn't {@link TrackedRace#hasStarted(TimePoint) started}
     * yet, 0 points will be allotted to the race for all competitors.
     * 
     * @param competitor
     *            a competitor contained in the {@link #getCompetitors()} result
     * @param race
     *            a race that is contained in the {@link #getRaceColumns()} result
     */
    int getTrackedPoints(Competitor competitor, RaceInLeaderboard race, TimePoint timePoint) throws NoWindException;

    /**
     * A possibly corrected number of points for the race specified. Defaults to the result of calling
     * {@link #getTrackedPoints(Competitor, TrackedRace, TimePoint)} but may be corrected by disqualifications or calls
     * by the jury for the particular race that differ from the tracking results.
     * 
     * @param competitor
     *            a competitor contained in the {@link #getCompetitors()} result
     * @param race
     *            a race that is contained in the {@link #getRaceColumns()} result
     */
    int getNetPoints(Competitor competitor, RaceInLeaderboard race, TimePoint timePoint) throws NoWindException;

    /**
     * Tells if and why a competitor received maximum points for a race.
     */
    MaxPointsReason getMaxPointsReason(Competitor competitor, RaceInLeaderboard race, TimePoint timePoint) throws NoWindException;

    /**
     * A possibly corrected number of points for the race specified. Defaults to the result of calling
     * {@link #getNetPoints(Competitor, TrackedRace, TimePoint)} but may be corrected by the regatta
     * rules for discarding results.
     * 
     * @param competitor
     *            a competitor contained in the {@link #getCompetitors()} result
     * @param race
     *            a race that is contained in the {@link #getRaceColumns()} result
     */
    int getTotalPoints(Competitor competitor, RaceInLeaderboard race, TimePoint timePoint) throws NoWindException;

    /**
     * Tells whether the contribution of <code>raceColumn</code> is discarded in the current leaderboard's
     * standings for <code>competitor</code>. A column representing a {@link RaceInLeaderboard#isMedalRace() medal race}
     * cannot be discarded.
     */
    boolean isDiscarded(Competitor competitor, RaceInLeaderboard raceColumn, TimePoint timePoint);

    /**
     * Adds a tracked race to this leaderboard. If a {@link RaceInLeaderboard} with name <code>columnName</code> already
     * exists in this leaderboard, <code>race</code> is {@link RaceInLeaderboard#setTrackedRace(TrackedRace) set as its
     * tracked race} and <code>medalRace</code> is ignored. Otherwise, a new
     * {@link RaceInLeaderboard} column, with <code>race</code> as its tracked race, is created and added to this
     * leaderboard.
     * 
     * @param medalRace
     *            tells if the column to add represents a medal race which has double score and cannot be discarded;
     *            ignored if the column named <code>columnName</code> already exists
     * 
     * @return the race column in the leaderboard used to represent the tracked <code>race</code>
     */
    RaceInLeaderboard addRace(TrackedRace race, String columnName, boolean medalRace);

    /**
     * Sums up the {@link #getTotalPoints(Competitor, TrackedRace, TimePoint) total points} of <code>competitor</code>
     * across all races tracked by this leaderboard.
     */
    int getTotalPoints(Competitor competitor, TimePoint timePoint) throws NoWindException;

    /**
     * Fetches all entries for all competitors of all races tracked by this leaderboard in one sweep. This saves some
     * computational effort compared to fetching all entries separately, particularly because all
     * {@link #isDiscarded(Competitor, RaceInLeaderboard, TimePoint) discarded races} of a competitor are computed in one
     * sweep using {@link ResultDiscardingRule#getDiscardedRaceColumns(Competitor, Leaderboard, TimePoint)} only once.
     * Note that in order to get the {@link #getTotalPoints(Competitor, TimePoint) total points} for a competitor
     * for the entire leaderboard, the {@link #getCarriedPoints(Competitor) carried-over points} need to be added.
     */
    Map<Pair<Competitor, RaceInLeaderboard>, Entry> getContent(TimePoint timePoint) throws NoWindException;

    /**
     * A leaderboard can be renamed. If a leaderboard is managed in a structure that keys leaderboards by name,
     * that structure's rules have to be obeyed to ensure the structure's consistency. For example,
     * <code>RacingEventService</code> has a <code>renameLeaderboard</code> method that ensures the internal
     * structure's consistency and invokes this method.
     */
    void setName(String newName);

    /**
     * Adds a new {@link RaceInLeaderboard} that has no {@link TrackedRace} associated yet to this leaderboard.
     * @param medalRace
     *            tells if the column to add represents a medal race which has double score and cannot be discarded
     * 
     * @return the race column in the leaderboard used to represent the tracked <code>race</code>
     */
    RaceInLeaderboard addRaceColumn(String name, boolean medalRace);
    
    /**
     * Retrieves all race columns that were added, either by {@link #addRace(TrackedRace, String, boolean)} or
     * {@link #addRaceColumn(String, boolean)}.
     */
    Iterable<RaceInLeaderboard> getRaceColumns();
    
    /**
     * Retrieves a {@link RaceInLeaderboard race column} by the name used in calls to either {@link #addRaceColumn} or
     * {@link #addRace}. If no race column by the requested <code>name</code> exists, <code>null</code> is returned.
     */
    RaceInLeaderboard getRaceColumnByName(String name);
    
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
     * A leaderboard can carry over points from races that are not tracked by this leaderboard in detail,
     * so for which no {@link RaceInLeaderboard} column is present in this leaderboard. These scores are
     * simply added to the scores tracked by this leaderboard in the {@link #getTotalPoints(Competitor, TimePoint)}
     * method.
     */
    void setCarriedPoints(Competitor competitor, int carriedPoints);
    
    /**
     * Reverses the effect of {@link #setCarriedPoints(Competitor, int)}, i.e., afterwards, asking {@link #getCarriedPoints(Competitor)}
     * will return <code>0</code>. Furthermore, other than invoking {@link #setCarriedPoints(Competitor, int) setCarriedPoints(c, 0)},
     * this will, when executed for all competitors of this leaderboard, have {@link #hasCarriedPoints} return <code>false</code>.
     */
    void unsetCarriedPoints(Competitor competitor);
    
    /**
     * Tells if a carry-column shall be displayed. If the result is <code>false</code>, then no
     * {@link #setCarriedPoints(Competitor, int) scores are carried} into this leaderboard, and
     * only the race columns will be accumulated by the board.
     */
    boolean hasCarriedPoints();
    
    boolean hasCarriedPoints(Competitor competitor);

    void removeRaceColumn(String columnName);

    SettableScoreCorrection getScoreCorrection();

    ThresholdBasedResultDiscardingRule getResultDiscardingRule();

    Competitor getCompetitorByName(String competitorName);
    
    void setDisplayName(Competitor competitor, String displayName);

    /**
     * If a display name different from the competitor's {@link Competitor#getName() name} has been defined,
     * this method returns it; otherwise, <code>null</code> is returned.
     */
    String getDisplayName(Competitor competitor);
    
    /**
     * Tells if the column represented by <code>raceInLeaderboard</code> shall be considered for discarding.
     * Medal races are never considered for discarding (not counted as a "started race" nor discarded themselves).
     * If a leaderboard has corrections for a column then that column shall be considered for discarding and counts
     * for determining the number of races so far. Also, if a tracked race is connected to the column and has
     * started already, the column is to be considered for discarding. 
     * @param timePoint TODO
     */
    boolean considerForDiscarding(RaceInLeaderboard raceInLeaderboard, TimePoint timePoint);
    
    void updateIsMedalRace(String raceName, boolean isMedalRace);
    
    public void setResultDiscardingRule(ThresholdBasedResultDiscardingRule discardingRule);

    Competitor getCompetitorByIdAsString(String idAsString);
}
