package com.sap.sse.datamining.test.functions.registry.test_classes.impl;

import com.sap.sse.datamining.test.functions.registry.test_classes.Test_Boat;
import com.sap.sse.datamining.test.functions.registry.test_classes.Test_Competitor;
import com.sap.sse.datamining.test.functions.registry.test_classes.Test_Team;

public class Test_CompetitorImpl implements Test_Competitor {

    private Test_Team team;
    private Test_Boat boat;

    public Test_CompetitorImpl(Test_Team team, Test_Boat boat) {
        this.team = team;
        this.boat = boat;
    }

    @Override
    public Test_Team getTeam() {
        return team;
    }

    @Override
    public Test_Boat getBoat() {
        return boat;
    }

}
