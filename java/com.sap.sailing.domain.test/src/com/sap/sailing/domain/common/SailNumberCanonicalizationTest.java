package com.sap.sailing.domain.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.CompetitorWithBoat;
import com.sap.sailing.domain.base.SailNumberCanonicalizerAndMatcher;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorWithBoatImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sse.common.Color;

public class SailNumberCanonicalizationTest {
    private SailNumberCanonicalizerAndMatcher canonicalizer = new SailNumberCanonicalizerAndMatcher();
    
    @Test
    public void testSimpleSailNumberMatching() {
        assertEquals("GER123", canonicalizer.canonicalizeSailID("GER 123", /* defaultNationality */ null));
    }
    
    @Test
    public void testSailNumberWithoutNationalityMatching() {
        assertEquals("117911", canonicalizer.canonicalizeSailID("117911", /* defaultNationality */ null));
    }
    
    @Test
    public void testSailNumberWithoutNationalityMatchingAgainstCompetitor() {
        final CompetitorWithBoat competitor = new CompetitorWithBoatImpl(UUID.randomUUID(), "TestCompetitor",
                "TC", Color.BLACK, /* email */ null, /* flagImage */ null,
                new TeamImpl("TestTeam",
                        Collections.singleton(new PersonImpl("TestPerson", new NationalityImpl("NED"),
                                /* dateOfBirth */ null,
                                /* description */ null)),
                        /* coach */ null), /* timeOnTimeFactor */ 1.0,
                /* timeOnDistanceAllowancePerNauticalMile */ null, /* searchTag */ null,
                new BoatImpl(UUID.randomUUID(), "TestBoat", new BoatClassImpl(BoatClassMasterdata._12M), "117911"));
        final Map<String, Competitor> map = canonicalizer
                .canonicalizeLeaderboardSailIDs(Collections.singleton(competitor));
        assertSame(competitor, map.get("117911"));
    }
}
