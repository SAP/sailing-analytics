package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.UnauthorizedException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.common.ConflictObjectType;
import com.sap.sailing.domain.common.ScoringSchemeType;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.LeaderboardGroup;
import com.sap.sailing.domain.leaderboard.impl.FlexibleLeaderboardImpl;
import com.sap.sailing.domain.leaderboard.impl.LeaderboardGroupImpl;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.domain.leaderboard.impl.ThresholdBasedResultDiscardingRuleImpl;
import com.sap.sailing.domain.leaderboard.meta.LeaderboardGroupMetaLeaderboard;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.LeaderboardGroupManagement;
import com.sap.sailing.server.operationaltransformation.CreateLeaderboardGroup;
import com.sap.sailing.server.operationaltransformation.UpdateLeaderboardGroup;
import com.sap.sailing.server.testsupport.SecurityBundleTestWrapper;
import com.sap.sse.common.ObjectAlreadyExistsException;
import com.sap.sse.common.Util;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.OwnershipAnnotation;
import com.sap.sse.security.shared.QualifiedObjectIdentifier;
import com.sap.sse.security.shared.TypeRelativeObjectIdentifier;

public class LeaderboardGroupCreationTest {
    private RacingEventServiceImpl service;
    private SecurityService securityService;
    private String name;
    private String overallName;
    private UUID id;

    @BeforeEach
    public void setUp() throws Exception {
        securityService = new SecurityBundleTestWrapper().initializeSecurityServiceForTesting();
        service = new RacingEventServiceImpl() {
            @Override
            public SecurityService getSecurityService() {
                return securityService;
            }
        };
        name = "bug6326-" + UUID.randomUUID();
        overallName = LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(name);
        id = UUID.randomUUID();
        ThreadContext.unbindSubject();
        SecurityUtils.getSubject().login(new UsernamePasswordToken("admin", "admin"));
    }

    @AfterEach
    public void tearDown() {
        SecurityUtils.getSubject().logout();
        ThreadContext.unbindSubject();
        for (final LeaderboardGroup group : service.getLeaderboardGroups().values()) {
            if (group.getName().startsWith(name)) {
                service.removeLeaderboardGroup(group.getId());
            }
        }
        for (final Leaderboard leaderboard : service.getLeaderboards().values()) {
            if (leaderboard.getName().startsWith(name)) {
                service.removeLeaderboard(leaderboard.getName());
            }
        }
    }

    private FlexibleLeaderboard createFlexible(final String leaderboardName) {
        return service.addFlexibleLeaderboard(leaderboardName, "Original", new int[0], new LowPoint(), Collections.emptyList());
    }

    private CreateLeaderboardGroup createOperation(final ScoringSchemeType scoringScheme) {
        return new CreateLeaderboardGroup(id, name, "Description", "Display", /* displayGroupsInReverseOrder */ false,
                Collections.emptyList(), new int[0], scoringScheme);
    }

    private LeaderboardGroup createSecured(final UUID groupId) {
        return LeaderboardGroupManagement.createLeaderboardGroup(service, securityService, groupId, name,
                "Description", "Display", /* displayGroupsInReverseOrder */ false, Collections.emptyList(),
                new int[0], ScoringSchemeType.LOW_POINT);
    }

    private QualifiedObjectIdentifier overallIdentifier() {
        return SecuredDomainType.LEADERBOARD.getQualifiedObjectIdentifier(new TypeRelativeObjectIdentifier(overallName));
    }

    private QualifiedObjectIdentifier groupIdentifier(final UUID groupId) {
        return SecuredDomainType.LEADERBOARD_GROUP.getQualifiedObjectIdentifier(
                LeaderboardGroupImpl.getTypeRelativeObjectIdentifier(groupId));
    }

    @Test
    public void createRejectsExistingLeaderboardWithoutReplacingPersistence() {
        final Leaderboard existing = createFlexible(overallName);
        assertThrows(ObjectAlreadyExistsException.class, () -> service.apply(createOperation(ScoringSchemeType.LOW_POINT)));
        assertSame(existing, service.getLeaderboardByName(overallName));
        assertNull(service.getLeaderboardGroupByID(id));
        assertNull(service.getDomainObjectFactory().loadLeaderboardGroup(name, service, /* leaderboardRegistry */ null));
        final Leaderboard stored = service.getDomainObjectFactory().loadLeaderboard(overallName, service,
                /* leaderboardRegistry */ null);
        assertTrue(stored instanceof FlexibleLeaderboard);
        assertEquals("Original", stored.getDisplayName());
    }

    @Test
    public void duplicateGroupNameDoesNotCreateOverallLeaderboard() {
        final LeaderboardGroup existing = service.apply(createOperation(/* scoringScheme */ null));
        assertThrows(ObjectAlreadyExistsException.class, () -> service.apply(new CreateLeaderboardGroup(UUID.randomUUID(),
                name, "Other", "Other", /* displayGroupsInReverseOrder */ false, Collections.emptyList(),
                new int[0], ScoringSchemeType.LOW_POINT)));
        assertSame(existing, service.getLeaderboardGroupByName(name));
        assertNull(service.getLeaderboardByName(overallName));
        assertNull(service.getDomainObjectFactory().loadLeaderboard(overallName, service, /* leaderboardRegistry */ null));
    }

    @Test
    public void duplicateGroupIdDoesNotCreateAnotherGroupOrOverallLeaderboard() {
        final LeaderboardGroup existing = service.apply(createOperation(/* scoringScheme */ null));
        assertThrows(ObjectAlreadyExistsException.class, () -> service.apply(new CreateLeaderboardGroup(id,
                name + " other", "Other", "Other", /* displayGroupsInReverseOrder */ false, Collections.emptyList(),
                new int[0], ScoringSchemeType.LOW_POINT)));
        assertSame(existing, service.getLeaderboardGroupByID(id));
        assertNull(service.getLeaderboardGroupByName(name + " other"));
        assertNull(service.getLeaderboardByName(LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(name + " other")));
    }

    @Test
    public void enablingOverallRejectsBeforeRenamingOrChangingMembership() {
        final Leaderboard member = createFlexible(name + " member");
        final LeaderboardGroup group = service.addLeaderboardGroup(id, name + " old", "Original description",
                "Original display", /* displayGroupsInReverseOrder */ false, Collections.singletonList(member.getName()),
                new int[0], /* overallLeaderboardScoringSchemeType */ null);
        final Leaderboard existing = createFlexible(overallName);
        assertThrows(ObjectAlreadyExistsException.class, () -> service.apply(new UpdateLeaderboardGroup(id, name,
                "Changed description", "Changed display", Collections.emptyList(), new int[0], ScoringSchemeType.LOW_POINT)));
        assertEquals(name + " old", group.getName());
        assertEquals("Original description", group.getDescription());
        assertEquals("Original display", group.getDisplayName());
        assertEquals(Collections.singletonList(member), Util.asList(group.getLeaderboards()));
        assertNull(group.getOverallLeaderboard());
        assertSame(existing, service.getLeaderboardByName(overallName));
        assertNull(service.getLeaderboardGroupByName(name));
        final LeaderboardGroup stored = service.getDomainObjectFactory().loadLeaderboardGroup(name + " old", service,
                /* leaderboardRegistry */ null);
        assertEquals("Original description", stored.getDescription());
        assertEquals(/* expected */ 1, Util.size(stored.getLeaderboards()));
        assertFalse(stored.hasOverallLeaderboard());
    }

    @Test
    public void existingOverallCanBeUpdatedWithoutTreatingItAsADuplicate() {
        final LeaderboardGroup group = createSecured(id);
        final Leaderboard overall = group.getOverallLeaderboard();
        LeaderboardGroupManagement.updateLeaderboardGroup(service, securityService, id, name, "Changed", "Changed",
                Collections.emptyList(), new int[] { 3 }, ScoringSchemeType.LOW_POINT);
        assertSame(overall, group.getOverallLeaderboard());
        assertSame(overall, service.getLeaderboardByName(overallName));
        assertEquals("Changed", group.getDescription());
    }

    @Test
    public void renamingGroupKeepsItsExistingOverallLeaderboard() {
        final LeaderboardGroup group = createSecured(id);
        final Leaderboard overall = group.getOverallLeaderboard();
        final String newName = name + " renamed";
        final Leaderboard other = createFlexible(LeaderboardGroupMetaLeaderboard.getOverallLeaderboardName(newName));
        LeaderboardGroupManagement.updateLeaderboardGroup(service, securityService, id, newName, "Changed", "Changed",
                Collections.emptyList(), new int[0], ScoringSchemeType.LOW_POINT);
        assertEquals(newName, group.getName());
        assertSame(overall, group.getOverallLeaderboard());
        assertSame(overall, service.getLeaderboardByName(overallName));
        assertSame(other, service.getLeaderboardByName(other.getName()));
    }

    @Test
    public void securedEnablingOverallDoesNotClaimTheConflictingLeaderboard() {
        final LeaderboardGroup group = service.apply(createOperation(/* scoringScheme */ null));
        securityService.setDefaultOwnership(group.getIdentifier(), name);
        final Leaderboard existing = createFlexible(overallName);
        assertThrows(ObjectAlreadyExistsException.class, () -> LeaderboardGroupManagement.updateLeaderboardGroup(
                service, securityService, id, name, "Changed", "Changed", Collections.emptyList(),
                new int[0], ScoringSchemeType.LOW_POINT));
        assertEquals("Description", group.getDescription());
        assertFalse(group.hasOverallLeaderboard());
        assertNull(securityService.getOwnership(existing.getIdentifier()));
        assertNotNull(securityService.getOwnership(group.getIdentifier()));
    }

    @Test
    public void deniedGroupCreationUnwindsBothOwnershipScopes() {
        final Subject subject = Mockito.spy(SecurityUtils.getSubject());
        Mockito.doThrow(new UnauthorizedException("Group creation denied")).when(subject)
                .checkPermission(groupIdentifier(id).getStringPermission(DefaultActions.CREATE));
        ThreadContext.bind(subject);
        assertThrows(UnauthorizedException.class, () -> createSecured(id));
        assertNull(service.getLeaderboardGroupByID(id));
        assertNull(service.getLeaderboardByName(overallName));
        assertNull(securityService.getOwnership(groupIdentifier(id)));
        assertNull(securityService.getOwnership(overallIdentifier()));
    }

    @Test
    public void nestedConflictDoesNotStripOwnershipFromAnAlreadyCreatedObject() {
        final String parentName = name + " parent";
        final QualifiedObjectIdentifier parentIdentifier = SecuredDomainType.LEADERBOARD.getQualifiedObjectIdentifier(
                new TypeRelativeObjectIdentifier(parentName));
        createFlexible(overallName);
        assertThrows(ObjectAlreadyExistsException.class, () -> securityService
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD,
                        parentIdentifier.getTypeRelativeObjectIdentifier(), parentName, () -> {
                            createFlexible(parentName);
                            return createSecured(id);
                        }));
        assertNotNull(service.getLeaderboardByName(parentName));
        assertNotNull(securityService.getOwnership(parentIdentifier));
        assertNull(securityService.getOwnership(groupIdentifier(id)));
        assertNull(securityService.getOwnership(overallIdentifier()));
    }

    @Test
    public void registryRejectsReplacementButAcceptsTheSameObjectAgain() {
        final Leaderboard existing = createFlexible(overallName);
        final Leaderboard replacement = Mockito.mock(Leaderboard.class);
        Mockito.when(replacement.getName()).thenReturn(overallName);
        service.addLeaderboard(existing);
        assertThrows(ObjectAlreadyExistsException.class, () -> service.addLeaderboard(replacement));
        assertSame(existing, service.getLeaderboardByName(overallName));
        Mockito.verify(replacement, Mockito.never()).addScoreCorrectionListener(Mockito.any());
    }

    @Test
    public void groupCreationAndRawRegistrationCannotOverwriteEachOther() throws Exception {
        final Leaderboard competitor = new FlexibleLeaderboardImpl(overallName,
                new ThresholdBasedResultDiscardingRuleImpl(new int[0]), new LowPoint(), /* courseArea */ null);
        final CyclicBarrier start = new CyclicBarrier(/* parties */ 2);
        final ExecutorService executor = Executors.newFixedThreadPool(/* nThreads */ 2);
        try {
            final Future<Boolean> groupCreated = executor.submit(() -> {
                start.await(/* timeout */ 30, TimeUnit.SECONDS);
                boolean created;
                try {
                    service.apply(createOperation(ScoringSchemeType.LOW_POINT));
                    created = true;
                } catch (final ObjectAlreadyExistsException e) {
                    created = false;
                }
                return created;
            });
            final Future<Boolean> competitorRegistered = executor.submit(() -> {
                start.await(/* timeout */ 30, TimeUnit.SECONDS);
                boolean registered;
                try {
                    service.addLeaderboard(competitor);
                    registered = true;
                } catch (final ObjectAlreadyExistsException e) {
                    registered = false;
                }
                return registered;
            });
            final boolean groupWon = groupCreated.get(/* timeout */ 30, TimeUnit.SECONDS);
            assertTrue(groupWon ^ competitorRegistered.get(/* timeout */ 30, TimeUnit.SECONDS));
            if (groupWon) {
                assertSame(service.getLeaderboardGroupByID(id).getOverallLeaderboard(), service.getLeaderboardByName(overallName));
            } else {
                assertNull(service.getLeaderboardGroupByID(id));
                assertNull(service.getDomainObjectFactory().loadLeaderboardGroup(name, service, /* leaderboardRegistry */ null));
                assertSame(competitor, service.getLeaderboardByName(overallName));
            }
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void securedCreationDoesNotClaimUnownedExistingLeaderboard() {
        final Leaderboard existing = createFlexible(overallName);
        assertNull(securityService.getOwnership(existing.getIdentifier()));
        assertThrows(ObjectAlreadyExistsException.class, () -> createSecured(id));
        assertNull(securityService.getOwnership(existing.getIdentifier()));
        assertNull(securityService.getOwnership(groupIdentifier(id)));
    }

    @Test
    public void securedCreationPreservesPreexistingOwnershipAndAclEvenForAdmin() {
        final Leaderboard existing = createFlexible(overallName);
        securityService.setDefaultOwnership(existing.getIdentifier(), overallName);
        securityService.overrideAccessControlList(existing.getIdentifier(), Collections.emptyMap());
        final OwnershipAnnotation ownership = securityService.getOwnership(existing.getIdentifier());
        final Object acl = securityService.getAccessControlList(existing.getIdentifier());
        assertThrows(ObjectAlreadyExistsException.class, () -> createSecured(id));
        assertSame(ownership, securityService.getOwnership(existing.getIdentifier()));
        assertSame(acl, securityService.getAccessControlList(existing.getIdentifier()));
    }

    @Test
    public void ownershipWithoutDomainObjectDoesNotPreventCreation() {
        securityService.setDefaultOwnership(overallIdentifier(), overallName);
        final OwnershipAnnotation ownership = securityService.getOwnership(overallIdentifier());
        assertNotNull(createSecured(id).getOverallLeaderboard());
        assertSame(ownership, securityService.getOwnership(overallIdentifier()));
        assertNotNull(securityService.getOwnership(groupIdentifier(id)));
    }

    @Test
    public void lateDomainRejectionUnwindsBothNewOwnershipScopes() {
        createFlexible(overallName);
        assertThrows(ObjectAlreadyExistsException.class, () -> securityService
                .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD,
                        new TypeRelativeObjectIdentifier(overallName), overallName, () -> {},
                        () -> securityService.setOwnershipCheckPermissionForObjectCreationAndRevertOnError(
                                SecuredDomainType.LEADERBOARD_GROUP, LeaderboardGroupImpl.getTypeRelativeObjectIdentifier(id),
                                name, () -> {}, () -> service.apply(createOperation(ScoringSchemeType.LOW_POINT)))));
        assertNull(securityService.getOwnership(overallIdentifier()));
        assertNull(securityService.getOwnership(groupIdentifier(id)));
        assertNull(service.getLeaderboardGroupByID(id));
    }

    @Test
    public void competingSecuredCreationsKeepOnlyTheWinnersOwnership() throws Exception {
        final UUID secondId = UUID.randomUUID();
        final CyclicBarrier start = new CyclicBarrier(/* parties */ 2);
        final ExecutorService executor = Executors.newFixedThreadPool(/* nThreads */ 2);
        final Subject subject = SecurityUtils.getSubject();
        try {
            final Future<Boolean> first = executor.submit(() -> subject.execute(competingCreation(id, start)));
            final Future<Boolean> second = executor.submit(() -> subject.execute(competingCreation(secondId, start)));
            final boolean firstWon = first.get(/* timeout */ 30, TimeUnit.SECONDS);
            assertTrue(firstWon ^ second.get(/* timeout */ 30, TimeUnit.SECONDS));
            final UUID winner = firstWon ? id : secondId;
            final UUID loser = firstWon ? secondId : id;
            assertEquals(winner, service.getLeaderboardGroupByName(name).getId());
            assertNotNull(securityService.getOwnership(groupIdentifier(winner)));
            assertNull(securityService.getOwnership(groupIdentifier(loser)));
            assertNotNull(securityService.getOwnership(overallIdentifier()));
            assertSame(service.getLeaderboardGroupByID(winner).getOverallLeaderboard(), service.getLeaderboardByName(overallName));
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void failedCreationReleasesOwnershipBeforeWaitingCreationUsesIt() throws Exception {
        final CountDownLatch failedActionEntered = new CountDownLatch(/* count */ 1);
        final CountDownLatch successfulActionRequested = new CountDownLatch(/* count */ 1);
        final CountDownLatch releaseFailedAction = new CountDownLatch(/* count */ 1);
        final AtomicReference<OwnershipAnnotation> rejectedOwnership = new AtomicReference<>();
        final ExecutorService executor = Executors.newFixedThreadPool(/* nThreads */ 2);
        final Subject subject = SecurityUtils.getSubject();
        try {
            final Future<ObjectAlreadyExistsException> rejected = executor.submit(() -> subject.execute(
                    (Callable<ObjectAlreadyExistsException>) () -> assertThrows(ObjectAlreadyExistsException.class, () -> securityService
                    .setOwnershipCheckPermissionForObjectCreationAndRevertOnError(SecuredDomainType.LEADERBOARD,
                            new TypeRelativeObjectIdentifier(overallName), overallName, () -> {}, () -> {
                                rejectedOwnership.set(securityService.getOwnership(overallIdentifier()));
                                failedActionEntered.countDown();
                                assertTrue(releaseFailedAction.await(/* timeout */ 30, TimeUnit.SECONDS));
                                throw new ObjectAlreadyExistsException(ConflictObjectType.LEADERBOARD_GROUP, name);
                            }))));
            assertTrue(failedActionEntered.await(/* timeout */ 30, TimeUnit.SECONDS));
            final Future<LeaderboardGroup> accepted = executor.submit(() -> subject.execute((Callable<LeaderboardGroup>) () -> {
                successfulActionRequested.countDown();
                return createSecured(id);
            }));
            assertTrue(successfulActionRequested.await(/* timeout */ 30, TimeUnit.SECONDS));
            assertThrows(TimeoutException.class, () -> accepted.get(/* timeout */ 200, TimeUnit.MILLISECONDS));
            releaseFailedAction.countDown();
            assertNotNull(rejected.get(/* timeout */ 30, TimeUnit.SECONDS));
            assertNotNull(accepted.get(/* timeout */ 30, TimeUnit.SECONDS).getOverallLeaderboard());
            final OwnershipAnnotation acceptedOwnership = securityService.getOwnership(overallIdentifier());
            assertNotNull(acceptedOwnership);
            assertFalse(rejectedOwnership.get() == acceptedOwnership);
            assertNotNull(securityService.getOwnership(groupIdentifier(id)));
        } finally {
            releaseFailedAction.countDown();
            executor.shutdownNow();
        }
    }

    private Callable<Boolean> competingCreation(final UUID groupId, final CyclicBarrier start) {
        return () -> {
            start.await(/* timeout */ 30, TimeUnit.SECONDS);
            boolean created;
            try {
                createSecured(groupId);
                created = true;
            } catch (final ObjectAlreadyExistsException e) {
                created = false;
            }
            return created;
        };
    }
}
