package com.sap.sailing.domain.test.markpassing;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import com.sap.sailing.domain.markpassingcalculation.CandidateChooserImpl;
import com.sap.sailing.domain.markpassingcalculation.CandidateFinderImpl;
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

    private void setUp(String raceNumber) throws IOException, InterruptedException, URISyntaxException, ParseException {
        super.setUp();
        URI storedUri = new URI("file:///" + new File("resources/" + getFileName() + raceNumber + ".mtb").getCanonicalPath().replace('\\', '/'));
        super.setUp(new URL("file:///" + new File("resources/" + getFileName() + raceNumber + ".txt").getCanonicalPath()),
        /* liveUri */null, /* storedUri */storedUri,
                new ReceiverType[] { ReceiverType.MARKPASSINGS, ReceiverType.RACECOURSE, ReceiverType.MARKPOSITIONS, ReceiverType.RAWPOSITIONS });
        getTrackedRace().recordWind(new WindImpl(/* position */null, MillisecondsTimePoint.now(), new KnotSpeedWithBearingImpl(12, new DegreeBearingImpl(65))),
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
        }
    }

    protected abstract String getFileName();

    protected void testRace(String raceNumber) throws IOException, InterruptedException, URISyntaxException, ParseException {
        setUp(raceNumber);
        testWholeRace();
        testMiddleOfRace(0);
        testMiddleOfRace(2);
    }

    private void testWholeRace() {

        Map<Competitor, Map<Waypoint, MarkPassing>> computedPasses = new HashMap<>();

        // Get calculatedMarkPasses
        long time = System.currentTimeMillis();
        new MarkPassingCalculator(getTrackedRace(), false);
        time = System.currentTimeMillis() - time;
        
        for (Competitor c : getRace().getCompetitors()){
            computedPasses.put(c, new HashMap<Waypoint, MarkPassing>());
            for (Waypoint w : waypoints){
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
                    long timeDelta = givenPasses.get(c).get(w).getTimePoint().asMillis() - computedPasses.get(c).get(w).getTimePoint().asMillis();
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
        assertTrue(accuracy >= 0.8);
    }

    private void testMiddleOfRace(int waypoint) {
        CandidateFinderImpl finder = new CandidateFinderImpl(getTrackedRace());
        CandidateChooserImpl chooser = new CandidateChooserImpl(getTrackedRace());
        int mistakes = 0;
        Waypoint start = waypoints.get(waypoint);
        Waypoint second = waypoints.get(waypoint + 1);
        for (Competitor c : getRace().getCompetitors()) {
            TimePoint startTime;
            TimePoint secondPass;
            try {
                startTime = givenPasses.get(c).get(start).getTimePoint();
                secondPass = givenPasses.get(c).get(second).getTimePoint();
            } catch (NullPointerException e) {
                continue;
            }
            TimePoint delta = new MillisecondsTimePoint(startTime.plus(secondPass.asMillis()).asMillis() / 2);
            List<GPSFix> fixes = new ArrayList<GPSFix>();
            try {
                getTrackedRace().getTrack(c).lockForRead();
                for (GPSFixMoving fix : getTrackedRace().getTrack(c).getFixes()) {
                    if (fix.getTimePoint().before(delta)) {
                        fixes.add(fix);
                    }
                }
            } finally {
                getTrackedRace().getTrack(c).unlockAfterRead();
            }
            Pair<Iterable<Candidate>, Iterable<Candidate>> f = finder.getCandidateDeltas(c, fixes);
            chooser.calculateMarkPassDeltas(c, f.getA(), f.getB());
            boolean gotPassed = true;
            boolean gotOther = false;
         //   System.out.println(c);
            for (Waypoint w : getRace().getCourse().getWaypoints()) {
                MarkPassing old = givenPasses.get(c).get(w);
                MarkPassing newm = getTrackedRace().getMarkPassing(c, w);
             //   System.out.println(newm);
                if (waypoints.indexOf(w) <= waypoint) {
                    if ((old == null) != (newm == null)) {
                        gotPassed = false;
                    }
                } else {
                    if (newm != null) {
                        gotOther = true;
                    }
                }
            }
            if (!gotPassed || gotOther) {
                mistakes++;
            }
        }
        Assert.assertTrue(mistakes == 0);
    }

    @AfterClass
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