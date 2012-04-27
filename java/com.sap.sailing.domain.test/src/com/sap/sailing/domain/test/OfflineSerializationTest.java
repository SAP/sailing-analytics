package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.junit.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Buoy;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.leaderboard.impl.ResultDiscardingRuleImpl;

public class OfflineSerializationTest extends AbstractSerializationTest {
    @Test
    public void testSerializingIntArray() throws ClassNotFoundException, IOException {
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        int[] intArray = new int[] { 5, 8 };
        int[] clone = cloneBySerialization(intArray, receiverDomainFactory);
        assertTrue(Arrays.equals(intArray, clone));
    }
    
    @Test
    public void testSerializingResultDiscardingRuleImpl() throws ClassNotFoundException, IOException {
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        ResultDiscardingRuleImpl rdri = new ResultDiscardingRuleImpl(new int[] { 5, 8 });
        ResultDiscardingRuleImpl clone = cloneBySerialization(rdri, receiverDomainFactory);
        assertTrue(Arrays.equals(rdri.getDiscardIndexResultsStartingWithHowManyRaces(),
                clone.getDiscardIndexResultsStartingWithHowManyRaces()));
    }
    
    @Test
    public void testIdentityStabilityOfBuoySerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl();
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        Buoy sendersBuoy1 = senderDomainFactory.getOrCreateBuoy("TestBuoy1");
        Buoy receiversBuoy1 = cloneBySerialization(sendersBuoy1, receiverDomainFactory);
        Buoy receiversSecondCopyOfBuoy1 = cloneBySerialization(sendersBuoy1, receiverDomainFactory);
        assertSame(receiversBuoy1, receiversSecondCopyOfBuoy1);
    }

    @Test
    public void testIdentityStabilityOfWaypointSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl();
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        Buoy sendersBuoy1 = senderDomainFactory.getOrCreateBuoy("TestBuoy1");
        Waypoint sendersWaypoint1 = senderDomainFactory.createWaypoint(sendersBuoy1);
        Waypoint receiversWaypoint1 = cloneBySerialization(sendersWaypoint1, receiverDomainFactory);
        Waypoint receiversSecondCopyOfWaypoint1 = cloneBySerialization(sendersWaypoint1, receiverDomainFactory);
        assertSame(receiversWaypoint1, receiversSecondCopyOfWaypoint1);
    }

    @Test
    public void testIdentityStabilityOfBoatClassSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl();
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        BoatClass sendersBoatClass1 = senderDomainFactory.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */ true);
        BoatClass receiversBoatClass1 = cloneBySerialization(sendersBoatClass1, receiverDomainFactory);
        BoatClass receiversSecondCopyOfBoatClass1 = cloneBySerialization(sendersBoatClass1, receiverDomainFactory);
        assertSame(receiversBoatClass1, receiversSecondCopyOfBoatClass1);
    }

    @Test
    public void testIdentityStabilityOfNationalitySerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl();
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        Nationality sendersNationality1 = senderDomainFactory.getOrCreateNationality("GER");
        Nationality receiversNationality1 = cloneBySerialization(sendersNationality1, receiverDomainFactory);
        Nationality receiversSecondCopyOfNationality1 = cloneBySerialization(sendersNationality1, receiverDomainFactory);
        assertSame(receiversNationality1, receiversSecondCopyOfNationality1);
    }

    @Test
    public void testIdentityStabilityOfCompetitorSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl();
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        String competitorName = "Tina Maximiliane Lutz";
        Competitor sendersCompetitor1 = new CompetitorImpl(123, competitorName, new TeamImpl("STG", Collections.singleton(
                new PersonImpl(competitorName, senderDomainFactory.getOrCreateNationality("GER"),
                /* dateOfBirth */ null, "This is famous "+competitorName)),
                new PersonImpl("Rigo van Maas", senderDomainFactory.getOrCreateNationality("GER"),
                /* dateOfBirth */null, "This is Rigo, the coach")), new BoatImpl(competitorName + "'s boat",
                        senderDomainFactory.getOrCreateBoatClass("470", /* typicallyStartsUpwind */ true), "GER 61"));
        Competitor receiversCompetitor1 = cloneBySerialization(sendersCompetitor1, receiverDomainFactory);
        Competitor receiversSecondCopyOfCompetitor1 = cloneBySerialization(sendersCompetitor1, receiverDomainFactory);
        assertSame(receiversCompetitor1, receiversSecondCopyOfCompetitor1);
    }
    
    @Test
    public void ensureSameObjectWrittenTwiceComesOutIdentical() throws ClassNotFoundException, IOException {
        final DomainFactoryImpl senderDomainFactory = new DomainFactoryImpl();
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        Nationality n = senderDomainFactory.getOrCreateNationality("GER");
        Object[] copies = cloneManyBySerialization(receiverDomainFactory, n, n);
        assertEquals(2, copies.length);
        assertSame(copies[0], copies[1]);
        assertNotSame(n, copies[0]);
        assertEquals(n.getName(), ((Nationality) copies[0]).getName());
    }
}
