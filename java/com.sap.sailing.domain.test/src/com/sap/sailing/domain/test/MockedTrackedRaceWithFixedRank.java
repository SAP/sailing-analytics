package com.sap.sailing.domain.test;

import java.util.Collections;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.RaceDefinition;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.test.mock.MockedTrackedRace;

public class MockedTrackedRaceWithFixedRank extends MockedTrackedRace {
    private final int rank;
    private final boolean started;
    private final RaceDefinition raceDefinition;
    private final Competitor competitor;
    
    public MockedTrackedRaceWithFixedRank(Competitor competitor, int rank, boolean started) {
        this.rank = rank;
        this.started = started;
        this.competitor = competitor;
        this.raceDefinition = new RaceDefinition() {
            private static final long serialVersionUID = 6812543850545870357L;
            @Override
            public String getName() {
                return null;
            }
            @Override
            public Course getCourse() {
                return null;
            }
            @Override
            public Iterable<Competitor> getCompetitors() {
                return Collections.singleton(MockedTrackedRaceWithFixedRank.this.competitor);
            }
            @Override
            public BoatClass getBoatClass() {
                return null;
            }
        };

    }

    @Override
    public boolean hasStarted(TimePoint at) {
        return started;
    }

    @Override
    public int getRank(Competitor competitor, TimePoint timePoint) throws NoWindException {
        return rank;
    }

    @Override
    public int getRank(Competitor competitor) throws NoWindException {
        return rank;
    }

    @Override
    public RaceDefinition getRace() {
        return raceDefinition;
    }
}
