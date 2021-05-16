package com.sap.sailing.server.test;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import java.util.Locale;
import java.util.UUID;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadContext;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.base.CourseArea;
import com.sap.sailing.domain.base.Event;
import com.sap.sailing.domain.base.impl.CourseAreaImpl;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.server.hierarchy.SailingHierarchyOwnershipUpdater;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.testsupport.SecurityBundleTestWrapper;
import com.sap.sse.common.Duration;
import com.sap.sse.common.TimePoint;
import com.sap.sse.mongodb.MongoDBConfiguration;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.UserManagementException;

public class LeagueEventHierarchyOwnershipChangeTest {
    private static final String USERNAME = "user-123";
    private static final String PASSWORD = "pass-234";

    private Event event;
    private LeaderboardGroup leaderboardGroup;
    private Leaderboard overallLeaderboard;
    private RacingEventService service;
    private Subject subject;
    private SubjectThreadState threadState;
    private SecurityService securityService;
    private CourseArea defaultCourseArea;

    @Before
    public void setUp() throws Exception {
        MongoDBConfiguration.getDefaultTestConfiguration().getService().getDB().drop();
        service = Mockito.spy(new RacingEventServiceImpl());
        securityService = new SecurityBundleTestWrapper().initializeSecurityServiceForTesting();
        Mockito.doReturn(securityService).when(service).getSecurityService();
        securityService.createSimpleUser(USERNAME, "a@b.c", PASSWORD, "The User", "SAP SE",
                /* validation URL */ Locale.ENGLISH, null, null);
        event = service.addEvent("Test", "Test Event", TimePoint.now(), TimePoint.now().plus(Duration.ONE_WEEK), "Here",
                /* isPublic */ true, UUID.randomUUID());
        defaultCourseArea = new CourseAreaImpl("Default", UUID.randomUUID());
        event.getVenue().addCourseArea(defaultCourseArea);
        leaderboardGroup = new LeaderboardGroupImpl("LG", "LGDesc", "The LG", /* displayGroupsInReverseOrder */ false,
                Collections.emptyList());
        overallLeaderboard = new LeaderboardGroupMetaLeaderboard(leaderboardGroup, new LowPoint(),
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup.setOverallLeaderboard(overallLeaderboard);
        event.addLeaderboardGroup(leaderboardGroup);
        ThreadContext.unbindSubject(); // ensure that a new subject is created that knows the current security manager
        subject = SecurityUtils.getSubject(); // this also binds the Subject to the ThreadContext
        subject.login(new UsernamePasswordToken("admin", "admin"));
        threadState = new SubjectThreadState(subject);
    }

    @Test
    public void testLeagueEventHierarchyOwnershipChange() {
        SailingHierarchyOwnershipUpdater.createOwnershipUpdater(/* createNewGroup */ true , /* existingGroupIdOrNull */ null,
                /* newGroupName */ "The new owning group",
                /* migrateCompetitors */ true, /* migrateBoats */ true, /* copyMembersAndRoles */ true,
                service)
            .updateGroupOwnershipForEventHierarchy(event);
        // if this works without an exception, we're happy; see bug 5541
    }

    @Test
    public void testLeagueHierarchyOwnershipChange() {
        SailingHierarchyOwnershipUpdater.createOwnershipUpdater(/* createNewGroup */ true , /* existingGroupIdOrNull */ null,
                /* newGroupName */ "The new owning group",
                /* migrateCompetitors */ true, /* migrateBoats */ true, /* copyMembersAndRoles */ true,
                service)
            .updateGroupOwnershipForLeaderboardGroupHierarchy(leaderboardGroup);
        // if this works without an exception, we're happy; see bug 5541
    }

    @Test
    public void testForeignLeagueWithinEventHierarchyOwnershipChangeStartingAtEvent() {
        final LeaderboardGroup leaderboardGroup2 = new LeaderboardGroupImpl("LG2", "LGDesc2", "The LG2", /* displayGroupsInReverseOrder */ false,
                Collections.emptyList());
        Leaderboard overallLeaderboard2 = new LeaderboardGroupMetaLeaderboard(leaderboardGroup2, new LowPoint(),
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup2.setOverallLeaderboard(overallLeaderboard2);
        leaderboardGroup2.addLeaderboard(new FlexibleLeaderboardImpl("FlexibleLeaderboard",
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(), new CourseAreaImpl("CA", UUID.randomUUID())));
        event.addLeaderboardGroup(leaderboardGroup2);
        SailingHierarchyOwnershipUpdater.createOwnershipUpdater(/* createNewGroup */ true , /* existingGroupIdOrNull */ null,
                /* newGroupName */ "The new owning group",
                /* migrateCompetitors */ true, /* migrateBoats */ true, /* copyMembersAndRoles */ true,
                service)
            .updateGroupOwnershipForEventHierarchy(event);
        final OwnershipAnnotation lg2Ownership = securityService.getOwnership(leaderboardGroup2.getIdentifier());
        assertNull(lg2Ownership); // leaderboard in lg2 doesn't belong to event, so we expect the ownership not to be set
    }
    
    @Test
    public void testOwnLeagueWithinEventHierarchyOwnershipChangeStartingAtEvent() {
        final LeaderboardGroup leaderboardGroup2 = new LeaderboardGroupImpl("LG2", "LGDesc2", "The LG2", /* displayGroupsInReverseOrder */ false,
                Collections.emptyList());
        Leaderboard overallLeaderboard2 = new LeaderboardGroupMetaLeaderboard(leaderboardGroup2, new LowPoint(),
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]));
        leaderboardGroup2.setOverallLeaderboard(overallLeaderboard2);
        leaderboardGroup2.addLeaderboard(new FlexibleLeaderboardImpl("FlexibleLeaderboard",
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(), defaultCourseArea));
        event.addLeaderboardGroup(leaderboardGroup2);
        SailingHierarchyOwnershipUpdater.createOwnershipUpdater(/* createNewGroup */ true , /* existingGroupIdOrNull */ null,
                /* newGroupName */ "The new owning group",
                /* migrateCompetitors */ true, /* migrateBoats */ true, /* copyMembersAndRoles */ true,
                service)
            .updateGroupOwnershipForEventHierarchy(event);
        final OwnershipAnnotation lg2Ownership = securityService.getOwnership(leaderboardGroup2.getIdentifier());
        final OwnershipAnnotation eventOwnership = securityService.getOwnership(event.getIdentifier());
        assertSame(eventOwnership.getAnnotation().getTenantOwner(), lg2Ownership.getAnnotation().getTenantOwner());
    }
    
    @After
    public void tearDown() throws UserManagementException {
        threadState.restore();
        subject.logout();
        securityService.deleteUser("admin");
    }
}
