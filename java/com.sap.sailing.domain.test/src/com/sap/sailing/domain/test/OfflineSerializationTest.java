package com.sap.sailing.domain.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.BoatImpl;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.EventImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.common.Color;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class OfflineSerializationTest extends AbstractSerializationTest {
    private static final Logger logger = Logger.getLogger(OfflineSerializationTest.class.getName());
    
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
        com.sap.sse.common.Util.Pair<Throwable, Throwable> p =
                new com.sap.sse.common.Util.Pair<Throwable, Throwable>(
                        s1, s2);
        HashSet<com.sap.sse.common.Util.Pair<Throwable, Throwable>> s =
                new HashSet<com.sap.sse.common.Util.Pair<Throwable, Throwable>>();
        s.add(p);
        Set<com.sap.sse.common.Util.Pair<Throwable, Throwable>> ss =
                cloneBySerialization(s, /* resolveAgainst */ receiverDomainFactory);
        
        com.sap.sse.common.Util.Pair<Throwable, Throwable> ps = ss.iterator().next();
        Throwable s1Des = ps.getA();
        Throwable s2Des = ps.getB();
        assertNotSame(s, ss);
        assertNotSame(s.iterator().next(), ss.iterator().next());
        assertNotSame(s1, s1Des);
        assertNotSame(s2, s2Des);
        assertEquals(1, ss.size());
        com.sap.sse.common.Util.Pair<Throwable, Throwable> pNew =
                new com.sap.sse.common.Util.Pair<Throwable, Throwable>(s1Des, s2Des);
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

    @Test
    public void testSerializingEventWithLeaderboardGroups() throws ClassNotFoundException, IOException {
        DomainFactory receiverDomainFactory = new DomainFactoryImpl();
        Event e = new EventImpl("Event Name", MillisecondsTimePoint.now(), MillisecondsTimePoint.now().plus(
                Duration.ONE_DAY.times(10)), "At Home", /* is public */true, UUID.randomUUID());
        LeaderboardGroup lg1 = new LeaderboardGroupImpl("LG1", "LG1 Description", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Collections.<Leaderboard> emptyList());
        e.addLeaderboardGroup(lg1);
        LeaderboardGroup lg2 = new LeaderboardGroupImpl("LG2", "LG2 Description", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Collections.<Leaderboard> emptyList());
        e.addLeaderboardGroup(lg2);
        Event deserialized = cloneBySerialization(e, receiverDomainFactory);
        assertEquals(Util.size(e.getLeaderboardGroups()), Util.size(deserialized.getLeaderboardGroups()));
        assertEquals(e.getLeaderboardGroups().iterator().next().getName(), deserialized.getLeaderboardGroups().iterator().next().getName());
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
        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("LeaderboardGroup", "Test Leaderboard Group", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Arrays.asList(new Leaderboard[] { leaderboard }));
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
        Waypoint sendersWaypoint1 = senderDomainFactory.createWaypoint(sendersMark1, /*passingInstruction*/null);
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
        Competitor sendersCompetitor1 = new CompetitorImpl(123, competitorName, Color.RED, new TeamImpl("STG", Collections.singleton(
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

    private static interface Op extends Serializable {
        String internalApplyTo(String s);
    }
    
    /**
     * To make absolutely sure that even if for strange reasons the test class was serializable, it would throw an
     * exception during serialization
     */
    private void writeObject(ObjectOutputStream oos) {
        fail("This class should not be serializable.");
    }

    @Test
    public void testLambdaDoesNotSerializeEnclosingInstance() throws IOException {
        Op op = s -> s+s;
        new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(op); // the test case is not serializable; if it were, its writeObject() would thrown an exception
        Op opWithRefToEnclosingInstance = s -> s+this.toString();
        try {
            new ObjectOutputStream(new ByteArrayOutputStream()).writeObject(opWithRefToEnclosingInstance);
            fail("Expected the lambda not to be serializable because it references the non-serializable enclosing instance");
        } catch (NotSerializableException nse) {
            // this is expected
            logger.info("Caught expected exception "+nse);
        }
    }
}
