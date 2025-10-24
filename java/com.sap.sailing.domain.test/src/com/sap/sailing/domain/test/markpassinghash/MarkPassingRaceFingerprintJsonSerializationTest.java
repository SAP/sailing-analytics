package com.sap.sailing.domain.test.markpassinghash;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.json.simple.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sap.sailing.domain.abstractlog.impl.LogEventAuthorImpl;
import com.sap.sailing.domain.abstractlog.race.RaceLogFixedMarkPassingEvent;
import com.sap.sailing.domain.abstractlog.race.RaceLogSuppressedMarkPassingsEvent;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogFixedMarkPassingEventImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogImpl;
import com.sap.sailing.domain.abstractlog.race.impl.RaceLogSuppressedMarkPassingsEventImpl;
import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.ControlPoint;
import com.sap.sailing.domain.base.ControlPointWithTwoMarks;
import com.sap.sailing.domain.base.Mark;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.base.impl.ControlPointWithTwoMarksImpl;
import com.sap.sailing.domain.base.impl.MarkImpl;
import com.sap.sailing.domain.base.impl.WaypointImpl;
import com.sap.sailing.domain.common.PassingInstruction;
import com.sap.sailing.domain.common.Position;
import com.sap.sailing.domain.common.SpeedWithBearing;
import com.sap.sailing.domain.common.abstractlog.NotRevokableException;
import com.sap.sailing.domain.common.impl.DegreePosition;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.common.tracking.impl.GPSFixImpl;
import com.sap.sailing.domain.common.tracking.impl.GPSFixMovingImpl;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprint;
import com.sap.sailing.domain.markpassinghash.MarkPassingRaceFingerprintFactory;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class MarkPassingRaceFingerprintJsonSerializationTest extends OnlineTracTracBasedTest {
    DynamicTrackedRaceImpl trackedRace1;
    DynamicTrackedRaceImpl trackedRace2;
    MarkPassingCalculator calculator1;
    MarkPassingCalculator calculator2;

    public MarkPassingRaceFingerprintJsonSerializationTest() throws MalformedURLException, URISyntaxException {
        super();
    }

    @Override
    protected String getExpectedEventName() {
        return "Academy Tracking 2011";
    }

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp("event_20110505_SailingTea", // Semifinale
                /* raceId */ "01ea3604-02ef-11e1-9efc-406186cbf87c", /* liveUri */ null, /* storedUri */ null,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE,
                        ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS, ReceiverType.SENSORDATA });
        trackedRace1 = getTrackedRace();
        trackedRace1.attachRaceLog(new RaceLogImpl(UUID.randomUUID()));
        super.setUp();
        super.setUp("event_20110505_SailingTea", // Semifinale
                /* raceId */ "01ea3604-02ef-11e1-9efc-406186cbf87c", /* liveUri */ null, /* storedUri */ null,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.MARKPOSITIONS, ReceiverType.RACECOURSE,
                        ReceiverType.RACESTARTFINISH, ReceiverType.RAWPOSITIONS, ReceiverType.SENSORDATA });
        trackedRace2 = getTrackedRace();
        trackedRace2.attachRaceLog(new RaceLogImpl(UUID.randomUUID()));
        calculator1 = new MarkPassingCalculator(trackedRace1, false, false, /* markPassingRaceFingerprintRegistry */ null);
        calculator2 = new MarkPassingCalculator(trackedRace2, false, false, /* markPassingRaceFingerprintRegistry */ null);
    }

    @Test
    public void testJsonSerialization() {
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        MarkPassingRaceFingerprint output1 = factory.fromJson(json1);
        assertTrue(output1.matches(trackedRace1), "Original and de-serialized copy are equal");
        assertEquals(fingerprint1, output1);
        assertEquals(fingerprint1.hashCode(), output1.hashCode());
    }
    
    @Test
    public void testJsonSerializationWithChangesInMarkFixes() {
        DynamicTrackedRaceImpl testRace = trackedRace2;
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace2));
        // Change of the race should result in a different hash
        TimePoint epoch = new MillisecondsTimePoint(0l);
        TimePoint now = MillisecondsTimePoint.now();
        Map<String, Position> markPositions = new HashMap<String, Position>();
        markPositions.put("CR Start - 1", new DegreePosition(53.562944999999985, 10.010104000000046));
        markPositions.put("CR Start - 2", new DegreePosition(53.562944999999985, 10.010104000000046));
        markPositions.put("Leeward mark", new DegreePosition(53.562145000000015, 10.009252));
        markPositions.put("Luvtonne", new DegreePosition(53.560581899999995, 10.005657));
        for (Waypoint w : trackedRace2.getRace().getCourse().getWaypoints()) {
            for (Mark mark : w.getMarks()) {
                assert markPositions.containsKey(mark.getName());
                testRace.getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()), epoch));
                testRace.getOrCreateTrack(mark).addGPSFix(new GPSFixImpl(markPositions.get(mark.getName()), now));
            }
        }
        assertFalse(fingerprint1.matches(trackedRace2));
        MarkPassingRaceFingerprint fingerprint2 = factory.createFingerprint(testRace);
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }

    @Test
    public void testWaypointChangePassingInstruction() {
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace2));
        Waypoint wp = trackedRace2.getRace().getCourse().getFirstWaypoint();
        ControlPoint cP = wp.getControlPoint();
        WaypointImpl wpNew = new WaypointImpl(cP, PassingInstruction.Gate);
        trackedRace2.getRace().getCourse().removeWaypoint(0);
        trackedRace2.getRace().getCourse().addWaypoint(0, wpNew);
        MarkPassingRaceFingerprint fingerprint2 = factory.createFingerprint(trackedRace2);
        assertFalse(fingerprint1.matches(trackedRace2));
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }

    @Test
    public void testControlPointChange() {
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace2));
        Mark gate1 = new MarkImpl("Gate1");
        Mark gate2 = new MarkImpl("Gate2");
        ControlPointWithTwoMarks cp = new ControlPointWithTwoMarksImpl(gate1, gate2, "cp", "");
        Waypoint wpNew = new WaypointImpl(cp, PassingInstruction.None);
        trackedRace2.getRace().getCourse().removeWaypoint(0);
        trackedRace2.getRace().getCourse().addWaypoint(0, wpNew);
        MarkPassingRaceFingerprint fingerprint2 = factory.createFingerprint(trackedRace2);
        assertFalse(fingerprint1.matches(trackedRace2));
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }

    @Test
    public void testFixedMarkPassingAddition() {
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace2));
        trackedRace2.getAttachedRaceLogs().iterator().next().add(new RaceLogFixedMarkPassingEventImpl(TimePoint.now(), new LogEventAuthorImpl("me", 0),
                trackedRace2.getRace().getCompetitors().iterator().next(), /* pPassId */ 1, TimePoint.now(), 0));
        MarkPassingRaceFingerprint fingerprint2 = factory.createFingerprint(trackedRace2);
        assertFalse(fingerprint1.matches(trackedRace2));
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }

    @Test
    public void testFixedMarkPassingRevocation() throws NotRevokableException {
        final LogEventAuthorImpl author = new LogEventAuthorImpl("me", 0);
        final RaceLogFixedMarkPassingEvent fixedMarkPassingEvent = new RaceLogFixedMarkPassingEventImpl(TimePoint.now(), author,
                trackedRace2.getRace().getCompetitors().iterator().next(), /* pPassId */ 1, TimePoint.now(), 0);
        trackedRace1.getAttachedRaceLogs().iterator().next().add(fixedMarkPassingEvent);
        trackedRace2.getAttachedRaceLogs().iterator().next().add(fixedMarkPassingEvent);
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace2));
        trackedRace2.getAttachedRaceLogs().iterator().next().revokeEvent(author, fixedMarkPassingEvent);
        MarkPassingRaceFingerprint fingerprint2 = factory.createFingerprint(trackedRace2);
        assertFalse(fingerprint1.matches(trackedRace2));
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }

    @Test
    public void testMarkPassingSuppressionAddition() {
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace2));
        trackedRace2.getAttachedRaceLogs().iterator().next().add(new RaceLogSuppressedMarkPassingsEventImpl(TimePoint.now(), new LogEventAuthorImpl("me", 0),
                trackedRace2.getRace().getCompetitors().iterator().next(), /* pPassId */ 1, 0));
        MarkPassingRaceFingerprint fingerprint2 = factory.createFingerprint(trackedRace2);
        assertFalse(fingerprint1.matches(trackedRace2));
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }

    @Test
    public void testMarkPassingSuppressionAdditionRevocation() throws NotRevokableException {
        final LogEventAuthorImpl author = new LogEventAuthorImpl("me", 0);
        final RaceLogSuppressedMarkPassingsEvent fixedMarkPassingEvent = new RaceLogSuppressedMarkPassingsEventImpl(TimePoint.now(), author,
                trackedRace2.getRace().getCompetitors().iterator().next(), /* pPassId */ 1, 0);
        trackedRace1.getAttachedRaceLogs().iterator().next().add(fixedMarkPassingEvent);
        trackedRace2.getAttachedRaceLogs().iterator().next().add(fixedMarkPassingEvent);
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace2));
        trackedRace2.getAttachedRaceLogs().iterator().next().revokeEvent(author, fixedMarkPassingEvent);
        MarkPassingRaceFingerprint fingerprint2 = factory.createFingerprint(trackedRace2);
        assertFalse(fingerprint1.matches(trackedRace2));
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }

    @Test
    public void testCompetitorFixChange() {
        DynamicTrackedRaceImpl testRace = trackedRace2;
        final DynamicTrackedRaceImpl secureRace = trackedRace2;
        MarkPassingRaceFingerprintFactory factory = MarkPassingRaceFingerprintFactory.INSTANCE;
        MarkPassingRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace2));
        Competitor firstCompetitor = trackedRace2.getRace().getCompetitors().iterator().next();
        secureRace.getTrack(firstCompetitor).lockForRead();
        GPSFixMoving firstFix;
        try {
            firstFix = testRace.getTrack(firstCompetitor).getRawFixes().iterator().next();
        } finally {
            secureRace.getTrack(firstCompetitor).unlockAfterRead();
        }
        SpeedWithBearing speed = firstFix.getSpeed();
        Position pos = firstFix.getPosition();
        TimePoint tp = firstFix.getTimePoint();
        DegreePosition degPos = new DegreePosition(pos.getLatDeg() + 0.05, pos.getLngDeg() + 0.05);
        GPSFixMoving gpsM = new GPSFixMovingImpl(degPos, tp, speed, /* optionalTrueHeading */ null);
        testRace.getTrack(firstCompetitor).add(gpsM, true);
        MarkPassingRaceFingerprint fingerprint2 = factory.createFingerprint(testRace);
        assertFalse(fingerprint1.matches(testRace));
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }
}