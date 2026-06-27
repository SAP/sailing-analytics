package com.sap.sailing.mongodb.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bson.BsonMaximumSizeExceededException;
import org.bson.Document;
import org.json.simple.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Course;
import com.sap.sailing.domain.base.impl.CompetitorImpl;
import com.sap.sailing.domain.common.ManeuverType;
import com.sap.sailing.domain.common.RegattaNameAndRaceName;
import com.sap.sailing.domain.common.Tack;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.persistence.FieldNames;
import com.sap.sailing.domain.persistence.impl.MongoObjectFactoryImpl;
import com.sap.sailing.domain.tracking.Maneuver;
import com.sap.sailing.domain.tracking.ManeuverCurveBoundaries;
import com.sap.sailing.domain.tracking.impl.ManeuverCurveBoundariesImpl;
import com.sap.sailing.domain.tracking.impl.ManeuverWithMainCurveBoundariesImpl;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.DegreePosition;
import com.sap.sse.common.impl.KnotSpeedImpl;
import com.sap.sse.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;
import com.sap.sse.mongodb.MongoDBConfiguration;

/**
 * Regression test for bug 6226 (https://github.com/eclipse-sailing-analytics/sailing-analytics/issues/6226):
 * {@link BsonMaximumSizeExceededException} storing maneuvers for large races.
 * <p>
 *
 * The original fix stored one document per competitor instead of one per race. This test also covers the follow-up
 * case: a single competitor with enough maneuvers to exceed 16MB in one document. The implementation paginates
 * across multiple documents keyed by (EVENT_NAME, RACE_NAME, COMPETITOR_ID, MANEUVER_PAGE_INDEX).
 */
public class ManeuverStorageLimitTest {
    private static final int COMPETITOR_COUNT = 200;
    private static final int MANEUVERS_PER_COMPETITOR = 500;
    private static final int MANEUVERS_PER_PAGE = 1000;

    private MongoDBConfiguration dbConfiguration;
    private MongoDatabase database;

    @BeforeEach
    public void setUp() {
        dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoClient mongoClient = MongoClients.create(dbConfiguration.getMongoClientURI());
        database = mongoClient.getDatabase(dbConfiguration.getDatabaseName());
        database.drop();
    }
    
    @AfterEach
    public void tearDown() {
        dbConfiguration = MongoDBConfiguration.getDefaultTestConfiguration();
        final MongoClient mongoClient = MongoClients.create(dbConfiguration.getMongoClientURI());
        database = mongoClient.getDatabase(dbConfiguration.getDatabaseName());
        database.drop(); // dropping *after* the test is particularly important to clean up otherwise unreadable
                         // DUMMY/mock fingerprints
    }

    /**
     * Verifies that competitors with fewer than 1000 maneuvers each produce one document per competitor.
     */
    @Test
    public void testNewImplementationStoresOneDocumentPerCompetitor() {
        final RegattaNameAndRaceName raceIdentifier = new RegattaNameAndRaceName("505 Pre-Worlds 2014 synthetic", "Race 1");
        final Map<Competitor, List<Maneuver>> maneuvers = buildSyntheticManeuvers(COMPETITOR_COUNT, MANEUVERS_PER_COMPETITOR);
        final ManeuverRaceFingerprint fingerprint = buildMockFingerprint();
        final Course course = mock(Course.class);
        new MongoObjectFactoryImpl(database).storeManeuvers(raceIdentifier, fingerprint, course, maneuvers);
        final MongoCollection<Document> collection = database.getCollection("MANEUVERS");
        assertEquals(COMPETITOR_COUNT, collection.countDocuments(),
                "Expected one document per competitor in the MANEUVERS collection");
    }

    /**
     * Verifies that a competitor with 15000 maneuvers (enough to exceed the 16MB BSON limit in one document)
     * is split into 15 pages of 1000 maneuvers each, and all maneuvers are stored successfully.
     */
    @Test
    public void testPaginationSplitsLargeCompetitorAcrossMultipleDocuments() {
        final int totalManeuvers = 15000;
        final RegattaNameAndRaceName raceIdentifier = new RegattaNameAndRaceName("505 Pre-Worlds 2014 synthetic", "Race 1");
        final Competitor competitor = mock(CompetitorImpl.class);
        when(competitor.getId()).thenReturn("competitor-0");
        final Map<Competitor, List<Maneuver>> maneuvers = new HashMap<>();
        maneuvers.put(competitor, buildManeuverList(totalManeuvers));
        final ManeuverRaceFingerprint fingerprint = buildMockFingerprint();
        final Course course = mock(Course.class);
        new MongoObjectFactoryImpl(database).storeManeuvers(raceIdentifier, fingerprint, course, maneuvers);
        final MongoCollection<Document> collection = database.getCollection("MANEUVERS");
        final int expectedPages = (int) Math.ceil((double) totalManeuvers / MANEUVERS_PER_PAGE);
        assertEquals(expectedPages, collection.countDocuments(), "Expected " + expectedPages + " page documents for one competitor with " + totalManeuvers + " maneuvers");
        final FindIterable<Document> docs = collection.find(new Document(FieldNames.COMPETITOR_ID.name(), "competitor-0"));
        int totalManeuversLoaded = 0;
        for (final Document doc : docs) {
            final List<Document> page = doc.getList(FieldNames.MANEUVERS.name(), Document.class);
            totalManeuversLoaded += page != null ? page.size() : 0;
        }
        assertEquals(totalManeuvers, totalManeuversLoaded, "Expected all maneuvers to be stored across pages");
    }

    private ManeuverRaceFingerprint buildMockFingerprint() {
        final ManeuverRaceFingerprint fingerprint = mock(ManeuverRaceFingerprint.class);
        final JSONObject fingerprintJson = new JSONObject();
        fingerprintJson.put("DUMMY", "fingerprint");
        when(fingerprint.toJson()).thenReturn(fingerprintJson);
        return fingerprint;
    }

    private Map<Competitor, List<Maneuver>> buildSyntheticManeuvers(final int competitorCount, final int maneuversPerCompetitor) {
        final Map<Competitor, List<Maneuver>> result = new HashMap<>();
        for (int i = 0; i < competitorCount; i++) {
            final Competitor competitor = mock(CompetitorImpl.class);
            when(competitor.getId()).thenReturn("competitor-" + i);
            result.put(competitor, buildManeuverList(maneuversPerCompetitor));
        }
        return result;
    }

    private List<Maneuver> buildManeuverList(final int count) {
        final List<Maneuver> result = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            result.add(buildManeuver(i));
        }
        return result;
    }

    private Maneuver buildManeuver(final int index) {
        final long baseMillis = 1_400_000_000_000L + index * 30_000L;
        final ManeuverCurveBoundaries curveBoundaries = new ManeuverCurveBoundariesImpl(
                new MillisecondsTimePoint(baseMillis),
                new MillisecondsTimePoint(baseMillis + 15_000L),
                new KnotSpeedWithBearingImpl(6.0, new DegreeBearingImpl(45.0)),
                new KnotSpeedWithBearingImpl(5.5, new DegreeBearingImpl(315.0)),
                90.0,
                new KnotSpeedImpl(4.0),
                new KnotSpeedImpl(6.5));
        return new ManeuverWithMainCurveBoundariesImpl(
                ManeuverType.TACK,
                Tack.STARBOARD,
                new DegreePosition(47.5 + index * 0.001, 9.0 + index * 0.001),
                new MillisecondsTimePoint(baseMillis + 7_500L),
                curveBoundaries,
                curveBoundaries,
                5.0,
                null,
                null);
    }
}
