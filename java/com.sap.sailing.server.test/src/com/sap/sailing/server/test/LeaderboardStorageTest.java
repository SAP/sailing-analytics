package com.sap.sailing.server.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.bson.Document;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.impl.DynamicPerson;
import com.sap.sailing.domain.base.impl.DynamicTeam;
import com.sap.sailing.domain.base.impl.NationalityImpl;
import com.sap.sailing.domain.base.impl.PersonImpl;
import com.sap.sailing.domain.base.impl.TeamImpl;
import com.sap.sailing.domain.leaderboard.Leaderboard;
import com.sap.sailing.domain.leaderboard.impl.LowPoint;
import com.sap.sailing.server.impl.RacingEventServiceImpl;
import com.sap.sailing.server.interfaces.RacingEventService;
import com.sap.sse.mongodb.MongoDBService;

public class LeaderboardStorageTest {

    private static final String LEADERBOARD_NAME = "test";

    @BeforeEach
    protected void setUp() throws Exception {
        removeTestLeaderboard();
    }

    @AfterEach
    protected void tearDown() throws Exception {
        removeTestLeaderboard();
    }

    private void removeTestLeaderboard() {
        RacingEventService service = new RacingEventServiceImpl();
        if (service.getLeaderboardByName(LEADERBOARD_NAME) != null) {
            service.removeLeaderboard(LEADERBOARD_NAME);
        }
    }

    @Test
    public void testIfCarriedPointsAreStoredIfNoRacesAreTracked() {
        RacingEventService service = new RacingEventServiceImpl();
        int[] dicardingThresholds = {};
        Leaderboard leaderboard = service.addFlexibleLeaderboard(LEADERBOARD_NAME, "testIt", dicardingThresholds,
                new LowPoint(), Collections.singleton(service.getBaseDomainFactory().getOrCreateCourseArea(UUID.randomUUID(), "maaap", /* centerPosition */ null, /* radius */ null).getId()));
        List<DynamicPerson> sailorList = new ArrayList<DynamicPerson>();
        sailorList.add(new PersonImpl("sailor", new NationalityImpl("GER"), null, ""));
        DynamicTeam team = new TeamImpl("team", sailorList, null);
        String competitorId = "testC";
        Competitor competitor = service.getBaseDomainFactory().getOrCreateCompetitor(competitorId, "Test C", null, null,
                null, null, team, /* timeOnTimeFactor */ null, /* timeOnDistanceAllowanceInSecondsPerNauticalMile */ null, null, /* storePersistently */ true);
        double carriedPoints = 2.0;
        leaderboard.setCarriedPoints(competitor, carriedPoints);
        service.getMongoObjectFactory().storeLeaderboard(leaderboard);
        // Test in db
        MongoCollection<Document> leaderboardCollection = MongoDBService.INSTANCE.getDB().getCollection("LEADERBOARDS");
        BasicDBObject query = new BasicDBObject();
        query.put("LEADERBOARD_NAME", LEADERBOARD_NAME);
        FindIterable<Document> leaderboardObjectCursor = leaderboardCollection.find();
        Document dbLeaderboard = leaderboardObjectCursor.iterator().next();
        Iterable<?> carriedPointsById = (Iterable<?>) dbLeaderboard.get("LEADERBOARD_CARRIED_POINTS_BY_ID");
        if (carriedPointsById != null) {
            for (Object o : carriedPointsById) {
                Document competitorIdAndCarriedPoints = (Document) o;
                Serializable competitorIdFromDB = (Serializable) competitorIdAndCarriedPoints.get("COMPETITOR_ID");
                Double carriedPointsForCompetitor = ((Number) competitorIdAndCarriedPoints
                        .get("LEADERBOARD_CARRIED_POINTS")).doubleValue();
                assertEquals(competitorId, competitorIdFromDB);
                assertEquals(carriedPoints, carriedPointsForCompetitor, 0.0001);

            }
        }
    }

}
