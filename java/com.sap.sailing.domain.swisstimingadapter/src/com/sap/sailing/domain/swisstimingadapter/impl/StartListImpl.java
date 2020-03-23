package com.sap.sailing.domain.swisstimingadapter.impl;

import java.io.Serializable;
import java.util.ArrayList;

import com.sap.sailing.domain.swisstimingadapter.Competitor;
import com.sap.sailing.domain.swisstimingadapter.StartList;

public class StartListImpl implements StartList, Serializable {

    private static final long serialVersionUID = 8444785825340224L;
    private final String raceId;
    private final Iterable<Competitor> competitors;
    
    public StartListImpl(String raceId, Iterable<Competitor> competitors) {
        super();
        this.raceId = raceId;
        ArrayList<Competitor> l = new ArrayList<Competitor>();
        for (Competitor competitor : competitors) {
            l.add(competitor);
        }
        this.competitors = l;
    }

    @Override
    public String getRaceID() {
        return raceId;
    }

    @Override
    public Iterable<Competitor> getCompetitors() {
        return competitors;
    }

}
