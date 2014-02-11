package com.sap.sailing.domain.test.markpassing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;

import com.sap.sailing.domain.base.Competitor;
import com.sap.sailing.domain.base.Waypoint;
import com.sap.sailing.domain.common.TimePoint;
import com.sap.sailing.domain.common.WindSourceType;
import com.sap.sailing.domain.common.impl.DegreeBearingImpl;
import com.sap.sailing.domain.common.impl.KnotSpeedWithBearingImpl;
import com.sap.sailing.domain.common.impl.MillisecondsTimePoint;
import com.sap.sailing.domain.common.impl.Util.Pair;
import com.sap.sailing.domain.common.impl.WindSourceImpl;
import com.sap.sailing.domain.markpassingcalculation.Candidate;
import com.sap.sailing.domain.markpassingcalculation.CandidateChooser;
import com.sap.sailing.domain.markpassingcalculation.CandidateFinder;
import com.sap.sailing.domain.markpassingcalculation.MarkPassingCalculator;
import com.sap.sailing.domain.test.OnlineTracTracBasedTest;
import com.sap.sailing.domain.test.measurements.Measurement;
import com.sap.sailing.domain.test.measurements.MeasurementCase;
import com.sap.sailing.domain.test.measurements.MeasurementXMLFile;
import com.sap.sailing.domain.tracking.GPSFix;
import com.sap.sailing.domain.tracking.GPSFixMoving;
import com.sap.sailing.domain.tracking.MarkPassing;
import com.sap.sailing.domain.tracking.impl.WindImpl;
import com.sap.sailing.domain.tractracadapter.ReceiverType;

public abstract class AbstractMarkPassingTest extends OnlineTracTracBasedTest {
    protected AbstractMarkPassingTest() throws MalformedURLException, URISyntaxException {
        super();
        className = getClass().getName();
        simpleName = getClass().getSimpleName();
        
    }

    protected abstract String getFileName();
    
    protected static String className;
    protected static String simpleName;

    private static List<Double> accuracys = new ArrayList<>();

    protected void testRace(int raceNumber) throws IOException, InterruptedException, URISyntaxException {
        setUp(raceNumber);
        testWholeRace();
        testStartOfRace();
    }

    private void setUp(int raceNumber) throws IOException, InterruptedException, URISyntaxException {
        super.setUp();
        URI storedUri = new URI("file:///" + new File("resources/" + getFileName() + raceNumber + ".mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///" + new File("resources/" + getFileName() + raceNumber + ".txt").getCanonicalPath()),
        /* liveUri */null, /* storedUri */storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.MARKPOSITIONS, ReceiverType.RAWPOSITIONS });
        getTrackedRace().recordWind(new WindImpl(/* position */null, MillisecondsTimePoint.now(), new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(65))),
                new WindSourceImpl(WindSourceType.WEB));
    }

    private void testWholeRace() {
        ArrayList<Waypoint> waypoints = new ArrayList<>();
        LinkedHashMap<Competitor, LinkedHashMap<Waypoint, MarkPassing>> computedPasses = new LinkedHashMap<>();
        LinkedHashMap<Competitor, LinkedHashMap<Waypoint, MarkPassing>> givenPasses = new LinkedHashMap<>();

        // Get Waypoints
        for (Waypoint w : getRace().getCourse().getWaypoints()) {
            waypoints.add(w);
        }

        // Get given Markpasses
        for (Competitor c : getRace().getCompetitors()) {
            LinkedHashMap<Waypoint, MarkPassing> givenMarkPasses = new LinkedHashMap<Waypoint, MarkPassing>();
            for (Waypoint wp : waypoints) {
                MarkPassing markPassing = getTrackedRace().getMarkPassing(c, wp);
                givenMarkPasses.put(wp, markPassing);
            }
            givenPasses.put(c, givenMarkPasses);
        }

        // Get calculatedMarkPasses
        long time = System.currentTimeMillis();
        final MarkPassingCalculator markPassCreator = new MarkPassingCalculator(getTrackedRace(), true);
        time = System.currentTimeMillis() - time;
        computedPasses = markPassCreator.getAllPasses();

        // Compare computed and calculated MarkPassings
        final int tolerance = 10000;
        int numberOfCompetitors = 0;
        int wronglyComputed = 0;
        int wronglyNotComputed = 0;
        int correctlyNotComputed = 0;
        int correctPasses = 0;
        int incorrectPasses = 0;
        int incorrectStarts = 0;

        boolean printRight = false;
        boolean printWrong = false;
        boolean printResult = true;

        for (Competitor c : getRace().getCompetitors()) {
            numberOfCompetitors++;
            for (Waypoint w : waypoints) {
                if (givenPasses.get(c).get(w) == null && !(computedPasses.get(c).get(w) == null)) {
                    wronglyComputed++;
                    if (printWrong) {
                        System.out.println(waypoints.indexOf(w));
                        System.out.println("Given is null");
                        System.out.println(computedPasses.get(c).get(w) + "\n");
                    }
                } else if (computedPasses.get(c).get(w) == null && !(givenPasses.get(c).get(w) == null)) {
                    wronglyNotComputed++;
                    if (waypoints.indexOf(w) == 0) {
                        incorrectStarts++;
                    } else if (printWrong) {
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
                    long timedelta = givenPasses.get(c).get(w).getTimePoint().asMillis() - computedPasses.get(c).get(w).getTimePoint().asMillis();
                    if ((Math.abs(timedelta) < tolerance)) {
                        correctPasses++;
                        if (printRight) {
                            System.out.println(waypoints.indexOf(w));
                            System.out.println("Calculated: " + computedPasses.get(c).get(w));
                            System.out.println("Given: " + givenPasses.get(c).get(w));
                            System.out.println(timedelta / 1000 + " s\n");
                        }
                    } else {
                        incorrectPasses++;
                        if (waypoints.indexOf(w) == 0) {
                            incorrectStarts++;
                        } else if (printWrong) {
                            System.out.println(waypoints.indexOf(w));
                            System.out.println("Calculated: " + computedPasses.get(c).get(w));
                            System.out.println("Given: " + givenPasses.get(c).get(w));
                            System.out.println(timedelta / 1000 + "\n");
                        }

                    }
                }
            }
        }

        int totalMarkPasses = numberOfCompetitors * waypoints.size();
        assertEquals(totalMarkPasses, incorrectPasses + correctPasses + wronglyNotComputed + correctlyNotComputed + wronglyComputed);
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
        accuracys.add(accuracy);
        assertTrue(accuracy >= 0.9);
    }

    private void testStartOfRace() {
        CandidateFinder finder = new CandidateFinder(getTrackedRace());
        CandidateChooser chooser = new CandidateChooser(getTrackedRace());
        int mistakes = 0;
        TimePoint t = getTrackedRace().getStartOfRace();
        for (Competitor c : getRace().getCompetitors()) {
            List<GPSFix> fixes = new ArrayList<GPSFix>();
            try {
                getTrackedRace().getTrack(c).lockForRead();
                for (GPSFixMoving fix : getTrackedRace().getTrack(c).getFixes()) {
                    if (fix.getTimePoint().minus(120000).before(t)) {
                        fixes.add(fix);
                    }
                }
            } finally {
                getTrackedRace().getTrack(c).unlockAfterRead();
            }
            Pair<List<Candidate>, List<Candidate>> f = finder.getCandidateDeltas(c, fixes);
            chooser.calculateMarkPassDeltas(c, f);
            Waypoint w1 = getRace().getCourse().getFirstWaypoint();
            boolean gotFirst = false;
            boolean gotOther = false;
            for (Waypoint w : getRace().getCourse().getWaypoints()) {
                // System.out.println(getTrackedRace().getMarkPassing(c, w));
                if (w == w1) {
                    gotFirst = (getTrackedRace().getMarkPassing(c, w) != null) ? true : false;
                } else {
                    if (getTrackedRace().getMarkPassing(c, w) != null) {
                        gotOther = true;
                    }
                }
            }
            if (!gotFirst || gotOther) {
                mistakes++;
            }
        }
        Assert.assertTrue(mistakes == 0);
    }

    @SuppressWarnings("unused")
    // TODO
    private void testFirstTwoWaypoints() {
        CandidateFinder finder = new CandidateFinder(getTrackedRace());
        CandidateChooser chooser = new CandidateChooser(getTrackedRace());
        int mistakes = 0;
        TimePoint t = getTrackedRace().getStartOfRace();
        for (Competitor c : getRace().getCompetitors()) {
            List<GPSFix> fixes = new ArrayList<GPSFix>();
            try {
                getTrackedRace().getTrack(c).lockForRead();
                for (GPSFixMoving fix : getTrackedRace().getTrack(c).getFixes()) {
                    if (fix.getTimePoint().minus(360000).before(t)) {
                        fixes.add(fix);
                    }
                }
            } finally {
                getTrackedRace().getTrack(c).unlockAfterRead();
            }
            Pair<List<Candidate>, List<Candidate>> f = finder.getCandidateDeltas(c, fixes);
            chooser.calculateMarkPassDeltas(c, f);
            Waypoint w1 = getRace().getCourse().getFirstWaypoint();
            Waypoint w2 = getRace().getCourse().getFirstLeg().getTo();
            boolean gotFirst = false;
            boolean gotSecond = false;
            boolean gotOther = false;
            for (Waypoint w : getRace().getCourse().getWaypoints()) {
                // System.out.println(getTrackedRace().getMarkPassing(c, w));
                if (w == w1) {
                    gotFirst = (getTrackedRace().getMarkPassing(c, w) != null) ? true : false;
                } else if (w == w2) {
                    gotSecond = (getTrackedRace().getMarkPassing(c, w) != null) ? true : false;
                } else if (getTrackedRace().getMarkPassing(c, w) != null) {
                    gotOther = true;

                }
            }
            if (!gotFirst || !gotSecond || gotOther) {
                mistakes++;
            }
        }
        Assert.assertTrue(mistakes == 0);
    }

    @AfterClass
    public static void createXML() throws IOException {
        double result = 0;
        for(Double d : accuracys){
            result += d;
        }
        result /= accuracys.size();
        MeasurementXMLFile performanceReport = new MeasurementXMLFile("TEST-"+simpleName+".xml", simpleName, className);
        MeasurementCase performanceReportCase = performanceReport.addCase(simpleName+" accuracy");
        performanceReportCase.addMeasurement(new Measurement(simpleName+" accuracy", result));
        performanceReport.write();
    }
}