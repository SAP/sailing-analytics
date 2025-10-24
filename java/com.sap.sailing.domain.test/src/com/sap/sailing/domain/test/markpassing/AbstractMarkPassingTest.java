package com.sap.sailing.domain.test.markpassing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.junit.jupiter.api.AfterAll;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.WindImpl;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.common.tracking.GPSFixMoving;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.markpassingcalculation.CandidateChooser;
import com.sap.sailing.domain.markpassingcalculation.CandidateFinder;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateChooserImpl;
import com.sap.sailing.domain.markpassingcalculation.impl.CandidateFinderImpl;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.test.measurements.Measurement;
import com.sap.sailing.domain.test.measurements.MeasurementCase;
import com.sap.sailing.domain.test.measurements.MeasurementXMLFile;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tractracadapter.ReceiverType;
import com.sap.sse.common.TimePoint;
import com.sap.sse.common.Util;
import com.sap.sse.common.impl.DegreeBearingImpl;
import com.sap.sse.common.impl.MillisecondsTimePoint;

public abstract class AbstractMarkPassingTest extends OnlineTracTracBasedTest {
    private static final Logger logger = Logger.getLogger(AbstractMarkPassingTest.class.getName());
    private Map<Competitor, Map<Waypoint, MarkPassing>> givenPasses = new HashMap<>();
    private List<Waypoint> waypoints = new ArrayList<>();
    private static String className;
    private static String simpleName;
    private static double totalPasses = 0;
    private static double correct = 0;
    private static double incorrect = 0;
    private static double skipped = 0;
    private static double extra = 0;

    public AbstractMarkPassingTest() throws MalformedURLException, URISyntaxException {
        super();
        className = getClass().getName();
        simpleName = getClass().getSimpleName();
    }

    protected void setUp(String raceNumber) throws Exception {
        super.setUp();
        URI storedUri = new URI("file:///"
                + new File("resources/" + getFileName() + raceNumber + ".mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///" + new File("resources/" + getFileName() + raceNumber + ".txt").getCanonicalPath()),
        /* liveUri */null, /* storedUri */storedUri, new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE,
                ReceiverType.MARKPOSITIONS, ReceiverType.RAWPOSITIONS });
        getTrackedRace().recordWind(
                new WindImpl(/* position */null, MillisecondsTimePoint.now(), new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(65))),
                new WindSourceImpl(WindSourceType.WEB));

        for (Waypoint w : getRace().getCourse().getWaypoints()) {
            waypoints.add(w);
        }
        for (Competitor c : getRace().getCompetitors()) {
            Map<Waypoint, MarkPassing> givenMarkPasses = new HashMap<Waypoint, MarkPassing>();
            for (Waypoint wp : waypoints) {
                MarkPassing markPassing = getTrackedRace().getMarkPassing(c, wp);
                givenMarkPasses.put(wp, markPassing);
            }
            givenPasses.put(c, givenMarkPasses);
            // now clear the mark passings in the TrackedRace for competitor c:
            getTrackedRace().updateMarkPassings(c, Collections.emptySet());
        }
    }

    protected Iterable<Waypoint> getWaypoints() {
        return waypoints;
    }
    
    protected Competitor getCompetitorByName(final String name) {
        Competitor result = null;
        for (Competitor c : getTrackedRace().getRace().getCompetitors()) {
            if (c.getName().equals(name)) {
                result = c;
                break;
            }
        }
        return result;
    }
    
    protected abstract String getFileName();

    protected void testRace(String raceNumber) throws Exception {
        setUp(raceNumber);
        synchronized (getSemaphor()) {
            while (!isStoredDataLoaded()) {
                getSemaphor().wait();
            }
        }
        testWholeRace();
        testMiddleOfRace(0);
        testMiddleOfRace(2);
    }

    private void testWholeRace() {
        Map<Competitor, Map<Waypoint, MarkPassing>> computedPasses = new HashMap<>();
        // Get calculatedMarkPasses
        long time = System.currentTimeMillis();
        MarkPassingCalculator mpc = new MarkPassingCalculator(getTrackedRace(), false, /* waitForInitialMarkPassingCalculation */ true, /* markPassingRaceFingerprintRegistry */ null);
        time = System.currentTimeMillis() - time;

        for (Competitor c : getRace().getCompetitors()) {
            computedPasses.put(c, new HashMap<Waypoint, MarkPassing>());
            for (Waypoint w : waypoints) {
                computedPasses.get(c).put(w, getTrackedRace().getMarkPassing(c, w));
            }
        }

        // Compare computed and calculated MarkPassings
        final int tolerance = 10000;
        double numberOfCompetitors = 0;
        double wronglyComputed = 0;
        double wronglyNotComputed = 0;
        double correctlyNotComputed = 0;
        double correctPasses = 0;
        double incorrectPasses = 0;
        double incorrectStarts = 0;

        boolean printRight = false;
        boolean printWrong = true;
        boolean printResult = true;

        System.out.println(getTrackedRace().getStartOfRace());

        for (Competitor c : getRace().getCompetitors()) {
            numberOfCompetitors++;
            for (Waypoint w : waypoints) {
                if (givenPasses.get(c).get(w) == null && !(computedPasses.get(c).get(w) == null)) {
                    wronglyComputed++;
                    if (waypoints.indexOf(w) == 0) {
                        incorrectStarts++;
                    }
                    if (printWrong) {
                        System.out.println(waypoints.indexOf(w));
                        System.out.println("Given is null");
                        System.out.println(computedPasses.get(c).get(w) + "\n");
                    }
                } else if (computedPasses.get(c).get(w) == null && !(givenPasses.get(c).get(w) == null)) {
                    wronglyNotComputed++;
                    if (waypoints.indexOf(w) == 0) {
                        incorrectStarts++;
                    }
                    if (printWrong) {
                        System.out.println(waypoints.indexOf(w));
                        System.out.println("Computed is null");
                        System.out.println(givenPasses.get(c).get(w) + "\n");
                    }
                } else if (givenPasses.get(c).get(w) == null && computedPasses.get(c).get(w) == null) {
                    correctlyNotComputed++;
                    if (printRight) {
                        System.out.println(waypoints.indexOf(w));
                        System.out.println("Both null" + "\n");
                    }
                } else {
                    long timeDelta = givenPasses.get(c).get(w).getTimePoint().asMillis()
                            - computedPasses.get(c).get(w).getTimePoint().asMillis();
                    if ((Math.abs(timeDelta) < tolerance)) {
                        correctPasses++;
                        if (printRight) {
                            System.out.println(waypoints.indexOf(w));
                            System.out.println("Calculated: " + computedPasses.get(c).get(w));
                            System.out.println("Given: " + givenPasses.get(c).get(w));
                            System.out.println(timeDelta / 1000 + " s\n");
                        }
                    } else {
                        incorrectPasses++;
                        if (waypoints.indexOf(w) == 0) {
                            incorrectStarts++;
                        }
                        if (printWrong) {
                            System.out.println(waypoints.indexOf(w));
                            System.out.println("Calculated: " + computedPasses.get(c).get(w));
                            System.out.println("Given: " + givenPasses.get(c).get(w));
                            System.out.println(timeDelta / 1000 + "\n");
                        }

                    }
                }
            }
        }

        double totalMarkPasses = numberOfCompetitors * waypoints.size();
        assertEquals(totalMarkPasses, incorrectPasses + correctPasses + wronglyNotComputed + correctlyNotComputed + wronglyComputed, 0);
        double accuracy = (double) (correctPasses + correctlyNotComputed) / totalMarkPasses;
        if (printResult) {
            System.out.println("Total theoretical Passes: " + totalMarkPasses);
            System.out.println("Correct comparison: " + correctPasses);
            System.out.println("Incorrect comparison: " + incorrectPasses);
            System.out.println("Incorrect Starts: " + incorrectStarts);
            System.out.println("Correctly Null: " + correctlyNotComputed);
            System.out.println("Should be null but arent:" + wronglyComputed);
            System.out.println("Should not be null but are: " + wronglyNotComputed);
            System.out.println("accuracy: " + accuracy);
            System.out.println("Computation time: " + time + " ms");
        }
        totalPasses += totalMarkPasses;
        correct += correctPasses + correctlyNotComputed;
        incorrect += incorrectPasses;
        skipped += wronglyNotComputed;
        extra += wronglyComputed;
        assertTrue(accuracy >= 0.8, "Expected accuracy to be at least 0.8 but was " + accuracy);
        logger.info(mpc.toString());
    }

    private void testMiddleOfRace(int zeroBasedIndexOfLastWaypointToBePassed) {
        int mistakes = 0;
        CandidateFinder finder = new CandidateFinderImpl(getTrackedRace());
        CandidateChooser chooser = new CandidateChooserImpl(getTrackedRace());
        Waypoint lastWaypointToBePassed = waypoints.get(zeroBasedIndexOfLastWaypointToBePassed);
        Waypoint wayPointAfterwards = waypoints.get(zeroBasedIndexOfLastWaypointToBePassed + 1);
        for (Competitor c : getRace().getCompetitors()) {
            MarkPassing markPassingAfter = givenPasses.get(c).get(wayPointAfterwards);
            if (markPassingAfter != null) {
                if (givenPasses.get(c).get(lastWaypointToBePassed) != null) {
                    final TimePoint lastGivenPassingToDetectAt = givenPasses.get(c).get(lastWaypointToBePassed).getTimePoint();
                    // add fixes up to 50% into the leg starting after the last passing to be detected
                    final TimePoint beforeNextPassing = lastGivenPassingToDetectAt.plus(lastGivenPassingToDetectAt.until(markPassingAfter.getTimePoint()).times(0.5));
                    calculateMarkPassingsForPartialTrack(c, beforeNextPassing, finder, chooser);
                    boolean gotPassed = true;
                    boolean gotOther = false;
                    System.out.println(c);
                    for (Waypoint w : getRace().getCourse().getWaypoints()) {
                        MarkPassing old = givenPasses.get(c).get(w);
                        MarkPassing newm = getTrackedRace().getMarkPassing(c, w);
                        System.out.println(newm);
                        if (waypoints.indexOf(w) <= zeroBasedIndexOfLastWaypointToBePassed) {
                            if ((old == null) != (newm == null)) {
                                gotPassed = false;
                                fail("Waypoint "+w+" was "+(old == null?"not ":"")+"passed by "+c+" originally"
                                        +(old==null?"":": "+old)+"; we detected it "+(newm==null?"not ":"")+"having been passed"+
                                        (newm == null ? "" : (": "+newm)));
                            }
                        } else {
                            if (w != wayPointAfterwards && newm != null) {
                                gotOther = true;
                                fail("Received a park passing "+newm+" for the "+waypoints.indexOf(w)+
                                        "th waypoint "+w+" by "+c+" although only up to "+
                                        zeroBasedIndexOfLastWaypointToBePassed +" were to be reported");
                            }
                        }
                    }
                    if (!gotPassed || gotOther) {
                        mistakes++;
                    }
                }
            }
        }
        assertEquals(0, mistakes);

    }

    protected void calculateMarkPassingsForPartialTrack(Competitor c, final TimePoint upToTimePoint,
            CandidateFinder finder, CandidateChooser chooser) {
        List<GPSFixMoving> fixes = new ArrayList<>();
        try {
            getTrackedRace().getTrack(c).lockForRead();
            for (GPSFixMoving fix : getTrackedRace().getTrack(c).getFixes()) {
                if (fix.getTimePoint().before(upToTimePoint)) {
                    fixes.add(fix);
                }
            }
        } finally {
            getTrackedRace().getTrack(c).unlockAfterRead();
        }
        Util.Pair<Iterable<Candidate>, Iterable<Candidate>> f = finder.getCandidateDeltas(c, fixes);
        chooser.calculateMarkPassDeltas(c, f.getA(), f.getB());
    }

    @AfterAll
    public static void createXML() throws IOException {
        double accuracy = correct / totalPasses;
        double different = incorrect / totalPasses;
        double allSkipped = skipped / totalPasses;
        double allExtra = extra / totalPasses;
        System.out.println(totalPasses);
        System.out.println(accuracy);
        System.out.println(different);
        System.out.println(allSkipped);
        System.out.println(allExtra);
        MeasurementXMLFile performanceReport = new MeasurementXMLFile("TEST-" + simpleName + ".xml", simpleName, className);
        MeasurementCase performanceReportCase = performanceReport.addCase(simpleName);
        performanceReportCase.addMeasurement(new Measurement("Accurate", accuracy));
        performanceReportCase.addMeasurement(new Measurement("Different", different));
        performanceReportCase.addMeasurement(new Measurement("Skipped", allSkipped));
        performanceReportCase.addMeasurement(new Measurement("Extra", allExtra));
        performanceReport.write();
    }
}