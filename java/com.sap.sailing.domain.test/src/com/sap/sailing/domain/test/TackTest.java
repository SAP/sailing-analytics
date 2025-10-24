package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.Boat;
import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.BoatClassImpl;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.NoWindException;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.common.Wind;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.tracking.DynamicGPSFixTrack;
import com.sap.sse.common.Color;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;


public class TackTest extends StoredTrackBasedTestWithTrackedRace {
    private Competitor competitor;
    
    @Override
    @BeforeEach
    public void setUp() {
        BoatClass boatClass = new BoatClassImpl("505", /* typicallyStartsUpwind */ true);
        competitor = new CompetitorImpl(123, "Wolfgang Hunger", "KYC", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                                new PersonImpl("Wolfgang Hunger", new NationalityImpl("GER"),
                                /* dateOfBirth */null, "This is famous Wolfgang Hunger")), new PersonImpl("Rigo van Maas",
                                        new NationalityImpl("NED"),
                                        /* dateOfBirth */null, "This is Rigo, the coach")),
                                /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        Boat boat = new BoatImpl(competitor.getId(), "Wolfgang Hunger's boat", boatClass, "123", null);
        Map<Competitor,Boat> competitorsAndBoats = new HashMap<>();
        competitorsAndBoats.put(competitor, boat);
        setTrackedRace(createTestTrackedRace("Kieler Woche", "505 Race 2", "505", competitorsAndBoats,
                new MillisecondsTimePoint(new GregorianCalendar(2011, 05, 23).getTime()), /* useMarkPassingCalculator */ false));
    }
    
    @Test
    public void testWindRoughlyFromNorth() {
        // as the upwind leg points roughtly north, the wind is expected to come roughly from the north
        Wind wind = getTrackedRace().getWind(getTrackedRace().getOrCreateTrack(getTrackedRace().getRace().getCourse().getFirstWaypoint().getMarks().iterator().next()).
                getEstimatedPosition(MillisecondsTimePoint.now(), /* extrapolate */ false),
                MillisecondsTimePoint.now());
        assertEquals(0., wind.getFrom().getDifferenceTo(new DegreeBearingImpl(0)).getDegrees(), 1.);
    }
    
    @Test
    public void testStarboardTack() throws NoWindException {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> hassosTrack = getTrackedRace().getTrack(competitor);
        TimePoint now = MillisecondsTimePoint.now();
        hassosTrack.addGPSFix(new GPSFixMovingImpl(new DegreePosition(54.4680424, 10.234451), now,
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(45)), /* optionalTrueHeading */ null));
        assertEquals(Tack.PORT, getTrackedRace().getTack(competitor, now));
    }

    @Test
    public void testPortTack() throws NoWindException {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> hassosTrack = getTrackedRace().getTrack(competitor);
        TimePoint now = MillisecondsTimePoint.now();
        hassosTrack.addGPSFix(new GPSFixMovingImpl(new DegreePosition(54.4680424, 10.234451), now,
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(270)), /* optionalTrueHeading */ null));
        assertEquals(Tack.STARBOARD, getTrackedRace().getTack(competitor, now));
    }

    @Test
    public void testStarboardTackForZeroDifference() throws NoWindException {
        DynamicGPSFixTrack<Competitor, GPSFixMoving> hassosTrack = getTrackedRace().getTrack(competitor);
        TimePoint now = MillisecondsTimePoint.now();
        hassosTrack.addGPSFix(new GPSFixMovingImpl(new DegreePosition(54.4680424, 10.234451), now,
                new KnotSpeedWithBearingImpl(10, new DegreeBearingImpl(180)), /* optionalTrueHeading */ null));
        assertEquals(Tack.PORT, getTrackedRace().getTack(competitor, now));
    }

}
