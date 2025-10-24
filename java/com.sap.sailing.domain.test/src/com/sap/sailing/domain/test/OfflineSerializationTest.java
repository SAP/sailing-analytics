package com.sap.sailing.domain.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.base.BoatClass;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.DomainFactory;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Nationality;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.base.impl.DomainFactoryImpl;
import com.sap.sailing.domain.base.impl.EventImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.HighPoint;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sse.common.Color;
import com.sap.sse.common.Duration;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.interfaces.UserStore;
import com.sap.sse.security.shared.UserGroupManagementException;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.impl.LockingAndBanningImpl;
import com.sap.sse.security.shared.impl.User;
import com.sap.sse.security.shared.impl.UserGroup;
import com.sap.sse.security.userstore.mongodb.UserStoreImpl;

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
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
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
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        int[] intArray = new int[] { 5, 8 };
        int[] clone = cloneBySerialization(intArray, receiverDomainFactory);
        assertTrue(Arrays.equals(intArray, clone));
    }
    
    @Test
    public void testSerializingUserStore() throws UserGroupManagementException, UserManagementException, ClassNotFoundException, IOException {
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        UserStore userStore = new UserStoreImpl("defaultTenant");
        userStore.clear();
        UserGroup defaultTenant = userStore.createUserGroup(UUID.randomUUID(), "admin"+SecurityService.TENANT_SUFFIX);
        User user = userStore.createUser("admin", "", new LockingAndBanningImpl());
        defaultTenant.add(user);
        userStore.updateUserGroup(defaultTenant);
        user.getDefaultTenantMap().put("testserver", defaultTenant);
        userStore.updateUser(user);

        {
            User admin = userStore.getUserByName("admin");
            UserGroup adminTenant = admin.getDefaultTenant("testserver");
            assertTrue(adminTenant.contains(admin));
            assertTrue(Util.contains(admin.getUserGroups(), adminTenant));
        }
        UserStore deserializedUserStore = cloneBySerialization(userStore, receiverDomainFactory);
        assertNotNull(deserializedUserStore);
        {
            User admin = deserializedUserStore.getUserByName("admin");
            UserGroup adminTenant = admin.getDefaultTenant("testserver");
            assertTrue(adminTenant.contains(admin));
            assertTrue(Util.contains(admin.getUserGroups(), adminTenant));
        }
    }

    @Test
    public void testSerializingEventWithLeaderboardGroups() throws ClassNotFoundException, IOException {
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
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
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        ThresholdBasedResultDiscardingRuleImpl rdri = new ThresholdBasedResultDiscardingRuleImpl(new int[] { 5, 8 });
        ThresholdBasedResultDiscardingRuleImpl clone = cloneBySerialization(rdri, receiverDomainFactory);
        assertTrue(Arrays.equals(rdri.getDiscardIndexResultsStartingWithHowManyRaces(),
                clone.getDiscardIndexResultsStartingWithHowManyRaces()));
    }
    
    // see bug 1605
    @Test
    public void testSerializingOverallLeaderboardWithFactorOnColumn() throws ClassNotFoundException, IOException {
        Leaderboard leaderboard = new FlexibleLeaderboardImpl("Test Leaderboard", new ThresholdBasedResultDiscardingRuleImpl(new int[] { 3, 5 }), new HighPoint(), new CourseAreaImpl("Alpha", UUID.randomUUID(), /* centerPosition */ null, /* radius */ null));
        LeaderboardGroup leaderboardGroup = new LeaderboardGroupImpl("LeaderboardGroup", "Test Leaderboard Group", /* displayName */ null, /* displayGroupsInReverseOrder */ false, Arrays.asList(new Leaderboard[] { leaderboard }));
        final LeaderboardGroupMetaLeaderboard overallLeaderboard =
                new LeaderboardGroupMetaLeaderboard(leaderboardGroup, new HighPoint(),
                        new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup.setOverallLeaderboard(overallLeaderboard);
        final double FACTOR = 2.0;
        overallLeaderboard.getRaceColumnByName("Test Leaderboard").setFactor(FACTOR);
        
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        LeaderboardGroup clone = cloneBySerialization(leaderboardGroup, receiverDomainFactory);
        assertEquals(FACTOR, overallLeaderboard.getScoringScheme().getScoreFactor(overallLeaderboard.getRaceColumnByName("Test Leaderboard")), 0.00000001);
        assertEquals(FACTOR, clone.getOverallLeaderboard().getScoringScheme().getScoreFactor(clone.getOverallLeaderboard().getRaceColumnByName("Test Leaderboard")), 0.00000001);
    }
    
    @Test
    public void testIdentityStabilityOfMarkSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        Mark sendersMark1 = senderDomainFactory.getOrCreateMark("TestBuoy1");
        Mark receiversMark1 = cloneBySerialization(sendersMark1, receiverDomainFactory);
        Mark receiversSecondCopyOfMark1 = cloneBySerialization(sendersMark1, receiverDomainFactory);
        assertSame(receiversMark1, receiversSecondCopyOfMark1);
    }

    @Test
    public void testIdentityStabilityOfWaypointSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        Mark sendersMark1 = senderDomainFactory.getOrCreateMark("TestBuoy1");
        Waypoint sendersWaypoint1 = senderDomainFactory.createWaypoint(sendersMark1, /*passingInstruction*/null);
        Waypoint receiversWaypoint1 = cloneBySerialization(sendersWaypoint1, receiverDomainFactory);
        Waypoint receiversSecondCopyOfWaypoint1 = cloneBySerialization(sendersWaypoint1, receiverDomainFactory);
        assertSame(receiversWaypoint1, receiversSecondCopyOfWaypoint1);
    }

    @Test
    public void testIdentityStabilityOfBoatClassSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        BoatClass sendersBoatClass1 = senderDomainFactory.getOrCreateBoatClass("49er", /* typicallyStartsUpwind */ true);
        BoatClass receiversBoatClass1 = cloneBySerialization(sendersBoatClass1, receiverDomainFactory);
        BoatClass receiversSecondCopyOfBoatClass1 = cloneBySerialization(sendersBoatClass1, receiverDomainFactory);
        assertSame(receiversBoatClass1, receiversSecondCopyOfBoatClass1);
    }

    @Test
    public void testIdentityStabilityOfNationalitySerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        Nationality sendersNationality1 = senderDomainFactory.getOrCreateNationality("GER");
        Nationality receiversNationality1 = cloneBySerialization(sendersNationality1, receiverDomainFactory);
        Nationality receiversSecondCopyOfNationality1 = cloneBySerialization(sendersNationality1, receiverDomainFactory);
        assertSame(receiversNationality1, receiversSecondCopyOfNationality1);
    }

    @Test
    public void testIdentityStabilityOfCompetitorSerialization() throws ClassNotFoundException, IOException {
        DomainFactory senderDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        String competitorName = "Tina Maximiliane Lutz";
        Competitor sendersCompetitor1 = new CompetitorImpl(123, competitorName, "KYC", Color.RED, null, null, new TeamImpl("STG", Collections.singleton(
                        new PersonImpl(competitorName, senderDomainFactory.getOrCreateNationality("GER"),
                        /* dateOfBirth */ null, "This is famous "+competitorName)),
                        new PersonImpl("Rigo van Maas", senderDomainFactory.getOrCreateNationality("GER"),
                        /* dateOfBirth */null, "This is Rigo, the coach")), /* timeOnTimeFactor */ null, /* timeOnDistanceAllowancePerNauticalMile */ null, null);
        Competitor receiversCompetitor1 = cloneBySerialization(sendersCompetitor1, receiverDomainFactory);
        Competitor receiversSecondCopyOfCompetitor1 = cloneBySerialization(sendersCompetitor1, receiverDomainFactory);
        assertSame(receiversCompetitor1, receiversSecondCopyOfCompetitor1);
    }
    
    @Test
    public void ensureSameObjectWrittenTwiceComesOutIdentical() throws ClassNotFoundException, IOException {
        final DomainFactoryImpl senderDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
        DomainFactory receiverDomainFactory = new DomainFactoryImpl(DomainFactory.TEST_RACE_LOG_RESOLVER);
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
    
    private static class NonSerializableFieldDeclaration implements Serializable {
        private static final long serialVersionUID = 1806419236999145531L;
        public Object nonSerializable;
    }
    @Test
    public void testSerializingSerializableFieldValueOfNonSerializableDeclaredType() throws IOException, ClassNotFoundException {
        final NonSerializableFieldDeclaration o = new NonSerializableFieldDeclaration();
        o.nonSerializable = "A serializable String object";
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        final ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(o);
        oos.close();
        final ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(bos.toByteArray()));
        NonSerializableFieldDeclaration o2 = (NonSerializableFieldDeclaration) ois.readObject();
        assertEquals(o.nonSerializable, o2.nonSerializable);
    }
}
