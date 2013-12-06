package com.sap.sailing.domain.test;

import java.util.Collections;

import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;

public abstract class AbstractLeaderboardTest {
    public static CompetitorImpl createCompetitor(String competitorName) {
        return new CompetitorImpl(123, competitorName, "#FF0000", new TeamImpl("STG", Collections.singleton(
                new PersonImpl(competitorName, new NationalityImpl("GER"),
                /* dateOfBirth */ null, "This is famous "+competitorName)),
                new PersonImpl("Rigo van Maas", new NationalityImpl("NED"),
                /* dateOfBirth */null, "This is Rigo, the coach")), new BoatImpl(competitorName + "'s boat",
                new BoatClassImpl("505", /* typicallyStartsUpwind */ true), /* sailID */ null));
    }
}
