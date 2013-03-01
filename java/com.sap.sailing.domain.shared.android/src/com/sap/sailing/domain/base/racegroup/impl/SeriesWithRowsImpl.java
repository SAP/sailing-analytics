package com.sap.sailing.domain.base.racegroup.impl;

import java.util.ArrayList;
import java.util.Collection;

import com.sap.sailing.domain.base.Fleet;
import com.sap.sailing.domain.base.racegroup.RaceRow;
import com.sap.sailing.domain.base.racegroup.SeriesWithRows;

public class SeriesWithRowsImpl implements SeriesWithRows {
    private static final long serialVersionUID = 8825402393444809944L;

    private String name;
    private Iterable<RaceRow> raceRows;
    private boolean isMedal;

    public SeriesWithRowsImpl(String name, boolean isMedal, Iterable<RaceRow> raceRows) {
        this.name = name;
        this.raceRows = raceRows;
        this.isMedal = isMedal;
    }

    public String getName() {
        return name;
    }

    public boolean isMedal() {
        return isMedal;
    }

    public Iterable<RaceRow> getRaceRows() {
        return raceRows;
    }

    public Iterable<? extends Fleet> getFleets() {
        Collection<Fleet> fleets = new ArrayList<Fleet>();
        for (RaceRow row : raceRows) {
            fleets.add(row.getFleet());
        }
        return fleets;
    }

}
