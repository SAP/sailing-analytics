package com.sap.sailing.domain.leaderboard.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.impl.FleetImpl;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.RaceColumn;
import com.sap.sailing.domain.leaderboard.SettableScoreCorrection;
import com.sap.sailing.domain.leaderboard.ThresholdBasedResultDiscardingRule;
import com.sap.sailing.domain.tracking.TrackedRace;

/**
 * A leaderboard implementation that allows users to flexibly configure which columns exist. No constraints need to be observed regarding
 * the columns belonging to the same regatta or even boat class.
 * 
 * @author Axel Uhl (D043530)
 *
 */
public class FlexibleLeaderboardImpl extends AbstractLeaderboardImpl implements FlexibleLeaderboard {
    protected static final Fleet defaultFleet = new FleetImpl("Default");
    private static final long serialVersionUID = -5708971849158747846L;
    private final List<RaceColumn> races;

    public FlexibleLeaderboardImpl(String name, SettableScoreCorrection scoreCorrection,
            ThresholdBasedResultDiscardingRule resultDiscardingRule) {
        super(name, scoreCorrection, resultDiscardingRule);
        this.races = new ArrayList<RaceColumn>();
    }
    
    @Override
    public RaceColumn addRaceColumn(String name, boolean medalRace, Fleet... fleets) {
        RaceColumnImpl column = createRaceColumn(name, medalRace, fleets);
        races.add(column);
        return column;
    }
    
    @Override
    public Fleet getFleet(String fleetName) {
        Fleet result;
        if (fleetName == null) {
            result = defaultFleet;
        } else {
            result = super.getFleet(fleetName);
        }
        return result;
    }

    @Override
    public void removeRaceColumn(String columnName) {
        races.remove(getRaceColumnByName(columnName));
    }
    
    @Override
    public Iterable<RaceColumn> getRaceColumns() {
        return Collections.unmodifiableCollection(new ArrayList<RaceColumn>(races));
    }
    
    @Override
    public RaceColumn addRace(TrackedRace race, String columnName, boolean medalRace, Fleet fleet) {
        RaceColumn column = getRaceColumnByName(columnName);
        if (column == null) {
            column = createRaceColumn(columnName, medalRace);
            races.add(column);
        }
        column.setTrackedRace(fleet, race);
        return column;
    }

    protected RaceColumnImpl createRaceColumn(String columnName, boolean medalRace, Fleet... fleets) {
        Iterable<Fleet> theFleets;
        if (fleets == null || fleets.length == 0) {
            theFleets = Collections.singleton(defaultFleet);
        } else {
            theFleets = Arrays.asList(fleets);
        }
        return new RaceColumnImpl(columnName, medalRace, theFleets);
    }

    @Override
    public void moveRaceColumnUp(String name) {
        RaceColumn race = null;
        for (RaceColumn r : races) {
            if (r.getName().equals(name)) {
                race = r;
            }
        }
        if (race == null) {
            return;
        }
        int index = 0;
        index = races.lastIndexOf(race);
        index--;
        if (index >= 0) {
            races.remove(race);
            races.add(index, race);
        }
    }

    @Override
    public void moveRaceColumnDown(String name) {
        RaceColumn race = null;
        for (RaceColumn r : races) {
            if (r.getName().equals(name)) {
                race = r;
            }
        }
        if (race == null) {
            return;
        }
        int index = 0;
        index = races.lastIndexOf(race);
        if (index == -1) {
            return;
        }
        index++;
        if (index < races.size()) {
            races.remove(race);
            races.add(index, race);
        }
    }

}
