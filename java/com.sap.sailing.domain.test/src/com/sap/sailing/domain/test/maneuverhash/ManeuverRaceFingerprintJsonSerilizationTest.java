package com.sap.sailing.domain.test.maneuverhash;



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
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprint;
import com.sap.sailing.domain.maneuverhash.ManeuverRaceFingerprintFactory;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.tracking.impl.DynamicTrackedRaceImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public class ManeuverRaceFingerprintJsonSerilizationTest extends OnlineTracTracBasedTest {
    DynamicTrackedRaceImpl trackedRace1;
    DynamicTrackedRaceImpl trackedRace2;
    MarkPassingCalculator calculator1;
    MarkPassingCalculator calculator2;

    public ManeuverRaceFingerprintJsonSerilizationTest() throws MalformedURLException, URISyntaxException {
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
        //calculator1 = new MarkPassingCalculator(trackedRace1, false, false, /* markPassingRaceFingerprintRegistry */ null);
        //calculator2 = new MarkPassingCalculator(trackedRace2, false, false, /* markPassingRaceFingerprintRegistry */ null);
    }

    @Test
    public void testJsonSerialization() {
        ManeuverRaceFingerprintFactory factory = ManeuverRaceFingerprintFactory.INSTANCE;
        ManeuverRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
        assertTrue(fingerprint1.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        ManeuverRaceFingerprint output1 = factory.fromJson(json1);
        assertTrue(output1.matches(trackedRace1), "Original and de-serialized copy are equal");
        assertEquals(fingerprint1, output1);
        assertEquals(fingerprint1.hashCode(), output1.hashCode());
    }

    @Test
    public void testCompetitorFixChange() {
        DynamicTrackedRaceImpl testRace = trackedRace2;
        final DynamicTrackedRaceImpl secureRace = trackedRace2;
        ManeuverRaceFingerprintFactory factory = ManeuverRaceFingerprintFactory.INSTANCE;
        ManeuverRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
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
        ManeuverRaceFingerprint fingerprint2 = factory.createFingerprint(testRace);
        assertFalse(fingerprint1.matches(testRace));
        assertFalse(fingerprint2.matches(trackedRace1));
        JSONObject json1 = fingerprint1.toJson();
        JSONObject json2 = fingerprint2.toJson();
        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
    }
    
//    @Test
//    public void testWindChange() {
//        DynamicTrackedRaceImpl testRace = trackedRace2;
//        final DynamicTrackedRaceImpl secureRace = trackedRace2;
//        ManeuverRaceFingerprintFactory factory = ManeuverRaceFingerprintFactory.INSTANCE;
//        ManeuverRaceFingerprint fingerprint1 = factory.createFingerprint(trackedRace1);
//        assertTrue(fingerprint1.matches(trackedRace2));
//        testRace.getWindStore().put();
//        ManeuverRaceFingerprint fingerprint2 = factory.createFingerprint(testRace);
//        assertFalse(fingerprint1.matches(testRace));
//        assertFalse(fingerprint2.matches(trackedRace1));
//        JSONObject json1 = fingerprint1.toJson();
//        JSONObject json2 = fingerprint2.toJson();
//        assertNotEquals(json1, json2, "Json1 and Json2 are equal: " + json1 + " json2: " + json2);
//    }
}