package com.sap.sailing.domain.leaderboard.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.common.Named;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.impl.Util;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.RaceInLeaderboard;
import com.sap.sailing.domain.leaderboard.ScoreCorrection.MaxPointsReason;
import com.sap.sailing.domain.leaderboard.ScoreCorrection.Result;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.tracking.TrackedRace;

public class LeaderboardImpl implements Named, Leaderboard {
    private final List<RaceInLeaderboard> races;
    private final SettableScoreCorrection scoreCorrection;
    private ThresholdBasedResultDiscardingRule resultDiscardingRule;
    private String name;
    
    /**
     * The optional display name mappings for competitors. This allows a user to override the tracking-provided
     * competitor names for display in a leaderboard.
     */
    private final Map<Competitor, String> displayNames;
    
    /**
     * Backs the {@link #getCarriedPoints(Competitor)} API with data. Can be used to prime this leaderboard
     * with aggregated results of races not tracked / displayed by this leaderboard in detail. The points
     * provided by this map are considered by {@link #getTotalPoints(Competitor, TimePoint)}.
     */
    private final Map<Competitor, Integer> carriedPoints;
    
    /**
     * A leaderboard entry representing a snapshot of a cell at a given time point for a single race/competitor.
     * 
     * @author Axel Uhl (d043530)
     *
     */
    private class EntryImpl implements Entry {
        private final int trackedPoints;
        private final int netPoints;
        private final int totalPoints;
        private final MaxPointsReason maxPointsReason;
        private final boolean discarded;
        private EntryImpl(int trackedPoints, int netPoints, int totalPoints, MaxPointsReason maxPointsReason, boolean discarded) {
            super();
            this.trackedPoints = trackedPoints;
            this.netPoints = netPoints;
            this.totalPoints = totalPoints;
            this.maxPointsReason = maxPointsReason;
            this.discarded = discarded;
        }
        @Override
        public int getTrackedPoints() {
            return trackedPoints;
        }
        @Override
        public int getNetPoints() {
            return netPoints;
        }
        @Override
        public int getTotalPoints() {
            return totalPoints;
        }
        @Override
        public MaxPointsReason getMaxPointsReason() {
            return maxPointsReason;
        }
        @Override
        public boolean isDiscarded() {
            return discarded;
        }
    }

    /**
     * @param name must not be <code>null</code>
     */
    public LeaderboardImpl(String name, SettableScoreCorrection scoreCorrection, ThresholdBasedResultDiscardingRule resultDiscardingRule) {
        if (name == null) {
            throw new IllegalArgumentException("A leaderboard's name must not be null");
        }
        this.name = name;
        this.carriedPoints = new HashMap<Competitor, Integer>();
        this.races = new ArrayList<RaceInLeaderboard>();
        this.scoreCorrection = scoreCorrection;
        this.displayNames = new HashMap<Competitor, String>();
        this.resultDiscardingRule = resultDiscardingRule;
    }
    
    @Override
    public RaceInLeaderboard addRaceColumn(String name, boolean medalRace) {
        RaceInLeaderboardImpl column = createRaceColumn(name, medalRace);
        races.add(column);
        return column;
    }
    
    @Override
    public void removeRaceColumn(String columnName) {
        races.remove(getRaceColumnByName(columnName));
    }
    
    @Override
    public Iterable<RaceInLeaderboard> getRaceColumns() {
        return Collections.unmodifiableCollection(new ArrayList<RaceInLeaderboard>(races));
    }
    
    @Override
    public RaceInLeaderboard getRaceColumnByName(String columnName) {
        RaceInLeaderboard result = null;
        for (RaceInLeaderboard r : races) {
            if (r.getName().equals(columnName)) {
                result = r;
                break;
            }
        }
        return result;
    }
    
    @Override
    public RaceInLeaderboard addRace(TrackedRace race, String columnName, boolean medalRace) {
        RaceInLeaderboard column = getRaceColumnByName(columnName);
        if (column == null) {
            column = createRaceColumn(columnName, medalRace);
            column.setTrackedRace(race);
            column.setRaceIdentifier(race.getRaceIdentifier());
            races.add(column);
        }
        column.setTrackedRace(race);
        return column;
    }

    protected RaceInLeaderboardImpl createRaceColumn(String columnName, boolean medalRace) {
        return new RaceInLeaderboardImpl(this, columnName, medalRace);
    }

    private Iterable<TrackedRace> getTrackedRaces() {
        Set<TrackedRace> trackedRaces = new HashSet<TrackedRace>();
        for (RaceInLeaderboard r : races) {
            TrackedRace trackedRace = r.getTrackedRace();
            if (trackedRace != null) {
                trackedRaces.add(trackedRace);
            }
        }
        return Collections.unmodifiableSet(trackedRaces);
    }

    @Override
    public Iterable<Competitor> getCompetitors() {
        Set<Competitor> result = new HashSet<Competitor>();
        for (TrackedRace r : getTrackedRaces()) {
            for (Competitor c : r.getRace().getCompetitors()) {
                result.add(c);
            }
        }
        return result;
    }
    
    @Override
    public Competitor getCompetitorByName(String competitorName) {
        for (Competitor competitor : getCompetitors()) {
            if (competitor.getName().equals(competitorName)) {
                return competitor;
            }
        }
        return null;
    }
    
    @Override
    public Competitor getCompetitorByID(Object id) {
        for (Competitor competitor : getCompetitors()) {
            if (competitor.getId().equals(id)) {
                return competitor;
            }
        }
        return null;
    }

    @Override
    public SettableScoreCorrection getScoreCorrection() {
        return scoreCorrection;
    }
    
    @Override
    public ThresholdBasedResultDiscardingRule getResultDiscardingRule() {
        return resultDiscardingRule;
    }

    @Override
    public int getTrackedPoints(Competitor competitor, RaceInLeaderboard race, TimePoint timePoint) throws NoWindException {
        return race.getTrackedRace() == null ? 0 : race.getTrackedRace().hasStarted(timePoint) ? race.getTrackedRace()
                .getRank(competitor, timePoint) : 0;
    }

    @Override
    public int getNetPoints(Competitor competitor, RaceInLeaderboard raceColumn, TimePoint timePoint) throws NoWindException {
        return getScoreCorrection().getCorrectedScore(getTrackedPoints(competitor, raceColumn, timePoint), competitor,
                raceColumn, timePoint, Util.size(getCompetitors())).getCorrectedScore();
    }
    
    
    @Override
    public MaxPointsReason getMaxPointsReason(Competitor competitor, RaceInLeaderboard raceColumn, TimePoint timePoint)
            throws NoWindException {
        return raceColumn.getTrackedRace() == null ? MaxPointsReason.NONE : getScoreCorrection().getCorrectedScore(
                getTrackedPoints(competitor, raceColumn, timePoint), competitor, raceColumn, timePoint, Util.size(getCompetitors()))
                .getMaxPointsReason();
    }
    
    @Override
    public boolean isDiscarded(Competitor competitor, RaceInLeaderboard raceColumn, TimePoint timePoint) {
        return !raceColumn.isMedalRace()
                && getResultDiscardingRule().getDiscardedRaceColumns(competitor, this, timePoint).contains(
                        raceColumn);
    }

    @Override
    public int getTotalPoints(Competitor competitor, RaceInLeaderboard raceColumn, TimePoint timePoint) throws NoWindException {
        return isDiscarded(competitor, raceColumn, timePoint) ?
                0 :
                (raceColumn.isMedalRace() ? 2 : 1) * getNetPoints(competitor, raceColumn, timePoint);
    }
    
    @Override
    public int getTotalPoints(Competitor competitor, TimePoint timePoint) throws NoWindException {
        int result = getCarriedPoints(competitor);
        for (RaceInLeaderboard r : getRaceColumns()) {
            result += getTotalPoints(competitor, r, timePoint);
        }
        return result;
    }

    @Override
    public Entry getEntry(Competitor competitor, RaceInLeaderboard race, TimePoint timePoint) throws NoWindException {
        int trackedPoints = getTrackedPoints(competitor, race, timePoint);
        final Result correctedResults = getScoreCorrection().getCorrectedScore(trackedPoints, competitor, race,
                timePoint, Util.size(getCompetitors()));
        boolean discarded = isDiscarded(competitor, race, timePoint);
        return new EntryImpl(trackedPoints, correctedResults.getCorrectedScore(), discarded ? 0
                : correctedResults.getCorrectedScore() * (race.isMedalRace() ? 2 : 1),
                correctedResults.getMaxPointsReason(), discarded);
    }
    
    @Override
    public Map<Pair<Competitor, RaceInLeaderboard>, Entry> getContent(TimePoint timePoint) throws NoWindException {
        Map<Pair<Competitor, RaceInLeaderboard>, Entry> result = new HashMap<Pair<Competitor, RaceInLeaderboard>, Entry>();
        Map<Competitor, Set<RaceInLeaderboard>> discardedRaces = new HashMap<Competitor, Set<RaceInLeaderboard>>();
        for (RaceInLeaderboard raceColumn : getRaceColumns()) {
            for (Competitor competitor : getCompetitors()) {
                int trackedPoints;
                if (raceColumn.getTrackedRace() != null && raceColumn.getTrackedRace().hasStarted(timePoint)) {
                    trackedPoints = raceColumn.getTrackedRace().getRank(competitor, timePoint);
                } else {
                    trackedPoints = 0;
                }
                Result correctedResults = getScoreCorrection().getCorrectedScore(trackedPoints, competitor, raceColumn,
                        timePoint, Util.size(getCompetitors()));
                Set<RaceInLeaderboard> discardedRacesForCompetitor = discardedRaces.get(competitor);
                if (discardedRacesForCompetitor == null) {
                    discardedRacesForCompetitor = getResultDiscardingRule().getDiscardedRaceColumns(competitor, this, timePoint);
                    discardedRaces.put(competitor, discardedRacesForCompetitor);
                }
                boolean discarded = discardedRacesForCompetitor.contains(raceColumn);
                Entry entry = new EntryImpl(trackedPoints, correctedResults.getCorrectedScore(),
                        discarded ? 0 : correctedResults.getCorrectedScore() * (raceColumn.isMedalRace() ? 2 : 1),
                                correctedResults.getMaxPointsReason(), discarded);
                result.put(new Pair<Competitor, RaceInLeaderboard>(competitor, raceColumn), entry);
            }
        }
        return result;
    }

    @Override
    public String getName() {
        return name;
    }

    /**
     * @param newName must not be <code>null</code>
     */
    public void setName(String newName) {
        if (newName == null) {
            throw new IllegalArgumentException("A leaderboard's name must not be null");
        }
        this.name = newName;
    }
    
    @Override
    public void setCarriedPoints(Competitor competitor, int carriedPoints) {
        this.carriedPoints.put(competitor, carriedPoints);
    }

    @Override
    public int getCarriedPoints(Competitor competitor) {
        Integer result = carriedPoints.get(competitor);
        return result == null ? 0 : result;
    }

    @Override
    public void unsetCarriedPoints(Competitor competitor) {
        carriedPoints.remove(competitor);
    }

    @Override
    public boolean hasCarriedPoints() {
        return !carriedPoints.isEmpty();
    }
    
    @Override
    public boolean hasCarriedPoints(Competitor competitor) {
        return carriedPoints.containsKey(competitor);
    }

    @Override
    public boolean considerForDiscarding(RaceInLeaderboard raceInLeaderboard, TimePoint timePoint) {
        return !raceInLeaderboard.isMedalRace()
                && (raceInLeaderboard.getTrackedRace() != null && raceInLeaderboard.getTrackedRace().hasStarted(
                        timePoint)) || getScoreCorrection().hasCorrectionFor(raceInLeaderboard);
    }

    @Override
    public String getDisplayName(Competitor competitor) {
        return displayNames.get(competitor);
    }
    
    @Override
    public void setDisplayName(Competitor competitor, String displayName) {
        displayNames.put(competitor, displayName);
    }

	@Override
	public void moveRaceColumnUp(String name) {
		RaceInLeaderboard race = null;
		for (RaceInLeaderboard r : races){
			if (r.getName().equals(name))
				race = r;
		}
		if (race == null)
			return;
		int index = 0;
		index = races.lastIndexOf(race);
		index--;
		if (index >= 0){
			races.remove(race);
			races.add(index, race);
		}
	}

	@Override
	public void moveRaceColumnDown(String name) {
		RaceInLeaderboard race = null;
		for (RaceInLeaderboard r : races){
			if (r.getName().equals(name))
				race = r;
		}
		if (race == null)
			return;
		int index = 0;
		index = races.lastIndexOf(race);
		if (index == -1)
			return;
		index++;
		if (index < races.size()){
			races.remove(race);
			races.add(index, race);
		}
	}

	@Override
	public void updateIsMedalRace(String raceName, boolean isMedalRace) {
		RaceInLeaderboard race = null;
		for (RaceInLeaderboard r : races){
			if (r.getName().equals(raceName))
				race = r;
		}
		if (race == null)
			return;
		
		race.setIsMedalRace(isMedalRace);
	}

	@Override
	public void setResultDiscardingRule(ThresholdBasedResultDiscardingRule discardingRule) {
	    this.resultDiscardingRule = discardingRule; 
	}
}
