package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.junit.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;

public class OfflineSerializationTest extends AbstractSerializationTest {
    /**
     * Bug 769 was based on an inconsistency of a cached hash code in Pair. The same problem existed for Triple.
     * Serialization changes the Object IDs of the objects contained and therefore the hash code based on this
     * identity. Serializing a cached hash code therefore leads to an inconsistency. The non-caching of this
     * hash code is tested here.
     */
    @Test
    public void testHashCodeOfSerializedPairIsConsistent() throws ClassNotFoundException, IOException {
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        final Throwable s1 = new Throwable();
        final Throwable s2 = new Throwable();
        Pair<Throwable, Throwable> p =
                new Pair<Throwable, Throwable>(
                        s1, s2);
        HashSet<Pair<Throwable, Throwable>> s =
                new HashSet<Pair<Throwable, Throwable>>();
        s.add(p);
        Set<Pair<Throwable, Throwable>> ss =
                cloneBySerialization(s, /* resolveAgainst */ receiverDomainFactory);
        
        Pair<Throwable, Throwable> ps = ss.iterator().next();
        Throwable s1Des = ps.getA();
        Throwable s2Des = ps.getB();
        assertNotSame(s, ss);
        assertNotSame(s.iterator().next(), ss.iterator().next());
        assertNotSame(s1, s1Des);
        assertNotSame(s2, s2Des);
        assertEquals(1, ss.size());
        Pair<Throwable, Throwable> pNew =
                new Pair<Throwable, Throwable>(s1Des, s2Des);
        assertEquals(ps.hashCode(), pNew.hashCode());
        assertTrue(ss.contains(pNew));
    }
    
    /**
     * We had trouble de-serializing int[] through our specialized ObjectInputStream with its own resolveClass
     * implementation. This test failed initially before we changed the call for loading classes.
     */
    @Test
    public void testSerializingIntArray() throws ClassNotFoundException, IOException {
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        int[] intArray = new int[] { 5, 8 };
        int[] clone = cloneBySerialization(intArray, receiverDomainFactory);
        assertTrue(Arrays.equals(intArray, clone));
    }
    
    /**
     * We had trouble de-serializing int[] through our specialized ObjectInputStream with its own resolveClass
     * implementation. This test failed initially before we changed the call for loading classes.
     */
    @Test
    public void testSerializingResultDiscardingRuleImpl() throws ClassNotFoundException, IOException {
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        ThresholdBasedResultDiscardingRuleImpl rdri = new ThresholdBasedResultDiscardingRuleImpl(new int[] { 5, 8 });
        ThresholdBasedResultDiscardingRuleImpl clone = cloneBySerialization(rdri, receiverDomainFactory);
        assertTrue(Arrays.equals(rdri.getDiscardIndexResultsStartingWithHowManyRaces(),
                clone.getDiscardIndexResultsStartingWithHowManyRaces()));
    }
    
    // see bug 1605
    @Test
    public void testSerializingOverallLeaderboardWithFactorOnColumn() throws ClassNotFoundException, IOException {
        Leaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 5 }), new HighPoint(), new CourseAreaImpl("Alpha", UUID.randomUUID()));
        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("LeaderboardGroup", "Test Leaderboard Group", /* displayGroupsInReverseOrder */ false, Arrays.asList(new Leaderboard[] { leaderboard }));
        final LeaderboardGroupMetaLeaderboard overallLeaderboard = new LeaderboardGroupMetaLeaderboard(leaderboardGroup, new HighPoint(), new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup.setOverallLeaderboard(overallLeaderboard);
        final double FACTOR = 2.0;
        overallLeaderboard.getRaceColumnByName("Test Leaderboard").setFactor(FACTOR);
        
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        LeaderboardGroup clone = cloneBySerialization(leaderboardGroup, receiverDomainFactory);
        assertEquals(FACTOR, overallLeaderboard.getRaceColumnByName("Test Leaderboard").getFactor(), 0.00000001);
        assertEquals(FACTOR, clone.getOverallLeaderboard().getRaceColumnByName("Test Leaderboard").getFactor(), 0.00000001);
    }
    
    @Test
    public void testIdentityStabilityOfMarkSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl();
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        Mark sendersMark1 = senderDomainFactory.getOrCreateMark("TestBuoy1");
        Mark receiversMark1 = cloneBySerialization(sendersMark1, receiverDomainFactory);
        Mark receiversSecondCopyOfMark1 = cloneBySerialization(sendersMark1, receiverDomainFactory);
        assertSame(receiversMark1, receiversSecondCopyOfMark1);
    }

    @Test
    public void testIdentityStabilityOfWaypointSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl();
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        Mark sendersMark1 = senderDomainFactory.getOrCreateMark("TestBuoy1");
        Waypoint sendersWaypoint1 = senderDomainFactory.createWaypoint(sendersMark1, /*passingSide*/null);
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
        Competitor sendersCompetitor1 = new CompetitorImpl(123, competitorName, "#FF0000", new TeamImpl("STG", Collections.singleton(
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
