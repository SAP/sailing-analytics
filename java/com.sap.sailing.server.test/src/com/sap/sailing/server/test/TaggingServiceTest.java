package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.List;
import java.util.Locale;
import java.util.logging.Logger;

import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.UsernamePasswordToken;
import org.apache.shiro.authz.AuthorizationException;
import org.apache.shiro.subject.Subject;
import org.apache.shiro.subject.support.SubjectThreadState;
import org.apache.shiro.util.ThreadContext;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.sap.sailing.domain.abstractlog.race.RaceLog;
import com.sap.sailing.domain.abstractlog.race.RaceLogEvent;
import com.sap.sailing.domain.base.RaceColumn;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.dto.TagDTO;
import com.sap.sailing.domain.common.security.SecuredDomainType;
import com.sap.sailing.domain.common.tagging.RaceLogNotFoundException;
import com.sap.sailing.domain.common.tagging.ServiceNotFoundException;
import com.sap.sailing.domain.common.tagging.TagAlreadyExistsException;
import com.sap.sailing.domain.leaderboard.FlexibleLeaderboard;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sailing.server.interfaces.RacingEventServiceOperation;
import com.sap.sailing.server.interfaces.TaggingService;
import com.sap.sailing.server.operationaltransformation.AddColumnToLeaderboard;
import com.sap.sailing.server.operationaltransformation.CreateFlexibleLeaderboard;
import com.sap.sailing.server.tagging.TagDTODeSerializer;
import com.sap.sailing.server.tagging.TaggingServiceImpl;
import com.sap.sailing.server.testsupport.SecurityBundleTestWrapper;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBService;
import com.sap.sse.security.SecurityService;
import com.sap.sse.security.shared.HasPermissions.DefaultActions;
import com.sap.sse.security.shared.UserManagementException;
import com.sap.sse.security.shared.WildcardPermission;

/**
 * Tests {@link TaggingService} which is used for all CRUD operations regarding {@link TagDTO tags}.
 */
public class TaggingServiceTest {

    // user
    private final static String username = "abc";
    private final static String email = "e@mail.com";
    private final static String password = "password";
    private final static String fullName = "Full Name";
    private final static String company = "Company";

    // race definition
    private final static String leaderboardName = "Leaderboard";
    private final static String raceColumnName = "RaceColumn";
    private final static String fleetName = "Default";
    private final static WildcardPermission readAndEditLeaderboardPermission = WildcardPermission.builder()
            .withTypes(SecuredDomainType.LEADERBOARD).withActions(DefaultActions.UPDATE, DefaultActions.READ)
            .withIds(Leaderboard.getTypeRelativeObjectIdentifier(leaderboardName)).build();

    // tagging & utilities
    private final static Logger logger = Logger.getLogger(TaggingServiceTest.class.getName());
    private final static TagDTODeSerializer serializer = new TagDTODeSerializer();
    private static RacingEventService racingService;
    private static SecurityService securityService;
    private static TaggingService taggingService;
    private static Subject subject;
    private static SubjectThreadState threadState;

    @BeforeAll
    public synchronized static void setUpClass() throws Exception {
        logger.info("current thread: "+Thread.currentThread());
        MongoDBService.INSTANCE.getDB().drop();
        // setup racing service and racelog
        racingService = Mockito.spy(new RacingEventServiceImpl());
        RacingEventServiceOperation<FlexibleLeaderboard> addLeaderboardOp = new CreateFlexibleLeaderboard(
                leaderboardName, leaderboardName, new int[] { 5 }, new LowPoint(), null);
        racingService.apply(addLeaderboardOp);
        RacingEventServiceOperation<RaceColumn> addLeaderboardColumn = new AddColumnToLeaderboard(raceColumnName,
                leaderboardName, true);
        racingService.apply(addLeaderboardColumn);
        // setup security service
        securityService = new SecurityBundleTestWrapper().initializeSecurityServiceForTesting();
        // create & login user
        securityService.createSimpleUser(username, email, password, fullName, company, Locale.ENGLISH, null,
                securityService.getDefaultTenantForCurrentUser(), /* clientIP */ null, /* enforce strong password */ false);
        ThreadContext.unbindSubject(); // ensure that a new subject is created that knows the current security manager
        subject = SecurityUtils.getSubject(); // this also binds the Subject to the ThreadContext
        subject.login(new UsernamePasswordToken(username, password));
        threadState = new SubjectThreadState(subject);
        // setup tagging service
        taggingService = Mockito.spy(new TaggingServiceImpl(racingService));
        Mockito.doReturn(securityService).when(racingService).getSecurityService();
        Mockito.doReturn(taggingService).when(racingService).getTaggingService();
    }

    @AfterAll
    public synchronized static void tearDownClass() {
        logger.info("current thread: "+Thread.currentThread());
        try {
            subject.logout();
            securityService.deleteUser(username);
        } catch (UserManagementException e) {
            logger.severe("Could not teardown TaggingServiceTest!");
        }
        MongoDBService.INSTANCE.getDB().drop();
    }

    @BeforeEach
    public void resetEnvironment() {
        logger.info("current thread: "+Thread.currentThread());
        synchronized (TaggingServiceTest.class) {
            // setup the Shiro SubjectThreadState to ensure that the tagging service can check whether a subject is logged in
            threadState.bind();
            assertSame(securityService.getSecurityManager(), ThreadContext.getSecurityManager());
            assertSame(subject, SecurityUtils.getSubject());
            assertEquals(username, subject.getPrincipal());
            securityService.addPermissionForUser(username, readAndEditLeaderboardPermission);
            securityService.unsetPreference(username,
                    serializer.generateUniqueKey(leaderboardName, raceColumnName, fleetName));
            final RaceLog raceLog = racingService.getRaceLog(leaderboardName, raceColumnName, fleetName);
            if (raceLog != null) {
                for (RaceLogEvent event : raceLog.getUnrevokedEvents()) {
                    try {
                        raceLog.revokeEvent(event.getAuthor(), event);
                    } catch (NotRevokableException e) {
                        logger.warning(
                                "Could not clean up test setup for TaggingServiceTest as public tag could not be removed!");
                    }
                }
            }
            try {
                List<TagDTO> tags = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false);
                logger.info("Tags after environment reset: " + tags.toString());
            } catch (Exception e) {
                logger.severe("Could not reset test environment");
            }
        }
    }

    @AfterEach
    public void restoreEnvironment() {
        logger.info("current thread: "+Thread.currentThread());
        synchronized (TaggingServiceTest.class) {
            threadState.restore();
        }
    }
    
    @Test
    public void testAddTag() throws AuthorizationException, IllegalArgumentException, RaceLogNotFoundException, ServiceNotFoundException, TagAlreadyExistsException {
        logger.entering(getClass().getName(), "testAddTag");
        final String tag = "TagToCreate";
        final String comment = "Comment To Create";
        final String hiddenInfo = "Hidden Info To Create";
        final String imageURL = "";
        final TimePoint raceTimepoint = new MillisecondsTimePoint(1);
        try {
            logger.info("Trying to add public tag with missing title which should be catched by this test.");
            taggingService.addTag(leaderboardName, raceColumnName, fleetName, null, comment, hiddenInfo, imageURL, imageURL,
                    true, raceTimepoint);
            fail("Tag should not be added because the tag title is missing!");
        } catch (IllegalArgumentException e) {
            assertTrue(true, "Invalid arguments were caught correctly!");
        }
        taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, hiddenInfo, imageURL, imageURL,
                false, raceTimepoint);
        String preference = securityService.getPreference(username,
                serializer.generateUniqueKey(leaderboardName, raceColumnName, fleetName));
        List<TagDTO> privateTags = serializer.deserializeTags(preference);
        assertTrue(privateTags.size() == 1 && privateTags.get(0).equals(tag, comment, hiddenInfo,
                imageURL, imageURL, false, subject.getPrincipal().toString(), raceTimepoint), "Create private tag");
        // we used to test for permission checking here, but it was wrong in the first place to check permissions
        // in TaggingService; permission checks shall happen in the REST API layer and in the GWT RPC service layer
        try {
            logger.info(
                    "Trying to add public tag with wrong racelog identifiers which should be catched by this test.");
            taggingService.addTag(leaderboardName, "bla", fleetName, tag, comment, hiddenInfo, imageURL, imageURL, true, raceTimepoint);
            fail("Tag should not be added because racelog does not exist!");
        } catch (RaceLogNotFoundException e) {
            assertTrue(true, "Missing racelog was caught correctly!");
        }
        logger.info("Trying to add public tag, should succeed");
        taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, hiddenInfo, imageURL, imageURL,
                true, raceTimepoint);
        List<TagDTO> publicTags = taggingService.getPublicTags(leaderboardName, raceColumnName, fleetName, null, false);
        assertTrue(publicTags.size() == 1 && publicTags.get(0).equals(tag, comment, hiddenInfo, imageURL, imageURL,
                true, subject.getPrincipal().toString(), raceTimepoint), "");
        try {
            logger.info("Trying to add already existing public tag which should be catched by this test.");
            taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, hiddenInfo, imageURL, imageURL,
                    true, raceTimepoint);
            fail("Tag should not be added because it already exists!");
        } catch (TagAlreadyExistsException e) {
            assertTrue(true, "Tag already exists was caught correctly!");
        }
        logger.exiting(getClass().getName(), "testAddTag");
    }

    @Test
    public void testGetTags() throws Exception {
        logger.entering(getClass().getName(), "testGetTags");
        final String tag = "TagToLoad";
        final String comment = "Comment To Load";
        final String hiddenInfo = "Hidden info To Load";
        final String imageURL = "localhost";
        final TimePoint raceTimepoint = new MillisecondsTimePoint(1000);
        logger.info("Adding tags which should be loaded via getTags() afterwards.");
        taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, hiddenInfo, imageURL, imageURL,
                false, raceTimepoint);
        taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, hiddenInfo, imageURL, imageURL,
                true, raceTimepoint);
        assertTrue(taggingService.getPrivateTags(leaderboardName, raceColumnName, fleetName).get(0).equals(tag, comment,
                hiddenInfo, imageURL, imageURL, false, subject.getPrincipal().toString(), raceTimepoint),
                "Private tags contain added tag");
        assertTrue(taggingService.getPublicTags(leaderboardName, raceColumnName, fleetName, null, false).get(0).equals(tag,
                comment, hiddenInfo, imageURL, imageURL, true, subject.getPrincipal().toString(), raceTimepoint),
                "Public tags contain added tag");
        assertEquals(1, taggingService.getPublicTags(leaderboardName, raceColumnName, fleetName, raceTimepoint, false).size(),
                "Public tags contain added tag with matching creation date filter");
        assertEquals(0, taggingService
                .getPublicTags(leaderboardName, raceColumnName, fleetName, MillisecondsTimePoint.now(), false).size(), "Public tags do not contain added tag with non-matching creation date filter");
        logger.exiting(getClass().getName(), "testGetTags");
    }

    @Test
    public void testUpdateTag() throws AuthorizationException, IllegalArgumentException, NotRevokableException,
            RaceLogNotFoundException, ServiceNotFoundException, TagAlreadyExistsException {
        logger.entering(getClass().getName(), "testUpdateTag");
        final String tag = "TagToUpdate";
        final String comment = "Comment To Update";
        final String hiddenInfo = "Hidden info To Update";
        final String imageURL = "localhost";
        final TimePoint raceTimepoint = new MillisecondsTimePoint(1000);
        final String updatedTag = "Upd/ated %Ta!g!���";
        final String updatedComment = "New comment...";
        final String updatedHiddenInfo = "New hidden info...";
        final String updatedImageURL = "";
        // add tag
        taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, hiddenInfo, imageURL, imageURL,
                false, raceTimepoint);
        TagDTO tagObject = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false).get(0);
        // update tag title
        taggingService.updateTag(leaderboardName, raceColumnName, fleetName, tagObject, updatedTag, comment, hiddenInfo,
                imageURL, imageURL, false);
        tagObject = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false).get(0);
        assertEquals(updatedTag, tagObject.getTag(), "Updated tag title");
        // update comment
        taggingService.updateTag(leaderboardName, raceColumnName, fleetName, tagObject, tag, updatedComment, hiddenInfo,
                imageURL, imageURL, false);
        tagObject = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false).get(0);
        assertEquals(updatedComment, tagObject.getComment(), "Updated comment");
        // update hidden info
        taggingService.updateTag(leaderboardName, raceColumnName, fleetName, tagObject, tag, updatedComment, updatedHiddenInfo,
                imageURL, imageURL, false);
        tagObject = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false).get(0);
        assertEquals(updatedHiddenInfo, tagObject.getHiddenInfo(), "Updated hidden info");
        // update image URL
        taggingService.updateTag(leaderboardName, raceColumnName, fleetName, tagObject, tag, comment, hiddenInfo,
                updatedImageURL, updatedImageURL, false);
        tagObject = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false).get(0);
        assertEquals(updatedImageURL, tagObject.getImageURL(), "Updated image URL");
        // update visibility (private -> public)
        taggingService.updateTag(leaderboardName, raceColumnName, fleetName, tagObject, tag, comment, hiddenInfo,
                imageURL, imageURL, true);
        tagObject = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false).get(0);
        assertEquals(true, tagObject.isVisibleForPublic(), "Updated visibility (private -> public)");
        // update visibility (public -> private)
        taggingService.updateTag(leaderboardName, raceColumnName, fleetName, tagObject, tag, comment, hiddenInfo,
                imageURL, imageURL, false);
        tagObject = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false).get(0);
        assertEquals(false, tagObject.isVisibleForPublic(), "Updated visibility (public -> private)");
        logger.exiting(getClass().getName(), "testUpdateTag");
    }

    @Test
    public void testRemoveTag() throws AuthorizationException, IllegalArgumentException, RaceLogNotFoundException,
            ServiceNotFoundException, TagAlreadyExistsException, NotRevokableException {
        logger.entering(getClass().getName(), "testRemoveTag");
        final String tag = "TagToRemove";
        final String comment = " Comment To Remove";
        final String imageURL = "localhost";
        final TimePoint raceTimepoint = new MillisecondsTimePoint(1);
        taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, /* hiddenInfo */ null, imageURL, imageURL,
                false, raceTimepoint);
        taggingService.addTag(leaderboardName, raceColumnName, fleetName, tag, comment, /* hiddenInfo */ null, imageURL, imageURL,
                true, raceTimepoint);
        final List<TagDTO> tags = taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false);
        assertEquals(2, tags.size(), "Tags were added successfully so they can be removed afterwards");
        for (TagDTO tagObject : tags) {
            taggingService.removeTag(leaderboardName, raceColumnName, fleetName, tagObject);
        }
        assertEquals(0, taggingService.getTags(leaderboardName, raceColumnName, fleetName, null, false).size(),
                "Tag list should be empty after deleting all tags");
        logger.exiting(getClass().getName(), "testRemoveTag");
    }
}
